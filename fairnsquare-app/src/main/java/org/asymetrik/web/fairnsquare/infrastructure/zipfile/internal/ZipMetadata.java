package org.asymetrik.web.fairnsquare.infrastructure.zipfile.internal;

/**
 * Metadata stored in the {@code metadata.json} entry of a ZIP archive. Describes the format version and deserializer
 * strategy to support future format evolution.
 *
 * @param version
 *            the format version (e.g. "1.0")
 * @param deserializer
 *            the deserializer code indicating how to read {@code data.bin} (e.g. "clear" for plain JSON)
 */
public record ZipMetadata(String version, String deserializer) {

    public static final String CURRENT_VERSION = "1.0";
    public static final String CLEAR_DESERIALIZER = "clear";
}
