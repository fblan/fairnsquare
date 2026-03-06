package org.asymetrik.web.fairnsquare.infrastructure.filesystem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.asymetrik.web.fairnsquare.infrastructure.filesystem.internal.PathId;
import org.asymetrik.web.fairnsquare.infrastructure.filesystem.internal.StorageLimitExceededError;
import org.asymetrik.web.fairnsquare.infrastructure.filesystem.internal.StorageStats;
import org.asymetrik.web.fairnsquare.infrastructure.filesystem.internal.TenantPathResolver;
import org.asymetrik.web.fairnsquare.sharedkernel.logging.Log;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Technical domain service for raw file system operations. Provides file read/write primitives and enforces storage
 * constraints:
 * <ul>
 * <li>Total size limit: rejects saves that would exceed the configured maximum.</li>
 * <li>File age limit: removes files older than the configured number of days.</li>
 * </ul>
 */
@ApplicationScoped
public class FileSystemService {

    private static final Logger LOG = Logger.getLogger(FileSystemService.class);

    private final TenantPathResolver pathResolver;
    private final long maxTotalSizeBytes;
    private final int maxFileAgeDays;

    @Inject
    public FileSystemService(TenantPathResolver pathResolver,
            @ConfigProperty(name = "fairnsquare.storage.max-total-size-bytes", defaultValue = "524288000") long maxTotalSizeBytes,
            @ConfigProperty(name = "fairnsquare.storage.max-file-age-days", defaultValue = "90") int maxFileAgeDays) {
        this.pathResolver = pathResolver;
        this.maxTotalSizeBytes = maxTotalSizeBytes;
        this.maxFileAgeDays = maxFileAgeDays;
    }

    /**
     * Saves raw bytes to a file under the default tenant directory. Checks the storage size limit before writing and
     * logs storage stats after a successful save.
     *
     * @param filename
     *            the file name
     * @param data
     *            the bytes to write
     *
     * @throws StorageLimitExceededError
     *             if saving would exceed the configured limit
     * @throws FileSystemException
     *             if the write operation fails
     */
    public void saveFile(Filename filename, byte[] data) {
        saveFile(new PathId(TenantPathResolver.DEFAULT_TENANT), filename, data);
    }

    /**
     * Saves raw bytes to a file. Checks the storage size limit before writing and logs storage stats after a successful
     * save.
     *
     * @param pathId
     *            the directory path segment (e.g. tenant identifier)
     * @param filename
     *            the file name
     * @param data
     *            the bytes to write
     *
     * @throws StorageLimitExceededError
     *             if saving would exceed the configured limit
     * @throws FileSystemException
     *             if the write operation fails
     */
    public void saveFile(PathId pathId, Filename filename, byte[] data) {
        Path filePath = pathResolver.resolve(pathId, filename);
        try {
            Path parentDir = filePath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }
            checkSizeLimitBeforeSave(filePath, data.length);
            Files.write(filePath, data);
        } catch (StorageLimitExceededError e) {
            throw e;
        } catch (IOException e) {
            throw new FileSystemException("Failed to save file to " + filePath, e);
        }
        computeStorageStats();
    }

    /**
     * Reads raw bytes from a file under the default tenant directory.
     *
     * @param filename
     *            the file name
     *
     * @return an Optional containing the file bytes if found, empty otherwise
     *
     * @throws FileSystemException
     *             if the read operation fails (other than file not found)
     */
    public Optional<byte[]> readFile(Filename filename) {
        return readFile(new PathId(TenantPathResolver.DEFAULT_TENANT), filename);
    }

    /**
     * Reads raw bytes from a file.
     *
     * @param pathId
     *            the directory path segment (e.g. tenant identifier)
     * @param filename
     *            the file name
     *
     * @return an Optional containing the file bytes if found, empty otherwise
     *
     * @throws FileSystemException
     *             if the read operation fails (other than file not found)
     */
    public Optional<byte[]> readFile(PathId pathId, Filename filename) {
        Path filePath = pathResolver.resolve(pathId, filename);
        if (!Files.exists(filePath)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readAllBytes(filePath));
        } catch (IOException e) {
            throw new FileSystemException("Failed to read file from " + filePath, e);
        }
    }

    /**
     * Deletes a file under the default tenant directory.
     *
     * @param filename
     *            the file name
     *
     * @throws FileSystemException
     *             if the delete operation fails
     */
    public void deleteFile(Filename filename) {
        deleteFile(new PathId(TenantPathResolver.DEFAULT_TENANT), filename);
    }

    /**
     * Deletes a file.
     *
     * @param pathId
     *            the directory path segment (e.g. tenant identifier)
     * @param filename
     *            the file name
     *
     * @throws FileSystemException
     *             if the delete operation fails
     */
    public void deleteFile(PathId pathId, Filename filename) {
        Path filePath = pathResolver.resolve(pathId, filename);
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new FileSystemException("Failed to delete file at " + filePath, e);
        }
    }

    /**
     * Checks whether a file exists under the default tenant directory.
     *
     * @param filename
     *            the file name
     *
     * @return true if the file exists, false otherwise
     */
    public boolean existsFile(Filename filename) {
        return existsFile(new PathId(TenantPathResolver.DEFAULT_TENANT), filename);
    }

    /**
     * Checks whether a file exists.
     *
     * @param pathId
     *            the directory path segment (e.g. tenant identifier)
     * @param filename
     *            the file name
     *
     * @return true if the file exists, false otherwise
     */
    public boolean existsFile(PathId pathId, Filename filename) {
        Path filePath = pathResolver.resolve(pathId, filename);
        return Files.exists(filePath);
    }

    /**
     * Resolves the file path for the given filename under the default tenant directory.
     *
     * @param filename
     *            the file name
     *
     * @return the resolved file path
     */
    public Path resolvePath(Filename filename) {
        return pathResolver.resolve(filename);
    }

    /**
     * Computes a snapshot of current storage usage.
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

    private void checkSizeLimitBeforeSave(Path filePath, long newFileSizeBytes) {
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

    /**
     * Runtime exception for file system operations.
     */
    public static class FileSystemException extends RuntimeException {
        public FileSystemException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
