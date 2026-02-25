package org.asymetrik.web.fairnsquare.split.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Direct unit tests for {@link StorageConstraintsService} — instantiated without CDI so JaCoCo can instrument the real
 * class. Complements {@link StorageConstraintsServiceTest} which uses @QuarkusTest.
 */
class StorageConstraintsServiceDirectTest {

    // Max 1024 bytes / 30-day age limit, matching the @QuarkusTest profile values
    private static final long MAX_SIZE = 1024;
    private static final int MAX_AGE_DAYS = 30;

    @TempDir
    Path tempDir;

    StorageConstraintsService service;

    @BeforeEach
    void setUp() {
        TenantPathResolver resolver = new TenantPathResolver();
        resolver.dataPath = tempDir.toString();
        service = new StorageConstraintsService(resolver, MAX_SIZE, MAX_AGE_DAYS);
    }

    // -------------------------------------------------------------------------
    // computeStorageStats — empty directory
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
        TenantPathResolver resolver = new TenantPathResolver();
        resolver.dataPath = tempDir.resolve("nonexistent").toString();
        StorageConstraintsService svc = new StorageConstraintsService(resolver, MAX_SIZE, MAX_AGE_DAYS);

        StorageStats stats = svc.computeStorageStats();

        assertThat(stats.usedBytes()).isEqualTo(0);
        assertThat(stats.fileCount()).isEqualTo(0);
    }

    // -------------------------------------------------------------------------
    // computeStorageStats — with ZIP files
    // -------------------------------------------------------------------------

    @Test
    void computeStorageStats_countsZipFilesAndSize() throws IOException {
        writeZip("a.zip", 300);
        writeZip("b.zip", 200);

        StorageStats stats = service.computeStorageStats();

        assertThat(stats.usedBytes()).isEqualTo(500);
        assertThat(stats.fileCount()).isEqualTo(2);
        assertThat(stats.maxBytes()).isEqualTo(MAX_SIZE);
    }

    @Test
    void computeStorageStats_ignoresNonZipFiles() throws IOException {
        writeZip("split.zip", 400);
        Files.write(tempDir.resolve("readme.txt"), new byte[100]);

        StorageStats stats = service.computeStorageStats();

        assertThat(stats.usedBytes()).isEqualTo(400);
        assertThat(stats.fileCount()).isEqualTo(1);
    }

    // -------------------------------------------------------------------------
    // checkSizeLimitBeforeSave
    // -------------------------------------------------------------------------

    @Test
    void checkSizeLimitBeforeSave_allowsWhenUnderLimit() throws IOException {
        writeZip("existing.zip", 500);
        Path newFile = tempDir.resolve("new.zip");

        assertThatCode(() -> service.checkSizeLimitBeforeSave(newFile, 200)).doesNotThrowAnyException();
    }

    @Test
    void checkSizeLimitBeforeSave_rejectsWhenOverLimit() throws IOException {
        writeZip("existing.zip", 900);
        Path newFile = tempDir.resolve("new.zip");

        assertThatThrownBy(() -> service.checkSizeLimitBeforeSave(newFile, 200))
                .isInstanceOf(StorageLimitExceededError.class);
    }

    @Test
    void checkSizeLimitBeforeSave_allowsWhenDirectoryDoesNotExist() {
        Path newFile = tempDir.resolve("nonexistent-dir").resolve("new.zip");

        assertThatCode(() -> service.checkSizeLimitBeforeSave(newFile, 200)).doesNotThrowAnyException();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void writeZip(String filename, int sizeBytes) throws IOException {
        Path file = tempDir.resolve(filename);
        Files.write(file, new byte[sizeBytes]);
    }
}
