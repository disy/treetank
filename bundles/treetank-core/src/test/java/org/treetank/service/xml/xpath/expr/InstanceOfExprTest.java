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
 *     * Neither the name of the <organization> nor the
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

package org.treetank.service.xml.xpath.expr;


import org.treetank.TestHelper;
import org.treetank.TestHelper.PATHS;
import org.treetank.api.IDatabase;
import org.treetank.api.IReadTransaction;
import org.treetank.api.ISession;
import org.treetank.api.IWriteTransaction;
import org.treetank.axis.AbsAxis;
import org.treetank.exception.AbsTTException;
import org.treetank.service.xml.xpath.XPathAxis;
import org.treetank.utils.DocumentCreater;
import org.treetank.utils.TypedValue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * JUnit-test class to test the functionality of the InstanceOfExpr.
 * 
 * @author Tina Scherer
 */
public class InstanceOfExprTest {
    @Before
    public void setUp() throws AbsTTException {

        TestHelper.deleteEverything();
    }

    @After
    public void tearDown() throws AbsTTException {
        TestHelper.closeEverything();
    }

    @Test
    public void testInstanceOfExpr() throws AbsTTException {
        // Build simple test tree.
        final IDatabase database = TestHelper.getDatabase(PATHS.PATH1.getFile());
        final ISession session = database.getSession();
        final IWriteTransaction wtx = session.beginWriteTransaction();
        DocumentCreater.create(wtx);
        wtx.commit();
        IReadTransaction rtx = session.beginReadTransaction();

        final AbsAxis axis1 = new XPathAxis(rtx, "1 instance of xs:integer");
        assertEquals(true, axis1.hasNext());
        assertEquals(rtx.keyForName("xs:boolean"), rtx.getNode().getTypeKey());
        assertEquals(true, Boolean.parseBoolean(TypedValue.parseString((rtx.getNode().getRawValue()))));
        assertEquals(false, axis1.hasNext());

        final AbsAxis axis2 = new XPathAxis(rtx, "\"hallo\" instance of xs:integer");
        assertEquals(true, axis2.hasNext());
        assertEquals(rtx.keyForName("xs:boolean"), rtx.getNode().getTypeKey());
        assertEquals(false, Boolean.parseBoolean(TypedValue.parseString((rtx.getNode().getRawValue()))));
        assertEquals(false, axis2.hasNext());

        final AbsAxis axis3 = new XPathAxis(rtx, "\"hallo\" instance of xs:string ?");
        assertEquals(true, axis3.hasNext());
        assertEquals(rtx.keyForName("xs:boolean"), rtx.getNode().getTypeKey());
        assertEquals(true, Boolean.parseBoolean(TypedValue.parseString((rtx.getNode().getRawValue()))));
        assertEquals(false, axis3.hasNext());

        final AbsAxis axis4 = new XPathAxis(rtx, "\"hallo\" instance of xs:string +");
        assertEquals(true, axis4.hasNext());
        assertEquals(rtx.keyForName("xs:boolean"), rtx.getNode().getTypeKey());
        assertEquals(true, Boolean.parseBoolean(TypedValue.parseString((rtx.getNode().getRawValue()))));
        assertEquals(false, axis4.hasNext());

        final AbsAxis axis5 = new XPathAxis(rtx, "\"hallo\" instance of xs:string *");
        assertEquals(true, axis5.hasNext());
        assertEquals(rtx.keyForName("xs:boolean"), rtx.getNode().getTypeKey());
        assertEquals(true, Boolean.parseBoolean(TypedValue.parseString((rtx.getNode().getRawValue()))));
        assertEquals(false, axis5.hasNext());

        rtx.close();
        wtx.close();
        session.close();
        database.close();
    }

}
