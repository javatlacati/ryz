package ryz.compiler;

import java.io.File;
import java.io.IOException;

import java.net.URL;
import java.net.MalformedURLException;
import java.net.URLClassLoader;
import java.util.logging.Logger;

/**
 * Utility class which is pretty much a wrapper for the compiler.
 * The point of this class is to avoid having same code repeated all over the
 * test classes and have some handy methods.
 */
class TestUtil {

    private static final Logger logger = Logger.getLogger(TestUtil.class.getName());
    private final RyzC ryzc;
    private final ClassLoader classPath;
    private final File output;

    public static TestUtil createWith( File output )
                throws MalformedURLException {
        return new TestUtil( output);
    }
    private TestUtil( File output ) throws MalformedURLException {
        this.output = output;
        ryzc = RyzC.getCompiler();
        ryzc.outDir(this.output);
        this.classPath = new URLClassLoader(new URL[]{
            new URL(output.toURI().toString())
        });
    }

    

    Class assertExists(String className)
            throws MalformedURLException,
            ClassNotFoundException {

        if( className == null ) { return null; }
        Class<?> aClass = classPath.loadClass(className);
        assert aClass != null;
        return aClass;
    }

    void assertMissing(String className) {
        if( className == null ) { return; }
        try {
            classPath.loadClass(className);
            throw new AssertionError("Should have throw " +
                                    "ClassNotFoundException for: "+ className);
        } catch (ClassNotFoundException e) {
            // ok, that was expected
        }
    }

    public void compile(String file) throws IOException {
        ryzc.compile(file);
    }

    public void deleteFromOutput(String file ) {
        logger.finest("file to delete = " + file);
        if( file == null ) { return; }
        new File( output.getPath()+"/"+file).delete();
    }

    public void addSourceDir(File sourceDir) {
        ryzc.sourceDirs(sourceDir);
    }
}