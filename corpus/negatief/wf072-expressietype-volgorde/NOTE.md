Violates WF-072 (`spec/IR-SPEC.md` ch. 2.4) at the expression layer: a
`StructLiteral` of the domain `dossier` carries as its `type` an inline copy
of the domain with the declared fields in REVERSED order (`telling`,
`waarde` where the declaration says `waarde`, `telling`). Same domain name,
same field names, same datatypes — only the order drifted, and declaration
order is normative. A consumer that encodes a struct term positionally out
of the inline list reads the fields crosswise.

    WF-072 FAIL /rules/consistencies/0/versions/0/assertion/operand/base/type: struct-type
    use site disagrees with the declaration of domain 'dossier' (same fields,
    datatypes and order required)

**Rewritten at L2 (2026-09-24), stamp: herschreven (i).** Until L2 the
drifted copy sat on the `type` of an `AttribuutRef` reading `dossier` — the
position this witness was written for. L2 took the annotation off the five
reference classes (B13), so the migration's translation was verifier-clean.
The expression-layer arm of WF-072 stands wherever a node still carries its
own type; written by hand, the base of the field access is now a
`StructLiteral`, whose `type` is required and read. `expect.txt` pins the
arm.
