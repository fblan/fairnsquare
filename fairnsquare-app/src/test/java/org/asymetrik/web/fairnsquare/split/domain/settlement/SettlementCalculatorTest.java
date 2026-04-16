package org.asymetrik.web.fairnsquare.split.domain.settlement;

import java.math.BigDecimal;
import java.util.List;

import org.asymetrik.web.fairnsquare.split.domain.Split;
import org.asymetrik.web.fairnsquare.split.domain.expenses.Expense;
import org.asymetrik.web.fairnsquare.split.domain.expenses.ExpenseByNight;
import org.asymetrik.web.fairnsquare.split.domain.expenses.ExpenseFree;
import org.asymetrik.web.fairnsquare.split.domain.participant.Participant;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for SettlementCalculator: balance computation and greedy reimbursement algorithm.
 */
class SettlementCalculatorTest {

    // --- Balance Calculation ---

    @Test
    void calculate_emptyExpenses_allBalancesZero() {
        Split split = createSplit();
        Participant alice = Participant.create("Alice", 3);
        Participant bob = Participant.create("Bob", 3);
        split.addParticipant(alice);
        split.addParticipant(bob);

        Settlement settlement = SettlementCalculator.calculate(split);

        assertThat(settlement.balances()).hasSize(2);
        assertThat(settlement.balances().get(0).totalPaid()).isEqualByComparingTo("0");
        assertThat(settlement.balances().get(0).totalCost()).isEqualByComparingTo("0");
        assertThat(settlement.balances().get(0).balance()).isEqualByComparingTo("0");
        assertThat(settlement.reimbursements()).isEmpty();
    }

    @Test
    void calculate_noParticipants_emptySettlement() {
        Split split = createSplit();

        Settlement settlement = SettlementCalculator.calculate(split);

        assertThat(settlement.balances()).isEmpty();
        assertThat(settlement.reimbursements()).isEmpty();
    }

    @Test
    void calculate_singlePayerEqualExpense_correctBalances() {
        // Alice pays €100 split equally between Alice and Bob
        Split split = createSplit();
        Participant alice = Participant.create("Alice", 1);
        Participant bob = Participant.create("Bob", 1);
        split.addParticipant(alice);
        split.addParticipant(bob);

        split.addExpense(equalFreeExpense(new BigDecimal("100.00"), "Dinner", alice.id(), alice, bob));

        Settlement settlement = SettlementCalculator.calculate(split);

        assertThat(settlement.balances()).hasSize(2);

        // Alice: paid 100, cost 50, balance +50
        ParticipantBalance aliceBalance = settlement.balances().get(0);
        assertThat(aliceBalance.participantId()).isEqualTo(alice.id());
        assertThat(aliceBalance.totalPaid()).isEqualByComparingTo("100.00");
        assertThat(aliceBalance.totalCost()).isEqualByComparingTo("50.00");
        assertThat(aliceBalance.balance()).isEqualByComparingTo("50.00");

        // Bob: paid 0, cost 50, balance -50
        ParticipantBalance bobBalance = settlement.balances().get(1);
        assertThat(bobBalance.participantId()).isEqualTo(bob.id());
        assertThat(bobBalance.totalPaid()).isEqualByComparingTo("0");
        assertThat(bobBalance.totalCost()).isEqualByComparingTo("50.00");
        assertThat(bobBalance.balance()).isEqualByComparingTo("-50.00");
    }

    @Test
    void calculate_multipleExpenses_accumulatesCorrectly() {
        // Alice pays €60, Bob pays €40 — split equally among 2
        Split split = createSplit();
        Participant alice = Participant.create("Alice", 1);
        Participant bob = Participant.create("Bob", 1);
        split.addParticipant(alice);
        split.addParticipant(bob);

        split.addExpense(equalFreeExpense(new BigDecimal("60.00"), "Groceries", alice.id(), alice, bob));
        split.addExpense(equalFreeExpense(new BigDecimal("40.00"), "Gas", bob.id(), alice, bob));

        Settlement settlement = SettlementCalculator.calculate(split);

        // Total: 100. Each owes 50.
        // Alice: paid 60, cost 50, balance +10
        ParticipantBalance aliceBalance = settlement.balances().get(0);
        assertThat(aliceBalance.totalPaid()).isEqualByComparingTo("60.00");
        assertThat(aliceBalance.totalCost()).isEqualByComparingTo("50.00");
        assertThat(aliceBalance.balance()).isEqualByComparingTo("10.00");

        // Bob: paid 40, cost 50, balance -10
        ParticipantBalance bobBalance = settlement.balances().get(1);
        assertThat(bobBalance.totalPaid()).isEqualByComparingTo("40.00");
        assertThat(bobBalance.totalCost()).isEqualByComparingTo("50.00");
        assertThat(bobBalance.balance()).isEqualByComparingTo("-10.00");
    }

    @Test
    void calculate_byNightExpense_proportionalCost() {
        // Alice 4 nights, Bob 2 nights. Alice pays €180 BY_NIGHT.
        Split split = createSplit();
        Participant alice = Participant.create("Alice", 4);
        Participant bob = Participant.create("Bob", 2);
        split.addParticipant(alice);
        split.addParticipant(bob);

        split.addExpense(ExpenseByNight.create(new BigDecimal("180.00"), "Hotel", alice.id()));

        Settlement settlement = SettlementCalculator.calculate(split);

        // Alice: paid 180, cost 120 (4/6 * 180), balance +60
        ParticipantBalance aliceBalance = settlement.balances().get(0);
        assertThat(aliceBalance.totalPaid()).isEqualByComparingTo("180.00");
        assertThat(aliceBalance.totalCost()).isEqualByComparingTo("120.00");
        assertThat(aliceBalance.balance()).isEqualByComparingTo("60.00");

        // Bob: paid 0, cost 60 (2/6 * 180), balance -60
        ParticipantBalance bobBalance = settlement.balances().get(1);
        assertThat(bobBalance.totalPaid()).isEqualByComparingTo("0");
        assertThat(bobBalance.totalCost()).isEqualByComparingTo("60.00");
        assertThat(bobBalance.balance()).isEqualByComparingTo("-60.00");
    }

    // --- Reimbursement Algorithm ---

    @Test
    void calculate_simpleDebt_singleReimbursement() {
        // Alice pays €100 split equally, Bob owes Alice €50
        Split split = createSplit();
        Participant alice = Participant.create("Alice", 1);
        Participant bob = Participant.create("Bob", 1);
        split.addParticipant(alice);
        split.addParticipant(bob);

        split.addExpense(equalFreeExpense(new BigDecimal("100.00"), "Dinner", alice.id(), alice, bob));

        Settlement settlement = SettlementCalculator.calculate(split);

        assertThat(settlement.reimbursements()).hasSize(1);
        Reimbursement r = settlement.reimbursements().get(0);
        assertThat(r.from()).isEqualTo(new SettlementPartyId.Individual(bob.id()));
        assertThat(r.to()).isEqualTo(new SettlementPartyId.Individual(alice.id()));
        assertThat(r.amount()).isEqualByComparingTo("50.00");
    }

    @Test
    void calculate_threeParticipants_multipleReimbursements() {
        // Alice pays €150 split equally among 3. Each owes 50.
        // Alice: +100, Bob: -50, Charlie: -50
        Split split = createSplit();
        Participant alice = Participant.create("Alice", 1);
        Participant bob = Participant.create("Bob", 1);
        Participant charlie = Participant.create("Charlie", 1);
        split.addParticipant(alice);
        split.addParticipant(bob);
        split.addParticipant(charlie);

        split.addExpense(equalFreeExpense(new BigDecimal("150.00"), "Hotel", alice.id(), alice, bob, charlie));

        Settlement settlement = SettlementCalculator.calculate(split);

        assertThat(settlement.reimbursements()).hasSize(2);
        // Bob pays Alice 50
        assertThat(settlement.reimbursements().get(0).from()).isEqualTo(new SettlementPartyId.Individual(bob.id()));
        assertThat(settlement.reimbursements().get(0).to()).isEqualTo(new SettlementPartyId.Individual(alice.id()));
        assertThat(settlement.reimbursements().get(0).amount()).isEqualByComparingTo("50.00");
        // Charlie pays Alice 50
        assertThat(settlement.reimbursements().get(1).from()).isEqualTo(new SettlementPartyId.Individual(charlie.id()));
        assertThat(settlement.reimbursements().get(1).to()).isEqualTo(new SettlementPartyId.Individual(alice.id()));
        assertThat(settlement.reimbursements().get(1).amount()).isEqualByComparingTo("50.00");
    }

    @Test
    void calculate_allSettled_noReimbursements() {
        // Both pay equal amounts split equally → everyone settled
        Split split = createSplit();
        Participant alice = Participant.create("Alice", 1);
        Participant bob = Participant.create("Bob", 1);
        split.addParticipant(alice);
        split.addParticipant(bob);

        split.addExpense(equalFreeExpense(new BigDecimal("50.00"), "Groceries", alice.id(), alice, bob));
        split.addExpense(equalFreeExpense(new BigDecimal("50.00"), "Gas", bob.id(), alice, bob));

        Settlement settlement = SettlementCalculator.calculate(split);

        assertThat(settlement.reimbursements()).isEmpty();

        // Both balances should be zero
        assertThat(settlement.balances().get(0).balance()).isEqualByComparingTo("0");
        assertThat(settlement.balances().get(1).balance()).isEqualByComparingTo("0");
    }

    @Test
    void calculate_greedyAlgorithm_splitsDebtAcrossCreditors() {
        // Alice pays €30, Bob pays €60, Charlie pays €0
        // Equal split: each owes 30
        // Balances: Alice 0, Bob +30, Charlie -30
        // Charlie should pay Bob €30
        Split split = createSplit();
        Participant alice = Participant.create("Alice", 1);
        Participant bob = Participant.create("Bob", 1);
        Participant charlie = Participant.create("Charlie", 1);
        split.addParticipant(alice);
        split.addParticipant(bob);
        split.addParticipant(charlie);

        split.addExpense(equalFreeExpense(new BigDecimal("30.00"), "Snacks", alice.id(), alice, bob, charlie));
        split.addExpense(equalFreeExpense(new BigDecimal("60.00"), "Dinner", bob.id(), alice, bob, charlie));

        Settlement settlement = SettlementCalculator.calculate(split);

        // Total: 90. Each owes 30.
        // Alice: paid 30, cost 30, balance 0
        // Bob: paid 60, cost 30, balance +30
        // Charlie: paid 0, cost 30, balance -30
        assertThat(settlement.reimbursements()).hasSize(1);
        assertThat(settlement.reimbursements().get(0).from()).isEqualTo(new SettlementPartyId.Individual(charlie.id()));
        assertThat(settlement.reimbursements().get(0).to()).isEqualTo(new SettlementPartyId.Individual(bob.id()));
        assertThat(settlement.reimbursements().get(0).amount()).isEqualByComparingTo("30.00");
    }

    @Test
    void calculate_preservesParticipantOrder() {
        Split split = createSplit();
        Participant charlie = Participant.create("Charlie", 1);
        Participant alice = Participant.create("Alice", 1);
        Participant bob = Participant.create("Bob", 1);
        split.addParticipant(charlie);
        split.addParticipant(alice);
        split.addParticipant(bob);

        Settlement settlement = SettlementCalculator.calculate(split);

        assertThat(settlement.balances().get(0).participantId()).isEqualTo(charlie.id());
        assertThat(settlement.balances().get(1).participantId()).isEqualTo(alice.id());
        assertThat(settlement.balances().get(2).participantId()).isEqualTo(bob.id());
    }

    @Test
    void calculate_largestDebtSettledFirst_regardlessOfInsertionOrder() {
        Split split = createSplit();
        Participant a = Participant.create("Alice", 1); // will owe €10
        Participant b = Participant.create("Bob", 1); // will owe €2
        Participant c = Participant.create("Charlie", 1); // will be owed €7
        Participant d = Participant.create("Dave", 1); // will be owed €5
        split.addParticipant(a);
        split.addParticipant(b);
        split.addParticipant(c);
        split.addParticipant(d);

        // Total expenses = 48: each owes 12.
        // Charlie pays 19 → balance +7. Dave pays 17 → balance +5.
        // Alice pays 2 → balance -10. Bob pays 10 → balance -2.
        split.addExpense(equalFreeExpense(new BigDecimal("19.00"), "Groceries", c.id(), a, b, c, d));
        split.addExpense(equalFreeExpense(new BigDecimal("17.00"), "Gas", d.id(), a, b, c, d));
        split.addExpense(equalFreeExpense(new BigDecimal("2.00"), "Coffee", a.id(), a, b, c, d));
        split.addExpense(equalFreeExpense(new BigDecimal("10.00"), "Snacks", b.id(), a, b, c, d));

        Settlement settlement = SettlementCalculator.calculate(split);

        // Sorted: debtors [Alice(-10), Bob(-2)], creditors [Charlie(+7), Dave(+5)]
        // Alice → Charlie 7 (Charlie done, Alice has 3 left)
        // Alice → Dave 3 (Dave has 2 left, Alice done)
        // Bob → Dave 2 (Dave done, Bob done)
        assertThat(settlement.reimbursements()).hasSize(3);
        assertThat(settlement.reimbursements().get(0).from()).isEqualTo(new SettlementPartyId.Individual(a.id()));
        assertThat(settlement.reimbursements().get(0).to()).isEqualTo(new SettlementPartyId.Individual(c.id()));
        assertThat(settlement.reimbursements().get(0).amount()).isEqualByComparingTo("7.00");

        assertThat(settlement.reimbursements().get(1).from()).isEqualTo(new SettlementPartyId.Individual(a.id()));
        assertThat(settlement.reimbursements().get(1).to()).isEqualTo(new SettlementPartyId.Individual(d.id()));
        assertThat(settlement.reimbursements().get(1).amount()).isEqualByComparingTo("3.00");

        assertThat(settlement.reimbursements().get(2).from()).isEqualTo(new SettlementPartyId.Individual(b.id()));
        assertThat(settlement.reimbursements().get(2).to()).isEqualTo(new SettlementPartyId.Individual(d.id()));
        assertThat(settlement.reimbursements().get(2).amount()).isEqualByComparingTo("2.00");
    }

    @Test
    void calculate_sortingIsDeterministic_regardlessOfInsertionOrder() {
        Split split = createSplit();
        Participant b = Participant.create("Bob", 1); // will owe €2
        Participant a = Participant.create("Alice", 1); // will owe €10
        Participant d = Participant.create("Dave", 1); // will be owed €5
        Participant c = Participant.create("Charlie", 1); // will be owed €7
        split.addParticipant(b);
        split.addParticipant(a);
        split.addParticipant(d);
        split.addParticipant(c);

        split.addExpense(equalFreeExpense(new BigDecimal("19.00"), "Groceries", c.id(), b, a, d, c));
        split.addExpense(equalFreeExpense(new BigDecimal("17.00"), "Gas", d.id(), b, a, d, c));
        split.addExpense(equalFreeExpense(new BigDecimal("2.00"), "Coffee", a.id(), b, a, d, c));
        split.addExpense(equalFreeExpense(new BigDecimal("10.00"), "Snacks", b.id(), b, a, d, c));

        Settlement settlement = SettlementCalculator.calculate(split);

        // Regardless of insertion order, sorting ensures:
        // largest debtor (Alice -10) pays largest creditor (Charlie +7) first
        assertThat(settlement.reimbursements()).hasSize(3);

        Reimbursement first = settlement.reimbursements().get(0);
        assertThat(first.amount()).isEqualByComparingTo("7.00"); // largest single transaction first
        assertThat(first.to()).isEqualTo(new SettlementPartyId.Individual(c.id())); // largest creditor (Charlie +7)
        assertThat(first.from()).isEqualTo(new SettlementPartyId.Individual(a.id())); // largest debtor (Alice -10)
    }

    // --- Shared Account ---

    @Test
    void calculate_sharedAccount_treatedAsSingleDebtor() {
        // Alice paid €150 equally among 3. Bob and Charlie share an account (same group).
        // Individual balances: Alice +100, Bob -50, Charlie -50.
        // Settlement participants: Alice (Standard), BobCharlie (SharedAccount, balance -100).
        // Expected: BobCharlie → Alice (100).
        Split split = createSplit();
        Participant alice = Participant.create("Alice", 1);
        Participant bob = Participant.create("Bob", 1);
        Participant charlie = Participant.create("Charlie", 1);
        split.addParticipant(alice);
        split.addParticipant(bob);
        split.addParticipant(charlie);
        split.addExpense(equalFreeExpense(new BigDecimal("150.00"), "Hotel", alice.id(), alice, bob, charlie));

        // Bob and Charlie share an account
        Participant.SharedAccountId groupId = Participant.SharedAccountId.generate();
        split.updateParticipant(bob.id(), bob.name().value(), bob.nights().value(), bob.share().value(), groupId);
        split.updateParticipant(charlie.id(), charlie.name().value(), charlie.nights().value(), charlie.share().value(),
                groupId);

        Settlement settlement = SettlementCalculator.calculate(split);

        // Individual balances: 3 entries (Alice, Bob, Charlie)
        assertThat(settlement.balances()).hasSize(3);

        // One reimbursement: the shared account group → Alice
        assertThat(settlement.reimbursements()).hasSize(1);
        Reimbursement r = settlement.reimbursements().get(0);
        assertThat(r.from()).isEqualTo(new SettlementPartyId.Group(groupId));
        assertThat(r.to()).isEqualTo(new SettlementPartyId.Individual(alice.id()));
        assertThat(r.amount()).isEqualByComparingTo("100.00");
    }

    @Test
    void calculate_sharedAccount_treatedAsSingleCreditor() {
        // Alice and Bob share an account. Alice paid €150 equally among 3.
        // Individual balances: Alice +100, Bob -50, Charlie -50.
        // SharedAccount balance: Alice+Bob = +50. Charlie = -50.
        // Settlement: Charlie → SharedAccount(50).
        Split split = createSplit();
        Participant alice = Participant.create("Alice", 1);
        Participant bob = Participant.create("Bob", 1);
        Participant charlie = Participant.create("Charlie", 1);
        split.addParticipant(alice);
        split.addParticipant(bob);
        split.addParticipant(charlie);
        split.addExpense(equalFreeExpense(new BigDecimal("150.00"), "Hotel", alice.id(), alice, bob, charlie));

        Participant.SharedAccountId groupId = Participant.SharedAccountId.generate();
        split.updateParticipant(alice.id(), alice.name().value(), alice.nights().value(), alice.share().value(),
                groupId);
        split.updateParticipant(bob.id(), bob.name().value(), bob.nights().value(), bob.share().value(), groupId);

        Settlement settlement = SettlementCalculator.calculate(split);

        // One reimbursement: Charlie → SharedAccount
        assertThat(settlement.reimbursements()).hasSize(1);
        Reimbursement r = settlement.reimbursements().get(0);
        assertThat(r.from()).isEqualTo(new SettlementPartyId.Individual(charlie.id()));
        assertThat(r.to()).isEqualTo(new SettlementPartyId.Group(groupId));
        assertThat(r.amount()).isEqualByComparingTo("50.00");
    }

    @Test
    void calculate_singletonSharedAccount_treatedAsStandard() {
        // Bob has a sharedAccountId but is the only participant in that group.
        // Should produce the same result as if he had no sharedAccountId.
        Split split = createSplit();
        Participant alice = Participant.create("Alice", 1);
        Participant bob = Participant.create("Bob", 1);
        split.addParticipant(alice);
        split.addParticipant(bob);
        split.addExpense(equalFreeExpense(new BigDecimal("100.00"), "Dinner", alice.id(), alice, bob));

        split.updateParticipant(bob.id(), bob.name().value(), bob.nights().value(), bob.share().value(),
                Participant.SharedAccountId.generate());

        Settlement settlement = SettlementCalculator.calculate(split);

        assertThat(settlement.reimbursements()).hasSize(1);
        // Bob is treated as Standard → Individual party ID
        assertThat(settlement.reimbursements().get(0).from()).isEqualTo(new SettlementPartyId.Individual(bob.id()));
        assertThat(settlement.reimbursements().get(0).to()).isEqualTo(new SettlementPartyId.Individual(alice.id()));
        assertThat(settlement.reimbursements().get(0).amount()).isEqualByComparingTo("50.00");
    }

    private Split createSplit() {
        return Split.create("Test Split");
    }

    private ExpenseFree equalFreeExpense(BigDecimal amount, String description, Participant.Id payerId,
            Participant... participants) {
        List<Expense.Share> shares = java.util.Arrays.stream(participants)
                .map(p -> Expense.Share.withParts(p.id(), BigDecimal.ONE)).toList();
        return ExpenseFree.create(amount, description, payerId, shares);
    }
}
