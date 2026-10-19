package dev.uchar.regelspraak.cases;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * IR-SPEC ch. 4.10 / RUNTIME-SEMANTICS ch. 10.6 — the §11 <b>variabelendeel</b> that the result part reads,
 * held to ONE answer in BOTH transpiler branches. {@link LetClusterWitnessTest} pins the cluster's semantics
 * through the exec-model branch alone; this one runs every case through both, because until 2026-09-18 the
 * DRL-text branch did not lower the construct at all (a {@code TODO expressie} marker on the first result
 * binding) and a branch that markers agrees with everything.
 *
 * <p><b>The arbiter is {@code ref/}, not this backend.</b> Every value below was produced on this exact model
 * and case by the verifier + {@code ref} + the clingo backend first (measured 2026-09-18: verifier PASS, ref
 * PASS, clingo PASS on all five cases), and only then pinned here. No corpus file is read, written or moved:
 * the model and its cases are authored in this file.
 *
 * <p>What each test isolates, and what a wrong lowering it would let through:
 * <ul>
 *   <li>a variable read in the VOORWAARDE <i>and</i> in the result — the frontend substitutes the first and
 *       binds the second (ch. 10.6, tracked delta 1), so both halves must arrive in one rule;</li>
 *   <li>a LATER binder reading an EARLIER one — the scope that makes a chain a chain; without it the second
 *       definition lowers to an unbound-read marker and the whole golden stops playing;</li>
 *   <li>the ZERO guard: the rule's voorwaarde keeps the route unentered, so the division inside the variable
 *       definition is never evaluated. Sharp rather than cosmetic — {@code rsGedeeld} by a numeric zero
 *       THROWS, and a throw inside a consequence aborts the whole fire, so an implementation that evaluated
 *       the need anyway reports a fault instead of these values;</li>
 *   <li>the same guard one level in: an OUTER need that is leeg must keep an INNER need (a division by zero)
 *       from being forced — the ch. 10.6 clause-2 barrier standing in front of a clause-1 forcing;</li>
 *   <li>leeg BESIDE gevuld, so that an always-writing and a never-writing implementation each go red;</li>
 *   <li>one binder read twice, name-sameness that shadows without capturing, and a sibling binding that keeps
 *       its own answer when its neighbour empties — the three shapes a consequence-wide null guard, an
 *       in-place binder map or a value-text substitution each get wrong;</li>
 *   <li>a VOORWAARDE-ONLY variable, which the frontend already brought to both branches WITHOUT a Let: the
 *       non-regression flank of this whole card;</li>
 *   <li><b>ch. 10.2</b>: each arm chosen in turn; a branch node INSIDE a Let chain whose guard reads an earlier
 *       binder and whose arm reads a later one (the tariefstelling shape); a need only the UNENTERED arm reads,
 *       which may be neither forced nor allowed to raise its barrier; and the §6.5 site in each arm — reported
 *       from the one that runs, silent from the one that does not.</li>
 * </ul>
 *
 * <p><b>What the pins actually catch, measured by mutation on 2026-09-18</b> — a suite that only agrees with
 * the implementation it was written beside proves nothing, so every way to get these two constructs wrong was
 * built and run (all counts over the 28 tests here):
 * <ul>
 *   <li>the ch. 10.6 barrier dropped (an ALWAYS-WRITING Let) → 6 red, among them
 *       {@code een_lege_behoefte_maakt_de_hele_let_leeg…} and {@code het_lege_doel_is_observeerbaar_afwezig…};</li>
 *   <li>the Let always leeg (a NEVER-WRITING one) → 22 red, every filled pin among them;</li>
 *   <li>the prelude's own guard dropped → 2 ERRORS, {@code een_lege_buitenbehoefte…} among them, with
 *       "Breuk deling door 0" — the fault ch. 10.6 says must not happen;</li>
 *   <li>the §6.5 companion walk left at the Let (no descent) → 2 red, among them
 *       {@code een_deling_door_leeg_binnen_een_behoefte…}: "verwacht inconsistent …, werkelijk consistent",
 *       the silent direction;</li>
 *   <li>the companion emitted WITHOUT its needs as a gate →
 *       {@code een_deling_achter_een_lege_behoefte…} goes red: "verwacht consistent, werkelijk inconsistent",
 *       a fault invented on a path the barrier cut off;</li>
 *   <li><b>ch. 10.2, the arms SWAPPED</b> → 9 red, both branch pins among them;</li>
 *   <li><b>the companion emitted without its BRANCH guard</b> → 2 red, among them
 *       {@code the_same_division_in_the_not_taken_arm_does_not_report_itself}, the arm-level twin of the barrier
 *       case above;</li>
 *   <li><b>a conditionally-read need forced unconditionally</b> (its branch guard dropped from the prelude and
 *       the barrier) → 2 red, among them
 *       {@code a_need_that_only_reads_the_not_taken_arm_is_not_forced} — the flank
 *       {@code synth3-takbarriere} pins in the corpus;</li>
 *   <li><b>the ARM's guard not joining the need gate</b> (the lens's first finding) →
 *       {@code a_let_in_the_not_taken_arm_is_not_forced} ERRORS with "Breuk deling door 0";</li>
 *   <li><b>the value of a conditionally-read need walked under the bare gate</b> (the lens's second) →
 *       {@code a_division_in_an_unread_need_does_not_report_itself} goes red with a fabricated
 *       Inconsistency.</li>
 * </ul>
 */
class LetResultArmWitnessTest {

    // ---------------- 1. the variable that feeds the voorwaarde AND the result ----------------

    @Test void a_variable_in_condition_and_result_comes_to_both_branches() throws Exception {
        // "de dubbel is het inkomen maal 2" is read by the voorwaarde (dubbel > 10,00 — SUBSTITUTED there,
        // since no Let can span the wire's separate condition and value fields) AND by the result
        // (dubbel plus dubbel — a Let with two BinderRefs). ref: aandeel = 160.00 at inkomen 40,00.
        playBoth(jutter(GEVULD, valueJson("aandeel", "160.00")));
    }

    @Test void the_condition_that_does_not_hold_lets_the_target_empty() throws Exception {
        // the other side of the same rule: inkomen 2,00 makes dubbel 4,00, which is not > 10,00, so the rule
        // does not fire and `aandeel` stays leeg — the substituted condition half really decides. ref: leeg.
        playBoth(jutter("\"inkomen\": { \"number\": \"2.00\" }, " + INPUT_REST, empty("aandeel")));
    }

    // ---------------- 2. a later binder reads an earlier one ----------------

    @Test void a_later_binder_reads_the_earlier_one() throws Exception {
        // gestaffeld := Let basis = inkomen in Let opslag = basis maal 2 in basis plus opslag.
        // `opslag`'s DEFINITION reads `basis`, which is in scope because the chain nests in declaration order
        // (§11.1 rule 1, outermost first). A branch that bound only the innermost binder lowers that read to an
        // unbound-read marker and refuses the whole rule. ref: 120.00 at inkomen 40,00 (40,00 + 80,00).
        playBoth(jutter(GEVULD, valueJson("gestaffeld", "120.00")));
    }

    // ---------------- 3. the zero guard: an unentered route evaluates nothing ----------------

    @Test void the_zero_guarding_keeps_the_division_out_of_the_not_taken_route() throws Exception {
        // quotient := Let q = teller gedeeld door deler in q plus 1,00, under the voorwaarde `deler ongelijk 0`.
        // With deler 0 the rule does not fire, so the need is never forced and the division never runs: the
        // target stays leeg and the run is CONSISTENT. Evaluating the need regardless is not a silent wrong
        // here — rsGedeeld throws "Breuk deling door 0" and the fire aborts — which is exactly what makes this
        // case a witness rather than a formality. ref: quotient leeg, consistent.
        playBoth(jutter(ZERO_DIVISOR, empty("quotient") + ", " + valueJson("aandeel", "160.00")));
    }

    @Test void the_filled_division_route_yields_the_original_outcome() throws Exception {
        // the route ENTERED, so the same need is forced and the division really runs. ref: 11.00 (40,00 / 4,
        // plus 1,00) — the absolute value, not merely "something non-empty".
        playBoth(jutter(GEVULD, valueJson("quotient", "11.00")));
    }

    @Test void an_empty_outside_need_keeps_the_innermost_division_unforced() throws Exception {
        // garde := Let veilig = inkomen in Let q = teller gedeeld door staffel in veilig plus q, with inkomen
        // ABSENT and staffel 0. ch. 10.6 reads `veilig` first, the barrier fires, and `q` is never forced — so
        // the division by zero never happens and the whole Let is leeg. This is the flank that makes the
        // prelude's own guard load-bearing: measured by mutation (guard dropped), this branch reports
        // "Breuk deling door 0" here instead of playing. ref: garde leeg, consistent.
        playBoth(jutter(BARRIER_BEFORE_THE_DIVISION, empty("garde") + ", " + empty("quotient")));
    }

    // ---------------- 3b. the §6.5 fout companion walks through the binding ----------------

    @Test void a_division_by_empty_within_a_need_still_reports_itself() throws Exception {
        // heffing := Let q = teller gedeeld door heffingsdeler in q plus 1,00, with the divisor ABSENT. §6.5
        // Tabel 12 makes that a Fout: the value degrades to leeg and the §9.5 channel carries the reason, so
        // ref answers INCONSISTENT. The DRL branch surfaces it through a SEPARATE companion rule built from a
        // walk over the rule value — a walk that stops at a Let sees no division at all and leaves the run
        // silently CONSISTENT, which is the direction that cannot be noticed from the outside.
        playBoth(jutterWith(EMPTY_HEFFINGS_DIVISOR, empty("heffing") + ", " + valueJson("bescherm", "50.00")
                + ", { \"kind\": \"inconsistent\", \"rule\": \"heffing van een jutter\", \"label\": \"J1\" }"));
    }

    @Test void a_division_behind_an_empty_need_precisely_does_not_report_itself() throws Exception {
        // the other direction, which a companion built WITHOUT the needs would get wrong: bescherm := Let
        // veilig = inkomen in Let r = teller gedeeld door beschermdeler in veilig plus r, with BOTH inkomen and
        // the divisor absent. The read of `veilig` empties the Let before `r` is ever forced, so no division
        // runs and there is nothing to report — ref: bescherm leeg, CONSISTENT. A companion that fired on the
        // bare "dividend vol, divisor leeg" pattern would invent an Inconsistency on a path that never ran.
        playBoth(jutter(EMPTY_DIVISOR_BEHIND_THE_BARRIER, valueJson("heffing", "11.00") + ", " + empty("bescherm")));
    }

    // ---------------- 4. leeg beside gevuld: both mutants go red ----------------

    @Test void an_empty_need_makes_the_whole_let_empty_while_the_same_times_without_let_computes_zero() throws Exception {
        // ch. 10.6's normative contrast in ONE case with `inkomen` ABSENT:
        //   metlet    := Let ink = inkomen in ink maal 2  -> the read forces the leeg need and raises the
        //                barrier, so the whole Let is leeg and nothing is written (ref: leeg);
        //   zonderlet := inkomen maal 2                   -> the bare maal COALESCES leeg to 0 (ref: 0.00).
        // An ALWAYS-WRITING lowering (the barrier dropped) writes 0.00 on both and fails here — measured.
        playBoth(jutter(LEEG, empty("metlet") + ", " + valueJson("zonderlet", "0.00")));
    }

    @Test void the_filled_input_indeed_writes_and_both_forms_come_out_at_the_same() throws Exception {
        // the other mutant's red: a NEVER-WRITING lowering (the Let always leeg) passes every emptiness pin
        // above and fails here — measured. ref: 80.00 for both forms at inkomen 40,00.
        playBoth(jutter(GEVULD, valueJson("metlet", "80.00") + ", " + valueJson("zonderlet", "80.00")));
    }

    @Test void the_empty_target_is_observable_and_absent_in_both_branches() throws Exception {
        // the comparator must SEE the absence rather than leave it unasserted: the same input with a VALUE
        // expectation on `metlet` must MISMATCH in both branches, and name the target.
        String caseJson = jutter(LEEG, valueJson("metlet", "0.00"));
        for (String branch : List.of("exec", "drl")) {
            CaseRunner.Result r = play(branch, caseJson);
            assertEquals("FAIL", r.status(), branch + ": de leeg-barriere moet observeerbaar zijn, niet slechts niet-geasserteerd");
            assertTrue(r.caseFailures().toString().contains("metlet"), branch + ": " + r.caseFailures());
        }
    }

    // ---------------- 5. one need per binder, shadowing, siblings ----------------

    @Test void the_same_binder_read_two_times_yields_the_same_need() throws Exception {
        // dubbelop := Let ink = inkomen in ink plus ink. A second read that answered leeg would coalesce to 0
        // inside plus and quietly halve the result. ref: 80.00 at inkomen 40,00.
        playBoth(jutter(GEVULD, valueJson("dubbelop", "80.00")));
    }

    @Test void name_equality_shadows_within_and_does_not_leak_outside() throws Exception {
        // schaduw := Let x = inkomen in ((Let x = 5,00 in x) plus x). WF-064: the INNER x wins inside its own
        // body, and the OUTER x is intact again outside it. Reading the outer binder inside gives 80.00;
        // binding the inner x IN PLACE of the outer (a mutated environment instead of a scoped one) leaves
        // x = 5,00 for the trailing read and gives 10.00. Two same-named binders also may not collapse onto
        // one emitted variable. ref: 45.00 (5,00 + 40,00).
        playBoth(jutter(GEVULD, valueJson("schaduw", "45.00")));
    }

    @Test void a_sister_binding_keeps_its_own_answer_if_the_neighbour_runs_empty() throws Exception {
        // zusters := (Let a = inkomen in a) plus (Let b = 5,00 in b), with inkomen ABSENT. The barrier belongs
        // to the Let that bound the binder that was read, so only the LEFT one empties; the §6.2 leegwaarde
        // table then coalesces it to 0 inside the plus and the right one still answers. A guard hung on the
        // whole consequence instead of on each Let node would empty the rule value and write leeg. ref: 5.00.
        playBoth(jutter(LEEG, valueJson("zusters", "5.00")));
    }

    @Test void the_sister_binding_simply_computes_along_once_filled() throws Exception {
        // the same pair with both sides gevuld, so the pin above is not passing on an arm that never adds.
        // ref: 45.00 (40,00 + 5,00).
        playBoth(jutter(GEVULD, valueJson("zusters", "45.00")));
    }

    // ---------------- 5b. ch. 10.2: the value-level conditional ----------------

    @Test void the_then_arm_is_taken_and_the_else_arm_is_not() throws Exception {
        // tak := Let k = inkomen in (als k groter dan 10,00 dan k maal 2 anders 2,00). ref: 80.00 at inkomen
        // 40,00 — the guard holds, so the DAN arm is the value and the anders arm is not evaluated at all.
        playBoth(jutter(GEVULD, valueJson("tak", "80.00")));
    }

    @Test void the_else_arm_is_taken_and_the_then_arm_is_not() throws Exception {
        // the same rule at inkomen 2,00: the guard fails, so the ANDERS arm decides. ref: 2.00. An
        // implementation with the arms swapped passes neither this test nor the one above.
        playBoth(jutter("\"inkomen\": { \"number\": \"2.00\" }, " + INPUT_REST, valueJson("tak", "2.00")));
    }

    @Test void an_empty_need_in_the_arm_guard_ends_the_let_before_the_choice() throws Exception {
        // the guard READS a binder (WF-077), so a leeg need ends the Let at that read (ch. 10.6 clause 2) and no
        // arm is chosen at all — this is WHY a ch. 10.2 guard never sees a leeg operand. Measured 2026-09-18 on
        // the only other route a WF-077-clean document has, an unsupplied parameter: ref FAULTS fail-fast
        // ("parameter … is unbound in this case", ch. 5.7), so it is not leeg either. The guard nevertheless goes
        // through the SHARED rsAggVergelijk, so a document that somehow presents a leeg operand gets ref's own
        // Tabel 33/37/44 rather than an mvel accident. ref here: tak leeg.
        playBoth(jutter(LEEG, empty("tak")));
    }

    @Test void an_ifthenelse_in_a_letbody_reads_an_earlier_need() throws Exception {
        // the jutterswet-tariefstelling shape, isolated: takketen := Let a = inkomen in Let b = a maal 2 in
        // (als a groter dan 10,00 dan b anders 0,00). The guard reads the EARLIER binder and the chosen arm reads
        // the LATER one — the chain and the branch in one value. ref: 80.00 at inkomen 40,00, 0.00 at 2,00.
        playBoth(jutter(GEVULD, valueJson("takketen", "80.00")));
        playBoth(jutter("\"inkomen\": { \"number\": \"2.00\" }, " + INPUT_REST, valueJson("takketen", "0.00")));
    }

    @Test void a_need_that_only_reads_the_not_taken_arm_is_not_forced() throws Exception {
        // takopslag := Let g = inkomen in Let o = takopslaggrond in (als g groter dan 10,00 dan 3,00 anders
        // 3,00 plus o), with the guard HOLDING and `takopslagGround` ABSENT. ch. 10.6 clause 3 over a ch. 10.2
        // arm: the dan arm never reads `o`, so its need is never forced and its barrier never fires — the target
        // is WRITTEN. ref: 3.00. An eager forcing (the need computed whatever the guard says) makes `o` leeg and
        // the barrier empties the whole Let: the corpus pins the same flank in synth3-takbarriere.
        playBoth(jutter(UNREAD_TAKOPSLAG, valueJson("takopslag", "3.00")));
    }

    @Test void the_same_need_indeed_counts_along_once_its_arm_is_taken() throws Exception {
        // the other side, so the pin above is not passing on an arm that never adds: at inkomen 2,00 the ANDERS
        // arm runs, `o` IS read and its need is forced. ref: 9.00 (3,00 + 6,00).
        playBoth(jutter("\"inkomen\": { \"number\": \"2.00\" }, " + INPUT_REST, valueJson("takopslag", "9.00")));
    }

    @Test void a_division_in_the_taken_arm_reports_itself() throws Exception {
        // takdeling := Let k = inkomen in (als k groter dan 10,00 dan teller gedeeld door takdeler anders 7,00),
        // guard HOLDING and the divisor ABSENT: the §6.5 site really runs, so ref answers INCONSISTENT and the
        // target stays leeg. The DRL companion carries the need gate AND the branch guard, and must still fire.
        playBoth(jutterWith(EMPTY_THEN_ARM_DIVISOR, empty("takdeling") + ", " + valueJson("tak", "80.00")
                + ", { \"kind\": \"inconsistent\", \"rule\": \"takdeling van een jutter\", \"label\": \"J1\" }"));
    }

    @Test void the_same_division_in_the_not_taken_arm_does_not_report_itself() throws Exception {
        // the same rule and the same absent divisor at inkomen 2,00: the ANDERS arm runs, the division is never
        // evaluated, and there is nothing to report — ref: takdeling 7.00, CONSISTENT. A companion without the
        // branch guard invents an Inconsistency here.
        //
        // NOTE, measured 2026-09-18 and reported rather than pinned away: on THIS shape the clingo backend
        // answers inconsistent where ref answers consistent ("expected consistent, but the model carries 1
        // inconsistent fact(s)") — its §6.5 fence is not path-following through a ch. 10.2 arm. That is a
        // ref-vs-clingo question in a backend this card may not touch, so the shape is witnessed here (where ref
        // is the arbiter and BOTH Drools branches agree with it) and never proposed as a corpus golden, which
        // gate 6 would rightly reject.
        playBoth(jutter(EMPTY_ARM_DIVISOR_ELSE, valueJson("takdeling", "7.00") + ", " + valueJson("tak", "2.00")));
    }

    // ---------------- 5c. the two shapes the adversarial lens found (2026-09-18) ----------------

    @Test void a_let_in_the_not_taken_arm_is_not_forced() throws Exception {
        // sprong := Let k = inkomen in (als k groter dan 10,00 dan 5,00 anders Let z = teller gedeeld door
        // nuldeler in z plus 1,00), guard HOLDING and `zeroDivisor` 0. The dan arm is the value, so the Let in the
        // ANDERS arm is never entered and its need never forced — ref: 5.00.
        //
        // As landed on 2026-09-18 this branch answered with an aborted fire ("Breuk deling door 0"): the arm's
        // own guard did not join the need-forcing gate, so a Let nested in an arm hoisted its prelude line under
        // the ENCLOSING needs alone and the unchosen arm's division really ran. The ternary was right; the
        // prelude around it was not. Measured then: verifier PASS, ref PASS, exec PLAYED, --drl ERROR.
        //
        // clingo on this shape: UNSATISFIABLE — a fail-fast guard fired: viol("division by numeric zero","J1"),
        // where ref reaches a model. A second divergence in the same family as the one below, in a backend this
        // card may not touch; recorded, not pinned away.
        playBoth(jutter(ZERO_IN_THE_NOT_TAKEN_ARM, valueJson("sprong", "5.00") + ", " + valueJson("takgat", "4.00")));
    }

    @Test void a_division_in_an_unread_need_does_not_report_itself() throws Exception {
        // takgat := Let k = inkomen in Let z = teller gedeeld door gatdeler in (als k groter dan 10,00 dan 4,00
        // anders z), guard HOLDING and the divisor ABSENT. `z` is read only by the ANDERS arm, so under ch. 10.6
        // clause 3 its need is never forced and its §6.5 site never runs — ref: takgat 4.00, CONSISTENT.
        //
        // As landed on 2026-09-18 the companion walk entered that need's value under the BARE gate, ignoring
        // that the read was conditional, and this branch inserted an Inconsistency for a division that never
        // executed. synth3-takbarriere has exactly this shape: one division in its opslag definition and the
        // corpus golden would have gone red on a fabricated fault. Measured then: ref PASS consistent, exec
        // PLAYED, --drl FAIL "verwacht consistent, werkelijk inconsistent".
        //
        // clingo here: MISMATCH, expected consistent — the same non-path-following §6.5 fence recorded above.
        playBoth(jutter(GAP_IN_A_UNREAD_NEED, valueJson("takgat", "4.00") + ", " + valueJson("sprong", "5.00")));
    }

    @Test void both_forms_simply_compute_once_their_arm_is_taken() throws Exception {
        // the other side of both, so neither pin passes on an arm that never computes: at inkomen 2,00 the
        // ANDERS arm runs in both rules, so both needs ARE forced. ref: sprong 11.00 (40/4 + 1), takgat 10.00.
        playBoth(jutter("\"inkomen\": { \"number\": \"2.00\" }, " + INPUT_REST,
                valueJson("sprong", "11.00") + ", " + valueJson("takgat", "10.00")));
    }

    @Test void two_different_arm_guards_play_on_a_read_in_both_branches() throws Exception {
        // dubbelgarde := Let k = inkomen in Let z = k maal 2 in ((als k groter dan 10,00 dan z anders 0,00) plus
        // (als k kleiner dan 99,00 dan z anders 0,00)). `z` is read under TWO different branch guards, so the
        // conjunction a prelude of the destructive DRL-text lowering would need had no guarantee that its operands
        // were assigned by the time the line ran, and that lowering refused the shape BY NAME ("behoefte gelezen onder
        // twee verschillende takgardes") until card drl-teksttak-op-truth-maintenance. The analysis, whose needs are
        // genuine thunks, plays it (ref: 160.00), on both branches since that card. clingo refuses the document
        // outright ("TRANSLATION FAULT: expression kind not translated").
        java.nio.file.Path ir = temporary("let-dubbelgarde-ir", DOUBLE_GUARD_MODEL);
        java.nio.file.Path c = temporary("let-dubbelgarde-case", DOUBLE_GUARD_CASE);
        try {
            assertEquals("PLAYED", CaseRunner.viaExecModel(ir, List.of(c)).status(), "exec speelt de vorm");
            CaseRunner.Result drl = CaseRunner.viaDrlText(ir, List.of(c));
            assertEquals("PLAYED", drl.status(), "de DRL-tak speelt de vorm: " + drl);
        } finally {
            Files.deleteIfExists(ir);
            Files.deleteIfExists(c);
        }
    }

    // ---------------- 6. the non-regression flank: a voorwaarde-only variable ----------------

    @Test void a_condition_only_variable_stays_working_without_let() throws Exception {
        // "de dubbel is het inkomen maal 2" read ONLY in the voorwaarde gets no Let at all (ch. 10.6: an
        // unread binder would be a guaranteed no-op) — the frontend substitutes it, and both branches have
        // carried that shape all along. ref: premie 7.00 at inkomen 40,00 (80 > 50), leeg at inkomen 2,00.
        playBoth(jutter(GEVULD, valueJson("premie", "7.00")));
        playBoth(jutter("\"inkomen\": { \"number\": \"2.00\" }, " + INPUT_REST, empty("premie")));
    }

    // ---------------- replay: one document, one case, BOTH branches ----------------

    /** Both transpiler branches on the authored model and one case; each must PLAY it. */
    private static void playBoth(String caseJson) throws Exception {
        for (String branch : List.of("exec", "drl")) {
            CaseRunner.Result r = play(branch, caseJson);
            assertEquals("PLAYED", r.status(), branch + ": " + r.reason() + " " + r.caseFailures());
        }
    }

    private static CaseRunner.Result play(String branch, String caseJson) throws Exception {
        Path ir = temporary("let-resultaat-ir", MODEL), c = temporary("let-resultaat-case", caseJson);
        try {
            return branch.equals("drl") ? CaseRunner.viaDrlText(ir, List.of(c)) : CaseRunner.viaExecModel(ir, List.of(c));
        } finally {
            Files.deleteIfExists(ir);
            Files.deleteIfExists(c);
        }
    }

    private static Path temporary(String name, String inhoud) throws Exception {
        Path p = Files.createTempFile(name, ".json");
        p.toFile().deleteOnExit();
        Files.writeString(p, inhoud);
        return p;
    }

    // ---------------- the case inputs ----------------

    /** The two divisors the ch. 10.2 arm witnesses read; every other case supplies them harmlessly. */
    private static final String ARM_INPUT =
            ", \"nuldeler\": { \"number\": \"4\" }, \"gatdeler\": { \"number\": \"4\" }";

    /** The divisors every case supplies the same way unless it says otherwise: a case that leaves one out
     *  is making a point about THAT divisor, since a leeg divisor is a §6.5 Fout wherever its need is forced. */
    private static final String INPUT_REST =
            "\"teller\": { \"number\": \"40.00\" }, \"deler\": { \"number\": \"4\" }, \"staffel\": { \"number\": \"4\" }, "
            + "\"heffingsdeler\": { \"number\": \"4\" }, \"beschermdeler\": { \"number\": \"4\" }, "
            + "\"takdeler\": { \"number\": \"4\" }, \"takopslaggrond\": { \"number\": \"6.00\" }" + ARM_INPUT;
    private static final String GEVULD = "\"inkomen\": { \"number\": \"40.00\" }, " + INPUT_REST;
    private static final String LEEG = INPUT_REST;                     // inkomen ABSENT
    private static final String ZERO_DIVISOR =
            "\"inkomen\": { \"number\": \"40.00\" }, \"teller\": { \"number\": \"40.00\" }, "
            + "\"deler\": { \"number\": \"0\" }, \"staffel\": { \"number\": \"4\" }, "
            + "\"heffingsdeler\": { \"number\": \"4\" }, \"beschermdeler\": { \"number\": \"4\" }, "
            + "\"takdeler\": { \"number\": \"4\" }, \"takopslaggrond\": { \"number\": \"6.00\" }" + ARM_INPUT;
    private static final String BARRIER_BEFORE_THE_DIVISION =                // inkomen ABSENT and staffel 0
            "\"teller\": { \"number\": \"40.00\" }, \"deler\": { \"number\": \"0\" }, \"staffel\": { \"number\": \"0\" }, "
            + "\"heffingsdeler\": { \"number\": \"4\" }, \"beschermdeler\": { \"number\": \"4\" }, "
            + "\"takdeler\": { \"number\": \"4\" }, \"takopslaggrond\": { \"number\": \"6.00\" }" + ARM_INPUT;
    /** `heffingsDivisor` ABSENT: the §6.5 site inside a NEED's own value really runs, and faults. */
    private static final String EMPTY_HEFFINGS_DIVISOR =
            "\"inkomen\": { \"number\": \"40.00\" }, \"teller\": { \"number\": \"40.00\" }, "
            + "\"deler\": { \"number\": \"4\" }, \"staffel\": { \"number\": \"4\" }, "
            + "\"beschermdeler\": { \"number\": \"4\" }, "
            + "\"takdeler\": { \"number\": \"4\" }, \"takopslaggrond\": { \"number\": \"6.00\" }" + ARM_INPUT;
    /** `protectedDivisor` AND inkomen ABSENT: the same site sits BEHIND a barrier that fires first. */
    private static final String EMPTY_DIVISOR_BEHIND_THE_BARRIER =
            "\"teller\": { \"number\": \"40.00\" }, \"deler\": { \"number\": \"4\" }, "
            + "\"staffel\": { \"number\": \"4\" }, \"heffingsdeler\": { \"number\": \"4\" }, "
            + "\"takdeler\": { \"number\": \"4\" }, \"takopslaggrond\": { \"number\": \"6.00\" }" + ARM_INPUT;
    /** inkomen 40,00 (the DAN arm runs) with `armDivisor` ABSENT: the §6.5 site in the ENTERED arm really runs. */
    private static final String EMPTY_THEN_ARM_DIVISOR =
            "\"inkomen\": { \"number\": \"40.00\" }, \"teller\": { \"number\": \"40.00\" }, "
            + "\"deler\": { \"number\": \"4\" }, \"staffel\": { \"number\": \"4\" }, "
            + "\"heffingsdeler\": { \"number\": \"4\" }, \"beschermdeler\": { \"number\": \"4\" }, "
            + "\"takopslaggrond\": { \"number\": \"6.00\" }" + ARM_INPUT;
    /** inkomen 2,00 (the ANDERS arm runs) with the same divisor absent: the site is in the arm NOT taken. */
    private static final String EMPTY_ARM_DIVISOR_ELSE =
            "\"inkomen\": { \"number\": \"2.00\" }, \"teller\": { \"number\": \"40.00\" }, "
            + "\"deler\": { \"number\": \"4\" }, \"staffel\": { \"number\": \"4\" }, "
            + "\"heffingsdeler\": { \"number\": \"4\" }, \"beschermdeler\": { \"number\": \"4\" }, "
            + "\"takopslaggrond\": { \"number\": \"6.00\" }" + ARM_INPUT;
    /** inkomen 40,00 (the DAN arm runs) with `takopslagGround` ABSENT — a need only the OTHER arm reads. */
    private static final String UNREAD_TAKOPSLAG =
            "\"inkomen\": { \"number\": \"40.00\" }, \"teller\": { \"number\": \"40.00\" }, "
            + "\"deler\": { \"number\": \"4\" }, \"staffel\": { \"number\": \"4\" }, "
            + "\"heffingsdeler\": { \"number\": \"4\" }, \"beschermdeler\": { \"number\": \"4\" }, "
            + "\"takdeler\": { \"number\": \"4\" }" + ARM_INPUT;

    /** `zeroDivisor` 0 with the guard HOLDING: the division sits in the arm that is NOT taken. */
    private static final String ZERO_IN_THE_NOT_TAKEN_ARM =
            "\"inkomen\": { \"number\": \"40.00\" }, \"teller\": { \"number\": \"40.00\" }, "
            + "\"deler\": { \"number\": \"4\" }, \"staffel\": { \"number\": \"4\" }, "
            + "\"heffingsdeler\": { \"number\": \"4\" }, \"beschermdeler\": { \"number\": \"4\" }, "
            + "\"takdeler\": { \"number\": \"4\" }, \"takopslaggrond\": { \"number\": \"6.00\" }, "
            + "\"nuldeler\": { \"number\": \"0\" }, \"gatdeler\": { \"number\": \"4\" }";
    /** `gapDivisor` ABSENT with the guard HOLDING: the §6.5 site sits in a NEED only the other arm reads. */
    private static final String GAP_IN_A_UNREAD_NEED =
            "\"inkomen\": { \"number\": \"40.00\" }, \"teller\": { \"number\": \"40.00\" }, "
            + "\"deler\": { \"number\": \"4\" }, \"staffel\": { \"number\": \"4\" }, "
            + "\"heffingsdeler\": { \"number\": \"4\" }, \"beschermdeler\": { \"number\": \"4\" }, "
            + "\"takdeler\": { \"number\": \"4\" }, \"takopslaggrond\": { \"number\": \"6.00\" }, "
            + "\"nuldeler\": { \"number\": \"4\" }";

    private static String jutter(String attributes, String expected) {
        return jutterWith(attributes, expected + ", { \"kind\": \"consistent\" }");
    }

    private static String jutterWith(String attributes, String expected) {
        return """
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "J1", "objectType": "Jutter",
                        "attributes": { %s },
                        "roles": {}, "characteristics": [] } ],
                    "parameters": {} },
                  "expected": [ %s ] }
                """.formatted(attributes, expected);
    }

    private static String valueJson(String attribute, String number) {
        return """
                { "kind": "attributeValue", "label": "J1", "attribute": "%s", "value": { "number": "%s" } }"""
                .formatted(attribute, number);
    }

    private static String empty(String attribute) {
        return "{ \"kind\": \"attributeEmpty\", \"label\": \"J1\", \"attribute\": \"%s\" }".formatted(attribute);
    }

    // ---------------- the wire shapes (IR-SPEC ch. 4.10) ----------------

    private static final String UNSPECIFIED = "unspecified", NN = "nonNegative";

    private static String num(int dec, boolean whole, String sign) {
        return "{\"node\":\"Number\",\"whole\":%b,\"decimals\":%d,\"sign\":\"%s\"}"
                .formatted(whole, dec, sign);
    }

    private static String attr(String name, int dec, boolean whole, String sign) {
        return ("{\"node\":\"AttributeReference\",\"attribute\":\"%s\",\"subjectType\":\"Jutter\","
                + "\"source\":{\"snippet\":\"het %s van de Jutter\"}}")
                .formatted(name, name);
    }

    private static String lit(String valueJson, int dec, boolean whole) {
        return "{\"node\":\"Literal\",\"value\":{\"number\":\"%s\"},\"type\":%s,\"source\":{\"snippet\":\"%s\"}}"
                .formatted(valueJson, num(dec, whole, UNSPECIFIED), valueJson);
    }

    private static String binary(String op, String left, String right) {
        return "{\"node\":\"Binary\",\"op\":\"%s\",\"left\":%s,\"right\":%s,\"type\":%s,\"source\":{\"snippet\":\"%s\"}}"
                .formatted(op, left, right, num(2, false, UNSPECIFIED), op.toLowerCase());
    }

    /** ch. 4.10 the value-level binding; WF-076 makes its {@code type} the BODY's, which {@code sign} included
     *  is what the verifier checks — so the body's sign is spelled at each call site. */
    private static String let(String binder, String value, String body, String bodySign) {
        return "{\"node\":\"Let\",\"type\":%s,\"binder\":\"%s\",\"value\":%s,\"body\":%s,\"source\":{\"snippet\":\"de %s is …\"}}"
                .formatted(num(2, false, bodySign), binder, value, body, binder);
    }

    /** ch. 4.10 a read of the nearest enclosing binder. NB the wire field is {@code name}, not {@code naam};
     *  WF-091 makes the read's sign that of the value it is bound to, so it rides along. */
    private static String bref(String name, String sign) {
        return "{\"node\":\"BinderRef\",\"name\":\"%s\",\"source\":{\"snippet\":\"de %s\"}}"
                .formatted(name, name);
    }

    /** ch. 4.10 the value-level conditional. WF-069 makes both arms agree with the node's {@code type}, sign
     *  included, which the verifier checks — so the arms' shared sign is spelled at each call site. */
    private static String ite(String condition, String then, String otherwise, String sign) {
        return ("{\"node\":\"IfThenElse\",\"type\":%s,\"condition\":%s,\"then\":%s,\"else\":%s,"
                + "\"source\":{\"snippet\":\"als … dan … anders …\"}}").formatted(num(2, false, sign), condition, then, otherwise);
    }

    private static String compare(String op, String left, String right) {
        return "{\"node\":\"Comparison\",\"op\":\"%s\",\"left\":%s,\"right\":%s,\"source\":{\"snippet\":\"%s\"}}"
                .formatted(op, left, right, op.toLowerCase());
    }

    private static String attribute(String name, String datatype) {
        return "{\"node\":\"Attribute\",\"name\":\"%s\",\"datatype\":%s}".formatted(name, datatype);
    }

    /** A rule of one always-valid computing version; {@code conditionJson} "null" writes no condition. */
    private static String rule(String name, String target, String valueJson, String conditionJson) {
        return ("{\"node\":\"Rule\",\"name\":\"%s\",\"versions\":[{\"validity\":{\"node\":\"Validity\""
                + "},\"target\":%s,\"value\":%s%s,\"initialisation\":false,"
                + "\"presentation\":\"calculation\"}]}")
                .formatted(name, target, valueJson, conditionJson.equals("null") ? "" : ",\"condition\":" + conditionJson);
    }

    // ---------------- the one document (verifier-clean, ref- and clingo-replayed before pinning) ----------

    private static final String MODEL = buildModel();

    /** A one-rule document whose single binder is read under TWO different branch guards — the ONBEPAALD arm of
     *  the read-guard lattice, which this branch refuses by name. Kept apart from {@link #MODEL} for the reason
     *  the refusal exists: a marker makes the WHOLE document an EXCLUDE, so it may not sit beside pins that must
     *  play. Verifier-clean and ref-replayed (160.00) before pinning. */
    private static final String DOUBLE_GUARD_MODEL = buildDoubleGuard();

    private static final String DOUBLE_GUARD_CASE = jutter("\"inkomen\": { \"number\": \"40.00\" }",
            valueJson("dubbelgarde", "160.00"));

    private static String buildDoubleGuard() {
        String inkomen = attr("inkomen", 2, false, NN);
        String z = bref("z", UNSPECIFIED);
        String w = let("k", inkomen,
                let("z", binary("times", bref("k", NN), lit("2", 0, true)),
                        binary("plus",
                                ite(compare("greater", bref("k", NN), lit("10.00", 2, false)), z,
                                        lit("0.00", 2, false), UNSPECIFIED),
                                ite(compare("less", bref("k", NN), lit("99.00", 2, false)), z,
                                        lit("0.00", 2, false), UNSPECIFIED)),
                        UNSPECIFIED), UNSPECIFIED);
        return ("{\"format\":\"uchar-ir-v1\",\"model\":{"
                + "\"objectTypes\":[{\"node\":\"ObjectType\",\"name\":\"Jutter\",\"animate\":true,\"plural\":\"Jutters\","
                + "\"attributes\":[%s,%s],\"characteristics\":[]}],"
                + "\"factTypes\":[],\"parameters\":[],\"domains\":[],\"unitSystems\":[],\"dimensions\":[],"
                + "\"timelines\":[],\"dayKinds\":[]},"
                + "\"rules\":{\"rules\":[%s],"
                + "\"dayKindDefinitions\":[],\"objectCreations\":[],\"allocations\":[],\"consistencies\":[],"
                + "\"uniquenesses\":[],\"factCreations\":[],\"startPointDeterminations\":[],"
                + "\"ruleGroups\":[]},\"diagnostics\":[]}")
                .formatted(attribute("inkomen", num(2, false, NN)), attribute("dubbelgarde", num(2, false, UNSPECIFIED)),
                        rule("dubbelgarde van een jutter", attr("dubbelgarde", 2, false, UNSPECIFIED), w, "null"));
    }

    private static String buildModel() {
        String inkomen = attr("inkomen", 2, false, NN);
        String counter = attr("teller", 2, false, NN);
        String divisor = attr("deler", 0, true, NN);
        String tier = attr("staffel", 0, true, NN);
        String heffingsDivisor = attr("heffingsdeler", 0, true, NN);
        String protectedDivisor = attr("beschermdeler", 0, true, NN);
        String armDivisor = attr("takdeler", 0, true, NN);
        String takopslagGround = attr("takopslaggrond", 2, false, NN);
        String zeroDivisor = attr("nuldeler", 0, true, NN);
        String gapDivisor = attr("gatdeler", 0, true, NN);
        String above10 = compare("greater", bref("k", NN), lit("10.00", 2, false));
        String doubleDef = binary("times", inkomen, lit("2", 0, true));

        String[][] rs = {
            // the variable read in the voorwaarde (substituted) AND in the result (bound)
            {"aandeel van een jutter", attr("aandeel", 2, false, UNSPECIFIED),
             let("dubbel", doubleDef, binary("plus", bref("dubbel", UNSPECIFIED), bref("dubbel", UNSPECIFIED)), UNSPECIFIED),
             compare("greater", doubleDef, lit("10.00", 2, false))},
            // a later binder reading an earlier one
            {"gestaffeld van een jutter", attr("gestaffeld", 2, false, UNSPECIFIED),
             let("basis", inkomen,
                 let("opslag", binary("times", bref("basis", NN), lit("2", 0, true)),
                     binary("plus", bref("basis", NN), bref("opslag", UNSPECIFIED)), UNSPECIFIED), UNSPECIFIED),
             "null"},
            // the zero guard: the division sits inside the variable definition, behind the rule's voorwaarde
            {"quotient van een jutter", attr("quotient", 2, false, UNSPECIFIED),
             let("q", binary("dividedBy", counter, divisor),
                 binary("plus", bref("q", UNSPECIFIED), lit("1.00", 2, false)), UNSPECIFIED),
             compare("notEqual", divisor, lit("0", 0, true))},
            // an outer need that empties must keep the inner (zero-dividing) need unforced
            {"garde van een jutter", attr("garde", 2, false, UNSPECIFIED),
             let("veilig", inkomen,
                 let("q", binary("dividedBy", counter, tier),
                     binary("plus", bref("veilig", NN), bref("q", UNSPECIFIED)), UNSPECIFIED), UNSPECIFIED),
             "null"},
            // the barrier beside its contrast
            {"met let van een jutter", attr("metlet", 2, false, UNSPECIFIED),
             let("ink", inkomen, binary("times", bref("ink", NN), lit("2", 0, true)), UNSPECIFIED), "null"},
            {"zonder let van een jutter", attr("zonderlet", 2, false, UNSPECIFIED),
             binary("times", inkomen, lit("2", 0, true)), "null"},
            // one binder, two reads
            {"dubbelop van een jutter", attr("dubbelop", 2, false, UNSPECIFIED),
             let("ink", inkomen, binary("plus", bref("ink", NN), bref("ink", NN)), UNSPECIFIED), "null"},
            // name-sameness: shadowed inside, intact outside
            {"schaduw van een jutter", attr("schaduw", 2, false, UNSPECIFIED),
             let("x", inkomen,
                 binary("plus", let("x", lit("5.00", 2, false), bref("x", UNSPECIFIED), UNSPECIFIED),
                        bref("x", NN)), UNSPECIFIED), "null"},
            // sibling bindings: the barrier belongs to the Let that bound the binder that was read
            {"zusters van een jutter", attr("zusters", 2, false, UNSPECIFIED),
             binary("plus", let("a", inkomen, bref("a", NN), NN),
                    let("b", lit("5.00", 2, false), bref("b", UNSPECIFIED), UNSPECIFIED)), "null"},
            // the non-regression flank: a variable NO result read reaches gets no Let at all
            {"premie van een jutter", attr("premie", 2, false, UNSPECIFIED), lit("7.00", 2, false),
             compare("greater", doubleDef, lit("50.00", 2, false))},
            // a §6.5 site inside a NEED's own value — the companion must still be built for it
            {"heffing van een jutter", attr("heffing", 2, false, UNSPECIFIED),
             let("q", binary("dividedBy", counter, heffingsDivisor),
                 binary("plus", bref("q", UNSPECIFIED), lit("1.00", 2, false)), UNSPECIFIED), "null"},
            // the same site BEHIND a barrier — the companion must carry that need as its gate
            {"bescherm van een jutter", attr("bescherm", 2, false, UNSPECIFIED),
             let("veilig", inkomen,
                 let("r", binary("dividedBy", counter, protectedDivisor),
                     binary("plus", bref("veilig", NN), bref("r", UNSPECIFIED)), UNSPECIFIED), UNSPECIFIED), "null"},
            // ---- ch. 10.2, the value-level conditional (WF-069: inside a Let body; WF-077: binder guard) ----
            // both arms, one rule
            {"tak van een jutter", attr("tak", 2, false, UNSPECIFIED),
             let("k", inkomen, ite(above10, binary("times", bref("k", NN), lit("2", 0, true)),
                     lit("2.00", 2, false), UNSPECIFIED), UNSPECIFIED), "null"},
            // a §6.5 site in ONE arm — it runs in the entered arm and not in the other
            {"takdeling van een jutter", attr("takdeling", 2, false, UNSPECIFIED),
             let("k", inkomen, ite(above10, binary("dividedBy", counter, armDivisor),
                     lit("7.00", 2, false), UNSPECIFIED), UNSPECIFIED), "null"},
            // a NEED only the anders arm reads: the dan arm may not force it, nor raise its barrier
            {"takopslag van een jutter", attr("takopslag", 2, false, UNSPECIFIED),
             let("g", inkomen,
                 let("o", takopslagGround,
                     ite(compare("greater", bref("g", NN), lit("10.00", 2, false)), lit("3.00", 2, false),
                         binary("plus", lit("3.00", 2, false), bref("o", NN)), UNSPECIFIED), UNSPECIFIED), UNSPECIFIED), "null"},
            // the tariefstelling shape: a LATER need read by one arm, the guard reading the earlier binder
            {"takketen van een jutter", attr("takketen", 2, false, UNSPECIFIED),
             let("a", inkomen,
                 let("b", binary("times", bref("a", NN), lit("2", 0, true)),
                     ite(compare("greater", bref("a", NN), lit("10.00", 2, false)), bref("b", UNSPECIFIED),
                         lit("0.00", 2, false), UNSPECIFIED), UNSPECIFIED), UNSPECIFIED), "null"},
            // a Let INSIDE an arm: its need may not be forced when that arm is not chosen
            {"sprong van een jutter", attr("sprong", 2, false, UNSPECIFIED),
             let("k", inkomen,
                 ite(compare("greater", bref("k", NN), lit("10.00", 2, false)), lit("5.00", 2, false),
                     let("z", binary("dividedBy", counter, zeroDivisor),
                         binary("plus", bref("z", UNSPECIFIED), lit("1.00", 2, false)), UNSPECIFIED),
                     UNSPECIFIED), UNSPECIFIED), "null"},
            // a §6.5 site in the VALUE of a need only ONE arm reads — its companion carries that arm's guard
            {"takgat van een jutter", attr("takgat", 2, false, UNSPECIFIED),
             let("k", inkomen,
                 let("z", binary("dividedBy", counter, gapDivisor),
                     ite(compare("greater", bref("k", NN), lit("10.00", 2, false)), lit("4.00", 2, false),
                         bref("z", UNSPECIFIED), UNSPECIFIED), UNSPECIFIED), UNSPECIFIED), "null"},
        };

        StringBuilder rules = new StringBuilder();
        for (String[] r : rs) {
            if (rules.length() > 0) rules.append(',');
            rules.append(rule(r[0], r[1], r[2], r[3]));
        }
        StringBuilder attributes = new StringBuilder(String.join(",",
                attribute("inkomen", num(2, false, NN)), attribute("teller", num(2, false, NN)),
                attribute("deler", num(0, true, NN)), attribute("staffel", num(0, true, NN)),
                attribute("heffingsdeler", num(0, true, NN)), attribute("beschermdeler", num(0, true, NN)),
                attribute("takdeler", num(0, true, NN)), attribute("takopslaggrond", num(2, false, NN)),
                attribute("nuldeler", num(0, true, NN)), attribute("gatdeler", num(0, true, NN))));
        for (String name : new String[]{"aandeel", "gestaffeld", "quotient", "garde", "metlet", "zonderlet",
                                        "dubbelop", "schaduw", "zusters", "premie", "heffing", "bescherm",
                                        "tak", "takdeling", "takopslag", "takketen", "sprong", "takgat"})
            attributes.append(',').append(attribute(name, num(2, false, UNSPECIFIED)));

        return ("{\"format\":\"uchar-ir-v1\",\"model\":{"
                + "\"objectTypes\":[{\"node\":\"ObjectType\",\"name\":\"Jutter\",\"animate\":true,\"plural\":\"Jutters\","
                + "\"attributes\":[%s],\"characteristics\":[]}],"
                + "\"factTypes\":[],\"parameters\":[],\"domains\":[],\"unitSystems\":[],\"dimensions\":[],"
                + "\"timelines\":[],\"dayKinds\":[]},"
                + "\"rules\":{\"rules\":[%s],"
                + "\"dayKindDefinitions\":[],\"objectCreations\":[],\"allocations\":[],\"consistencies\":[],"
                + "\"uniquenesses\":[],\"factCreations\":[],\"startPointDeterminations\":[],"
                + "\"ruleGroups\":[]},\"diagnostics\":[]}")
                .formatted(attributes, rules);
    }
}
