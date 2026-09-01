Violates WF-089 (`spec/IR-SPEC.md` ch. 2.4): a `DimensieCel` read of the
dimensioned enumeration attribute `vaarwater` (dimension `seizoen`, cell
`voorjaar`) carries a `type` naming the domain `Kust`; the inner
`AttribuutRef` is faithful (`Vaarwater`), and so is the attribute's
declaration. A cell of a dimensioned attribute carries that attribute's
datatype, so the cell read's own `type` is a use site of the same
declaration.

    WF-089 FAIL /resolvedRegels/0/waarde/scrutinee/type: dimension-cell read
    of 'vaarwater' is typed with keuzetype domain 'Kust', but the
    declaration it reads from names domain 'Vaarwater'

Why it has to exist: the `Dimensie_cel` arm checked the labels (WF-040)
and walked the inner reference, but never the cell's own `type` — the
one position among the reference shapes that reads a declaration without
any structural cross at all. It is a verifier-only flank: the case format
carries no dimension-cell input (the `jutterswet-vormenkabinet` golden has
no case file for the same reason), so no executor witness is possible and
the rule is the only fence.

Falsifiable: disable the `wf089_domain_identity` call in the
`Dimensie_cel` arm and this case passes — the inner reference is faithful,
so the `Attribuut_ref` arm has nothing to say — while every other wf089
case stays red.
