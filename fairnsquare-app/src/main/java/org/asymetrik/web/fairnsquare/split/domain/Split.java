package org.asymetrik.web.fairnsquare.split.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Split aggregate root - represents an expense split with participants and expenses. Rich domain model: no setters,
 * behavior methods only, value objects for fields.
 */
public class Split {

    /**
     * Value object for split name with validation.
     */
    public record Name(@JsonValue String value) {
        @JsonCreator
        public static Name fromJson(String value) {
            return new Name(value);
        }

        public Name {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("Split name cannot be blank");
            }
            if (value.length() > 100) {
                throw new IllegalArgumentException("Split name cannot exceed 100 characters");
            }
        }
    }

    private final Id id;
    private Name name;
    private final Instant createdAt;
    private final List<Participant> participants;
    private final List<Expense> expenses;

    /**
     * Factory method to create a new Split with generated ID.
     */
    public static Split create(String name) {
        return new Split(Id.generate(), new Name(name), Instant.now());
    }

    /**
     * Constructor for creating Split with all fields (used by factory and Jackson).
     */
    @JsonCreator
    public Split(@JsonProperty("id") Id id, @JsonProperty("name") Name name,
            @JsonProperty("createdAt") Instant createdAt) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
        this.participants = new ArrayList<>();
        this.expenses = new ArrayList<>();
    }

    /**
     * Full constructor including collections (for Jackson deserialization).
     */
    @JsonCreator
    public static Split fromJson(@JsonProperty("id") Id id, @JsonProperty("name") Name name,
            @JsonProperty("createdAt") Instant createdAt, @JsonProperty("participants") List<Participant> participants,
            @JsonProperty("expenses") List<Expense> expenses) {
        Split split = new Split(id, name, createdAt);
        if (participants != null) {
            split.participants.addAll(participants);
        }
        if (expenses != null) {
            split.expenses.addAll(expenses);
        }
        return split;
    }

    // Behavior methods

    /**
     * Rename the split.
     */
    public void rename(String newName) {
        this.name = new Name(newName);
    }

    /**
     * Add a participant to the split.
     */
    public void addParticipant(Participant participant) {
        if (participant == null) {
            throw new IllegalArgumentException("Participant cannot be null");
        }
        this.participants.add(participant);
    }

    /**
     * Add an expense to the split.
     */
    public void addExpense(Expense expense) {
        if (expense == null) {
            throw new IllegalArgumentException("Expense cannot be null");
        }
        this.expenses.add(expense);
    }

    // Getters - return value objects and unmodifiable collections

    public Id getId() {
        return id;
    }

    public Name getName() {
        return name;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<Participant> getParticipants() {
        return Collections.unmodifiableList(participants);
    }

    public List<Expense> getExpenses() {
        return Collections.unmodifiableList(expenses);
    }

    /**
     * Value object wrapping a NanoID for split identification. Generates cryptographically secure, URL-safe
     * identifiers.
     */
    public record Id(@JsonValue String value) {

        @JsonCreator
        public static Id fromJson(String value) {
            return new Id(value);
        }

        private static final int NANOID_LENGTH = 21;
        private static final String NANOID_PATTERN = "^[A-Za-z0-9_-]+$";

        public Id {
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
            return value != null && !value.isBlank() && value.length() == NANOID_LENGTH
                    && value.matches(NANOID_PATTERN);
        }

        /**
         * Generates a new random SplitId using NanoID.
         *
         * @return a new SplitId with a 21-character URL-safe identifier
         */
        public static Id generate() {
            return new Id(NanoIdUtils.randomNanoId());
        }

        /**
         * Creates a SplitId from an existing string value.
         *
         * @param value
         *            the existing split ID string
         *
         * @return a SplitId wrapping the provided value
         */
        public static Id of(String value) {
            return new Id(value);
        }

        @Override
        public String toString() {
            return value;
        }
    }
}
