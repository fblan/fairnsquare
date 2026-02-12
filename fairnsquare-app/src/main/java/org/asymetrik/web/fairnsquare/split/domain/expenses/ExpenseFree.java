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
 * Expense with manually specified share parts per participant. Parts are stored and amounts are calculated
 * proportionally. Example: expense €100 with parts [Alice:2, Bob:3] calculates to amounts [Alice:€40, Bob:€60].
 */
public final class ExpenseFree extends Expense {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    private final List<Share> shares;

    /**
     * Factory method for creating new FREE expenses with manual parts specification.
     *
     * @param amount
     *            the expense amount (must be positive)
     * @param description
     *            the expense description (required, max 200 chars)
     * @param payerId
     *            the ID of the participant who paid
     * @param shares
     *            the shares with parts per participant (amounts will be calculated proportionally)
     *
     * @return a new ExpenseFree with generated ID and createdAt set to now
     *
     * @throws IllegalArgumentException
     *             if shares are null, empty, or have invalid parts
     */
    public static ExpenseFree create(BigDecimal amount, String description, Participant.Id payerId,
            List<Share> shares) {
        validateAmount(amount);
        validateDescription(description);
        validateShares(shares, amount);
        return new ExpenseFree(Id.generate(), amount, description, payerId, List.copyOf(shares), Instant.now());
    }

    /**
     * Reconstitutes an ExpenseFree from stored fields (used by persistence mapper).
     */
    public static ExpenseFree fromJson(Id id, BigDecimal amount, String description, Participant.Id payerId,
            List<Share> shares, Instant createdAt) {
        return new ExpenseFree(id, amount, description, payerId, List.copyOf(shares), createdAt);
    }

    /**
     * Package-private constructor for internal use.
     */
    ExpenseFree(Id id, BigDecimal amount, String description, Participant.Id payerId, List<Share> shares,
            Instant createdAt) {
        super(id, amount, description, payerId, createdAt);
        this.shares = shares;
    }

    @Override
    public SplitMode getSplitMode() {
        return SplitMode.FREE;
    }

    /**
     * Returns the raw shares with parts (used for persistence). Package-private to restrict access to persistence
     * layer.
     */
    public List<Share> getSharesWithParts() {
        return Collections.unmodifiableList(shares);
    }

    @Override
    public List<Share> getShares(Split split) {
        // Calculate amounts from parts proportionally
        BigDecimal totalParts = shares.stream().map(Share::parts).reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalParts.compareTo(BigDecimal.ZERO) == 0) {
            return Collections.emptyList();
        }

        List<Share> calculatedShares = new ArrayList<>();
        BigDecimal totalAssigned = BigDecimal.ZERO;

        for (int i = 0; i < shares.size(); i++) {
            Share partShare = shares.get(i);
            BigDecimal amount;

            if (i == shares.size() - 1) {
                // Last participant gets remainder to ensure sum equals expense amount
                amount = getAmount().subtract(totalAssigned);
            } else {
                // Calculate: amount = expenseTotal × (parts / totalParts)
                amount = getAmount().multiply(partShare.parts()).divide(totalParts, SCALE, ROUNDING_MODE);
                totalAssigned = totalAssigned.add(amount);
            }

            // Return share with both calculated amount AND original parts
            calculatedShares.add(Share.fromJson(partShare.participantId(), amount, partShare.parts()));
        }

        return calculatedShares;
    }

    /**
     * Validates that shares list is valid with positive parts.
     *
     * @param shares
     *            the shares to validate
     * @param amount
     *            the expense amount (not used for validation, kept for signature compatibility)
     *
     * @throws IllegalArgumentException
     *             if validation fails
     */
    private static void validateShares(List<Share> shares, BigDecimal amount) {
        if (shares == null) {
            throw new IllegalArgumentException("Shares cannot be null");
        }
        if (shares.isEmpty()) {
            throw new IllegalArgumentException("FREE mode expense must have at least one share");
        }

        // Validate all shares have parts specified
        for (Share share : shares) {
            if (share.parts() == null) {
                throw new IllegalArgumentException("FREE mode shares must have parts specified");
            }
            if (share.parts().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Share parts cannot be negative");
            }
        }

        // At least one share must have positive parts
        BigDecimal totalParts = shares.stream().map(Share::parts).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalParts.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("At least one share must have positive parts");
        }
    }
}
