package org.asymetrik.web.fairnsquare.split.domain.settlement;

import java.math.BigDecimal;

import org.asymetrik.web.fairnsquare.split.domain.participant.Participant;

/**
 * Represents a proposed reimbursement transfer from a debtor to a creditor.
 */
public record Reimbursement(Participant.Id fromId, String fromName, Participant.Id toId, String toName,
        BigDecimal amount) {
}
