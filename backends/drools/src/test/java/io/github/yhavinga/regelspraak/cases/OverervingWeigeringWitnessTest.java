package io.github.yhavinga.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.github.yhavinga.regelspraak.loader.IrLoader;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel;
import io.github.yhavinga.regelspraak.model.RegelSpraakModel.Objecttype;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The refusals object-type inheritance (IR-SPEC ch. 2.2) adds to this backend, and the reason none of
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
 * <p>The rest are STRUCTURAL, in the IR loader. Until 2026-09-12 the first of them refused every chain
 * carrying a temporal OR dimensioned member, because the facet SIDE MAPS (granularity, timeline count,
 * dimensions) were read directly, per DECLARED type, at 122 reads (121 lines) in both transpiler branches. Those
 * reads walk the chain now, so that refusal is WITHDRAWN and its two tests are turned around into
 * witnesses of what the same mutations do today; the dimensioned half was over-broad even then, since
 * every dimension read already went through the chain-aware {@code dimensiesVan}.
 *
 * <p>What is LEFT is narrower and named for what it is: the §9.9 startpunt-ANCHOR family, a PROTOCOL
 * keyed on the DECLARED SUBJECT NAME rather than a member lookup. Two sites that fall independently —
 * an inherited attribute riding a named tijdlijn WITH A VARIABEL STARTPUNT, and a startpuntbepaling
 * declared on an ancestor — each with its own boundary test, because the narrowing has to be shown not
 * to over-reach: a FIXED-startpunt tijdlijn and a determination beside the chain both load. Measured
 * against the merge base, the exchange is not a relabelling in either direction: the narrowed guard
 * FREES the fixed-startpunt shape (base EXCLUDE, head PLAYED) and CATCHES two the broad one let through
 * silently — a determination on an ancestor (base PLAYED 4, the one non-degenerate hole) and the
 * degenerate {@code granulariteit: null} tijdlijn on which WF-005 demands no granularity facet at all
 * (base PLAYED 4). M1 made that degenerate shape unwritable — a TimelineFacet requires its granularity — so
 * its witness left this class on 2026-09-23 and stands in {@code loader.LaderWeigeringWitnessTest} as what it
 * pins now, a loader refusal of a grid-less facet. The unknown supertype and the cycle stay a last line
 * behind WF-103/WF-104: the
 * verifier rejects both at the document, and a loader that walked a cycle would hang instead of
 * reporting.
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
        ((ObjectNode) IrPad.rulesLijst(ir, "rules").get(0)).put("name", "uitkering van een strandrecht (kaal)");
        ObjectNode regel = IrPad.versie(ir, "rules", 0, 0);
        ((ObjectNode) regel.get("target")).put("objecttype", "Strandrecht");
        ((ObjectNode) regel.get("value").get("links")).put("objecttype", "Strandrecht");
        ObjectNode pd = (ObjectNode) IrPad.rulesLijst(ir, "prioritizedDefaults").get(0);
        pd.put("naam", "uitkering van een wrakrecht").put("onderwerp", "Wrakrecht");
        ((ObjectNode) pd.get("defaults").get(1).get("consequentie")).put("objecttype", "Wrakrecht");

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
        ArrayNode pds = IrPad.rulesLijst(ir, "prioritizedDefaults");
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

    @Test void een_eigen_ongeordende_set_weigert_luid() throws Exception {
        // The other half of the pair, and the one ch. 7.10 claims is pinned here: a
        // declared-unordered SET on the OWN level against an inherited stack. Here the design itself
        // is silent — §4.1 rule 3 speaks of an unordered PARENT group and no rule places a subtype's
        // own set against an inherited stack — so this refusal says "undefined", not "not lowered".
        // The two messages must stay apart: one is a lowering the wire cannot carry, the other is a
        // reading nobody has written.
        ObjectNode ir = goldenIr(KETEN);
        ArrayNode pds = IrPad.rulesLijst(ir, "prioritizedDefaults");
        ObjectNode wrak = (ObjectNode) pds.get(1);                     // Wrakrecht.uitkering
        wrak.put("ongeordend", true);
        ObjectNode tweede = wrak.deepCopy();
        tweede.put("naam", "uitkering van een wrakrecht #2");
        pds.add(tweede);

        Path irPad = model("overerving-eigen-set", ir);
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                () -> CaseRunner.speelCase(irPad, geval(KETEN, "case1-basis.json")));
        assertTrue(e.getMessage().contains("has no defined reading"), e.getMessage());
    }

    @Test void een_venster_in_de_basis_weigert_luid() throws Exception {
        // NOT lowered, deliberately: a base carrying a VALIDITY WINDOW would put version selection
        // inside the stack, and ch. 7.4 keeps that apart from defeasibility. The refusal has to be
        // LOUD because absorption takes the rule out of the rule circuit — a fold that merely skipped
        // the window would silence the writer without ever making it a base. Measured on
        // verifier-clean documents before this arm: the target came out silently leeg in all three
        // executors, and an ACTIVE windowed rule answered from the inherited base instead of its own
        // value. Versioned rules are a live corpus form, so this is not a hypothetical.
        ObjectNode ir = goldenIr(WRAKBASIS);
        ObjectNode geldigheid = (ObjectNode) IrPad.versie(ir, "rules", 0, 0).get("validity");
        geldigheid.put("vanaf", "2020-01-01");

        Path irPad = model("overerving-venster", ir);
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                () -> CaseRunner.speelCase(irPad, geval(WRAKBASIS, "case1-regel-als-basis.json")));
        assertTrue(e.getMessage().contains("a rule with a VALIDITY WINDOW beside an inherited defaults"
                + " stack"), e.getMessage());
        assertTrue(e.getMessage().contains("ch. 7.4 keeps apart from defeasibility"), e.getMessage());
    }

    // ---------------- 2. the loader refusals, and the two that were WITHDRAWN ----------------

    @Test void een_geerfd_temporeel_lid_laadt_en_leest_zijn_facet_op_de_keten() throws Exception {
        // Was a REFUSAL until 2026-09-12 (card drools-facet-keten): the facet SIDE MAPS were read per
        // DECLARED type at 122 reads in both branches, so an inherited timeline answered "not temporal"
        // for the subtype. Those reads go through the chain-closed accessors of Objecttype now, and the
        // witness is turned around with the refusal — from "the loader says no" to "the facet answers on
        // the subtype", which is what those reads were changed to ask.
        //
        // The mutation is the very one the refusal used: `grondslag` becomes a YEAR timeline on the
        // SUPERTYPE. The document LOADS now, and the subtype answers the facet question for a member it
        // does not declare — RED on the base twice over (the loader refused the document outright, and
        // with the refusal removed the own map is empty, which is the wrong judgement the refusal
        // existed to prevent). `golden/jutterswet-erfwacht` is the VALUE half, where an inherited
        // monthly attribute is folded over an inherited monthly characteristic and all three executors
        // answer 71; this test is the facet half, on a shape no golden can hold (a temporal READ into a
        // scalar target is Phase F in this backend and faults in ref).
        ObjectNode ir = goldenIr(KETEN);
        IrPad.timeline(IrPad.attribute(IrPad.objectType(ir, "Strandrecht"), "grondslag"), "JAAR");
        RegelSpraakModel m = IrLoader.loadAll(JSON.readTree(Files.readString(model("keten-temporeel", ir)))).model();
        Objecttype wrak = m.objecttypeOpNaam("Wrakrecht");
        assertEquals(RegelSpraakModel.Granulariteit.JAAR, wrak.granulariteitVan("grondslag"));
        assertTrue(wrak.granulariteitLeden().containsKey("grondslag"),
                "the chain-closed facet map is what the routed read sites ask");
        assertTrue(wrak.attribuutGranulariteit.isEmpty(),
                "the OWN map stays own — the bean generator subtracts inherited members and needs it");
    }

    @Test void een_geerfd_gedimensioneerd_lid_laadt_en_leest_zijn_dimensies_op_de_keten() throws Exception {
        // The other half of the withdrawn refusal, and it was OVER-BROAD even when it was written:
        // every dimension read in this backend already went through `dimensiesVan`, which has walked the
        // chain since the kernel — measured at the same card. So this test is red on the base for ONE
        // reason only, the loader, and the accessor half passed all along. Recorded that way rather than
        // as a repair, because a refusal that was never load-bearing is worth knowing about.
        ObjectNode ir = goldenIr(KETEN);
        IrPad.attribute(IrPad.objectType(ir, "Strandrecht"), "grondslag")
                .set("dimensions", JSON.createArrayNode().add("seizoen"));
        RegelSpraakModel m = IrLoader.loadAll(JSON.readTree(Files.readString(model("keten-dimensie", ir)))).model();
        Objecttype wrak = m.objecttypeOpNaam("Wrakrecht");
        assertEquals(List.of("seizoen"), wrak.dimensiesVan("grondslag"));
        assertTrue(wrak.gedimensioneerdeLeden().containsKey("grondslag"));
    }

    // The §9.9 startpunt-ANCHOR family: what is LEFT of the withdrawn refusal, and the one part of it
    // that really is chain-blind. Two sites that fall independently, plus the boundary the narrowing
    // must NOT cross. Every fixture below is VERIFIER-CLEAN — measured, and it is the point: a refusal
    // pinned on a document the verifier already rejects pins nothing, since no such document reaches an
    // executor through any gate. That is what the first versions of these two got wrong (an
    // `attribuutTijdlijnNaam` entry with no `tijdlijnen` declaration and no matching granularity is
    // WF-003 + WF-005), and the shapes here are the corrected ones.

    @Test void een_geerfde_variabele_tijdlijn_weigert_bij_het_laden() throws Exception {
        // Site (a). An ancestor declares an attribute riding a named tijdlijn WITH A VARIABEL STARTPUNT.
        // `heeftVariabelStartpunt` reads `attribuutTijdlijnNaam` on the type the reference names, so
        // from a subtype it answers false where the truth is true: the bucketing is skipped, silently,
        // in both branches.
        //
        // The determination is put on Kustgemeente — NOT on the chain — for two reasons: WF-016 demands
        // that a variable-start tijdlijn have one at all, and putting it on the ancestor would make site
        // (b) fire too, so the test would not show that (a) falls on its own.
        //
        // Measured (probe p1a): verifier PASS; on the merge base this was EXCLUDE anyway, caught by the
        // WITHDRAWN broad refusal — WF-005 forces the granularity facet to match the declaration, so a
        // named tijdlijn on verifier-clean input carries a granularity and the old test saw it. This
        // shape is therefore RESIDUE, not a newly-closed hole; the hole was the degenerate twin, which M1
        // made unwritable (its witness is now a loader refusal in loader.LaderWeigeringWitnessTest).
        ObjectNode ir = goldenIr(KETEN);
        benoemdeTijdlijn(ir, /*variabelStartpunt=*/true, /*granulariteit=*/"JAAR");
        startpuntOp(ir, "Kustgemeente");
        IrLoader.Refusal r = assertThrows(IrLoader.Refusal.class,
                () -> IrLoader.loadAll(JSON.readTree(Files.readString(model("keten-variabele-tijdlijn", ir)))));
        assertTrue(r.getMessage().contains("riding a NAMED tijdlijn with a VARIABEL STARTPUNT"), r.getMessage());
        assertTrue(r.getMessage().contains("keys on the DECLARED subject name"), r.getMessage());
    }

    @Test void een_geerfde_tijdlijn_met_VAST_startpunt_laadt_gewoon() throws Exception {
        // The boundary, and the whole reason the guard asks `heeftVariabelStartpunt` instead of testing
        // `attribuutTijdlijnNaam` for emptiness. On a FIXED-startpunt tijdlijn that predicate answers
        // false from the own map and from the chain alike: there is no blindness, so refusing would
        // print a reason ("reads it as unanchored") that is simply TRUE of the document. Measured
        // (probe p1): verifier PASS, merge base EXCLUDE (the broad refusal caught it), head PLAYED 4 —
        // so the narrowing FREES a shape as well as catching two, and this test is what holds it free.
        ObjectNode ir = goldenIr(KETEN);
        benoemdeTijdlijn(ir, /*variabelStartpunt=*/false, /*granulariteit=*/"JAAR");
        Path irPad = model("keten-vast-startpunt", ir);
        assertNull(CaseRunner.speelCase(irPad, geval(KETEN, "case1-basis.json")));
    }

    @Test void een_startpuntbepaling_op_een_voorouder_weigert_bij_het_laden() throws Exception {
        // Site (b), and it falls independently of (a): here NO attribute rides a named tijdlijn at all,
        // but a startpuntbepaling names the ANCESTOR. `isStartpuntLid` is asked of the type the
        // REFERENCE names and compares the determination's declared subject name, so for every subtype
        // instance it answers no and the §6.8 leeg-fout companion silently disappears for exactly those.
        //
        // Measured (probe p2): verifier PASS, merge base PLAYED 4. This is the ONE non-degenerate hole
        // this card closes — the withdrawn refusal tested granularity and dimensions, and a
        // determination entry carries neither.
        ObjectNode ir = goldenIr(KETEN);
        variabeleTijdlijnDeclaratie(ir);                                   // WF-016 wants one
        ankerAttribuut(ir, "Strandrecht");                                 // WF-032 wants the member
        startpuntOp(ir, "Strandrecht");
        IrLoader.Refusal r = assertThrows(IrLoader.Refusal.class,
                () -> IrLoader.loadAll(JSON.readTree(Files.readString(model("startpunt-voorouder", ir)))));
        assertTrue(r.getMessage().contains("startpuntbepaling declared on an ANCESTOR"), r.getMessage());
    }

    @Test void een_startpuntbepaling_naast_de_keten_laadt_gewoon() throws Exception {
        // Site (b)'s boundary: the same determination on a type that is NOT on the subtype's chain
        // decides nothing about a subtype read, so it must stay admitted. Measured (probe p3): verifier
        // PASS, merge base PLAYED 4, head PLAYED 4.
        ObjectNode ir = goldenIr(KETEN);
        variabeleTijdlijnDeclaratie(ir);
        ankerAttribuut(ir, "Kustgemeente");
        startpuntOp(ir, "Kustgemeente");
        assertNull(CaseRunner.speelCase(model("startpunt-naast-keten", ir), geval(KETEN, "case1-basis.json")));
    }

    @Test void een_onbekend_supertype_weigert_bij_het_laden() throws Exception {
        // WF-103's shape. The verifier rejects it at the document; the loader is the last line, because a
        // null parent would otherwise reach the chain walk and answer "no such member" for every inherited
        // name — a wrong diagnosis of a real defect. A defect of the document, so a hard error at the
        // position of the reference, never the gate's auto-excluded channel (2026-09-23).
        ObjectNode ir = goldenIr(KETEN);
        IrPad.objectType(ir, "Wrakrecht").put("supertype", "Bestaatniet");
        IrLoader.Documentfout e = assertThrows(IrLoader.Documentfout.class,
                () -> IrLoader.loadAll(JSON.readTree(Files.readString(model("supertype-onbekend", ir)))));
        assertTrue(e.getMessage().contains("supertype 'Bestaatniet' is not declared"), e.getMessage());
        assertTrue(e.plek().endsWith("/supertype"), e.getMessage());
    }

    @Test void een_supertype_bindt_onder_de_naamidentiteit_van_de_verifier() throws Exception {
        // The other side of the WF-103 guard above. WF-103 resolves the reference under the verifier's name
        // identity (checks.ml norm: runs of spaces and tabs collapsed, the ends trimmed, A-Z folded), and
        // verifier, ref and clingo all play each spelling below. Looked up exactly and then case-insensitively,
        // the two spellings with a space were reported undeclared (measured 2026-09-23): a hard error on a
        // document the three other executors play.
        for (String spelling : List.of("Strandrecht ", " strandrecht", "STRANDRECHT")) {
            ObjectNode ir = goldenIr(KETEN);
            IrPad.objectType(ir, "Wrakrecht").put("supertype", spelling);
            Path irPad = model("supertype-spelling", ir);
            RegelSpraakModel m = IrLoader.load(JSON.readTree(Files.readString(irPad)));
            assertSame(m.objecttypen.get("Strandrecht"), m.objecttypen.get("Wrakrecht").supertype, spelling);
            assertNull(CaseRunner.speelCase(irPad, geval(KETEN, "case1-basis.json")), spelling);
        }
    }

    @Test void een_supertypecyclus_weigert_bij_het_laden() throws Exception {
        // WF-104's shape, self-reference included. Without the guard the chain walk has no root to stop at
        // and this loader would hang rather than report — which is why it is a refusal here and not a
        // comment saying the verifier has it covered.
        ObjectNode ir = goldenIr(KETEN);
        IrPad.objectType(ir, "Strandrecht").put("supertype", "Wrakrecht");
        IrLoader.Documentfout e = assertThrows(IrLoader.Documentfout.class,
                () -> IrLoader.loadAll(JSON.readTree(Files.readString(model("supertype-cyclus", ir)))));
        assertTrue(e.getMessage().contains("supertype cycle at"), e.getMessage());
    }

    // ---------------- helpers ----------------

    private static ObjectNode goldenIr(String naam) throws Exception {
        return (ObjectNode) JSON.readTree(Files.readString(CORPUS.resolve(naam).resolve("ir.json")));
    }

    /** Declare tijdlijn `boekjaar` and hang a new SUPERTYPE attribute `peilwaarde` on it, with the
     *  granularity facet WF-005 demands (the declaration's own). This is the whole §13.3.6.9 shape,
     *  verifier-clean by construction rather than by luck. */
    private static void benoemdeTijdlijn(ObjectNode ir, boolean variabelStartpunt, String granulariteit) {
        IrPad.modelLijst(ir, "timelines").add(JSON.createObjectNode().put("node", "Tijdlijn").put("naam", "boekjaar")
                .put("variabelStartpunt", variabelStartpunt).put("granulariteit", granulariteit).put("aantal", 1));
        ObjectNode peilwaarde = IrPad.addAttribute(IrPad.objectType(ir, "Strandrecht"), "peilwaarde", getal());
        IrPad.timeline(peilwaarde, granulariteit).put("timelineName", "boekjaar");   // WF-005: adoption must be total
    }

    /** The tijdlijn declaration alone, variable-start — for site (b), where no attribute rides it. */
    private static void variabeleTijdlijnDeclaratie(ObjectNode ir) {
        IrPad.modelLijst(ir, "timelines").add(JSON.createObjectNode().put("node", "Tijdlijn")
                .put("naam", "boekjaar").put("variabelStartpunt", true)
                .put("granulariteit", "JAAR").put("aantal", 1));
    }

    /** The synthetic anchor member a startpuntbepaling names — WF-032 requires it to be declared. */
    private static void ankerAttribuut(ObjectNode ir, String objecttype) {
        IrPad.addAttribute(IrPad.objectType(ir, objecttype), "meetdatum",
                JSON.createObjectNode().put("node", "Date").put("precision", "day"));
    }

    private static void startpuntOp(ObjectNode ir, String objecttype) {
        IrPad.rulesLijst(ir, "startPointDeterminations").add(JSON.createObjectNode()
                .put("node", "ResolvedStartpunt").put("tijdlijn", "boekjaar")
                .put("objecttype", objecttype).put("lid", "meetdatum"));
    }

    private static ObjectNode getal() {
        return JSON.createObjectNode().put("node", "Number").put("whole", true).put("decimals", 0)
                .put("sign", "NIET_NEGATIEF");
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
