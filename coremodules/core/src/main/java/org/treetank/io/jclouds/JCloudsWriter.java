/**
 * 
 */
package org.treetank.io.jclouds;

import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.blobstore.domain.BlobBuilder;
import org.treetank.exception.TTByteHandleException;
import org.treetank.exception.TTException;
import org.treetank.exception.TTIOException;
import org.treetank.io.IBackendWriter;
import org.treetank.io.bytepipe.IByteHandler.IByteHandlerPipeline;
import org.treetank.page.PageFactory;
import org.treetank.page.interfaces.IPage;

/**
 * @author Sebastian Graf, University of Konstanz
 * 
 */
public class JCloudsWriter implements IBackendWriter {

    /** Delegate for reader. */
    private final JCloudsReader mReader;

    public JCloudsWriter(BlobStore pBlobStore, PageFactory pFac, IByteHandlerPipeline pByteHandler,
        String pResourceName) throws TTException {
        mReader = new JCloudsReader(pBlobStore, pFac, pByteHandler, pResourceName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IPage read(long pKey) throws TTIOException, TTByteHandleException {
        return mReader.read(pKey);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(IPage pPage) throws TTIOException, TTByteHandleException {
        final byte[] rawPage = pPage.getByteRepresentation();
        final byte[] decryptedPage = mReader.mByteHandler.serialize(rawPage);
        BlobBuilder blobbuilder = mReader.mBlobStore.blobBuilder(Long.toString(pPage.getPageKey()));
        Blob blob = blobbuilder.build();
        blob.setPayload(decryptedPage);
        mReader.mBlobStore.putBlob(mReader.mResourceName, blob);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws TTIOException {
        mReader.close();
    }

}
