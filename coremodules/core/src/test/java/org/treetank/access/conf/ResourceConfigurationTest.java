/**
 * 
 */
package org.treetank.access.conf;

import static org.testng.AssertJUnit.assertEquals;

import java.io.File;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;
import org.treetank.ModuleFactory;
import org.treetank.TestHelper;
import org.treetank.access.conf.ResourceConfiguration.IResourceConfigurationFactory;
import org.treetank.exception.TTException;

import com.google.inject.Inject;

/**
 * Test for {@link ResourceConfiguration}.
 * 
 * @author Sebastian Graf, University of Konstanz
 * 
 */
@Guice(moduleFactory = ModuleFactory.class)
public class ResourceConfigurationTest {

    @Inject
    private IResourceConfigurationFactory mResourceConfig;

    @BeforeMethod
    public void setUp() throws TTException {
        TestHelper.closeEverything();
        TestHelper.deleteEverything();
        TestHelper.PATHS.PATH1.getFile().mkdirs();
    }

    @AfterMethod
    public void tearDown() throws TTException {
        TestHelper.closeEverything();
        TestHelper.deleteEverything();
    }

    /**
     * Test method for
     * {@link org.treetank.access.conf.ResourceConfiguration#serialize(org.treetank.access.conf.ResourceConfiguration)}
     * and {@link org.treetank.access.conf.ResourceConfiguration#deserialize(java.io.File)}.
     */
    @Test
    public void testDeSerialize() throws Exception {
        ResourceConfiguration conf =
            mResourceConfig.create(TestHelper.PATHS.PATH1.getFile(), TestHelper.RESOURCENAME, 10);
        ResourceConfiguration.serialize(conf);
        ResourceConfiguration serializedConf =
            ResourceConfiguration.deserialize(new File(TestHelper.PATHS.PATH1.getFile(),
                TestHelper.RESOURCENAME));
        assertEquals(conf, serializedConf);

    }
}
