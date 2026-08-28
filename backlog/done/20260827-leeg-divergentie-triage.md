# Triage the seven gate-12 value divergences — port bug or genuine v0 finding?

Goal: gate 12 (`scripts/check-drools.sh`) carries seven goldens in its
reason-carrying value-divergence list (the leeg-semantics family:
leeg-valt-door traced to the unconditional werkBij write,
leeg-in-condition, and kin). Nobody has yet established, per golden,
WHICH of three explanations holds — and they demand different actions:

(a) **A port/loader/runner bug in backends/drools** — our copy, loader
    or CaseRunner mishandles the form. Action: fix it (backends/drools
    is not protected), the golden moves from value-divergence to
    played, gate 12's counts shift.
(b) **A genuine v0 production-semantics divergence** — the REAL
    toolchain Drools also computes the deviating value. That is a
    DISCOVERY: the spec (ref) is the norm, so the golden stays
    excluded, but its reason line gets the hard evidence and the
    finding is recorded honestly (this is the divergence class the
    whole programme exists to surface).
(c) **A case/driver artefact** — the expectation comparison or bean
    mapping is at fault, not the semantics. Action: fix the runner.

METHOD (the decisive experiment, per golden): replay the same program +
case through the ORIGINAL toolchain's Drools machinery (read-only:
/Users/yeb/Developer/yhavinga/regelspraak-java, its scenario/engine
paths, work in /tmp) and compare: original agrees with our port →
genuine v0 divergence (b); original agrees with ref's pinned value →
our port is buggy (a). Where the golden has no .rgs source, translate
the ir.json form to an equivalent minimal .rgs for the original chain
— document the translation and its fidelity, or if untranslatable,
reason from the runtime code (file:line) and say so explicitly.

Acceptance:
- Every one of the seven carries a verified verdict (a/b/c) with the
  decisive evidence quoted (original-toolchain output or code trace).
- (a)/(c) findings are FIXED; those goldens play (gate 12's played
  count rises; the exclusion list shrinks and its staleness check
  proves it).
- (b) findings keep their exclusion with the evidence written into the
  reason and a summary in the card Result — candidates for a follow-up
  spec/v1-delta card are named, not built.
- All twelve gates green; red-first where a fix lands (show the
  pre-fix divergence output).

May touch: `backends/drools/**`, `scripts/check-drools.sh` (the
exclusion list and its reasons), `backlog/`. The toolchain and /tmp
are read-only working space.
Out of scope: `spec/**` (a (b)-finding produces a card candidate, not
a spec edit), `ref/`, `corpus/**` (the pinned expectations are the
norm — never adjust an expectation to make Drools pass),
`spec/CONSTRUCTS.md`, any toolchain commit.

---

Result (2026-08-28, card leeg-divergentie-triage):

VERDICT: all seven gate-12 value-divergences are class (b) — GENUINE v0
production-semantics divergences, NOT port bugs (a), NOT driver artefacts (c).
Established by the decisive experiment per golden: replay of an equivalent
minimal .rgs through the ORIGINAL regelspraak-java toolchain (read-only,
/tmp work), firing BOTH its DrlTranspiler oracle AND its ExecModelTranspiler
over hand-fired facts. In every case the original toolchain agrees with our
PORT and NOT with ref/clingo's pinned value. None of the seven carries a
bron.rgs, so each mechanism was reproduced with a minimal equivalent over the
toolchain's own SYNTHETIC test POJOs (Passagier/Route/Pot/Deelnemer/Vlucht —
no TOKA content); fidelity noted per row. (c) is ruled out everywhere: an
independent harness with direct getter reads reproduces the same deviating
value, so the semantics genuinely differ — it is not our comparison/bean map.

CONSEQUENCE: nothing is fixed, no golden moves to played, gate 12 counts are
UNCHANGED (played 41, auto-excluded 72, value-divergence 7, no-case 1). The
exclusion reasons in scripts/check-drools.sh are HARDENED with the replay
evidence, and TWO mis-described mechanisms are corrected: kustwacht and
voorwaardedeling are NOT leeg-operand issues.

Per golden (mechanism -> decisive replay -> pre-triage divergence output):

1. anchor/jutterswet-waardestelling (b) — a gelijkstelling with a LEEG source
   OVERWRITES the pre-filled doel with null; ref preserves it (leeg valt door).
   Replay prijs:=basistarief[leeg] over prijs=40: DRL=null AND exec=null.
   Pre-triage: "attribuut geschatte waarde: verwacht 40.00, werkelijk leeg".

2. anchor/jutterswet-waardering-v0vorm (b, exec-model-specific) — the
   leeg-source gelijkstelling (writes null) oscillates against the
   INITIALISATIE (refills 25); the EXEC-MODEL never reaches a fixpoint
   (fireAllRules hits 100k) — our port mirrors it and reports "geen fixpunt".
   The toolchain DRL-oracle settles at 25 (=ref) [fired=2]; the toolchain
   exec-model does NOT [HIT-LIMIT]. Same leeg-write root as #1.
   Pre-triage: "geen fixpunt (kringverwijzing?)".

3. anchor/jutterswet-kustwacht (b) [reason CORRECTED — NOT a leeg operand] —
   NEGATION-as-failure is unstratified: rustwacht (= geen risicopost) fires
   before the positive kenmerk risicopost is derived and is never retracted,
   so it reads present though the post IS a risicopost. Replay "reis geen
   internationaal" with the reis IS internationaal: DRL=true AND exec=true
   [fired=2]. Pre-triage: "kenmerk rustwacht: verwacht afwezig, werkelijk
   aanwezig".

4. jutterswet-borgvlag (b) — leeg ONGELIJK gevuld reads WAAR (Tabel 44 / Java
   null-ne-x) so betwist fires; ref pins it absent (leeg propagates). Replay
   of the EIGEN attribuut-ne-literal cell (Tekst + Numeriek): DRL=true AND
   exec=true; the toolchain's own PHASE_TEKST_ATTR fixture pins "ONGELIJK
   one-leeg -> WAAR (Tabel 44)". geborgd is a Booleaan attr the port maps to a
   nullable Boolean (leeg=null), same cell. Pre-triage: "kenmerk betwist:
   verwacht afwezig, werkelijk aanwezig".

5. jutterswet-vondstetiket (b) — same cell as #4 for Tekst: leeg ONGELIJK the
   literal zeevondst fires omstreden. Replay: DRL=true AND exec=true.
   Pre-triage: "kenmerk omstreden: verwacht afwezig, werkelijk aanwezig".

6. jutterswet-gelijkdeling (b) — a verdeling with NO recipients still fires the
   onverdeelde-rest rule (sum of shares = 0), writing restant = totaal; ref
   pins restant leeg. Replay (Pot totaal=40, no Deelnemer): DRL=40 AND exec=40.
   Pre-triage: "verwacht leeg, werkelijk 40".

7. jutterswet-voorwaardedeling (b) [reason CORRECTED — NOT leeg] — the verdeling
   VOORWAARDE is dropped: neither transpiler reads v.voorwaarde()
   (DrlTranspiler.verdeling L673-768; ExecModelTranspiler.verdeling L4420,
   byte-identical to our port's), so the split runs even when the gate is false.
   Replay (naar rato, voorwaarde totaal>=1000 FALSE, totaal=40): DRL=40 AND
   exec=40; control totaal=2000 also distributes. A vrijgavescore of 7 in the
   golden is a REAL value (7>=10 false), not leeg. Pre-triage: "verwacht leeg,
   werkelijk 40; verwacht leeg, werkelijk 50".

Follow-up v1-delta card candidates (NAMED, not built):
 - leeg-source assignment is a no-op (covers #1, #2).
 - a leeg-operand comparison yields leeg / drops the rule (covers #4, #5).
 - stratify negated-kenmerk reads (covers #3).
 - a no-recipient verdeling restant is leeg (covers #6).
 - gate a verdeling on its voorwaarde (covers #7).

Gates: all twelve green. Gate 12 unchanged (41/72/7/1). Red-first note: no
backends/drools code change lands (all seven are b), so there is no red->green
fix transition; the seven pre-triage divergence outputs above ARE the baseline.

Left deliberately: no backends/drools code change — a port that faithfully
mirrors v0 is the correct state; every fix above is a tracked v1-delta, out of
this card's scope and out of scope per the card (spec/**, ref/, corpus/**,
spec/CONSTRUCTS.md, any toolchain commit). scripts/check-drools.sh is a
protected path: this branch is the proposal for the maintainer to merge.
