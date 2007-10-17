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
 * $Id$
 */

package org.treetank.pagelayer;

import org.treetank.api.IConstants;
import org.treetank.api.IPage;
import org.treetank.api.IWriteTransactionState;
import org.treetank.utils.FastByteArrayReader;
import org.treetank.utils.FastByteArrayWriter;

final public class IndirectPage extends AbstractPage implements IPage {

  private final PageReference[] mIndirectPageReferences;

  /**
   * Constructor to assure minimal common setup.
   * 
   * @param pageCache IPageCache to read from.
   */
  private IndirectPage() {
    mIndirectPageReferences = new PageReference[IConstants.INP_REFERENCE_COUNT];
  }

  /**
   * Create new uncommitted in-memory uber page.
   * 
   * @param pageCache
   * @return
   * @throws Exception
   */
  public static final IndirectPage create() {
    final IndirectPage indirectPage = new IndirectPage();
    createPageReferences(indirectPage.mIndirectPageReferences);
    indirectPage.setDirty(true);
    return indirectPage;
  }

  /**
   * Read committed uber page from disk.
   * 
   * @param pageCache
   * @param in
   * @param pageKind
   * @throws Exception
   */
  public static final IndirectPage read(final FastByteArrayReader in)
      throws Exception {
    final IndirectPage indirectPage = new IndirectPage();
    readPageReferences(indirectPage.mIndirectPageReferences, in);
    indirectPage.setDirty(false);
    return indirectPage;
  }

  public static final IndirectPage clone(
      final IndirectPage committedIndirectPage) {
    final IndirectPage indirectPage = new IndirectPage();
    clonePageReferences(
        indirectPage.mIndirectPageReferences,
        committedIndirectPage.mIndirectPageReferences);
    indirectPage.setDirty(false);
    return indirectPage;
  }

  /**
   * Get reference by page offset.
   * 
   * @param offset Offset of referenced page.
   * @return Reference at given offset.
   */
  public final PageReference getPageReference(final int offset) {
    return mIndirectPageReferences[offset];
  }

  /**
   * Set page reference at page offset.
   * 
   * @param offset Offset of referenced page.
   * @param reference Page reference to set at pageOffset.
   */
  public final void setPageReference(
      final int offset,
      final PageReference reference) {
    mIndirectPageReferences[offset] = reference;
    setDirty(true);
  }

  /**
   * {@inheritDoc}
   */
  public final void commit(final IWriteTransactionState state) throws Exception {
    state.commit(mIndirectPageReferences);
    setDirty(false);
  }

  /**
   * {@inheritDoc}
   */
  public final void serialize(final FastByteArrayWriter out) throws Exception {
    serialize(out, mIndirectPageReferences);
  }

}
