package ryz.compiler;

import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Created by IntelliJ IDEA.
 * User: oscarryz
 * Date: Sep 7, 2010
 * Time: 11:33:18 AM
 * To change this template use File | Settings | File Templates.
 */
@Test
public class TestLoadClass {


    private RyzC ryzc;
    private File output;

    @BeforeTest
    void init(){
        ryzc = RyzC.getCompiler();
        ryzc.sourceDirs(new File("./resources/loading/"));
        output = new File("./resources/classes/");
        ryzc.outDir(output);
    }

    @AfterTest
    void cleanUp() {
        new File( output.getPath()+"/load/test/First.class").deleteOnExit();
    }

    /**
     * Test that a given class which is not in the classpath
     * is loaded dynamically with the URLClassLoader
     * @throws MalformedURLException
     * @throws ClassNotFoundException
     */
    public void loadClass() throws MalformedURLException, ClassNotFoundException {

        String className = "hello.world.Hello";
        assertMissing(className);
        assertExists(className);

    }

    /**
     * Test the compiler to verify if a given file exists in the file system
     * before a compilation attempt is made.
     * @throws IOException
     */
    public void testFileNotFound() throws IOException {

        // Redirect standard error
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setErr(new PrintStream(baos));

        ryzc.compile("NonExisting.ryz");

        assert baos.toString().equals("RyzC: file not found NonExisting.ryz\n");

    }

    /**
     * Compile a simple class and load it dinamically. 
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void compileAndLoad() throws IOException, ClassNotFoundException {
        String className = "load.test.First";
        assertMissing(className);
        ryzc.compile("First.ryz");

        assertExists(className);

    }


    private void assertExists(String className)
            throws MalformedURLException,
                    ClassNotFoundException {

        URL[] urls = new URL[]{new URL(output.toURI().toString())};
        assert new URLClassLoader(urls).loadClass(className) != null;

    }

    private void assertMissing(String className) {
        try {
            // hello.world.Hello class is in resources directory
            ClassLoader.getSystemClassLoader().loadClass(className);
            throw new AssertionError("Should have throw ClassNotFoundException");
        } catch (ClassNotFoundException e) {
            // ok, that was expected
        }
    }
    

}
