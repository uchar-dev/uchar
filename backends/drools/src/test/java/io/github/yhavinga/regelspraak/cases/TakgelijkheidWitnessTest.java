package io.github.yhavinga.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The two places where the DRL-TEXT branch did not ask a question the EXEC-MODEL branch asks — one construct, two
 * answers. {@link RolveldlezerWitnessTest} closed the reader audit WITHIN each branch; these two are inequalities
 * BETWEEN the branches, found while measuring that audit and closed here (2026-09-11).
 *
 * <ol>
 *   <li>{@code DrlTranspiler.scope} derived the aggregation join and emitted {@code <rol> == $subject} without
 *       asking whether that role has a bean field. The exec counterpart has asked since 2026-08-30.</li>
 *   <li>{@code DrlTranspiler.feitCreatie} asked its ch. 13.3.9 questions only behind
 *       {@code if (anker != null && doel != null && zetVeld != null && joinVeld != null)}, and let a null one fall
 *       through to the emission. The exec counterpart refuses all four unconditionally.</li>
 * </ol>
 *
 * <p>No corpus form reaches either place with a missing field or a null position — measured, not assumed: the whole
 * golden set with case files (168) through the DRL branch gives ZERO bare {@code EXCLUDE drl compile} outcomes, and
 * the four feitcreaties in {@code corpus/golden} carry no null anker, doel, zetVeld or joinVeld. So every model
 * below is SYNTHETIC, built in memory from a golden's {@code ir.json} off the wire; the corpus files are read and
 * never written. Each was pushed through the verifier, {@code ref/} and the clingo backend, and the measurements
 * are recorded at the test that pins them.
 */
class TakgelijkheidWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** cwd under surefire is the module dir (backends/drools); the corpus is two levels up. */
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    /** A ch. 5.8 AANTAL and a ch. 5.8.2 SOM over `alle vondsten van de Jutter` — the pair whose single bean field
     *  lives on Vondst, named `aanmelder` after the role the SUBJECT plays. */
    private static final String VONDSTTELLING = "jutterswet-vondsttelling";
    /** A ch. 13.4.7 feitcreatie with one tussenstap: Kustvak -> Post -> Sein, writing Sein.seinvak. */
    private static final String POSTWACHT = "jutterswet-postwacht";

    // ---------------- 1. the DRL branch's own aggregation join ----------------

    @Test void de_drl_aggregatiejoin_weigert_een_join_zonder_veld() throws Exception {
        // The UNDERIVED half: with BOTH roles of `vondstaanmelding` to-many the bean model derives no reference at
        // either end, so the emitted join reads a getter nobody has. Measured on the pre-port jar:
        //     verifier: PASS
        //     ref     : oracle: PASS (7 expectations)
        //     clingo  : clingo: PASS (7 expectations)
        //     Drools exec-model : EXCLUDE transpiler SKIP — "de aggregatie-join heeft geen beanveld … beide zijden
        //                         meervoudig (§13.3.9 opslagrichting)"
        //     Drools DRL-tekst  : EXCLUDE drl compile: Unable to Analyse Expression aanmelder == $subject
        // Unlike most of the ch. 13.3.9 family this shape HAS an arbiter — ref and clingo both answer — so a golden
        // of it could exist. What the port buys is the DIAGNOSIS: a bare compiler message that named neither the
        // role, the member, nor the limit, and sank the WHOLE package, becomes the same per-construct sentence the
        // exec branch already printed.
        Path ir = aggregatieJoinOnafgeleid(), c = ir(VONDSTTELLING).resolveSibling("case.json");
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                () -> CaseRunner.speelCase(ir, JSON.readTree(Files.readString(c))));
        assertTrue(e.getMessage().contains("de aggregatie-join heeft geen beanveld"), e.getMessage());
        String drl = drlReden(ir, c);
        assertTrue(drl.contains("aggregatie-join"), drl);
        assertTrue(drl.contains("het beanmodel leidt voor de rol 'aanmelder' op Vondst geen referentieveld"
                + " naar Jutter af"), drl);
        assertTrue(drl.contains("§13.3.9 opslagrichting"), drl);
    }

    @Test void de_drl_aggregatiejoin_weigert_het_verdrongen_veld() throws Exception {
        // The DISPLACED half, and the sharper red: a declared Numeriek `aanmelder` on Vondst takes the name, so the
        // field EXISTS and holds a BigDecimal. `aanmelder == $subject` then compiles in the mvel dialect and simply
        // never matches — the fold ranges over nothing and the rule ANSWERS. Measured on the pre-port jar, with a
        // case that supplies no roles (a case that DID supply them would be refused by the case-input port first,
        // which would hide the lowering's own answer):
        //     verifier: PASS
        //     ref     : oracle: PASS (2 expectations)
        //     clingo  : clingo: PASS (2 expectations)
        //     Drools exec-model : EXCLUDE transpiler SKIP — the ch. 13.3.9 name-sharing sentence
        //     Drools DRL-tekst  : PLAYED 1
        // One construct, two answers, and the silent one is the branch that answered. The value it produced happens
        // to be right here (no link is established, so the empty fold is what ref computes too) — but only because
        // CaseRunner's own port refuses to write such a link; any route that DID establish the relation would fold
        // the BigDecimal's absence as an absent relation, silently.
        Path ir = aggregatieJoinVerdrongen(), c = geval(telCaseZonderRollen());
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                () -> CaseRunner.speelCase(ir, JSON.readTree(Files.readString(c))));
        assertTrue(e.getMessage().contains("de aggregatie-join heeft geen beanveld"), e.getMessage());
        String drl = drlReden(ir, c);
        assertTrue(drl.contains("aggregatie-join"), drl);
        assertTrue(drl.contains("de rol 'aanmelder' op Vondst deelt haar naam met een gedeclareerd lid"), drl);
        assertTrue(drl.contains("§13.3.9 opslagrichting"), drl);
    }

    @Test void de_ongestoorde_aggregatie_speelt_nog() throws Exception {
        // The over-refusal flank, and it matters more here than at any other port of this family: `scope` is on the
        // path of EVERY aggregation the DRL branch lowers, so a question asked one notch too strictly would empty
        // the branch. The untouched golden plays in BOTH branches, on the same case.
        Path ir = ir(VONDSTTELLING), c = ir.resolveSibling("case.json");
        assertNull(CaseRunner.speelCase(ir, JSON.readTree(Files.readString(c))), "de aggregatie draagt haar veld");
        assertEquals("PLAYED", CaseRunner.viaDrlTekst(ir, List.of(c)).status());
    }

    // ---------------- 2. the DRL branch's conditional feitcreatie gate ----------------

    @Test void de_drl_feitcreatie_weigert_een_onopgelost_doel() throws Exception {
        // `doel` is one of four wire positions the ch. 13.4.7 schema types ["string","null"], and WF-034 does NOT
        // cover it (it covers zetVeld, joinVeld and each tussenstap's veld/type — measured: a null zetVeld or
        // joinVeld FAILS the verifier, a null anker or doel PASSES). So this document is WELL-FORMED, and ref runs
        // it to a model: the reachable half of the gap. Measured on the pre-port jar:
        //     verifier: PASS
        //     ref     : oracle: PASS (10 expectations)   — ref reaches a model; a golden of this shape could exist
        //     clingo  : TRANSLATION FAULT: missing FeitCreatie.doel
        //     Drools exec-model : EXCLUDE transpiler SKIP — "feitcreatie (onopgelost)"
        //     Drools DRL-tekst  : ERROR NullPointerException: Cannot read field "naam" because "doel" is null
        // The red outcome is NOT a silent pass: it is a NullPointerException that names no construct — the message
        // says only that "doel" is null — and takes the whole transpile down, where the exec branch refuses that one
        // construct by name and carries on.
        Path ir = feitcreatieZonder("doel"), c = ir(POSTWACHT).resolveSibling("case.json");
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                () -> CaseRunner.speelCase(ir, JSON.readTree(Files.readString(c))));
        assertTrue(e.getMessage().contains("feitcreatie (onopgelost): vakbeseining door het kustvak"), e.getMessage());
        String drl = drlReden(ir, c);
        assertTrue(drl.contains("feitcreatie"), drl);
        assertTrue(drl.contains("onopgelost feitcreatie (onopgelost): vakbeseining door het kustvak"), drl);
    }

    @Test void de_drl_feitcreatie_weigert_een_onopgelost_anker() throws Exception {
        // The same position on the other end of the construct, and equally well-formed. ref parts company here —
        // it faults instead of reaching a model — so this half could not become a golden; it is gated with the
        // other because it is one construct and one question, exactly as the exec branch asks it. Measured on the
        // pre-port jar:
        //     verifier: PASS
        //     ref     : EVALUATION FAULT: missing FeitCreatie.anker in IR node
        //     clingo  : TRANSLATION FAULT: missing FeitCreatie.anker
        //     Drools exec-model : EXCLUDE transpiler SKIP — "feitcreatie (onopgelost)"
        //     Drools DRL-tekst  : ERROR NullPointerException: Cannot read field "naam" because "anker" is null
        Path ir = feitcreatieZonder("anker"), c = ir(POSTWACHT).resolveSibling("case.json");
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                () -> CaseRunner.speelCase(ir, JSON.readTree(Files.readString(c))));
        assertTrue(e.getMessage().contains("feitcreatie (onopgelost): vakbeseining door het kustvak"), e.getMessage());
        String drl = drlReden(ir, c);
        assertTrue(drl.contains("onopgelost feitcreatie (onopgelost): vakbeseining door het kustvak"), drl);
    }

    @Test void de_drl_feitcreatie_speelt_een_naamloze_creatie() throws Exception {
        // `naam` is the FIFTH wire-nullable position of this construct, and the only one of the five that no
        // well-formedness rule covers at all: WF-034 gates zetVeld, joinVeld and the tussenstappen, and WF-094
        // clause (d) gates the five KEYED tables while feitCreaties is a LIST. So a nameless creation is a fully
        // well-formed document that every other executor runs. Measured on the pre-fix jar:
        //     verifier: PASS
        //     ref     : oracle: PASS (10 expectations)
        //     clingo  : clingo: PASS (10 expectations)
        //     Drools exec-model : PLAYED 1 — the null goes straight into the rule header and the creation FIRES
        //     Drools DRL-tekst  : ERROR NullPointerException: Cannot invoke "String.replace(…)" because "naam" is null
        // Here the aligned answer is to PLAY, not to refuse: three executors run this document, so a refusal would
        // be the DRL branch inventing a limit the wire does not have. That the creation really fires is pinned by
        // the mutation probe below and not by the bare PLAYED — V1.vakseinaantal counts the CREATED vakbeseining
        // relation, so 2 is only reachable through the nameless rule.
        Path ir = feitcreatieZonder("naam"), c = ir(POSTWACHT).resolveSibling("case.json");
        assertNull(CaseRunner.speelCase(ir, JSON.readTree(Files.readString(c))), "exec speelt de naamloze creatie");
        assertEquals("PLAYED", CaseRunner.viaDrlTekst(ir, List.of(c)).status());

        Path mut = vakseinaantalOp(c, "99");
        String execFout = CaseRunner.speelCase(ir, JSON.readTree(Files.readString(mut)));
        assertTrue(execFout != null && execFout.contains("vakseinaantal: verwacht 99, werkelijk 2"),
                String.valueOf(execFout));
        CaseRunner.Result drl = CaseRunner.viaDrlTekst(ir, List.of(mut));
        assertEquals("FAIL", drl.status(), drl.reason());
        assertTrue(drl.caseFailures().toString().contains("vakseinaantal: verwacht 99, werkelijk 2"),
                drl.caseFailures().toString());
    }

    @Test void twee_naamloze_creaties_botsen_niet_op_hun_terugvalnaam() throws Exception {
        // The flank the nameless fallback itself opens: a DRL package refuses duplicate rule names, so ONE flat
        // fallback string turns two nameless creations into "Duplicate rule name: <unnamed ResolvedFeitCreatie>"
        // — measured — where the exec branch plays them. That would have replaced the inequality this card closes
        // with another one, so the fallback carries the construct's WIRE POSITION. Both branches play.
        Path ir = tweeNaamlozeCreaties(), c = ir(POSTWACHT).resolveSibling("case.json");
        assertNull(CaseRunner.speelCase(ir, JSON.readTree(Files.readString(c))), "exec speelt beide creaties");
        CaseRunner.Result drl = CaseRunner.viaDrlTekst(ir, List.of(c));
        assertEquals("PLAYED", drl.status(), drl.reason());
    }

    // ---------------- fixtures ----------------

    private static Path ir(String golden) {
        Path p = CORPUS.resolve(golden).resolve("ir.json");
        assertTrue(Files.exists(p), "golden ir.json present: " + p.toAbsolutePath());
        return p;
    }

    private static ObjectNode goldenIr(String golden) throws Exception {
        return (ObjectNode) JSON.readTree(Files.readString(ir(golden)));
    }

    private static ObjectNode numeriek() {
        return JSON.createObjectNode().put("node", "Numeriek").put("geheel", true)
                .put("decimalen", 0).put("teken", "ONBEPAALD").putNull("eenheid");
    }

    private static Path tijdelijk(String naam, String inhoud) throws Exception {
        Path tmp = Files.createTempFile(naam, ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, inhoud);
        return tmp;
    }

    private static Path model(String naam, ObjectNode ir) throws Exception {
        return tijdelijk(naam, ir.toString());
    }

    private static Path geval(String caseJson) throws Exception {
        return tijdelijk("takgelijkheid-case", caseJson);
    }

    /** The DRL-TEXT branch's verdict on the same pair — the harness entry, never a gate path. */
    private static String drlReden(Path ir, Path c) {
        CaseRunner.Result r = CaseRunner.viaDrlTekst(ir, List.of(c));
        assertEquals("EXCLUDE", r.status(), "de DRL-tak weigert de vorm met een reden: " + r.reason());
        return r.reason();
    }

    /** jutterswet-vondsttelling with the to-ONE `aanmelder` role made to-many as well: one feittype, two to-many
     *  roles, so {@code FactModelGenerator.leden} derives a reference at neither end. Verifier-clean, ref-clean
     *  and clingo-clean. */
    private static Path aggregatieJoinOnafgeleid() throws Exception {
        ObjectNode ir = goldenIr(VONDSTTELLING);
        for (JsonNode r : ir.get("feittypen").get("vondstaanmelding").get("rollen"))
            if ("aanmelder".equals(r.get("naam").asText())) {
                ((ObjectNode) r).put("meervoudig", true);
                ((ObjectNode) r).put("meervoud", "aanmelders");
            }
        return model("aggregatiejoin-onafgeleid", ir);
    }

    /** jutterswet-vondsttelling with a declared Numeriek `aanmelder` on Vondst displacing the reference field the
     *  join reads. Verifier-clean, ref-clean and clingo-clean. */
    private static Path aggregatieJoinVerdrongen() throws Exception {
        ObjectNode ir = goldenIr(VONDSTTELLING);
        ((ObjectNode) ir.get("objecttypen").get("Vondst").get("attributen")).set("aanmelder", numeriek());
        return model("aggregatiejoin-verdrongen", ir);
    }

    /** One Jutter and one Vondst with NO roles: the fold has nothing to find either way, so what the test observes
     *  is the LOWERING's answer and not the case-input port's. */
    private static String telCaseZonderRollen() {
        return """
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "J1", "objectType": "Jutter", "attributes": {}, "roles": {}, "characteristics": [] },
                      { "label": "V1", "objectType": "Vondst", "attributes": { "gewicht": { "number": "2.50" } },
                        "roles": {}, "characteristics": [] } ],
                    "parameters": {} },
                  "expected": [
                      { "kind": "attributeValue", "label": "J1", "attribute": "aantal aangemelde vondsten",
                        "value": { "number": "0" } },
                      { "kind": "consistent" } ] }
                """;
    }

    /** jutterswet-postwacht with one of the feitcreatie's five wire-nullable positions set to null. */
    private static Path feitcreatieZonder(String positie) throws Exception {
        ObjectNode ir = goldenIr(POSTWACHT);
        ((ObjectNode) ir.get("feitCreaties").get(0)).putNull(positie);
        return model("feitcreatie-zonder-" + positie, ir);
    }

    /** jutterswet-postwacht carrying the SAME nameless feitcreatie twice — two constructs whose only distinguisher
     *  is their wire position. Verifier-clean. */
    private static Path tweeNaamlozeCreaties() throws Exception {
        ObjectNode ir = goldenIr(POSTWACHT);
        ObjectNode fc = (ObjectNode) ir.get("feitCreaties").get(0);
        fc.putNull("naam");
        ((com.fasterxml.jackson.databind.node.ArrayNode) ir.get("feitCreaties")).add(fc.deepCopy());
        return model("feitcreatie-twee-naamloos", ir);
    }

    /** The postwacht case with the ONE expectation that observes the created relation retuned — the mutation probe
     *  that turns a bare PLAYED into proof the nameless rule fired. */
    private static Path vakseinaantalOp(Path caseFile, String waarde) throws Exception {
        ObjectNode c = (ObjectNode) JSON.readTree(Files.readString(caseFile));
        int geraakt = 0;
        for (JsonNode e : c.get("expected"))
            if (e.has("attribute") && "vakseinaantal".equals(e.get("attribute").asText())) {
                ((ObjectNode) e.get("value")).put("number", waarde);
                geraakt++;
            }
        assertEquals(1, geraakt, "de case draagt precies één vakseinaantal-verwachting");
        return geval(c.toString());
    }
}
