/**
 * Copyright (c) 2011, University of Konstanz, Distributed Systems Group
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the University of Konstanz nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
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

package org.treetank.gui.view.sunburst;

import org.treetank.api.IItem;
import org.treetank.diff.DiffDepth;
import org.treetank.diff.DiffFactory.EDiff;

/**
 * Container for diffs.
 * 
 * @author Johannes Lichtenberger, University of Konstanz
 * 
 */
class Diff {
    /** {@link EDiff} which specifies the kind of diff between two nodes. */
    private transient EDiff mDiff;

    /** {@link IItem} in new revision. */
    private transient IItem mNewNode;

    /** {@link IItem} in old revision. */
    private transient IItem mOldNode;

    /** {@link DiffDepth} instance. */
    private transient DiffDepth mDepth;

    /**
     * Constructor.
     * 
     * @param paramDiff
     *            {@link EDiff} which specifies the kind of diff between two nodes
     * @param paramNewNode
     *            {@link IItem} in new revision
     * @param paramOldNode
     *            {@link IItem} in old revision
     * @param paramDepth
     *            current {@link Depth}
     */
    public Diff(final EDiff paramDiff, final IItem paramNewNode, final IItem paramOldNode,
        final DiffDepth paramDepth) {
        assert paramDiff != null;
        assert paramNewNode != null;
        assert paramOldNode != null;

        mDiff = paramDiff;
        mNewNode = paramNewNode;
        mOldNode = paramOldNode;
        mDepth = paramDepth;
    }

    /**
     * Get diff.
     * 
     * @return the kind of diff
     */
    EDiff getDiff() {
        return mDiff;
    }

    /**
     * Get new node.
     * 
     * @return the new node
     */
    IItem getNewNode() {
        return mNewNode;
    }

    /**
     * Get old node.
     * 
     * @return the old node
     */
    IItem getOldNode() {
        return mOldNode;
    }

    /**
     * Get depth.
     * 
     * @return the depth
     */
    DiffDepth getDepth() {
        return mDepth;
    }

    @Override
    public String toString() {
        return new StringBuilder("diff: ").append(mDiff).append(" new node: ").append(mNewNode).append(
            " old node: ").append(mOldNode).toString();
    }
}