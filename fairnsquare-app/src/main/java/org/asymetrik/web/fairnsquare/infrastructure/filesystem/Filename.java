package org.asymetrik.web.fairnsquare.infrastructure.filesystem;

/**
 * Value object representing a file name (including extension). Used by {@link FileSystemService} to locate files on
 * disk.
 *
 * @param value
 *            the file name (e.g. "abc123.zip")
 */
public record Filename(String value) {
}
