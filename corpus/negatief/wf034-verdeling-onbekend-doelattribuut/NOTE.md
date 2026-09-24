Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6): an **allocation** (ch. 3.2) whose
target endpoint names an attribute *no declaration carries*.

    WF-034 FAIL /rules/verdelingen/0/versions/0/targetAttribute: 'bestaatniet'
    is not a declared attribute of the target type 'Berger': the allocation's
    targetAttribute resolves against no declaration

The clause is the one described in full in the sibling NOTE
(`wf034-verdeling-onbekend-ratoattribuut`): every filled attribute name on the
allocation's six name fields resolves against the declaration scope of its own
side. `targetAttribute` is the field the allocation *writes* — the share
deposited on each `targetType` recipient — where `sourceAttribute` names the
pot it reads from; the three facet fields beside it (`ratioAttribute`,
`maximumAttribute`, `orderingAttribute`) resolve against the same
`targetType` but are read, not written.

The written endpoint is the one the pair could not do without: WF-026's
same-target comparison, WF-050's overlap with a defaults group and the
dependency graph's write edge are all keyed on it, and every one of them sits
behind a `Some a` that an undeclared name satisfies. The verifier accepted such
a name until this clause; the write it authorised has no target.

Measured on this document, against `corpus/golden/jutterswet-berging/case.json`:

    ref    : EVALUATION FAULT: unknown attribute "bestaatniet" on "Berger"
    clingo : TRANSLATION FAULT: unknown attribute "bestaatniet" on "Berger"

Both refuse, by the target type's own name. This is the second **loud**
witness of the class, and it is here for the reason the first one is: the two
silent rows (`-onbekend-ratoattribuut`, `-onbekend-groepeerattribuut`) and the
divergent one (`-onbekend-maximumattribuut`) must not be mistaken for the whole
story, and neither must the loud ones.

Measured breadth for this field: **11 of 11** golden allocations stayed
verifier-clean when `doelAttribuut` was pointed at an undeclared name — the
same 11 of 11 as `bronAttribuut`, since every allocation carries both. **0**
stay clean after the clause.

*Measured breadth of the shape itself:* one document outside the six
`-onbekend-` allocation witnesses carries it — the parity casus, whose own
`doelAttribuut` names an attribute its target type does not declare, six times
over; it keeps its verdict and gains six truthful incidental lines. No golden
and no rood document carries the shape. See the sibling NOTE for the four-zone
sweep.

A hand-broken variant of `corpus/golden/jutterswet-berging/ir.json` — one
field repointed, a one-line diff; schema-valid, since the version's
`targetAttribute` is `["string","null"]` on the wire.

Falsifiable, arm-exact: in `wf034_verdeling_naamresolutie` delete the single
line `resolve ~side:"target" ~scope:r.doel_type "targetAttribute"
r.doel_attribuut`. This case, alone in all four zones, turns clean; the parity
casus loses its six incidental lines and keeps its verdict. Measured: 486
documents compared, 2 reports change, 1 verdict. Dropping the whole clause
instead frees this case and its five `-onbekend-` allocation siblings, those
six and nothing else: 7 reports change, 6 verdicts.

Since L4 an allocation is an `Allocation` with its versions (delta B19), and a
version's fields carry their IR-SPEC names (`sourceType`, `sourceAttribute`,
`targetType`, `targetAttribute`, `method`, `ratioAttribute`,
`maximumAttribute`, `remainderAttribute`, `orderingAttribute`,
`connectingRole`); the report cites the version and names the L4 field. The
measurements above name the fields as they were spelled then (`bronAttribuut`
→ `sourceAttribute`, `doelAttribuut` → `targetAttribute`). `expect.txt` pins
the arm.
