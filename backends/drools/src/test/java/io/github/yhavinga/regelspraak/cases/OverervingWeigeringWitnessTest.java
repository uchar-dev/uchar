package io.github.yhavinga.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.github.yhavinga.regelspraak.loader.IrLoader;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The five refusals object-type inheritance (IR-SPEC ch. 2.2) adds to this backend, and the reason none of
 * them can have a corpus witness.
 *
 * <p>Two are SEMANTIC (RUNTIME-SEMANTICS ch. 7.10): the corners the block order does not lower. A corpus
 * golden must REPLAY, and {@code ref} faults on both shapes, so neither can ever sit in {@code
 * corpus/golden}; a red case must fault in every executor that CARRIES the form, and this backend does not
 * carry it either. The backend's own suite is where such a refusal is pinned — which is exactly the hole
 * this class closes: corner (b) shipped SILENT here (the per-type reduce-rule won and the ancestor's rule
 * left no trace, answering 30,00 where ref and clingo both faulted) while the two OCaml executors refused,
 * and no gate could see the split because no document of that shape can enter the corpus. Measured
 * 2026-09-12 on the three-level probe below:
 * <pre>
 *     verifier : PASS
 *     ref      : EVALUATION FAULT: 'Cc' carries a defaults stack for 'uitkering', …
 *     clingo   : TRANSLATION FAULT: (the same sentence)
 *     Drools   : PLAYED — uitkering 30,00
 * </pre>
 *
 * <p>Three are STRUCTURAL, in the IR loader: a chain carrying a temporal or dimensioned member, an unknown
 * supertype, and a cycle. The first is load-bearing rather than defensive — the facet SIDE MAPS
 * (granularity, timeline count, dimensions) are read directly, per DECLARED type, at some forty places in
 * both transpiler branches, so an inherited temporal member would answer "not temporal" for the subtype and
 * lower a timeline as a scalar. The other two are a last line behind WF-103/WF-104: the verifier rejects
 * both at the document, and a loader that walked a cycle would hang instead of reporting.
 *
 * <p>The messages are asserted by FRAGMENT, not merely by exception class. A refusal whose text drifts is a
 * refusal that stopped saying what it refuses, and on the two semantic corners the fragment is held
 * byte-for-byte against the OCaml sentence so the three executors keep refusing one document with one
 * voice.
 */
class OverervingWeigeringWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** cwd under surefire is the module dir (backends/drools); the corpus is two levels up. */
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    /** The kernel anchor: Strandrecht -> Wrakrecht plus an aggregating Kustgemeente. Every probe below is a
     *  single mutation of it, so what the refusal reacts to is exactly the mutation. */
    private static final String KETEN = "anchor/jutterswet-strandrechtketen";
    /** The rule-as-base golden: the subtype's own writer is a bare RULE over an inherited stack. */
    private static final String WRAKBASIS = "jutterswet-wrakbasis";

    // ---------------- 1. an ordinary rule as the base of its level ----------------

    @Test void een_voorouderregel_wordt_de_geerfde_basis() throws Exception {
        // SUBTYPING §4.1 rule 4: an ordinary rule on the ANCESTOR is the descendant's INHERITED base,
        // and rule 1 then lets the descendant's own base replace it. Built by swapping the two levels
        // of `jutterswet-wrakbasis`: the rule moves up to Strandrecht (grondslag x 2) and the group
        // moves down to Wrakrecht (base = grondslag, exception 99,00 on betwist).
        //
        // The pins are the discriminator. A Wrakrecht with grondslag 10,00 reads **10,00** — its own
        // base — where an implementation that let the ancestor's rule write anyway would read 20,00;
        // the Strandrecht beside it reads **20,00**, because on ITS chain no group is declared and the
        // rule keeps governing through the ordinary ch. 7.8 path. Until 2026-09-12 this whole shape
        // was a refusal in all three executors; it is lowered now, and this is the Drools half of the
        // witness (ref's is WV-7.10-f, and no corpus golden carries the swap).
        ObjectNode ir = goldenIr(WRAKBASIS);
        ObjectNode regel = (ObjectNode) ((ArrayNode) ir.get("resolvedRegels")).get(0);
        regel.put("naam", "uitkering van een strandrecht (kaal)");
        ((ObjectNode) regel.get("doel")).put("objecttype", "Strandrecht");
        ((ObjectNode) regel.get("waarde").get("links")).put("objecttype", "Strandrecht");
        ((ObjectNode) ir.get("regels").get(1)).put("naam", "uitkering van een strandrecht (kaal)");
        ObjectNode pd = (ObjectNode) ((ArrayNode) ir.get("prioritizedDefaults")).get(0);
        pd.put("naam", "uitkering van een wrakrecht").put("onderwerp", "Wrakrecht");
        ((ObjectNode) pd.get("defaults").get(1).get("consequentie")).put("objecttype", "Wrakrecht");
        ((ObjectNode) ir.get("regels").get(0)).put("naam", "uitkering van een wrakrecht");

        assertNull(CaseRunner.speelCase(model("overerving-regel-4", ir), geval(
                "{ \"format\": \"uchar-case-v0\", \"asOfDate\": null, \"input\": { \"instances\": ["
                + " { \"label\": \"S1\", \"objectType\": \"Strandrecht\","
                + "   \"attributes\": { \"grondslag\": { \"number\": \"10.00\" } },"
                + "   \"roles\": {}, \"characteristics\": [] },"
                + " { \"label\": \"W1\", \"objectType\": \"Wrakrecht\","
                + "   \"attributes\": { \"grondslag\": { \"number\": \"10.00\" } },"
                + "   \"roles\": {}, \"characteristics\": [] } ], \"parameters\": {} },"
                + " \"expected\": ["
                + " { \"kind\": \"attributeValue\", \"label\": \"S1\", \"attribute\": \"uitkering\","
                + "   \"value\": { \"number\": \"20.00\" } },"
                + " { \"kind\": \"attributeValue\", \"label\": \"W1\", \"attribute\": \"uitkering\","
                + "   \"value\": { \"number\": \"10.00\" } } ] }")));
    }

    @Test void de_kale_regel_zonder_geerfde_stapel_blijft_gewoon_spelen() throws Exception {
        // The boundary the absorption must NOT cross. The anchor's `registratiekosten` is exactly the
        // ordinary ch. 7.8 standing norm — a bare rule on a supertype with no defaults group anywhere
        // on that target's chain — and it fires extensionally for the subtype, 5,00 on both. If
        // absorption keyed on "an ancestor has a rule" alone it would swallow most of the corpus.
        // a null verdict IS the pass: speelCase returns the first mismatch, or nothing
        assertNull(CaseRunner.speelCase(
                CORPUS.resolve(KETEN).resolve("ir.json"), geval(KETEN, "case1-basis.json")));
    }

    // ---------------- 1b. what STAYS refused ----------------

    @Test void een_geerfde_ongeordende_set_weigert_luid() throws Exception {
        // A declared-unordered SET on the supertype and one own group on the subtype. SUBTYPING §4.1
        // rule 3 DEFINES this — the inherited block moves whole, above the base and below the own
        // exceptions — so the refusal names it as not lowered rather than as undefined. The reason is
        // a property of the wire: that reading is a three-layer priority (own exceptions > the
        // unordered inherited block > the own base) and a defaults group's members reconcile as
        // EQUALS, with no order between them. It reaches the gate report through the MANIFEST (a
        // per-form transpiler SKIP), never as an unclassified fire-time ERROR.
        ObjectNode ir = goldenIr(KETEN);
        ArrayNode pds = (ArrayNode) ir.get("prioritizedDefaults");
        ObjectNode eersteGroep = (ObjectNode) pds.get(0);              // Strandrecht.uitkering
        eersteGroep.put("ongeordend", true);
        ObjectNode tweede = eersteGroep.deepCopy();
        tweede.put("naam", "uitkering van een strandrecht #2");
        pds.insert(1, tweede);

        Path irPad = model("overerving-geerfde-set", ir);
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                () -> CaseRunner.speelCase(irPad, geval(KETEN, "case1-basis.json")));
        assertTrue(e.getMessage().contains("SUBTYPING §4.1 rule 3 defines it"), e.getMessage());
        assertTrue(e.getMessage().contains("not lowered in this release"), e.getMessage());
    }

    // ---------------- 2. the three loader refusals ----------------

    @Test void een_keten_met_een_temporeel_lid_weigert_bij_het_laden() throws Exception {
        ObjectNode ir = goldenIr(KETEN);
        ((ObjectNode) ir.get("objecttypen").get("Strandrecht")
                .get("attribuutGranulariteit")).put("grondslag", "JAAR");
        IrLoader.Refusal r = assertThrows(IrLoader.Refusal.class,
                () -> IrLoader.loadAll(JSON.readTree(Files.readString(model("keten-temporeel", ir)))));
        assertTrue(r.getMessage().contains("a supertype chain carrying a temporal or dimensioned member"),
                r.getMessage());
    }

    @Test void een_keten_met_een_gedimensioneerd_lid_weigert_bij_het_laden() throws Exception {
        ObjectNode ir = goldenIr(KETEN);
        ((ObjectNode) ir.get("objecttypen").get("Wrakrecht").get("attribuutDimensies"))
                .set("bergingsdiepte", JSON.createArrayNode().add("seizoen"));
        IrLoader.Refusal r = assertThrows(IrLoader.Refusal.class,
                () -> IrLoader.loadAll(JSON.readTree(Files.readString(model("keten-dimensie", ir)))));
        assertTrue(r.getMessage().contains("a supertype chain carrying a temporal or dimensioned member"),
                r.getMessage());
    }

    @Test void een_onbekend_supertype_weigert_bij_het_laden() throws Exception {
        // WF-103's shape. The verifier rejects it at the document; the loader is the last line, because a
        // null parent would otherwise reach the chain walk and answer "no such member" for every inherited
        // name — a wrong diagnosis of a real defect.
        ObjectNode ir = goldenIr(KETEN);
        ((ObjectNode) ir.get("objecttypen").get("Wrakrecht")).put("supertype", "Bestaatniet");
        IrLoader.Refusal r = assertThrows(IrLoader.Refusal.class,
                () -> IrLoader.loadAll(JSON.readTree(Files.readString(model("supertype-onbekend", ir)))));
        assertTrue(r.getMessage().contains("supertype 'Bestaatniet' is not declared"), r.getMessage());
    }

    @Test void een_supertypecyclus_weigert_bij_het_laden() throws Exception {
        // WF-104's shape, self-reference included. Without the guard the chain walk has no root to stop at
        // and this loader would hang rather than report — which is why it is a refusal here and not a
        // comment saying the verifier has it covered.
        ObjectNode ir = goldenIr(KETEN);
        ((ObjectNode) ir.get("objecttypen").get("Strandrecht")).put("supertype", "Wrakrecht");
        IrLoader.Refusal r = assertThrows(IrLoader.Refusal.class,
                () -> IrLoader.loadAll(JSON.readTree(Files.readString(model("supertype-cyclus", ir)))));
        assertTrue(r.getMessage().contains("supertype cycle at"), r.getMessage());
    }

    // ---------------- helpers ----------------

    private static ObjectNode goldenIr(String naam) throws Exception {
        return (ObjectNode) JSON.readTree(Files.readString(CORPUS.resolve(naam).resolve("ir.json")));
    }

    private static JsonNode geval(String golden, String bestand) throws Exception {
        return JSON.readTree(Files.readString(CORPUS.resolve(golden).resolve(bestand)));
    }

    private static JsonNode geval(String caseJson) throws Exception {
        return JSON.readTree(caseJson);
    }

    private static Path model(String naam, ObjectNode ir) throws Exception {
        Path tmp = Files.createTempFile(naam, ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir.toString());
        return tmp;
    }
}
