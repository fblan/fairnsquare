package org.asymetrik.web.fairnsquare.split.domain.expenses;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

import org.asymetrik.web.fairnsquare.split.domain.participant.Participant;
import org.asymetrik.web.fairnsquare.split.domain.Split;

/**
 * Expense split proportionally based on nights stayed weighted by number of persons, but only for a specified subset of
 * participants. Share calculation: (participant_nights × participant_persons) / total_weighted_nights × amount
 */
public final class ExpenseByNightCustom extends Expense {

    private final List<Participant.Id> participantIds;

    /**
     * Factory method for creating new BY_NIGHT_CUSTOM expenses.
     *
     * @param amount
     *            the expense amount (must be positive)
     * @param description
     *            the expense description (required, max 200 chars)
     * @param payerId
     *            the ID of the participant who paid
     * @param participantIds
     *            the IDs of participants included in the split (must not be null or empty)
     *
     * @return a new ExpenseByNightCustom with generated ID and createdAt set to now
     */
    public static ExpenseByNightCustom create(BigDecimal amount, String description, Participant.Id payerId,
            List<Participant.Id> participantIds) {
        validateAmount(amount);
        validateDescription(description);
        validateParticipantIds(participantIds);
        return new ExpenseByNightCustom(Id.generate(), amount, description, payerId, participantIds, Instant.now());
    }

    /**
     * Reconstitutes an ExpenseByNightCustom from stored fields (used by persistence mapper).
     */
    public static ExpenseByNightCustom fromJson(Id id, BigDecimal amount, String description, Participant.Id payerId,
            List<Participant.Id> participantIds, Instant createdAt) {
        validateParticipantIds(participantIds);
        return new ExpenseByNightCustom(id, amount, description, payerId, participantIds, createdAt);
    }

    private static void validateParticipantIds(List<Participant.Id> participantIds) {
        Objects.requireNonNull(participantIds, "participantIds cannot be null");
        if (participantIds.isEmpty()) {
            throw new IllegalArgumentException("participantIds cannot be empty");
        }
    }

    /**
     * Package-private constructor for internal use.
     */
    ExpenseByNightCustom(Id id, BigDecimal amount, String description, Participant.Id payerId,
            List<Participant.Id> participantIds, Instant createdAt) {
        super(id, amount, description, payerId, createdAt);
        this.participantIds = participantIds;
    }

    @Override
    public SplitMode getSplitMode() {
        return SplitMode.BY_NIGHT_CUSTOM;
    }

    @Override
    public List<Share> getShares(Split split) {
        List<Participant> filtered = split.getParticipants().stream().filter(p -> participantIds.contains(p.id()))
                .toList();
        ExpenseByNight delegate = new ExpenseByNight(getId(), getAmount(), getDescription(), getPayerId(),
                getCreatedAt());
        return delegate.calculateShares(filtered);
    }

    public List<Participant.Id> getParticipantIds() {
        return participantIds;
    }
}
