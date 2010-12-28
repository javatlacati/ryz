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

/**
 * Indicates the class is inside a method definition 
 * * User: oscarryz
 * Date: Dec 11, 2010
 * Time: 1:47:43 AM
 * To change this template use File | Settings | File Templates.
 */
public class InsideMethodState extends RyzClassState {
    public InsideMethodState(RyzClass ryzClass) {
        super(ryzClass);
        transformers(Arrays.asList(
                new AttributeTransformer(this, false),
                new CommentTransformer(this),
                new ClosingKeyTransformer(this),
                new MethodTransformer(this),
                new ReturnTransformer(this),
                new StatementTransformer(this),
                new SimpleAssignmentTransformer(this)
        ));
    }



    @Override
    void previousState() {
        ryzClass().setState(new InsideClassState(ryzClass()));
    }

    @Override
    void nextState() {
        ryzClass().setState(new InitialState(ryzClass()));
    }

    @Override
    public boolean addVariable(String accessModifier, String variableName, String variableType) {
        String method = ryzClass().methods().get( ryzClass().methods().size()-1);
        if( ryzClass().attributes().get( method ) == null ){
            ryzClass().attributes().put( method , new ArrayList<String>());
        }
        String variable = variableName;//String.format("(%s)%s:%s", accessModifier, variableName, variableType);

        logger.warning("variable = "+ variable);
        if(!ryzClass().attributes().get(method).contains(variable)){
            return ryzClass().attributes().get(method).add(variable);
        }
        return false;

    }
}
