package org.asymetrik.web.fairnsquare.split.persistence;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonException;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.json.JsonWriter;
import jakarta.json.JsonWriterFactory;
import jakarta.json.stream.JsonGenerator;

import org.asymetrik.web.fairnsquare.infrastructure.zipfile.ZipSerializer;
import org.asymetrik.web.fairnsquare.split.persistence.dto.ExpenseByNightPersistenceDTO;
import org.asymetrik.web.fairnsquare.split.persistence.dto.ExpenseBySharePersistenceDTO;
import org.asymetrik.web.fairnsquare.split.persistence.dto.ExpenseEqualPersistenceDTO;
import org.asymetrik.web.fairnsquare.split.persistence.dto.ExpenseFreePersistenceDTO;
import org.asymetrik.web.fairnsquare.split.persistence.dto.ExpensePersistenceDTO;
import org.asymetrik.web.fairnsquare.split.persistence.dto.ParticipantPersistenceDTO;
import org.asymetrik.web.fairnsquare.split.persistence.dto.SettlementPersistenceDTO;
import org.asymetrik.web.fairnsquare.split.persistence.dto.SettlementPersistenceDTO.ParticipantBalancePersistenceDTO;
import org.asymetrik.web.fairnsquare.split.persistence.dto.SettlementPersistenceDTO.ReimbursementPersistenceDTO;
import org.asymetrik.web.fairnsquare.split.persistence.dto.SplitPersistenceDTO;

/**
 * The JSON of a stored split ({@code data.bin}), read and written with JSON-P, by hand: every rule that keeps old files
 * readable is here, in plain code, rather than in a binding library's annotations and defaults.
 * <p>
 * Reading accepts every format ever written (the fixtures of {@code PersistenceBackwardCompatibilityTest}):
 * <ul>
 * <li>an expense without {@code type} is {@code BY_NIGHT};</li>
 * <li>{@code BY_PERSON} is the former name of {@code BY_SHARE};</li>
 * <li>{@code EQUAL} is read as such, and {@code SplitPersistenceMapper} turns it into a {@code FREE} expense;</li>
 * <li>{@code numberOfPersons} is the former name of a participant's {@code share};</li>
 * <li>a missing or {@code null} field is {@code null} (0 for a participant's numbers), and unknown fields such as
 * {@code preferredCreditorId} are ignored;</li>
 * <li>a number may be written as a JSON number or as a string ({@code "100.00"}).</li>
 * </ul>
 * Writing gives the same JSON as the former Jackson writer: the fields in the same order, {@code type} first in an
 * expense, {@code null} written out, and indented.
 */
final class SplitJsonCodec {

    private static final JsonWriterFactory WRITERS = Json
            .createWriterFactory(Map.of(JsonGenerator.PRETTY_PRINTING, true));

    private static final Map<String, String> EXPENSE_TYPES = Map.of("BY_NIGHT", "BY_NIGHT", "BY_SHARE", "BY_SHARE",
            "BY_PERSON", "BY_SHARE", "EQUAL", "EQUAL", "FREE", "FREE");

    private SplitJsonCodec() {
    }

    static byte[] write(SplitPersistenceDTO split) {
        JsonObjectBuilder json = Json.createObjectBuilder();
        put(json, "id", split.id());
        put(json, "name", split.name());
        put(json, "createdAt", split.createdAt());
        put(json, "updatedAt", split.updatedAt());
        json.add("participants", array(split.participants(), SplitJsonCodec::participant));
        json.add("expenses", array(split.expenses(), SplitJsonCodec::expense));
        json.add("settlement", split.settlement() == null ? JsonValue.NULL : settlement(split.settlement()));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (JsonWriter writer = WRITERS.createWriter(bytes)) {
            writer.writeObject(json.build());
        }
        return bytes.toByteArray();
    }

    static SplitPersistenceDTO read(byte[] bytes) {
        JsonObject json;
        try (JsonReader reader = Json.createReader(new ByteArrayInputStream(bytes))) {
            json = reader.readObject();
        } catch (JsonException | ClassCastException e) {
            throw new ZipSerializer.ZipException("Failed to deserialize entity", e);
        }
        return new SplitPersistenceDTO(string(json, "id"), string(json, "name"), string(json, "createdAt"),
                string(json, "updatedAt"), list(json, "participants", SplitJsonCodec::readParticipant),
                list(json, "expenses", SplitJsonCodec::readExpense),
                json.get("settlement") instanceof JsonObject settlement ? readSettlement(settlement) : null);
    }

    // --- writing ---

    private static JsonObject participant(ParticipantPersistenceDTO participant) {
        JsonObjectBuilder json = Json.createObjectBuilder();
        put(json, "id", participant.id());
        put(json, "name", participant.name());
        json.add("nights", participant.nights());
        json.add("share", participant.share());
        put(json, "sharedAccountId", participant.sharedAccountId());
        return json.build();
    }

    private static JsonObject expense(ExpensePersistenceDTO expense) {
        JsonObjectBuilder json = Json.createObjectBuilder();
        json.add("type", switch (expense) {
            case ExpenseByNightPersistenceDTO _ -> "BY_NIGHT";
            case ExpenseBySharePersistenceDTO _ -> "BY_SHARE";
            case ExpenseEqualPersistenceDTO _ -> "EQUAL";
            case ExpenseFreePersistenceDTO _ -> "FREE";
        });
        put(json, "id", expense.id());
        put(json, "amount", expense.amount());
        put(json, "description", expense.description());
        put(json, "payerId", expense.payerId());
        put(json, "createdAt", expense.createdAt());
        if (expense instanceof ExpenseFreePersistenceDTO free) {
            json.add("shares", array(free.shares(), share -> {
                JsonObjectBuilder shareJson = Json.createObjectBuilder();
                put(shareJson, "participantId", share.participantId());
                put(shareJson, "parts", share.parts());
                return shareJson.build();
            }));
        }
        return json.build();
    }

    private static JsonObject settlement(SettlementPersistenceDTO settlement) {
        return Json.createObjectBuilder().add("balances", array(settlement.balances(), balance -> {
            JsonObjectBuilder json = Json.createObjectBuilder();
            put(json, "participantId", balance.participantId());
            put(json, "totalPaid", balance.totalPaid());
            put(json, "totalCost", balance.totalCost());
            put(json, "balance", balance.balance());
            return json.build();
        })).add("reimbursements", array(settlement.reimbursements(), reimbursement -> {
            JsonObjectBuilder json = Json.createObjectBuilder();
            put(json, "fromId", reimbursement.fromId());
            put(json, "fromType", reimbursement.fromType());
            put(json, "toId", reimbursement.toId());
            put(json, "toType", reimbursement.toType());
            put(json, "amount", reimbursement.amount());
            return json.build();
        })).build();
    }

    private static <T> JsonValue array(List<T> items, Function<T, JsonObject> toJson) {
        if (items == null) {
            return JsonValue.NULL;
        }
        JsonArrayBuilder array = Json.createArrayBuilder();
        items.forEach(item -> array.add(toJson.apply(item)));
        return array.build();
    }

    private static void put(JsonObjectBuilder json, String name, String value) {
        if (value == null) {
            json.addNull(name);
        } else {
            json.add(name, value);
        }
    }

    private static void put(JsonObjectBuilder json, String name, BigDecimal value) {
        if (value == null) {
            json.addNull(name);
        } else {
            json.add(name, value);
        }
    }

    // --- reading ---

    private static ParticipantPersistenceDTO readParticipant(JsonObject json) {
        BigDecimal share = json.containsKey("share") ? decimal(json, "share") : decimal(json, "numberOfPersons");
        return new ParticipantPersistenceDTO(string(json, "id"), string(json, "name"),
                primitive(decimal(json, "nights")), primitive(share), string(json, "sharedAccountId"));
    }

    private static ExpensePersistenceDTO readExpense(JsonObject json) {
        String id = string(json, "id");
        BigDecimal amount = decimal(json, "amount");
        String description = string(json, "description");
        String payerId = string(json, "payerId");
        String createdAt = string(json, "createdAt");
        String written = string(json, "type");
        String type = written == null ? "BY_NIGHT" : EXPENSE_TYPES.get(written);
        if (type == null) {
            throw new ZipSerializer.ZipException("Unknown expense type '" + written + "'", null);
        }
        return switch (type) {
            case "BY_SHARE" -> new ExpenseBySharePersistenceDTO(id, amount, description, payerId, createdAt);
            case "EQUAL" -> new ExpenseEqualPersistenceDTO(id, amount, description, payerId, createdAt);
            case "FREE" -> new ExpenseFreePersistenceDTO(id, amount, description, payerId, createdAt,
                    list(json, "shares",
                            share -> new ExpenseFreePersistenceDTO.SharePersistenceDTO(string(share, "participantId"),
                                    decimal(share, "parts"))));
            default -> new ExpenseByNightPersistenceDTO(id, amount, description, payerId, createdAt);
        };
    }

    private static SettlementPersistenceDTO readSettlement(JsonObject json) {
        return new SettlementPersistenceDTO(
                list(json, "balances", balance -> new ParticipantBalancePersistenceDTO(string(balance, "participantId"),
                        decimal(balance, "totalPaid"), decimal(balance, "totalCost"), decimal(balance, "balance"))),
                list(json, "reimbursements",
                        reimbursement -> new ReimbursementPersistenceDTO(string(reimbursement, "fromId"),
                                string(reimbursement, "fromType"), string(reimbursement, "toId"),
                                string(reimbursement, "toType"), decimal(reimbursement, "amount"))));
    }

    private static <T> List<T> list(JsonObject json, String name, Function<JsonObject, T> read) {
        return json.get(name) instanceof JsonArray array
                ? array.stream().map(item -> read.apply(item.asJsonObject())).toList()
                : null;
    }

    private static String string(JsonObject json, String name) {
        return json.get(name) instanceof JsonString string ? string.getString() : null;
    }

    private static BigDecimal decimal(JsonObject json, String name) {
        return switch (json.get(name)) {
            case JsonNumber number -> number.bigDecimalValue();
            case JsonString string -> new BigDecimal(string.getString());
            case null, default -> null;
        };
    }

    private static double primitive(BigDecimal value) {
        return value == null ? 0 : value.doubleValue();
    }
}
