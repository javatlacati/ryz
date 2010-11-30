/*
 * Copyright (c) 2010, Ryz language developers.
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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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
 * User: oscarryz
 * Date: Sep 8, 2010
 * Time: 8:21:57 PM
 */
@Test
public class TestCompiler {

    // Hashcode for the access modifiers
    private final static int PUBLIC    = 0xc5bdb269;
    private final static int PROTECTED = 0xdbba6bae;
    private final static int PRIVATE   = 0xed412583;
    private final static int DEFAULT   = 0x5c13d641;

    private TestUtil testUtil;

    @BeforeMethod
    void init() throws MalformedURLException {
        testUtil = TestUtil.createWith( new File("./test-resources/output/"));
    }

    @AfterMethod
    void cleanUp() {
        testUtil.deleteFromOutput("load/test/First.class");
    }
    



    @Test(dataProvider="sourceFiles")
    public void runTests(Properties spec) throws IOException, ClassNotFoundException {

        String className  = spec.getProperty("className");
        String testFile   = spec.getProperty("fileName");
        String sourcePath = spec.getProperty("sourcePath");

        testUtil.addSourceDir(new File("./test-resources/"+sourcePath+"/"));

        try {

            testUtil.assertMissing(className);
            testUtil.compile(testFile);
            Class clazz = testUtil.assertExists(className);

            assertExtends( spec, testFile, clazz );
            assertImplements( spec, testFile, clazz );
            assertAttributes(spec, testFile, clazz);
            assertMethods(spec, testFile, clazz);

        } finally {
            testUtil.deleteFromOutput(spec.getProperty("classFile"));
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

    private void assertImplements(Properties spec,
                                         String testFile,
                                         Class clazz) {

        String interfaces;
        if( isNull(clazz) || isNull(interfaces=spec.getProperty("implements"))){
            return;
        }

        System.out.println("interfaces = " + interfaces);
        for( String currentInterface : interfaces.split(",")){
            boolean implemented = false;
            for( Class implementedInterfaces : clazz.getInterfaces() ) {
                if( implementedInterfaces.getName().equals(currentInterface)){
                    implemented = true;
                }
            }
            assert implemented :  testFile + " doesn't implement : "+ currentInterface;

        }

    }

    /**
     * Verifies the given class inherits from the class/interface specified in
     * the "spec"
     * @param spec  Where the supertype is specified
     * @param testFile - Name of the file being tested.
     * @param clazz  - The .class generated by the compiler.
     */
    private void assertExtends(Properties spec,
                               String testFile,
                               Class clazz) {
        String superClass;
        if( isNull(clazz) || isNull(superClass=spec.getProperty("extends"))){
            return;
        }
        assert clazz.getSuperclass().getName().equals(superClass) :
                testFile + " doesn't' inherits from : " + superClass;


    }



    /**
     * Assert that the given class has the attributes defined in the spec
     * object.
     * @param spec
     * @param file
     * @param clazz
     */
    private void assertAttributes(Properties spec, String file, Class clazz) {
        String attributes;
        if( isNull(clazz) || isNull(attributes=spec.getProperty("attributes"))){
            return ;
        }
        for (String pairs : attributes.split(",")) {
            String[] nameType = pairs.split(":");
            boolean matched = false;
            for (Field f : clazz.getDeclaredFields()) {
                String name = getNameFromSpec(nameType[0].trim());
                String type = nameType[1].trim();
                String modifier = getModifierFromSpec( nameType[0].trim() );
                boolean sameName = f.getName().equals(name);
                boolean sameType = f.getType().getName().equals(type);
                boolean sameModifier = sameModifier( modifier, f );
                if ( sameModifier && sameName && sameType) {
                    matched = true;
                    break;
                }
            }
            assert matched : file + " didn't fulfilled: " + pairs + " attribute";
        }
    }

    /**
     * Test if the given field has the same access modifier as specified in the
     * modifier string
     * @param modifier - could be any of: "public protected default private"
     * @param f  - a field which modifier is to be evaluated.
     * @return if the value of "modifier" string is public and the field
     * is indeed public return true. Same goes for protected, default and private.
     * Otherwise return false.
     */
    private boolean sameModifier(String modifier, Field f) {
        int modifiers = f.getModifiers();
        switch ( modifier.hashCode() ) {
            case PUBLIC:
                return Modifier.isPublic(modifiers);
            case PROTECTED:
                return Modifier.isProtected(modifiers);
            case DEFAULT:
                return !Modifier.isPublic(modifiers)
                        && !Modifier.isProtected(modifiers)
                        && !Modifier.isPrivate(modifiers);
            case PRIVATE:
                return Modifier.isPrivate(modifiers);
            default: return false;
        }
    }

    /**
     * Return the acccess modifier specified in the spec.
     * @param spec - The spec containing the access modifier along with the name
     * @return a string representing the access modifier
     */
    private String getModifierFromSpec(String spec) {
        return spec.split(" ")[0];
    }

    /**
     * Return the name of the thing specified in the spec.
     * @param spec - The spec containing the name  along with the access modifier
     * @return a string representing name of the thing.
     */
    private String getNameFromSpec(String spec) {
        return spec.split(" ")[1];
    }


    /**
     * Assert the given class has all the methods defined in the spec
     * @param spec
     * @param file
     * @param clazz
     */
    private void assertMethods(Properties spec, String file, Class clazz) {
        String methods;
        if( isNull(clazz) || isNull(methods=spec.getProperty("methods"))){
            return; 
        }

        // TODO: refactor this code is duplicated and looks very similar to that in
        // assertAttribute
        for (String pairs : methods.split(",")) {
            String[] nameType = pairs.split(":");
            boolean matched = false;
            for (Method m : clazz.getDeclaredMethods()) {
                if (m.getName().equals(nameType[0].trim())
                        && m.getReturnType()
                            .getName()
                            .equals(nameType[1].trim())) {
                    matched = true;
                    break;
                }
            }
            assert matched : file + " didn't fulfilled: " + pairs + " method";
        }

        String classMethods;
        if( isNull(clazz) || isNull(classMethods=spec.getProperty("class-methods"))){
            return;
        }
        for (String pairs : classMethods.split(",")) {
            String[] nameType = pairs.split(":");
            boolean matched = false;
            for (Method m : clazz.getDeclaredMethods()) {;
                if (Modifier.isStatic(m.getModifiers())
                        && m.getName().equals(nameType[0].trim())
                        && m.getReturnType()
                            .getName()
                            .equals(nameType[1].trim())) {
                    matched = true;
                    break;
                }
            }
            assert matched : file + " didn't fulfilled class method: " + pairs ;
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
        p.setProperty("fileName", testFile.getName());
        p.setProperty("sourcePath", testFile.getParentFile().getName());
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
                    return pathname.getName().endsWith("Spec.ryz");
                }
            })));
        }
        return files;
    }
    /**
     * Test if the given object is null.
     * @param object to test
     * @return object == null
     */
    private final boolean isNull(Object object) {
        return object == null;
    }
    

}
