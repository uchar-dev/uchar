package io.github.yhavinga.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
 * null write" implementation would pass every other assertion in the gate set.
 */
class StaandeScalaireNormWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    // ---------------- 1. the standing scalar target (ch. 7.8): one computing rule, no initialisatie ----------------

    @Test void staand_doel_met_lege_opbrengst_laat_de_invoerwaarde_staan() throws Exception {
        // the rule FIRES (gemeld) but its source is leeg: it decides nothing, so the input value stands.
        assertNull(speel("anchor/jutterswet-waardestelling",
                vondst("\"geschatte waarde\": { \"number\": \"77.00\" }", "\"gemeld\"", waarde("77.00"))),
                "a leeg computed value must not displace the case input");
    }

    @Test void staand_doel_met_gevulde_opbrengst_verdringt_de_invoerwaarde() throws Exception {
        // the other side of the same norm: a deciding computation overwrites the input — the arm may not
        // degenerate into "a standing target is never written".
        assertNull(speel("anchor/jutterswet-waardestelling",
                vondst("\"taxatiewaarde\": { \"number\": \"51.00\" }, \"geschatte waarde\": { \"number\": \"77.00\" }",
                        "\"gemeld\"", waarde("51.00"))),
                "a gevuld computed value displaces the case input");
    }

    @Test void staand_doel_zonder_invoer_blijft_leeg_bij_lege_opbrengst() throws Exception {
        // "the target keeps what the fact base holds" — and here it holds nothing, so the outcome is leeg.
        // The arm is a NO-WRITE, never a fallback to some other value.
        assertNull(speel("anchor/jutterswet-waardestelling",
                vondst("", "\"gemeld\"",
                        "{ \"kind\": \"attributeEmpty\", \"label\": \"v1\", \"attribute\": \"geschatte waarde\" }")),
                "no input and a leeg computation leaves the target leeg");
    }

    // ---------------- 2. the ch. 7.7 desugaring domain: one computing rule BESIDE an initialisatie ----------------

    @Test void ontsuikerd_doel_met_lege_opbrengst_laat_de_initialisatie_beslissen() throws Exception {
        // the fire-but-leeg corner that used to OSCILLATE here: the gelijkstelling wrote null, the
        // initialisatie refilled 25, and the exec model never reached a fixpoint. With the arm the
        // gelijkstelling writes nothing and the initialisatie decides — once.
        assertNull(speel("anchor/jutterswet-waardering-v0vorm", jutter("", "\"erkend\"", waardering("25"))),
                "a leeg computed value lets the initialisatie decide, without an oscillation");
    }

    @Test void ontsuikerd_doel_met_lege_opbrengst_laat_de_invoerwaarde_staan() throws Exception {
        // same domain, input present: the initialisatie's own §9.6 leeg-guard blocks it, so the INPUT stands.
        assertNull(speel("anchor/jutterswet-waardering-v0vorm",
                jutter("\"waardering\": { \"number\": \"41\" }", "\"erkend\"", waardering("41"))),
                "a leeg computed value leaves a case input standing over the initialisatie");
    }

    @Test void ontsuikerd_doel_met_gevulde_opbrengst_verdringt_beide() throws Exception {
        // and the deciding side: the computation beats both the input and the initialisatie.
        assertNull(speel("anchor/jutterswet-waardering-v0vorm",
                jutter("\"basisbedrag\": { \"number\": \"50\" }, \"waardering\": { \"number\": \"41\" }",
                        "\"erkend\"", waardering("50"))),
                "a gevuld computed value displaces input and initialisatie alike");
    }

    // ---------------- 3. the domain gate: a second writer takes the target OUT of the norm ----------------

    @Test void twee_rekenregels_op_een_doel_wissen_de_invoerwaarde_wel() throws Exception {
        // The waardestelling model plus ONE extra gelijkstelling on the same target: writer count 2, so
        // NEITHER the standing domain (exactly one writer) NOR the desugaring domain (no initialisatie)
        // holds it — the v0 erase is unretired there, and a leeg source wipes the input. ref agrees on this
        // very model+case (checked before pinning); a domain-free "skip null writes" would fail here.
        Path ir = tweedeSchrijver();
        assertNull(CaseRunner.speelCase(ir, JSON.readTree(
                vondst("\"geschatte waarde\": { \"number\": \"77.00\" }", "\"gemeld\"",
                        "{ \"kind\": \"attributeEmpty\", \"label\": \"v1\", \"attribute\": \"geschatte waarde\" }"))),
                "outside the domain the v0 erase still applies: the leeg source wipes the input");

        // and the same case with the standing expectation is a MISMATCH, so the assertion above is not
        // passing on a comparator that ignores absence.
        String fout = CaseRunner.speelCase(ir, JSON.readTree(
                vondst("\"geschatte waarde\": { \"number\": \"77.00\" }", "\"gemeld\"", waarde("77.00"))));
        assertNotNull(fout, "the input must be observably GONE, not merely unasserted");
        assertTrue(fout.contains("geschatte waarde"), "the mismatch names the wiped attribute: " + fout);
    }

    // ---------------- 4. the other_writer clause: a defaults group evicts the target ----------------

    @Test void een_defaults_groep_haalt_het_doel_uit_het_staande_domein() throws Exception {
        // ref's other_writer clause, witnessed on its OBSERVABLE consequence: with a prioritizedDefaults
        // group on the same target the RULE writer count stays 1, yet the target leaves the standing domain,
        // so the leeg write is NOT suppressed — the group refills what the rule erases and BOTH executors
        // diverge (ref on this exact model: "no fixpoint within 100 passes — divergence is a fault
        // (ch. 5.7)"; verified before pinning). WITHOUT the clause the write would be suppressed, the input
        // would stand at 77.00 and nothing would oscillate — a silent divergence from ref, invisible to
        // every other check in the gate set. The group node is copied off a PLAYED golden's wire
        // (nestdiepte), so its shape is demonstrably carried by loader and transpiler.
        ObjectNode ir = (ObjectNode) JSON.readTree(Files.readString(
                CORPUS.resolve("anchor").resolve("jutterswet-waardestelling").resolve("ir.json")));
        ObjectNode groep = ((ObjectNode) JSON.readTree(Files.readString(
                CORPUS.resolve("anchor").resolve("jutterswet-nestdiepte").resolve("ir.json")))
                .get("prioritizedDefaults").get(0)).deepCopy();
        groep.put("naam", "geschatte waarde default (witness)");
        groep.put("onderwerp", "Vondst");
        groep.put("attribuut", "geschatte waarde");
        groep.put("bron", "witness: gemeld geeft 12.00");
        ObjectNode d = ((ObjectNode) groep.get("defaults").get(0)).deepCopy();
        d.set("voorwaarde", JSON.readTree(
                "{\"node\":\"Kenmerk\",\"kenmerk\":\"gemeld\",\"navigatie\":null,\"negatie\":false,"
                        + "\"eigenaar\":null,\"periode\":null,\"bron\":\"hij gemeld is\"}"));
        ((ObjectNode) d.get("consequentie")).put("waarde", "12.00");
        d.put("bron", "gemeld: 12.00");
        d.remove("excepties");   // absent, not null/[] — the only wire shape a childless Default carries
        ArrayNode defaults = JSON.createArrayNode();
        defaults.add(d);
        groep.set("defaults", defaults);
        ArrayNode pds = JSON.createArrayNode();
        pds.add(groep);
        ir.set("prioritizedDefaults", pds);
        Path tmp = Files.createTempFile("other-writer-witness", ".json");
        try {
            Files.writeString(tmp, ir.toString());
            String fout = CaseRunner.speelCase(tmp, JSON.readTree(
                    vondst("\"geschatte waarde\": { \"number\": \"77.00\" }", "\"gemeld\"", waarde("12.00"))));
            assertNotNull(fout, "the target must have LEFT the standing domain: the erase-refill oscillation "
                    + "must be observable, exactly as ref faults on this model (ch. 5.7)");
            assertTrue(fout.contains("geen fixpunt"), fout);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    // ---------------- helpers ----------------

    /** One authored case over a corpus golden's {@code ir.json}; the per-case failure line, null on a match. */
    private static String speel(String golden, String caseJson) throws Exception {
        return CaseRunner.speelCase(CORPUS.resolve(golden).resolve("ir.json"), JSON.readTree(caseJson));
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

    /** The waardestelling IR with a SECOND gelijkstelling on the same target (its own leeg-by-default source),
     *  written to a temp file: the same model one writer past the standing domain. Derived from the golden so
     *  the two models differ in exactly that one rule — the corpus file itself is only read. */
    private static Path tweedeSchrijver() throws Exception {
        ObjectNode ir = (ObjectNode) JSON.readTree(CORPUS.resolve("anchor")
                .resolve("jutterswet-waardestelling").resolve("ir.json").toFile());
        ObjectNode getal = JSON.createObjectNode();
        getal.put("node", "Numeriek").put("geheel", false).put("decimalen", 2)
                .put("teken", "ONBEPAALD").putNull("eenheid");
        ((ObjectNode) ir.get("objecttypen").get("Vondst").get("attributen")).set("tweede taxatie", getal);

        ArrayNode regels = (ArrayNode) ir.get("resolvedRegels");
        ObjectNode tweede = regels.get(0).deepCopy();
        tweede.put("naam", "Waardestelling tweede");
        ObjectNode bron = (ObjectNode) tweede.get("waarde");
        bron.put("attribuut", "tweede taxatie")
                .put("onderwerpBron", "de tweede taxatie van de Vondst")
                .put("bron", "de tweede taxatie van de Vondst");
        regels.add(tweede);
        ((ArrayNode) ir.get("regels")).add(JSON.createObjectNode()
                .put("node", "Regel").put("naam", "Waardestelling tweede").put("soort", "regel"));

        Path uit = Files.createTempFile("tweede-schrijver", ".json");
        uit.toFile().deleteOnExit();
        Files.writeString(uit, JSON.writerWithDefaultPrettyPrinter().writeValueAsString(ir));
        return uit;
    }
}
