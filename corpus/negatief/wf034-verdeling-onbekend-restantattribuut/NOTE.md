Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6): an **allocation** (ch. 3.2) whose
remainder endpoint names an attribute *no declaration carries*.

    WF-034 FAIL /rules/verdelingen/0/versions/0/remainderAttribute: 'bestaatniet'
    is not a declared attribute of the source type 'Berging': the allocation's
    remainderAttribute resolves against no declaration

The clause is the one described in full in the sibling NOTE
(`wf034-verdeling-onbekend-ratoattribuut`): every filled attribute name on the
allocation's six name fields resolves against the declaration scope of its own
side. `restantAttribuut` is the second **source-side** field — the remainder
that is written back on the `bronType` instance when the shares do not exhaust
the pot, resolved against `bronType` exactly as `bronAttribuut` is, while the
other four resolve against `doelType`.

This witness exists because the sub-arm it pins left **no trace anywhere**.
Drop the one `restantAttribuut` line from `wf034_verdeling_naamresolutie` and
the verifier's report over all four zones changes on exactly **one** document —
this one, and on none of the other 485. Before this witness was written that
mutant changed nothing at all, so the sub-arm was carried by the clause and by
no artifact — the silence a negatief exists to break. Its two
source-side neighbours are not in that position: the `bronAttribuut` sub-arm has
its own witness, and the `doelAttribuut` one is also read by the parity fixture.

Measured on this document, against `corpus/golden/jutterswet-berging/case.json`:

    ref    : EVALUATION FAULT: unknown attribute "bestaatniet" on "Berging"
    clingo : TRANSLATION FAULT: unknown attribute "bestaatniet" on "Berging"

Both refuse, by the same words, on the same missing name. That the executors
are loud here is not a reason to leave the verifier silent: a rule that catches
a dangling reference only where an engine would have caught it anyway is a rule
that catches nothing — and the two silent rows of this class
(`wf034-verdeling-onbekend-ratoattribuut`,
`wf034-verdeling-onbekend-groepeerattribuut`) are what the loud ones are here to
keep in proportion.

Measured breadth for this field: **9 of 9** golden allocations that carry a
`restantAttribuut` stayed verifier-clean when it was pointed at an undeclared
name. **0** stay clean after the clause.

*Measured breadth of the shape itself:* zero outside the six `-onbekend-`
allocation witnesses; see the sibling NOTE for the four-zone sweep.

A hand-broken variant of `corpus/golden/jutterswet-berging/ir.json` — one
field repointed, a one-line diff; schema-valid, since
`ResolvedVerdeling.restantAttribuut` is `["string","null"]` on the v0 wire.

Falsifiable, arm-exact: in `wf034_verdeling_naamresolutie` delete the single
line `resolve ~side:"source" ~scope:r.bron_type "restantAttribuut"
r.restant_attribuut`. This case, alone in all four zones, turns clean.
Measured: 486 documents compared, 1 report changes, 1 verdict. Dropping the
whole clause instead frees this case and its five `-onbekend-` allocation
siblings, those six and nothing else: 7 reports change, 6 verdicts.

Since L4 an allocation is an `Allocation` with its versions (delta B19), and a
version's fields carry their IR-SPEC names (`sourceType`, `sourceAttribute`,
`targetType`, `targetAttribute`, `method`, `ratioAttribute`,
`maximumAttribute`, `remainderAttribute`, `orderingAttribute`,
`connectingRole`); the report cites the version and names the L4 field.
`expect.txt` pins the arm.
