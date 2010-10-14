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
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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


    private static final List<String> javaKeywords = Arrays.asList(
            "abstract","assert","boolean","break","byte","case","catch",
            "char","class","const","continue","default","do","double",
            "else","enum","extends","final ","finally","float","for",
            "goto","if","implements","import","instanceof","int",
            "interface","long","native ","new","package","private ",
            "protected ","public return","short","static ","strictfp ",
            "super","switch","synchronized ","this","throw","transient ",
            "try","void","volatile ","while"
    );


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

            List<String> lines = readLines(toCompile);
            List<String> output = new ArrayList<String>();
            getPackage(lines, output);
            createClassDefinition( getClass( output), output );
        }
    }


    /**
     * Iterate all the lines and tries to get one representing
     * the package and class definition ie: "some.package.Class {"
     * Then stores the correspondent Java representation into the "output"
     * list
     *
     * @param lines - A list with the Ryz source code
     * @param output - A list where the Java source code will be placed
     */
    private void getPackage(List<String> lines, List<String> output) {
        for( String line : lines ) {
            line = line.trim();

            int iod = line.indexOf(".");
            int iok = line.indexOf("{", iod);
            if (iod > 0 && iok > iod) {
                // ej: some.package.Name {
                String possiblePackageAndClass = line.substring(0, iok);
                int liod = possiblePackageAndClass.lastIndexOf(".");
                String possibleClass = possiblePackageAndClass.substring(liod+1).trim();
                System.out.println("line = " + line);
                System.out.println("possibleClass = " + possibleClass);
                
                if (Character.isUpperCase(possibleClass.charAt(0))) {
                    String packageName = possiblePackageAndClass.substring(0, liod);
                    StringBuilder sb = new StringBuilder("");
                    for( String s : packageName.split("\\.")){
                        sb.append(scapeName(s));
                        sb.append(".");
                    }
                    sb.delete(sb.length()-1, sb.length());
                    output.add(String.format("package %s;%n", sb.toString()));
                    output.add(String.format("class %s {%n", scapeName(possibleClass)));
                }
            }
        }
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
            if( s.startsWith("class")){
                return s.substring(6, s.indexOf("{"));
            }
        }
        return "First";

    }

    private String scapeName(String name) {
        if( javaKeywords.contains(name)){
            return name + "$";
        }

        return name;
    }

    private void createClassDefinition(String className, List<String> outputLines ) throws IOException {
        System.out.println("outputLines = " + outputLines);
        // write the test class
        File sourceFile = new File(className + ".java");
        FileWriter writer = new FileWriter(sourceFile);

        boolean containsPackage = false;
        for (String s : outputLines) {
            if (s.startsWith("package")) {
                containsPackage = true;
            }
            writer.write(s);
        }

        String packageName = containsPackage ? "" : "package load.test;\n public class First{\n";
        writer.write(
                packageName +
                "    private int i = 0;" +
                        "    public int i(){ return i;}" +
                        "}"
        );
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
     * @param input
     * @param output
     * @return
     * @throws IOException
     */
	private long copyLarge( InputStream input, OutputStream output ) throws IOException {
		byte[] buffer = new byte[1024];
		long count = 0;
		int n = 0;
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
