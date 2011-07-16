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
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import java.net.MalformedURLException;

/**
 * This test class will load from the filesystem all the .ryz source files
 * and it will compile them and assert they match the specification
 * described by the properties object loaded from that same file.
 */
@Test
public class TestCompileSpecs {


    private TestUtil testUtil;
    

    private final AssertStrategy annotationsAssertion = new AnnotationsAssertStrategy();
    private final AssertStrategy attributesAssertion  = new AttributesAssertStrategy();
    private final AssertStrategy methodsAssertion     = new MethodsAssertStrategy();
    private final AssertStrategy implementsAssertion  = new ImplementsAssertStrategy();
    private final AssertStrategy extendsAssertion     = new ExtendsAssertionStrategy();
    private final AssertStrategy assertBehaviour      = new BehaviorAssertionStrategy();


    @BeforeMethod
    void init() throws MalformedURLException {
        testUtil = TestUtil.createWith( new File("test-samples/output/"));
    }

    @AfterMethod
    void cleanUp() {
        testUtil.deleteFromOutput("load/test/First.class");
    }
    



    @Test(dataProvider="sourceFiles")
    public void runTests(Properties spec) throws IOException, ClassNotFoundException {

        String className  = spec.getProperty(  "className" ).trim();
        String testFile   = spec.getProperty(  "fileName"  ).trim();
        String sourcePath = spec.getProperty( "sourcePath" ).trim();

        testUtil.addSourceDir( new File("test-samples/"+sourcePath+"/"));

        try {

            testUtil.assertMissing(className);
            testUtil.compile(testFile);
            Class clazz = testUtil.assertExists(className);

            extendsAssertion    .assertDefinition(clazz, spec, testFile);
            implementsAssertion .assertDefinition(clazz, spec, testFile);
            attributesAssertion .assertDefinition(clazz, spec, testFile);
            methodsAssertion    .assertDefinition(clazz, spec, testFile);
            assertBehaviour     .assertDefinition(clazz, spec, testFile);
            annotationsAssertion.assertDefinition(clazz, spec, testFile);

        } finally {
            testUtil.deleteFromOutput(spec.getProperty("classFile"));
            String otherClasses = spec.getProperty("otherClasses");
            if( otherClasses != null ) {
                for( String otherClassFile : otherClasses.split(",") ) {
                    testUtil.deleteFromOutput(otherClassFile.trim());
                }
            }
        }
        
    }



    @DataProvider(name="sourceFiles")
    private Object [][] loadSourceFiles() throws IOException {

        // collect all source files
        List<File> sourceFiles = collectSourceFiles();
        List<Properties[]> sourceProperties = new ArrayList<Properties[]>();

        for (File testFile : sourceFiles) {
            // assert the properties match
            sourceProperties.add(new Properties[]{
                    getPropertiesFrom(testFile)
            });
        }
        return  sourceProperties.toArray(
                    new Properties[sourceProperties.size()][]
                 );
    }


    /**
     * Creates a Properties based in the content of the test file.
     * @param testFile The file containing the Ryz source code.
     * @return - A java.util.Properties object containing the key/value
     * pairs that conform the spec.
     * @throws IOException - if there is an error while reading the source file
     */
    private Properties getPropertiesFrom(File testFile) throws IOException {
        Properties p = new Properties();
        FileInputStream inStream = new FileInputStream(testFile);
        try {
            p.load(inStream);
        } finally {
            //noinspection EmptyCatchBlock
            try {
                inStream.close();
            } catch (IOException ioe ){}
        }
        p.setProperty("fileName"  , testFile.getName());
        p.setProperty("sourcePath", testFile.getParentFile().getName());
        return p;

    }

    /**
     * Walks through the "test-samples" directory and from all its sub-directories
     * and loads those files ending with .ryz extension.
     * @return  - A list of files found ending with "Spec.ryz"
     */
    private List<File> collectSourceFiles() {
        List<File> files = new ArrayList<File>();
        // just search at first level
        File useSamples = new File(System.getProperty("use.samples","./test-samples"));
        if(    useSamples.isFile() 
           && !useSamples.getName().endsWith("Spec.ryz")) {
                throw new IllegalArgumentException("Value of system property:" +
                    " [use.samples] should end with \"Spec.ryz\"");
        }
        if( useSamples.isFile() 
         && useSamples.getName().endsWith("Spec.ryz")) {
                files.add( useSamples );
                return files;
        }
        File[] testFiles = useSamples.listFiles();
        for( File file : testFiles) if( file.isDirectory() ) {
            files.addAll( Arrays.asList(file.listFiles(new FileFilter(){
                public boolean accept(File pathname) {
                    return pathname.getName().endsWith("Spec.ryz");
                }
            })));
        } else if( file.getName().endsWith("Spec.ryz") ) {
            files.add(file );
        }
        return files;
    }

}