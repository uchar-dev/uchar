Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6): a **dimension-cell read**
(ch. 4.1) whose attribute position holds something that is not an attribute
reference — here a numeric literal typed in the same unit as the cell.

    WF-034 FAIL /resolvedRegels/0/waarde/attribuut: dimension-cell read whose
    attribute position is not an attribute reference: it resolves against no
    declaration

This is the **second spelling** of the escape its sibling
(`wf034-dimensiecel-zonder-attribuut`) closes in its first, and it needs its
own document because the two are different match failures with the same
consequence. The cell arm resolves only through
`Some (AttribuutRef { attribuut = Some _; objecttype = Some _ })`; an empty
field misses that pattern by being `None`, and any other expression node
misses it by being the wrong constructor. On the wire the field is
`anyOf [expression, null]`, so a producer that wanted the identity family to
look away had **two** ways to arrange it, and closing only one would have
left the channel open at full width.

The consequence is the same and is measured on an existing artifact:
`corpus/negatief/wf091-dimensiecel-eenheid` is refused for a unit lie on the
cell's own type —

    WF-091 FAIL /resolvedRegels/0/waarde/type: unit mismatch: dimension-cell
    read of 'kern' is typed 't' of system 'Gewicht' (factor 1000 to 'kg'),
    but the declaration it reads from is 'kg' of system 'Gewicht' (factor 1
    to 'kg')

— and replacing that document's `attribuut` with a literal turns the refusal
off: `verifier: PASS`. WF-040's label/dimension cross goes with it.

An incomplete attribute reference in this position is deliberately **not**
this arm's business: the walk descends into the inner node and its own
WF-034 arms report the missing half, so the arm stays silent there rather
than reporting the same defect twice under two wordings.

*No executor witness, and none possible* — see the sibling NOTE: a cell read
is declined as a form by both OCaml executors, so no golden carrying one has
a case file. This is a verifier-only channel.

*Measured dracht:* zero — no `DimensieCel` whose `attribuut` is a
non-reference node occurs in the 153 golden + 22 rood + 30 parity documents
or in the 257 negatief cases that predate this card. Both golden cell
positions stayed verifier-clean when overwritten with a literal one at a time
before this clause and neither does after; no document in any zone changes
verdict.

A hand-broken variant of `corpus/golden/jutterswet-seizoenvondst/ir.json` —
one child node replaced, a one-node diff; schema-valid by construction, since
a `Literal` is an expression and the field admits any expression.

Falsifiable, clause-exact: remove the non-reference arm from `walk_expr`'s
`Dimensie_cel` case and this case alone turns clean — measured over all four
zones, with the empty-field sibling and every other document keeping its
verdict.
