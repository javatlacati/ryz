/*
 * Copyright (c)  2010 - 2011, Oscar Reyes and Ryz language developers.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *     - Redistributions of source code must retain the above copyright notice,
 *  this list of conditions and the folLowing disclaimer.
 *     - Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 *     - Neither the name of Ryz nor the names of its contributors may be used
 * to endorse or promote products derived from this software without specific
 * prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES ( INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OR LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * ( INCLUDING NEGLIGENCE OR OTHERWISE ) ARISING IN ANY WAY OUT OF THE USE  OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package ryz.compiler;


import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import javax.tools.Diagnostic;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.System.err;


/**
 * This class represents the Ryz compiler, that will be eventually used
 * to create .class files from .ryz files.
 * <p/>
 * <p/>
 * User: oscarryz
 * Date: Sep 7, 2010
 * Time: 9:42:07 PM
 */
public class RyzC {

    private static final String CATCH_OR_THROW = "compiler.err.unreported.exception.need.to.catch.or.throw";
    private static final String CANT_DEREF     = "compiler.err.cant.deref";
    private static final String CANT_RESOLVE   = "compiler.err.cant.resolve.location";

    private static final Logger logger = Logger.getLogger( RyzC.class.getName() );
    private static Logger sourceLogger = Logger.getLogger(
                                            "ryz.compiler.RyzC.viewJavaSource" );
    private static final Map<Character, String> operatorMap = new HashMap<Character, String>() {{
        put( '+', "$plus" );
        put( '-', "$minus" );
        put( '*', "$star" );
        put( '/', "$slash" );
        put( '%', "$percent" );
        put( '<', "$lt" );
        put( '>', "$gt" );
        put( '=', "$eq" );
        put( '!', "$em" );
        put( '&', "$amp" );
        put( '^', "$up" );
        put( '|', "$bar" );
        put( '?', "$qm" );
        put( ':', "$colon" );
        //+ - * / % < > = ! & ^ | ? :
    }};
    private static final String METH_DOESNT_THROW = "compiler.err.override.meth.doesnt.throw";

    public static void main( String[] args ) throws IOException {
        RyzC c = RyzC.getCompiler();
        c.sourceDirs( new File( "." ) );
        c.outDir( new File( "." ) );
        // fixed to 2nd position
        if ( "-cp".equals( args[0] ) ) {
            c.classPath( args[1].split( System.getProperty( "path.separator" ) ));
            args = Arrays.copyOfRange( args, 2, args.length );
        }
        c.compile( args );
    }


    /**
     * Return a new instance of the Ryz Compiler.
     *
     * @return - A new instance of the compiler
     */
    public static RyzC getCompiler() {
        return new RyzC();
    }

    private RyzC() {
    }

    /**
     * Where to find .ryz source class default to current directory
     */
    private File[] sourceDirs = new File[]{new File( "" )};
    /**
     * Classpath for the compiler.
     */
    private File[] classPath = new File[]{
            new File("."),
            new File("./lib/"),
            new File("./out/build/"),
            new File(getClass()
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .getFile())
    };

    /**
     * Where to put the outputDir, default to current directory.
     */
    private File outputDir = new File( "" );

    /**
     * Holds a representation of the Ryz classes compiled during this session.
     */
    private final List<RyzClass> classes = new ArrayList<RyzClass>();

    /**
     * Specify the source path directories for the compiler to use.
     *
     * @param dirs - Where to find .ryz source files.
     */
    public void sourceDirs( File ... dirs ) {
        sourceDirs = dirs.clone();
    }

    /**
     * Specity the classpath for the compiler to use.
     *
     * @param filesName a list of files where to find classes for the compilation
     */
    public void classPath( String ... filesName ) {
        List<File> cp = new ArrayList<File>();
        cp.addAll( Arrays.asList( classPath ) );
        for ( String aFile : filesName ) {
            cp.add( new File( aFile ) );
        }
        classPath = cp.toArray( new File[cp.size()] );
    }

    /**
     * Specify where to put the generated .class files
     *
     * @param output - Specify the outputDir directory for the generated .class files
     */
    public void outDir( File output ) {
        this.outputDir = output;
    }

    /**
     * Compile the .ryz source files.
     *
     * @param files - The files to be compiled
     * @throws IOException - If there an IO problem while compiling
     */
    public void compile( String ... files ) throws IOException {
        for ( String file : files ) {
            File toCompile = validateExists( file );
            if ( toCompile == null ) {
                return;
            }

            RyzClass ryzclass = new RyzClass( file, cleanLines( readLines( toCompile ) ) );
            classes.add( ryzclass );
            ryzclass.transformSourceCode();
        }
        createClassDefinition( classes );
    }


    //TODO: Move to Utility class when the time comes

    /**
     * This method pre-process the input source file.
     * This helps to things like putting the single line comment in a separate line
     * TODO: keep the original line numbre.
     *
     * @param input - The original source code as lines
     * @return The same original source with small changes to parse it properly.
     */
    private List<String> cleanLines( List<String> input ) {
        List<String> result = trimLines( input );
        result = replaceSelfWithThis( result );
        result = splitLineComments( result );
        result = substituteNonTextMethodsDefinitions( result );
        result = substituteNonTextMethodsInvocations( result );
        return result;
    }

    private String getReplacement( Matcher m ) {
        String line = m.group( 0 );
        String methodName = m.group( 1 );
        StringBuilder sb = new StringBuilder(
                line.substring( 0, m.start( 1 ) - m.start( 0 ) )
        );
        for ( char c : methodName.toCharArray() ) {
            String mapped = operatorMap.get( c );
            sb.append( ( mapped == null ?
                                 c + "" :
                                 mapped ).replace( "$", "\\$" ) );
        }
        sb.append( line.substring( m.end( 1 ) - m.start( 0 ) ) );
        return sb.toString();
    }

    //TODO: refactor this method with: @substituteNonTextMethodsDefinitions
    private List<String> substituteNonTextMethodsInvocations( List<String> input ) {
        List<String> result = new ArrayList<String>();
        // The pattern matches for instance: .+ (
        Pattern pattern =
                Pattern.compile( "\\.\\s*([+\\-*/%<>=!&^|?:\\w]+)\\s*\\(" );

        for ( String line : input ) {
            if ( line.startsWith( "//" ) ) {
                continue;
            }
            Matcher m = pattern.matcher( line );
            StringBuffer sbf = new StringBuffer();
            while ( m.find() ) {
                String replacement = getReplacement( m );
                logger.finest( "line = " + line
                             + ", replacement = " + replacement );
                m.appendReplacement( sbf, replacement );
            }
            m.appendTail( sbf );
            result.add( sbf.toString() );
        }
        return result;
    }

    //TODO: refactor this method with: @substituteNonTextMethodsInvocations
    private List<String> substituteNonTextMethodsDefinitions( List<String> input ) {
        List<String> result = new ArrayList<String>();
        Pattern pattern = Pattern.compile( "[+#~-]??\\s*_{0,2}\\s*"
                                         + "([+\\-*/%<>=!&^|?:\\w]+)\\s*\\(" );

        for ( String line : input ) {
            if ( line.startsWith( "//" ) ) {
                continue;
            }
            Matcher m = pattern.matcher( line );
            StringBuffer sbf = new StringBuffer();
            if ( m.lookingAt() ) {
                String replacement = getReplacement( m );
                logger.finest( "line = " + line
                            + ", replacement = " + replacement );
                m.appendReplacement( sbf, replacement );
            }
            m.appendTail( sbf );
            result.add( sbf.toString() );
        }
        return result;
    }

    private List<String> trimLines( List<String> input ) {
        List<String> r = new ArrayList<String>();
        for ( String s : input ) {
            r.add( s.trim() );
        }
        return r;
    }

    private List<String> splitLineComments( List<String> input ) {
        List<String> trimmed = new ArrayList<String>();
        for ( String line : input ) {
            // if line starts with single line comment: "//"
            // then put that line in the next one.
            // to avoid problems.
            // TODO: this may cause problems when reporting line numbers
            if ( line.contains( "//" ) && !line.startsWith( "//" ) ) {
                String[] strings = line.split( "\\/\\/" );
                trimmed.add( strings[0].trim() );
                trimmed.add( "//" + strings[1].trim() );
            } else {
                trimmed.add( line );
            }
        }
        return trimmed;
    }

    private List<String> replaceSelfWithThis( List<String> input ) {
        List<String> result = new ArrayList<String>();
        for ( String line : input ) {
            result.add( line.replaceAll( "self\\.", "this." )
                       .replaceAll( "^self$", "this" )
            );
        }
        return result;
    }

    /**
     * Compiles a java source code file from the given RyzClass
     *
     * @param currentClasses - The class to be transformed into .class file
     * @throws IOException - If it is not possible to write the file
     */
    private void createClassDefinition( List<RyzClass> currentClasses )
    throws IOException {

        // Get the java compiler for this platform
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if ( compiler == null ) {
            throw new IllegalStateException( "Couldn't get java compiler. Make "
                        + "sure the javac is in the execution path. (HINT put "
                        + "JAVA_HOME/bin before in the path ) JAVA_HOME="
                        + System.getProperty( "java.home" ) );
        }
        StandardJavaFileManager fileManager =
                compiler.getStandardFileManager( null, null, null );

        //TODO: parameterize options
        Iterable<String> options =
                logger.isLoggable( Level.FINEST ) ? Arrays.asList( "-verbose" ) : null;

        fileManager.setLocation(
                StandardLocation.CLASS_OUTPUT, Arrays.asList( outputDir )
        );

        fileManager.setLocation(
                StandardLocation.CLASS_PATH, Arrays.asList( classPath )
        );

        DiagnosticCollector<JavaFileObject> collector
                                    = new DiagnosticCollector<JavaFileObject>();
        // Compile the files
        List<JavaSourceFromString> compilationUnits
                                        = new ArrayList<JavaSourceFromString>();

        for ( RyzClass currentClass : currentClasses ) {
            JavaSourceFromString javaSourceFromString =
                             new JavaSourceFromString( currentClass.className(),
                                    getGeneratedSourceCodeFrom( currentClass ) );


            compilationUnits.add( javaSourceFromString );
            //String generatedSourceCode = ;
            //JavaFileObject source = new JavaSourceFromString(currentClass.className(), generatedSourceCode);
        }


        boolean succesfullCompilation =
                compiler.getTask( null, fileManager, collector, options,
                                  null, compilationUnits ).call();

        if ( sourceLogger.isLoggable( Level.FINEST ) ) {
            //sourceLogger.finest(generatedSourceCode);
        }
        if ( ( !succesfullCompilation && logger.isLoggable( Level.FINE ) )
                || logger.isLoggable( Level.FINEST ) ) {
            logger.fine( collector.getDiagnostics().toString() );
            //sourceLogger.fine( numberedContent(generatedSourceCode) );
        }

        fileManager.close();

        if ( !succesfullCompilation ) {
            Map<String, DiagnosticList> diagnosticsMap = toMap( new DiagnosticList(collector.getDiagnostics()) );
            logger.warning( diagnosticsMap.toString() );
            reportException( currentClasses, diagnosticsMap.get( CATCH_OR_THROW ) );
            resolveSymbol(   currentClasses, diagnosticsMap.get( CANT_DEREF ) );
            resolveSymbol(   currentClasses, diagnosticsMap.get( CANT_RESOLVE) );
            removeException( currentClasses, diagnosticsMap.get( METH_DOESNT_THROW ) );
            createClassDefinition( currentClasses );
            cleanCheckedExceptions( currentClasses );
        }
        /*
                    for ( Diagnostic<? extends JavaFileObject> diagnostic : collector.getDiagnostics() ) {
                        if("compiler.err.unreported.exception.need.to.catch.or.throw".equals(diagnostic.getCode())){
                            createClassDefinition(currentClass.reportExceptions());
                            ClassInstrumentation.removeCheckedExceptions( currentClass, output );
                            return;
                        } else if ( ("compiler.err.cant.resolve.location".equals( diagnostic.getCode())
                                     || "compiler.err.cant.deref".equals( diagnostic.getCode()))
                                         && isDifferentProblem(currentClass, diagnostic)) {
                            createClassDefinition(resolveSymbol(currentClass, collector.getDiagnostics()));
                            return;
                        } else if("compiler.err.override.meth.doesnt.throw".equals(diagnostic.getCode())) {
                            createClassDefinition(removeException(currentClass, diagnostic,collector.getDiagnostics()));
                            return;
                        } else {
                            logger.info( collector.getDiagnostics().toString());
                            logger.info( diagnostic.getCode());
                            sourceLogger.info(numberedContent(generatedSourceCode));
                        }
                    }
                }
        */


        // delete the file
        //sourceFile.deleteOnExit();


    }

    private void cleanCheckedExceptions( List<RyzClass> currentClasses )
    throws IOException {
        for ( RyzClass ryzClass : currentClasses ) {
            ClassInstrumentation.removeCheckedExceptions( ryzClass, outputDir );
        }
    }

    private void reportException( List<RyzClass> currentClasses,
                                  List<Diagnostic<? extends JavaFileObject>> diagnostics ) {
        if ( diagnostics != null ) {
            for ( RyzClass ryzClass : currentClasses ) {
                ryzClass.reportExceptions();
            }
        }

    }

    private void removeException( List<RyzClass> currentClasses,
                                  List<Diagnostic<? extends JavaFileObject>> diagnostics ) {
        for ( RyzClass ryzClass : currentClasses ) {
            removeException( ryzClass, diagnostics );
        }
    }

    private void resolveSymbol( List<RyzClass> currentClasses,
                                List<Diagnostic<? extends JavaFileObject>> diagnostics ) {
        for ( RyzClass ryzClass : currentClasses ) {
            resolveSymbol( ryzClass, diagnostics );
        }
    }

    static final class DiagnosticList extends ArrayList<Diagnostic<? extends JavaFileObject>>{
        public DiagnosticList(){}
        public DiagnosticList( List<Diagnostic<? extends JavaFileObject>> diagnostics ) {
            super( diagnostics);
        }
    }
    private Map<String, DiagnosticList> toMap( DiagnosticList diagnostics ) {
        Map<String, DiagnosticList> result = new HashMap<String, DiagnosticList>();
        for ( Diagnostic<? extends JavaFileObject> d : diagnostics ) {
            DiagnosticList list = result.get(d.getCode());
            if( list == null ) {
              result.put( d.getCode(), (list = new DiagnosticList()));
            }
            list.add( d );
        }
        return result;
    }

    /**
     * Createa a string from the current class.
     *
     * @param currentClass - The class where to take the source code from.
     * @return a string with the generated source code.
     * @throws IOException - should not happen :P
     */
    private String getGeneratedSourceCodeFrom( RyzClass currentClass ) throws IOException {
        logger.finest( "className=[" + currentClass.className() + "]" );
        Writer writer = new StringWriter();

        //TODO: move this to the RyzClass
        // from here
        String packageString = "";
        List<String> outputLines = currentClass.outputLines();
        List<String> toRemove = new ArrayList<String>();
        boolean packageWritten = false;
        for ( String s : outputLines ) {
            if ( s.startsWith( "package" ) && !packageWritten ) {
                packageString = s;
                packageWritten = true;

            }
            if ( s.startsWith( "import" ) ) {
                toRemove.add( s );
            }
        }
        // Before writing put package and imports at the top
        outputLines.remove( packageString );
        outputLines.removeAll( toRemove );
        outputLines.addAll( 0, toRemove );
        outputLines.add( 0, packageString );
        // to here

        for ( String s : outputLines ) {
            writer.write( s );
        }
        writer.close();
        return writer.toString();
    }


    /**
     * Check if the given class has already have a problem in the same position.
     *
     * @param currentClass - The class with the problem
     * @param diagnostic   - The information of the new problem
     * @return true if the class have already had the same problem.
     */
    private boolean isDifferentProblem( RyzClass currentClass, Diagnostic<? extends JavaFileObject> diagnostic ) {
        return currentClass.isNewProblem( diagnostic.getCode(),
                                          diagnostic.getStartPosition(),
                                          diagnostic.getPosition() );
    }

    //TODO add testcase for this method
    private RyzClass removeException( RyzClass currentClass, List<Diagnostic<? extends JavaFileObject>> diagnostics ) {
        if ( diagnostics == null ) {
            return currentClass;
        }
        for ( Diagnostic<? extends JavaFileObject> diagnostic : diagnostics ) {
            //currentClass.generatedSource()
            // Take the source code from the diagnostic
            StringBuilder sb;
            try {
                sb = new StringBuilder( diagnostic.getSource().getCharContent( true ) );
            } catch ( IOException e ) {
                sb = new StringBuilder();
            }
            //sb.diagnostic.getLineNumber();
            // take information of the error.
            int startPosition = (int) diagnostic.getStartPosition();
            int position = (int) diagnostic.getPosition();
            int endPosition = (int) diagnostic.getEndPosition();
            String substring = logDiagnostic( diagnostic, sb, startPosition, position, endPosition );
            logger.finest( "substring.replace(\"throws Exception\",\"\") = "
                    + substring.replace( "throws Exception", "/*rows Excepti*/" ) );
            sb.replace( startPosition, endPosition,
                    substring.replace( " throws Exception { ", " /*te*/ {" ) );
            currentClass.markError( sb.toString(),
                    diagnostic.getCode(),
                    startPosition, position );
        }
        return currentClass;
    }

    private String logDiagnostic( Diagnostic<? extends JavaFileObject> diagnostic,
                                  StringBuilder sb,
                                  int startPosition,
                                  int position,
                                  int endPosition ) {
        logger.fine( "diagnostic.getColumnNumber() = " + diagnostic.getColumnNumber() );
        //logger.fine("diagnostic.getClass() = " + diagnostic.getClass());
        logger.fine( "startPosition = " + startPosition );
        logger.fine( "position = " + position );
        logger.fine( "endPosition = " + endPosition );
        String substring = sb.substring( startPosition, endPosition );
        logger.fine( "sb.substring(startPosition,endPosition) = " + substring );
        logger.fine( "sb.substring(startPosition,endPosition) = "
                    + sb.substring( startPosition, position ) );
        return substring;
    }


    /**
     * Tries to resolve the symbol by replacing the receiver as the first argument of the given method.
     * For instance if the symbol not found is "string.reverse()" tries to replace it for "reverse(string)"
     * and ask the current class to take the fixed source code and remember this error  to avoid it.
     *
     * @param currentClass - The class being fixed.
     * @param diagnostics  -
     * @return the instance of currentClass with fixed source code.
     */
    private RyzClass resolveSymbol( RyzClass currentClass,
                                    List<Diagnostic<? extends JavaFileObject>> diagnostics ) {
        if ( diagnostics == null ) {
            return currentClass;
        }
        for ( Diagnostic<? extends JavaFileObject> diagnostic : diagnostics ) {
            if ( CANT_RESOLVE.equals( diagnostic.getCode() )
              || CANT_DEREF.equals( diagnostic.getCode() ) ) {

                // Take the source code from the diagnostic
                StringBuilder sb;
                try {
                    sb = new StringBuilder( getGeneratedSourceCodeFrom( currentClass ) );
                } catch ( IOException e ) {
                    sb = new StringBuilder();
                }

                // take information of the error.
                int startPosition = (int) diagnostic.getStartPosition();
                int position = (int) diagnostic.getPosition();
                int endPosition = (int) diagnostic.getEndPosition();

                // put the receiver as the first parameter of the method
                String pieceInQuestion = sb.substring( startPosition, endPosition );
                logger.fine( "Size of source before "
                        + sb.toString().length()
                        + " pieceInQuestion[" + pieceInQuestion + "]" );
                String receiver = pieceInQuestion.substring( 0, position - startPosition );
                String method = pieceInQuestion.substring( position - startPosition + 1 );
                logger.fine( "pieceInQuestion = [" + pieceInQuestion + "]" );
                logger.fine( "receiver = [" + receiver + "]" );
                logger.fine( "method = [" + method + "]" );
                sb.replace( startPosition, endPosition, method + "(" + receiver );

                // remove the opening parenthesis
                int p = endPosition;
                while ( true ) {
                    char c = sb.charAt( p );
                    if ( Character.isSpaceChar( c ) ) {
                        p++;
                    } else if ( c == '(' ) {
                        sb.deleteCharAt( p );
                    } else if ( c == ')' ) {
                        sb.insert( p, " " );
                        break;
                    } else {
                        sb.insert( p, ',' );
                        break;
                    }
                }
                // and finally let the current class take the new source code.
                currentClass.markError( sb.toString(),
                                        diagnostic.getCode(),
                                        startPosition, position );
                try {
                    logger.fine( "Size of source after  "
                            + getGeneratedSourceCodeFrom( currentClass ).length()
                            + " pieceInQuestion[" + pieceInQuestion + "]" );
                } catch ( IOException e ) {
                }
            }
        }
        return currentClass;
    }


    /**
     * Adds line number to the passed string.
     *
     * @param content - The source code as string.
     * @return The same source with numbers.
     */
    private String numberedContent( final String content ) {
        int i = 1;
        StringBuilder numberedContent = new StringBuilder();
        for ( String line : content.split( "\n" ) ) {
            numberedContent.append( i++ );
            numberedContent.append( i <= 10 ? ' ' : "" );
            numberedContent.append( ' ' );
            numberedContent.append( line );
            numberedContent.append( '\n' );
        }
        return numberedContent.toString();
    }

    /**
     * Read the given files as a list of lines
     *
     * @param f - the file to read
     * @return a list of strings each one containing one line.
     */
    private List<String> readLines( File f ) {
        String separator = "\n"; // System.gerProperty("line.separator");
        return Arrays.asList( readFile( f ).split( separator ) );
    }

    /**
     * Read all the file into a single string.
     *
     * @param f - the file to read
     * @return a String with the content of the file
     */
    private String readFile( File f ) {
        ByteArrayOutputStream out = new ByteArrayOutputStream( (int) f.length() );
        try {
            copyLarge( new FileInputStream( f ), out );
            return out.toString( "UTF-8" );
        } catch ( UnsupportedEncodingException e ) {
            return "";
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * Utility method to read all all the input into the outputDir. Use by the
     * "readFile" method
     *
     * @param input  - The input to read from.
     * @param output - Where to copy the data to.
     * @return a count of the bytes read.
     * @throws IOException - If there is an IO problem while reading
     */
    private long copyLarge( InputStream input, OutputStream output ) throws IOException {
        byte[] buffer = new byte[1024];
        long count = 0;
        int n;
        while ( ( n = input.read( buffer ) ) >= 0 ) {
            output.write( buffer, 0, n );
            count += n;
        }
        return count;
    }


    /**
     * Validate the file exists before attempting a compilation.
     * The file is searched through the source path directories.
     * <p/>
     * If no file is found null is returned.
     *
     * @param file - The name of the file to be compiled.
     * @return The file represented by the name "file" in the first source directory
     *         where it appears, or null if is not found in any of the directories.
     */
    private File validateExists( String file ) {
        File toCompile = null;
        for ( File dir : sourceDirs ) {
            toCompile = new File( dir, file );
            if ( toCompile.exists() ) {
                break;
            }
        }
        if ( toCompile != null && !toCompile.exists() ) {
            err.println( "RyzC: file not found " + file );
            return null;
        }
        return toCompile;
    }


}

//http://www.java2s.com/Tutorial/Java/0120__Development/CompilingfromMemory.htm
class JavaSourceFromString extends SimpleJavaFileObject {
    final String code;

    JavaSourceFromString( String name, String code ) {
        super( URI.create( "string:///" + name.replace( '.', '/' )
                           + Kind.SOURCE.extension ),
                Kind.SOURCE );
        this.code = code;
    }


    @Override
    public CharSequence getCharContent( boolean ignoreEncodingErrors ) {
        return code;
    }
}
