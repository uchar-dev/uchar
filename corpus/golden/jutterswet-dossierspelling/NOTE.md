# jutterswet-dossierspelling — the ci witness of WF-090

A `Jutter` has a `dossier` of the declared struct domain `dossier`
(`waarde`, `telling`); `waardekopie` copies the `waarde` field out of it
and `spiegel` is a `StructLiteral` rebuilding the same record.

The single point of the case: both struct use sites spell their domain
**`DOSSIER`**, in another case than the declaration's `dossier`. The
field list is repeated exactly — same names, same datatypes, same order
— so WF-072 is satisfied on its own terms; the only difference is the
case of the domain NAME, at both halves of the identity rule:

- the READ half, `/resolvedRegels/0/waarde/base/type`: the
  `AttribuutRef` reading `dossier` is typed `DOSSIER`;
- the WRITE half, `/resolvedRegels/1/waarde/type`: the `StructLiteral`
  assigned into `spiegel` is typed `DOSSIER` while the target attribute
  declares `dossier`.

Why the case exists: WF-090 (struct-domain identity,
`spec/IR-SPEC.md` ch. 2.4) compares a use site's struct domain against
the declaration's **ci**, following WF-001's naming rule, at every read
position and at all three write positions. That ci clause was normative
but unpinned — no golden, rood or parity document spelled a struct use
site differently from its declaration, so making the comparison
case-sensitive left the whole corpus green and nothing failed. This
document is the falsifier: it is verifier-clean only because the
comparison is ci. It is what `jutterswet-vaarwaterspelling` is for
WF-089.

    Falsifiable: drop the ci normalisation from BOTH halves of the
    struct-domain comparison in the verifier (`norm a = norm b` ->
    `a = b` in `wf090_struct_identity` and in the struct clamp of
    `wf023_assignment`) and this golden alone turns red — measured, both
    halves at once:

        FAIL: document corpus/golden/jutterswet-dossierspelling/ir.json
        is not verifier-clean:
        WF-090 FAIL /resolvedRegels/1: a value of the struct domain
        'DOSSIER' is assigned to a target of the struct domain 'dossier'
        WF-090 FAIL /resolvedRegels/0/waarde/base/type: attribute
        reference 'dossier' is typed with struct domain 'DOSSIER', but
        the declaration it reads from names struct domain 'dossier'

    while every other golden stays clean and every negatief case keeps
    failing for its own declared rule.

A drift on the declaration side would NOT pin it: rewriting the
attribute declarations to `DOSSIER` as well leaves the document clean
under that mutant, because it exercises WF-072's ci lookup into
`domeinen` rather than WF-090's crossing. Only drift at a use site
against an unchanged declaration does.

Deliberately carries no `bron.rgs` and no `bron.catala_en`: the drift is
a wire-level spelling that neither frontend emits — both derive a
reference's and a literal's type from the declaration — so a source file
would have to be round-tripped away. Hand-authored, in the Jutterswet's
own vocabulary. The case file pins absolute derived values (120.50 and
the record `{120.50, 3}`), measured with the reference interpreter and
confirmed by the ASP backend, so the identity transpiler fails on it:
`waardekopie` and `spiegel` are absent from the input.
