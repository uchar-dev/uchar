# RUNTIME-SEMANTICS ch. 1–2: values, emptiness, arithmetic (proposal)

Goal: Draft chapters 1 (values and emptiness — the "empty = 0" design
question opened with its exception cases, per `spec/PLAN.md` §9 decision 7)
and 2 (arithmetic and rounding) of `spec/RUNTIME-SEMANTICS.md` as equations
and tables plus worked examples with concrete expected values, mined from
`RegelSpraakRuntime` and the decision records in `regelspraak-java`'s
`docs/grammar-divergences.md` at the pinned commit. Genuinely open decisions
are stated as OPEN with their cases, not silently frozen.

Acceptance: every semantics clause has a worked example with concrete
expected output (the future `ref/` unit tests, verbatim); the empty-value
chapter lists the cases where empty must *not* be zero;
`scripts/check-self-contained.sh` passes.

May touch: `spec/RUNTIME-SEMANTICS.md` (protected path — proposal; the
maintainer merges), `backlog/`

Out of scope: freezing the emptiness decision (maintainer call); chapters
3–8 (follow-up cards).
