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

package org.treetank.access;

import static org.testng.AssertJUnit.assertEquals;
import static org.treetank.data.IConstants.ROOT_NODE;

import java.util.Properties;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;
import org.treetank.access.conf.ResourceConfiguration;
import org.treetank.access.conf.ResourceConfiguration.IResourceConfigurationFactory;
import org.treetank.access.conf.StandardSettings;
import org.treetank.data.IConstants;
import org.treetank.data.interfaces.ITreeStructData;
import org.treetank.exception.TTException;
import org.treetank.testutil.CoreTestHelper;
import org.treetank.testutil.Holder;
import org.treetank.testutil.ModuleFactory;
import org.treetank.testutil.NodeElementTestHelper;

import com.google.inject.Inject;

@Guice(moduleFactory = ModuleFactory.class)
public class ReadTransactionTest {

    private Holder holder;

    @Inject
    private IResourceConfigurationFactory mResourceConfig;

    private ResourceConfiguration mResource;

    @BeforeMethod
    public void setUp() throws TTException {
        CoreTestHelper.deleteEverything();
        final CoreTestHelper.Holder holder = CoreTestHelper.Holder.generateStorage();
        Properties props =
            StandardSettings.getProps(CoreTestHelper.PATHS.PATH1.getFile().getAbsolutePath(),
                CoreTestHelper.RESOURCENAME);
        mResource = mResourceConfig.create(props);
        NodeElementTestHelper.createTestDocument(mResource);
        this.holder = Holder.generateWtx(holder, mResource);
    }

    @AfterMethod
    public void tearDown() throws TTException {
        holder.close();
        CoreTestHelper.deleteEverything();
    }

    @Test
    public void testDocumentRoot() throws TTException {
        assertEquals(true, holder.getNRtx().moveTo(ROOT_NODE));
        assertEquals(IConstants.ROOT, holder.getNRtx().getNode().getKind());
        assertEquals(false, holder.getNRtx().getNode().hasParent());
        assertEquals(false, ((ITreeStructData)holder.getNRtx().getNode()).hasLeftSibling());
        assertEquals(false, ((ITreeStructData)holder.getNRtx().getNode()).hasRightSibling());
        assertEquals(true, ((ITreeStructData)holder.getNRtx().getNode()).hasFirstChild());
        holder.getNRtx().close();
    }

    @Test
    public void testConventions() throws TTException {

        // INodeReadTrx Convention 1.
        assertEquals(true, holder.getNRtx().moveTo(ROOT_NODE));
        long key = holder.getNRtx().getNode().getDataKey();

        // INodeReadTrx Convention 2.
        assertEquals(holder.getNRtx().getNode().hasParent(), holder.getNRtx().moveTo(
            holder.getNRtx().getNode().getParentKey()));
        assertEquals(key, holder.getNRtx().getNode().getDataKey());

        assertEquals(((ITreeStructData)holder.getNRtx().getNode()).hasFirstChild(), holder.getNRtx().moveTo(
            ((ITreeStructData)holder.getNRtx().getNode()).getFirstChildKey()));
        assertEquals(1L, holder.getNRtx().getNode().getDataKey());

        assertEquals(((ITreeStructData)holder.getNRtx().getNode()).hasRightSibling(), holder.getNRtx().moveTo(
            ((ITreeStructData)holder.getNRtx().getNode()).getRightSiblingKey()));
        assertEquals(1L, holder.getNRtx().getNode().getDataKey());

        assertEquals(((ITreeStructData)holder.getNRtx().getNode()).hasFirstChild(), holder.getNRtx().moveTo(
            ((ITreeStructData)holder.getNRtx().getNode()).getFirstChildKey()));
        assertEquals(4L, holder.getNRtx().getNode().getDataKey());

        assertEquals(((ITreeStructData)holder.getNRtx().getNode()).hasRightSibling(), holder.getNRtx().moveTo(
            ((ITreeStructData)holder.getNRtx().getNode()).getRightSiblingKey()));
        assertEquals(5L, holder.getNRtx().getNode().getDataKey());

        assertEquals(((ITreeStructData)holder.getNRtx().getNode()).hasLeftSibling(), holder.getNRtx().moveTo(
            ((ITreeStructData)holder.getNRtx().getNode()).getLeftSiblingKey()));
        assertEquals(4L, holder.getNRtx().getNode().getDataKey());

        assertEquals(holder.getNRtx().getNode().hasParent(), holder.getNRtx().moveTo(
            holder.getNRtx().getNode().getParentKey()));
        assertEquals(1L, holder.getNRtx().getNode().getDataKey());

        holder.getNRtx().close();
    }

}
