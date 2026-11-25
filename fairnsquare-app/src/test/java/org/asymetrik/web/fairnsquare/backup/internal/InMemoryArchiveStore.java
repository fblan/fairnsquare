package org.asymetrik.web.fairnsquare.backup.internal;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

/**
 * A bucket in memory, for the tests; a test profile enables it in place of {@link S3ArchiveStore}.
 */
@Alternative
@ApplicationScoped
public class InMemoryArchiveStore implements ArchiveStore {

    private final Map<String, byte[]> objects = new ConcurrentHashMap<>();
    private volatile IOException failure;

    @Override
    public List<StoredObject> list() throws IOException {
        failIfAsked();
        return objects.entrySet().stream().map(entry -> new StoredObject(entry.getKey(), entry.getValue().length))
                .toList();
    }

    @Override
    public void put(String name, Path file) throws IOException {
        failIfAsked();
        objects.put(name, Files.readAllBytes(file));
    }

    @Override
    public InputStream open(String name) throws IOException {
        failIfAsked();
        byte[] bytes = objects.get(name);
        if (bytes == null) {
            throw new FileNotFoundException(name);
        }
        return new ByteArrayInputStream(bytes);
    }

    @Override
    public void delete(String name) throws IOException {
        failIfAsked();
        objects.remove(name);
    }

    /** The stored objects, by name. */
    public Map<String, byte[]> objects() {
        return objects;
    }

    /** Makes every call fail with this, or work again with null. */
    public void failWith(IOException failure) {
        this.failure = failure;
    }

    private void failIfAsked() throws IOException {
        if (failure != null) {
            throw failure;
        }
    }
}
