package org.asymetrik.web.fairnsquare.split.domain.settlement;

import java.math.BigDecimal;

import org.asymetrik.web.fairnsquare.split.domain.participant.Participant;

/**
 * Represents a proposed reimbursement transfer from a debtor to a creditor.
 */
public record Reimbursement(Participant.Id fromId, Participant.Id toId, BigDecimal amount) {

    @Override
    public String toString() {
        return "Reimbursement{from=%s, to=%s, amount=%s}".formatted(fromId, toId, amount);
    }
}
