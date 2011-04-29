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
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.System.err;


/**
 * This class represents the Ryz compiler, that will be eventually used
 * to create .class files from .ryz files.
 *
 *
 * User: oscarryz
 * Date: Sep 7, 2010
 * Time: 9:42:07 PM
 */
public class RyzC {

    private static final Logger logger = Logger.getLogger(RyzC.class.getName());
    private static final Map<Character,String> operatorMap = new HashMap<Character,String>(){{
      put('+',"$plus");
      put('-',"$minus");
      put('*',"$star");
      put('/',"$slash");
      put('%',"$percent");
      put('<',"$lt");
      put('>',"$gt");
      put('=',"$eq");
      put('!',"$em");
      put('&',"$amp");
      put('^',"$up");
      put('|',"$bar");
      put('?',"$qm");
      put(':',"$colon");
      //+ - * / % < > = ! & ^ | ? :
    }};

    public static void main( String [] args ) throws IOException {
        RyzC c = RyzC.getCompiler();
        c.sourceDirs(new File("."));
        c.outDir(new File("."));
        c.compile(args);
    }


    /**
     * Return a new instance of the Ryz Compiler
     * @return - A new instance of the compiler
     */
    public static RyzC getCompiler() {
        return new RyzC();
    }

    private RyzC(){}
    /**
     * Where to find .ryz source class default to current directory
     */
    private File[] sourceDirs = new File[]{ new File("") };

    /**
     * Where to put the output, default to current directory.
     */
    private File output = new File("");

  /**
     * Holds a representation of the Ryz classes compiled during this session.
     */
    private final List<RyzClass> classes = new ArrayList<RyzClass>();

    /**
     * Specify the source path directories for the compiler to use.
     *
     * @param dirs - Where to find .ryz source files.
     */
    public void sourceDirs(File ... dirs) {
        sourceDirs = dirs.clone();

    }

    /**
     * Specify where to put the generated .class files
     * @param output - Specify the output directory for the generated .class files
     */
    public void outDir(File output) {
        this.output = output;
    }

    /**
     * Compile the .ryz source files.
     * @param files - The files to be compiled
     * @throws IOException - If there an IO problem while compiling
     */
    public void compile(String ... files) throws IOException {
        for( String file : files ){
            File toCompile = validateExists(file);
            if (toCompile == null) { return; }

            RyzClass currentClass = new RyzClass( file, cleanLines(readLines(toCompile)));
            classes.add( currentClass );
            currentClass.transformSourceCode();
            createClassDefinition( currentClass );
        }
    }


    //TODO: Move to Utility class when the time comes
    /**
     * This method pre-process the input source file. 
     * This helps to things like putting the single line comment in a separate line
     * TODO: keep the original line numbre.
     */
    private List<String> cleanLines(List<String> input) {
        List<String> result = trimLines(input);
        result  = splitLineComments(result);
        result = substituteNonTextMethods( result );
        return result;
    }

    private List<String> substituteNonTextMethods(List<String> input) {
        List<String> result = new ArrayList<String>();
        Pattern pattern = Pattern.compile("[+#~-]??\\s*(__)?\\s*([+\\-*/%<>=!&^|?:]+)\\s*\\(");



        for (String line : input) {
            if(line.startsWith("//")){
                continue;
            }
            Matcher m = pattern.matcher(line);
            if( m.lookingAt() ) {
                String methodName = m.group(0);
                methodName = methodName.substring(0,methodName.length()-1);
                System.out.println("methodName = " + methodName);
                StringBuilder sb = new StringBuilder();
                for( char c : methodName.toCharArray() ){
                    sb.append( operatorMap.get(c) );

                }
                sb.append(line.substring(methodName.length()));
                result.add( sb.toString() );
            }else {
               result.add( line );
           }
        }
        return result;
    }

    private List<String> trimLines(List<String> input) {
        List<String> r = new ArrayList<String>();
          for( String s : input ){
              r.add( s.trim() );
          }
        return r;
    }

    private List<String> splitLineComments(List<String> input) {
        List<String> trimmed = new ArrayList<String>();
        for( String line : input ){
            // if line starts with single line comment: "//"
            // then put that line in the next one.
            // to avoid problems.
            // TODO: this may cause problems when reporting line numbers
            if( line.contains("//") && !line.startsWith("//")){
                String[] strings = line.split("\\/\\/");
                trimmed.add( strings[0].trim() );
                trimmed.add( "//"+strings[1].trim());
            } else {
                trimmed.add(line);
            }
        }
        return trimmed;
    }

    /**
     * Compiles a java source code file from the given RyzClass
      * @param currentClass  - The class to be transformed into .class file
     * @throws IOException   - If it is not possible to write the file
     */
    private void createClassDefinition( RyzClass currentClass ) throws IOException {
        //System.out.println("outputLines = " + outputLines);
        // write the  class
        logger.finest("className=["+currentClass.className()+"]");
        Writer writer = new StringWriter();

        writer.write(String.format("//-- Create from: %s %n" , currentClass.sourceFile()));

        //TODO: move this to the RyzClass
        // from here
        String importString = "";
        String packageString = "";
        List<String> outputLines = currentClass.outputLines();
        List<String> toRemove = new ArrayList<String>();
        boolean packageWritten = false;
        for( String s: outputLines){
            if( s.startsWith("package") && !packageWritten) {
                packageString = s;
                packageWritten = true;

            }
            if( s.startsWith("import")){
                importString = s;
            }
            if(s.startsWith("import")){
                toRemove.add( s ) ;
            }
        }
        // Before writing put package and imports at the top
        outputLines.remove( packageString );
        outputLines.removeAll(toRemove);
        outputLines.addAll(0,toRemove);
        outputLines.add( 0, packageString );
        // to here

        for (String s : outputLines) {
            writer.write(s);
        }
        writer.close();

        JavaFileObject source = new JavaSourceFromString(currentClass.className(), writer.toString());

        // Get the java compiler for this platform
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if( compiler == null ) {
            throw new IllegalStateException("Couldn't get java compiler. Make " +
                    "sure the javac is in the execution path. (HINT put " +
                    "JAVA_HOME/bin before in the path ) JAVA_HOME="+System.getProperty("java.home"));
        }
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(
                null,
                null,
                null);

        //TODO: parameterize options
        Iterable<String> options = logger.isLoggable(Level.FINEST) ? Arrays.asList("-verbose") : null ;
        //TODO: parameterize CLASSPATH
        List<File> classPath = Arrays.asList(new File("./lib/"), new File("./out/build/"));

        fileManager.setLocation(StandardLocation.CLASS_OUTPUT, Arrays.asList(output));
        fileManager.setLocation(StandardLocation.CLASS_PATH, classPath);

        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<JavaFileObject>();
        // Compile the file
        Iterable<? extends JavaFileObject> compilationUnits = Arrays.asList(source);
        boolean succesfullCompilation =
                compiler
                    .getTask(null,
                        fileManager,
                        collector,
                        options,
                        null,
                            compilationUnits)
                .call();
        if(!succesfullCompilation  || logger.isLoggable( Level.FINEST ) ) { 
            logger.fine( numberedContent( writer.toString() ) );
            logger.info( collector.getDiagnostics().toString());
        }

       fileManager.close();

         if( !succesfullCompilation ) { 
            for ( Diagnostic<? extends JavaFileObject> diagnostic : collector.getDiagnostics() ) { 
                if("compiler.err.unreported.exception.need.to.catch.or.throw".equals(diagnostic.getCode())){
                    createClassDefinition( currentClass.reportExceptions() );
                    ClassInstrumentation.removeCheckedExceptions( currentClass, output );
                    return;
                }
            }
        }


        // delete the file
        //sourceFile.deleteOnExit();


    }
    /**
     * Adds line number to the passed string.
     */
    private final String numberedContent( final String content ) { 
        int i = 1;
        StringBuilder numberedContent = new StringBuilder();
        for( String line : content.split("\n")) {
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
     * @param f - the file to read
     * @return a list of strings each one containing one line.
     */
    private List<String> readLines(File f){
        String separator = "\n"; // System.gerProperty("line.separator");
        return Arrays.asList(readFile( f ).split(separator));
    }

    /**
     * Read all the file into a single string.
     * @param f - the file to read
     * @return a String with the content of the file
     */
    private String readFile( File f )  {
		ByteArrayOutputStream out = new ByteArrayOutputStream( (int) f.length() );
        try {
            copyLarge( new FileInputStream( f ), out );
            return out.toString("UTF-8");
        } catch (UnsupportedEncodingException e) {
            return "";
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

  /**
     * Utility method to read all all the input into the output. Use by the
     * "readFile" method
     * @param input - The input to read from.
     * @param output - Where to copy the data to.
     * @return a count of the bytes read.
     * @throws IOException - If there is an IO problem while reading
     */
	private long copyLarge( InputStream input, OutputStream output ) throws IOException {
		byte[] buffer = new byte[1024];
		long count = 0;
		int n;
		while( ( n = input.read(buffer)) >= 0 ) {
			output.write( buffer, 0, n );
			count += n;
		}
		return count;
	}


    /**
     * Validate the file exists before attempting a compilation.
     * The file is searched through the source path directories.
     *
     * If no file is found null is returned.
     *
     * @param file - The name of the file to be compiled.
     * @return The file represented by the name "file" in the first source directory
     * where it appears, or null if is not found in any of the directories.
     */
    private File validateExists(String file) {
        File toCompile = null;
        for( File dir : sourceDirs ){
            toCompile = new File(dir, file);
            if( toCompile.exists() ) {
                break;
            }
        }
        if (toCompile != null && !toCompile.exists() ) {
                err.println("RyzC: file not found "+ file);
                return null;
        }
        return toCompile;
    }


}

//http://www.java2s.com/Tutorial/Java/0120__Development/CompilingfromMemory.htm
class JavaSourceFromString extends SimpleJavaFileObject {
  final String code;

  JavaSourceFromString(String name, String code) {
    super(URI.create("string:///" + name.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
    this.code = code;
  }


    @Override
  public CharSequence getCharContent(boolean ignoreEncodingErrors) {
    return code;
  }
}
