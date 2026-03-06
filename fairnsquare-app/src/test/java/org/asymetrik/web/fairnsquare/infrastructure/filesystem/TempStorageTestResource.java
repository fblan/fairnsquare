package org.asymetrik.web.fairnsquare.infrastructure.filesystem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

/**
 * Quarkus test resource that creates a fresh temporary directory before each annotated test class and deletes it
 * afterwards. Configures {@code fairnsquare.data.path} to the temp directory, giving each test class complete storage
 * isolation and enabling parallel test class execution.
 * <p>
 * Usage: annotate the test class with
 * {@code @QuarkusTestResource(value = TempStorageTestResource.class, restrictToAnnotatedClass = true)}.
 * <p>
 * Optional init parameter {@code maxStorageBytes}: when set, overrides {@code fairnsquare.storage.max-total-size-bytes}
 * for the annotated class. Useful for classes that test storage-limit behaviour with specific byte thresholds.
 */
public class TempStorageTestResource implements QuarkusTestResourceLifecycleManager {

    private Path tempDir;
    private String maxStorageBytes;

    @Override
    public void init(Map<String, String> initArgs) {
        maxStorageBytes = initArgs.get("maxStorageBytes");
    }

    @Override
    public Map<String, String> start() {
        try {
            tempDir = Files.createTempDirectory("fairnsquare-test-");
            Map<String, String> config = new HashMap<>();
            config.put("fairnsquare.data.path", tempDir.toString());
            if (maxStorageBytes != null) {
                config.put("fairnsquare.storage.max-total-size-bytes", maxStorageBytes);
            }
            return config;
        } catch (IOException e) {
            throw new RuntimeException("Failed to create temp storage directory for tests", e);
        }
    }

    @Override
    public void stop() {
        if (tempDir != null && Files.exists(tempDir)) {
            try {
                Files.walk(tempDir).sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException _) {
                    }
                });
            } catch (IOException _) {
            }
        }
    }
}
