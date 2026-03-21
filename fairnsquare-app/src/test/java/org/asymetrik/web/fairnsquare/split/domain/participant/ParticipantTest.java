package org.asymetrik.web.fairnsquare.split.domain.participant;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Participant domain record.
 */
class ParticipantTest {

    @Test
    void toString_doesNotContainName() {
        Participant participant = Participant.create("Alice", 3, 2);

        assertThat(participant.toString()).doesNotContain("Alice");
    }

    @Test
    void toString_containsIdNightsAndPersons() {
        Participant participant = Participant.create("Bob", 5, 1);

        String result = participant.toString();
        assertThat(result).contains(participant.id().value());
        assertThat(result).contains("5.0");
        assertThat(result).contains("1.0");
    }

    @Test
    void create_initializesBalanceFieldsToZero() {
        Participant participant = Participant.create("Alice", 3, 2);

        assertThat(participant.totalPaid()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(participant.totalCost()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(participant.balance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void toString_containsBalance() {
        Participant participant = Participant.create("Bob", 5, 1);

        assertThat(participant.toString()).contains("balance=");
    }
}
