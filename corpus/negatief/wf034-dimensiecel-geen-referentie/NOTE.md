Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6): a **dimension-cell read**
(ch. 4.1) whose attribute position holds something that is not an attribute
reference — here a numeric literal typed in the same unit as the cell.

    WF-034 FAIL /rules/rules/0/versions/0/value/attribute: dimension-cell
    read whose attribute position is not an attribute reference: it resolves
    against no declaration

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

That the corpus carries no such document today is a fact about this corpus,
not about the wire. The structural half of the argument, which also covers
producers this corpus has never seen: the reference frontend's IR types this
field as an attribute reference, so its own emissions cannot reach this
spelling at all — the clause is a fence on the WIRE, where any conforming
producer may write any expression node the schema admits.

The consequence was the same and was measured on the artifacts of the day.
**Four** of them lost their pin entirely when their cell `attribute` was
replaced by a literal — the same four, and the same result, as under the
sibling's empty spelling (measured with the verifier of the day before this
card):
`wf091-dimensiecel-eenheid` (unit lie), `wf089-dimensiecel-enum`
(keuzetype-domain lie), `wf090-dimensiecel` (struct-domain lie) and
`wf040-unpinned-cell` (an unpinned dimension plus a sign lie) each went
`verifier: PASS`. Spelled out on the first, until L2:
`corpus/negatief/wf091-dimensiecel-eenheid` was refused for a unit lie on the
cell's own type —

    WF-091 FAIL /rules/resolvedRegels/0/waarde/type: unit mismatch: dimension-cell
    read of 'kern' is typed 't' of system 'Gewicht' (factor 1000 to 'kg'),
    but the declaration it reads from is 'kg' of system 'Gewicht' (factor 1
    to 'kg')

— and replacing that document's `attribuut` with a literal turned the refusal
off: `verifier: PASS`. WF-040's label/dimension cross went with it.

*Since L2* the cell read carries no type of its own (B13), so the unit, sign
and domain lies on it cannot be written: `wf091-dimensiecel-eenheid` was
removed with reason, `wf089-dimensiecel-enum` and `wf090-dimensiecel` were
rewritten onto WF-063 and WF-074 (`corpus/negatief/README.md`, section L2),
and `wf040-unpinned-cell` lost its sign-lie line. WF-040's cross still hangs
off the reference pattern. Measured in the L2 fix round (2026-09-24), with
the non-reference arm disabled in a copy of `spec/verifier/lib/checks.ml`:
this document answers `verifier: PASS`, and over golden, rood, negatief and
the parity zone (718 documents) no other report changes, so this witness is
the only thing that pins the arm.

An incomplete attribute reference in this position is deliberately **not**
this arm's business: the walk descends into the inner node and its own
WF-034 arms report the missing half, so the arm stays silent there rather
than reporting the same defect twice under two wordings.

*No executor witness with the current executors* — see the sibling NOTE:
today's executors decline a cell read as a FORM, so no golden carrying one
has a case file. This is a verifier-only channel as things stand.

*Measured dracht:* zero — no `DimensionCellRead` whose `attribute` is a
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

**Migrated at L2 (2026-09-24), stamp: herschreven (m).** The document is the
migration's translation (`spec/proposals/v1-migratie/m2.py`): the cell read
lost its own `type` (B13); its attribute position still holds a literal
instead of an attribute reference. The rule still fires on the same arm, and
`expect.txt` pins it.
