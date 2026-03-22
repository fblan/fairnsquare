package org.asymetrik.web.fairnsquare.split.domain.settlement;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.asymetrik.web.fairnsquare.split.domain.participant.Participant;

/**
 * Sealed interface representing a participant as seen by the settlement algorithm.
 * <p>
 * Three variants:
 * <ul>
 * <li>{@link Standard} — a normal individual participant, used in settlement calculation.</li>
 * <li>{@link SharedAccount} — a group of participants sharing a bank account, treated as one entity in settlement
 * calculation. Its name is the alphabetically-sorted member names joined by {@code " & "}, its balance is the sum of
 * member balances.</li>
 * <li>{@link SharedAccountMember} — an individual who belongs to a {@link SharedAccount}. Excluded from settlement
 * calculation but kept to correctly display legacy settled reimbursements that reference individual participant
 * IDs.</li>
 * </ul>
 * <p>
 * SettlementParticipants are never persisted; they are computed from {@link Participant} records on demand.
 */
public sealed interface SettlementParticipant permits SettlementParticipant.Standard,
        SettlementParticipant.SharedAccount, SettlementParticipant.SharedAccountMember {

    String name();

    BigDecimal balance();

    /**
     * A normal individual participant — appears in both balance display and settlement calculation.
     */
    record Standard(Participant participant) implements SettlementParticipant {

        @Override
        public String name() {
            return participant.name().value();
        }

        @Override
        public BigDecimal balance() {
            return participant.balance();
        }
    }

    /**
     * A group of participants sharing a bank account. Treated as a single debtor/creditor during settlement
     * calculation.
     * <p>
     * Name is the alphabetically-sorted member names joined by {@code " & "}. Balance is the sum of member balances.
     */
    record SharedAccount(Participant.SharedAccountId id, List<Participant> members) implements SettlementParticipant {

        public SharedAccount {
            members = List.copyOf(members);
        }

        @Override
        public String name() {
            return members.stream().map(p -> p.name().value()).sorted().collect(Collectors.joining(" & "));
        }

        @Override
        public BigDecimal balance() {
            return members.stream().map(Participant::balance).reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }

    /**
     * An individual who is a member of a {@link SharedAccount}. Excluded from settlement calculation. Kept to display
     * legacy settled reimbursements that reference individual participant IDs from before shared accounts were
     * introduced.
     */
    record SharedAccountMember(Participant participant) implements SettlementParticipant {

        @Override
        public String name() {
            return participant.name().value();
        }

        @Override
        public BigDecimal balance() {
            return participant.balance();
        }
    }
}
