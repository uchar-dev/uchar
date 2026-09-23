package io.github.yhavinga.regelspraak.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.github.yhavinga.regelspraak.cases.CaseRunner;
import io.github.yhavinga.regelspraak.cases.IrPad;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The loader's refusals of a document that is not a well-formed {@code uchar-ir-v1} document in its M1 shape: the
 * container, the declaration lists, and the member nodes of an object type. Every fixture is a replaying corpus
 * golden with ONE twist in memory; the corpus file is read and never written.
 *
 * <p>Measured 2026-09-23 by the M1 lens on the loader as M1 first wrote it: each twist below was read without a
 * word — a node spelled {@code Objecttype} or without {@code node}, a v0 side map beside the member nodes, an
 * unknown field on a member node, an optional field written as {@code null}, a missing {@code diagnostics},
 * {@code dayKinds} or {@code characteristics}, {@code animate: "true"}, {@code objectTypes: {}}, a count of
 * {@code "2"} or {@code 2.0}, a characteristic declared twice, two object types whose names differ only in case,
 * an Enumeratie domain that kept its v0 self-name — each PLAYED, or loaded with part of the document ignored. The
 * OCaml loader refuses every one of them with a JSON pointer; this loader now does the same, in the same notation.
 *
 * <p>Each refusal is an {@link IrLoader.Documentfout}, a hard error, and never an {@link IrLoader.Refusal}: a
 * Refusal is the differential gate's auto-excluded channel ("form not carried"), and a malformed document filed
 * there leaves the gate green over a document nobody read. Corpus golden and red documents are verifier-clean,
 * so none of them reaches these arms; this suite is the only place they are pinned.
 */
class LaderWeigeringWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** cwd under surefire is the module dir (backends/drools); the corpus is two levels up. */
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    /** One object type, Bakenmeester: temporal attributes (JAAR and MAAND) and the temporal characteristic
     *  'dienstdoend'. One case, both branches PLAYED. */
    private static final String BAKENMEESTER = "jutterswet-bakenmeester";
    /** Klipjutter declares the supertype Jutter. One case, both branches PLAYED. */
    private static final String ERFWACHT = "jutterswet-erfwacht";
    /** The Enumeratie domain Vondstklasse. Three cases, both branches PLAYED. */
    private static final String VONDSTKLASSE = "jutterswet-vondstklasse";
    /** The kernel anchor of inheritance: Strandrecht -&gt; Wrakrecht plus an aggregating Kustgemeente. */
    private static final String KETEN = "anchor/jutterswet-strandrechtketen";
    /** One parameter, vondstdrempel. One case, both branches PLAYED. */
    private static final String PENNINGDREMPEL = "jutterswet-penningdrempel";
    /** Carries the model's functionDefs extension: EXCLUDE in both branches, by a loader Refusal. */
    private static final String KAMERHEFFING = "jutterswet-kamerheffing-catala";

    // ---------------- the node and its fields ----------------

    @Test void een_knoop_met_de_v0_spelling_van_zijn_soort_weigert() throws Exception {
        ObjectNode ir = golden(BAKENMEESTER);
        String ot = objectTypePad(ir, "Bakenmeester");
        IrPad.objectType(ir, "Bakenmeester").put("node", "Objecttype");
        IrLoader.Documentfout e = weigert(ir, ot + "/node");
        assertTrue(e.getMessage().contains("\"Objecttype\""), e.getMessage());
    }

    @Test void een_ledenknoop_zonder_node_weigert() throws Exception {
        ObjectNode ir = golden(BAKENMEESTER);
        String attr = lidPad(ir, "Bakenmeester", "attributes", "onderhoudstarief");
        IrPad.attribute(IrPad.objectType(ir, "Bakenmeester"), "onderhoudstarief").remove("node");
        weigert(ir, attr + "/node");
    }

    @Test void een_v0_zijkaart_naast_de_ledenknopen_weigert() throws Exception {
        // The side map was ignored whole: its entry gave no member a timeline, and the member node it
        // duplicates decided alone. A side map that DISAGREED with the member nodes went unread just as quietly.
        ObjectNode ir = golden(BAKENMEESTER);
        String ot = objectTypePad(ir, "Bakenmeester");
        IrPad.objectType(ir, "Bakenmeester").putObject("attribuutGranulariteit").put("onderhoudstarief", "DAG");
        IrLoader.Documentfout e = weigert(ir, ot + "/attribuutGranulariteit");
        assertTrue(e.getMessage().contains("unknown field"), e.getMessage());
    }

    @Test void een_onbekend_veld_op_een_m1_knoop_weigert() throws Exception {
        ObjectNode ir = golden(BAKENMEESTER);
        String ot = objectTypePad(ir, "Bakenmeester");
        IrPad.objectType(ir, "Bakenmeester").put("naam", "Bakenmeester");
        weigert(ir, ot + "/naam");

        ir = golden(BAKENMEESTER);
        String attr = lidPad(ir, "Bakenmeester", "attributes", "onderhoudstarief");
        IrPad.attribute(IrPad.objectType(ir, "Bakenmeester"), "onderhoudstarief").put("granulariteit", "JAAR");
        weigert(ir, attr + "/granulariteit");

        ir = golden(BAKENMEESTER);
        ((ObjectNode) IrPad.attribute(IrPad.objectType(ir, "Bakenmeester"), "onderhoudstarief").get("timeline"))
                .put("aantal", 1);
        weigert(ir, attr + "/timeline/aantal");

        ir = golden(BAKENMEESTER);
        String kenmerk = lidPad(ir, "Bakenmeester", "characteristics", "dienstdoend");
        IrPad.characteristic(IrPad.objectType(ir, "Bakenmeester"), "dienstdoend").put("soort", "BIJVOEGLIJK");
        weigert(ir, kenmerk + "/soort");
    }

    @Test void een_optioneel_veld_als_null_weigert() throws Exception {
        // IR-SPEC § Method: an optional field is written whole or not at all. Read leniently, each null below
        // was the same as the absent field, so a producer that wrote one never heard of it.
        ObjectNode ir = golden(BAKENMEESTER);
        String ot = objectTypePad(ir, "Bakenmeester");
        String attr = lidPad(ir, "Bakenmeester", "attributes", "onderhoudstarief");
        IrPad.objectType(ir, "Bakenmeester").putNull("plural");
        weigert(ir, ot + "/plural");

        ir = golden(BAKENMEESTER);
        IrPad.attribute(IrPad.objectType(ir, "Bakenmeester"), "onderhoudstarief").putNull("plural");
        weigert(ir, attr + "/plural");

        ir = golden(BAKENMEESTER);
        IrPad.attribute(IrPad.objectType(ir, "Bakenmeester"), "onderhoudstarief").putNull("dimensions");
        weigert(ir, attr + "/dimensions");

        ir = golden(BAKENMEESTER);
        ((ObjectNode) IrPad.attribute(IrPad.objectType(ir, "Bakenmeester"), "onderhoudstarief").get("timeline"))
                .putNull("timelineName");
        weigert(ir, attr + "/timeline/timelineName");

        // a null supertype made Klipjutter a root type without a word
        ir = golden(ERFWACHT);
        String klip = objectTypePad(ir, "Klipjutter");
        IrPad.objectType(ir, "Klipjutter").putNull("supertype");
        weigert(ir, klip + "/supertype");
    }

    @Test void een_ontbrekend_verplicht_veld_weigert() throws Exception {
        ObjectNode ir = golden(BAKENMEESTER);
        ir.remove("diagnostics");
        weigert(ir, "/diagnostics");

        ir = golden(BAKENMEESTER);
        IrPad.model(ir).remove("dayKinds");
        weigert(ir, "/model/dayKinds");

        ir = golden(BAKENMEESTER);
        String ot = objectTypePad(ir, "Bakenmeester");
        IrPad.objectType(ir, "Bakenmeester").remove("characteristics");
        weigert(ir, ot + "/characteristics");
    }

    @Test void een_veld_van_het_verkeerde_json_type_weigert() throws Exception {
        // animate "true" read as true, and objectTypes {} as a document with no object types at all
        ObjectNode ir = golden(BAKENMEESTER);
        String ot = objectTypePad(ir, "Bakenmeester");
        IrPad.objectType(ir, "Bakenmeester").put("animate", "true");
        weigert(ir, ot + "/animate");

        ir = golden(BAKENMEESTER);
        IrPad.model(ir).putObject("objectTypes");
        weigert(ir, "/model/objectTypes");

        ir = golden(BAKENMEESTER);
        String kenmerk = lidPad(ir, "Bakenmeester", "characteristics", "dienstdoend");
        IrPad.characteristic(IrPad.objectType(ir, "Bakenmeester"), "dienstdoend").put("name", 5);
        weigert(ir, kenmerk + "/name");
    }

    @Test void een_periodetelling_die_geen_geheel_getal_is_weigert() throws Exception {
        // "2" and 2.0 both read as 2: a raster the document did not write as a count
        for (JsonNode telling : List.of(JSON.getNodeFactory().textNode("2"), JSON.getNodeFactory().numberNode(2.0))) {
            ObjectNode ir = golden(BAKENMEESTER);
            String attr = lidPad(ir, "Bakenmeester", "attributes", "onderhoudstarief");
            ((ObjectNode) IrPad.attribute(IrPad.objectType(ir, "Bakenmeester"), "onderhoudstarief").get("timeline"))
                    .set("count", telling);
            IrLoader.Documentfout e = weigert(ir, attr + "/timeline/count");
            assertTrue(e.getMessage().contains("integer"), e.getMessage());
        }
    }

    @Test void een_tijdlijnfacet_zonder_granulariteit_weigert_bij_het_laden() throws Exception {
        // Until 2026-09-23 this was OverervingWeigeringWitnessTest's "de gedegenereerde variabele tijdlijn is het
        // gat dat werkelijk stil was": under v0 a tijdlijn declaration could carry `granulariteit: null`
        // (schema-legal), WF-005 then compared None with None, and an attribute riding that named tijdlijn needed
        // NO granularity entry — the silent §9.9 hole the narrowed inheritance guard closed (measured then, probe
        // p1cv: verifier PASS, merge base PLAYED 4). M1 made the shape unwritable: a member rides a named tijdlijn
        // only through a TimelineFacet, and the facet requires its granularity. What is left to pin is this
        // loader's refusal of the grid-less facet, instead of letting the member in as temporal with a null grid.
        // The old fixture's startpuntbepaling is gone: the facet is refused while the object types are read,
        // before any rule is.
        ObjectNode ir = golden(KETEN);
        IrPad.modelLijst(ir, "timelines").add(JSON.createObjectNode().put("node", "Tijdlijn").put("naam", "boekjaar")
                .put("variabelStartpunt", true).putNull("granulariteit").put("aantal", 1));
        ObjectNode peilwaarde = IrPad.addAttribute(IrPad.objectType(ir, "Strandrecht"), "peilwaarde",
                JSON.createObjectNode().put("node", "Numeriek").put("geheel", true).put("decimalen", 0)
                        .put("teken", "NIET_NEGATIEF").putNull("eenheid"));
        peilwaarde.putObject("timeline").put("node", "TimelineFacet").put("count", 1).put("timelineName", "boekjaar");
        IrLoader.Documentfout e = weigert(ir, lidPad(ir, "Strandrecht", "attributes", "peilwaarde")
                + "/timeline/granularity");
        assertTrue(e.getMessage().contains("required field is absent"), e.getMessage());
    }

    @Test void een_tijdlijnfacet_zonder_telling_weigert() throws Exception {
        // the characteristic's facet, the second of the two places a facet is read
        ObjectNode ir = golden(BAKENMEESTER);
        String kenmerk = lidPad(ir, "Bakenmeester", "characteristics", "dienstdoend");
        ((ObjectNode) IrPad.characteristic(IrPad.objectType(ir, "Bakenmeester"), "dienstdoend").get("timeline"))
                .remove("count");
        weigert(ir, kenmerk + "/timeline/count");
    }

    @Test void een_kenmerk_zonder_soort_of_met_een_onbekende_soort_weigert() throws Exception {
        ObjectNode ir = golden(BAKENMEESTER);
        String kenmerk = lidPad(ir, "Bakenmeester", "characteristics", "dienstdoend");
        IrPad.characteristic(IrPad.objectType(ir, "Bakenmeester"), "dienstdoend").remove("kind");
        weigert(ir, kenmerk + "/kind");

        // the v0 spelling of the value stays until M8, and its lower-case twin is no member
        ir = golden(BAKENMEESTER);
        IrPad.characteristic(IrPad.objectType(ir, "Bakenmeester"), "dienstdoend").put("kind", "bijvoeglijk");
        IrLoader.Documentfout e = weigert(ir, kenmerk + "/kind");
        assertTrue(e.getMessage().contains("'bijvoeglijk'") && e.getMessage().contains("BIJVOEGLIJK"), e.getMessage());
    }

    @Test void een_onbekende_sectie_weigert_en_functionDefs_blijft_een_vorm() throws Exception {
        // the v0 rule stubs (delta B3), a v0 model table name, a misspelt rules list: each was an "unknown
        // section" Refusal, the auto-excluded channel; each is a defect of the document
        ObjectNode ir = golden(BAKENMEESTER);
        ir.putArray("regels");
        weigert(ir, "/regels");

        ir = golden(BAKENMEESTER);
        IrPad.model(ir).putObject("objecttypen");
        weigert(ir, "/model/objecttypen");

        ir = golden(BAKENMEESTER);
        IrPad.rules(ir).putArray("prioritizedDefault");
        weigert(ir, "/rules/prioritizedDefault");

        // The boundary: functionDefs is an extension the schema admits and this backend does not carry — a
        // form, so it stays a Refusal and its goldens stay in the reasoned EXCLUDE channel.
        JsonNode catala = JSON.readTree(Files.readString(CORPUS.resolve(KAMERHEFFING).resolve("ir.json")));
        IrLoader.Refusal r = assertThrows(IrLoader.Refusal.class, () -> IrLoader.loadAll(catala));
        assertEquals("functionDefs", r.form(), r.getMessage());
    }

    // ---------------- names ----------------

    @Test void een_exact_dubbel_kenmerk_weigert() throws Exception {
        // WF-002. Read leniently, the second declaration's kind won and the first one's timeline stayed.
        ObjectNode ir = golden(BAKENMEESTER);
        ObjectNode ot = IrPad.objectType(ir, "Bakenmeester");
        ArrayNode kenmerken = (ArrayNode) ot.get("characteristics");
        kenmerken.addObject().put("node", "Characteristic").put("name", "dienstdoend").put("kind", "BEZITTELIJK");
        IrLoader.Documentfout e = weigert(ir, objectTypePad(ir, "Bakenmeester") + "/characteristics/"
                + (kenmerken.size() - 1) + "/name");
        assertTrue(e.getMessage().contains("'dienstdoend'"), e.getMessage());
    }

    @Test void twee_objecttypen_die_alleen_in_hoofdletters_verschillen_weigeren() throws Exception {
        // WF-001 compares case-insensitively; the model's tables are keyed on the exact string and its
        // lookup falls back to a case-insensitive search, which would pick one of the two.
        ObjectNode ir = golden(BAKENMEESTER);
        ArrayNode lijst = IrPad.modelLijst(ir, "objectTypes");
        ObjectNode kopie = IrPad.objectType(ir, "Bakenmeester").deepCopy().put("name", "bakenmeester");
        lijst.add(kopie);
        IrLoader.Documentfout e = weigert(ir, "/model/objectTypes/" + (lijst.size() - 1) + "/name");
        assertTrue(e.getMessage().contains("'Bakenmeester'"), "de melding noemt de eerste spelling: " + e.getMessage());
    }

    @Test void namen_botsen_onder_de_naamidentiteit_van_de_verifier() throws Exception {
        // WF-001 on a list whose node kept its v0 shape: a difference in case makes no second name (checks.ml
        // norm), and the exact-key table would have kept both
        ObjectNode ir = golden(PENNINGDREMPEL);
        ArrayNode parameters = IrPad.modelLijst(ir, "parameters");
        parameters.add(IrPad.declaratie(ir, "parameters", "vondstdrempel").deepCopy().put("naam", "Vondstdrempel"));
        IrLoader.Documentfout e = weigert(ir, "/model/parameters/" + (parameters.size() - 1) + "/naam");
        assertTrue(e.getMessage().contains("WF-001"), e.getMessage());

        // WF-002 on attributes, which the loader compared exactly before: neither case nor a run of spaces makes
        // a second name
        ir = golden(ERFWACHT);
        ObjectNode ot = IrPad.objectType(ir, "Jutter");
        ArrayNode attributen = (ArrayNode) ot.get("attributes");
        attributen.add(IrPad.attribute(ot, "wachtvergoeding per maand").deepCopy()
                .put("name", "Wachtvergoeding  per maand"));
        e = weigert(ir, objectTypePad(ir, "Jutter") + "/attributes/" + (attributen.size() - 1) + "/name");
        assertTrue(e.getMessage().contains("WF-002"), e.getMessage());
    }

    // ---------------- the domain ----------------

    @Test void een_enumeratiedomein_met_achtergebleven_zelfnaam_weigert() throws Exception {
        // IR-SPEC ch. 2.3 / delta B2: the Domain wrapper holds the name, and a self-name beside it is a second
        // spelling of one name.
        ObjectNode ir = golden(VONDSTKLASSE);
        String domein = "/model/domains/" + index(IrPad.modelLijst(ir, "domains"), "name", "Vondstklasse");
        ((ObjectNode) IrPad.domain(ir, "Vondstklasse").get("datatype")).put("domein", "Vondstklasse");
        weigert(ir, domein + "/datatype/domein");
    }

    // ---------------- what a refusal says ----------------

    @Test void een_onbekende_waarde_noemt_veld_en_waarde_en_geen_java_klasse() throws Exception {
        // Before: "No enum constant io.github.yhavinga.regelspraak.model.RegelSpraakModel.Granulariteit.jaar",
        // the JDK's sentence, naming a Java class and no position in the document.
        ObjectNode ir = golden(BAKENMEESTER);
        String attr = lidPad(ir, "Bakenmeester", "attributes", "onderhoudstarief");
        ((ObjectNode) IrPad.attribute(IrPad.objectType(ir, "Bakenmeester"), "onderhoudstarief").get("timeline"))
                .put("granularity", "jaar");
        IrLoader.Documentfout e = weigert(ir, attr + "/timeline/granularity");
        assertTrue(e.getMessage().contains("'jaar'"), e.getMessage());
        assertTrue(e.getMessage().contains("JAAR"), "de melding noemt de toegestane waarden: " + e.getMessage());
        assertFalse(e.getMessage().contains("No enum constant"), e.getMessage());
        assertFalse(e.getMessage().contains("io.github"), e.getMessage());
    }

    @Test void een_documentfout_bereikt_het_uitsluitkanaal_niet() throws Exception {
        // The channel, end to end: the replay turns a Refusal into EXCLUDE and lets every other exception
        // through, which the CLI prints as ERROR. Both replay branches and the red path build through this
        // loader first; the DRL branch and the red path are the ones reachable from this package.
        ObjectNode ir = golden(BAKENMEESTER);
        IrPad.objectType(ir, "Bakenmeester").put("node", "Objecttype");
        Path tmp = Files.createTempFile("documentfout", ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir.toString());
        List<Path> gevallen = gevallen(BAKENMEESTER);
        assertThrows(IrLoader.Documentfout.class, () -> CaseRunner.viaDrlTekst(tmp, gevallen));
        assertThrows(IrLoader.Documentfout.class, () -> CaseRunner.runRood(tmp, gevallen.get(0)));
    }

    // ---------------- helpers ----------------

    /** Loads the twisted document and demands a document defect at exactly {@code plek}. */
    private static IrLoader.Documentfout weigert(JsonNode ir, String plek) {
        IrLoader.Documentfout e = assertThrows(IrLoader.Documentfout.class, () -> IrLoader.loadAll(ir),
                "verwacht een documentfout op " + plek);
        assertEquals(plek, e.plek(), e.getMessage());
        return e;
    }

    private static ObjectNode golden(String naam) throws Exception {
        Path ir = CORPUS.resolve(naam).resolve("ir.json");
        assertTrue(Files.exists(ir), "golden ir.json present: " + ir.toAbsolutePath());
        ObjectNode doc = (ObjectNode) JSON.readTree(Files.readString(ir));
        IrLoader.loadAll(doc);   // the untwisted golden loads, so the refusal below is the twist's alone
        return doc;
    }

    private static List<Path> gevallen(String golden) throws Exception {
        try (var s = Files.list(CORPUS.resolve(golden))) {
            List<Path> uit = s.filter(p -> p.getFileName().toString().startsWith("case")).sorted().toList();
            assertFalse(uit.isEmpty(), "golden carries case files: " + golden);
            return uit;
        }
    }

    private static String objectTypePad(JsonNode ir, String objecttype) {
        return "/model/objectTypes/" + index(IrPad.modelLijst(ir, "objectTypes"), "name", objecttype);
    }

    private static String lidPad(JsonNode ir, String objecttype, String leden, String lid) {
        return objectTypePad(ir, objecttype) + "/" + leden + "/"
                + index((ArrayNode) IrPad.objectType(ir, objecttype).get(leden), "name", lid);
    }

    private static int index(ArrayNode lijst, String naamVeld, String naam) {
        for (int i = 0; i < lijst.size(); i++)
            if (naam.equals(lijst.get(i).path(naamVeld).asText(null))) return i;
        throw new AssertionError("no entry with " + naamVeld + " '" + naam + "' in " + lijst);
    }
}
