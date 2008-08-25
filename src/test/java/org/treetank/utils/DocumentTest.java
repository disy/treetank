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
 * $Id:TestDocument.java 4373 2008-08-25 07:24:30Z kramis $
 */

package org.treetank.utils;

import org.treetank.api.IWriteTransaction;

/**
 * <h1>TestDocument</h1>
 * 
 * <p>
 * This class creates an XML document that contains all features seen in
 * the Extensible Markup Language (XML) 1.1 (Second Edition) as well as the
 * Namespaces in XML 1.1 (Second Edition).
 * </p>
 * 
 * <p>
 * The following figure describes the created test document (see
 * <code>xml/test.xml</code>). The nodes are described as follows:
 * 
 * <ul>
 * <li><code>IConstants.DOCUMENT : doc()</code></li>
 * <li><code>IConstants.FULLTEXT : ft()</code></li>
 * <li><code>IConstants.ELEMENT  : &lt;prefix:localPart&gt;</code></li>
 * <li><code>IConstants.ATTRIBUTE: &#64;prefix:localPart='value'</code></li>
 * <li><code>IConstants.TEXT     : #value</code></li>
 * </ul>
 *
 * <pre>
 * 0 doc()
 * 1 ft()
 * |-  2 &lt;p:a §p:ns &#64;i='j'&gt;
 *     |-  3 #oops1
 *     |-  4 &lt;b&gt;
 *     |   |-  5 #foo
 *     |   |-  6 &lt;c&gt;
 *     |-  7 #oops2
 *     |-  8 &lt;b &#64;p:x='y'&gt;
 *     |   |-  9 &lt;c&gt;
 *     |   |- 10 #bar
 *     |- 11 #oops3
 * </pre>
 * 
 * </p>
 */
public final class DocumentTest {

  /** String representation of test document with TNK environment. */
  public static final String XML_TANK =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
          + "<rest:sequence xmlns:rest=\"REST\"><rest:item>"
          + "<p:a xmlns:p=\"ns\" i=\"j\">oops1<b>foo<c/></b>oops2<b p:x=\"y\">"
          + "<c/>bar</b>oops3</p:a>"
          + "</rest:item></rest:sequence>";

  /** String representation of test document. */
  public static final String XML =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
          + "<p:a xmlns:p=\"ns\" i=\"j\">oops1<b>foo<c/></b>oops2<b p:x=\"y\">"
          + "<c/>bar</b>oops3</p:a>";

  /** String representation of test document without attributes. */
  public static final String XMLWITHOUTATTRIBUTES =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
          + "<p:a>oops1<b>foo<c></c></b>oops2<b>"
          + "<c></c>bar</b>oops3</p:a>";

  /**
   * Hidden constructor.
   *
   */
  private DocumentTest() {
    // Hidden.
  }

  /**
   * Create simple test document containing all supported node kinds.
   * 
   * @param wtx IWriteTransaction to write to.
   */
  public static void create(final IWriteTransaction wtx) {

    wtx.moveToDocumentRoot();

    wtx.insertElementAsFirstChild("p:a", "ns");
    wtx.insertAttribute("i", "", "j");
    wtx.insertNamespace("ns", "p");

    wtx.insertTextAsFirstChild("oops1");

    wtx.insertElementAsRightSibling("b", "");

    wtx.insertTextAsFirstChild("foo");
    wtx.insertElementAsRightSibling("c", "");
    wtx.moveToParent();

    wtx.insertTextAsRightSibling("oops2");

    wtx.insertElementAsRightSibling("b", "");
    wtx.insertAttribute("p:x", "ns", "y");

    wtx.insertElementAsFirstChild("c", "");
    wtx.insertTextAsRightSibling("bar");
    wtx.moveToParent();

    wtx.insertTextAsRightSibling("oops3");

    wtx.moveToDocumentRoot();

  }

  /**
   * Create simple test document containing all supported node kinds except
   * the attributes.
   * 
   * @param wtx IWriteTransaction to write to.
   */
  public static void createWithoutAttributes(final IWriteTransaction wtx) {

    wtx.moveToDocumentRoot();

    wtx.insertElementAsFirstChild("p:a", "ns");

    wtx.insertTextAsFirstChild("oops1");

    wtx.insertElementAsRightSibling("b", "");

    wtx.insertTextAsFirstChild("foo");
    wtx.insertElementAsRightSibling("c", "");
    wtx.moveToParent();

    wtx.insertTextAsRightSibling("oops2");

    wtx.insertElementAsRightSibling("b", "");

    wtx.insertElementAsFirstChild("c", "");
    wtx.insertTextAsRightSibling("bar");
    wtx.moveToParent();

    wtx.insertTextAsRightSibling("oops3");

    wtx.moveToDocumentRoot();

  }
  
  /**
   * Create simple test document containing all supported node kinds, but 
   * ignoring their namespace prefixes.
   * 
   * @param wtx IWriteTransaction to write to.
   */
  public static void createWithoutNamespace(final IWriteTransaction wtx) {

    wtx.moveToDocumentRoot();

    wtx.insertElementAsFirstChild("a", "");
    wtx.insertAttribute("i", "", "j");
   

    wtx.insertTextAsFirstChild("oops1");

    wtx.insertElementAsRightSibling("b", "");

    wtx.insertTextAsFirstChild("foo");
    wtx.insertElementAsRightSibling("c", "");
    wtx.moveToParent();

    wtx.insertTextAsRightSibling("oops2");

    wtx.insertElementAsRightSibling("b", "");
    wtx.insertAttribute("x", "", "y");

    wtx.insertElementAsFirstChild("c", "");
    wtx.insertTextAsRightSibling("bar");
    wtx.moveToParent();

    wtx.insertTextAsRightSibling("oops3");

    wtx.moveToDocumentRoot();

  }

}