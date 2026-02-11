package org.asymetrik.web.fairnsquare.split.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import jakarta.inject.Inject;

import org.asymetrik.web.fairnsquare.split.domain.expenses.Expense;
import org.asymetrik.web.fairnsquare.split.domain.expenses.ExpenseByNight;
import org.asymetrik.web.fairnsquare.split.domain.expenses.ExpenseEqual;
import org.asymetrik.web.fairnsquare.split.domain.participant.Participant;
import org.asymetrik.web.fairnsquare.split.domain.Split;
import org.asymetrik.web.fairnsquare.split.persistence.dto.SplitPersistenceDTO;
import org.asymetrik.web.fairnsquare.split.persistence.mapper.SplitPersistenceMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Integration tests verifying the full persistence round-trip: domain -> DTO -> JSON file -> DTO -> domain.
 */
@QuarkusTest
class PersistenceRoundTripTest {

    @Inject
    JsonFileRepository repository;

    @Inject
    SplitPersistenceMapper mapper;

    @Inject
    TenantPathResolver pathResolver;

    @BeforeEach
    void setUp() throws IOException {
        Path defaultTenant = pathResolver.resolveDefaultTenantDirectory();
        if (Files.exists(defaultTenant)) {
            Files.walk(defaultTenant).sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException _) {
                }
            });
        }
    }

    @Test
    void shouldPersistAndLoadEmptySplit() {
        Split original = Split.create("Empty Split");
        String splitId = original.getId().value();

        repository.save(splitId, mapper.toPersistenceDTO(original));
        Split loaded = repository.load(splitId, SplitPersistenceDTO.class).map(mapper::toDomain).orElseThrow();

        assertThat(loaded.getId()).isEqualTo(original.getId());
        assertThat(loaded.getName()).isEqualTo(original.getName());
        assertThat(loaded.getParticipants()).isEmpty();
        assertThat(loaded.getExpenses()).isEmpty();
    }

    @Test
    void shouldPersistAndLoadSplitWithParticipants() {
        Split original = Split.create("With Participants");
        Participant alice = Participant.create("Alice", 3);
        Participant bob = Participant.create("Bob", 5);
        original.addParticipant(alice);
        original.addParticipant(bob);
        String splitId = original.getId().value();

        repository.save(splitId, mapper.toPersistenceDTO(original));
        Split loaded = repository.load(splitId, SplitPersistenceDTO.class).map(mapper::toDomain).orElseThrow();

        assertThat(loaded.getParticipants()).hasSize(2);
        assertThat(loaded.getParticipants().get(0).name().value()).isEqualTo("Alice");
        assertThat(loaded.getParticipants().get(1).name().value()).isEqualTo("Bob");
    }

    @Test
    void shouldPersistAndLoadSplitWithByNightExpense() {
        Split original = Split.create("With BY_NIGHT Expense");
        Participant alice = Participant.create("Alice", 3);
        Participant bob = Participant.create("Bob", 5);
        original.addParticipant(alice);
        original.addParticipant(bob);

        ExpenseByNight expense = ExpenseByNight.create(new BigDecimal("160.00"), "Hotel", alice.id());
        original.addExpense(expense);
        String splitId = original.getId().value();

        repository.save(splitId, mapper.toPersistenceDTO(original));
        Split loaded = repository.load(splitId, SplitPersistenceDTO.class).map(mapper::toDomain).orElseThrow();

        assertThat(loaded.getExpenses()).hasSize(1);
        assertThat(loaded.getExpenses().getFirst()).isInstanceOf(ExpenseByNight.class);
        assertThat(loaded.getExpenses().getFirst().getAmount()).isEqualByComparingTo(new BigDecimal("160.00"));

    }

    @Test
    void shouldPersistAndLoadSplitWithEqualExpense() {
        Split original = Split.create("With EQUAL Expense");
        Participant alice = Participant.create("Alice", 2);
        original.addParticipant(alice);

        ExpenseEqual expense = ExpenseEqual.create(new BigDecimal("50.00"), "Dinner", alice.id());
        original.addExpense(expense);
        String splitId = original.getId().value();

        repository.save(splitId, mapper.toPersistenceDTO(original));
        Split loaded = repository.load(splitId, SplitPersistenceDTO.class).map(mapper::toDomain).orElseThrow();

        assertThat(loaded.getExpenses()).hasSize(1);
        assertThat(loaded.getExpenses().getFirst()).isInstanceOf(ExpenseEqual.class);
    }

    @Test
    void shouldPersistAndLoadSplitWithMixedExpenseTypes() {
        Split original = Split.create("Mixed Types");
        Participant alice = Participant.create("Alice", 3);
        original.addParticipant(alice);

        ExpenseByNight byNight = ExpenseByNight.create(new BigDecimal("80.00"), "Hotel", alice.id());
        ExpenseEqual equal = ExpenseEqual.create(new BigDecimal("40.00"), "Taxi", alice.id());
        original.addExpense(byNight);
        original.addExpense(equal);
        String splitId = original.getId().value();

        repository.save(splitId, mapper.toPersistenceDTO(original));
        Split loaded = repository.load(splitId, SplitPersistenceDTO.class).map(mapper::toDomain).orElseThrow();

        assertThat(loaded.getExpenses()).hasSize(2);
        assertThat(loaded.getExpenses().get(0)).isInstanceOf(ExpenseByNight.class);
        assertThat(loaded.getExpenses().get(1)).isInstanceOf(ExpenseEqual.class);
    }

    @Test
    void shouldVerifyJsonFileFormatMatchesExpected() throws IOException {
        Split split = Split.create("Format Check");
        Participant alice = Participant.create("Alice", 2);
        split.addParticipant(alice);
        String splitId = split.getId().value();

        repository.save(splitId, mapper.toPersistenceDTO(split));

        Path filePath = pathResolver.resolve(splitId);
        String json = Files.readString(filePath);

        // Verify camelCase field names and structure
        assertThat(json).contains("\"id\"");
        assertThat(json).contains("\"name\"");
        assertThat(json).contains("\"createdAt\"");
        assertThat(json).contains("\"participants\"");
        assertThat(json).contains("\"expenses\"");
        assertThat(json).contains("\"nights\"");
        assertThat(json).contains("\"Alice\"");
    }

    @Test
    void shouldNotPersistSharesAndRecalculateOnLoad() throws IOException {
        Split original = Split.create("No Shares Persisted");
        Participant alice = Participant.create("Alice", 3);
        Participant bob = Participant.create("Bob", 5);
        original.addParticipant(alice);
        original.addParticipant(bob);

        ExpenseByNight expense = ExpenseByNight.create(new BigDecimal("160.00"), "Hotel", alice.id());
        original.addExpense(expense);
        String splitId = original.getId().value();

        repository.save(splitId, mapper.toPersistenceDTO(original));

        // Verify JSON does not contain shares
        Path filePath = pathResolver.resolve(splitId);
        String json = Files.readString(filePath);
        assertThat(json).doesNotContain("\"shares\"");
        assertThat(json).doesNotContain("participantId");

        // Verify shares are recalculated on load
        Split loaded = repository.load(splitId, SplitPersistenceDTO.class).map(mapper::toDomain).orElseThrow();
        List<Expense.Share> shares = loaded.getExpenses().getFirst().getShares(loaded);
        assertThat(shares).hasSize(2);
        // Alice: 3/8 * 160 = 60.00, Bob: 5/8 * 160 = 100.00
        assertThat(
                shares.stream().map(Expense.Share::amount).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add))
                        .isEqualByComparingTo(new BigDecimal("160.00"));
    }

    @Test
    void shouldLoadLegacyJsonFile() throws IOException {
        // Simulate a legacy JSON file (format matching current domain serialization)
        String legacyId = Split.Id.generate().value();
        String participantId = Participant.Id.generate().value();
        String legacyJson = """
                {
                  "id" : "%s",
                  "name" : "Legacy Split",
                  "createdAt" : "2026-01-01T00:00:00Z",
                  "participants" : [ {
                    "id" : "%s",
                    "name" : "Alice",
                    "nights" : 2
                  } ],
                  "expenses" : [ ]
                }
                """.formatted(legacyId, participantId);

        Path filePath = pathResolver.resolve(legacyId);
        Files.createDirectories(filePath.getParent());
        Files.writeString(filePath, legacyJson);

        Split loaded = repository.load(legacyId, SplitPersistenceDTO.class).map(mapper::toDomain).orElseThrow();

        assertThat(loaded.getId().value()).isEqualTo(legacyId);
        assertThat(loaded.getName().value()).isEqualTo("Legacy Split");
        assertThat(loaded.getParticipants()).hasSize(1);
        assertThat(loaded.getParticipants().getFirst().name().value()).isEqualTo("Alice");
        assertThat(loaded.getExpenses()).isEmpty();
    }
}
