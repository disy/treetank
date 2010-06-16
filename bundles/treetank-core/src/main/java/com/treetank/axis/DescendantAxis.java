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
 * $Id: DescendantAxis.java 4258 2008-07-14 16:45:28Z kramis $
 */

package com.treetank.axis;

import com.treetank.api.IAxis;
import com.treetank.api.IReadTransaction;
import com.treetank.node.IStructuralNode;
import com.treetank.settings.EFixed;
import com.treetank.utils.FastStack;

/**
 * <h1>DescendantAxis</h1>
 * 
 * <p>
 * Iterate over all descendants of kind ELEMENT or TEXT starting at a given
 * node. Self is not included.
 * </p>
 */
public class DescendantAxis extends AbstractAxis implements IAxis {

	/** Stack for remembering next nodeKey in document order. */
	private FastStack<Long> mRightSiblingKeyStack;

	/** The nodeKey of the next node to visit. */
	private long mNextKey;

	/**
	 * Constructor initializing internal state.
	 * 
	 * @param rtx
	 *            Exclusive (immutable) trx to iterate with.
	 */
	public DescendantAxis(final IReadTransaction rtx) {
		super(rtx);
	}

	/**
	 * Constructor initializing internal state.
	 * 
	 * @param rtx
	 *            Exclusive (immutable) trx to iterate with.
	 * @param includeSelf
	 *            Is self included?
	 */
	public DescendantAxis(final IReadTransaction rtx, final boolean includeSelf) {
		super(rtx, includeSelf);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void reset(final long nodeKey) {
		super.reset(nodeKey);
		mRightSiblingKeyStack = new FastStack<Long>();
		if (isSelfIncluded()) {
			mNextKey = getTransaction().getNode().getNodeKey();
		} else {
			mNextKey = ((IStructuralNode) getTransaction().getNode())
					.getFirstChildKey();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public final boolean hasNext() {
		resetToLastKey();

		// Fail if there is no node anymore.
		if (mNextKey == (Long) EFixed.NULL_NODE_KEY.getStandardProperty()) {
			resetToStartKey();
			return false;
		}

		getTransaction().moveTo(mNextKey);

		// Fail if the subtree is finished.
		if (((IStructuralNode) getTransaction().getNode()).getLeftSiblingKey() == getStartKey()) {
			resetToStartKey();
			return false;
		}

		// Always follow first child if there is one.
		if (((IStructuralNode) getTransaction().getNode()).hasFirstChild()) {
			mNextKey = ((IStructuralNode) getTransaction().getNode())
					.getFirstChildKey();
			if (((IStructuralNode) getTransaction().getNode())
					.hasRightSibling()) {
				mRightSiblingKeyStack.push(((IStructuralNode) getTransaction()
						.getNode()).getRightSiblingKey());
			}
			return true;
		}

		// Then follow right sibling if there is one.
		if (((IStructuralNode) getTransaction().getNode()).hasRightSibling()) {
			mNextKey = ((IStructuralNode) getTransaction().getNode()).getRightSiblingKey();
			return true;
		}

		// Then follow right sibling on stack.
		if (mRightSiblingKeyStack.size() > 0) {
			mNextKey = mRightSiblingKeyStack.pop();
			return true;
		}

		// Then end.
		mNextKey = (Long) EFixed.NULL_NODE_KEY.getStandardProperty();
		return true;
	}

}