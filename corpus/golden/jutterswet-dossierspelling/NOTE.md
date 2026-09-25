# jutterswet-dossierspelling — the ci witness of WF-090

A `Jutter` has a `dossier` of the declared struct domain `dossier`
(`waarde`, `telling`); `waardekopie` copies the `waarde` field out of it
and `spiegel` is a `StructLiteral` rebuilding the same record.

The single point of the case: both struct use sites spell their domain
**`DOSSIER`**, in another case than the declaration's `dossier`. The
field list is repeated exactly — same names, same datatypes, same order
— so WF-072 is satisfied on its own terms; the only difference is the
case of the domain NAME, at both halves of the identity rule:

- the READ half, until L2: the `AttribuutRef` reading `dossier` was typed
  `DOSSIER` (the `type` of the reference at `/rules/resolvedRegels/0/waarde/base`
  on the wire of that time); since L2 a reference carries no type of its own
  (B13), so this half has no wire position left;
- the WRITE half, `/rules/rules/1/versions/0/value/type`: the
  `StructLiteral` assigned into `spiegel` is typed `DOSSIER` while the target
  attribute declares `dossier`.

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
        WF-090 FAIL /rules/resolvedRegels/1: a value of the struct domain
        'DOSSIER' is assigned to a target of the struct domain 'dossier'
        WF-090 FAIL /rules/resolvedRegels/0/waarde/base/type: attribute
        reference 'dossier' is typed with struct domain 'DOSSIER', but
        the declaration it reads from names struct domain 'dossier'

    while every other golden stays clean and every negatief case keeps
    failing for its own declared rule.

What does NOT pin it, both variants measured under that same mutant:

- a UNIFORM respelling — every `StructType` on the wire on `DOSSIER`,
  the key in `domeinen` still `dossier` — is mutant-clean
  (`verifier: PASS`), because it exercises WF-072's ci lookup into
  `domeinen` rather than WF-090's crossing;
- a PARTIAL respelling of only the two attribute declarations
  (`dossier`, `spiegel`) does not leave the document clean either — it
  turns it red at two OTHER positions, because the two `FieldAccess`
  bases inside the `StructLiteral` still spell `dossier` and become the
  drifted use sites themselves:

        WF-090 FAIL /rules/resolvedRegels/1/waarde/fields/0/value/base/type:
        attribute reference 'dossier' is typed with struct domain
        'dossier', but the declaration it reads from names struct domain
        'DOSSIER'
        WF-090 FAIL /rules/resolvedRegels/1/waarde/fields/1/value/base/type:
        ... (idem)

Both measured reports and the two variants carry the names of the wire they
were taken on: since M1 the model's `domeinen` map is the list `domains`,
since L4 a rule's value sits at `/rules/<list>/<r>/versions/<v>/value` (delta
B19), and since L5 that list is `rules` (delta B6).

Only drift at a use site against an unchanged declaration pins the ci
clause — which is what this document, and only this document, carries.

Deliberately carries no `bron.rgs` and no `bron.catala_en`: the drift is
a wire-level spelling that neither frontend emits — both derive a
reference's and a literal's type from the declaration — so a source file
would have to be round-tripped away. Hand-authored, in the Jutterswet's
own vocabulary. The case file pins absolute derived values (120.50 and
the record `{120.50, 3}`), measured with the reference interpreter and
confirmed by the ASP backend, so the identity transpiler fails on it:
`waardekopie` and `spiegel` are absent from the input.
