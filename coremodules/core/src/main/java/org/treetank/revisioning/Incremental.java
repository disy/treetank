/**
 * 
 */
package org.treetank.revisioning;

import static com.google.common.base.Objects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;

import org.treetank.log.LogValue;
import org.treetank.page.NodePage;

/**
 * Incremental versioning of {@link NodePage}s.
 * 
 * @author Sebastian Graf, University of Konstanz
 * 
 */
public class Incremental implements IRevisioning {

    /**
     * {@inheritDoc}
     */
    @Override
    public NodePage combinePages(final NodePage[] pages) {
        checkArgument(pages.length > 0, "At least one Nodepage must be provided");
        // create entire page..
        final NodePage returnVal = new NodePage(pages[0].getPageKey(), pages[0].getLastPagePointer());
        // ...iterate through the nodes and check if it is stored..
        for (int i = 0; i < pages[0].getNodes().length; i++) {
            boolean pageSkip = false;
            // ... form the newest version to the oldest one..
            for (int j = 0; !pageSkip && j < pages.length; j++) {
                // if the node is not set yet but existing in the current version..
                if (pages[j].getNode(i) != null) {
                    // ...break out the loop the next time and..
                    pageSkip = true;
                    // ...set it
                    returnVal.setNode(i, pages[j].getNode(i));
                }

            }
        }
        return returnVal;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LogValue combinePagesForModification(int pRevisionsToRestore, long pNewPageKey, NodePage[] pages,
        boolean pFullDump) {
        checkArgument(pages.length > 0, "At least one Nodepage must be provided");
        // create pages for container..
        final NodePage[] returnVal =
            {
                new NodePage(pages[0].getPageKey(), pages[0].getLastPagePointer()),
                new NodePage(pNewPageKey, pages[0].getPageKey())
            };
        // ...iterate through the nodes and check if it is stored..
        for (int i = 0; i < pages[0].getNodes().length; i++) {
            boolean pageSkip = false;
            // ... form the newest version to the oldest one..
            for (int j = 0; !pageSkip && j < pages.length; j++) {
                // if the node is not set yet but existing in the current version..
                if (pages[j].getNode(i) != null) {
                    // ...break out the loop the next time and..
                    pageSkip = true;
                    // ...set it to the read-cache and..
                    returnVal[0].setNode(i, pages[j].getNode(i));
                    // ...if a fulldump becomes necessary, set it to the write cache as well.
                    if (pFullDump) {
                        returnVal[1].setNode(i, pages[j].getNode(i));
                    }
                }
            }
        }
        // return the container
        return new LogValue(returnVal[0], returnVal[1]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return toStringHelper(this).toString();
    }

}
