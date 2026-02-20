package org.asymetrik.web.fairnsquare.split.api.mapper;

import static org.assertj.core.api.Assertions.*;

import org.asymetrik.web.fairnsquare.split.api.dto.ParticipantDTO;
import org.asymetrik.web.fairnsquare.split.domain.participant.Participant;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ParticipantMapperTest {

    @Inject
    ParticipantMapper mapper;

    @Test
    void shouldMapParticipantToDTO() {
        Participant participant = Participant.create("Alice", 3, 2.5);

        ParticipantDTO dto = mapper.toDTO(participant);

        assertThat(dto.id()).isEqualTo(participant.id().value());
        assertThat(dto.name()).isEqualTo("Alice");
        assertThat(dto.nights()).isEqualTo(3);
        assertThat(dto.numberOfPersons()).isEqualTo(2.5);
    }

    @Test
    void shouldHandleNullInput() {
        assertThatNullPointerException().isThrownBy(() -> mapper.toDTO(null)).withMessage("Participant cannot be null");
    }
}
