package org.asymetrik.web.fairnsquare.backup;

import java.time.Instant;

/**
 * An archive in the bucket.
 *
 * @param name
 *            its name, e.g. {@code splits-2026-11-25T03-00-00-000Z.fnsbackup}
 * @param createdAt
 *            when it was made
 * @param sizeBytes
 *            its encrypted size
 */
public record BackupArchive(String name, Instant createdAt, long sizeBytes) {
}
