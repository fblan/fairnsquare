package org.asymetrik.web.fairnsquare.split.domain.settlement;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.asymetrik.web.fairnsquare.split.domain.Split;

/**
 * Computes settlement for a split: calculates per-participant balances and proposes reimbursement transfers using a
 * greedy min-cash-flow algorithm that sorts debtors and creditors by amount descending to produce deterministic,
 * intuitive results regardless of participant insertion order.
 * <p>
 * Participants belonging to a {@link SettlementParticipant.SharedAccount} are grouped and treated as a single
 * debtor/creditor. {@link SettlementParticipant.SharedAccountMember}s are excluded from the calculation; they are kept
 * only to support display of legacy settled reimbursements.
 */
public class SettlementCalculator {

    private SettlementCalculator() {
    }

    /**
     * Calculates the full settlement for a split.
     *
     * @param split
     *            the split to calculate settlement for
     *
     * @return a Settlement containing per-participant balances and reimbursement proposals
     */
    public static Settlement calculate(Split split) {
        List<ParticipantBalance> balances = calculateBalances(split);
        List<SettlementParticipant> settlementParticipants = SettlementParticipantMapper.from(split.getParticipants());
        List<Reimbursement> reimbursements = calculateReimbursements(settlementParticipants);
        return new Settlement(balances, reimbursements);
    }

    /**
     * Reads per-participant balances directly from the participant fields maintained by the Split aggregate. The Split
     * keeps totalPaid, totalCost, and balance up-to-date after every mutation.
     */
    private static List<ParticipantBalance> calculateBalances(Split split) {
        return split.getParticipants().stream()
                .map(p -> new ParticipantBalance(p.id(), p.totalPaid(), p.totalCost(), p.balance())).toList();
    }

    /**
     * Greedy min-cash-flow algorithm operating on {@link SettlementParticipant}s.
     * <p>
     * Only {@link SettlementParticipant.Standard} and {@link SettlementParticipant.SharedAccount} participants are
     * considered. {@link SettlementParticipant.SharedAccountMember}s are excluded. Debtors and creditors are sorted by
     * amount descending for deterministic, intuitive results.
     */
    private static List<Reimbursement> calculateReimbursements(List<SettlementParticipant> settlementParticipants) {
        List<SettlementParticipant> debtors = new ArrayList<>();
        List<BigDecimal> debtRemaining = new ArrayList<>();
        List<SettlementParticipant> creditors = new ArrayList<>();
        List<BigDecimal> creditRemaining = new ArrayList<>();

        for (SettlementParticipant sp : settlementParticipants) {
            if (sp instanceof SettlementParticipant.SharedAccountMember) {
                continue;
            }
            int cmp = sp.balance().compareTo(BigDecimal.ZERO);
            if (cmp < 0) {
                debtors.add(sp);
                debtRemaining.add(sp.balance().abs());
            } else if (cmp > 0) {
                creditors.add(sp);
                creditRemaining.add(sp.balance());
            }
        }

        sortByAmountDescending(debtors, debtRemaining);
        sortByAmountDescending(creditors, creditRemaining);

        List<Reimbursement> reimbursements = new ArrayList<>();
        int d = 0;
        int c = 0;

        while (d < debtors.size() && c < creditors.size()) {
            BigDecimal transfer = debtRemaining.get(d).min(creditRemaining.get(c));

            if (transfer.compareTo(BigDecimal.ZERO) > 0) {
                reimbursements.add(new Reimbursement(toPartyId(debtors.get(d)), toPartyId(creditors.get(c)), transfer));
            }

            debtRemaining.set(d, debtRemaining.get(d).subtract(transfer));
            creditRemaining.set(c, creditRemaining.get(c).subtract(transfer));

            if (debtRemaining.get(d).compareTo(BigDecimal.ZERO) == 0) {
                d++;
            }
            if (creditRemaining.get(c).compareTo(BigDecimal.ZERO) == 0) {
                c++;
            }
        }

        return reimbursements;
    }

    /**
     * Converts a {@link SettlementParticipant} to its corresponding {@link SettlementPartyId}.
     */
    private static SettlementPartyId toPartyId(SettlementParticipant sp) {
        return switch (sp) {
            case SettlementParticipant.Standard s -> new SettlementPartyId.Individual(s.participant().id());
            case SettlementParticipant.SharedAccount sa -> new SettlementPartyId.Group(sa.id());
            case SettlementParticipant.SharedAccountMember m -> new SettlementPartyId.Individual(m.participant().id());
        };
    }

    /**
     * Sorts participants and their corresponding remaining-amount list together by amount descending.
     */
    private static void sortByAmountDescending(List<SettlementParticipant> participants, List<BigDecimal> amounts) {
        int n = participants.size();
        for (int i = 0; i < n - 1; i++) {
            for (int j = i + 1; j < n; j++) {
                if (amounts.get(j).compareTo(amounts.get(i)) > 0) {
                    SettlementParticipant tmpP = participants.get(i);
                    participants.set(i, participants.get(j));
                    participants.set(j, tmpP);
                    BigDecimal tmpA = amounts.get(i);
                    amounts.set(i, amounts.get(j));
                    amounts.set(j, tmpA);
                }
            }
        }
    }
}
