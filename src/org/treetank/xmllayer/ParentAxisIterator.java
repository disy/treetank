/*
 * TreeTank - Embedded Native XML Database
 * 
 * Copyright (C) 2007 Marc Kramis
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
 * $Id$
 */

package org.treetank.xmllayer;

import org.treetank.api.IAxisIterator;
import org.treetank.api.IReadTransaction;

/**
 * <h1>ParentAxisIteratorTest</h1>
 * 
 * <p>
 * Iterate to parent node starting at a given
 * node. Self is not included.
 * </p>
 */
public class ParentAxisIterator implements IAxisIterator {

  /** Exclusive (immutable) trx to iterate with. */
  private final IReadTransaction trx;

  /** The nodeKey of the next node to visit. */
  private long nextKey;

  /** Track number of calls of next. */
  private boolean isFirstNext;

  /**
   * Constructor initializing internal state.
   * 
   * @param initTrx Exclusive (immutable) trx to iterate with.
   * @throws Exception of any kind.
   */
  public ParentAxisIterator(final IReadTransaction initTrx) throws Exception {
    trx = initTrx;
    isFirstNext = true;
    nextKey = trx.getParentKey();
  }

  /**
   * {@inheritDoc}
   */
  public final boolean next() throws Exception {
    if (isFirstNext && trx.moveTo(nextKey)) {
      isFirstNext = false;
      return true;
    } else {
      return false;
    }
  }

}
