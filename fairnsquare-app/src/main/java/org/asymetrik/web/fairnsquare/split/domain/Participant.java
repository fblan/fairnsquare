package org.asymetrik.web.fairnsquare.split.domain;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Participant entity - represents a person participating in a split with their stay duration. Rich domain model with
 * value objects for fields.
 */
public record Participant(Id id, Name name, Nights nights) {

    /**
     * Factory method to create a new Participant with generated ID.
     *
     * @param name
     *            the participant's name
     * @param nights
     *            the number of nights stayed
     *
     * @return a new Participant with a generated NanoID
     */
    public static Participant create(String name, int nights) {
        return new Participant(Id.generate(), new Name(name), new Nights(nights));
    }

    /**
     * Jackson constructor for deserialization.
     */
    @JsonCreator
    public static Participant fromJson(@JsonProperty("id") Id id, @JsonProperty("name") Name name,
            @JsonProperty("nights") Nights nights) {
        return new Participant(id, name, nights);
    }

    /**
     * Value object wrapping a NanoID for participant identification. Uses the same 21-character URL-safe format as
     * Split.Id.
     */
    public record Id(@JsonValue String value) {

        private static final int NANOID_LENGTH = 21;
        private static final String NANOID_PATTERN = "^[A-Za-z0-9_-]+$";

        @JsonCreator
        public static Id fromJson(String value) {
            return new Id(value);
        }

        public Id {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("Participant ID cannot be null or blank");
            }
            if (!value.matches(NANOID_PATTERN)) {
                throw new IllegalArgumentException("Participant ID contains invalid characters");
            }
            if (value.length() != NANOID_LENGTH) {
                throw new IllegalArgumentException("Participant ID must be exactly " + NANOID_LENGTH + " characters");
            }
        }

        /**
         * Validates if a string is a valid Participant ID format without throwing.
         *
         * @param value
         *            the string to validate
         *
         * @return true if valid NanoID format, false otherwise
         */
        public static boolean isValid(String value) {
            return value != null && !value.isBlank() && value.length() == NANOID_LENGTH
                    && value.matches(NANOID_PATTERN);
        }

        /**
         * Generates a new random Participant ID using NanoID.
         *
         * @return a new Id with a 21-character URL-safe identifier
         */
        public static Id generate() {
            return new Id(NanoIdUtils.randomNanoId());
        }

        /**
         * Creates a Participant ID from an existing string value.
         *
         * @param value
         *            the existing participant ID string
         *
         * @return an Id wrapping the provided value
         */
        public static Id of(String value) {
            return new Id(value);
        }

        @Override
        public String toString() {
            return value;
        }
    }

    /**
     * Value object for participant name with validation.
     */
    public record Name(@JsonValue String value) {

        private static final int MAX_LENGTH = 50;

        @JsonCreator
        public static Name fromJson(String value) {
            return new Name(value);
        }

        public Name {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("Participant name cannot be blank");
            }
            if (value.length() > MAX_LENGTH) {
                throw new IllegalArgumentException("Participant name cannot exceed " + MAX_LENGTH + " characters");
            }
        }

        @Override
        public String toString() {
            return value;
        }
    }

    /**
     * Value object for number of nights with validation.
     */
    public record Nights(@JsonValue int value) {

        private static final int MIN_NIGHTS = 1;
        private static final int MAX_NIGHTS = 365;

        @JsonCreator
        public static Nights fromJson(int value) {
            return new Nights(value);
        }

        public Nights {
            if (value < MIN_NIGHTS) {
                throw new IllegalArgumentException("Nights must be at least " + MIN_NIGHTS);
            }
            if (value > MAX_NIGHTS) {
                throw new IllegalArgumentException("Nights cannot exceed " + MAX_NIGHTS);
            }
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }
}
