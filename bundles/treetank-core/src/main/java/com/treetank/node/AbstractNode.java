/*
 * Copyright (c) 2008, Marc Kramis (Ph.D. Thesis), University of Konstanz
 * 
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 * 
 * $Id: AbstractNode.java 4550 2009-02-05 09:25:46Z graf $
 */

package com.treetank.node;

import com.treetank.api.IItem;
import com.treetank.io.ITTSink;
import com.treetank.io.ITTSource;
import com.treetank.settings.EFixed;
import com.treetank.settings.ENodes;

/**
 * <h1>AbstractNode</h1>
 * 
 * <p>
 * Abstract node class to implement all methods required with INode. To reduce
 * implementation overhead in subclasses it implements all methods but does
 * silently not do anything there. A subclass must only implement those methods
 * that are required to provide proper subclass functionality.
 * </p>
 */
public abstract class AbstractNode implements IItem, Comparable<AbstractNode> {

	/** standard NODE_KEY. */
	protected static final int NODE_KEY = 0;

	/** Node key is common to all node kinds. */
	protected final long[] mData;

	/**
	 * Constructor for inserting node.
	 * 
	 * @param nodeKey
	 *            Key of node.
	 * @param size
	 *            Size of the data.
	 */
	public AbstractNode(final int size, final long nodeKey) {
		mData = new long[size];
		mData[NODE_KEY] = nodeKey;
	}

	/**
	 * Constructor for modification of existing node.
	 * 
	 * @param node
	 *            to be set
	 */
	protected AbstractNode(final AbstractNode node) {
		mData = new long[node.mData.length];
		System.arraycopy(node.mData, 0, mData, 0, mData.length);
	}

	/**
	 * Constructor for read only access of nodes
	 * 
	 * @param in
	 *            Input bytes to read node from.
	 * @param size
	 *            Size of the data.
	 */
	protected AbstractNode(final int size, final ITTSource in) {
		mData = new long[size];
		for (int i = 0; i < size; i++) {
			mData[i] = in.readLong();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public final long getNodeKey() {
		return mData[NODE_KEY];
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean hasParent() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	public long getParentKey() {
		return (Long) EFixed.NULL_NODE_KEY.getStandardProperty();
	}

	/**
	 * {@inheritDoc}
	 */
	public ENodes getKind() {
		return ENodes.UNKOWN_KIND;
	}

	/**
	 * {@inheritDoc}
	 */
	public int getNameKey() {
		return (Integer) EFixed.NULL_INT_KEY.getStandardProperty();
	}

	/**
	 * {@inheritDoc}
	 */
	public int getURIKey() {
		return (Integer) EFixed.NULL_INT_KEY.getStandardProperty();
	}

	/**
	 * {@inheritDoc}
	 */
	public int getTypeKey() {
		return (Integer) EFixed.NULL_INT_KEY.getStandardProperty();
	}

	/**
	 * {@inheritDoc}
	 */
	public byte[] getRawValue() {
		return null;
	}

	/**
	 * Serializing the data.
	 * 
	 * @param out
	 *            target to serialize.
	 */
	public void serialize(final ITTSink out) {
		for (final long longVal : mData) {
			out.writeLong(longVal);
		}

	}

	/**
	 * {@inheritDoc}
	 */
	public void setNodeKey(final long nodeKey) {
		mData[NODE_KEY] = nodeKey;
	}

	/**
	 * Setting the parent key.
	 * 
	 * @param parentKey
	 *            the key for the parent.
	 */
	public void setParentKey(final long parentKey) {
	}


	/**
	 * Setting the kind of this node.
	 * 
	 * @param kind
	 *            to be set.
	 */
	public void setKind(final byte kind) {
	}

	/**
	 * Setting the name key for this node.
	 * 
	 * @param nameKey
	 *            to be set.
	 */
	public void setNameKey(final int nameKey) {
	}

	/**
	 * Setting the uri for this node.
	 * 
	 * @param uriKey
	 *            to be set.
	 */
	public void setURIKey(final int uriKey) {
	}

	/**
	 * Setting the value for this node.
	 * 
	 * @param valueType
	 *            type of value to be set.
	 * @param value
	 *            the value to be set.
	 */
	public void setValue(final int valueType, final byte[] value) {
	}

	/**
	 * Setting the type of this node.
	 * 
	 * @param valueType
	 *            to be set.
	 */
	public void setType(final int valueType) {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		return (int) mData[NODE_KEY];
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(final Object obj) {
		return ((obj != null) && (mData[NODE_KEY] == ((AbstractNode) obj).mData[NODE_KEY]));
	}

	/**
	 * {@inheritDoc}
	 */
	public int compareTo(final AbstractNode node) {
		final long nodeKey = (node).getNodeKey();
		if (mData[NODE_KEY] < nodeKey) {
			return -1;
		} else if (mData[NODE_KEY] == nodeKey) {
			return 0;
		} else {
			return 1;
		}
	}

	@Override
	public String toString() {
		return new StringBuilder(this.getClass().getName()).append(
				"\n\tnode key: ").append(getNodeKey()).append(
				"\n\tparentKey: ").append(getParentKey()).toString();
	}

	static class Builder {

	}

}