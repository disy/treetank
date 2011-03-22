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

/**
 * 
 */
package org.treetank.service.jaxrx.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.treetank.access.Database;
import com.treetank.api.IDatabase;
import com.treetank.api.IReadTransaction;
import com.treetank.api.ISession;
import com.treetank.axis.AbsAxis;
import com.treetank.exception.AbsTTException;
import com.treetank.service.xml.xpath.XPathAxis;

/**
 * This class is responsible to offer XPath processing functions for REST.
 * 
 * @author Patrick Lang, Lukas Lewandowski, University of Konstanz.
 * 
 */
public class RestXPathProcessor {

    /**
     * This field the begin result element of a XQuery or XPath expression.
     */
    private static transient String beginResult = "<jaxrx:result xmlns:jaxrx=\"http://jaxrx.org/\">";

    /**
     * This field the end result element of a XQuery or XPath expression.
     */
    private static transient String endResult = "</jaxrx:result>";

    /**
     * Getting part of the XML based on a XPath query
     * 
     * @param resourceName
     *            where the content should be extracted
     * @param xpath
     *            contains XPath query
     * @param nodeid
     *            To response the resource with a restid for each node ( <code>true</code>) or without (
     *            <code>false</code>).
     * @param revision
     *            The revision of the requested resource. If <code>null</code>,
     *            than response the latest revision.
     * @param output
     *            The OutputStream reference which have to be modified and
     *            returned
     * @return the queried XML fragment
     * @throws IOException
     *             The exception occurred.
     * @throws AbsTTException
     */
    public OutputStream getXpathResource(final String resourceName, final String xpath, final boolean nodeid,
        final Long revision, final OutputStream output, final boolean wrapResult) throws IOException,
        AbsTTException {

        // work around because of query root char '/'
        String qQuery = xpath;
        if (xpath.charAt(0) == '/')
            qQuery = ".".concat(xpath);
        if (resourceName.endsWith(RESTProps.COLEND)) {
            final List<File> tnks = checkColForTnks(resourceName);
            // Serializing the XML
            if (wrapResult) {
                output.write(beginResult.getBytes());
                for (final File aTNK : tnks) {
                    doXPathRes(aTNK, revision, output, nodeid, qQuery);
                }
                output.write(endResult.getBytes());
            } else {
                for (final File aTNK : tnks) {
                    doXPathRes(aTNK, revision, output, nodeid, qQuery);
                }
            }

        } else {
            final String tnkFile =
                RESTProps.STOREDBPATH + File.separatorChar + resourceName + RESTProps.TNKEND;
            final File dbFile = new File(tnkFile);
            if (WorkerHelper.checkExistingResource(dbFile)) {

                if (wrapResult) {
                    output.write(beginResult.getBytes());
                    doXPathRes(dbFile, revision, output, nodeid, qQuery);
                    output.write(endResult.getBytes());
                } else {
                    doXPathRes(dbFile, revision, output, nodeid, qQuery);
                }

            } else {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
        }
        return output;
    }

    /**
     * Getting part of the XML based on a XPath query
     * 
     * @param dbFile
     *            where the content should be extracted
     * 
     * @param query
     *            contains XPath query
     * @param rId
     *            To response the resource with a restid for each node ( <code>true</code>) or without (
     *            <code>false</code>).
     * @param doRevision
     *            The revision of the requested resource. If <code>null</code>,
     *            than response the latest revision.
     * @param output
     *            The OutputStream reference which have to be modified and
     *            returned
     * @param doNodeId
     *            specifies whether node id should be shown
     * @param doWrap
     *            output of result elements
     * @throws AbsTTException
     */
    public void getXpathResource(final File dbFile, final long rId, final String query,
        final boolean doNodeId, final Long doRevision, final OutputStream output, final boolean doWrap)
        throws AbsTTException {

        // work around because of query root char '/'
        String qQuery = query;
        if (query.charAt(0) == '/')
            qQuery = ".".concat(query);

        IDatabase database = null;
        ISession session = null;
        IReadTransaction rtx = null;
        try {
            database = Database.openDatabase(dbFile);
            // Creating a new session
            session = database.getSession();
            // Creating a transaction

            if (doRevision == null) {
                rtx = session.beginReadTransaction();
            } else {
                rtx = session.beginReadTransaction(doRevision);
            }

            final boolean exist = rtx.moveTo(rId);
            if (exist) {
                final AbsAxis axis = new XPathAxis(rtx, qQuery);
                if (doWrap) {
                    output.write(beginResult.getBytes());
                    for (final long key : axis) {
                        if (key >= 0) {
                            WorkerHelper.serializeXML(session, output, false, doNodeId, key, doRevision)
                                .call();
                        } else {
                            output.write(rtx.getNode().getRawValue());
                        }
                    }

                    output.write(endResult.getBytes());
                } else {
                    for (final long key : axis) {
                        if (key >= 0) {
                            WorkerHelper.serializeXML(session, output, false, doNodeId, key, doRevision)
                                .call();
                        } else {
                            output.write(rtx.getNode().getRawValue());
                        }
                    }

                }
            } else {
                throw new WebApplicationException(404);
            }

        } catch (final Exception globExcep) {
            throw new WebApplicationException(globExcep, Response.Status.INTERNAL_SERVER_ERROR);
        } finally {
            WorkerHelper.closeRTX(rtx, session, database);
        }
    }

    /**
     * This method performs an XPath evaluation and writes it to a given output
     * stream.
     * 
     * @param dbFile
     *            The existing tnk file.
     * @param revision
     *            The revision of the requested document.
     * @param output
     *            The output stream where the results are written.
     * @param nodeid
     *            <code>true</code> if node id's have to be delivered. <code>false</code> otherwise.
     * @param xpath
     *            The XPath expression.
     * @throws AbsTTException
     */
    private void doXPathRes(final File dbFile, final Long revision, final OutputStream output,
        final boolean nodeid, final String xpath) throws AbsTTException {
        // Database connection to treetank
        IDatabase database = null;
        ISession session = null;
        IReadTransaction rtx = null;
        try {
            database = Database.openDatabase(dbFile);
            // Creating a new session
            session = database.getSession();
            // Creating a transaction
            if (revision == null) {
                rtx = session.beginReadTransaction();
            } else {
                rtx = session.beginReadTransaction(revision);
            }

            final AbsAxis axis = new XPathAxis(rtx, xpath);
            for (final long key : axis) {
                if (key >= 0) {
                    WorkerHelper.serializeXML(session, output, false, nodeid, key, revision).call();
                    final String book = new String(((ByteArrayOutputStream)output).toByteArray());
                    System.out.print(book);
                } else {
                    output.write(rtx.getNode().getRawValue());
                }
            }

        } catch (final Exception globExcep) {
            throw new WebApplicationException(globExcep, Response.Status.INTERNAL_SERVER_ERROR);
        } finally {
            rtx.moveToDocumentRoot();

            WorkerHelper.closeRTX(rtx, session, database);

            // rtx.close();
            // session.close();
            // database.close();
        }
    }

    /**
     * This method checks a collection for included tnk files.
     * 
     * @param resource
     *            The name of the collection.
     * @return A list of available tnk files.
     */
    private List<File> checkColForTnks(final String resource) {
        List<File> tnks;
        final File colFile = new File(RESTProps.STOREDBPATH + File.separatorChar + resource);
        if (colFile.exists()) {
            tnks = new ArrayList<File>();
            final File[] files = colFile.listFiles();
            for (final File file : files) {
                if (file.getName().endsWith(RESTProps.TNKEND)) {
                    tnks.add(file);
                }
            }
            return tnks;
        } else {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

    }

}
