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

import java.util.Arrays;

/**
 * Used to keep the state of the class when it is inside a class
 * User: oscarryz
 * Date: Dec 11, 2010
 * Time: 1:45:58 AM
 * To change this template use File | Settings | File Templates.
 */
public class InsideClassState extends RyzClassState {
    public InsideClassState(RyzClass ryzClass) {
        super(ryzClass);
        transformers(Arrays.asList(
            new ImportTransformer(this),
            new AttributeTransformer(this),
            new CommentTransformer(this),
            new ClosingKeyTransformer(this),
            new MethodTransformer(this),
            new ConstructorTransformer(this),
            new AnnotationTransformer(this)
        ));
        
    }

    /**
     * When the transforming state is inside a class, then add the variable
     * if it doesn't exists already.
     * @param accessModifier - variable access modifier
     * @param variableName - the variable name to add
     * @param variableType - the type of the variable
     * @return true if the variable was added.
     */
    @Override
    public boolean addVariable(String accessModifier, String variableName, String variableType) {
        String variable = variableName + ":"+ variableType;
        //TODO: introduce value object
        ensureVariablesHolderInitialized("instance");
        // TODO: Think on an scenario where we need to know if a variable was already declared.
        //return !ryzClass().variables().get("instance").contains(variable)
        //        && ryzClass().variables().get("instance").add(variable);
        return ryzClass().variables().get("instance").add(variable);
    }

    @Override
    void previousState() {
        ryzClass().state(new InitialState(ryzClass()));
    }

    @Override
    void nextState() {
        ryzClass().state(new InsideMethodState(ryzClass()));
    }
}
