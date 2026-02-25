package org.asymetrik.web.fairnsquare.split.domain.participant;

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
}
