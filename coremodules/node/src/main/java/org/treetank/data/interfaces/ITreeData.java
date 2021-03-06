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

package org.treetank.data.interfaces;

/**
 * <h1>IItem</h1>
 * <p>
 * Common interface for all item kinds. An item can be a node or an atomic value.
 * </p>
 */
public interface ITreeData extends org.treetank.api.IData {

    /**
     * Gets key of the context item's parent.
     * 
     * @return parent key
     */
    long getParentKey();

    /**
     * Declares, whether the item has a parent.
     * 
     * @return true, if item has a parent
     */
    boolean hasParent();

    /**
     * Gets the kind of the item (atomic value, element node, attribute
     * node....).
     * 
     * @return kind of item
     */
    int getKind();

    /**
     * Gets value type of the item.
     * 
     * @return value type
     */
    int getTypeKey();

    /**
     * Setting the parent key.
     * 
     * @param pNodeKey
     *            the parent to be set.
     */
    void setParentKey(long pNodeKey);

    /**
     * Setting the type key.
     * 
     * @param pTypeKey
     *            the type to be set.
     */
    void setTypeKey(int pTypeKey);

    /**
     * Getting the persisted hash value for this node.
     * 
     * @return the hash stored in this node
     */
    long getHash();

    /**
     * Setting the hash of this node including substructure.
     * 
     * @param pHash
     *            to be set
     */
    void setHash(long pHash);

}
