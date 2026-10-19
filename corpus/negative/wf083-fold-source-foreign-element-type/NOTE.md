# wf083-fold-source-foreign-element-type

Violates WF-083 (`spec/IR-SPEC.md` ch. 4.3), the **anchor cross**, in its set
half: a set source reads each member of the `elementType`, so its
`subjectType` must name that element type. Here it names the fold subject
instead, and the attribute it spells is declared there and nowhere near the
members being folded.

    WF-083 FAIL /rules/rules/0/versions/0/value/arguments/0: fold-source
    subjectType 'Jutter' is not the fold's anchor 'Vondst' (a set source reads
    each member of the elementType): the executors resolve 'kistgeld' against
    the anchor, never against this field, so the two spellings name different
    declarations

The document. A Jutter-subject `total` over the Jutter's finds —
`elementType: "Vondst"`, `sourceRole` bound to the role `vondst` (which makes
the operand the set source, IR-SPEC ch. 4.3), no navigation — whose operand
names `kistgeld` with `subjectType: "Jutter"`. `Jutter` declares `kistgeld`;
`Vondst` does not. Every declaration cross in the document is satisfied, which
is exactly why the form was verifier-clean.

Why this one is the worst of the four. It is the case where the two OCaml
executors DISAGREE under a well-formed document:

    ref         : oracle PASS — bergloon = 0
    ASP backend : TRANSLATION FAULT: unknown attribute "kistgeld" on "Vondst"
    Drools      : ERROR ConstraintEvaluationException

`ref` folds each member, asks every `Vondst` for `kistgeld`, gets *leeg* from
all of them, and totals 0 — silently, because 0 is what a `total` over an
empty contribution set is. The ASP backend looks the attribute up on the
element type, the anchor it names in its own code, and refuses. The bean-model
executor throws inside the accumulate. A silent number on one side and a loud
refusal on the other is the differential-square failure the plan calls a spec
bug by definition, and the reason the fix belongs in the verifier and not in
either executor: neither is wrong about its own anchor — the document is.

The annotation is INERT in both OCaml executors for a set source: `ref`'s
set-source arm binds no `subjectType` at all, and the ASP backend anchors every
read at the element variable. That is not an argument for leaving the field
free — it is the argument for pinning it, because a field nobody reads is a
field that can say anything, and this document is what "anything" buys.

Hand-authored, from the temporal-total golden's declarations: one rule, one
set operand, one attribute that lives on the wrong side of the relation.

Falsifiable, arm-exact: in `aggregation_source_check`, let the anchor of the
cross fall back to the operand itself in the set case (`let anchor = if set
then Some otn else subject`). Measured over golden + red + negative + parity
— 508 documents — exactly one report changes and this case, alone, turns
clean; its non-set sibling `wf083-fold-source-foreign-object-type` stays rejected,
and so do the two self-denotation cases.

**Migrated at L2 (2026-09-24), stamp: herschreven (m).** The document is the
migration's translation (`spec/proposals/v1-migratie/m2.py`): the set source
lost its own `type` and set reading (B11, B13); it is the set source because
the fold carries a range (IR-SPEC ch. 4.3), so its anchor is the element type,
and its `subjectType` still names the fold subject. The rule still fires on the
same arm.

Since L4 the node is an `Aggregation` or a `BuiltinCall` (delta B21), each
with only its own fields — `arguments`, and on an aggregation `elementType`,
`emptyFallback`, `dimensionSelection`, `sourceRole` and `sourcePath`, on a
builtin `granularity`; the report cites and names them. `expect.txt` pins the
arm.
