package org.asymetrik.web.fairnsquare.split.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Sealed abstract class representing a shared expense in a split. Each concrete subclass implements its own share
 * calculation strategy.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type", defaultImpl = ExpenseByNight.class)
@JsonSubTypes({ @JsonSubTypes.Type(value = ExpenseByNight.class, name = "BY_NIGHT"),
        @JsonSubTypes.Type(value = ExpenseEqual.class, name = "EQUAL") })
public sealed abstract class Expense permits ExpenseByNight, ExpenseEqual {

    private static final int MAX_DESCRIPTION_LENGTH = 200;

    private final Id id;
    private final BigDecimal amount;
    private final String description;
    private final Participant.Id payerId;
    private final Instant createdAt;
    private final List<Share> shares;

    /**
     * Factory method to create an Expense of the appropriate subtype based on splitMode.
     *
     * @deprecated Use ExpenseByNight.create() or ExpenseEqual.create() directly
     */
    @Deprecated
    public static Expense create(BigDecimal amount, String description, Participant.Id payerId, SplitMode splitMode,
            List<Share> shares) {
        validateAmount(amount);
        validateDescription(description);
        return switch (splitMode) {
            case BY_NIGHT -> new ExpenseByNight(Id.generate(), amount, description, payerId, Instant.now(), shares);
            case EQUAL -> new ExpenseEqual(Id.generate(), amount, description, payerId, Instant.now(), shares);
            case FREE -> throw new UnsupportedOperationException("FREE mode not yet implemented");
        };
    }

    /**
     * Jackson constructor for backward compatibility with legacy JSON. Routes to appropriate subclass based on
     * splitMode field.
     */
    @JsonCreator
    public static Expense fromJson(@JsonProperty("id") Id id, @JsonProperty("amount") BigDecimal amount,
            @JsonProperty("description") String description, @JsonProperty("payerId") Participant.Id payerId,
            @JsonProperty("splitMode") SplitMode splitMode, @JsonProperty("createdAt") Instant createdAt,
            @JsonProperty("shares") List<Share> shares) {
        // Support backward compatibility: minimal expenses from Story 3.3 only had payerId
        if (id == null && amount == null && description == null && splitMode == null && createdAt == null) {
            return new ExpenseByNight(null, null, null, payerId, null, null);
        }
        // Route to appropriate subclass based on splitMode for legacy JSON without "type" field
        SplitMode mode = splitMode != null ? splitMode : SplitMode.BY_NIGHT;
        return switch (mode) {
            case BY_NIGHT -> new ExpenseByNight(id, amount, description, payerId, createdAt, shares);
            case EQUAL -> new ExpenseEqual(id, amount, description, payerId, createdAt, shares);
            case FREE -> throw new UnsupportedOperationException("FREE mode not yet implemented");
        };
    }

    /**
     * Protected constructor for subclasses.
     */
    protected Expense(Id id, BigDecimal amount, String description, Participant.Id payerId, Instant createdAt,
            List<Share> shares) {
        this.id = id;
        this.amount = amount;
        this.description = description;
        this.payerId = payerId;
        this.createdAt = createdAt;
        this.shares = shares != null ? new ArrayList<>(shares) : new ArrayList<>();
    }

    /**
     * Abstract method - each subclass implements its calculation strategy.
     *
     * @param participants
     *            the list of participants to calculate shares for
     *
     * @return calculated shares for each participant
     */
    public abstract List<Share> calculateShares(List<Participant> participants);

    /**
     * Returns the split mode for this expense type.
     */
    public abstract SplitMode getSplitMode();

    protected static void validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }

    protected static void validateDescription(String description) {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Description cannot be blank");
        }
        if (description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException("Description cannot exceed " + MAX_DESCRIPTION_LENGTH + " characters");
        }
    }

    // Getters

    public Id getId() {
        return id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public Participant.Id getPayerId() {
        return payerId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<Share> getShares() {
        return shares != null ? Collections.unmodifiableList(shares) : Collections.emptyList();
    }

    /**
     * Value object wrapping a NanoID for expense identification. Uses the same 21-character URL-safe format as Split.Id
     * and Participant.Id.
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
                throw new IllegalArgumentException("Expense ID cannot be null or blank");
            }
            if (!value.matches(NANOID_PATTERN)) {
                throw new IllegalArgumentException("Expense ID contains invalid characters");
            }
            if (value.length() != NANOID_LENGTH) {
                throw new IllegalArgumentException("Expense ID must be exactly " + NANOID_LENGTH + " characters");
            }
        }

        /**
         * Validates if a string is a valid Expense ID format without throwing.
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
         * Generates a new random Expense ID using NanoID.
         *
         * @return a new Id with a 21-character URL-safe identifier
         */
        public static Id generate() {
            return new Id(NanoIdUtils.randomNanoId());
        }

        /**
         * Creates an Expense ID from an existing string value.
         *
         * @param value
         *            the existing expense ID string
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
     * Value object representing a participant's share of an expense.
     */
    public record Share(Participant.Id participantId, BigDecimal amount) {

        @JsonCreator
        public static Share fromJson(@JsonProperty("participantId") Participant.Id participantId,
                @JsonProperty("amount") BigDecimal amount) {
            return new Share(participantId, amount);
        }

        public Share {
            if (participantId == null) {
                throw new IllegalArgumentException("Share participantId cannot be null");
            }
            if (amount == null) {
                throw new IllegalArgumentException("Share amount cannot be null");
            }
            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Share amount cannot be negative");
            }
        }
    }
}
