Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6): a **dimension-cell read**
(ch. 4.1) with no attribute reference in it. A cell read reads one cell of
one dimensioned attribute; `attribute` is where that attribute is named, so
an empty one resolves against no declaration.

    WF-034 FAIL /rules/rules/0/versions/0/value: dimension-cell read
    without an attribute reference: it resolves against no declaration

Why nothing else stood in for it: the cell arm resolves only through the
`Some (AttribuutRef { attribuut = Some _; objecttype = Some _ })` pattern, and
everything the arm does hangs off that one match — WF-040's cross of the
written labels against the attribute's declared dimensions (exactly one label
per dimension, and the two counts EQUAL: the cross refuses in both
directions, too many labels as well as too few), and the identity family's
cross of the **cell read's own `type`** against the declaration the cell
comes from. Empty the field and both go away together, silently.

That second half was what made the shape dangerous, and it was measured on
the artifacts of the day rather than argued. **Four** of them lost their pin
entirely under either spelling of this escape — measured with the verifier of
the day before this card, replacing each document's cell `attribute` with
`null` and, separately, with a numeric literal:

    wf091-dimensiecel-eenheid   WF-091 unit lie             -> verifier: PASS
    wf089-dimensiecel-enum      WF-089 keuzetype-domain lie -> verifier: PASS
    wf090-dimensiecel           WF-090 struct-domain lie    -> verifier: PASS
    wf040-unpinned-cell         WF-040 + WF-091 sign lie    -> verifier: PASS

The last one is the reason WF-040 is named beside the identity family above:
its label/dimension cross hangs off the same match and travels with them.
Spelled out on the first, until L2: `corpus/negative/wf091-dimensiecel-eenheid`
was refused for a unit lie on the cell's own type —

    WF-091 FAIL /rules/resolvedRegels/0/waarde/type: unit mismatch: dimension-cell
    read of 'kern' is typed 't' of system 'Gewicht' (factor 1000 to 'kg'),
    but the declaration it reads from is 'kg' of system 'Gewicht' (factor 1
    to 'kg')

— and emptying that document's `attribuut` field turned the refusal off
entirely: `verifier: PASS`. A document could therefore buy its way out of the
unit, precision and domain checks by deleting the very field that identifies
what it is reading.

*Since L2* the cell read carries no type of its own (B13), so those lies
cannot be written on it: of the four artifacts, `wf091-dimensiecel-eenheid`
was removed with reason, `wf089-dimensiecel-enum` and `wf090-dimensiecel`
were rewritten onto WF-063 and WF-074 (`corpus/negative/README.md`, section
L2), and `wf040-unpinned-cell` lost its sign-lie line. What the empty field
still switches off is WF-040's label/dimension cross. Measured in the L2 fix
round (2026-09-24), with the `attribuut = None` arm disabled in a copy of
`spec/verifier/lib/checks.ml`: this document answers `verifier: PASS`, and
over golden, red, negative and the parity zone (718 documents) no other
report changes, so this witness is the only thing that pins the arm.

*What no measurement here can show,* said plainly: this shape has **no
executor witness with the current executors**. A cell read is declined as a
FORM by both OCaml executors, measured on a throwaway probe case over the
unbroken golden this document was broken from:

    ref    : EVALUATION FAULT: expression kind not implemented in this card
    clingo : TRANSLATION FAULT: expression kind not translated (no corpus
             case exercises it)

So neither golden that carries a cell read has a case file today, and neither
could while the executors decline the form — a committed case over one would
turn the oracle and differential gates red on the unbroken document. This is
a **verifier-only** channel as things stand: the argument for closing it is
the identity family's silence above, not a divergence between two engines.
Should an executor learn the form, the four artifacts above stay the
measurement that carries the clause.

*Measured dracht:* zero outside this witness and its non-reference sibling — no
`DimensionCellRead` with a null `attribute` occurs in the 153 golden + 22 red +
30 parity documents or in the 257 negative cases that predate this card. Both
golden cell positions stayed verifier-clean when emptied one at a time before
this clause and neither does after; no document in any zone changes verdict.

A hand-broken variant of `corpus/golden/jutterswet-seizoenvondst/ir.json` —
one field emptied, a one-field diff. The shape is schema-valid:
`DimensionCellRead.attribute` is `anyOf [expression, null]` on the wire, which
is precisely why the verifier, not the schema, is the fence.

Falsifiable, clause-exact: remove the `attribuut = None` arm from
`walk_expr`'s `Dimensie_cel` case and this case alone turns clean — measured
over all four zones, with the non-reference sibling and every other document
keeping its verdict.

**Migrated at L2 (2026-09-24), stamp: herschreven (m).** The document is the
migration's translation (`spec/proposals/v1-migratie/m2.py`): the cell read
lost its own `type` (B13); its attribute position is still empty. The rule
still fires on the same arm, and `expect.txt` pins it.
