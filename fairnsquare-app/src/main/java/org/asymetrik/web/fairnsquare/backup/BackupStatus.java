package org.asymetrik.web.fairnsquare.backup;

/**
 * Whether backups are on.
 *
 * @param enabled
 *            a bucket is configured
 * @param problem
 *            what prevents backups despite the bucket (e.g. no valid key), or null
 */
public record BackupStatus(boolean enabled, String problem) {
}
