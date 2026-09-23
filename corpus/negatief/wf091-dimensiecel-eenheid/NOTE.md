Violates WF-091 (`spec/IR-SPEC.md` ch. 4.1) on the `DimensionCellRead`
arm, unit facet: a cell read of the dimensioned attribute `Berging.kern`
(dimension `seizoen`, cell `voorjaar`), declared in `kg`, carries a `type`
in `t` — same canonical `kg`, factor 1000. The inner `AttribuutRef` is
FAITHFUL (`kg`), so the `Attribuut_ref` arm has nothing to say; the lie
sits on the cell read's own `type`, which is a use site of the same
declaration because a cell of a dimensioned attribute carries that
attribute's datatype.

    WF-091 FAIL /rules/resolvedRegels/0/waarde/type: unit mismatch:
    dimension-cell read of 'kern' is typed 't' of system 'Gewicht' (factor
    1000 to 'kg'), but the declaration it reads from is 'kg' of system
    'Gewicht' (factor 1 to 'kg')

Why it has to exist: with it, every arm this rule crosses IN FULL — the
attribute reference, the cell read over one, the parameter reference, the
binder reference, the fold source and the rule target — has a unit-facet
case of its own. The cell read is the arm whose own `type` no other rule
reaches: `Dimensie_cel` checks its labels (WF-040) and walks the inner
reference, and a faithful inner reference leaves the cell's numeric facets
uncrossed. Like its domain twins `wf089-dimensiecel-enum` and
`wf090-dimensiecel` this is a **verifier-only** flank: the case format
carries no dimension-cell input, so no executor witness is possible and
the rule is the only fence. The arm has no positive golden witness either
— no golden or rood document carries a unit-bearing `DimensionCellRead`
(measured) — the honesty note WF-089, WF-090 and WF-091 all record for it.

Falsifiable, facet-exact and arm-exact: neuter the unit facet in the
identity engine (`use_site_identity`) and this case degrades to `residual
structure mismatch` — still WF-091, still red, the backstop doing its work
— while calling `use_site_domain` instead of `use_site_identity` in the
`Dimensie_cel` arm of `walk_expr` turns this case alone clean, every other
WF-091 case and both dimension-cell domain cases staying red (measured).
