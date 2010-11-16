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
