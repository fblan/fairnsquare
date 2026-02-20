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
 * Expense split proportionally based on number of persons. Share calculation: participant_numberOfPersons /
 * total_numberOfPersons × amount
 */
public final class ExpenseByPerson extends Expense {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    /**
     * Factory method for creating new BY_PERSON expenses.
     *
     * @param amount
     *            the expense amount (must be positive)
     * @param description
     *            the expense description (required, max 200 chars)
     * @param payerId
     *            the ID of the participant who paid
     *
     * @return a new ExpenseByPerson with generated ID and createdAt set to now
     */
    public static ExpenseByPerson create(BigDecimal amount, String description, Participant.Id payerId) {
        validateAmount(amount);
        validateDescription(description);
        return new ExpenseByPerson(Id.generate(), amount, description, payerId, Instant.now());
    }

    /**
     * Reconstitutes an ExpenseByPerson from stored fields (used by persistence mapper).
     */
    public static ExpenseByPerson fromJson(Id id, BigDecimal amount, String description, Participant.Id payerId,
            Instant createdAt) {
        return new ExpenseByPerson(id, amount, description, payerId, createdAt);
    }

    /**
     * Package-private constructor for internal use.
     */
    ExpenseByPerson(Id id, BigDecimal amount, String description, Participant.Id payerId, Instant createdAt) {
        super(id, amount, description, payerId, createdAt);
    }

    @Override
    public SplitMode getSplitMode() {
        return SplitMode.BY_PERSON;
    }

    @Override
    public List<Share> getShares(Split split) {
        return calculateShares(split.getParticipants());
    }

    List<Share> calculateShares(List<Participant> participants) {
        if (participants == null || participants.isEmpty()) {
            return Collections.emptyList();
        }

        double totalPersons = participants.stream().mapToDouble(p -> p.numberOfPersons().value()).sum();

        if (totalPersons == 0) {
            return Collections.emptyList();
        }

        List<Share> calculatedShares = new ArrayList<>();
        BigDecimal totalAssigned = BigDecimal.ZERO;

        for (int i = 0; i < participants.size(); i++) {
            Participant p = participants.get(i);
            double weight = p.numberOfPersons().value();
            BigDecimal share;

            if (i == participants.size() - 1) {
                // Last participant gets the remainder to ensure sum = amount
                share = getAmount().subtract(totalAssigned);
            } else {
                share = getAmount().multiply(BigDecimal.valueOf(weight)).divide(BigDecimal.valueOf(totalPersons), SCALE,
                        ROUNDING_MODE);
                totalAssigned = totalAssigned.add(share);
            }

            calculatedShares.add(Share.withAmount(p.id(), share));
        }

        return calculatedShares;
    }
}
