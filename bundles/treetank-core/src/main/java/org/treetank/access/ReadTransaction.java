/**
 * Copyright (c) 2011, University of Konstanz, Distributed Systems Group
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the University of Konstanz nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
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

package org.treetank.access;

import java.lang.reflect.Field;

import javax.xml.namespace.QName;

import org.treetank.api.IItem;
import org.treetank.api.IItemList;
import org.treetank.api.IReadTransaction;
import org.treetank.api.IStructuralItem;
import org.treetank.exception.AbsTTException;
import org.treetank.exception.TTIOException;
import org.treetank.node.*;
import org.treetank.service.xml.xpath.AtomicValue;
import org.treetank.settings.EFixed;
import org.treetank.utils.NamePageHash;
import org.treetank.utils.TypedValue;

/**
 * <h1>ReadTransaction</h1>
 * 
 * <p>
 * Read-only transaction wiht single-threaded cursor semantics. Each read-only transaction works on a given
 * revision key.
 * </p>
 */
public class ReadTransaction implements IReadTransaction {

    /** ID of transaction. */
    private final long mTransactionID;

    /** Session state this write transaction is bound to. */
    private SessionState mSessionState;

    /** State of transaction including all cached stuff. */
    private ReadTransactionState mTransactionState;

    /** Strong reference to currently selected node. */
    private IItem mCurrentNode;

    /** Tracks whether the transaction is closed. */
    private boolean mClosed;

    /**
     * Constructor.
     * 
     * @param paramTransactionID
     *            ID of transaction.
     * @param paramSessionState
     *            Session state to work with.
     * @param paramTransactionState
     *            Transaction state to work with.
     * @throws TTIOException
     *             if something odd happens within the creation process.
     */
    protected ReadTransaction(final long paramTransactionID, final SessionState paramSessionState,
        final ReadTransactionState paramTransactionState) throws TTIOException {
        mTransactionID = paramTransactionID;
        mSessionState = paramSessionState;
        mTransactionState = paramTransactionState;
        mCurrentNode = getTransactionState().getNode((Long)EFixed.ROOT_NODE_KEY.getStandardProperty());
        mClosed = false;
    }

    /**
     * Constructor.
     * 
     * @param paramRtx
     *            Transaction state to work with.
     */
    public ReadTransaction(final ReadTransaction paramRtx) {
        mTransactionID = paramRtx.getTransactionID();
        Class c = paramRtx.getClass();
        Field[] fields = c.getDeclaredFields();
        try {
            for (int i = 0; i < fields.length; i++) {
                if (fields[i].getName().equals("mSessionState")) {
                    mSessionState = (SessionState)(fields[i].get(paramRtx));
                } else if (fields[i].getName().equals("mTransactionState")) {
                    mTransactionState = (ReadTransactionState)fields[i].get(paramRtx);
                }
            }

            assert mSessionState != null;
            assert mTransactionState != null;
            assert paramRtx.getNode().getNodeKey() >= 0; // has to be node
            mCurrentNode = getTransactionState().getNode(paramRtx.getNode().getNodeKey());
            mClosed = false;

        } catch (final TTIOException mExp) {
            mExp.getStackTrace();
        } catch (final IllegalArgumentException mExp) {
            mExp.printStackTrace();
        } catch (final IllegalAccessException mExp) {
            mExp.printStackTrace();
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final long getTransactionID() {
        return mTransactionID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final long getRevisionNumber() throws TTIOException {
        assertNotClosed();
        return mTransactionState.getActualRevisionRootPage().getRevision();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final long getRevisionTimestamp() throws TTIOException {
        assertNotClosed();
        return mTransactionState.getActualRevisionRootPage().getRevisionTimestamp();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean moveTo(final long mNodeKey) {
        assertNotClosed();
        if (mNodeKey != (Long)EFixed.NULL_NODE_KEY.getStandardProperty()) {
            // Remember old node and fetch new one.
            final IItem oldNode = mCurrentNode;
            try {
                mCurrentNode = mTransactionState.getNode(mNodeKey);
            } catch (final Exception e) {
                mCurrentNode = null;
            }
            if (mCurrentNode != null) {
                return true;
            } else {
                mCurrentNode = oldNode;
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean moveToDocumentRoot() {
        return moveTo((Long)EFixed.ROOT_NODE_KEY.getStandardProperty());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean moveToParent() {
        return moveTo(mCurrentNode.getParentKey());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean moveToFirstChild() {
        final IStructuralItem node = getStructuralNode();
        return node == null ? false : moveTo(node.getFirstChildKey());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean moveToLeftSibling() {
        final IStructuralItem node = getStructuralNode();
        return node == null ? false : moveTo(node.getLeftSiblingKey());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean moveToRightSibling() {
        final IStructuralItem node = getStructuralNode();
        return node == null ? false : moveTo(node.getRightSiblingKey());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean moveToAttribute(final int mIndex) {
        if (mCurrentNode.getKind() == ENodes.ELEMENT_KIND) {
            return moveTo(((ElementNode)mCurrentNode).getAttributeKey(mIndex));
        } else {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean moveToNamespace(final int mIndex) {
        if (mCurrentNode.getKind() == ENodes.ELEMENT_KIND) {
            return moveTo(((ElementNode)mCurrentNode).getNamespaceKey(mIndex));
        } else {
            return false;
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final String getValueOfCurrentNode() {
        assertNotClosed();
        return TypedValue.parseString(mCurrentNode.getRawValue());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final QName getQNameOfCurrentNode() {
        assertNotClosed();
        final String name = mTransactionState.getName(mCurrentNode.getNameKey());
        final String uri = mTransactionState.getName(mCurrentNode.getURIKey());
        return name == null ? null : buildQName(uri, name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final String getTypeOfCurrentNode() {
        assertNotClosed();
        return mTransactionState.getName(mCurrentNode.getTypeKey());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final int keyForName(final String mName) {
        assertNotClosed();
        return NamePageHash.generateHashForString(mName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final String nameForKey(final int mKey) {
        assertNotClosed();
        return mTransactionState.getName(mKey);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final byte[] rawNameForKey(final int paramKey) {
        assertNotClosed();
        return mTransactionState.getRawName(paramKey);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final IItemList getItemList() {
        return mTransactionState.getItemList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws AbsTTException {
        if (!mClosed) {
            // Close own state.
            mTransactionState.close();

            // Callback on session to make sure everything is cleaned up.
            mSessionState.closeReadTransaction(mTransactionID);

            // Immediately release all references.
            mSessionState = null;
            mTransactionState = null;
            mCurrentNode = null;

            mClosed = true;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final String toString() {
        assertNotClosed();
        final StringBuilder builder = new StringBuilder();
        if (getNode().getKind() == ENodes.ATTRIBUTE_KIND || getNode().getKind() == ENodes.ELEMENT_KIND) {
            builder.append("Name of Node: ");
            builder.append(getQNameOfCurrentNode().toString());
            builder.append("\n");
        }
        if (getNode().getKind() == ENodes.ATTRIBUTE_KIND || getNode().getKind() == ENodes.TEXT_KIND) {
            builder.append("Value of Node: ");
            builder.append(getValueOfCurrentNode());
            builder.append("\n");
        }
        if (getNode().getKind() == ENodes.ROOT_KIND) {
            builder.append("Node is DocumentRoot");
            builder.append("\n");
        }
        builder.append(getNode().toString());

        return builder.toString();
    }

    /**
     * Set state to closed.
     */
    protected final void setClosed() {
        mClosed = true;
    }

    /**
     * Is the transaction closed?
     * 
     * @return True if the transaction was closed.
     */
    public final boolean isClosed() {
        return mClosed;
    }

    /**
     * Make sure that the session is not yet closed when calling this method.
     */
    protected final void assertNotClosed() {
        if (mClosed) {
            throw new IllegalStateException("Transaction is already closed.");
        }
    }

    /**
     * Getter for superclasses.
     * 
     * @return The state of this transaction.
     */
    public ReadTransactionState getTransactionState() {
        return mTransactionState;
    }

    /**
     * Replace the state of the transaction.
     * 
     * @param paramTransactionState
     *            State of transaction.
     */
    protected final void setTransactionState(final ReadTransactionState paramTransactionState) {
        mTransactionState = paramTransactionState;
    }

    /**
     * Getter for superclasses.
     * 
     * @return The session state.
     */
    public final SessionState getSessionState() {
        return mSessionState;
    }

    /**
     * Set session state.
     * 
     * @param paramSessionState
     *            Session state to set.
     */
    protected final void setSessionState(final SessionState paramSessionState) {
        mSessionState = paramSessionState;
    }

    /**
     * Getter for superclasses.
     * 
     * @return The current node.
     */
    protected final IItem getCurrentNode() {
        return mCurrentNode;
    }

    /**
     * Setter for superclasses.
     * 
     * @param paramCurrentNode
     *            The current node to set.
     */
    protected final void setCurrentNode(final IItem paramCurrentNode) {
        mCurrentNode = paramCurrentNode;
    }

    // /**
    // * {@inheritDoc}
    // */
    // @Override
    // public final IItem getNode() {
    // return mCurrentNode;
    // }

    /**
     * {@inheritDoc}
     */
    @Override
    public final long getMaxNodeKey() throws TTIOException {
        return getTransactionState().getActualRevisionRootPage().getMaxNodeKey();
    }

    /**
     * Building QName out of uri and name. The name can have the prefix denoted
     * with ":";
     * 
     * @param mUri
     *            the namespaceuri
     * @param mName
     *            the name including a possible prefix
     * @return the QName obj
     */
    protected static final QName buildQName(final String mUri, final String mName) {
        QName qname;
        if (mName.contains(":")) {
            qname = new QName(mUri, mName.split(":")[1], mName.split(":")[0]);
        } else {
            qname = new QName(mUri, mName);
        }
        return qname;
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    public final IStructuralItem getStructuralNode() {
        if (mCurrentNode instanceof AbsStructNode) {
            return (IStructuralItem)mCurrentNode;
        } else {
            return DummyNode.createData(mCurrentNode.getNodeKey(), mCurrentNode.getParentKey());
        }
    }

    /** {@inheritDoc} */
    @Override
    public TextNode getNode(final TextNode paramNode) {
        return paramNode;
    }

    /** {@inheritDoc} */
    @Override
    public ElementNode getNode(final ElementNode paramNode) {
        return paramNode;
    }

    /** {@inheritDoc} */
    @Override
    public AttributeNode getNode(final AttributeNode paramNode) {
        return paramNode;
    }

    /** {@inheritDoc} */
    @Override
    public NamespaceNode getNode(final NamespaceNode paramNode) {
        return paramNode;
    }

    /** {@inheritDoc} */
    @Override
    public DeletedNode getNode(final DeletedNode paramNode) {
        return paramNode;
    }

    /** {@inheritDoc} */
    @Override
    public DocumentRootNode getNode(final DocumentRootNode paramNode) {
        return paramNode;
    }

    /** {@inheritDoc} */
    @Override
    public IItem getNode(final AtomicValue paramNode) {
        return paramNode;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public <T extends IItem> T getNode() {
        return (T)mCurrentNode.accept(this);
    }

    /** {@inheritDoc} */
    @Override
    public DummyNode getNode(final DummyNode paramNode) {
        return paramNode;
    }
}