Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6): an **allocation** (ch. 3.2) that
names no target object type.

    WF-034 FAIL /rules/verdelingen/0/versions/0/targetType: allocation without a
    target object type: it resolves against no declaration

The empty-reference class of the earlier waves, one field further along, and
the other half of the pair its sibling NOTE
(`wf034-verdeling-zonder-brontype`) sets out in full: `bronType` names the
object type an allocation distributes **from**, `doelType` the type it
distributes **to**; both are references, both are nullable on the v0 wire, and
a null in either resolves against nothing.

The two halves are one field pattern read twice, but they are not one arm: each
has its own `match` in `wf034_verdeling_typen`, and each therefore needs its
own witness. Without this document, deleting the `doelType` arm alone changed
no **verdict** anywhere in the four zones — three type-less allocation
negatiefs merely lost an incidental line, and the `bronType` witness kept
standing on its own arm, proving nothing about this one.

Measured on this document, against `corpus/golden/jutterswet-berging/case.json`:

    ref    : EVALUATION FAULT: missing doelType in IR node
    clingo : TRANSLATION FAULT: missing doelType

Both refuse, by the field's own name — exactly as they do on the `bronType`
half. The clause makes the verifier say before the run what both executors say
during it.

Measured breadth: **11 of 11** golden allocations stayed verifier-clean with
`doelType` emptied, and the same 11 of 11 with `bronType` emptied; **0** stay
clean after the clause. A null takes with it everything else keyed on the
type — WF-025's ordering-key datatype, WF-050's defaults overlap, the
dependency graph's read and write edges, and the name-resolution arm beside it,
which has no scope to resolve in without it. An *undeclared* type is another
rule's business: WF-047, against the recipient role the allocation carries.

*Measured breadth of the shape itself:* three pre-existing allocation negatiefs
(`wf020-crossform-collision`, `wf024-complex` (removed at L4, where `COMPLEX` became a load error), `wf025-max-without-ratio`) are
minimal fragments that carry no types at all; each gains a truthful incidental
line here and keeps its verdict and the rule its directory declares.
(`wf026-two-allocations-one-target` is the fourth of that family and gains two
incidental lines as well, but both come from the sibling arm: its two
allocations name a declared `doelType` and no `bronType` at all.) No golden and
no rood document carries the shape.

A hand-broken variant of `corpus/golden/jutterswet-berging/ir.json` — one
field nulled, a one-line diff; schema-valid, since `ResolvedVerdeling.doelType`
is `["string","null"]` on the v0 wire.

Falsifiable, arm-exact: in `wf034_verdeling_typen` replace the whole
`match r.doel_type with …` arm by `[]`, leaving the `bronType` arm untouched.
This case, alone in all four zones, turns clean; its `bronType` sibling stays
rejected. Measured: 486 documents compared, 4 reports change, 1 verdict.
Dropping the whole clause instead frees this case and
`wf034-verdeling-zonder-brontype`, those two and nothing else: 6 reports
change, 2 verdicts.

Since L4 an allocation is an `Allocation` with its versions (delta B19), and a
version's fields carry their IR-SPEC names (`sourceType`, `sourceAttribute`,
`targetType`, `targetAttribute`, `method`, `ratioAttribute`,
`maximumAttribute`, `remainderAttribute`, `orderingAttribute`,
`connectingRole`); the report cites the version and names the L4 field.
`expect.txt` pins the arm.
