/**
 * 
 */
package org.treetank.access.conf;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.security.Key;
import java.util.Properties;

import javax.crypto.spec.SecretKeySpec;

import org.jclouds.Constants;
import org.jclouds.filesystem.reference.FilesystemConstants;
import org.treetank.exception.TTIOException;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

/**
 * Standard Module defining standard settings.
 * 
 * @author Sebastian Graf, University of Konstanz
 * 
 */
public class StandardSettings {

    private static byte[] keyValue = new byte[] {
        'k', 'k', 'k', 'k', 'k', 'k', 'k', 'k', 'k', 'k', 'k', 'k', 'k', 'k', 'k', 'k'
    };
    public static final Key KEY = new SecretKeySpec(keyValue, "AES");
    public static final HashFunction HASHFUNC = Hashing.sha512();

    public static Properties getProps(final String pathToStorage, final String resource) throws TTIOException {
        Properties properties = new Properties();
        properties.setProperty(ConstructorProps.STORAGEPATH, pathToStorage);
        properties.setProperty(ConstructorProps.RESOURCE, resource);
        properties.setProperty(ConstructorProps.RESOURCEPATH, FileSystems.getDefault().getPath(pathToStorage,
            StorageConfiguration.Paths.Data.getFile().getName(), resource).toString());
        properties.setProperty(ConstructorProps.NUMBERTORESTORE, Integer.toString(4));

        properties.setProperty(ConstructorProps.JCLOUDSTYPE, "filesystem");
        
        //properties.setProperty(ConstructorProps.JCLOUDSTYPE, "imagestore");
        //properties.setProperty(ImageStoreConstants.PROPERTY_IMAGEHOSTER, "org.jclouds.imagestore.imagehoster.facebook.ImageHostFacebook");
        //properties.setProperty(ImageStoreConstants.PROPERTY_BYTEPAINTER, "org.jclouds.imagestore.imagegenerator.bytepainter.BytesToImagePainter");
        //properties.setProperty(ImageStoreConstants.PROPERTY_ENCODER, "org.jclouds.imagestore.imagegenerator.IEncoder$DummyEncoder");
 //properties.setProperty(ImageStoreConstants.PROPERTY_LAYERS, "4");

        // Path not to main storage but to any to simulate remote cloud location.

        properties.setProperty(FilesystemConstants.PROPERTY_BASEDIR, FileSystems.getDefault().getPath(
            pathToStorage, StorageConfiguration.Paths.Data.getFile().getName(), resource,
            ResourceConfiguration.Paths.Data.getFile().getName()).toString());

        String[] awsCredentials = getCredentials();
        if (awsCredentials.length == 0) {
            properties.setProperty(Constants.PROPERTY_CREDENTIAL, "test");
        } else {
            properties.setProperty(Constants.PROPERTY_IDENTITY, awsCredentials[0]);
            properties.setProperty(Constants.PROPERTY_CREDENTIAL, awsCredentials[1]);
        }

        // Class name for painter for imagehost
        // properties.setProperty(ImageStoreConstants.PROPERTY_BYTEPAINTER,
        // "org.jclouds.imagestore.imagegenerator.bytepainter.HexadecimalBytesToImagePainter");
        // Class name for imagehost
        // properties.setProperty(ImageStoreConstants.PROPERTY_IMAGEHOSTER,
        // "org.jclouds.imagestore.imagehoster.file.ImageHostFile");
        // properties.setProperty(ImageStoreConstants.PROPERTY_IMAGEHOSTER,
        // "org.jclouds.imagestore.imagehoster.flickr.ImageHostFlickr");

        return properties;

    }

    private static String[] getCredentials() {
        File userStore =
            new File(System.getProperty("user.home"), new StringBuilder(".credentials")
                .append(File.separator).append("aws.properties").toString());
        if (!userStore.exists()) {
            return new String[0];
        } else {
            Properties props = new Properties();
            try {
                props.load(new FileReader(userStore));
                return new String[] {
                    props.getProperty("access"), props.getProperty("secret")
                };

            } catch (IOException exc) {
                throw new RuntimeException(exc);
            }
        }

    }

}
