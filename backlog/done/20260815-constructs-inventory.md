# Complete the CONSTRUCTS.md inventory (Phase 0)

Goal: The in-repo Phase-0 deliverable (`spec/PLAN.md` §6): complete
`spec/CONSTRUCTS.md` from its sources — every `Expr`/`Conditie` node and
every `Resolved*` top-level form in `regelspraak-java` at the pinned commit
`b85dff3`, the model layer (datatypes, units, dimensions, timelines,
parameters), every MARKER/SKIP reason in the coverage manifest and the
`Onopgelost` inventory, the remaining LegalRuleML requirement rows, and the
DMN hit policies — so that every construct carries an explicit v1 verdict
(`core`/`dialect`/`sugar`/`later(…)`/`non-goal`), the kernel-or-sugar answer,
and its v0→v1 delta.

Acceptance: every permitted `Expr`/`Conditie` subtype and every
`ir/Resolved*` form at the pinned commit appears in the matrix or inventory;
every row has a non-empty Verdict (v1); no verbatim RegelSpraak-specification
text is quoted (section numbers and construct names only);
`scripts/check-self-contained.sh` passes.

May touch: `spec/CONSTRUCTS.md`, `backlog/`

Out of scope: normative `IR-SPEC.md` content; changes to `regelspraak-java`;
corpus files; `spec/PLAN.md` (unless scope itself changes).

Result: `spec/CONSTRUCTS.md` completed: a 37-row scope matrix (the 18
existing rows refreshed with mined findings, 19 new rows), the full node
inventory of `regelspraak-java` @ `b85dff3` (12 `Expr` subtypes, 9 `Conditie`
subtypes, every `Resolved*` form + `Geldigheid`, the model layer incl. the
`Datatype` sum), the coverage-manifest mechanism with its marker/skip
families mapped to matrix rows, and the ~24-site `Onopgelost` production
inventory in four categories (feeding the row-16 delta: structured reasons).
Verified by three adversarial reviewers: completeness vs source (no missing
subtype or top-level form), consistency vs `spec/PLAN.md` §9 (all decided
positions honoured; the one found contradiction — `Dimensie` core vs dialect
— resolved as an explicit declaration/use split in row 28), rights
(publishable: no reproduced specification text, no machine-local paths).
Deliberately left: per-builtin semantics (Phase 1), the scenario→corpus-schema
mapping (corpus workstream), exhaustive FEEL function enumeration (row 15
keeps decision tables as frontend sugar). Verdicts are session proposals for
maintainer review.
