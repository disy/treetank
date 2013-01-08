/**
 * Copyright (c) 2011, University of Konstanz, Distributed Systems Group
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * * Neither the name of the University of Konstanz nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.treetank.api;

import org.treetank.api.INode;
import org.treetank.exception.TTException;
import org.treetank.exception.TTIOException;


public interface IIscsiWriteTrx extends IIscsiReadTrx{

  /**
   * This method inserts the given node into the database.
   * @param node
   * @throws TTException
   */
  public void insert(INode node) throws TTException;

  /**
   * This method inserts the given node, after
   * the currently selected node and increments all
   * following indizes.
   * @param node
   * @throws TTException
   */
  public void insertAfter(INode node) throws TTException;

  /**
   * Remove currently selected node.
   * 
   * @throws TTException
   *             if node couldn't be removed
   */
  public void remove() throws TTException;
  
  /**
   * Set value of node.
   * 
   * @param pValue
   *            new value of node
   * @throws TTIOException
   *             if value couldn't be set
   */
  public void setValue(final byte[] pValue) throws TTException;

  /**
   * Commit all modifications of the exclusive write transaction. Even commit
   * if there are no modification at all.
   * 
   * @throws TTException
   *             if this revision couldn't be commited
   */
  public void commit() throws TTException;

  /**
   * Abort all modifications of the exclusive write transaction.
   * 
   * @throws TTIOException
   *             if this revision couldn't be aborted
   */
  public void abort() throws TTException;
  
  /**
   * 
   * @return the maximum node key in the list
   */
  public long getMaxNodeKey();
}