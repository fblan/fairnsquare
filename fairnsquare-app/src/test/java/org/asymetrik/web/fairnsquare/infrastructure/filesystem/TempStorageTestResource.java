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
 * Usage: annotate the test class with {@code @QuarkusTestResource(TempStorageTestResource.class)}. Multiple test
 * classes using this resource without {@code restrictToAnnotatedClass} share a single Quarkus context and a single temp
 * directory, which is safe as long as each test creates resources with unique IDs and does not enumerate all stored
 * files.
 * <p>
 * Use {@code restrictToAnnotatedClass = true} only when the class requires a custom {@code initArgs} (e.g.
 * {@code maxFileCount}) that would conflict with other classes.
 * <p>
 * Optional init parameter {@code maxFileCount}: when set, overrides {@code fairnsquare.storage.max-file-count} for the
 * annotated class. Useful for classes that test file-count limit behaviour with a small threshold.
 */
public class TempStorageTestResource implements QuarkusTestResourceLifecycleManager {

    private Path tempDir;
    private String maxFileCount;

    @Override
    public void init(Map<String, String> initArgs) {
        maxFileCount = initArgs.get("maxFileCount");
    }

    @Override
    public Map<String, String> start() {
        try {
            tempDir = Files.createTempDirectory("fairnsquare-test-");
            Map<String, String> config = new HashMap<>();
            config.put("fairnsquare.data.path", tempDir.toString());
            if (maxFileCount != null) {
                config.put("fairnsquare.storage.max-file-count", maxFileCount);
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
