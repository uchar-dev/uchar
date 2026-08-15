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
