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
