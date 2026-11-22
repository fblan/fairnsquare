package org.asymetrik.web.fairnsquare.split.domain.expenses;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.List;

import org.asymetrik.web.fairnsquare.split.domain.Split;
import org.asymetrik.web.fairnsquare.split.domain.participant.Participant;

/**
 * Expense paid by one participant for another one only: the beneficiary bears the whole amount. Example: Alice pays
 * Bob's €40 train ticket, or Bob pays Alice back €40 in cash.
 */
public final class ExpenseReimbursement extends Expense {

    private final Participant.Id beneficiaryId;

    /**
     * Creates a new reimbursement. That the payer and the beneficiary are two participants of the split is checked by
     * the split ({@link Split#validateBeneficiary}).
     */
    public static ExpenseReimbursement create(BigDecimal amount, String description, Participant.Id payerId,
            Participant.Id beneficiaryId) {
        validateAmount(amount);
        validateDescription(description);
        return new ExpenseReimbursement(Id.generate(), amount, description, payerId, beneficiaryId, Instant.now());
    }

    /**
     * Reconstitutes a reimbursement from stored fields (persistence mapper, or an update keeping id and creation).
     */
    public static ExpenseReimbursement fromJson(Id id, BigDecimal amount, String description, Participant.Id payerId,
            Participant.Id beneficiaryId, Instant createdAt) {
        return new ExpenseReimbursement(id, amount, description, payerId, beneficiaryId, createdAt);
    }

    ExpenseReimbursement(Id id, BigDecimal amount, String description, Participant.Id payerId,
            Participant.Id beneficiaryId, Instant createdAt) {
        super(id, amount, description, payerId, createdAt);
        if (beneficiaryId == null) {
            throw new IllegalArgumentException("Beneficiary cannot be null");
        }
        this.beneficiaryId = beneficiaryId;
    }

    @Override
    public SplitMode getSplitMode() {
        return SplitMode.REIMBURSEMENT;
    }

    public Participant.Id getBeneficiaryId() {
        return beneficiaryId;
    }

    @Override
    public List<Share> getShares(Split split) {
        return List.of(Share.withAmount(beneficiaryId, getAmount()));
    }
}
