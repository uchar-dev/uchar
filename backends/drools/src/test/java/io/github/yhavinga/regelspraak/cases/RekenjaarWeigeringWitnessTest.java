package io.github.yhavinga.regelspraak.cases;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.github.yhavinga.regelspraak.engine.BeanCompiler;
import io.github.yhavinga.regelspraak.loader.IrLoader;
import io.github.yhavinga.regelspraak.schema.FactModelGenerator;
import io.github.yhavinga.regelspraak.transpiler.DrlTranspiler;
import io.github.yhavinga.regelspraak.transpiler.kern.Dekking;
import io.github.yhavinga.regelspraak.transpiler.model.ExecModelTranspiler;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The context value {@code Rekenjaar}, which {@code spec/IR-SPEC.md} ch. 4.1 keeps in WF-098's closed set as
 * admitted-and-unexecuted: this backend refuses it in both branches, at every position the node can hold, in the
 * reference interpreter's own sentence — and the sibling {@code Rekendatum} still plays.
 *
 * <p>RED FIRST, measured on the jar of the L4 landing before its fix round, on the documents below, every one of
 * them {@code verifier: PASS}. The three authored rows (the node as a rule value, as the operand of a subtraction,
 * as a comparison operand): exec-model PLAYED 1 and DRL-text PLAYED 1 on each, with the year of the case's evaluation
 * date as the answer, where {@code ref} reported {@code EVALUATION FAULT: the Rekenjaar context value is not exercised
 * by any corpus case (ch. 8.2)} and the ASP backend declined in translation. The corpus twists (one literal turned
 * into the node): the DRL-text branch FAILED on the consistency assertion, the creation guard, the allocation guard,
 * the fact-creation guard and the fold filter, and ran out of its fire cap on the creation's initialisation — every
 * one of those is the year READ, not a refusal — while the exec-model branch fired it on the three guards and refused
 * the other positions under a generic scope note ("schending buiten Phase-D scope", "aggregatie buiten Phase-C
 * scope"), which names no form.
 *
 * <p>The refusal is made twice (see {@code kern.Rekenjaar}), and the rows are split to match. The rows through the
 * replay pin the check before lowering: one sentence, both branches, every position — the consistency row is the
 * one the transpiler arms alone cannot carry, since the exec-model branch reads that assertion through operand
 * readers that never meet the node. The manifest row pins the arms themselves: driven without the replay, neither
 * transpiler lowers the node, and each names it.
 */
class RekenjaarWeigeringWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    /** The reference interpreter's sentence, written out here rather than read from the backend's constant, so a
     *  drift of the constant away from {@code ref} fails this witness. */
    private static final String ZIN = "the Rekenjaar context value is not exercised by any corpus case (ch. 8.2)";

    private static final String GETAL = "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"ONBEPAALD\"}";
    private static final String DAG = "{\"node\":\"Date\",\"precision\":\"day\"}";
    private static final String REKENJAAR =
            "{\"node\":\"ContextValue\",\"name\":\"Rekenjaar\",\"type\":" + GETAL + ",\"bron\":\"het Rekenjaar\"}";
    private static final String REKENDATUM =
            "{\"node\":\"ContextValue\",\"name\":\"Rekendatum\",\"type\":" + DAG + ",\"bron\":\"de Rekendatum\"}";

    /** One object type and one rule, as the RegelSpraak frontend emits the source of each row. */
    private static final String DOCUMENT = """
        { "format": "uchar-ir-v1",
          "model": { "objectTypes": [ { "node": "ObjectType", "name": "Vondst", "animate": false,
              "attributes": [
                { "node": "Attribute", "name": "meldjaar", "datatype": %1$s },
                { "node": "Attribute", "name": "ouderdom", "datatype": %1$s },
                { "node": "Attribute", "name": "meldcode", "datatype": %1$s },
                { "node": "Attribute", "name": "vondstjaar", "datatype": %1$s },
                { "node": "Attribute", "name": "peildatum", "datatype": %2$s } ],
              "characteristics": [] } ],
            "factTypes": [], "parameters": [], "domains": [], "unitSystems": [], "dimensions": [], "timelines": [],
            "dayKinds": [] },
          "rules": { "resolvedRegels": [ %3$s ], "resolvedDagsoorten": [], "objectCreaties": [], "verdelingen": [],
            "consistenties": [], "uniciteiten": [], "feitCreaties": [], "resolvedStartpunten": [], "regelGroepen": [] },
          "diagnostics": [] }
        """;

    // ---------------- through the replay: both branches, one sentence ----------------

    @Test void als_regelwaarde_weigert_rekenjaar_bij_naam_in_beide_takken() throws Exception {
        // "Het meldjaar van een Vondst moet gesteld worden op het Rekenjaar." The case pins the answer both branches
        // gave before the refusal (2026 for an evaluation date in 2026), so the row would read a PLAY as a pass
        // and a FAIL as a fail: only the refusal it asserts satisfies it.
        BeideTakken.geweigerd(document(waardeRegel()), zaak("meldjaar", "{\"number\":\"2026\"}"), ZIN,
                "the context value as a rule value");
    }

    @Test void als_operand_van_een_berekening_of_een_vergelijking_ook() throws Exception {
        // "… berekend worden als het Rekenjaar min zijn vondstjaar" and "… gesteld worden op 1 indien het Rekenjaar
        // groter is dan zijn vondstjaar": the node inside an expression and inside a condition.
        BeideTakken.geweigerd(document(rekenRegel()), zaak("ouderdom", "{\"number\":\"26\"}"), ZIN,
                "the context value as the operand of a subtraction");
        BeideTakken.geweigerd(document(vergelijkRegel()), zaak("meldcode", "{\"number\":\"1\"}"), ZIN,
                "the context value as a comparison operand");
    }

    @Test void op_elke_andere_plek_in_een_document_ook() throws Exception {
        // One numeric literal of a playing golden turned into the node, one position per row; each golden PLAYS in
        // both branches untwisted, so the refusal is the twist's and nothing else's.
        String[][] plekken = {
            { "synth2-budgetgrens-onder", "/rules/consistenties/0/versions/0/assertion/rechts", "the assertion of a consistency" },
            { "synth2-budgetgrens-onder", "/rules/objectCreaties/0/versions/0/condition/rechts", "the guard of an object creation" },
            { "synth2-budgetgrens-onder", "/rules/objectCreaties/0/versions/0/initialisations/0/waarde/rechts",
              "an initialisation of an object creation" },
            { "jutterswet-voorwaardedeling", "/rules/verdelingen/0/versions/0/condition/rechts", "the guard of an allocation" },
            { "jutterswet-dubbelverbinding", "/rules/feitCreaties/1/versions/0/condition/rechts", "the guard of a fact creation" },
            { "jutterswet-drempelvangst", "/rules/resolvedRegels/0/versions/0/value/filter/rechts", "the filter of a fold" },
            { "anchor/jutterswet-strandrechtketen", "/rules/prioritizedDefaults/1/defaults/1/consequentie/rechts",
              "the consequence of a prioritized default" },
        };
        for (String[] p : plekken)
            BeideTakken.geweigerd(getwist(p[0], p[1]), eersteZaak(p[0]), ZIN, p[2] + " (" + p[0] + p[1] + ")");
    }

    @Test void de_rekendatum_blijft_in_beide_takken_spelen() throws Exception {
        // NARROWNESS. The sibling context value in the same position, read as the evaluation date: the refusal is of
        // one name, not of the node kind.
        BeideTakken.groen(document(IrPad.regel("peildatum", "GELIJKSTELLING", doel("peildatum"), REKENDATUM, "null")),
                zaak("peildatum", "{\"date\":\"2026-03-15\"}"), "Rekendatum as a rule value still plays");
    }

    // ---------------- without the replay: each transpiler's own arm ----------------

    @Test void geen_van_beide_transpilers_verlaagt_de_knoop_en_elk_noemt_hem() throws Exception {
        for (String regel : List.of(waardeRegel(), rekenRegel(), vergelijkRegel())) {
            IrLoader.Loaded l = IrLoader.loadAll(JSON.readTree(Files.readString(document(regel))));
            String sleutel = "regel: " + l.model().resolvedRegels.get(0).naam();
            Dekking drl = dekkingVan(DrlTranspiler.build(l.model(), l.lets(), l.prioritizedDefaults(), l.bronPaden(),
                    l.rolBindingen(), l.navigatieBindingen()).dekking(), sleutel);
            assertEquals(Dekking.Verdict.MARKER, drl.verdict(), "drl-tekst — " + sleutel + ": " + drl.reden());
            assertEquals(ZIN, drl.reden(), "drl-tekst names the node — " + sleutel);
            Dekking exec = dekkingVan(execDekking(l), sleutel);
            assertEquals(Dekking.Verdict.SKIP, exec.verdict(), "exec-model — " + sleutel + ": " + exec.reden());
            assertTrue(exec.reden().contains(ZIN), "exec-model names the node — " + sleutel + ": " + exec.reden());
        }
    }

    // ---------------- fixtures ----------------

    private static String waardeRegel() {
        return IrPad.regel("meldjaar", "GELIJKSTELLING", doel("meldjaar"), REKENJAAR, "null");
    }

    private static String rekenRegel() {
        return IrPad.regel("ouderdom", "BEREKENING", doel("ouderdom"),
                "{\"node\":\"Binair\",\"op\":\"MIN\",\"links\":" + REKENJAAR + ",\"rechts\":" + lees("vondstjaar")
                        + ",\"type\":" + GETAL + ",\"bron\":\"het Rekenjaar min zijn vondstjaar\"}", "null");
    }

    private static String vergelijkRegel() {
        return IrPad.regel("meldcode", "GELIJKSTELLING", doel("meldcode"), IrPad.literal("1", GETAL, "1"),
                "{\"node\":\"Vergelijking\",\"op\":\"GROTER\",\"links\":" + REKENJAAR + ",\"rechts\":" + lees("vondstjaar")
                        + ",\"periode\":null,\"bron\":\"het Rekenjaar groter is dan zijn vondstjaar\"}");
    }

    private static String doel(String attribuut) {
        return "{\"node\":\"AttribuutRef\",\"attribuut\":\"" + attribuut + "\",\"objecttype\":\"Vondst\",\"bron\":\"de "
                + attribuut + " van een Vondst\"}";
    }

    private static String lees(String attribuut) {
        return "{\"node\":\"AttribuutRef\",\"attribuut\":\"" + attribuut + "\",\"objecttype\":\"Vondst\",\"bron\":\"zijn "
                + attribuut + "\"}";
    }

    private static Path document(String regel) throws Exception {
        return tijdelijk("rekenjaar", (ObjectNode) JSON.readTree(DOCUMENT.formatted(GETAL, DAG, regel)));
    }

    /** One Vondst in the evaluation year 2026, found in 2000. */
    private static JsonNode zaak(String attribuut, String waarde) throws Exception {
        return JSON.readTree("""
            { "format": "uchar-case-v0", "asOfDate": "2026-03-15",
              "input": { "instances": [ { "label": "V1", "objectType": "Vondst",
                  "attributes": { "vondstjaar": { "number": "2000" } }, "roles": {}, "characteristics": [] } ],
                "parameters": {} },
              "expected": [ { "kind": "attributeValue", "label": "V1", "attribute": "%s", "value": %s } ] }
            """.formatted(attribuut, waarde));
    }

    /** The golden with the node at {@code pointer} replaced by Rekenjaar under the replaced node's own type. The node
     *  there must be a Literal: a twist that hit anything else would test a document other than the one described. */
    private static Path getwist(String golden, String pointer) throws Exception {
        ObjectNode ir = (ObjectNode) JSON.readTree(Files.readString(CORPUS.resolve(golden).resolve("ir.json")));
        JsonPointer p = JsonPointer.compile(pointer);
        JsonNode oud = ir.at(p);
        assertEquals("Literal", oud.path("node").asText(), "the twist hits a literal: " + golden + pointer);
        ObjectNode nieuw = (ObjectNode) JSON.readTree(REKENJAAR);
        nieuw.set("type", oud.get("type"));
        ((ObjectNode) ir.at(p.head())).set(p.last().getMatchingProperty(), nieuw);
        return tijdelijk("rekenjaar-twist", ir);
    }

    private static JsonNode eersteZaak(String golden) throws Exception {
        try (Stream<Path> s = Files.list(CORPUS.resolve(golden))) {
            Path zaak = s.filter(f -> f.getFileName().toString().matches("case.*\\.json")).sorted().findFirst()
                    .orElseThrow(() -> new AssertionError("no case file in " + golden));
            return JSON.readTree(Files.readString(zaak));
        }
    }

    private static Dekking dekkingVan(List<Dekking> dekking, String sleutel) {
        for (Dekking d : dekking) if (sleutel.equals(d.sleutel())) return d;
        throw new AssertionError("no manifest entry " + sleutel + " in " + dekking);
    }

    private static List<Dekking> execDekking(IrLoader.Loaded l) {
        ClassLoader cl = BeanCompiler.compileer(FactModelGenerator.genereer(l.model()));
        ClassLoader vorige = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(cl);
        try {
            return ExecModelTranspiler.build(l.model(), cl, l.prioritizedDefaults(), l.rolBindingen(), l.lets(),
                    l.bronPaden(), l.navigatieBindingen()).dekking();
        } finally {
            Thread.currentThread().setContextClassLoader(vorige);
        }
    }

    private static Path tijdelijk(String naam, ObjectNode ir) throws Exception {
        Path tmp = Files.createTempFile(naam, ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir.toString());
        return tmp;
    }
}
