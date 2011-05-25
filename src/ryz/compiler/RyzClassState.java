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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Intended to hold current RyzClass state to know if we are inside
 * a method, a class, or where.
 * 
 * User: oscarryz
 * Date: Dec 11, 2010
 * Time: 12:06:05 AM
 */
public abstract class RyzClassState {

    final Logger logger = Logger.getLogger(this.getClass().getName());
    private final RyzClass ryzClass;
    private List<LineTransformer> transformers;


    // TODO: have the transformers initialized differently
    // TODO: I think there is too much passing of the "ryzClass" instance, perhaps is not needed.
    RyzClassState(RyzClass ryzClass) {
        this.ryzClass = ryzClass;
    }


    RyzClass ryzClass() {
        return this.ryzClass;
    }


    abstract void previousState();
    

    public  List<LineTransformer> transformers(){
        return this.transformers;
    }

    void transformers(List<LineTransformer> lineTransformers) {
        this.transformers = lineTransformers;
    }


    abstract void nextState();

    public boolean addVariable(String accessModifier, String variableName, String variableType){
        logger.finest (getClass().getSimpleName() + " RyzClass: "+ryzClass.className() +" (not) adding variable: " + variableName );
       return true;
    }

    public void insideComment() {
        ryzClass().state(new InsideCommentState(ryzClass(), this));
    }

    public void outsideComment() {
        ryzClass().state().previousState();
    }

    public void insideBlock(String blockSignature){
        ryzClass.state(new InsideBlockState(ryzClass(), this, blockSignature));
    }
    public void keyClosed() {
        ryzClass().state().previousState();
    }

    public void insideMultilineString(int indentation) {
        ryzClass.state(new InsideMultiLineStringState(ryzClass(), this, indentation));
    }

    void ensureVariablesHolderInitialized(String method) {
        if( ryzClass().variables().get( method ) == null ){
            ryzClass().variables().put( method , new ArrayList<String>());
        }
    }

    public void outsideMultilineString() {
        ryzClass().state().previousState();
    }

    public void insideParameters() {}
}
