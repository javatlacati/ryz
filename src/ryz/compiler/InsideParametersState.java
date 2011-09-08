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
 * Indicates the class is inside the parameters definition
 * Date: Dec 11, 2010
 * Time: 1:47:43 AM
 */
public class InsideParametersState extends RyzClassState {

    private final RyzClassState previousState;

    public InsideParametersState(RyzClass ryzClass, RyzClassState currentState) {
        super(ryzClass);
        transformers(Arrays.asList(
                new CommentTransformer(this),
                new SimpleAssignmentTransformer(this),
                new StatementTransformer(this),
                new SingleValueLineTransformer(this)
        ));

        this.previousState = currentState;
    }


    @Override
    void previousState() {
        ryzClass().state(previousState);
    }

    @Override
    void nextState() {
        ryzClass().state(previousState);
    }


    /**
     * When we are inside parameters definition, we just add shadowing any
     * attribute var.
     * @param accessModifier - ignored, because it not needed here
     * @param variableName - the name to add ( or not )
     * @param variableType  - The variable type to add ( or not )
     * @return true if the variable could be added (if it was not previously added )
     */
    @Override
    public boolean addVariable(String accessModifier, String variableName, String variableType) {
        //TODO: refactor into a single piece of logic between: InsideMethodState, InsideClassState and this class InsideParameterState
        String variable = variableName+":"+variableType;

        //TODO: consider scenarios where the variable was added as attribute but a local var is needed and when the variable was added as a parameter
        String method = ryzClass().lastElementAdded(); //;ryzClass().methods().get( ryzClass().methods().size()-1);
        ensureVariablesHolderInitialized(method);

        //logger.finest("variable = "+ variable);
        //logger.finest("ryzClass().variables().get(method) = " + ryzClass().variables().get(method));
        //logger.finest("ryzClass().variables().get(\"instance\") = " +ryzClass().variables().get("instance"));
        //logger.finest("ryzClass().variables().get(method).contains(variable) = " + ryzClass().variables().get(method).contains(variable));
        //TODO: findout if this is needed, otherwise just remove it
        //return !ryzClass().variables().get(method).contains(variable)
        //        && ryzClass().variables().get(method).add(variable);
          return    ryzClass().variables().get(method).add(variable);

    }

}
