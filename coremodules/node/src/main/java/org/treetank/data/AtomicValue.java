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

package org.treetank.data;

import static com.google.common.base.Objects.toStringHelper;
import static org.treetank.data.IConstants.NULL_NODE;

import java.io.DataOutput;

import org.treetank.data.interfaces.ITreeData;
import org.treetank.data.interfaces.ITreeValData;
import org.treetank.exception.TTIOException;
import org.treetank.utils.NamePageHash;
import org.treetank.utils.TypedValue;

import com.google.common.hash.Funnel;
import com.google.common.hash.PrimitiveSink;

/**
 * <h1>AtomicValue</h1>
 * <p>
 * An item represents either an atomic value or a node. An atomic value is a value in the value space of an
 * atomic type, as defined in <a href="http://www.w3.org/TR/xmlschema11-2/">XMLSchema 1.1</a>. (Definition:
 * Atomic types are anyAtomicType and all types derived from it.)
 * </p>
 */
public class AtomicValue implements ITreeData, ITreeValData {

    /**
     * Enum for AtomicValueFunnel.
     * 
     * @author Sebastian Graf, University of Konstanz
     * 
     */
    enum AtomicValueFunnel implements Funnel<org.treetank.api.IData> {
        INSTANCE;
        public void funnel(org.treetank.api.IData data, PrimitiveSink into) {
            final AtomicValue from = (AtomicValue)data;
            into.putLong(from.mItemKey).putBytes(from.mValue).putInt(from.mType);
        }
    }

    /** Value of the item as byte array. */
    private byte[] mValue;

    /** The item's value type. */
    private int mType;

    /**
     * The item's key. In case of an Atomic value this is always a negative to
     * make them distinguishable from nodes.
     */
    private long mItemKey;

    /**
     * Constructor. Initializes the internal state.
     * 
     * @param pValue
     *            the value of the Item
     * @param pType
     *            the item's type
     */
    public AtomicValue(final byte[] pValue, final int pType) {

        this.mValue = pValue;
        this.mType = pType;
    }

    /**
     * Constructor. Initializes the internal state.
     * 
     * @param pValue
     *            the value of the Item
     */
    public AtomicValue(final boolean pValue) {

        this.mValue = TypedValue.getBytes(Boolean.toString(pValue));
        this.mType = NamePageHash.generateHashForString("xs:boolean");

    }

    /**
     * Constructor. Initializes the internal state.
     * 
     * @param pValue
     *            the value of the Item
     * @param pType
     *            the item's type
     */
    public AtomicValue(final Number pValue, final Type pType) {

        this.mValue = TypedValue.getBytes(pValue.toString());
        this.mType = NamePageHash.generateHashForString(pType.getStringRepr());
    }

    /**
     * Constructor. Initializes the internal state.
     * 
     * @param pValue
     *            the value of the Item
     * @param pType
     *            the item's type
     */
    public AtomicValue(final String pValue, final Type pType) {

        this.mValue = TypedValue.getBytes(pValue);
        this.mType = NamePageHash.generateHashForString(pType.getStringRepr());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getParentKey() {
        return NULL_NODE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasParent() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getDataKey() {
        return mItemKey;
    }

    /**
     * Setting the node key for an item
     * 
     * @param pNodeKey
     *            to be set
     */
    public void setNodeKey(long pNodeKey) {
        mItemKey = pNodeKey;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getKind() {
        return IConstants.UNKNOWN;
    }

    /**
     * Check if is fulltext.
     * 
     * @return true if fulltext, false otherwise
     */
    public boolean isFullText() {

        return false;
    }

    /**
     * Test if the lead is tes.
     * 
     * @return true if fulltest leaf, false otherwise
     */
    public boolean isFullTextLeaf() {

        return false;
    }

    /**
     * Test if the root is full text.
     * 
     * @return true if fulltest root, false otherwise
     */
    public boolean isFullTextRoot() {

        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final int getTypeKey() {
        return mType;
    }

    /**
     * Getting the type of the value.
     * 
     * @return the type of this value
     */
    public final String getType() {
        return Type.getType(mType).getStringRepr();
    }

    /**
     * Returns the atomic value as an integer.
     * 
     * @return the value as an integer
     */
    public int getInt() {

        return (int)getDBL();
    }

    /**
     * Returns the atomic value as a boolean.
     * 
     * @return the value as a boolean
     */
    public boolean getBool() {

        return Boolean.parseBoolean(new String(mValue));
    }

    /**
     * Returns the atomic value as a float.
     * 
     * @return the value as a float
     */
    public float getFLT() {

        return Float.parseFloat(new String(mValue));
    }

    /**
     * Returns the atomic value as a double.
     * 
     * @return the value as a double
     */
    public double getDBL() {
        return Double.parseDouble(new String(mValue));
    }

    @Override
    public void setHash(long hash) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getHash() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setParentKey(long paramKey) {
        throw new UnsupportedOperationException();

    }

    @Override
    public void setTypeKey(int paramType) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] getRawValue() {
        return mValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setValue(byte[] paramVal) {
        mValue = paramVal;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return toStringHelper(this).add("mValue", mValue).add("mType", mType).add("mItemKey", mItemKey)
            .toString();
    }

    /**
     * Serializing to given dataput
     * 
     * @param pOutput
     *            to serialize to
     * @throws TTIOException
     */
    public void serialize(final DataOutput pOutput) throws TTIOException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Funnel<org.treetank.api.IData> getFunnel() {
        return AtomicValueFunnel.INSTANCE;
    }

}
