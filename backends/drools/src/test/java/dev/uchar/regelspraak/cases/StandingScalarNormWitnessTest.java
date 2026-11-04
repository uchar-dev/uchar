package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * RUNTIME-SEMANTICS ch. 7.8 (the SCALAR standing norm: every computing rule decides like a default) and ch. 3.11
 * ("Leeg in a write": a writer that fires with a leeg value decides nothing) — witnessed through
 * {@link CaseRunner#playCase} over corpus goldens' {@code ir.json} with cases authored HERE (no corpus expectation is
 * read, moved or touched), and over the documents of {@code ref/test/fixtures/empty-write} with their cases.
 *
 * <p>Both sides of the norm are pinned: a LEEG computed value decides nothing and what the fact base holds stands (the
 * input, or the initialisatie's value), while a GEVULD one displaces it. A one-sided implementation (never write /
 * always write) fails one of the two. The norm holds for a rule beside a second writer and for a rule with a validity
 * window as for the only, version-free one, and each of those rows asserts the erased outcome observably false.
 *
 * <p>Every row runs through BOTH Drools transpiler branches ({@link BothBranches}).
 */
class StandingScalarNormWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");
    private static final Path EMPTY_WRITE = Path.of("..", "..", "ref", "test", "fixtures", "empty-write");

    // ---------------- 1. the standing scalar target (ch. 7.8): one computing rule, no initialisatie ----------------

    @Test void standing_target_with_empty_opbrengst_leaves_the_input_value_standing() throws Exception {
        // the rule FIRES (gemeld) but its source is leeg: it decides nothing, so the input value stands.
        play("anchor/jutterswet-waardestelling",
                vondst("\"geschatte waarde\": { \"number\": \"77.00\" }", "\"gemeld\"", value("77.00")),
                "a leeg computed value must not displace the case input");
    }

    @Test void standing_target_with_filled_opbrengst_displaces_the_input_value() throws Exception {
        // the other side of the same norm: a deciding computation overwrites the input — the arm may not
        // degenerate into "a standing target is never written".
        play("anchor/jutterswet-waardestelling",
                vondst("\"taxatiewaarde\": { \"number\": \"51.00\" }, \"geschatte waarde\": { \"number\": \"77.00\" }",
                        "\"gemeld\"", value("51.00")),
                "a gevuld computed value displaces the case input");
    }

    @Test void standing_target_without_input_stays_empty_at_empty_opbrengst() throws Exception {
        // "the target keeps what the fact base holds" — and here it holds nothing, so the outcome is leeg.
        // The arm is a NO-WRITE, never a fallback to some other value.
        play("anchor/jutterswet-waardestelling",
                vondst("", "\"gemeld\"",
                        "{ \"kind\": \"attributeEmpty\", \"label\": \"v1\", \"attribute\": \"geschatte waarde\" }"),
                "no input and a leeg computation leaves the target leeg");
    }

    // ---------------- 2. the ch. 7.7 desugaring domain: one computing rule BESIDE an initialisatie ----------------

    @Test void desugared_target_with_empty_opbrengst_lets_the_initialisation_decide() throws Exception {
        // the fire-but-leeg corner that used to OSCILLATE here: the gelijkstelling wrote null, the
        // initialisatie refilled 25, and the exec model never reached a fixpoint. With the arm the
        // gelijkstelling writes nothing and the initialisatie decides — once.
        play("anchor/jutterswet-waardering-v0vorm", jutter("", "\"erkend\"", waardering("25")),
                "a leeg computed value lets the initialisatie decide, without an oscillation");
    }

    @Test void desugared_target_with_empty_opbrengst_leaves_the_input_value_standing() throws Exception {
        // same domain, input present: the initialisatie's own §9.6 leeg-guard blocks it, so the INPUT stands.
        play("anchor/jutterswet-waardering-v0vorm",
                jutter("\"waardering\": { \"number\": \"41\" }", "\"erkend\"", waardering("41")),
                "a leeg computed value leaves a case input standing over the initialisatie");
    }

    @Test void desugared_target_with_filled_opbrengst_displaces_both() throws Exception {
        // and the deciding side: the computation beats both the input and the initialisatie.
        play("anchor/jutterswet-waardering-v0vorm",
                jutter("\"basisbedrag\": { \"number\": \"50\" }, \"waardering\": { \"number\": \"41\" }",
                        "\"erkend\"", waardering("50")),
                "a gevuld computed value displaces input and initialisatie alike");
    }

    // ---------------- 3. a second writer: the norm holds for every computing rule ----------------

    @Test void beside_a_second_rule_a_leeg_computation_leaves_the_input_value_standing() throws Exception {
        // The waardestelling model plus ONE extra gelijkstelling on the same target, whose source is leeg as well: both
        // rules fire, neither decides, so the input stands (RUNTIME-SEMANTICS ch. 3.11: a writer that fires with a leeg
        // value decides nothing, on every target, beside a second writer too).
        Path ir = secondWriter();
        BothBranches.green(ir, JSON.readTree(
                vondst("\"geschatte waarde\": { \"number\": \"77.00\" }", "\"gemeld\"", value("77.00"))),
                "beside a second writer a leeg source decides nothing: the input stands");

        // and the same case with an empty expectation is a MISMATCH, so the assertion above is not passing on a
        // comparator that ignores the value: the input is observably THERE.
        BothBranches.red(ir, JSON.readTree(
                vondst("\"geschatte waarde\": { \"number\": \"77.00\" }", "\"gemeld\"",
                        "{ \"kind\": \"attributeEmpty\", \"label\": \"v1\", \"attribute\": \"geschatte waarde\" }")),
                "verwacht leeg, werkelijk 77.00", "the input must observably stand, not merely go unasserted");
    }

    // ---------------- 3b. the version flank: a versioned rule decides like any other ----------------

    @Test void a_versioned_rule_that_computes_leeg_leaves_the_input_value_standing() throws Exception {
        // Same model as the standing rows, one clause different — a geldigheidswindow on the rule — and the case's
        // asOfDate sits inside it, so the rule fires and computes leeg: it decides nothing and the input 77.00 stands, as
        // for a version-free rule (ch. 3.11).
        Path ir = versionWriter();
        BothBranches.green(ir, JSON.readTree(versionCase(value("77.00"))),
                "a versioned writer that computes leeg decides nothing: the input stands");

        // the erased outcome is observably NOT what happened, in both branches.
        BothBranches.red(ir, JSON.readTree(versionCase(
                "{ \"kind\": \"attributeEmpty\", \"label\": \"v1\", \"attribute\": \"geschatte waarde\" }")),
                "verwacht leeg, werkelijk 77.00", "a versioned writer must not erase the input");
    }

    // ---------------- 3c. the documents of ref/test/fixtures/empty-write ----------------

    @Test void a_leeg_write_decides_nothing_beside_a_second_writer_and_in_a_window() throws Exception {
        // the documents the reference interpreter and the ASP backend play as well (RUNTIME-SEMANTICS ch. 3.11, "Leeg in
        // a write"): taxatie 40 in the input beside a writer of "zijn opgave" that fires without an opgave, next to a
        // second writer that does not fire, under the window "geldig vanaf 2020", and next to a writer of 50
        for (String d : List.of("one-fires-empty", "versioned-fires-empty", "empty-and-value")) {
            Path dir = EMPTY_WRITE.resolve(d);
            BothBranches.green(dir.resolve("ir.json"), JSON.readTree(Files.readString(dir.resolve("case.json"))), d);
        }
    }

    // ---------------- 4. a defaults group beside the rule ----------------

    @Test void a_rule_beside_a_defaults_group_on_its_target_is_refused() throws Exception {
        // The waardestelling model with a prioritizedDefaults group on the same target and type: a target has one
        // writer, the group or the rule, so the model is WF-050-invalid, and the analysis refuses it by name as the
        // verifier does. The group node is copied off a PLAYED golden's wire (nestdiepte), so its shape is demonstrably
        // carried by loader and transpiler.
        ObjectNode ir = (ObjectNode) JSON.readTree(Files.readString(
                CORPUS.resolve("anchor").resolve("jutterswet-waardestelling").resolve("ir.json")));
        ObjectNode group = ((ObjectNode) IrPath.rulesList(JSON.readTree(Files.readString(
                CORPUS.resolve("anchor").resolve("jutterswet-nestdiepte").resolve("ir.json"))),
                "prioritizedDefaults").get(0)).deepCopy();
        group.put("name", "geschatte waarde default (witness)");
        group.put("subject", "Vondst");
        group.put("attribute", "geschatte waarde");
        group.set("source", IrPath.sourceReference("witness: gemeld geeft 12.00"));
        ObjectNode d = ((ObjectNode) group.get("defaults").get(0)).deepCopy();
        d.set("justification", JSON.readTree(
                "{\"node\":\"CharacteristicCheck\",\"characteristic\":\"gemeld\",\"negated\":false,"
                        + "\"source\":{\"snippet\":\"hij gemeld is\"}}"));
        ((ObjectNode) d.get("consequence")).putObject("value").put("number", "12.00");
        d.set("source", IrPath.sourceReference("gemeld: 12.00"));
        d.remove("exceptions");   // absent, not null/[] — the only wire shape a childless Default carries
        ArrayNode defaults = JSON.createArrayNode();
        defaults.add(d);
        group.set("defaults", defaults);
        ArrayNode pds = JSON.createArrayNode();
        pds.add(group);
        IrPath.rules(ir).set("prioritizedDefaults", pds);
        Path tmp = Files.createTempFile("other-writer-witness", ".json");
        try {
            Files.writeString(tmp, ir.toString());
            BothBranches.refused(tmp, JSON.readTree(vondst("\"geschatte waarde\": { \"number\": \"77.00\" }",
                    "\"gemeld\"", value("12.00"))), "een regel naast een defaultsgroep van hetzelfde type op hetzelfde"
                    + " doel ('geschatte waarde' van Vondst): een doel heeft één schrijver, de groep of de regel (WF-050)",
                    "a rule beside a defaults group on one target");
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    // ---------------- helpers ----------------

    /** One authored case over a corpus golden's {@code ir.json}, replayed through BOTH transpiler branches —
     *  each must MATCH the expectation ({@link BothBranches}). */
    private static void play(String golden, String caseJson, String message) throws Exception {
        BothBranches.green(CORPUS.resolve(golden).resolve("ir.json"), JSON.readTree(caseJson), message);
    }

    /** A one-Vondst case for {@code jutterswet-waardestelling}: the given attributes, kenmerken, expectation. */
    private static String vondst(String attributes, String characteristics, String expectedNode) {
        return """
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "v1", "objectType": "Vondst",
                        "attributes": { %s },
                        "roles": {}, "characteristics": [ %s ] } ],
                    "parameters": {} },
                  "expected": [ %s, { "kind": "consistent" } ] }
                """.formatted(attributes, characteristics, expectedNode);
    }

    /** A one-Jutter case for {@code jutterswet-waardering-v0vorm}: attributes, kenmerken, expectation. */
    private static String jutter(String attributes, String characteristics, String expectedNode) {
        return """
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "M", "objectType": "Jutter",
                        "attributes": { %s },
                        "roles": {}, "characteristics": [ %s ] } ],
                    "parameters": {} },
                  "expected": [ %s, { "kind": "consistent" } ] }
                """.formatted(attributes, characteristics, expectedNode);
    }

    private static String value(String number) {
        return """
                { "kind": "attributeValue", "label": "v1", "attribute": "geschatte waarde",
                  "value": { "number": "%s" } }""".formatted(number);
    }

    private static String waardering(String number) {
        return """
                { "kind": "attributeValue", "label": "M", "attribute": "waardering",
                  "value": { "number": "%s" } }""".formatted(number);
    }

    /** The version-flank case: an asOfDate INSIDE the window {@link #versionWriter} puts on the rule, an input of
     *  77.00, and a leeg taxatiewaarde — so the rule fires and computes leeg. */
    private static String versionCase(String expectedNode) {
        return """
                { "format": "uchar-case-v0", "asOfDate": "2026-06-01",
                  "input": { "instances": [
                      { "label": "v1", "objectType": "Vondst",
                        "attributes": { "geschatte waarde": { "number": "77.00" } },
                        "roles": {}, "characteristics": [ "gemeld" ] } ],
                    "parameters": {} },
                  "expected": [ %s, { "kind": "consistent" } ] }
                """.formatted(expectedNode);
    }

    /** The waardestelling IR with a geldigheids-WINDOW on its single rule, written to a temp file. Derived from the
     *  golden, so the two models differ in exactly that clause — the corpus file itself is only read. */
    private static Path versionWriter() throws Exception {
        ObjectNode ir = (ObjectNode) JSON.readTree(CORPUS.resolve("anchor")
                .resolve("jutterswet-waardestelling").resolve("ir.json").toFile());
        IrPath.version(ir, "rules", 0, 0).set("validity", JSON.createObjectNode()
                .put("node", "Validity").put("from", "2026-01-01").put("to", "2026-12-31"));
        Path out = Files.createTempFile("versie-schrijver", ".json");
        out.toFile().deleteOnExit();
        Files.writeString(out, JSON.writerWithDefaultPrettyPrinter().writeValueAsString(ir));
        return out;
    }

    /** The waardestelling IR with a SECOND gelijkstelling on the same target (its own leeg-by-default source),
     *  written to a temp file. Derived from the golden so the two models differ in exactly that one rule — the corpus
     *  file itself is only read. */
    private static Path secondWriter() throws Exception {
        ObjectNode ir = (ObjectNode) JSON.readTree(CORPUS.resolve("anchor")
                .resolve("jutterswet-waardestelling").resolve("ir.json").toFile());
        ObjectNode number = JSON.createObjectNode();
        number.put("node", "Number").put("whole", false).put("decimals", 2).put("sign", "unspecified");
        IrPath.addAttribute(IrPath.objectType(ir, "Vondst"), "tweede taxatie", number);

        ArrayNode rules = IrPath.rulesList(ir, "rules");
        ObjectNode second = rules.get(0).deepCopy();
        second.put("name", "Waardestelling tweede");
        ObjectNode source = (ObjectNode) second.get("versions").get(0).get("value");
        source.put("attribute", "tweede taxatie")
                .set("source", IrPath.sourceReference("de tweede taxatie van de Vondst"));
        rules.add(second);

        Path out = Files.createTempFile("tweede-schrijver", ".json");
        out.toFile().deleteOnExit();
        Files.writeString(out, JSON.writerWithDefaultPrettyPrinter().writeValueAsString(ir));
        return out;
    }
}
