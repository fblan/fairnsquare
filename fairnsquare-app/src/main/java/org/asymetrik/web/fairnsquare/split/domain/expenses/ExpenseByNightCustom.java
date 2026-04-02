package org.asymetrik.web.fairnsquare.split.domain.expenses;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import org.asymetrik.web.fairnsquare.split.domain.participant.Participant;
import org.asymetrik.web.fairnsquare.split.domain.Split;

/**
 * Expense split proportionally based on nights stayed, but only among a custom subset of participants.
 * Useful when some participants (e.g. van guests) should be excluded from certain by-night expenses.
 *
 * <p>Share calculation: same as {@link ExpenseByNight}, applied only to the included participants.
 */
public final class ExpenseByNightCustom extends Expense {

    private final List<Participant.Id> includedParticipantIds;

    /**
     * Factory method for creating new BY_NIGHT_CUSTOM expenses.
     *
     * @param amount
     *            the expense amount (must be positive)
     * @param description
     *            the expense description (required, max 200 chars)
     * @param payerId
     *            the ID of the participant who paid
     * @param includedParticipantIds
     *            the IDs of participants included in this expense (must not be empty)
     *
     * @return a new ExpenseByNightCustom with generated ID and createdAt set to now
     */
    public static ExpenseByNightCustom create(BigDecimal amount, String description, Participant.Id payerId,
            List<Participant.Id> includedParticipantIds) {
        validateAmount(amount);
        validateDescription(description);
        validateIncludedParticipants(includedParticipantIds);
        return new ExpenseByNightCustom(Id.generate(), amount, description, payerId,
                List.copyOf(includedParticipantIds), Instant.now());
    }

    /**
     * Reconstitutes an ExpenseByNightCustom from stored fields (used by persistence mapper).
     */
    public static ExpenseByNightCustom fromJson(Id id, BigDecimal amount, String description, Participant.Id payerId,
            List<Participant.Id> includedParticipantIds, Instant createdAt) {
        return new ExpenseByNightCustom(id, amount, description, payerId, List.copyOf(includedParticipantIds),
                createdAt);
    }

    /**
     * Package-private constructor for internal use.
     */
    ExpenseByNightCustom(Id id, BigDecimal amount, String description, Participant.Id payerId,
            List<Participant.Id> includedParticipantIds, Instant createdAt) {
        super(id, amount, description, payerId, createdAt);
        this.includedParticipantIds = includedParticipantIds;
    }

    @Override
    public SplitMode getSplitMode() {
        return SplitMode.BY_NIGHT_CUSTOM;
    }

    /**
     * Returns the IDs of participants included in this expense.
     * Used by the persistence layer to store which participants participate.
     */
    public List<Participant.Id> getIncludedParticipantIds() {
        return Collections.unmodifiableList(includedParticipantIds);
    }

    @Override
    public List<Share> getShares(Split split) {
        List<Participant> included = split.getParticipants().stream()
                .filter(p -> includedParticipantIds.contains(p.id()))
                .toList();
        if (included.isEmpty()) {
            return Collections.emptyList();
        }
        // Delegate calculation to ExpenseByNight logic
        ExpenseByNight calculator = new ExpenseByNight(getId(), getAmount(), getDescription(), getPayerId(),
                getCreatedAt());
        return calculator.calculateShares(included);
    }

    private static void validateIncludedParticipants(List<Participant.Id> includedParticipantIds) {
        if (includedParticipantIds == null || includedParticipantIds.isEmpty()) {
            throw new IllegalArgumentException("BY_NIGHT_CUSTOM expense must include at least one participant");
        }
    }
}
