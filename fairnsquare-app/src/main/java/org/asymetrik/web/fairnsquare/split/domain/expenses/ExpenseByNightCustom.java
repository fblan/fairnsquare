package org.asymetrik.web.fairnsquare.split.domain.expenses;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.asymetrik.web.fairnsquare.split.domain.participant.Participant;
import org.asymetrik.web.fairnsquare.split.domain.Split;

/**
 * Expense split proportionally based on nights stayed, but only among a selected subset of participants.
 * <p>
 * Use case: van guests who participate in all nightly expenses except the house rent. The list of participating
 * participant IDs is stored and used to filter participants before applying the standard BY_NIGHT calculation.
 * </p>
 * Share calculation (same as BY_NIGHT, but restricted to the stored participant IDs): (participant_nights ×
 * participant_persons) / total_weighted_nights × amount
 */
public final class ExpenseByNightCustom extends Expense {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    /** IDs of the participants who actually participate in this expense. */
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
     *            the IDs of the participants who actually participate
     *
     * @return a new ExpenseByNightCustom with generated ID and createdAt set to now
     *
     * @throws IllegalArgumentException
     *             if participantIds is null or empty
     */
    public static ExpenseByNightCustom create(BigDecimal amount, String description, Participant.Id payerId,
            List<Participant.Id> participantIds) {
        validateAmount(amount);
        validateDescription(description);
        validateParticipantIds(participantIds);
        return new ExpenseByNightCustom(Id.generate(), amount, description, payerId, List.copyOf(participantIds),
                Instant.now());
    }

    /**
     * Reconstitutes an ExpenseByNightCustom from stored fields (used by persistence mapper).
     */
    public static ExpenseByNightCustom fromJson(Id id, BigDecimal amount, String description, Participant.Id payerId,
            List<Participant.Id> participantIds, Instant createdAt) {
        return new ExpenseByNightCustom(id, amount, description, payerId, List.copyOf(participantIds), createdAt);
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

    /**
     * Returns the participant IDs who participate in this expense (used for persistence). The list is unmodifiable.
     */
    public List<Participant.Id> getParticipantIds() {
        return Collections.unmodifiableList(participantIds);
    }

    @Override
    public List<Share> getShares(Split split) {
        Set<Participant.Id> participatingIds = participantIds.stream().collect(Collectors.toSet());
        List<Participant> participatingParticipants = split.getParticipants().stream()
                .filter(p -> participatingIds.contains(p.id())).toList();
        return calculateShares(participatingParticipants);
    }

    List<Share> calculateShares(List<Participant> participants) {
        if (participants == null || participants.isEmpty()) {
            return Collections.emptyList();
        }

        double totalWeight = participants.stream().mapToDouble(p -> p.nights().value() * p.share().value()).sum();

        if (totalWeight == 0) {
            return Collections.emptyList();
        }

        List<Share> calculatedShares = new ArrayList<>();
        BigDecimal totalAssigned = BigDecimal.ZERO;

        for (int i = 0; i < participants.size(); i++) {
            Participant p = participants.get(i);
            double weight = p.nights().value() * p.share().value();
            BigDecimal share;

            if (i == participants.size() - 1) {
                // Last participant gets the remainder to ensure sum = amount
                share = getAmount().subtract(totalAssigned);
            } else {
                share = getAmount().multiply(BigDecimal.valueOf(weight)).divide(BigDecimal.valueOf(totalWeight), SCALE,
                        ROUNDING_MODE);
                totalAssigned = totalAssigned.add(share);
            }

            calculatedShares.add(Share.withAmount(p.id(), share));
        }

        return calculatedShares;
    }

    private static void validateParticipantIds(List<Participant.Id> participantIds) {
        if (participantIds == null) {
            throw new IllegalArgumentException("Participant IDs cannot be null");
        }
        if (participantIds.isEmpty()) {
            throw new IllegalArgumentException("BY_NIGHT_CUSTOM expense must have at least one participant");
        }
    }
}
