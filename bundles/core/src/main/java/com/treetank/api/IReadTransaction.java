/*
 * Copyright (c) 2008, Marc Kramis (Ph.D. Thesis), University of Konstanz
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
 * $Id: IReadTransaction.java 4460 2008-09-02 09:08:36Z scherer $
 */

package com.treetank.api;

import javax.xml.namespace.QName;

import com.treetank.exception.TreetankException;
import com.treetank.exception.TreetankIOException;

/**
 * <h1>IReadTransaction</h1>
 * 
 * <h2>Description</h2>
 * 
 * <p>
 * Interface to access nodes based on the
 * Key/ParentKey/FirstChildKey/LeftSiblingKey/RightSiblingKey/ChildCount
 * encoding. This encoding keeps the children ordered but has no knowledge of
 * the global node ordering. The underlying tree is accessed in a cursor-like
 * fashion.
 * </p>
 * 
 * <h2>Convention</h2>
 * 
 * <p>
 * <ol>
 * <li>Only a single thread accesses each IReadTransaction instance.</li>
 * <li><strong>Precondition</strong> before moving cursor:
 * <code>IReadTransaction.getRelatedNode().getNodeKey() == n</code>.</li>
 * <li><strong>Postcondition</strong> after moving cursor:
 * <code>(IReadTransaction.moveX() == true &&
 *       IReadTransaction.getRelatedNode().getNodeKey() == m) ||
 *       (IReadTransaction.moveX() == false &&
 *       IReadTransaction.getRelatedNode().getNodeKey() == n)</code>.</li>
 * </ol>
 * </p>
 * 
 * <h2>User Example</h2>
 * 
 * <p>
 * 
 * <pre>
 *   final IReadTransaction rtx = session.beginReadTransaction();
 *   
 *   // Either test before moving...
 *   if (rtx.getRelatedNode().hasFirstChild()) {
 *     rtx.moveToFirstChild();
 *     ...
 *   }
 *   
 *   // or test after moving. Whatever, do the test!
 *   if (rtx.moveToFirstChild()) {
 *     ...
 *   }
 *   
 *   // Access local part of element.
 *   if (rtx.getRelatedNode().isElement() &amp;&amp; rtx.getRelatedNode().getName().equalsIgnoreCase(&quot;foo&quot;) {
 *     ...
 *   }
 *   
 *   // Access value of first attribute of element.
 *   if (rtx.getRelatedNode().isElement() &amp;&amp; (rtx.getRelatedNode().getAttributeCount() &gt; 0)) {
 *     rtx.moveToAttribute(0);
 *     System.out.println(UTF.parseString(rtx.getValue()));
 *   }
 *   
 *   rtx.close();
 * </pre>
 * 
 * </p>
 * 
 * <h2>Developer Example</h2>
 * 
 * <p>
 * 
 * <pre>
 *   public final void someIReadTransactionMethod() {
 *     // This must be called to make sure the transaction is not closed.
 *     assertNotClosed();
 *     ...
 *   }
 * </pre>
 * 
 * </p>
 */
public interface IReadTransaction {

    /** String constants used by xpath. */
    String[] XPATHCONSTANTS = { "xs:anyType", "xs:anySimpleType",
            "xs:anyAtomicType", "xs:untypedAtomic", "xs:untyped", "xs:string",
            "xs:duration", "xs:yearMonthDuration", "xs:dayTimeDuration",
            "xs:dateTime", "xs:time", "xs:date", "xs:gYearMonth", "xs:gYear",
            "xs:gMonthDay", "xs:gDay", "xs:gMonth", "xs:boolean",
            "xs:base64Binary", "xs:hexBinary", "xs:anyURI", "xs:QName",
            "xs:NOTATION", "xs:float", "xs:double", "xs:pDecimal",
            "xs:decimal", "xs:integer", "xs:long", "xs:int", "xs:short",
            "xs:byte", "xs:nonPositiveInteger", "xs:negativeInteger",
            "xs:nonNegativeInteger", "xs:positiveInteger", "xs:unsignedLong",
            "xs:unsignedInt", "xs:unsignedShort", "xs:unsignedByte",
            "xs:normalizedString", "xs:token", "xs:language", "xs:name",
            "xs:NCName", "xs:ID", "xs:IDREF", "xs:ENTITY", "xs:IDREFS",
            "xs:NMTOKEN", "xs:NMTOKENS", };

    /**
     * Get ID of transaction.
     * 
     * @return ID of transaction.
     */
    long getTransactionID();

    /**
     * What is the revision number of this IReadTransaction?
     * 
     * @return Immutable revision number of this IReadTransaction.
     */
    long getRevisionNumber() throws TreetankIOException;

    /**
     * UNIX-style timestamp of the commit of the revision.
     * 
     * @return Timestamp of revision commit.
     */
    long getRevisionTimestamp() throws TreetankIOException;

    /**
     * Getting the maximum nodekey avaliable in this revision.
     * 
     * @return the maximum nodekey
     */
    long getMaxNodeKey() throws TreetankIOException;

    // --- Node Selectors
    // --------------------------------------------------------

    /**
     * Move cursor to a node by its node key.
     * 
     * @param nodeKey
     *            Key of node to select.
     * @return True if the node with the given node key is selected.
     */
    boolean moveTo(final long nodeKey);

    /**
     * Move cursor to document root node.
     * 
     * @return True if the document root node is selected.
     */
    boolean moveToDocumentRoot();

    /**
     * Move cursor to parent node of currently selected node.
     * 
     * @return True if the parent node is selected.
     */
    boolean moveToParent();

    /**
     * Move cursor to first child node of currently selected node.
     * 
     * @return True if the first child node is selected.
     */
    boolean moveToFirstChild();

    /**
     * Move cursor to left sibling node of the currently selected node.
     * 
     * @return True if the left sibling node is selected.
     */
    boolean moveToLeftSibling();

    /**
     * Move cursor to right sibling node of the currently selected node.
     * 
     * @return True if the right sibling node is selected.
     */
    boolean moveToRightSibling();

    /**
     * Move cursor to attribute by its index.
     * 
     * @param index
     *            Index of attribute to move to.
     * @return True if the attribute node is selected.
     */
    boolean moveToAttribute(final int index);

    /**
     * Move cursor to namespace declaration by its index.
     * 
     * @param index
     *            Index of attribute to move to.
     * @return True if the namespace node is selected.
     */
    boolean moveToNamespace(final int index);

    // --- Node Getters
    // ----------------------------------------------------------

    /**
     * Getting the value of the current node
     */
    String getValueOfCurrentNode();

    /**
     * Getting the name of a current node
     * 
     * @return
     */
    QName getQNameOfCurrentNode();

    /**
     * Getting the name of the current node
     */
    @Deprecated
    String getNameOfCurrentNode();

    /**
     * Getting the uri of the current node
     */
    @Deprecated
    String getURIOfCurrentNode();

    /**
     * Getting the type of the current node
     */
    String getTypeOfCurrentNode();

    /**
     * Get key for given name. This is used for efficient name testing.
     * 
     * @param name
     *            Name, i.e., local part, URI, or prefix.
     * @return Internal key assigned to given name.
     */
    int keyForName(final String name);

    /**
     * Get name for key. This is used for efficient key testing.
     * 
     * @param key
     *            Key, i.e., local part key, URI key, or prefix key.
     * @return String containing name for given key.
     */
    String nameForKey(final int key);

    /**
     * Get raw name for key. This is used for efficient key testing.
     * 
     * @param key
     *            Key, i.e., local part key, URI key, or prefix key.
     * @return Byte array containing name for given key.
     */
    byte[] rawNameForKey(final int key);

    /**
     * Get item list containing volatile items such as atoms or fragments.
     * 
     * @return Item list.
     */
    IItemList getItemList();

    /**
     * Getting the current node.
     * 
     * @return the node
     */
    IItem getNode();

    /**
     * Close shared read transaction and immediately release all resources.
     * 
     * This is an idempotent operation and does nothing if the transaction is
     * already closed.
     */
    void close() throws TreetankException;
}