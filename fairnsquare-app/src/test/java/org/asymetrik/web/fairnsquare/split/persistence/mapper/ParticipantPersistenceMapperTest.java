package org.asymetrik.web.fairnsquare.split.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.asymetrik.web.fairnsquare.split.domain.Participant;
import org.asymetrik.web.fairnsquare.split.persistence.dto.ParticipantPersistenceDTO;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class ParticipantPersistenceMapperTest {

    @Inject
    ParticipantPersistenceMapper mapper;

    @Test
    void shouldMapDomainToPersistenceDTO() {
        Participant participant = Participant.create("Alice", 3);

        ParticipantPersistenceDTO dto = mapper.toPersistenceDTO(participant);

        assertThat(dto.id()).isEqualTo(participant.id().value());
        assertThat(dto.name()).isEqualTo("Alice");
        assertThat(dto.nights()).isEqualTo(3);
    }

    @Test
    void shouldMapPersistenceDTOToDomain() {
        ParticipantPersistenceDTO dto = new ParticipantPersistenceDTO("ABCDEFGHIJKLMNOPQRSTu", "Bob", 5);

        Participant participant = mapper.toDomain(dto);

        assertThat(participant.id().value()).isEqualTo("ABCDEFGHIJKLMNOPQRSTu");
        assertThat(participant.name().value()).isEqualTo("Bob");
        assertThat(participant.nights().value()).isEqualTo(5);
    }

    @Test
    void shouldPreserveDataInRoundTrip() {
        Participant original = Participant.create("Charlie", 7);

        ParticipantPersistenceDTO dto = mapper.toPersistenceDTO(original);
        Participant roundTrip = mapper.toDomain(dto);

        assertThat(roundTrip.id()).isEqualTo(original.id());
        assertThat(roundTrip.name()).isEqualTo(original.name());
        assertThat(roundTrip.nights()).isEqualTo(original.nights());
    }
}
