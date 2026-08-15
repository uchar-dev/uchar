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

Result: `spec/RUNTIME-SEMANTICS.md` ch. 1 (values and emptiness) and ch. 2
(arithmetic and rounding) drafted as v0 mirror with tracked deltas and OPEN
decisions. Ch. 1: the full empty-behaviour tables per context (operators,
dates, text, comparisons incl. the numeric/non-numeric equality asymmetry,
aggregations with the som-vs-totaal split, the two coexisting timeline
rules), the distilled coalescing invariant, and the reopened PLAN §9(7)
decision with the must-not-be-zero list, the FEEL-null alternative, the
open/closed-world boundary named concretely from the ASP hand-translation,
and the error-channel cases (division-by-zero routing left OPEN). Ch. 2:
the exact-rational model with equations and collapse points, per-operator
semantics and precision typing (incl. the division precision under-report,
closed by a candidate WF delta: a possibly-non-terminating value must pass
explicit rounding before a bounded write), the §6.1.2 assignment gate, the
five rounding modes with two fully worked tables (40 cells, independently
recomputed by a verifier), unit arithmetic, clamping, and the
time-proportional part. Verified by two adversarial reviewers against the
source at the pinned commit: all worked-example values confirmed; findings
fixed (34-significant-digit precision misstatement, incomplete inequality
clause, leeg÷leeg vs the must-not-be-zero list, missing dimension-fold and
timeline examples, empty-collection identity vs the invariant, ASP
cross-reference concretes). One item flagged for maintainer judgment: the
fallback-clause surface phrase "… of 0 als die er niet zijn" is quoted as
language syntax (construct keyword), consistent with earlier review
precedent. Deliberately left: ch. 3-8 (skeletons updated with obligations
recorded here); freezing the emptiness decision (maintainer call). This
branch is a PROPOSAL: spec/RUNTIME-SEMANTICS.md is a protected path — the
maintainer merges (fast-forward).
