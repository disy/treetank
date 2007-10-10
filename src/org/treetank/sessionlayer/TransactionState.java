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
 * $Id:SessionConfiguration.java 3019 2007-10-10 13:28:24Z kramis $
 */

package org.treetank.sessionlayer;

import org.treetank.pagelayer.PageCache;
import org.treetank.pagelayer.PageReader;

public final class TransactionState {

  private final PageCache mPageCache;

  private final PageReader mPageReader;

  public TransactionState(final PageCache pageCache, final PageReader pageReader) {
    mPageCache = pageCache;
    mPageReader = pageReader;
  }

  public final PageCache getPageCache() {
    return mPageCache;
  }

  public final PageReader getPageReader() {
    return mPageReader;
  }

}