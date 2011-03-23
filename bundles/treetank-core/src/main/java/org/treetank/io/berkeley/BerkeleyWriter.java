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

package org.treetank.io.berkeley;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.Transaction;

import org.slf4j.LoggerFactory;
import org.treetank.exception.TTIOException;
import org.treetank.io.IWriter;
import org.treetank.page.AbsPage;
import org.treetank.page.PageReference;
import org.treetank.utils.LogWrapper;

/**
 * This class represents an reading instance of the Treetank-Application
 * implementing the {@link IWriter}-interface. It inherits and overrides some
 * reader methods because of the transaction layer.
 * 
 * @author Sebastian Graf, University of Konstanz
 * 
 */
public class BerkeleyWriter implements IWriter {

    /**
     * Log wrapper for better output.
     */
    private static final LogWrapper LOGWRAPPER =
        new LogWrapper(LoggerFactory.getLogger(BerkeleyWriter.class));

    /** Current {@link Database} to write to. */
    private transient final Database mDatabase;

    /** Current {@link Transaction} to write with. */
    private transient Transaction mTxn;

    /** Current {@link BerkeleyReader} to read with. */
    private transient final BerkeleyReader mReader;

    private long mNodepagekey;

    /**
     * Simple constructor starting with an {@link Environment} and a {@link Database}.
     * 
     * @param mEnv
     *            for the write
     * @param mDatabase
     *            where the data should be written to
     * @throws TTIOException
     *             if something off happens
     */
    public BerkeleyWriter(final Environment mEnv, final Database mDatabase) throws TTIOException {

        try {
            this.mTxn = mEnv.beginTransaction(null, null);
            this.mDatabase = mDatabase;
            this.mNodepagekey = getLastNodePage();
        } catch (final DatabaseException exc) {
            LOGWRAPPER.error(exc);
            throw new TTIOException(exc);
        }

        mReader = new BerkeleyReader(mDatabase, mTxn);
    }

    /**
     * {@inheritDoc}
     */
    public void close() throws TTIOException {
        try {
            setLastNodePage(mNodepagekey);
            mTxn.commit();
        } catch (final DatabaseException exc) {
            LOGWRAPPER.error(exc);
            throw new TTIOException(exc);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void write(final PageReference pageReference) throws TTIOException {
        final AbsPage page = pageReference.getPage();

        final DatabaseEntry valueEntry = new DatabaseEntry();
        final DatabaseEntry keyEntry = new DatabaseEntry();

        // TODO make this better
        mNodepagekey++;
        final BerkeleyKey key = new BerkeleyKey(mNodepagekey);

        BerkeleyFactory.PAGE_VAL_B.objectToEntry(page, valueEntry);
        BerkeleyFactory.KEY.objectToEntry(key, keyEntry);

        try {
            final OperationStatus status = mDatabase.put(mTxn, keyEntry, valueEntry);
            if (status != OperationStatus.SUCCESS) {
                throw new DatabaseException(new StringBuilder("Write of ").append(pageReference.toString())
                    .append(" failed!").toString());
            }
        } catch (final DatabaseException exc) {
            LOGWRAPPER.error(exc);
            throw new TTIOException(exc);
        }
        pageReference.setKey(key);

    }

    /**
     * Setting the last nodePage to the persistent storage.
     * 
     * @throws TTIOException
     *             If can't set last Node page
     * @param mData
     *            key to be stored
     */
    private void setLastNodePage(final Long mData) throws TTIOException {
        final DatabaseEntry keyEntry = new DatabaseEntry();
        final DatabaseEntry valueEntry = new DatabaseEntry();

        final BerkeleyKey key = BerkeleyKey.getDataInfoKey();
        BerkeleyFactory.KEY.objectToEntry(key, keyEntry);
        BerkeleyFactory.DATAINFO_VAL_B.objectToEntry(mData, valueEntry);
        try {
            mDatabase.put(mTxn, keyEntry, valueEntry);

        } catch (final DatabaseException exc) {
            LOGWRAPPER.error(exc);
            throw new TTIOException(exc);
        }
    }

    /**
     * Getting the last nodePage from the persistent storage.
     * 
     * @throws TTIOException
     *             If can't get last Node page
     * @return the last nodepage-key
     */
    private long getLastNodePage() throws TTIOException {
        final DatabaseEntry keyEntry = new DatabaseEntry();
        final DatabaseEntry valueEntry = new DatabaseEntry();

        final BerkeleyKey key = BerkeleyKey.getDataInfoKey();
        BerkeleyFactory.KEY.objectToEntry(key, keyEntry);

        try {
            final OperationStatus status = mDatabase.get(mTxn, keyEntry, valueEntry, LockMode.DEFAULT);
            Long val;
            if (status == OperationStatus.SUCCESS) {
                val = BerkeleyFactory.DATAINFO_VAL_B.entryToObject(valueEntry);
            } else {
                val = 0L;
            }
            return val;
        } catch (final DatabaseException exc) {
            LOGWRAPPER.error(exc);
            throw new TTIOException(exc);
        }

    }

    /**
     * {@inheritDoc}
     */
    public void writeFirstReference(final PageReference pageReference) throws TTIOException {
        write(pageReference);

        final DatabaseEntry keyEntry = new DatabaseEntry();
        BerkeleyFactory.KEY.objectToEntry(BerkeleyKey.getFirstRevKey(), keyEntry);

        final DatabaseEntry valueEntry = new DatabaseEntry();
        BerkeleyFactory.FIRST_REV_VAL_B.objectToEntry(pageReference, valueEntry);

        try {
            mDatabase.put(mTxn, keyEntry, valueEntry);
        } catch (final DatabaseException exc) {
            LOGWRAPPER.error(exc);
            throw new TTIOException(exc);
        }

    }

    /**
     * {@inheritDoc}
     */
    public AbsPage read(final PageReference pageReference) throws TTIOException {
        return mReader.read(pageReference);
    }

    /**
     * {@inheritDoc}
     */
    public PageReference readFirstReference() throws TTIOException {
        return mReader.readFirstReference();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((mDatabase == null) ? 0 : mDatabase.hashCode());
        result = prime * result + ((mTxn == null) ? 0 : mTxn.hashCode());
        result = prime * result + ((mReader == null) ? 0 : mReader.hashCode());
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object mObj) {
        boolean returnVal = true;
        if (mObj == null) {
            returnVal = false;
        } else if (getClass() != mObj.getClass()) {
            returnVal = false;
        }
        final BerkeleyWriter other = (BerkeleyWriter)mObj;
        if (mDatabase == null) {
            if (other.mDatabase != null) {
                returnVal = false;
            }
        } else if (!mDatabase.equals(other.mDatabase)) {
            returnVal = false;
        }
        if (mTxn == null) {
            if (other.mTxn != null) {
                returnVal = false;
            }
        } else if (!mTxn.equals(other.mTxn)) {
            returnVal = false;
        }
        if (mReader == null) {
            if (other.mReader != null) {
                returnVal = false;
            }
        } else if (!mReader.equals(other.mReader)) {
            returnVal = false;
        }
        return returnVal;
    }

}