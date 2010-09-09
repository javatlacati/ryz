package ryz.compiler;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * This test class will load from the filesytem all the .ryz source files
 * and it will compile them and assert they match the specification
 * described by the properties object loaded from that same file.
 *
 * TODO: Use the data provider from testng
 *
 * User: oscarryz
 * Date: Sep 8, 2010
 * Time: 8:21:57 PM
 */
@Test
public class TestCompiler {

    private TestUtil testUtil;

    @BeforeMethod
    void init() throws MalformedURLException {
        testUtil = TestUtil.createWith(new File("./test-resources/00.loading/"),
                                       new File("./test-resources/output/"));
    }

    public void runTests() throws IOException, ClassNotFoundException {

        // collect all source files
        List<File> sourceFiles = collectSourceFiles();
        Properties testProperties = null;

        // and for each one of them
        for (File testFile : sourceFiles)
            try {

                // assert the properties match
                testProperties = getPropertiesFrom(testFile);
                String className = testProperties.getProperty("className");
                // if they have a "className" property
                if( className == null ){
                    continue; // we won't be able to test this one
                }

                // TODO: move out this, when each file represent a single test case
                testUtil = TestUtil.createWith(new File("./test-resources/00.loading/"),
                                               new File("./test-resources/output/"));

                testUtil.assertMissing(className);
                testUtil.compile(testFile.getName());
                Class clazz = testUtil.assertExists(
                                       testProperties.getProperty("className"));

                assertAttributes(testProperties, testFile, clazz);
                assertMethods(testProperties, testFile, clazz);

            } finally {
                    testUtil.deleteFromOutput(testProperties.getProperty("classFile"));
            }

    }


    /**
     * Assert that the given class has the attributes defined in the testProperties
     * object.
     * @param testProperties
     * @param testFile
     * @param clazz
     */
    private void assertAttributes(Properties testProperties, File testFile, Class clazz) {
        if( clazz == null || testProperties.getProperty("attributes") == null ){
            return ;
        }
        String attributes = testProperties.getProperty("attributes");
        for (String pairs : attributes.split(",")) {
            String[] nameType = pairs.split(":");
            boolean matched = false;
            for (Field f : clazz.getDeclaredFields()) {
                if (f.getName().equals(nameType[0].trim())
                        && f.getType().toString().equals(nameType[1].trim())) {
                    matched = true;
                    break;
                }
            }
            assert matched : testFile.getName() + " didn't fulfilled: " + pairs + "attribute";
        }
    }

    /**
     * Assert the given class has all the methods defined in the testProperties
     * @param testProperties
     * @param testFile
     * @param clazz
     */
    private void assertMethods(Properties testProperties, File testFile, Class clazz) {
        if( clazz == null || testProperties.getProperty("methods") == null ){
            return; 
        }
        String methods = testProperties.getProperty("methods");
        for (String pairs : methods.split(",")) {
            String[] nameType = pairs.split(":");
            boolean matched = false;
            for (Method m : clazz.getDeclaredMethods()) {
                if (m.getName().equals(nameType[0].trim())
                        && m.getReturnType().toString().equals(nameType[1].trim())) {
                    matched = true;
                    break;
                }
            }
            assert matched : testFile.getName() + " didn't fulfilled: " + pairs + "attribute";
        }
    }


    /**
     * Creates a Properties based in the content of the test file.
     * @param testFile
     * @return
     * @throws IOException
     */
    private Properties getPropertiesFrom(File testFile) throws IOException {
        Properties p = new Properties();
        FileInputStream inStream = new FileInputStream(testFile);
        try {
            p.load(inStream);
        } finally {
            if( inStream != null ) try {
                inStream.close();
            } catch (IOException ioe ){}
        }
        return p;

    }

    /**
     * Walks through the "test-resources" directory and from all its subdirs
     * loads those files ending with .ryz extension.
     * @return
     */
    private List<File> collectSourceFiles() {
        List<File> files = new ArrayList<File>();
        // just search at first level
        File[] testFiles = new File("./test-resources").listFiles();
        for( File file : testFiles) if( file.isDirectory() ) {
            files.addAll( Arrays.asList(file.listFiles(new FileFilter(){
                public boolean accept(File pathname) {
                    return pathname.getName().endsWith(".ryz");
                }
            })));
        }
        return files;
    }


    @AfterMethod
    void cleanUp() {
        testUtil.deleteFromOutput("load/test/First.class");
    }
}
