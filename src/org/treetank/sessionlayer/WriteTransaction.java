/*
 * TreeTank - Embedded Native XML Database
 * 
 * Copyright 2007 Marc Kramis
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * $Id:WriteTransaction.java 3019 2007-10-10 13:28:24Z kramis $
 */

package org.treetank.sessionlayer;

import org.treetank.api.IConstants;
import org.treetank.api.IWriteTransaction;
import org.treetank.pagelayer.Node;
import org.treetank.pagelayer.RevisionRootPage;
import org.treetank.utils.UTF;

/**
 * <h1>WriteTransaction</h1>
 * 
 * <p>
 * Single-threaded instance of only write transaction per session.
 * </p>
 */
public final class WriteTransaction extends ReadTransaction
    implements
    IWriteTransaction {

  /**
   * Constructor.
   * 
   * @param initRevisionRootPage Revision root page to work with.
   */
  protected WriteTransaction(
      final TransactionState state,
      final RevisionRootPage revisionRootPage) {
    super(state, revisionRootPage);
  }

  /**
   * {@inheritDoc}
   */
  public final long insertRoot(final String document) throws Exception {

    if (mRevisionRootPage.getNodeCount() != 0) {
      throw new IllegalStateException("Root node already exists.");
    }

    // Create new root node.
    mCurrentNode =
        mRevisionRootPage.createNode(
            mState,
            IConstants.NULL_KEY,
            IConstants.NULL_KEY,
            IConstants.NULL_KEY,
            IConstants.NULL_KEY,
            IConstants.DOCUMENT,
            mRevisionRootPage.createNameKey(mState, ""),
            mRevisionRootPage.createNameKey(mState, ""),
            mRevisionRootPage.createNameKey(mState, ""),
            UTF.convert(document));

    return mCurrentNode.getNodeKey();
  }

  /**
   * {@inheritDoc}
   */
  public final long insertFirstChild(
      final int kind,
      final String localPart,
      final String uri,
      final String prefix,
      final byte[] value) throws Exception {

    assertIsSelected();

    // Insert new node in place of current first child.
    if (mCurrentNode.getChildCount() > 0) {

      // Create new first child node.
      mCurrentNode =
          mRevisionRootPage.createNode(
              mState,
              mCurrentNode.getNodeKey(),
              IConstants.NULL_KEY,
              IConstants.NULL_KEY,
              mCurrentNode.getFirstChildKey(),
              kind,
              mRevisionRootPage.createNameKey(mState, localPart),
              mRevisionRootPage.createNameKey(mState, uri),
              mRevisionRootPage.createNameKey(mState, prefix),
              value);

      // Change existing first child node.
      if (mCurrentNode.getRightSiblingKey() != IConstants.NULL_KEY) {
        final Node rightSiblingNode =
            mRevisionRootPage.prepareNode(mState, mCurrentNode
                .getRightSiblingKey());
        rightSiblingNode.setLeftSiblingKey(mCurrentNode.getNodeKey());
      }

      // Change parent node.
      final Node parentNode =
          mRevisionRootPage.prepareNode(mState, mCurrentNode.getParentKey());
      parentNode.setFirstChildKey(mCurrentNode.getNodeKey());
      parentNode.incrementChildCount();

      // Insert new node as first child.
    } else {

      // Create new first child node.
      mCurrentNode =
          mRevisionRootPage.createNode(
              mState,
              mCurrentNode.getNodeKey(),
              IConstants.NULL_KEY,
              IConstants.NULL_KEY,
              IConstants.NULL_KEY,
              kind,
              mRevisionRootPage.createNameKey(mState, localPart),
              mRevisionRootPage.createNameKey(mState, uri),
              mRevisionRootPage.createNameKey(mState, prefix),
              value);

      // Change parent node.
      final Node parentNode =
          mRevisionRootPage.prepareNode(mState, mCurrentNode.getParentKey());
      parentNode.setFirstChildKey(mCurrentNode.getNodeKey());
      parentNode.incrementChildCount();

    }

    return mCurrentNode.getNodeKey();
  }

  /**
   * {@inheritDoc}
   */
  public final long insertRightSibling(
      final int kind,
      final String localPart,
      final String uri,
      final String prefix,
      final byte[] value) throws Exception {

    assertIsSelected();

    if (mCurrentNode.getNodeKey() == IConstants.ROOT_KEY) {
      throw new IllegalStateException("Root node can not have siblings.");
    }

    // Create new right sibling node.
    mCurrentNode =
        mRevisionRootPage.createNode(
            mState,
            mCurrentNode.getParentKey(),
            IConstants.NULL_KEY,
            mCurrentNode.getNodeKey(),
            mCurrentNode.getRightSiblingKey(),
            kind,
            mRevisionRootPage.createNameKey(mState, localPart),
            mRevisionRootPage.createNameKey(mState, uri),
            mRevisionRootPage.createNameKey(mState, prefix),
            value);

    // Adapt parent node.
    final Node parentNode =
        mRevisionRootPage.prepareNode(mState, mCurrentNode.getParentKey());
    parentNode.incrementChildCount();

    // Adapt left sibling node.
    final Node leftSiblingNode =
        mRevisionRootPage.prepareNode(mState, mCurrentNode.getLeftSiblingKey());
    leftSiblingNode.setRightSiblingKey(mCurrentNode.getNodeKey());

    // Adapt right sibling node.
    if (mCurrentNode.getRightSiblingKey() != IConstants.NULL_KEY) {
      final Node rightSiblingNode =
          mRevisionRootPage.prepareNode(mState, mCurrentNode
              .getRightSiblingKey());
      rightSiblingNode.setLeftSiblingKey(mCurrentNode.getNodeKey());
    }

    return mCurrentNode.getNodeKey();
  }

  /**
   * {@inheritDoc}
   */
  public final void insertAttribute(
      final String localPart,
      final String uri,
      final String prefix,
      final byte[] value) throws Exception {
    assertIsSelected();
    mCurrentNode =
        mRevisionRootPage.prepareNode(mState, mCurrentNode.getNodeKey());
    ((Node) mCurrentNode).insertAttribute(
        mRevisionRootPage.createNameKey(mState, localPart),
        mRevisionRootPage.createNameKey(mState, uri),
        mRevisionRootPage.createNameKey(mState, prefix),
        value);
  }

  /**
   * {@inheritDoc}
   */
  public final void insertNamespace(final String uri, final String prefix)
      throws Exception {
    assertIsSelected();
    mCurrentNode =
        mRevisionRootPage.prepareNode(mState, mCurrentNode.getNodeKey());
    ((Node) mCurrentNode).insertNamespace(mRevisionRootPage.createNameKey(
        mState,
        uri), mRevisionRootPage.createNameKey(mState, prefix));
  }

  /**
   * {@inheritDoc}
   */
  public final void remove() throws Exception {

    assertIsSelected();

    if (mCurrentNode.getChildCount() > 0) {
      throw new IllegalStateException("INode "
          + mCurrentNode.getNodeKey()
          + " has "
          + mCurrentNode.getChildCount()
          + " child(ren) and can not be removed.");
    }

    if (mCurrentNode.getNodeKey() == IConstants.ROOT_KEY) {
      throw new IllegalStateException("Root node can not be removed.");
    }

    // Remember left and right sibling keys.
    final long parentKey = mCurrentNode.getParentKey();
    final long nodeKey = mCurrentNode.getNodeKey();
    final long leftSiblingNodeKey = mCurrentNode.getLeftSiblingKey();
    final long rightSiblingNodeKey = mCurrentNode.getRightSiblingKey();

    // Remove old node.
    mRevisionRootPage.removeNode(mState, nodeKey);

    // Get and adapt parent node.
    mCurrentNode = mRevisionRootPage.prepareNode(mState, parentKey);
    ((Node) mCurrentNode).decrementChildCount();
    ((Node) mCurrentNode).setFirstChildKey(rightSiblingNodeKey);

    // Adapt left sibling node if there is one.
    if (leftSiblingNodeKey != IConstants.NULL_KEY) {
      final Node leftSiblingNode =
          mRevisionRootPage.prepareNode(mState, leftSiblingNodeKey);
      leftSiblingNode.setRightSiblingKey(rightSiblingNodeKey);
    }

    // Adapt right sibling node if there is one.
    if (rightSiblingNodeKey != IConstants.NULL_KEY) {
      final Node rightSiblingNode =
          mRevisionRootPage.prepareNode(mState, rightSiblingNodeKey);
      rightSiblingNode.setLeftSiblingKey(leftSiblingNodeKey);
    }

  }

  /**
   * {@inheritDoc}
   */
  public final void setAttribute(
      final int index,
      final String localPart,
      final String uri,
      final String prefix,
      final byte[] value) throws Exception {
    assertIsSelected();
    mCurrentNode =
        mRevisionRootPage.prepareNode(mState, mCurrentNode.getNodeKey());
    ((Node) mCurrentNode).setAttribute(
        index,
        mRevisionRootPage.createNameKey(mState, localPart),
        mRevisionRootPage.createNameKey(mState, uri),
        mRevisionRootPage.createNameKey(mState, prefix),
        value);
  }

  /**
   * {@inheritDoc}
   */
  public final void setNamespace(
      final int index,
      final String uri,
      final String prefix) throws Exception {
    assertIsSelected();
    mCurrentNode =
        mRevisionRootPage.prepareNode(mState, mCurrentNode.getNodeKey());
    ((Node) mCurrentNode).setNamespace(index, mRevisionRootPage.createNameKey(
        mState,
        uri), mRevisionRootPage.createNameKey(mState, prefix));
  }

  /**
   * {@inheritDoc}
   */
  public final void setLocalPart(final String localPart) throws Exception {
    assertIsSelected();
    mCurrentNode =
        mRevisionRootPage.prepareNode(mState, mCurrentNode.getNodeKey());
    ((Node) mCurrentNode).setLocalPartKey(mRevisionRootPage.createNameKey(
        mState,
        localPart));
  }

  /**
   * {@inheritDoc}
   */
  public final void setURI(final String uri) throws Exception {
    assertIsSelected();
    mCurrentNode =
        mRevisionRootPage.prepareNode(mState, mCurrentNode.getNodeKey());
    ((Node) mCurrentNode).setURIKey(mRevisionRootPage
        .createNameKey(mState, uri));
  }

  /**
   * {@inheritDoc}
   */
  public void setPrefix(final String prefix) throws Exception {
    assertIsSelected();
    mCurrentNode =
        mRevisionRootPage.prepareNode(mState, mCurrentNode.getNodeKey());
    ((Node) mCurrentNode).setPrefixKey(mRevisionRootPage.createNameKey(
        mState,
        prefix));
  }

  /**
   * {@inheritDoc}
   */
  public final void setValue(final byte[] value) throws Exception {
    assertIsSelected();
    mCurrentNode =
        mRevisionRootPage.prepareNode(mState, mCurrentNode.getNodeKey());
    ((Node) mCurrentNode).setValue(value);
  }

}