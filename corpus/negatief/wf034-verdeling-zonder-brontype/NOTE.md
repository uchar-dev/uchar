Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6): an **allocation** (ch. 3.2) that
names no source object type.

    WF-034 FAIL /verdelingen/0/bronType: allocation without a source object
    type: it resolves against no declaration

The empty-reference class of the earlier waves, one field further along. An
allocation's `bronType` names the object type it distributes **from**, as
`doelType` names the type it distributes **to**; both are references, both are
nullable on the v0 wire, and a null in either resolves against nothing.

An *undeclared* type is already caught — WF-047 crosses it against the
connecting role's fillers on the bound form, WF-048 against the derivable fact
type on the bindingless one, both measured loud. A **null** slips past both,
because both read `Some t`, and it takes with it everything else keyed on the
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
clean after the clause. One witness carries both halves of the pair, because
they are one field pattern read twice — the falsifiability instruction below
turns both arms off at once.

*Measured breadth of the shape itself:* four pre-existing allocation negatiefs
(`wf020-crossform-collision`, `wf024-complex`, `wf025-max-without-ratio`,
`wf026-two-allocations-one-target`) are minimal fragments that carry no types
at all; each gains a truthful incidental line here and keeps its verdict and
the rule its directory declares. No golden and no rood document carries the
shape. Over all 483 documents of golden + rood + negatief + parity this arm
changes exactly one verdict — this one.

A hand-broken variant of `corpus/golden/jutterswet-berging/ir.json` — one
field nulled, a one-line diff; schema-valid, since `ResolvedVerdeling.bronType`
is `["string","null"]` on the v0 wire.

Falsifiable, clause-exact: drop `wf034_verdeling_typen` from the verifier's
check list — its own function, distinct from the name arms beside it — and
this case, alone in all four zones, turns clean; the four pre-existing
allocation negatiefs lose their incidental line and keep their verdict.
Measured: 483 documents compared, 5 reports change, 1 verdict.
