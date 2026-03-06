package org.asymetrik.web.fairnsquare.infrastructure.filesystem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.asymetrik.web.fairnsquare.infrastructure.filesystem.internal.StorageLimitExceededError;
import org.asymetrik.web.fairnsquare.infrastructure.filesystem.internal.StorageStats;
import org.asymetrik.web.fairnsquare.infrastructure.filesystem.internal.TenantPathResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Direct unit tests for {@link FileSystemService} — instantiated without CDI so JaCoCo can instrument the real class.
 * Complements {@link FileSystemServiceTest} which uses @QuarkusTest.
 */
class FileSystemServiceDirectTest {

    // Max 1024 bytes / 30-day age limit, matching the @QuarkusTest profile values
    private static final long MAX_SIZE = 1024;
    private static final int MAX_AGE_DAYS = 30;

    @TempDir
    Path tempDir;

    FileSystemService service;
    TenantPathResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new TenantPathResolver();
        resolver.dataPath = tempDir.toString();
        service = new FileSystemService(resolver, MAX_SIZE, MAX_AGE_DAYS);
    }

    // -------------------------------------------------------------------------
    // computeStorageStats — empty / nonexistent directory
    // -------------------------------------------------------------------------

    @Test
    void computeStorageStats_returnsZerosWhenDirectoryIsEmpty() {
        StorageStats stats = service.computeStorageStats();

        assertThat(stats.usedBytes()).isEqualTo(0);
        assertThat(stats.fileCount()).isEqualTo(0);
        assertThat(stats.remainingPercent()).isEqualTo(100.0);
    }

    @Test
    void computeStorageStats_returnsZerosWhenDirectoryDoesNotExist() {
        TenantPathResolver nonExistentResolver = new TenantPathResolver();
        nonExistentResolver.dataPath = tempDir.resolve("nonexistent").toString();
        FileSystemService svc = new FileSystemService(nonExistentResolver, MAX_SIZE, MAX_AGE_DAYS);

        StorageStats stats = svc.computeStorageStats();

        assertThat(stats.usedBytes()).isEqualTo(0);
        assertThat(stats.fileCount()).isEqualTo(0);
    }

    // -------------------------------------------------------------------------
    // computeStorageStats — with ZIP files (via saveFile)
    // -------------------------------------------------------------------------

    @Test
    void computeStorageStats_countsZipFilesAndSize() {
        service.saveFile(new Filename("a.zip"), new byte[300]);
        service.saveFile(new Filename("b.zip"), new byte[200]);

        StorageStats stats = service.computeStorageStats();

        assertThat(stats.usedBytes()).isEqualTo(500);
        assertThat(stats.fileCount()).isEqualTo(2);
        assertThat(stats.maxBytes()).isEqualTo(MAX_SIZE);
    }

    @Test
    void computeStorageStats_ignoresNonZipFiles() throws IOException {
        service.saveFile(new Filename("split.zip"), new byte[400]);
        Files.write(tempDir.resolve("readme.txt"), new byte[100]);

        StorageStats stats = service.computeStorageStats();

        assertThat(stats.usedBytes()).isEqualTo(400);
        assertThat(stats.fileCount()).isEqualTo(1);
    }

    // -------------------------------------------------------------------------
    // Size limit enforcement (via saveFile)
    // -------------------------------------------------------------------------

    @Test
    void saveFile_allowsWhenUnderLimit() {
        service.saveFile(new Filename("existing.zip"), new byte[500]);

        assertThatCode(() -> service.saveFile(new Filename("new.zip"), new byte[200])).doesNotThrowAnyException();
    }

    @Test
    void saveFile_rejectsWhenOverLimit() {
        service.saveFile(new Filename("existing.zip"), new byte[900]);

        assertThatThrownBy(() -> service.saveFile(new Filename("new.zip"), new byte[200]))
                .isInstanceOf(StorageLimitExceededError.class);
    }

    @Test
    void saveFile_allowsWhenDirectoryDoesNotExist() {
        assertThatCode(() -> service.saveFile(new Filename("new.zip"), new byte[200])).doesNotThrowAnyException();
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
