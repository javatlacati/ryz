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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: oscarryz
 * Date: Dec 2, 2010
 * Time: 4:08:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class RyzClass {
    private final List<String> sourceLines;
    private final List<String> generatedSource = new ArrayList<String>();
    private String name;

    public RyzClass(List<String> sourceLines) {
        this.sourceLines = sourceLines;
    }

    public String name() {
        if( name == null ){
            this.name = getClass(generatedSource);
        }
        return this.name;
    }

    public void transformSourceCode() {
        for (LineTransformer transformer : transformers) {
            transformer.currentClass(this);
        }

        for( String line : sourceLines ) {
            for( LineTransformer t : transformers() ) {
                t.transform( line, generatedSource );
            }
        }
        
    }

    public List<String> outputLines() {
        return generatedSource;  //To change body of created methods use File | Settings | File Templates.
    }

    private List<LineTransformer> transformers = Arrays.asList(
            new PackageClassTransformer(),
            new ImportTransformer(),
            new AttributeTransformer(),
            new CommentTransformer(),
            new ClosingKeyTransformer(),
            new MethodTransformer(),
            new ReturnTransformer(),
            new StatementTransformer()
    );
    private List<LineTransformer> transformers() {
        return transformers;
    }

    /**
     * Tries to get a class name from the "outputlines" searching for a line
     * that looks like "class Xyz {"
     *
     * @param outputLines - The generated java source code.
     * @return - A class name found in those lines
     */
    private String getClass(List<String> outputLines) {
        for(String s : outputLines) {
            //TODO: refactorme
            if( s.startsWith("public class") && s.contains("extends")){
                return s.substring("public class".length(), s.indexOf("extends")).trim();
            } else if(s.startsWith("public class") && s.contains("implements") ){
                return s.substring("public class".length(), s.indexOf("implements")).trim();
            }
        }
        return "First";

    }
    
    

}
