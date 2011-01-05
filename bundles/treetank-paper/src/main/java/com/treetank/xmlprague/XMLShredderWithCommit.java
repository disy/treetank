/**
 * Copyright (c) 2010, Distributed Systems Group, University of Konstanz
 * 
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 * 
 * THE SOFTWARE IS PROVIDED AS IS AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 * 
 */

package com.treetank.xmlprague;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import com.treetank.access.Database;
import com.treetank.access.DatabaseConfiguration;
import com.treetank.access.WriteTransaction;
import com.treetank.api.IDatabase;
import com.treetank.api.ISession;
import com.treetank.api.IWriteTransaction;
import com.treetank.exception.TTException;
import com.treetank.exception.TTIOException;
import com.treetank.exception.TTUsageException;
import com.treetank.node.ENodes;
import com.treetank.node.ElementNode;
import com.treetank.service.xml.shredder.EShredderCommit;
import com.treetank.service.xml.shredder.EShredderInsert;
import com.treetank.service.xml.shredder.ListEventReader;
import com.treetank.settings.EFixed;
import com.treetank.utils.FastStack;
import com.treetank.utils.LogWrapper;
import com.treetank.utils.TypedValue;

import org.slf4j.LoggerFactory;

/**
 * This class appends a given {@link XMLStreamReader} to a {@link IWriteTransaction}. The content of the
 * stream is added as a subtree.
 * Based on a boolean which identifies the point of insertion, the subtree is
 * either added as subtree or as rightsibling.
 * 
 * @author Marc Kramis, Seabix
 * @author Sebastian Graf, University of Konstanz
 * 
 */
public class XMLShredderWithCommit implements Callable<Long> {
    /**
     * Log wrapper for better output.
     */
    private static final LogWrapper LOGWRAPPER = new LogWrapper(LoggerFactory
        .getLogger(XMLShredderWithCommit.class));

    private static final Random ran = new Random(1234);

    /** {@link IWriteTransaction}. */
    protected final transient IWriteTransaction mWtx;

    /** {@link XMLEventReader}. */
    protected transient XMLEventReader mReader;

    /** Append as first child or not. */
    protected transient EShredderInsert mFirstChildAppend;

    /** Determines if changes are going to be commit right after shredding. */
    private transient EShredderCommit mCommit;

    private final double prob;

    /**
     * Normal constructor to invoke a shredding process on a existing {@link WriteTransaction}.
     * 
     * @param paramWtx
     *            {@link IWriteTransaction} where the new XML Fragment should be placed
     * @param paramReader
     *            {@link XMLEventReader} to parse the xml fragment, which should be inserted
     * @param paramAddAsFirstChild
     *            determines if the insert is occuring on a node in an existing tree. <code>false</code> is
     *            not possible
     *            when wtx is on root node
     * @param paramCommit
     *            determine
     *            s if inserted nodes should be commited right afterwards
     * @throws TTUsageException
     *             if insertasfirstChild && updateOnly is both true OR if wtx is
     *             not pointing to doc-root and updateOnly= true
     */
    public XMLShredderWithCommit(final IWriteTransaction paramWtx, final XMLEventReader paramReader,
        final double prob) throws TTUsageException {
        mWtx = paramWtx;
        mReader = paramReader;
        mFirstChildAppend = EShredderInsert.ADDASFIRSTCHILD;
        mCommit = EShredderCommit.COMMIT;
        this.prob = prob;
    }

    /**
     * Invoking the shredder.
     * 
     * @throws TTException
     *             if any kind of Treetank exception which has occured
     * @return revision of file
     */
    @Override
    public Long call() throws TTException {
        final long revision = mWtx.getRevisionNumber();
        insertNewContent();

        if (mCommit == EShredderCommit.COMMIT) {
            mWtx.commit();
        }
        return revision;
    }

    /**
     * Insert new content based on a StAX parser {@link XMLStreamReader}.
     * 
     * @throws TTException
     *             if something went wrong while inserting
     */
    protected final void insertNewContent() throws TTException {
        try {
            FastStack<Long> leftSiblingKeyStack = new FastStack<Long>();

            leftSiblingKeyStack.push((Long)EFixed.NULL_NODE_KEY.getStandardProperty());
            boolean firstElement = true;
            int level = 0;
            QName rootElement = null;
            boolean endElemReached = false;
            StringBuilder sBuilder = new StringBuilder();

            // Iterate over all nodes.
            while (mReader.hasNext() && !endElemReached) {
                final XMLEvent event = mReader.nextEvent();

                switch (event.getEventType()) {
                case XMLStreamConstants.START_ELEMENT:
                    level++;
                    leftSiblingKeyStack = addNewElement(leftSiblingKeyStack, (StartElement)event);
                    if (firstElement) {
                        firstElement = false;
                        rootElement = event.asStartElement().getName();
                    }
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    level--;
                    if (level == 0 && rootElement != null
                        && rootElement.equals(event.asEndElement().getName())) {
                        endElemReached = true;
                    }
                    leftSiblingKeyStack.pop();
                    mWtx.moveTo(leftSiblingKeyStack.peek());
                    break;
                case XMLStreamConstants.CHARACTERS:
                    if (mReader.peek().getEventType() == XMLStreamConstants.CHARACTERS) {
                        sBuilder.append(event.asCharacters().getData().trim());
                    } else {
                        sBuilder.append(event.asCharacters().getData().trim());
                        leftSiblingKeyStack = addNewText(leftSiblingKeyStack, sBuilder.toString());
                        sBuilder = new StringBuilder();
                    }
                    break;
                default:
                    // Node kind not known.
                }
                if (ran.nextDouble() < prob) {
                    mWtx.commit();
                }
            }
        } catch (final XMLStreamException e) {
            LOGWRAPPER.error(e.getMessage(), e);
            throw new TTIOException(e);
        }
    }

    /**
     * Add a new element node.
     * 
     * @param paramLeftSiblingKeyStack
     *            stack used to determine if the new element has to be inserted
     *            as a right sibling or as a new child (in the latter case is
     *            NULL on top of the stack)
     * @param paramEvent
     *            the current event from the StAX parser
     * @return the modified stack
     * @throws TTException
     *             if adding {@link ElementNode} fails
     */
    protected final FastStack<Long> addNewElement(final FastStack<Long> paramLeftSiblingKeyStack,
        final StartElement paramEvent) throws TTException {
        assert paramLeftSiblingKeyStack != null && paramEvent != null;
        long key;

        final QName name = paramEvent.getName();

        if (mFirstChildAppend == EShredderInsert.ADDASRIGHTSIBLING) {
            if (mWtx.getNode().getKind() == ENodes.ROOT_KIND) {
                throw new TTUsageException("Subtree can not be inserted as sibling of Root");
            }
            key = mWtx.insertElementAsRightSibling(name);
            mFirstChildAppend = EShredderInsert.ADDASFIRSTCHILD;
        } else {
            if (paramLeftSiblingKeyStack.peek() == (Long)EFixed.NULL_NODE_KEY.getStandardProperty()) {
                key = mWtx.insertElementAsFirstChild(name);
            } else {
                key = mWtx.insertElementAsRightSibling(name);
            }
        }

        paramLeftSiblingKeyStack.pop();
        paramLeftSiblingKeyStack.push(key);
        paramLeftSiblingKeyStack.push((Long)EFixed.NULL_NODE_KEY.getStandardProperty());

        // Parse namespaces.
        for (final Iterator<?> it = paramEvent.getNamespaces(); it.hasNext();) {
            final Namespace namespace = (Namespace)it.next();
            mWtx.insertNamespace(namespace.getNamespaceURI(), namespace.getPrefix());
            mWtx.moveTo(key);
        }

        // Parse attributes.
        for (final Iterator<?> it = paramEvent.getAttributes(); it.hasNext();) {
            final Attribute attribute = (Attribute)it.next();
            mWtx.insertAttribute(attribute.getName(), attribute.getValue());
            mWtx.moveTo(key);
        }
        return paramLeftSiblingKeyStack;
    }

    /**
     * Add a new text node.
     * 
     * @param paramLeftSiblingKeyStack
     *            stack used to determine if the new element has to be inserted
     *            as a right sibling or as a new child (in the latter case is
     *            NULL on top of the stack)
     * @param paramText
     *            the text string to add
     * @return the modified stack
     * @throws TTException
     *             if adding text fails
     */
    protected final FastStack<Long> addNewText(final FastStack<Long> paramLeftSiblingKeyStack,
        final String paramText) throws TTException {
        assert paramLeftSiblingKeyStack != null;
        final String text = paramText;
        long key;
        final ByteBuffer textByteBuffer = ByteBuffer.wrap(TypedValue.getBytes(text));
        if (textByteBuffer.array().length > 0) {

            if (paramLeftSiblingKeyStack.peek() == (Long)EFixed.NULL_NODE_KEY.getStandardProperty()) {
                key = mWtx.insertTextAsFirstChild(new String(textByteBuffer.array()));
            } else {
                key = mWtx.insertTextAsRightSibling(new String(textByteBuffer.array()));
            }

            paramLeftSiblingKeyStack.pop();
            paramLeftSiblingKeyStack.push(key);

        }
        return paramLeftSiblingKeyStack;
    }

    /**
     * Main method.
     * 
     * @param mArgs
     *            Input and output files.
     * @throws Exception
     *             In case of any exception.
     */
    public static void main(final String... mArgs) throws Exception {
        if (mArgs.length != 2) {
            System.out.println("Usage: XMLShredder input.xml output.tnk");
            System.exit(1);
        }

        System.out.print("Shredding '" + mArgs[0] + "' to '" + mArgs[1] + "' ... ");
        final long time = System.currentTimeMillis();
        final File target = new File(mArgs[1]);
        Database.truncateDatabase(target);
        Database.createDatabase(new DatabaseConfiguration(target));
        final IDatabase db = Database.openDatabase(target);
        final ISession session = db.getSession();
        final IWriteTransaction wtx = session.beginWriteTransaction();
        final XMLEventReader reader = createReader(new File(mArgs[0]));
        final XMLShredderWithCommit shredder = new XMLShredderWithCommit(wtx, reader, 0.0d);
        shredder.call();

        wtx.close();
        session.close();
        db.close();

        System.out.println(" done [" + (System.currentTimeMillis() - time) + "ms].");
    }

    /**
     * Create a new StAX reader on a file.
     * 
     * @param paramFile
     *            the XML file to parse
     * @return an {@link XMLEventReader}
     * @throws IOException
     *             if I/O operation fails
     * @throws XMLStreamException
     *             if any parsing error occurs
     */
    public static synchronized XMLEventReader createReader(final File paramFile) throws IOException,
        XMLStreamException {
        final XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        final InputStream in = new FileInputStream(paramFile);
        return factory.createXMLEventReader(in);
    }

    /**
     * Create a new StAX reader based on a List of {@link XMLEvent}s.
     * 
     * @param paramEvents
     *            {@link XMLEvent}s
     * @return an {@link XMLEventReader}
     * @throws IOException
     *             if I/O operation fails
     * @throws XMLStreamException
     *             if any parsing error occurs
     */
    public static synchronized XMLEventReader createListReader(final List<XMLEvent> paramEvents)
        throws IOException, XMLStreamException {
        if (paramEvents == null) {
            throw new IllegalArgumentException("paramEvents may not be null!");
        }
        return new ListEventReader(paramEvents);
    }
}
