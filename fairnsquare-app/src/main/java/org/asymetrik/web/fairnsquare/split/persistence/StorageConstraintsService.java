package org.asymetrik.web.fairnsquare.split.persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.asymetrik.web.fairnsquare.sharedkernel.logging.Log;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Enforces storage constraints for the persistence layer:
 * <ul>
 * <li>Total size limit: rejects saves that would exceed the configured maximum.</li>
 * <li>File age limit: removes ZIP files older than the configured number of days.</li>
 * </ul>
 */
@ApplicationScoped
public class StorageConstraintsService {

    private static final Logger LOG = Logger.getLogger(StorageConstraintsService.class);

    private final TenantPathResolver pathResolver;
    private final long maxTotalSizeBytes;
    private final int maxFileAgeDays;

    @Inject
    public StorageConstraintsService(TenantPathResolver pathResolver,
            @ConfigProperty(name = "fairnsquare.storage.max-total-size-bytes", defaultValue = "524288000") long maxTotalSizeBytes,
            @ConfigProperty(name = "fairnsquare.storage.max-file-age-days", defaultValue = "90") int maxFileAgeDays) {
        this.pathResolver = pathResolver;
        this.maxTotalSizeBytes = maxTotalSizeBytes;
        this.maxFileAgeDays = maxFileAgeDays;
    }

    /**
     * Checks whether saving a new ZIP of the given size would exceed the total storage limit.
     * <p>
     * If {@code filePath} already exists (an update), its current size is subtracted from the total to avoid counting
     * the replaced file twice.
     *
     * @param filePath
     *            the destination path of the file being saved
     * @param newFileSizeBytes
     *            the size in bytes of the new ZIP content
     *
     * @throws StorageLimitExceededError
     *             if saving would exceed the configured limit
     */
    public void checkSizeLimitBeforeSave(Path filePath, long newFileSizeBytes) {
        Path rootDir = pathResolver.resolveRootDirectory();
        if (!Files.exists(rootDir)) {
            return;
        }

        long currentTotal = computeTotalSize(rootDir);

        long existingFileSize = 0;
        if (Files.exists(filePath)) {
            try {
                existingFileSize = Files.size(filePath);
            } catch (IOException e) {
                LOG.warnf("Could not read size of existing file %s: %s", filePath, e.getMessage());
            }
        }

        long projectedTotal = currentTotal - existingFileSize + newFileSizeBytes;
        if (projectedTotal > maxTotalSizeBytes) {
            throw new StorageLimitExceededError(projectedTotal, maxTotalSizeBytes);
        }
    }

    /**
     * Deletes all ZIP files in the data directory whose last-modified time is older than the configured number of days.
     * Logs a summary of deleted files.
     */
    public void cleanOldFiles() {
        Path rootDir = pathResolver.resolveRootDirectory();
        if (!Files.exists(rootDir)) {
            LOG.info("Storage cleanup: root directory does not exist, nothing to clean.");
            return;
        }

        Instant cutoff = Instant.now().minus(maxFileAgeDays, ChronoUnit.DAYS);
        AtomicInteger deleted = new AtomicInteger(0);
        AtomicInteger errors = new AtomicInteger(0);

        try {
            Files.walk(rootDir).filter(path -> path.toString().endsWith(".zip"))
                    .filter(path -> isOlderThan(path, cutoff)).forEach(path -> {
                        try {
                            Files.delete(path);
                            deleted.incrementAndGet();
                            LOG.debugf("Storage cleanup: deleted old file %s", path);
                        } catch (IOException e) {
                            errors.incrementAndGet();
                            LOG.warnf("Storage cleanup: failed to delete %s: %s", path, e.getMessage());
                        }
                    });
        } catch (IOException e) {
            LOG.errorf("Storage cleanup: failed to walk data directory %s: %s", rootDir, e.getMessage());
            return;
        }

        LOG.infof("Storage cleanup complete: %d file(s) deleted, %d error(s).", deleted.get(), errors.get());
    }

    /**
     * Computes a snapshot of current storage usage. Called after each split is persisted so that the result is logged
     * via the {@link Log} interceptor, showing remaining space and file count.
     *
     * @return a {@link StorageStats} with used bytes, max bytes, and file count
     */
    @Log
    public StorageStats computeStorageStats() {
        Path rootDir = pathResolver.resolveRootDirectory();
        if (!Files.exists(rootDir)) {
            return new StorageStats(0, maxTotalSizeBytes, 0);
        }

        AtomicLong totalSize = new AtomicLong(0);
        AtomicInteger fileCount = new AtomicInteger(0);
        try {
            Files.walk(rootDir).filter(path -> path.toString().endsWith(".zip")).filter(Files::isRegularFile)
                    .forEach(path -> {
                        try {
                            totalSize.addAndGet(Files.size(path));
                            fileCount.incrementAndGet();
                        } catch (IOException e) {
                            LOG.warnf("Could not read size of file %s: %s", path, e.getMessage());
                        }
                    });
        } catch (IOException e) {
            LOG.warnf("Could not walk data directory for stats computation: %s", e.getMessage());
        }
        return new StorageStats(totalSize.get(), maxTotalSizeBytes, fileCount.get());
    }

    private long computeTotalSize(Path rootDir) {
        AtomicLong total = new AtomicLong(0);
        try {
            Files.walk(rootDir).filter(path -> path.toString().endsWith(".zip")).filter(Files::isRegularFile)
                    .forEach(path -> {
                        try {
                            total.addAndGet(Files.size(path));
                        } catch (IOException e) {
                            LOG.warnf("Could not read size of file %s: %s", path, e.getMessage());
                        }
                    });
        } catch (IOException e) {
            LOG.warnf("Could not walk data directory for size computation: %s", e.getMessage());
        }
        return total.get();
    }

    private boolean isOlderThan(Path path, Instant cutoff) {
        try {
            FileTime lastModified = Files.getLastModifiedTime(path);
            return lastModified.toInstant().isBefore(cutoff);
        } catch (IOException e) {
            LOG.warnf("Could not read last-modified time of %s: %s", path, e.getMessage());
            return false;
        }
    }
}
