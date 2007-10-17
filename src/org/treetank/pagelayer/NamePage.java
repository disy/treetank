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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.treetank.api.IPage;
import org.treetank.api.IWriteTransactionState;
import org.treetank.utils.FastByteArrayReader;
import org.treetank.utils.FastByteArrayWriter;
import org.treetank.utils.UTF;

final public class NamePage implements IPage {

  /** True if page was created or cloned. False if it was read or committed. */
  private boolean mDirty;
  
  /** Map the hash of a name to its name. */
  private final Map<Integer, String> mNameMap;

  private NamePage(final boolean dirty) {
    mDirty = dirty;
    mNameMap = new HashMap<Integer, String>();
  }

  public static final NamePage create() {
    final NamePage namePage = new NamePage(true);
    return namePage;
  }

  public static final NamePage read(final FastByteArrayReader in)
      throws Exception {

    final NamePage namePage = new NamePage(false);

    // Names (deep load).
    for (int i = 0, l = in.readVarInt(); i < l; i++) {
      namePage.mNameMap.put(in.readVarInt(), UTF.convert(in.readByteArray()));
    }

    return namePage;

  }

  public static final NamePage clone(final NamePage committedNamePage) {

    final NamePage namePage = new NamePage(true);

    // Names (deep COW).
    namePage.mNameMap.putAll(committedNamePage.mNameMap);

    return namePage;
  }

  /**
   * Get name belonging to name key.
   * 
   * @param key Name key identifying name.
   * @return Name of name key.
   */
  public final String getName(final int key) {
    return mNameMap.get(key);
  }

  /**
   * Create name key given a name.
   * 
   * @param key Key for given name.
   * @param name Name to create key for.
   */
  public final void setName(final int key, final String name) {
    mNameMap.put(key, name);
  }

  /**
   * {@inheritDoc}
   */
  public final void commit(final IWriteTransactionState state) throws Exception {
    mDirty = false;
  }

  /**
   * {@inheritDoc}
   */
  public void serialize(final FastByteArrayWriter out) throws Exception {
    out.writeVarInt(mNameMap.size());
    Iterator<Integer> keyIterator = mNameMap.keySet().iterator();
    int key;
    while (keyIterator.hasNext()) {
      key = keyIterator.next();
      out.writeVarInt(key);
      out.writeByteArray(UTF.convert(mNameMap.get(key)));
    }
  }
  
  /**
   * {@inheritDoc}
   */
  public final boolean isDirty() {
    return mDirty;
  }

}
