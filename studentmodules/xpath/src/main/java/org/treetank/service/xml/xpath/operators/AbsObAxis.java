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

package org.treetank.service.xml.xpath.operators;

import static org.treetank.service.xml.xpath.XPathAxis.XPATH_10_COMP;

import java.util.List;

import org.treetank.api.INodeReadTransaction;
import org.treetank.axis.AbsAxis;
import org.treetank.exception.TTXPathException;
import org.treetank.node.interfaces.INode;
import org.treetank.service.xml.xpath.AtomicValue;
import org.treetank.service.xml.xpath.functions.Function;
import org.treetank.service.xml.xpath.types.Type;

/**
 * <h1>AbstractOpAxis</h1>
 * <p>
 * Abstract axis for all operators performing an arithmetic operation.
 * </p>
 */
public abstract class AbsObAxis extends AbsAxis {

    /** First arithmetic operand. */
    private final AbsAxis mOperand1;

    /** Second arithmetic operand. */
    private final AbsAxis mOperand2;

    /** True, if axis has not been evaluated yet. */
    private boolean mIsFirst;

    private List<AtomicValue> mToStore;

    /**
     * Constructor. Initializes the internal state.
     * 
     * @param rtx
     *            Exclusive (immutable) trx to iterate with.
     * @param mOp1
     *            First value of the operation
     * @param mOp2
     *            Second value of the operation
     */
    public AbsObAxis(final INodeReadTransaction rtx, final AbsAxis mOp1, final AbsAxis mOp2,
        final List<AtomicValue> pToStore) {

        super(rtx);
        mOperand1 = mOp1;
        mOperand2 = mOp2;
        mIsFirst = true;
        mToStore = pToStore;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void reset(final long mNodeKey) {

        super.reset(mNodeKey);
        mIsFirst = true;
        if (mOperand1 != null) {
            mOperand1.reset(mNodeKey);
        }

        if (mOperand2 != null) {
            mOperand2.reset(mNodeKey);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasNext() {

        resetToLastKey();

        if (mIsFirst) {
            mIsFirst = false;

            if (mOperand1.hasNext()) {
                // atomize operand
                final AtomicValue mItem1 = atomize(mOperand1);

                if (mOperand2.hasNext()) {
                    // atomize operand
                    final AtomicValue mItem2 = atomize(mOperand2);
                    try {
                        final AtomicValue result = operate(mItem1, mItem2);
                        // add retrieved AtomicValue to item list
                        mToStore.add(result);
                        final int itemKey = getTransaction().getItemList().addItem(result);
                        getTransaction().moveTo(itemKey);

                        return true;
                    } catch (TTXPathException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            if (XPATH_10_COMP) { // and empty sequence, return NaN
                final AtomicValue result = new AtomicValue(Double.NaN, Type.DOUBLE);
                mToStore.add(result);
                final int itemKey = getTransaction().getItemList().addItem(result);
                getTransaction().moveTo(itemKey);
                return true;
            }

        }
        // either not the first call, or empty sequence
        resetToStartKey();
        return false;

    }

    /**
     * Atomizes an operand according to the rules specified in the XPath
     * specification.
     * 
     * @param mOperand
     *            the operand to atomize
     * @return the atomized operand. (always an atomic value)
     */
    private AtomicValue atomize(final AbsAxis mOperand) {

        final INodeReadTransaction rtx = getTransaction();
        int type = rtx.getNode().getTypeKey();
        AtomicValue atom;

        if (XPATH_10_COMP) {

            atom = new AtomicValue(rtx.getValueOfCurrentNode().getBytes(), rtx.getNode().getTypeKey());
        } else {
            // unatomicType is cast to double
            if (type == rtx.keyForName("xs:untypedAtomic")) {
                type = rtx.keyForName("xs:double");
                // TODO: throw error, of cast fails
            }

            atom = new AtomicValue(rtx.getValueOfCurrentNode().getBytes(), type);
        }

        // if (!XPATH_10_COMP && operand.hasNext()) {
        // throw new XPathError(ErrorType.XPTY0004);
        // }

        return atom;
    }

    /**
     * Performs the operation on the two input operands. First checks if the
     * types of the operands are a valid combination for the operation and if so
     * computed the result. Otherwise an XPathError is thrown.
     * 
     * @param mOperand1
     *            first input operand
     * @param mOperand2
     *            second input operand
     * @return result of the operation
     * @throws TTXPathException
     *             if the operations fails
     */
    protected abstract AtomicValue operate(final AtomicValue mOperand1, final AtomicValue mOperand2)
        throws TTXPathException;

    /**
     * Checks if the types of the operands are a valid combination for the
     * operation and if so returns the corresponding result type. Otherwise an
     * XPathError is thrown. This typed check is done according to the <a
     * href="http://www.w3.org/TR/xpath20/#mapping">Operator Mapping</a>.
     * 
     * @param mOp1
     *            first operand's type key
     * @param mOp2
     *            second operand's type key
     * @return return type of the arithmetic function according to the operand
     *         type combination.
     * @throws TTXPathException
     *             if type is not specified
     */
    protected abstract Type getReturnType(final int mOp1, final int mOp2) throws TTXPathException;

}
