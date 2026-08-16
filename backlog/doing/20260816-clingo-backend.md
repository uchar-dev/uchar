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
