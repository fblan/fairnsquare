package org.asymetrik.web.fairnsquare.infrastructure.filesystem.internal;

/**
 * Value object identifying a directory path segment (e.g. a tenant identifier). Used by
 * {@link org.asymetrik.web.fairnsquare.infrastructure.filesystem.FileSystemService} to locate files on disk.
 *
 * @param value
 *            the directory path segment (e.g. "default" for the default tenant)
 */
public record PathId(String value) {
}
