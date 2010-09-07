package ryz.compiler;

import org.testng.annotations.Test;

import java.io.File;
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

    /**
     * Test that a given class which is not in the classpath
     * is loaded dynamically with the URLClassLoader
     * @throws MalformedURLException
     * @throws ClassNotFoundException
     */
    public void loadClass() throws MalformedURLException, ClassNotFoundException {

        String className = "hello.world.Hello";

        try {
            // hello.world.Hello class is in resources directory
            ClassLoader.getSystemClassLoader().loadClass(className);
            throw new AssertionError("Should have throw ClassNotFoundException");
        } catch (ClassNotFoundException e) {
            // ok, that was expected
        }


        URL[] urls = new URL[]{new URL(new File("").toURI()+"resources/")};
        new URLClassLoader(urls).loadClass(className);

    }

}
