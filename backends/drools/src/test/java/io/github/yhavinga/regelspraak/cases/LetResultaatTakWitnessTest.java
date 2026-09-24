package io.github.yhavinga.regelspraak.cases;

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
 *       {@code dezelfde_deling_in_de_niet_betreden_tak_meldt_zich_niet}, the arm-level twin of the barrier
 *       case above;</li>
 *   <li><b>a conditionally-read need forced unconditionally</b> (its branch guard dropped from the prelude and
 *       the barrier) → 2 red, among them
 *       {@code een_behoefte_die_alleen_de_niet_betreden_tak_leest_wordt_niet_geforceerd} — the flank
 *       {@code synth3-takbarriere} pins in the corpus;</li>
 *   <li><b>the ARM's guard not joining the need gate</b> (the lens's first finding) →
 *       {@code een_let_in_de_niet_betreden_tak_wordt_niet_geforceerd} ERRORS with "Breuk deling door 0";</li>
 *   <li><b>the value of a conditionally-read need walked under the bare gate</b> (the lens's second) →
 *       {@code een_deling_in_een_ongelezen_behoefte_meldt_zich_niet} goes red with a fabricated
 *       Inconsistentie.</li>
 * </ul>
 */
class LetResultaatTakWitnessTest {

    // ---------------- 1. the variable that feeds the voorwaarde AND the result ----------------

    @Test void een_variabele_in_voorwaarde_en_resultaat_komt_in_beide_takken_aan() throws Exception {
        // "de dubbel is het inkomen maal 2" is read by the voorwaarde (dubbel > 10,00 — SUBSTITUTED there,
        // since no Let can span the wire's separate condition and value fields) AND by the result
        // (dubbel plus dubbel — a Let with two BinderRefs). ref: aandeel = 160.00 at inkomen 40,00.
        beideSpelen(jutter(GEVULD, waarde("aandeel", "160.00")));
    }

    @Test void de_voorwaarde_die_niet_houdt_laat_het_doel_leeg() throws Exception {
        // the other side of the same rule: inkomen 2,00 makes dubbel 4,00, which is not > 10,00, so the rule
        // does not fire and `aandeel` stays leeg — the substituted condition half really decides. ref: leeg.
        beideSpelen(jutter("\"inkomen\": { \"number\": \"2.00\" }, " + INVOER_REST, leeg("aandeel")));
    }

    // ---------------- 2. a later binder reads an earlier one ----------------

    @Test void een_latere_binder_leest_de_eerdere() throws Exception {
        // gestaffeld := Let basis = inkomen in Let opslag = basis maal 2 in basis plus opslag.
        // `opslag`'s DEFINITION reads `basis`, which is in scope because the chain nests in declaration order
        // (§11.1 rule 1, outermost first). A branch that bound only the innermost binder lowers that read to an
        // unbound-read marker and refuses the whole rule. ref: 120.00 at inkomen 40,00 (40,00 + 80,00).
        beideSpelen(jutter(GEVULD, waarde("gestaffeld", "120.00")));
    }

    // ---------------- 3. the zero guard: an unentered route evaluates nothing ----------------

    @Test void de_nulgardering_houdt_de_deling_uit_de_niet_betreden_route() throws Exception {
        // quotient := Let q = teller gedeeld door deler in q plus 1,00, under the voorwaarde `deler ongelijk 0`.
        // With deler 0 the rule does not fire, so the need is never forced and the division never runs: the
        // target stays leeg and the run is CONSISTENT. Evaluating the need regardless is not a silent wrong
        // here — rsGedeeld throws "Breuk deling door 0" and the fire aborts — which is exactly what makes this
        // case a witness rather than a formality. ref: quotient leeg, consistent.
        beideSpelen(jutter(NULDELER, leeg("quotient") + ", " + waarde("aandeel", "160.00")));
    }

    @Test void de_gevulde_delingsroute_levert_de_oorspronkelijke_uitkomst() throws Exception {
        // the route ENTERED, so the same need is forced and the division really runs. ref: 11.00 (40,00 / 4,
        // plus 1,00) — the absolute value, not merely "something non-empty".
        beideSpelen(jutter(GEVULD, waarde("quotient", "11.00")));
    }

    @Test void een_lege_buitenbehoefte_houdt_de_binnenste_deling_ongeforceerd() throws Exception {
        // garde := Let veilig = inkomen in Let q = teller gedeeld door staffel in veilig plus q, with inkomen
        // ABSENT and staffel 0. ch. 10.6 reads `veilig` first, the barrier fires, and `q` is never forced — so
        // the division by zero never happens and the whole Let is leeg. This is the flank that makes the
        // prelude's own guard load-bearing: measured by mutation (guard dropped), this branch reports
        // "Breuk deling door 0" here instead of playing. ref: garde leeg, consistent.
        beideSpelen(jutter(BARRIERE_VOOR_DE_DELING, leeg("garde") + ", " + leeg("quotient")));
    }

    // ---------------- 3b. the §6.5 fout companion walks through the binding ----------------

    @Test void een_deling_door_leeg_binnen_een_behoefte_meldt_zich_nog_steeds() throws Exception {
        // heffing := Let q = teller gedeeld door heffingsdeler in q plus 1,00, with the divisor ABSENT. §6.5
        // Tabel 12 makes that a Fout: the value degrades to leeg and the §9.5 channel carries the reason, so
        // ref answers INCONSISTENT. The DRL branch surfaces it through a SEPARATE companion rule built from a
        // walk over the rule value — a walk that stops at a Let sees no division at all and leaves the run
        // silently CONSISTENT, which is the direction that cannot be noticed from the outside.
        beideSpelen(jutterMet(LEGE_HEFFINGSDELER, leeg("heffing") + ", " + waarde("bescherm", "50.00")
                + ", { \"kind\": \"inconsistent\", \"rule\": \"heffing van een jutter\", \"label\": \"J1\" }"));
    }

    @Test void een_deling_achter_een_lege_behoefte_meldt_zich_juist_niet() throws Exception {
        // the other direction, which a companion built WITHOUT the needs would get wrong: bescherm := Let
        // veilig = inkomen in Let r = teller gedeeld door beschermdeler in veilig plus r, with BOTH inkomen and
        // the divisor absent. The read of `veilig` empties the Let before `r` is ever forced, so no division
        // runs and there is nothing to report — ref: bescherm leeg, CONSISTENT. A companion that fired on the
        // bare "dividend vol, divisor leeg" pattern would invent an Inconsistentie on a path that never ran.
        beideSpelen(jutter(LEGE_DELER_ACHTER_DE_BARRIERE, waarde("heffing", "11.00") + ", " + leeg("bescherm")));
    }

    // ---------------- 4. leeg beside gevuld: both mutants go red ----------------

    @Test void een_lege_behoefte_maakt_de_hele_let_leeg_terwijl_dezelfde_maal_zonder_let_nul_rekent() throws Exception {
        // ch. 10.6's normative contrast in ONE case with `inkomen` ABSENT:
        //   metlet    := Let ink = inkomen in ink maal 2  -> the read forces the leeg need and raises the
        //                barrier, so the whole Let is leeg and nothing is written (ref: leeg);
        //   zonderlet := inkomen maal 2                   -> the bare maal COALESCES leeg to 0 (ref: 0.00).
        // An ALWAYS-WRITING lowering (the barrier dropped) writes 0.00 on both and fails here — measured.
        beideSpelen(jutter(LEEG, leeg("metlet") + ", " + waarde("zonderlet", "0.00")));
    }

    @Test void de_gevulde_invoer_schrijft_wel_en_beide_vormen_komen_uit_op_hetzelfde() throws Exception {
        // the other mutant's red: a NEVER-WRITING lowering (the Let always leeg) passes every emptiness pin
        // above and fails here — measured. ref: 80.00 for both forms at inkomen 40,00.
        beideSpelen(jutter(GEVULD, waarde("metlet", "80.00") + ", " + waarde("zonderlet", "80.00")));
    }

    @Test void het_lege_doel_is_observeerbaar_afwezig_in_beide_takken() throws Exception {
        // the comparator must SEE the absence rather than leave it unasserted: the same input with a VALUE
        // expectation on `metlet` must MISMATCH in both branches, and name the target.
        String geval = jutter(LEEG, waarde("metlet", "0.00"));
        for (String tak : List.of("exec", "drl")) {
            CaseRunner.Result r = speel(tak, geval);
            assertEquals("FAIL", r.status(), tak + ": de leeg-barriere moet observeerbaar zijn, niet slechts niet-geasserteerd");
            assertTrue(r.caseFailures().toString().contains("metlet"), tak + ": " + r.caseFailures());
        }
    }

    // ---------------- 5. one need per binder, shadowing, siblings ----------------

    @Test void dezelfde_binder_twee_keer_gelezen_levert_dezelfde_behoefte() throws Exception {
        // dubbelop := Let ink = inkomen in ink plus ink. A second read that answered leeg would coalesce to 0
        // inside plus and quietly halve the result. ref: 80.00 at inkomen 40,00.
        beideSpelen(jutter(GEVULD, waarde("dubbelop", "80.00")));
    }

    @Test void naamgelijkheid_schaduwt_binnen_en_lekt_niet_naar_buiten() throws Exception {
        // schaduw := Let x = inkomen in ((Let x = 5,00 in x) plus x). WF-064: the INNER x wins inside its own
        // body, and the OUTER x is intact again outside it. Reading the outer binder inside gives 80.00;
        // binding the inner x IN PLACE of the outer (a mutated environment instead of a scoped one) leaves
        // x = 5,00 for the trailing read and gives 10.00. Two same-named binders also may not collapse onto
        // one emitted variable. ref: 45.00 (5,00 + 40,00).
        beideSpelen(jutter(GEVULD, waarde("schaduw", "45.00")));
    }

    @Test void een_zusterbinding_houdt_haar_eigen_antwoord_als_de_buur_leegloopt() throws Exception {
        // zusters := (Let a = inkomen in a) plus (Let b = 5,00 in b), with inkomen ABSENT. The barrier belongs
        // to the Let that bound the binder that was read, so only the LEFT one empties; the §6.2 leegwaarde
        // table then coalesces it to 0 inside the plus and the right one still answers. A guard hung on the
        // whole consequence instead of on each Let node would empty the rule value and write leeg. ref: 5.00.
        beideSpelen(jutter(LEEG, waarde("zusters", "5.00")));
    }

    @Test void de_zusterbinding_rekent_gevuld_gewoon_mee() throws Exception {
        // the same pair with both sides gevuld, so the pin above is not passing on an arm that never adds.
        // ref: 45.00 (40,00 + 5,00).
        beideSpelen(jutter(GEVULD, waarde("zusters", "45.00")));
    }

    // ---------------- 5b. ch. 10.2: the value-level conditional ----------------

    @Test void de_danstak_wordt_betreden_en_de_anderstak_niet() throws Exception {
        // tak := Let k = inkomen in (als k groter dan 10,00 dan k maal 2 anders 2,00). ref: 80.00 at inkomen
        // 40,00 — the guard holds, so the DAN arm is the value and the anders arm is not evaluated at all.
        beideSpelen(jutter(GEVULD, waarde("tak", "80.00")));
    }

    @Test void de_anderstak_wordt_betreden_en_de_danstak_niet() throws Exception {
        // the same rule at inkomen 2,00: the guard fails, so the ANDERS arm decides. ref: 2.00. An
        // implementation with the arms swapped passes neither this test nor the one above.
        beideSpelen(jutter("\"inkomen\": { \"number\": \"2.00\" }, " + INVOER_REST, waarde("tak", "2.00")));
    }

    @Test void een_lege_behoefte_in_de_takgarde_eindigt_de_let_voor_de_keuze() throws Exception {
        // the guard READS a binder (WF-077), so a leeg need ends the Let at that read (ch. 10.6 clause 2) and no
        // arm is chosen at all — this is WHY a ch. 10.2 guard never sees a leeg operand. Measured 2026-09-18 on
        // the only other route a WF-077-clean document has, an unsupplied parameter: ref FAULTS fail-fast
        // ("parameter … is unbound in this case", ch. 5.7), so it is not leeg either. The guard nevertheless goes
        // through the SHARED rsAggVergelijk, so a document that somehow presents a leeg operand gets ref's own
        // Tabel 33/37/44 rather than an mvel accident. ref here: tak leeg.
        beideSpelen(jutter(LEEG, leeg("tak")));
    }

    @Test void een_ifthenelse_in_een_letbody_leest_een_eerdere_behoefte() throws Exception {
        // the jutterswet-tariefstelling shape, isolated: takketen := Let a = inkomen in Let b = a maal 2 in
        // (als a groter dan 10,00 dan b anders 0,00). The guard reads the EARLIER binder and the chosen arm reads
        // the LATER one — the chain and the branch in one value. ref: 80.00 at inkomen 40,00, 0.00 at 2,00.
        beideSpelen(jutter(GEVULD, waarde("takketen", "80.00")));
        beideSpelen(jutter("\"inkomen\": { \"number\": \"2.00\" }, " + INVOER_REST, waarde("takketen", "0.00")));
    }

    @Test void een_behoefte_die_alleen_de_niet_betreden_tak_leest_wordt_niet_geforceerd() throws Exception {
        // takopslag := Let g = inkomen in Let o = takopslaggrond in (als g groter dan 10,00 dan 3,00 anders
        // 3,00 plus o), with the guard HOLDING and `takopslaggrond` ABSENT. ch. 10.6 clause 3 over a ch. 10.2
        // arm: the dan arm never reads `o`, so its need is never forced and its barrier never fires — the target
        // is WRITTEN. ref: 3.00. An eager forcing (the need computed whatever the guard says) makes `o` leeg and
        // the barrier empties the whole Let: the corpus pins the same flank in synth3-takbarriere.
        beideSpelen(jutter(ONGELEZEN_TAKOPSLAG, waarde("takopslag", "3.00")));
    }

    @Test void dezelfde_behoefte_telt_wel_mee_zodra_haar_tak_betreden_wordt() throws Exception {
        // the other side, so the pin above is not passing on an arm that never adds: at inkomen 2,00 the ANDERS
        // arm runs, `o` IS read and its need is forced. ref: 9.00 (3,00 + 6,00).
        beideSpelen(jutter("\"inkomen\": { \"number\": \"2.00\" }, " + INVOER_REST, waarde("takopslag", "9.00")));
    }

    @Test void een_deling_in_de_betreden_tak_meldt_zich() throws Exception {
        // takdeling := Let k = inkomen in (als k groter dan 10,00 dan teller gedeeld door takdeler anders 7,00),
        // guard HOLDING and the divisor ABSENT: the §6.5 site really runs, so ref answers INCONSISTENT and the
        // target stays leeg. The DRL companion carries the need gate AND the branch guard, and must still fire.
        beideSpelen(jutterMet(LEGE_TAKDELER_DAN, leeg("takdeling") + ", " + waarde("tak", "80.00")
                + ", { \"kind\": \"inconsistent\", \"rule\": \"takdeling van een jutter\", \"label\": \"J1\" }"));
    }

    @Test void dezelfde_deling_in_de_niet_betreden_tak_meldt_zich_niet() throws Exception {
        // the same rule and the same absent divisor at inkomen 2,00: the ANDERS arm runs, the division is never
        // evaluated, and there is nothing to report — ref: takdeling 7.00, CONSISTENT. A companion without the
        // branch guard invents an Inconsistentie here.
        //
        // NOTE, measured 2026-09-18 and reported rather than pinned away: on THIS shape the clingo backend
        // answers inconsistent where ref answers consistent ("expected consistent, but the model carries 1
        // inconsistent fact(s)") — its §6.5 fence is not path-following through a ch. 10.2 arm. That is a
        // ref-vs-clingo question in a backend this card may not touch, so the shape is witnessed here (where ref
        // is the arbiter and BOTH Drools branches agree with it) and never proposed as a corpus golden, which
        // gate 6 would rightly reject.
        beideSpelen(jutter(LEGE_TAKDELER_ANDERS, waarde("takdeling", "7.00") + ", " + waarde("tak", "2.00")));
    }

    // ---------------- 5c. the two shapes the adversarial lens found (2026-09-18) ----------------

    @Test void een_let_in_de_niet_betreden_tak_wordt_niet_geforceerd() throws Exception {
        // sprong := Let k = inkomen in (als k groter dan 10,00 dan 5,00 anders Let z = teller gedeeld door
        // nuldeler in z plus 1,00), guard HOLDING and `nuldeler` 0. The dan arm is the value, so the Let in the
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
        beideSpelen(jutter(NUL_IN_DE_NIET_BETREDEN_ARM, waarde("sprong", "5.00") + ", " + waarde("takgat", "4.00")));
    }

    @Test void een_deling_in_een_ongelezen_behoefte_meldt_zich_niet() throws Exception {
        // takgat := Let k = inkomen in Let z = teller gedeeld door gatdeler in (als k groter dan 10,00 dan 4,00
        // anders z), guard HOLDING and the divisor ABSENT. `z` is read only by the ANDERS arm, so under ch. 10.6
        // clause 3 its need is never forced and its §6.5 site never runs — ref: takgat 4.00, CONSISTENT.
        //
        // As landed on 2026-09-18 the companion walk entered that need's value under the BARE gate, ignoring
        // that the read was conditional, and this branch inserted an Inconsistentie for a division that never
        // executed. synth3-takbarriere has exactly this shape: one division in its opslag definition and the
        // corpus golden would have gone red on a fabricated fault. Measured then: ref PASS consistent, exec
        // PLAYED, --drl FAIL "verwacht consistent, werkelijk inconsistent".
        //
        // clingo here: MISMATCH, expected consistent — the same non-path-following §6.5 fence recorded above.
        beideSpelen(jutter(GAT_IN_EEN_ONGELEZEN_BEHOEFTE, waarde("takgat", "4.00") + ", " + waarde("sprong", "5.00")));
    }

    @Test void beide_vormen_rekenen_gewoon_zodra_hun_tak_betreden_wordt() throws Exception {
        // the other side of both, so neither pin passes on an arm that never computes: at inkomen 2,00 the
        // ANDERS arm runs in both rules, so both needs ARE forced. ref: sprong 11.00 (40/4 + 1), takgat 10.00.
        beideSpelen(jutter("\"inkomen\": { \"number\": \"2.00\" }, " + INVOER_REST,
                waarde("sprong", "11.00") + ", " + waarde("takgat", "10.00")));
    }

    @Test void twee_verschillende_takgardes_op_een_lezing_worden_bij_naam_geweigerd() throws Exception {
        // dubbelgarde := Let k = inkomen in Let z = k maal 2 in ((als k groter dan 10,00 dan z anders 0,00) plus
        // (als k kleiner dan 99,00 dan z anders 0,00)). `z` is read under TWO different branch guards, so the
        // conjunction its prelude would need has no guarantee that its operands are assigned by the time the
        // line runs. This branch refuses the shape BY NAME rather than guessing; the exec branch, whose needs
        // are genuine thunks, plays it (ref: 160.00). The refusal is the honest half of the ONBEPAALD answer in
        // the read-guard lattice, and this is its only witness. clingo refuses the document outright
        // ("TRANSLATION FAULT: expression kind not translated").
        java.nio.file.Path ir = tijdelijk("let-dubbelgarde-ir", DUBBELGARDE_MODEL);
        java.nio.file.Path c = tijdelijk("let-dubbelgarde-case", DUBBELGARDE_CASE);
        try {
            assertEquals("PLAYED", CaseRunner.viaExecModel(ir, List.of(c)).status(), "exec speelt de vorm");
            CaseRunner.Result drl = CaseRunner.viaDrlTekst(ir, List.of(c));
            assertEquals("EXCLUDE", drl.status(), "de DRL-tak weigert bij naam: " + drl.reason());
            assertTrue(drl.reason().contains("behoefte gelezen onder twee verschillende takgardes"), drl.reason());
        } finally {
            Files.deleteIfExists(ir);
            Files.deleteIfExists(c);
        }
    }

    // ---------------- 6. the non-regression flank: a voorwaarde-only variable ----------------

    @Test void een_voorwaarde_only_variabele_blijft_zonder_let_werken() throws Exception {
        // "de dubbel is het inkomen maal 2" read ONLY in the voorwaarde gets no Let at all (ch. 10.6: an
        // unread binder would be a guaranteed no-op) — the frontend substitutes it, and both branches have
        // carried that shape all along. ref: premie 7.00 at inkomen 40,00 (80 > 50), leeg at inkomen 2,00.
        beideSpelen(jutter(GEVULD, waarde("premie", "7.00")));
        beideSpelen(jutter("\"inkomen\": { \"number\": \"2.00\" }, " + INVOER_REST, leeg("premie")));
    }

    // ---------------- replay: one document, one case, BOTH branches ----------------

    /** Both transpiler branches on the authored model and one case; each must PLAY it. */
    private static void beideSpelen(String geval) throws Exception {
        for (String tak : List.of("exec", "drl")) {
            CaseRunner.Result r = speel(tak, geval);
            assertEquals("PLAYED", r.status(), tak + ": " + r.reason() + " " + r.caseFailures());
        }
    }

    private static CaseRunner.Result speel(String tak, String geval) throws Exception {
        Path ir = tijdelijk("let-resultaat-ir", MODEL), c = tijdelijk("let-resultaat-case", geval);
        try {
            return tak.equals("drl") ? CaseRunner.viaDrlTekst(ir, List.of(c)) : CaseRunner.viaExecModel(ir, List.of(c));
        } finally {
            Files.deleteIfExists(ir);
            Files.deleteIfExists(c);
        }
    }

    private static Path tijdelijk(String naam, String inhoud) throws Exception {
        Path p = Files.createTempFile(naam, ".json");
        p.toFile().deleteOnExit();
        Files.writeString(p, inhoud);
        return p;
    }

    // ---------------- the case inputs ----------------

    /** The two divisors the ch. 10.2 arm witnesses read; every other case supplies them harmlessly. */
    private static final String TAKINVOER =
            ", \"nuldeler\": { \"number\": \"4\" }, \"gatdeler\": { \"number\": \"4\" }";

    /** The divisors every case supplies the same way unless it says otherwise: a case that leaves one out
     *  is making a point about THAT divisor, since a leeg divisor is a §6.5 Fout wherever its need is forced. */
    private static final String INVOER_REST =
            "\"teller\": { \"number\": \"40.00\" }, \"deler\": { \"number\": \"4\" }, \"staffel\": { \"number\": \"4\" }, "
            + "\"heffingsdeler\": { \"number\": \"4\" }, \"beschermdeler\": { \"number\": \"4\" }, "
            + "\"takdeler\": { \"number\": \"4\" }, \"takopslaggrond\": { \"number\": \"6.00\" }" + TAKINVOER;
    private static final String GEVULD = "\"inkomen\": { \"number\": \"40.00\" }, " + INVOER_REST;
    private static final String LEEG = INVOER_REST;                     // inkomen ABSENT
    private static final String NULDELER =
            "\"inkomen\": { \"number\": \"40.00\" }, \"teller\": { \"number\": \"40.00\" }, "
            + "\"deler\": { \"number\": \"0\" }, \"staffel\": { \"number\": \"4\" }, "
            + "\"heffingsdeler\": { \"number\": \"4\" }, \"beschermdeler\": { \"number\": \"4\" }, "
            + "\"takdeler\": { \"number\": \"4\" }, \"takopslaggrond\": { \"number\": \"6.00\" }" + TAKINVOER;
    private static final String BARRIERE_VOOR_DE_DELING =                // inkomen ABSENT and staffel 0
            "\"teller\": { \"number\": \"40.00\" }, \"deler\": { \"number\": \"0\" }, \"staffel\": { \"number\": \"0\" }, "
            + "\"heffingsdeler\": { \"number\": \"4\" }, \"beschermdeler\": { \"number\": \"4\" }, "
            + "\"takdeler\": { \"number\": \"4\" }, \"takopslaggrond\": { \"number\": \"6.00\" }" + TAKINVOER;
    /** `heffingsdeler` ABSENT: the §6.5 site inside a NEED's own value really runs, and faults. */
    private static final String LEGE_HEFFINGSDELER =
            "\"inkomen\": { \"number\": \"40.00\" }, \"teller\": { \"number\": \"40.00\" }, "
            + "\"deler\": { \"number\": \"4\" }, \"staffel\": { \"number\": \"4\" }, "
            + "\"beschermdeler\": { \"number\": \"4\" }, "
            + "\"takdeler\": { \"number\": \"4\" }, \"takopslaggrond\": { \"number\": \"6.00\" }" + TAKINVOER;
    /** `beschermdeler` AND inkomen ABSENT: the same site sits BEHIND a barrier that fires first. */
    private static final String LEGE_DELER_ACHTER_DE_BARRIERE =
            "\"teller\": { \"number\": \"40.00\" }, \"deler\": { \"number\": \"4\" }, "
            + "\"staffel\": { \"number\": \"4\" }, \"heffingsdeler\": { \"number\": \"4\" }, "
            + "\"takdeler\": { \"number\": \"4\" }, \"takopslaggrond\": { \"number\": \"6.00\" }" + TAKINVOER;
    /** inkomen 40,00 (the DAN arm runs) with `takdeler` ABSENT: the §6.5 site in the ENTERED arm really runs. */
    private static final String LEGE_TAKDELER_DAN =
            "\"inkomen\": { \"number\": \"40.00\" }, \"teller\": { \"number\": \"40.00\" }, "
            + "\"deler\": { \"number\": \"4\" }, \"staffel\": { \"number\": \"4\" }, "
            + "\"heffingsdeler\": { \"number\": \"4\" }, \"beschermdeler\": { \"number\": \"4\" }, "
            + "\"takopslaggrond\": { \"number\": \"6.00\" }" + TAKINVOER;
    /** inkomen 2,00 (the ANDERS arm runs) with the same divisor absent: the site is in the arm NOT taken. */
    private static final String LEGE_TAKDELER_ANDERS =
            "\"inkomen\": { \"number\": \"2.00\" }, \"teller\": { \"number\": \"40.00\" }, "
            + "\"deler\": { \"number\": \"4\" }, \"staffel\": { \"number\": \"4\" }, "
            + "\"heffingsdeler\": { \"number\": \"4\" }, \"beschermdeler\": { \"number\": \"4\" }, "
            + "\"takopslaggrond\": { \"number\": \"6.00\" }" + TAKINVOER;
    /** inkomen 40,00 (the DAN arm runs) with `takopslaggrond` ABSENT — a need only the OTHER arm reads. */
    private static final String ONGELEZEN_TAKOPSLAG =
            "\"inkomen\": { \"number\": \"40.00\" }, \"teller\": { \"number\": \"40.00\" }, "
            + "\"deler\": { \"number\": \"4\" }, \"staffel\": { \"number\": \"4\" }, "
            + "\"heffingsdeler\": { \"number\": \"4\" }, \"beschermdeler\": { \"number\": \"4\" }, "
            + "\"takdeler\": { \"number\": \"4\" }" + TAKINVOER;

    /** `nuldeler` 0 with the guard HOLDING: the division sits in the arm that is NOT taken. */
    private static final String NUL_IN_DE_NIET_BETREDEN_ARM =
            "\"inkomen\": { \"number\": \"40.00\" }, \"teller\": { \"number\": \"40.00\" }, "
            + "\"deler\": { \"number\": \"4\" }, \"staffel\": { \"number\": \"4\" }, "
            + "\"heffingsdeler\": { \"number\": \"4\" }, \"beschermdeler\": { \"number\": \"4\" }, "
            + "\"takdeler\": { \"number\": \"4\" }, \"takopslaggrond\": { \"number\": \"6.00\" }, "
            + "\"nuldeler\": { \"number\": \"0\" }, \"gatdeler\": { \"number\": \"4\" }";
    /** `gatdeler` ABSENT with the guard HOLDING: the §6.5 site sits in a NEED only the other arm reads. */
    private static final String GAT_IN_EEN_ONGELEZEN_BEHOEFTE =
            "\"inkomen\": { \"number\": \"40.00\" }, \"teller\": { \"number\": \"40.00\" }, "
            + "\"deler\": { \"number\": \"4\" }, \"staffel\": { \"number\": \"4\" }, "
            + "\"heffingsdeler\": { \"number\": \"4\" }, \"beschermdeler\": { \"number\": \"4\" }, "
            + "\"takdeler\": { \"number\": \"4\" }, \"takopslaggrond\": { \"number\": \"6.00\" }, "
            + "\"nuldeler\": { \"number\": \"4\" }";

    private static String jutter(String attributen, String verwacht) {
        return jutterMet(attributen, verwacht + ", { \"kind\": \"consistent\" }");
    }

    private static String jutterMet(String attributen, String verwacht) {
        return """
                { "format": "uchar-case-v0", "asOfDate": null,
                  "input": { "instances": [
                      { "label": "J1", "objectType": "Jutter",
                        "attributes": { %s },
                        "roles": {}, "characteristics": [] } ],
                    "parameters": {} },
                  "expected": [ %s ] }
                """.formatted(attributen, verwacht);
    }

    private static String waarde(String attribuut, String getal) {
        return """
                { "kind": "attributeValue", "label": "J1", "attribute": "%s", "value": { "number": "%s" } }"""
                .formatted(attribuut, getal);
    }

    private static String leeg(String attribuut) {
        return "{ \"kind\": \"attributeEmpty\", \"label\": \"J1\", \"attribute\": \"%s\" }".formatted(attribuut);
    }

    // ---------------- the wire shapes (IR-SPEC ch. 4.10) ----------------

    private static final String ONBEPAALD = "ONBEPAALD", NN = "NIET_NEGATIEF";

    private static String num(int dec, boolean geheel, String teken) {
        return "{\"node\":\"Number\",\"whole\":%b,\"decimals\":%d,\"sign\":\"%s\"}"
                .formatted(geheel, dec, teken);
    }

    private static String attr(String naam, int dec, boolean geheel, String teken) {
        return ("{\"node\":\"AttributeReference\",\"attribute\":\"%s\",\"subjectType\":\"Jutter\","
                + "\"source\":{\"snippet\":\"het %s van de Jutter\"}}")
                .formatted(naam, naam);
    }

    private static String lit(String waarde, int dec, boolean geheel) {
        return "{\"node\":\"Literal\",\"value\":{\"number\":\"%s\"},\"type\":%s,\"source\":{\"snippet\":\"%s\"}}"
                .formatted(waarde, num(dec, geheel, ONBEPAALD), waarde);
    }

    private static String binair(String op, String links, String rechts) {
        return "{\"node\":\"Binary\",\"op\":\"%s\",\"left\":%s,\"right\":%s,\"type\":%s,\"source\":{\"snippet\":\"%s\"}}"
                .formatted(op, links, rechts, num(2, false, ONBEPAALD), op.toLowerCase());
    }

    /** ch. 4.10 the value-level binding; WF-076 makes its {@code type} the BODY's, which {@code teken} included
     *  is what the verifier checks — so the body's sign is spelled at each call site. */
    private static String let(String binder, String value, String body, String bodyTeken) {
        return "{\"node\":\"Let\",\"type\":%s,\"binder\":\"%s\",\"value\":%s,\"body\":%s,\"source\":{\"snippet\":\"de %s is …\"}}"
                .formatted(num(2, false, bodyTeken), binder, value, body, binder);
    }

    /** ch. 4.10 a read of the nearest enclosing binder. NB the wire field is {@code name}, not {@code naam};
     *  WF-091 makes the read's sign that of the value it is bound to, so it rides along. */
    private static String bref(String naam, String teken) {
        return "{\"node\":\"BinderRef\",\"name\":\"%s\",\"source\":{\"snippet\":\"de %s\"}}"
                .formatted(naam, naam);
    }

    /** ch. 4.10 the value-level conditional. WF-069 makes both arms agree with the node's {@code type}, sign
     *  included, which the verifier checks — so the arms' shared sign is spelled at each call site. */
    private static String ite(String conditie, String dan, String anders, String teken) {
        return ("{\"node\":\"IfThenElse\",\"type\":%s,\"condition\":%s,\"then\":%s,\"else\":%s,"
                + "\"source\":{\"snippet\":\"als … dan … anders …\"}}").formatted(num(2, false, teken), conditie, dan, anders);
    }

    private static String verg(String op, String links, String rechts) {
        return "{\"node\":\"Comparison\",\"op\":\"%s\",\"left\":%s,\"right\":%s,\"source\":{\"snippet\":\"%s\"}}"
                .formatted(op, links, rechts, op.toLowerCase());
    }

    private static String attribuut(String naam, String datatype) {
        return "{\"node\":\"Attribute\",\"name\":\"%s\",\"datatype\":%s}".formatted(naam, datatype);
    }

    /** A rule of one always-valid computing version; {@code voorwaarde} "null" writes no condition. */
    private static String regel(String naam, String doel, String waarde, String voorwaarde) {
        return ("{\"node\":\"Rule\",\"name\":\"%s\",\"versions\":[{\"validity\":{\"node\":\"Validity\""
                + "},\"target\":%s,\"value\":%s%s,\"initialisation\":false,"
                + "\"presentation\":\"calculation\"}]}")
                .formatted(naam, doel, waarde, voorwaarde.equals("null") ? "" : ",\"condition\":" + voorwaarde);
    }

    // ---------------- the one document (verifier-clean, ref- and clingo-replayed before pinning) ----------

    private static final String MODEL = bouwModel();

    /** A one-rule document whose single binder is read under TWO different branch guards — the ONBEPAALD arm of
     *  the read-guard lattice, which this branch refuses by name. Kept apart from {@link #MODEL} for the reason
     *  the refusal exists: a marker makes the WHOLE document an EXCLUDE, so it may not sit beside pins that must
     *  play. Verifier-clean and ref-replayed (160.00) before pinning. */
    private static final String DUBBELGARDE_MODEL = bouwDubbelgarde();

    private static final String DUBBELGARDE_CASE = jutter("\"inkomen\": { \"number\": \"40.00\" }",
            waarde("dubbelgarde", "160.00"));

    private static String bouwDubbelgarde() {
        String inkomen = attr("inkomen", 2, false, NN);
        String z = bref("z", ONBEPAALD);
        String w = let("k", inkomen,
                let("z", binair("MAAL", bref("k", NN), lit("2", 0, true)),
                        binair("PLUS",
                                ite(verg("GROTER", bref("k", NN), lit("10.00", 2, false)), z,
                                        lit("0.00", 2, false), ONBEPAALD),
                                ite(verg("KLEINER", bref("k", NN), lit("99.00", 2, false)), z,
                                        lit("0.00", 2, false), ONBEPAALD)),
                        ONBEPAALD), ONBEPAALD);
        return ("{\"format\":\"uchar-ir-v1\",\"model\":{"
                + "\"objectTypes\":[{\"node\":\"ObjectType\",\"name\":\"Jutter\",\"animate\":true,\"plural\":\"Jutters\","
                + "\"attributes\":[%s,%s],\"characteristics\":[]}],"
                + "\"factTypes\":[],\"parameters\":[],\"domains\":[],\"unitSystems\":[],\"dimensions\":[],"
                + "\"timelines\":[],\"dayKinds\":[]},"
                + "\"rules\":{\"rules\":[%s],"
                + "\"dayKindDefinitions\":[],\"objectCreations\":[],\"allocations\":[],\"consistencies\":[],"
                + "\"uniquenesses\":[],\"factCreations\":[],\"startPointDeterminations\":[],"
                + "\"ruleGroups\":[]},\"diagnostics\":[]}")
                .formatted(attribuut("inkomen", num(2, false, NN)), attribuut("dubbelgarde", num(2, false, ONBEPAALD)),
                        regel("dubbelgarde van een jutter", attr("dubbelgarde", 2, false, ONBEPAALD), w, "null"));
    }

    private static String bouwModel() {
        String inkomen = attr("inkomen", 2, false, NN);
        String teller = attr("teller", 2, false, NN);
        String deler = attr("deler", 0, true, NN);
        String staffel = attr("staffel", 0, true, NN);
        String heffingsdeler = attr("heffingsdeler", 0, true, NN);
        String beschermdeler = attr("beschermdeler", 0, true, NN);
        String takdeler = attr("takdeler", 0, true, NN);
        String takopslaggrond = attr("takopslaggrond", 2, false, NN);
        String nuldeler = attr("nuldeler", 0, true, NN);
        String gatdeler = attr("gatdeler", 0, true, NN);
        String boven10 = verg("GROTER", bref("k", NN), lit("10.00", 2, false));
        String dubbelDef = binair("MAAL", inkomen, lit("2", 0, true));

        String[][] rs = {
            // the variable read in the voorwaarde (substituted) AND in the result (bound)
            {"aandeel van een jutter", attr("aandeel", 2, false, ONBEPAALD),
             let("dubbel", dubbelDef, binair("PLUS", bref("dubbel", ONBEPAALD), bref("dubbel", ONBEPAALD)), ONBEPAALD),
             verg("GROTER", dubbelDef, lit("10.00", 2, false))},
            // a later binder reading an earlier one
            {"gestaffeld van een jutter", attr("gestaffeld", 2, false, ONBEPAALD),
             let("basis", inkomen,
                 let("opslag", binair("MAAL", bref("basis", NN), lit("2", 0, true)),
                     binair("PLUS", bref("basis", NN), bref("opslag", ONBEPAALD)), ONBEPAALD), ONBEPAALD),
             "null"},
            // the zero guard: the division sits inside the variable definition, behind the rule's voorwaarde
            {"quotient van een jutter", attr("quotient", 2, false, ONBEPAALD),
             let("q", binair("GEDEELD_DOOR", teller, deler),
                 binair("PLUS", bref("q", ONBEPAALD), lit("1.00", 2, false)), ONBEPAALD),
             verg("ONGELIJK", deler, lit("0", 0, true))},
            // an outer need that empties must keep the inner (zero-dividing) need unforced
            {"garde van een jutter", attr("garde", 2, false, ONBEPAALD),
             let("veilig", inkomen,
                 let("q", binair("GEDEELD_DOOR", teller, staffel),
                     binair("PLUS", bref("veilig", NN), bref("q", ONBEPAALD)), ONBEPAALD), ONBEPAALD),
             "null"},
            // the barrier beside its contrast
            {"met let van een jutter", attr("metlet", 2, false, ONBEPAALD),
             let("ink", inkomen, binair("MAAL", bref("ink", NN), lit("2", 0, true)), ONBEPAALD), "null"},
            {"zonder let van een jutter", attr("zonderlet", 2, false, ONBEPAALD),
             binair("MAAL", inkomen, lit("2", 0, true)), "null"},
            // one binder, two reads
            {"dubbelop van een jutter", attr("dubbelop", 2, false, ONBEPAALD),
             let("ink", inkomen, binair("PLUS", bref("ink", NN), bref("ink", NN)), ONBEPAALD), "null"},
            // name-sameness: shadowed inside, intact outside
            {"schaduw van een jutter", attr("schaduw", 2, false, ONBEPAALD),
             let("x", inkomen,
                 binair("PLUS", let("x", lit("5.00", 2, false), bref("x", ONBEPAALD), ONBEPAALD),
                        bref("x", NN)), ONBEPAALD), "null"},
            // sibling bindings: the barrier belongs to the Let that bound the binder that was read
            {"zusters van een jutter", attr("zusters", 2, false, ONBEPAALD),
             binair("PLUS", let("a", inkomen, bref("a", NN), NN),
                    let("b", lit("5.00", 2, false), bref("b", ONBEPAALD), ONBEPAALD)), "null"},
            // the non-regression flank: a variable NO result read reaches gets no Let at all
            {"premie van een jutter", attr("premie", 2, false, ONBEPAALD), lit("7.00", 2, false),
             verg("GROTER", dubbelDef, lit("50.00", 2, false))},
            // a §6.5 site inside a NEED's own value — the companion must still be built for it
            {"heffing van een jutter", attr("heffing", 2, false, ONBEPAALD),
             let("q", binair("GEDEELD_DOOR", teller, heffingsdeler),
                 binair("PLUS", bref("q", ONBEPAALD), lit("1.00", 2, false)), ONBEPAALD), "null"},
            // the same site BEHIND a barrier — the companion must carry that need as its gate
            {"bescherm van een jutter", attr("bescherm", 2, false, ONBEPAALD),
             let("veilig", inkomen,
                 let("r", binair("GEDEELD_DOOR", teller, beschermdeler),
                     binair("PLUS", bref("veilig", NN), bref("r", ONBEPAALD)), ONBEPAALD), ONBEPAALD), "null"},
            // ---- ch. 10.2, the value-level conditional (WF-069: inside a Let body; WF-077: binder guard) ----
            // both arms, one rule
            {"tak van een jutter", attr("tak", 2, false, ONBEPAALD),
             let("k", inkomen, ite(boven10, binair("MAAL", bref("k", NN), lit("2", 0, true)),
                     lit("2.00", 2, false), ONBEPAALD), ONBEPAALD), "null"},
            // a §6.5 site in ONE arm — it runs in the entered arm and not in the other
            {"takdeling van een jutter", attr("takdeling", 2, false, ONBEPAALD),
             let("k", inkomen, ite(boven10, binair("GEDEELD_DOOR", teller, takdeler),
                     lit("7.00", 2, false), ONBEPAALD), ONBEPAALD), "null"},
            // a NEED only the anders arm reads: the dan arm may not force it, nor raise its barrier
            {"takopslag van een jutter", attr("takopslag", 2, false, ONBEPAALD),
             let("g", inkomen,
                 let("o", takopslaggrond,
                     ite(verg("GROTER", bref("g", NN), lit("10.00", 2, false)), lit("3.00", 2, false),
                         binair("PLUS", lit("3.00", 2, false), bref("o", NN)), ONBEPAALD), ONBEPAALD), ONBEPAALD), "null"},
            // the tariefstelling shape: a LATER need read by one arm, the guard reading the earlier binder
            {"takketen van een jutter", attr("takketen", 2, false, ONBEPAALD),
             let("a", inkomen,
                 let("b", binair("MAAL", bref("a", NN), lit("2", 0, true)),
                     ite(verg("GROTER", bref("a", NN), lit("10.00", 2, false)), bref("b", ONBEPAALD),
                         lit("0.00", 2, false), ONBEPAALD), ONBEPAALD), ONBEPAALD), "null"},
            // a Let INSIDE an arm: its need may not be forced when that arm is not chosen
            {"sprong van een jutter", attr("sprong", 2, false, ONBEPAALD),
             let("k", inkomen,
                 ite(verg("GROTER", bref("k", NN), lit("10.00", 2, false)), lit("5.00", 2, false),
                     let("z", binair("GEDEELD_DOOR", teller, nuldeler),
                         binair("PLUS", bref("z", ONBEPAALD), lit("1.00", 2, false)), ONBEPAALD),
                     ONBEPAALD), ONBEPAALD), "null"},
            // a §6.5 site in the VALUE of a need only ONE arm reads — its companion carries that arm's guard
            {"takgat van een jutter", attr("takgat", 2, false, ONBEPAALD),
             let("k", inkomen,
                 let("z", binair("GEDEELD_DOOR", teller, gatdeler),
                     ite(verg("GROTER", bref("k", NN), lit("10.00", 2, false)), lit("4.00", 2, false),
                         bref("z", ONBEPAALD), ONBEPAALD), ONBEPAALD), ONBEPAALD), "null"},
        };

        StringBuilder regels = new StringBuilder();
        for (String[] r : rs) {
            if (regels.length() > 0) regels.append(',');
            regels.append(regel(r[0], r[1], r[2], r[3]));
        }
        StringBuilder attributen = new StringBuilder(String.join(",",
                attribuut("inkomen", num(2, false, NN)), attribuut("teller", num(2, false, NN)),
                attribuut("deler", num(0, true, NN)), attribuut("staffel", num(0, true, NN)),
                attribuut("heffingsdeler", num(0, true, NN)), attribuut("beschermdeler", num(0, true, NN)),
                attribuut("takdeler", num(0, true, NN)), attribuut("takopslaggrond", num(2, false, NN)),
                attribuut("nuldeler", num(0, true, NN)), attribuut("gatdeler", num(0, true, NN))));
        for (String naam : new String[]{"aandeel", "gestaffeld", "quotient", "garde", "metlet", "zonderlet",
                                        "dubbelop", "schaduw", "zusters", "premie", "heffing", "bescherm",
                                        "tak", "takdeling", "takopslag", "takketen", "sprong", "takgat"})
            attributen.append(',').append(attribuut(naam, num(2, false, ONBEPAALD)));

        return ("{\"format\":\"uchar-ir-v1\",\"model\":{"
                + "\"objectTypes\":[{\"node\":\"ObjectType\",\"name\":\"Jutter\",\"animate\":true,\"plural\":\"Jutters\","
                + "\"attributes\":[%s],\"characteristics\":[]}],"
                + "\"factTypes\":[],\"parameters\":[],\"domains\":[],\"unitSystems\":[],\"dimensions\":[],"
                + "\"timelines\":[],\"dayKinds\":[]},"
                + "\"rules\":{\"rules\":[%s],"
                + "\"dayKindDefinitions\":[],\"objectCreations\":[],\"allocations\":[],\"consistencies\":[],"
                + "\"uniquenesses\":[],\"factCreations\":[],\"startPointDeterminations\":[],"
                + "\"ruleGroups\":[]},\"diagnostics\":[]}")
                .formatted(attributen, regels);
    }
}
