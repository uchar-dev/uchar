package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The §13.3.9 reference-field readers that are NOT navigated reads — the second half of the family
 * {@link OpslagrichtingWitnessTest} section 3 opened. Those five READ an attribute over a hop; these six TOUCH the
 * relation field for something else: an exists-join, a fold's own join, a timeline hop, an allocation's recipient
 * join, a creation's three field positions, and a presence count.
 *
 * <p>Why they need witnesses of their own: {@code FactModelGenerator.leden} derives a bean field only where the role
 * is to-one AND uniquely reachable AND its name is not taken by a declared member. A reader that asks for the NAME
 * gets less than it thinks in two different ways, and the two fail differently. A DISPLACED field still exists and
 * holds a {@code BigDecimal}/{@code boolean}: a join comparing it never matches (silently), a chained read off it
 * faults, a setter handed a fact throws. An UNDERIVED field does not exist at all: every read of it is an
 * unclassified fault at first fire. Gate 12 refuses to excuse a fault, so each is a per-form refusal now.
 *
 * <p>Every model below is built in memory from a corpus golden's {@code ir.json} off the wire; the corpus files are
 * read and never written. Each was pushed through the verifier, {@code ref/} and the clingo backend BEFORE the gate
 * existed, and the four measurements are recorded at the test that pins them (2026-09-11). Both transpiler branches
 * are asserted where both carry the reader, because a reason in one branch and a bare compiler error in the other is
 * exactly the state this card closed.
 */
class RolveldlezerWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** cwd under surefire is the module dir (backends/drools); the corpus is two levels up. */
    private static final Path CORPUS = Path.of("..", "..", "corpus", "golden");

    /** A to-ONE `werf` beside a to-many `schuit` — the pair whose single field lives on Schuit, named after the role. */
    private static final String NAVIGATIEPREMIE = "synth-navigatiepremie";
    /** A §9.7.3 NAAR RATO allocation: Berging distributes over its Bergers, joined on Berger.berging. */
    private static final String BERGING = "jutterswet-berging";
    /** A §7.1 fold over a temporal element attribute (Vondst.bewaartarief, MAAND) joined on Vondst."bewarende jutter". */
    private static final String TEMPOVOUW = "jutterswet-tempovouw";
    /** A §13.4.7 feitcreatie with one tussenstap: Kustvak -> Post -> Sein, writing Sein.seinvak. */
    private static final String POSTWACHT = "jutterswet-postwacht";
    /** A §13.4.7 feitcreatie with one tussenstap, whose join runs over a fact type with both roles to-many. */
    private static final String GEMEENTEBERGING = "anchor/jutterswet-gemeenteberging";
    /** A §9.5 consistentie counting a to-one role ("het aantal houder van de Penning"). */
    private static final String PENNINGUITGIFTE = "anchor/jutterswet-penninguitgifte";

    // ---------------- 1. the §8.1.7 FAR-SIDE rolcheck ----------------

    @Test void de_verre_rolcheck_weigert_een_terugverwijzing_zonder_veld() throws Exception {
        // The twin of the near-side rolcheck gated 2026-09-10: "de Werf heeft een schuit" is an exists-join that
        // reads the FILLER's reference back at the subject. With BOTH roles of `ligplaats` to-many the bean model
        // derives no field at either end, so the join probes a getter nobody has. Measured on the pre-gate jar, on a
        // verifier-clean model:
        //     verifier: PASS
        //     ref     : EVALUATION FAULT: condition kind not implemented in this card
        //     clingo  : TRANSLATION FAULT: condition kind not translated (no corpus case exercises it)
        //     Drools exec-model : ERROR ConstraintEvaluationException: Error evaluating constraint ''
        //     Drools DRL-tekst  : EXCLUDE drl compile: Unable to Analyse Expression werf == $subject
        // ref and clingo carried NO far-side rolcheck at all then, so no golden of this shape could enter the
        // corpus and the differential square could not arbitrate this reader. What the gate buys is therefore the
        // DIAGNOSIS, not a value: an unclassified fault and a bare compiler message both become the one §13.3.9
        // sentence. Since L2 of the v1 wire migration the check is one node, RoleMembership, and where its link is
        // stored is one derivation (FactModelGenerator.lidmaatschapsopslag) with one sentence for both branches.
        Path ir = verreRolcheck(), c = geval(werfEnSchuit(false, "{}", "{ \"kind\": \"attributeEmpty\","
                + " \"label\": \"W1\", \"attribute\": \"wachtgeld\" }"));
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                () -> CaseRunner.speelCase(ir, JSON.readTree(Files.readString(c))));
        String zin = "rollidmaatschap (ligplaats, werf): beide rollen zijn meervoudig, dus geen bean draagt de link"
                + " (§13.3.9 opslagrichting)";
        assertTrue(e.getMessage().contains(zin), e.getMessage());
        assertTrue(drlReden(ir, c).contains(zin), drlReden(ir, c));
    }

    // ---------------- 2. the two fold joins outside the gated aggregation ----------------

    @Test void de_temporele_setsommatie_weigert_een_join_zonder_veld() throws Exception {
        // §5.8.2/§5.1.4 "de som van <temporeel attr> van alle <rol>" into a TEMPORAL doel resolves its OWN join,
        // apart from the aggregation gated 2026-08-30, so it asked nothing. With the element role to-many the field
        // lives at the far end and this end derives none. Measured on the pre-gate jar:
        //     verifier: PASS
        //     ref     : EVALUATION FAULT: temporal attribute "bewaarlast" as a target: temporal writes are not implemented
        //     clingo  : TRANSLATION FAULT: a temporal target takes only the ch. 3.7 pro-rata or ch. 3.8 day-count shape
        //     Drools exec-model : ERROR ConstraintEvaluationException
        //     Drools DRL-tekst  : EXCLUDE drl compile: Unable to Analyse Expression bewarendeJutter == $subject
        // Like the far-side rolcheck: no golden of this shape can exist (ref writes no temporal target), so the gate
        // is for the diagnosis. The refusal ALSO had to reach the caller's note — the temporal write arm printed the
        // generic Phase-F scope line and swallowed the reason until this card wired navReden through it.
        // Since L2 of the v1 wire migration the fold's join is its binding's, read by the one derivation every
        // fold seam shares (FactModelGenerator.bereikjoin), so the refusal is that derivation's sentence.
        Path ir = temporeleSetSommatie(), c = geval(tempovouwGeval());
        // the truth-maintenance branch refuses the sum before its join: it writes a timeline target only with a
        // time-proportional part or a day count
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                () -> CaseRunner.speelCase(ir, JSON.readTree(Files.readString(c)), DroolsBackend.Branch.TRUTH_MAINTENANCE));
        assertTrue(e.getMessage().contains("regel: bewaarlast van een jutter — een waarde van het tijdsafhankelijke doel"
                + " 'bewaarlast' van Jutter die geen tijdsevenredig deel en geen dagentelling is ('de som van het"
                + " bewaartarief van alle vondsten van de Jutter')"), e.getMessage());
        String zin = "aggregatie-bereik: de wire-binding wijst de rol 'vondst' aan, wiens zusterrol 'bewarende jutter'"
                + " MEERVOUDIG is";
        assertTrue(drlReden(ir, c).contains(zin), drlReden(ir, c));
    }

    // The §8.2 whole-period fold over a NAVIGATED SET (geheelPeriodeAlle) got no gate and no witness, and the reason
    // was measured rather than assumed: every arm of it entered on a `set` reading, and WF-096 (2026-09-09) admitted
    // a set reading ONLY as the source of a single-argument instance fold. Built anyway and pushed through
    // (2026-09-11, before M1; the verifier path read /rules/resolvedRegels/0/versions/0/condition/links since):
    //     verifier: WF-096 FAIL /resolvedRegels/0/voorwaarde/links: set reading at an ordinary read position
    //     ref     : EVALUATION FAULT: whole-period comparison is ch. 3 scope
    // The document a gate would protect was not well-formed, so the gate could carry no witness. Since L2 the arm
    // is gone with the set flag: a read over a to-many step is WF-053's, and neither branch lowers one.

    // ---------------- 3. the §8.2 navigated characteristic timeline ----------------

    @Test void de_genavigeerde_kenmerktijdreeks_weigert_het_verdrongen_veld() throws Exception {
        // The same two-step hop navReader walks, on the timeline arm and with its own resolution: `get(s, rol)` then
        // `get(rel, kenmerk)`. A displacing attribute makes the first step a BigDecimal and the second an
        // unclassified fault. Measured on the pre-gate jar, on a verifier-clean AND ref-clean model:
        //     verifier: PASS   ref: oracle: PASS (1 expectations)   clingo: clingo: PASS (1 expectations)
        //     Drools exec-model : ERROR ConstraintEvaluationException
        //     Drools DRL-tekst  : EXCLUDE drl compile: Unable to Analyse Expression rsGeheelPeriode(($subject.getWerf() …
        // Unlike the two readers above this one HAS an arbiter: ref and clingo both answer, and they leave the
        // toeslag leeg. Recorded beside it, and deliberately not fixed here: the UNDISPLACED shape of this same rule
        // diverges anyway (ref/clingo leeg, both Drools branches 8) — a §8.2 reading of the null relation as a
        // vacuously true whole period. That is a different question from which field the hop reads, and it belongs
        // to whoever takes the §8.2 null-relation semantics.
        Path ir = kenmerkTijdreeks(), c = geval(werfEnSchuit(true, "{}", "{ \"kind\": \"attributeEmpty\","
                + " \"label\": \"S1\", \"attribute\": \"toeslag\" }"));
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                () -> CaseRunner.speelCase(ir, JSON.readTree(Files.readString(c)), DroolsBackend.Branch.TRUTH_MAINTENANCE));
        assertTrue(e.getMessage().contains("regel: toeslag van een schuit — een CharacteristicCheck met wholePeriod ('de"
                + " werf van de Schuit gedurende het gehele jaar bedrijvig is'): Schuit heeft geen referentieveld 'werf'"
                + " naar Werf (§13.3.9 opslagrichting)"), e.getMessage());
        assertTrue(drlReden(ir, c).contains("genavigeerde kenmerktijdreeks"), drlReden(ir, c));
    }

    // ---------------- 4. the §13.4.10 verdeling's recipient join ----------------

    @Test void de_verdeling_weigert_een_ontvangerjoin_zonder_veld() throws Exception {
        // `join` comes from verbindendeRol(doel, bron) and is then read at SIX places (share, rest, ordering and the
        // §9.7.6 Fout companions) without anyone asking whether it is a field. With the recipient role to-many the
        // storage sits at the source end. Measured on the pre-gate jar, on a verifier-clean AND ref-clean model:
        //     verifier: PASS   ref: oracle: PASS (2 expectations)   clingo: clingo: PASS (2 expectations)
        //     Drools exec-model : ERROR ConstraintEvaluationException
        //     Drools DRL-tekst  : EXCLUDE drl compile: Unable to Analyse Expression berging == $src
        // The name-DISPLACED half of the same limit was measured too and is NOT reachable with a live relation:
        // CaseRunner.naamdelingsPoort refuses to write the link at all ("role 'bergers' resolves to the to-one role
        // 'berging' on Berger, which shares its name with a declared member"), so the allocation then finds no
        // recipients — which is what ref computes for a relation that is not there. Both halves ask one question.
        Path ir = verdelingZonderVeld(), c = geval(bergingGeval());
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                () -> CaseRunner.speelCase(ir, JSON.readTree(Files.readString(c)), DroolsBackend.Branch.TRUTH_MAINTENANCE));
        assertTrue(e.getMessage().contains("verdeling: verdeel de opbrengst van een berging — een verdeling over de"
                + " enkelvoudige ontvangerrol 'berger' wiens zusterrol 'berging' meervoudig is: dat feittype heeft één"
                + " beanveld voor beide richtingen"), e.getMessage());
        assertTrue(drlReden(ir, c).contains("verdeling"), drlReden(ir, c));
    }

    // ---------------- 5. the §13.4.7 feitcreatie's three field positions ----------------

    @Test void de_feitcreatie_met_de_naamdelende_schrijfrol_speelt_met_truth_maintenance() throws Exception {
        // The written field is WRITTEN in the consequence: modify($doel){ set<field>($subject) }. A declared member
        // of that name makes the field a BigDecimal and the write a cast fault — the objectcreatie arm's exact
        // failure, on the arm that was never gated. Measured on the pre-gate jar, on a verifier-clean AND ref-clean
        // model:
        //     verifier: PASS   ref: oracle: PASS (2 expectations)   clingo: clingo: PASS (2 expectations)
        //     Drools exec-model : ERROR ConsequenceException … ClassCastException: Cannot cast …facts.Kustvak to
        //                         java.math.BigDecimal   (rule "vakbeseining door het kustvak")
        //     Drools DRL-tekst  : EXCLUDE drl compile: Unable to Analyse Expression $doel.setSeinvak( $subject )
        Path ir = feitcreatieMetVerdringer("Sein", "seinvak"), c = geval(postwachtGeval());
        // The destructive exec-model branch refused the creation by name once it was gated. The truth-maintenance
        // branch lays the link as a fact while it fires and answers as ref on this case (TmsZoalsRefWitnessTest,
        // feitcreatie-naamdelende-schrijfrol). The DRL-text branch still refuses.
        assertNull(CaseRunner.speelCase(ir, JSON.readTree(Files.readString(c)), DroolsBackend.Branch.TRUTH_MAINTENANCE),
                "tms — ref agrees");
        assertTrue(drlReden(ir, c).contains("feitcreatie"), drlReden(ir, c));
    }

    @Test void de_feitcreatie_weigert_een_join_zonder_veld() throws Exception {
        // The gemeenteberging creation, reduced to that rule. Until P2 (2026-09-22) this row pinned the refusal on
        // the HOP: the wire's viaOuderVeld put the hop's field `gemeente van inschrijving` on the anker Gemeente,
        // whose bean carries no such getter. Measured then, on a verifier-clean AND ref-clean model:
        //     verifier: PASS   ref: oracle: PASS (1 expectations)   clingo: clingo: PASS (1 expectations)
        //     Drools exec-model : ERROR ConstraintEvaluationException
        //     Drools DRL-tekst  : EXCLUDE drl compile: Unable to Analyse Expression this == $subject.gemeenteVanInschrijving
        // Since P2 the hop is derived from its binding: `ingeschreven jutter` is to-many, so the field is Jutter's
        // own `gemeente van inschrijving`, which exists — the side ref walks. The creation now stops one position
        // on, at the join over `aanmelding`, whose roles are both to-many, so no bean carries the join field. The
        // hop's own gate keeps a row of its own below (de_feitcreatie_weigert_een_verdrongen_tussenstapveld).
        Path ir = feitcreatieTussenstap(), c = geval(gemeenteGevalZonderRollen());
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                () -> CaseRunner.speelCase(ir, JSON.readTree(Files.readString(c)), DroolsBackend.Branch.TRUTH_MAINTENANCE));
        assertTrue(e.getMessage().contains("feitcreatie: berging door de kustgemeente — het pad van de feitcreatie"
                + " 'berging door de kustgemeente': de stap over 'aanmelding' heeft twee rollen naar veel, waarvoor het"
                + " beanmodel geen veld heeft (§13.3.9 opslagrichting)"), e.getMessage());
        assertTrue(drlReden(ir, c).contains("feitcreatie"), drlReden(ir, c));
    }

    @Test void de_feitcreatie_weigert_een_verdrongen_tussenstapveld() throws Exception {
        // The hop's gate, the position the row above no longer reaches. The same reduced gemeenteberging with a
        // declared Jutter attribute `gemeente van inschrijving`: that member displaces the hop's reference field
        // (leden(): attribuut > kenmerk > referentie), so the creation must stop AT THE HOP, before the join.
        // Verifier-clean and ref-clean. With the hop gate removed from both branches the suite stayed green
        // (lens, 2026-09-22): the creation then refused only at the join, which is the row above.
        ObjectNode m = goldenIr(GEMEENTEBERGING);
        houdRegels(m, Set.of("berging door de kustgemeente"));
        IrPad.addAttribute(IrPad.objectType(m, "Jutter"), "gemeente van inschrijving", numeriek());
        Path ir = model("feitcreatie-verdrongen-hop", m), c = geval(gemeenteGevalZonderRollen());
        String reden = "de rol 'gemeente van inschrijving' op Jutter deelt haar naam met een gedeclareerd lid";
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                () -> CaseRunner.speelCase(ir, JSON.readTree(Files.readString(c)), DroolsBackend.Branch.TRUTH_MAINTENANCE));
        assertTrue(e.getMessage().contains("feitcreatie: berging door de kustgemeente — het pad van de feitcreatie"
                + " 'berging door de kustgemeente': " + reden), e.getMessage());
        assertTrue(drlReden(ir, c).contains("feitcreatie: " + reden), drlReden(ir, c));
    }

    @Test void de_ongestoorde_feitcreatie_speelt_nog() throws Exception {
        // The over-refusal flank: three new questions on one construct could refuse a creation whose fields are all
        // there. The same postwacht model with a declaring attribute the roles do NOT share still plays, in BOTH
        // branches — the gate fires on the collision, not on the shape.
        Path ir = feitcreatieMetVerdringer("Sein", "ongebruikt"), c = geval(postwachtGeval());
        assertNull(CaseRunner.speelCase(ir, JSON.readTree(Files.readString(c))), "de creatie draagt haar velden nog");
        assertEquals("PLAYED", CaseRunner.viaDrlTekst(ir, List.of(c)).status());
    }

    // ---------------- 6. the §9.5 presence count: one derivation, one question ----------------

    @Test void de_aantalassertie_leest_de_opgeloste_rol_niet_de_geschreven_naam() throws Exception {
        // toOneAantalReader tested the name-sharing itself — `subject.heeftLid(a.attribuut())` — instead of asking
        // the shared question. That is a second reading of leden()'s law, and it reads the WRONG NAME whenever the
        // written element name and the resolved role name differ. They differ in the ordinary case: the AANTAL's
        // argument NAMES the element role as written ("het aantal penningen"), which resolves through the role's
        // meervoud to the field `penning`. A Jutter declaring a Numeriek `penning` therefore passed the copy (it
        // declares no `penningen`) and the reader read the attribute's value as a filled reference.
        //
        // RED FIRST — measured on the pre-card jar, on a verifier-clean AND ref-clean model (penningbezit clipped to
        // 1:1, the consistentie "het aantal penningen van de Jutter is groter of gelijk aan 1", no link supplied,
        // the displacing attribute holding 5):
        //     verifier: PASS
        //     ref     : oracle: PASS (1 expectations)      — J1 holds no penning, so the consistentie is VIOLATED
        //     clingo  : clingo: PASS (1 expectations)      — idem
        //     Drools exec-model : FAIL verwacht inconsistent (penningtelcontrole), werkelijk consistent
        // A VALUE divergence, not a fault: the backend reported a violated consistentie as satisfied. The DRL branch
        // never lowered this consistentie at all ("onopgelost consistentieregel"), which is why it is not asserted
        // here — measured, and unchanged by this card.
        Path ir = aantalNaamdrift(), c = geval(penningGeval());
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                () -> CaseRunner.speelCase(ir, JSON.readTree(Files.readString(c))));
        assertTrue(e.getMessage().contains("penningtelcontrole"), e.getMessage());
        assertTrue(e.getMessage().contains("de rol 'penning' op Jutter deelt haar naam met een gedeclareerd lid"),
                e.getMessage());
        assertTrue(e.getMessage().contains("§13.3.9 opslagrichting"), e.getMessage());
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
        return JSON.createObjectNode().put("node", "Number").put("whole", true)
                .put("decimals", 0).put("sign", "unspecified");
    }

    /** Reduce a model to the named rules in the resolved list. */
    private static void houdRegels(ObjectNode ir, Set<String> namen) {
        ArrayNode houd = JSON.createArrayNode();
        for (JsonNode r : IrPad.rulesLijst(ir, "rules"))
            if (namen.contains(r.path("name").asText())) houd.add(r);
        IrPad.rules(ir).set("rules", houd);
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
        return tijdelijk("rolveldlezer-case", caseJson);
    }

    /** The DRL-TEXT branch's verdict on the same pair — the harness entry, never a gate path. */
    private static String drlReden(Path ir, Path c) {
        CaseRunner.Result r = CaseRunner.viaDrlTekst(ir, List.of(c));
        assertEquals("EXCLUDE", r.status(), "de DRL-tak weigert de vorm ook: " + r.reason());
        return r.reason();
    }

    /** synth-navigatiepremie with BOTH ligplaats roles to-many (no field at either end) and a §8.1.7 far-side check
     *  "de Werf heeft een schuit" gating a Werf rule. Verifier-clean; ref and clingo carry no such condition. */
    private static Path verreRolcheck() throws Exception {
        ObjectNode ir = goldenIr(NAVIGATIEPREMIE);
        IrPad.addAttribute(IrPad.objectType(ir, "Werf"), "wachtgeld", numeriek());
        for (JsonNode r : IrPad.declaratie(ir, "factTypes", "ligplaats").get("roles"))
            if ("werf".equals(r.get("name").asText())) {
                ((ObjectNode) r).put("many", true);
                ((ObjectNode) r).put("plural", "werven");
            }
        ObjectNode vorm = IrPad.rules(ir).putArray("rules").addObject();
        vorm.put("node", "Rule").put("name", "wachtgeld van een werf");
        ObjectNode regel = vorm.putArray("versions").addObject();
        regel.putObject("validity").put("node", "Validity");
        ObjectNode doel = regel.putObject("target");
        doel.put("node", "AttributeReference").put("attribute", "wachtgeld").put("subjectType", "Werf")
            .set("source", IrPad.bronrecord("Het wachtgeld van een Werf"));
        ObjectNode waarde = regel.putObject("value");
        waarde.put("node", "Literal").putObject("value").put("number", "8");
        waarde.set("type", numeriek());
        waarde.set("source", IrPad.bronrecord("8"));
        // "de Werf heeft een schuit": the subject Werf fills `werf` in ligplaats
        regel.putObject("condition").put("node", "RoleMembership").put("factType", "ligplaats")
             .put("role", "werf").put("negated", false).set("source", IrPad.bronrecord("de Werf een schuit heeft"));
        regel.put("initialisation", false).put("presentation", "equation");
        return model("verre-rolcheck-zonder-veld", ir);
    }

    /** synth-navigatiepremie with a temporal kenmerk on Werf, its `toeslag` rule's voorwaarde replaced by the §8.2
     *  navigated characteristic check, and a Numeriek attribute displacing Schuit's `werf` reference.
     *  Verifier-clean and ref-clean (ref leaves toeslag leeg). */
    private static Path kenmerkTijdreeks() throws Exception {
        ObjectNode ir = goldenIr(NAVIGATIEPREMIE);
        ObjectNode bedrijvig = IrPad.objectType(ir, "Werf").putArray("characteristics").addObject();
        bedrijvig.put("node", "Characteristic").put("name", "bedrijvig").put("kind", "adjectival");
        IrPad.timeline(bedrijvig, "month");
        IrPad.addAttribute(IrPad.objectType(ir, "Schuit"), "werf", numeriek());
        houdRegels(ir, Set.of("toeslag van een schuit"));
        ObjectNode regel = IrPad.versie(ir, "rules", 0, 0);
        regel.put("initialisation", false).put("presentation", "equation");
        ObjectNode eigen = JSON.createObjectNode();
        eigen.put("node", "AttributeReference").put("attribute", "lengte").put("subjectType", "Schuit")
             .set("source", IrPad.bronrecord("de lengte van de Schuit"));
        regel.set("value", eigen);
        ObjectNode check = JSON.createObjectNode();
        check.put("node", "CharacteristicCheck").put("characteristic", "bedrijvig")
             .put("negated", false);
        check.putObject("wholePeriod").put("node", "WholePeriod").put("granularity", "year").put("negated", false);
        check.set("source", IrPad.bronrecord("de werf van de Schuit gedurende het gehele jaar bedrijvig is"));
        check.putObject("navigation").put("node", "RoleBinding").put("factType", "ligplaats").put("role", "werf");
        regel.set("condition", check);
        return model("kenmerktijdreeks-naamdeling", ir);
    }

    /** One Werf (tarief 312) and one Schuit (lengte 8), the Schuit optionally filling the DISPLACING attribute. */
    private static String werfEnSchuit(boolean verdringerGevuld, String schuitRollen, String verwacht) {
        return """
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "W1", "objectType": "Werf", "attributes": { "tarief": { "number": "312" } },
                        "roles": {}, "characteristics": [] },
                      { "label": "S1", "objectType": "Schuit", "attributes": { "lengte": { "number": "8" }%s },
                        "roles": %s, "characteristics": [] } ],
                    "parameters": {} },
                  "expected": [ %s ] }
                """.formatted(verdringerGevuld ? ", \"werf\": { \"number\": \"5\" }" : "", schuitRollen, verwacht);
    }

    /** jutterswet-tempovouw with the doel made TEMPORAL (MAAND, the element's own type) and the fold a §5.8.2 SOM,
     *  plus the `bewaring` roles flipped so the element role is to-many. Verifier-clean; ref refuses temporal
     *  targets, so this shape has no corpus home. */
    private static Path temporeleSetSommatie() throws Exception {
        ObjectNode ir = goldenIr(TEMPOVOUW);
        JsonNode elemType = IrPad.attribute(IrPad.objectType(ir, "Vondst"), "bewaartarief").get("datatype");
        ObjectNode bewaarlast = IrPad.attribute(IrPad.objectType(ir, "Jutter"), "bewaarlast");
        bewaarlast.set("datatype", elemType.deepCopy());
        IrPad.timeline(bewaarlast, "month");
        for (JsonNode r : IrPad.declaratie(ir, "factTypes", "bewaring").get("roles")) {
            ObjectNode rol = (ObjectNode) r;
            if ("bewarende jutter".equals(rol.get("name").asText())) {
                rol.put("many", true); rol.put("plural", "bewarende jutters");
            } else if ("vondst".equals(rol.get("name").asText())) {
                rol.put("many", false);
            }
        }
        ObjectNode regel = IrPad.versie(ir, "rules", 0, 0);
        // the target's datatype is bewaarlast's declaration above; only the fold carries an annotation of its own
        ObjectNode fold = (ObjectNode) regel.get("value");
        fold.put("kind", "sum").set("source", IrPad.bronrecord("de som van het bewaartarief van alle vondsten van de Jutter"));
        fold.set("type", elemType.deepCopy());
        return model("temporele-setsommatie-zonder-veld", ir);
    }

    /** Two Vondsten with a bewaartarief timeline and a Jutter, no roles — the fold has nothing to find, so the
     *  refusal can only come from the lowering. */
    private static String tempovouwGeval() {
        return """
                { "format": "uchar-case-v0", "asOfDate": "2026-03-15",
                  "input": { "instances": [
                      { "label": "J1", "objectType": "Jutter", "attributes": {}, "roles": {}, "characteristics": [] },
                      { "label": "V1", "objectType": "Vondst", "attributes": { "bewaartarief": { "timeline": [
                            { "from": "2026-01-01", "to": "2026-04-01", "value": { "number": "12" } } ] } },
                        "roles": {}, "characteristics": [] } ],
                    "parameters": {} },
                  "expected": [ { "kind": "consistent" } ] }
                """;
    }

    /** jutterswet-berging with the bergingsdeelname roles flipped: the recipient role becomes to-many, so the single
     *  bean field sits at the SOURCE end and Berger derives none. Verifier-clean and ref-clean. */
    private static Path verdelingZonderVeld() throws Exception {
        ObjectNode ir = goldenIr(BERGING);
        for (JsonNode r : IrPad.declaratie(ir, "factTypes", "bergingsdeelname").get("roles")) {
            ObjectNode rol = (ObjectNode) r;
            if ("berging".equals(rol.get("name").asText())) {
                rol.put("many", true); rol.put("plural", "bergingen");
            } else {
                rol.put("many", false);
            }
        }
        return model("verdeling-join-zonder-veld", ir);
    }

    /** One Berging (opbrengst 1200) naming its single Berger, who carries a weight and a cap. */
    private static String bergingGeval() {
        return """
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "B1", "objectType": "Berging", "attributes": { "opbrengst": { "number": "1200" } },
                        "roles": { "berger": ["W"] }, "characteristics": [] },
                      { "label": "W", "objectType": "Berger",
                        "attributes": { "geborgen gewicht": { "number": "3.00" },
                                        "bergingsmaximum": { "number": "300" } },
                        "roles": {}, "characteristics": [] } ],
                    "parameters": {} },
                  "expected": [
                      { "kind": "attributeValue", "label": "W", "attribute": "bergingsaandeel", "value": { "number": "300" } },
                      { "kind": "consistent" } ] }
                """;
    }

    /** jutterswet-postwacht reduced to its feitcreatie, with a Numeriek attribute of the given name on the given
     *  objecttype. Verifier-clean and ref-clean in both spellings. */
    private static Path feitcreatieMetVerdringer(String objecttype, String lid) throws Exception {
        ObjectNode ir = goldenIr(POSTWACHT);
        IrPad.addAttribute(IrPad.objectType(ir, objecttype), lid, numeriek());
        houdRegels(ir, Set.of("vakbeseining door het kustvak"));
        IrPad.rules(ir).putArray("uniquenesses");
        return model("feitcreatie-" + lid, ir);
    }

    /** The postwacht case, expectations reduced to an input echo (the computing rules are stripped above). */
    private static String postwachtGeval() {
        return """
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "V1", "objectType": "Kustvak", "attributes": { "gevarenklasse": { "number": "7" } },
                        "roles": { "posten": ["P1"] }, "characteristics": [] },
                      { "label": "P1", "objectType": "Post", "attributes": { "hoogte": { "number": "15" } },
                        "roles": { "kustvak": ["V1"], "seinen": ["S1"] }, "characteristics": [] },
                      { "label": "S1", "objectType": "Sein", "attributes": { "gewicht": { "number": "4" } },
                        "roles": {}, "characteristics": [] } ],
                    "parameters": {} },
                  "expected": [
                      { "kind": "attributeValue", "label": "V1", "attribute": "gevarenklasse", "value": { "number": "7" } },
                      { "kind": "consistent" } ] }
                """;
    }

    /** anchor/jutterswet-gemeenteberging reduced to its feitcreatie — the fold that refuses first is dropped, so the
     *  creation's own path is the position reached. Verifier-clean and ref-clean. */
    private static Path feitcreatieTussenstap() throws Exception {
        ObjectNode ir = goldenIr(GEMEENTEBERGING);
        houdRegels(ir, Set.of("berging door de kustgemeente"));
        return model("feitcreatie-tussenstap", ir);
    }

    /** A Gemeente, a Jutter and a Vondst with NO roles: the hop's predicate is evaluated per (Gemeente, Jutter)
     *  pair regardless of any relation, which is exactly why an absent field faults without one. */
    private static String gemeenteGevalZonderRollen() {
        return """
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "G1", "objectType": "Gemeente", "attributes": {}, "roles": {},
                        "characteristics": ["kustgemeente"] },
                      { "label": "J1", "objectType": "Jutter", "attributes": {}, "roles": {}, "characteristics": [] },
                      { "label": "V1", "objectType": "Vondst", "attributes": { "gewicht": { "number": "12" } },
                        "roles": {}, "characteristics": [] } ],
                    "parameters": {} },
                  "expected": [ { "kind": "consistent" } ] }
                """;
    }

    /** anchor/jutterswet-penninguitgifte clipped to a 1:1 penningbezit, its consistentie rewired to count the
     *  element role BY ITS WRITTEN PLURAL ("het aantal penningen"), and a Numeriek `penning` on Jutter displacing
     *  the field that count reads. Verifier-clean and ref-clean (ref reports the violation). */
    private static Path aantalNaamdrift() throws Exception {
        ObjectNode ir = goldenIr(PENNINGUITGIFTE);
        for (JsonNode r : IrPad.declaratie(ir, "factTypes", "penningbezit").get("roles"))
            if ("penning".equals(r.get("name").asText())) ((ObjectNode) r).put("many", false);
        IrPad.addAttribute(IrPad.objectType(ir, "Jutter"), "penning", numeriek());
        ObjectNode rules = IrPad.rules(ir);
        rules.putArray("objectCreations"); rules.putArray("rules"); rules.putArray("ruleGroups");
        rules.putArray("uniquenesses");
        ObjectNode arg = JSON.createObjectNode();
        // the count source: its written name is the element role's declared plural, read at the fold subject
        arg.put("node", "AttributeReference").put("attribute", "penningen").put("subjectType", "Jutter")
           .set("source", IrPad.bronrecord("penningen van de Jutter"));
        ObjectNode aantal = JSON.createObjectNode();
        aantal.put("node", "Aggregation").put("kind", "count");
        aantal.putArray("arguments").add(arg);
        aantal.put("elementType", "Penning");
        aantal.putObject("sourceRole").put("node", "RoleBinding").put("factType", "penningbezit").put("role", "penning");
        aantal.set("type", numeriek().put("sign", "nonNegative"));
        aantal.set("source", IrPad.bronrecord("het aantal penningen van de Jutter"));
        ObjectNode rechts = JSON.createObjectNode();
        rechts.put("node", "Literal").putObject("value").put("number", "1");
        rechts.set("type", numeriek());
        rechts.set("source", IrPad.bronrecord("1"));
        ObjectNode assertie = JSON.createObjectNode();
        assertie.put("node", "Comparison").put("op", "greaterOrEqual");
        assertie.set("left", aantal);
        assertie.set("right", rechts);
        assertie
                .set("source", IrPad.bronrecord("het aantal penningen van de Jutter is groter of gelijk aan 1"));
        ObjectNode vorm = rules.putArray("consistencies").addObject();
        vorm.put("node", "Consistency").put("name", "penningtelcontrole");
        ObjectNode con = vorm.putArray("versions").addObject();
        con.put("subject", "Jutter");
        con.set("assertion", assertie);
        con.putObject("validity").put("node", "Validity");
        con.set("source", IrPad.bronrecord("Er moet worden voldaan aan: elke Jutter heeft een penning").put("line", 1));
        return model("aantal-naamdrift", ir);
    }

    /** One Jutter filling the DISPLACING attribute and holding no penning at all — ref reports the violation. */
    private static String penningGeval() {
        return """
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "J1", "objectType": "Jutter", "attributes": { "penning": { "number": "5" } },
                        "roles": {}, "characteristics": [] } ],
                    "parameters": {} },
                  "expected": [ { "kind": "inconsistent", "rule": "penningtelcontrole" } ] }
                """;
    }
}
