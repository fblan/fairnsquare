package org.asymetrik.web.fairnsquare.split.domain.settlement;

import java.math.BigDecimal;

/**
 * Represents a proposed reimbursement transfer from a debtor to a creditor.
 * <p>
 * Both {@code from} and {@code to} are {@link SettlementPartyId}s: either an {@link SettlementPartyId.Individual}
 * (single participant) or a {@link SettlementPartyId.Group} (shared-account group).
 */
public record Reimbursement(SettlementPartyId from, SettlementPartyId to, BigDecimal amount) {

    @Override
    public String toString() {
        return "Reimbursement{from=%s, to=%s, amount=%s}".formatted(from.value(), to.value(), amount);
    }
}
