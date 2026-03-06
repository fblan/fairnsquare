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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Tests for {@link FileSystemService}: size limit enforcement, old file cleanup, storage stats. Uses test profile
 * config: max-total-size-bytes=1024, max-file-age-days=30.
 */
@QuarkusTest
class FileSystemServiceTest {

    @Inject
    FileSystemService fileSystemService;

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
    // Size limit tests (via saveFile)
    // -------------------------------------------------------------------------

    @Test
    void shouldAllowSaveWhenStorageIsEmpty() {
        assertThatCode(() -> fileSystemService.saveFile(new Filename("split-new.zip"), new byte[200]))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldAllowSaveWhenTotalWouldStayWithinLimit() {
        // 500 existing + 200 new = 700 < 1024
        fileSystemService.saveFile(new Filename("split1.zip"), new byte[500]);

        assertThatCode(() -> fileSystemService.saveFile(new Filename("split-new.zip"), new byte[200]))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectSaveWhenTotalWouldExceedLimit() {
        // 900 existing + 200 new = 1100 > 1024
        fileSystemService.saveFile(new Filename("split1.zip"), new byte[900]);

        assertThatThrownBy(() -> fileSystemService.saveFile(new Filename("split-new.zip"), new byte[200]))
                .isInstanceOf(StorageLimitExceededError.class);
    }

    @Test
    void shouldNotDoubleCountExistingFileOnUpdate() {
        // splitA occupies 800 bytes; updating it with 500 bytes → (800 - 800) + 500 = 500 < 1024
        fileSystemService.saveFile(new Filename("splitA.zip"), new byte[800]);

        assertThatCode(() -> fileSystemService.saveFile(new Filename("splitA.zip"), new byte[500]))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectUpdateWhenNewVersionWouldExceedLimit() {
        // 600 bytes of other files + updating splitA (currently 100 bytes) with 800 bytes
        // → (600 + 100 - 100) + 800 = 1400 > 1024
        fileSystemService.saveFile(new Filename("split1.zip"), new byte[600]);
        fileSystemService.saveFile(new Filename("splitA.zip"), new byte[100]);

        assertThatThrownBy(() -> fileSystemService.saveFile(new Filename("splitA.zip"), new byte[800]))
                .isInstanceOf(StorageLimitExceededError.class);
    }

    // -------------------------------------------------------------------------
    // Storage stats tests
    // -------------------------------------------------------------------------

    @Test
    void shouldReturnZeroStatsWhenStorageIsEmpty() {
        StorageStats stats = fileSystemService.computeStorageStats();

        assertThat(stats.usedBytes()).isEqualTo(0);
        assertThat(stats.fileCount()).isEqualTo(0);
        assertThat(stats.remainingPercent()).isEqualTo(100.0);
    }

    @Test
    void shouldReturnCorrectStatsWhenFilesExist() {
        fileSystemService.saveFile(new Filename("split1.zip"), new byte[300]);
        fileSystemService.saveFile(new Filename("split2.zip"), new byte[200]);

        StorageStats stats = fileSystemService.computeStorageStats();

        assertThat(stats.usedBytes()).isEqualTo(500);
        assertThat(stats.fileCount()).isEqualTo(2);
        assertThat(stats.remainingPercent()).isLessThan(100.0);
        assertThat(stats.remainingMb()).isPositive();
    }

    @Test
    void shouldFormatStatsAsHumanReadableString() {
        fileSystemService.saveFile(new Filename("split1.zip"), new byte[1024]);

        StorageStats stats = fileSystemService.computeStorageStats();

        assertThat(stats.toString()).contains("usedSize=");
        assertThat(stats.toString()).contains("remainingMo=");
        assertThat(stats.toString()).contains("remainingPct=");
        assertThat(stats.toString()).contains("fileCount=1");
    }

    // -------------------------------------------------------------------------
    // Cleanup tests
    // -------------------------------------------------------------------------

    @Test
    void shouldDeleteFilesOlderThanConfiguredDays() throws IOException {
        fileSystemService.saveFile(new Filename("old-split.zip"), new byte[100]);
        fileSystemService.saveFile(new Filename("recent-split.zip"), new byte[100]);

        Path defaultDir = pathResolver.resolveDefaultTenantDirectory();
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
        fileSystemService.saveFile(new Filename("split1.zip"), new byte[100]);
        fileSystemService.saveFile(new Filename("split2.zip"), new byte[100]);

        Path defaultDir = pathResolver.resolveDefaultTenantDirectory();
        Path file1 = defaultDir.resolve("split1.zip");
        Path file2 = defaultDir.resolve("split2.zip");

        fileSystemService.cleanOldFiles();

        assertThat(Files.exists(file1)).isTrue();
        assertThat(Files.exists(file2)).isTrue();
    }

    @Test
    void shouldDeleteAllFilesOlderThanLimit() throws IOException {
        fileSystemService.saveFile(new Filename("old1.zip"), new byte[100]);
        fileSystemService.saveFile(new Filename("old2.zip"), new byte[100]);

        Path defaultDir = pathResolver.resolveDefaultTenantDirectory();
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
