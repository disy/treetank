package org.treetank.access.conf;

import java.security.Key;

import org.treetank.access.conf.ResourceConfiguration.IResourceConfigurationFactory;
import org.treetank.access.conf.SessionConfiguration.ISessionConfigurationFactory;
import org.treetank.api.IMetaEntryFactory;
import org.treetank.api.INodeFactory;
import org.treetank.io.IBackend;
import org.treetank.io.IBackend.IBackendFactory;
import org.treetank.io.berkeley.BerkeleyStorage;
import org.treetank.io.bytepipe.ByteHandlerPipeline;
import org.treetank.io.bytepipe.IByteHandler;
import org.treetank.io.bytepipe.IByteHandler.IByteHandlerPipeline;
import org.treetank.io.jclouds.JCloudsStorage;
import org.treetank.revisioning.IRevisioning;
import org.treetank.revisioning.SlidingSnapshot;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;

/**
 * Builder-like construction to generate AbstractModules based on former set values;
 * 
 * @author Sebastian Graf, University of Konstanz.
 * 
 */
public class ModuleSetter {

    /** Class for NodeFactory. */
    private Class<? extends INodeFactory> mNodeFacClass;
    /** Class for MetaFactory. */
    private Class<? extends IMetaEntryFactory> mMetaFacClass;
    /** Class for Revision. */
    private Class<? extends IRevisioning> mRevisioningClass = SlidingSnapshot.class;
    /** Class for IBackend. */
    private Class<? extends IBackend> mBackend = BerkeleyStorage.class;
    /** Instance for ByteHandler. */
    private IByteHandlerPipeline mByteHandler = new ByteHandlerPipeline();
    /** Instance for Key. */
    private Key mKey = StandardSettings.KEY;

    /**
     * Setting an {@link INodeFactory}-class
     * 
     * @param pNodeFac
     *            to be set
     * @return the current ModuleSetter
     */
    public ModuleSetter setNodeFacClass(final Class<? extends INodeFactory> pNodeFac) {
        this.mNodeFacClass = pNodeFac;
        return this;
    }

    /**
     * Setting an {@link IMetaEntryFactory}-class
     * 
     * @param pMetaFac
     *            to be set
     * @return the current ModuleSetter
     */
    public ModuleSetter setMetaFacClass(final Class<? extends IMetaEntryFactory> pMetaFac) {
        this.mMetaFacClass = pMetaFac;
        return this;
    }

    /**
     * Setting an {@link IRevisioning}-class
     * 
     * @param pRevision
     *            to be set
     * @return the current ModuleSetter
     */
    public ModuleSetter setRevisioningClass(final Class<? extends IRevisioning> pRevision) {
        this.mRevisioningClass = pRevision;
        return this;
    }

    /**
     * Setting an {@link IBackend}-class
     * 
     * @param pBackend
     *            to be set
     * @return the current ModuleSetter
     */
    public ModuleSetter setBackendClass(final Class<? extends IBackend> pBackend) {
        this.mBackend = pBackend;
        return this;
    }

    /**
     * Setting an {@link IByteHandler}-instance.
     * 
     * @param pByteHandler
     *            to be set
     * @return the current ModuleSetter
     */
    public ModuleSetter setIByteHandlerInstance(final IByteHandlerPipeline pByteHandler) {
        this.mByteHandler = pByteHandler;
        return this;
    }

    /**
     * Setting an {@link Key}-instance.
     * 
     * @param pKey
     *            to be set
     * @return the current ModuleSetter
     */
    public ModuleSetter setKeyInstance(final Key pKey) {
        this.mKey = pKey;
        return this;
    }

    /**
     * Creating an Guice Module based on the parameters set.
     * 
     * @return the {@link AbstractModule} to be set
     */
    public AbstractModule createModule() {
        return new AbstractModule() {
            @Override
            protected void configure() {
                bind(INodeFactory.class).to(mNodeFacClass);
                bind(IMetaEntryFactory.class).to(mMetaFacClass);
                bind(IRevisioning.class).to(mRevisioningClass);
                bind(IByteHandlerPipeline.class).toInstance(mByteHandler);
                install(new FactoryModuleBuilder().implement(IBackend.class, mBackend).build(
                    IBackendFactory.class));
                install(new FactoryModuleBuilder().build(IResourceConfigurationFactory.class));
                bind(Key.class).toInstance(mKey);
                install(new FactoryModuleBuilder().build(ISessionConfigurationFactory.class));
            }
        };
    }

}
