Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6): a **dimension-cell read**
(ch. 4.1) with no attribute reference in it. A cell read reads one cell of
one dimensioned attribute; `attribuut` is where that attribute is named, so
an empty one resolves against no declaration.

    WF-034 FAIL /resolvedRegels/0/waarde: dimension-cell read without an
    attribute reference: it resolves against no declaration

Why nothing else stood in for it: the cell arm resolves only through the
`Some (AttribuutRef { attribuut = Some _; objecttype = Some _ })` pattern, and
everything the arm does hangs off that one match — WF-040's cross of the
written labels against the attribute's declared dimensions (exactly one label
per dimension, no more dimensions than labels), and the identity family's
cross of the **cell read's own `type`** against the declaration the cell
comes from. Empty the field and both go away together, silently.

That second half is what makes the shape dangerous, and it is measured on an
existing artifact rather than argued: `corpus/negatief/wf091-dimensiecel-eenheid`
is refused for a unit lie on the cell's own type —

    WF-091 FAIL /resolvedRegels/0/waarde/type: unit mismatch: dimension-cell
    read of 'kern' is typed 't' of system 'Gewicht' (factor 1000 to 'kg'),
    but the declaration it reads from is 'kg' of system 'Gewicht' (factor 1
    to 'kg')

— and emptying that document's `attribuut` field turns the refusal off
entirely: `verifier: PASS`. A document could therefore buy its way out of the
unit, precision and domain checks by deleting the very field that identifies
what it is reading.

*What no measurement here can show,* said plainly: this shape has **no
executor witness**, and cannot have one. A cell read is declined as a FORM by
both OCaml executors, measured on a throwaway probe case over the unbroken
golden this document was broken from:

    ref    : EVALUATION FAULT: expression kind not implemented in this card
    clingo : TRANSLATION FAULT: expression kind not translated (no corpus
             case exercises it)

So neither golden that carries a cell read has a case file, and neither
could — a committed case over one would turn the oracle and differential
gates red on the unbroken document. This is a **verifier-only** channel: the
argument for closing it is the identity family's silence above, not a
divergence between two engines.

*Measured dracht:* zero outside this witness and its non-reference sibling —
no `DimensieCel` with a null `attribuut` occurs in the 153 golden + 22 rood +
30 parity documents or in the 257 negatief cases that predate this card. Both
golden cell positions stayed verifier-clean when emptied one at a time before
this clause and neither does after; no document in any zone changes verdict.

A hand-broken variant of `corpus/golden/jutterswet-seizoenvondst/ir.json` —
one field emptied, a one-field diff. The shape is schema-valid:
`DimensieCel.attribuut` is `anyOf [expression, null]` on the v0 wire, which
is precisely why the verifier, not the schema, is the fence.

Falsifiable, clause-exact: remove the `attribuut = None` arm from
`walk_expr`'s `Dimensie_cel` case and this case alone turns clean — measured
over all four zones, with the non-reference sibling and every other document
keeping its verdict.
