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

package org.treetank.service.xml.xpath.parser;


import org.treetank.TestHelper;
import org.treetank.TestHelper.PATHS;
import org.treetank.api.IDatabase;
import org.treetank.api.IReadTransaction;
import org.treetank.api.ISession;
import org.treetank.axis.AbsAxis;
import org.treetank.exception.AbsTTException;
import org.treetank.service.xml.xpath.XPathAxis;
import org.treetank.service.xml.xpath.parser.XPathParser;
import org.treetank.utils.TypedValue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class XPathParserTest {

    private XPathParser parser;

    @Before
    public void setUp() throws AbsTTException {
        TestHelper.deleteEverything();
    }

    @After
    public void tearDown() throws AbsTTException {
        TestHelper.closeEverything();
    }

    @Test
    public void testLiterals() throws AbsTTException {

        // Build simple test tree.
        final IDatabase database = TestHelper.getDatabase(PATHS.PATH1.getFile());
        final ISession session = database.getSession();
        final IReadTransaction rtx = session.beginReadTransaction();

        rtx.moveTo(2L);

        AbsAxis axis;

        axis = new XPathAxis(rtx, "\"12.5\"");
        assertEquals(true, axis.hasNext());
        assertEquals("12.5", TypedValue.parseString(rtx.getNode().getRawValue()));
        assertEquals(rtx.keyForName("xs:string"), rtx.getNode().getTypeKey());
        assertEquals(false, axis.hasNext());

        axis = new XPathAxis(rtx, "\"He said, \"\"I don't like it\"\"\"");
        assertEquals(true, axis.hasNext());
        assertEquals("He said, I don't like it", TypedValue.parseString(rtx.getNode().getRawValue()));
        assertEquals(rtx.keyForName("xs:string"), rtx.getNode().getTypeKey());
        assertEquals(false, axis.hasNext());

        axis = new XPathAxis(rtx, "12");
        assertEquals(true, axis.hasNext());
        assertEquals(rtx.keyForName("xs:integer"), rtx.getNode().getTypeKey());
        assertEquals("12", TypedValue.parseString(rtx.getNode().getRawValue()));
        assertEquals(false, axis.hasNext());

        axis = new XPathAxis(rtx, "12.5");
        assertEquals(true, axis.hasNext());
        assertEquals(rtx.keyForName("xs:decimal"), rtx.getNode().getTypeKey());
        assertEquals("12.5", TypedValue.parseString(rtx.getNode().getRawValue()));
        assertEquals(false, axis.hasNext());

        axis = new XPathAxis(rtx, "12.5E2");
        assertEquals(true, axis.hasNext());
        assertEquals(rtx.keyForName("xs:double"), rtx.getNode().getTypeKey());
        assertEquals("12.5E2", TypedValue.parseString(rtx.getNode().getRawValue()));
        assertEquals(false, axis.hasNext());

        axis = new XPathAxis(rtx, "1");
        assertEquals(true, axis.hasNext());
        assertEquals("1", TypedValue.parseString(rtx.getNode().getRawValue()));
        assertEquals(rtx.keyForName("xs:integer"), rtx.getNode().getTypeKey());
        assertEquals(false, axis.hasNext());

        rtx.close();
        session.close();
        database.close();
    }

    @Test
    public void testEBNF() throws AbsTTException {

        // Build simple test tree.
        final IDatabase database = TestHelper.getDatabase(PATHS.PATH1.getFile());
        final ISession session = database.getSession();
        final IReadTransaction rtx = session.beginReadTransaction();

        parser = new XPathParser(rtx, "/p:a");
        parser.parseQuery();

        parser = new XPathParser(rtx, "/p:a/node(), /b/descendant-or-self::adsfj");
        parser.parseQuery();

        parser = new XPathParser(rtx, "for $i in /p:a return $i");
        parser.parseQuery();

        parser = new XPathParser(rtx, "for $i in /p:a return /p:a");
        parser.parseQuery();

        parser = new XPathParser(rtx, "child::element(person)");
        parser.parseQuery();

        parser = new XPathParser(rtx, "child::element(person, xs:string)");
        parser.parseQuery();

        parser = new XPathParser(rtx, " child::element(*, xs:string)");
        parser.parseQuery();

        parser = new XPathParser(rtx, "child::element()");
        parser.parseQuery();

        // parser = new XPathParser(rtx, ". treat as item()");
        // parser.parseQuery();

        parser = new XPathParser(rtx, "/b instance of item()");
        parser.parseQuery();

        rtx.close();
        session.close();
        database.close();

    }

}