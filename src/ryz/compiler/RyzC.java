package ryz.compiler;



import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.io.FileWriter;
import java.io.IOException;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

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
     * Compile the .ryz source files.
     * @param files - The files to be compiled
     * @throws IOException
     */
    public void compile(String ... files) throws IOException {
        for( String file : files ){
            File toCompile = validateExists(file);
            if (toCompile == null) { return; }

            String content = readFile(toCompile);
            String packageName = getPackage( content );
            String className   = getClass( content );
            if( packageName == null  ){ packageName = "load.test"; }
            if( className == null  ){ className = "First"; }
            compileClass(packageName, className);
        }


    }

    private String getPackage(String content) {
        if( content.contains("simple.class")){
        return "simple.class$";  //To change body of created methods use File | Settings | File Templates.
        }

        return null;
    }

    private String getClass(String content) {
        if( content.contains("One")){
            return "One";  //To change body of created methods use File | Settings | File Templates.
        }
        return null;
    }

    private String readFile(File toCompile) throws IOException {
        InputStream is = new FileInputStream(toCompile);
        BufferedInputStream bis = new BufferedInputStream(is);
        int c = -1;
        StringBuilder builder = new StringBuilder();

        while( ( c = bis.read() ) >= 0 ) {
            builder.append( (char) c );
        }

        bis.close();
        is.close();
        System.out.println("builder.toString() = " + builder.toString());
        return builder.toString();

    }

    /**
     * Creates a sample class and compiles it.
     *
     * This method would be removed in the future.
     * @throws IOException
     * @param packageName
     * @param className
     */
    private void compileClass(String packageName, String className) throws IOException {
        // write the test class
        File sourceFile = new File(className+".java");
        FileWriter writer     = new FileWriter(sourceFile);

        writer.write(
                "package "+ packageName +";\n" +
                "public class "+ className + " {" +
                "    private int i = 0;" +
                "    public int i(){ return i;}" +
                "}"
        );
        writer.close();

        // Get the java compiler for this platform
        JavaCompiler compiler    = ToolProvider.getSystemJavaCompiler();
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
        if( !toCompile.exists() ) {
            err.println("RyzC: file not found "+ file);
            return null;
        }
        return toCompile;
    }

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
}
