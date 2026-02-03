package org.asymetrik.web.fairnsquare.split.persistence.mapper;

import java.time.Instant;

import jakarta.enterprise.context.ApplicationScoped;

import org.asymetrik.web.fairnsquare.split.domain.Expense;
import org.asymetrik.web.fairnsquare.split.domain.ExpenseByNight;
import org.asymetrik.web.fairnsquare.split.domain.ExpenseEqual;
import org.asymetrik.web.fairnsquare.split.domain.Participant;
import org.asymetrik.web.fairnsquare.split.persistence.dto.ExpenseByNightPersistenceDTO;
import org.asymetrik.web.fairnsquare.split.persistence.dto.ExpenseEqualPersistenceDTO;
import org.asymetrik.web.fairnsquare.split.persistence.dto.ExpensePersistenceDTO;

/**
 * Bidirectional mapper between Expense domain hierarchy and ExpensePersistenceDTO hierarchy. Shares are not persisted —
 * they are recalculated from participant data on load.
 */
@ApplicationScoped
public class ExpensePersistenceMapper {

    public ExpensePersistenceDTO toPersistenceDTO(Expense expense) {
        String id = expense.getId() != null ? expense.getId().value() : null;
        String payerId = expense.getPayerId() != null ? expense.getPayerId().value() : null;
        String createdAt = expense.getCreatedAt() != null ? expense.getCreatedAt().toString() : null;

        return switch (expense) {
            case ExpenseByNight _ -> new ExpenseByNightPersistenceDTO(id, expense.getAmount(), expense.getDescription(),
                    payerId, createdAt);
            case ExpenseEqual _ -> new ExpenseEqualPersistenceDTO(id, expense.getAmount(), expense.getDescription(),
                    payerId, createdAt);
        };
    }

    /**
     * Converts a persistence DTO back to a domain Expense. Shares are not stored — they are recalculated lazily via
     * {@code expense.getShares(split)} when needed.
     *
     * @param dto
     *            the persistence DTO
     *
     * @return the reconstituted domain Expense
     */
    public Expense toDomain(ExpensePersistenceDTO dto) {
        Expense.Id id = dto.id() != null ? Expense.Id.of(dto.id()) : null;
        Participant.Id payerId = dto.payerId() != null ? Participant.Id.of(dto.payerId()) : null;
        Instant createdAt = dto.createdAt() != null ? Instant.parse(dto.createdAt()) : null;

        return switch (dto) {
            case ExpenseByNightPersistenceDTO _ ->
                    ExpenseByNight.fromJson(id, dto.amount(), dto.description(), payerId, createdAt);
            case ExpenseEqualPersistenceDTO _ ->
                    ExpenseEqual.fromJson(id, dto.amount(), dto.description(), payerId, createdAt);
        };

    }
}
