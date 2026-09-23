# wf083-vouwbron-vreemd-elementtype

Violates WF-083 (`spec/IR-SPEC.md` ch. 4.3), the **anchor cross**, in its set
half: a set source reads each member of the `elementObjecttype`, so its
`objecttype` must name that element type. Here it names the fold subject
instead, and the attribute it spells is declared there and nowhere near the
members being folded.

    WF-083 FAIL /rules/resolvedRegels/0/waarde/argumenten/0: fold-source objecttype
    'Jutter' is not the fold's anchor 'Vondst' (the set source reads each
    member of the elementObjecttype): the executors resolve 'kistgeld'
    against the anchor, never against this annotation, so the two spellings
    name different declarations

The document. A Jutter-subject `TOTAAL` over the Jutter's finds —
`elementObjecttype: "Vondst"`, `bronRol` bound to the role `vondst`, the
operand `set: true`, no navigation — whose operand names `kistgeld` with
`objecttype: "Jutter"`. `Jutter` declares `kistgeld`; `Vondst` does not. Every
declaration cross in the document is satisfied, which is exactly why the form
was verifier-clean.

Why this one is the worst of the four. It is the case where the two OCaml
executors DISAGREE under a well-formed document:

    ref         : oracle PASS — bergloon = 0
    ASP backend : TRANSLATION FAULT: unknown attribute "kistgeld" on "Vondst"
    Drools      : ERROR ConstraintEvaluationException

`ref` folds each member, asks every `Vondst` for `kistgeld`, gets *leeg* from
all of them, and totals 0 — silently, because 0 is what a `TOTAAL` over an
empty contribution set is. The ASP backend looks the attribute up on the
element type, the anchor it names in its own code, and refuses. The bean-model
executor throws inside the accumulate. A silent number on one side and a loud
refusal on the other is the differential-square failure the plan calls a spec
bug by definition, and the reason the fix belongs in the verifier and not in
either executor: neither is wrong about its own anchor — the document is.

The annotation is INERT in both OCaml executors for a set source: `ref`'s
set-source arm binds no `objecttype` at all, and the ASP backend anchors every
read at the element variable. That is not an argument for leaving the field
free — it is the argument for pinning it, because a field nobody reads is a
field that can say anything, and this document is what "anything" buys.

Hand-authored, from the temporal-total golden's declarations: one rule, one
set operand, one attribute that lives on the wrong side of the relation.

Falsifiable, arm-exact: in `aggregation_source_check`, let the anchor of the
cross fall back to the operand itself in the set case (`let anchor = if set
then Some otn else subject`). Measured over golden + rood + negatief + parity
— 508 documents — exactly one report changes and this case, alone, turns
clean; its non-set sibling `wf083-vouwbron-vreemd-objecttype` stays rejected,
and so do the two self-denotation cases.
