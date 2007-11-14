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

import org.treetank.utils.FastByteArrayReader;
import org.treetank.utils.FastByteArrayWriter;

/**
 * <h1>PageReference</h1>
 * 
 * <p>
 * Page reference pointing to a page. This might be on stable storage
 * pointing to the start byte in a file, including the length in bytes, and
 * the checksum of the serialized page. Or it might be an immediate reference
 * to an in-memory instance of the deserialized page.
 * </p>
 *
 * @param <T>
 */
public final class PageReference<T extends AbstractPage> {

  /** In-memory deserialized page instance. */
  private T mPage;

  /** Start byte in file. */
  private long mStart;

  /** Length of serialized page in bytes. */
  private int mLength;

  /** Checksum of serialized page. */
  private long mChecksum;

  /**
   * Default constructor setting up an uninitialized page reference.
   */
  public PageReference() {
    this(null, -1L, -1, -1L);
  }

  /**
   * Constructor to clone an existing page reference.
   * 
   * @param pageReference Page reference to clone.
   */
  public PageReference(final PageReference<T> pageReference) {
    this(
        pageReference.mPage,
        pageReference.mStart,
        pageReference.mLength,
        pageReference.mChecksum);
  }

  /**
   * Constructor to properly set up a page reference.
   * 
   * @param page In-memory deserialized page instance.
   * @param start Start byte of serialized page.
   * @param length Length of serialized page in bytes.
   * @param checksum Checksum of serialized page.
   */
  public PageReference(
      final T page,
      final long start,
      final int length,
      final long checksum) {
    mPage = page;
    mStart = start;
    mLength = length;
    mChecksum = checksum;
  }

  /**
   * Read page reference from storage.
   * 
   * @param in Input bytes.
   */
  public PageReference(final FastByteArrayReader in) {
    //    this(null, in.readVarLong(), in.readVarInt(), in.readVarLong());
    this(null, in.readLong(), in.readInt(), in.readLong());
  }

  /**
   * Is there an instantiated page?
   * 
   * @return True if the reference points to an in-memory instance.
   */
  public final boolean isInstantiated() {
    return (mPage != null);
  }

  /**
   * Was the referenced page ever committed?
   * 
   * @return True if the page was committed.
   */
  public final boolean isCommitted() {
    return (mStart != -1L);
  }

  /**
   * Is the in-memory page dirty?
   * 
   * @return True if the page is dirty.
   * @throws IllegalStateException of there is no in-memory instance.
   */
  public final boolean isDirty() {
    if (mPage != null) {
      return mPage.isDirty();
    } else {
      throw new IllegalStateException("Page is not instantiated.");
    }
  }

  /**
   * Get the checksum of the serialized page.
   * 
   * @return Checksum of serialized page.
   */
  public final long getChecksum() {
    return mChecksum;
  }

  /**
   * Set the checksum of the serialized page.
   * 
   * @param checksum Checksum of serialized page.
   */
  public final void setChecksum(final long checksum) {
    mChecksum = checksum;
  }

  /**
   * Get in-memory instance of deserialized page.
   * 
   * @return In-memory instance of deserialized page.
   */
  public final T getPage() {
    return mPage;
  }

  /**
   * Set in-memory instance of deserialized page.
   * 
   * @param page Deserialized page.
   */
  public final void setPage(final T page) {
    mPage = page;
  }

  /**
   * Get the length of the serialized page in bytes.
   * 
   * @return Length of serialized page in bytes
   */
  public final int getLength() {
    return mLength;
  }

  /**
   * Set the length of the serialized page in bytes.
   * 
   * @param length Length of serialized page in bytes.
   */
  public final void setLength(final int length) {
    mLength = length;
  }

  /**
   * Get start byte offset in file.
   * 
   * @return Start offset in file.
   */
  public final long getStart() {
    return mStart;
  }

  /**
   * Set start byte offset in file.
   * 
   * @param start Start byte offset in file.
   */
  public final void setStart(final long start) {
    mStart = start;
  }

  /**
   * Serialize page reference to output.
   * 
   * @param out Output bytes that get written to a file.
   */
  public final void serialize(final FastByteArrayWriter out) {
    //    out.writeVarLong(mStart);
    //    out.writeVarInt(mLength);
    //    out.writeVarLong(mChecksum);
    out.writeLong(mStart);
    out.writeInt(mLength);
    out.writeLong(mChecksum);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final boolean equals(final Object object) {
    if (!(object instanceof PageReference)) {
      return false;
    }
    final PageReference<T> pageReference = (PageReference<T>) object;
    return ((mChecksum == pageReference.mChecksum)
        && (mStart == pageReference.mStart) && (mLength == pageReference.mLength));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final String toString() {
    return super.toString()
        + ": start="
        + mStart
        + ", length="
        + mLength
        + ", checksum="
        + mChecksum
        + ", page=("
        + mPage
        + ")";
  }

}
