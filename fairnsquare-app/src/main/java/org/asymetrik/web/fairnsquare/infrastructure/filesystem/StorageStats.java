package org.asymetrik.web.fairnsquare.infrastructure.filesystem;

/**
 * Snapshot of storage usage.
 *
 * @param fileCount
 *            number of files currently stored
 * @param maxFileCount
 *            configured maximum number of files allowed
 * @param usedBytes
 *            total bytes currently occupied by all files
 * @param maxTotalBytes
 *            theoretical maximum total bytes (maxFileCount × maxFileSizeBytes)
 */
public record StorageStats(int fileCount, int maxFileCount, long usedBytes, long maxTotalBytes) {

    public int remainingFileCount() {
        return Math.max(0, maxFileCount - fileCount);
    }

    public double usedPercent() {
        if (maxFileCount == 0) {
            return 100.0;
        }
        return 100.0 * fileCount / maxFileCount;
    }

    @Override
    public String toString() {
        return String.format("fileCount=%d maxFileCount=%d usedPct=%.1f%% usedBytes=%d maxTotalBytes=%d", fileCount,
                maxFileCount, usedPercent(), usedBytes, maxTotalBytes);
    }
}
