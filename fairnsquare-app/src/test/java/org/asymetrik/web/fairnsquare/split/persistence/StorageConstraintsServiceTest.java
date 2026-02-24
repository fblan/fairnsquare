package org.asymetrik.web.fairnsquare.split.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Tests for storage constraints: size limit enforcement and old file cleanup. Uses test profile config:
 * max-total-size-bytes=1024, max-file-age-days=30.
 */
@QuarkusTest
class StorageConstraintsServiceTest {

    @Inject
    StorageConstraintsService storageConstraints;

    @Inject
    TenantPathResolver pathResolver;

    @BeforeEach
    void setUp() throws IOException {
        Path rootDir = pathResolver.resolveRootDirectory();
        if (Files.exists(rootDir)) {
            Files.walk(rootDir).sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException _) {
                }
            });
        }
    }

    // -------------------------------------------------------------------------
    // Size limit tests
    // -------------------------------------------------------------------------

    @Test
    void shouldAllowSaveWhenStorageIsEmpty() {
        Path newPath = pathResolver.resolve("split-new");
        assertThatCode(() -> storageConstraints.checkSizeLimitBeforeSave(newPath, 200)).doesNotThrowAnyException();
    }

    @Test
    void shouldAllowSaveWhenTotalWouldStayWithinLimit() throws IOException {
        // 500 existing + 200 new = 700 < 1024
        writeTestFile("split1", 500);
        Path newPath = pathResolver.resolve("split-new");

        assertThatCode(() -> storageConstraints.checkSizeLimitBeforeSave(newPath, 200)).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectSaveWhenTotalWouldExceedLimit() throws IOException {
        // 900 existing + 200 new = 1100 > 1024
        writeTestFile("split1", 900);
        Path newPath = pathResolver.resolve("split-new");

        assertThatThrownBy(() -> storageConstraints.checkSizeLimitBeforeSave(newPath, 200))
                .isInstanceOf(StorageLimitExceededError.class);
    }

    @Test
    void shouldNotDoubleCountExistingFileOnUpdate() throws IOException {
        // splitA occupies 800 bytes; updating it with 500 bytes → (800 - 800) + 500 = 500 < 1024
        Path splitAPath = pathResolver.resolve("splitA");
        writeFile(splitAPath, 800);

        assertThatCode(() -> storageConstraints.checkSizeLimitBeforeSave(splitAPath, 500)).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectUpdateWhenNewVersionWouldExceedLimit() throws IOException {
        // 600 bytes of other files + updating splitA (currently 100 bytes) with 800 bytes
        // → (600 + 100 - 100) + 800 = 1400 > 1024
        writeTestFile("split1", 600);
        Path splitAPath = pathResolver.resolve("splitA");
        writeFile(splitAPath, 100);

        assertThatThrownBy(() -> storageConstraints.checkSizeLimitBeforeSave(splitAPath, 800))
                .isInstanceOf(StorageLimitExceededError.class);
    }

    // -------------------------------------------------------------------------
    // Cleanup tests
    // -------------------------------------------------------------------------

    @Test
    void shouldDeleteFilesOlderThanConfiguredDays() throws IOException {
        Path oldFile = writeTestFile("old-split", 100);
        Path recentFile = writeTestFile("recent-split", 100);

        // Set old file's modification time to 60 days ago (> 30-day test limit)
        Files.setLastModifiedTime(oldFile, FileTime.from(Instant.now().minus(60, ChronoUnit.DAYS)));

        storageConstraints.cleanOldFiles();

        assertThat(Files.exists(oldFile)).isFalse();
        assertThat(Files.exists(recentFile)).isTrue();
    }

    @Test
    void shouldKeepFilesNewerThanConfiguredDays() throws IOException {
        Path file1 = writeTestFile("split1", 100);
        Path file2 = writeTestFile("split2", 100);

        storageConstraints.cleanOldFiles();

        assertThat(Files.exists(file1)).isTrue();
        assertThat(Files.exists(file2)).isTrue();
    }

    @Test
    void shouldDeleteAllFilesOlderThanLimit() throws IOException {
        Path old1 = writeTestFile("old1", 100);
        Path old2 = writeTestFile("old2", 100);
        Instant thirtyOneDaysAgo = Instant.now().minus(31, ChronoUnit.DAYS);
        Files.setLastModifiedTime(old1, FileTime.from(thirtyOneDaysAgo));
        Files.setLastModifiedTime(old2, FileTime.from(thirtyOneDaysAgo));

        storageConstraints.cleanOldFiles();

        assertThat(Files.exists(old1)).isFalse();
        assertThat(Files.exists(old2)).isFalse();
    }

    @Test
    void shouldNotFailWhenDataDirectoryDoesNotExist() {
        // Root directory was deleted in setUp — cleanOldFiles should handle gracefully
        assertThatCode(() -> storageConstraints.cleanOldFiles()).doesNotThrowAnyException();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Path writeTestFile(String splitId, int sizeBytes) throws IOException {
        Path file = pathResolver.resolve(splitId);
        writeFile(file, sizeBytes);
        return file;
    }

    private void writeFile(Path file, int sizeBytes) throws IOException {
        Files.createDirectories(file.getParent());
        Files.write(file, new byte[sizeBytes]);
    }
}
