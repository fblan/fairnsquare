package org.asymetrik.web.fairnsquare.split.domain.settlement;

import org.asymetrik.web.fairnsquare.split.domain.participant.Participant;

/**
 * Sealed identifier for a party in a {@link Reimbursement}.
 * <p>
 * A party is either an {@link Individual} (a single participant) or a {@link Group} (a shared-account group). Both
 * variants expose a {@link #value()} method returning the underlying NanoId string for serialisation.
 */
public sealed interface SettlementPartyId permits SettlementPartyId.Individual, SettlementPartyId.Group {

    /**
     * Returns the raw NanoId string of this party, regardless of whether it is an individual or a group.
     */
    String value();

    /**
     * A single participant identified by their {@link Participant.Id}.
     */
    record Individual(Participant.Id participantId) implements SettlementPartyId {

        @Override
        public String value() {
            return participantId.value();
        }
    }

    /**
     * A shared-account group identified by its {@link Participant.SharedAccountId}.
     */
    record Group(Participant.SharedAccountId groupId) implements SettlementPartyId {

        @Override
        public String value() {
            return groupId.value();
        }
    }
}
