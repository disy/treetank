/**
 * 
 */
package org.treetank.node;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.treetank.api.INode;
import org.treetank.api.INodeFactory;
import org.treetank.node.delegates.NameNodeDelegate;
import org.treetank.node.delegates.NodeDelegate;
import org.treetank.node.delegates.StructNodeDelegate;
import org.treetank.node.delegates.ValNodeDelegate;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;

/**
 * @author Sebastian Graf, University of Konstanz
 * 
 */
public class NodeFactory implements INodeFactory {

    private final static NodeFactory INSTANCE = new NodeFactory();

    private NodeFactory() {
    }

    public static final NodeFactory getInstance() {
        return INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public INode deserializeNode(byte[] pSource) {
        final ByteArrayDataInput input = ByteStreams.newDataInput(pSource);
        final int kind = input.readInt();

        final NodeDelegate nodeDel = new NodeDelegate(input.readLong(),
                input.readLong(), input.readLong());
        StructNodeDelegate strucDel;
        NameNodeDelegate nameDel;
        ValNodeDelegate valDel;

        INode returnVal = null;
        switch (kind) {
        case IConstants.ELEMENT:
            strucDel = new StructNodeDelegate(nodeDel, input.readLong(),
                    input.readLong(), input.readLong(), input.readLong());
            nameDel = new NameNodeDelegate(nodeDel, input.readInt(),
                    input.readInt());

            final List<Long> attrKeys = new ArrayList<Long>();
            final List<Long> namespKeys = new ArrayList<Long>();

            // Attributes getting
            int attrCount = input.readInt();
            for (int i = 0; i < attrCount; i++) {
                attrKeys.add(input.readLong());
            }

            // Namespace getting
            int nsCount = input.readInt();
            for (int i = 0; i < nsCount; i++) {
                namespKeys.add(input.readLong());
            }

            returnVal = new ElementNode(nodeDel, strucDel, nameDel, attrKeys,
                    namespKeys);
        case IConstants.TEXT:
            // Struct Node are 4*8 bytes
            strucDel = new StructNodeDelegate(nodeDel, input.readLong(),
                    input.readLong(), input.readLong(), input.readLong());
            // Val is the rest
            valDel = new ValNodeDelegate(nodeDel, Arrays.copyOfRange(pSource,
                    24, pSource.length));
            returnVal = new TextNode(nodeDel, strucDel, valDel);
        case IConstants.ROOT:
            // Struct Node are 4*8 bytes
            strucDel = new StructNodeDelegate(nodeDel, input.readLong(),
                    input.readLong(), input.readLong(), input.readLong());
            returnVal = new DocumentRootNode(nodeDel, strucDel);
        case IConstants.ATTRIBUTE:
            // Name Node are 2*4 bytes
            nameDel = new NameNodeDelegate(nodeDel, input.readInt(),
                    input.readInt());
            // Val is the rest
            valDel = new ValNodeDelegate(nodeDel, Arrays.copyOfRange(pSource,
                    8, pSource.length));
            returnVal = new AttributeNode(nodeDel, nameDel, valDel);
        case IConstants.NAMESPACE:
            // Name Node are 2*4 bytes
            nameDel = new NameNodeDelegate(nodeDel, input.readInt(),
                    input.readInt());
            returnVal = new NamespaceNode(nodeDel, nameDel);
            break;
        case IConstants.DELETE:
            returnVal = new DeletedNode(nodeDel);
            break;
        default:
            throw new IllegalStateException(
                    "Invalid Kind of Node. Something went wrong in the serialization/deserialization");
        }
        return returnVal;
    }

}
