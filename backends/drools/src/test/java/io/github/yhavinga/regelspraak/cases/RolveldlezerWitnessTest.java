package io.github.yhavinga.regelspraak.cases;

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
    /** A §13.4.7 feitcreatie whose tussenstap reads the ANKER's own field (viaOuderVeld) — a field Gemeente lacks. */
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
        // ref and clingo carry NO far-side rolcheck at all, so no golden of this shape can enter the corpus and the
        // differential square can never arbitrate this reader. What the gate buys is therefore the DIAGNOSIS, not a
        // value: an unclassified fault and a bare compiler message both become the one §13.3.9 sentence.
        Path ir = verreRolcheck(), c = geval(werfEnSchuit(false, "{}", "{ \"kind\": \"attributeEmpty\","
                + " \"label\": \"W1\", \"attribute\": \"wachtgeld\" }"));
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                () -> CaseRunner.speelCase(ir, JSON.readTree(Files.readString(c))));
        assertTrue(e.getMessage().contains("verre rollidmaatschapscheck"), e.getMessage());
        assertTrue(e.getMessage().contains("het beanmodel leidt voor de rol 'werf' op Schuit geen referentieveld"
                + " naar Werf af"), e.getMessage());
        assertTrue(e.getMessage().contains("§13.3.9 opslagrichting"), e.getMessage());
        assertTrue(drlReden(ir, c).contains("verre rollidmaatschapscheck"), drlReden(ir, c));
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
        Path ir = temporeleSetSommatie(), c = geval(tempovouwGeval());
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                () -> CaseRunner.speelCase(ir, JSON.readTree(Files.readString(c))));
        assertTrue(e.getMessage().contains("temporele set-sommatie"), e.getMessage());
        assertTrue(e.getMessage().contains("het beanmodel leidt voor de rol 'bewarende jutter' op Vondst geen"
                + " referentieveld naar Jutter af"), e.getMessage());
        assertTrue(e.getMessage().contains("§13.3.9 opslagrichting"), e.getMessage());
        assertTrue(drlReden(ir, c).contains("temporele set-sommatie"), drlReden(ir, c));
    }

    // The §8.2 whole-period fold over a NAVIGATED SET (geheelPeriodeAlle) gets no gate and no witness, and the reason
    // is measured rather than assumed: every arm of it enters on a `set` reading, and WF-096 (2026-09-09) admits a
    // set reading ONLY as the source of a single-argument instance fold. Built anyway and pushed through:
    //     verifier: WF-096 FAIL /resolvedRegels/0/voorwaarde/links: set reading at an ordinary read position
    //     ref     : EVALUATION FAULT: whole-period comparison is ch. 3 scope
    // The document a gate would protect is not well-formed, so the gate could carry no witness — the one thing this
    // repo does not admit. Both branches carry that sentence at the join instead.

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
                () -> CaseRunner.speelCase(ir, JSON.readTree(Files.readString(c))));
        assertTrue(e.getMessage().contains("genavigeerde kenmerktijdreeks"), e.getMessage());
        assertTrue(e.getMessage().contains("de rol 'werf' op Schuit deelt haar naam met een gedeclareerd lid"),
                e.getMessage());
        assertTrue(e.getMessage().contains("§13.3.9 opslagrichting"), e.getMessage());
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
                () -> CaseRunner.speelCase(ir, JSON.readTree(Files.readString(c))));
        assertTrue(e.getMessage().contains("verdeling ("), e.getMessage());
        assertTrue(e.getMessage().contains("het beanmodel leidt voor de rol 'berging' op Berger geen referentieveld"
                + " naar Berging af"), e.getMessage());
        assertTrue(drlReden(ir, c).contains("verdeling"), drlReden(ir, c));
    }

    // ---------------- 5. the §13.4.7 feitcreatie's three field positions ----------------

    @Test void de_feitcreatie_weigert_de_naamdelende_schrijfrol() throws Exception {
        // zetVeld is WRITTEN in the consequence: modify($doel){ set<zetVeld>($subject) }. A declared member of that
        // name makes the field a BigDecimal and the write a cast fault — the objectcreatie arm's exact failure, on
        // the arm that was never gated. Measured on the pre-gate jar, on a verifier-clean AND ref-clean model:
        //     verifier: PASS   ref: oracle: PASS (2 expectations)   clingo: clingo: PASS (2 expectations)
        //     Drools exec-model : ERROR ConsequenceException … ClassCastException: Cannot cast …facts.Kustvak to
        //                         java.math.BigDecimal   (rule "vakbeseining door het kustvak")
        //     Drools DRL-tekst  : EXCLUDE drl compile: Unable to Analyse Expression $doel.setSeinvak( $subject )
        Path ir = feitcreatieMetVerdringer("Sein", "seinvak"), c = geval(postwachtGeval());
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                () -> CaseRunner.speelCase(ir, JSON.readTree(Files.readString(c))));
        assertTrue(e.getMessage().contains("feitcreatie ("), e.getMessage());
        assertTrue(e.getMessage().contains("de rol 'seinvak' op Sein deelt haar naam met een gedeclareerd lid"),
                e.getMessage());
        assertTrue(drlReden(ir, c).contains("feitcreatie"), drlReden(ir, c));
    }

    @Test void de_feitcreatie_weigert_een_tussenstap_zonder_veld() throws Exception {
        // The §9.4 multi-hop path reads a field per step — on the PARENT for a viaOuderVeld hop. The gemeenteberging
        // creation walks Gemeente -> Jutter that way, over `gemeente van inschrijving`, which is a field of JUTTER:
        // the anker's bean carries no such getter at all. The golden itself is excluded earlier (its fold refuses
        // first), so the hop is reached only with that rule stripped and the case's roles cleared. Measured on the
        // pre-gate jar, on a verifier-clean AND ref-clean model:
        //     verifier: PASS   ref: oracle: PASS (1 expectations)   clingo: clingo: PASS (1 expectations)
        //     Drools exec-model : ERROR ConstraintEvaluationException
        //     Drools DRL-tekst  : EXCLUDE drl compile: Unable to Analyse Expression this == $subject.gemeenteVanInschrijving
        // Note WHICH half of the limit this is: nothing is displaced here — the model simply derives no field for
        // that role on that type. The shared question covers both halves, which is why one gate suffices.
        Path ir = feitcreatieTussenstap(), c = geval(gemeenteGevalZonderRollen());
        CaseRunner.Excluded e = assertThrows(CaseRunner.Excluded.class,
                () -> CaseRunner.speelCase(ir, JSON.readTree(Files.readString(c))));
        assertTrue(e.getMessage().contains("feitcreatie ("), e.getMessage());
        // Since P2 the hop is derived from its binding, not from the wire's viaOuderVeld: the field is Jutter's own
        // `gemeente van inschrijving`, which exists, and the creation stops one position on, at the join over
        // `aanmelding`, whose roles are both to-many — the same shared gate, with its own reason.
        assertTrue(e.getMessage().contains("het beanmodel leidt voor de rol 'aanmelder' op Vondst"
                + " geen referentieveld naar Jutter af"), e.getMessage());
        assertTrue(drlReden(ir, c).contains("feitcreatie"), drlReden(ir, c));
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
        return JSON.createObjectNode().put("node", "Numeriek").put("geheel", true)
                .put("decimalen", 0).put("teken", "ONBEPAALD").putNull("eenheid");
    }

    /** Reduce a model to the named rules, in both the declaration list and the resolved list. */
    private static void houdRegels(ObjectNode ir, Set<String> namen) {
        for (String veld : new String[] { "regels", "resolvedRegels" }) {
            ArrayNode houd = JSON.createArrayNode();
            for (JsonNode r : ir.get(veld)) if (namen.contains(r.get("naam").asText())) houd.add(r);
            ir.set(veld, houd);
        }
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
        ((ObjectNode) ir.get("objecttypen").get("Werf").get("attributen")).set("wachtgeld", numeriek());
        for (JsonNode r : ir.get("feittypen").get("ligplaats").get("rollen"))
            if ("werf".equals(r.get("naam").asText())) {
                ((ObjectNode) r).put("meervoudig", true);
                ((ObjectNode) r).put("meervoud", "werven");
            }
        ir.putArray("regels").addObject()
                .put("node", "Regel").put("naam", "wachtgeld van een werf").put("soort", "regel");
        ObjectNode regel = ir.putArray("resolvedRegels").addObject();
        regel.put("node", "ResolvedRegel").put("naam", "wachtgeld van een werf").put("soort", "GELIJKSTELLING");
        ObjectNode doel = regel.putObject("doel");
        doel.put("node", "AttribuutRef").put("attribuut", "wachtgeld").put("objecttype", "Werf")
            .put("onderwerpBron", "een Werf").putNull("navigatieRol");
        doel.set("type", numeriek());
        doel.put("bron", "Het wachtgeld van een Werf").put("set", false);
        doel.putArray("navigatiePad");
        ObjectNode waarde = regel.putObject("waarde");
        waarde.put("node", "Literal").put("waarde", "8");
        waarde.set("type", numeriek());
        waarde.put("bron", "8");
        regel.putObject("voorwaarde").put("node", "VerreRollidmaatschap").put("filler", "Schuit")
             .put("backref", "werf").put("negatie", false).put("bron", "de Werf een schuit heeft");
        regel.putObject("geldigheid").put("node", "Geldigheid").putNull("vanaf").putNull("tot");
        return model("verre-rolcheck-zonder-veld", ir);
    }

    /** synth-navigatiepremie with a temporal kenmerk on Werf, its `toeslag` rule's voorwaarde replaced by the §8.2
     *  navigated characteristic check, and a Numeriek attribute displacing Schuit's `werf` reference.
     *  Verifier-clean and ref-clean (ref leaves toeslag leeg). */
    private static Path kenmerkTijdreeks() throws Exception {
        ObjectNode ir = goldenIr(NAVIGATIEPREMIE);
        ObjectNode werf = (ObjectNode) ir.get("objecttypen").get("Werf");
        werf.putArray("kenmerken").add("bedrijvig");
        werf.putObject("kenmerkSoorten").put("bedrijvig", "BIJVOEGLIJK");
        werf.putObject("kenmerkGranulariteit").put("bedrijvig", "MAAND");
        ((ObjectNode) ir.get("objecttypen").get("Schuit").get("attributen")).set("werf", numeriek());
        houdRegels(ir, Set.of("toeslag van een schuit"));
        ObjectNode regel = (ObjectNode) ir.get("resolvedRegels").get(0);
        regel.put("soort", "GELIJKSTELLING");
        ObjectNode eigen = JSON.createObjectNode();
        eigen.put("node", "AttribuutRef").put("attribuut", "lengte").put("objecttype", "Schuit")
             .put("onderwerpBron", "de Schuit").putNull("navigatieRol");
        eigen.set("type", numeriek());
        eigen.put("bron", "de lengte van de Schuit").put("set", false);
        eigen.putArray("navigatiePad");
        regel.set("waarde", eigen);
        ObjectNode check = JSON.createObjectNode();
        check.put("node", "Kenmerk").put("kenmerk", "bedrijvig").put("navigatie", "werf")
             .put("negatie", false).putNull("eigenaar");
        check.putObject("periode").put("node", "GeheelPeriode").put("jaar", true).put("niet", false);
        check.put("bron", "de werf van de Schuit gedurende het gehele jaar bedrijvig is");
        check.putObject("navigatieBinding").put("node", "RolBinding").put("feittype", "ligplaats").put("rol", "werf");
        regel.set("voorwaarde", check);
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
        JsonNode elemType = ir.get("objecttypen").get("Vondst").get("attributen").get("bewaartarief");
        ObjectNode jutter = (ObjectNode) ir.get("objecttypen").get("Jutter");
        ((ObjectNode) jutter.get("attributen")).set("bewaarlast", elemType.deepCopy());
        jutter.putObject("attribuutGranulariteit").put("bewaarlast", "MAAND");
        for (JsonNode r : ir.get("feittypen").get("bewaring").get("rollen")) {
            ObjectNode rol = (ObjectNode) r;
            if ("bewarende jutter".equals(rol.get("naam").asText())) {
                rol.put("meervoudig", true); rol.put("meervoud", "bewarende jutters");
            } else if ("vondst".equals(rol.get("naam").asText())) {
                rol.put("meervoudig", false); rol.putNull("meervoud");
            }
        }
        ObjectNode regel = (ObjectNode) ir.get("resolvedRegels").get(0);
        ((ObjectNode) regel.get("doel")).set("type", elemType.deepCopy());
        ObjectNode fold = (ObjectNode) regel.get("waarde");
        fold.put("kind", "SOM").put("bron", "de som van het bewaartarief van alle vondsten van de Jutter");
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
        for (JsonNode r : ir.get("feittypen").get("bergingsdeelname").get("rollen")) {
            ObjectNode rol = (ObjectNode) r;
            if ("berging".equals(rol.get("naam").asText())) {
                rol.put("meervoudig", true); rol.put("meervoud", "bergingen");
            } else {
                rol.put("meervoudig", false); rol.putNull("meervoud");
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
        ((ObjectNode) ir.get("objecttypen").get(objecttype).get("attributen")).set(lid, numeriek());
        houdRegels(ir, Set.of("vakbeseining door het kustvak"));
        ir.putArray("uniciteiten");
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
     *  viaOuderVeld tussenstap is the position reached. Verifier-clean and ref-clean. */
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
        for (JsonNode r : ir.get("feittypen").get("penningbezit").get("rollen"))
            if ("penning".equals(r.get("naam").asText())) ((ObjectNode) r).put("meervoudig", false);
        ((ObjectNode) ir.get("objecttypen").get("Jutter").get("attributen")).set("penning", numeriek());
        ir.putArray("objectCreaties"); ir.putArray("resolvedRegels"); ir.putArray("regelGroepen");
        ir.putArray("uniciteiten");
        ir.putArray("regels").addObject()
                .put("node", "Regel").put("naam", "penningtelcontrole").put("soort", "regel");
        ObjectNode arg = JSON.createObjectNode();
        arg.put("node", "AttribuutRef").put("attribuut", "penningen").put("objecttype", "Jutter")
           .put("onderwerpBron", "de Jutter").putNull("navigatieRol");
        arg.putObject("type").put("node", "Onbekend").put("reden", "niet opgelost");
        arg.put("bron", "penningen van de Jutter").put("set", false);
        arg.putArray("navigatiePad");
        ObjectNode aantal = JSON.createObjectNode();
        aantal.put("node", "Functie").put("kind", "AANTAL");
        aantal.putArray("argumenten").add(arg);
        aantal.put("elementObjecttype", "Penning").putNull("leegwaarde").putNull("filter").putNull("dimensieSelectie");
        aantal.set("type", numeriek().put("teken", "NIET_NEGATIEF"));
        aantal.put("bron", "het aantal penningen van de Jutter").putNull("granulariteit");
        aantal.putObject("bronRol").put("node", "RolBinding").put("feittype", "penningbezit").put("rol", "penning");
        ObjectNode rechts = JSON.createObjectNode();
        rechts.put("node", "Literal").put("waarde", "1");
        rechts.set("type", numeriek());
        rechts.put("bron", "1");
        ObjectNode assertie = JSON.createObjectNode();
        assertie.put("node", "Vergelijking").put("op", "GROTER_OF_GELIJK");
        assertie.set("links", aantal);
        assertie.set("rechts", rechts);
        assertie.putNull("periode").put("bron", "het aantal penningen van de Jutter is groter of gelijk aan 1");
        ObjectNode con = ir.putArray("consistenties").addObject();
        con.put("node", "ResolvedConsistentie").put("naam", "penningtelcontrole").put("subject", "Jutter");
        con.set("assertie", assertie);
        con.put("bron", "Er moet worden voldaan aan: elke Jutter heeft een penning").put("line", 1);
        con.putObject("geldigheid").put("node", "Geldigheid").putNull("vanaf").putNull("tot");
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
