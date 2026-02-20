package org.asymetrik.web.fairnsquare.expense.api.dto;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Polymorphic DTO for Expense REST API responses. Decouples domain model from API contract. Uses Jackson type
 * discriminator matching domain structure (BY_NIGHT, EQUAL).
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({ @JsonSubTypes.Type(value = ExpenseByNightDTO.class, name = "BY_NIGHT"),
        @JsonSubTypes.Type(value = ExpenseByPersonDTO.class, name = "BY_PERSON"),
        @JsonSubTypes.Type(value = ExpenseEqualDTO.class, name = "EQUAL"),
        @JsonSubTypes.Type(value = ExpenseFreeDTO.class, name = "FREE") })
public sealed interface ExpenseDTO permits ExpenseByNightDTO, ExpenseByPersonDTO, ExpenseEqualDTO, ExpenseFreeDTO {

    String id();

    String description();

    BigDecimal amount();

    String payerId();

    String type();

    String splitMode();

    String createdAt();

    List<ShareDTO> shares();
}
