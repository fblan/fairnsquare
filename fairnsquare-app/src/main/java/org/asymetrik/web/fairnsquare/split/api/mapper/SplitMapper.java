package org.asymetrik.web.fairnsquare.split.api.mapper;

import org.asymetrik.web.fairnsquare.expense.api.mapper.ExpenseMapper;
import org.asymetrik.web.fairnsquare.split.api.dto.SplitResponseDTO;
import org.asymetrik.web.fairnsquare.split.domain.Split;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Mapper for converting Split domain objects to DTOs for REST API responses.
 */
@ApplicationScoped
public class SplitMapper {

    private final ParticipantMapper participantMapper;
    private final ExpenseMapper expenseMapper;

    @Inject
    public SplitMapper(ParticipantMapper participantMapper, ExpenseMapper expenseMapper) {
        this.participantMapper = participantMapper;
        this.expenseMapper = expenseMapper;
    }

    /**
     * Converts a Split domain object to a SplitResponseDTO.
     *
     * @param split
     *            the domain split
     *
     * @return the DTO representation
     *
     * @throws NullPointerException
     *             if split is null
     */
    public SplitResponseDTO toDTO(Split split) {
        if (split == null) {
            throw new NullPointerException("Split cannot be null");
        }

        return new SplitResponseDTO(split.getId().value(), split.getName().value(), split.getCreatedAt().toString(),
                split.getParticipants().stream().map(participantMapper::toDTO).toList(),
                split.getExpenses().stream().map(e -> expenseMapper.toDTO(e, split)).toList());
    }
}
