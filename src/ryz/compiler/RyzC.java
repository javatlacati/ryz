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
            new PackageTransformer(),
            new AttributeTransformer(),
            new CommentTransformer(),
            new ClosingKeyTransformer(),
            new MethodTransformer()
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
            if( s.startsWith("public class")){
                return s.substring("public class".length(), s.indexOf("{")).trim();
            }
        }
        return "First";

    }


    private void createClassDefinition(String className, List<String> outputLines ) throws IOException {
        System.out.println("outputLines = " + outputLines);
        // write the test class
        File sourceFile = new File(className + ".java");
        FileWriter writer = new FileWriter(sourceFile);
        StringWriter sWriter = new StringWriter();
        boolean containsPackage = false;
        for (String s : outputLines) {
            if (s.startsWith("package")) {
                containsPackage = true;
            }
            writer.write(s);
            sWriter.write(s);

        }

        String packageName = containsPackage ? "" : "package load.test;\n" +
                " public class First{\n" +
               "    private int i = 0;\n" +
               "    public int i(){ return i;} \n" +
               "}";
        String code = packageName ;
        writer.write(code);

        sWriter.write(code);
        System.out.println("sWriter = " + sWriter);
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
