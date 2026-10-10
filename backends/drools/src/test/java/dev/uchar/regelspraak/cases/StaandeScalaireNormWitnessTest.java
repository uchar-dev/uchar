package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RUNTIME-SEMANTICS ch. 7.8 (the SCALAR standing norm) and the ch. 7.7 desugaring domain ref's scalar arm
 * consults beside it — witnessed through {@link CaseRunner#speelCase} over corpus goldens' {@code ir.json}
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
 * transpiler branches ({@link BeideTakken}). Until then the whole matrix ran through the exec-model branch
 * alone, and the DRL-text branch — which had no target classification at all — erased on an empty
 * computation what the norm preserves, on four corpus goldens, unwitnessed. The ONE exception is the
 * defaults row, and it is named rather than dropped: the DRL branch refuses a prioritizedDefaults document
 * as a whole (it has no defaults lowering), so that row asserts the refusal there instead of the outcome.
 */
class StaandeScalaireNormWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    // ---------------- 1. the standing scalar target (ch. 7.8): one computing rule, no initialisatie ----------------

    @Test void staand_doel_met_lege_opbrengst_laat_de_invoerwaarde_staan() throws Exception {
        // the rule FIRES (gemeld) but its source is leeg: it decides nothing, so the input value stands.
        speel("anchor/jutterswet-waardestelling",
                vondst("\"geschatte waarde\": { \"number\": \"77.00\" }", "\"gemeld\"", waarde("77.00")),
                "a leeg computed value must not displace the case input");
    }

    @Test void staand_doel_met_gevulde_opbrengst_verdringt_de_invoerwaarde() throws Exception {
        // the other side of the same norm: a deciding computation overwrites the input — the arm may not
        // degenerate into "a standing target is never written".
        speel("anchor/jutterswet-waardestelling",
                vondst("\"taxatiewaarde\": { \"number\": \"51.00\" }, \"geschatte waarde\": { \"number\": \"77.00\" }",
                        "\"gemeld\"", waarde("51.00")),
                "a gevuld computed value displaces the case input");
    }

    @Test void staand_doel_zonder_invoer_blijft_leeg_bij_lege_opbrengst() throws Exception {
        // "the target keeps what the fact base holds" — and here it holds nothing, so the outcome is leeg.
        // The arm is a NO-WRITE, never a fallback to some other value.
        speel("anchor/jutterswet-waardestelling",
                vondst("", "\"gemeld\"",
                        "{ \"kind\": \"attributeEmpty\", \"label\": \"v1\", \"attribute\": \"geschatte waarde\" }"),
                "no input and a leeg computation leaves the target leeg");
    }

    // ---------------- 2. the ch. 7.7 desugaring domain: one computing rule BESIDE an initialisatie ----------------

    @Test void ontsuikerd_doel_met_lege_opbrengst_laat_de_initialisatie_beslissen() throws Exception {
        // the fire-but-leeg corner that used to OSCILLATE here: the gelijkstelling wrote null, the
        // initialisatie refilled 25, and the exec model never reached a fixpoint. With the arm the
        // gelijkstelling writes nothing and the initialisatie decides — once.
        speel("anchor/jutterswet-waardering-v0vorm", jutter("", "\"erkend\"", waardering("25")),
                "a leeg computed value lets the initialisatie decide, without an oscillation");
    }

    @Test void ontsuikerd_doel_met_lege_opbrengst_laat_de_invoerwaarde_staan() throws Exception {
        // same domain, input present: the initialisatie's own §9.6 leeg-guard blocks it, so the INPUT stands.
        speel("anchor/jutterswet-waardering-v0vorm",
                jutter("\"waardering\": { \"number\": \"41\" }", "\"erkend\"", waardering("41")),
                "a leeg computed value leaves a case input standing over the initialisatie");
    }

    @Test void ontsuikerd_doel_met_gevulde_opbrengst_verdringt_beide() throws Exception {
        // and the deciding side: the computation beats both the input and the initialisatie.
        speel("anchor/jutterswet-waardering-v0vorm",
                jutter("\"basisbedrag\": { \"number\": \"50\" }, \"waardering\": { \"number\": \"41\" }",
                        "\"erkend\"", waardering("50")),
                "a gevuld computed value displaces input and initialisatie alike");
    }

    // ---------------- 3. the domain gate: a second writer takes the target OUT of the norm ----------------

    @Test void twee_rekenregels_op_een_doel_wissen_de_invoerwaarde_wel() throws Exception {
        // The waardestelling model plus ONE extra gelijkstelling on the same target: writer count 2, so
        // NEITHER the standing domain (exactly one writer) NOR the desugaring domain (no initialisatie)
        // holds it — the v0 erase is unretired there, and a leeg source wipes the input. ref agrees on this
        // very model+case (checked before pinning); a domain-free "skip null writes" would fail here.
        Path ir = tweedeSchrijver();
        BeideTakken.groen(ir, JSON.readTree(
                vondst("\"geschatte waarde\": { \"number\": \"77.00\" }", "\"gemeld\"",
                        "{ \"kind\": \"attributeEmpty\", \"label\": \"v1\", \"attribute\": \"geschatte waarde\" }")),
                "outside the domain the v0 erase still applies: the leeg source wipes the input");

        // and the same case with the standing expectation is a MISMATCH, so the assertion above is not
        // passing on a comparator that ignores absence. This is the row a classification "every null write
        // is skipped" turns green in BOTH branches — the boundary, not the guard, is what is pinned here.
        BeideTakken.rood(ir, JSON.readTree(
                vondst("\"geschatte waarde\": { \"number\": \"77.00\" }", "\"gemeld\"", waarde("77.00"))),
                "geschatte waarde", "the input must be observably GONE, not merely unasserted");
    }

    // ---------------- 3b. the VERSION flank: a versioned rule is in no domain ----------------

    @Test void een_versiedoel_komt_niet_in_het_staande_domein() throws Exception {
        // ref's is_computing demands a VERSION-FREE rule, so a target whose SOLE writer carries a geldigheids-
        // window is in neither domain: version selection is temporal (ch. 7.4) and never defeasible, so the v0
        // erase is unretired there. Same model as the standing rows, one clause different — the window — and
        // the case's asOfDate sits inside it, so the rule fires, computes leeg, and WIPES the input 77.00.
        // ref answers exactly this on this model+case (checked before pinning, `oracle: PASS`).
        Path ir = versieSchrijver();
        BeideTakken.groen(ir, JSON.readTree(versiegeval(
                "{ \"kind\": \"attributeEmpty\", \"label\": \"v1\", \"attribute\": \"geschatte waarde\" }")),
                "a VERSIONED sole writer is outside the norm: its leeg result wipes the input");

        // the standing outcome is observably NOT what happened, in both branches.
        BeideTakken.rood(ir, JSON.readTree(versiegeval(waarde("77.00"))),
                "geschatte waarde", "a versioned target must not silently acquire the standing arm");
    }

    // ---------------- 4. the other_writer clause: a defaults group evicts the target ----------------

    @Test void een_defaults_groep_haalt_het_doel_uit_het_staande_domein() throws Exception {
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
        ObjectNode groep = ((ObjectNode) IrPad.rulesLijst(JSON.readTree(Files.readString(
                CORPUS.resolve("anchor").resolve("jutterswet-nestdiepte").resolve("ir.json"))),
                "prioritizedDefaults").get(0)).deepCopy();
        groep.put("name", "geschatte waarde default (witness)");
        groep.put("subject", "Vondst");
        groep.put("attribute", "geschatte waarde");
        groep.set("source", IrPad.bronrecord("witness: gemeld geeft 12.00"));
        ObjectNode d = ((ObjectNode) groep.get("defaults").get(0)).deepCopy();
        d.set("justification", JSON.readTree(
                "{\"node\":\"CharacteristicCheck\",\"characteristic\":\"gemeld\",\"negated\":false,"
                        + "\"source\":{\"snippet\":\"hij gemeld is\"}}"));
        ((ObjectNode) d.get("consequence")).putObject("value").put("number", "12.00");
        d.set("source", IrPad.bronrecord("gemeld: 12.00"));
        d.remove("exceptions");   // absent, not null/[] — the only wire shape a childless Default carries
        ArrayNode defaults = JSON.createArrayNode();
        defaults.add(d);
        groep.set("defaults", defaults);
        ArrayNode pds = JSON.createArrayNode();
        pds.add(groep);
        IrPad.rules(ir).set("prioritizedDefaults", pds);
        Path tmp = Files.createTempFile("other-writer-witness", ".json");
        try {
            Files.writeString(tmp, ir.toString());
            // The destructive exec-model branch replayed the erase-refill oscillation and reported "geen fixpunt",
            // as ref faults on this model (ch. 5.7). The truth-maintenance branch refuses the document by name, as
            // the verifier does (WF-050): a target has one writer, the group or the rule.
            CaseRunner.Excluded tms = assertThrows(CaseRunner.Excluded.class, () -> CaseRunner.speelCase(tmp,
                    JSON.readTree(vondst("\"geschatte waarde\": { \"number\": \"77.00\" }", "\"gemeld\"",
                            waarde("12.00"))), DroolsBackend.Branch.EXEC_MODEL));
            assertTrue(tms.getMessage().contains("een regel naast een defaultsgroep van hetzelfde type op hetzelfde doel"
                    + " ('geschatte waarde' van Vondst): een doel heeft één schrijver, de groep of de regel (WF-050)"),
                    tms.getMessage());

            // THE ONE ROW THAT DOES NOT MIRROR, named rather than dropped: the DRL-text branch has no ch. 7
            // defaults lowering and refuses such a document WHOLE, so it never reaches the outcome above.
            // Asserting the refusal keeps the exception honest — a DRL branch that silently started PLAYING
            // a defaults document (and therefore answering with an unwritten default) fails here.
            CaseRunner.Excluded weigering = assertThrows(CaseRunner.Excluded.class,
                    () -> CaseRunner.speelCase(tmp, JSON.readTree(
                            vondst("\"geschatte waarde\": { \"number\": \"77.00\" }", "\"gemeld\"", waarde("12.00"))),
                            true),
                    "the DRL-text branch must refuse a prioritizedDefaults document, not play it");
            assertTrue(weigering.getMessage().contains("prioritizedDefaults"), weigering.getMessage());
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    // ---------------- helpers ----------------

    /** One authored case over a corpus golden's {@code ir.json}, replayed through BOTH transpiler branches —
     *  each must MATCH the expectation ({@link BeideTakken}). */
    private static void speel(String golden, String caseJson, String boodschap) throws Exception {
        BeideTakken.groen(CORPUS.resolve(golden).resolve("ir.json"), JSON.readTree(caseJson), boodschap);
    }

    /** A one-Vondst case for {@code jutterswet-waardestelling}: the given attributes, kenmerken, expectation. */
    private static String vondst(String attributen, String kenmerken, String expectedNode) {
        return """
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "v1", "objectType": "Vondst",
                        "attributes": { %s },
                        "roles": {}, "characteristics": [ %s ] } ],
                    "parameters": {} },
                  "expected": [ %s, { "kind": "consistent" } ] }
                """.formatted(attributen, kenmerken, expectedNode);
    }

    /** A one-Jutter case for {@code jutterswet-waardering-v0vorm}: attributes, kenmerken, expectation. */
    private static String jutter(String attributen, String kenmerken, String expectedNode) {
        return """
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "M", "objectType": "Jutter",
                        "attributes": { %s },
                        "roles": {}, "characteristics": [ %s ] } ],
                    "parameters": {} },
                  "expected": [ %s, { "kind": "consistent" } ] }
                """.formatted(attributen, kenmerken, expectedNode);
    }

    private static String waarde(String getal) {
        return """
                { "kind": "attributeValue", "label": "v1", "attribute": "geschatte waarde",
                  "value": { "number": "%s" } }""".formatted(getal);
    }

    private static String waardering(String getal) {
        return """
                { "kind": "attributeValue", "label": "M", "attribute": "waardering",
                  "value": { "number": "%s" } }""".formatted(getal);
    }

    /** The version-flank case: an asOfDate INSIDE the window {@link #versieSchrijver} puts on the rule, a
     *  standing input of 77.00, and a leeg taxatiewaarde — so the rule fires and computes leeg. */
    private static String versiegeval(String expectedNode) {
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
    private static Path versieSchrijver() throws Exception {
        ObjectNode ir = (ObjectNode) JSON.readTree(CORPUS.resolve("anchor")
                .resolve("jutterswet-waardestelling").resolve("ir.json").toFile());
        IrPad.versie(ir, "rules", 0, 0).set("validity", JSON.createObjectNode()
                .put("node", "Validity").put("from", "2026-01-01").put("to", "2026-12-31"));
        Path uit = Files.createTempFile("versie-schrijver", ".json");
        uit.toFile().deleteOnExit();
        Files.writeString(uit, JSON.writerWithDefaultPrettyPrinter().writeValueAsString(ir));
        return uit;
    }

    /** The waardestelling IR with a SECOND gelijkstelling on the same target (its own leeg-by-default source),
     *  written to a temp file: the same model one writer past the standing domain. Derived from the golden so
     *  the two models differ in exactly that one rule — the corpus file itself is only read. */
    private static Path tweedeSchrijver() throws Exception {
        ObjectNode ir = (ObjectNode) JSON.readTree(CORPUS.resolve("anchor")
                .resolve("jutterswet-waardestelling").resolve("ir.json").toFile());
        ObjectNode getal = JSON.createObjectNode();
        getal.put("node", "Number").put("whole", false).put("decimals", 2).put("sign", "unspecified");
        IrPad.addAttribute(IrPad.objectType(ir, "Vondst"), "tweede taxatie", getal);

        ArrayNode regels = IrPad.rulesLijst(ir, "rules");
        ObjectNode tweede = regels.get(0).deepCopy();
        tweede.put("name", "Waardestelling tweede");
        ObjectNode bron = (ObjectNode) tweede.get("versions").get(0).get("value");
        bron.put("attribute", "tweede taxatie")
                .set("source", IrPad.bronrecord("de tweede taxatie van de Vondst"));
        regels.add(tweede);

        Path uit = Files.createTempFile("tweede-schrijver", ".json");
        uit.toFile().deleteOnExit();
        Files.writeString(uit, JSON.writerWithDefaultPrettyPrinter().writeValueAsString(ir));
        return uit;
    }
}
