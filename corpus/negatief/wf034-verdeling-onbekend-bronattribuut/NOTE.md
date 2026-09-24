Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6): an **allocation** (ch. 3.2) whose
source endpoint names an attribute *no declaration carries*.

    WF-034 FAIL /rules/verdelingen/0/versions/0/sourceAttribute: 'bestaatniet'
    is not a declared attribute of the source type 'Berging': the allocation's
    sourceAttribute resolves against no declaration

The clause is the one described in full in the sibling NOTE
(`wf034-verdeling-onbekend-ratoattribuut`): every filled attribute name on the
allocation's six name fields resolves against the declaration scope of its own
side. `sourceAttribute` is a **source-side** field — it names the pot on the
`sourceType` instance, as `remainderAttribute` names the remainder written
back there; the other four resolve against `targetType`.

This is a **loud** witness — one of three, since the fix round gave
`targetAttribute` and `remainderAttribute` their own — and it is here for exactly
that reason: the class has three observability rows, and the two silent ones
must not be mistaken for the whole story. Measured on this document, against
`corpus/golden/jutterswet-berging/case.json`:

    ref    : EVALUATION FAULT: empty source amount in proportional allocation
             (v0 mirror hole, ch. 4.4)
    clingo : TRANSLATION FAULT: unknown attribute "bestaatniet" on "Berging"

Both refuse. That the executors happen to be loud here is not a reason to
leave the verifier silent: a rule that catches a dangling reference only where
an engine would have caught it anyway is a rule that catches nothing. The same
field emptied to `null` is the wave-3 arm (`wf034-verdeling-zonder-bronattribuut`)
and blanked to `""` is the fix-round arm (`wf034-verdeling-lege-bronattribuut`);
this is the third and last way to spell the same defect — a name that is
present, non-blank, and resolves to nothing.

Measured breadth for this field: **11 of 11** golden allocations stayed
verifier-clean when `bronAttribuut` was pointed at an undeclared name; the
same 11 of 11 for `doelAttribuut` and 9 of 9 for `restantAttribuut`, all three
loud in both executors. **0** stay clean after the clause.

*Measured breadth of the shape itself:* zero outside the six `-onbekend-`
allocation witnesses; see the sibling NOTE for the four-zone sweep.

A hand-broken variant of `corpus/golden/jutterswet-berging/ir.json` — one
field repointed, a one-line diff; schema-valid, since the version's
`sourceAttribute` is `["string","null"]` on the wire.

Falsifiable, clause-exact: drop `wf034_verdeling_naamresolutie` from the
verifier's check list and this case and its five `-onbekend-` allocation
siblings, those six and nothing else, turn clean. Arm-exact instead: delete
the single `resolve … "sourceAttribute" …` line and this case, alone in all
four zones, turns clean.

Since L4 an allocation is an `Allocation` with its versions (delta B19), and a
version's fields carry their IR-SPEC names (`sourceType`, `sourceAttribute`,
`targetType`, `targetAttribute`, `method`, `ratioAttribute`,
`maximumAttribute`, `remainderAttribute`, `orderingAttribute`,
`connectingRole`); the report cites the version and names the L4 field. The
measurements above name the fields as they were spelled then (`bronAttribuut`
→ `sourceAttribute`, `doelAttribuut` → `targetAttribute`, `restantAttribuut` →
`remainderAttribute`). `expect.txt` pins the arm.
