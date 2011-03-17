/**
 * Copyright (c) 2010, Distributed Systems Group, University of Konstanz
 * 
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 * 
 * THE SOFTWARE IS PROVIDED AS IS AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 * 
 */
package com.treetank.diff;

/**
 * Immutable diff container class.
 * 
 * @author Johannes Lichtenberger, University of Konstanz
 * 
 */
public final class DiffDepth {
    /** Depth in new revision. */
    private final int mNewDepth;

    /** Depth in old revision. */
    private final int mOldDepth;

    /**
     * Constructor.
     * 
     * @param paramNewDepth
     *            current depth in new revision
     * @param paramOldDepth
     *            current depth in old revision
     */
    DiffDepth(final int paramNewDepth, final int paramOldDepth) {
        assert paramNewDepth >= -1;
        assert paramOldDepth >= -1;
        mNewDepth = paramNewDepth;
        mOldDepth = paramOldDepth;
    }

    /**
     * Get depth in new revision.
     * 
     * @return depth in new revision
     */
    public int getNewDepth() {
        return mNewDepth;
    }

    /**
     * Get depth in old revision.
     * 
     * @return depth in old revision
     */
    public int getOldDepth() {
        return mOldDepth;
    }
}
