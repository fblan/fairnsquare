package org.asymetrik.web.fairnsquare.split.domain.settlement;

import java.math.BigDecimal;

import org.asymetrik.web.fairnsquare.split.domain.participant.Participant;

/**
 * Represents a participant's financial balance in a split: total paid, total cost (owed), and net balance.
 */
public record ParticipantBalance(Participant.Id participantId, String participantName, BigDecimal totalPaid,
        BigDecimal totalCost, BigDecimal balance) {
}
