package org.asymetrik.web.fairnsquare.split.domain;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Value object wrapping a NanoID for split identification. Generates cryptographically secure, URL-safe identifiers.
 */
public record SplitId(@JsonValue String value) {

    @JsonCreator
    public static SplitId fromJson(String value) {
        return new SplitId(value);
    }

    private static final int NANOID_LENGTH = 21;
    private static final String NANOID_PATTERN = "^[A-Za-z0-9_-]+$";

    public SplitId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Split ID cannot be null or blank");
        }
        if (!value.matches(NANOID_PATTERN)) {
            throw new IllegalArgumentException("Split ID contains invalid characters");
        }
        if (value.length() != NANOID_LENGTH) {
            throw new IllegalArgumentException("Split ID must be exactly " + NANOID_LENGTH + " characters");
        }
    }

    /**
     * Validates if a string is a valid SplitId format without throwing.
     *
     * @param value
     *            the string to validate
     *
     * @return true if valid NanoID format, false otherwise
     */
    public static boolean isValid(String value) {
        return value != null && !value.isBlank() && value.length() == NANOID_LENGTH && value.matches(NANOID_PATTERN);
    }

    /**
     * Generates a new random SplitId using NanoID.
     *
     * @return a new SplitId with a 21-character URL-safe identifier
     */
    public static SplitId generate() {
        return new SplitId(NanoIdUtils.randomNanoId());
    }

    /**
     * Creates a SplitId from an existing string value.
     *
     * @param value
     *            the existing split ID string
     *
     * @return a SplitId wrapping the provided value
     */
    public static SplitId of(String value) {
        return new SplitId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
