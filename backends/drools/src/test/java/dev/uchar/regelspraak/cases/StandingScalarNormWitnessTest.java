package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * RUNTIME-SEMANTICS ch. 7.8 (the SCALAR standing norm) and the ch. 7.7 desugaring domain ref's scalar arm
 * consults beside it — witnessed through {@link CaseRunner#playCase} over corpus goldens' {@code ir.json}
 * with cases authored HERE (no corpus expectation is read, moved or touched).
 *
 * <p>Both sides of the norm are pinned for both domains: a LEEG computed value decides nothing and what the
 * fact base holds stands (the input, or — in the desugaring domain — the initialisatie's value), while a
 * GEVULD one displaces it. A one-sided implementation (never write / always write) fails one of the two.
 *
 * <p>The third witness is the DOMAIN GATE itself: the same model with ONE EXTRA computing rule on the same
 * target leaves the standing domain (ref's writer count is 2), and there the v0 erase is still in force —
 * a leeg source DOES wipe the input. Verified against {@code ref/} on this exact model before it was pinned
 * here, so the two executors agree on the flank as well as on the norm. Without this witness a "skip every
 * null write" implementation would pass every other assertion in the gate set. Its twin is the VERSION
 * flank: a versioned computing rule is not version-free, so its target is in neither domain and the v0
 * erase stands there too — selection is temporal (ch. 7.4) and never defeasible.
 *
 * <p>DUAL-BRANCH since the drl-staande-waarden card (2026-09-18): every row below runs through BOTH Drools
 * transpiler branches ({@link BothBranches}). Until then the whole matrix ran through the exec-model branch
 * alone, and the DRL-text branch — which had no target classification at all — erased on an empty
 * computation what the norm preserves, on four corpus goldens, unwitnessed. The ONE exception is the
 * defaults row, and it is named rather than dropped: the DRL branch refuses a prioritizedDefaults document
 * as a whole (it has no defaults lowering), so that row asserts the refusal there instead of the outcome.
 */
class StandingScalarNormWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    // ---------------- 1. the standing scalar target (ch. 7.8): one computing rule, no initialisatie ----------------

    @Test void standing_target_with_empty_opbrengst_leaves_the_input_value_standing() throws Exception {
        // the rule FIRES (gemeld) but its source is leeg: it decides nothing, so the input value stands.
        play("anchor/jutterswet-waardestelling",
                vondst("\"geschatte waarde\": { \"number\": \"77.00\" }", "\"gemeld\"", waarde("77.00")),
                "a leeg computed value must not displace the case input");
    }

    @Test void standing_target_with_filled_opbrengst_displaces_the_input_value() throws Exception {
        // the other side of the same norm: a deciding computation overwrites the input — the arm may not
        // degenerate into "a standing target is never written".
        play("anchor/jutterswet-waardestelling",
                vondst("\"taxatiewaarde\": { \"number\": \"51.00\" }, \"geschatte waarde\": { \"number\": \"77.00\" }",
                        "\"gemeld\"", waarde("51.00")),
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

    // ---------------- 3. the domain gate: a second writer takes the target OUT of the norm ----------------

    @Test void two_rules_that_compute_on_a_target_clear_the_input_value_indeed() throws Exception {
        // The waardestelling model plus ONE extra gelijkstelling on the same target: writer count 2, so
        // NEITHER the standing domain (exactly one writer) NOR the desugaring domain (no initialisatie)
        // holds it — the v0 erase is unretired there, and a leeg source wipes the input. ref agrees on this
        // very model+case (checked before pinning); a domain-free "skip null writes" would fail here.
        Path ir = secondWriter();
        BothBranches.green(ir, JSON.readTree(
                vondst("\"geschatte waarde\": { \"number\": \"77.00\" }", "\"gemeld\"",
                        "{ \"kind\": \"attributeEmpty\", \"label\": \"v1\", \"attribute\": \"geschatte waarde\" }")),
                "outside the domain the v0 erase still applies: the leeg source wipes the input");

        // and the same case with the standing expectation is a MISMATCH, so the assertion above is not
        // passing on a comparator that ignores absence. This is the row a classification "every null write
        // is skipped" turns green in BOTH branches — the boundary, not the guard, is what is pinned here.
        BothBranches.red(ir, JSON.readTree(
                vondst("\"geschatte waarde\": { \"number\": \"77.00\" }", "\"gemeld\"", waarde("77.00"))),
                "geschatte waarde", "the input must be observably GONE, not merely unasserted");
    }

    // ---------------- 3b. the VERSION flank: a versioned rule is in no domain ----------------

    @Test void a_version_target_does_not_come_into_the_standing_domain() throws Exception {
        // ref's is_computing demands a VERSION-FREE rule, so a target whose SOLE writer carries a geldigheids-
        // window is in neither domain: version selection is temporal (ch. 7.4) and never defeasible, so the v0
        // erase is unretired there. Same model as the standing rows, one clause different — the window — and
        // the case's asOfDate sits inside it, so the rule fires, computes leeg, and WIPES the input 77.00.
        // ref answers exactly this on this model+case (checked before pinning, `oracle: PASS`).
        Path ir = versionWriter();
        BothBranches.green(ir, JSON.readTree(versionCase(
                "{ \"kind\": \"attributeEmpty\", \"label\": \"v1\", \"attribute\": \"geschatte waarde\" }")),
                "a VERSIONED sole writer is outside the norm: its leeg result wipes the input");

        // the standing outcome is observably NOT what happened, in both branches.
        BothBranches.red(ir, JSON.readTree(versionCase(waarde("77.00"))),
                "geschatte waarde", "a versioned target must not silently acquire the standing arm");
    }

    // ---------------- 4. the other_writer clause: a defaults group evicts the target ----------------

    @Test void a_defaults_group_gets_the_target_out_of_the_standing_domain() throws Exception {
        // ref's other_writer clause, witnessed on its OBSERVABLE consequence: with a prioritizedDefaults
        // group on the same target the RULE writer count stays 1, yet the target leaves the standing domain,
        // so the leeg write is NOT suppressed — the group refills what the rule erases and BOTH executors
        // diverge (ref on this exact model: "no fixpoint within 100 passes — divergence is a fault
        // (ch. 5.7)"; verified before pinning). WITHOUT the clause the erase is suppressed and the group
        // decides 12.00 unopposed — no oscillation, a quiet match with this very expectation — so the
        // assertNotNull below is what catches the clause's removal (lens-proven red), while ref still
        // diverges: a silent executor split no other check in the gate set can see.
        // HONESTY NOTE: this model is WF-050-INVALID (a rule beside a defaults target — the verifier rejects
        // it), which is exactly WHY the pd branch of other_writer is unreachable on valid IR; the witness
        // pins the clause's mechanics on the one shape that makes it observable, outside the gated corpus
        // zone. The verdeling/objectcreatie branches are legal but carry no corpus model (see the Result).
        // The group node is copied off a PLAYED golden's wire (nestdiepte), so its shape is demonstrably
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
            // The destructive exec-model branch replayed the erase-refill oscillation and reported "geen fixpunt",
            // as ref faults on this model (ch. 5.7). The analysis refuses the document by name, as the verifier does
            // (WF-050): a target has one writer, the group or the rule; on both branches since card
            // drl-teksttak-op-truth-maintenance, before which the DRL-text branch refused every prioritizedDefaults
            // document whole ("… the DRL-text branch has no defaults lowering"), the one row that did not mirror.
            BothBranches.refused(tmp, JSON.readTree(vondst("\"geschatte waarde\": { \"number\": \"77.00\" }",
                    "\"gemeld\"", waarde("12.00"))), "een regel naast een defaultsgroep van hetzelfde type op hetzelfde"
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
    private static String vondst(String attributes, String kenmerken, String expectedNode) {
        return """
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "v1", "objectType": "Vondst",
                        "attributes": { %s },
                        "roles": {}, "characteristics": [ %s ] } ],
                    "parameters": {} },
                  "expected": [ %s, { "kind": "consistent" } ] }
                """.formatted(attributes, kenmerken, expectedNode);
    }

    /** A one-Jutter case for {@code jutterswet-waardering-v0vorm}: attributes, kenmerken, expectation. */
    private static String jutter(String attributes, String kenmerken, String expectedNode) {
        return """
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "M", "objectType": "Jutter",
                        "attributes": { %s },
                        "roles": {}, "characteristics": [ %s ] } ],
                    "parameters": {} },
                  "expected": [ %s, { "kind": "consistent" } ] }
                """.formatted(attributes, kenmerken, expectedNode);
    }

    private static String waarde(String number) {
        return """
                { "kind": "attributeValue", "label": "v1", "attribute": "geschatte waarde",
                  "value": { "number": "%s" } }""".formatted(number);
    }

    private static String waardering(String number) {
        return """
                { "kind": "attributeValue", "label": "M", "attribute": "waardering",
                  "value": { "number": "%s" } }""".formatted(number);
    }

    /** The version-flank case: an asOfDate INSIDE the window {@link #versionWriter} puts on the rule, a
     *  standing input of 77.00, and a leeg taxatiewaarde — so the rule fires and computes leeg. */
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

    /** The waardestelling IR with a geldigheids-WINDOW on its single rule, written to a temp file: the same
     *  model one clause past version-freedom, and therefore out of both domains. Derived from the golden, so
     *  the two models differ in exactly that clause — the corpus file itself is only read. */
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
     *  written to a temp file: the same model one writer past the standing domain. Derived from the golden so
     *  the two models differ in exactly that one rule — the corpus file itself is only read. */
    private static Path secondWriter() throws Exception {
        ObjectNode ir = (ObjectNode) JSON.readTree(CORPUS.resolve("anchor")
                .resolve("jutterswet-waardestelling").resolve("ir.json").toFile());
        ObjectNode number = JSON.createObjectNode();
        number.put("node", "Number").put("whole", false).put("decimals", 2).put("sign", "unspecified");
        IrPath.addAttribute(IrPath.objectType(ir, "Vondst"), "tweede taxatie", number);

        ArrayNode rules = IrPath.rulesList(ir, "rules");
        ObjectNode tweede = rules.get(0).deepCopy();
        tweede.put("name", "Waardestelling tweede");
        ObjectNode bron = (ObjectNode) tweede.get("versions").get(0).get("value");
        bron.put("attribute", "tweede taxatie")
                .set("source", IrPath.sourceReference("de tweede taxatie van de Vondst"));
        rules.add(tweede);

        Path out = Files.createTempFile("tweede-schrijver", ".json");
        out.toFile().deleteOnExit();
        Files.writeString(out, JSON.writerWithDefaultPrettyPrinter().writeValueAsString(ir));
        return out;
    }
}
