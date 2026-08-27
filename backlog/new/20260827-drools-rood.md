# The red path closes the square — a drools fault leg in case-v0 and gate 8

Goal: decision V3-b (2026-08-27). Today `expectedFault` carries
`{ref, clingo}`; the Drools leg of the square covers only the green
path. Deliver, stacked on the drools-backend card: (1) an ADDITIVE
optional `drools` member in the case schema's expectedFault (spec/
schema — protected path; every existing case stays valid byte-
identically); (2) the IR-SPEC case-format note updated accordingly;
(3) `scripts/check-rood.sh` gains the Drools leg: every rood case that
carries a drools fragment MUST fail observably in the Drools runner
with that fragment in its report; rood cases whose forms the backend
does not carry are listed with reason (stale list fails); (4) pin the
drools fragment on every rood case whose form Drools carries —
investigate each of the 22, add the fragment where it plays, list the
rest with reason. The Onopgelost/WF-044 guard among them: if the
unsolved-state form is Drools-carriable, its fragment is mandatory.

Acceptance:
- Red-first: show one rood case failing the new leg before its
  fragment is pinned (or the leg red on a stale exclusion), then green.
- Gate 4 (case-schema) green: all 365+ cases validate, old dumps
  byte-identical; gates 1–12 green.

May touch: `spec/schema/uchar-case-v0.schema.json`, `spec/IR-SPEC.md`
(case-format section), `scripts/check-rood.sh`, `corpus/rood/**`
(adding drools fragments only), `backends/drools/**` (runner fault
reporting only).
Out of scope: new rood cases, the toka leg, `spec/CONSTRUCTS.md`
(serial at merge).
