package io.github.yhavinga.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.github.yhavinga.regelspraak.loader.IrLoader;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

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

    // ---------------- 1. the two ch. 7.10 corners ----------------

    @Test void hoek_b_de_voorouderregel_als_geerfde_basis_weigert_luid() throws Exception {
        // A bare rule on the ANCESTOR's target that a descendant covers with its own defaults group. Under
        // ch. 7.1 that rule is the descendant's inherited base (SUBTYPING §4.1 rule 4), but it is also a
        // writer in its own right and fires extensionally on the descendant's instances — so lowering it
        // by dropping one of the two is a silent answer, and this backend used to drop the rule.
        ObjectNode ir = goldenIr(KETEN);
        // give Strandrecht.uitkering a bare rule beside the two defaults groups
        ObjectNode regel = ((ArrayNode) ir.get("resolvedRegels")).get(0).deepCopy();
        regel.put("naam", "uitkering van een strandrecht (kaal)");
        ((ObjectNode) regel.get("doel")).put("attribuut", "uitkering");
        ((ArrayNode) ir.get("resolvedRegels")).add(regel);
        ((ArrayNode) ir.get("regels")).add(
                JSON.createObjectNode().put("node", "Regel")
                        .put("naam", "uitkering van een strandrecht (kaal)").put("soort", "regel"));

        Path irPad = model("overerving-hoek-b", ir);
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                () -> CaseRunner.speelCase(irPad, geval(KETEN, "case1-basis.json")));
        // the OCaml sentence, verbatim: one document, one refusal, three executors
        assertTrue(e.getMessage().contains("carries a defaults stack for 'uitkering', which an ordinary rule"
                + " on the ancestor 'Strandrecht' also writes"), e.getMessage());
        assertTrue(e.getMessage().contains("an inherited ordinary writer as the stack's base is not lowered"
                + " (ch. 7.10)"), e.getMessage());
    }

    @Test void hoek_a_de_ongeordende_verzameling_over_de_grens_weigert_luid() throws Exception {
        // A declared-unordered SET on the supertype and an own stack on the subtype. The block order
        // composes one stack over one stack; a fold across that boundary is not defined, and guessing one
        // is the one thing a lowering must not do. It reaches the gate report through the MANIFEST (a
        // per-form transpiler SKIP), never as an unclassified fire-time ERROR.
        ObjectNode ir = goldenIr(KETEN);
        ArrayNode pds = (ArrayNode) ir.get("prioritizedDefaults");
        ObjectNode eersteGroep = (ObjectNode) pds.get(0);              // Strandrecht.uitkering
        eersteGroep.put("ongeordend", true);
        ObjectNode tweede = eersteGroep.deepCopy();
        tweede.put("naam", "uitkering van een strandrecht #2");
        pds.insert(1, tweede);

        Path irPad = model("overerving-hoek-a", ir);
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                () -> CaseRunner.speelCase(irPad, geval(KETEN, "case1-basis.json")));
        assertTrue(e.getMessage().contains("draagt 1 eigen en 2 geërfde groep(en)"), e.getMessage());
        assertTrue(e.getMessage().contains("de blokorde stelt één stapel over één stapel"
                + " samen (RUNTIME-SEMANTICS ch. 7.10)"), e.getMessage());
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

    private static Path model(String naam, ObjectNode ir) throws Exception {
        Path tmp = Files.createTempFile(naam, ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, ir.toString());
        return tmp;
    }
}
