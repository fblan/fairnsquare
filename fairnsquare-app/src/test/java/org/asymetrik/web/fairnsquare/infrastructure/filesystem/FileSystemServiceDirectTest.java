package org.asymetrik.web.fairnsquare.infrastructure.filesystem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.asymetrik.web.fairnsquare.infrastructure.filesystem.internal.StorageFileCountLimitExceededError;
import org.asymetrik.web.fairnsquare.infrastructure.filesystem.internal.StorageFileSizeLimitExceededError;
import org.asymetrik.web.fairnsquare.infrastructure.filesystem.StorageStats;
import org.asymetrik.web.fairnsquare.infrastructure.filesystem.internal.TenantPathResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Direct unit tests for {@link FileSystemService} — instantiated without CDI so JaCoCo can instrument the real class.
 * Complements {@link FileSystemServiceTest} which uses @QuarkusTest.
 */
class FileSystemServiceDirectTest {

    private static final int MAX_FILE_COUNT = 3;
    private static final long MAX_FILE_SIZE_BYTES = 1024;
    private static final int MAX_AGE_DAYS = 30;

    @TempDir
    Path tempDir;

    FileSystemService service;
    TenantPathResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new TenantPathResolver();
        resolver.dataPath = tempDir.toString();
        service = new FileSystemService(resolver, MAX_FILE_COUNT, MAX_FILE_SIZE_BYTES, MAX_AGE_DAYS);
    }

    // -------------------------------------------------------------------------
    // computeStorageStats — empty / nonexistent directory
    // -------------------------------------------------------------------------

    @Test
    void computeStorageStats_returnsZerosWhenDirectoryIsEmpty() {
        StorageStats stats = service.computeStorageStats();

        assertThat(stats.fileCount()).isEqualTo(0);
        assertThat(stats.usedBytes()).isEqualTo(0);
        assertThat(stats.maxFileCount()).isEqualTo(MAX_FILE_COUNT);
        assertThat(stats.maxTotalBytes()).isEqualTo((long) MAX_FILE_COUNT * MAX_FILE_SIZE_BYTES);
        assertThat(stats.remainingFileCount()).isEqualTo(MAX_FILE_COUNT);
    }

    @Test
    void computeStorageStats_returnsZerosWhenDirectoryDoesNotExist() {
        TenantPathResolver nonExistentResolver = new TenantPathResolver();
        nonExistentResolver.dataPath = tempDir.resolve("nonexistent").toString();
        FileSystemService svc = new FileSystemService(nonExistentResolver, MAX_FILE_COUNT, MAX_FILE_SIZE_BYTES,
                MAX_AGE_DAYS);

        StorageStats stats = svc.computeStorageStats();

        assertThat(stats.fileCount()).isEqualTo(0);
        assertThat(stats.usedBytes()).isEqualTo(0);
        assertThat(stats.maxFileCount()).isEqualTo(MAX_FILE_COUNT);
    }

    // -------------------------------------------------------------------------
    // computeStorageStats — with ZIP files (via saveFile)
    // -------------------------------------------------------------------------

    @Test
    void computeStorageStats_countsZipFilesAndSize() {
        service.saveFile(new Filename("a.zip"), new byte[300]);
        service.saveFile(new Filename("b.zip"), new byte[200]);

        StorageStats stats = service.computeStorageStats();

        assertThat(stats.fileCount()).isEqualTo(2);
        assertThat(stats.usedBytes()).isEqualTo(500);
        assertThat(stats.maxFileCount()).isEqualTo(MAX_FILE_COUNT);
    }

    @Test
    void computeStorageStats_ignoresNonZipFiles() throws IOException {
        service.saveFile(new Filename("split.zip"), new byte[400]);
        Files.write(tempDir.resolve("readme.txt"), new byte[100]);

        StorageStats stats = service.computeStorageStats();

        assertThat(stats.fileCount()).isEqualTo(1);
        assertThat(stats.usedBytes()).isEqualTo(400);
    }

    // -------------------------------------------------------------------------
    // File size limit enforcement
    // -------------------------------------------------------------------------

    @Test
    void saveFile_allowsWhenUnderSizeLimit() {
        assertThatCode(() -> service.saveFile(new Filename("new.zip"), new byte[(int) MAX_FILE_SIZE_BYTES]))
                .doesNotThrowAnyException();
    }

    @Test
    void saveFile_rejectsWhenExceedsSizeLimit() {
        assertThatThrownBy(() -> service.saveFile(new Filename("new.zip"), new byte[(int) MAX_FILE_SIZE_BYTES + 1]))
                .isInstanceOf(StorageFileSizeLimitExceededError.class);
    }

    @Test
    void saveFile_allowsUpdateExceedingCountWhenFileSizeIsWithinLimit() {
        // An update to an existing file is never rejected for size as long as payload <= limit
        service.saveFile(new Filename("existing.zip"), new byte[100]);
        assertThatCode(() -> service.saveFile(new Filename("existing.zip"), new byte[(int) MAX_FILE_SIZE_BYTES]))
                .doesNotThrowAnyException();
    }

    // -------------------------------------------------------------------------
    // File count limit enforcement
    // -------------------------------------------------------------------------

    @Test
    void saveFile_allowsWhenUnderCountLimit() {
        service.saveFile(new Filename("a.zip"), new byte[10]);
        service.saveFile(new Filename("b.zip"), new byte[10]);

        assertThatCode(() -> service.saveFile(new Filename("c.zip"), new byte[10])).doesNotThrowAnyException();
    }

    @Test
    void saveFile_rejectsNewFileWhenCountLimitReached() {
        service.saveFile(new Filename("a.zip"), new byte[10]);
        service.saveFile(new Filename("b.zip"), new byte[10]);
        service.saveFile(new Filename("c.zip"), new byte[10]);

        assertThatThrownBy(() -> service.saveFile(new Filename("d.zip"), new byte[10]))
                .isInstanceOf(StorageFileCountLimitExceededError.class);
    }

    @Test
    void saveFile_allowsUpdateWhenCountLimitReached() {
        service.saveFile(new Filename("a.zip"), new byte[10]);
        service.saveFile(new Filename("b.zip"), new byte[10]);
        service.saveFile(new Filename("c.zip"), new byte[10]);

        // Updating an existing file must succeed even when at the limit
        assertThatCode(() -> service.saveFile(new Filename("a.zip"), new byte[20])).doesNotThrowAnyException();
    }

    @Test
    void saveFile_allowsWhenDirectoryDoesNotExist() {
        assertThatCode(() -> service.saveFile(new Filename("new.zip"), new byte[10])).doesNotThrowAnyException();
    }

    // -------------------------------------------------------------------------
    // Read / delete / exists
    // -------------------------------------------------------------------------

    @Test
    void readFile_returnsDataAfterSave() {
        byte[] data = new byte[] { 10, 20, 30 };
        service.saveFile(new Filename("test.zip"), data);

        assertThat(service.readFile(new Filename("test.zip"))).contains(data);
    }

    @Test
    void readFile_returnsEmptyWhenMissing() {
        assertThat(service.readFile(new Filename("missing.zip"))).isEmpty();
    }

    @Test
    void deleteFile_removesExistingFile() {
        service.saveFile(new Filename("to-delete.zip"), new byte[10]);

        service.deleteFile(new Filename("to-delete.zip"));

        assertThat(service.existsFile(new Filename("to-delete.zip"))).isFalse();
    }

    @Test
    void existsFile_returnsTrueAfterSave() {
        service.saveFile(new Filename("present.zip"), new byte[5]);

        assertThat(service.existsFile(new Filename("present.zip"))).isTrue();
    }

    @Test
    void existsFile_returnsFalseWhenNotSaved() {
        assertThat(service.existsFile(new Filename("absent.zip"))).isFalse();
    }
}
