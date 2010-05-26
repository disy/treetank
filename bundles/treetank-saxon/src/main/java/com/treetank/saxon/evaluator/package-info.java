/**
<h1>Evaluators to simplify Saxon access.</h1>
  <p>
    Implemented as Callables, therefore can be used in ThreadPools.
  </p>
  <p>
    Code example for use with XPathEvaluator:
    <code><pre>
      Database.truncateDatabase(file);
      final IDatabase database = Database.openDatabase(file);
      final ISession session = database.getSession();   
      final XPathSelector selector = new XPathEvaluator(QUERYSTRING,
        session, file).call();

      final StringBuilder strBuilder = new StringBuilder();

      for (final XdmItem item : selector) {
        strBuilder.append(item.toString());
      }
        
      System.out.println(strBuilder.toString());
    </pre></code>
    
    You can do everything you like with the resulting items. The method <em>
    getStringValue()</em> of the item get's the string value from the underlying 
    node type. In case of an element it concatenates all descending text-nodes.
    To get the underlying node, which is a Treetank item instead of a Saxon item
    can be retrieved by using the identically named method <em>
    getUnderlyingNode()</em>.
  </p>
  <p>
    Code example for use with XQueryEvaluator
    <code><pre>
      Database.truncateDatabase(file);
      final IDatabase database = Database.openDatabase(file);
      final ISession session = database.getSession(); 
      
      final StringBuilder strBuilder = new StringBuilder();

      for (final XdmItem item : value) {
        strBuilder.append(item.toString());
      }
      
      System.out.println(strBuilder.toString());
    </pre></code>
  </p>
  <p>
    Code example for use with XQueryEvaluatorOutputStream:
    <code><pre>
      Database.truncateDatabase(file);
      final IDatabase database = Database.openDatabase(file);
      final ISession session = database.getSession();   
      final XQueryEvaluator xqe =
        new XQueryEvaluator(
          XQUERYSTRING,
          session,
          file,
          new ByteArrayOutputStream());
      final String result = xqe.call().toString();
        
      System.out.println(result);
    </pre></code>
    
    Note that you can use other output streams as well.
  </p>
  <p>
    Code example for use with XQueryEvaluatorSAXHandler:
    <code><pre>    
      final StringBuilder strBuilder = new StringBuilder();
      final ContentHandler contHandler = new XMLFilterImpl() {

        @Override
        public void startElement(
          final String uri,
          final String localName,
          final String qName,
          final Attributes atts) throws SAXException {
          strBuilder.append("<" + localName);

          for (int i = 0; i < atts.getLength(); i++) {
            strBuilder.append(" " + atts.getQName(i));
            strBuilder.append("=\"" + atts.getValue(i) + "\"");
          }

          strBuilder.append(">");
        }

        @Override
        public void endElement(String uri, String localName, String qName)
            throws SAXException {
          strBuilder.append("</" + localName + ">");
        }

        @Override
        public void characters(final char[] ch, final int start, final int length)
            throws SAXException {
          for (int i = start; i < start + length; i++) {
            strBuilder.append(ch[i]);
          }
        }
      };

      Database.truncateDatabase(file);
      final IDatabase database = Database.openDatabase(file);
      final ISession session = database.getSession();   

      new XQueryEvaluatorSAXHandler(
          XQUERYSTRING,
          session,
          file,
          contHandler).call();
          
      System.out.println(strBuilder.toString());
    </pre></code>
  </p>
  <p>
    Code example for use with XSLTEvaluator:
    <code><pre>
    </pre></code>
  </p>
 *
 * @author Johannes Lichtenberger, University of Konstanz
 */
package com.treetank.saxon.evaluator;