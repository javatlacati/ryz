/*
 * Copyright (c) 2010, Oscar Reyes de la Cruz
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


import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

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

    private static Logger logger = Logger.getLogger(RyzC.class.getName());
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

            List<String> lines = trimLines(readLines(toCompile));

            List<String> generatedSource = new ArrayList<String>();
            for( String line : lines ) {
                for( LineTransformer t : transformers() ) {
                    t.transform( line, generatedSource );
                }
            }
            createClassDefinition( getClass( generatedSource), generatedSource );
        }
    }

    private List<LineTransformer> transformers = Arrays.asList(
            new PackageClassTransformer(),
            new ImportTransformer(),
            new AttributeTransformer(),
            new CommentTransformer(),
            new ClosingKeyTransformer(),
            new MethodTransformer(),
            new ReturnTransformer(),
            new StatementTransformer()
    );

    private List<LineTransformer> transformers() {
        return transformers;
    }

    private List<String> trimLines(List<String> output) {
        List<String> trimmed = new ArrayList<String>();
        for( String s : output ){
            trimmed.add( s.trim() );
        }
        return trimmed;
    }




    /**
     * Tries to get a class name from the "outputlines" searching for a line
     * that looks like "class Xyz {"
     *
     * @param outputLines - The generated java source code.
     * @return - A class name found in those lines
     */
    private String getClass(List<String> outputLines) {
        for(String s : outputLines) {
            //TODO: refactorme
            if( s.startsWith("public class") && s.contains("extends")){
                return s.substring("public class".length(), s.indexOf("extends")).trim();
            } else if(s.startsWith("public class") && s.contains("implements") ){
                return s.substring("public class".length(), s.indexOf("implements")).trim();   
            }
        }
        return "First";

    }


    private void createClassDefinition(String className, List<String> outputLines ) throws IOException {
        //System.out.println("outputLines = " + outputLines);
        // write the  class
        logger.finest("className=["+className+"]");
        File sourceFile = new File(className + ".java");
        StringWriter sWriter = new StringWriter();
        Writer writer = new SourceWriter( new FileWriter(sourceFile), sWriter);
        String importString = "";
        String packageString = "";
        for( String s: outputLines ){
            if( s.startsWith("package")) {
                packageString = s;
                writer.write( packageString );
            }
            if( s.startsWith("import")){
                importString = s;
                writer.write( importString );
            }
        }
        if( packageString == "" ){
            writer.write(
               "package load.test;\n" +
               " public class First{\n" +
               "    private int i = 0;\n" +
               "    public int i(){ return i;} \n" +
               "}");
        }
        outputLines.remove(packageString);
        outputLines.remove(importString);

        for (String s : outputLines) {
            writer.write(s);
        }
        logger.finest("sWriter = " + sWriter);
        writer.close();

        // Get the java compiler for this platform
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(
                null,
                null,
                null);

        fileManager.setLocation(StandardLocation.CLASS_OUTPUT, Arrays.asList(output));
        // Compile the file
        compiler
                .getTask(null,
                        fileManager,
                        null,
                        null,
                        null,
                        fileManager.getJavaFileObjectsFromFiles(Arrays.asList(sourceFile)))
                .call();
        fileManager.close();

        // delete the file
        sourceFile.deleteOnExit();


    }

    /**
     * Read the given files as a list of lines
     * @param f - the file to read
     * @return a list of strings each one containing one line.
     */
    private List<String> readLines(File f){
        String separator = System.getProperty("line.separator");
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
class SourceWriter extends Writer {

    private final Writer a;
    private final Writer b;

    public SourceWriter(Writer b, Writer a) {
        this.b = b;
        this.a = a;
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        a.write(cbuf, off, len );
        b.write(cbuf, off, len );
    }

    @Override
    public void flush() throws IOException {
        a.flush();
        b.flush();
    }

    @Override
    public void close() throws IOException {
        a.close();
        b.close();
    }
}
