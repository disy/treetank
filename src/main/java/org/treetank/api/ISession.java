/*
 * Copyright (c) 2007, Marc Kramis
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
 * $Id$
 */

package org.treetank.api;

/**
 * <h1>ISession</h1>
 * 
 * <h2>Description</h2>
 * 
 * <p>
 * Each TreeTank file is bound to one instance implementing
 * <code>ISession</code>.
 * Transactions can then be started from this instance. There can only be one
 * <code>IWriteTransaction</code> at the time. However, multiple
 * <code>IReadTransactions</code> can coexist concurrently.
 * </p>
 * 
 * <h2>Convention</h2>
 * 
 * <h2>User Example</h2>
 * 
 * <p>
 *  <pre>
 *   // Simple session without encryption or end-to-end integrity.
 *   final ISession session = Session.beginSession("example.tnk");
 *   
 *   // Session with encryption and end-to-end integrity.
 *   final SessionConfiguration config = new SessionConfiguration(
 *       "example.tnk",
 *       "exampleKey......".getBytes(),
 *       true);
 *   final ISession session = Session.beginSession(config);
 *  </pre>
 * </p>
 * 
 * <h2>Developer Example</h2>
 */
public interface ISession {

  /**
   * Get file name of TreeTank file.
   * 
   * @return File name of TreeTank file.
   */
  public String getFileName();

  /**
   * Get absolute path to TreeTank file.
   * 
   * @return Absolute path to TreeTank file.
   */
  public String getAbsolutePath();

  /**
   * Tells whether the session is bound to an encrypted TreeTank file.
   * 
   * @return True if the TreeTank file is encrypted. False else.
   */
  public boolean isEncrypted();

  /**
   * Tells whether the session is bound to a checksummed TreeTank file.
   * 
   * @return True if the TreeTank file is checksummed. False else.
   */
  public boolean isChecksummed();

  /**
   * Get the major revision of the TreeTank version.
   * 
   * @return Major revision of TreeTank version.
   */
  public int getVersionMajor();

  /**
   * Get the minor revision of the TreeTank version.
   * 
   * @return Minor revision of TreeTank version.
   */
  public int getVersionMinor();

  /**
   * Begin a read-only transaction on the latest committed revision key.
   * 
   * @return IReadTransaction instance.
   */
  public IReadTransaction beginReadTransaction();

  /**
   * Begin a read-only transaction on the given revision key.
   * 
   * @param revisionKey Revision key to read from.
   * @return IReadTransaction instance.
   */
  public IReadTransaction beginReadTransaction(final long revisionKey);

  /**
   * Begin exclusive read/write transaction without auto commit.
   * 
   * @return IWriteTransaction instance.
   */
  public IWriteTransaction beginWriteTransaction();

  /**
   * Begin exclusive read/write transaction with auto commit.
   * 
   * @param maxNodeCount Count of node modifications after which a commit is
   *        issued.
   * @param maxTime Time in seconds after which a commit is issued.
   * @return IWriteTransaction instance.
   */
  public IWriteTransaction beginWriteTransaction(
      final int maxNodeCount,
      final int maxTime);
  
  /**
   * Get number of running read transactions.
   * 
   * @return Number of running read transactions.
   */
  public int getReadTransactionCount();
  
  /**
   * Get number of running write transactions.
   * 
   * @return Number of running write transactions.
   */
  public int getWriteTransactionCount();
  
  /**
   * Safely close session and immediately release all resources. If there are
   * running transactions, they will automatically be closed.
   * 
   * This is an idempotent operation and does nothing if the session is
   * already closed.
   */
  public void close();

}