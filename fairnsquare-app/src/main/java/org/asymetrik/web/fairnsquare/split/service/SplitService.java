package org.asymetrik.web.fairnsquare.split.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.asymetrik.web.fairnsquare.sharedkernel.persistence.JsonFileRepository;
import org.asymetrik.web.fairnsquare.split.domain.AddExpenseRequest;
import org.asymetrik.web.fairnsquare.split.domain.AddParticipantRequest;
import org.asymetrik.web.fairnsquare.split.domain.CreateSplitRequest;
import org.asymetrik.web.fairnsquare.split.domain.Expense;
import org.asymetrik.web.fairnsquare.split.domain.Participant;
import org.asymetrik.web.fairnsquare.split.domain.Split;
import org.asymetrik.web.fairnsquare.split.domain.UpdateParticipantRequest;

/**
 * Service for managing splits.
 */
@ApplicationScoped
public class SplitService {

    private final JsonFileRepository repository;
    private final SplitCalculator splitCalculator;

    @Inject
    public SplitService(JsonFileRepository repository, SplitCalculator splitCalculator) {
        this.repository = repository;
        this.splitCalculator = splitCalculator;
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

        repository.save(split.getId().value(), split);

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
        return repository.load(splitId, Split.class);
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
        return repository.load(splitId, Split.class).map(split -> {
            Participant participant = Participant.create(request.name(), request.nights());
            split.addParticipant(participant);
            repository.save(splitId, split);
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
        return repository.load(splitId, Split.class).map(split -> {
            Participant.Id partId = Participant.Id.of(participantId);
            Participant updated = split.updateParticipant(partId, request.name(), request.nights());
            repository.save(splitId, split);
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
        return repository.load(splitId, Split.class).map(split -> {
            Participant.Id partId = Participant.Id.of(participantId);
            split.removeParticipant(partId);
            repository.save(splitId, split);
            return true;
        }).orElse(false);
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
     */
    public Optional<Expense> addExpense(String splitId, AddExpenseRequest request) {
        return repository.load(splitId, Split.class).map(split -> {
            // Validate payer exists
            Participant.Id payerId = Participant.Id.of(request.payerId());
            split.validatePayerExists(payerId);

            // Calculate shares based on split mode
            List<Expense.Share> shares = splitCalculator.calculateShares(request.amount(), request.splitMode(),
                    split.getParticipants());

            // Create and add expense
            Expense expense = Expense.create(request.amount(), request.description(), payerId, request.splitMode(),
                    shares);
            split.addExpense(expense);
            repository.save(splitId, split);

            return expense;
        });
    }
}
