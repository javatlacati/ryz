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

import com.sun.corba.se.pept.transport.OutboundConnectionCache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class would be used to hold the state of the current compiled class.
 *
 * @author oscarryz
 * Date: Dec 2, 2010
 * Time: 4:08:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class RyzClass {
    private final List<String> sourceLines;
    private final List<String> generatedSource = new ArrayList<String>();
    private String name;
    private String packageName;
    RyzClassState state;

    private List<String> methods;
    private HashMap<String, List<String>> attributes = new HashMap<String,List<String>>();
    private Logger logger = Logger.getLogger(this.getClass().getName());

    public RyzClass(List<String> sourceLines) {
        if( logger.isLoggable(Level.FINEST)){
            StringBuilder sb = new StringBuilder();
            for (String sourceLine : sourceLines) {
                sb.append(sourceLine);
                sb.append("\n");
            }
            logger.finest(sb.toString());
        }
        this.sourceLines = sourceLines;
        this.methods = new ArrayList<String>();
        setState(new InitialState(this));
    }


    public void setClassName(String theClassName ) {
        this.name = theClassName.trim();
        state.nextState();
    }


    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String name() {
        if( name == null ){
            throw new IllegalStateException("Should have name by now");
        }
        return this.name;
    }

    List<String> methods(){
        return this.methods;
    }
    Map<String,List<String>> attributes(){
        return this.attributes;
    }

    /**
     * Takes the list of source code lines and sends it to the transformers
     * to produce translated  ( java ) code.
     */
    public void transformSourceCode() {

        for( String line : sourceLines ) {
            for( LineTransformer t : transformers() ) {
                t.transform( line, generatedSource );
            }
        }
        
    }

    /**
     * Returns the list of translated source code.
     * @return
     */
    public List<String> outputLines() {
        return generatedSource;
    }

    private List<LineTransformer> transformers() {
        return state.transformers();
    }

    /**
     * Close key means the current state has finished and we should return
     * to the previous state.
     */
    public void closeKey() {
        this.state.previousState();
        //To change body of created methods use File | Settings | File Templates.
    }

    public void addMethod(String methodName, String methodType) {
        this.methods.add(methodName + ":" + methodType );// TODO: add args
        state.nextState();
    }

    public void setState(RyzClassState classState) {
        this.state = classState;
    }

    public boolean addVariable(String accessModifier, String variableName, String variableType) {
        return state.addVariable( accessModifier, variableName, variableType);
    }

    /**
     * Makes this class to enter in "comment" mode so the lines are not
     * interpreted anymore
     */
    public void insideComment() {
        state.insideComment();
    }

    /**
     * Makes this class to enter in "outside comment" mode, returning to the
     * previous state
     */
    public void outsideComment() {
        state.outsideComment();
    }
}
