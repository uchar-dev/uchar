Violates WF-038 (`spec/IR-SPEC.md` ch. 4.6), the ordering-fold operand
clause, in the **articulated** form: `de laatste van zijn bakboordkust
en zijn stuurboordkust` lists two operands, both declared as the
enumeration `Kust`. The articulated fold keeps the greatest operand
*value* (`RUNTIME-SEMANTICS.md` ch. 1.2 — never a positional pick), and
`Enumeration` is equality-only by the ch. 2.4 orderability paragraph, so
there is no greatest.

    WF-038 FAIL /rules/resolvedRegels/0/versions/0/value/arguments/0: operand of
    the LAATSTE ordering fold is enumeration, must be a number or a date
    WF-038 FAIL /rules/resolvedRegels/0/versions/0/value/arguments/1: operand of
    the LAATSTE ordering fold is enumeration, must be a number or a date

Why it has to exist beside the set-source case: this is the form on
which the three executors do not merely disagree about admissibility —
they disagree about the **answer**. Measured on this document with
`bakboordkust = Zuid` and `stuurboordkust = Noord`: `ref` faults
(`EVALUATION FAULT: unordered comparison: Noord vs Zuid`); the ASP
backend refuses ahead of translation (`TRANSLATION FAULT: a
text/enum/boolean operand here is an own-subject attribute read or a
literal`); and the Drools backend **plays the document and answers
`Zuid`** — the alphabetically greatest label, case-sensitive ASCII on
the written spelling, on both of its transpiler branches. That order is
`Comparable` on the label string: not a designed order over the domain,
but the incidental one a Java string carries, under which a domain
renamed from `Noord`/`Zuid` to `noord`/`ZUID` would answer differently.
One verifier-clean document, one executor faulting and another returning
a value: a ref ≢ Drools divergence that is invisible today only because
no corpus document carries the form. The same measurement holds for the
other equality-only element types — an articulated `EERSTE` over two
`Text` attributes and a `MAXIMUM` over two `Boolean` ones both had `ref`
fault (`unordered comparison: "noord" vs "zuid"`, `unordered comparison:
onwaar vs waar`) while the Drools backend played and answered — which is
why the clause is written over orderability, not over an enumerated list
of two forbidden types.

Falsifiable, arm-exact: make the per-operand orderability arm of the
WF-038 ordering-fold clause return `[]` and this case turns clean
together with `wf038-minimum-enum-setbron`,
`wf038-eerste-somtype-geleed` and `wf038-maximum-somtype-setbron`, while
`wf038-eerste-gemengde-familie` stays red.

Since L4 the node is an `Aggregation` or a `BuiltinCall` (delta B21), each
with only its own fields — `arguments`, and on an aggregation `elementType`,
`emptyFallback`, `dimensionSelection`, `sourceRole` and `sourcePath`, on a
builtin `granularity`; the report cites and names them. `expect.txt` pins the
arm.
