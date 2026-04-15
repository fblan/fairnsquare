package org.asymetrik.web.fairnsquare.infrastructure.filesystem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.asymetrik.web.fairnsquare.infrastructure.filesystem.internal.PathId;
import org.asymetrik.web.fairnsquare.infrastructure.filesystem.internal.StorageFileCountLimitExceededError;
import org.asymetrik.web.fairnsquare.infrastructure.filesystem.internal.StorageFileSizeLimitExceededError;
import org.asymetrik.web.fairnsquare.infrastructure.filesystem.StorageStats;
import org.asymetrik.web.fairnsquare.infrastructure.filesystem.internal.TenantPathResolver;
import org.asymetrik.web.fairnsquare.sharedkernel.logging.Log;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Technical domain service for raw file system operations. Provides file read/write primitives and enforces storage
 * constraints:
 * <ul>
 * <li>File count limit: rejects saves of new files when the count has reached the configured maximum.</li>
 * <li>Single file size limit: rejects saves whose payload exceeds the configured maximum.</li>
 * <li>File age limit: removes files older than the configured number of days.</li>
 * </ul>
 */
@ApplicationScoped
public class FileSystemService {

    private static final Logger LOG = Logger.getLogger(FileSystemService.class);

    private final TenantPathResolver pathResolver;
    private final int maxFileCount;
    private final long maxFileSizeBytes;
    private final int maxFileAgeDays;

    @Inject
    public FileSystemService(TenantPathResolver pathResolver,
            @ConfigProperty(name = "fairnsquare.storage.max-file-count", defaultValue = "5000") int maxFileCount,
            @ConfigProperty(name = "fairnsquare.storage.max-file-size-bytes", defaultValue = "524288") long maxFileSizeBytes,
            @ConfigProperty(name = "fairnsquare.storage.max-file-age-days", defaultValue = "90") int maxFileAgeDays) {
        this.pathResolver = pathResolver;
        this.maxFileCount = maxFileCount;
        this.maxFileSizeBytes = maxFileSizeBytes;
        this.maxFileAgeDays = maxFileAgeDays;
    }

    /**
     * Saves raw bytes to a file under the default tenant directory. Enforces single-file size limit and, for new files,
     * the file count limit.
     *
     * @param filename
     *            the file name
     * @param data
     *            the bytes to write
     *
     * @throws StorageFileSizeLimitExceededError
     *             if the payload exceeds the configured single-file size limit
     * @throws StorageFileCountLimitExceededError
     *             if saving a new file would exceed the configured file count limit
     * @throws FileSystemException
     *             if the write operation fails
     */
    public void saveFile(Filename filename, byte[] data) {
        saveFile(new PathId(TenantPathResolver.DEFAULT_TENANT), filename, data);
    }

    /**
     * Saves raw bytes to a file. Enforces single-file size limit and, for new files, the file count limit.
     *
     * @param pathId
     *            the directory path segment (e.g. tenant identifier)
     * @param filename
     *            the file name
     * @param data
     *            the bytes to write
     *
     * @throws StorageFileSizeLimitExceededError
     *             if the payload exceeds the configured single-file size limit
     * @throws StorageFileCountLimitExceededError
     *             if saving a new file would exceed the configured file count limit
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
            checkFileSizeLimit(data.length);
            checkFileCountLimit(filePath);
            Files.write(filePath, data);
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
     * Lists all split IDs currently stored. Scans the storage directory for {@code .zip} files and returns the file
     * stem (filename without the {@code .zip} extension).
     *
     * @return list of split IDs, empty if the directory does not exist or contains no files
     */
    public java.util.List<String> listAllSplitIds() {
        Path rootDir = pathResolver.resolveDefaultTenantDirectory();
        if (!Files.exists(rootDir)) {
            return java.util.List.of();
        }
        java.util.List<String> ids = new java.util.ArrayList<>();
        try (Stream<Path> stream = Files.walk(rootDir)) {
            stream.filter(path -> path.toString().endsWith(".zip")).filter(Files::isRegularFile).forEach(path -> {
                String filename = path.getFileName().toString();
                ids.add(filename.substring(0, filename.length() - 4));
            });
        } catch (IOException e) {
            LOG.warnf("Could not list split IDs from %s: %s", rootDir, e.getMessage());
        }
        return ids;
    }

    /**
     * Returns the configured maximum number of files allowed.
     *
     * @return the file count limit
     */
    public int getMaxFileCount() {
        return maxFileCount;
    }

    /**
     * Returns the configured maximum size in bytes for a single file.
     *
     * @return the per-file size limit in bytes
     */
    public long getMaxFileSizeBytes() {
        return maxFileSizeBytes;
    }

    /**
     * Computes a snapshot of current storage usage. Sums the size of all ZIP files and counts them. This scan is
     * intended for infrequent admin use only — it is not called on every save.
     *
     * @return a {@link StorageStats} with the current file count, used bytes, and configured limits
     */
    @Log
    public StorageStats computeStorageStats() {
        Path rootDir = pathResolver.resolveDefaultTenantDirectory();
        long maxTotalBytes = (long) maxFileCount * maxFileSizeBytes;
        if (!Files.exists(rootDir)) {
            return new StorageStats(0, maxFileCount, 0, maxTotalBytes);
        }

        AtomicInteger fileCount = new AtomicInteger(0);
        AtomicLong usedBytes = new AtomicLong(0);
        try (Stream<Path> stream = Files.walk(rootDir)) {
            stream.filter(path -> path.toString().endsWith(".zip")).filter(Files::isRegularFile).forEach(path -> {
                fileCount.incrementAndGet();
                try {
                    usedBytes.addAndGet(Files.size(path));
                } catch (IOException e) {
                    LOG.warnf("Could not read size of file %s: %s", path, e.getMessage());
                }
            });
        } catch (IOException e) {
            LOG.warnf("Could not walk data directory for stats computation: %s", e.getMessage());
        }
        return new StorageStats(fileCount.get(), maxFileCount, usedBytes.get(), maxTotalBytes);
    }

    /**
     * Deletes all ZIP files in the data directory whose last-modified time is older than the configured number of days.
     * Logs a summary of deleted files.
     */
    public void cleanOldFiles() {
        Path rootDir = pathResolver.resolveDefaultTenantDirectory();
        if (!Files.exists(rootDir)) {
            LOG.info("Storage cleanup: root directory does not exist, nothing to clean.");
            return;
        }

        Instant cutoff = Instant.now().minus(maxFileAgeDays, ChronoUnit.DAYS);
        AtomicInteger deleted = new AtomicInteger(0);
        AtomicInteger errors = new AtomicInteger(0);

        try (Stream<Path> stream = Files.walk(rootDir)) {
            stream.filter(path -> path.toString().endsWith(".zip")).filter(path -> isOlderThan(path, cutoff))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                            deleted.incrementAndGet();
                            LOG.infof("Storage cleanup: deleted old file %s", path);
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

    private void checkFileSizeLimit(long fileSizeBytes) {
        if (fileSizeBytes > maxFileSizeBytes) {
            throw new StorageFileSizeLimitExceededError(fileSizeBytes, maxFileSizeBytes);
        }
    }

    private void checkFileCountLimit(Path filePath) {
        if (Files.exists(filePath)) {
            // Update to an existing file — count does not change
            return;
        }
        Path rootDir = pathResolver.resolveDefaultTenantDirectory();
        int currentCount = countZipFiles(rootDir);
        if (currentCount >= maxFileCount) {
            throw new StorageFileCountLimitExceededError(currentCount, maxFileCount);
        }
    }

    private int countZipFiles(Path rootDir) {
        if (!Files.exists(rootDir)) {
            return 0;
        }
        AtomicInteger count = new AtomicInteger(0);
        try (Stream<Path> stream = Files.walk(rootDir)) {
            stream.filter(path -> path.toString().endsWith(".zip")).filter(Files::isRegularFile)
                    .forEach(_ -> count.incrementAndGet());
        } catch (IOException e) {
            LOG.warnf("Could not walk data directory for file count: %s", e.getMessage());
        }
        return count.get();
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
