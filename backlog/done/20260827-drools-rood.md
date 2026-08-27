# The red path closes the square — a drools fault leg in case-v0 and gate 8

Goal: decision V3-b (2026-08-27). Today `expectedFault` carries
`{ref, clingo}`; the Drools leg of the square covers only the green
path. Deliver, stacked on the drools-backend card: (1) an ADDITIVE
optional `drools` member in the case schema's expectedFault (spec/
schema — protected path; every existing case stays valid byte-
identically); (2) the IR-SPEC case-format note updated accordingly;
(3) `scripts/check-rood.sh` gains the Drools leg: every rood case that
carries a drools fragment MUST fail observably in the Drools runner
with that fragment in its report; rood cases whose forms the backend
does not carry are listed with reason (stale list fails); (4) pin the
drools fragment on every rood case whose form Drools carries —
investigate each of the 22, add the fragment where it plays, list the
rest with reason. The Onopgelost/WF-044 guard among them: if the
unsolved-state form is Drools-carriable, its fragment is mandatory.

Acceptance:
- Red-first: show one rood case failing the new leg before its
  fragment is pinned (or the leg red on a stale exclusion), then green.
- Gate 4 (case-schema) green: all 365+ cases validate, old dumps
  byte-identical; gates 1–12 green.

May touch: `spec/schema/uchar-case-v0.schema.json`, `spec/IR-SPEC.md`
(case-format section), `scripts/check-rood.sh`, `corpus/rood/**`
(adding drools fragments only), `backends/drools/**` (runner fault
reporting only).
Out of scope: new rood cases, the toka leg, `spec/CONSTRUCTS.md`
(serial at merge).

Result (landed on `card/drools-rood`, stacked on `card/drools-backend`;
protected-path proposal — the maintainer merges ff-only):

- **(1) Schema, additive.** `expectedFault` gains an OPTIONAL `drools`
  member (`required` stays `{ref, clingo}`). Gate 4 green: 365 case
  files validate; the 364 untouched cases stay byte-identical.
- **(2) IR-SPEC.** The WF-044 gate-8 note now describes the additive
  Drools leg and is honest that Onopgelost is *not* Drools-carried
  (its reaching rule does not lower — a transpiler SKIP), so it is
  listed with a reason, never Drools-fragment-pinned.
- **(3) Gate 8 Drools leg** (`scripts/check-rood.sh`). Each rood case
  either fault-pins its `expectedFault.drools` fragment or is listed
  with a reason; stale-guarded three ways (a listed case that now
  faults, a listed case that is gone, a case that is neither pinned nor
  listed all fail — all three verified live). Backend side: `CaseRunner`
  extracts a shared `build()` (green path verbatim — gate 12 still 41
  played) and adds `runRood()`; `DroolsRoodMain` drives it (FAULT /
  EXCLUDE / NOFAULT). Conditional on java + python3 + the shaded jar
  (the gate-9 pattern): runs in full where present, skips loudly
  otherwise — the hosted gate-8 runner carries no JDK and `.github/` is
  out of this card's scope, so the leg is the reviewer's pre-merge duty.
- **(4) Per-case investigation of all 22.** Exactly ONE is
  Drools-carriable: `division-abs-by-numeric-zero` faults at fire with
  `Breuk deling door 0` (gedeeld-door-abs through `Breuk.delen`), pinned.
  The other 21 are not carried and listed with reasons: 6 EXCLUDE via
  the loader (list/struct datatypes, temporal attribute inputs, the
  TemporalRead vintage node, an underivable role reference), 5 EXCLUDE
  as objectcreatie/transpiler-SKIP (ambiguous-backlink, finite-chain,
  runaway-recursion, division-by-numeric-zero, near-side-rolcheck,
  wf044-onopgelost), 4 NOFAULT where the v0 backend does not enforce the
  red-path constraint (input/param over-precision, decimal-nonterminating
  — no §6.1.2 write-precision guard, DECIMAL64 collapse — and the
  ambiguous fold role name). The WF-044/Onopgelost guard resolves to
  *not carriable* → listed, not mandatory.

Red-first evidence: with the fragment unpinned and the case unlisted, the
completeness guard fired (`FAIL: division-abs-by-numeric-zero carries no
drools fragment and is not in the Drools exclusion list …`, exit 1);
pinning the fragment turned it green (`Drools leg PASS (1 fault-pinned,
21 not-carried with reason)`).

Gates 1–12 all green (12 = 41 played unchanged; 8's Drools leg 1+21;
4 = 365 cases). Left deliberately: only 1 of 22 carriable — the other 21
are honest not-carried outcomes of the v0 backend's current form
coverage, not a gap this card widens (`backends/drools` scope was
"runner fault reporting only"). A follow-up that broadens the backend's
form coverage would reclassify some NOFAULT/EXCLUDE cases into
fault-pins; the stale guard will demand it. Not touched: `.github/` (a
CI leg for the Drools rood gate is a maintainer decision — noted, not
taken).
