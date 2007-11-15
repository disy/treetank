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

package org.treetank.axislayer;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Before;
import org.junit.Test;
import org.treetank.api.IAxis;
import org.treetank.api.ISession;
import org.treetank.api.IWriteTransaction;
import org.treetank.sessionlayer.Session;
import org.treetank.utils.TestDocument;

public class AncestorAxisTest {

  public static final String PATH =
      "generated" + File.separator + "AncestorAxisTest.tnk";

  @Before
  public void setUp() {
    Session.removeSession(PATH);
  }

  @Test
  public void testIterate() {

    // Build simple test tree.
    final ISession session = Session.beginSession(PATH);
    final IWriteTransaction wtx = session.beginWriteTransaction();
    TestDocument.create(wtx);

    // Find ancestors starting from nodeKey 0L (root).
    wtx.moveTo(9L);
    final IAxis axis1 = new AncestorAxis(wtx);
    assertEquals(true, axis1.hasNext());
    assertEquals(8L, axis1.next());

    assertEquals(true, axis1.hasNext());
    assertEquals(2L, axis1.next());

    assertEquals(false, axis1.hasNext());

    // Find ancestors starting from nodeKey 1L (first child of root).
    wtx.moveTo(4L);
    final IAxis axis2 = new AncestorAxis(wtx);
    assertEquals(true, axis2.hasNext());
    assertEquals(2L, axis2.next());

    assertEquals(false, axis2.hasNext());

    // Find ancestors starting from nodeKey 4L (second child of root).
    wtx.moveTo(3L);
    final IAxis axis3 = new AncestorAxis(wtx);
    assertEquals(true, axis3.hasNext());
    assertEquals(2L, axis3.next());

    assertEquals(false, axis3.hasNext());

    // Find ancestors starting from nodeKey 5L (last in document order).
    wtx.moveTo(2L);
    final IAxis axis4 = new AncestorAxis(wtx);
    assertEquals(false, axis4.hasNext());

    wtx.abort();
    wtx.close();
    session.close();

  }

}
