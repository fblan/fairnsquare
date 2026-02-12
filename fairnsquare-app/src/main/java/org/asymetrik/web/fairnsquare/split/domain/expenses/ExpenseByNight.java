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
 * Expense split proportionally based on nights stayed. Share calculation: participant's nights / total nights × amount
 */
public final class ExpenseByNight extends Expense {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    /**
     * Factory method for creating new BY_NIGHT expenses.
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
     * @return a new ExpenseByNight with generated ID and createdAt set to now
     */
    public static ExpenseByNight create(BigDecimal amount, String description, Participant.Id payerId) {
        validateAmount(amount);
        validateDescription(description);
        return new ExpenseByNight(Id.generate(), amount, description, payerId, Instant.now());
    }

    /**
     * Reconstitutes an ExpenseByNight from stored fields (used by persistence mapper).
     */
    public static ExpenseByNight fromJson(Id id, BigDecimal amount, String description, Participant.Id payerId,
            Instant createdAt) {
        return new ExpenseByNight(id, amount, description, payerId, createdAt);
    }

    /**
     * Package-private constructor for internal use.
     */
    ExpenseByNight(Id id, BigDecimal amount, String description, Participant.Id payerId, Instant createdAt) {
        super(id, amount, description, payerId, createdAt);
    }

    @Override
    public SplitMode getSplitMode() {
        return SplitMode.BY_NIGHT;
    }

    @Override
    public List<Share> getShares(Split split) {
        return calculateShares(split.getParticipants());
    }

    List<Share> calculateShares(List<Participant> participants) {
        if (participants == null || participants.isEmpty()) {
            return Collections.emptyList();
        }

        double totalNights = participants.stream().mapToDouble(p -> p.nights().value()).sum();

        if (totalNights == 0) {
            return Collections.emptyList();
        }

        List<Share> calculatedShares = new ArrayList<>();
        BigDecimal totalAssigned = BigDecimal.ZERO;

        for (int i = 0; i < participants.size(); i++) {
            Participant p = participants.get(i);
            BigDecimal share;

            if (i == participants.size() - 1) {
                // Last participant gets the remainder to ensure sum = amount
                share = getAmount().subtract(totalAssigned);
            } else {
                share = getAmount().multiply(BigDecimal.valueOf(p.nights().value()))
                        .divide(BigDecimal.valueOf(totalNights), SCALE, ROUNDING_MODE);
                totalAssigned = totalAssigned.add(share);
            }

            calculatedShares.add(Share.withAmount(p.id(), share));
        }

        return calculatedShares;
    }
}
