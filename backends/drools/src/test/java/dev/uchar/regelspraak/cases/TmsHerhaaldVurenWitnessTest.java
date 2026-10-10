package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import dev.uchar.regelspraak.transpiler.kern.Namen;
import dev.uchar.regelspraak.transpiler.tms.Fixpunt;
import dev.uchar.regelspraak.transpiler.tms.Fout;

import org.junit.jupiter.api.Test;
import org.kie.api.runtime.KieSession;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocumenten.GEHEEL;
import static dev.uchar.regelspraak.cases.TmsDocumenten.aggregatie;
import static dev.uchar.regelspraak.cases.TmsDocumenten.attribuut;
import static dev.uchar.regelspraak.cases.TmsDocumenten.attribuutgroep;
import static dev.uchar.regelspraak.cases.TmsDocumenten.binair;
import static dev.uchar.regelspraak.cases.TmsDocumenten.bronrol;
import static dev.uchar.regelspraak.cases.TmsDocumenten.check;
import static dev.uchar.regelspraak.cases.TmsDocumenten.feittypeEenNaarVeel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.getal;
import static dev.uchar.regelspraak.cases.TmsDocumenten.kenmerkregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.lezing;
import static dev.uchar.regelspraak.cases.TmsDocumenten.objecttype;
import static dev.uchar.regelspraak.cases.TmsDocumenten.rekenregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.standaard;
import static dev.uchar.regelspraak.cases.TmsDocumenten.vergelijking;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Two fires in one session of the truth-maintenance branch, through the entry a consumer has ({@link DroolsBackend#voegIn},
 * {@link DroolsBackend#vuur}, {@link DroolsBackend#lees}), with an input change between them (card tms-omschakeling,
 * points 4 and 11). The beans hold the input alone between the fires, so a derivation whose reads change lapses, a
 * fault arises or lapses, and an initialisation fires again, and the second reading equals that of a fresh session of
 * the new input.
 *
 * <p>The values document: a Jutter J1 with Vondsten V1 (gewicht 10) and V2 (gewicht 4), "De uitkering van een Vondst
 * moet berekend worden als zijn gewicht plus 10", "Een Vondst is zwaar indien zijn gewicht groter is dan 5", "De bonus
 * van een Vondst moet gesteld worden op 7 indien hij zwaar is", "Het totaalgewicht van een Jutter moet berekend worden
 * als de som van de gewichten van zijn vondsten", "Het vindloon van een Vondst moet geïnitialiseerd worden op zijn
 * gewicht plus 5", and a defaults group that gives the heffing 40 where the Vondst is zwaar. At the first fixpoint: V1
 * uitkering 20, zwaar, bonus 7, vindloon 15, heffing 40; V2 uitkering 14, vindloon 9; J1 totaalgewicht 14. Then V1's
 * gewicht becomes 3 ({@code update}): uitkering 13, not zwaar, no bonus and no heffing, vindloon 8, J1 totaalgewicht 7,
 * the values of a fresh session. The faults document: "Het aandeel van een Vondst moet berekend worden als 100 gedeeld
 * door zijn noemer", V1 noemer 0 and V2 noemer 5, then V1 noemer 4 and V2 noemer 0: the fault on V1 lapses and V1 has
 * aandeel 25, and a fault on V2 arises. The verifier passes both documents, and the reference interpreter and the ASP
 * runner give these values on both inputs of the values document, and on the faults document "division by numeric
 * zero" for a Vondst of noemer 0 and aandeel 20 and 25 for noemer 5 and 4, one Vondst per case, since there a fault
 * ends the evaluation (measured 2026-10-10).
 *
 * <p>After the write-back ({@link DroolsBackend#zetAfgeleideWaarden}) the beans hold derived values beside their input,
 * and {@link DroolsBackend#vuur} refuses the session with the sentence that says what to do instead.
 *
 * <p>A session of a model with a creation fires once (the fix round of step 3, point 1). On golden
 * {@code anchor/jutterswet-rekenkamer}, case 1, the object creation makes a Rekenkamer whose grondslag and aftrek it
 * initialises from J1's, 100 and 30, and J1's heffing is 70. Then J1's grondslag becomes 150 ({@code update}), and
 * {@code vuur} refuses the session, with the sentence that what a creation made is stated and that the way is a fresh
 * session per input. A fresh session of grondslag 150 gives heffing 120, as the reference interpreter and the ASP runner
 * give it on that case (measured 2026-10-10). <b>Red before the change</b> (measured 2026-10-10 on {@code f4b83d7b}):
 * the second {@code vuur} raised nothing, "Expected java.lang.IllegalStateException to be thrown, but nothing was
 * thrown.", and the old form of the row, fire, change, fire and read, gave the state of the first input: "expected:
 * &lt;[…, J1.heffing = 120, gemaakt: richt de rekenkamer op voor J1, gemaakte Rekenkamer.heffing = 120]&gt; but was:
 * &lt;[…, J1.heffing = 70, gemaakt: richt de rekenkamer op voor J1, gemaakte Rekenkamer.heffing = 70]&gt;", since the
 * made Rekenkamer kept the grondslag it was initialised with.
 *
 * <p>Between two fires the input changes through {@code update} on the beans {@code voegIn} put in, and in no other
 * way (the second fix round of step 3, point 2). On the values document, after the first fire, V1's vinder becomes a
 * Jutter J2 that the input did not give ({@code update}): {@code vuur} refuses, since the update changed a relation
 * (until the addition to step 5a1 it refused here in the sentence of the reference check of {@code voegIn}, which it
 * now gives where the field changed without an update), and {@code lees} still refuses after that refusal, since
 * the update stands. A fresh session that gives J2 derives J2's totaalgewicht 10 and J1's 4. Where J2 is inserted with
 * {@code sessie.insert} before V1 is set on it, the reference check would pass, as J2 is in the session; the watch
 * refuses first ({@code Bewaking}): input came in outside {@code voegIn}, which states the places of a list and the
 * bucket scalars over the beans of one input, so a new object asks a fresh session. The watch refuses alike a bean
 * deleted between two fires, and a bean inserted between {@code voegIn} and the first fire. The reference interpreter
 * and the ASP runner give the reading with J2 given (measured 2026-10-10; the verifier passes the document). <b>Red
 * before the change</b> (measured 2026-10-10 on {@code 32a649c4}): both rows failed on "Expected
 * java.lang.IllegalStateException to be thrown, but nothing was thrown."; the old form of the first row fired a second
 * time without a word and read no derivation for J2, "expected: &lt;[J1.totaalgewicht = 4, J2.totaalgewicht = 10, …]&gt;
 * but was: &lt;[J1.totaalgewicht = 4, V1 is zwaar, …]&gt;"; with J2 inserted the old form read what a fresh session
 * reads, and on the filter document of {@link TmsFilterfoutWitnessTest} a Vondst inserted between two fires stopped
 * the second fire with a {@code ConsequenceException} out of the filter fault rule, "… maar een lid van zijn route
 * heeft geen Lijstplaats onder het veld 'aanmelder' …", since only {@code voegIn} states the places.
 *
 * <p>Outside {@code vuur} a consumer changes nothing but an attribute or a characteristic of a bean {@code voegIn} put
 * in, with {@code update}, from {@code voegIn} on (the addition to step 5a1, 2026-10-10). {@code vuur} refuses, naming
 * what happened and sending the consumer to a fresh session, and {@code lees} and {@code zetAfgeleideWaarden} refuse
 * with the way to a fresh session:
 * <ul>
 *   <li>a global that changed after the first {@code vuur}: on golden {@code jutterswet-aanvoergeld} the aanvoertarief
 *       12.50 becomes 22.50 with {@code setGlobal}, which no rule sees, since the branch states a global as a fact once,
 *       at the first fire; a fresh session at 22.50 gives J1, J2 and J3 the aanvoergeld 332.50, 109.90 and 22.50, as the
 *       reference interpreter and the ASP runner give it ("oracle: PASS (7 expectations)", "clingo: PASS (7
 *       expectations)", measured 2026-10-10);</li>
 *   <li>a fact of the branch deleted ({@code Waarde} of V1's uitkering) or updated, and a fact of no kind {@code voegIn}
 *       puts in inserted;</li>
 *   <li>a plain {@code fireAllRules}, named by the first rule it fired;</li>
 *   <li>a relation changed, a reference field of a bean of the input, since {@code voegIn} numbers the places of a list
 *       over the relations of one input. On the filter document of {@link TmsFilterfoutWitnessTest} a Vondst V2 (9 by 0)
 *       without an aanmelder gets J1, whose list holds V1 (10 by 2): at the first fixpoint J1's zware vondsten is 1,
 *       as the reference interpreter gives it, and a fresh session of the new input faults on V2's 9; and a Vondst V1
 *       (9 by 0) moves from J1 to J2, whose list holds V2 (4 by 0), each at place 0 of its list: a fresh session gives
 *       J1's zware vondsten 0 and the fault of J2's first member, V1's 9 (RUNTIME-SEMANTICS ch. 5.11), where the old
 *       session kept both members at place 0 and named V2's 4. The reference interpreter faults on both fresh inputs,
 *       "division by numeric zero", a sentence without the operands, and gives J1's 0 where J2 is not actief (measured
 *       2026-10-10). The ASP runner faults on all of these inputs, also where the fold is not evaluated: it divides in
 *       the filter of every Vondst ("viol("division by numeric zero","V2")" with V2 in no list), a divergence of its own
 *       that these rows do not pin. A reference field set without an update makes no event: set to an object outside
 *       the session it is refused in the sentence of the reference check of {@code vuur}, and set to an object of the
 *       input in the sentence of the comparison of the relations (below);</li>
 *   <li>the agenda changed: {@code Agenda.clear} after {@code voegIn}, before the first fire, of which Drools makes no
 *       event; {@code AgendaGroup.clear} after a fire, which Drools queues for the next fire; and
 *       {@code AgendaGroup.setFocus} on a creation group after a fire on {@code anchor/jutterswet-rekenkamer};</li>
 *   <li>a fired session from which a consumer took the watch: what happened since is not to be seen.</li>
 * </ul>
 * <b>Red before the change</b> (measured 2026-10-10 on {@code 81ac1d6c}): nine rows failed on "Expected
 * java.lang.IllegalStateException to be thrown, but nothing was thrown.", the relation that comes into being on
 * "Unexpected exception type thrown, expected: &lt;java.lang.IllegalStateException&gt; but was:
 * &lt;org.kie.api.runtime.rule.ConsequenceException&gt;", and the two rows whose sentence changed on the sentence. The
 * old form of each (not committed) fired a second time without a word: at tarief 22.50 it read J1, J2 and J3 at 322.5,
 * 99.9 and 12.5; with V1's uitkering deleted it read no uitkering for V1, where a fresh session gives 20; the new
 * relation stopped the fire on "… heeft geen Lijstplaats onder het veld 'aanmelder' …"; the moved Vondst gave J2 the
 * fault "4 gedeeld door 0"; after {@code Agenda.clear} the fire derived nothing at all, no uitkering 20 for V1 and no
 * totaalgewicht 14 for J1; and after {@code AgendaGroup.clear} the next fire stopped on an
 * {@code UnsupportedOperationException} out of Drools, while {@code lees} read without a word before it.
 *
 * <p>A relation changed without an update, and an action of an unknown kind on the propagation list (the lens on step
 * 5a1, 2026-10-10). On the values document with a second Jutter J2 in the input, V1's vinder becomes J2 after the
 * first fire without an update, which makes no event: {@code lees}, {@code zetAfgeleideWaarden} and {@code vuur}
 * refuse, naming the field and the two Jutters, since the watch compares the reference fields of the beans of the
 * input with what they held at {@code voegIn}; a fresh session gives J1 4 and J2 10 ({@link #MET_J2}). The reference
 * interpreter and the ASP runner give J1 14 and J2 leeg on the first input and J1 4 and J2 10 on the changed one, and
 * the verifier passes the document (measured 2026-10-10). The write-back sets reference fields itself, the links of a
 * creation, and those do not count as a changed relation ({@link TmsAfnemerWitnessTest}). An action of a kind the
 * watch does not know, queued with {@code InternalAgenda.addPropagation} as another version of Drools could queue
 * one, makes the three refuse in the sentence that names its class, and the refused fire runs nothing. <b>Red before
 * the change</b> (measured 2026-10-10 on {@code fad6a24c}): both rows failed on "Expected
 * java.lang.IllegalStateException to be thrown, but nothing was thrown."; the old form (not committed) fired a second
 * time with 0 firings and read J1 totaalgewicht 14 and no value for J2, and with the action queued it read the
 * fixpoint without a word and the second fire ran the action.
 *
 * <p><b>Red before the change</b> (measured 2026-10-10 on {@code 095a586a}, the old form of the witness, in which a
 * consumer could only fire, write back and read the beans): after the second fire and write-back V1 still carried
 * zwaar, bonus 7, heffing 40 and vindloon 15, "second reading ==&gt; expected: &lt;[J1.totaalgewicht = 7, V1.uitkering =
 * 13, V1.vindloon = 8, V2.uitkering = 14, V2.vindloon = 9]&gt; but was: &lt;[J1.totaalgewicht = 7, V1 is zwaar, V1.bonus
 * = 7, V1.heffing = 40, V1.uitkering = 13, V1.vindloon = 15, V2.uitkering = 14, V2.vindloon = 9]&gt;", and the fire
 * after the write-back raised nothing, "Expected java.lang.IllegalStateException to be thrown, but nothing was thrown." The witness itself does not compile there: the entry had no {@code voegIn}, no public {@code vuur} and no
 * {@code lees}.
 */
class TmsHerhaaldVurenWitnessTest {

    static final String MODEL = TmsDocumenten.model(List.of(
            objecttype("Jutter", List.of(attribuut("totaalgewicht", GEHEEL)), List.of()),
            objecttype("Vondst", List.of(attribuut("gewicht", GEHEEL), attribuut("uitkering", GEHEEL),
                    attribuut("bonus", GEHEEL), attribuut("vindloon", GEHEEL), attribuut("heffing", GEHEEL),
                    attribuut("noemer", GEHEEL), attribuut("aandeel", TmsFilterfoutWitnessTest.BREUK)), List.of("zwaar"))),
            List.of(feittypeEenNaarVeel("vinding", "vinder", "Jutter", "vondst", "Vondst", "vondsten")));

    static final List<String> WAARDEREGELS = List.of(
            rekenregel("uitkering", "BEREKENING", "Vondst", "uitkering",
                    binair("plus", lezing("gewicht", "Vondst"), getal("10")), "null"),
            kenmerkregel("zwaarte", "Vondst", "zwaar", vergelijking("greater", lezing("gewicht", "Vondst"), getal("5"))),
            rekenregel("bonus", "GELIJKSTELLING", "Vondst", "bonus", getal("7"), check("zwaar", false)),
            rekenregel("totaalgewicht", "BEREKENING", "Jutter", "totaalgewicht",
                    aggregatie("sum", lezing("gewicht", "Vondst"), "Vondst", bronrol("vinding", "vondst"), "null",
                            "null", GEHEEL, "de som van de gewichten van zijn vondsten"), "null"),
            rekenregel("standaardvindloon", "INITIALISATIE", "Vondst", "vindloon",
                    binair("plus", lezing("gewicht", "Vondst"), getal("5")), "null"));

    static final List<String> GROEPEN = List.of(attribuutgroep("heffing van een vondst", "Vondst", "heffing",
            List.of(standaard(check("zwaar", false), getal("40")))));

    static final List<String> FOUTREGELS = List.of(rekenregel("aandeel", "BEREKENING", "Vondst", "aandeel",
            TmsFilterfoutWitnessTest.quotient(getal("100"), lezing("noemer", "Vondst")), "null"));

    static Path waardedocument() throws Exception {
        return TmsDocumenten.document(MODEL, WAARDEREGELS, List.of(), GROEPEN);
    }

    static Path foutdocument() throws Exception {
        return TmsDocumenten.document(MODEL, FOUTREGELS);
    }

    static final List<String> EERSTE_WAARDEN = List.of("J1.totaalgewicht = 14", "V1 is zwaar", "V1.bonus = 7",
            "V1.heffing = 40", "V1.uitkering = 20", "V1.vindloon = 15", "V2.uitkering = 14", "V2.vindloon = 9");
    static final List<String> TWEEDE_WAARDEN = List.of("J1.totaalgewicht = 7", "V1.uitkering = 13", "V1.vindloon = 8",
            "V2.uitkering = 14", "V2.vindloon = 9");

    private static final String NUL = "gedeeld door 0 — een numerieke nul als deler is een fout, geen leeg"
            + " (RUNTIME-SEMANTICS ch. 1.4, punt 4)";

    @Test void een_afleiding_vervalt_en_een_initialisatie_vuurt_opnieuw_tussen_twee_fires() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(waardedocument(), DroolsBackend.Branch.EXEC_MODEL);
        inSessie(b, () -> {
            Map<String, Object> bonen = waardebonen(b, "10");
            KieSession sessie = b.kieBase().newKieSession();
            try {
                DroolsBackend.voegIn(b, sessie, new Invoer().beans(bonen.values()));
                assertNull(DroolsBackend.vuur(b, sessie).geenFixpunt(), "first fire");
                assertEquals(EERSTE_WAARDEN, beschrijf(DroolsBackend.lees(b, sessie), bonen), "first reading");
                Object v1 = bonen.get("V1");
                // between the fires the beans hold the input alone
                assertNull(TmsDocumenten.lees(v1, "uitkering"), "V1 uitkering on the bean between the fires");
                assertNull(TmsDocumenten.lees(v1, "vindloon"), "V1 vindloon on the bean between the fires");
                assertFalse(TmsDocumenten.heeft(v1, "zwaar"), "V1 zwaar on the bean between the fires");
                zet(v1, "gewicht", "3");
                sessie.update(sessie.getFactHandle(v1), v1);
                assertNull(DroolsBackend.vuur(b, sessie).geenFixpunt(), "second fire");
                List<String> tweede = beschrijf(DroolsBackend.lees(b, sessie), bonen);
                assertEquals(TWEEDE_WAARDEN, tweede, "second reading: the characteristic, the conditional value and the"
                        + " default lapse, the sum follows, and the initialisation fires again, since its target stayed"
                        + " empty on the bean");
                assertEquals(verseLezing(b, bonen), tweede, "the second reading is that of a fresh session");
            } finally {
                sessie.dispose();
            }
        });
    }

    @Test void een_fout_vervalt_en_een_fout_ontstaat_tussen_twee_fires() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(foutdocument(), DroolsBackend.Branch.EXEC_MODEL);
        inSessie(b, () -> {
            Map<String, Object> bonen = new LinkedHashMap<>();
            bonen.put("V1", TmsDocumenten.boon(b.beanLoader(), "Vondst", Map.of("noemer", "0"), List.of()));
            bonen.put("V2", TmsDocumenten.boon(b.beanLoader(), "Vondst", Map.of("noemer", "5"), List.of()));
            KieSession sessie = b.kieBase().newKieSession();
            try {
                DroolsBackend.voegIn(b, sessie, new Invoer().beans(bonen.values()));
                assertNull(DroolsBackend.vuur(b, sessie).geenFixpunt(), "first fire");
                assertEquals(List.of("V1: deling door nul in de regel \"aandeel\": 100 " + NUL, "V2.aandeel = 20"),
                        beschrijf(DroolsBackend.lees(b, sessie), bonen), "first reading");
                zet(bonen.get("V1"), "noemer", "4");
                zet(bonen.get("V2"), "noemer", "0");
                for (Object v : bonen.values()) sessie.update(sessie.getFactHandle(v), v);
                assertNull(DroolsBackend.vuur(b, sessie).geenFixpunt(), "second fire");
                List<String> tweede = beschrijf(DroolsBackend.lees(b, sessie), bonen);
                assertEquals(List.of("V1.aandeel = 25", "V2: deling door nul in de regel \"aandeel\": 100 " + NUL), tweede,
                        "second reading: the fault on V1 lapsed, the one on V2 arose");
                assertEquals(verseLezing(b, bonen), tweede, "the second reading is that of a fresh session");
            } finally {
                sessie.dispose();
            }
        });
    }

    @Test void na_het_terugzetten_weigert_vuur_de_sessie() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(waardedocument(), DroolsBackend.Branch.EXEC_MODEL);
        inSessie(b, () -> {
            Map<String, Object> bonen = waardebonen(b, "10");
            KieSession sessie = b.kieBase().newKieSession();
            try {
                DroolsBackend.voegIn(b, sessie, new Invoer().beans(bonen.values()));
                assertNull(DroolsBackend.vuur(b, sessie).geenFixpunt(), "first fire");
                DroolsBackend.zetAfgeleideWaarden(b, sessie);
                assertGetal("15", TmsDocumenten.lees(bonen.get("V1"), "vindloon"), "V1 vindloon written back");
                IllegalStateException e = assertThrows(IllegalStateException.class, () -> DroolsBackend.vuur(b, sessie));
                assertEquals("deze sessie is teruggezet (zetAfgeleideWaarden): haar beans dragen afgeleide waarden naast"
                        + " de invoer, en een volgende fire zou die als invoer lezen; lees tussen twee fires met"
                        + " DroolsBackend.lees en zet pas na de laatste fire terug, of open een verse sessie met beans die"
                        + " alleen invoer dragen", e.getMessage());
                assertEquals(EERSTE_WAARDEN, beschrijf(DroolsBackend.lees(b, sessie), bonen),
                        "the reading after the write-back is the one it wrote");
            } finally {
                sessie.dispose();
            }
        });
    }

    @Test void een_model_met_creatie_weigert_een_tweede_vuur() throws Exception {
        Path ir = Path.of("..", "..", "corpus", "golden", "anchor", "jutterswet-rekenkamer", "ir.json");
        String zaak = Files.readString(ir.resolveSibling("case1-berekend.json"));
        CaseRunner runner = CaseRunner.voorMeting(ir, DroolsBackend.Branch.EXEC_MODEL);
        runner.metSessie(TmsDocumenten.JSON.readTree(zaak), (bouw, sessie, labels) -> {
            Object j1 = labels.get("J1");
            assertNull(DroolsBackend.vuur(bouw, sessie).geenFixpunt(), "first fire");
            assertEquals("70", heffing(DroolsBackend.lees(bouw, sessie), j1), "J1 heffing, first fire");
            try {
                zet(j1, "grondslag", "150");
            } catch (Exception e) {
                throw new AssertionError(e);
            }
            sessie.update(sessie.getFactHandle(j1), j1);
            IllegalStateException e = assertThrows(IllegalStateException.class, () -> DroolsBackend.vuur(bouw, sessie));
            assertEquals("dit model heeft creatieregels, en deze sessie is al via DroolsBackend.vuur gevuurd: wat een"
                    + " creatie maakte, met de initialisaties, de koppelingen en de fout van een mislukte initialisatie,"
                    + " is gesteld en vervalt niet (RUNTIME-SEMANTICS ch. 5.5), dus een volgende fire zou na een"
                    + " invoerwijziging een toestand geven die geen verse evaluatie geeft; open per invoer een verse"
                    + " sessie", e.getMessage());
            return null;
        });
        // the way the sentence names: a fresh session of the changed input
        JsonNode veranderd = TmsDocumenten.JSON.readTree(zaak.replace("\"100\"", "\"150\""));
        runner.metSessie(veranderd, (bouw, sessie, labels) -> {
            assertNull(DroolsBackend.vuur(bouw, sessie).geenFixpunt(), "fresh fire");
            assertEquals("120", heffing(DroolsBackend.lees(bouw, sessie), labels.get("J1")),
                    "J1 heffing in a fresh session of grondslag 150");
            return null;
        });
    }

    /** The reading of a fresh session of the values document in which V1's vinder is a second Jutter J2. */
    static final List<String> MET_J2 = List.of("J1.totaalgewicht = 4", "J2.totaalgewicht = 10", "V1 is zwaar",
            "V1.bonus = 7", "V1.heffing = 40", "V1.uitkering = 20", "V1.vindloon = 15", "V2.uitkering = 14",
            "V2.vindloon = 9");

    @Test void een_relatie_naar_een_object_buiten_de_invoer_weigert_vuur() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(waardedocument(), DroolsBackend.Branch.EXEC_MODEL);
        inSessie(b, () -> {
            Map<String, Object> bonen = waardebonen(b, "10");
            Object v1 = bonen.get("V1");
            Object j2 = TmsDocumenten.boon(b.beanLoader(), "Jutter", Map.of(), List.of());
            KieSession sessie = b.kieBase().newKieSession();
            try {
                DroolsBackend.voegIn(b, sessie, new Invoer().beans(bonen.values()));
                assertNull(DroolsBackend.vuur(b, sessie).geenFixpunt(), "first fire");
                zetRol(v1, "vinder", j2);
                sessie.update(sessie.getFactHandle(v1), v1);
                assertEquals(meerDanEenUpdate(NA_VUUR, relatie("vinder", v1)), assertThrows(IllegalStateException.class,
                        () -> DroolsBackend.vuur(b, sessie)).getMessage(), "the second fire: a changed relation");
                assertEquals(TmsAfnemerWitnessTest.buitenVuurOm(TmsAfnemerWitnessTest.naam(v1)
                        + " werd gewijzigd met update", VERS_BUITEN_VUUR_OM),
                        assertThrows(IllegalStateException.class, () -> DroolsBackend.lees(b, sessie)).getMessage(),
                        "the reading after the refused fire still sees the update");
            } finally {
                sessie.dispose();
            }
            // the reference set without an update: the watch sees no event, and the reference check refuses
            zetRol(v1, "vinder", bonen.get("J1"));
            KieSession zonderUpdate = b.kieBase().newKieSession();
            try {
                DroolsBackend.voegIn(b, zonderUpdate, new Invoer().beans(bonen.values()));
                assertNull(DroolsBackend.vuur(b, zonderUpdate).geenFixpunt(), "first fire");
                zetRol(v1, "vinder", j2);
                assertEquals("vuur: het veld 'vinder' van " + TmsAfnemerWitnessTest.naam(v1) + " wijst naar een Jutter ("
                        + TmsAfnemerWitnessTest.naam(j2) + ") die niet als bean in de invoer staat: de tak leidt alleen af"
                        + " voor de objecten in de sessie, dus geef elk object waarnaar een bean wijst ook zelf als bean op",
                        assertThrows(IllegalStateException.class, () -> DroolsBackend.vuur(b, zonderUpdate)).getMessage(),
                        "the second fire: the reference check");
            } finally {
                zonderUpdate.dispose();
            }
            // the way the sentence names: a fresh session that gives J2 as a bean
            bonen.put("J2", j2);
            assertEquals(MET_J2, verseLezing(b, bonen), "a fresh session that gives J2");
        });
    }

    @Test void invoer_die_buiten_voegIn_om_bijkomt_of_weggaat_weigert_vuur() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(waardedocument(), DroolsBackend.Branch.EXEC_MODEL);
        inSessie(b, () -> {
            String nieuwOfWeg = "): een afnemer geeft alle beans in één Invoer aan voegIn en verandert daarna alleen hun"
                    + " attributen en kenmerken, met update, want voegIn legt de plaatsen van een lijst en de"
                    + " emmerscalairen vast over de beans van één invoer; een nieuw of verwijderd object vraagt een verse"
                    + " sessie";
            String naVuur = "sinds de laatste DroolsBackend.vuur van deze sessie kwam er buiten voegIn om invoer bij of"
                    + " ging er invoer weg (";
            // J2 inserted between the fires and V1 set on it: J2 is in the session, so the reference check would pass
            Map<String, Object> bonen = waardebonen(b, "10");
            Object v1 = bonen.get("V1");
            Object j2 = TmsDocumenten.boon(b.beanLoader(), "Jutter", Map.of(), List.of());
            KieSession sessie = b.kieBase().newKieSession();
            try {
                DroolsBackend.voegIn(b, sessie, new Invoer().beans(bonen.values()));
                assertNull(DroolsBackend.vuur(b, sessie).geenFixpunt(), "first fire");
                sessie.insert(j2);
                zetRol(v1, "vinder", j2);
                sessie.update(sessie.getFactHandle(v1), v1);
                String ingevoegd = TmsAfnemerWitnessTest.naam(j2) + " werd ingevoegd";
                assertEquals(naVuur + ingevoegd + nieuwOfWeg, assertThrows(IllegalStateException.class,
                        () -> DroolsBackend.vuur(b, sessie)).getMessage(), "a bean inserted between two fires");
                assertEquals(TmsAfnemerWitnessTest.buitenVuurOm(ingevoegd, NIEUW_OF_WEG), assertThrows(
                        IllegalStateException.class, () -> DroolsBackend.lees(b, sessie)).getMessage(),
                        "the reading sends the consumer to a fresh session");
            } finally {
                sessie.dispose();
            }
            // V2 deleted between the fires
            Map<String, Object> tweede = waardebonen(b, "10");
            Object v2 = tweede.get("V2");
            KieSession weg = b.kieBase().newKieSession();
            try {
                DroolsBackend.voegIn(b, weg, new Invoer().beans(tweede.values()));
                assertNull(DroolsBackend.vuur(b, weg).geenFixpunt(), "first fire");
                weg.delete(weg.getFactHandle(v2));
                assertEquals(naVuur + TmsAfnemerWitnessTest.naam(v2) + " werd verwijderd" + nieuwOfWeg,
                        assertThrows(IllegalStateException.class, () -> DroolsBackend.vuur(b, weg)).getMessage(),
                        "a bean deleted between two fires");
            } finally {
                weg.dispose();
            }
            // a Jutter inserted after voegIn, before the first fire
            Map<String, Object> derde = waardebonen(b, "10");
            Object j3 = TmsDocumenten.boon(b.beanLoader(), "Jutter", Map.of(), List.of());
            KieSession vooraf = b.kieBase().newKieSession();
            try {
                DroolsBackend.voegIn(b, vooraf, new Invoer().beans(derde.values()));
                vooraf.insert(j3);
                assertEquals("sinds DroolsBackend.voegIn kwam er buiten voegIn om invoer bij of ging er invoer weg ("
                        + TmsAfnemerWitnessTest.naam(j3) + " werd ingevoegd" + nieuwOfWeg,
                        assertThrows(IllegalStateException.class, () -> DroolsBackend.vuur(b, vooraf)).getMessage(),
                        "a bean inserted after voegIn, before the first fire");
            } finally {
                vooraf.dispose();
            }
        });
    }

    /** The way on of the two readings where input came or went outside {@code voegIn}. */
    static final String NIEUW_OF_WEG = "invoer die buiten DroolsBackend.voegIn om bijkwam of wegging, vraagt een verse"
            + " sessie, dus DroolsBackend.vuur weigert haar: open een verse sessie met alle beans in één Invoer";

    /** The call of the entry the watch keeps from after a fire. */
    static final String NA_VUUR = "de laatste DroolsBackend.vuur van deze sessie";

    /** The sentence of {@code vuur} where, since {@code sinds}, more happened outside it than an update of an attribute
     *  or a characteristic of a bean, {@code wat} naming it. */
    static String meerDanEenUpdate(String sinds, String wat) {
        return "sinds " + sinds + " gebeurde er buiten vuur om meer dan een update van een attribuut of kenmerk van een"
                + " bean (" + wat + "), en meer laat de ingang niet toe: open een verse sessie met alle beans in één"
                + " Invoer";
    }

    /** The way on of the two readings where {@code vuur} refuses for that reason. */
    static final String VERS_BUITEN_VUUR_OM = "DroolsBackend.vuur weigert een sessie waarin buiten vuur om meer gebeurde"
            + " dan een update van een attribuut of kenmerk van een bean: open een verse sessie met alle beans in één"
            + " Invoer";

    /** The clause of a relation that an update changed: field {@code veld} of {@code boon}. */
    static String relatie(String veld, Object boon) {
        return "het veld '" + veld + "' van " + TmsAfnemerWitnessTest.naam(boon) + ", een relatie, werd gewijzigd met"
                + " update";
    }

    @Test void een_global_die_na_de_eerste_vuur_verandert_weigert_vuur_lees_en_terugzetten() throws Exception {
        Path ir = Path.of("..", "..", "corpus", "golden", "jutterswet-aanvoergeld", "ir.json");
        String zaak = Files.readString(ir.resolveSibling("case.json"));
        CaseRunner runner = CaseRunner.voorMeting(ir, DroolsBackend.Branch.EXEC_MODEL);
        String veranderd = "de global 'aanvoertarief' veranderde van 12.50 in 22.50 na de eerste DroolsBackend.vuur van"
                + " deze sessie";
        runner.metSessie(TmsDocumenten.JSON.readTree(zaak), (bouw, sessie, labels) -> {
            assertNull(DroolsBackend.vuur(bouw, sessie).geenFixpunt(), "first fire");
            assertEquals(List.of("J1 322.5", "J2 99.9", "J3 12.5"), aanvoergeld(DroolsBackend.lees(bouw, sessie), labels),
                    "the golden's aanvoergeld at tarief 12.50");
            sessie.setGlobal("aanvoertarief", new BigDecimal("22.50"));
            assertEquals(veranderd + ": de tak leest een global één keer, in een regel die haar bij de eerste fire als"
                    + " feit stelt, dus een volgende fire zou met de oude waarde rekenen; zet de globals vóór de eerste"
                    + " vuur, en open voor een andere waarde een verse sessie",
                    assertThrows(IllegalStateException.class, () -> DroolsBackend.vuur(bouw, sessie)).getMessage(),
                    "the second fire");
            String lezing = "geen fixpunt om te lezen of terug te zetten: " + veranderd + ", en het fixpunt hoort bij de"
                    + " oude waarde; DroolsBackend.vuur weigert de sessie, dus open een verse sessie";
            assertEquals(lezing, assertThrows(IllegalStateException.class, () -> DroolsBackend.lees(bouw, sessie))
                    .getMessage(), "the reading");
            assertEquals(lezing, assertThrows(IllegalStateException.class,
                    () -> DroolsBackend.zetAfgeleideWaarden(bouw, sessie)).getMessage(), "the write-back");
            return null;
        });
        // the way the sentence names: a fresh session with the other value
        JsonNode ander = TmsDocumenten.JSON.readTree(zaak);
        ((com.fasterxml.jackson.databind.node.ObjectNode) ander.get("input").get("parameters").get("aanvoertarief"))
                .put("number", "22.50");
        runner.metSessie(ander, (bouw, sessie, labels) -> {
            assertNull(DroolsBackend.vuur(bouw, sessie).geenFixpunt(), "fresh fire");
            assertEquals(List.of("J1 332.5", "J2 109.9", "J3 22.5"), aanvoergeld(DroolsBackend.lees(bouw, sessie), labels),
                    "a fresh session at tarief 22.50");
            return null;
        });
    }

    @Test void een_verwijderd_feit_van_de_tak_weigert_vuur() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(waardedocument(), DroolsBackend.Branch.EXEC_MODEL);
        inSessie(b, () -> {
            Map<String, Object> bonen = waardebonen(b, "10");
            KieSession sessie = b.kieBase().newKieSession();
            try {
                DroolsBackend.voegIn(b, sessie, new Invoer().beans(bonen.values()));
                assertNull(DroolsBackend.vuur(b, sessie).geenFixpunt(), "first fire");
                org.kie.api.runtime.rule.FactHandle h = uitkering(sessie, bonen.get("V1"));
                Object waarde = sessie.getObject(h);
                sessie.delete(h);
                String wat = TmsAfnemerWitnessTest.naam(waarde) + " werd verwijderd";
                assertEquals(meerDanEenUpdate(NA_VUUR, wat), assertThrows(IllegalStateException.class,
                        () -> DroolsBackend.vuur(b, sessie)).getMessage(), "the second fire");
                assertEquals(TmsAfnemerWitnessTest.buitenVuurOm(wat, VERS_BUITEN_VUUR_OM), assertThrows(
                        IllegalStateException.class, () -> DroolsBackend.lees(b, sessie)).getMessage(), "the reading");
            } finally {
                sessie.dispose();
            }
        });
    }

    @Test void een_ingevoegd_of_gewijzigd_feit_dat_geen_bean_van_de_invoer_is_weigert_vuur() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(waardedocument(), DroolsBackend.Branch.EXEC_MODEL);
        inSessie(b, () -> {
            // a fact of no kind voegIn puts in
            Map<String, Object> bonen = waardebonen(b, "10");
            KieSession sessie = b.kieBase().newKieSession();
            try {
                DroolsBackend.voegIn(b, sessie, new Invoer().beans(bonen.values()));
                assertNull(DroolsBackend.vuur(b, sessie).geenFixpunt(), "first fire");
                String vreemd = "geen invoer";
                sessie.insert(vreemd);
                assertEquals(meerDanEenUpdate(NA_VUUR, TmsAfnemerWitnessTest.naam(vreemd) + " werd ingevoegd"),
                        assertThrows(IllegalStateException.class, () -> DroolsBackend.vuur(b, sessie)).getMessage(),
                        "a fact inserted between two fires");
            } finally {
                sessie.dispose();
            }
            // a fact of the branch updated
            Map<String, Object> tweede = waardebonen(b, "10");
            KieSession andere = b.kieBase().newKieSession();
            try {
                DroolsBackend.voegIn(b, andere, new Invoer().beans(tweede.values()));
                assertNull(DroolsBackend.vuur(b, andere).geenFixpunt(), "first fire");
                org.kie.api.runtime.rule.FactHandle h = uitkering(andere, tweede.get("V1"));
                andere.update(h, andere.getObject(h));
                assertEquals(meerDanEenUpdate(NA_VUUR, TmsAfnemerWitnessTest.naam(andere.getObject(h))
                        + " werd gewijzigd met update"), assertThrows(IllegalStateException.class,
                        () -> DroolsBackend.vuur(b, andere)).getMessage(), "a fact of the branch updated between two fires");
            } finally {
                andere.dispose();
            }
        });
    }

    @Test void een_kale_fire_tussen_twee_fires_weigert_vuur() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(waardedocument(), DroolsBackend.Branch.EXEC_MODEL);
        inSessie(b, () -> {
            Map<String, Object> bonen = waardebonen(b, "10");
            KieSession sessie = b.kieBase().newKieSession();
            try {
                DroolsBackend.voegIn(b, sessie, new Invoer().beans(bonen.values()));
                assertNull(DroolsBackend.vuur(b, sessie).geenFixpunt(), "first fire");
                Object v1 = bonen.get("V1");
                zet(v1, "gewicht", "3");
                sessie.update(sessie.getFactHandle(v1), v1);
                String regel = TmsAfnemerWitnessTest.kaleFire(sessie);
                assertNotNull(regel, "the plain fire fires a rule: the network has not seen V1's new gewicht");
                assertEquals(meerDanEenUpdate(NA_VUUR, "de regel \"" + regel + "\" vuurde"), assertThrows(
                        IllegalStateException.class, () -> DroolsBackend.vuur(b, sessie)).getMessage(), "the fire after it");
                assertEquals(TmsAfnemerWitnessTest.buitenVuurOm(TmsAfnemerWitnessTest.naam(v1) + " werd gewijzigd met"
                        + " update en de regel \"" + regel + "\" vuurde", VERS_BUITEN_VUUR_OM),
                        assertThrows(IllegalStateException.class, () -> DroolsBackend.lees(b, sessie)).getMessage(),
                        "the reading");
            } finally {
                sessie.dispose();
            }
        });
    }

    /** J1 actief with V1 (10 by 2) as its aanmelder's Vondst, and V2 (9 by 0) without an aanmelder; then V2 gets J1. */
    @Test void een_relatie_die_ontstaat_tussen_twee_fires_weigert_vuur() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(TmsFilterfoutWitnessTest.document(TmsFilterfoutWitnessTest.ZWARE),
                DroolsBackend.Branch.EXEC_MODEL);
        inSessie(b, () -> {
            Map<String, Object> bonen = new LinkedHashMap<>();
            Object j1 = TmsDocumenten.boon(b.beanLoader(), "Jutter", Map.of(), List.of("actief"));
            bonen.put("J1", j1);
            bonen.put("V1", TmsDocumenten.boon(b.beanLoader(), "Vondst", Map.of("gewicht", "10", "deelnemers", "2"),
                    List.of()));
            bonen.put("V2", TmsDocumenten.boon(b.beanLoader(), "Vondst", Map.of("gewicht", "9", "deelnemers", "0"),
                    List.of()));
            zetRol(bonen.get("V1"), "aanmelder", j1);
            KieSession sessie = b.kieBase().newKieSession();
            try {
                DroolsBackend.voegIn(b, sessie, new Invoer().beans(bonen.values()));
                assertNull(DroolsBackend.vuur(b, sessie).geenFixpunt(), "first fire");
                assertEquals(List.of("J1.zware vondsten = 1"), beschrijf(DroolsBackend.lees(b, sessie), bonen),
                        "first reading: V2 is no member");
                Object v2 = bonen.get("V2");
                zetRol(v2, "aanmelder", j1);
                sessie.update(sessie.getFactHandle(v2), v2);
                assertEquals(meerDanEenUpdate(NA_VUUR, relatie("aanmelder", v2)), assertThrows(
                        IllegalStateException.class, () -> DroolsBackend.vuur(b, sessie)).getMessage(), "the second fire");
                assertEquals(TmsAfnemerWitnessTest.buitenVuurOm(TmsAfnemerWitnessTest.naam(v2) + " werd gewijzigd met"
                        + " update", VERS_BUITEN_VUUR_OM), assertThrows(IllegalStateException.class,
                        () -> DroolsBackend.lees(b, sessie)).getMessage(), "the reading");
            } finally {
                sessie.dispose();
            }
            // the way the sentence names: a fresh session, in which V2 divides 9 by 0 in J1's list
            assertEquals(List.of("J1: " + ZWARE_FOUT.formatted("9")), verseLezing(b, bonen), "a fresh session");
        });
    }

    /** J1 and J2 actief, V1 (9 by 0) J1's, V2 (4 by 0) J2's, each at place 0 of its list; then V1 moves to J2. */
    @Test void een_relatie_die_tussen_twee_fires_verhuist_weigert_vuur() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(TmsFilterfoutWitnessTest.document(TmsFilterfoutWitnessTest.ZWARE),
                DroolsBackend.Branch.EXEC_MODEL);
        inSessie(b, () -> {
            Map<String, Object> bonen = new LinkedHashMap<>();
            Object j1 = TmsDocumenten.boon(b.beanLoader(), "Jutter", Map.of(), List.of("actief"));
            Object j2 = TmsDocumenten.boon(b.beanLoader(), "Jutter", Map.of(), List.of("actief"));
            bonen.put("J1", j1);
            bonen.put("J2", j2);
            bonen.put("V1", TmsDocumenten.boon(b.beanLoader(), "Vondst", Map.of("gewicht", "9", "deelnemers", "0"),
                    List.of()));
            bonen.put("V2", TmsDocumenten.boon(b.beanLoader(), "Vondst", Map.of("gewicht", "4", "deelnemers", "0"),
                    List.of()));
            zetRol(bonen.get("V1"), "aanmelder", j1);
            zetRol(bonen.get("V2"), "aanmelder", j2);
            KieSession sessie = b.kieBase().newKieSession();
            try {
                DroolsBackend.voegIn(b, sessie, new Invoer().beans(bonen.values()));
                assertNull(DroolsBackend.vuur(b, sessie).geenFixpunt(), "first fire");
                assertEquals(List.of("J1: " + ZWARE_FOUT.formatted("9"), "J2: " + ZWARE_FOUT.formatted("4")),
                        beschrijf(DroolsBackend.lees(b, sessie), bonen), "first reading");
                Object v1 = bonen.get("V1");
                zetRol(v1, "aanmelder", j2);
                sessie.update(sessie.getFactHandle(v1), v1);
                assertEquals(meerDanEenUpdate(NA_VUUR, relatie("aanmelder", v1)), assertThrows(
                        IllegalStateException.class, () -> DroolsBackend.vuur(b, sessie)).getMessage(), "the second fire");
            } finally {
                sessie.dispose();
            }
            // the way the sentence names: a fresh session, in which V1 comes before V2 in J2's list and divides 9
            assertEquals(List.of("J1.zware vondsten = 0", "J2: " + ZWARE_FOUT.formatted("9")), verseLezing(b, bonen),
                    "a fresh session");
        });
    }

    /** The clause of a relation that changed without an update: field {@code veld} of {@code bean}, which held
     *  {@code voor} at {@code voegIn} and holds {@code na}. */
    static String zonderUpdate(String veld, Object bean, Object voor, Object na) {
        return "het veld '" + veld + "' van " + TmsAfnemerWitnessTest.naam(bean) + ", een relatie, veranderde zonder"
                + " update: het hield bij DroolsBackend.voegIn " + TmsAfnemerWitnessTest.naam(voor) + " en houdt nu "
                + TmsAfnemerWitnessTest.naam(na);
    }

    /** J1 and J2 in the input, V1 (10) and V2 (4) found by J1; after the first fire V1's vinder becomes J2 without an
     *  update, which Drools does not see. */
    @Test void een_relatie_die_zonder_update_verandert_weigert_vuur_lees_en_terugzetten() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(waardedocument(), DroolsBackend.Branch.EXEC_MODEL);
        inSessie(b, () -> {
            Map<String, Object> bonen = waardebonen(b, "10");
            Object j1 = bonen.get("J1"), v1 = bonen.get("V1");
            Object j2 = TmsDocumenten.boon(b.beanLoader(), "Jutter", Map.of(), List.of());
            bonen.put("J2", j2);
            KieSession sessie = b.kieBase().newKieSession();
            try {
                DroolsBackend.voegIn(b, sessie, new Invoer().beans(bonen.values()));
                assertNull(DroolsBackend.vuur(b, sessie).geenFixpunt(), "first fire");
                assertEquals(EERSTE_WAARDEN, beschrijf(DroolsBackend.lees(b, sessie), bonen), "first reading");
                zetRol(v1, "vinder", j2);
                String veranderd = zonderUpdate("vinder", v1, j1, j2);
                String lezing = "geen fixpunt om te lezen of terug te zetten: " + veranderd + ", en het fixpunt hoort bij"
                        + " de oude relatie; DroolsBackend.vuur weigert de sessie, dus open een verse sessie met alle"
                        + " beans in één Invoer";
                assertEquals(lezing, assertThrows(IllegalStateException.class, () -> DroolsBackend.lees(b, sessie))
                        .getMessage(), "the reading");
                assertEquals(lezing, assertThrows(IllegalStateException.class,
                        () -> DroolsBackend.zetAfgeleideWaarden(b, sessie)).getMessage(), "the write-back");
                assertEquals(veranderd + ": Drools ziet een veld pas na een update, dus een fire zou de oude relatie"
                        + " blijven lezen, en ook met een update vraagt een gewijzigde relatie een verse sessie, want"
                        + " voegIn legt de plaatsen van een lijst vast over de relaties van één invoer; open een verse"
                        + " sessie met alle beans in één Invoer", assertThrows(IllegalStateException.class,
                        () -> DroolsBackend.vuur(b, sessie)).getMessage(), "the second fire");
            } finally {
                sessie.dispose();
            }
            // the way the sentences name: a fresh session of the changed input
            assertEquals(MET_J2, verseLezing(b, bonen), "a fresh session");
        });
    }

    @Test void een_geleegde_agenda_of_een_focuswissel_buiten_vuur_om_weigert_vuur_lees_en_terugzetten()
            throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(waardedocument(), DroolsBackend.Branch.EXEC_MODEL);
        String main = "de agendagroep \"MAIN\" werd geleegd";
        inSessie(b, () -> {
            // the agenda cleared after voegIn, before the first fire
            Map<String, Object> bonen = waardebonen(b, "10");
            KieSession sessie = b.kieBase().newKieSession();
            try {
                DroolsBackend.voegIn(b, sessie, new Invoer().beans(bonen.values()));
                sessie.getAgenda().clear();
                assertEquals(meerDanEenUpdate("DroolsBackend.voegIn", main), assertThrows(IllegalStateException.class,
                        () -> DroolsBackend.vuur(b, sessie)).getMessage(), "the fire after the agenda was cleared");
            } finally {
                sessie.dispose();
            }
            // a group cleared after the fire
            Map<String, Object> tweede = waardebonen(b, "10");
            KieSession andere = b.kieBase().newKieSession();
            try {
                DroolsBackend.voegIn(b, andere, new Invoer().beans(tweede.values()));
                assertNull(DroolsBackend.vuur(b, andere).geenFixpunt(), "first fire");
                andere.getAgenda().getAgendaGroup("MAIN").clear();
                // Drools queues the clear of a group for the next fire, which ran it with an
                // UnsupportedOperationException (measured 2026-10-10), so the watch names the call that waits
                String groep = "een agendagroep werd geleegd met AgendaGroup.clear";
                String lezing = TmsAfnemerWitnessTest.buitenVuurOm(groep, VERS_BUITEN_VUUR_OM);
                assertEquals(lezing, assertThrows(IllegalStateException.class, () -> DroolsBackend.lees(b, andere))
                        .getMessage(), "the reading");
                assertEquals(lezing, assertThrows(IllegalStateException.class,
                        () -> DroolsBackend.zetAfgeleideWaarden(b, andere)).getMessage(), "the write-back");
                assertEquals(meerDanEenUpdate(NA_VUUR, groep), assertThrows(IllegalStateException.class,
                        () -> DroolsBackend.vuur(b, andere)).getMessage(), "the second fire");
            } finally {
                andere.dispose();
            }
        });
        // a focus change after the fire, on a model with a creation, the only kind whose KieBase has a group beside MAIN
        Path ir = Path.of("..", "..", "corpus", "golden", "anchor", "jutterswet-rekenkamer", "ir.json");
        CaseRunner.voorMeting(ir, DroolsBackend.Branch.EXEC_MODEL).metSessie(TmsDocumenten.JSON.readTree(
                Files.readString(ir.resolveSibling("case1-berekend.json"))), (bouw, sessie, labels) -> {
            assertNull(DroolsBackend.vuur(bouw, sessie).geenFixpunt(), "first fire");
            sessie.getAgenda().getAgendaGroup(dev.uchar.regelspraak.transpiler.tms.TmsTranspiler.OBJECTCREATIE)
                    .setFocus();
            String lezing = TmsAfnemerWitnessTest.buitenVuurOm("een agendagroep kreeg de focus met"
                    + " AgendaGroup.setFocus", TmsAfnemerWitnessTest.VERS_BIJ_CREATIE);
            assertEquals(lezing, assertThrows(IllegalStateException.class, () -> DroolsBackend.lees(bouw, sessie))
                    .getMessage(), "the reading");
            assertEquals(lezing, assertThrows(IllegalStateException.class,
                    () -> DroolsBackend.zetAfgeleideWaarden(bouw, sessie)).getMessage(), "the write-back");
            return null;
        });
    }

    /** An action on the propagation list of a kind the watch does not know, as another version of Drools could queue
     *  for the next fire; it records whether a fire ran it. */
    private static final class OnbekendeActie extends org.drools.core.phreak.PropagationEntry.AbstractPropagationEntry {
        boolean uitgevoerd;

        @Override public void internalExecute(org.drools.core.common.ReteEvaluator evaluator) {
            uitgevoerd = true;
        }
    }

    @Test void een_onbekende_actie_op_de_propagatielijst_weigert_vuur_lees_en_terugzetten() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(waardedocument(), DroolsBackend.Branch.EXEC_MODEL);
        inSessie(b, () -> {
            Map<String, Object> bonen = waardebonen(b, "10");
            KieSession sessie = b.kieBase().newKieSession();
            try {
                DroolsBackend.voegIn(b, sessie, new Invoer().beans(bonen.values()));
                assertNull(DroolsBackend.vuur(b, sessie).geenFixpunt(), "first fire");
                OnbekendeActie actie = new OnbekendeActie();
                ((org.drools.core.common.InternalAgenda) sessie.getAgenda()).addPropagation(actie);
                String zin = "op de propagatielijst van deze sessie wacht een " + OnbekendeActie.class.getName()
                        + ", een actie die de bewaking niet kent: zij kent naast een insert, update of delete van een"
                        + " feit alleen die van AgendaGroup.clear en AgendaGroup.setFocus, dus ze kan niet zeggen of een"
                        + " afnemer de agenda veranderde";
                assertEquals(zin, assertThrows(IllegalStateException.class, () -> DroolsBackend.lees(b, sessie))
                        .getMessage(), "the reading");
                assertEquals(zin, assertThrows(IllegalStateException.class,
                        () -> DroolsBackend.zetAfgeleideWaarden(b, sessie)).getMessage(), "the write-back");
                assertEquals(zin, assertThrows(IllegalStateException.class, () -> DroolsBackend.vuur(b, sessie))
                        .getMessage(), "the second fire");
                assertFalse(actie.uitgevoerd, "the refused fire ran no action");
            } finally {
                sessie.dispose();
            }
        });
    }

    @Test void een_gevuurde_sessie_zonder_haar_bewaking_weigert_vuur_en_lees() throws Exception {
        DroolsBackend.Build b = DroolsBackend.build(waardedocument(), DroolsBackend.Branch.EXEC_MODEL);
        inSessie(b, () -> {
            Map<String, Object> bonen = waardebonen(b, "10");
            KieSession sessie = b.kieBase().newKieSession();
            try {
                DroolsBackend.voegIn(b, sessie, new Invoer().beans(bonen.values()));
                assertNull(DroolsBackend.vuur(b, sessie).geenFixpunt(), "first fire");
                for (org.kie.api.event.rule.RuleRuntimeEventListener l : List.copyOf(sessie.getRuleRuntimeEventListeners()))
                    sessie.removeEventListener(l);
                assertEquals("de bewaking die DroolsBackend.vuur op deze sessie zette, staat er niet meer op, dus wat er"
                        + " na de laatste vuur gebeurde, is niet te zien; open een verse sessie met alle beans in één"
                        + " Invoer", assertThrows(IllegalStateException.class, () -> DroolsBackend.vuur(b, sessie))
                        .getMessage(), "the second fire");
                assertEquals("geen fixpunt om te lezen of terug te zetten: de bewaking die DroolsBackend.vuur op deze"
                        + " sessie zet, staat er niet meer op, dus wat er na de laatste vuur gebeurde, is niet te zien;"
                        + " DroolsBackend.vuur weigert haar ook: open een verse sessie met alle beans in één Invoer",
                        assertThrows(IllegalStateException.class, () -> DroolsBackend.lees(b, sessie)).getMessage(),
                        "the reading");
            } finally {
                sessie.dispose();
            }
        });
    }

    /** The fault of the rule "zware vondsten" where the filter divides {@code %s} by 0, as {@code Fixpunt} gives it. */
    static final String ZWARE_FOUT = "deling door nul in de regel \"zware vondsten\": %s gedeeld door 0 — een numerieke"
            + " nul als deler is een fout, geen leeg (RUNTIME-SEMANTICS ch. 1.4, punt 4)";

    /** The handle of the {@code Waarde} that gives {@code vondst} its uitkering. */
    private static org.kie.api.runtime.rule.FactHandle uitkering(KieSession sessie, Object vondst) {
        List<org.kie.api.runtime.rule.FactHandle> uit = new ArrayList<>(sessie.getFactHandles(o ->
                o instanceof dev.uchar.regelspraak.transpiler.tms.Waarde w && w.onderwerp() == vondst
                        && w.attribuut().naam().equals("uitkering")));
        assertEquals(1, uit.size(), "the Waarde of V1's uitkering");
        return uit.get(0);
    }

    /** The aanvoergeld {@code f} derives, per label of {@code labels}, as "J1 322.5". */
    private static List<String> aanvoergeld(Fixpunt f, Map<String, Object> labels) {
        List<String> uit = new ArrayList<>();
        labels.forEach((label, boon) -> {
            for (Fixpunt.AfgeleideWaarde w : f.waarden())
                if (w.object() == boon && w.attribuut().equals("aanvoergeld"))
                    uit.add(label + " " + ((BigDecimal) w.waarde()).stripTrailingZeros().toPlainString());
        });
        uit.sort(null);
        return uit;
    }

    // ---------------- helpers ----------------

    /** The heffing {@code f} derives for {@code jutter}, as a plain number, or null. */
    private static String heffing(Fixpunt f, Object jutter) {
        for (Fixpunt.AfgeleideWaarde w : f.waarden())
            if (w.object() == jutter && w.attribuut().equals("heffing"))
                return ((BigDecimal) w.waarde()).stripTrailingZeros().toPlainString();
        return null;
    }

    /** J1, and V1 of {@code gewichtV1} and V2 of 4, each found by J1. */
    static Map<String, Object> waardebonen(DroolsBackend.Build b, String gewichtV1) throws Exception {
        Map<String, Object> bonen = new LinkedHashMap<>();
        Object j1 = TmsDocumenten.boon(b.beanLoader(), "Jutter", Map.of(), List.of());
        bonen.put("J1", j1);
        bonen.put("V1", TmsDocumenten.boon(b.beanLoader(), "Vondst", Map.of("gewicht", gewichtV1), List.of()));
        bonen.put("V2", TmsDocumenten.boon(b.beanLoader(), "Vondst", Map.of("gewicht", "4"), List.of()));
        for (String v : List.of("V1", "V2")) zetRol(bonen.get(v), "vinder", j1);
        return bonen;
    }

    /** The reading of a fresh session of {@code b} over the same beans, which hold the input alone. */
    private static List<String> verseLezing(DroolsBackend.Build b, Map<String, Object> bonen) {
        KieSession vers = b.kieBase().newKieSession();
        try {
            DroolsBackend.voegIn(b, vers, new Invoer().beans(bonen.values()));
            assertNull(DroolsBackend.vuur(b, vers).geenFixpunt(), "fresh fire");
            return beschrijf(DroolsBackend.lees(b, vers), bonen);
        } finally {
            vers.dispose();
        }
    }

    /** {@code f} as sorted lines over the labels of {@code bonen}: "V1.uitkering = 20", "V1 is zwaar", "V1: &lt;fault&gt;",
     *  "gemaakt: …", "koppeling: …" and "inconsistent: …". */
    static List<String> beschrijf(Fixpunt f, Map<String, Object> bonen) {
        Map<Object, String> label = new IdentityHashMap<>();
        bonen.forEach((l, boon) -> label.put(boon, l));
        List<String> uit = new ArrayList<>();
        for (Fixpunt.AfgeleideWaarde w : f.waarden())
            uit.add(label.get(w.object()) + "." + w.attribuut() + " = "
                    + (w.waarde() instanceof BigDecimal d ? d.stripTrailingZeros().toPlainString() : w.waarde()));
        for (Fixpunt.AfgeleidKenmerk k : f.kenmerken()) uit.add(label.get(k.object()) + " is " + k.kenmerk());
        for (Fixpunt.GemaaktObject o : f.objecten())
            uit.add("gemaakt: " + o.creatie() + " voor " + label.get(o.onderwerp()));
        for (Fixpunt.GemaakteKoppeling k : f.koppelingen())
            uit.add("koppeling: " + k.feittype() + " " + label.get(k.eerste()) + " " + label.get(k.tweede()));
        for (Fout fout : f.fouten()) uit.add(label.get(fout.onderwerp()) + ": " + fout.melding());
        for (Object i : f.inconsistenties()) uit.add("inconsistent: " + i);
        uit.sort(null);
        return uit;
    }

    static void zet(Object boon, String attribuut, String lexeem) throws Exception {
        boon.getClass().getMethod(Namen.setter(attribuut), BigDecimal.class).invoke(boon, new BigDecimal(lexeem));
    }

    static void zetRol(Object boon, String rol, Object doel) throws Exception {
        for (java.lang.reflect.Method m : boon.getClass().getMethods())
            if (m.getName().equals(Namen.setter(rol)) && m.getParameterCount() == 1) {
                m.invoke(boon, doel);
                return;
            }
        throw new AssertionError("geen setter voor " + rol + " op " + boon.getClass());
    }

    /** Work that may throw, run with the bean loader of the build as the thread's context class loader. */
    interface Werk {
        void doe() throws Exception;
    }

    static void inSessie(DroolsBackend.Build b, Werk werk) throws Exception {
        ClassLoader vorige = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(b.beanLoader());
        try {
            werk.doe();
        } finally {
            Thread.currentThread().setContextClassLoader(vorige);
        }
    }

    private static void assertGetal(String verwacht, Object werkelijk, String wat) {
        assertTrue(werkelijk instanceof BigDecimal w && new BigDecimal(verwacht).compareTo(w) == 0,
                () -> wat + ": verwacht " + verwacht + ", werkelijk " + werkelijk);
    }
}
