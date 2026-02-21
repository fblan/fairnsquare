package org.asymetrik.web.fairnsquare.split.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
 * Integration tests verifying the full persistence round-trip: domain -> DTO -> ZIP archive -> DTO -> domain.
 */
@QuarkusTest
class PersistenceRoundTripTest {

    @Inject
    ZipFileRepository repository;

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
    void shouldVerifyZipArchiveContainsMetadataAndData() throws IOException {
        Split split = Split.create("Format Check");
        Participant alice = Participant.create("Alice", 2);
        split.addParticipant(alice);
        String splitId = split.getId().value();

        repository.save(splitId, mapper.toPersistenceDTO(split));

        Path filePath = pathResolver.resolve(splitId);
        String metadata = readZipEntry(filePath, ZipFileRepository.METADATA_ENTRY);
        String data = readZipEntry(filePath, ZipFileRepository.DATA_ENTRY);

        // Verify metadata
        assertThat(metadata).contains("\"version\"");
        assertThat(metadata).contains("\"1.0\"");
        assertThat(metadata).contains("\"deserializer\"");
        assertThat(metadata).contains("\"clear\"");

        // Verify data contains expected JSON structure
        assertThat(data).contains("\"id\"");
        assertThat(data).contains("\"name\"");
        assertThat(data).contains("\"createdAt\"");
        assertThat(data).contains("\"participants\"");
        assertThat(data).contains("\"expenses\"");
        assertThat(data).contains("\"nights\"");
        assertThat(data).contains("\"Alice\"");
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

        // Verify data.bin does not contain shares
        Path filePath = pathResolver.resolve(splitId);
        String data = readZipEntry(filePath, ZipFileRepository.DATA_ENTRY);
        assertThat(data).doesNotContain("\"shares\"");
        assertThat(data).doesNotContain("participantId");

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
    void shouldPersistAsZipFile() {
        Split split = Split.create("Zip Check");
        String splitId = split.getId().value();

        repository.save(splitId, mapper.toPersistenceDTO(split));

        Path filePath = pathResolver.resolve(splitId);
        assertThat(filePath.toString()).endsWith(".zip");
        assertThat(Files.exists(filePath)).isTrue();
    }

    private String readZipEntry(Path zipPath, String entryName) throws IOException {
        try (InputStream fis = Files.newInputStream(zipPath);
                ZipInputStream zis = new ZipInputStream(fis)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equals(entryName)) {
                    String content = new String(zis.readAllBytes(), StandardCharsets.UTF_8);
                    zis.closeEntry();
                    return content;
                }
                zis.closeEntry();
            }
        }
        throw new AssertionError("ZIP entry '" + entryName + "' not found in " + zipPath);
    }
}
