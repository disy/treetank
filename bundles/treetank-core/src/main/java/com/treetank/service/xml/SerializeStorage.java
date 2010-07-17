package com.treetank.service.xml;

import com.treetank.api.IAxis;
import com.treetank.api.IReadTransaction;
import com.treetank.axis.DescendantAxis;
import com.treetank.node.AbsStructNode;
import com.treetank.node.ENodes;
import com.treetank.utils.FastStack;

/**
 * Class implements main serialization 
 * 
 * @author johannes
 *
 */
abstract class SerializeStorage implements ISerialize {
  
  /** Transaction to read from (is the same as the mAxis). */
  final IReadTransaction mRTX;

  /** Descendant-or-self axis used to traverse subtree. */
  final IAxis mAxis;

  /** Stack for reading end element. */
  final FastStack<Long> mStack;
  
  /** Serialize XML declaration. */
  final boolean mSerializeXMLDeclaration;

  /** Serialize rest header and closer and rest:id */
  final boolean mSerializeRest;

  /** Serialize id */
  final boolean mSerializeId;
  
  /**
   * Constructor.
   * 
   * @param rtx
   * @param serializeXMLDeclaration
   * @param serializeRest
   * @param serializeId
   */
  public SerializeStorage(
      final IReadTransaction rtx,
      final boolean serializeXMLDeclaration,
      final boolean serializeRest,
      final boolean serializeId) {
    mRTX = rtx;
    mAxis = new DescendantAxis(rtx, true);
    mStack = new FastStack<Long>();
    mSerializeXMLDeclaration = serializeXMLDeclaration;
    mSerializeRest = serializeRest;
    mSerializeId = serializeId;
  }
  
  /**
   * Serialize the storage.
   * 
   * @param serialize
   *                    Class which implements {@link ISerialize} interface.
   * @throws Exception
   */
  public void serialize() throws Exception {
    // Setup primitives.
    boolean closeElements = false;
    long key = mAxis.getTransaction().getNode().getNodeKey();

    // Iterate over all nodes of the subtree including self.
    while (mAxis.hasNext()) {
      key = mAxis.next();

      // Emit all pending end elements.
      if (closeElements) {
        while (!mStack.empty()
            && mStack.peek() != ((AbsStructNode) mRTX.getNode())
                .getLeftSiblingKey()) {
          mRTX.moveTo(mStack.pop());
          emitEndElement();
          mRTX.moveTo(key);
        }
        if (!mStack.empty()) {
          mRTX.moveTo(mStack.pop());
          emitEndElement();
        }
        mRTX.moveTo(key);
        closeElements = false;
      }

      // Emit node.
      emitNode();

      // Push end element to stack if we are a start element with
      // children.
      if (mRTX.getNode().getKind() == ENodes.ELEMENT_KIND
          && ((AbsStructNode) mRTX.getNode()).hasFirstChild()) {
        mStack.push(mRTX.getNode().getNodeKey());
      }

      // Remember to emit all pending end elements from stack if
      // required.
      if (!((AbsStructNode) mRTX.getNode()).hasFirstChild()
          && !((AbsStructNode) mRTX.getNode()).hasRightSibling()) {
        closeElements = true;
      }
    }

    // Finally emit all pending end elements.
    while (!mStack.empty()) {
      mRTX.moveTo(mStack.pop());
      emitEndElement();
    }
  }
}
