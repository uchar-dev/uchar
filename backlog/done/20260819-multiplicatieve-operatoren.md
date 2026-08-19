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
`corpus/golden/anchor/jutterswet-<new>/`, `corpus/golden/jutterswet-<new>/`
(the emitter golden), `corpus/rood/<new>/`,
`spec/ASP-TRANSLATION.md`, `spec/CONSTRUCTS.md`, `spec/IR-SPEC.md`
(operator-index notes only, if any), `backlog/`.

Out of scope: `Gedeeld_door_abs` and `Tot_de_macht` in clingo (no corpus
demand yet); unit-conversion paths (rate cancellation — the translator
guards them with a fault); any wire/schema change; any `ref/` change.

Result: Maal, Gedeeld_door and Percentage_van execute in clingo
(ASP-TRANSLATION ch. 6b): maal/percentage-van multiply coalesced scaled
integers (the /100 folds into the scale — no division anywhere);
gedeeld-door carries all three §6.5 edges (empty dividend → 0 even under
a zero divisor, empty divisor → §9.5 channel fact beside leeg, numeric
zero → viol/2, UNSAT naming the reason) plus an exactness guard — a
remainder is a loud viol, never a truncated value; Afgerond-over-division
rounds the true quotient per mode (non-negative operands; richting-nul
needs no fence); a standalone Afgerond scales exactly or divides the
surplus away. The adversarial round found and fixed two real defects:
value-expression faults now fire only under the enclosing rule's
voorwaarde (case4 pins a zero divisor under a false condition staying
consistent), and precision fits against the write target rather than
max(sl,sr) (case5 pins 10/4 = 2.50 on a 2-decimal target and 1.5×2.0
downscaling to 3.0). Anchored absolutely by jutterswet-vindersloon
(5 cases, every empty cell, the channel edge, fractional cents),
differentially by the bergloon emitter golden (pin fe46e49 wire shapes,
including the Numeriek-typed percentage literal), red-pinned by
division-by-numeric-zero. Deliberately left: Tot_de_macht and
Gedeeld_door_abs still fault in clingo (no corpus case); unit-carrying
multiplicative operands are refused (ref's conversion factors have no
integer encoding); rounded roots untranslated; the non-exact-division
and does-not-fit viol fences are inherently witness-less in golden
(they exist to fail loudly, recorded as loud asymmetries in ch. 6b).
