package ryz.compiler;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.ByteArrayOutputStream;
import java.net.MalformedURLException;

/**
 * Basic test case. It makes sure the tests themselves may load dynamically
 * a .class file created during the tests.
 *
 * User: oscarryz
 * Date: Sep 7, 2010
 * Time: 11:33:18 AM
 */
@Test
public class TestLoadClass {


    private TestUtil testUtil;

    @BeforeMethod
    void init() throws MalformedURLException {
        testUtil = TestUtil.createWith(new File("./test-resources/output/"));
    }

    @AfterMethod
    void cleanUp() {
        testUtil.deleteFromOutput("/load/test/First.class");
    }

    /**
     * Test that a given class which is not in the classpath
     * is loaded dynamically with the URLClassLoader
     * @throws MalformedURLException
     * @throws ClassNotFoundException
     */
    public void loadClass() throws MalformedURLException, ClassNotFoundException {

        // hello.world.Hello compiled class is in resources/output directory
        String className = "hello.world.Hello";
        testUtil.assertExists(className);

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

        testUtil.compile("NonExisting.ryz");

        assert baos.toString().equals("RyzC: file not found NonExisting.ryz\n");

    }

    /**
     * Compile a simple class and load it dinamically. 
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void compileAndLoad() throws IOException, ClassNotFoundException {
        String className = "load.test.First";
        testUtil.addSourceDir(new File("./test-resources/00.loading/"));
        testUtil.assertMissing(className);
        testUtil.compile("First.ryz");
        testUtil.assertExists(className);

    }

}
