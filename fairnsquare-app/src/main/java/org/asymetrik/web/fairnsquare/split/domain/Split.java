package org.asymetrik.web.fairnsquare.split.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    private final SplitId id;
    private Name name;
    private final Instant createdAt;
    private final List<Participant> participants;
    private final List<Expense> expenses;

    /**
     * Factory method to create a new Split with generated ID.
     */
    public static Split create(String name) {
        return new Split(SplitId.generate(), new Name(name), Instant.now());
    }

    /**
     * Constructor for creating Split with all fields (used by factory and Jackson).
     */
    @JsonCreator
    public Split(@JsonProperty("id") SplitId id, @JsonProperty("name") Name name,
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
    public static Split fromJson(@JsonProperty("id") SplitId id, @JsonProperty("name") Name name,
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

    public SplitId getId() {
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
}
