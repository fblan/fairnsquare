package org.asymetrik.web.fairnsquare.split.api.mapper;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;

import org.asymetrik.web.fairnsquare.expense.api.mapper.ExpenseMapper;
import org.asymetrik.web.fairnsquare.split.api.dto.SplitResponseDTO;
import org.asymetrik.web.fairnsquare.split.domain.ExpenseByNight;
import org.asymetrik.web.fairnsquare.split.domain.Participant;
import org.asymetrik.web.fairnsquare.split.domain.Split;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
class SplitMapperTest {

    @Inject
    SplitMapper mapper;

    @Test
    void shouldMapSplitToDTO() {
        Split split = Split.create("Weekend Trip");
        Participant participant = Participant.create("Alice", 3);
        split.addParticipant(participant);
        ExpenseByNight expense = ExpenseByNight.create(BigDecimal.valueOf(100.00), "Hotel", participant.id());
        split.addExpense(expense);

        SplitResponseDTO dto = mapper.toDTO(split);

        assertThat(dto.id()).isEqualTo(split.getId().value());
        assertThat(dto.name()).isEqualTo("Weekend Trip");
        assertThat(dto.createdAt()).isNotNull();
        assertThat(dto.participants()).hasSize(1);
        assertThat(dto.participants().get(0).name()).isEqualTo("Alice");
        assertThat(dto.expenses()).hasSize(1);
        assertThat(dto.expenses().get(0).description()).isEqualTo("Hotel");
        assertThat(dto.expenses().get(0).shares()).hasSize(1);
        assertThat(dto.expenses().get(0).shares().get(0).participantId()).isEqualTo(participant.id().value());
    }

    @Test
    void shouldHandleEmptySplit() {
        Split split = Split.create("Empty Split");

        SplitResponseDTO dto = mapper.toDTO(split);

        assertThat(dto.participants()).isEmpty();
        assertThat(dto.expenses()).isEmpty();
    }

    @Test
    void shouldHandleNullInput() {
        assertThatNullPointerException().isThrownBy(() -> mapper.toDTO(null)).withMessage("Split cannot be null");
    }
}
