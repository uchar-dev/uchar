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
 * for an element in no fold's range. A filter that divides now goes on the element bound again in the source of the
 * accumulate, after the join to the subject and the walk of a path, where it is tested per tuple that reaches the
 * accumulate.
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

    static final String MODEL = TmsDocumenten.model(List.of(
            TmsDocumenten.objecttype("Jutter", List.of(attribuut("zwaarte", GEHEEL), attribuut("meldtelling", GEHEEL)),
                    List.of("actief", "druk", "beladen", "ontlast")),
            TmsDocumenten.objecttype("Vondst", List.of(attribuut("gewicht", GEHEEL), attribuut("deelnemers", GEHEEL)),
                    List.of()),
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
        return aggregatie("count", "{\"node\":\"AttributeReference\",\"attribute\":\"vondsten\",\"subjectType\":"
                + "\"Jutter\",\"source\":{\"snippet\":\"vondsten van de Jutter\"}}", "Vondst", bronrol("aanmelding", "vondst"),
                FILTER, "null", TELLING, "het aantal vondsten van de Jutter die aan de volgende voorwaarde voldoen");
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
}
