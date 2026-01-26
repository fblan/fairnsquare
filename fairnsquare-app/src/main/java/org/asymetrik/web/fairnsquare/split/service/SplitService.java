package org.asymetrik.web.fairnsquare.split.service;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.asymetrik.web.fairnsquare.sharedkernel.persistence.JsonFileRepository;
import org.asymetrik.web.fairnsquare.split.domain.AddParticipantRequest;
import org.asymetrik.web.fairnsquare.split.domain.CreateSplitRequest;
import org.asymetrik.web.fairnsquare.split.domain.Participant;
import org.asymetrik.web.fairnsquare.split.domain.Split;
import org.asymetrik.web.fairnsquare.split.domain.UpdateParticipantRequest;

/**
 * Service for managing splits.
 */
@ApplicationScoped
public class SplitService {

    private final JsonFileRepository repository;

    @Inject
    public SplitService(JsonFileRepository repository) {
        this.repository = repository;
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
}
