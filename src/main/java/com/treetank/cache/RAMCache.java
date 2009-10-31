/*
 * Copyright (c) 2009, Sebastian Graf (Ph.D. Thesis), University of Konstanz
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
 */
package com.treetank.cache;

import com.treetank.page.NodePage;
import com.treetank.utils.FastWeakHashMap;

/**
 * Simple RAM implementation with the help of a {@link FastWeakHashMap}.
 * 
 * @author Sebastian Graf, University of Konstanz
 * 
 */
public final class RAMCache implements ICache {

    /**
     * local instance
     */
    private final transient FastWeakHashMap<Long, NodePage> map;

    /**
     * Simple constructor
     */
    public RAMCache() {
        super();
        map = new FastWeakHashMap<Long, NodePage>();
    }

    /**
     * {@inheritDoc}
     */
    public void clear() {
        map.clear();
    }

    /**
     * {@inheritDoc}
     */
    public NodePage get(final long key) {
        return map.get(key);
    }

    /**
     * {@inheritDoc}
     */
    public void put(final long key, final NodePage page) {
        map.put(key, page);
    }

}