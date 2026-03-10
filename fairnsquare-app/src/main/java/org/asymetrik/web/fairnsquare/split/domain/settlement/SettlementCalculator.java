package org.asymetrik.web.fairnsquare.split.domain.settlement;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.asymetrik.web.fairnsquare.split.domain.Split;
import org.asymetrik.web.fairnsquare.split.domain.expenses.Expense;
import org.asymetrik.web.fairnsquare.split.domain.participant.Participant;

/**
 * Computes settlement for a split: calculates per-participant balances and proposes reimbursement transfers using a
 * greedy min-cash-flow algorithm that sorts debtors and creditors by amount descending to produce deterministic,
 * intuitive results regardless of participant insertion order.
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
     * @return a Settlement containing balances and reimbursement proposals
     */
    public static Settlement calculate(Split split) {
        List<ParticipantBalance> balances = calculateBalances(split);
        List<Reimbursement> reimbursements = calculateReimbursements(balances, split.getParticipants());
        return new Settlement(balances, reimbursements);
    }

    /**
     * Calculates per-participant balances: totalPaid, totalCost, and net balance.
     */
    private static List<ParticipantBalance> calculateBalances(Split split) {
        Map<Participant.Id, BigDecimal> paid = new HashMap<>();
        Map<Participant.Id, BigDecimal> cost = new HashMap<>();

        // Initialize all participants with zero
        for (Participant p : split.getParticipants()) {
            paid.put(p.id(), BigDecimal.ZERO);
            cost.put(p.id(), BigDecimal.ZERO);
        }

        // Process each expense
        for (Expense expense : split.getExpenses()) {
            // Payer paid the full amount
            paid.merge(expense.getPayerId(), expense.getAmount(), BigDecimal::add);

            // Each participant owes their share
            for (Expense.Share share : expense.getShares(split)) {
                cost.merge(share.participantId(), share.amount(), BigDecimal::add);
            }
        }

        // Build balance list (preserving participant order)
        List<ParticipantBalance> balances = new ArrayList<>();
        for (Participant p : split.getParticipants()) {
            BigDecimal totalPaid = paid.getOrDefault(p.id(), BigDecimal.ZERO);
            BigDecimal totalCost = cost.getOrDefault(p.id(), BigDecimal.ZERO);
            BigDecimal balance = totalPaid.subtract(totalCost);
            balances.add(new ParticipantBalance(p.id(), totalPaid, totalCost, balance));
        }

        return balances;
    }

    /**
     * Greedy min-cash-flow algorithm with preferred-pairing support.
     * <p>
     * First honours each debtor's {@code preferredCreditorId}: if the preferred creditor is actually owed money in this
     * settlement, the debtor's reimbursement to them is processed before the general optimisation. The greedy
     * two-pointer then resolves all remaining balances in the usual largest-first order.
     * <p>
     * Processing preferred pairs before sorting ensures the user's choice is always respected, even when it is not the
     * "optimal" pairing from a minimise-transfers perspective.
     *
     * @param balances
     *            per-participant balances (paid minus cost)
     * @param participants
     *            participants list used to read {@code preferredCreditorId} values
     */
    private static List<Reimbursement> calculateReimbursements(List<ParticipantBalance> balances,
            List<Participant> participants) {
        // Separate into debtors (balance < 0) and creditors (balance > 0)
        List<ParticipantBalance> debtors = new ArrayList<>();
        List<BigDecimal> debtRemaining = new ArrayList<>();
        List<ParticipantBalance> creditors = new ArrayList<>();
        List<BigDecimal> creditRemaining = new ArrayList<>();

        for (ParticipantBalance pb : balances) {
            int cmp = pb.balance().compareTo(BigDecimal.ZERO);
            if (cmp < 0) {
                debtors.add(pb);
                debtRemaining.add(pb.balance().abs());
            } else if (cmp > 0) {
                creditors.add(pb);
                creditRemaining.add(pb.balance());
            }
        }

        // Sort largest debt/credit first for deterministic, intuitive results
        sortByAmountDescending(debtors, debtRemaining);
        sortByAmountDescending(creditors, creditRemaining);

        // Build lookup maps for preferred-pairing processing
        Map<Participant.Id, Participant.Id> preferredCreditorMap = new HashMap<>();
        for (Participant p : participants) {
            if (p.preferredCreditorId() != null) {
                preferredCreditorMap.put(p.id(), p.preferredCreditorId());
            }
        }
        Map<Participant.Id, Integer> creditorIndex = new HashMap<>();
        for (int i = 0; i < creditors.size(); i++) {
            creditorIndex.put(creditors.get(i).participantId(), i);
        }

        List<Reimbursement> reimbursements = new ArrayList<>();

        // Phase 1: honour preferred pairings before the greedy pass
        for (int d = 0; d < debtors.size(); d++) {
            Participant.Id debtorId = debtors.get(d).participantId();
            Participant.Id preferredId = preferredCreditorMap.get(debtorId);
            if (preferredId == null) {
                continue;
            }
            Integer ci = creditorIndex.get(preferredId);
            if (ci == null) {
                // Preferred creditor is not owed money in this settlement — skip silently
                continue;
            }
            BigDecimal transfer = debtRemaining.get(d).min(creditRemaining.get(ci));
            if (transfer.compareTo(BigDecimal.ZERO) > 0) {
                reimbursements.add(new Reimbursement(debtorId, preferredId, transfer));
                debtRemaining.set(d, debtRemaining.get(d).subtract(transfer));
                creditRemaining.set(ci, creditRemaining.get(ci).subtract(transfer));
            }
        }

        // Phase 2: greedy two-pointer for remaining balances
        int d = 0;
        int c = 0;

        while (d < debtors.size() && c < creditors.size()) {
            BigDecimal transfer = debtRemaining.get(d).min(creditRemaining.get(c));

            if (transfer.compareTo(BigDecimal.ZERO) > 0) {
                ParticipantBalance debtor = debtors.get(d);
                ParticipantBalance creditor = creditors.get(c);
                reimbursements.add(new Reimbursement(debtor.participantId(), creditor.participantId(), transfer));
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
     * Sorts participants and their corresponding remaining-amount list together by amount descending.
     */
    private static void sortByAmountDescending(List<ParticipantBalance> participants, List<BigDecimal> amounts) {
        int n = participants.size();
        for (int i = 0; i < n - 1; i++) {
            for (int j = i + 1; j < n; j++) {
                if (amounts.get(j).compareTo(amounts.get(i)) > 0) {
                    ParticipantBalance tmpP = participants.get(i);
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
