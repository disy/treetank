/**
 * 
 */
package org.treetank.api;

import java.io.DataOutput;

import org.treetank.exception.TTIOException;

/**
 * Overall {@link INode}-Interface for the interaction with the bucket-layer. All
 * persistence functionality must be handled over this interface while all
 * node-layers interfaces inherit from this interface.
 * 
 * @author Sebastian Graf, University of Konstanz
 * 
 */
public interface INode {

    /**
     * Serializing to given dataput
     * 
     * @param pOutput
     *            to serialize to
     * @throws TTIOException
     */
    void serialize(final DataOutput pOutput) throws TTIOException;

    /**
     * Gets unique {@link INode} key.
     * This key should be set over the <code>IBucketWriteTrx.incrementNodeKey</code> for getting the correct offset
     * within retrievals.
     * 
     * @return node key
     */
    long getNodeKey();

    /**
     * Getting the persistent stored hash.
     * 
     * @return the hash of this {@link INode}
     */
    long getHash();

}
