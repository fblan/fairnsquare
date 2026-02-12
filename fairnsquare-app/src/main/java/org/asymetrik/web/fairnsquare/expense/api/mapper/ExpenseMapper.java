package org.asymetrik.web.fairnsquare.expense.api.mapper;

import java.util.Collections;
import java.util.List;

import org.asymetrik.web.fairnsquare.expense.api.dto.ExpenseByNightDTO;
import org.asymetrik.web.fairnsquare.expense.api.dto.ExpenseDTO;
import org.asymetrik.web.fairnsquare.expense.api.dto.ExpenseEqualDTO;
import org.asymetrik.web.fairnsquare.expense.api.dto.ExpenseFreeDTO;
import org.asymetrik.web.fairnsquare.expense.api.dto.ShareDTO;
import org.asymetrik.web.fairnsquare.split.domain.expenses.Expense;
import org.asymetrik.web.fairnsquare.split.domain.expenses.ExpenseByNight;
import org.asymetrik.web.fairnsquare.split.domain.expenses.ExpenseEqual;
import org.asymetrik.web.fairnsquare.split.domain.expenses.ExpenseFree;
import org.asymetrik.web.fairnsquare.split.domain.Split;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Mapper for converting Expense domain objects to DTOs for REST API responses. Handles polymorphic expense types
 * (BY_NIGHT, EQUAL).
 */
@ApplicationScoped
public class ExpenseMapper {

    /**
     * Converts an Expense domain object to the appropriate ExpenseDTO subtype.
     *
     * @param expense
     *            the domain expense
     * @param split
     *            the split context for calculating shares (null if shares not available)
     *
     * @return the DTO representation matching the expense type
     *
     * @throws NullPointerException
     *             if expense is null
     * @throws IllegalArgumentException
     *             if expense is an unsupported type
     */
    public ExpenseDTO toDTO(Expense expense, Split split) {
        if (expense == null) {
            throw new NullPointerException("Expense cannot be null");
        }

        List<ShareDTO> shares = split != null ? mapShares(expense.getShares(split)) : Collections.emptyList();

        return switch (expense) {
            case ExpenseByNight byNight -> new ExpenseByNightDTO(byNight.getId().value(), byNight.getDescription(),
                    byNight.getAmount(), byNight.getPayerId() != null ? byNight.getPayerId().value() : null, "BY_NIGHT",
                    "BY_NIGHT", byNight.getCreatedAt().toString(), shares);
            case ExpenseEqual equal -> new ExpenseEqualDTO(equal.getId().value(), equal.getDescription(),
                    equal.getAmount(), equal.getPayerId() != null ? equal.getPayerId().value() : null, "EQUAL", "EQUAL",
                    equal.getCreatedAt().toString(), shares);
            case ExpenseFree free -> new ExpenseFreeDTO(free.getId().value(), free.getDescription(), free.getAmount(),
                    free.getPayerId() != null ? free.getPayerId().value() : null, "FREE", "FREE",
                    free.getCreatedAt().toString(), shares);
        };
    }

    private List<ShareDTO> mapShares(List<Expense.Share> domainShares) {
        return domainShares.stream().map(s -> new ShareDTO(s.participantId().value(), s.amount(), s.parts())).toList();
    }
}
