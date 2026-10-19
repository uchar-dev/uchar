package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.JsonNode;


import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static dev.uchar.regelspraak.cases.TmsDocuments.WHOLE;
import static dev.uchar.regelspraak.cases.TmsDocuments.COUNTING;
import static dev.uchar.regelspraak.cases.TmsDocuments.aggregation;
import static dev.uchar.regelspraak.cases.TmsDocuments.attribute;
import static dev.uchar.regelspraak.cases.TmsDocuments.binary;
import static dev.uchar.regelspraak.cases.TmsDocuments.sourceRole;
import static dev.uchar.regelspraak.cases.TmsDocuments.check;
import static dev.uchar.regelspraak.cases.TmsDocuments.consistent;
import static dev.uchar.regelspraak.cases.TmsDocuments.factType;
import static dev.uchar.regelspraak.cases.TmsDocuments.factTypeOneToMany;
import static dev.uchar.regelspraak.cases.TmsDocuments.number;
import static dev.uchar.regelspraak.cases.TmsDocuments.instanceWithRoles;
import static dev.uchar.regelspraak.cases.TmsDocuments.characteristicRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.ownRead;
import static dev.uchar.regelspraak.cases.TmsDocuments.pathRead;
import static dev.uchar.regelspraak.cases.TmsDocuments.computingRule;
import static dev.uchar.regelspraak.cases.TmsDocuments.roleBinding;
import static dev.uchar.regelspraak.cases.TmsDocuments.compound;
import static dev.uchar.regelspraak.cases.TmsDocuments.comparison;
import static dev.uchar.regelspraak.cases.TmsDocuments.expected;
import static dev.uchar.regelspraak.cases.TmsDocuments.value;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <b>Since the card zero-division-channel (2026-10-19)</b> a division by a numeric zero is no fault but a §9.5 deposit of
 * kind EF-005 beside a leeg quotient (RUNTIME-SEMANTICS ch. 1.4, point 4), so the rows of this class whose filter
 * divided by 0 now pin where that deposit arises: under the form whose evaluation reaches the fold, and nowhere else,
 * which is where the fault arose ({@link #deposits}). A leeg quotient does not pass the filter, so the member does not
 * count. The path row and a matrix row pin a fault still, with the square root without a rounding of a number that is
 * no square ({@link TmsDocuments#root}, {@link #fails}); the order of the faults of a fold's filter is pinned by
 * {@code TmsFilterFaultWitnessTest}. The text below is that of the division as a fault, with the measurements of that
 * time.
 *
 * <p>A fault in the subselection of a fold, on the exec-model and the DRL-text branch, arises where the evaluation of a
 * form reaches the fold, and nowhere else. The reference interpreter evaluates a rule's condition first and its value
 * only where the rule fires, a composite condition only up to the member after which its verdict stands
 * (RUNTIME-SEMANTICS ch. 5.10), and a fold's filter only where it evaluates the fold, per member of that subject's
 * range (ch. 3.4a); a filter that divides by a numeric zero there faults the evaluation (ch. 1.4, point 4). Both
 * branches tested the filter on the element's pattern, which Drools tests for every element of the type as it is
 * inserted, so they faulted where a rule's condition did not hold, where a composite had settled before the fold, and
 * for an element in no fold's range. A filter that can fault ({@code kern.Faalbaar}, which went with the destructive
 * lowering at card drl-teksttak-op-truth-maintenance) then went on the element bound again in the source of the
 * accumulate, after the join to the subject and the walk of a path, where it was tested per tuple that reached the
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
 * a role, the fold before "de Haven veilig is": {@code played} where the division faults. The fold in the value under
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
 *
 * <p>The third fix round narrowed that refusal to a fold the reference interpreter can leave unread, and added the row
 * of a fold in a member that is always read. <b>Red before the round</b> (measured 2026-10-09, this class through
 * Maven): with the transpilers of {@code 9c6adc07} the row failed in all twelve cells of its six lowered forms with
 * {@code Excluded: transpiler SKIP: regel: drukte — regel (een GEEN-samenstelling met een vouw …}, where the reference
 * interpreter answers, or faults as the branch did before the refusal; its three refused forms passed. A refusal of a
 * fold in a member after the first at the top only, the narrowing first proposed, fails the row both ways: it keeps
 * refusing the three forms whose later member no trigger can skip, and it lowers the three refused forms, which then
 * fault where the reference interpreter answers ({@code atMost 1 EXEC_MODEL: DelingDoorNul: … 9 gedeeld door 0 …}).
 *
 * <p>Those refusals were the destructive exec-model branch's. Since card tms-omschakeling (2026-10-10) the rows that
 * pinned them run on the exec-model branch with truth maintenance, which plays every such form as the reference
 * interpreter answers; the rows whose form that branch refuses by name (a division in a member initialisation of a
 * creation, in a default, and a power whose exponent is no literal) pin its refusal. Since card
 * drl-teksttak-op-truth-maintenance the DRL-text branch renders the same analysis, and every row demands of it the
 * verdict of the exec-model branch: where it faulted at the constraint that holds the filter, with the quotient of that
 * constraint ("deling rsGedeeld(gewicht, deelnemers)"), it now gives the named division of the fixpoint, and where it
 * played or refused a form in sentences of its own ("drl-transpiler MARKER: regel: drukte"), it now plays or refuses as
 * the exec-model branch does.
 */
class FilterFaultWitnessTest {

    // cwd under surefire is the module dir (backends/drools); the corpus is two levels up.
    private static final Path FILTERWACHT =
            Path.of("..", "..", "corpus", "golden", "jutterswet-filterwacht", "ir.json");

    private static final DroolsBackend.Branch EXEC = DroolsBackend.Branch.EXEC_MODEL;
    private static final DroolsBackend.Branch DRL = DroolsBackend.Branch.DRL_TEXT;

    /** Both branches play {@code caseDocument} on {@code ir} with its expectations. */
    private static void played(Path ir, JsonNode caseDocument) throws Exception {
        assertEquals("gespeeld", TmsFaultWitnessTest.play(ir, caseDocument, EXEC), "exec-model");
        assertEquals("gespeeld", TmsFaultWitnessTest.play(ir, caseDocument, DRL), "drl-tekst");
    }

    /** Both branches fault on {@code caseDocument}, where rule {@code regel} reaches its fold and the filter or the
     *  matrix takes the root of {@code radicand}, which is no square, for a member: with the fault named after the
     *  rule, a fault of the fixpoint. Until the card zero-division-channel the fault was a division by zero. */
    private static void fails(Path ir, JsonNode caseDocument, String regel, String radicand) throws Exception {
        for (DroolsBackend.Branch branch : List.of(EXEC, DRL))
            assertEquals("ComputeFault: " + TmsDocuments.irrational(regel, radicand),
                    TmsFaultWitnessTest.play(ir, caseDocument, branch), branch.toString());
    }

    /** Both branches play {@code instances}, where rule {@code regel} reaches its fold on {@code label} and the filter
     *  divides by a numeric zero for a member: {@code outcome}, and the §9.5 deposit of kind EF-005 under that rule on
     *  that subject. Until the card zero-division-channel such a row faulted ({@link #fails}). */
    private static void deposits(Path ir, List<String> instances, List<String> outcome, String regel, String label)
            throws Exception {
        List<String> expectations = new ArrayList<>(outcome);
        expectations.add(TmsDocuments.inconsistent(regel, label, "EF-005"));
        played(ir, TmsDocuments.caseDocument(instances, expectations));
    }

    private static String vondst(String label, String weight, String deelnemers) {
        return instanceWithRoles(label, "Vondst", Map.of("gewicht", weight, "deelnemers", deelnemers), List.of(),
                Map.of());
    }

    private static String jutter(String label, boolean actief, List<String> vondsten) {
        return instanceWithRoles(label, "Jutter", Map.of(), actief ? List.of("actief") : List.of(),
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

    private static List<String> combined(List<String> a, List<String> b) {
        List<String> out = new ArrayList<>(a);
        out.addAll(b);
        return out;
    }

    // ---------------- the golden's document, with what the golden cannot carry ----------------

    /** The golden's first case: J1 is not actief, so neither rule reaches its fold, and the composite of "drukte"
     *  stops after its first member. "vondstental" counts the same range without a filter. */
    @Test void a_fold_under_a_condition_that_does_not_apply_does_not_fail() throws Exception {
        played(FILTERWACHT, TmsDocuments.caseDocument(j1(false), List.of(value("J1", "vondstental", "2"),
                value("J1", "zware vondsten", null), expected("J1", "druk", false), consistent())));
    }

    /** J1 as above beside J2, actief, in one session: the folds of J2 read V3 and V4, never J1's V2. The reference
     *  interpreter answers this; the ASP runner reports viol("division by numeric zero","V2"). */
    @Test void beside_an_active_jutter_computes_only_its_fold() throws Exception {
        played(FILTERWACHT, TmsDocuments.caseDocument(combined(j1(false), j2j3()), List.of(
                value("J1", "zware vondsten", null), expected("J1", "druk", false),
                value("J2", "zware vondsten", "1"), expected("J2", "druk", true),
                value("J3", "zware vondsten", "0"), expected("J3", "druk", false), consistent())));
    }

    /** V9 (9 by 0) is linked to no Jutter, so no fold has it in its range. The ASP runner reports
     *  viol("division by numeric zero","V9") here too. */
    @Test void a_member_outside_each_range_is_not_filtered() throws Exception {
        played(FILTERWACHT, TmsDocuments.caseDocument(combined(j2j3(), List.of(vondst("V9", "9", "0"))), List.of(
                value("J2", "zware vondsten", "1"), expected("J2", "druk", true), consistent())));
    }

    /** The golden {@code fold-filter-divides-by-zero} (a red case until the card zero-division-channel): J1 actief,
     *  both rules reach the fold and divide 9 by 0, so each deposits EF-005 on J1, and V2 does not count. Until that
     *  card the exec-model branch counted a fault at the fixpoint and gave both, ordered by the name of the rule. */
    @Test void where_the_fold_is_reached_the_division_deposits() throws Exception {
        List<String> expectations = new ArrayList<>(List.of(value("J1", "vondstental", "2"), value("J1", "zware vondsten",
                "1"), expected("J1", "druk", true)));
        expectations.add(TmsDocuments.inconsistent("zware vondsten", "J1", "EF-005"));
        deposits(FILTERWACHT, j1(true), expectations, "drukte", "J1");
    }

    // ---------------- the other places where a fold stands under a condition ----------------

    /** A date of the day precision. */
    static final String DATE = "{\"node\":\"Date\",\"precision\":\"day\"}";

    static final String MODEL = TmsDocuments.model(List.of(
            TmsDocuments.objectType("Jutter", List.of(attribute("zwaarte", WHOLE), attribute("meldtelling", WHOLE),
                    attribute("saldo", WHOLE), attribute("noemer", WHOLE), attribute("peildatum", DATE)),
                    List.of("actief", "druk", "beladen", "ontlast")),
            TmsDocuments.objectType("Vondst", List.of(attribute("gewicht", WHOLE), attribute("deelnemers", WHOLE),
                    attribute("vinddatum", DATE), attribute("ploeg", WHOLE)), List.of()),
            TmsDocuments.objectType("Melding", List.of(attribute("telling", WHOLE)), List.of())),
            List.of(factTypeOneToMany("aanmelding", "aanmelder", "Jutter", "vondst", "Vondst", "vondsten"),
                    factType("JutterHeeftMelding", "melder", "Jutter", "melding", "Melding")));

    /** "zijn gewicht gedeeld door zijn deelnemers is groter dan 2", the quotient typed as a quotient derives its type
     *  (WF-091). */
    static final String FILTER = comparison("greater", "{\"node\":\"Binary\",\"op\":\"dividedBy\",\"left\":"
            + ownRead("gewicht", "Vondst") + ",\"right\":" + ownRead("deelnemers", "Vondst") + ",\"type\":{\"node\":"
            + "\"Number\",\"whole\":false,\"decimals\":0,\"sign\":\"unspecified\"},\"source\":{\"snippet\":\"zijn gewicht"
            + " gedeeld door zijn deelnemers\"}}", number("2"));

    /** "het aantal vondsten van de Jutter die aan de volgende voorwaarde voldoen: {@link #FILTER}". */
    static String fold() {
        return fold(FILTER);
    }

    /** "het aantal vondsten van de Jutter die aan de volgende voorwaarde voldoen: {@code filter}". */
    static String fold(String filter) {
        return aggregation("count", "{\"node\":\"AttributeReference\",\"attribute\":\"vondsten\",\"subjectType\":"
                + "\"Jutter\",\"source\":{\"snippet\":\"vondsten van de Jutter\"}}", "Vondst", sourceRole("aanmelding", "vondst"),
                filter, "null", COUNTING, "het aantal vondsten van de Jutter die aan de volgende voorwaarde voldoen");
    }

    /** "Een Jutter heeft een melding met telling gelijk aan {@code telling}, indien {@code condition}". */
    static String creation(String count, String condition) {
        return "{\"node\":\"ObjectCreation\",\"name\":\"meld\",\"versions\":[{\"createdType\":\"Melding\",\"subject\":"
                + "\"Jutter\",\"initialisations\":[{\"node\":\"MemberInit\",\"member\":\"telling\",\"value\":" + count
                + ",\"characteristic\":false}],\"condition\":" + condition + ",\"validity\":{\"node\":\"Validity\"},"
                + "\"connectingRole\":" + roleBinding("JutterHeeftMelding", "melding") + "}]}";
    }

    /** "De meldtelling van een Jutter moet berekend worden als de telling van de melding van de Jutter". */
    static final String MELDTELLING = computingRule("meldtelling", "BEREKENING", "Jutter", "meldtelling",
            pathRead("telling", "Melding", "JutterHeeftMelding", "melding"), "null");

    /** A quantifier fold over the vondsten of the Jutter under {@code quantifier}, with the subselection {@code filter}
     *  ("null" for none) and the matrix {@code test}. */
    static String quantifierFold(String quantifier, String filter, String test) {
        return "{\"node\":\"QuantifierFold\",\"quantifier\":\"" + quantifier + "\"" + (quantifier.equals("atLeast")
                ? ",\"threshold\":1" : "") + ",\"elementType\":\"Vondst\"" + (filter.equals("null") ? ""
                : ",\"filter\":" + filter) + ",\"test\":" + test + ",\"source\":{\"snippet\":\"het gewicht van "
                + quantifier + " vondsten\"},\"sourceRole\":" + roleBinding("aanmelding", "vondst") + "}";
    }

    /** "{@code kenmerk} indien hij aan alle volgende voorwaarden voldoet: hij is actief; {@code quantifierFold}". */
    static String quantifierRule(String naam, String characteristic, String quantifierFold) {
        return characteristicRule(naam, "Jutter", characteristic, compound("all", 0, List.of(check("actief", false),
                quantifierFold)));
    }

    /** The document of {@link #MODEL} with {@code rules} and object creations {@code creations}. */
    static Path document(List<String> rules, List<String> creations) throws Exception {
        String ir = "{\"format\":\"uchar-ir-v1\",\"model\":" + MODEL + ",\"rules\":{\"rules\":[" + String.join(",", rules)
                + "],\"dayKindDefinitions\":[],\"objectCreations\":[" + String.join(",", creations) + "],\"allocations\":[],"
                + "\"consistencies\":[],\"uniquenesses\":[],\"factCreations\":[],\"startPointDeterminations\":[],"
                + "\"ruleGroups\":[],\"prioritizedDefaults\":[]},\"diagnostics\":[]}";
        Path tmp = Files.createTempFile("filterfout-getuige", ".json");
        tmp.toFile().deleteOnExit();
        return Files.writeString(tmp, ir);
    }

    /** "De zwaarte van een Jutter moet berekend worden als (het aantal vondsten … groter dan 2) plus 1, indien hij
     *  actief is": the fold is an operand of the value. J2: 1 plus 1. */
    @Test void a_fold_in_the_value_of_a_rule_with_condition() throws Exception {
        Path ir = document(List.of(computingRule("zwaarte", "BEREKENING", "Jutter", "zwaarte",
                binary("plus", fold(), number("1")), check("actief", false))), List.of());
        played(ir, TmsDocuments.caseDocument(j1(false), List.of(value("J1", "zwaarte", null), consistent())));
        played(ir, TmsDocuments.caseDocument(j2j3(), List.of(value("J2", "zwaarte", "2"), value("J3", "zwaarte", "1"),
                consistent())));
        deposits(ir, j1(true), List.of(value("J1", "zwaarte", "2")), "zwaarte", "J1");
    }

    /** "Een Jutter heeft een melding met telling gelijk aan 1 indien hij aan alle volgende voorwaarden voldoet: hij is
     *  actief; het aantal vondsten … is groter dan 0", read back through "de telling van de melding van de Jutter". J2
     *  has one heavy vondst and gets the melding; J3 has none. */
    @Test void a_fold_in_the_condition_of_a_creation() throws Exception {
        Path ir = document(List.of(MELDTELLING), List.of(creation(number("1"), compound("all", 0, List.of(
                check("actief", false), comparison("greater", fold(), number("0")))))));
        played(ir, TmsDocuments.caseDocument(j1(false), List.of(value("J1", "meldtelling", null), consistent())));
        played(ir, TmsDocuments.caseDocument(j2j3(), List.of(value("J2", "meldtelling", "1"),
                value("J3", "meldtelling", null), consistent())));
        deposits(ir, j1(true), List.of(value("J1", "meldtelling", "1")), "meld", "J1");
    }

    /** "Een Jutter heeft een melding met telling gelijk aan het aantal vondsten … indien hij actief is": the fold is
     *  the creation's initialisation. The analysis refuses the creation by name ({@link #initialisationRefused}), on
     *  both branches; until card drl-teksttak-op-truth-maintenance the DRL-text branch played it, J2 counting 1 and J3
     *  0, and faulted on J1 at the constraint that held the filter. */
    @Test void a_fold_in_the_initialisation_of_a_creation() throws Exception {
        Path ir = document(List.of(MELDTELLING), List.of(creation(fold(), check("actief", false))));
        initialisationRefused(ir);
    }

    /** The analysis refuses a creation whose member initialisation divides, by name, on both branches: it makes no
     *  §6.5 deposit there. Coverage lost against the destructive exec-model branch, accepted (card tms-omschakeling). */
    private static void initialisationRefused(Path ir) throws Exception {
        for (DroolsBackend.Branch branch : List.of(EXEC, DRL)) {
            String tms = TmsFaultWitnessTest.play(ir, TmsDocuments.caseDocument(j1(false), List.of(consistent())), branch);
            assertTrue(tms.startsWith("Excluded: ") && tms.contains("objectcreatie: meld — een deling in een"
                    + " ledeninitialisatie ('zijn gewicht gedeeld door zijn deelnemers'), waarvan deze tak de"
                    + " §6.5-deponering niet maakt"), branch + ": " + tms);
        }
    }

    /** The filter of a quantifier fold, after "hij is actief" in a composite, under ten minste één (druk), alle
     *  (beladen) and geen (ontlast) of "het gewicht … is groter dan 5". J2's contributors are V3 alone (gewicht 10):
     *  druk and beladen, not ontlast. J3 has none: alle and geen hold vacuously, ten minste één does not. Each
     *  document holds one rule, so the fault names it. */
    @Test void a_fold_in_the_filter_of_a_quantifier_fold() throws Exception {
        for (String[] r : new String[][] {{"drukte", "druk", "atLeast", "true", "false"},
                {"belading", "beladen", "all", "true", "true"}, {"ontlasting", "ontlast", "none", "false", "true"}}) {
            Path ir = document(List.of(quantifierRule(r[0], r[1], quantifierFold(r[2], FILTER,
                    comparison("greater", ownRead("gewicht", "Vondst"), number("5"))))), List.of());
            played(ir, TmsDocuments.caseDocument(j1(false), List.of(expected("J1", r[1], false), consistent())));
            played(ir, TmsDocuments.caseDocument(j2j3(), List.of(expected("J2", r[1], Boolean.parseBoolean(r[3])),
                    expected("J3", r[1], Boolean.parseBoolean(r[4])), consistent())));
            // V2 divides by 0 and is no contributor; V1 (gewicht 10) is, so as J2
            deposits(ir, j1(true), List.of(expected("J1", r[1], Boolean.parseBoolean(r[3]))), r[0], "J1");
        }
    }

    /** A matrix that divides, "het gewicht van ten minste één van de vondsten van de Jutter is groter dan 100 gedeeld
     *  door zijn deelnemers", after "hij is actief": the DRL-text branch wrote the matrix on the element pattern of its
     *  accumulate as well, the destructive exec-model branch asked it after the accumulate (until card
     *  tms-omschakeling, 2026-10-10). J4's V6 (60 above 100 by 2) holds; the
     *  members of J2 and J3 do not, and divide by no 0. Where J1 is actief, V1 (10 above 50) does not hold, so the fold
     *  goes on to V2, 100 by 0: a leeg quotient, which does not hold, and the deposit. */
    @Test void a_division_in_the_matrix_of_a_quantifier_fold() throws Exception {
        String test = comparison("greater", ownRead("gewicht", "Vondst"), "{\"node\":\"Binary\",\"op\":\"dividedBy\","
                + "\"left\":" + number("100") + ",\"right\":" + ownRead("deelnemers", "Vondst") + ",\"type\":{\"node\":"
                + "\"Number\",\"whole\":false,\"decimals\":0,\"sign\":\"unspecified\"},\"source\":{\"snippet\":\"100"
                + " gedeeld door zijn deelnemers\"}}");
        Path ir = document(List.of(quantifierRule("drukte", "druk", quantifierFold("atLeast", "null", test))), List.of());
        played(ir, TmsDocuments.caseDocument(j1(false), List.of(expected("J1", "druk", false), consistent())));
        played(ir, TmsDocuments.caseDocument(combined(j2j3(), List.of(jutter("J4", true, List.of("V6")),
                vondst("V6", "60", "2"))), List.of(expected("J2", "druk", false), expected("J3", "druk", false),
                expected("J4", "druk", true), consistent())));
        deposits(ir, j1(true), List.of(expected("J1", "druk", false)), "drukte", "J1");
    }

    /** The fault of a matrix, since the division of the row above deposits: "het gewicht van ten minste één van de
     *  vondsten van de Jutter: de wortel van zijn gewicht is groter dan 3", after "hij is actief". J5's V8 (gewicht 4,
     *  the root 2) does not hold, so the fold goes on to V9 (gewicht 2), whose root is irrational: the fault. J6 reads
     *  V10 (gewicht 16, the root 4) first, which settles the fold, so V9 is not read and J6 is druk. J7 is not actief,
     *  so the fold is not read. */
    @Test void a_root_in_the_matrix_of_a_quantifier_fold_faults_where_it_is_reached() throws Exception {
        String test = comparison("greater", TmsDocuments.root(ownRead("gewicht", "Vondst"), "de wortel van zijn gewicht"),
                number("3"));
        Path ir = document(List.of(quantifierRule("drukte", "druk", quantifierFold("atLeast", "null", test))), List.of());
        fails(ir, TmsDocuments.caseDocument(List.of(jutter("J5", true, List.of("V8", "V9")), vondst("V8", "4", "1"),
                vondst("V9", "2", "1")), List.of(consistent())), "drukte", "2");
        played(ir, TmsDocuments.caseDocument(List.of(jutter("J6", true, List.of("V10", "V9")), vondst("V10", "16", "1"),
                vondst("V9", "2", "1")), List.of(expected("J6", "druk", true), consistent())));
        played(ir, TmsDocuments.caseDocument(List.of(jutter("J7", false, List.of("V9")), vondst("V9", "2", "1")),
                List.of(expected("J7", "druk", false), consistent())));
    }

    /** The bootlengte of {@code TmsFilterFaultWitnessTest}: the sum of the lengte of all boten of all jutters of the
     *  Haven the root of whose lengte is greater than 2, or 0, over a path that walks back to the jutters and forward to
     *  their boot. H1's jutters J1 and J2 share B1 (lengte 9, the root 3): 9, once. B9 (lengte 2) is the boot of no
     *  jutter of H1; where J3 of H1 uses it, the fold reaches it and takes the root of 2: the fault. Until the card
     *  zero-division-channel the filter divided the lengte by the breedte and B9 divided 4 by 0. */
    @Test void a_member_that_the_path_does_not_reach_is_not_filtered() throws Exception {
        Path ir = TmsDocuments.document(TmsFilterFaultWitnessTest.HAVENMODEL, TmsFilterFaultWitnessTest.BOOT_LENGTH);
        String b1 = instanceWithRoles("B1", "Boot", Map.of("lengte", "9", "breedte", "3"), List.of(), Map.of());
        String b9 = instanceWithRoles("B9", "Boot", Map.of("lengte", "2", "breedte", "0"), List.of(), Map.of());
        String j1 = instanceWithRoles("J1", "Jutter", Map.of(), List.of(), Map.of("boot", List.of("B1")));
        String j2 = instanceWithRoles("J2", "Jutter", Map.of(), List.of(), Map.of("boot", List.of("B1")));
        played(ir, TmsDocuments.caseDocument(List.of(instanceWithRoles("H1", "Haven", Map.of(), List.of(),
                Map.of("jutters", List.of("J1", "J2"))), j1, j2, b1, b9), List.of(value("H1", "bootlengte", "9"),
                consistent())));
        String j3 = instanceWithRoles("J3", "Jutter", Map.of(), List.of(), Map.of("boot", List.of("B9")));
        fails(ir, TmsDocuments.caseDocument(List.of(instanceWithRoles("H1", "Haven", Map.of(), List.of(),
                Map.of("jutters", List.of("J1", "J2", "J3"))), j1, j2, j3, b1, b9), List.of(consistent())),
                "bootlengte", "2");
    }

    // ---------------- the order of an "alle" composite around a fold ----------------

    /** "zijn gewicht gedeeld door zijn ploeg is groter dan 2": the subselection of member F, which divides by another
     *  attribute than {@link #FILTER} does, so that a case can make one of the two folds fault and not the other, and
     *  each names a division of its own. */
    static final String PLOEGFILTER = comparison("greater", "{\"node\":\"Binary\",\"op\":\"dividedBy\",\"left\":"
            + ownRead("gewicht", "Vondst") + ",\"right\":" + ownRead("ploeg", "Vondst") + ",\"type\":{\"node\":"
            + "\"Number\",\"whole\":false,\"decimals\":0,\"sign\":\"unspecified\"},\"source\":{\"snippet\":"
            + "\"zijn gewicht gedeeld door zijn ploeg\"}}", number("2"));

    /** The members of "Een Jutter is druk indien hij aan alle volgende voorwaarden voldoet", by letter: A "hij is
     *  actief"; F "{@link #fold(String)} over {@link #PLOEGFILTER} is groter dan 0", a fold whose subselection divides
     *  by the ploeg; D "zijn saldo gedeeld door zijn noemer is groter dan 1"; K "het gewicht van ten minste één van de
     *  vondsten van de Jutter die {@link #FILTER} is groter dan 5", a quantifier fold whose subselection divides by the
     *  deelnemers. */
    static String member(char letter) {
        return switch (letter) {
            case 'A' -> check("actief", false);
            case 'F' -> comparison("greater", fold(PLOEGFILTER), number("0"));
            case 'D' -> comparison("greater", TmsFilterFaultWitnessTest.quotient(ownRead("saldo", "Jutter"),
                    ownRead("noemer", "Jutter")), number("1"));
            case 'K' -> quantifierFold("atLeast", FILTER, comparison("greater", ownRead("gewicht", "Vondst"), number("5")));
            default -> throw new IllegalArgumentException("geen lid " + letter);
        };
    }

    /** One case of the composite: the Jutter it is about, its instances, and per member its verdict there: "W" it
     *  holds, "O" it does not, and a "D" after either where its evaluation divides by 0 and deposits (until the card
     *  zero-division-channel the dividend of that division, a fault). */
    private record Case(String label, List<String> instances, Map<Character, String> verdicts) {}

    private static String jutter(String label, boolean actief, String balance, String denominator, List<String> vondsten) {
        return instanceWithRoles(label, "Jutter", Map.of("saldo", balance, "noemer", denominator),
                actief ? List.of("actief") : List.of(), Map.of("vondsten", vondsten));
    }

    private static String vondst(String label, String weight, String deelnemers, String ploeg) {
        return instanceWithRoles(label, "Vondst", Map.of("gewicht", weight, "deelnemers", deelnemers, "ploeg", ploeg),
                List.of(), Map.of());
    }

    /** J1 is not actief, and every other member divides by 0 on its way: F at V1, 10 by its ploeg 0, and V2 counts (9
     *  by 3), so F holds; D, 5 by the noemer 0, which does not hold; K at V2, 9 by its deelnemers 0, and V1 is in the
     *  subselection and holds the matrix, so K holds (the subselection is settled for every vondst before the matrix,
     *  ch. 3.9a). J2 is actief and every member holds: V3, 10 by 2 in both quotients and gewicht 10 above 5, counts for
     *  F and for K, and 6 by 2 is 3. J3 is actief, its one vondst V5, 2 by 2 in both quotients, counts for neither, so F
     *  and K do not hold, and its noemer is 0. J4 is actief and D holds; its one vondst V7 divides 10 by its ploeg 0 in
     *  F, which then counts nothing and does not hold, and does not count for K, 10 by its deelnemers 5 being 2: whether
     *  the composite deposits there follows the order of F and K alone. Until the card zero-division-channel each of
     *  those divisions was a fault, named by its dividend. */
    static final List<Case> CASES = List.of(
            new Case("J1", List.of(jutter("J1", false, "5", "0", List.of("V1", "V2")), vondst("V1", "10", "2", "0"),
                    vondst("V2", "9", "0", "3")), Map.of('A', "O", 'F', "WD", 'D', "OD", 'K', "WD")),
            new Case("J2", List.of(jutter("J2", true, "6", "2", List.of("V3", "V4")), vondst("V3", "10", "2", "2"),
                    vondst("V4", "3", "3", "3")), Map.of('A', "W", 'F', "W", 'D', "W", 'K', "W")),
            new Case("J3", List.of(jutter("J3", true, "5", "0", List.of("V5")), vondst("V5", "2", "2", "2")),
                    Map.of('A', "W", 'F', "O", 'D', "OD", 'K', "O")),
            new Case("J4", List.of(jutter("J4", true, "6", "2", List.of("V7")), vondst("V7", "10", "5", "0")),
                    Map.of('A', "W", 'F', "OD", 'D', "W", 'K', "O")));

    /** The composite's verdict on {@code g} by RUNTIME-SEMANTICS ch. 5.10 and 5.11: the members are read in
     *  {@code order} and the first that does not hold decides; "W" where every member holds and "O" where one does not,
     *  with a "D" after it where a member read divided by 0 and deposited. */
    static String verdict(List<Character> order, Case g) {
        boolean deposited = false;
        for (char c : order) {
            String o = g.verdicts().get(c);
            deposited |= o.endsWith("D");
            if (o.startsWith("O")) return deposited ? "OD" : "O";
        }
        return deposited ? "WD" : "W";
    }

    /** What {@code branch} gives on {@code caseDocument}: "gespeeld", the verdict of an expectation that failed, or the fault with
     *  its class. */
    static String observation(Path ir, JsonNode caseDocument, DroolsBackend.Branch branch) throws Exception {
        try {
            String v = CaseRunner.playCase(ir, caseDocument, branch);
            return v == null ? "gespeeld" : v;
        } catch (RuntimeException e) {
            return e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    /** The expectations of the case of {@code label} where the verdict of the rule {@code regel} is {@code verdict}:
     *  druk where it holds, and the deposit of EF-005 where a member deposited, else consistent. */
    static List<String> expectations(String verdict, String regel, String label) {
        return List.of(expected(label, "druk", verdict.startsWith("W")), verdict.endsWith("D")
                ? TmsDocuments.inconsistent(regel, label, "EF-005") : consistent());
    }

    /** Every order of the four members, on the four cases, on both branches: a member is evaluated only where every
     *  member before it holds (ch. 5.10, 5.11), so the verdict, and whether a division by 0 deposits, follows the order;
     *  on J4 the composite deposits where the order puts F before K, and not where it puts K first. Until the card
     *  zero-division-channel a division by 0 was a fault that ended the evaluation, and the paragraph below is of that
     *  time: On J1 each branch names the division of the first member, F's, D's or
     *  K's, so a branch that evaluated the two folds in the other order names the other fold's division; on J4 it
     *  faults where the order puts F before K, and answers where it puts K first. The reference interpreter's message
     *  names no division ("division by numeric zero"); it faults on J4 in each of the twelve orders with F before K and
     *  answers in each of the other twelve, and agrees with every other cell (measured 2026-10-09 on the RegelSpraak
     *  frontend's documents of all 24 orders; the ASP runner agrees cell for cell). */
    @Test void the_members_of_an_all_compound_in_every_order() throws Exception {
        List<String> deviating = new ArrayList<>();
        for (List<Character> order : TmsDocuments.permutations(List.of('A', 'F', 'D', 'K'))) {
            List<String> members = new ArrayList<>();
            for (char c : order) members.add(member(c));
            Path ir = document(List.of(characteristicRule("drukte", "Jutter", "druk", compound("all", 0, members))), List.of());
            for (Case g : CASES) {
                String verdict = verdict(order, g);
                JsonNode caseDocument = TmsDocuments.caseDocument(g.instances(), expectations(verdict, "drukte", g.label()));
                for (DroolsBackend.Branch branch : List.of(EXEC, DRL)) {
                    String received = observation(ir, caseDocument, branch);
                    if (!received.equals("gespeeld"))
                        deviating.add(order + " " + g.label() + " " + branch + ": verwacht " + verdict + ", kreeg " + received);
                }
            }
        }
        assertEquals(List.of(), deviating);
    }

    /** "de telling van de melding van de Jutter is groter dan 0", a member that reads through a role, beside
     *  {@link #fold()} in both orders: on the exec-model branch such a condition goes on the subject bound again after
     *  the join to the melding, and the members after the fold on the subject bound again from that. J1's V2 divides 9
     *  by 0 and its melding counts 0: with the fold first the composite deposits (a fault until the card
     *  zero-division-channel) and is not druk, after the member the fold is not read. J2's V3
     *  (10 by 2) counts and its melding counts 3: druk. J3's V5 (2 by 2) does not count: not druk. */
    @Test void a_navigated_member_beside_the_fold_in_both_orders() throws Exception {
        String navigated = comparison("greater", pathRead("telling", "Melding", "JutterHeeftMelding", "melding"),
                number("0"));
        String foldMember = comparison("greater", fold(), number("0"));
        List<String> j1 = List.of(instanceWithRoles("J1", "Jutter", Map.of(), List.of(), Map.of("vondsten",
                List.of("V1", "V2"), "melding", List.of("M1"))), vondst("V1", "10", "2"), vondst("V2", "9", "0"),
                instanceWithRoles("M1", "Melding", Map.of("telling", "0"), List.of(), Map.of()));
        List<String> j2 = List.of(instanceWithRoles("J2", "Jutter", Map.of(), List.of(), Map.of("vondsten",
                List.of("V3"), "melding", List.of("M2"))), vondst("V3", "10", "2"),
                instanceWithRoles("M2", "Melding", Map.of("telling", "3"), List.of(), Map.of()));
        List<String> j3 = List.of(instanceWithRoles("J3", "Jutter", Map.of(), List.of(), Map.of("vondsten",
                List.of("V5"), "melding", List.of("M3"))), vondst("V5", "2", "2"),
                instanceWithRoles("M3", "Melding", Map.of("telling", "3"), List.of(), Map.of()));
        Path foldFirst = document(List.of(characteristicRule("drukte", "Jutter", "druk", compound("all", 0,
                List.of(foldMember, navigated)))), List.of());
        Path memberFirst = document(List.of(characteristicRule("drukte", "Jutter", "druk", compound("all", 0,
                List.of(navigated, foldMember)))), List.of());
        deposits(foldFirst, j1, List.of(expected("J1", "druk", false)), "drukte", "J1");
        played(memberFirst, TmsDocuments.caseDocument(j1, List.of(expected("J1", "druk", false), consistent())));
        for (Path ir : List.of(foldFirst, memberFirst)) {
            played(ir, TmsDocuments.caseDocument(j2, List.of(expected("J2", "druk", true), consistent())));
            played(ir, TmsDocuments.caseDocument(j3, List.of(expected("J3", "druk", false), consistent())));
        }
    }

    /** An instance as {@link TmsDocuments#instanceWithRoles} writes it, with the dates of {@code dates} added. */
    private static String withDates(String instance, Map<String, String> dates) {
        List<String> d = new ArrayList<>();
        dates.forEach((naam, iso) -> d.add("\"" + naam + "\":{\"date\":\"" + iso + "\"}"));
        return instance.contains("\"attributes\":{}")
                ? instance.replace("\"attributes\":{}", "\"attributes\":{" + String.join(",", d) + "}")
                : instance.replace("\"attributes\":{", "\"attributes\":{" + String.join(",", d) + ",");
    }

    /** "de eerste van de vinddatum van alle vondsten van de Jutter die {@link #FILTER} is later dan zijn peildatum"
     *  beside "hij is actief", in both orders, on both branches (until card drl-teksttak-op-truth-maintenance the
     *  DRL-text branch refused a fold of dates in a condition by name, measured 2026-10-09). J1 is not actief and its V2 divides 9 by 0: with the fold first the
     *  composite deposits (a fault until the card zero-division-channel) and is not druk, after "hij is actief" the fold
     *  is not read. J2 is actief, and its one contributor V3 was found
     *  on 2026-01-05, later than its peildatum 2020-01-01. */
    @Test void a_date_fold_beside_a_member_in_both_orders() throws Exception {
        String dateFold = comparison("later", aggregation("first", ownRead("vinddatum", "Vondst"), "Vondst",
                sourceRole("aanmelding", "vondst"), FILTER, "null", DATE, "de eerste van de vinddatum van alle vondsten van de"
                        + " Jutter die aan de volgende voorwaarde voldoen"), ownRead("peildatum", "Jutter"));
        List<String> j1 = List.of(withDates(jutter("J1", false, "0", "1", List.of("V1", "V2")), Map.of("peildatum",
                "2020-01-01")), withDates(vondst("V1", "10", "2"), Map.of("vinddatum", "2026-01-05")),
                withDates(vondst("V2", "9", "0"), Map.of("vinddatum", "2026-01-06")));
        List<String> j2 = List.of(withDates(jutter("J2", true, "0", "1", List.of("V3", "V4")), Map.of("peildatum",
                "2020-01-01")), withDates(vondst("V3", "10", "2"), Map.of("vinddatum", "2026-01-05")),
                withDates(vondst("V4", "3", "3"), Map.of("vinddatum", "2019-01-01")));
        Path foldFirst = document(List.of(characteristicRule("drukte", "Jutter", "druk", compound("all", 0,
                List.of(dateFold, check("actief", false))))), List.of());
        Path actiefFirst = document(List.of(characteristicRule("drukte", "Jutter", "druk", compound("all", 0,
                List.of(check("actief", false), dateFold)))), List.of());
        deposits(foldFirst, j1, List.of(expected("J1", "druk", false)), "drukte", "J1");
        played(actiefFirst, TmsDocuments.caseDocument(j1, List.of(expected("J1", "druk", false), consistent())));
        for (Path ir : List.of(foldFirst, actiefFirst))
            played(ir, TmsDocuments.caseDocument(j2, List.of(expected("J2", "druk", true), consistent())));
    }

    /** "de eerste van de vinddatum van alle vondsten van de Jutter die aan de volgende voorwaarde voldoen:
     *  {@code filter} is later dan zijn peildatum". */
    private static String dateFold(String filter) {
        return comparison("later", aggregation("first", ownRead("vinddatum", "Vondst"), "Vondst",
                sourceRole("aanmelding", "vondst"), filter, "null", DATE, "de eerste van de vinddatum van alle vondsten"
                        + " van de Jutter die aan de volgende voorwaarde voldoen"), ownRead("peildatum", "Jutter"));
    }

    /** {@link #dateFold} over {@link #FILTER} as the third member after "hij is actief" and "hij is beladen", under
     *  geen, ten minste 1, ten hoogste 1 and precies 1, and under ten minste 1 after "hij is beladen", nested in an
     *  alle composite after "hij is actief". J1 is actief and beladen and its V2 divides 9 by 0. Every composite
     *  settles before the fold (ch. 5.10), so the reference interpreter answers: not druk, druk, not druk, not druk,
     *  and nested druk (measured 2026-10-09 on the RegelSpraak frontend's documents of these forms; the ASP runner
     *  agrees). The destructive exec-model branch lowered such a composite as one test after the accumulates of all its
     *  folds, and Drools folded those for every subject before the test read the members in their order, so that branch
     *  divided 9 by 0 there and refused the form by name since 2026-10-09. The exec-model branch with truth maintenance
     *  plays it as the reference interpreter answers ({@code TmsLikeRefWitnessTest} holds these documents too), on
     *  both branches since card drl-teksttak-op-truth-maintenance, before which the DRL-text branch refused a fold of
     *  dates in a condition. The control: the same composites over "zijn gewicht is groter dan 5", which cannot fault,
     *  play on the exec-model branch as the reference interpreter answers, on J1 and on J2, neither actief nor beladen,
     *  whose V3 is heavy and was found after its peildatum, so that geen, ten minste 1 and precies 1 read the fold: not
     *  druk, druk, druk, druk. */
    @Test void a_fold_that_can_fail_under_another_quantifier_than_all_plays_like_ref() throws Exception {
        List<String> j1 = List.of(withDates(instanceWithRoles("J1", "Jutter", Map.of(), List.of("actief", "beladen"),
                Map.of("vondsten", List.of("V1", "V2"))), Map.of("peildatum", "2020-01-01")),
                withDates(vondst("V1", "10", "2"), Map.of("vinddatum", "2026-01-05")),
                withDates(vondst("V2", "9", "0"), Map.of("vinddatum", "2026-01-06")));
        List<String> j2 = List.of(withDates(instanceWithRoles("J2", "Jutter", Map.of(), List.of(),
                Map.of("vondsten", List.of("V3", "V4"))), Map.of("peildatum", "2020-01-01")),
                withDates(vondst("V3", "10", "2"), Map.of("vinddatum", "2026-01-05")),
                withDates(vondst("V4", "3", "0"), Map.of("vinddatum", "2019-01-01")));
        String zwaar = comparison("greater", ownRead("gewicht", "Vondst"), number("5"));
        // per quantifier: its wire spelling and threshold, its name, druk on J1, druk on J2 (control)
        String[][] quantifiers = {{"none", "0", "GEEN", "false", "false"}, {"atLeast", "1", "TEN_MINSTE", "true", "true"},
                {"atMost", "1", "TEN_HOOGSTE", "false", "true"}, {"exactly", "1", "PRECIES", "false", "true"}};
        List<String> deviating = new ArrayList<>();
        for (String[] k : quantifiers) {
            int threshold = Integer.parseInt(k[1]);
            Path ir = document(List.of(characteristicRule("drukte", "Jutter", "druk", compound(k[0], threshold,
                    List.of(check("actief", false), check("beladen", false), dateFold(FILTER))))), List.of());
            likeRef(ir, TmsDocuments.caseDocument(j1, List.of(expected("J1", "druk", Boolean.parseBoolean(k[3])),
                    consistent())), k[0] + " " + threshold, deviating);
            Path check = document(List.of(characteristicRule("drukte", "Jutter", "druk", compound(k[0], threshold,
                    List.of(check("actief", false), check("beladen", false), dateFold(zwaar))))), List.of());
            for (JsonNode caseDocument : List.of(TmsDocuments.caseDocument(j1, List.of(expected("J1", "druk",
                    Boolean.parseBoolean(k[3])), consistent())), TmsDocuments.caseDocument(j2, List.of(expected("J2", "druk",
                    Boolean.parseBoolean(k[4])), consistent())))) {
                for (DroolsBackend.Branch branch : List.of(EXEC, DRL)) {
                    String v = TmsFaultWitnessTest.play(check, caseDocument, branch);
                    if (!v.equals("gespeeld")) deviating.add(k[0] + " controle " + branch + ": " + v);
                }
            }
        }
        Path nested = document(List.of(characteristicRule("drukte", "Jutter", "druk", compound("all", 0, List.of(
                check("actief", false), compound("atLeast", 1, List.of(check("beladen", false),
                        dateFold(FILTER))))))), List.of());
        likeRef(nested, TmsDocuments.caseDocument(j1, List.of(expected("J1", "druk", true), consistent())), "atLeast 1",
                deviating);
        assertEquals(List.of(), deviating);
    }

    /** Adds to {@code deviating} where a branch does not play {@code caseDocument} on {@code ir}, a composite ({@code source})
     *  whose fold divides, with the reference interpreter's answer. Until card drl-teksttak-op-truth-maintenance the
     *  DRL-text branch refused the rule "drukte" here ("drl-transpiler MARKER: regel: drukte — …"). */
    private static void likeRef(Path ir, JsonNode caseDocument, String source, List<String> deviating) throws Exception {
        for (DroolsBackend.Branch branch : List.of(EXEC, DRL)) {
            String v = TmsFaultWitnessTest.play(ir, caseDocument, branch);
            if (!v.equals("gespeeld")) deviating.add(source + " " + branch + ": " + v);
        }
    }

    /** J1 with the characteristics {@code kenmerken}, the peildatum 2020-01-01 and V1, 10 by 2, found on 2026-01-05,
     *  and where {@code withV2} also V2, 9 by 0, found on 2026-01-06. */
    private static List<String> asOfJutter(List<String> kenmerken, boolean withV2) {
        List<String> out = new ArrayList<>(List.of(withDates(instanceWithRoles("J1", "Jutter", Map.of(), kenmerken,
                Map.of("vondsten", withV2 ? List.of("V1", "V2") : List.of("V1"))), Map.of("peildatum", "2020-01-01")),
                withDates(vondst("V1", "10", "2"), Map.of("vinddatum", "2026-01-05"))));
        if (withV2) out.add(withDates(vondst("V2", "9", "0"), Map.of("vinddatum", "2026-01-06")));
        return out;
    }

    /** {@link #dateFold} over {@link #FILTER} in a member the short circuit of ch. 5.10 never skips, so that the
     *  reference interpreter reads the fold wherever it reads the composite: as the only member under geen; before "hij
     *  is actief" under ten minste 1; as the first member of an alle composite that is the only member under geen; and
     *  as the second member after "hij is actief" under precies 1 of two members, and under ten minste 2 and under ten
     *  hoogste 1 of three, with "hij is beladen" third, where no stop trigger can fire before the fold whatever the
     *  first member gives. The exec-model branch folds there what the reference interpreter reads, so it lowers these
     *  forms as before the refusal and plays them as the reference interpreter answers: with V1 alone, 10 by 2 and
     *  found after the peildatum, not druk, druk, druk, not druk, druk and not druk; with V2 beside it, 9 by 0, the
     *  deposit EF-005 in each, V2 no contributor, under the characteristics of that case: not druk, druk, druk, druk,
     *  not druk and druk (a fault in each until the card zero-division-channel). The guard: where the threshold settles the composite before any member, ten hoogste 1 and ten minste 2
     *  over the fold alone, and where the fold is the second member of an alle composite that is the only member under
     *  geen, after "hij is actief" on a J1 that is not actief, the reference interpreter reads no fold and answers
     *  (druk, not druk, druk), and the branch before the refusal divided 9 by 0: the destructive exec-model branch kept
     *  these forms refused, and the exec-model branch with truth maintenance plays them as the reference interpreter
     *  answers ({@code TmsLikeRefWitnessTest} holds these documents too). Measured 2026-10-09 on the RegelSpraak
     *  frontend's documents of these forms, with the ASP runner and the branch before the refusal ({@code 4ada319d}).
     *  */
    @Test void a_fold_that_can_fail_in_a_member_that_is_always_read_plays_like_ref() throws Exception {
        String actief = check("actief", false), beladen = check("beladen", false), fold = dateFold(FILTER);
        List<String> deviating = new ArrayList<>();
        // per form: its composite, J1's characteristics with V1 alone and its druk there, and with V2 beside V1 and its
        // druk there
        Map<String, Object[]> playing = new java.util.LinkedHashMap<>();
        playing.put("geen, enig lid", new Object[] {compound("none", 0, List.of(fold)), List.of(), false,
                List.of(), false});
        playing.put("ten minste 1, eerste lid", new Object[] {compound("atLeast", 1, List.of(fold, actief)),
                List.of(), true, List.of("actief"), true});
        playing.put("geen over alle, eerste lid", new Object[] {compound("none", 0, List.of(compound("all", 0,
                List.of(fold, actief)))), List.of(), true, List.of(), true});
        playing.put("precies 1, tweede van twee", new Object[] {compound("exactly", 1, List.of(actief, fold)),
                List.of("actief"), false, List.of(), true});
        playing.put("ten minste 2, tweede van drie", new Object[] {compound("atLeast", 2, List.of(actief, fold,
                beladen)), List.of("beladen"), true, List.of(), false});
        playing.put("ten hoogste 1, tweede van drie", new Object[] {compound("atMost", 1, List.of(actief, fold,
                beladen)), List.of("actief"), false, List.of(), true});
        for (Map.Entry<String, Object[]> v : playing.entrySet()) {
            Object[] w = v.getValue();
            Path ir = document(List.of(characteristicRule("drukte", "Jutter", "druk", (String) w[0])), List.of());
            @SuppressWarnings("unchecked") List<String> kenmerken = (List<String>) w[1], withV2 = (List<String>) w[3];
            JsonNode answer = TmsDocuments.caseDocument(asOfJutter(kenmerken, false), List.of(expected("J1", "druk",
                    (Boolean) w[2]), consistent()));
            JsonNode deposit = TmsDocuments.caseDocument(asOfJutter(withV2, true), List.of(expected("J1", "druk",
                    (Boolean) w[4]), TmsDocuments.inconsistent("drukte", "J1", "EF-005")));
            for (DroolsBackend.Branch branch : List.of(EXEC, DRL)) {
                String given = TmsFaultWitnessTest.play(ir, answer, branch);
                if (!given.equals("gespeeld")) deviating.add(v.getKey() + " " + branch + ": " + given);
                given = TmsFaultWitnessTest.play(ir, deposit, branch);
                if (!given.equals("gespeeld")) deviating.add(v.getKey() + " met V2 " + branch + ": " + given);
            }
        }
        likeRef(document(List.of(characteristicRule("drukte", "Jutter", "druk", compound("atMost", 1,
                List.of(fold)))), List.of()), TmsDocuments.caseDocument(asOfJutter(List.of(), true),
                List.of(expected("J1", "druk", true), consistent())), "atMost 1", deviating);
        likeRef(document(List.of(characteristicRule("drukte", "Jutter", "druk", compound("atLeast", 2,
                List.of(fold)))), List.of()), TmsDocuments.caseDocument(asOfJutter(List.of(), true),
                List.of(expected("J1", "druk", false), consistent())), "atLeast 2", deviating);
        likeRef(document(List.of(characteristicRule("drukte", "Jutter", "druk", compound("none", 0, List.of(
                compound("all", 0, List.of(actief, fold)))))), List.of()), TmsDocuments.caseDocument(
                asOfJutter(List.of(), true), List.of(expected("J1", "druk", true), consistent())), "none 0",
                deviating);
        assertEquals(List.of(), deviating);
    }

    /** "het aantal vondsten van de Jutter die aan de volgende voorwaarde voldoen: zijn gewicht is groter dan 50", a fold
     *  whose subselection cannot fault. */
    static String zwaarFold() {
        return aggregation("count", "{\"node\":\"AttributeReference\",\"attribute\":\"vondsten\",\"subjectType\":"
                + "\"Jutter\",\"source\":{\"snippet\":\"vondsten van de Jutter\"}}", "Vondst", sourceRole("aanmelding", "vondst"),
                comparison("greater", ownRead("gewicht", "Vondst"), number("50")), "null", COUNTING,
                "het aantal vondsten van de Jutter die aan de volgende voorwaarde voldoen");
    }

    /** "De zwaarte van een Jutter moet berekend worden als {@link #fold()} indien hij aan alle volgende voorwaarden
     *  voldoet: - {@link #zwaarFold()} is groter dan 0", and the creation of a melding with that telling under that
     *  condition: the value is read only where the condition holds, so the fold in the condition comes first. J1 has no
     *  vondst above 50. J2's V6 (60 by 2) is above 50 and counts, its V4 (3 by 3) does not: 1. J4 has V6 and V2, so the
     *  condition holds and the value divides 9 by 0: V6 counts, V2 deposits (a fault until the card
     *  zero-division-channel). */
    @Test void a_fold_in_the_value_under_a_fold_in_the_condition() throws Exception {
        String condition = compound("all", 0, List.of(comparison("greater", zwaarFold(), number("0"))));
        Path regel = document(List.of(computingRule("zwaarte", "BEREKENING", "Jutter", "zwaarte", fold(), condition)),
                List.of());
        Path creation = document(List.of(MELDTELLING), List.of(creation(fold(), condition)));
        String v6 = vondst("V6", "60", "2");
        played(regel, TmsDocuments.caseDocument(j1(false), List.of(value("J1", "zwaarte", null), consistent())));
        List<String> j2 = List.of(jutter("J2", false, List.of("V6", "V4")), v6, vondst("V4", "3", "3"));
        played(regel, TmsDocuments.caseDocument(j2, List.of(value("J2", "zwaarte", "1"), consistent())));
        List<String> j4 = List.of(jutter("J4", false, List.of("V6", "V2")), v6, vondst("V2", "9", "0"));
        deposits(regel, j4, List.of(value("J4", "zwaarte", "1")), "zwaarte", "J4");
        // the creation, whose initialisation divides, the analysis refuses by name on both branches; until card
        // drl-teksttak-op-truth-maintenance the DRL-text branch played it on J1 and J2 and faulted on J4
        initialisationRefused(creation);
    }

    /** A subselection that divides by an attribute another rule writes on the element: "De deelnemers van een Vondst
     *  moet berekend worden als zijn ploeg plus 1" beside "De zwaarte van een Jutter moet berekend worden als
     *  {@link #fold()}", and beside "Een Jutter is druk indien hij aan alle volgende voorwaarden voldoet: - het gewicht van
     *  ten minste één van de vondsten van de Jutter die {@link #FILTER} is groter dan 5", each in both orders. The case
     *  supplies deelnemers 10 and 3, which the rule overwrites with 2 and 1, so the fold reads 10 by 2 and 3 by 1: it
     *  counts both, and V1, gewicht 10, holds the quantifier fold; the input values would count none and hold nothing. */
    @Test void a_dividing_filter_over_an_overwritten_divisor_in_every_rule_order() throws Exception {
        String deelnemers = computingRule("deelnemers", "BEREKENING", "Vondst", "deelnemers",
                binary("plus", ownRead("ploeg", "Vondst"), number("1")), "null");
        List<String> instances = List.of(jutter("J1", false, List.of("V1", "V2")),
                instanceWithRoles("V1", "Vondst", Map.of("gewicht", "10", "ploeg", "1", "deelnemers", "10"), List.of(),
                        Map.of()),
                instanceWithRoles("V2", "Vondst", Map.of("gewicht", "3", "ploeg", "0", "deelnemers", "3"), List.of(),
                        Map.of()));
        String zwaarte = computingRule("zwaarte", "BEREKENING", "Jutter", "zwaarte", fold(), "null");
        for (List<String> rules : TmsDocuments.permutations(List.of(deelnemers, zwaarte)))
            played(document(rules, List.of()), TmsDocuments.caseDocument(instances,
                    List.of(value("J1", "zwaarte", "2"), consistent())));
        String drukte = characteristicRule("drukte", "Jutter", "druk", compound("all", 0, List.of(member('K'))));
        for (List<String> rules : TmsDocuments.permutations(List.of(deelnemers, drukte)))
            played(document(rules, List.of()), TmsDocuments.caseDocument(instances,
                    List.of(expected("J1", "druk", true), consistent())));
    }

    /** "De zwaarte van een Jutter moet berekend worden als zijn saldo gedeeld door zijn noemer indien hij aan alle
     *  volgende voorwaarden voldoet: - {@link #zwaarFold()} is groter dan 0 - hij is actief": "hij is actief" follows the
     *  fold and goes on the subject bound again. The DRL-text branch's companion for a division by a leeg noemer reads
     *  the condition's inline constraint and joins, so it takes the condition lowered without that move and still asks
     *  "hij is actief". J5 is not actief and has no noemer: the rule does not fire and nothing is deposited. J6 is actief,
     *  6 by 2: 3. J7 is actief and has no noemer: the division by leeg deposits. A guard of the move: on the branch before
     *  it the companion read "hij is actief" as well; a companion fed the moved condition's inline deposits for J5
     *  (mutant, measured 2026-10-09). */
    @Test void a_companion_reads_the_condition_without_move() throws Exception {
        String division = "{\"node\":\"Binary\",\"op\":\"dividedBy\",\"left\":" + ownRead("saldo", "Jutter") + ",\"right\":"
                + ownRead("noemer", "Jutter") + ",\"type\":{\"node\":\"Number\",\"whole\":false,\"decimals\":0,\"sign\":"
                + "\"unspecified\"},\"source\":{\"snippet\":\"zijn saldo gedeeld door zijn noemer\"}}";
        Path ir = document(List.of(computingRule("zwaarte", "BEREKENING", "Jutter", "zwaarte", division, compound("all", 0,
                List.of(comparison("greater", zwaarFold(), number("0")), check("actief", false))))), List.of());
        String v6 = vondst("V6", "60", "2");
        played(ir, TmsDocuments.caseDocument(List.of(instanceWithRoles("J5", "Jutter", Map.of("saldo", "6"), List.of(),
                Map.of("vondsten", List.of("V6"))), v6), List.of(value("J5", "zwaarte", null), consistent())));
        played(ir, TmsDocuments.caseDocument(List.of(jutter("J6", true, "6", "2", List.of("V6")), v6),
                List.of(value("J6", "zwaarte", "3"), consistent())));
        played(ir, TmsDocuments.caseDocument(List.of(instanceWithRoles("J7", "Jutter", Map.of("saldo", "6"), List.of("actief"),
                Map.of("vondsten", List.of("V6"))), v6), List.of(value("J7", "zwaarte", null),
                TmsDocuments.inconsistent("zwaarte", "J7"))));
    }

    /** "Een winterschuit van een Haven is een ligger van de Haven indien hij aan alle volgende voorwaarden voldoet:" with
     *  the members "het aantal liggers van de Haven die aan de volgende voorwaarde voldoen: zijn lengte gedeeld door zijn
     *  breedte is groter dan 2 is groter dan 0" and "de Haven veilig is", in both orders, read back through "het
     *  wintertal van een Haven ... het aantal winterschuiten van de Haven": the condition of a fact creation's anchor.
     *  H1 is not veilig and its S2 divides 9 by 0: with the fold first the composite deposits (a fault until the card
     *  zero-division-channel), after "de Haven veilig is" the fold is not read; H1 gets no winterschuit either way. H2 is veilig and its S3 (10 by 2) is its winterschuit. */
    @Test void a_fact_creation_reads_the_members_of_its_condition_in_order() throws Exception {
        String model = TmsDocuments.model(List.of(
                TmsDocuments.objectType("Haven", List.of(attribute("wintertal", COUNTING)), List.of("veilig")),
                TmsDocuments.objectType("Schuit", List.of(attribute("lengte", WHOLE), attribute("breedte", WHOLE)),
                        List.of())),
                List.of(factTypeOneToMany("thuisligging", "thuishaven", "Haven", "ligger", "Schuit", "liggers"),
                        factTypeOneToMany("winterberging", "winterhaven", "Haven", "winterschuit", "Schuit",
                                "winterschuiten")));
        String wintertal = computingRule("wintertal", "BEREKENING", "Haven", "wintertal", aggregation("count",
                "{\"node\":\"AttributeReference\",\"attribute\":\"winterschuiten\",\"subjectType\":\"Haven\",\"source\":"
                        + "{\"snippet\":\"winterschuiten van de Haven\"}}", "Schuit", sourceRole("winterberging", "winterschuit"),
                "null", "null", COUNTING, "het aantal winterschuiten van de Haven"), "null");
        String fold = comparison("greater", aggregation("count", "{\"node\":\"AttributeReference\",\"attribute\":"
                + "\"liggers\",\"subjectType\":\"Haven\",\"source\":{\"snippet\":\"liggers van de Haven\"}}", "Schuit",
                sourceRole("thuisligging", "ligger"), comparison("greater", TmsFilterFaultWitnessTest.quotient(
                        ownRead("lengte", "Schuit"), ownRead("breedte", "Schuit")), number("2")), "null", COUNTING,
                "het aantal liggers van de Haven die aan de volgende voorwaarde voldoen"), number("0"));
        String safe = check("veilig", false);
        String h1 = instanceWithRoles("H1", "Haven", Map.of(), List.of(), Map.of("liggers", List.of("S1", "S2")));
        String h2 = instanceWithRoles("H2", "Haven", Map.of(), List.of("veilig"), Map.of("liggers", List.of("S3")));
        List<String> schuiten1 = List.of(instanceWithRoles("S1", "Schuit", Map.of("lengte", "10", "breedte", "2"),
                List.of(), Map.of()), instanceWithRoles("S2", "Schuit", Map.of("lengte", "9", "breedte", "0"), List.of(),
                Map.of()));
        String s3 = instanceWithRoles("S3", "Schuit", Map.of("lengte", "10", "breedte", "2"), List.of(), Map.of());
        for (List<String> members : List.of(List.of(fold, safe), List.of(safe, fold))) {
            String factCreation = "{\"node\":\"FactCreation\",\"name\":\"winterberging door de haven\",\"versions\":[{"
                    + "\"factType\":\"winterberging\",\"anchorRole\":\"winterhaven\",\"targetRole\":\"winterschuit\","
                    + "\"anchor\":\"Haven\",\"path\":[" + roleBinding("thuisligging", "ligger") + "],\"condition\":"
                    + compound("all", 0, members) + ",\"validity\":{\"node\":\"Validity\"}}]}";
            Path ir = Files.writeString(Files.createTempFile("filterfout-getuige", ".json"), "{\"format\":\"uchar-ir-v1\","
                    + "\"model\":" + model + ",\"rules\":{\"rules\":[" + wintertal + "],\"dayKindDefinitions\":[],"
                    + "\"objectCreations\":[],\"allocations\":[],\"consistencies\":[],\"uniquenesses\":[],\"factCreations\":["
                    + factCreation + "],\"startPointDeterminations\":[],\"ruleGroups\":[],\"prioritizedDefaults\":[]},"
                    + "\"diagnostics\":[]}");
            ir.toFile().deleteOnExit();
            List<String> h1Case = new ArrayList<>(List.of(h1));
            h1Case.addAll(schuiten1);
            if (members.get(0).equals(fold)) deposits(ir, h1Case, List.of(value("H1", "wintertal", "0")),
                    "winterberging door de haven", "H1");
            else played(ir, TmsDocuments.caseDocument(h1Case, List.of(value("H1", "wintertal", "0"), consistent())));
            played(ir, TmsDocuments.caseDocument(List.of(h2, s3), List.of(value("H2", "wintertal", "1"), consistent())));
        }
    }

    // ---------------- a defaults group reads its defaults by priority ----------------

    /** A defaults group on the zwaarte, the higher default "1 indien hij actief is" and below it {@code lower}. */
    static Path defaultsGroup(String lower) throws Exception {
        return TmsDocuments.document(MODEL, List.of(), List.of(), List.of(TmsDocuments.attributeGroup("zwaarte basis",
                "Jutter", "zwaarte", List.of(TmsDocuments.defaultEntry(check("actief", false), number("1")), lower))));
    }

    /** The lower default "2 indien hij aan alle volgende voorwaarden voldoet: - {@link #fold()} is groter dan 0", and
     *  the lower default "{@link #fold()} indien hij druk is": the reference interpreter reads a lower default only where
     *  no higher one decides (RUNTIME-SEMANTICS ch. 7.1), the exec-model branch computes a default as soon as what
     *  it reads stands, one a higher default displaces among them. Where the fold's subselection divides, the branch
     *  refuses the group by name: a division by zero there is a fault on an intermediate state, and whether it counts
     *  waits on the norm decision of card {@code 20261008-fout-op-tussenstand} (until card
     *  drl-teksttak-op-truth-maintenance the DRL-text branch refused every defaults group). */
    @Test void a_defaults_group_with_a_fold_that_can_fail_is_refused() throws Exception {
        JsonNode caseDocument = TmsDocuments.caseDocument(j1(true), List.of(value("J1", "zwaarte", "1"), consistent()));
        for (String lower : List.of(
                TmsDocuments.defaultEntry(compound("all", 0, List.of(comparison("greater", fold(), number("0")))),
                        number("2")),
                TmsDocuments.defaultEntry(check("druk", false), fold()))) {
            String tms = TmsFaultWitnessTest.play(defaultsGroup(lower), caseDocument, EXEC);
            assertTrue(tms.startsWith("Excluded: ") && tms.contains("defaults: defaults A:Jutter.zwaarte — een deling in"
                    + " een default ('zijn gewicht gedeeld door zijn deelnemers'): "), tms);
        }
    }

    /** The control: the lower default "2 indien … het aantal vondsten van de Jutter die aan de volgende voorwaarde
     *  voldoen: zijn gewicht is groter dan 5 is groter dan 0", whose subselection cannot fault, is lowered as before. J1
     *  is actief: 1. J2 is not, and its V3 weighs 10: 2. J3 is not, and its V5 weighs 4: the group decides nothing. */
    @Test void a_defaults_group_with_a_fold_that_can_not_fail_plays() throws Exception {
        String five = aggregation("count", "{\"node\":\"AttributeReference\",\"attribute\":\"vondsten\",\"subjectType\":"
                + "\"Jutter\",\"source\":{\"snippet\":\"vondsten van de Jutter\"}}", "Vondst", sourceRole("aanmelding", "vondst"),
                comparison("greater", ownRead("gewicht", "Vondst"), number("5")), "null", COUNTING,
                "het aantal vondsten van de Jutter die aan de volgende voorwaarde voldoen");
        Path ir = defaultsGroup(TmsDocuments.defaultEntry(compound("all", 0, List.of(comparison("greater", five,
                number("0")))), number("2")));
        assertEquals("gespeeld", TmsFaultWitnessTest.play(ir, TmsDocuments.caseDocument(j1(true),
                List.of(value("J1", "zwaarte", "1"), consistent())), EXEC));
        assertEquals("gespeeld", TmsFaultWitnessTest.play(ir, TmsDocuments.caseDocument(List.of(jutter("J2", false,
                List.of("V3")), vondst("V3", "10", "2")), List.of(value("J2", "zwaarte", "2"), consistent())), EXEC));
        assertEquals("gespeeld", TmsFaultWitnessTest.play(ir, TmsDocuments.caseDocument(List.of(jutter("J3", false,
                List.of("V5")), vondst("V5", "4", "4")), List.of(value("J3", "zwaarte", null), consistent())), EXEC));
    }

    // ---------------- a subselection that faults otherwise than by a division ----------------

    /** A whole number in the unit {@code unit}. */
    private static String inUnit(String unit) {
        return "{\"node\":\"Number\",\"whole\":true,\"decimals\":0,\"sign\":\"unspecified\",\"unit\":{\"node\":\"Unit\","
                + "\"abbreviation\":\"" + unit + "\",\"canonical\":\"" + unit + "\",\"factorToCanonical\":{\"num\":1,"
                + "\"den\":1}}}";
    }

    /** A Jutter with a vondsttal; a Vondst with a gewicht, deelnemers, a vinddatum, a wachttijd in nooitdagen, a termijn
     *  in jaren, a jaar and a maand; and the day kind nooitdag, which no day is of. */
    static final String MODEL_FALLIBLE = TmsDocuments.model(List.of(
            TmsDocuments.objectType("Jutter", List.of(attribute("vondsttal", WHOLE)), List.of("actief")),
            TmsDocuments.objectType("Vondst", List.of(attribute("gewicht", WHOLE), attribute("deelnemers", WHOLE),
                    attribute("vinddatum", DATE), attribute("wachttijd", inUnit("nooitdagen")),
                    attribute("termijn", inUnit("jr")), attribute("jaar", WHOLE), attribute("maand", WHOLE)),
                    List.of())),
            List.of(factTypeOneToMany("aanmelding", "aanmelder", "Jutter", "vondst", "Vondst", "vondsten")))
            .replace("\"dayKinds\":[]", "\"dayKinds\":[{\"node\":\"DayKind\",\"name\":\"nooitdag\",\"plural\":"
                    + "\"nooitdagen\"}]");

    /** "Een dag is een nooitdag indien de dag uit de dag gelijk is aan 32". */
    static final String NOOITDAG = "{\"node\":\"DayKindDefinition\",\"dayKind\":\"nooitdag\",\"condition\":{\"node\":"
            + "\"Comparison\",\"op\":\"equal\",\"left\":{\"node\":\"BuiltinCall\",\"kind\":\"dayOf\",\"arguments\":[{\"node\":"
            + "\"BoundDay\",\"type\":" + DATE + ",\"source\":{\"snippet\":\"de dag\"}}],\"type\":" + COUNTING + ",\"source\":"
            + "{\"snippet\":\"de dag uit de dag\"}},\"right\":" + number("32") + ",\"source\":{\"snippet\":\"de dag uit de dag"
            + " gelijk is aan 32\"}},\"validity\":{\"node\":\"Validity\"}}";

    /** "Het vondsttal van een Jutter moet berekend worden als het aantal vondsten van de Jutter die aan de volgende
     *  voorwaarde voldoen: {@code filter}, indien hij actief is", in a document of {@link #MODEL_FALLIBLE}. */
    static Path vondsttal(String filter) throws Exception {
        String regel = computingRule("vondsttal", "BEREKENING", "Jutter", "vondsttal", aggregation("count",
                "{\"node\":\"AttributeReference\",\"attribute\":\"vondsten\",\"subjectType\":\"Jutter\",\"source\":"
                        + "{\"snippet\":\"vondsten van de Jutter\"}}", "Vondst", sourceRole("aanmelding", "vondst"), filter,
                "null", COUNTING, "het aantal vondsten van de Jutter die aan de volgende voorwaarde voldoen"),
                check("actief", false));
        String ir = "{\"format\":\"uchar-ir-v1\",\"model\":" + MODEL_FALLIBLE + ",\"rules\":{\"rules\":[" + regel
                + "],\"dayKindDefinitions\":[" + NOOITDAG + "],\"objectCreations\":[],\"allocations\":[],"
                + "\"consistencies\":[],\"uniquenesses\":[],\"factCreations\":[],\"startPointDeterminations\":[],"
                + "\"ruleGroups\":[],\"prioritizedDefaults\":[]},\"diagnostics\":[]}";
        Path tmp = Files.createTempFile("filterfout-getuige", ".json");
        tmp.toFile().deleteOnExit();
        return Files.writeString(tmp, ir);
    }

    /** A node of the wire with its source text. */
    private static String node(String fields, String source) {
        return "{" + fields + ",\"source\":{\"snippet\":\"" + source + "\"}}";
    }

    /** The messages of {@code t} and its causes, joined. */
    private static String causes(Throwable t) {
        StringBuilder b = new StringBuilder();
        for (Throwable o = t; o != null; o = o.getCause() == o ? null : o.getCause()) b.append(o.getMessage()).append(" <- ");
        return b.toString();
    }

    /** Each operation that can fault other than the division (the closed list the destructive DRL-text lowering kept),
     *  in the subselection of a fold under "indien hij actief is", on a Jutter that is not actief, whose one vondst makes
     *  the operation fault: a step of one nooitdag, which no day is of; a step of 2 000 000 000 jaren; zijn gewicht 0 to
     *  the power -1; the date of maand 13; the Rekenjaar of a case without an evaluation date; the eerste paasdag of the
     *  year 2 000 000 000. The reference interpreter does not evaluate the fold and answers (measured 2026-10-09); the
     *  destructive lowering tested the subselection on the element's pattern and faulted for all six. The analysis
     *  answers as the reference interpreter for five, and refuses the power by name: an exponent that is no literal
     *  whole number of at least 0 is a real power, which is OPEN (RUNTIME-SEMANTICS ch. 2.1). Since card
     *  drl-teksttak-op-truth-maintenance the DRL-text branch renders that analysis, so both branches answer the five and
     *  refuse the power in one sentence.
     *  Where J1 is actief, the reference interpreter faults on the day-kind walk and on the Rekenjaar too, and so do
     *  both branches, with the runtime's sentence in the cause chain; for the other four it answers or crashes where
     *  this backend faults (reported with the fix round, not pinned here). */
    @Test void a_filter_that_fails_otherwise_than_by_a_division() throws Exception {
        String vinddatum = ownRead("vinddatum", "Vondst");
        Map<String, String> filters = new java.util.LinkedHashMap<>();
        filters.put("dagsoortstap", comparison("later", node("\"node\":\"Binary\",\"op\":\"plus\",\"left\":" + vinddatum
                + ",\"right\":" + ownRead("wachttijd", "Vondst") + ",\"type\":" + DATE, "zijn vinddatum plus zijn wachttijd"),
                vinddatum));
        filters.put("datumbereik", comparison("later", node("\"node\":\"Binary\",\"op\":\"plus\",\"left\":" + vinddatum
                + ",\"right\":" + ownRead("termijn", "Vondst") + ",\"type\":" + DATE, "zijn vinddatum plus zijn termijn"),
                vinddatum));
        filters.put("macht", comparison("greater", node("\"node\":\"Rounding\",\"base\":" + node("\"node\":\"Binary\","
                + "\"op\":\"power\",\"left\":" + ownRead("gewicht", "Vondst") + ",\"right\":" + ownRead("deelnemers", "Vondst")
                + ",\"type\":{\"node\":\"Number\",\"whole\":false,\"decimals\":0,\"sign\":\"unspecified\"}",
                "zijn gewicht tot de macht zijn deelnemers") + ",\"mode\":\"floor\",\"decimals\":0,\"type\":" + WHOLE,
                "zijn gewicht tot de macht zijn deelnemers naar beneden afgerond op 0 decimalen"), number("2")));
        filters.put("datum met", comparison("later", node("\"node\":\"BuiltinCall\",\"kind\":\"dateWith\","
                + "\"arguments\":[" + ownRead("jaar", "Vondst") + "," + ownRead("maand", "Vondst") + "," + number("1") + "],"
                + "\"type\":" + DATE, "de datum met jaar: zijn jaar, maand: zijn maand en dag: 1"), vinddatum));
        filters.put("rekenjaar", comparison("greater", node("\"node\":\"ContextValue\",\"name\":\"evaluationYear\","
                + "\"type\":" + WHOLE, "het Rekenjaar"), ownRead("gewicht", "Vondst")));
        filters.put("paasdag", comparison("later", node("\"node\":\"BuiltinCall\",\"kind\":\"easterSunday\","
                + "\"arguments\":[" + ownRead("jaar", "Vondst") + "],\"type\":" + DATE, "de eerste paasdag van zijn jaar"),
                vinddatum));
        Map<String, Map<String, String>> numbers = Map.of(
                "dagsoortstap", Map.of("wachttijd", "1"), "datumbereik", Map.of("termijn", "2000000000"),
                "macht", Map.of("gewicht", "0", "deelnemers", "-1"), "datum met", Map.of("jaar", "2026", "maand", "13"),
                "rekenjaar", Map.of("gewicht", "10"), "paasdag", Map.of("jaar", "2000000000"));
        java.util.Set<String> refused = java.util.Set.of("macht");
        Map<String, String> fault = Map.of("dagsoortstap", "day-kind walk: no day of the kind within 40000 calendar days",
                "rekenjaar", "the evaluationYear context value needs an evaluation date");
        List<String> deviating = new ArrayList<>();
        for (Map.Entry<String, String> f : filters.entrySet()) {
            Path ir = vondsttal(f.getValue());
            String v1 = withDates(instanceWithRoles("V1", "Vondst", numbers.get(f.getKey()), List.of(), Map.of()),
                    Map.of("vinddatum", "2026-01-05"));
            if (refused.contains(f.getKey())) {
                // the analysis refuses the form by name, on both branches in one sentence
                JsonNode caseDocument = TmsDocuments.caseDocument(List.of(instanceWithRoles("J1", "Jutter", Map.of(), List.of(),
                        Map.of("vondsten", List.of("V1"))), v1), List.of(value("J1", "vondsttal", null), consistent()));
                String exec = TmsFaultWitnessTest.play(ir, caseDocument, EXEC), drl = TmsFaultWitnessTest.play(ir, caseDocument, DRL);
                if (!exec.startsWith("Excluded: ") || !exec.equals(drl))
                    deviating.add(f.getKey() + ": " + EXEC + " " + exec + ", " + DRL + " " + drl);
                continue;
            }
            for (DroolsBackend.Branch branch : List.of(EXEC, DRL)) {
                String notActive = TmsFaultWitnessTest.play(ir, TmsDocuments.caseDocument(List.of(instanceWithRoles("J1", "Jutter",
                        Map.of(), List.of(), Map.of("vondsten", List.of("V1"))), v1),
                        List.of(value("J1", "vondsttal", null), consistent())), branch);
                if (!notActive.equals("gespeeld")) deviating.add(f.getKey() + " " + branch + " niet actief: " + notActive);
                if (!fault.containsKey(f.getKey())) continue;
                JsonNode actief = TmsDocuments.caseDocument(List.of(instanceWithRoles("J1", "Jutter", Map.of(),
                        List.of("actief"), Map.of("vondsten", List.of("V1"))), v1), List.of(consistent()));
                try {
                    String v = CaseRunner.playCase(ir, actief, branch);
                    deviating.add(f.getKey() + " " + branch + " actief: geen fout maar " + (v == null ? "gespeeld" : v));
                } catch (RuntimeException e) {
                    if (!causes(e).contains(fault.get(f.getKey())))
                        deviating.add(f.getKey() + " " + branch + " actief: " + causes(e));
                }
            }
        }
        String power = TmsFaultWitnessTest.play(vondsttal(filters.get("macht")), TmsDocuments.caseDocument(List.of(),
                List.of(consistent())), EXEC);
        if (!(power.startsWith("Excluded: ") && power.endsWith("regel: vondsttal — een macht met een exponent die geen"
                + " geheel getal van ten minste 0 als literal is ('zijn gewicht tot de macht zijn deelnemers'): de reële"
                + " macht is OPEN (RUNTIME-SEMANTICS ch. 2.1), waar de referentie-interpreter een exacte wortel geeft of"
                + " faalt")))
            deviating.add("macht " + EXEC + ": " + power);
        assertEquals(List.of(), deviating);
    }
}
