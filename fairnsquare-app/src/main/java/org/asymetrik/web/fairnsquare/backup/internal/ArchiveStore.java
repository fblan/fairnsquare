package org.asymetrik.web.fairnsquare.backup.internal;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

/**
 * Where the encrypted archives are kept: an S3 bucket ({@link S3ArchiveStore}).
 */
public interface ArchiveStore {

    /** A stored object. */
    record StoredObject(String name, long sizeBytes) {
    }

    /** The objects under the store's prefix, in any order; names are relative to the prefix. */
    List<StoredObject> list() throws IOException;

    void put(String name, Path file) throws IOException;

    /**
     * @throws java.io.FileNotFoundException
     *             when there is no such object
     */
    InputStream open(String name) throws IOException;

    void delete(String name) throws IOException;
}
