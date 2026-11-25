package org.asymetrik.web.fairnsquare.backup;

/**
 * How the current splits differ from an archive's.
 *
 * @param archived
 *            the splits in the archive
 * @param current
 *            the splits now
 * @param added
 *            the splits created since the archive
 * @param deleted
 *            the archive's splits that are gone
 * @param changed
 *            the splits in both that changed since
 * @param unchanged
 *            the splits in both that didn't
 */
public record BackupComparison(int archived, int current, int added, int deleted, int changed, int unchanged) {
}
