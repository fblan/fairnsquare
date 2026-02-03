package org.asymetrik.web.fairnsquare.split.persistence.mapper;

import java.time.Instant;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.asymetrik.web.fairnsquare.split.domain.Split;
import org.asymetrik.web.fairnsquare.split.persistence.dto.ExpensePersistenceDTO;
import org.asymetrik.web.fairnsquare.split.persistence.dto.ParticipantPersistenceDTO;
import org.asymetrik.web.fairnsquare.split.persistence.dto.SplitPersistenceDTO;

/**
 * Bidirectional mapper between Split aggregate root and SplitPersistenceDTO. Delegates to ParticipantPersistenceMapper
 * and ExpensePersistenceMapper for nested entities.
 */
@ApplicationScoped
public class SplitPersistenceMapper {

    private final ParticipantPersistenceMapper participantMapper;
    private final ExpensePersistenceMapper expenseMapper;

    @Inject
    public SplitPersistenceMapper(ParticipantPersistenceMapper participantMapper,
            ExpensePersistenceMapper expenseMapper) {
        this.participantMapper = participantMapper;
        this.expenseMapper = expenseMapper;
    }

    public SplitPersistenceDTO toPersistenceDTO(Split split) {
        List<ParticipantPersistenceDTO> participants = split.getParticipants().stream()
                .map(participantMapper::toPersistenceDTO).toList();

        List<ExpensePersistenceDTO> expenses = split.getExpenses().stream().map(expenseMapper::toPersistenceDTO)
                .toList();

        return new SplitPersistenceDTO(split.getId().value(), split.getName().value(), split.getCreatedAt().toString(),
                participants, expenses);
    }

    public Split toDomain(SplitPersistenceDTO dto) {
        Split split = new Split(Split.Id.of(dto.id()), new Split.Name(dto.name()), Instant.parse(dto.createdAt()));

        if (dto.participants() != null) {
            dto.participants().forEach(p -> split.addParticipant(participantMapper.toDomain(p)));
        }

        if (dto.expenses() != null) {
            dto.expenses().forEach(e -> split.addExpense(expenseMapper.toDomain(e)));
        }

        return split;
    }
}
