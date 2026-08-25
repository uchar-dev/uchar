# C2 — the articulated eerste/laatste fold, ORDERED as v0 computes it

Goal: stress-test instance-fold sub-form C2: an articulated value fold
(EERSTE/LAATSTE over ≥2 own scalar attributes) carrying a vestigial
elementObjecttype falls into ref's "unsupported instance-fold source
shape" arm (eval.ml:824-828 demands element_objecttype = None). AND the
exploration found a latent silent divergence waiting behind it: ref's
Arith.agg_eerste/agg_laatste (arith.ml:433-434) are POSITIONAL (first
filled in list order) where v0 production is ORDERING ("<"/">" — "de
eerste van 5 en 3" is 3, not 5). Mirror-then-diverge decides this
without a new maintainer question: v0's ordering semantics is the
mirror. Deliver: (1) fix agg_eerste/agg_laatste to the ordering fold
(empty operands skipped; all-empty → empty — verify the exact v0
empty rule in the runtime and pin it); FIRST verify no existing corpus
case pins the positional behaviour (if one does, STOP and report — that
is a divergence discovery, not a rewrite); (2) widen the articulated
arm to match on argument SHAPE instead of element absence; (3) the
matching clingo translation (min/max over the argument terms with the
empty-skip discipline); (4) RUNTIME-SEMANTICS worked example (mixed
values, an empty operand, the all-empty corner); (5) golden with
hand-computed cases (a date pair and a numeric pair; the all-empty
corner), through both executors.

Acceptance:
- Red-first: the verifier-clean articulated repro faults in today's
  ref (quoted); after the change both executors replay the golden and
  agree (gates 5–6); the ordering semantics is pinned by a case where
  positional and ordering answers DIFFER.
- All nine gates green with counts.

May touch: `spec/RUNTIME-SEMANTICS.md` (worked example),
`ref/` (arith.ml, eval.ml), `backends/clingo/`,
`spec/ASP-TRANSLATION.md`, `corpus/golden/jutterswet-*`.
Out of scope: sub-form C1 (lands earlier in this chain), new WF rules
(the shape is already schema-legal; add one only if the verifier must
reject a genuinely meaningless combination — justify it),
`spec/CONSTRUCTS.md` (serial at merge).
