package org.asymetrik.web.fairnsquare.split.persistence;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.asymetrik.web.fairnsquare.infrastructure.zipfile.ZipSerializer;
import org.junit.jupiter.api.Test;

/**
 * Utility that generates frozen ZIP fixture files for {@link PersistenceBackwardCompatibilityTest}.
 * <p>
 * Run the single {@code @Test} method once (plain JUnit — no Quarkus context needed) to regenerate all fixtures. The
 * output files are committed to {@code src/test/resources/fixtures/compat/} and must never be regenerated
 * automatically; they represent a point-in-time snapshot of a real on-disk format.
 * <p>
 * The format version is passed explicitly (not via {@code ZipMetadata.CURRENT_VERSION}) so that each fixture is
 * permanently pinned to the version it was created with. When a new backward-compatibility scenario is introduced, add
 * the corresponding JSON string and output path here, pass the version that was current at the time, regenerate, and
 * commit the new fixture.
 */
class FixtureZipGenerator {

    /** Format version current when these fixtures were generated. Must not be changed retroactively. */
    private static final String FIXTURE_VERSION = "1.0";

    private static final Path OUTPUT_DIR = Path.of("src/test/resources/fixtures/compat");

    private static final String V1_NO_PREFERRED_CREDITOR_JSON = """
            {
              "id": "splitNoPreferredCred1",
              "name": "Compat - No Preferred Creditor",
              "createdAt": "2024-06-01T10:00:00Z",
              "participants": [
                {
                  "id": "participantAliceOld01",
                  "name": "Alice",
                  "nights": 3.0,
                  "share": 1.0
                },
                {
                  "id": "participantBobOldId01",
                  "name": "Bob",
                  "nights": 5.0,
                  "share": 1.0
                }
              ],
              "expenses": [],
              "settlement": null
            }
            """;

    private static final String V1_NUMBER_OF_PERSONS_JSON = """
            {
              "id": "splitNumberOfPersons1",
              "name": "Compat - numberOfPersons Field",
              "createdAt": "2024-06-01T10:00:00Z",
              "participants": [
                {
                  "id": "participantCarolOld01",
                  "name": "Carol",
                  "nights": 2.0,
                  "numberOfPersons": 3.0
                }
              ],
              "expenses": [],
              "settlement": null
            }
            """;

    private static final String V1_BY_PERSON_EXPENSE_JSON = """
            {
              "id": "splitByPersonExpense1",
              "name": "Compat - BY_PERSON Expense Type",
              "createdAt": "2024-06-01T10:00:00Z",
              "participants": [
                {
                  "id": "participantDaveOld001",
                  "name": "Dave",
                  "nights": 4.0,
                  "share": 1.0
                }
              ],
              "expenses": [
                {
                  "type": "BY_PERSON",
                  "id": "expenseByPersonOld001",
                  "amount": "100.00",
                  "description": "Groceries",
                  "payerId": "participantDaveOld001",
                  "createdAt": "2024-06-01T11:00:00Z"
                }
              ],
              "settlement": null
            }
            """;

    private static final String V1_EQUAL_EXPENSE_JSON = """
            {
              "id": "splitEqualExpense0001",
              "name": "Compat - EQUAL Expense Type",
              "createdAt": "2024-06-01T10:00:00Z",
              "participants": [
                {
                  "id": "participantEveOld0001",
                  "name": "Eve",
                  "nights": 3.0,
                  "share": 1.0
                },
                {
                  "id": "participantFrankOld01",
                  "name": "Frank",
                  "nights": 2.0,
                  "share": 1.0
                }
              ],
              "expenses": [
                {
                  "type": "EQUAL",
                  "id": "expenseEqualOld000001",
                  "amount": "90.00",
                  "description": "Dinner",
                  "payerId": "participantEveOld0001",
                  "createdAt": "2024-06-01T11:00:00Z"
                }
              ],
              "settlement": null
            }
            """;

    @Test
    void generateFixtures() throws IOException {
        ZipSerializer zipSerializer = new ZipSerializer();
        Files.createDirectories(OUTPUT_DIR);

        writeFixture(zipSerializer, "v1-no-preferred-creditor.zip", V1_NO_PREFERRED_CREDITOR_JSON);
        writeFixture(zipSerializer, "v1-number-of-persons.zip", V1_NUMBER_OF_PERSONS_JSON);
        writeFixture(zipSerializer, "v1-by-person-expense.zip", V1_BY_PERSON_EXPENSE_JSON);
        writeFixture(zipSerializer, "v1-equal-expense.zip", V1_EQUAL_EXPENSE_JSON);

        System.out.println("Fixtures written to " + OUTPUT_DIR.toAbsolutePath());
    }

    private void writeFixture(ZipSerializer zipSerializer, String filename, String json) throws IOException {
        byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
        byte[] zipBytes = zipSerializer.toZip(jsonBytes, FIXTURE_VERSION);
        Path target = OUTPUT_DIR.resolve(filename);
        Files.write(target, zipBytes);
        System.out.println("  wrote " + target);
    }
}
