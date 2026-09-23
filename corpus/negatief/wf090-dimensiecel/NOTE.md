Violates WF-090 (`spec/IR-SPEC.md` ch. 2.4) on the READ side, the
`DimensionCellRead` position: a cell read of the dimensioned struct
attribute `Berging.kern` (dimension `seizoen`, cell `voorjaar`) carries a
`type` naming `logboek` while the attribute — and the inner
`AttribuutRef` — are `dossier`. A cell of a dimensioned attribute carries
that attribute's datatype, so the cell read's own `type` is a use site of
the same declaration.

    WF-090 FAIL /rules/resolvedRegels/0/waarde/base/type: dimension-cell read of
    'kern' is typed with struct domain 'logboek', but the declaration it
    reads from names struct domain 'dossier'

Why it has to exist: the `Dimensie_cel` arm checks its labels (WF-040) and
walks the inner reference, but the cell's own `type` has no structural
cross at all — the inner reference being faithful, the `Attribuut_ref` arm
has nothing to say. Like its keuzetype twin `wf089-dimensiecel-enum` this
is a **verifier-only** flank: the case format carries no dimension-cell
input, so no executor witness is possible and the rule is the only fence.

Falsifiable, arm-exact: call `wf089_domain_identity` instead of
`use_site_identity` in the `Dimensie_cel` arm of `walk_expr` and this case
alone turns clean; every other WF-090 case stays red.
