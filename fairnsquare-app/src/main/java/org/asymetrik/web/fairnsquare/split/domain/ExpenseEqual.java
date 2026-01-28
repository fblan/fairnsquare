package org.asymetrik.web.fairnsquare.split.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Expense split equally among all participants. Share calculation: amount / number of participants
 */
@JsonIgnoreProperties(ignoreUnknown = true)
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
    public static ExpenseEqual create(BigDecimal amount, String description, Participant.Id payerId,
            List<Share> shares) {
        validateAmount(amount);
        validateDescription(description);
        return new ExpenseEqual(Id.generate(), amount, description, payerId, Instant.now(), shares);
    }

    /**
     * Jackson constructor for deserialization.
     */
    @JsonCreator
    public static ExpenseEqual fromJson(@JsonProperty("id") Id id, @JsonProperty("amount") BigDecimal amount,
            @JsonProperty("description") String description, @JsonProperty("payerId") Participant.Id payerId,
            @JsonProperty("createdAt") Instant createdAt, @JsonProperty("shares") List<Share> shares) {
        return new ExpenseEqual(id, amount, description, payerId, createdAt, shares);
    }

    /**
     * Package-private constructor for internal use.
     */
    ExpenseEqual(Id id, BigDecimal amount, String description, Participant.Id payerId, Instant createdAt,
            List<Share> shares) {
        super(id, amount, description, payerId, createdAt, shares);
    }

    @Override
    public SplitMode getSplitMode() {
        return SplitMode.EQUAL;
    }

    @Override
    public List<Share> calculateShares(List<Participant> participants) {
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

            calculatedShares.add(new Share(p.id(), share));
        }

        return calculatedShares;
    }
}
