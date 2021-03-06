/**
 * 
 */
package org.treetank.revisioning;


import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.treetank.testutil.CoreTestHelper.getDataBucket;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.treetank.bucket.DataBucket;
import org.treetank.bucket.IConstants;
import org.treetank.exception.TTByteHandleException;
import org.treetank.io.LogValue;
import org.treetank.testutil.CoreTestHelper;


/**
 * Test for {@link IRevisioning}-interface.
 * 
 * @author Sebastian Graf, University of Konstanz
 * 
 */
public class IRevisioningTest {

    /**
     * @throws java.lang.Exception
     */
    @BeforeMethod
    public void setUp () throws Exception {
        CoreTestHelper.deleteEverything();

    }

    /**
     * @throws java.lang.Exception
     */
    @AfterMethod
    public void tearDown () throws Exception {
        CoreTestHelper.deleteEverything();
    }

    /**
     * Test method for {@link org.treetank.revisioning.IRevisioning#combineBuckets(org.treetank.bucket.DataBucket[])} .
     * This test just takes two versions and checks if the version-counter is interpreted correctly.
     * 
     * @param pRevisioningClass class for the revisioning approaches
     * @param pRevisioning the different revisioning approaches
     * @param pRevisionCheckerClass class for the revisioning-check approaches
     * @param pRevisionChecker the different revisioning-check approaches
     * @param pDataGeneratorClass class for data-generator
     * @param pDataGenerator different data-generators
     */
    @Test (dataProvider = "instantiateVersioning")
    public void testCombineBucketsForModification (IRevisioning pRevisioning, IRevisionChecker pRevisionChecker, IDataBucketGenerator pDataGenerator) {

        // test for full-dumps-including versionings
        // if revision is not SlidingSnapshot (since
        // SlidingSnapshot is not working with entire
        // full-dump...
        if (!(pRevisioning instanceof SlidingSnapshot)) {
            // ...get the data buckets for not full-dump test and...
            final DataBucket[] buckets = pDataGenerator.generateDataBuckets();
            // ..recombine them...
            final LogValue bucket = pRevisioning.combineBucketsForModification(buckets.length, 0, buckets, true);
            // ...and check them suitable to the versioning approach
            pRevisionChecker.checkCompleteBucketsForModification(bucket, buckets, true);
        }

        // test for non-full-dumps-including versionings
        // check if revision is not FullDump (since FullDump must always
        // be used within FullDump)
        // and...
        if (!(pRevisioning instanceof FullDump)) {
            // ...get the data buckets for full-dump test and...
            final DataBucket[] buckets = pDataGenerator.generateDataBuckets();
            // ..recombine them...
            final LogValue bucket = pRevisioning.combineBucketsForModification(buckets.length - 1, 0, buckets, false);
            // ...and check them suitable to the versioning approach
            pRevisionChecker.checkCompleteBucketsForModification(bucket, buckets, false);
        }
    }

    /**
     * Test method for
     * {@link org.treetank.revisioning.IRevisioning#combineBucketsForModification(int, long, DataBucket[], boolean)} .
     * This test just takes two versions and checks if the version-counter is interpreted correctly.
     * 
     * @param pRevisioningClass class for the revisioning approaches
     * @param pRevisioning the different revisioning approaches
     * @param pRevisionCheckerClass class for the revisioning-check approaches
     * @param pRevisionChecker the different revisioning-check approaches
     * @param pDataGeneratorClass class for data-generator
     * @param pDataGenerator different data-generators
     */
    @Test (dataProvider = "instantiateVersioning")
    public void testCombineBuckets (IRevisioning pRevisioning, IRevisionChecker pRevisionChecker, IDataBucketGenerator pDataGenerator) {
        // for all revision-approaches...
        // ...get the data buckets and...
        final DataBucket[] buckets = pDataGenerator.generateDataBuckets();
        // ..and recombine them...
        final DataBucket bucket = pRevisioning.combineBuckets(buckets);
        // ...and check them suitable to the versioning approach
        pRevisionChecker.checkCompleteBuckets(bucket, buckets);
    }

    /**
     * Providing different implementations of the {@link IRevisioning} as Dataprovider to the test class.
     * 
     * @return different classes of the {@link IRevisioning} and <code>IRevisionChecker</code>
     * @throws TTByteHandleException
     */
    @DataProvider (name = "instantiateVersioning")
    public Object[][] instantiateVersioning () throws TTByteHandleException {

        Object[][] returnVal = { { new FullDump(), new IRevisionChecker() {
            @Override
            public void checkCompleteBuckets (DataBucket pComplete, DataBucket[] pFragments) {
                // Check only the last version since the
                // complete dump consists out of the last
                // version within the FullDump
                for (int i = 0; i < pComplete.getDatas().length; i++) {
                    assertEquals("Check for FullDump failed.", pFragments[0].getData(i), pComplete.getData(i));
                }
            }

            @Override
            public void checkCompleteBucketsForModification (LogValue pComplete, DataBucket[] pFragments, boolean pFullDump) {
                // must always be true since it is the Fulldump
                assertTrue(pFullDump);
                // Check only the last version since the
                // complete dump consists out of the last
                // version within the FullDump
                DataBucket complete = (DataBucket) pComplete.getComplete();
                DataBucket modified = (DataBucket) pComplete.getModified();
                for (int i = 0; i < complete.getDatas().length; i++) {
                    assertEquals("Check for FullDump failed.", pFragments[0].getData(i), complete.getData(i));
                    assertEquals("Check for FullDump failed.", pFragments[0].getData(i), modified.getData(i));
                }

            }
        }, // Checker for FullDump
        new IDataBucketGenerator() {
            @Override
            public DataBucket[] generateDataBuckets () {
                DataBucket[] returnVal = { getDataBucket(0, IConstants.CONTENT_COUNT, 0, -1) };
                return returnVal;
            }
        } },

                // INCREMENTAL
        { new Incremental(),
                // Checker for Incremental
        new IRevisionChecker() {
            @Override
            public void checkCompleteBuckets (DataBucket pComplete, DataBucket[] pFragments) {
                // Incrementally iterate through all buckets to
                // reconstruct the complete bucket.
                int j = 0;
                // taking first the fragments into account and..
                for (int i = 0; i < pFragments.length - 1; i++) {
                    for (j = i * 2; j < (i * 2) + 2; j++) {
                        assertEquals("Check for Incremental failed.", pFragments[i].getData(j), pComplete.getData(j));
                    }
                }
                // ...fill the test up with the rest
                for (; j < pComplete.getDatas().length; j++) {
                    assertEquals("Check for Incremental failed.", pFragments[pFragments.length - 1].getData(j), pComplete.getData(j));
                }
            }

            @Override
            public void checkCompleteBucketsForModification (LogValue pComplete, DataBucket[] pFragments, boolean pFullDump) {
                DataBucket complete = (DataBucket) pComplete.getComplete();
                DataBucket modified = (DataBucket) pComplete.getModified();
                int j = 0;
                // taking first the fragments into account and..
                for (int i = 0; i < pFragments.length - 1; i++) {
                    for (j = i * 2; j < (i * 2) + 2; j++) {
                        assertEquals("Check for Incremental failed.", pFragments[i].getData(j), complete.getData(j));
                        if (pFullDump) {
                            assertEquals("Check for Incremental failed.", pFragments[i].getData(j), modified.getData(j));
                        } else {
                            assertNull(modified.getData(j));
                        }
                    }
                }
                // ...fill the test up with the rest
                for (; j < complete.getDatas().length; j++) {
                    assertEquals("Check for Incremental failed.", pFragments[pFragments.length - 1].getData(j), complete.getData(j));
                    if (pFullDump) {
                        assertEquals("Check for Incremental failed.", pFragments[pFragments.length - 1].getData(j), modified.getData(j));
                    } else {
                        assertNull(modified.getData(j));
                    }
                }
            }
        },
                // Checker for Incremental
        new IDataBucketGenerator() {
            @Override
            public DataBucket[] generateDataBuckets () {
                // initialize all fragments first...
                final DataBucket[] buckets = new DataBucket[63];
                // fill all buckets up to number of restores
                // first...
                for (int j = 0; j < 62; j++) {
                    // filling databuckets from end to start
                    // with 2 elements each slot
                    buckets[j] = getDataBucket(j * 2, (j * 2) + 2, buckets.length - j - 1, buckets.length - j - 2);
                }
                // set a fulldump as last revision
                buckets[62] = getDataBucket(0, 128, 0, -1);
                return buckets;
            }
        } },
                // DIFFERENTIAL
        { new Differential(),
                // Checker for Differential
        new IRevisionChecker() {
            @Override
            public void checkCompleteBuckets (DataBucket pComplete, DataBucket[] pFragments) {
                int j = 0;
                // Take the last version first, to get the data
                // out there...
                for (j = 0; j < 32; j++) {
                    assertEquals("Check for Differential failed.", pFragments[0].getData(j), pComplete.getData(j));
                }
                // ...and iterate through the first version
                // afterwards for the rest of the
                // reconstruction
                for (; j < pComplete.getDatas().length; j++) {
                    assertEquals(new StringBuilder("Check for Differential: ").append(" failed.").toString(), pFragments[pFragments.length - 1].getData(j), pComplete.getData(j));
                }
            }

            @Override
            public void checkCompleteBucketsForModification (LogValue pComplete, DataBucket[] pFragments, boolean pFullDump) {
                DataBucket complete = (DataBucket) pComplete.getComplete();
                DataBucket modified = (DataBucket) pComplete.getModified();
                int j = 0;
                // Take the last version first, to get the data
                // out there...
                for (j = 0; j < 32; j++) {
                    assertEquals("Check for Differential failed.", pFragments[0].getData(j), complete.getData(j));
                    assertEquals("Check for Differential failed.", pFragments[0].getData(j), modified.getData(j));
                }
                // ...and iterate through the first version
                // afterwards for the rest of the
                // reconstruction
                for (; j < complete.getDatas().length; j++) {
                    assertEquals("Check for Differential failed.", pFragments[1].getData(j), complete.getData(j));
                    if (pFullDump) {
                        assertEquals("Check for Differential failed.", pFragments[1].getData(j), modified.getData(j));
                    } else {
                        assertNull(modified.getData(j));
                    }
                }
            }
        },
                // Generator for Differential
        new IDataBucketGenerator() {
            @Override
            public DataBucket[] generateDataBuckets () {
                // initialize all fragments first...
                final DataBucket[] buckets = new DataBucket[2];
                // setting one buckets to a fragment only...
                buckets[0] = getDataBucket(0, 32, 0, -1);
                // ..and the other as entire fulldump
                buckets[1] = getDataBucket(0, 128, 1, 0);
                return buckets;
            }
        } },
                // SLIDING
        { new SlidingSnapshot(), // check for Sliding Snapshot
        new IRevisionChecker() {
            @Override
            public void checkCompleteBuckets (DataBucket pComplete, DataBucket[] pFragments) {
                for (int i = 0; i < pFragments.length; i++) {
                    for (int j = i * 2; j < (i * 2) + 2; j++) {
                        assertEquals("Check for Sliding Snapshot failed.", pFragments[i].getData(j), pComplete.getData(j));
                    }
                }
            }

            @Override
            public void checkCompleteBucketsForModification (LogValue pComplete, DataBucket[] pFragments, boolean fullDump) {
                DataBucket complete = (DataBucket) pComplete.getComplete();
                DataBucket modified = (DataBucket) pComplete.getModified();
                int j = 0;
                // Taking all fragments in the middle, only
                // checking against
                // complete-fragment and..
                for (int i = 0; i < pFragments.length - 1; i++) {
                    for (j = i * 2; j < (i * 2) + 2; j++) {
                        assertEquals("Check for Sliding Snapshot failed.", pFragments[i].getData(j), complete.getData(j));
                    }
                }
                // ..at last, checking the last fragment,
                // against write- and read-fragment
                for (; j < complete.getDatas().length; j++) {
                    assertEquals("Check for Sliding Snapshot failed.", pFragments[pFragments.length - 1].getData(j), complete.getData(j));
                    assertEquals("Check for Sliding Snapshot failed.", pFragments[pFragments.length - 1].getData(j), modified.getData(j));
                }

            }
        },
                // Checker for Sliding Snapshot
        new IDataBucketGenerator() {
            @Override
            public DataBucket[] generateDataBuckets () {
                // initialize all fragments first...
                final DataBucket[] buckets = new DataBucket[64];
                // fill all buckets up to number of restores
                // first...
                for (int j = 0; j < 64; j++) {
                    // filling databuckets from end to start
                    // with 2 elements each slot
                    buckets[j] = getDataBucket(j * 2, (j * 2) + 2, buckets.length - j - 1, buckets.length - j - 2);
                }
                return buckets;
            }
        }
        } };
        return returnVal;
    }

    /**
     * Interface to check reconstructed buckets.
     * 
     * @author Sebastian Graf, University of Konstanz
     * 
     */
    interface IRevisionChecker {
        void checkCompleteBuckets (DataBucket pComplete, DataBucket[] pFragments);

        void checkCompleteBucketsForModification (LogValue pComplete, DataBucket[] pFragments, boolean fullDump);
    }

    /**
     * Data Bucket Generator for new DataBuckets.
     * 
     * @author Sebastian Graf, University of Konstanz
     * 
     */
    interface IDataBucketGenerator {
        DataBucket[] generateDataBuckets ();
    }

}
