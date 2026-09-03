Violates WF-090 (`spec/IR-SPEC.md` ch. 2.4) on the READ side, the
`AttributeReference` position: the reference reads `Jutter.dossier`,
declared with the struct domain `dossier`, but its `type` names the second
declared struct domain `logboek`, and the `FieldAccess` over it reads
`bedrag` — a declared field of `logboek`, so WF-074 (which resolves the
domain the BASE names) is satisfied.

    WF-090 FAIL /resolvedRegels/5/waarde/base/type: attribute reference
    'dossier' is typed with struct domain 'logboek', but the declaration it
    reads from names struct domain 'dossier'

Why it has to exist beside the write cases: the read half and the write
half are independent, exactly as they were for keuzetypes (WF-089 needed
WF-023 and vice versa). Here every write in the document is faithful and
the lie sits in the reference: verifier-clean before, ref faulted (`field
"bedrag" is not present on the record value`) and clingo left
`J1.waardekopie` underived where 120.50 was expected.

Falsifiable, arm-exact: call `wf089_domain_identity` instead of
`use_site_identity` in the `Attribuut_ref` arm of `walk_expr` and this case
turns clean together with `wf090-attribuutref-lijst` (the same arm, one
`ListType` deeper), while the binder, parameter, dimension-cell,
fold-source, aggregation and write cases stay red.
