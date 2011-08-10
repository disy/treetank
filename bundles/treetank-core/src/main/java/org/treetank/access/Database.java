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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.treetank.access.conf.DatabaseConfiguration;
import org.treetank.access.conf.IConfigureSerializable;
import org.treetank.access.conf.ResourceConfiguration;
import org.treetank.access.conf.SessionConfiguration;
import org.treetank.api.IDatabase;
import org.treetank.api.ISession;
import org.treetank.exception.AbsTTException;
import org.treetank.exception.TTIOException;
import org.treetank.exception.TTUsageException;

/**
 * This class represents one concrete database for enabling several {@link ISession} objects.
 * 
 * @see IDatabase
 * @author Sebastian Graf, University of Konstanz
 */
public final class Database implements IDatabase {

    /** Central repository of all running sessions. */
    private static final ConcurrentMap<File, Database> DATABASEMAP = new ConcurrentHashMap<File, Database>();

    /** Central repository of all running sessions. */
    private static final Map<File, Session> SESSIONMAP = new ConcurrentHashMap<File, Session>();

    /** DatabaseConfiguration with fixed settings. */
    final DatabaseConfiguration mDBConfig;

    /**
     * Private constructor.
     * 
     * @param paramDBConf
     *            {@link ResourceConfiguration} reference to configure the {@link IDatabase}
     * @throws AbsTTException
     *             Exception if something weird happens
     */
    private Database(final DatabaseConfiguration paramDBConf) throws AbsTTException {
        mDBConfig = paramDBConf;

    }

    // //////////////////////////////////////////////////////////
    // START Creation/Deletion of Databases /////////////////////
    // //////////////////////////////////////////////////////////
    /**
     * Creating a database. This includes loading the database configurations,
     * building up the structure and preparing everything for login.
     * 
     * 
     * @param paramConf
     *            which are used for the database, including storage location
     * @return true if creation is valid, false otherwise
     * @throws TTIOException
     *             if something odd happens within the creation process.
     * @throws IOException
     */
    public static synchronized boolean createDatabase(final DatabaseConfiguration paramConfig)
        throws TTIOException {
        boolean returnVal = true;
        if (paramConfig.mFile.exists()) {
            return false;
        } else {
            returnVal = paramConfig.mFile.mkdirs();
            if (returnVal) {
                for (DatabaseConfiguration.Paths paths : DatabaseConfiguration.Paths.values()) {
                    final File toCreate = new File(paramConfig.mFile, paths.getFile().getName());
                    if (paths.isFolder()) {
                        returnVal = toCreate.mkdir();
                    } else {
                        try {
                            returnVal = toCreate.createNewFile();
                        } catch (final IOException exc) {
                            throw new TTIOException(exc);
                        }
                    }
                    if (!returnVal) {
                        break;
                    }
                }
            }

            try {
                serializeConfiguration(paramConfig);
            } catch (final IOException exc) {
                throw new TTIOException(exc);
            }
            // if something was not correct, delete the partly created
            // substructure
            if (!returnVal) {
                paramConfig.mFile.delete();
            }
            return returnVal;
        }
    }

    /**
     * Truncate a database. This deletes all relevant data. If there are
     * existing sessions against this database, the method returns null.
     * 
     * @param paramConfig
     *            the database at this path should be deleted.
     * @return true if removal is successful, false otherwise
     * @throws TTIOException
     */
    public static synchronized void truncateDatabase(final DatabaseConfiguration paramConfig)
        throws AbsTTException {
        // check that database must be closed beforehand
        if (!DATABASEMAP.containsKey(paramConfig.mFile)) {
            // if file is existing and folder is a tt-dataplace, delete it
            if (paramConfig.mFile.exists()
                && DatabaseConfiguration.Paths.compareStructure(paramConfig.mFile) == 0) {
                // instantiate the database for deletion
                recursiveDelete(paramConfig.mFile);
            }
        }
    }

    // //////////////////////////////////////////////////////////
    // END Creation/Deletion of Databases ///////////////////////
    // //////////////////////////////////////////////////////////

    // //////////////////////////////////////////////////////////
    // START Creation/Deletion of Resources /////////////////////
    // //////////////////////////////////////////////////////////

    public synchronized boolean createResource(final ResourceConfiguration paramConfig) throws TTIOException {
        boolean returnVal = true;
        // Setting the missing params in the settings, this overrides already set data.
        final File path =
            new File(new File(mDBConfig.mFile, DatabaseConfiguration.Paths.Data.getFile().getName()),
                paramConfig.mPath.getName());
        if (path.exists()) {
            return false;
        } else {
            returnVal = path.mkdir();
            if (returnVal) {
                for (ResourceConfiguration.Paths paths : ResourceConfiguration.Paths.values()) {
                    final File toCreate = new File(path, paths.getFile().getName());
                    if (paths.isFolder()) {
                        returnVal = toCreate.mkdir();
                    } else {
                        try {
                            returnVal = toCreate.createNewFile();
                        } catch (final IOException exc) {
                            throw new TTIOException(exc);
                        }
                    }
                    if (!returnVal) {
                        break;
                    }
                }
            }

            try {
                serializeConfiguration(paramConfig);
            } catch (final IOException exc) {
                throw new TTIOException(exc);
            }
            // if something was not correct, delete the partly created
            // substructure
            if (!returnVal) {
                paramConfig.mPath.delete();
            }
            return returnVal;
        }
    }

    public synchronized void truncateResource(final ResourceConfiguration paramConfig) {
        final File resourceFile =
            new File(new File(mDBConfig.mFile, DatabaseConfiguration.Paths.Data.getFile().getName()),
                paramConfig.mPath.getName());
        // check that database must be closed beforehand
        if (!SESSIONMAP.containsKey(resourceFile)) {
            // if file is existing and folder is a tt-dataplace, delete it
            if (resourceFile.exists() && ResourceConfiguration.Paths.compareStructure(resourceFile) == 0) {
                // instantiate the database for deletion
                recursiveDelete(resourceFile);
            }
        }
    }

    // //////////////////////////////////////////////////////////
    // END Creation/Deletion of Resources ///////////////////////
    // //////////////////////////////////////////////////////////

    // //////////////////////////////////////////////////////////
    // START Opening of Databases ///////////////////////
    // //////////////////////////////////////////////////////////
    /**
     * Open database. A database can be opened only once. Afterwards the
     * singleton instance bound to the File is given back.
     * 
     * @param paramFile
     *            where the database is located sessionConf a {@link SessionConfiguration} object to set up
     *            the session
     * @return {@link IDatabase} instance.
     * @throws AbsTTException
     *             if something odd happens
     */
    public static synchronized IDatabase openDatabase(final File paramFile) throws AbsTTException {
        if (!paramFile.exists()) {
            throw new TTUsageException("DB could not be opened (since it was not created?) at location",
                paramFile.toString());
        }
        FileInputStream is = null;
        DatabaseConfiguration config = null;
        try {
            is =
                new FileInputStream(new File(paramFile, DatabaseConfiguration.Paths.ConfigBinary.getFile()
                    .getName()));
            final ObjectInputStream de = new ObjectInputStream(is);
            config = (DatabaseConfiguration)de.readObject();
            de.close();
            is.close();
        } catch (final IOException exc) {
            throw new TTIOException(exc);
        } catch (ClassNotFoundException exc) {
            throw new TTIOException(exc.toString());
        }
        final Database database = new Database(config);
        final IDatabase returnVal = DATABASEMAP.putIfAbsent(paramFile, database);
        if (returnVal == null) {
            return database;
        } else {
            return returnVal;
        }
    }

    // //////////////////////////////////////////////////////////
    // END Opening of Databases ///////////////////////
    // //////////////////////////////////////////////////////////

    // //////////////////////////////////////////////////////////
    // START DB-Operations//////////////////////////////////
    // /////////////////////////////////////////////////////////

    public synchronized ISession getSession(final SessionConfiguration paramConfig) throws AbsTTException {

        final File resourceFile =
            new File(new File(mDBConfig.mFile, DatabaseConfiguration.Paths.Data.getFile().getName()),
                paramConfig.getFile());
        Session returnVal = SESSIONMAP.get(resourceFile);
        if (returnVal == null) {
            if (!resourceFile.exists()) {
                throw new TTUsageException(
                    "Resource could not be opened (since it was not created?) at location", resourceFile
                        .toString());
            }
            FileInputStream is = null;
            ResourceConfiguration config = null;
            try {
                is =
                    new FileInputStream(new File(resourceFile, ResourceConfiguration.Paths.ConfigBinary
                        .getFile().getName()));
                final ObjectInputStream de = new ObjectInputStream(is);
                config = (ResourceConfiguration)de.readObject();
                de.close();
                is.close();
            } catch (final ClassNotFoundException exc) {
                throw new TTIOException(exc.toString());
            } catch (final IOException exc) {
                throw new TTIOException(exc);
            }

            // Resource of session must be associated to this database
            assert config.mPath.getParentFile().getParentFile().equals(mDBConfig.mFile);
            returnVal = new Session(config, paramConfig);
            SESSIONMAP.put(resourceFile, returnVal);
        }
        return returnVal;
    }

    /**
     * This method forces the Database to close an existing instance.
     * 
     * @param paramFile
     *            where the database should be closed
     * @throws AbsTTException
     *             if something weird happens while closing
     */
    public synchronized void close() throws AbsTTException {
        for (final ISession session : SESSIONMAP.values()) {
            session.close();
        }
        DATABASEMAP.remove(mDBConfig.mFile);
    }

    // //////////////////////////////////////////////////////////
    // End DB-Operations//////////////////////////////////
    // /////////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append(this.mDBConfig);
        return builder.toString();
    }

    /**
     * Deleting a storage recursive. Used for deleting a databases
     * 
     * @param paramFile
     *            which should be deleted included descendants
     * @return true if delete is valid
     */
    protected static boolean recursiveDelete(final File paramFile) {
        if (paramFile.isDirectory()) {
            for (final File child : paramFile.listFiles()) {
                if (!recursiveDelete(child)) {
                    return false;
                }
            }
        }
        return paramFile.delete();
    }

    protected static boolean closeResource(final File paramFile) {
        return SESSIONMAP.remove(paramFile) != null ? true : false;
    }

    private static void serializeConfiguration(final IConfigureSerializable paramToSerialize)
        throws IOException {
        FileOutputStream os = null;
        os = new FileOutputStream(paramToSerialize.getConfigFile());
        final ObjectOutputStream en = new ObjectOutputStream(os);
        en.writeObject(paramToSerialize);
        en.close();
        os.close();
    }

}