# Multiplicative operators enter the translated ASP fragment

Goal: `Maal`, `Gedeeld_door` and `Percentage_van` — the last everyday
operators on which the clingo backend still faults with "no corpus case" —
translate to ASP-Core-2, mirroring `ref/lib/arith.ml` exactly. Maal and
percentage-van coalesce BOTH operands (ch. 1.2a); percentage-van needs no
division at all (the /100 folds into the scale). Gedeeld-door carries its
three edges: an empty dividend yields 0, an empty divisor under a filled
dividend deposits a §9.5 channel fact (`inconsistent/2`) beside an empty
result, and a numeric zero divisor is a hard fault (`viol/2` witness, so
the UNSAT diagnosis names the reason). Integer truncation must never
silently diverge from ref's exact rationals: a division result that does
not land exactly on its scale derives a `viol/2` witness instead of a
truncated value, and the mandatory-rounding practice is served by an
`Afgerond`-over-division arm computed exactly in integers per rounding
mode. New anchor goldens pin absolute values for every edge; a red case
pins the zero divisor observably in both executors.

Acceptance: all eight gates green (`bash scripts/check-*.sh`); the new
golden cases replay differentially (gates 5 and 6); the red zero-divisor
case fails observably in both executors with its pinned fragments
(gate 8); `dune test` green in `ref/`, `spec/verifier/`,
`backends/clingo/`.

May touch: `backends/clingo/lib/translate.ml`,
`backends/clingo/bin/clingo_runner.ml` (only if fault reporting needs it),
`corpus/golden/anchor/jutterswet-<new>/`, `corpus/rood/<new>/`,
`spec/ASP-TRANSLATION.md`, `spec/CONSTRUCTS.md`, `spec/IR-SPEC.md`
(operator-index notes only, if any), `backlog/`.

Out of scope: `Gedeeld_door_abs` and `Tot_de_macht` in clingo (no corpus
demand yet); unit-conversion paths (rate cancellation — the translator
guards them with a fault); any wire/schema change; any `ref/` change.
