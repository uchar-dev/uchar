# Harness guards: no-op, unsolved-state, and the expected-fault case form

Goal: the two standing harness guards CLAUDE.md lists as still to land,
plus the case form that makes red paths gate material. (1) **No-op
guard** (gate 7): the reference runner gains a `--noop` mode that skips
evaluation and checks expectations against the raw input; the gate
demands that every golden case refutes a do-nothing executor, with the
cases that legitimately survive (they pin defeasible standing) in an
explicit, reason-carrying survivor list that fails when stale. (2)
**Expected-fault case form + red corpus** (gate 8): `uchar-case-v0`
gains an `expectedFault {ref, clingo}` alternative to `expected`; a new
zone `corpus/rood/` holds verifier-clean documents whose cases must
fault observably in *both* executors, substring-pinned. First
inhabitants: the WF-044 unsolved-state witness (an `Onopgelost` node
reached in evaluation — the third standing guard), the to-one
second-filler red path from the creation card, and the ch. 8.4
uncovered-vintage session-start error.

Acceptance: gates 1–6 green with `corpus/rood` folded into the schema,
verifier, and case-schema globs (gate 4 additionally pins the zone
discipline: golden carries `expected`, rood carries `expectedFault`);
`scripts/check-noop.sh` and `scripts/check-rood.sh` green, each shown
red first (a no-op run over the corpus fails; an empty rood set fails);
CI runs both; CLAUDE.md's gate list grows to eight and its
"still to land" sentence goes.

May touch: `ref/bin/runner.ml`, `backends/clingo/bin/clingo_runner.ml`,
`ref/lib/case.ml`, `backends/clingo/lib/translate.ml` (explicit WF-044
fault arm), `spec/schema/uchar-case-v0.schema.json`,
`spec/verifier/lib/loader.ml` (a mirror bug the red case exposed: the
loader matched the expr/conditie marker as "Onbekend" where the pinned
emitter writes "Onopgelost"), `scripts/` (two new gates + glob widening
in four existing), `corpus/rood/` (new), `corpus/README.md` (zone row),
`.github/workflows/gates.yml`, `CLAUDE.md` (gate list),
`WAY-OF-WORKING.md` (zone list), `backlog/`.

Out of scope: ref/test files; new WF rules; red paths that only one
executor can observe (recorded instead); corpus/negatief changes.

Result: landed in full; the gate set grows from six to eight. Gate 7
(check-noop): the runner's --noop mode replays expectations against the
raw input; 29 golden cases refute a do-nothing executor, and the three
legitimate survivors are pinned with reasons (two defeasible-standing
witnesses, one input-true consistency); shown red first (without the
list, exactly those three are reported), and after the verification
round only exit 1 counts as refutation — a load error refutes nothing.
Gate 8 (check-rood): the expectedFault {ref, clingo} case form
(schema + shared loader, empty fragments refused in both layers), three
red inhabitants (WF-044 unsolved state — the third standing guard;
the ch. 5.5 to-one second filler; the ch. 8.4 uncovered vintage), both
runners verifying fragment and fault themselves; zone discipline
enforced structurally in gate 4.

The verification round (two opus lenses) hardened before merge: the
UNSAT catch-all became named viol/2 witnesses with a diagnosis re-run,
so a red fragment pins the *reason* a program is unsatisfiable
(demonstrated: the false diagnosis "not stratified" now fails against
the to-one case); the zone check moved from a text grep to parsed keys;
gate 7 distinguishes exit 1 from exit 2; gate 3's message no longer
calls a rood document "golden"; the WF-044 index row carries its
artifacts. Bycatch worth naming: the red WF-044 case exposed a loader
mirror bug — the expr/conditie honest marker is "Onopgelost" on the
emitted wire ("Onbekend" is the datatype sentinel), but the loader
matched "Onbekend"; the schema (gate 2) rejected the wrongly-named
node in the new red document, which is what surfaced the divergence;
the loader now matches the emitter.

Recorded, deliberately accepted: the survivor list is a review-gated
escape hatch by design (scripts/ is a protected path — pinning a
neutered case would need a reviewed diff of the reason-carrying list);
Case.contains accepts an empty needle but both schema (minLength 1)
and loader refuse empty fragments; two golden cases sit one
expectation away from no-op survival (a future weakening lands them
loudly in the survivor check, which is the mechanism working).
