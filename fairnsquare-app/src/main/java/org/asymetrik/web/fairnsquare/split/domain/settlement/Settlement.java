package org.asymetrik.web.fairnsquare.split.domain.settlement;

import java.util.List;

/**
 * Computed settlement for a split: participant balances and proposed reimbursement transfers. This is a read-only view
 * — not persisted.
 */
public record Settlement(List<ParticipantBalance> balances, List<Reimbursement> reimbursements) {

    @Override
    public String toString() {
        return "Settlement{balances=%d, reimbursements=%d}".formatted(balances.size(), reimbursements.size());
    }
}
