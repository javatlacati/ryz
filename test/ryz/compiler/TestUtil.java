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
            throws ClassNotFoundException {

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

    public void compile(String ... files) throws IOException {
        ryzc.compile( files );
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    public void deleteFromOutput(String file ) {
        logger.finest("file to delete = " + file);
        if( file == null ) { return; }
        new File( output.getPath()+"/"+file).delete();
    }

    public void addSourceDir(File sourceDir) {
        ryzc.sourceDirs(sourceDir);
    }

    public void classPath(String ... classPath) {
        ryzc.classPath(classPath);
    }
}