package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;

import dev.uchar.regelspraak.runtime.Breuk;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocumenten.GEHEEL;
import static dev.uchar.regelspraak.cases.TmsDocumenten.TELLING;
import static dev.uchar.regelspraak.cases.TmsDocumenten.aggregatie;
import static dev.uchar.regelspraak.cases.TmsDocumenten.attribuut;
import static dev.uchar.regelspraak.cases.TmsDocumenten.binair;
import static dev.uchar.regelspraak.cases.TmsDocumenten.bronrol;
import static dev.uchar.regelspraak.cases.TmsDocumenten.check;
import static dev.uchar.regelspraak.cases.TmsDocumenten.consistent;
import static dev.uchar.regelspraak.cases.TmsDocumenten.feittype;
import static dev.uchar.regelspraak.cases.TmsDocumenten.feittypeEenNaarVeel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.getal;
import static dev.uchar.regelspraak.cases.TmsDocumenten.instantieMetRollen;
import static dev.uchar.regelspraak.cases.TmsDocumenten.kenmerkregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.lezing;
import static dev.uchar.regelspraak.cases.TmsDocumenten.padlezing;
import static dev.uchar.regelspraak.cases.TmsDocumenten.rekenregel;
import static dev.uchar.regelspraak.cases.TmsDocumenten.rolbinding;
import static dev.uchar.regelspraak.cases.TmsDocumenten.samengesteld;
import static dev.uchar.regelspraak.cases.TmsDocumenten.vergelijking;
import static dev.uchar.regelspraak.cases.TmsDocumenten.verwacht;
import static dev.uchar.regelspraak.cases.TmsDocumenten.waarde;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A fault in the subselection of a fold, on the exec-model and the DRL-text branch, arises where the evaluation of a
 * form reaches the fold, and nowhere else. The reference interpreter evaluates a rule's condition first and its value
 * only where the rule fires, a composite condition only up to the member after which its verdict stands
 * (RUNTIME-SEMANTICS ch. 5.10), and a fold's filter only where it evaluates the fold, per member of that subject's
 * range (ch. 3.4a); a filter that divides by a numeric zero there faults the evaluation (ch. 1.4, point 4). Both
 * branches tested the filter on the element's pattern, which Drools tests for every element of the type as it is
 * inserted, so they faulted where a rule's condition did not hold, where a composite had settled before the fold, and
 * for an element in no fold's range. A filter that can fault ({@code kern.Faalbaar}) now goes on the element bound
 * again in the source of the accumulate, after the join to the subject and the walk of a path, where it is tested per
 * tuple that reaches the accumulate.
 *
 * <p>The corpus golden {@code jutterswet-filterwacht} carries the two forms of the card, a computation under "indien
 * hij actief is" and a characteristic whose composite tests "hij is actief" before the fold, and gates 5, 6 and 12
 * replay it. This class adds what the golden cannot carry and the other places where a fold stands under a condition:
 *
 * <ul>
 *   <li>a Jutter that is not actief beside one that is, in one session, and a Vondst in no Jutter's range: the ASP
 *       backend faults on both where the reference interpreter answers (its guard on the division in the filter does
 *       not join the member to the fold's subject; measured 2026-10-09, reported with the card), so no golden can
 *       hold them;</li>
 *   <li>a fold inside the value of a rule with a condition, "(het aantal …) plus 1, indien hij actief is";</li>
 *   <li>a fold in the condition of an object creation and one in its initialisation;</li>
 *   <li>the filter of a quantifier fold under three quantifiers, a matrix that divides, which the DRL-text branch
 *       wrote on the element pattern as well, and a member of a path's range that the path does not reach.</li>
 * </ul>
 *
 * <p>Every row runs on both branches. A row where the fold is reached faults on each branch in its own sentence: the
 * exec-model branch's division named after the rule; the DRL-text branch's exception for the constraint that holds the
 * filter, with the division in its cause chain. That exception no longer names a rule: Drools names none for a
 * constraint on a pattern bound from a declaration in the source of an accumulate, where before it named every rule
 * whose accumulate shared the element pattern's constraint (measured 2026-10-09).
 *
 * <p>The reference interpreter's runner gave every outcome pinned here, on documents of the same forms: the RegelSpraak
 * frontend's, written from the sentences in the method comments (its filter is a composite of one member, here the
 * comparison itself), and for the path and the matrix that frontend's document with the filter or the matrix put in by
 * hand (measured 2026-10-09). The ASP runner agreed except where its guard on the division does not join the member to
 * the fold's subject (the two rows above, and the boot the path does not reach) and in the matrix row's second case,
 * where it refuses 100 divided by 3 as a non-exact division.
 *
 * <p><b>Red before the change</b> (measured 2026-10-09 on the branch's base, {@code abc71db2}, this class through
 * Maven, each branch also through the CLI on the same documents as the RegelSpraak frontend writes them): every row that
 * pins "gespeeld" failed, the exec-model branch with "DelingDoorNul: deling door nul in de regel "&lt;rule&gt;": 9
 * gedeeld door 0 — …" (4 for the path), except the matrix row, which it played, and the DRL-text branch with
 * "ConstraintEvaluationException: Error evaluating constraint '(rsAggVergelijk(rsGedeeld(gewicht, deelnemers), …' in
 * [Rule "&lt;rule&gt;" …]" (for the matrix "'rsAggVergelijk(gewicht, rsGedeeld(new java.math.BigDecimal("100"),
 * deelnemers), …'"). The rows where the fold is reached were green and pin what stays. Mutants, each caught by the
 * path row or a quantifier row: the element's tests before the path's {@code exists} in the exec-model branch
 * ({@code accBron}) and in the DRL-text branch, and the quantifier fold of the DRL-text branch without the element
 * bound again, for the subselection and for the matrix.
 *
 * <p>The rows from "the order of an alle composite" on came with the first fix round: the order of a composite's
 * members around a fold, also beside a member that reads through a role and in the condition of a fact creation's
 * anchor; the order of a rule's value and condition on the DRL-text branch; the DRL-text branch's companion under a
 * composite whose members moved; a divisor another rule writes on the element; a defaults group; and the operations of
 * {@code kern.Faalbaar} other than the division. Their outcomes are the reference interpreter's on the RegelSpraak
 * frontend's documents of the same forms (every order of the composite's members and of the rules included; measured
 * 2026-10-09); the ASP runner agreed except on the lower default (it faults where the higher one decides), on the object
 * creation (it does not ground that program) and where it does not translate a form. <b>Red before the round</b>
 * (measured 2026-10-09 with the transpilers of {@code 9482ec1d}, this class through Maven): 8 of its 10 rows failed; the
 * companion row, a guard of the move that the code before it passed, and the control of the defaults group passed. The
 * composite: 56 of the 144 cells, among them {@code [A, F, D, K] J3 EXEC_MODEL: verwacht gespeeld, kreeg DelingDoorNul:
 * … "drukte": 5 gedeeld door 0 …} (the division of a member after the fold that settled the composite) and
 * {@code [F, A, D, K] J1 DRL_TEXT: verwacht deling rsGedeeld(gewicht, deelnemers), kreeg gespeeld} (the fold not
 * evaluated after "hij is actief" did not hold). The fold of dates first, the fold before the member that reads through
 * a role, the fold before "de Haven veilig is": {@code gespeeld} where the division faults. The fold in the value under
 * a fold in the condition: the DRL-text branch's {@code ConstraintEvaluationException … rsGedeeld(gewicht, deelnemers)
 * …} on J1. The divisor written on the element: the DRL-text branch's {@code attribuut zwaarte: verwacht 2, werkelijk 0}
 * and, for the quantifier fold, {@code kenmerk druk: verwacht aanwezig, werkelijk afwezig}, in both orders (the first
 * form of the change put the subselection on the element bound again from a pattern with its join alone, which passed
 * no update on; before that change, with the test on the element's pattern, the branch played this). The defaults
 * group: {@code DelingDoorNul: deling door nul in de regel "defaults A:Jutter.zwaarte": 9 gedeeld door 0 …}. The other
 * operations: every branch that lowers the form faulted where J1 is not actief, the exec-model branch's
 * {@code ConstraintEvaluationException: Error evaluating constraint 'later' …} for the day-kind step. Mutants, each
 * caught by its row: the companion fed the moved condition's inline ({@code inconsistent (zwaarte @ J5)}), the element
 * pattern without its always-true call in a fold and in a quantifier fold, and the value's clauses before the
 * condition's in a rule and in an object creation.
 *
 * <p>The second fix round gave member F of the composite a divisor of its own, the ploeg, where K divides by the
 * deelnemers. With one filter for both, J1 faulted in F and in K with the same division, so a branch that evaluated the
 * two folds in the other order gave the same answer in all 144 cells. Now J1 faults in F, D and K each in a division of
 * its own, and a fourth case, J4, makes F fault where K does not hold, so the order of the two folds decides whether
 * the composite faults there (192 cells). The round also added the row of a composite under another quantifier than
 * alle whose fold can fault, which the exec-model branch now refuses by name. <b>Red before the round</b> (measured
 * 2026-10-09, this class through Maven): with the transpilers of {@code 4ada319d} that row failed in all five of its
 * forms with the exec-model branch's {@code DelingDoorNul: deling door nul in de regel "drukte": 9 gedeeld door 0 …},
 * where the reference interpreter answers; its control and the DRL-text branch's refusals passed. A mutant that swaps
 * the first two folds of an alle composite before it lowers the members, in either branch, passed the composite row
 * of the first fix round, and fails the new one in 36 of that branch's 96 cells: every J4 cell, and every J1 cell whose
 * order starts with F or K, among them {@code [F, A, D, K] J1 EXEC_MODEL: verwacht … 10 gedeeld door 0 …, kreeg … 9
 * gedeeld door 0 …} and {@code [A, D, K, F] J4 DRL_TEXT: verwacht gespeeld, kreeg deling rsGedeeld(gewicht, ploeg)}.
 */
class FilterfoutWitnessTest {

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path FILTERWACHT =
            Path.of("..", "..", "corpus", "golden", "jutterswet-filterwacht", "ir.json");

    private static final DroolsBackend.Branch EXEC = DroolsBackend.Branch.EXEC_MODEL;
    private static final DroolsBackend.Branch DRL = DroolsBackend.Branch.DRL_TEXT;

    /** Both branches play {@code zaak} on {@code ir} with its expectations. */
    private static void gespeeld(Path ir, JsonNode zaak) throws Exception {
        assertEquals("gespeeld", TmsFoutWitnessTest.speel(ir, zaak, EXEC), "exec-model");
        assertEquals("gespeeld", TmsFoutWitnessTest.speel(ir, zaak, DRL), "drl-tekst");
    }

    /** Both branches fault on {@code zaak}, where rule {@code regel} reaches its fold and the filter divides
     *  {@code deeltal} by zero for a member: the exec-model branch with its division named after the rule, the DRL-text
     *  branch with Drools' exception for the constraint that holds the filter, the division in its cause chain. */
    private static void faalt(Path ir, JsonNode zaak, String regel, String deeltal) throws Exception {
        assertEquals("DelingDoorNul: deling door nul in de regel \"" + regel + "\": " + deeltal + " gedeeld door 0 — een"
                + " numerieke nul als deler is een fout, geen leeg (RUNTIME-SEMANTICS ch. 1.4, punt 4)",
                TmsFoutWitnessTest.speel(ir, zaak, EXEC), "exec-model");
        RuntimeException drl = assertThrows(RuntimeException.class, () -> CaseRunner.speelCase(ir, zaak, DRL));
        assertEquals("ConstraintEvaluationException", drl.getClass().getSimpleName(), () -> "drl-tekst: " + drl);
        assertTrue(drl.getMessage().startsWith("Error evaluating constraint '") && drl.getMessage().contains("rsGedeeld("),
                () -> "drl-tekst: " + drl.getMessage());
        assertTrue(deling(drl), () -> "drl-tekst zonder de deling in de oorzaken: " + drl);
    }

    /** Whether {@code t} or a cause of it is the division by a numeric zero. */
    private static boolean deling(Throwable t) {
        for (Throwable o = t; o != null; o = o.getCause() == o ? null : o.getCause())
            if (o instanceof Breuk.DelingDoorNul) return true;
        return false;
    }

    private static String vondst(String label, String gewicht, String deelnemers) {
        return instantieMetRollen(label, "Vondst", Map.of("gewicht", gewicht, "deelnemers", deelnemers), List.of(),
                Map.of());
    }

    private static String jutter(String label, boolean actief, List<String> vondsten) {
        return instantieMetRollen(label, "Jutter", Map.of(), actief ? List.of("actief") : List.of(),
                Map.of("vondsten", vondsten));
    }

    /** J1 with V1 (gewicht 10, deelnemers 2: 5, heavy) and V2 (gewicht 9, deelnemers 0: the filter divides 9 by 0). */
    private static List<String> j1(boolean actief) {
        return List.of(jutter("J1", actief, List.of("V1", "V2")), vondst("V1", "10", "2"), vondst("V2", "9", "0"));
    }

    /** J2, actief, with V3 (10 by 2: 5, heavy, gewicht above 5) and V4 (3 by 3: 1, not heavy); J3, actief, with V5
     *  (4 by 4: 1, not heavy). No member divides by 0. */
    private static List<String> j2j3() {
        return List.of(jutter("J2", true, List.of("V3", "V4")), vondst("V3", "10", "2"), vondst("V4", "3", "3"),
                jutter("J3", true, List.of("V5")), vondst("V5", "4", "4"));
    }

    private static List<String> samen(List<String> a, List<String> b) {
        List<String> uit = new ArrayList<>(a);
        uit.addAll(b);
        return uit;
    }

    // ---------------- the golden's document, with what the golden cannot carry ----------------

    /** The golden's first case: J1 is not actief, so neither rule reaches its fold, and the composite of "drukte"
     *  stops after its first member. "vondstental" counts the same range without a filter. */
    @Test void een_vouw_onder_een_voorwaarde_die_niet_geldt_faalt_niet() throws Exception {
        gespeeld(FILTERWACHT, TmsDocumenten.zaak(j1(false), List.of(waarde("J1", "vondstental", "2"),
                waarde("J1", "zware vondsten", null), verwacht("J1", "druk", false), consistent())));
    }

    /** J1 as above beside J2, actief, in one session: the folds of J2 read V3 and V4, never J1's V2. The reference
     *  interpreter answers this; the ASP runner reports viol("division by numeric zero","V2"). */
    @Test void naast_een_actieve_jutter_rekent_alleen_diens_vouw() throws Exception {
        gespeeld(FILTERWACHT, TmsDocumenten.zaak(samen(j1(false), j2j3()), List.of(
                waarde("J1", "zware vondsten", null), verwacht("J1", "druk", false),
                waarde("J2", "zware vondsten", "1"), verwacht("J2", "druk", true),
                waarde("J3", "zware vondsten", "0"), verwacht("J3", "druk", false), consistent())));
    }

    /** V9 (9 by 0) is linked to no Jutter, so no fold has it in its range. The ASP runner reports
     *  viol("division by numeric zero","V9") here too. */
    @Test void een_lid_buiten_elk_bereik_wordt_niet_gefilterd() throws Exception {
        gespeeld(FILTERWACHT, TmsDocumenten.zaak(samen(j2j3(), List.of(vondst("V9", "9", "0"))), List.of(
                waarde("J2", "zware vondsten", "1"), verwacht("J2", "druk", true), consistent())));
    }

    /** The red case {@code vouwfilter-deelt-door-nul}: J1 actief, both rules reach the fold and divide 9 by 0. Which of
     *  the two the branch names first is the order of its evaluation, so this row asks for the division only. */
    @Test void waar_de_vouw_bereikt_wordt_blijft_de_deling_een_fout() throws Exception {
        JsonNode zaak = TmsDocumenten.zaak(j1(true), List.of(consistent()));
        String exec = TmsFoutWitnessTest.speel(FILTERWACHT, zaak, EXEC);
        assertTrue(exec.startsWith("DelingDoorNul: deling door nul in de regel \"")
                && exec.contains("\": 9 gedeeld door 0 — een numerieke nul als deler is een fout"), "exec-model: " + exec);
        RuntimeException drl = assertThrows(RuntimeException.class, () -> CaseRunner.speelCase(FILTERWACHT, zaak, DRL));
        assertTrue(drl.getMessage().startsWith("Error evaluating constraint '(rsAggVergelijk(rsGedeeld(gewicht, deelnemers)")
                && deling(drl), () -> "drl-tekst: " + drl);
    }

    // ---------------- the other places where a fold stands under a condition ----------------

    /** A date of the day precision. */
    static final String DATUM = "{\"node\":\"Date\",\"precision\":\"day\"}";

    static final String MODEL = TmsDocumenten.model(List.of(
            TmsDocumenten.objecttype("Jutter", List.of(attribuut("zwaarte", GEHEEL), attribuut("meldtelling", GEHEEL),
                    attribuut("saldo", GEHEEL), attribuut("noemer", GEHEEL), attribuut("peildatum", DATUM)),
                    List.of("actief", "druk", "beladen", "ontlast")),
            TmsDocumenten.objecttype("Vondst", List.of(attribuut("gewicht", GEHEEL), attribuut("deelnemers", GEHEEL),
                    attribuut("vinddatum", DATUM), attribuut("ploeg", GEHEEL)), List.of()),
            TmsDocumenten.objecttype("Melding", List.of(attribuut("telling", GEHEEL)), List.of())),
            List.of(feittypeEenNaarVeel("aanmelding", "aanmelder", "Jutter", "vondst", "Vondst", "vondsten"),
                    feittype("JutterHeeftMelding", "melder", "Jutter", "melding", "Melding")));

    /** "zijn gewicht gedeeld door zijn deelnemers is groter dan 2", the quotient typed as a quotient derives its type
     *  (WF-091). */
    static final String FILTER = vergelijking("greater", "{\"node\":\"Binary\",\"op\":\"dividedBy\",\"left\":"
            + lezing("gewicht", "Vondst") + ",\"right\":" + lezing("deelnemers", "Vondst") + ",\"type\":{\"node\":"
            + "\"Number\",\"whole\":false,\"decimals\":0,\"sign\":\"unspecified\"},\"source\":{\"snippet\":\"zijn gewicht"
            + " gedeeld door zijn deelnemers\"}}", getal("2"));

    /** "het aantal vondsten van de Jutter die aan de volgende voorwaarde voldoen: {@link #FILTER}". */
    static String vouw() {
        return vouw(FILTER);
    }

    /** "het aantal vondsten van de Jutter die aan de volgende voorwaarde voldoen: {@code filter}". */
    static String vouw(String filter) {
        return aggregatie("count", "{\"node\":\"AttributeReference\",\"attribute\":\"vondsten\",\"subjectType\":"
                + "\"Jutter\",\"source\":{\"snippet\":\"vondsten van de Jutter\"}}", "Vondst", bronrol("aanmelding", "vondst"),
                filter, "null", TELLING, "het aantal vondsten van de Jutter die aan de volgende voorwaarde voldoen");
    }

    /** "Een Jutter heeft een melding met telling gelijk aan {@code telling}, indien {@code voorwaarde}". */
    static String creatie(String telling, String voorwaarde) {
        return "{\"node\":\"ObjectCreation\",\"name\":\"meld\",\"versions\":[{\"createdType\":\"Melding\",\"subject\":"
                + "\"Jutter\",\"initialisations\":[{\"node\":\"MemberInit\",\"member\":\"telling\",\"value\":" + telling
                + ",\"characteristic\":false}],\"condition\":" + voorwaarde + ",\"validity\":{\"node\":\"Validity\"},"
                + "\"connectingRole\":" + rolbinding("JutterHeeftMelding", "melding") + "}]}";
    }

    /** "De meldtelling van een Jutter moet berekend worden als de telling van de melding van de Jutter". */
    static final String MELDTELLING = rekenregel("meldtelling", "BEREKENING", "Jutter", "meldtelling",
            padlezing("telling", "Melding", "JutterHeeftMelding", "melding"), "null");

    /** A quantifier fold over the vondsten of the Jutter under {@code kwantor}, with the subselection {@code filter}
     *  ("null" for none) and the matrix {@code toets}. */
    static String kwantorvouw(String kwantor, String filter, String toets) {
        return "{\"node\":\"QuantifierFold\",\"quantifier\":\"" + kwantor + "\"" + (kwantor.equals("atLeast")
                ? ",\"threshold\":1" : "") + ",\"elementType\":\"Vondst\"" + (filter.equals("null") ? ""
                : ",\"filter\":" + filter) + ",\"test\":" + toets + ",\"source\":{\"snippet\":\"het gewicht van "
                + kwantor + " vondsten\"},\"sourceRole\":" + rolbinding("aanmelding", "vondst") + "}";
    }

    /** "{@code kenmerk} indien hij aan alle volgende voorwaarden voldoet: hij is actief; {@code kwantorvouw}". */
    static String kwantorregel(String naam, String kenmerk, String kwantorvouw) {
        return kenmerkregel(naam, "Jutter", kenmerk, samengesteld("all", 0, List.of(check("actief", false),
                kwantorvouw)));
    }

    /** The document of {@link #MODEL} with {@code regels} and object creations {@code creaties}. */
    static Path document(List<String> regels, List<String> creaties) throws Exception {
        String ir = "{\"format\":\"uchar-ir-v1\",\"model\":" + MODEL + ",\"rules\":{\"rules\":[" + String.join(",", regels)
                + "],\"dayKindDefinitions\":[],\"objectCreations\":[" + String.join(",", creaties) + "],\"allocations\":[],"
                + "\"consistencies\":[],\"uniquenesses\":[],\"factCreations\":[],\"startPointDeterminations\":[],"
                + "\"ruleGroups\":[],\"prioritizedDefaults\":[]},\"diagnostics\":[]}";
        Path tmp = Files.createTempFile("filterfout-getuige", ".json");
        tmp.toFile().deleteOnExit();
        return Files.writeString(tmp, ir);
    }

    /** "De zwaarte van een Jutter moet berekend worden als (het aantal vondsten … groter dan 2) plus 1, indien hij
     *  actief is": the fold is an operand of the value. J2: 1 plus 1. */
    @Test void een_vouw_in_de_waarde_van_een_regel_met_voorwaarde() throws Exception {
        Path ir = document(List.of(rekenregel("zwaarte", "BEREKENING", "Jutter", "zwaarte",
                binair("plus", vouw(), getal("1")), check("actief", false))), List.of());
        gespeeld(ir, TmsDocumenten.zaak(j1(false), List.of(waarde("J1", "zwaarte", null), consistent())));
        gespeeld(ir, TmsDocumenten.zaak(j2j3(), List.of(waarde("J2", "zwaarte", "2"), waarde("J3", "zwaarte", "1"),
                consistent())));
        faalt(ir, TmsDocumenten.zaak(j1(true), List.of(consistent())), "zwaarte", "9");
    }

    /** "Een Jutter heeft een melding met telling gelijk aan 1 indien hij aan alle volgende voorwaarden voldoet: hij is
     *  actief; het aantal vondsten … is groter dan 0", read back through "de telling van de melding van de Jutter". J2
     *  has one heavy vondst and gets the melding; J3 has none. */
    @Test void een_vouw_in_de_voorwaarde_van_een_creatie() throws Exception {
        Path ir = document(List.of(MELDTELLING), List.of(creatie(getal("1"), samengesteld("all", 0, List.of(
                check("actief", false), vergelijking("greater", vouw(), getal("0")))))));
        gespeeld(ir, TmsDocumenten.zaak(j1(false), List.of(waarde("J1", "meldtelling", null), consistent())));
        gespeeld(ir, TmsDocumenten.zaak(j2j3(), List.of(waarde("J2", "meldtelling", "1"),
                waarde("J3", "meldtelling", null), consistent())));
        faalt(ir, TmsDocumenten.zaak(j1(true), List.of(consistent())), "meld", "9");
    }

    /** "Een Jutter heeft een melding met telling gelijk aan het aantal vondsten … indien hij actief is": the fold is
     *  the creation's initialisation. J2 counts 1, J3 0. */
    @Test void een_vouw_in_de_initialisatie_van_een_creatie() throws Exception {
        Path ir = document(List.of(MELDTELLING), List.of(creatie(vouw(), check("actief", false))));
        gespeeld(ir, TmsDocumenten.zaak(j1(false), List.of(waarde("J1", "meldtelling", null), consistent())));
        gespeeld(ir, TmsDocumenten.zaak(j2j3(), List.of(waarde("J2", "meldtelling", "1"),
                waarde("J3", "meldtelling", "0"), consistent())));
        faalt(ir, TmsDocumenten.zaak(j1(true), List.of(consistent())), "meld", "9");
    }

    /** The filter of a quantifier fold, after "hij is actief" in a composite, under ten minste één (druk), alle
     *  (beladen) and geen (ontlast) of "het gewicht … is groter dan 5". J2's contributors are V3 alone (gewicht 10):
     *  druk and beladen, not ontlast. J3 has none: alle and geen hold vacuously, ten minste één does not. Each
     *  document holds one rule, so the fault names it. */
    @Test void een_vouw_in_het_filter_van_een_kwantorvouw() throws Exception {
        for (String[] r : new String[][] {{"drukte", "druk", "atLeast", "true", "false"},
                {"belading", "beladen", "all", "true", "true"}, {"ontlasting", "ontlast", "none", "false", "true"}}) {
            Path ir = document(List.of(kwantorregel(r[0], r[1], kwantorvouw(r[2], FILTER,
                    vergelijking("greater", lezing("gewicht", "Vondst"), getal("5"))))), List.of());
            gespeeld(ir, TmsDocumenten.zaak(j1(false), List.of(verwacht("J1", r[1], false), consistent())));
            gespeeld(ir, TmsDocumenten.zaak(j2j3(), List.of(verwacht("J2", r[1], Boolean.parseBoolean(r[3])),
                    verwacht("J3", r[1], Boolean.parseBoolean(r[4])), consistent())));
            faalt(ir, TmsDocumenten.zaak(j1(true), List.of(consistent())), r[0], "9");
        }
    }

    /** A matrix that divides, "het gewicht van ten minste één van de vondsten van de Jutter is groter dan 100 gedeeld
     *  door zijn deelnemers", after "hij is actief": the DRL-text branch wrote the matrix on the element pattern of its
     *  accumulate as well, the exec-model branch asks it after the accumulate. J4's V6 (60 above 100 by 2) holds; the
     *  members of J2 and J3 do not, and divide by no 0. Where J1 is actief, V1 (10 above 50) does not hold, so the fold
     *  goes on to V2, 100 by 0. */
    @Test void een_deling_in_de_matrix_van_een_kwantorvouw() throws Exception {
        String toets = vergelijking("greater", lezing("gewicht", "Vondst"), "{\"node\":\"Binary\",\"op\":\"dividedBy\","
                + "\"left\":" + getal("100") + ",\"right\":" + lezing("deelnemers", "Vondst") + ",\"type\":{\"node\":"
                + "\"Number\",\"whole\":false,\"decimals\":0,\"sign\":\"unspecified\"},\"source\":{\"snippet\":\"100"
                + " gedeeld door zijn deelnemers\"}}");
        Path ir = document(List.of(kwantorregel("drukte", "druk", kwantorvouw("atLeast", "null", toets))), List.of());
        gespeeld(ir, TmsDocumenten.zaak(j1(false), List.of(verwacht("J1", "druk", false), consistent())));
        gespeeld(ir, TmsDocumenten.zaak(samen(j2j3(), List.of(jutter("J4", true, List.of("V6")),
                vondst("V6", "60", "2"))), List.of(verwacht("J2", "druk", false), verwacht("J3", "druk", false),
                verwacht("J4", "druk", true), consistent())));
        faalt(ir, TmsDocumenten.zaak(j1(true), List.of(consistent())), "drukte", "100");
    }

    /** The bootlengte of {@code TmsFilterfoutWitnessTest}: the sum of the lengte of all boten of all jutters of the
     *  Haven whose lengte divided by their breedte is greater than 2, or 0, over a path that walks back to the jutters
     *  and forward to their boot. H1's jutters J1 and J2 share B1 (9 by 3): 9, once. B9 (4 by 0) is the boot of no
     *  jutter of H1; where J3 of H1 uses it, the fold reaches it and divides 4 by 0. */
    @Test void een_lid_dat_het_pad_niet_bereikt_wordt_niet_gefilterd() throws Exception {
        Path ir = TmsDocumenten.document(TmsFilterfoutWitnessTest.HAVENMODEL, TmsFilterfoutWitnessTest.BOOTLENGTE);
        String b1 = instantieMetRollen("B1", "Boot", Map.of("lengte", "9", "breedte", "3"), List.of(), Map.of());
        String b9 = instantieMetRollen("B9", "Boot", Map.of("lengte", "4", "breedte", "0"), List.of(), Map.of());
        String j1 = instantieMetRollen("J1", "Jutter", Map.of(), List.of(), Map.of("boot", List.of("B1")));
        String j2 = instantieMetRollen("J2", "Jutter", Map.of(), List.of(), Map.of("boot", List.of("B1")));
        gespeeld(ir, TmsDocumenten.zaak(List.of(instantieMetRollen("H1", "Haven", Map.of(), List.of(),
                Map.of("jutters", List.of("J1", "J2"))), j1, j2, b1, b9), List.of(waarde("H1", "bootlengte", "9"),
                consistent())));
        String j3 = instantieMetRollen("J3", "Jutter", Map.of(), List.of(), Map.of("boot", List.of("B9")));
        faalt(ir, TmsDocumenten.zaak(List.of(instantieMetRollen("H1", "Haven", Map.of(), List.of(),
                Map.of("jutters", List.of("J1", "J2", "J3"))), j1, j2, j3, b1, b9), List.of(consistent())),
                "bootlengte", "4");
    }

    // ---------------- the order of an "alle" composite around a fold ----------------

    /** "zijn gewicht gedeeld door zijn ploeg is groter dan 2": the subselection of member F, which divides by another
     *  attribute than {@link #FILTER} does, so that a case can make one of the two folds fault and not the other, and
     *  each names a division of its own. */
    static final String PLOEGFILTER = vergelijking("greater", "{\"node\":\"Binary\",\"op\":\"dividedBy\",\"left\":"
            + lezing("gewicht", "Vondst") + ",\"right\":" + lezing("ploeg", "Vondst") + ",\"type\":{\"node\":"
            + "\"Number\",\"whole\":false,\"decimals\":0,\"sign\":\"unspecified\"},\"source\":{\"snippet\":"
            + "\"zijn gewicht gedeeld door zijn ploeg\"}}", getal("2"));

    /** The members of "Een Jutter is druk indien hij aan alle volgende voorwaarden voldoet", by letter: A "hij is
     *  actief"; F "{@link #vouw(String)} over {@link #PLOEGFILTER} is groter dan 0", a fold whose subselection divides
     *  by the ploeg; D "zijn saldo gedeeld door zijn noemer is groter dan 1"; K "het gewicht van ten minste één van de
     *  vondsten van de Jutter die {@link #FILTER} is groter dan 5", a quantifier fold whose subselection divides by the
     *  deelnemers. */
    static String lid(char letter) {
        return switch (letter) {
            case 'A' -> check("actief", false);
            case 'F' -> vergelijking("greater", vouw(PLOEGFILTER), getal("0"));
            case 'D' -> vergelijking("greater", TmsFilterfoutWitnessTest.quotient(lezing("saldo", "Jutter"),
                    lezing("noemer", "Jutter")), getal("1"));
            case 'K' -> kwantorvouw("atLeast", FILTER, vergelijking("greater", lezing("gewicht", "Vondst"), getal("5")));
            default -> throw new IllegalArgumentException("geen lid " + letter);
        };
    }

    /** One case of the composite: the Jutter it is about, its instances, and per member its verdict there: "W" it
     *  holds, "O" it does not, or the dividend of the division by 0 it reaches. */
    private record Geval(String label, List<String> instanties, Map<Character, String> oordeel) {}

    private static String jutter(String label, boolean actief, String saldo, String noemer, List<String> vondsten) {
        return instantieMetRollen(label, "Jutter", Map.of("saldo", saldo, "noemer", noemer),
                actief ? List.of("actief") : List.of(), Map.of("vondsten", vondsten));
    }

    private static String vondst(String label, String gewicht, String deelnemers, String ploeg) {
        return instantieMetRollen(label, "Vondst", Map.of("gewicht", gewicht, "deelnemers", deelnemers, "ploeg", ploeg),
                List.of(), Map.of());
    }

    /** J1 is not actief, and every other member faults in a division of its own: F at V1, 10 by its ploeg 0; D, 5 by
     *  the noemer 0; K at V2, 9 by its deelnemers 0 (the subselection is settled for every vondst before the matrix,
     *  ch. 3.9a, so V1, which holds the matrix, does not stop it). J2 is actief and every member holds: V3, 10 by 2 in
     *  both quotients and gewicht 10 above 5, counts for F and for K, and 6 by 2 is 3. J3 is actief, its one vondst V5,
     *  2 by 2 in both quotients, counts for neither, so F and K do not hold, and its noemer is 0. J4 is actief and D
     *  holds; its one vondst V7 makes F fault, 10 by its ploeg 0, and does not count for K, 10 by its deelnemers 5
     *  being 2: whether the composite faults or answers there follows the order of F and K alone. */
    static final List<Geval> GEVALLEN = List.of(
            new Geval("J1", List.of(jutter("J1", false, "5", "0", List.of("V1", "V2")), vondst("V1", "10", "2", "0"),
                    vondst("V2", "9", "0", "3")), Map.of('A', "O", 'F', "10", 'D', "5", 'K', "9")),
            new Geval("J2", List.of(jutter("J2", true, "6", "2", List.of("V3", "V4")), vondst("V3", "10", "2", "2"),
                    vondst("V4", "3", "3", "3")), Map.of('A', "W", 'F', "W", 'D', "W", 'K', "W")),
            new Geval("J3", List.of(jutter("J3", true, "5", "0", List.of("V5")), vondst("V5", "2", "2", "2")),
                    Map.of('A', "W", 'F', "O", 'D', "5", 'K', "O")),
            new Geval("J4", List.of(jutter("J4", true, "6", "2", List.of("V7")), vondst("V7", "10", "5", "0")),
                    Map.of('A', "W", 'F', "10", 'D', "W", 'K', "O")));

    /** The composite's verdict on {@code g} by RUNTIME-SEMANTICS ch. 5.10 and 5.11: the members are read in
     *  {@code volgorde}, and the first that does not hold or faults decides, a fault ending the evaluation; "W" where
     *  every member holds, "O" where one does not, and for a fault the member's letter and its dividend ("F 10"). */
    static String verdict(List<Character> volgorde, Geval g) {
        for (char c : volgorde) {
            String o = g.oordeel().get(c);
            if (o.equals("O")) return o;
            if (!o.equals("W")) return c + " " + o;
        }
        return "W";
    }

    /** What {@code tak} gives on {@code zaak}: "gespeeld", the verdict of an expectation that failed, or the fault with
     *  its class. For the DRL-text branch, whose exception names the constraint, a division by a numeric zero in the
     *  cause chain is "deling" with the quotient of that constraint. */
    static String waarneming(Path ir, JsonNode zaak, DroolsBackend.Branch tak) throws Exception {
        try {
            String v = CaseRunner.speelCase(ir, zaak, tak);
            return v == null ? "gespeeld" : v;
        } catch (RuntimeException e) {
            if (tak == DRL && deling(e)) {
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("rsGedeeld\\([^()]*\\)")
                        .matcher(String.valueOf(e.getMessage()));
                return "deling " + (m.find() ? m.group() : "zonder quotiënt in '" + e.getMessage() + "'");
            }
            return e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    /** The quotient of each member's division as the DRL-text branch writes it in the constraint it names. */
    private static final Map<Character, String> QUOTIENT = Map.of('F', "rsGedeeld(gewicht, ploeg)",
            'D', "rsGedeeld(saldo, noemer)", 'K', "rsGedeeld(gewicht, deelnemers)");

    /** What {@code tak} must give where the verdict on rule {@code regel} is {@code verdict}: for a fault, the
     *  exec-model branch's division with its dividend, the DRL-text branch's exception with the member's quotient. */
    static String verwachting(String verdict, String regel, DroolsBackend.Branch tak) {
        if (verdict.equals("W") || verdict.equals("O")) return "gespeeld";
        if (tak == EXEC) return "DelingDoorNul: deling door nul in de regel \"" + regel + "\": " + verdict.substring(2)
                + " gedeeld door 0 — een numerieke nul als deler is een fout, geen leeg (RUNTIME-SEMANTICS ch. 1.4,"
                + " punt 4)";
        return "deling " + QUOTIENT.get(verdict.charAt(0));
    }

    /** Every order of the four members, on the four cases, on both branches: a member is evaluated only where every
     *  member before it holds, and a fault ends the evaluation (ch. 5.10, 5.11), so the verdict, and whether and which
     *  division by 0 happens, follows the order. On J1 each branch names the division of the first member, F's, D's or
     *  K's, so a branch that evaluated the two folds in the other order names the other fold's division; on J4 it
     *  faults where the order puts F before K, and answers where it puts K first. The reference interpreter's message
     *  names no division ("division by numeric zero"); it faults on J4 in each of the twelve orders with F before K and
     *  answers in each of the other twelve, and agrees with every other cell (measured 2026-10-09 on the RegelSpraak
     *  frontend's documents of all 24 orders; the ASP runner agrees cell for cell). */
    @Test void de_leden_van_een_alle_samenstelling_in_elke_volgorde() throws Exception {
        List<String> afwijkend = new ArrayList<>();
        for (List<Character> volgorde : TmsDocumenten.permutaties(List.of('A', 'F', 'D', 'K'))) {
            List<String> leden = new ArrayList<>();
            for (char c : volgorde) leden.add(lid(c));
            Path ir = document(List.of(kenmerkregel("drukte", "Jutter", "druk", samengesteld("all", 0, leden))), List.of());
            for (Geval g : GEVALLEN) {
                String verdict = verdict(volgorde, g);
                JsonNode zaak = TmsDocumenten.zaak(g.instanties(), verdict.equals("W") || verdict.equals("O")
                        ? List.of(verwacht(g.label(), "druk", verdict.equals("W")), consistent()) : List.of(consistent()));
                for (DroolsBackend.Branch tak : List.of(EXEC, DRL)) {
                    String gekregen = waarneming(ir, zaak, tak), verwacht = verwachting(verdict, "drukte", tak);
                    if (!gekregen.equals(verwacht))
                        afwijkend.add(volgorde + " " + g.label() + " " + tak + ": verwacht " + verwacht + ", kreeg " + gekregen);
                }
            }
        }
        assertEquals(List.of(), afwijkend);
    }

    /** "de telling van de melding van de Jutter is groter dan 0", a member that reads through a role, beside
     *  {@link #vouw()} in both orders: on the exec-model branch such a condition goes on the subject bound again after
     *  the join to the melding, and the members after the fold on the subject bound again from that. J1's V2 divides 9
     *  by 0 and its melding counts 0: with the fold first the composite faults, after the member it is not read. J2's V3
     *  (10 by 2) counts and its melding counts 3: druk. J3's V5 (2 by 2) does not count: not druk. */
    @Test void een_genavigeerd_lid_naast_de_vouw_in_beide_volgordes() throws Exception {
        String genavigeerd = vergelijking("greater", padlezing("telling", "Melding", "JutterHeeftMelding", "melding"),
                getal("0"));
        String vouwlid = vergelijking("greater", vouw(), getal("0"));
        List<String> j1 = List.of(instantieMetRollen("J1", "Jutter", Map.of(), List.of(), Map.of("vondsten",
                List.of("V1", "V2"), "melding", List.of("M1"))), vondst("V1", "10", "2"), vondst("V2", "9", "0"),
                instantieMetRollen("M1", "Melding", Map.of("telling", "0"), List.of(), Map.of()));
        List<String> j2 = List.of(instantieMetRollen("J2", "Jutter", Map.of(), List.of(), Map.of("vondsten",
                List.of("V3"), "melding", List.of("M2"))), vondst("V3", "10", "2"),
                instantieMetRollen("M2", "Melding", Map.of("telling", "3"), List.of(), Map.of()));
        List<String> j3 = List.of(instantieMetRollen("J3", "Jutter", Map.of(), List.of(), Map.of("vondsten",
                List.of("V5"), "melding", List.of("M3"))), vondst("V5", "2", "2"),
                instantieMetRollen("M3", "Melding", Map.of("telling", "3"), List.of(), Map.of()));
        Path vouwEerst = document(List.of(kenmerkregel("drukte", "Jutter", "druk", samengesteld("all", 0,
                List.of(vouwlid, genavigeerd)))), List.of());
        Path lidEerst = document(List.of(kenmerkregel("drukte", "Jutter", "druk", samengesteld("all", 0,
                List.of(genavigeerd, vouwlid)))), List.of());
        faalt(vouwEerst, TmsDocumenten.zaak(j1, List.of(consistent())), "drukte", "9");
        gespeeld(lidEerst, TmsDocumenten.zaak(j1, List.of(verwacht("J1", "druk", false), consistent())));
        for (Path ir : List.of(vouwEerst, lidEerst)) {
            gespeeld(ir, TmsDocumenten.zaak(j2, List.of(verwacht("J2", "druk", true), consistent())));
            gespeeld(ir, TmsDocumenten.zaak(j3, List.of(verwacht("J3", "druk", false), consistent())));
        }
    }

    /** An instance as {@link TmsDocumenten#instantieMetRollen} writes it, with the dates of {@code datums} added. */
    private static String metDatums(String instantie, Map<String, String> datums) {
        List<String> d = new ArrayList<>();
        datums.forEach((naam, iso) -> d.add("\"" + naam + "\":{\"date\":\"" + iso + "\"}"));
        return instantie.contains("\"attributes\":{}")
                ? instantie.replace("\"attributes\":{}", "\"attributes\":{" + String.join(",", d) + "}")
                : instantie.replace("\"attributes\":{", "\"attributes\":{" + String.join(",", d) + ",");
    }

    /** "de eerste van de vinddatum van alle vondsten van de Jutter die {@link #FILTER} is later dan zijn peildatum"
     *  beside "hij is actief", in both orders, on the exec-model branch; the DRL-text branch refuses a fold of dates in a
     *  condition by name (measured 2026-10-09). J1 is not actief and its V2 divides 9 by 0: with the fold first the
     *  composite faults, after "hij is actief" the fold is not read. J2 is actief, and its one contributor V3 was found
     *  on 2026-01-05, later than its peildatum 2020-01-01. */
    @Test void een_datumvouw_naast_een_lid_in_beide_volgordes() throws Exception {
        String datumvouw = vergelijking("later", aggregatie("first", lezing("vinddatum", "Vondst"), "Vondst",
                bronrol("aanmelding", "vondst"), FILTER, "null", DATUM, "de eerste van de vinddatum van alle vondsten van de"
                        + " Jutter die aan de volgende voorwaarde voldoen"), lezing("peildatum", "Jutter"));
        List<String> j1 = List.of(metDatums(jutter("J1", false, "0", "1", List.of("V1", "V2")), Map.of("peildatum",
                "2020-01-01")), metDatums(vondst("V1", "10", "2"), Map.of("vinddatum", "2026-01-05")),
                metDatums(vondst("V2", "9", "0"), Map.of("vinddatum", "2026-01-06")));
        List<String> j2 = List.of(metDatums(jutter("J2", true, "0", "1", List.of("V3", "V4")), Map.of("peildatum",
                "2020-01-01")), metDatums(vondst("V3", "10", "2"), Map.of("vinddatum", "2026-01-05")),
                metDatums(vondst("V4", "3", "3"), Map.of("vinddatum", "2019-01-01")));
        Path vouwEerst = document(List.of(kenmerkregel("drukte", "Jutter", "druk", samengesteld("all", 0,
                List.of(datumvouw, check("actief", false))))), List.of());
        Path actiefEerst = document(List.of(kenmerkregel("drukte", "Jutter", "druk", samengesteld("all", 0,
                List.of(check("actief", false), datumvouw)))), List.of());
        assertEquals("DelingDoorNul: deling door nul in de regel \"drukte\": 9 gedeeld door 0 — een numerieke nul als"
                + " deler is een fout, geen leeg (RUNTIME-SEMANTICS ch. 1.4, punt 4)",
                TmsFoutWitnessTest.speel(vouwEerst, TmsDocumenten.zaak(j1, List.of(consistent())), EXEC));
        assertEquals("gespeeld", TmsFoutWitnessTest.speel(actiefEerst, TmsDocumenten.zaak(j1,
                List.of(verwacht("J1", "druk", false), consistent())), EXEC));
        for (Path ir : List.of(vouwEerst, actiefEerst))
            assertEquals("gespeeld", TmsFoutWitnessTest.speel(ir, TmsDocumenten.zaak(j2,
                    List.of(verwacht("J2", "druk", true), consistent())), EXEC));
    }

    /** "de eerste van de vinddatum van alle vondsten van de Jutter die aan de volgende voorwaarde voldoen:
     *  {@code filter} is later dan zijn peildatum". */
    private static String datumvouw(String filter) {
        return vergelijking("later", aggregatie("first", lezing("vinddatum", "Vondst"), "Vondst",
                bronrol("aanmelding", "vondst"), filter, "null", DATUM, "de eerste van de vinddatum van alle vondsten"
                        + " van de Jutter die aan de volgende voorwaarde voldoen"), lezing("peildatum", "Jutter"));
    }

    /** {@link #datumvouw} over {@link #FILTER} as the third member after "hij is actief" and "hij is beladen", under
     *  geen, ten minste 1, ten hoogste 1 and precies 1, and under ten minste 1 after "hij is beladen", nested in an
     *  alle composite after "hij is actief". J1 is actief and beladen and its V2 divides 9 by 0. Every composite
     *  settles before the fold (ch. 5.10), so the reference interpreter answers: not druk, druk, not druk, not druk,
     *  and nested druk (measured 2026-10-09 on the RegelSpraak frontend's documents of these forms; the ASP runner
     *  agrees). The exec-model branch lowers such a composite as one test after the accumulates of all its folds, and
     *  Drools folds those for every subject before the test reads the members in their order, so the branch divided 9
     *  by 0 there. It now refuses the form by name; the DRL-text branch refuses a fold of dates in a condition. The
     *  control: the same composites over "zijn gewicht is groter dan 5", which cannot fault, play on the exec-model
     *  branch as the reference interpreter answers, on J1 and on J2, neither actief nor beladen, whose V3 is heavy and
     *  was found after its peildatum, so that geen, ten minste 1 and precies 1 read the fold: not druk, druk, druk,
     *  druk. */
    @Test void een_vouw_die_kan_falen_onder_een_andere_kwantor_dan_alle_wordt_geweigerd() throws Exception {
        List<String> j1 = List.of(metDatums(instantieMetRollen("J1", "Jutter", Map.of(), List.of("actief", "beladen"),
                Map.of("vondsten", List.of("V1", "V2"))), Map.of("peildatum", "2020-01-01")),
                metDatums(vondst("V1", "10", "2"), Map.of("vinddatum", "2026-01-05")),
                metDatums(vondst("V2", "9", "0"), Map.of("vinddatum", "2026-01-06")));
        List<String> j2 = List.of(metDatums(instantieMetRollen("J2", "Jutter", Map.of(), List.of(),
                Map.of("vondsten", List.of("V3", "V4"))), Map.of("peildatum", "2020-01-01")),
                metDatums(vondst("V3", "10", "2"), Map.of("vinddatum", "2026-01-05")),
                metDatums(vondst("V4", "3", "0"), Map.of("vinddatum", "2019-01-01")));
        String zwaar = vergelijking("greater", lezing("gewicht", "Vondst"), getal("5"));
        // per quantifier: its wire spelling and threshold, its name in the refusal, druk on J1, druk on J2 (control)
        String[][] kwantoren = {{"none", "0", "GEEN", "false", "false"}, {"atLeast", "1", "TEN_MINSTE", "true", "true"},
                {"atMost", "1", "TEN_HOOGSTE", "false", "true"}, {"exactly", "1", "PRECIES", "false", "true"}};
        List<String> afwijkend = new ArrayList<>();
        for (String[] k : kwantoren) {
            int drempel = Integer.parseInt(k[1]);
            Path ir = document(List.of(kenmerkregel("drukte", "Jutter", "druk", samengesteld(k[0], drempel,
                    List.of(check("actief", false), check("beladen", false), datumvouw(FILTER))))), List.of());
            weigering(ir, TmsDocumenten.zaak(j1, List.of(verwacht("J1", "druk", Boolean.parseBoolean(k[3])),
                    consistent())), k[2], k[0] + " " + drempel, afwijkend);
            Path controle = document(List.of(kenmerkregel("drukte", "Jutter", "druk", samengesteld(k[0], drempel,
                    List.of(check("actief", false), check("beladen", false), datumvouw(zwaar))))), List.of());
            for (JsonNode zaak : List.of(TmsDocumenten.zaak(j1, List.of(verwacht("J1", "druk",
                    Boolean.parseBoolean(k[3])), consistent())), TmsDocumenten.zaak(j2, List.of(verwacht("J2", "druk",
                    Boolean.parseBoolean(k[4])), consistent())))) {
                String v = TmsFoutWitnessTest.speel(controle, zaak, EXEC);
                if (!v.equals("gespeeld")) afwijkend.add(k[0] + " controle " + EXEC + ": " + v);
            }
        }
        Path genest = document(List.of(kenmerkregel("drukte", "Jutter", "druk", samengesteld("all", 0, List.of(
                check("actief", false), samengesteld("atLeast", 1, List.of(check("beladen", false),
                        datumvouw(FILTER))))))), List.of());
        weigering(genest, TmsDocumenten.zaak(j1, List.of(verwacht("J1", "druk", true), consistent())), "TEN_MINSTE",
                "atLeast 1", afwijkend);
        assertEquals(List.of(), afwijkend);
    }

    /** Adds to {@code afwijkend} where the exec-model branch does not refuse rule "drukte" of {@code ir} for a
     *  composite under {@code kwantor} (its source text {@code bron}) whose fold divides, or the DRL-text branch does
     *  not refuse the rule. */
    private static void weigering(Path ir, JsonNode zaak, String kwantor, String bron, List<String> afwijkend)
            throws Exception {
        String exec = TmsFoutWitnessTest.speel(ir, zaak, EXEC);
        String reden = "Excluded: transpiler SKIP: regel: drukte — regel (een " + kwantor + "-samenstelling met een"
                + " vouw waarvan de subselectie een fout kan geven, een deling ('zijn gewicht gedeeld door zijn"
                + " deelnemers'): deze tak vouwt elke vouw van de samenstelling op de linkerkant van haar regel, ook"
                + " waar de samenstelling stopt voordat zij het lid met die vouw leest (RUNTIME-SEMANTICS ch. 5.10): "
                + bron + "): drukte";
        if (!exec.equals(reden)) afwijkend.add(bron + " " + EXEC + ": " + exec);
        String drl = TmsFoutWitnessTest.speel(ir, zaak, DRL);
        if (!drl.startsWith("Excluded: drl-transpiler MARKER: regel: drukte — "))
            afwijkend.add(bron + " " + DRL + ": " + drl);
    }

    /** "het aantal vondsten van de Jutter die aan de volgende voorwaarde voldoen: zijn gewicht is groter dan 50", a fold
     *  whose subselection cannot fault. */
    static String zwaarVouw() {
        return aggregatie("count", "{\"node\":\"AttributeReference\",\"attribute\":\"vondsten\",\"subjectType\":"
                + "\"Jutter\",\"source\":{\"snippet\":\"vondsten van de Jutter\"}}", "Vondst", bronrol("aanmelding", "vondst"),
                vergelijking("greater", lezing("gewicht", "Vondst"), getal("50")), "null", TELLING,
                "het aantal vondsten van de Jutter die aan de volgende voorwaarde voldoen");
    }

    /** "De zwaarte van een Jutter moet berekend worden als {@link #vouw()} indien hij aan alle volgende voorwaarden
     *  voldoet: - {@link #zwaarVouw()} is groter dan 0", and the creation of a melding with that telling under that
     *  condition: the value is read only where the condition holds, so the fold in the condition comes first. J1 has no
     *  vondst above 50. J2's V6 (60 by 2) is above 50 and counts, its V4 (3 by 3) does not: 1. J4 has V6 and V2, so the
     *  condition holds and the value divides 9 by 0. */
    @Test void een_vouw_in_de_waarde_onder_een_vouw_in_de_voorwaarde() throws Exception {
        String voorwaarde = samengesteld("all", 0, List.of(vergelijking("greater", zwaarVouw(), getal("0"))));
        Path regel = document(List.of(rekenregel("zwaarte", "BEREKENING", "Jutter", "zwaarte", vouw(), voorwaarde)),
                List.of());
        Path creatie = document(List.of(MELDTELLING), List.of(creatie(vouw(), voorwaarde)));
        String v6 = vondst("V6", "60", "2");
        gespeeld(regel, TmsDocumenten.zaak(j1(false), List.of(waarde("J1", "zwaarte", null), consistent())));
        gespeeld(creatie, TmsDocumenten.zaak(j1(false), List.of(waarde("J1", "meldtelling", null), consistent())));
        List<String> j2 = List.of(jutter("J2", false, List.of("V6", "V4")), v6, vondst("V4", "3", "3"));
        gespeeld(regel, TmsDocumenten.zaak(j2, List.of(waarde("J2", "zwaarte", "1"), consistent())));
        gespeeld(creatie, TmsDocumenten.zaak(j2, List.of(waarde("J2", "meldtelling", "1"), consistent())));
        List<String> j4 = List.of(jutter("J4", false, List.of("V6", "V2")), v6, vondst("V2", "9", "0"));
        faalt(regel, TmsDocumenten.zaak(j4, List.of(consistent())), "zwaarte", "9");
        faalt(creatie, TmsDocumenten.zaak(j4, List.of(consistent())), "meld", "9");
    }

    /** A subselection that divides by an attribute another rule writes on the element: "De deelnemers van een Vondst
     *  moet berekend worden als zijn ploeg plus 1" beside "De zwaarte van een Jutter moet berekend worden als
     *  {@link #vouw()}", and beside "Een Jutter is druk indien hij aan alle volgende voorwaarden voldoet: - het gewicht van
     *  ten minste één van de vondsten van de Jutter die {@link #FILTER} is groter dan 5", each in both orders. The case
     *  supplies deelnemers 10 and 3, which the rule overwrites with 2 and 1, so the fold reads 10 by 2 and 3 by 1: it
     *  counts both, and V1, gewicht 10, holds the quantifier fold; the input values would count none and hold nothing. */
    @Test void een_delend_filter_over_een_overschreven_deler_in_elke_regelvolgorde() throws Exception {
        String deelnemers = rekenregel("deelnemers", "BEREKENING", "Vondst", "deelnemers",
                binair("plus", lezing("ploeg", "Vondst"), getal("1")), "null");
        List<String> instanties = List.of(jutter("J1", false, List.of("V1", "V2")),
                instantieMetRollen("V1", "Vondst", Map.of("gewicht", "10", "ploeg", "1", "deelnemers", "10"), List.of(),
                        Map.of()),
                instantieMetRollen("V2", "Vondst", Map.of("gewicht", "3", "ploeg", "0", "deelnemers", "3"), List.of(),
                        Map.of()));
        String zwaarte = rekenregel("zwaarte", "BEREKENING", "Jutter", "zwaarte", vouw(), "null");
        for (List<String> regels : TmsDocumenten.permutaties(List.of(deelnemers, zwaarte)))
            gespeeld(document(regels, List.of()), TmsDocumenten.zaak(instanties,
                    List.of(waarde("J1", "zwaarte", "2"), consistent())));
        String drukte = kenmerkregel("drukte", "Jutter", "druk", samengesteld("all", 0, List.of(lid('K'))));
        for (List<String> regels : TmsDocumenten.permutaties(List.of(deelnemers, drukte)))
            gespeeld(document(regels, List.of()), TmsDocumenten.zaak(instanties,
                    List.of(verwacht("J1", "druk", true), consistent())));
    }

    /** "De zwaarte van een Jutter moet berekend worden als zijn saldo gedeeld door zijn noemer indien hij aan alle
     *  volgende voorwaarden voldoet: - {@link #zwaarVouw()} is groter dan 0 - hij is actief": "hij is actief" follows the
     *  fold and goes on the subject bound again. The DRL-text branch's companion for a division by a leeg noemer reads
     *  the condition's inline constraint and joins, so it takes the condition lowered without that move and still asks
     *  "hij is actief". J5 is not actief and has no noemer: the rule does not fire and nothing is deposited. J6 is actief,
     *  6 by 2: 3. J7 is actief and has no noemer: the division by leeg deposits. A guard of the move: on the branch before
     *  it the companion read "hij is actief" as well; a companion fed the moved condition's inline deposits for J5
     *  (mutant, measured 2026-10-09). */
    @Test void een_begeleider_leest_de_voorwaarde_zonder_verplaatsing() throws Exception {
        String deling = "{\"node\":\"Binary\",\"op\":\"dividedBy\",\"left\":" + lezing("saldo", "Jutter") + ",\"right\":"
                + lezing("noemer", "Jutter") + ",\"type\":{\"node\":\"Number\",\"whole\":false,\"decimals\":0,\"sign\":"
                + "\"unspecified\"},\"source\":{\"snippet\":\"zijn saldo gedeeld door zijn noemer\"}}";
        Path ir = document(List.of(rekenregel("zwaarte", "BEREKENING", "Jutter", "zwaarte", deling, samengesteld("all", 0,
                List.of(vergelijking("greater", zwaarVouw(), getal("0")), check("actief", false))))), List.of());
        String v6 = vondst("V6", "60", "2");
        gespeeld(ir, TmsDocumenten.zaak(List.of(instantieMetRollen("J5", "Jutter", Map.of("saldo", "6"), List.of(),
                Map.of("vondsten", List.of("V6"))), v6), List.of(waarde("J5", "zwaarte", null), consistent())));
        gespeeld(ir, TmsDocumenten.zaak(List.of(jutter("J6", true, "6", "2", List.of("V6")), v6),
                List.of(waarde("J6", "zwaarte", "3"), consistent())));
        gespeeld(ir, TmsDocumenten.zaak(List.of(instantieMetRollen("J7", "Jutter", Map.of("saldo", "6"), List.of("actief"),
                Map.of("vondsten", List.of("V6"))), v6), List.of(waarde("J7", "zwaarte", null),
                TmsDocumenten.inconsistent("zwaarte", "J7"))));
    }

    /** "Een winterschuit van een Haven is een ligger van de Haven indien hij aan alle volgende voorwaarden voldoet:" with
     *  the members "het aantal liggers van de Haven die aan de volgende voorwaarde voldoen: zijn lengte gedeeld door zijn
     *  breedte is groter dan 2 is groter dan 0" and "de Haven veilig is", in both orders, read back through "het
     *  wintertal van een Haven ... het aantal winterschuiten van de Haven": the condition of a fact creation's anchor.
     *  H1 is not veilig and its S2 divides 9 by 0: with the fold first the composite faults, after "de Haven veilig is"
     *  the fold is not read and H1 gets no winterschuit. H2 is veilig and its S3 (10 by 2) is its winterschuit. */
    @Test void een_feitcreatie_leest_de_leden_van_haar_voorwaarde_in_volgorde() throws Exception {
        String model = TmsDocumenten.model(List.of(
                TmsDocumenten.objecttype("Haven", List.of(attribuut("wintertal", TELLING)), List.of("veilig")),
                TmsDocumenten.objecttype("Schuit", List.of(attribuut("lengte", GEHEEL), attribuut("breedte", GEHEEL)),
                        List.of())),
                List.of(feittypeEenNaarVeel("thuisligging", "thuishaven", "Haven", "ligger", "Schuit", "liggers"),
                        feittypeEenNaarVeel("winterberging", "winterhaven", "Haven", "winterschuit", "Schuit",
                                "winterschuiten")));
        String wintertal = rekenregel("wintertal", "BEREKENING", "Haven", "wintertal", aggregatie("count",
                "{\"node\":\"AttributeReference\",\"attribute\":\"winterschuiten\",\"subjectType\":\"Haven\",\"source\":"
                        + "{\"snippet\":\"winterschuiten van de Haven\"}}", "Schuit", bronrol("winterberging", "winterschuit"),
                "null", "null", TELLING, "het aantal winterschuiten van de Haven"), "null");
        String vouw = vergelijking("greater", aggregatie("count", "{\"node\":\"AttributeReference\",\"attribute\":"
                + "\"liggers\",\"subjectType\":\"Haven\",\"source\":{\"snippet\":\"liggers van de Haven\"}}", "Schuit",
                bronrol("thuisligging", "ligger"), vergelijking("greater", TmsFilterfoutWitnessTest.quotient(
                        lezing("lengte", "Schuit"), lezing("breedte", "Schuit")), getal("2")), "null", TELLING,
                "het aantal liggers van de Haven die aan de volgende voorwaarde voldoen"), getal("0"));
        String veilig = check("veilig", false);
        String h1 = instantieMetRollen("H1", "Haven", Map.of(), List.of(), Map.of("liggers", List.of("S1", "S2")));
        String h2 = instantieMetRollen("H2", "Haven", Map.of(), List.of("veilig"), Map.of("liggers", List.of("S3")));
        List<String> schuiten1 = List.of(instantieMetRollen("S1", "Schuit", Map.of("lengte", "10", "breedte", "2"),
                List.of(), Map.of()), instantieMetRollen("S2", "Schuit", Map.of("lengte", "9", "breedte", "0"), List.of(),
                Map.of()));
        String s3 = instantieMetRollen("S3", "Schuit", Map.of("lengte", "10", "breedte", "2"), List.of(), Map.of());
        for (List<String> leden : List.of(List.of(vouw, veilig), List.of(veilig, vouw))) {
            String feitcreatie = "{\"node\":\"FactCreation\",\"name\":\"winterberging door de haven\",\"versions\":[{"
                    + "\"factType\":\"winterberging\",\"anchorRole\":\"winterhaven\",\"targetRole\":\"winterschuit\","
                    + "\"anchor\":\"Haven\",\"path\":[" + rolbinding("thuisligging", "ligger") + "],\"condition\":"
                    + samengesteld("all", 0, leden) + ",\"validity\":{\"node\":\"Validity\"}}]}";
            Path ir = Files.writeString(Files.createTempFile("filterfout-getuige", ".json"), "{\"format\":\"uchar-ir-v1\","
                    + "\"model\":" + model + ",\"rules\":{\"rules\":[" + wintertal + "],\"dayKindDefinitions\":[],"
                    + "\"objectCreations\":[],\"allocations\":[],\"consistencies\":[],\"uniquenesses\":[],\"factCreations\":["
                    + feitcreatie + "],\"startPointDeterminations\":[],\"ruleGroups\":[],\"prioritizedDefaults\":[]},"
                    + "\"diagnostics\":[]}");
            ir.toFile().deleteOnExit();
            List<String> h1Zaak = new ArrayList<>(List.of(h1));
            h1Zaak.addAll(schuiten1);
            if (leden.get(0).equals(vouw)) faaltIn(ir, TmsDocumenten.zaak(h1Zaak, List.of(consistent())),
                    "winterberging door de haven", "9", "rsGedeeld(lengte, breedte)");
            else gespeeld(ir, TmsDocumenten.zaak(h1Zaak, List.of(waarde("H1", "wintertal", "0"), consistent())));
            gespeeld(ir, TmsDocumenten.zaak(List.of(h2, s3), List.of(waarde("H2", "wintertal", "1"), consistent())));
        }
    }

    /** {@code zaak} faults on both branches in a division by 0 of {@code deeltal} in rule {@code regel}: the exec-model
     *  branch's named division, the DRL-text branch's exception for the constraint holding {@code quotient}. */
    private static void faaltIn(Path ir, JsonNode zaak, String regel, String deeltal, String quotient) throws Exception {
        assertEquals("DelingDoorNul: deling door nul in de regel \"" + regel + "\": " + deeltal + " gedeeld door 0 — een"
                + " numerieke nul als deler is een fout, geen leeg (RUNTIME-SEMANTICS ch. 1.4, punt 4)",
                TmsFoutWitnessTest.speel(ir, zaak, EXEC), "exec-model");
        assertEquals("deling " + quotient, waarneming(ir, zaak, DRL), "drl-tekst");
    }

    // ---------------- a defaults group reads its defaults by priority ----------------

    /** A defaults group on the zwaarte, the higher default "1 indien hij actief is" and below it {@code lager}. */
    static Path defaultsgroep(String lager) throws Exception {
        return TmsDocumenten.document(MODEL, List.of(), List.of(), List.of(TmsDocumenten.attribuutgroep("zwaarte basis",
                "Jutter", "zwaarte", List.of(TmsDocumenten.standaard(check("actief", false), getal("1")), lager))));
    }

    /** The lower default "2 indien hij aan alle volgende voorwaarden voldoet: - {@link #vouw()} is groter dan 0", and
     *  the lower default "{@link #vouw()} indien hij druk is": the reference interpreter reads a lower default only where
     *  no higher one decides (RUNTIME-SEMANTICS ch. 7.1), the exec-model branch folds every fold of the group on the
     *  left-hand side of its rule, for every subject. Where the fold's subselection can fault the branch refuses the group
     *  by name (the DRL-text branch refuses every defaults group). */
    @Test void een_defaultsgroep_met_een_vouw_die_kan_falen_wordt_geweigerd() throws Exception {
        JsonNode zaak = TmsDocumenten.zaak(j1(true), List.of(waarde("J1", "zwaarte", "1"), consistent()));
        for (String lager : List.of(
                TmsDocumenten.standaard(samengesteld("all", 0, List.of(vergelijking("greater", vouw(), getal("0")))),
                        getal("2")),
                TmsDocumenten.standaard(check("druk", false), vouw()))) {
            String exec = TmsFoutWitnessTest.speel(defaultsgroep(lager), zaak, EXEC);
            assertTrue(exec.startsWith("Excluded: transpiler SKIP: defaults: ") && exec.contains("defaults (Jutter.zwaarte): ")
                    && exec.contains("een deling ('zijn gewicht gedeeld door zijn deelnemers')"), exec);
        }
    }

    /** The control: the lower default "2 indien … het aantal vondsten van de Jutter die aan de volgende voorwaarde
     *  voldoen: zijn gewicht is groter dan 5 is groter dan 0", whose subselection cannot fault, is lowered as before. J1
     *  is actief: 1. J2 is not, and its V3 weighs 10: 2. J3 is not, and its V5 weighs 4: the group decides nothing. */
    @Test void een_defaultsgroep_met_een_vouw_die_niet_kan_falen_speelt() throws Exception {
        String vijf = aggregatie("count", "{\"node\":\"AttributeReference\",\"attribute\":\"vondsten\",\"subjectType\":"
                + "\"Jutter\",\"source\":{\"snippet\":\"vondsten van de Jutter\"}}", "Vondst", bronrol("aanmelding", "vondst"),
                vergelijking("greater", lezing("gewicht", "Vondst"), getal("5")), "null", TELLING,
                "het aantal vondsten van de Jutter die aan de volgende voorwaarde voldoen");
        Path ir = defaultsgroep(TmsDocumenten.standaard(samengesteld("all", 0, List.of(vergelijking("greater", vijf,
                getal("0")))), getal("2")));
        assertEquals("gespeeld", TmsFoutWitnessTest.speel(ir, TmsDocumenten.zaak(j1(true),
                List.of(waarde("J1", "zwaarte", "1"), consistent())), EXEC));
        assertEquals("gespeeld", TmsFoutWitnessTest.speel(ir, TmsDocumenten.zaak(List.of(jutter("J2", false,
                List.of("V3")), vondst("V3", "10", "2")), List.of(waarde("J2", "zwaarte", "2"), consistent())), EXEC));
        assertEquals("gespeeld", TmsFoutWitnessTest.speel(ir, TmsDocumenten.zaak(List.of(jutter("J3", false,
                List.of("V5")), vondst("V5", "4", "4")), List.of(waarde("J3", "zwaarte", null), consistent())), EXEC));
    }

    // ---------------- a subselection that faults otherwise than by a division ----------------

    /** A whole number in the unit {@code eenheid}. */
    private static String inEenheid(String eenheid) {
        return "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\",\"unit\":{\"node\":\"Unit\","
                + "\"abbreviation\":\"" + eenheid + "\",\"canonical\":\"" + eenheid + "\",\"factorToCanonical\":{\"num\":1,"
                + "\"den\":1}}}";
    }

    /** A Jutter with a vondsttal; a Vondst with a gewicht, deelnemers, a vinddatum, a wachttijd in nooitdagen, a termijn
     *  in jaren, a jaar and a maand; and the day kind nooitdag, which no day is of. */
    static final String MODEL_FAALBAAR = TmsDocumenten.model(List.of(
            TmsDocumenten.objecttype("Jutter", List.of(attribuut("vondsttal", GEHEEL)), List.of("actief")),
            TmsDocumenten.objecttype("Vondst", List.of(attribuut("gewicht", GEHEEL), attribuut("deelnemers", GEHEEL),
                    attribuut("vinddatum", DATUM), attribuut("wachttijd", inEenheid("nooitdagen")),
                    attribuut("termijn", inEenheid("jr")), attribuut("jaar", GEHEEL), attribuut("maand", GEHEEL)),
                    List.of())),
            List.of(feittypeEenNaarVeel("aanmelding", "aanmelder", "Jutter", "vondst", "Vondst", "vondsten")))
            .replace("\"dayKinds\":[]", "\"dayKinds\":[{\"node\":\"DayKind\",\"name\":\"nooitdag\",\"plural\":"
                    + "\"nooitdagen\"}]");

    /** "Een dag is een nooitdag indien de dag uit de dag gelijk is aan 32". */
    static final String NOOITDAG = "{\"node\":\"DayKindDefinition\",\"dayKind\":\"nooitdag\",\"condition\":{\"node\":"
            + "\"Comparison\",\"op\":\"equal\",\"left\":{\"node\":\"BuiltinCall\",\"kind\":\"dayOf\",\"arguments\":[{\"node\":"
            + "\"BoundDay\",\"type\":" + DATUM + ",\"source\":{\"snippet\":\"de dag\"}}],\"type\":" + TELLING + ",\"source\":"
            + "{\"snippet\":\"de dag uit de dag\"}},\"right\":" + getal("32") + ",\"source\":{\"snippet\":\"de dag uit de dag"
            + " gelijk is aan 32\"}},\"validity\":{\"node\":\"Validity\"}}";

    /** "Het vondsttal van een Jutter moet berekend worden als het aantal vondsten van de Jutter die aan de volgende
     *  voorwaarde voldoen: {@code filter}, indien hij actief is", in a document of {@link #MODEL_FAALBAAR}. */
    static Path vondsttal(String filter) throws Exception {
        String regel = rekenregel("vondsttal", "BEREKENING", "Jutter", "vondsttal", aggregatie("count",
                "{\"node\":\"AttributeReference\",\"attribute\":\"vondsten\",\"subjectType\":\"Jutter\",\"source\":"
                        + "{\"snippet\":\"vondsten van de Jutter\"}}", "Vondst", bronrol("aanmelding", "vondst"), filter,
                "null", TELLING, "het aantal vondsten van de Jutter die aan de volgende voorwaarde voldoen"),
                check("actief", false));
        String ir = "{\"format\":\"uchar-ir-v1\",\"model\":" + MODEL_FAALBAAR + ",\"rules\":{\"rules\":[" + regel
                + "],\"dayKindDefinitions\":[" + NOOITDAG + "],\"objectCreations\":[],\"allocations\":[],"
                + "\"consistencies\":[],\"uniquenesses\":[],\"factCreations\":[],\"startPointDeterminations\":[],"
                + "\"ruleGroups\":[],\"prioritizedDefaults\":[]},\"diagnostics\":[]}";
        Path tmp = Files.createTempFile("filterfout-getuige", ".json");
        tmp.toFile().deleteOnExit();
        return Files.writeString(tmp, ir);
    }

    /** A node of the wire with its source text. */
    private static String knoop(String velden, String bron) {
        return "{" + velden + ",\"source\":{\"snippet\":\"" + bron + "\"}}";
    }

    /** The messages of {@code t} and its causes, joined. */
    private static String oorzaken(Throwable t) {
        StringBuilder b = new StringBuilder();
        for (Throwable o = t; o != null; o = o.getCause() == o ? null : o.getCause()) b.append(o.getMessage()).append(" <- ");
        return b.toString();
    }

    /** Each operation on the closed list of {@code kern.Faalbaar} other than the division, in the subselection of a fold
     *  under "indien hij actief is", on a Jutter that is not actief, whose one vondst makes the operation fault: a step of
     *  one nooitdag, which no day is of; a step of 2 000 000 000 jaren; zijn gewicht 0 to the power -1; the date of
     *  maand 13; the Rekenjaar of a case without an evaluation date; the eerste paasdag of the year 2 000 000 000. The
     *  reference interpreter does not evaluate the fold and answers (measured 2026-10-09); both branches tested the
     *  subselection on the element's pattern and faulted, the DRL-text branch for all six, the exec-model branch for
     *  the three it lowers in a subselection (it refuses the date of parts, the Rekenjaar and the paasdag there by name).
     *  Where J1 is actief, the reference interpreter faults on the day-kind walk and on the Rekenjaar too, and so do
     *  both branches, with the runtime's sentence in the cause chain; for the other four it answers or crashes where
     *  this backend faults (reported with the fix round, not pinned here). */
    @Test void een_filter_dat_anders_dan_door_een_deling_faalt() throws Exception {
        String vinddatum = lezing("vinddatum", "Vondst");
        Map<String, String> filters = new java.util.LinkedHashMap<>();
        filters.put("dagsoortstap", vergelijking("later", knoop("\"node\":\"Binary\",\"op\":\"plus\",\"left\":" + vinddatum
                + ",\"right\":" + lezing("wachttijd", "Vondst") + ",\"type\":" + DATUM, "zijn vinddatum plus zijn wachttijd"),
                vinddatum));
        filters.put("datumbereik", vergelijking("later", knoop("\"node\":\"Binary\",\"op\":\"plus\",\"left\":" + vinddatum
                + ",\"right\":" + lezing("termijn", "Vondst") + ",\"type\":" + DATUM, "zijn vinddatum plus zijn termijn"),
                vinddatum));
        filters.put("macht", vergelijking("greater", knoop("\"node\":\"Rounding\",\"base\":" + knoop("\"node\":\"Binary\","
                + "\"op\":\"power\",\"left\":" + lezing("gewicht", "Vondst") + ",\"right\":" + lezing("deelnemers", "Vondst")
                + ",\"type\":{\"node\":\"Number\",\"whole\":false,\"decimals\":0,\"sign\":\"unspecified\"}",
                "zijn gewicht tot de macht zijn deelnemers") + ",\"mode\":\"floor\",\"decimals\":0,\"type\":" + GEHEEL,
                "zijn gewicht tot de macht zijn deelnemers naar beneden afgerond op 0 decimalen"), getal("2")));
        filters.put("datum met", vergelijking("later", knoop("\"node\":\"BuiltinCall\",\"kind\":\"dateWith\","
                + "\"arguments\":[" + lezing("jaar", "Vondst") + "," + lezing("maand", "Vondst") + "," + getal("1") + "],"
                + "\"type\":" + DATUM, "de datum met jaar: zijn jaar, maand: zijn maand en dag: 1"), vinddatum));
        filters.put("rekenjaar", vergelijking("greater", knoop("\"node\":\"ContextValue\",\"name\":\"evaluationYear\","
                + "\"type\":" + GEHEEL, "het Rekenjaar"), lezing("gewicht", "Vondst")));
        filters.put("paasdag", vergelijking("later", knoop("\"node\":\"BuiltinCall\",\"kind\":\"easterSunday\","
                + "\"arguments\":[" + lezing("jaar", "Vondst") + "],\"type\":" + DATUM, "de eerste paasdag van zijn jaar"),
                vinddatum));
        Map<String, Map<String, String>> getallen = Map.of(
                "dagsoortstap", Map.of("wachttijd", "1"), "datumbereik", Map.of("termijn", "2000000000"),
                "macht", Map.of("gewicht", "0", "deelnemers", "-1"), "datum met", Map.of("jaar", "2026", "maand", "13"),
                "rekenjaar", Map.of("gewicht", "10"), "paasdag", Map.of("jaar", "2000000000"));
        java.util.Set<String> inExec = java.util.Set.of("dagsoortstap", "datumbereik", "macht");
        Map<String, String> fout = Map.of("dagsoortstap", "day-kind walk: no day of the kind within 40000 calendar days",
                "rekenjaar", "the evaluationYear context value needs an evaluation date");
        List<String> afwijkend = new ArrayList<>();
        for (Map.Entry<String, String> f : filters.entrySet()) {
            Path ir = vondsttal(f.getValue());
            String v1 = metDatums(instantieMetRollen("V1", "Vondst", getallen.get(f.getKey()), List.of(), Map.of()),
                    Map.of("vinddatum", "2026-01-05"));
            for (DroolsBackend.Branch tak : inExec.contains(f.getKey()) ? List.of(EXEC, DRL) : List.of(DRL)) {
                String niet = TmsFoutWitnessTest.speel(ir, TmsDocumenten.zaak(List.of(instantieMetRollen("J1", "Jutter",
                        Map.of(), List.of(), Map.of("vondsten", List.of("V1"))), v1),
                        List.of(waarde("J1", "vondsttal", null), consistent())), tak);
                if (!niet.equals("gespeeld")) afwijkend.add(f.getKey() + " " + tak + " niet actief: " + niet);
                if (!fout.containsKey(f.getKey())) continue;
                JsonNode actief = TmsDocumenten.zaak(List.of(instantieMetRollen("J1", "Jutter", Map.of(),
                        List.of("actief"), Map.of("vondsten", List.of("V1"))), v1), List.of(consistent()));
                try {
                    String v = CaseRunner.speelCase(ir, actief, tak);
                    afwijkend.add(f.getKey() + " " + tak + " actief: geen fout maar " + (v == null ? "gespeeld" : v));
                } catch (RuntimeException e) {
                    if (!oorzaken(e).contains(fout.get(f.getKey())))
                        afwijkend.add(f.getKey() + " " + tak + " actief: " + oorzaken(e));
                }
            }
        }
        assertEquals(List.of(), afwijkend);
    }
}
