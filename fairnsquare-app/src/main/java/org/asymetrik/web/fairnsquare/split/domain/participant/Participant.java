package org.asymetrik.web.fairnsquare.split.domain.participant;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;

/**
 * Participant entity - represents a person participating in a split with their stay duration. Rich domain model with
 * value objects for fields. A participant can represent a family or group via the share field.
 */
public record Participant(Id id, Name name, Nights nights, Share share) {

    @Override
    public String toString() {
        return "Participant{id=%s, nights=%s, share=%s}".formatted(id, nights, share);
    }

    /**
     * Factory method to create a new Participant with generated ID and default share of 1.
     *
     * @param name
     *            the participant's name
     * @param nights
     *            the number of nights stayed
     *
     * @return a new Participant with a generated NanoID
     */
    public static Participant create(String name, double nights) {
        return new Participant(Id.generate(), new Name(name), new Nights(nights), new Share(1.0));
    }

    /**
     * Factory method to create a new Participant with generated ID and specified share.
     *
     * @param name
     *            the participant's name
     * @param nights
     *            the number of nights stayed
     * @param share
     *            the share this participant represents (e.g. 2 for a family of 2, 0.5 for a child)
     *
     * @return a new Participant with a generated NanoID
     */
    public static Participant create(String name, double nights, double share) {
        return new Participant(Id.generate(), new Name(name), new Nights(nights), new Share(share));
    }

    /**
     * Value object wrapping a NanoID for participant identification. Uses the same 21-character URL-safe format as
     * Split.Id.
     */
    public record Id(String value) {

        private static final int NANOID_LENGTH = 21;
        private static final String NANOID_PATTERN = "^[A-Za-z0-9_-]+$";

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
    public record Name(String value) {

        private static final int MAX_LENGTH = 50;

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
    public record Nights(double value) {

        private static final double MIN_NIGHTS = 0.5;
        private static final double MAX_NIGHTS = 365;

        public Nights {
            if (value < MIN_NIGHTS) {
                throw new IllegalArgumentException("Nights must be at least " + MIN_NIGHTS);
            }
            if (value > MAX_NIGHTS) {
                throw new IllegalArgumentException("Nights cannot exceed " + (int) MAX_NIGHTS);
            }
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }

    /**
     * Value object for the share this participant represents. Allows representing families or groups. Children can be
     * counted as 0.5.
     */
    public record Share(double value) {

        private static final double MIN_SHARE = 0.5;
        private static final double MAX_SHARE = 50;
        private static final double STEP = 0.5;

        public Share {
            if (value < MIN_SHARE) {
                throw new IllegalArgumentException("Share must be at least " + MIN_SHARE);
            }
            if (value > MAX_SHARE) {
                throw new IllegalArgumentException("Share cannot exceed " + (int) MAX_SHARE);
            }
            if (value % STEP != 0) {
                throw new IllegalArgumentException("Share must be a multiple of " + STEP);
            }
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }
}
