package org.asymetrik.web.fairnsquare.infrastructure.filesystem;

/**
 * Value object identifying a directory path segment (e.g. a tenant identifier). Used by {@link FileSystemService} to
 * locate files on disk.
 *
 * @param value
 *            the directory path segment (e.g. "default" for the default tenant)
 */
public record PathId(String value) {
}
