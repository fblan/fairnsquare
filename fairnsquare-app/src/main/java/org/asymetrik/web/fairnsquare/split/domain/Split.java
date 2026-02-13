package org.asymetrik.web.fairnsquare.split.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.asymetrik.web.fairnsquare.split.domain.expenses.Expense;
import org.asymetrik.web.fairnsquare.split.domain.expenses.ExpenseNotFoundError;
import org.asymetrik.web.fairnsquare.split.domain.expenses.PayerNotFoundError;
import org.asymetrik.web.fairnsquare.split.domain.expenses.SplitMode;
import org.asymetrik.web.fairnsquare.split.domain.participant.DuplicateParticipantNameError;
import org.asymetrik.web.fairnsquare.split.domain.participant.Participant;
import org.asymetrik.web.fairnsquare.split.domain.participant.ParticipantHasExpensesError;
import org.asymetrik.web.fairnsquare.split.domain.participant.ParticipantNotFoundError;
import org.asymetrik.web.fairnsquare.split.domain.settlement.Settlement;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;

/**
 * Split aggregate root - represents an expense split with participants and expenses. Rich domain model: no setters,
 * behavior methods only, value objects for fields.
 */
public class Split {

    public static final int MAX_PARTICIPANTS = 100;
    public static final int MAX_EXPENSES = 1000;

    /**
     * Value object for split name with validation.
     */
    public record Name(String value) {

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
    private Settlement settlement;

    /**
     * Factory method to create a new Split with generated ID.
     */
    public static Split create(String name) {
        return new Split(Id.generate(), new Name(name), Instant.now());
    }

    /**
     * Constructor for creating Split with all fields.
     */
    public Split(Id id, Name name, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
        this.participants = new ArrayList<>();
        this.expenses = new ArrayList<>();
        validate();
    }

    public void validate() {
        Objects.requireNonNull(id, "Split ID cannot be null");
        Objects.requireNonNull(name, "Split name cannot be null");
        Objects.requireNonNull(createdAt, "CreatedAt cannot be null");
        Objects.requireNonNull(participants, "Participants list cannot be null");
        Objects.requireNonNull(expenses, "Expenses list cannot be null");
        if (this.participants.size() > MAX_PARTICIPANTS) {
            throw new SplitMaximumParticipantNumberReachedError(this);
        }
        if (this.expenses.size() > MAX_EXPENSES) {
            throw new SplitMaximumExpenseReachedError(this);
        }
        // Validate split name
        Set<String> participantNames = new java.util.HashSet<>();
        // Validate participants
        for (Participant participant : participants) {
            if (participant == null) {
                throw new IllegalArgumentException("Participant cannot be null");
            }
            if (participantNames.contains(participant.name().value())) {
                throw new DuplicateParticipantNameError(participant, this);
            }
            participantNames.add(participant.name().value());

        }

        // Validate expenses
        for (Expense expense : expenses) {
            if (expense == null) {
                throw new IllegalArgumentException("Expenses cannot be null");
            }
        }
    }

    /**
     * Add a participant to the split.
     */
    public void addParticipant(Participant participant) {
        if (participant == null) {
            throw new IllegalArgumentException("Participant cannot be null");
        }
        this.participants.add(participant);
        clearSettlement();
        validate();
    }

    /**
     * Add an expense to the split.
     */
    public void addExpense(Expense expense) {
        if (expense == null) {
            throw new IllegalArgumentException("Expense cannot be null");
        }
        this.expenses.add(expense);
        clearSettlement();
        validate();
    }

    /**
     * Update an existing participant in the split.
     *
     * @param participantId
     *            the ID of the participant to update
     * @param newName
     *            the new name for the participant
     * @param newNights
     *            the new number of nights
     *
     * @return the updated participant
     *
     * @throws ParticipantNotFoundError
     *             if no participant with the given ID exists
     */
    public Participant updateParticipant(Participant.Id participantId, String newName, double newNights) {
        for (int i = 0; i < participants.size(); i++) {
            if (participants.get(i).id().equals(participantId)) {
                Participant updated = new Participant(participantId, new Participant.Name(newName),
                        new Participant.Nights(newNights));
                participants.set(i, updated);
                clearSettlement();
                validate();
                return updated;
            }
        }
        throw new ParticipantNotFoundError(participantId.value(), id.value());
    }

    /**
     * Find a participant by ID.
     *
     * @param participantId
     *            the ID of the participant to find
     *
     * @return the participant if found
     *
     * @throws ParticipantNotFoundError
     *             if no participant with the given ID exists
     */
    public Participant getParticipant(Participant.Id participantId) {
        return participants.stream().filter(p -> p.id().equals(participantId)).findFirst()
                .orElseThrow(() -> new ParticipantNotFoundError(participantId.value(), id.value()));
    }

    /**
     * Check if a participant has any associated expenses.
     *
     * @param participantId
     *            the ID of the participant to check
     *
     * @return true if the participant is a payer on any expense, false otherwise
     */
    public boolean hasExpensesForParticipant(Participant.Id participantId) {
        return expenses.stream().anyMatch(e -> e.getPayerId() != null && e.getPayerId().equals(participantId));
    }

    /**
     * Remove a participant from the split.
     *
     * @param participantId
     *            the ID of the participant to remove
     *
     * @throws ParticipantHasExpensesError
     *             if the participant has associated expenses
     * @throws ParticipantNotFoundError
     *             if no participant with the given ID exists
     */
    public void removeParticipant(Participant.Id participantId) {
        if (hasExpensesForParticipant(participantId)) {
            throw new ParticipantHasExpensesError();
        }
        boolean removed = participants.removeIf(p -> p.id().equals(participantId));
        if (!removed) {
            throw new ParticipantNotFoundError(participantId.value(), id.value());
        }
        clearSettlement();
        validate();
    }

    /**
     * Update an existing expense in the split.
     *
     * @param expenseId
     *            the ID of the expense to update
     * @param amount
     *            the new amount
     * @param description
     *            the new description
     * @param payerId
     *            the new payer ID
     * @param splitMode
     *            the new split mode
     *
     * @return the updated expense with recalculated shares
     *
     * @throws ExpenseNotFoundError
     *             if no expense with the given ID exists
     * @throws PayerNotFoundError
     *             if the new payer is not a participant in this split
     */
    public Expense updateExpense(Expense.Id expenseId, java.math.BigDecimal amount, String description,
            Participant.Id payerId, SplitMode splitMode) {
        // Validate payer exists
        validatePayerExists(payerId);

        // Find and update the expense
        for (int i = 0; i < expenses.size(); i++) {
            Expense existing = expenses.get(i);
            if (existing.getId().equals(expenseId)) {
                // Preserve original ID and createdAt, create new instance with updated values
                Expense updated = Expense.fromJson(expenseId, amount, description, payerId, splitMode,
                        existing.getCreatedAt());
                expenses.set(i, updated);
                clearSettlement();
                validate();
                return updated;
            }
        }
        throw new ExpenseNotFoundError(expenseId.value(), id.value());
    }

    /**
     * Remove an expense from the split.
     *
     * @param expenseId
     *            the ID of the expense to remove
     *
     * @throws ExpenseNotFoundError
     *             if no expense with the given ID exists
     */
    public void removeExpense(Expense.Id expenseId) {
        boolean removed = expenses.removeIf(e -> e.getId().equals(expenseId));
        if (!removed) {
            throw new ExpenseNotFoundError(expenseId.value(), id.value());
        }
        clearSettlement();
        validate();
    }

    /**
     * Validate that a payer exists in the split's participants.
     *
     * @param payerId
     *            the ID of the payer to validate
     *
     * @throws PayerNotFoundError
     *             if the payer is not a participant in this split
     */
    public void validatePayerExists(Participant.Id payerId) {
        boolean exists = participants.stream().anyMatch(p -> p.id().equals(payerId));
        if (!exists) {
            throw new PayerNotFoundError(payerId.value(), id.value());
        }
    }

    /**
     * Persists a calculated settlement in this split.
     */
    public void settle(Settlement settlement) {
        Objects.requireNonNull(settlement, "Settlement cannot be null");
        this.settlement = settlement;
    }

    /**
     * Clears the persisted settlement. Called automatically when participants or expenses are modified.
     */
    public void clearSettlement() {
        this.settlement = null;
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

    public Settlement getSettlement() {
        return settlement;
    }

    /**
     * Value object wrapping a NanoID for split identification. Generates cryptographically secure, URL-safe
     * identifiers.
     */
    public record Id(String value) {

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
