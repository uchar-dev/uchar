Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6): an **allocation** (ch. 3.2) whose
cap names an attribute *no declaration carries*.

    WF-034 FAIL /rules/allocations/0/versions/0/maximumAttribute: 'bestaatniet'
    is not a declared attribute of the target type 'Berger': the allocation's
    maximumAttribute resolves against no declaration

The clause is the one described in full in the sibling NOTE
(`wf034-verdeling-onbekend-ratoattribuut`): every filled attribute name on the
allocation's six name fields resolves against the declaration scope of its own
side. `maximumAttribute` is the per-recipient cap of a `NAAR_RATO`
allocation — the ceiling above which a share is not paid out and the excess
falls to the remainder.

This is the witness for the **divergent** row: the one place in this class
where the two executors disagree about whether the document exists at all.
Measured on this document, against `corpus/golden/jutterswet-berging/case.json`:

    ref    : MISMATCH: W.bergingsaandeel = 600, expected 300
             MISMATCH: D.bergingsaandeel = 400, expected 300
             MISMATCH: B1.onverdeelde rest = 0, expected 400
             MISMATCH: expected consistent, but the channel carries:
                       verdeel de opbrengst van een berging × D;
                       verdeel de opbrengst van een berging × H;
                       verdeel de opbrengst van een berging × W
    clingo : TRANSLATION FAULT: unknown attribute "bestaatniet" on "Berger"

`ref` plays on with the cap simply gone: W is paid 600 where the declared
ceiling is 300, the remainder that should have been 400 is 0, and the run
reports a *value*, not a refusal. The ASP backend refuses to translate the
same document. One document, two executors, two incompatible verdicts — on
input the verifier called well-formed. The differential square cannot arbitrate
this: it is not a disagreement about an answer, it is a disagreement about
whether there is a question.

Measured breadth for this field: **4 of 4** golden allocations that carry a
`maximumAttribuut` stayed verifier-clean when it was pointed at an undeclared
name. **0** stay clean after the clause.

*Measured breadth of the shape itself:* zero outside the six `-onbekend-`
allocation witnesses; see the sibling NOTE for the four-zone sweep.

A hand-broken variant of `corpus/golden/jutterswet-berging/ir.json` — one
field repointed, a one-line diff; schema-valid, since the version's
`maximumAttribute` is an optional string on the wire.

Falsifiable, clause-exact: drop `wf034_verdeling_naamresolutie` from the
verifier's check list and this case and its five `-onbekend-` allocation
siblings, those six and nothing else, turn clean.

Since L4 an allocation is an `Allocation` with its versions (delta B19), and a
version's fields carry their IR-SPEC names (`sourceType`, `sourceAttribute`,
`targetType`, `targetAttribute`, `method`, `ratioAttribute`,
`maximumAttribute`, `remainderAttribute`, `orderingAttribute`,
`connectingRole`); the report cites the version and names the L4 field. The
measurements above name the fields as they were spelled then
(`maximumAttribuut` → `maximumAttribute`). `expect.txt` pins the arm.
