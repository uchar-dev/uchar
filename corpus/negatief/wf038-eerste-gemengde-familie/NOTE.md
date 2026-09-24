Violates WF-038 (`spec/IR-SPEC.md` ch. 4.6), the **cross-operand** half
of the ordering-fold clause: `de eerste van zijn sluitingsdag en zijn
volgnummer` lists one date operand and one numeric one. Each operand is
orderable on its own — this is not the equality-only defect the four
sibling cases carry — but an ordering fold keeps the least
*contributor*, and a date and a number lie in two comparable families
that share no ordering between them.

    WF-038 FAIL /rules/resolvedRegels/0/versions/0/value: EERSTE orders operands
    of two comparable families, numbers and dates, which share no ordering

Why it has to exist: the clause admits either family, so the per-operand
arm alone would let a document mix them and stay clean. It is reported
at the fold node rather than at an operand deliberately — neither
operand is wrong by itself; their combination is, and there is no
principled way to elect one of the two as the offender.

Measured before the clause, verifier-clean and three different
outcomes: `ref` faults at evaluation (`EVALUATION FAULT: unordered
comparison: 3 vs 2026-05-01`); the ASP backend already refused the form
by name (`TRANSLATION FAULT: mixed date/non-date operands under an
eerste/laatste fold are not translated`) — a backend fence that had no
document-layer rule behind it until now; and the Drools backend
**throws** (`ERROR ConsequenceException: … java.lang.ClassCastException:
class java.math.BigDecimal cannot be cast to class
java.time.chrono.ChronoLocalDate`), a crash on verifier-clean input.
The single-family cases stay legitimate and are pinned as goldens:
`corpus/golden/jutterswet-hoogwaterpaar` (an articulated `EERSTE` and
`LAATSTE` over two dates) and `corpus/golden/jutterswet-peildatum`
(which carries both a date pair and a numeric pair), all replayed by
gates 5 and 6.

Falsifiable, arm-exact: make the cross-operand family arm of the WF-038
ordering-fold clause return `[]` and this case alone turns clean, while
`wf038-eerste-somtype-geleed`, `wf038-laatste-enum-geleed`,
`wf038-minimum-enum-setbron` and `wf038-maximum-somtype-setbron` — whose
defect is the per-operand orderability arm — all stay red.

Since L4 the node is an `Aggregation` or a `BuiltinCall` (delta B21), each
with only its own fields — `arguments`, and on an aggregation `elementType`,
`emptyFallback`, `dimensionSelection`, `sourceRole` and `sourcePath`, on a
builtin `granularity`; the report cites and names them. `expect.txt` pins the
arm.
