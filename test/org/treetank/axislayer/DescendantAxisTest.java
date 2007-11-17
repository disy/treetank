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

import java.io.File;

import org.junit.Before;
import org.junit.Test;
import org.treetank.api.ISession;
import org.treetank.api.IWriteTransaction;
import org.treetank.sessionlayer.Session;
import org.treetank.utils.TestDocument;

public class DescendantAxisTest {

  public static final String PATH =
      "generated" + File.separator + "DescendantAxisTest.tnk";

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

    wtx.moveToDocumentRoot();
    AbstractAxisTest.testAxisConventions(new DescendantAxis(wtx), new long[] {
        2L,
        3L,
        4L,
        5L,
        6L,
        7L,
        8L,
        9L,
        10L,
        11L });

    wtx.moveTo(2L);
    AbstractAxisTest.testAxisConventions(new DescendantAxis(wtx), new long[] {
        3L,
        4L,
        5L,
        6L,
        7L,
        8L,
        9L,
        10L,
        11L });

    wtx.moveTo(8L);
    AbstractAxisTest.testAxisConventions(new DescendantAxis(wtx), new long[] {
        9L,
        10L });

    wtx.moveTo(11L);
    AbstractAxisTest
        .testAxisConventions(new DescendantAxis(wtx), new long[] {});

    wtx.abort();
    wtx.close();
    session.close();

  }

}
