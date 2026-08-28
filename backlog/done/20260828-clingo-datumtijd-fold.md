# clingo: the datum-tijd ordering fold — EERSTE/LAATSTE over pair terms

Goal: dt_term (the datum-tijd pair-term lowering in
backends/clingo/lib/translate.ml, catch-all around translate.ml:2098-2102 on
the pre-card-1 line numbers) gains the fold arm: a listed EERSTE/LAATSTE
(Functie kind=EERSTE/LAATSTE, >=2 operands, typ DatumTijd) over datum-tijd
operands. ref is the meaning and is NOT touched: agg_eerste/agg_laatste =
agg_extreme over scalar_cmp (ref/lib/arith.ml:437-453) — empty contributors
are skipped, all-empty yields Empty, and the fold orders datum-tijd values
totally (civil day first, ms-of-day as tie-break — one instant is one
(D,MS) pair, so value-level min/max is well-defined).

Encoding: the pair term dt(D,MS) cannot be linearised into one integer
(D*86400000+MS exceeds clingo's int range — exactly why the pair term
exists). Lower the fold lexicographically: first the extreme over D, then
the extreme over MS among the operands that carry that D — two-stage
aggregation over ground operand tuples, or an equivalent fenced encoding
that provably stays inside the integer range. Presence guard: an operand
whose value is empty contributes nothing; all-empty derives nothing (leeg).
Mixed-granularity operands (a bare datum next to a datum-tijd) follow
whatever ref does — check scalar_cmp and mirror it exactly, or keep that
mix faulting loudly if ref's behaviour has no witness. Anything dt_term
still cannot carry (datum-met constructor, navigated datum-tijd read into
the fold, set-typed datum-tijd folds) keeps faulting loudly with the
existing fragment.

One rights-free, own-authored Jutterswet golden (hand-computed absolute
values from RUNTIME-SEMANTICS ch. 3.8, noop-refuting) witnesses: EERSTE and
LAATSTE over >=2 datum-tijd operands, including (a) a pair that differs only
in ms-of-day on the same civil day (the tie-break leg is load-bearing), (b)
a pair that differs in civil day, and (c) an empty-operand corner (one
operand empty -> skipped; all-empty -> leeg) if representable in one golden
— otherwise two case files on the one golden. The toka pin
scenario-toka-22-datum-tijd-berekening moves FAULT->PASS in
toka/cases/parity.manifest (clingo 28 -> 29 of 30). spec/ASP-TRANSLATION.md:
S6a/S8 gain the fold paragraph; the S19 sentence about datum-tijd operands
in folds shrinks to exactly the residue (ordering/equality comparisons if
they stay untranslated, and whatever else remains fenced). S19 stays the
highest section and shrinks only by translation.

Acceptance:
- Red-first: BEFORE the change, the clingo runner on the toka-22 pair exits
  non-zero matching the pinned fragment, and the new golden faults the same
  way (evidence summarised without casus content).
- After: bash scripts/check-toka-parity.sh green with the toka-22 pin at
  PASS|- (clingo 29 PASS / 1 loud fault: the non-numeric operand class).
- The new golden passes gates 2-8; gate 12 plays it or carries a
  reason-carrying, evidence-backed exclusion (never silent) — NOTE:
  datum-tijd inputs may already fall under gate 12's per-form auto-refusal
  (temporal input); check what jutterswet-getijdenpaar does there and land
  in the same honest bucket.
- All twelve gates green in the worktree.
May touch: backends/clingo/**, corpus/golden/** (new golden only),
toka/cases/parity.manifest, spec/ASP-TRANSLATION.md, scripts/check-drools.sh
(ONLY for a reasoned exclusion of the new golden), backlog/.
Out of scope: ref/, spec/IR-SPEC.md, spec/RUNTIME-SEMANTICS.md, spec/schema/,
spec/verifier/, frontends/**, backends/drools/** (code), the remaining fault
class (non-numeric operand / unit algebra / Onbekend — the standing
maintainer question), scripts/check-noop.sh survivor entries (design the
cases to refute noop), spec/CONSTRUCTS.md, spec/PLAN.md, any toolchain
commit.

## Result (2026-08-28 — review round handled, card closed)

Review round: two independent lens reports found no CRITICAL, no MAJOR. One
sensible minor fixed with the narrowest change; the remaining minors
motivated-rejected.

- FIXED (Lens 2, prose accuracy) — the dt_term catch-all fault fragment
  (backends/clingo/lib/translate.ml) read "a fold (eerste/laatste) ... is a
  loud flank", which over-claims now that a listed >=2-operand datum-tijd
  fold is carried. Tightened to "a set-typed fold (eerste/laatste)",
  mirroring ASP-TRANSLATION.md S6a's exact residue wording. No test, manifest
  or gate asserts the old string (toka-22 moved off it; toka-45 uses a
  different fragment), so the change is inert to the differential — rebuilt,
  gates 6 and 11 re-green.
- REJECTED (Lens 1/2) — no dimensieSelectie guard on the new fold-arm: it
  mirrors expr_term's existing numeric fold-arm (which likewise guards only
  `filter`), a consistent pre-existing pattern, no new opening, no corpus
  case, and gate 12 auto-refuses dimensioned input. Narrowest change is none.
- REJECTED (Lens 1) — the >=2-operand match drops a degenerate 1-operand
  listed fold to the catch-all (loud, where ref would return the single
  value): frontends never emit a 1-arg eerste/laatste, it is a loud fault
  (never silently wrong), and gate 6 would catch any corpus case. No
  reachable defect; narrowest change is none.

All twelve gates rerun green in the worktree (opam env; regelspraak jars in
frontends/regelspraak/lib/; drools shaded jar via mvn -o package):
1 self-contained PASS · 2 ir-schema PASS (150) · 3 verifier PASS (150 golden,
161 negatief) · 4 case-schema PASS (375) · 5 oracle PASS (353) · 6 clingo
PASS (353, ref=clingo) · 7 noop PASS (320 refute, 33 pinned survivors, none
new) · 8 rood PASS (22) · 9 catala PASS (169 checks, 11 goldens regenerate) ·
10 regelspraak PASS (130 tests, 19 goldens re-emit) · 11 toka-parity PASS —
clingo 29 PASS / 1 loud fault (toka-45 non-numeric operand) / 0 silent
diverge, toka-22 FAULT->PASS|- · 12 drools PASS — new golden
jutterswet-hoogwaterpaar PLAYED 3 (ref=clingo=Drools, full square; not
excluded), played 51 / auto-excluded 72 / value-divergence 4 / no-case 1.

Red-first (from the build round, unchanged): toka-22 pre-change exited 1 on
the dt_term catch-all fragment; the new golden's case1 faulted identically
against the pre-change translator. Both restored before the fold arm landed.

No merge, no push.
