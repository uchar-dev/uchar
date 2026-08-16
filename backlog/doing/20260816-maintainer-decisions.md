# Maintainer decisions: freeze empty, uniform allocation, row 30, full-Catala goal, licenses

Goal: record the decisions the maintainer ratified on 2026-08-16, each in
the place where its OPEN currently lives, plus the license files.
(1) RUNTIME-SEMANTICS ch. 4.6 erratum: the cap-degrade example surfaces
one §9.5 error *per degraded cap* (two there), per ch. 4.3 + 5.6.
(2) Allocation edge rules decided (v1 deltas): a negative proportional
weight adopts the ordered clamp uniformly (counts as 0 + §9.5 error);
"empty pot ⇒ no distribution" becomes uniform across the three methods.
(3) The empty-value decision (PLAN §9(7)) is frozen: the ch. 1.2 mirror
with the ch. 1.3 coalesce whitelist is v1 semantics; empty-as-unknown
(FEEL null propagation) is a non-goal; numeric-zero division routes to
the §9.5 channel in v1; the open/closed-world split is expressed through
Phase-4 defaults, not operator behaviour.
(4) Row 30 "is gevuurd" is redefined declaratively — "the condition of X
holds at the fixpoint" — admitted in the RegelSpraak dialect, never the
core; edge divergences from the pin's firing-marker lowering route to
differential triage.
(5) Full-Catala expressibility is adopted as the Phase-5 goal: general
folds and function values (and sum types with match) become planned
kernel additions designed at Phase 5, no longer a subset stance — PLAN
Phase 5 and CONSTRUCTS rows 10/36 updated; no node design now.
(6) Licenses: Apache-2.0 for code (verifier, ref, backends, scripts),
CC-BY-4.0 for the spec and corpus text; PLAN §9(9)/(10) updated —
visibility decided as public at latest at Phase-1 exit. A short
top-level README states what the repo is and the license split.

Acceptance: all five gates green; every decided OPEN cell carries the
decision (no stray "OPEN" left for decisions 7/row 30/allocation holes);
LICENSE and LICENSE-CC-BY-4.0 present with canonical texts; PLAN status
line bumped with a one-line v5 change note.

May touch: `spec/RUNTIME-SEMANTICS.md`, `spec/CONSTRUCTS.md`,
`spec/PLAN.md`, `LICENSE` (new), `LICENSE-CC-BY-4.0` (new), `README.md`
(rewrite of the existing root README: license section + status refresh,
prior content restored), `backlog/**`; in `ref/` only decision-locking
text: the WV-4.6-e erratum comment + assertion (= 2, behaviour ref/
already exhibits) and the two stale "row 30 is OPEN" strings
(`ref/test/test_ref.ml`, `ref/lib/eval.ml`, `ref/README.md`).

Out of scope: behaviour changes in `ref/` or the verifier (the decided
v1 deltas take effect with the v1 schema work, tracked in CONSTRUCTS);
`spec/IR-SPEC.md`; corpus changes.
