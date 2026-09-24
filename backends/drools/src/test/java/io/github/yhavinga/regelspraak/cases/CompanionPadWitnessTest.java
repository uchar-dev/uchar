package io.github.yhavinga.regelspraak.cases;

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
 * path that does. Witnessed via {@link CaseRunner#speelCase} over models authored HERE (no corpus file is
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
 * on one Begrensd flank while missing its mirror).
 *
 * <p><b>"Drager" / "carrier" below means the probe's HOST</b> — the OUTERMOST ch. 4.10 node on the path from
 * the value root down to the site, which the companion's condition re-evaluates
 * ({@code ExecModelTranspiler.Plek.drager}). It does NOT mean the {@code Expr.Onopgelost} carrier the loader
 * once built for an {@code IfThenElse}: since the union card (2026-09-10) all three cluster nodes are
 * ordinary {@code Expr} union nodes and nothing rides beside them.
 */
class CompanionPadWitnessTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    // ---------------- (a) the spiegelflank: a Let body always runs ----------------

    @Test void een_deling_in_een_draaiende_letbody_met_ongelezen_binder_is_inconsistent() throws Exception {
        // "uit := Let d = deler in inkomen gedeeld door tweede" — `d` is never read, so ch. 10.6 clause 3 never
        // evaluates `deler` and the body ALWAYS runs: the division by the absent `tweede` is real, whatever
        // `deler` holds. ref, both cases: uit leeg, INCONSISTENT. The bare twin (no Let) says the same.
        String verwacht = leeg("uit") + ", " + inconsistent();
        assertNull(speel(delingModel(), jutter(INKOMEN, verwacht)), "de kale deling door leeg");
        assertNull(speel(spiegelModel(), jutter(INKOMEN, verwacht)), "dezelfde deling in een Let-body, deler leeg");
        assertNull(speel(spiegelModel(), jutter(INKOMEN + ", " + n("deler", "2.00"), verwacht)),
                "dezelfde deling in een Let-body, deler gevuld");
    }

    // ---------------- (b) the barrier that IS passed first, and the one that is not ----------------

    @Test void een_lezing_die_aan_de_deling_voorafgaat_kapt_via_de_barriere_de_fout_af() throws Exception {
        // "uit := Let d = deler in d plus (inkomen gedeeld door tweede)" — the read of `d` comes FIRST (left
        // operand). With `deler` absent that read raises the barrier, the Let ends, and the division never
        // happens: ref uit leeg, CONSISTENT. With `deler` = 2,00 the read succeeds, the division runs and
        // faults (leeg, coalesced to 0 by the plus): ref uit 2.0000, INCONSISTENT.
        assertNull(speel(barriereModel(true), jutter(INKOMEN, leeg("uit") + ", " + consistent())),
                "lezing eerst, barriere: geen deling, geen fout");
        assertNull(speel(barriereModel(true), jutter(INKOMEN + ", " + n("deler", "2.00"),
                        waarde("uit", "2.0000") + ", " + inconsistent())),
                "lezing eerst, gevuld: de deling draait en faalt");
    }

    @Test void dezelfde_deling_voor_de_lezing_deponeert_de_fout_wel() throws Exception {
        // the mirror: "(inkomen gedeeld door tweede) plus d" — the division is evaluated BEFORE the read, so
        // its fault is deposited whatever the read then does. ref: deler absent -> leeg, INCONSISTENT (the
        // barrier still empties the Let, but after the fault); deler 2,00 -> 2.0000, INCONSISTENT.
        assertNull(speel(barriereModel(false), jutter(INKOMEN, leeg("uit") + ", " + inconsistent())),
                "deling eerst, dan de barriere: de fout staat");
        assertNull(speel(barriereModel(false), jutter(INKOMEN + ", " + n("deler", "2.00"),
                        waarde("uit", "2.0000") + ", " + inconsistent())),
                "deling eerst, gevuld");
    }

    // ---------------- (c) ch. 10.2: the branch taken, and the branch not taken ----------------

    @Test void een_deling_in_de_genomen_tak_is_inconsistent_en_in_de_niet_genomen_niet() throws Exception {
        // "als a groter dan 100 dan inkomen gedeeld door deler anders 7,0000", `deler` absent. a = 200 takes
        // the DAN branch: the division runs -> ref uit leeg, INCONSISTENT. a = 1 takes ANDERS: ref 7.0000,
        // CONSISTENT. Both orientations, bare (the IfThenElse as the rule value) and in a Let body whose binder
        // feeds the condition (the WF-077 shape).
        String genomen = leeg("uit") + ", " + inconsistent();
        String nietGenomen = waarde("uit", "7.0000") + ", " + consistent();
        for (boolean inLet : new boolean[]{false, true}) {
            String vorm = inLet ? " (in een Let-body)" : " (kaal)";
            assertNull(speel(keuzeModel(inLet, false), jutter(INKOMEN + ", " + n("a", "200.00"), genomen)),
                    "de genomen tak" + vorm);
            assertNull(speel(keuzeModel(inLet, false), jutter(INKOMEN + ", " + n("a", "1.00"), nietGenomen)),
                    "de niet-genomen tak" + vorm);
        }
    }

    // ---------------- (d) ch. 10.6: a division in the VALUE runs only when a read forces it ----------------

    @Test void een_deling_in_een_letvalue_faalt_alleen_als_de_binder_gelezen_wordt() throws Exception {
        // "Let q = inkomen gedeeld door tweede in q plus 1,0000" — the read forces the need: the division runs,
        // faults (leeg) and the barrier then empties the Let. ref: leeg, INCONSISTENT.
        // "Let q = inkomen gedeeld door tweede in 42,0000" — nothing reads q, nothing runs. ref: 42.0000, CONSISTENT.
        assertNull(speel(valueModel(true), jutter(INKOMEN, leeg("uit") + ", " + inconsistent())), "gelezen");
        assertNull(speel(valueModel(false), jutter(INKOMEN, waarde("uit", "42.0000") + ", " + consistent())), "ongelezen");
    }

    @Test void een_letvalue_gelezen_in_slechts_een_tak_faalt_per_tak() throws Exception {
        // "Let k = a in Let q = inkomen gedeeld door tweede in (als k groter dan 100 dan q anders 7,0000)":
        // q is read only in the DAN branch. a = 200 -> the read forces the faulting division: ref leeg,
        // INCONSISTENT. a = 1 -> ANDERS, q never read: ref 7.0000, CONSISTENT. (The card before this one could
        // certify neither — "read on some but not every path" got no companion at all.)
        assertNull(speel(eenTakModel(), jutter(INKOMEN + ", " + n("a", "200.00"), leeg("uit") + ", " + inconsistent())),
                "gelezen op de genomen tak");
        assertNull(speel(eenTakModel(), jutter(INKOMEN + ", " + n("a", "1.00"), waarde("uit", "7.0000") + ", " + consistent())),
                "ongelezen op de niet-genomen tak");
    }

    // ---------------- (e) a FOREIGN barrier: another Let's read, before or after the forcing read ----------------

    @Test void een_lezing_van_een_andere_lege_binder_voor_de_deling_kapt_af_erna_niet() throws Exception {
        // "Let x = leegding in Let q = inkomen gedeeld door tweede in x plus q", `leegding` absent: the read of
        // x raises the OUTER Let's barrier before q is ever forced — ref leeg, CONSISTENT. Mirror "q plus x":
        // q is forced first (fault), then x ends the outer Let — ref leeg, INCONSISTENT. The barrier belongs to
        // the Let that bound the binder that was read, and the order of the two reads decides.
        assertNull(speel(vreemdeBarriereModel(true), jutter(INKOMEN, leeg("uit") + ", " + consistent())),
                "de vreemde lezing eerst");
        assertNull(speel(vreemdeBarriereModel(false), jutter(INKOMEN, leeg("uit") + ", " + inconsistent())),
                "de vreemde lezing erna");
    }

    // ---------------- (f) the §6.6 wortel twins ----------------

    @Test void een_wortel_in_een_draaiende_letbody_is_even_inconsistent_als_de_kale() throws Exception {
        // "Let d = deler in de wortel van het saldo", saldo = -4,00, binder unread: the body runs, the negative
        // radicand degrades to leeg AND deposits the §6.6 Fout. ref on both models: uit leeg, INCONSISTENT.
        String verwacht = leeg("uit") + ", " + inconsistent();
        String invoer = n("saldo", "-4.00");
        assertNull(speel(wortelModel(false), jutter(invoer, verwacht)), "de kale wortel van negatief");
        assertNull(speel(wortelModel(true), jutter(invoer, verwacht)), "dezelfde wortel in een Let-body");
    }

    @Test void een_wortel_in_de_genomen_tak_is_inconsistent_en_in_de_niet_genomen_niet() throws Exception {
        // "Let a = a in (als a groter dan 100 dan de wortel van het saldo anders 7,0000)", saldo = -4,00.
        // ref: a = 200 -> leeg, INCONSISTENT; a = 1 -> 7.0000, CONSISTENT.
        assertNull(speel(keuzeModel(true, true), jutter(n("saldo", "-4.00") + ", " + n("a", "200.00"),
                leeg("uit") + ", " + inconsistent())), "de genomen tak");
        assertNull(speel(keuzeModel(true, true), jutter(n("saldo", "-4.00") + ", " + n("a", "1.00"),
                waarde("uit", "7.0000") + ", " + consistent())), "de niet-genomen tak");
    }

    // ---------------- (a'') a second, NON-qualifying site under the same carrier ----------------

    @Test void een_niet_kwalificerende_plek_onder_dezelfde_drager_meldt_zijn_fout_ook() throws Exception {
        // "Let d = deler in ((inkomen ÷ tweede) plus (5,00 ÷ leegding))", inkomen 10,00 and tweede 2,00 present,
        // `leegding` absent. Site 1 qualifies (two attributes) and does NOT fault; site 2 has a LITERAL dividend,
        // so no static predicate can guard it and it carries no companion of its own — but it RUNS and faults.
        // ref: uit 5.0000, INCONSISTENT. The probe sees that fault (Sonde.meld sits in the division arm, which is
        // operand-blind), so the carrier's companion reports on "a §6.5/§6.6 site under this carrier ran and
        // faulted", not on "THIS site did": a per-site identity gate throws the observation away and answers a
        // silent consistent. Twin: with `leegding` = 5,00 nothing faults -> ref 6.0000, CONSISTENT.
        assertNull(speel(medePlekModel(), jutter(INKOMEN + ", " + n("tweede", "2.00"),
                        waarde("uit", "5.0000") + ", " + inconsistent())),
                "de literal-deling onder dezelfde drager faalt: de sonde ziet haar");
        assertNull(speel(medePlekModel(), jutter(INKOMEN + ", " + n("tweede", "2.00") + ", " + n("leegding", "5.00"),
                        waarde("uit", "6.0000") + ", " + consistent())),
                "niets faalt: de verbrede poort mag niet vuren");
    }

    @Test void de_verbrede_poort_vuurt_niet_als_de_barriere_beide_plekken_afkapt() throws Exception {
        // the fabricage twin of (a''): the same two sites behind a read of `d`,
        // "Let d = deler in (d plus ((inkomen ÷ tweede) plus (5,00 ÷ leegding)))". `deler` absent -> the read
        // raises the barrier before either site runs: ref uit leeg, CONSISTENT — nothing reports, nothing fires.
        // `deler` = 2,00 -> both sites run, only the non-qualifying one faults: ref uit 7.0000 (2 + 5 + leeg-as-0),
        // INCONSISTENT.
        assertNull(speel(medePlekAchterBarriereModel(), jutter(INKOMEN + ", " + n("tweede", "2.00"),
                        leeg("uit") + ", " + consistent())),
                "de barriere kapt beide plekken af");
        assertNull(speel(medePlekAchterBarriereModel(), jutter(INKOMEN + ", " + n("tweede", "2.00") + ", " + n("deler", "2.00"),
                        waarde("uit", "7.0000") + ", " + inconsistent())),
                "het pad loopt door: de niet-kwalificerende plek faalt");
    }

    // ---------------- (e') a nested Let must pass a FOREIGN barrier through ----------------

    @Test void een_binnenste_let_laat_de_vreemde_barriere_door_naar_de_buitenste() throws Exception {
        // "Let x = leegding in ((Let q = 1,0000 in x plus q) plus (inkomen ÷ tweede))", `leegding` and `tweede`
        // absent. The read of x sits INSIDE the inner Let but binds to the OUTER one, so ch. 10.6 clause 2 ends
        // the OUTER Let: the inner handler must RETHROW a barrier that is not its own instance. Then the division
        // to its right never runs. ref: uit leeg, CONSISTENT. An inner handler that swallows every barrier would
        // answer leeg for the inner Let and go on to the division — a fabricated fault.
        assertNull(speel(vreemdeBarriereDoorgifteModel(true), jutter(INKOMEN, leeg("uit") + ", " + consistent())),
                "de sonde-kant: de doorgegeven barriere kapt de deling af");
        // the value-side twin (no §6.5 site at all): swallowing the barrier would let the outer body finish at
        // 1 + 5 = 6.0000 instead of emptying the Let. ref: uit leeg. (This flank belongs beside the ch. 10.6
        // barrier witnesses of LetClusterWitnessTest — that file is out of this card's mandate, see the card.)
        assertNull(speel(vreemdeBarriereDoorgifteModel(false), jutter(INKOMEN, leeg("uit") + ", " + consistent())),
                "de waarde-kant: de buitenste Let wordt leeg, niet 6.0000");
    }

    // ---------------- (h) two sites in one rule value: each carries its OWN path ----------------

    @Test void twee_delingen_in_een_regelwaarde_krijgen_elk_hun_eigen_padconditie() throws Exception {
        // "(inkomen gedeeld door deler) plus (Let d = leegding in d plus (inkomen gedeeld door tweede))" — the
        // first division is bare (runs whenever the rule does), the second sits behind the read of `d`. Two
        // companions, indexed 1 and 2 (the DRL naming parity), each with its own condition. ref:
        //   everything absent      -> 0.0000, INCONSISTENT (site 1 faults; site 2 is cut off by the barrier)
        //   deler 2, leegding 1    -> 6.0000, INCONSISTENT (site 1 = 5; d read fine, site 2 faults -> 1 + 0)
        //   deler 2, leegding leeg -> 5.0000, CONSISTENT   (site 1 = 5; the barrier empties the Let; no fault)
        assertNull(speel(tweeDelingenModel(), jutter(INKOMEN, waarde("uit", "0.0000") + ", " + inconsistent())),
                "beide operanden leeg: de kale deling faalt, de omhulde is afgekapt");
        assertNull(speel(tweeDelingenModel(), jutter(INKOMEN + ", " + n("deler", "2.00") + ", " + n("leegding", "1.00"),
                waarde("uit", "6.0000") + ", " + inconsistent())), "alleen de omhulde deling faalt");
        assertNull(speel(tweeDelingenModel(), jutter(INKOMEN + ", " + n("deler", "2.00"),
                waarde("uit", "5.0000") + ", " + consistent())), "de omhulde deling is afgekapt, de kale rekent");
    }

    // ---------------- (p) a Parameter inside the carrier: the companion binds what the path reads ----------------

    @Test void een_parameter_in_de_drager_wordt_voor_de_padconditie_gebonden() throws Exception {
        // "Let d = deler in (inkomen gedeeld door tweede) plus GRONDSLAG", grondslag = 3 (a Parameter global).
        // The path condition evaluates the carrier, so it must bind the same globals the host binds. ref:
        // tweede absent -> 3.0000, INCONSISTENT; tweede 2,00 -> 8.0000, CONSISTENT.
        assertNull(speel(parameterModel(), jutter(INKOMEN, waarde("uit", "3.0000") + ", " + inconsistent(), "\"grondslag\": { \"number\": \"3\" }")),
                "de deling faalt, de parameter telt op");
        assertNull(speel(parameterModel(), jutter(INKOMEN + ", " + n("tweede", "2.00"), waarde("uit", "8.0000") + ", " + consistent(), "\"grondslag\": { \"number\": \"3\" }")),
                "de deling rekent");
    }

    // ---------------- (g) the defaults consequence keeps its fence ----------------

    @Test void het_defaults_delingshek_blijft_staan_voor_een_deling_in_een_letbody() throws Exception {
        // A ch. 7 default whose consequence is "Let d = deler in inkomen gedeeld door tweede". ref on this
        // model+case (tweede absent, kindertal 3): toeslag 0.0000, CONSISTENT — the faulting default falls
        // through and ch. 7.1 discards its speculative channel deposit. That value this backend deliberately
        // does not produce: the division fence refuses the group, and the path-carrying companions of this
        // card are RULE machinery that must not start playing a defaults consequence. The honest outcome is
        // still the refusal, naming the fence.
        CaseRunner.Excluded weigering = assertThrows(CaseRunner.Excluded.class,
                () -> speel(defaultsModel(let("d", attr("deler"), DELING, num(4))), jutter(INKOMEN + ", " + n("kindertal", "3"),
                        waarde("toeslag", "0.0000") + ", " + consistent())));
        assertTrue(weigering.getMessage().contains("deling"),
                "de weigering noemt het delingshek: " + weigering.getMessage());
    }

    // ---------------- (a') the §6.5 predicate's edges under a carrier: the ABS variant, the leeg dividend ----------------

    @Test void een_abs_deling_in_een_draaiende_letbody_is_even_inconsistent_als_de_kale() throws Exception {
        // "gedeeld door (ABS)" is the second §6.5 variant and lowers through its own arm; under a carrier its
        // fault must reach the probe exactly like the plain division's. ref, both models (tweede absent):
        // uit leeg, INCONSISTENT.
        String verwacht = leeg("uit") + ", " + inconsistent();
        assertNull(speel(uitModel(DELING_ABS), jutter(INKOMEN, verwacht)), "de kale ABS-deling door leeg");
        assertNull(speel(uitModel(let("d", attr("deler"), DELING_ABS, num(4))), jutter(INKOMEN, verwacht)),
                "dezelfde ABS-deling in een Let-body");
    }

    @Test void een_leeg_dividend_onder_een_drager_is_nul_zonder_fout() throws Exception {
        // §6.5 / ch. 1.2: the LEFT operand is tested first — leeg ÷ x coalesces to 0 and deposits nothing, even
        // when the divisor is leeg too. The probe must keep that guard: a report on "divisor leeg" alone would
        // fabricate. ref: "Let d = deler in leegding gedeeld door tweede", both absent -> 0.0000, CONSISTENT;
        // tweede 2,00 -> 0.0000, CONSISTENT; the bare twin -> 0.0000, CONSISTENT.
        String verwacht = waarde("uit", "0.0000") + ", " + consistent();
        assertNull(speel(uitModel(DELING_LEEGDING), jutter(INKOMEN, verwacht)), "de kale deling met leeg dividend");
        assertNull(speel(leegDividendModel(), jutter(INKOMEN, verwacht)), "leeg dividend, lege deler, in een Let-body");
        assertNull(speel(leegDividendModel(), jutter(INKOMEN + ", " + n("tweede", "2.00"), verwacht)),
                "leeg dividend, gevulde deler, in een Let-body");
    }

    // ---------------- (f') the §6.6 predicate's edge under a carrier: the zero radicand ----------------

    @Test void een_wortel_van_nul_onder_een_drager_is_nul_zonder_fout() throws Exception {
        // §6.6 faults on a NEGATIVE radicand only; zero is a real root. The probe must not widen the predicate.
        // ref, saldo = 0,00: bare -> 0.0000, CONSISTENT; in a Let body -> 0.0000, CONSISTENT.
        String verwacht = waarde("uit", "0.0000") + ", " + consistent();
        String invoer = n("saldo", "0.00");
        assertNull(speel(wortelModel(false), jutter(invoer, verwacht)), "de kale wortel van nul");
        assertNull(speel(wortelModel(true), jutter(invoer, verwacht)), "dezelfde wortel in een Let-body");
    }

    // ---------------- (i) evaluation ORDER inside a non-carrier form: Begrensd, maximum before minimum ----------------

    @Test void de_grenzen_van_een_begrenzing_evalueren_maximum_voor_minimum() throws Exception {
        // ref evaluates a Begrensd as basis, then MAXIMUM, then minimum (ref/lib/eval.ml, Begrensd arm —
        // measured 2026-09-02). With a barrier read in one bound and a division in the other, that order decides
        // whether the division runs. "Let d = deler in begrensd(inkomen, minimum d, maximum inkomen ÷ tweede)",
        // deler and tweede absent: the maximum's division runs and faults, THEN the minimum's read ends the
        // Let -> ref uit leeg, INCONSISTENT. Mirror (minimum the division, maximum the read): the read ends the
        // Let first and the division never runs -> ref uit leeg, CONSISTENT.
        assertNull(speel(begrensdModel(true), jutter(INKOMEN, leeg("uit") + ", " + inconsistent())),
                "lezing in het minimum, deling in het maximum: de deling draait eerst");
        assertNull(speel(begrensdModel(false), jutter(INKOMEN, leeg("uit") + ", " + consistent())),
                "deling in het minimum, lezing in het maximum: de barriere komt eerst");
    }

    @Test void dezelfde_grensvolgorde_beslist_tussen_harde_fout_en_barriere() throws Exception {
        // the value-side twin: a division by NUMERIC zero (a hard fault, not a channel deposit) in one bound,
        // the barrier read in the other, nul = 0,00. "… minimum d, maximum inkomen ÷ nul": ref evaluates the
        // maximum first and dies — EVALUATION FAULT "division by numeric zero" — mirrored here by the runtime's
        // own ArithmeticException out of fire. Mirror "… minimum inkomen ÷ nul, maximum d": the maximum's read
        // ends the Let first and the zero division never runs -> ref uit leeg, CONSISTENT.
        String invoer = INKOMEN + ", " + n("nul", "0.00");
        RuntimeException fout = assertThrows(RuntimeException.class,
                () -> speel(begrensdNulModel(true), jutter(invoer, leeg("uit") + ", " + consistent())));
        assertTrue(oorzaakTekst(fout).contains("deling door 0"),
                "het maximum deelt door nul voordat het minimum de barriere opwerpt: " + oorzaakTekst(fout));
        assertNull(speel(begrensdNulModel(false), jutter(invoer, leeg("uit") + ", " + consistent())),
                "de lezing in het maximum kapt af voordat het minimum door nul deelt");
    }

    @Test void de_basis_van_een_begrenzing_evalueert_voor_het_maximum() throws Exception {
        // the third point of the same order: basis BEFORE maximum. (i) above pins only the PAIR minimum/maximum,
        // which an implementation evaluating maximum, basis, minimum still satisfies. "Let d = deler in
        // begrensd(basis inkomen ÷ tweede, minimum 0,0000, maximum d)", deler and tweede absent: the basis
        // division runs and faults FIRST, then the maximum's read ends the Let -> ref uit leeg, INCONSISTENT.
        // Mirror (basis the read, maximum the division): the basis read ends the Let before the maximum's
        // division runs -> ref uit leeg, CONSISTENT.
        assertNull(speel(begrensdBasisModel(true), jutter(INKOMEN, leeg("uit") + ", " + inconsistent())),
                "deling in de basis, lezing in het maximum: de deling draait eerst");
        assertNull(speel(begrensdBasisModel(false), jutter(INKOMEN, leeg("uit") + ", " + consistent())),
                "lezing in de basis, deling in het maximum: de barriere komt eerst");
    }

    @Test void de_operanden_van_een_keuzeconditie_evalueren_links_voor_rechts() throws Exception {
        // the ch. 10.2 guard's Vergelijking: ref evaluates links before rechts (ref/lib/eval.ml, eval_cond).
        // "Let d = deler in (als d groter dan (inkomen ÷ tweede) dan 1 anders 2)", deler and tweede absent: the
        // read of d is the LEFT operand, raises the barrier, the division never runs -> ref uit leeg,
        // CONSISTENT. Mirror "(inkomen ÷ tweede) groter dan d": the division runs and faults, then the read
        // ends the Let -> ref uit leeg, INCONSISTENT.
        assertNull(speel(conditieVolgordeModel(true), jutter(INKOMEN, leeg("uit") + ", " + consistent())),
                "lezing links, deling rechts");
        assertNull(speel(conditieVolgordeModel(false), jutter(INKOMEN, leeg("uit") + ", " + inconsistent())),
                "deling links, lezing rechts");
    }

    // ---------------- (r) class-reactive: the probe re-runs after a producer that fires AFTER the companion ----------------

    @Test void de_sonde_hersondeert_de_drager_na_een_producent_die_na_de_companion_vuurt() throws Exception {
        // `deler` is DERIVED by a rule that §9.2 stratification FORCES to fire after this companion (its
        // voorwaarde reads a negated kenmerk that has a producer -> salience -1; the LetClusterWitnessTest
        // idiom). At the companion's first activation `deler` is still leeg: the body's read of d raises the
        // barrier and the division is cut off — no fault yet. Then the producer writes deler = 2,00, the subject
        // is updated, and only a CLASS-REACTIVE companion pattern (the always-true freshness expr) re-probes:
        // now the read succeeds, the division by the absent `tweede` runs and faults. Without the freshness
        // expr the companion activates once and the fault is lost (verified by mutation).
        // ref: "Let d = deler in d plus (inkomen ÷ tweede)" -> uit 2.0000, INCONSISTENT;
        //      "Let d = deler in (als d groter dan 1 dan inkomen ÷ tweede anders 7)" -> uit leeg, INCONSISTENT.
        assertNull(speel(lateProducentModel(false), jutter(INKOMEN, waarde("uit", "2.0000") + ", " + inconsistent())),
                "de barriere-vorm: de late producent opent het pad naar de deling");
        assertNull(speel(lateProducentModel(true), jutter(INKOMEN, leeg("uit") + ", " + inconsistent())),
                "de tak-vorm: de late producent kiest de delende tak");
    }

    // ---------------- (g') the defaults fence covers the §6.6 wortel too ----------------

    @Test void het_defaults_hek_weigert_ook_een_wortel_in_de_consequentie() throws Exception {
        // the §6.6 sibling of the division fence: a default whose consequence is "wortel(saldo) plus 0,0000",
        // taken (kindertal 3 >= 2) with saldo = -4,00. ref: toeslag 0.0000 (leeg + 0), INCONSISTENT — the §6.6
        // deposit stands. This backend lowers no §9.5 channel for a defaults consequence, so playing the group
        // would answer CONSISTENT in silence; the honest outcome is the same loud refusal the division gets,
        // naming the wortel.
        CaseRunner.Excluded weigering = assertThrows(CaseRunner.Excluded.class,
                () -> speel(defaultsModel(binair("PLUS", WORTEL, lit("0.0000", num(4)), num(4))),
                        jutter(n("saldo", "-4.00") + ", " + n("kindertal", "3"),
                                waarde("toeslag", "0.0000") + ", " + inconsistent())));
        assertTrue(weigering.getMessage().contains("wortel"),
                "de weigering noemt de wortel: " + weigering.getMessage());
    }

    // ---------------- (j) a BINDER as the operand of a site under its Let: the variabelendeel shape ----------------

    @Test void een_binder_als_dividend_onder_zijn_let_krijgt_de_padconditie() throws Exception {
        // "Let n = inkomen in n gedeeld door tweede" — the shape the RegelSpraak emitter produces for a
        // "Daarbij geldt" variabelendeel. The dividend is a binder READ, not an attribute: there is no operand
        // to re-read for a static leeg/vol predicate, but under a carrier the probe observes the division itself,
        // so the site qualifies. ref: inkomen 10,00, tweede absent -> the read succeeds and the division faults:
        // uit leeg, INCONSISTENT; inkomen absent -> the read raises the barrier before the division: uit leeg,
        // CONSISTENT; inkomen 10,00, tweede 2,00 -> 5.0000, CONSISTENT. The bare twin of (a) is repeated first.
        assertNull(speel(delingModel(), jutter(INKOMEN, leeg("uit") + ", " + inconsistent())), "de kale deling door leeg");
        assertNull(speel(binderDividendModel(), jutter(INKOMEN, leeg("uit") + ", " + inconsistent())),
                "de binder als dividend, de deler leeg: de deling draait en faalt");
        assertNull(speel(binderDividendModel(), jutter("", leeg("uit") + ", " + consistent())),
                "de binder zelf leeg: de barriere komt voor de deling");
        assertNull(speel(binderDividendModel(), jutter(INKOMEN + ", " + n("tweede", "2.00"),
                        waarde("uit", "5.0000") + ", " + consistent())), "beide gevuld: geen fout");
    }

    @Test void een_binder_als_deler_onder_zijn_let_faalt_nooit_op_leeg() throws Exception {
        // "Let d = deler in inkomen gedeeld door d": a binder read never ANSWERS leeg — a leeg need raises the
        // barrier at the read (ch. 10.6 clause 2) — so the division never sees a leeg divisor and §6.5 has
        // nothing to deposit. The widened companion must not fabricate here. ref: deler absent -> uit leeg,
        // CONSISTENT; deler 2,00 -> 5.0000, CONSISTENT.
        assertNull(speel(binderDelerModel(), jutter(INKOMEN, leeg("uit") + ", " + consistent())),
                "de deler-binder leeg: de barriere, geen fout");
        assertNull(speel(binderDelerModel(), jutter(INKOMEN + ", " + n("deler", "2.00"),
                        waarde("uit", "5.0000") + ", " + consistent())), "gevuld: de deling rekent");
    }

    @Test void een_binder_als_radicand_onder_zijn_let_krijgt_de_padconditie() throws Exception {
        // the §6.6 sibling: "Let s = saldo in de wortel van s". ref: saldo -4,00 -> the read succeeds and the
        // negative radicand faults: uit leeg, INCONSISTENT; saldo absent -> the barrier: uit leeg, CONSISTENT.
        // The bare twin of (f) is repeated first.
        assertNull(speel(wortelModel(false), jutter(n("saldo", "-4.00"), leeg("uit") + ", " + inconsistent())),
                "de kale wortel van negatief");
        assertNull(speel(binderRadicandModel(), jutter(n("saldo", "-4.00"), leeg("uit") + ", " + inconsistent())),
                "de binder als radicand, negatief: de wortel draait en faalt");
        assertNull(speel(binderRadicandModel(), jutter("", leeg("uit") + ", " + consistent())),
                "de binder zelf leeg: de barriere komt voor de wortel");
    }

    // ---------------- helpers: replay ----------------

    /** One authored model + case through the FULL replay; the per-case failure line, null on a match. */
    private static String speel(String ir, String caseJson) throws Exception {
        Path tmp = Files.createTempFile("companion-pad-witness", ".json");
        try {
            Files.writeString(tmp, ir);
            return CaseRunner.speelCase(tmp, JSON.readTree(caseJson));
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    /** Every message down the cause chain (Drools wraps a consequence's exception). */
    private static String oorzaakTekst(Throwable t) {
        StringBuilder sb = new StringBuilder();
        for (Throwable c = t; c != null; c = c.getCause()) {
            sb.append(c.getClass().getSimpleName()).append(": ").append(c.getMessage()).append(" <- ");
            if (c.getCause() == c) break;
        }
        return sb.toString();
    }

    // ---------------- helpers: the case shapes (uchar-case-v0) ----------------

    private static final String INKOMEN = "\"inkomen\": { \"number\": \"10.00\" }";

    private static String n(String attribuut, String getal) {
        return "\"%s\": { \"number\": \"%s\" }".formatted(attribuut, getal);
    }

    private static String jutter(String attributen, String verwacht) { return jutter(attributen, verwacht, ""); }

    private static String jutter(String attributen, String verwacht, String parameters) {
        return """
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "J1", "objectType": "Jutter",
                        "attributes": { %s },
                        "roles": {}, "characteristics": [] } ],
                    "parameters": { %s } },
                  "expected": [ %s ] }
                """.formatted(attributen, parameters, verwacht);
    }

    private static String waarde(String attribuut, String getal) {
        return "{ \"kind\": \"attributeValue\", \"label\": \"J1\", \"attribute\": \"%s\", \"value\": { \"number\": \"%s\" } }"
                .formatted(attribuut, getal);
    }

    private static String leeg(String attribuut) {
        return "{ \"kind\": \"attributeEmpty\", \"label\": \"J1\", \"attribute\": \"%s\" }".formatted(attribuut);
    }

    private static String consistent() { return "{ \"kind\": \"consistent\" }"; }
    private static String inconsistent() { return "{ \"kind\": \"inconsistent\" }"; }

    // ---------------- helpers: the wire shapes (IR-SPEC ch. 4.10 and its neighbours) ----------------

    private static String num(int dec, boolean geheel, String teken) {
        return "{\"node\":\"Number\",\"whole\":%b,\"decimals\":%d,\"sign\":\"%s\"}"
                .formatted(geheel, dec, teken);
    }
    private static String num(int dec) { return num(dec, false, "ONBEPAALD"); }
    private static String pos(int dec) { return num(dec, false, "NIET_NEGATIEF"); }
    private static final String GEHEEL_POS = num(0, true, "NIET_NEGATIEF");

    private static String attr(String naam, String type) {
        return ("{\"node\":\"AttribuutRef\",\"attribuut\":\"%s\",\"objecttype\":\"Jutter\","
                + "\"source\":{\"snippet\":\"het %s van de Jutter\"}}")
                .formatted(naam, naam);
    }
    private static String attr(String naam) { return attr(naam, pos(2)); }

    private static String lit(String waarde, String type) {
        return IrPad.literal(waarde, type, waarde);
    }

    private static String param(String naam, String type) {
        return "{\"node\":\"ParameterRef\",\"parameter\":\"%s\",\"source\":{\"snippet\":\"de %s\"}}".formatted(naam, naam);
    }

    private static String binair(String op, String links, String rechts, String type) {
        return "{\"node\":\"Binair\",\"op\":\"%s\",\"links\":%s,\"rechts\":%s,\"type\":%s,\"source\":{\"snippet\":\"%s\"}}"
                .formatted(op, links, rechts, type, op.toLowerCase(java.util.Locale.ROOT));
    }

    private static String wortel(String argument, String type) {
        return "{\"node\":\"BuiltinCall\",\"kind\":\"WORTEL\",\"arguments\":[%s],\"type\":%s,\"source\":{\"snippet\":\"de wortel van\"}}"
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

    private static String ite(String conditie, String dan, String anders, String type) {
        return ("{\"node\":\"IfThenElse\",\"type\":%s,\"condition\":%s,\"then\":%s,\"else\":%s,"
                + "\"source\":{\"snippet\":\"als … dan … anders …\"}}").formatted(type, conditie, dan, anders);
    }

    private static String begrensd(String basis, String minimum, String maximum) {
        return "{\"node\":\"Begrensd\",\"basis\":%s,\"minimum\":%s,\"maximum\":%s,\"type\":%s,\"source\":{\"snippet\":\"begrensd\"}}"
                .formatted(basis, minimum, maximum, num(4));
    }

    private static String verg(String op, String links, String rechts) {
        return "{\"node\":\"Vergelijking\",\"op\":\"%s\",\"links\":%s,\"rechts\":%s,\"periode\":null,\"source\":{\"snippet\":\"%s\"}}"
                .formatted(op, links, rechts, op.toLowerCase(java.util.Locale.ROOT));
    }

    private static final String ALTIJD =
            "{\"node\":\"Samengesteld\",\"kwantor\":\"ALLE\",\"drempel\":0,\"onderdelen\":[],\"source\":{\"snippet\":\"altijd\"}}";

    private static String regel(String naam, String doel, String waarde) {
        return regel(naam, "BEREKENING", doel, waarde, "null");
    }

    private static String regel(String naam, String soort, String doel, String waarde, String voorwaarde) {
        return IrPad.regel(naam, soort, doel, waarde, voorwaarde);
    }

    /** The one Jutter every model here uses, carrying every attribute any flank reads. */
    private static final String ATTRIBUTEN = String.join(",", attribuut("a", pos(2)), attribuut("inkomen", pos(2)),
            attribuut("deler", pos(2)), attribuut("tweede", pos(2)), attribuut("leegding", pos(2)),
            attribuut("saldo", num(2)), attribuut("nul", pos(2)), attribuut("kindertal", GEHEEL_POS),
            attribuut("uit", num(4)), attribuut("toeslag", num(4)));

    private static String attribuut(String naam, String datatype) {
        return "{\"node\":\"Attribute\",\"name\":\"%s\",\"datatype\":%s}".formatted(naam, datatype);
    }

    /** A one-objecttype document over {@code Jutter}; {@code parameters} and {@code prioritizedDefaults} are
     *  spliced verbatim (empty for the plain shapes). */
    private static String model(String[] regels, String parameters, String prioritizedDefaults) {
        return ("{\"format\":\"uchar-ir-v1\",\"model\":{"
                + "\"objectTypes\":[{\"node\":\"ObjectType\",\"name\":\"Jutter\",\"animate\":true,\"plural\":\"Jutters\","
                + "\"attributes\":[%s],\"characteristics\":[]}],"
                + "\"factTypes\":[],\"parameters\":[%s],\"domains\":[],\"unitSystems\":[],\"dimensions\":[],"
                + "\"timelines\":[],\"dayKinds\":[]},"
                + "\"rules\":{\"resolvedRegels\":[%s],"
                + "\"resolvedDagsoorten\":[],\"objectCreaties\":[],\"verdelingen\":[],\"consistenties\":[],"
                + "\"uniciteiten\":[],\"feitCreaties\":[],\"resolvedStartpunten\":[],"
                + "\"regelGroepen\":[]%s},\"diagnostics\":[]}")
                .formatted(ATTRIBUTEN, parameters, String.join(",", regels), prioritizedDefaults);
    }

    /** {@link #model} with the kenmerk {@code vermist} declared (BIJVOEGLIJK) — the §9.2 forcing idiom of (r). */
    private static String modelK(String[] regels) {
        return model(regels, "", "")
                .replace("\"characteristics\":[]",
                        "\"characteristics\":[{\"node\":\"Characteristic\",\"name\":\"vermist\",\"kind\":\"BIJVOEGLIJK\"}]");
    }

    /** "uit van een jutter" over the given value. */
    private static String uitModel(String waarde) { return uitModel(waarde, ""); }
    private static String uitModel(String waarde, String parameters) {
        return model(new String[]{regel("uit van een jutter", attr("uit", num(4)), waarde)}, parameters, "");
    }

    // ---------------- the models (each ref-replayed before its values were pinned) ----------------

    /** "het inkomen gedeeld door de tweede" — the §6.5 site every Let-shaped model here wraps. */
    private static final String DELING = binair("GEDEELD_DOOR", attr("inkomen"), attr("tweede"), num(4));
    private static final String DELING_DELER = binair("GEDEELD_DOOR", attr("inkomen"), attr("deler"), num(4));
    private static final String DELING_ABS = binair("GEDEELD_DOOR_ABS", attr("inkomen"), attr("tweede"), num(4));
    private static final String DELING_LEEGDING = binair("GEDEELD_DOOR", attr("leegding"), attr("tweede"), num(4));
    private static final String DELING_NUL = binair("GEDEELD_DOOR", attr("inkomen"), attr("nul"), num(4));
    /** (a'') "5,00 gedeeld door het leegding" — a LITERAL dividend, so companionOperand refuses it and the site
     *  qualifies for no companion of its own, bare or under a carrier. */
    private static final String DELING_LITERAAL = binair("GEDEELD_DOOR", lit("5.00", pos(2)), attr("leegding"), num(4));
    private static final String ZEVEN = lit("7.0000", num(4));
    private static final String WORTEL = wortel(attr("saldo", num(2)), num(4));

    /** (a) the bare division. */
    private static String delingModel() { return uitModel(DELING); }

    /** (a) the spiegelflank: the same division as the BODY of a Let whose binder the body never reads. */
    private static String spiegelModel() { return uitModel(let("d", attr("deler"), DELING, num(4))); }

    /** (b) "Let d = deler in d plus (inkomen gedeeld door tweede)" ({@code lezingEerst}) or its mirror with the
     *  division on the left. */
    private static String barriereModel(boolean lezingEerst) {
        String body = lezingEerst
                ? binair("PLUS", bref("d", pos(2)), DELING, num(4))
                : binair("PLUS", DELING, bref("d", pos(2)), num(4));
        return uitModel(let("d", attr("deler"), body, num(4)));
    }

    /** (c)/(f) a ch. 10.2 choice over "a groter dan 100" with the §6.5 division (or the §6.6 wortel) in the DAN
     *  branch — the IfThenElse bare as the rule value, or in a Let body whose binder feeds the condition. */
    private static String keuzeModel(boolean inLetBody, boolean metWortel) {
        String dan = metWortel ? WORTEL : DELING_DELER;
        String links = inLetBody ? bref("a", pos(2)) : attr("a");
        String keuze = ite(verg("GROTER", links, lit("100", GEHEEL_POS)), dan, ZEVEN, num(4));
        return uitModel(inLetBody ? let("a", attr("a"), keuze, num(4)) : keuze);
    }

    /** (d) the division as a Let VALUE, read ({@code q plus 1}) or not ({@code 42}). */
    private static String valueModel(boolean gelezen) {
        String body = gelezen ? binair("PLUS", bref("q", num(4)), lit("1.0000", num(4)), num(4)) : lit("42.0000", num(4));
        return uitModel(let("q", DELING, body, num(4)));
    }

    /** (d) the division as a Let VALUE read only in the DAN branch of a choice on another binder. */
    private static String eenTakModel() {
        String keuze = ite(verg("GROTER", bref("k", pos(2)), lit("100", GEHEEL_POS)), bref("q", num(4)), ZEVEN, num(4));
        return uitModel(let("k", attr("a"), let("q", DELING, keuze, num(4)), num(4)));
    }

    /** (e) "Let x = leegding in Let q = inkomen gedeeld door tweede in x plus q" ({@code vreemdeEerst}) or "q plus x". */
    private static String vreemdeBarriereModel(boolean vreemdeEerst) {
        String body = vreemdeEerst
                ? binair("PLUS", bref("x", pos(2)), bref("q", num(4)), num(4))
                : binair("PLUS", bref("q", num(4)), bref("x", pos(2)), num(4));
        return uitModel(let("x", attr("leegding"), let("q", DELING, body, num(4)), num(4)));
    }

    /** (f) "de wortel van het saldo", bare or as the body of a Let whose binder is never read. */
    private static String wortelModel(boolean inLetBody) {
        return uitModel(inLetBody ? let("d", attr("deler"), WORTEL, num(4)) : WORTEL);
    }

    /** (a') the leeg dividend as the BODY of a Let whose binder is never read. */
    private static String leegDividendModel() { return uitModel(let("d", attr("deler"), DELING_LEEGDING, num(4))); }

    /** (i) "Let d = deler in begrensd(inkomen, …)" with the read of d in one bound and the §6.5 division in the
     *  other — {@code lezingInMinimum} puts the read in the minimum and the division in the maximum. */
    private static String begrensdModel(boolean lezingInMinimum) {
        String lezing = bref("d", pos(2));
        String body = lezingInMinimum ? begrensd(attr("inkomen"), lezing, DELING) : begrensd(attr("inkomen"), DELING, lezing);
        return uitModel(let("d", attr("deler"), body, num(4)));
    }

    /** (i') the same carrier with the read and the §6.5 division in the BASIS / MAXIMUM pair instead. */
    private static String begrensdBasisModel(boolean delingInBasis) {
        String lezing = bref("d", pos(2));
        String nul = lit("0.0000", num(4));
        String body = delingInBasis ? begrensd(DELING, nul, lezing) : begrensd(lezing, nul, DELING);
        return uitModel(let("d", attr("deler"), body, num(4)));
    }

    /** (a'') "Let d = deler in ((inkomen ÷ tweede) plus (5,00 ÷ leegding))" — the second site's dividend is a
     *  LITERAL, so it qualifies for no companion of its own; it still runs and reports to the probe. */
    private static String medePlekModel() {
        return uitModel(let("d", attr("deler"), binair("PLUS", DELING, DELING_LITERAAL, num(4)), num(4)));
    }

    /** (a'') the same two sites behind the read of {@code d}, so a barrier can cut both off. */
    private static String medePlekAchterBarriereModel() {
        String rechts = binair("PLUS", DELING, DELING_LITERAAL, num(4));
        return uitModel(let("d", attr("deler"), binair("PLUS", bref("d", pos(2)), rechts, num(4)), num(4)));
    }

    /** (e') "Let x = leegding in ((Let q = 1,0000 in x plus q) plus …)" — the read of the OUTER binder sits
     *  inside the INNER Let, whose handler must let that foreign barrier through. */
    private static String vreemdeBarriereDoorgifteModel(boolean metDeling) {
        String binnen = let("q", lit("1.0000", num(4)),
                binair("PLUS", bref("x", pos(2)), bref("q", num(4)), num(4)), num(4));
        String rechts = metDeling ? DELING : lit("5.0000", num(4));
        return uitModel(let("x", attr("leegding"), binair("PLUS", binnen, rechts, num(4)), num(4)));
    }

    /** (i) the same, with the division by NUMERIC zero ("inkomen gedeeld door nul") as the other bound. */
    private static String begrensdNulModel(boolean lezingInMinimum) {
        String lezing = bref("d", pos(2));
        String body = lezingInMinimum ? begrensd(attr("inkomen"), lezing, DELING_NUL) : begrensd(attr("inkomen"), DELING_NUL, lezing);
        return uitModel(let("d", attr("deler"), body, num(4)));
    }

    /** (i) a ch. 10.2 guard "d groter dan (inkomen ÷ tweede)" ({@code lezingLinks}) or its mirror, in a Let on d. */
    private static String conditieVolgordeModel(boolean lezingLinks) {
        String lezing = bref("d", pos(2));
        String conditie = lezingLinks ? verg("GROTER", lezing, DELING) : verg("GROTER", DELING, lezing);
        String keuze = ite(conditie, lit("1.0000", num(4)), lit("2.0000", num(4)), num(4));
        return uitModel(let("d", attr("deler"), keuze, num(4)));
    }

    /** (r) `deler` produced by a rule that §9.2 forces to fire AFTER the consumer's companion: its voorwaarde reads
     *  the negated kenmerk {@code vermist}, whose producer (never true for the case input) exists. The consumer is
     *  the (b) barrier body ("d plus (inkomen ÷ tweede)") or, with {@code tak}, the (c) choice on d. */
    private static String lateProducentModel(boolean tak) {
        String vermistDoel = ("{\"node\":\"AttribuutRef\",\"attribuut\":\"vermist\",\"objecttype\":\"Jutter\","
                + "\"source\":{\"snippet\":\"Een Jutter is vermist\"}}");
        String waar = "{\"node\":\"Literal\",\"value\":{\"boolean\":true},\"type\":{\"node\":\"Booleaan\"},\"source\":{\"snippet\":\"waar\"}}";
        String nietVermist = ("{\"node\":\"Kenmerk\",\"kenmerk\":\"vermist\",\"negatie\":true,"
                + "\"eigenaar\":null,\"periode\":null,\"source\":{\"snippet\":\"hij niet vermist is\"}}");
        String body = tak
                ? ite(verg("GROTER", bref("d", pos(2)), lit("1", GEHEEL_POS)), DELING, ZEVEN, num(4))
                : binair("PLUS", bref("d", pos(2)), DELING, num(4));
        return modelK(new String[]{
                        regel("vermissing van een jutter", "KENMERKTOEKENNING", vermistDoel, waar,
                                verg("GROTER", attr("inkomen"), lit("999999.00", pos(2)))),
                        regel("deler van een jutter", "BEREKENING", attr("deler"), lit("2.00", pos(2)), nietVermist),
                        regel("uit van een jutter", attr("uit", num(4)), let("d", attr("deler"), body, num(4)))});
    }

    /** (j) "Let n = inkomen in n gedeeld door tweede" — the binder is the DIVIDEND. */
    private static String binderDividendModel() {
        return uitModel(let("n", attr("inkomen"), binair("GEDEELD_DOOR", bref("n", pos(2)), attr("tweede"), num(4)), num(4)));
    }

    /** (j) "Let d = deler in inkomen gedeeld door d" — the binder is the DIVISOR. */
    private static String binderDelerModel() {
        return uitModel(let("d", attr("deler"), binair("GEDEELD_DOOR", attr("inkomen"), bref("d", pos(2)), num(4)), num(4)));
    }

    /** (j) "Let s = saldo in de wortel van s" — the binder is the RADICAND. */
    private static String binderRadicandModel() {
        return uitModel(let("s", attr("saldo", num(2)), wortel(bref("s", num(2)), num(4)), num(4)));
    }

    /** (h) a bare division plus a Let-wrapped one behind a read. */
    private static String tweeDelingenModel() {
        String omhuld = let("d", attr("leegding"), binair("PLUS", bref("d", pos(2)), DELING, num(4)), num(4));
        return uitModel(binair("PLUS", DELING_DELER, omhuld, num(4)));
    }

    /** (p) a Parameter read inside the carrier, beside the division. */
    private static String parameterModel() {
        String body = binair("PLUS", DELING, param("grondslag", GEHEEL_POS), num(4));
        return uitModel(let("d", attr("deler"), body, num(4)),
                "{\"node\":\"Parameter\",\"name\":\"grondslag\",\"datatype\":%s}"
                        .formatted(GEHEEL_POS));
    }

    /** (g) a ch. 7 group over {@code Jutter.toeslag}: a justified first default with the given consequence, then
     *  an unconditional 0. */
    private static String defaultsModel(String consequentie) {
        String groep = ("{\"node\":\"PrioritizedDefaults\",\"naam\":\"toeslag\",\"onderwerp\":\"Jutter\",\"kenmerk\":null,"
                + "\"attribuut\":\"toeslag\",\"defaults\":["
                + "{\"node\":\"Default\",\"besluit\":null,\"voorwaarde\":%s,\"consequentie\":%s,\"source\":{\"snippet\":\"toeslag\"}},"
                + "{\"node\":\"Default\",\"besluit\":null,\"voorwaarde\":%s,\"consequentie\":%s,\"source\":{\"snippet\":\"toeslag\"}}],"
                + "\"source\":{\"snippet\":\"toeslag\"},\"geldigheid\":{\"node\":\"Geldigheid\",\"vanaf\":null,\"tot\":null}}")
                .formatted(verg("GROTER_OF_GELIJK", attr("kindertal", GEHEEL_POS), lit("2", GEHEEL_POS)),
                        consequentie, ALTIJD, lit("0.0000", num(4)));
        return model(new String[]{}, "", ",\"prioritizedDefaults\":[" + groep + "]");
    }
}
