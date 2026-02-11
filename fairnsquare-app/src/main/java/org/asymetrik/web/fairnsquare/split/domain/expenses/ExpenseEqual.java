package org.asymetrik.web.fairnsquare.split.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Expense split equally among all participants. Share calculation: amount / number of participants
 */
public final class ExpenseEqual extends Expense {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    /**
     * Factory method for creating new EQUAL expenses.
     *
     * @param amount
     *            the expense amount (must be positive)
     * @param description
     *            the expense description (required, max 200 chars)
     * @param payerId
     *            the ID of the participant who paid
     * @param shares
     *            the calculated shares per participant
     *
     * @return a new ExpenseEqual with generated ID and createdAt set to now
     */
    public static ExpenseEqual create(BigDecimal amount, String description, Participant.Id payerId) {
        validateAmount(amount);
        validateDescription(description);
        return new ExpenseEqual(Id.generate(), amount, description, payerId, Instant.now());
    }

    /**
     * Reconstitutes an ExpenseEqual from stored fields (used by persistence mapper).
     */
    public static ExpenseEqual fromJson(Id id, BigDecimal amount, String description, Participant.Id payerId,
            Instant createdAt) {
        return new ExpenseEqual(id, amount, description, payerId, createdAt);
    }

    /**
     * Package-private constructor for internal use.
     */
    ExpenseEqual(Id id, BigDecimal amount, String description, Participant.Id payerId, Instant createdAt) {
        super(id, amount, description, payerId, createdAt);
    }

    @Override
    public SplitMode getSplitMode() {
        return SplitMode.EQUAL;
    }

    @Override
    public List<Share> getShares(Split split) {
        return calculateShares(split.getParticipants());
    }

    List<Share> calculateShares(List<Participant> participants) {
        if (participants == null || participants.isEmpty()) {
            return Collections.emptyList();
        }

        int count = participants.size();
        BigDecimal baseShare = getAmount().divide(BigDecimal.valueOf(count), SCALE, ROUNDING_MODE);

        List<Share> calculatedShares = new ArrayList<>();
        BigDecimal totalAssigned = BigDecimal.ZERO;

        for (int i = 0; i < count; i++) {
            Participant p = participants.get(i);
            BigDecimal share;

            if (i == count - 1) {
                // Last participant gets the remainder to ensure sum = amount
                share = getAmount().subtract(totalAssigned);
            } else {
                share = baseShare;
                totalAssigned = totalAssigned.add(share);
            }

            calculatedShares.add(Share.withAmount(p.id(), share));
        }

        return calculatedShares;
    }

}
