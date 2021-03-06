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

import java.io.DataOutput;
import java.io.IOException;

import org.treetank.data.delegates.NameNodeDelegate;
import org.treetank.data.delegates.NodeDelegate;
import org.treetank.data.interfaces.ITreeData;
import org.treetank.data.interfaces.ITreeNameData;
import org.treetank.exception.TTIOException;

import com.google.common.hash.Funnel;
import com.google.common.hash.PrimitiveSink;

/**
 * <h1>NamespaceNode</h1>
 * 
 * <p>
 * Node representing a namespace.
 * </p>
 */
public final class NamespaceNode implements ITreeData, ITreeNameData {

    /**
     * Enum for NamespaceFunnel.
     * 
     * @author Sebastian Graf, University of Konstanz
     * 
     */
    enum NamespaceFunnel implements Funnel<org.treetank.api.IData> {
        INSTANCE;
        public void funnel(org.treetank.api.IData data, PrimitiveSink into) {
            final NamespaceNode from = (NamespaceNode)data;
            from.mDel.getFunnel().funnel(from, into);
            from.mNameDel.getFunnel().funnel(from, into);
        }
    }

    /** Delegate for common node information. */
    private final NodeDelegate mDel;

    /** Delegate for name node information. */
    private final NameNodeDelegate mNameDel;

    /**
     * Constructor.
     * 
     * @param pDel
     *            delegate of node properties
     * @param pNameDel
     *            delegate for name properties
     */
    public NamespaceNode(final NodeDelegate pDel, final NameNodeDelegate pNameDel) {
        mDel = pDel;
        mNameDel = pNameDel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getKind() {
        return IConstants.NAMESPACE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNameKey() {
        return mNameDel.getNameKey();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setNameKey(final int pNameKey) {
        mNameDel.setNameKey(pNameKey);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getURIKey() {
        return mNameDel.getURIKey();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setURIKey(final int pUriKey) {
        mNameDel.setURIKey(pUriKey);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setHash(final long pHash) {
        mDel.setHash(pHash);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getHash() {
        return mDel.getHash();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getDataKey() {
        return mDel.getDataKey();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getParentKey() {
        return mDel.getParentKey();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasParent() {
        return mDel.hasParent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getTypeKey() {
        return mDel.getTypeKey();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setParentKey(final long pKey) {
        mDel.setParentKey(pKey);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTypeKey(final int pTypeKey) {
        mDel.setTypeKey(pTypeKey);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return toStringHelper(this).add("mDel", mDel).add("mNameDel", mNameDel).toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void serialize(final DataOutput pOutput) throws TTIOException {
        try {
            pOutput.writeInt(IConstants.NAMESPACE);
            mDel.serialize(pOutput);
            mNameDel.serialize(pOutput);
        } catch (final IOException exc) {
            throw new TTIOException(exc);
        }
    }

    @Override
    public Funnel<org.treetank.api.IData> getFunnel() {
        return NamespaceFunnel.INSTANCE;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((mDel == null) ? 0 : mDel.hashCode());
        result = prime * result + ((mNameDel == null) ? 0 : mNameDel.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        return this.hashCode() == obj.hashCode();
    }

}
