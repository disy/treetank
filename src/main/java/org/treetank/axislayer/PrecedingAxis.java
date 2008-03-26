
package org.treetank.axislayer;

import org.treetank.api.IAxis;
import org.treetank.api.IReadTransaction;
import org.treetank.utils.FastStack;

/**
 * <h1>PrecedingAxis</h1>
 * 
 * <p>
 * Iterate over all preceding nodes of kind ELEMENT or TEXT starting at a
 * given node. Self is not included.
 * </p>
 */
public class PrecedingAxis extends AbstractAxis implements IAxis {

  
  private boolean mIsFirst;
  
  private FastStack<Long> stack;
  
  /**
   * Constructor initializing internal state.
   * 
   * @param rtx
   *          Exclusive (immutable) trx to iterate with.
   */
  public PrecedingAxis(final IReadTransaction rtx) {

    super(rtx);
    mIsFirst = true;
   stack = new FastStack<Long>();
    
    
    
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final void reset(final long nodeKey) {

    super.reset(nodeKey);
    mIsFirst = true;
    stack = new FastStack<Long>();

  }

  /**
   * {@inheritDoc}
   */
  public final boolean hasNext() {

    
    //assure, that preceding is not evaluated on an attribute or a namespace
    if (mIsFirst) {
      mIsFirst = false;
      if (getTransaction().isAttributeKind() 
      //   || getTransaction().isNamespaceKind()
      ) {
          resetToStartKey();
          return false;
        }
      }
    
    resetToLastKey();
        
      
    if (!stack.empty()) {
      //return all nodes of the current subtree in reverse document order
      getTransaction().moveTo(stack.pop());
      return true;
    } else {
      
      if (getTransaction().hasLeftSibling()) {
        getTransaction().moveToLeftSibling();
        //because this axis return the precedings in reverse document order, we
        //need to travel to the node in the subtree, that comes last in document
        //order.
        getLastChild();
        return true;
          
      } else {
        while (getTransaction().hasParent()) {
          //ancestors are not part of the preceding set
          getTransaction().moveToParent();
          if (getTransaction().hasLeftSibling()) {
            getTransaction().moveToLeftSibling();
            //move to last node in the subtree
            getLastChild();
            return true;
          }
        }
      }
    }
    
    resetToStartKey();
    return false;
        
  }
  
  
  /**
   * Moves the transaction to the node in the current subtree, that is last in 
   * document order and pushes all other node key on a stack.
   * At the end the stack contains all node keys except for the last one in
   * reverse document order.
   */
  private void getLastChild() {
    
    if (getTransaction().hasFirstChild()) {
      while (getTransaction().hasFirstChild()) {
        stack.push(getTransaction().getNodeKey());
        getTransaction().moveToFirstChild();
      }
    
      while (getTransaction().hasRightSibling()) {
        stack.push(getTransaction().getNodeKey());
        getTransaction().moveToRightSibling();
        getLastChild();
      }
    }
  }

}