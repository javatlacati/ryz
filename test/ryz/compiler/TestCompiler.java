/*
 * Copyright (c)  2010 - 2011, Oscar Reyes and Ryz language developers.
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
import java.net.MalformedURLException;


/**
 * This test uses a file living in: test-samples/00.loading/someTest.jar
 * and this is passed to the compiler to dynamically load
 * a class inside of it. This class in turn is used to declare
 * a local variable inside the class defined by:
 * test-samples/00.loading/WithClassPath.ryz
 *
 * User: oscarryz
 * Date: 6/24/11
 * Time: 3:26 AM
 */
public class TestCompiler {

    private TestUtil testUtil;

    @BeforeMethod
    void init() throws MalformedURLException {
        testUtil = TestUtil.createWith(new File("test-samples/output/"));
        testUtil.classPath("test-samples/00.loading/someTest.jar");
    }

    @AfterMethod
    void cleanUp() {
        testUtil.deleteFromOutput("test/classpath/WithClassPath.class");
        testUtil.deleteFromOutput("test/two/files/CompileOne.class");
        testUtil.deleteFromOutput("test/two/files/CompileTwo.class");
    }

    /**
     * Compile a simple class and load it dinamically.
     * @throws IOException  If the compiler can't write/read from disk
     * @throws ClassNotFoundException  - If the class is not found.
     */
    @Test
    public void compileAndLoad() throws IOException, ClassNotFoundException {
        String className = "test.classpath.WithClassPath";
        testUtil.addSourceDir(new File("test-samples/00.loading/"));
        testUtil.assertMissing(className);
        testUtil.compile("WithClassPath.ryz");
        testUtil.assertExists(className);
    }
    @Test
    public void compileTwoFiles() throws ClassNotFoundException, IOException {
        String first = "test.two.files.CompileOne";
        String second = "test.two.files.CompileTwo";

        testUtil.addSourceDir(new File("test-samples/00.loading/"));
        testUtil.assertMissing(first);
        testUtil.assertMissing(second);
        testUtil.compile("CompileOne.ryz", "CompileTwo.ryz");
        testUtil.assertExists(first);
        testUtil.assertExists(second);
    }
}
