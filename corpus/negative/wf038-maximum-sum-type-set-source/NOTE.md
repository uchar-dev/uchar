Violates WF-038 (`spec/IR-SPEC.md` ch. 4.6), the ordering-fold operand
clause, in the **set-source** form over a sum type: `maximum(de stand
van alle schuiten van de Berging)` folds over `stand` of `Schuit`,
declared with the sum-type domain `stand`. `SumType` is equality-only by
the ch. 2.4 orderability paragraph, so the greatest contributor is not
defined.

    WF-038 FAIL /rules/rules/0/versions/0/value/arguments/0: operand of
    the maximum ordering fold is sumtype, must be a number or a date

Why it has to exist beside the other three: the four ordering kinds are
one family in the rule but not in the executors, and the four cases
cross the two wire forms with the two equality-only type families so
that no cell of that square is left to be discovered later — this is the
`maximum` cell of the set-source form. Measured: `ref` faults
(`EVALUATION FAULT: unordered comparison: Vermist vs Geborgen(10)`); the
ASP backend refuses with a different message than it gives the
articulated sibling (`TRANSLATION FAULT: unscalable datatype in the ASP
translation` — its scaling pass reaches this form, where the articulated
one crashes it); the Drools backend declines at load (`EXCLUDE loader:
form not carried by the v0 Drools backend: Datatype:SumType`). Note the
asymmetry the enumeration sibling makes loud: over an *enumeration*
element the same set fold does not decline in Drools at all — it throws
inside the accumulate (`wf038-minimum-enum-set-source`). The refusal here
is the datatype's, not the fold's, and it would disappear the day the
loader learned sum types.

Falsifiable, arm-exact: make the per-operand orderability arm of the
WF-038 ordering-fold clause return `[]` and this case turns clean
together with `wf038-first-sum-type-articulated`,
`wf038-last-enum-articulated` and `wf038-minimum-enum-set-source`, while
`wf038-first-mixed-family` stays red.

Since L4 the node is an `Aggregation` or a `BuiltinCall` (delta B21), each
with only its own fields — `arguments`, and on an aggregation `elementType`,
`emptyFallback`, `dimensionSelection`, `sourceRole` and `sourcePath`, on a
builtin `granularity`; the report cites and names them. `expect.txt` pins the
arm.
