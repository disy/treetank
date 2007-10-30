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
 * $Id: Node.java 3268 2007-10-25 13:16:01Z kramis $
 */

package org.treetank.nodelayer;

import org.treetank.api.IConstants;
import org.treetank.api.INode;
import org.treetank.api.IReadTransaction;
import org.treetank.utils.FastByteArrayReader;
import org.treetank.utils.FastByteArrayWriter;

/**
 * <h1>FullTextRootNode</h1>
 * 
 * <p>
 * Node representing a fulltext root node.
 * </p>
 */
public final class FullTextRootNode extends AbstractNode {

  /** Key of first child. */
  private long mFirstChildKey;

  /** Child count including element and text nodes. */
  private long mChildCount;

  /**
   * Constructor to create full text root node.
   * 
   * @param firstChildKey Key of first child.
   */
  public FullTextRootNode(final long firstChildKey) {
    super(IConstants.FULLTEXT_ROOT_KEY);
    mFirstChildKey = firstChildKey;
    mChildCount = 0;
  }

  /**
   * Create new fulltext root node.
   */
  public FullTextRootNode() {
    this(IConstants.NULL_KEY);
  }

  /**
   * Clone full text root node.
   * 
   * @param node Node to clone.
   */
  public FullTextRootNode(final INode node) {
    super(node.getNodeKey());
    mFirstChildKey = node.getFirstChildKey();
    mChildCount = node.getChildCount();
  }

  /**
   * Read full text root node.
   * 
   * @param in Byte input to read node from.
   */
  public FullTextRootNode(final FastByteArrayReader in) {
    super(IConstants.FULLTEXT_ROOT_KEY);
    mFirstChildKey = in.readVarLong();
    mChildCount = in.readVarLong();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final boolean isFullTextRoot() {
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final boolean hasFirstChild() {
    return (mFirstChildKey != IConstants.NULL_KEY);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final long getFirstChildKey() {
    return mFirstChildKey;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final INode getFirstChild(final IReadTransaction rtx) {
    return rtx.moveTo(mFirstChildKey);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final void setFirstChildKey(final long firstChildKey) {
    mFirstChildKey = firstChildKey;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final long getChildCount() {
    return mChildCount;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final void setChildCount(final long childCount) {
    mChildCount = childCount;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final void incrementChildCount() {
    mChildCount += 1;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final void decrementChildCount() {
    mChildCount -= 1;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final int getKind() {
    return IConstants.FULLTEXT_ROOT;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final void serialize(final FastByteArrayWriter out) {
    out.writeVarLong(mFirstChildKey);
    out.writeVarLong(mChildCount);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return "FullTextRootNode "
        + "\n\tnodeKey: "
        + getNodeKey()
        + "\n\tchildcount: "
        + mChildCount
        + "\n\tfirstChildKey: "
        + mFirstChildKey;
  }
}
