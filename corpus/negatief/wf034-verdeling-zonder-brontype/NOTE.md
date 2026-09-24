Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6): an **allocation** (ch. 3.2) that
names no source object type.

    WF-034 FAIL /rules/allocations/0/versions/0/sourceType: allocation without a
    source object type: it resolves against no declaration

The empty-reference class of the earlier waves, one field further along. An
allocation's `sourceType` names the object type it distributes **from**, as
`targetType` names the type it distributes **to**; both are references, both
are nullable on the wire, and a null in either resolves against nothing.

An *undeclared* type is already caught — WF-047 crosses it against the
fillers of the recipient role the allocation carries (required since L2; the
derivation for a bindingless allocation, WF-048, left the wire with it),
measured loud (the executors are
not: with `bronType` pointed at an undeclared name `ref` does not fault at all,
it merely leaves every share *leeg* where 300/200/300 and a 400 remainder are
pinned, while the ASP backend refuses to translate — so on that shape too the
verifier, not the differential square, is what catches it). A **null** slips
past it,
because it reads `Some t`, and it takes with it everything else keyed on the
type: WF-025's ordering-key datatype, WF-050's defaults overlap, the
dependency graph's read and write edges, and this card's own name resolution,
which has no scope to resolve in without it.

Measured on this document, against `corpus/golden/jutterswet-berging/case.json`:

    ref    : EVALUATION FAULT: missing bronType in IR node
    clingo : TRANSLATION FAULT: missing bronType

Both refuse, by the field's own name. The clause makes the verifier say before
the run what both executors say during it — which is the whole point of a
well-formedness rule: a document that no executor can execute is not
well-formed, whether or not the executors are polite about it.

Measured breadth: **11 of 11** golden allocations stayed verifier-clean with
`bronType` emptied, and the same 11 of 11 with `doelType` emptied; **0** stay
clean after the clause. One witness per half: the two are one field pattern
read twice, but they are two `match` arms, and deleting either on its own moves
only its own document — so `targetType` has its own witness,
`wf034-verdeling-zonder-doeltype`.

*Measured breadth of the shape itself:* at this card four pre-existing
allocation negatiefs (`wf020-crossform-collision`, `wf024-complex`,
`wf025-max-without-ratio`, `wf026-two-allocations-one-target`) were minimal
fragments that carried no source type at all (three of them no target type
either); each gained a truthful incidental line here and kept its verdict and
the rule its directory declares. Measured on the L4 wire: `wf024-complex` was
removed at L4 (where `COMPLEX` became a load error), and since L2 the other
three name `Jutter` as source type, so this arm's line now stands on this
witness alone. No golden and no rood document carries the shape. Over all 486 documents of
golden + rood + negatief + parity this arm changes exactly one verdict — this
one; its `doelType` twin changes exactly one too, its own.

A hand-broken variant of `corpus/golden/jutterswet-berging/ir.json` — one
field nulled, a one-line diff; schema-valid, since the version's
`sourceType` is `["string","null"]` on the wire.

Falsifiable, clause-exact: drop `wf034_verdeling_typen` from the verifier's
check list — its own function, distinct from the name arms beside it — and
this case and its sibling `wf034-verdeling-zonder-doeltype`, those two and
nothing else, turn clean; the four pre-existing allocation negatiefs lose
their incidental lines and keep their verdict. Measured: 486 documents
compared, 6 reports change, 2 verdicts. Arm-exact instead: replace the
`match r.bron_type with …` arm alone by `[]` and this case, alone in all four
zones, turns clean — 5 reports change, 1 verdict.

Since L4 an allocation is an `Allocation` with its versions (delta B19), and a
version's fields carry their IR-SPEC names (`sourceType`, `sourceAttribute`,
`targetType`, `targetAttribute`, `method`, `ratioAttribute`,
`maximumAttribute`, `remainderAttribute`, `orderingAttribute`,
`connectingRole`); the report cites the version and names the L4 field. The
measurements above name the fields as they were spelled then (`bronType` →
`sourceType`, `doelType` → `targetType`). `expect.txt` pins the arm.
