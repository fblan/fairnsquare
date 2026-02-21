package org.asymetrik.web.fairnsquare.split.persistence;

/**
 * Metadata stored alongside persisted data in ZIP archives. Contains format version and deserializer code to support
 * future format evolution and encryption.
 *
 * @param version
 *            the format version (e.g. "1.0")
 * @param deserializer
 *            the deserializer code indicating how to read data.bin (e.g. "clear" for plain JSON)
 */
public record PersistenceMetadata(String version, String deserializer) {

    public static final String CURRENT_VERSION = "1.0";
    public static final String CLEAR_DESERIALIZER = "clear";

    /**
     * Creates metadata with current default values.
     */
    public static PersistenceMetadata current() {
        return new PersistenceMetadata(CURRENT_VERSION, CLEAR_DESERIALIZER);
    }
}
