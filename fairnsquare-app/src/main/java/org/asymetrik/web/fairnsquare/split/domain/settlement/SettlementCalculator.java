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
        List<Reimbursement> reimbursements = calculateReimbursements(balances);
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
     * Greedy min-cash-flow algorithm: sorts debtors (largest debt first) and creditors (largest credit first), then
     * matches them with a two-pointer approach, transferring the minimum of each pair's remaining amount until all
     * debts are settled. Sorting makes results deterministic and independent of participant insertion order.
     */
    private static List<Reimbursement> calculateReimbursements(List<ParticipantBalance> balances) {
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

        List<Reimbursement> reimbursements = new ArrayList<>();
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
