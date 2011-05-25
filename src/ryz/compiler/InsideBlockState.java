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
 * When we're inside a block, closing a key should append another key and semicolon.
 * This class helps to this purpose. 
 * User: oscarryz
 * Date: Jan 7, 2011
 * Time: 4:42:24 PM
 */
public class InsideBlockState extends InsideMethodState {
    private final RyzClassState previousState;
    private final String blockSignature;

    public InsideBlockState(RyzClass ryzClass, RyzClassState state, String blockSignature) {
        super( ryzClass );

        this.blockSignature = blockSignature;
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

        this.previousState = state;
    }





    @Override
    void previousState() {
        ryzClass().markLastLineAsReturn(this.blockSignature.split(":")[1]);
        ryzClass().state(previousState);
    }

    @Override
    void nextState() {
        ryzClass().state(previousState);
    }

    @Override
    public void keyClosed() {
        ryzClass().outputLines().add(String.format("};%n"));
        super.keyClosed();
    }
}
