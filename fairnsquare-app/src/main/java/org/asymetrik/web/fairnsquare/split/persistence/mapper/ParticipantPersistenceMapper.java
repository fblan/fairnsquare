package org.asymetrik.web.fairnsquare.split.persistence.mapper;

import jakarta.enterprise.context.ApplicationScoped;

import org.asymetrik.web.fairnsquare.split.domain.participant.Participant;
import org.asymetrik.web.fairnsquare.split.persistence.dto.ParticipantPersistenceDTO;

/**
 * Bidirectional mapper between Participant domain object and ParticipantPersistenceDTO.
 */
@ApplicationScoped
public class ParticipantPersistenceMapper {

    public ParticipantPersistenceDTO toPersistenceDTO(Participant participant) {
        return new ParticipantPersistenceDTO(participant.id().value(), participant.name().value(),
                participant.nights().value());
    }

    public Participant toDomain(ParticipantPersistenceDTO dto) {
        return new Participant(Participant.Id.of(dto.id()), new Participant.Name(dto.name()),
                new Participant.Nights(dto.nights()));
    }
}
