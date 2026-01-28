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
 * Expense split proportionally based on nights stayed. Share calculation: participant's nights / total nights × amount
 */
@JsonIgnoreProperties(ignoreUnknown = true)
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
    public static ExpenseByNight create(BigDecimal amount, String description, Participant.Id payerId,
            List<Share> shares) {
        validateAmount(amount);
        validateDescription(description);
        return new ExpenseByNight(Id.generate(), amount, description, payerId, Instant.now(), shares);
    }

    /**
     * Jackson constructor for deserialization.
     */
    @JsonCreator
    public static ExpenseByNight fromJson(@JsonProperty("id") Id id, @JsonProperty("amount") BigDecimal amount,
            @JsonProperty("description") String description, @JsonProperty("payerId") Participant.Id payerId,
            @JsonProperty("createdAt") Instant createdAt, @JsonProperty("shares") List<Share> shares) {
        return new ExpenseByNight(id, amount, description, payerId, createdAt, shares);
    }

    /**
     * Package-private constructor for internal use.
     */
    ExpenseByNight(Id id, BigDecimal amount, String description, Participant.Id payerId, Instant createdAt,
            List<Share> shares) {
        super(id, amount, description, payerId, createdAt, shares);
    }

    @Override
    public SplitMode getSplitMode() {
        return SplitMode.BY_NIGHT;
    }

    @Override
    public List<Share> calculateShares(List<Participant> participants) {
        if (participants == null || participants.isEmpty()) {
            return Collections.emptyList();
        }

        int totalNights = participants.stream().mapToInt(p -> p.nights().value()).sum();

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

            calculatedShares.add(new Share(p.id(), share));
        }

        return calculatedShares;
    }
}
