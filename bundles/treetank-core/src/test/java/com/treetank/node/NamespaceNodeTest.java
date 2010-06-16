/*
 * Copyright (c) 2008, Marc Kramis (Ph.D. Thesis), University of Konstanz
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
 * $Id: NamespaceNodeTest.java 4413 2008-08-27 16:59:32Z kramis $
 */

package com.treetank.node;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.treetank.settings.ENodes;

public class NamespaceNodeTest {

	@Test
	public void testNamespaceNode() {

		// Create empty node.
		final AbstractNode node1 = new NamespaceNode(99L, 13L, 14, 15);


		// Clone node.
		final AbstractNode node2 = new NamespaceNode(node1);

		// Now compare.
		assertEquals(99L, node2.getNodeKey());
		assertEquals(13L, node2.getParentKey());

		assertEquals(14, node2.getURIKey());
		assertEquals(15, node2.getNameKey());
		assertEquals(null, node2.getRawValue());
		assertEquals(ENodes.NAMESPACE_KIND, node2.getKind());
		assertEquals(true, node2.hasParent());
		assertEquals(ENodes.NAMESPACE_KIND, node2.getKind());

	}

}