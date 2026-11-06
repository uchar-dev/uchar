package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The names and labels of a case as the reference interpreter's case reader takes them, on both branches. A name the
 * case writes, an object type, an attribute, a characteristic or a role key, in the input or in an expectation, names
 * the declaration that it is under the name equality of that reader ({@code Namen.norm}: runs of spaces and tabs as one
 * space, A–Z as a–z, every other character as written). A name that is no declaration, and a label that two instances
 * carry, is a defect of the case, which that reader refuses at load and the Drools backend refuses as an
 * {@link IllegalArgumentException}: never a form it does not carry, and never a member found through the Java name its
 * spelling derives ("geschatte-waarde" and "geschatte waarde" derive one getter).
 *
 * <p>The documents are {@code anchor/jutterswet-waardestelling} (the attribute "geschatte waarde" and the characteristic
 * "gemeld" of a Vondst), that document with the attribute renamed to "geschatte İş", and {@code jutterswet-ketenband}
 * (the role key "opvolgers" of a Wachter). The cases are their own cases with one name respelled; the corpus is read,
 * not changed.
 */
class CaseNameRuleWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");
    private static final Path WAARDESTELLING = CORPUS.resolve("anchor/jutterswet-waardestelling");
    private static final Path KETENBAND = CORPUS.resolve("jutterswet-ketenband");

    @Test void a_name_in_other_spaces_or_capitals_a_to_z_is_the_declared_name() throws Exception {
        Path ir = WAARDESTELLING.resolve("ir.json");
        BothBranches.green(ir, waarde(c -> renameKey(input(c).with("attributes"), "geschatte waarde",
                "geschatte  waarde")), "an input key with two spaces");
        BothBranches.green(ir, waarde(c -> renameKey(input(c).with("attributes"), "geschatte waarde",
                "Geschatte\tWaarde")), "an input key with a tab and capitals");
        BothBranches.green(ir, waarde(c -> ((ObjectNode) c.get("expected").get(0)).put("attribute",
                "GESCHATTE  WAARDE")), "an expectation key in capitals with two spaces");
        BothBranches.green(ir, waarde(c -> input(c).set("characteristics", JSON.createArrayNode().add("Gemeld"))),
                "a characteristic with a capital");
        BothBranches.green(ir, waarde(c -> ((ArrayNode) c.get("expected")).insert(0, JSON.createObjectNode()
                .put("kind", "characteristicPresent").put("label", "v1").put("characteristic", "GEMELD"))),
                "an expected characteristic in capitals");
        BothBranches.green(ir, waarde(c -> input(c).put("objectType", " VONDST")),
                "an object type with a leading space and capitals");
        BothBranches.green(KETENBAND.resolve("ir.json"), keten(c -> renameKey(input(c).with("roles"), "opvolgers",
                "Opvolgers ")), "a role key with a capital and a trailing space");
    }

    @Test void a_name_that_is_no_declaration_is_a_defect_of_the_case() throws Exception {
        Path ir = WAARDESTELLING.resolve("ir.json");
        defect(ir, waarde(c -> renameKey(input(c).with("attributes"), "geschatte waarde", "geschatte-waarde")),
                "attribute \"geschatte-waarde\" is not declared on \"Vondst\"");
        defect(ir, waarde(c -> ((ObjectNode) c.get("expected").get(0)).put("attribute", "geschatte-waarde")),
                "attribute \"geschatte-waarde\" is not declared on \"Vondst\"");
        defect(ir, waarde(c -> input(c).set("characteristics", JSON.createArrayNode().add("ge-meld"))),
                "characteristic \"ge-meld\" is not declared on \"Vondst\"");
        defect(ir, waarde(c -> ((ArrayNode) c.get("expected")).insert(0, JSON.createObjectNode()
                .put("kind", "characteristicPresent").put("label", "v1").put("characteristic", "ge meld"))),
                "characteristic \"ge meld\" is not declared on \"Vondst\"");
        defect(ir, waarde(c -> input(c).put("objectType", "Vondsten")),
                "objectType \"Vondsten\" is not declared");
        defect(KETENBAND.resolve("ir.json"), keten(c -> renameKey(input(c).with("roles"), "opvolgers", "op-volgers")),
                "role key \"op-volgers\" names no declared role");
    }

    @Test void a_capital_outside_a_to_z_makes_another_name(@TempDir Path dir) throws Exception {
        // ref folds A-Z only (lowercase_ascii), so "İŞ" is not the declared "İş"; Java's equalsIgnoreCase folds both
        Path ir = dir.resolve("ir.json");
        Files.writeString(ir, Files.readString(WAARDESTELLING.resolve("ir.json"), StandardCharsets.UTF_8)
                .replace("geschatte waarde", "geschatte İş"), StandardCharsets.UTF_8);
        BothBranches.green(ir, turkish(c -> {}), "the case as declared");
        defect(ir, turkish(c -> renameKey(input(c).with("attributes"), "geschatte İş", "geschatte İŞ")),
                "attribute \"geschatte İŞ\" is not declared on \"Vondst\"");
        defect(ir, turkish(c -> ((ObjectNode) c.get("expected").get(0)).put("attribute", "geschatte İŞ")),
                "attribute \"geschatte İŞ\" is not declared on \"Vondst\"");
    }

    @Test void two_instances_with_one_label_are_a_defect_of_the_case() throws Exception {
        // the second v1 would otherwise land on the bean of the first and overwrite its taxatiewaarde
        JsonNode c = waarde(k -> {
            ObjectNode second = input(k).deepCopy();
            second.set("attributes", JSON.createObjectNode().set("taxatiewaarde",
                    JSON.createObjectNode().put("number", "50.00")));
            ((ArrayNode) k.get("input").get("instances")).add(second);
        });
        defect(WAARDESTELLING.resolve("ir.json"), c, "duplicate instance label \"v1\"");
    }

    // ---------------- helpers ----------------

    /** Both branches refuse case {@code c} on {@code ir} as a defect of the case, in one sentence that names
     *  {@code fragment}. */
    private static void defect(Path ir, JsonNode c, String fragment) {
        String exec = null;
        for (boolean viaDrl : new boolean[] { false, true }) {
            String branch = viaDrl ? "drl-text" : "exec-model";
            RuntimeException e = assertThrows(RuntimeException.class,
                    () -> CaseRunner.playCase(ir, c, viaDrl), branch + ": " + fragment);
            assertTrue(e instanceof IllegalArgumentException, branch + ": a defect of the case, not a refused form: "
                    + e.getClass().getSimpleName() + ": " + e.getMessage());
            assertTrue(e.getMessage().contains(fragment), branch + " names '" + fragment + "': " + e.getMessage());
            if (exec == null) exec = e.getMessage();
            else assertEquals(exec, e.getMessage(), "drl-text refuses in the sentence of exec-model");
        }
    }

    private static JsonNode waarde(Consumer<ObjectNode> twist) throws Exception {
        return twisted(WAARDESTELLING.resolve("case1-berekend-over-input.json"), twist);
    }

    private static JsonNode keten(Consumer<ObjectNode> twist) throws Exception {
        return twisted(KETENBAND.resolve("case2.json"), twist);
    }

    private static JsonNode turkish(Consumer<ObjectNode> twist) throws Exception {
        ObjectNode c = (ObjectNode) JSON.readTree(Files.readString(WAARDESTELLING.resolve(
                "case1-berekend-over-input.json"), StandardCharsets.UTF_8).replace("geschatte waarde", "geschatte İş"));
        twist.accept(c);
        return c;
    }

    private static JsonNode twisted(Path file, Consumer<ObjectNode> twist) throws Exception {
        ObjectNode c = (ObjectNode) JSON.readTree(Files.readString(file, StandardCharsets.UTF_8));
        twist.accept(c);
        return c;
    }

    /** The first instance of the input of case {@code c}. */
    private static ObjectNode input(JsonNode c) {
        return (ObjectNode) c.get("input").get("instances").get(0);
    }

    /** {@code from} under {@code map} as {@code to}, the other keys in their order. */
    private static void renameKey(ObjectNode map, String from, String to) {
        ObjectNode copy = map.deepCopy();
        map.removeAll();
        copy.properties().forEach(e -> map.set(e.getKey().equals(from) ? to : e.getKey(), e.getValue()));
        if (!copy.has(from)) throw new IllegalStateException("no key '" + from + "' to rename in " + copy);
    }
}
