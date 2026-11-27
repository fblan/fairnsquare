package org.asymetrik.web.fairnsquare.admin.api.dto;

import java.util.List;

import org.asymetrik.web.fairnsquare.backup.BackupArchive;

/**
 * The backups: whether they are on, what prevents them, and the archives, newest first.
 *
 * @param enabled
 *            a bucket is configured
 * @param problem
 *            why backups don't work (no valid key, bucket unreachable...), or null
 * @param archives
 *            the archives in the bucket; empty when off or on a problem
 */
public record AdminBackupsResponse(boolean enabled, String problem, List<BackupArchive> archives) {
}
