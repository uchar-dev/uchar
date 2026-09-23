Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6): a near-side **role check**
(ch. 5.8a) whose `anderTeFiller` names a type the subject is related to by *no
declared fact type*.

    WF-034 FAIL /rules/resolvedRegels/0/voorwaarde/anderTeFiller: no fact type
    relates the subject 'Jutter' to 'Bestaatniet': the role check's
    anderTeFiller resolves against no declaration

**Which field carries the reference.** A `Rollidmaatschap` spells the written
condition "hij is een `<rol>`" with two fields, and only one of them resolves
anything. The executors take the connecting role from the **subject** to
`anderTeFiller` and null-test it (ch. 5.8a); the written `rol` is
presentation, mirrored from the surface text and never looked up. So
`anderTeFiller` is the whole of the reference, and nothing crossed it.

**The cross.** One, against the role fillers: the ch. 4.3 candidate list
(`connecting_role_candidates`), which is the executors' own derivation
mirrored into the verifier. An **empty** list is this arm's report — it is
exactly `ref`'s `[]` fault, lifted to well-formedness. A candidate list with
several entries is a different defect (the pair is ambiguous, ch. 4.1) and
stays WF-048's business; a subject the expression walk does not know keeps the
arm silent.

**Measured** over all four golden positions (`jutterswet-wachtindeling` 2,
`jutterswet-wachtploeg` 2), the field pointed at an undeclared type:

    verifier   4/4 PASS
    ref        4/4 EVALUATION FAULT: no fact type relates "Jutter" to
               "Bestaatniet"
    clingo     4/4 the same sentence, as a TRANSLATION FAULT

and, on this document, the third executor takes a third road: the bean-model
backend goes from playing the golden to dropping the rule (a transpiler skip,
"voorwaarde buiten Phase-B scope"), so the document neither faults nor
answers. Three executors, three different treatments of one verifier-clean
document.

This is the cheapest of the fifth wave's four planes and the loudest: both
OCaml executors already refuse it at run time, so nothing is at risk of going
silently wrong. It is here for the reason the family exists — a rule that
catches a dangling reference only where an engine would have caught it anyway
catches nothing, and the third executor shows the failure mode is not even
uniform.

*Measured breadth:* zero documents lose anything. Measured at the pre-card
state, of the 8 `Rollidmaatschap` nodes in golden, rood, negatief and the
parity zone seven named a type the subject is related to and the eighth carried
`anderTeFiller: null`, which this arm passes over (the null half is not this
arm's question). Re-measured over the 10 those zones hold since: **8** resolve
against exactly one connecting role — four golden, one rood, two parity, and
the second, intact rule of this very document — **1** is the null one, and
**1** resolves against nothing: the first rule here. The four-zone sweep moves
no other verdict and no other report.

A hand-broken variant of `corpus/golden/jutterswet-wachtindeling/ir.json` —
one field repointed, a one-line diff; schema-valid, since
`Rollidmaatschap.anderTeFiller` is `["string","null"]` on the v0 wire.

Falsifiable, arm-exact: negate the guard of the `Rollidmaatschap` arm of
`walk_cond` (`spec/verifier/lib/checks.ml`) and this case, alone in all four
zones, turns clean — 503 documents compared, one verdict moved, no report
elsewhere.
