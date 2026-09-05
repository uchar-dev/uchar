Violates WF-038 (`spec/IR-SPEC.md` ch. 4.6), the ordering-fold operand
clause, in the **articulated** form over the second equality-only
family: `de eerste van zijn bakboordstand en zijn stuurboordstand` lists
two operands of the sum type `stand` (`Geborgen` carrying a number,
`Vermist` carrying nothing). `SumType` is equality-only by the ch. 2.4
orderability paragraph; a constructor tag has no least.

    WF-038 FAIL /resolvedRegels/0/waarde/argumenten/0: operand of the
    EERSTE ordering fold is sumtype, must be a number or a date
    WF-038 FAIL /resolvedRegels/0/waarde/argumenten/1: operand of the
    EERSTE ordering fold is sumtype, must be a number or a date

Why it has to exist beside the enumeration cases: the sum type is the
family whose payload makes the missing order visible. `ref` faults at
evaluation (`EVALUATION FAULT: unordered comparison: Vermist vs
Geborgen(10)`) — the fault message shows exactly what an order here
would have to decide: whether a nullary constructor precedes a unary one
carrying 10. Nothing in the type says. The ASP backend does not refuse
this form; it **crashes** on it (`Fatal error: exception
Division_by_zero`), which is a translator defect of its own — recorded
separately, and the one measurement here that is a backend bug rather
than an executor's honest refusal. The Drools backend declines the
family at load (`EXCLUDE loader: form not carried by the v0 Drools
backend: Datatype:SumType`). So the document as it stood was
verifier-clean, faulted in one executor, crashed the second and was
never reached by the third — three different accounts of a document that
under this clause is simply ill-formed, and unreachable by any valid
route.

A payload-less sum type is not the sharper witness it looks: WF-087
normalises a constructor set with no payloads to an `Enumeration`, so
that cell does not exist, and the enumeration cases carry it.

Falsifiable, arm-exact: make the per-operand orderability arm of the
WF-038 ordering-fold clause return `[]` and this case turns clean
together with `wf038-laatste-enum-geleed`, `wf038-minimum-enum-setbron`
and `wf038-maximum-somtype-setbron`, while
`wf038-eerste-gemengde-familie` stays red.
