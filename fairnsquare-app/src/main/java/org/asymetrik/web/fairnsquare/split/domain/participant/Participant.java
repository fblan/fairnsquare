package org.asymetrik.web.fairnsquare.split.domain.participant;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;

/**
 * Participant entity - represents a person participating in a split with their stay duration. Rich domain model with
 * value objects for fields. A participant can represent a family or group via the numberOfPersons field.
 */
public record Participant(Id id, Name name, Nights nights, NumberOfPersons numberOfPersons) {

    @Override
    public String toString() {
        return "Participant{id=%s, nights=%s, persons=%s}".formatted(id, nights, numberOfPersons);
    }

    /**
     * Factory method to create a new Participant with generated ID and default 1 person.
     *
     * @param name
     *            the participant's name
     * @param nights
     *            the number of nights stayed
     *
     * @return a new Participant with a generated NanoID
     */
    public static Participant create(String name, double nights) {
        return new Participant(Id.generate(), new Name(name), new Nights(nights), new NumberOfPersons(1.0));
    }

    /**
     * Factory method to create a new Participant with generated ID and specified number of persons.
     *
     * @param name
     *            the participant's name
     * @param nights
     *            the number of nights stayed
     * @param numberOfPersons
     *            the number of persons this participant represents (e.g. family)
     *
     * @return a new Participant with a generated NanoID
     */
    public static Participant create(String name, double nights, double numberOfPersons) {
        return new Participant(Id.generate(), new Name(name), new Nights(nights), new NumberOfPersons(numberOfPersons));
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
     * Value object for number of persons this participant represents. Allows representing families or groups. Children
     * can be counted as 0.5.
     */
    public record NumberOfPersons(double value) {

        private static final double MIN_PERSONS = 0.5;
        private static final double MAX_PERSONS = 50;
        private static final double STEP = 0.5;

        public NumberOfPersons {
            if (value < MIN_PERSONS) {
                throw new IllegalArgumentException("Number of persons must be at least " + MIN_PERSONS);
            }
            if (value > MAX_PERSONS) {
                throw new IllegalArgumentException("Number of persons cannot exceed " + (int) MAX_PERSONS);
            }
            if (value % STEP != 0) {
                throw new IllegalArgumentException("Number of persons must be a multiple of " + STEP);
            }
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }
}
