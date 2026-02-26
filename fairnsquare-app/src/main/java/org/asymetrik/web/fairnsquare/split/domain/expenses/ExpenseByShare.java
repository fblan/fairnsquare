package org.asymetrik.web.fairnsquare.split.domain.expenses;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.asymetrik.web.fairnsquare.split.domain.participant.Participant;
import org.asymetrik.web.fairnsquare.split.domain.Split;

/**
 * Expense split proportionally based on share. Share calculation: participant_share / total_share × amount
 */
public final class ExpenseByShare extends Expense {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    /**
     * Factory method for creating new BY_SHARE expenses.
     *
     * @param amount
     *            the expense amount (must be positive)
     * @param description
     *            the expense description (required, max 200 chars)
     * @param payerId
     *            the ID of the participant who paid
     *
     * @return a new ExpenseByShare with generated ID and createdAt set to now
     */
    public static ExpenseByShare create(BigDecimal amount, String description, Participant.Id payerId) {
        validateAmount(amount);
        validateDescription(description);
        return new ExpenseByShare(Id.generate(), amount, description, payerId, Instant.now());
    }

    /**
     * Reconstitutes an ExpenseByShare from stored fields (used by persistence mapper).
     */
    public static ExpenseByShare fromJson(Id id, BigDecimal amount, String description, Participant.Id payerId,
            Instant createdAt) {
        return new ExpenseByShare(id, amount, description, payerId, createdAt);
    }

    /**
     * Package-private constructor for internal use.
     */
    ExpenseByShare(Id id, BigDecimal amount, String description, Participant.Id payerId, Instant createdAt) {
        super(id, amount, description, payerId, createdAt);
    }

    @Override
    public SplitMode getSplitMode() {
        return SplitMode.BY_SHARE;
    }

    @Override
    public List<Share> getShares(Split split) {
        return calculateShares(split.getParticipants());
    }

    List<Share> calculateShares(List<Participant> participants) {
        if (participants == null || participants.isEmpty()) {
            return Collections.emptyList();
        }

        double totalShares = participants.stream().mapToDouble(p -> p.share().value()).sum();

        if (totalShares == 0) {
            return Collections.emptyList();
        }

        List<Share> calculatedShares = new ArrayList<>();
        BigDecimal totalAssigned = BigDecimal.ZERO;

        for (int i = 0; i < participants.size(); i++) {
            Participant p = participants.get(i);
            double weight = p.share().value();
            BigDecimal share;

            if (i == participants.size() - 1) {
                // Last participant gets the remainder to ensure sum = amount
                share = getAmount().subtract(totalAssigned);
            } else {
                share = getAmount().multiply(BigDecimal.valueOf(weight)).divide(BigDecimal.valueOf(totalShares), SCALE,
                        ROUNDING_MODE);
                totalAssigned = totalAssigned.add(share);
            }

            calculatedShares.add(Share.withAmount(p.id(), share));
        }

        return calculatedShares;
    }
}
