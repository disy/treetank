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

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.treetank.TestHelper;
import org.treetank.TestHelper.PATHS;
import org.treetank.api.IDatabase;
import org.treetank.api.IReadTransaction;
import org.treetank.api.ISession;
import org.treetank.api.IWriteTransaction;
import org.treetank.axis.AbsAxis;
import org.treetank.axis.DescendantAxis;
import org.treetank.exception.AbsTTException;
import org.treetank.utils.DocumentCreater;
import org.treetank.utils.TypedValue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ThreadTest {

    public static final int WORKER_COUNT = 50;

    @Before
    public void setUp() throws AbsTTException {
        TestHelper.deleteEverything();
    }

    @After
    public void tearDown() throws AbsTTException {
        TestHelper.closeEverything();
    }

    @Test
    public void testThreads() throws Exception {
        final IDatabase database = TestHelper.getDatabase(PATHS.PATH1.getFile());
        final ISession session = database.getSession(new SessionConfiguration.Builder().build());
        IWriteTransaction wtx = session.beginWriteTransaction();

        DocumentCreater.create(wtx);
        wtx.commit();
        wtx.close();

        ExecutorService taskExecutor = Executors.newFixedThreadPool(WORKER_COUNT);
        long newKey = 10L;
        for (int i = 0; i < WORKER_COUNT; i++) {
            taskExecutor.submit(new Task(session.beginReadTransaction(i)));
            wtx = session.beginWriteTransaction();
            wtx.moveTo(newKey);
            wtx.setValue("value" + i);
            newKey = wtx.getNode().getNodeKey();
            wtx.commit();
            wtx.close();
        }
        taskExecutor.shutdown();
        taskExecutor.awaitTermination(1000000, TimeUnit.SECONDS);

        session.close();
    }

    private class Task implements Callable<Void> {

        private IReadTransaction mRTX;

        public Task(final IReadTransaction rtx) {
            mRTX = rtx;
        }

        public Void call() throws Exception {
            final AbsAxis axis = new DescendantAxis(mRTX);
            while (axis.hasNext()) {
                axis.next();
            }

            mRTX.moveTo(12L);
            assertEquals("bar", TypedValue.parseString(mRTX.getNode().getRawValue()));
            mRTX.close();
            return null;
        }
    }

}
