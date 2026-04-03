package org.asymetrik.web.fairnsquare.split.api.expense.dto;

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
        @JsonSubTypes.Type(value = ExpenseByShareDTO.class, name = "BY_SHARE"),
        @JsonSubTypes.Type(value = ExpenseEqualDTO.class, name = "EQUAL"),
        @JsonSubTypes.Type(value = ExpenseFreeDTO.class, name = "FREE"),
        @JsonSubTypes.Type(value = ExpenseByNightCustomDTO.class, name = "BY_NIGHT_CUSTOM") })
public sealed interface ExpenseDTO
        permits ExpenseByNightDTO, ExpenseByShareDTO, ExpenseEqualDTO, ExpenseFreeDTO, ExpenseByNightCustomDTO {

    String id();

    String description();

    BigDecimal amount();

    String payerId();

    String type();

    String splitMode();

    String createdAt();

    List<ShareDTO> shares();
}
