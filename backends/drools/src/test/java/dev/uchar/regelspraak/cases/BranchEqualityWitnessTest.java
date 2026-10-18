package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.uchar.regelspraak.loader.IrLoader;

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
 * answers. {@link RoleFieldReaderWitnessTest} closed the reader audit WITHIN each branch; these two are inequalities
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
 * the four feitcreaties in {@code corpus/golden} carried no null anker, doel, zetVeld or joinVeld (the last two left
 * the wire at L2, with the rest of the bean-field names). So every model
 * below is SYNTHETIC, built in memory from a golden's {@code ir.json} off the wire; the corpus files are read and
 * never written. Each was pushed through the verifier, {@code ref/} and the clingo backend, and the measurements
 * are recorded at the test that pins them.
 */
class BranchEqualityWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** cwd under surefire is the module dir (backends/drools); the corpus is two levels up. */
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    /** A ch. 5.8 AANTAL and a ch. 5.8.2 SOM over `alle vondsten van de Jutter` — the pair whose single bean field
     *  lives on Vondst, named `aanmelder` after the role the SUBJECT plays. */
    private static final String VONDSTTELLING = "jutterswet-vondsttelling";
    /** A ch. 13.4.7 feitcreatie with one tussenstap: Kustvak -> Post -> Sein, writing Sein.seinvak. */
    private static final String POSTWACHT = "jutterswet-postwacht";

    // ---------------- 1. the DRL branch's own aggregation join ----------------

    @Test void the_drl_aggregation_join_refuses_a_join_without_field() throws Exception {
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
        Path ir = aggregationJoinUnderived(), c = ir(VONDSTTELLING).resolveSibling("case.json");
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                () -> CaseRunner.playCase(ir, JSON.readTree(Files.readString(c)), DroolsBackend.Branch.EXEC_MODEL));
        assertTrue(e.getMessage().contains("regel: aantal aangemelde vondsten — de aggregatie-join van 'het aantal"
                + " vondsten van de Jutter' heeft geen beanveld — de rol 'aanmelder' op Vondst is meervoudig"), e.getMessage());
        String drl = drlReason(ir, c);
        assertTrue(drl.contains("aggregatie-join"), drl);
        // SHARPENED 2026-09-18 (card drools-rolvelden-herhaalde-typenparen). This row used to demand the GENERIC
        // sentence here — "het beanmodel leidt voor de rol 'aanmelder' op Vondst geen referentieveld naar Jutter
        // af" — while the exec assertion three lines up demands the CARDINALITY one. That gap was the last of the
        // asymmetry this file exists to close: one fold-join failure, two diagnoses, and the DRL-text one silent
        // about WHY. The cardinality half now lives beside the shared reason function
        // (FactModelGenerator.noJoinFieldReason) and both branches restate it at this seam, so the row can ask
        // for the same words it asks of the exec branch.
        assertTrue(drl.contains("de rol 'aanmelder' op Vondst is meervoudig, dus haar opslag is het veld aan de"
                + " overzijde, en ook daar leidt het beanmodel er geen af (beide zijden meervoudig)"), drl);
        assertTrue(drl.contains("§13.3.9 opslagrichting"), drl);
    }

    @Test void the_drl_aggregation_join_refuses_the_displaced_field() throws Exception {
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
        Path ir = aggregationJoinDisplaced(), c = caseOf(countCaseWithoutRoles());
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                () -> CaseRunner.playCase(ir, JSON.readTree(Files.readString(c)), DroolsBackend.Branch.EXEC_MODEL));
        assertTrue(e.getMessage().contains("regel: aantal aangemelde vondsten — de aggregatie-join van 'het aantal"
                + " vondsten van de Jutter' heeft geen beanveld — de rol 'aanmelder' op Vondst deelt haar naam met een"
                + " gedeclareerd lid"), e.getMessage());
        String drl = drlReason(ir, c);
        assertTrue(drl.contains("aggregatie-join"), drl);
        assertTrue(drl.contains("de rol 'aanmelder' op Vondst deelt haar naam met een gedeclareerd lid"), drl);
        assertTrue(drl.contains("§13.3.9 opslagrichting"), drl);
    }

    @Test void the_undisturbed_aggregation_still_plays() throws Exception {
        // The over-refusal flank, and it matters more here than at any other port of this family: `scope` is on the
        // path of EVERY aggregation the DRL branch lowers, so a question asked one notch too strictly would empty
        // the branch. The untouched golden plays in BOTH branches, on the same case.
        Path ir = ir(VONDSTTELLING), c = ir.resolveSibling("case.json");
        assertNull(CaseRunner.playCase(ir, JSON.readTree(Files.readString(c))), "de aggregatie draagt haar veld");
        assertEquals("PLAYED", CaseRunner.viaDrlText(ir, List.of(c)).status());
    }

    // ---------------- 2. the DRL branch's conditional feitcreatie gate ----------------

    @Test void a_fact_creation_without_resolved_target_kind_does_not_load() throws Exception {
        // Until L4 the target type was a wire position of its own, typed ["string","null"], and a null there was a
        // well-formed document the exec branch refused by name ("feitcreatie (onopgelost)") while the DRL-text branch
        // died on a NullPointerException. Since L4 the wire does not write it: the target type is the declared
        // filler of the target role (IR-SPEC ch. 3.4), so the null has no spelling left, and a target role that names
        // no role of the created fact type leaves the creation without a target type. Both branches read one loader,
        // which refuses that document at the role, by name, in the verifier's WF-028 sentence — no branch gets a
        // record whose target type is empty.
        ObjectNode doc = goldenIr(POSTWACHT);
        IrPath.version(doc, "factCreations", 0, 0).put("targetRole", "vlaggenmast");
        Path ir = model("feitcreatie-zonder-doelsoort", doc), c = ir(POSTWACHT).resolveSibling("case.json");
        for (boolean viaDrl : new boolean[] { false, true }) {
            IrLoader.Documentfout e = assertThrows(IrLoader.Documentfout.class,
                    () -> CaseRunner.playCase(ir, JSON.readTree(Files.readString(c)), viaDrl));
            assertEquals("/rules/factCreations/0/versions/0/targetRole", e.site(), e.getMessage());
            assertTrue(e.getMessage().contains("'vlaggenmast' is not a role of 'vakbeseining'"), e.getMessage());
        }
    }

    @Test void the_drl_fact_creation_refuses_an_unresolved_anchor() throws Exception {
        // The same position on the other end of the construct, and equally well-formed. ref parts company here —
        // it faults instead of reaching a model — so this half could not become a golden; it is gated with the
        // other because it is one construct and one question, exactly as the exec branch asks it. Measured on the
        // pre-port jar:
        //     verifier: PASS
        //     ref     : EVALUATION FAULT: missing FeitCreatie.anker in IR node
        //     clingo  : TRANSLATION FAULT: missing FeitCreatie.anker
        //     Drools exec-model : EXCLUDE transpiler SKIP — "feitcreatie (onopgelost)"
        //     Drools DRL-tekst  : ERROR NullPointerException: Cannot read field "naam" because "anker" is null
        Path ir = factCreationWithout("anchor"), c = ir(POSTWACHT).resolveSibling("case.json");
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                () -> CaseRunner.playCase(ir, JSON.readTree(Files.readString(c)), DroolsBackend.Branch.EXEC_MODEL));
        assertTrue(e.getMessage().contains("feitcreatie: vakbeseining door het kustvak — een feitcreatie zonder opgelost"
                + " anker, doel, feittype of pad"), e.getMessage());
        // the DRL-text branch renders the analysis since card drl-teksttak-op-truth-maintenance and refuses in its
        // sentence; until then it said "onopgelost feitcreatie (onopgelost): vakbeseining door het kustvak"
        assertEquals(e.getMessage(), drlReason(ir, c));
    }

    @Test void the_drl_fact_creation_refuses_an_unnamed_creation() throws Exception {
        // `naam` is a wire-nullable position of this construct that no well-formedness rule covered when this row was
        // written: WF-034 gated zetVeld, joinVeld and the tussenstappen until L2 and crosses the symbolic block since,
        // and WF-094 clause (d) gated the five KEYED tables while feitCreaties is a LIST. Since then WF-107 covers it:
        // the verifier rejects a nameless form ("form in factCreations carries no name", measured 2026-10-10), so the
        // document is NOT well-formed, and the exec-model branch refuses it by name. ref and clingo still run
        // it. Measured on the pre-fix jar:
        //     verifier: PASS
        //     ref     : oracle: PASS (10 expectations)
        //     clingo  : clingo: PASS (10 expectations)
        //     Drools exec-model : PLAYED 1 — the null goes straight into the rule header and the creation FIRES
        //     Drools DRL-tekst  : ERROR NullPointerException: Cannot invoke "String.replace(…)" because "naam" is null
        // Until card drl-teksttak-op-truth-maintenance the destructive DRL-text branch played it, with a mutation probe
        // that pinned the creation firing (vakseinaantal 2 where 99 was asked); since that card it renders the
        // analysis and refuses the nameless form by name, as the verifier does (WF-107).
        Path ir = unnamedCreation(), c = ir(POSTWACHT).resolveSibling("case.json");
        unnamedRefused(ir, c);
    }

    @Test void two_unnamed_creations_do_not_collide_on_their_fallback_name() throws Exception {
        // The flank the nameless fallback itself opens: a DRL package refuses duplicate rule names, so ONE flat
        // fallback string turns two nameless creations into "Duplicate rule name: <unnamed ResolvedFeitCreatie>"
        // — measured — where the destructive exec-model branch played them. That would have replaced the inequality
        // this card closes with another one, so the fallback carried the construct's WIRE POSITION, and the destructive
        // DRL-text branch played until card drl-teksttak-op-truth-maintenance. The verifier rejects a nameless form
        // (WF-107), and the analysis refuses it, on both branches since that card.
        unnamedRefused(twoUnnamedCreations(), ir(POSTWACHT).resolveSibling("case.json"));
    }

    // ---------------- fixtures ----------------

    /** The analysis refuses a nameless fact creation by name, as the verifier does (WF-107), on both branches. */
    private static void unnamedRefused(Path ir, Path c) throws Exception {
        BothBranches.refused(ir, JSON.readTree(Files.readString(c)), "feitcreatie: (zonder naam) — een feitcreatie"
                + " zonder naam (WF-107)", "a nameless fact creation");
    }

        private static Path ir(String golden) {
        Path p = CORPUS.resolve(golden).resolve("ir.json");
        assertTrue(Files.exists(p), "golden ir.json present: " + p.toAbsolutePath());
        return p;
    }

    private static ObjectNode goldenIr(String golden) throws Exception {
        return (ObjectNode) JSON.readTree(Files.readString(ir(golden)));
    }

    private static ObjectNode number() {
        return JSON.createObjectNode().put("node", "Number").put("whole", true)
                .put("decimals", 0).put("sign", "unspecified");
    }

    private static Path temporary(String naam, String inhoud) throws Exception {
        Path tmp = Files.createTempFile(naam, ".json");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, inhoud);
        return tmp;
    }

    private static Path model(String naam, ObjectNode ir) throws Exception {
        return temporary(naam, ir.toString());
    }

    private static Path caseOf(String caseJson) throws Exception {
        return temporary("takgelijkheid-case", caseJson);
    }

    /** The DRL-TEXT branch's verdict on the same pair — the harness entry, never a gate path. */
    private static String drlReason(Path ir, Path c) {
        CaseRunner.Result r = CaseRunner.viaDrlText(ir, List.of(c));
        assertEquals("EXCLUDE", r.status(), "de DRL-tak weigert de vorm met een reden: " + r.reason());
        return r.reason();
    }

    /** jutterswet-vondsttelling with the to-ONE `aanmelder` role made to-many as well: one feittype, two to-many
     *  roles, so {@code FactModelGenerator.members} derives a reference at neither end. Verifier-clean, ref-clean
     *  and clingo-clean. */
    private static Path aggregationJoinUnderived() throws Exception {
        ObjectNode ir = goldenIr(VONDSTTELLING);
        for (JsonNode r : IrPath.declaratie(ir, "factTypes", "vondstaanmelding").get("roles"))
            if ("aanmelder".equals(r.get("name").asText())) {
                ((ObjectNode) r).put("many", true);
                ((ObjectNode) r).put("plural", "aanmelders");
            }
        return model("aggregatiejoin-onafgeleid", ir);
    }

    /** jutterswet-vondsttelling with a declared Numeriek `aanmelder` on Vondst displacing the reference field the
     *  join reads. Verifier-clean, ref-clean and clingo-clean. */
    private static Path aggregationJoinDisplaced() throws Exception {
        ObjectNode ir = goldenIr(VONDSTTELLING);
        IrPath.addAttribute(IrPath.objectType(ir, "Vondst"), "aanmelder", number());
        return model("aggregatiejoin-verdrongen", ir);
    }

    /** One Jutter and one Vondst with NO roles: the fold has nothing to find either way, so what the test observes
     *  is the LOWERING's answer and not the case-input port's. */
    private static String countCaseWithoutRoles() {
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

    /** jutterswet-postwacht with a wire-nullable position of the feitcreatie's version set to null. */
    private static Path factCreationWithout(String positie) throws Exception {
        ObjectNode ir = goldenIr(POSTWACHT);
        IrPath.version(ir, "factCreations", 0, 0).putNull(positie);
        return model("feitcreatie-zonder-" + positie, ir);
    }

    /** jutterswet-postwacht with the feitcreatie's name set to null (WF-107's position, nullable on the form). */
    private static Path unnamedCreation() throws Exception {
        ObjectNode ir = goldenIr(POSTWACHT);
        ((ObjectNode) IrPath.rulesList(ir, "factCreations").get(0)).putNull("name");
        return model("feitcreatie-zonder-naam", ir);
    }

    /** jutterswet-postwacht carrying the SAME nameless feitcreatie twice — two constructs whose only distinguisher
     *  is their wire position. Verifier-clean. */
    private static Path twoUnnamedCreations() throws Exception {
        ObjectNode ir = goldenIr(POSTWACHT);
        ObjectNode fc = (ObjectNode) IrPath.rulesList(ir, "factCreations").get(0);
        fc.putNull("name");
        IrPath.rulesList(ir, "factCreations").add(fc.deepCopy());
        return model("feitcreatie-twee-naamloos", ir);
    }
}
