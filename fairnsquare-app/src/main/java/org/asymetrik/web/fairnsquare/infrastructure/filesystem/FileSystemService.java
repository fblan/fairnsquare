package org.asymetrik.web.fairnsquare.infrastructure.filesystem;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
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
import org.eclipse.microprofile.config.Config;

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

    private static final Logger LOG = System.getLogger(FileSystemService.class.getName());

    private final TenantPathResolver pathResolver;
    private final int maxFileCount;
    private final long maxFileSizeBytes;
    private final int maxFileAgeDays;

    /**
     * The limits come from the configuration ({@code fairnsquare.storage.*}). Read from {@link Config} rather than with
     * {@code @ConfigProperty} parameters: vidocq 0.4.0-RC2 loses their metadata on an intercepted bean's constructor
     * ("@ConfigProperty metadata is required on injection point"), and this bean is intercepted ({@code @Log}).
     */
    @Inject
    public FileSystemService(TenantPathResolver pathResolver, Config config) {
        this(pathResolver, config.getOptionalValue("fairnsquare.storage.max-file-count", Integer.class).orElse(5000),
                config.getOptionalValue("fairnsquare.storage.max-file-size-bytes", Long.class).orElse(524288L),
                config.getOptionalValue("fairnsquare.storage.max-file-age-days", Integer.class).orElse(90));
    }

    public FileSystemService(TenantPathResolver pathResolver, int maxFileCount, long maxFileSizeBytes,
            int maxFileAgeDays) {
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
            LOG.log(Level.WARNING, "Could not list split IDs from %s: %s".formatted(rootDir, e.getMessage()));
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
                    LOG.log(Level.WARNING, "Could not read size of file %s: %s".formatted(path, e.getMessage()));
                }
            });
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Could not walk data directory for stats computation: %s".formatted(e.getMessage()));
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
            LOG.log(Level.INFO, "Storage cleanup: root directory does not exist, nothing to clean.");
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
                            LOG.log(Level.INFO, "Storage cleanup: deleted old file %s".formatted(path));
                        } catch (IOException e) {
                            errors.incrementAndGet();
                            LOG.log(Level.WARNING,
                                    "Storage cleanup: failed to delete %s: %s".formatted(path, e.getMessage()));
                        }
                    });
        } catch (IOException e) {
            LOG.log(Level.ERROR,
                    "Storage cleanup: failed to walk data directory %s: %s".formatted(rootDir, e.getMessage()));
            return;
        }

        LOG.log(Level.INFO,
                "Storage cleanup complete: %d file(s) deleted, %d error(s).".formatted(deleted.get(), errors.get()));
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
            LOG.log(Level.WARNING, "Could not walk data directory for file count: %s".formatted(e.getMessage()));
        }
        return count.get();
    }

    private boolean isOlderThan(Path path, Instant cutoff) {
        try {
            FileTime lastModified = Files.getLastModifiedTime(path);
            return lastModified.toInstant().isBefore(cutoff);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Could not read last-modified time of %s: %s".formatted(path, e.getMessage()));
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
