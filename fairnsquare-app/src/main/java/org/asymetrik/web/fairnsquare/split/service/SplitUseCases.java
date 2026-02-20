package org.asymetrik.web.fairnsquare.split.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.asymetrik.web.fairnsquare.split.domain.expenses.Expense;
import org.asymetrik.web.fairnsquare.split.domain.expenses.ExpenseByNight;
import org.asymetrik.web.fairnsquare.split.domain.expenses.ExpenseByPerson;
import org.asymetrik.web.fairnsquare.split.domain.expenses.ExpenseEqual;
import org.asymetrik.web.fairnsquare.split.domain.expenses.ExpenseFree;
import org.asymetrik.web.fairnsquare.split.domain.expenses.InvalidSharesError;
import org.asymetrik.web.fairnsquare.split.domain.participant.Participant;
import org.asymetrik.web.fairnsquare.split.domain.participant.ParticipantNotFoundError;
import org.asymetrik.web.fairnsquare.split.domain.settlement.Settlement;
import org.asymetrik.web.fairnsquare.split.domain.settlement.SettlementCalculator;
import org.asymetrik.web.fairnsquare.split.domain.Split;
import org.asymetrik.web.fairnsquare.split.domain.UpdateExpenseRequest;
import org.asymetrik.web.fairnsquare.split.domain.UpdateParticipantRequest;
import org.asymetrik.web.fairnsquare.split.persistence.SplitRepository;
import org.asymetrik.web.fairnsquare.split.persistence.mapper.SplitPersistenceMapper;

/**
 * Service for managing splits.
 */
@ApplicationScoped
public class SplitUseCases {

    private final SplitRepository repository;
    private final SplitPersistenceMapper splitMapper;

    @Inject
    public SplitUseCases(SplitRepository repository, SplitPersistenceMapper splitMapper) {
        this.repository = repository;
        this.splitMapper = splitMapper;
    }

    /**
     * Creates a new split with a generated NanoID.
     *
     * @param request
     *            the create split request containing the split name
     *
     * @return the created split with generated ID and timestamp
     */
    public Split createSplit(CreateSplitRequest request) {
        Split split = Split.create(request.getName());

        repository.save(split);

        return split;
    }

    /**
     * Retrieves a split by its ID.
     *
     * @param splitId
     *            the split identifier
     *
     * @return an Optional containing the split if found, empty otherwise
     */
    public Optional<Split> getSplit(String splitId) {
        return repository.load(splitId);
    }

    /**
     * Calculates the settlement for a split, persists it in the split, and returns it.
     *
     * @param splitId
     *            the split identifier
     *
     * @return an Optional containing the settlement if the split exists, empty otherwise
     */
    public Optional<Settlement> calculateSettlement(String splitId) {
        return repository.load(splitId).map(split -> {
            Settlement settlement = SettlementCalculator.calculate(split);
            split.settle(settlement);
            repository.save(split);
            return settlement;
        });
    }

    /**
     * Checks if a split exists.
     *
     * @param splitId
     *            the split identifier
     *
     * @return true if the split exists, false otherwise
     */
    public boolean exists(String splitId) {
        return repository.exists(splitId);
    }

    /**
     * Adds a participant to an existing split.
     *
     * @param splitId
     *            the split identifier
     * @param request
     *            the add participant request
     *
     * @return an Optional containing the created participant if the split exists, empty otherwise
     */
    public Optional<Participant> addParticipant(String splitId, AddParticipantRequest request) {
        return repository.load(splitId).map(split -> {
            Participant participant = Participant.create(request.name(), request.nights(),
                    request.numberOfPersonsOrDefault());
            split.addParticipant(participant);
            repository.save(split);
            return participant;
        });
    }

    /**
     * Updates an existing participant in a split.
     *
     * @param splitId
     *            the split identifier
     * @param participantId
     *            the participant identifier
     * @param request
     *            the update participant request
     *
     * @return an Optional containing the updated participant if the split exists, empty otherwise. Throws
     *         ParticipantNotFoundError if the participant doesn't exist within the split.
     */
    public Optional<Participant> updateParticipant(String splitId, String participantId,
            UpdateParticipantRequest request) {
        return repository.load(splitId).map(split -> {
            Participant.Id partId = Participant.Id.of(participantId);
            Participant updated = split.updateParticipant(partId, request.name(), request.nights(),
                    request.numberOfPersonsOrDefault());
            repository.save(split);
            return updated;
        });
    }

    /**
     * Removes a participant from a split.
     *
     * @param splitId
     *            the split identifier
     * @param participantId
     *            the participant identifier
     *
     * @return true if the split exists and participant was removed, false if split not found. Throws
     *         ParticipantNotFoundError if the participant doesn't exist within the split. Throws
     *         ParticipantHasExpensesError if the participant has associated expenses.
     */
    public boolean removeParticipant(String splitId, String participantId) {
        return repository.load(splitId).map(split -> {
            Participant.Id partId = Participant.Id.of(participantId);
            split.removeParticipant(partId);
            repository.save(split);
            return true;
        }).orElse(false);
    }

    /**
     * Removes an expense from a split.
     *
     * @param splitId
     *            the split identifier
     * @param expenseId
     *            the expense identifier
     *
     * @return true if the split exists and expense was removed, false if split not found. Throws ExpenseNotFoundError
     *         if the expense doesn't exist within the split.
     */
    public boolean removeExpense(String splitId, String expenseId) {
        return repository.load(splitId).map(split -> {
            Expense.Id expId = Expense.Id.of(expenseId);
            split.removeExpense(expId);
            repository.save(split);
            return true;
        }).orElse(false);
    }

    /**
     * Updates an existing expense in a split.
     *
     * @param splitId
     *            the split identifier
     * @param expenseId
     *            the expense identifier
     * @param request
     *            the update expense request
     *
     * @return an Optional containing the updated expense if the split exists, empty otherwise. Throws
     *         ExpenseNotFoundError if the expense doesn't exist within the split.
     */
    public Optional<Expense> updateExpense(String splitId, String expenseId, UpdateExpenseRequest request) {
        return repository.load(splitId).map(split -> {
            Expense.Id expId = Expense.Id.of(expenseId);
            Participant.Id payerId = Participant.Id.of(request.payerId());
            Expense updated = split.updateExpense(expId, request.amount(), request.description(), payerId,
                    request.splitMode());
            repository.save(split);
            return updated;
        });
    }

    /**
     * Adds an expense to an existing split.
     *
     * @param splitId
     *            the split identifier
     * @param request
     *            the add expense request
     *
     * @return an Optional containing the created expense if the split exists, empty otherwise. Throws
     *         PayerNotFoundError if the payer is not a participant in the split.
     *
     * @deprecated Use addExpenseByNight(), addExpenseEqual(), or addExpenseFree() instead.
     */
    @Deprecated
    public Optional<Expense> addExpense(String splitId, AddExpenseRequest request) {
        return switch (request.splitMode()) {
            case BY_NIGHT ->
                    addExpenseByNight(splitId, request.amount(), request.description(), request.payerId()).map(e -> e);
            case BY_PERSON ->
                    addExpenseByPerson(splitId, request.amount(), request.description(), request.payerId()).map(e -> e);
            case EQUAL ->
                    addExpenseEqual(splitId, request.amount(), request.description(), request.payerId()).map(e -> e);
            case FREE -> throw new UnsupportedOperationException(
                    "FREE mode requires shares - use addExpenseFree() with AddFreeExpenseRequest");
        };
    }

    /**
     * Adds a BY_NIGHT expense to an existing split. Shares are calculated proportionally based on nights stayed.
     *
     * @param splitId
     *            the split identifier
     * @param amount
     *            the expense amount
     * @param description
     *            the expense description
     * @param payerId
     *            the ID of the participant who paid
     *
     * @return an Optional containing the created expense if the split exists, empty otherwise
     */
    public Optional<ExpenseByNight> addExpenseByNight(String splitId, BigDecimal amount, String description,
            String payerId) {
        return repository.load(splitId).map(split -> {
            Participant.Id payer = Participant.Id.of(payerId);
            split.validatePayerExists(payer);

            // Create expense and calculate shares using encapsulated logic
            ExpenseByNight expense = ExpenseByNight.create(amount, description, payer);
            split.addExpense(expense);
            repository.save(split);

            return expense;
        });
    }

    /**
     * Adds a BY_PERSON expense to an existing split. Shares are calculated proportionally based on number of persons.
     *
     * @param splitId
     *            the split identifier
     * @param amount
     *            the expense amount
     * @param description
     *            the expense description
     * @param payerId
     *            the ID of the participant who paid
     *
     * @return an Optional containing the created expense if the split exists, empty otherwise
     */
    public Optional<ExpenseByPerson> addExpenseByPerson(String splitId, BigDecimal amount, String description,
            String payerId) {
        return repository.load(splitId).map(split -> {
            Participant.Id payer = Participant.Id.of(payerId);
            split.validatePayerExists(payer);

            ExpenseByPerson expense = ExpenseByPerson.create(amount, description, payer);
            split.addExpense(expense);
            repository.save(split);

            return expense;
        });
    }

    /**
     * Adds an EQUAL expense to an existing split. Shares are calculated equally among all participants.
     *
     * @param splitId
     *            the split identifier
     * @param amount
     *            the expense amount
     * @param description
     *            the expense description
     * @param payerId
     *            the ID of the participant who paid
     *
     * @return an Optional containing the created expense if the split exists, empty otherwise
     */
    public Optional<ExpenseEqual> addExpenseEqual(String splitId, BigDecimal amount, String description,
            String payerId) {
        return repository.load(splitId).map(split -> {
            Participant.Id payer = Participant.Id.of(payerId);
            split.validatePayerExists(payer);

            // Create expense and calculate shares using encapsulated logic
            ExpenseEqual expense = ExpenseEqual.create(amount, description, payer);
            split.addExpense(expense);
            repository.save(split);

            return expense;
        });
    }

    /**
     * Adds a FREE expense with manually specified shares to an existing split.
     *
     * @param splitId
     *            the split identifier
     * @param request
     *            the add free expense request containing amount, description, payerId, and manual shares
     *
     * @return an Optional containing the created expense if the split exists, empty otherwise. Throws
     *         InvalidSharesError if shares don't sum to amount or if any participant ID is invalid.
     */
    public Optional<ExpenseFree> addExpenseFree(String splitId, AddFreeExpenseRequest request) {
        return repository.load(splitId).map(split -> {
            Participant.Id payer = Participant.Id.of(request.payerId());
            split.validatePayerExists(payer);

            // Validate all share participant IDs exist in split
            for (AddFreeExpenseRequest.ShareRequest shareReq : request.shares()) {
                try {
                    Participant.Id participantId = Participant.Id.of(shareReq.participantId());
                    split.getParticipant(participantId);
                } catch (ParticipantNotFoundError e) {
                    throw new InvalidSharesError("Participant with ID '" + shareReq.participantId()
                            + "' not found in split. All share participants must exist.");
                }
            }

            // AC8: Validate all split participants have a share specified
            Set<String> shareParticipantIds = request.shares().stream()
                    .map(AddFreeExpenseRequest.ShareRequest::participantId).collect(Collectors.toSet());
            for (Participant participant : split.getParticipants()) {
                if (!shareParticipantIds.contains(participant.id().value())) {
                    throw new InvalidSharesError(
                            "All participants must have a share specified. Missing share for participant '"
                                    + participant.name().value() + "'.");
                }
            }

            // Convert ShareRequest to Expense.Share with parts
            List<Expense.Share> shares = request.shares().stream()
                    .map(sr -> Expense.Share.withParts(Participant.Id.of(sr.participantId()), sr.parts())).toList();

            // Create expense with manual shares (validation happens in ExpenseFree.create())
            try {
                ExpenseFree expense = ExpenseFree.create(request.amount(), request.description(), payer, shares);
                split.addExpense(expense);
                repository.save(split);
                return expense;
            } catch (IllegalArgumentException e) {
                throw new InvalidSharesError(e.getMessage());
            }
        });
    }
}
