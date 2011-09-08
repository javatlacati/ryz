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
                new ReturnTransformer(this),
                new StatementTransformer(this),
                new SimpleAssignmentTransformer(this),
                new SingleValueLineTransformer(this),
                new InlineBlockTransformer(this)
        ));
    }



    @Override
    void previousState() {
        ryzClass().markLastLineAsReturn();
        ryzClass().state(new InsideClassState(ryzClass()));
    }

    @Override
    void nextState() {
        ryzClass().state(new InitialState(ryzClass()));
    }

    /**
     * When we are inside a method, just add the variable if is not already defined
     * in the class ( as an attribute ) or if it wasn't already defined in this
     * same method.
     * @param accessModifier - ignored, because it not needed here
     * @param variableName - the name to add ( or not )
     * @param variableType  - The variable type to add ( or not )
     * @return true if the variable could be added ( didn't exist as attribute nor was previously added )
     */
    @Override
    public boolean addVariable(String accessModifier, String variableName, String variableType) {
        //TODO: work on scope for variables when using blocks.
        // For instance, the followings fails:
        /*
          a : Integer
          test() {
             some( (){
                a  = a.+(1)
             })
          }
         */
        String variable = variableName + ":"+ variableType;
        //TODO: consider scenarios where the variable was added as attribute but a local var is needed and when the variable was added as a parameter
        String method = ryzClass().lastElementAdded();// ryzClass().methods().get( ryzClass().methods().size()-1);
        ensureVariablesHolderInitialized(method);
        ensureVariablesHolderInitialized("instance");

        logger.finest("variable = "+ variableName);
        logger.finest("ryzClass().variables().get(method) = " + ryzClass().variables().get(method));
        logger.finest("ryzClass().variables().get(\"instance\") = " +ryzClass().variables().get("instance"));
        logger.finest("ryzClass().variables().get(method).contains(variable) = " + containsVariable(method, variableName));
        logger.finest("ryzClass().variables().get(\"instance\").contains(variable) = " + containsVariable("instance", variable));
        //TODO: New local var may be ommited. This change introduces a new problem, if a local variable is declared
        return !containsVariable(method, variable)
                && !containsVariable("instance", variable)
                && ryzClass().variables().get(method).add(variable);
    }

    private boolean containsVariable(String scope, String variable) {
        String vn = variable.split(":")[0];
        for(String name : ryzClass().variables().get(scope) )  {
            String cvn = name.split(":")[0];
            if( cvn.equals( vn )) {
                return true;
            }
        }
        return false;
        //return ryzClass().variables().get(scope).contains(variable);
    }

    @Override
    public void insideParameters() {
        ryzClass().state( new InsideParametersState(ryzClass(),this));
    }
}
