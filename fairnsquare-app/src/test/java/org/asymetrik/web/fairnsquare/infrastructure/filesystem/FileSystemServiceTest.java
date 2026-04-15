package org.asymetrik.web.fairnsquare.infrastructure.filesystem;

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

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.ResourceArg;
import io.quarkus.test.junit.QuarkusTest;

import org.asymetrik.web.fairnsquare.infrastructure.filesystem.internal.StorageFileCountLimitExceededError;
import org.asymetrik.web.fairnsquare.infrastructure.filesystem.internal.StorageFileSizeLimitExceededError;
import org.asymetrik.web.fairnsquare.infrastructure.filesystem.StorageStats;

/**
 * Tests for {@link FileSystemService}: file-count limit enforcement, single-file-size limit enforcement, old file
 * cleanup, and storage stats. Uses a max-file-count of 3 (pinned via {@code maxFileCount} init parameter) so that count
 * thresholds in these tests remain accurate regardless of the global test-profile default.
 */
@QuarkusTest
@QuarkusTestResource(value = TempStorageTestResource.class, restrictToAnnotatedClass = true, initArgs = @ResourceArg(name = "maxFileCount", value = "3"))
class FileSystemServiceTest {

    @Inject
    FileSystemService fileSystemService;

    @ConfigProperty(name = "fairnsquare.data.path")
    String dataPath;

    @BeforeEach
    void setUp() throws IOException {
        Path rootDir = Path.of(dataPath);
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
    // File count limit tests (via saveFile)
    // -------------------------------------------------------------------------

    @Test
    void shouldAllowSaveWhenStorageIsEmpty() {
        assertThatCode(() -> fileSystemService.saveFile(new Filename("split-new.zip"), new byte[10]))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldAllowSaveWhenUnderCountLimit() {
        fileSystemService.saveFile(new Filename("split1.zip"), new byte[10]);
        fileSystemService.saveFile(new Filename("split2.zip"), new byte[10]);

        assertThatCode(() -> fileSystemService.saveFile(new Filename("split3.zip"), new byte[10]))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectNewFileWhenCountLimitReached() {
        fileSystemService.saveFile(new Filename("split1.zip"), new byte[10]);
        fileSystemService.saveFile(new Filename("split2.zip"), new byte[10]);
        fileSystemService.saveFile(new Filename("split3.zip"), new byte[10]);

        assertThatThrownBy(() -> fileSystemService.saveFile(new Filename("split4.zip"), new byte[10]))
                .isInstanceOf(StorageFileCountLimitExceededError.class);
    }

    @Test
    void shouldAllowUpdateWhenCountLimitReached() {
        fileSystemService.saveFile(new Filename("split1.zip"), new byte[10]);
        fileSystemService.saveFile(new Filename("split2.zip"), new byte[10]);
        fileSystemService.saveFile(new Filename("split3.zip"), new byte[10]);

        assertThatCode(() -> fileSystemService.saveFile(new Filename("split1.zip"), new byte[20]))
                .doesNotThrowAnyException();
    }

    // -------------------------------------------------------------------------
    // Single file size limit tests (via saveFile)
    // -------------------------------------------------------------------------

    @Test
    void shouldRejectFileExceedingSizeLimit() {
        // max-file-size-bytes is 524288 in the test profile; use a clearly oversized payload
        byte[] oversized = new byte[600_000];

        assertThatThrownBy(() -> fileSystemService.saveFile(new Filename("big.zip"), oversized))
                .isInstanceOf(StorageFileSizeLimitExceededError.class);
    }

    // -------------------------------------------------------------------------
    // Storage stats tests
    // -------------------------------------------------------------------------

    @Test
    void shouldReturnZeroStatsWhenStorageIsEmpty() {
        StorageStats stats = fileSystemService.computeStorageStats();

        assertThat(stats.fileCount()).isEqualTo(0);
        assertThat(stats.usedBytes()).isEqualTo(0);
        assertThat(stats.maxFileCount()).isEqualTo(3);
        assertThat(stats.remainingFileCount()).isEqualTo(3);
    }

    @Test
    void shouldReturnCorrectStatsWhenFilesExist() {
        fileSystemService.saveFile(new Filename("split1.zip"), new byte[10]);
        fileSystemService.saveFile(new Filename("split2.zip"), new byte[10]);

        StorageStats stats = fileSystemService.computeStorageStats();

        assertThat(stats.fileCount()).isEqualTo(2);
        assertThat(stats.usedBytes()).isEqualTo(20);
        assertThat(stats.maxFileCount()).isEqualTo(3);
        assertThat(stats.remainingFileCount()).isEqualTo(1);
    }

    @Test
    void shouldFormatStatsAsHumanReadableString() {
        fileSystemService.saveFile(new Filename("split1.zip"), new byte[10]);

        StorageStats stats = fileSystemService.computeStorageStats();

        assertThat(stats.toString()).contains("fileCount=1");
        assertThat(stats.toString()).contains("maxFileCount=3");
        assertThat(stats.toString()).contains("usedPct=");
        assertThat(stats.toString()).contains("usedBytes=10");
    }

    // -------------------------------------------------------------------------
    // Cleanup tests
    // -------------------------------------------------------------------------

    @Test
    void shouldDeleteFilesOlderThanConfiguredDays() throws IOException {
        fileSystemService.saveFile(new Filename("old-split.zip"), new byte[10]);
        fileSystemService.saveFile(new Filename("recent-split.zip"), new byte[10]);

        Path defaultDir = fileSystemService.resolvePath(new Filename("dummy.zip")).getParent();
        Path oldFile = defaultDir.resolve("old-split.zip");
        Path recentFile = defaultDir.resolve("recent-split.zip");

        // Set old file's modification time to 60 days ago (> 30-day test limit)
        Files.setLastModifiedTime(oldFile, FileTime.from(Instant.now().minus(60, ChronoUnit.DAYS)));

        fileSystemService.cleanOldFiles();

        assertThat(Files.exists(oldFile)).isFalse();
        assertThat(Files.exists(recentFile)).isTrue();
    }

    @Test
    void shouldKeepFilesNewerThanConfiguredDays() throws IOException {
        fileSystemService.saveFile(new Filename("split1.zip"), new byte[10]);
        fileSystemService.saveFile(new Filename("split2.zip"), new byte[10]);

        Path defaultDir = fileSystemService.resolvePath(new Filename("dummy.zip")).getParent();
        Path file1 = defaultDir.resolve("split1.zip");
        Path file2 = defaultDir.resolve("split2.zip");

        fileSystemService.cleanOldFiles();

        assertThat(Files.exists(file1)).isTrue();
        assertThat(Files.exists(file2)).isTrue();
    }

    @Test
    void shouldDeleteAllFilesOlderThanLimit() throws IOException {
        fileSystemService.saveFile(new Filename("old1.zip"), new byte[10]);
        fileSystemService.saveFile(new Filename("old2.zip"), new byte[10]);

        Path defaultDir = fileSystemService.resolvePath(new Filename("dummy.zip")).getParent();
        Path old1 = defaultDir.resolve("old1.zip");
        Path old2 = defaultDir.resolve("old2.zip");

        Instant thirtyOneDaysAgo = Instant.now().minus(31, ChronoUnit.DAYS);
        Files.setLastModifiedTime(old1, FileTime.from(thirtyOneDaysAgo));
        Files.setLastModifiedTime(old2, FileTime.from(thirtyOneDaysAgo));

        fileSystemService.cleanOldFiles();

        assertThat(Files.exists(old1)).isFalse();
        assertThat(Files.exists(old2)).isFalse();
    }

    @Test
    void shouldNotFailWhenDataDirectoryDoesNotExist() {
        // Root directory was deleted in setUp — cleanOldFiles should handle gracefully
        assertThatCode(() -> fileSystemService.cleanOldFiles()).doesNotThrowAnyException();
    }

    // -------------------------------------------------------------------------
    // Read / delete / exists tests
    // -------------------------------------------------------------------------

    @Test
    void shouldReadFileAfterSave() {
        byte[] data = new byte[] { 1, 2, 3, 4, 5 };
        fileSystemService.saveFile(new Filename("test.zip"), data);

        assertThat(fileSystemService.readFile(new Filename("test.zip"))).contains(data);
    }

    @Test
    void shouldReturnEmptyWhenFileDoesNotExist() {
        assertThat(fileSystemService.readFile(new Filename("nonexistent.zip"))).isEmpty();
    }

    @Test
    void shouldDeleteFile() {
        fileSystemService.saveFile(new Filename("to-delete.zip"), new byte[10]);

        fileSystemService.deleteFile(new Filename("to-delete.zip"));

        assertThat(fileSystemService.existsFile(new Filename("to-delete.zip"))).isFalse();
    }

    @Test
    void shouldReportFileExistsAfterSave() {
        fileSystemService.saveFile(new Filename("exists.zip"), new byte[10]);

        assertThat(fileSystemService.existsFile(new Filename("exists.zip"))).isTrue();
    }

    @Test
    void shouldReportFileNotExistsBeforeSave() {
        assertThat(fileSystemService.existsFile(new Filename("not-yet.zip"))).isFalse();
    }
}
