package org.asymetrik.web.fairnsquare.split.api.mapper;

import org.asymetrik.web.fairnsquare.split.api.dto.ParticipantDTO;
import org.asymetrik.web.fairnsquare.split.domain.participant.Participant;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Mapper for converting Participant domain objects to DTOs for REST API responses.
 */
@ApplicationScoped
public class ParticipantMapper {

    /**
     * Converts a Participant domain object to a ParticipantDTO.
     *
     * @param participant
     *            the domain participant
     *
     * @return the DTO representation
     *
     * @throws NullPointerException
     *             if participant is null
     */
    public ParticipantDTO toDTO(Participant participant) {
        if (participant == null) {
            throw new NullPointerException("Participant cannot be null");
        }

        return new ParticipantDTO(participant.id().value(), participant.name().value(), participant.nights().value(),
                participant.share().value());
    }
}
