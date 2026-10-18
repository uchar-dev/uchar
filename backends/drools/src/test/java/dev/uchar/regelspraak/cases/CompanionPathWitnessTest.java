package dev.uchar.regelspraak.cases;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The §6.5/§6.6 fout-companions carry the PATH: a companion fires exactly when the division / wortel it
 * guards is actually EVALUATED under ch. 10.2 (exactly one branch runs) and ch. 10.6 (call-by-need, the
 * leeg-barrier at the read) — never for a site on a path that does not run, never silent for a site on a
 * path that does. Witnessed via {@link CaseRunner#playCase} over models authored HERE (no corpus file is
 * read, moved or touched).
 *
 * <p>Before this card the companion was a SEPARATE static rule keyed on the operands' leegte, which cannot
 * see which path ran. The card before it chose never-fabricate ({@code letKinderenZeker}): no companion for a
 * site in a branch, in a body, or in a value not read on every path — so every such site with a REAL fault
 * missed it (the "spiegelflank": ref inconsistent, Drools consistent). Both flank families are pinned here,
 * each beside its twin, so an implementation that errs in either direction fails:
 * <ul>
 *   <li><b>the MISSER family</b> (a fault ref raises that a path-blind companion cannot): a division in a
 *       Let BODY, which under call-by-need always runs; a division in the branch ch. 10.2 DOES take; a
 *       division in a Let VALUE whose binder IS read; a division evaluated BEFORE a foreign barrier;</li>
 *   <li><b>the FABRICAGE family</b> (a fault ref does not raise, because the site never runs): a division
 *       behind a read that raises the barrier first; in the branch NOT taken; in a value no read forces; in
 *       a value read only on the branch not taken.</li>
 * </ul>
 * <b>Every pinned value was produced by {@code ref/} on this exact model+case first</b> (2026-09-02, the
 * L-ref) — ref is the arbiter, this backend the third executor. Evaluation ORDER matters for the barrier
 * flanks and was measured rather than assumed, PER FORM ({@code ref/lib/eval.ml}, 2026-09-02): a
 * {@code Binair} left before right, a {@code Vergelijking} in a ch. 10.2 guard left before right, and a
 * {@code Begrensd} basis, then MAXIMUM, then minimum — so a read evaluated before a division ends the Let
 * before the division, and a read evaluated after it does not. This backend mirrors each measured order
 * (fix round 1 of the companion-onder-drager card: the Begrensd arm evaluated minimum first and fabricated
 * on one Begrensd flank while missing its mirror). Since then ref evaluates a {@code Begrensd} basis, minimum,
 * maximum (RUNTIME-SEMANTICS ch. 2.7); the rows of (i) pin that order on the exec-model branch.
 *
 * <p><b>"Drager" / "carrier" below means the probe's HOST</b> — the OUTERMOST ch. 4.10 node on the path from
 * the value root down to the site, which the companion's condition re-evaluates
 * ({@code Site.carrier} of the destructive lowering's {@code ExecModelTranspiler}, until card tms-omschakeling,
 * 2026-10-10). It does NOT mean the {@code Expr.Onopgelost} carrier the loader
 * once built for an {@code IfThenElse}: since the union card (2026-09-10) all three cluster nodes are
 * ordinary {@code Expr} union nodes and nothing rides beside them.
 */
class CompanionPathWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // ---------------- (a) the spiegelflank: a Let body always runs ----------------

    @Test void a_division_in_a_running_letbody_with_unread_binder_is_inconsistent() throws Exception {
        // "uit := Let d = deler in inkomen gedeeld door tweede" — `d` is never read, so ch. 10.6 clause 3 never
        // evaluates `deler` and the body ALWAYS runs: the division by the absent `tweede` is real, whatever
        // `deler` holds. ref, both cases: uit leeg, INCONSISTENT. The bare twin (no Let) says the same.
        String verwacht = empty("uit") + ", " + inconsistent();
        assertNull(play(divisionModel(), jutter(INKOMEN, verwacht)), "de kale deling door leeg");
        assertNull(play(spiegelModel(), jutter(INKOMEN, verwacht)), "dezelfde deling in een Let-body, deler leeg");
        assertNull(play(spiegelModel(), jutter(INKOMEN + ", " + n("deler", "2.00"), verwacht)),
                "dezelfde deling in een Let-body, deler gevuld");
    }

    // ---------------- (b) the barrier that IS passed first, and the one that is not ----------------

    @Test void a_read_that_precedes_the_division_cuts_the_fault_off_via_the_barrier() throws Exception {
        // "uit := Let d = deler in d plus (inkomen gedeeld door tweede)" — the read of `d` comes FIRST (left
        // operand). With `deler` absent that read raises the barrier, the Let ends, and the division never
        // happens: ref uit leeg, CONSISTENT. With `deler` = 2,00 the read succeeds, the division runs and
        // faults (leeg, coalesced to 0 by the plus): ref uit 2.0000, INCONSISTENT.
        assertNull(play(barrierModel(true), jutter(INKOMEN, empty("uit") + ", " + consistent())),
                "lezing eerst, barriere: geen deling, geen fout");
        assertNull(play(barrierModel(true), jutter(INKOMEN + ", " + n("deler", "2.00"),
                        waarde("uit", "2.0000") + ", " + inconsistent())),
                "lezing eerst, gevuld: de deling draait en faalt");
    }

    @Test void the_same_division_before_the_read_deposits_the_fault_indeed() throws Exception {
        // the mirror: "(inkomen gedeeld door tweede) plus d" — the division is evaluated BEFORE the read, so
        // its fault is deposited whatever the read then does. ref: deler absent -> leeg, INCONSISTENT (the
        // barrier still empties the Let, but after the fault); deler 2,00 -> 2.0000, INCONSISTENT.
        assertNull(play(barrierModel(false), jutter(INKOMEN, empty("uit") + ", " + inconsistent())),
                "deling eerst, dan de barriere: de fout staat");
        assertNull(play(barrierModel(false), jutter(INKOMEN + ", " + n("deler", "2.00"),
                        waarde("uit", "2.0000") + ", " + inconsistent())),
                "deling eerst, gevuld");
    }

    // ---------------- (c) ch. 10.2: the branch taken, and the branch not taken ----------------

    @Test void a_division_in_the_taken_arm_is_inconsistent_and_in_the_not_taken_is_not() throws Exception {
        // "als a groter dan 100 dan inkomen gedeeld door deler anders 7,0000", `deler` absent. a = 200 takes
        // the DAN branch: the division runs -> ref uit leeg, INCONSISTENT. a = 1 takes ANDERS: ref 7.0000,
        // CONSISTENT. Both orientations, bare (the IfThenElse as the rule value) and in a Let body whose binder
        // feeds the condition (the WF-077 shape).
        String taken = empty("uit") + ", " + inconsistent();
        String notTaken = waarde("uit", "7.0000") + ", " + consistent();
        for (boolean inLet : new boolean[]{false, true}) {
            String vorm = inLet ? " (in een Let-body)" : " (kaal)";
            assertNull(play(choiceModel(inLet, false), jutter(INKOMEN + ", " + n("a", "200.00"), taken)),
                    "de genomen tak" + vorm);
            assertNull(play(choiceModel(inLet, false), jutter(INKOMEN + ", " + n("a", "1.00"), notTaken)),
                    "de niet-genomen tak" + vorm);
        }
    }

    // ---------------- (d) ch. 10.6: a division in the VALUE runs only when a read forces it ----------------

    @Test void a_division_in_a_letvalue_fails_only_if_the_binder_is_read() throws Exception {
        // "Let q = inkomen gedeeld door tweede in q plus 1,0000" — the read forces the need: the division runs,
        // faults (leeg) and the barrier then empties the Let. ref: leeg, INCONSISTENT.
        // "Let q = inkomen gedeeld door tweede in 42,0000" — nothing reads q, nothing runs. ref: 42.0000, CONSISTENT.
        assertNull(play(valueModel(true), jutter(INKOMEN, empty("uit") + ", " + inconsistent())), "gelezen");
        assertNull(play(valueModel(false), jutter(INKOMEN, waarde("uit", "42.0000") + ", " + consistent())), "ongelezen");
    }

    @Test void a_letvalue_read_in_only_one_arm_fails_per_arm() throws Exception {
        // "Let k = a in Let q = inkomen gedeeld door tweede in (als k groter dan 100 dan q anders 7,0000)":
        // q is read only in the DAN branch. a = 200 -> the read forces the faulting division: ref leeg,
        // INCONSISTENT. a = 1 -> ANDERS, q never read: ref 7.0000, CONSISTENT. (The card before this one could
        // certify neither — "read on some but not every path" got no companion at all.)
        assertNull(play(oneArmModel(), jutter(INKOMEN + ", " + n("a", "200.00"), empty("uit") + ", " + inconsistent())),
                "gelezen op de genomen tak");
        assertNull(play(oneArmModel(), jutter(INKOMEN + ", " + n("a", "1.00"), waarde("uit", "7.0000") + ", " + consistent())),
                "ongelezen op de niet-genomen tak");
    }

    // ---------------- (e) a FOREIGN barrier: another Let's read, before or after the forcing read ----------------

    @Test void a_read_of_another_empty_binder_before_the_division_cuts_off_but_after_it_does_not() throws Exception {
        // "Let x = leegding in Let q = inkomen gedeeld door tweede in x plus q", `leegding` absent: the read of
        // x raises the OUTER Let's barrier before q is ever forced — ref leeg, CONSISTENT. Mirror "q plus x":
        // q is forced first (fault), then x ends the outer Let — ref leeg, INCONSISTENT. The barrier belongs to
        // the Let that bound the binder that was read, and the order of the two reads decides.
        assertNull(play(foreignBarrierModel(true), jutter(INKOMEN, empty("uit") + ", " + consistent())),
                "de vreemde lezing eerst");
        assertNull(play(foreignBarrierModel(false), jutter(INKOMEN, empty("uit") + ", " + inconsistent())),
                "de vreemde lezing erna");
    }

    // ---------------- (f) the §6.6 wortel twins ----------------

    @Test void a_square_root_in_a_running_letbody_is_equally_inconsistent_as_the_bare() throws Exception {
        // "Let d = deler in de wortel van het saldo", saldo = -4,00, binder unread: the body runs, the negative
        // radicand degrades to leeg AND deposits the §6.6 Fout. ref on both models: uit leeg, INCONSISTENT.
        String verwacht = empty("uit") + ", " + inconsistent();
        String invoer = n("saldo", "-4.00");
        assertNull(play(squareRootModel(false), jutter(invoer, verwacht)), "de kale wortel van negatief");
        assertNull(play(squareRootModel(true), jutter(invoer, verwacht)), "dezelfde wortel in een Let-body");
    }

    @Test void a_square_root_in_the_taken_arm_is_inconsistent_and_in_the_not_taken_is_not() throws Exception {
        // "Let a = a in (als a groter dan 100 dan de wortel van het saldo anders 7,0000)", saldo = -4,00.
        // ref: a = 200 -> leeg, INCONSISTENT; a = 1 -> 7.0000, CONSISTENT.
        assertNull(play(choiceModel(true, true), jutter(n("saldo", "-4.00") + ", " + n("a", "200.00"),
                empty("uit") + ", " + inconsistent())), "de genomen tak");
        assertNull(play(choiceModel(true, true), jutter(n("saldo", "-4.00") + ", " + n("a", "1.00"),
                waarde("uit", "7.0000") + ", " + consistent())), "de niet-genomen tak");
    }

    // ---------------- (a'') a second, NON-qualifying site under the same carrier ----------------

    @Test void a_non_qualifying_site_under_the_same_carrier_also_reports_its_fault() throws Exception {
        // "Let d = deler in ((inkomen ÷ tweede) plus (5,00 ÷ leegding))", inkomen 10,00 and tweede 2,00 present,
        // `leegding` absent. Site 1 qualifies (two attributes) and does NOT fault; site 2 has a LITERAL dividend,
        // so no static predicate can guard it and it carries no companion of its own — but it RUNS and faults.
        // ref: uit 5.0000, INCONSISTENT. The probe sees that fault (DepositFlag.report sits in the division arm, which is
        // operand-blind), so the carrier's companion reports on "a §6.5/§6.6 site under this carrier ran and
        // faulted", not on "THIS site did": a per-site identity gate throws the observation away and answers a
        // silent consistent. Twin: with `leegding` = 5,00 nothing faults -> ref 6.0000, CONSISTENT.
        assertNull(play(coSiteModel(), jutter(INKOMEN + ", " + n("tweede", "2.00"),
                        waarde("uit", "5.0000") + ", " + inconsistent())),
                "de literal-deling onder dezelfde drager faalt: de sonde ziet haar");
        assertNull(play(coSiteModel(), jutter(INKOMEN + ", " + n("tweede", "2.00") + ", " + n("leegding", "5.00"),
                        waarde("uit", "6.0000") + ", " + consistent())),
                "niets faalt: de verbrede poort mag niet vuren");
    }

    @Test void the_widened_gate_does_not_fire_if_the_barrier_truncates_both_sites() throws Exception {
        // the fabricage twin of (a''): the same two sites behind a read of `d`,
        // "Let d = deler in (d plus ((inkomen ÷ tweede) plus (5,00 ÷ leegding)))". `deler` absent -> the read
        // raises the barrier before either site runs: ref uit leeg, CONSISTENT — nothing reports, nothing fires.
        // `deler` = 2,00 -> both sites run, only the non-qualifying one faults: ref uit 7.0000 (2 + 5 + leeg-as-0),
        // INCONSISTENT.
        assertNull(play(coSiteBehindBarrierModel(), jutter(INKOMEN + ", " + n("tweede", "2.00"),
                        empty("uit") + ", " + consistent())),
                "de barriere kapt beide plekken af");
        assertNull(play(coSiteBehindBarrierModel(), jutter(INKOMEN + ", " + n("tweede", "2.00") + ", " + n("deler", "2.00"),
                        waarde("uit", "7.0000") + ", " + inconsistent())),
                "het pad loopt door: de niet-kwalificerende plek faalt");
    }

    // ---------------- (e') a nested Let must pass a FOREIGN barrier through ----------------

    @Test void an_innermost_let_lets_the_foreign_barrier_through_to_the_outermost() throws Exception {
        // "Let x = leegding in ((Let q = 1,0000 in x plus q) plus (inkomen ÷ tweede))", `leegding` and `tweede`
        // absent. The read of x sits INSIDE the inner Let but binds to the OUTER one, so ch. 10.6 clause 2 ends
        // the OUTER Let: the inner handler must RETHROW a barrier that is not its own instance. Then the division
        // to its right never runs. ref: uit leeg, CONSISTENT. An inner handler that swallows every barrier would
        // answer leeg for the inner Let and go on to the division — a fabricated fault.
        assertNull(play(foreignBarrierPassThroughModel(true), jutter(INKOMEN, empty("uit") + ", " + consistent())),
                "de sonde-kant: de doorgegeven barriere kapt de deling af");
        // the value-side twin (no §6.5 site at all): swallowing the barrier would let the outer body finish at
        // 1 + 5 = 6.0000 instead of emptying the Let. ref: uit leeg. (This flank belongs beside the ch. 10.6
        // barrier witnesses of LetClusterWitnessTest — that file is out of this card's mandate, see the card.)
        assertNull(play(foreignBarrierPassThroughModel(false), jutter(INKOMEN, empty("uit") + ", " + consistent())),
                "de waarde-kant: de buitenste Let wordt leeg, niet 6.0000");
    }

    // ---------------- (h) two sites in one rule value: each carries its OWN path ----------------

    @Test void two_divisions_in_a_rule_value_each_get_their_own_path_condition() throws Exception {
        // "(inkomen gedeeld door deler) plus (Let d = leegding in d plus (inkomen gedeeld door tweede))" — the
        // first division is bare (runs whenever the rule does), the second sits behind the read of `d`. Two
        // companions, indexed 1 and 2 (the DRL naming parity), each with its own condition. ref:
        //   everything absent      -> 0.0000, INCONSISTENT (site 1 faults; site 2 is cut off by the barrier)
        //   deler 2, leegding 1    -> 6.0000, INCONSISTENT (site 1 = 5; d read fine, site 2 faults -> 1 + 0)
        //   deler 2, leegding leeg -> 5.0000, CONSISTENT   (site 1 = 5; the barrier empties the Let; no fault)
        assertNull(play(twoDivisionsModel(), jutter(INKOMEN, waarde("uit", "0.0000") + ", " + inconsistent())),
                "beide operanden leeg: de kale deling faalt, de omhulde is afgekapt");
        assertNull(play(twoDivisionsModel(), jutter(INKOMEN + ", " + n("deler", "2.00") + ", " + n("leegding", "1.00"),
                waarde("uit", "6.0000") + ", " + inconsistent())), "alleen de omhulde deling faalt");
        assertNull(play(twoDivisionsModel(), jutter(INKOMEN + ", " + n("deler", "2.00"),
                waarde("uit", "5.0000") + ", " + consistent())), "de omhulde deling is afgekapt, de kale rekent");
    }

    // ---------------- (p) a Parameter inside the carrier: the companion binds what the path reads ----------------

    @Test void a_parameter_in_the_carrier_is_bound_for_the_path_condition() throws Exception {
        // "Let d = deler in (inkomen gedeeld door tweede) plus GRONDSLAG", grondslag = 3 (a Parameter global).
        // The path condition evaluates the carrier, so it must bind the same globals the host binds. ref:
        // tweede absent -> 3.0000, INCONSISTENT; tweede 2,00 -> 8.0000, CONSISTENT.
        assertNull(play(parameterModel(), jutter(INKOMEN, waarde("uit", "3.0000") + ", " + inconsistent(), "\"grondslag\": { \"number\": \"3\" }")),
                "de deling faalt, de parameter telt op");
        assertNull(play(parameterModel(), jutter(INKOMEN + ", " + n("tweede", "2.00"), waarde("uit", "8.0000") + ", " + consistent(), "\"grondslag\": { \"number\": \"3\" }")),
                "de deling rekent");
    }

    // ---------------- (g) the defaults consequence keeps its fence ----------------

    @Test void the_defaults_division_fence_stays_standing_for_a_division_in_a_letbody() throws Exception {
        // A ch. 7 default whose consequence is "Let d = deler in inkomen gedeeld door tweede". ref on this
        // model+case (tweede absent, kindertal 3): toeslag 0.0000, CONSISTENT — the faulting default falls
        // through and ch. 7.1 discards its speculative channel deposit. That value this backend deliberately
        // does not produce: the division fence refuses the group, and the path-carrying companions of this
        // card are RULE machinery that must not start playing a defaults consequence. The honest outcome is
        // still the refusal, naming the fence.
        CaseRunner.Excluded refusal = assertThrows(CaseRunner.Excluded.class,
                () -> play(defaultsModel(let("d", attr("deler"), DIVISION, num(4))), jutter(INKOMEN + ", " + n("kindertal", "3"),
                        waarde("toeslag", "0.0000") + ", " + consistent())));
        assertTrue(refusal.getMessage().contains("deling"),
                "de weigering noemt het delingshek: " + refusal.getMessage());
    }

    // ---------------- (a') the §6.5 predicate's edges under a carrier: the ABS variant, the leeg dividend ----------------

    @Test void an_abs_division_in_a_running_letbody_is_equally_inconsistent_as_the_bare() throws Exception {
        // "gedeeld door (ABS)" is the second §6.5 variant and lowers through its own arm; under a carrier its
        // fault must reach the probe exactly like the plain division's. ref, both models (tweede absent):
        // uit leeg, INCONSISTENT.
        String verwacht = empty("uit") + ", " + inconsistent();
        assertNull(play(ofModel(DIVISION_ABS), jutter(INKOMEN, verwacht)), "de kale ABS-deling door leeg");
        assertNull(play(ofModel(let("d", attr("deler"), DIVISION_ABS, num(4))), jutter(INKOMEN, verwacht)),
                "dezelfde ABS-deling in een Let-body");
    }

    @Test void an_empty_dividend_under_a_carrier_is_zero_without_fault() throws Exception {
        // §6.5 / ch. 1.2: the LEFT operand is tested first — leeg ÷ x coalesces to 0 and deposits nothing, even
        // when the divisor is leeg too. The probe must keep that guard: a report on "divisor leeg" alone would
        // fabricate. ref: "Let d = deler in leegding gedeeld door tweede", both absent -> 0.0000, CONSISTENT;
        // tweede 2,00 -> 0.0000, CONSISTENT; the bare twin -> 0.0000, CONSISTENT.
        String verwacht = waarde("uit", "0.0000") + ", " + consistent();
        assertNull(play(ofModel(DIVISION_LEEGDING), jutter(INKOMEN, verwacht)), "de kale deling met leeg dividend");
        assertNull(play(emptyDividendModel(), jutter(INKOMEN, verwacht)), "leeg dividend, lege deler, in een Let-body");
        assertNull(play(emptyDividendModel(), jutter(INKOMEN + ", " + n("tweede", "2.00"), verwacht)),
                "leeg dividend, gevulde deler, in een Let-body");
    }

    // ---------------- (f') the §6.6 predicate's edge under a carrier: the zero radicand ----------------

    @Test void a_square_root_of_zero_under_a_carrier_is_zero_without_fault() throws Exception {
        // §6.6 faults on a NEGATIVE radicand only; zero is a real root. The probe must not widen the predicate.
        // ref, saldo = 0,00: bare -> 0.0000, CONSISTENT; in a Let body -> 0.0000, CONSISTENT.
        String verwacht = waarde("uit", "0.0000") + ", " + consistent();
        String invoer = n("saldo", "0.00");
        assertNull(play(squareRootModel(false), jutter(invoer, verwacht)), "de kale wortel van nul");
        assertNull(play(squareRootModel(true), jutter(invoer, verwacht)), "dezelfde wortel in een Let-body");
    }

    // ---------------- (i) evaluation ORDER inside a non-carrier form: Begrensd, minimum before maximum ----------------

    @Test void the_bounds_of_a_clamp_evaluate_minimum_before_maximum() throws Exception {
        // ref evaluates a Begrensd as basis, then minimum, then maximum (RUNTIME-SEMANTICS ch. 2.7). With a barrier
        // read in one bound and a division in the other, that order decides whether the division runs. "Let d = deler
        // in begrensd(inkomen, minimum d, maximum inkomen ÷ tweede)", deler and tweede absent: the minimum's read ends
        // the Let before the maximum divides -> ref uit leeg, CONSISTENT. Mirror (minimum the division, maximum the
        // read): the division runs first and deposits (§6.5) -> ref uit leeg, INCONSISTENT. On the truth-maintenance
        // branch; until 2026-10-10 this row pinned the order maximum, minimum of the destructive exec-model branch,
        // which ref no longer gives. Measured 2026-10-10: both documents verifier-clean (the clamp and the Let typed
        // with the 2 decimals of the base), ref passes both answers, clingo declines a clamp bound that is no literal.
        assertNull(playTms(clampModel(true), jutter(INKOMEN, empty("uit") + ", " + consistent())),
                "lezing in het minimum, deling in het maximum: de barriere komt eerst");
        assertNull(playTms(clampModel(false), jutter(INKOMEN, empty("uit") + ", " + inconsistent())),
                "deling in het minimum, lezing in het maximum: de deling draait eerst");
    }

    @Test void the_same_bound_order_decides_between_hard_fault_and_barrier() throws Exception {
        // the value-side twin: a division by NUMERIC zero (a hard fault, not a channel deposit) in one bound,
        // the barrier read in the other, nul = 0,00. "… minimum d, maximum inkomen ÷ nul": the minimum's read ends
        // the Let first and the zero division never runs -> ref uit leeg, CONSISTENT. Mirror "… minimum inkomen ÷
        // nul, maximum d": ref evaluates the minimum first and dies, EVALUATION FAULT "division by numeric zero",
        // which the branch gives as the runtime's own ArithmeticException at the fixpoint. On the truth-maintenance
        // branch, measured as the row above.
        String invoer = INKOMEN + ", " + n("nul", "0.00");
        assertNull(playTms(clampZeroModel(true), jutter(invoer, empty("uit") + ", " + consistent())),
                "de lezing in het minimum kapt af voordat het maximum door nul deelt");
        RuntimeException fout = assertThrows(RuntimeException.class,
                () -> playTms(clampZeroModel(false), jutter(invoer, empty("uit") + ", " + consistent())));
        assertTrue(causeText(fout).contains("10 gedeeld door 0"),
                "het minimum deelt door nul voordat het maximum de barriere opwerpt: " + causeText(fout));
    }

    @Test void the_base_of_a_clamp_evaluates_before_the_maximum() throws Exception {
        // the third point of the same order: basis BEFORE maximum. (i) above pins only the PAIR minimum/maximum,
        // which an implementation evaluating maximum, basis, minimum still satisfies. "Let d = deler in
        // begrensd(basis inkomen ÷ tweede, minimum 0,0000, maximum d)", deler and tweede absent: the basis
        // division runs and faults FIRST, then the maximum's read ends the Let -> ref uit leeg, INCONSISTENT.
        // Mirror (basis the read, maximum the division): the basis read ends the Let before the maximum's
        // division runs -> ref uit leeg, CONSISTENT.
        assertNull(play(clampBaseModel(true), jutter(INKOMEN, empty("uit") + ", " + inconsistent())),
                "deling in de basis, lezing in het maximum: de deling draait eerst");
        assertNull(play(clampBaseModel(false), jutter(INKOMEN, empty("uit") + ", " + consistent())),
                "lezing in de basis, deling in het maximum: de barriere komt eerst");
    }

    @Test void the_operands_of_a_choice_condition_evaluate_left_before_right() throws Exception {
        // the ch. 10.2 guard's Vergelijking: ref evaluates links before rechts (ref/lib/eval.ml, eval_cond).
        // "Let d = deler in (als d groter dan (inkomen ÷ tweede) dan 1 anders 2)", deler and tweede absent: the
        // read of d is the LEFT operand, raises the barrier, the division never runs -> ref uit leeg,
        // CONSISTENT. Mirror "(inkomen ÷ tweede) groter dan d": the division runs and faults, then the read
        // ends the Let -> ref uit leeg, INCONSISTENT.
        assertNull(play(conditionOrderModel(true), jutter(INKOMEN, empty("uit") + ", " + consistent())),
                "lezing links, deling rechts");
        assertNull(play(conditionOrderModel(false), jutter(INKOMEN, empty("uit") + ", " + inconsistent())),
                "deling links, lezing rechts");
    }

    // ---------------- (r) class-reactive: the probe re-runs after a producer that fires AFTER the companion ----------------

    @Test void the_deposit_flag_checks_the_carrier_again_after_a_producer_that_fires_after_the_companion() throws Exception {
        // `deler` is DERIVED by a rule that §9.2 stratification FORCES to fire after this companion (its
        // voorwaarde reads a negated kenmerk that has a producer -> salience -1; the LetClusterWitnessTest
        // idiom). At the companion's first activation `deler` is still leeg: the body's read of d raises the
        // barrier and the division is cut off — no fault yet. Then the producer writes deler = 2,00, the subject
        // is updated, and only a CLASS-REACTIVE companion pattern (the always-true freshness expr) re-probes:
        // now the read succeeds, the division by the absent `tweede` runs and faults. Without the freshness
        // expr the companion activates once and the fault is lost (verified by mutation).
        // ref: "Let d = deler in d plus (inkomen ÷ tweede)" -> uit 2.0000, INCONSISTENT;
        //      "Let d = deler in (als d groter dan 1 dan inkomen ÷ tweede anders 7)" -> uit leeg, INCONSISTENT.
        assertNull(play(lateProducerModel(false), jutter(INKOMEN, waarde("uit", "2.0000") + ", " + inconsistent())),
                "de barriere-vorm: de late producent opent het pad naar de deling");
        assertNull(play(lateProducerModel(true), jutter(INKOMEN, empty("uit") + ", " + inconsistent())),
                "de tak-vorm: de late producent kiest de delende tak");
    }

    // ---------------- (g') the defaults fence covers the §6.6 wortel too ----------------

    @Test void the_defaults_fence_also_refuses_a_square_root_in_the_consequence() throws Exception {
        // the §6.6 sibling of the division fence: a default whose consequence is "wortel(saldo) plus 0,0000",
        // taken (kindertal 3 >= 2) with saldo = -4,00. ref: toeslag 0.0000 (leeg + 0), INCONSISTENT — the §6.6
        // deposit stands. This backend lowers no §9.5 channel for a defaults consequence, so playing the group
        // would answer CONSISTENT in silence; the honest outcome is the same loud refusal the division gets,
        // naming the wortel.
        CaseRunner.Excluded refusal = assertThrows(CaseRunner.Excluded.class,
                () -> play(defaultsModel(binary("plus", SQUARE_ROOT, lit("0.0000", num(4)), num(4))),
                        jutter(n("saldo", "-4.00") + ", " + n("kindertal", "3"),
                                waarde("toeslag", "0.0000") + ", " + inconsistent())));
        assertTrue(refusal.getMessage().contains("wortel"),
                "de weigering noemt de wortel: " + refusal.getMessage());
    }

    // ---------------- (j) a BINDER as the operand of a site under its Let: the variabelendeel shape ----------------

    @Test void a_binder_as_dividend_under_its_let_gets_the_path_condition() throws Exception {
        // "Let n = inkomen in n gedeeld door tweede" — the shape the RegelSpraak emitter produces for a
        // "Daarbij geldt" variabelendeel. The dividend is a binder READ, not an attribute: there is no operand
        // to re-read for a static leeg/vol predicate, but under a carrier the probe observes the division itself,
        // so the site qualifies. ref: inkomen 10,00, tweede absent -> the read succeeds and the division faults:
        // uit leeg, INCONSISTENT; inkomen absent -> the read raises the barrier before the division: uit leeg,
        // CONSISTENT; inkomen 10,00, tweede 2,00 -> 5.0000, CONSISTENT. The bare twin of (a) is repeated first.
        assertNull(play(divisionModel(), jutter(INKOMEN, empty("uit") + ", " + inconsistent())), "de kale deling door leeg");
        assertNull(play(binderDividendModel(), jutter(INKOMEN, empty("uit") + ", " + inconsistent())),
                "de binder als dividend, de deler leeg: de deling draait en faalt");
        assertNull(play(binderDividendModel(), jutter("", empty("uit") + ", " + consistent())),
                "de binder zelf leeg: de barriere komt voor de deling");
        assertNull(play(binderDividendModel(), jutter(INKOMEN + ", " + n("tweede", "2.00"),
                        waarde("uit", "5.0000") + ", " + consistent())), "beide gevuld: geen fout");
    }

    @Test void a_binder_as_divisor_under_its_let_never_fails_on_empty() throws Exception {
        // "Let d = deler in inkomen gedeeld door d": a binder read never ANSWERS leeg — a leeg need raises the
        // barrier at the read (ch. 10.6 clause 2) — so the division never sees a leeg divisor and §6.5 has
        // nothing to deposit. The widened companion must not fabricate here. ref: deler absent -> uit leeg,
        // CONSISTENT; deler 2,00 -> 5.0000, CONSISTENT.
        assertNull(play(binderDivisorModel(), jutter(INKOMEN, empty("uit") + ", " + consistent())),
                "de deler-binder leeg: de barriere, geen fout");
        assertNull(play(binderDivisorModel(), jutter(INKOMEN + ", " + n("deler", "2.00"),
                        waarde("uit", "5.0000") + ", " + consistent())), "gevuld: de deling rekent");
    }

    @Test void a_binder_as_radicand_under_its_let_gets_the_path_condition() throws Exception {
        // the §6.6 sibling: "Let s = saldo in de wortel van s". ref: saldo -4,00 -> the read succeeds and the
        // negative radicand faults: uit leeg, INCONSISTENT; saldo absent -> the barrier: uit leeg, CONSISTENT.
        // The bare twin of (f) is repeated first.
        assertNull(play(squareRootModel(false), jutter(n("saldo", "-4.00"), empty("uit") + ", " + inconsistent())),
                "de kale wortel van negatief");
        assertNull(play(binderRadicandModel(), jutter(n("saldo", "-4.00"), empty("uit") + ", " + inconsistent())),
                "de binder als radicand, negatief: de wortel draait en faalt");
        assertNull(play(binderRadicandModel(), jutter("", empty("uit") + ", " + consistent())),
                "de binder zelf leeg: de barriere komt voor de wortel");
    }

    // ---------------- helpers: replay ----------------

    /** One authored model + case through the FULL replay; the per-case failure line, null on a match. */
    private static String play(String ir, String caseJson) throws Exception {
        Path tmp = Files.createTempFile("companion-pad-witness", ".json");
        try {
            Files.writeString(tmp, ir);
            return CaseRunner.playCase(tmp, JSON.readTree(caseJson));
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    /** As {@link #play}, on the exec-model branch. */
    private static String playTms(String ir, String caseJson) throws Exception {
        Path tmp = Files.createTempFile("companion-pad-witness", ".json");
        try {
            Files.writeString(tmp, ir);
            return CaseRunner.playCase(tmp, JSON.readTree(caseJson), DroolsBackend.Branch.EXEC_MODEL);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    /** Every message down the cause chain (Drools wraps a consequence's exception). */
    private static String causeText(Throwable t) {
        StringBuilder sb = new StringBuilder();
        for (Throwable c = t; c != null; c = c.getCause()) {
            sb.append(c.getClass().getSimpleName()).append(": ").append(c.getMessage()).append(" <- ");
            if (c.getCause() == c) break;
        }
        return sb.toString();
    }

    // ---------------- helpers: the case shapes (uchar-case-v0) ----------------

    private static final String INKOMEN = "\"inkomen\": { \"number\": \"10.00\" }";

    private static String n(String attribuut, String number) {
        return "\"%s\": { \"number\": \"%s\" }".formatted(attribuut, number);
    }

    private static String jutter(String attributes, String verwacht) { return jutter(attributes, verwacht, ""); }

    private static String jutter(String attributes, String verwacht, String parameters) {
        return """
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "J1", "objectType": "Jutter",
                        "attributes": { %s },
                        "roles": {}, "characteristics": [] } ],
                    "parameters": { %s } },
                  "expected": [ %s ] }
                """.formatted(attributes, parameters, verwacht);
    }

    private static String waarde(String attribuut, String number) {
        return "{ \"kind\": \"attributeValue\", \"label\": \"J1\", \"attribute\": \"%s\", \"value\": { \"number\": \"%s\" } }"
                .formatted(attribuut, number);
    }

    private static String empty(String attribuut) {
        return "{ \"kind\": \"attributeEmpty\", \"label\": \"J1\", \"attribute\": \"%s\" }".formatted(attribuut);
    }

    private static String consistent() { return "{ \"kind\": \"consistent\" }"; }
    private static String inconsistent() { return "{ \"kind\": \"inconsistent\" }"; }

    // ---------------- helpers: the wire shapes (IR-SPEC ch. 4.10 and its neighbours) ----------------

    private static String num(int dec, boolean whole, String sign) {
        return "{\"node\":\"Number\",\"whole\":%b,\"decimals\":%d,\"sign\":\"%s\"}"
                .formatted(whole, dec, sign);
    }
    private static String num(int dec) { return num(dec, false, "unspecified"); }
    private static String pos(int dec) { return num(dec, false, "nonNegative"); }
    private static final String WHOLE_POS = num(0, true, "nonNegative");

    private static String attr(String naam, String type) {
        return ("{\"node\":\"AttributeReference\",\"attribute\":\"%s\",\"subjectType\":\"Jutter\","
                + "\"source\":{\"snippet\":\"het %s van de Jutter\"}}")
                .formatted(naam, naam);
    }
    private static String attr(String naam) { return attr(naam, pos(2)); }

    private static String lit(String waarde, String type) {
        return IrPath.literal(waarde, type, waarde);
    }

    private static String param(String naam, String type) {
        return "{\"node\":\"ParameterReference\",\"parameter\":\"%s\",\"source\":{\"snippet\":\"de %s\"}}".formatted(naam, naam);
    }

    private static String binary(String op, String left, String right, String type) {
        return "{\"node\":\"Binary\",\"op\":\"%s\",\"left\":%s,\"right\":%s,\"type\":%s,\"source\":{\"snippet\":\"%s\"}}"
                .formatted(op, left, right, type, op.toLowerCase(java.util.Locale.ROOT));
    }

    private static String squareRoot(String argument, String type) {
        return "{\"node\":\"BuiltinCall\",\"kind\":\"squareRoot\",\"arguments\":[%s],\"type\":%s,\"source\":{\"snippet\":\"de wortel van\"}}"
                .formatted(argument, type);
    }

    private static String let(String binder, String value, String body, String type) {
        return "{\"node\":\"Let\",\"type\":%s,\"binder\":\"%s\",\"value\":%s,\"body\":%s,\"source\":{\"snippet\":\"%s is …\"}}"
                .formatted(type, binder, value, body, binder);
    }

    /** NB the wire field is {@code name}, not {@code naam}. */
    private static String bref(String naam, String type) {
        return "{\"node\":\"BinderRef\",\"name\":\"%s\",\"source\":{\"snippet\":\"%s\"}}".formatted(naam, naam);
    }

    private static String ite(String condition, String then, String otherwise, String type) {
        return ("{\"node\":\"IfThenElse\",\"type\":%s,\"condition\":%s,\"then\":%s,\"else\":%s,"
                + "\"source\":{\"snippet\":\"als … dan … anders …\"}}").formatted(type, condition, then, otherwise);
    }

    private static String clamped(String base, String minimum, String maximum) {
        return clamped(base, minimum, maximum, num(4));
    }

    private static String clamped(String base, String minimum, String maximum, String type) {
        return "{\"node\":\"Clamp\",\"base\":%s,\"minimum\":%s,\"maximum\":%s,\"type\":%s,\"source\":{\"snippet\":\"begrensd\"}}"
                .formatted(base, minimum, maximum, type);
    }

    private static String verg(String op, String left, String right) {
        return "{\"node\":\"Comparison\",\"op\":\"%s\",\"left\":%s,\"right\":%s,\"source\":{\"snippet\":\"%s\"}}"
                .formatted(op, left, right, op.toLowerCase(java.util.Locale.ROOT));
    }

    private static final String ALTIJD =
            "{\"node\":\"Compound\",\"quantifier\":\"all\",\"threshold\":0,\"parts\":[],\"source\":{\"snippet\":\"altijd\"}}";

    private static String regel(String naam, String doel, String waarde) {
        return regel(naam, "BEREKENING", doel, waarde, "null");
    }

    private static String regel(String naam, String soort, String doel, String waarde, String voorwaarde) {
        return IrPath.regel(naam, soort, doel, waarde, voorwaarde);
    }

    /** The one Jutter every model here uses, carrying every attribute any flank reads. */
    private static final String ATTRIBUTES = String.join(",", attribuut("a", pos(2)), attribuut("inkomen", pos(2)),
            attribuut("deler", pos(2)), attribuut("tweede", pos(2)), attribuut("leegding", pos(2)),
            attribuut("saldo", num(2)), attribuut("nul", pos(2)), attribuut("kindertal", WHOLE_POS),
            attribuut("uit", num(4)), attribuut("toeslag", num(4)));

    private static String attribuut(String naam, String datatype) {
        return "{\"node\":\"Attribute\",\"name\":\"%s\",\"datatype\":%s}".formatted(naam, datatype);
    }

    /** A one-objecttype document over {@code Jutter}; {@code parameters} and {@code prioritizedDefaults} are
     *  spliced verbatim (empty for the plain shapes). */
    private static String model(String[] rules, String parameters, String prioritizedDefaults) {
        return ("{\"format\":\"uchar-ir-v1\",\"model\":{"
                + "\"objectTypes\":[{\"node\":\"ObjectType\",\"name\":\"Jutter\",\"animate\":true,\"plural\":\"Jutters\","
                + "\"attributes\":[%s],\"characteristics\":[]}],"
                + "\"factTypes\":[],\"parameters\":[%s],\"domains\":[],\"unitSystems\":[],\"dimensions\":[],"
                + "\"timelines\":[],\"dayKinds\":[]},"
                + "\"rules\":{\"rules\":[%s],"
                + "\"dayKindDefinitions\":[],\"objectCreations\":[],\"allocations\":[],\"consistencies\":[],"
                + "\"uniquenesses\":[],\"factCreations\":[],\"startPointDeterminations\":[],"
                + "\"ruleGroups\":[]%s},\"diagnostics\":[]}")
                .formatted(ATTRIBUTES, parameters, String.join(",", rules), prioritizedDefaults);
    }

    /** {@link #model} with the kenmerk {@code vermist} declared (BIJVOEGLIJK) — the §9.2 forcing idiom of (r). */
    private static String modelK(String[] rules) {
        return model(rules, "", "")
                .replace("\"characteristics\":[]",
                        "\"characteristics\":[{\"node\":\"Characteristic\",\"name\":\"vermist\",\"kind\":\"adjectival\"}]");
    }

    /** "uit van een jutter" over the given value. */
    private static String ofModel(String waarde) { return ofModel(waarde, ""); }
    private static String ofModel(String waarde, String parameters) {
        return model(new String[]{regel("uit van een jutter", attr("uit", num(4)), waarde)}, parameters, "");
    }

    // ---------------- the models (each ref-replayed before its values were pinned) ----------------

    /** "het inkomen gedeeld door de tweede" — the §6.5 site every Let-shaped model here wraps. */
    private static final String DIVISION = binary("dividedBy", attr("inkomen"), attr("tweede"), num(4));
    private static final String DIVISION_DIVISOR = binary("dividedBy", attr("inkomen"), attr("deler"), num(4));
    private static final String DIVISION_ABS = binary("dividedByAbs", attr("inkomen"), attr("tweede"), num(4));
    private static final String DIVISION_LEEGDING = binary("dividedBy", attr("leegding"), attr("tweede"), num(4));
    private static final String DIVISION_ZERO = binary("dividedBy", attr("inkomen"), attr("nul"), num(4));
    /** (a'') "5,00 gedeeld door het leegding" — a LITERAL dividend, so companionOperand refuses it and the site
     *  qualifies for no companion of its own, bare or under a carrier. */
    private static final String DIVISION_LITERAL = binary("dividedBy", lit("5.00", pos(2)), attr("leegding"), num(4));
    private static final String SEVEN = lit("7.0000", num(4));
    private static final String SQUARE_ROOT = squareRoot(attr("saldo", num(2)), num(4));

    /** (a) the bare division. */
    private static String divisionModel() { return ofModel(DIVISION); }

    /** (a) the spiegelflank: the same division as the BODY of a Let whose binder the body never reads. */
    private static String spiegelModel() { return ofModel(let("d", attr("deler"), DIVISION, num(4))); }

    /** (b) "Let d = deler in d plus (inkomen gedeeld door tweede)" ({@code readFirst}) or its mirror with the
     *  division on the left. */
    private static String barrierModel(boolean readFirst) {
        String body = readFirst
                ? binary("plus", bref("d", pos(2)), DIVISION, num(4))
                : binary("plus", DIVISION, bref("d", pos(2)), num(4));
        return ofModel(let("d", attr("deler"), body, num(4)));
    }

    /** (c)/(f) a ch. 10.2 choice over "a groter dan 100" with the §6.5 division (or the §6.6 wortel) in the DAN
     *  branch — the IfThenElse bare as the rule value, or in a Let body whose binder feeds the condition. */
    private static String choiceModel(boolean inLetBody, boolean withSquareRoot) {
        String then = withSquareRoot ? SQUARE_ROOT : DIVISION_DIVISOR;
        String left = inLetBody ? bref("a", pos(2)) : attr("a");
        String keuze = ite(verg("greater", left, lit("100", WHOLE_POS)), then, SEVEN, num(4));
        return ofModel(inLetBody ? let("a", attr("a"), keuze, num(4)) : keuze);
    }

    /** (d) the division as a Let VALUE, read ({@code q plus 1}) or not ({@code 42}). */
    private static String valueModel(boolean read) {
        String body = read ? binary("plus", bref("q", num(4)), lit("1.0000", num(4)), num(4)) : lit("42.0000", num(4));
        return ofModel(let("q", DIVISION, body, num(4)));
    }

    /** (d) the division as a Let VALUE read only in the DAN branch of a choice on another binder. */
    private static String oneArmModel() {
        String keuze = ite(verg("greater", bref("k", pos(2)), lit("100", WHOLE_POS)), bref("q", num(4)), SEVEN, num(4));
        return ofModel(let("k", attr("a"), let("q", DIVISION, keuze, num(4)), num(4)));
    }

    /** (e) "Let x = leegding in Let q = inkomen gedeeld door tweede in x plus q" ({@code foreignFirst}) or "q plus x". */
    private static String foreignBarrierModel(boolean foreignFirst) {
        String body = foreignFirst
                ? binary("plus", bref("x", pos(2)), bref("q", num(4)), num(4))
                : binary("plus", bref("q", num(4)), bref("x", pos(2)), num(4));
        return ofModel(let("x", attr("leegding"), let("q", DIVISION, body, num(4)), num(4)));
    }

    /** (f) "de wortel van het saldo", bare or as the body of a Let whose binder is never read. */
    private static String squareRootModel(boolean inLetBody) {
        return ofModel(inLetBody ? let("d", attr("deler"), SQUARE_ROOT, num(4)) : SQUARE_ROOT);
    }

    /** (a') the leeg dividend as the BODY of a Let whose binder is never read. */
    private static String emptyDividendModel() { return ofModel(let("d", attr("deler"), DIVISION_LEEGDING, num(4))); }

    /** (i) "Let d = deler in begrensd(inkomen, …)" with the read of d in one bound and the §6.5 division in the
     *  other — {@code readInMinimum} puts the read in the minimum and the division in the maximum. The clamp and the
     *  Let carry the type of the base, 2 decimals, which the verifier demands (WF-091). */
    private static String clampModel(boolean readInMinimum) {
        String binderRead = bref("d", pos(2));
        String body = readInMinimum ? clamped(attr("inkomen"), binderRead, DIVISION, num(2))
                : clamped(attr("inkomen"), DIVISION, binderRead, num(2));
        return ofModel(let("d", attr("deler"), body, num(2)));
    }

    /** (i') the same carrier with the read and the §6.5 division in the BASIS / MAXIMUM pair instead. */
    private static String clampBaseModel(boolean divisionInBase) {
        String binderRead = bref("d", pos(2));
        String zero = lit("0.0000", num(4));
        String body = divisionInBase ? clamped(DIVISION, zero, binderRead) : clamped(binderRead, zero, DIVISION);
        return ofModel(let("d", attr("deler"), body, num(4)));
    }

    /** (a'') "Let d = deler in ((inkomen ÷ tweede) plus (5,00 ÷ leegding))" — the second site's dividend is a
     *  LITERAL, so it qualifies for no companion of its own; it still runs and reports to the probe. */
    private static String coSiteModel() {
        return ofModel(let("d", attr("deler"), binary("plus", DIVISION, DIVISION_LITERAL, num(4)), num(4)));
    }

    /** (a'') the same two sites behind the read of {@code d}, so a barrier can cut both off. */
    private static String coSiteBehindBarrierModel() {
        String right = binary("plus", DIVISION, DIVISION_LITERAL, num(4));
        return ofModel(let("d", attr("deler"), binary("plus", bref("d", pos(2)), right, num(4)), num(4)));
    }

    /** (e') "Let x = leegding in ((Let q = 1,0000 in x plus q) plus …)" — the read of the OUTER binder sits
     *  inside the INNER Let, whose handler must let that foreign barrier through. */
    private static String foreignBarrierPassThroughModel(boolean withDivision) {
        String inside = let("q", lit("1.0000", num(4)),
                binary("plus", bref("x", pos(2)), bref("q", num(4)), num(4)), num(4));
        String right = withDivision ? DIVISION : lit("5.0000", num(4));
        return ofModel(let("x", attr("leegding"), binary("plus", inside, right, num(4)), num(4)));
    }

    /** (i) the same, with the division by NUMERIC zero ("inkomen gedeeld door nul") as the other bound. */
    private static String clampZeroModel(boolean readInMinimum) {
        String binderRead = bref("d", pos(2));
        String body = readInMinimum ? clamped(attr("inkomen"), binderRead, DIVISION_ZERO, num(2))
                : clamped(attr("inkomen"), DIVISION_ZERO, binderRead, num(2));
        return ofModel(let("d", attr("deler"), body, num(2)));
    }

    /** (i) a ch. 10.2 guard "d groter dan (inkomen ÷ tweede)" ({@code readLeft}) or its mirror, in a Let on d. */
    private static String conditionOrderModel(boolean readLeft) {
        String binderRead = bref("d", pos(2));
        String condition = readLeft ? verg("greater", binderRead, DIVISION) : verg("greater", DIVISION, binderRead);
        String keuze = ite(condition, lit("1.0000", num(4)), lit("2.0000", num(4)), num(4));
        return ofModel(let("d", attr("deler"), keuze, num(4)));
    }

    /** (r) `deler` produced by a rule that §9.2 forces to fire AFTER the consumer's companion: its voorwaarde reads
     *  the negated kenmerk {@code vermist}, whose producer (never true for the case input) exists. The consumer is
     *  the (b) barrier body ("d plus (inkomen ÷ tweede)") or, with {@code branch}, the (c) choice on d. */
    private static String lateProducerModel(boolean branch) {
        String vermistTarget = ("{\"node\":\"AttributeReference\",\"attribute\":\"vermist\",\"subjectType\":\"Jutter\","
                + "\"source\":{\"snippet\":\"Een Jutter is vermist\"}}");
        String trueLiteral = "{\"node\":\"Literal\",\"value\":{\"boolean\":true},\"type\":{\"node\":\"Boolean\"},\"source\":{\"snippet\":\"waar\"}}";
        String notVermist = ("{\"node\":\"CharacteristicCheck\",\"characteristic\":\"vermist\",\"negated\":true,"
                + "\"source\":{\"snippet\":\"hij niet vermist is\"}}");
        String body = branch
                ? ite(verg("greater", bref("d", pos(2)), lit("1", WHOLE_POS)), DIVISION, SEVEN, num(4))
                : binary("plus", bref("d", pos(2)), DIVISION, num(4));
        return modelK(new String[]{
                        regel("vermissing van een jutter", "KENMERKTOEKENNING", vermistTarget, trueLiteral,
                                verg("greater", attr("inkomen"), lit("999999.00", pos(2)))),
                        regel("deler van een jutter", "BEREKENING", attr("deler"), lit("2.00", pos(2)), notVermist),
                        regel("uit van een jutter", attr("uit", num(4)), let("d", attr("deler"), body, num(4)))});
    }

    /** (j) "Let n = inkomen in n gedeeld door tweede" — the binder is the DIVIDEND. */
    private static String binderDividendModel() {
        return ofModel(let("n", attr("inkomen"), binary("dividedBy", bref("n", pos(2)), attr("tweede"), num(4)), num(4)));
    }

    /** (j) "Let d = deler in inkomen gedeeld door d" — the binder is the DIVISOR. */
    private static String binderDivisorModel() {
        return ofModel(let("d", attr("deler"), binary("dividedBy", attr("inkomen"), bref("d", pos(2)), num(4)), num(4)));
    }

    /** (j) "Let s = saldo in de wortel van s" — the binder is the RADICAND. */
    private static String binderRadicandModel() {
        return ofModel(let("s", attr("saldo", num(2)), squareRoot(bref("s", num(2)), num(4)), num(4)));
    }

    /** (h) a bare division plus a Let-wrapped one behind a read. */
    private static String twoDivisionsModel() {
        String wrapped = let("d", attr("leegding"), binary("plus", bref("d", pos(2)), DIVISION, num(4)), num(4));
        return ofModel(binary("plus", DIVISION_DIVISOR, wrapped, num(4)));
    }

    /** (p) a Parameter read inside the carrier, beside the division. */
    private static String parameterModel() {
        String body = binary("plus", DIVISION, param("grondslag", WHOLE_POS), num(4));
        return ofModel(let("d", attr("deler"), body, num(4)),
                "{\"node\":\"Parameter\",\"name\":\"grondslag\",\"datatype\":%s}"
                        .formatted(WHOLE_POS));
    }

    /** (g) a ch. 7 group over {@code Jutter.toeslag}: a justified first default with the given consequence, then
     *  an unconditional 0. */
    private static String defaultsModel(String consequentie) {
        String group = ("{\"node\":\"PrioritizedDefaults\",\"name\":\"toeslag\",\"subject\":\"Jutter\","
                + "\"attribute\":\"toeslag\",\"defaults\":["
                + "{\"node\":\"Default\",\"justification\":%s,\"consequence\":%s,\"source\":{\"snippet\":\"toeslag\"}},"
                + "{\"node\":\"Default\",\"justification\":%s,\"consequence\":%s,\"source\":{\"snippet\":\"toeslag\"}}],"
                + "\"source\":{\"snippet\":\"toeslag\"},\"validity\":{\"node\":\"Validity\"}}")
                .formatted(verg("greaterOrEqual", attr("kindertal", WHOLE_POS), lit("2", WHOLE_POS)),
                        consequentie, ALTIJD, lit("0.0000", num(4)));
        return model(new String[]{}, "", ",\"prioritizedDefaults\":[" + group + "]");
    }
}
