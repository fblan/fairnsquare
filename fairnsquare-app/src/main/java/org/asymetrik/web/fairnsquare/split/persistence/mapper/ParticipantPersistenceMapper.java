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
                participant.nights().value(), participant.numberOfPersons().value());
    }

    public Participant toDomain(ParticipantPersistenceDTO dto) {
        // Default to 1.0 for backward compatibility with existing data without numberOfPersons
        double numberOfPersons = dto.numberOfPersons() > 0 ? dto.numberOfPersons() : 1.0;
        return new Participant(Participant.Id.of(dto.id()), new Participant.Name(dto.name()),
                new Participant.Nights(dto.nights()), new Participant.NumberOfPersons(numberOfPersons));
    }
}
