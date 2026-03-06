package org.asymetrik.web.fairnsquare.infrastructure.filesystem.internal;

/**
 * Snapshot of storage usage.
 *
 * @param usedBytes
 *            total bytes currently occupied by all files
 * @param maxBytes
 *            configured storage limit in bytes
 * @param fileCount
 *            number of files currently stored
 */
public record StorageStats(long usedBytes, long maxBytes, int fileCount) {

    public double remainingMb() {
        return (maxBytes - usedBytes) / (1024.0 * 1024.0);
    }

    public double remainingPercent() {
        if (maxBytes == 0) {
            return 100.0;
        }
        return 100.0 * (maxBytes - usedBytes) / maxBytes;
    }

    public String formattedUsedSize() {
        if (usedBytes >= 1024L * 1024 * 1024) {
            return String.format("%.2fGo", usedBytes / (1024.0 * 1024 * 1024));
        } else if (usedBytes >= 1024L * 1024) {
            return String.format("%.2fMo", usedBytes / (1024.0 * 1024));
        } else {
            return String.format("%.2fKo", usedBytes / 1024.0);
        }
    }

    @Override
    public String toString() {
        return String.format("usedSize=%s remainingMo=%.1f remainingPct=%.1f%% fileCount=%d", formattedUsedSize(),
                remainingMb(), remainingPercent(), fileCount);
    }
}
