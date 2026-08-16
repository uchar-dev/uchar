# backends/clingo: the second executor + gate 6 (ref ≡ Clingo)

Goal: the Phase-3 deliverable's first landing — a systematic translator
from regelspraak-ir-v0 documents plus uchar-case-v0 cases to ASP-Core-2,
executed by clingo, with a differential gate. `backends/clingo/` becomes
an OCaml dune project in the workspace (reusing the `ir_v0` loader and,
for IO only, `ref/`'s case loader and value lexing — the *semantics*
run in clingo, nothing of `ref/`'s evaluator is linked). The
translation follows `spec/ASP-TRANSLATION.md`'s conventions (predicates
over instances, roles as argument positions, NAF for negated
characteristic reads — sound because WF-045/046 guarantee
stratifiability) extended with: declared-decimals integer scaling for
exact arithmetic, aggregation as `#count`/`#sum` with the
contributor-skip encoded by the body join, the allocation recurrence in
integer arithmetic (cap, floor-by-integer-division, remainder),
consistency/uniqueness as `inconsistent/2` derivations, dates as civil
day numbers, and the year-bucket temporal total. Constructs outside the
corpus subset fault the translator — fail fast, no silent SKIP. Gate 6
(`scripts/check-clingo.sh`): every `corpus/golden/**/case*.json` runs
through translate → clingo → model check against the same absolute
expectations gate 5 pins for `ref/` — the differential ref ≡ Clingo
over the corpus, shown red then green; an empty case set is a failure.
CI gains the job (clingo via pip); the CLAUDE.md gate list grows.

Acceptance: `sh scripts/check-clingo.sh` green (7 cases, exact); shown
red first; gates 1–5 stay green; every translation deviation or
friction found lands as a finding note in the card's Result (feeding
CONSTRUCTS, per the PLAN's "every node that does not translate cleanly
is still carrying an assumption").

May touch: `backends/clingo/**`, `scripts/check-clingo.sh` (new),
`.github/workflows/gates.yml`, `CLAUDE.md` (gate list), `backlog/**`,
`corpus/golden/README.md` (a differential note if needed),
`ref/dune-project` + `ref/lib/dune` (build metadata only: a
`public_name` so the backend can link ref's IO layer — case loading and
value lexing; no evaluator semantics are linked).

Out of scope: the Drools leg of the differential square (pin-bump
territory); role-binding IR changes (the Phase-3 "relations
first-class" refactor is spec work, a later card); any change to
`spec/**` or `ref/**` semantics.

Result: delivered — the differential square has its second executor.
backends/clingo translates the full corpus subset (characteristic
attribution, computations, #count/#sum folds with the body-join
contributor skip, the proportional allocation with cap/floor/remainder
in scaled integer arithmetic, consistency/uniqueness as inconsistent/2,
the year-bucket temporal total, NAF for negated reads — sound by
WF-045/046) and gate 6 replays all 7 cases against the same absolute
expectations gate 5 pins: ref ≡ Clingo over the corpus, anchored, never
parity-only. Shown red (broken expectation → exit 1; no clingo → exit
2) then green; CI installs clingo via apt (the pip wheel ships no CLI —
a verification lens caught the broken job before it ever ran).
Verification (3 lenses) also forced: a wider scale guard (cap/remainder
scales), generated fail-fast constraints so clingo goes UNSAT exactly
where ref faults or where integer truncation would silently diverge
from floor (negative/empty allocation operands — both reproduced), and
honest claims (the connecting-role derivation is shared ch. 4.1
semantics pending the relations-first-class refactor; clingo's 32-bit
integers bound the exact encoding, reproduced via cents-scaled money).
Recorded findings feeding CONSTRUCTS/Phase 3: (1) the ch. 1.2d
equality/inequality empty asymmetry has no absence-based ASP encoding —
faulted until a case demands an explicit leeg encoding (row 18
adjacent); (2) the negative-weight v0 hole now fails loud on both legs
but with different shapes (ref computes the unclamped mirror value,
clingo refuses) — resolved when the decided v1 clamp lands; (3) bignum
or rational ASP encoding is follow-up for realistic monetary
magnitudes; (4) the temporal total is translated for the year bucket
only. Deliberately left: the Drools leg (pin bump); the
relations-first-class refactor (spec card).
