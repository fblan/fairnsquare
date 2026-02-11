package org.asymetrik.web.fairnsquare.split.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;

/**
 * Sealed abstract class representing a shared expense in a split. Each concrete subclass implements its own share
 * calculation strategy.
 */
public sealed abstract class Expense permits ExpenseByNight, ExpenseEqual, ExpenseFree {

    private static final int MAX_DESCRIPTION_LENGTH = 200;

    private final Id id;
    private final BigDecimal amount;
    private final String description;
    private final Participant.Id payerId;
    private final Instant createdAt;

    /**
     * Factory method to create an Expense of the appropriate subtype based on splitMode.
     *
     * @deprecated Use ExpenseByNight.create() or ExpenseEqual.create() directly
     */
    @Deprecated
    public static Expense create(BigDecimal amount, String description, Participant.Id payerId, SplitMode splitMode) {
        validateAmount(amount);
        validateDescription(description);
        return switch (splitMode) {
            case BY_NIGHT -> new ExpenseByNight(Id.generate(), amount, description, payerId, Instant.now());
            case EQUAL -> new ExpenseEqual(Id.generate(), amount, description, payerId, Instant.now());
            case FREE -> throw new UnsupportedOperationException(
                    "FREE mode requires shares - use ExpenseFree.create(amount, description, payerId, shares)");
        };
    }

    /**
     * Reconstitutes an Expense from stored fields, routing to the appropriate subclass based on splitMode.
     */
    public static Expense fromJson(Id id, BigDecimal amount, String description, Participant.Id payerId,
            SplitMode splitMode, Instant createdAt) {
        // Support backward compatibility: minimal expenses from Story 3.3 only had payerId
        if (id == null && amount == null && description == null && splitMode == null && createdAt == null) {
            return new ExpenseByNight(null, null, null, payerId, null);
        }
        // Route to appropriate subclass based on splitMode
        SplitMode mode = splitMode != null ? splitMode : SplitMode.BY_NIGHT;
        return switch (mode) {
            case BY_NIGHT -> new ExpenseByNight(id, amount, description, payerId, createdAt);
            case EQUAL -> new ExpenseEqual(id, amount, description, payerId, createdAt);
            case FREE -> throw new UnsupportedOperationException(
                    "FREE mode requires shares - use ExpenseFree.fromJson(id, amount, description, payerId, shares, createdAt)");
        };
    }

    /**
     * Protected constructor for subclasses.
     */
    protected Expense(Id id, BigDecimal amount, String description, Participant.Id payerId, Instant createdAt) {
        this.id = id;
        this.amount = amount;
        this.description = description;
        this.payerId = payerId;
        this.createdAt = createdAt;

    }

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

    public abstract List<Share> getShares(Split split);

    /**
     * Value object wrapping a NanoID for expense identification. Uses the same 21-character URL-safe format as Split.Id
     * and Participant.Id.
     */
    public record Id(String value) {

        private static final int NANOID_LENGTH = 21;
        private static final String NANOID_PATTERN = "^[A-Za-z0-9_-]+$";

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
     * Value object representing a participant's share of an expense. - For BY_NIGHT/EQUAL modes: amount is calculated,
     * parts is null - For FREE mode: parts is stored, amount is calculated from parts
     */
    public record Share(Participant.Id participantId, BigDecimal amount, BigDecimal parts) {

        /**
         * Creates a share with calculated amount (used by BY_NIGHT/EQUAL).
         */
        public static Share withAmount(Participant.Id participantId, BigDecimal amount) {
            return new Share(participantId, amount, null);
        }

        /**
         * Creates a share with parts (used by FREE mode).
         */
        public static Share withParts(Participant.Id participantId, BigDecimal parts) {
            return new Share(participantId, null, parts);
        }

        /**
         * For JSON deserialization - reconstructs share from stored data.
         */
        public static Share fromJson(Participant.Id participantId, BigDecimal amount, BigDecimal parts) {
            return new Share(participantId, amount, parts);
        }

        public Share {
            if (participantId == null) {
                throw new IllegalArgumentException("Share participantId cannot be null");
            }
            if (amount != null && amount.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Share amount cannot be negative");
            }
            if (parts != null && parts.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Share parts cannot be negative");
            }
        }
    }
}
