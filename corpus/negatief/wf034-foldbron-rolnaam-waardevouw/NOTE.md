Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6): a **fold source** outside the
row-1 wart naming a **declared role** instead of an attribute — the other
branch of the ch. 4.3 exception applied beyond its scoping.

    WF-034 FAIL /resolvedRegels/1/waarde/argumenten/0: 'vondsten' is not a
    declared attribute of 'Vondst': outside the v0 instance-fold source (the
    unknown-typed row-1 wart) a fold-source argument is an ordinary attribute
    reference — the ch. 4.3 role-name exception and its type-pair fallback do
    not reach here

The scoping argument is the one set out in full in the sibling NOTE
(`wf034-foldbron-onbekend-attribuut`): WF-034's single exception is the
fold-source **role** name, and ch. 4.3 scopes it to the v0 instance-fold
source — the row-1 wart, spelled on the wire by the unknown sentinel on the
argument's own `type` and keyed on exactly that sentinel by both executors.
The exception has two branches, and this document pins the **first** one:
resolution against a declared role name or declared plural, before the
type-pair fallback is ever consulted.

The document. `corpus/golden/jutterswet-vondsttelling` sums the `gewicht` of a
Jutter's reported finds; the source argument carries a real datatype (kg, two
decimals), so it is no wart. Repointing it at `vondsten` — the **declared
plural** of the role `vondst` in the fact type `vondstaanmelding` — left the
document verifier-clean, because the role-name route accepted the name without
asking whether this position wants a role at all. Measured against
`corpus/golden/jutterswet-vondsttelling/case.json`:

    ref    : MISMATCH: J1.aangemeld gewicht = 0, expected 3.75
    clingo : TRANSLATION FAULT: unknown attribute "vondsten" on "Vondst"

`ref` does not fault: the finds are found, each is asked for `vondsten`, each
answers *leeg*, and the sum is 0 where 3.75 kg is pinned. The ASP backend
refuses. The same silent-versus-refusing split as the sibling.

The choice of golden is deliberate and is what makes this witness carry its
own branch. On this document the far pair is `Vondst`/`Vondst`, and **no** fact
type relates `Vondst` to itself — so the type-pair fallback derives zero
candidates and cannot stand in. Only the role-name route ever admitted this
name. (On the sibling's `anchor/jutterswet-maatschap` the reciprocal
self-relation makes the fallback succeed for any name at all, so a witness
placed there would be admitted by either branch and would pin neither.)

*Measured breadth of the shape itself:* zero outside this witness and its
sibling. Over all 483 documents of golden + rood + negatief + parity the
narrowing changes exactly those two verdicts and nothing else; the 32
wart sources in the corpus — among them the counting folds of
`jutterswet-kaveltelling`, `jutterswet-aanmeldertelling` and this very
golden's first rule — keep both routes and replay unchanged.

A hand-broken variant of `corpus/golden/jutterswet-vondsttelling/ir.json` —
one name repointed, a one-line diff; schema-valid, since
`AttribuutRef.attribuut` is `["string","null"]` on the v0 wire.

Falsifiable, branch-exact: in `aggregation_source_check` — the fold source's
own arm, which replaces the generic `walk_expr` for this position — drop the
wart guard from the role-name test alone (`has_attr ot a || is_role_name ctx a`
instead of `has_attr ot a || (wart && is_role_name ctx a)`). This case, alone
in all four zones, turns clean; its type-pair sibling stays rejected. Measured:
483 documents compared, 1 report changes.
