/**
 * Copyright (c) 2011, University of Konstanz, Distributed Systems Group
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * * Neither the name of the University of Konstanz nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.treetank.service.xml.xpath.expr;

import org.treetank.api.INodeReadTrx;
import org.treetank.node.AtomicValue;
import org.treetank.service.xml.xpath.axis.VariableAxis;

/**
 * <h1>VarRefExpr</h1>
 * <p>
 * Reference to the current item of the variable expression.
 * </p>
 */
public class VarRefExpr extends AbsExpression {

    /** Key of the item the variable is set to at the moment. */
    private long mVarKey;

    /**
     * Constructor. Initializes the internal state.
     * 
     * @param rtx
     *            Exclusive (immutable) trx to iterate with.
     * @param mVariable
     *            Reference the variable expression that computes the items the
     *            variable holds.
     */
    public VarRefExpr(final INodeReadTrx rtx, final VariableAxis mVariable) {

        super(rtx);
        mVariable.addObserver(this);
        mVarKey = -1;

    }

    /**
     * {@inheritDoc}
     */
    public void update(final long mVarKey) {

        this.mVarKey = mVarKey;
        reset(this.mVarKey);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AtomicValue evaluate() {

        // assure that the transaction is set to the current context item of the
        // variable's binding sequence.
        moveTo(mVarKey);
        return null;
    }

}