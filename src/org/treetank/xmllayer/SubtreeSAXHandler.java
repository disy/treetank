package org.treetank.xmllayer;

import java.util.Hashtable;

import org.treetank.api.IConstants;
import org.treetank.api.ISession;
import org.treetank.utils.FastLongStack;
import org.treetank.utils.FastObjectStack;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * Subtree SAXHandler to handle a xml input and store it into serveral given subtrees.
 * Every subtree starts at the first level of the xml.
 * 
 * @author sebi
 *
 */
public class SubtreeSAXHandler extends SAXHandler {

  private static final int COMMITTHRESHOLD = 10000;

  /** TreeTank write transaction. */
  private final ISession session;

  /**
   * Counter for commiting after the insertation of a fixed number of
   * nodes
   */
  private int nodeCounter = 0;

  /** Mapping for nodekeys for each subtree */
  private final Hashtable<Long, Long> subtreeKeyNodeMapping;

  /** Stack for storing the different leftSiblingStacks */
  private final FastObjectStack<FastLongStack> stacks;

  /**
   * Constructor.
   * 
   * @param initDocument
   *            Name of document.
   * @param initSession
   *            Writing transaction to write to.
   */
  public SubtreeSAXHandler(final ISession initSession) throws Exception {
    super(initSession);
    session = initSession;
    subtreeKeyNodeMapping = new Hashtable<Long, Long>(0);
    stacks = new FastObjectStack<FastLongStack>();
  }

  @Override
  public void endDocument() throws SAXException {
    try {
      if (!this.subtreeKeyNodeMapping.isEmpty()) {
        throw new IllegalStateException(
            "Mapping not empty. There are subtrees left to be closed.");
      }
      super.endDocument();
    } catch (Exception e) {
      throw new SAXException(e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void startElement(
      final String uri,
      final String localName,
      final String qName,
      final Attributes attr) throws SAXException {

    try {

      if (nodeCounter > COMMITTHRESHOLD) {
        final long tempkey = mWTX.getNodeKey();
        session.commit();
        mWTX = session.beginWriteTransaction();
        System.gc();
        mWTX.moveTo(tempkey);
        nodeCounter = 0;
      }
      nodeCounter++;

      super.startElement(uri, localName, qName, attr);

    } catch (Exception e) {
      throw new SAXException(e);
    }
  }

  /**
   * Starts a new subtree with the given subtreeID. The current node is
   * stored in the mapping and the writetransaction is moved to the first
   * level in order to insert a new subtree.
   * 
   * @param subtreeID
   *            to start
   * @throws SAXException
   */
  public void subtreeStarting(final long subtreeID) throws SAXException {
    try {

      this.subtreeKeyNodeMapping.put(subtreeID, mWTX.getNodeKey());
      this.stacks.push(mLeftSiblingKeyStack);
      mLeftSiblingKeyStack = new FastLongStack();
      mLeftSiblingKeyStack.push(IConstants.NULL_KEY);
      mLeftSiblingKeyStack.push(IConstants.NULL_KEY);
      mWTX.moveToRoot();

    } catch (Exception e) {
      throw new SAXException(e);
    }
  }

  /**
   * Ends the current subtree. The pointer is moved to the last position
   * where the new subtree event happens
   * 
   * @param subtreeID
   *            where the new subtree starts
   * @throws SAXException
   */
  public void subtreeEnding(final long subtreeID) throws SAXException {
    try {
      session.commit();
      mWTX = session.beginWriteTransaction();
      System.gc();
      nodeCounter = 0;
      mWTX.moveTo(this.subtreeKeyNodeMapping.remove(subtreeID));
      mLeftSiblingKeyStack = stacks.pop();
    } catch (Exception e) {
      throw new SAXException(e);
    }
  }

}