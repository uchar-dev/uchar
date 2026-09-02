# jutterswet-vaarwaterspelling — the ci witness of WF-088/WF-089

A `Kadeplaats` has a `vaarwater` of the declared enumeration domain
`Vaarwater` (`Wad`, `Zeegat`, `Ruimzee`) and a `kadegeld` a `Match` over
that attribute derives: `Wad` → 2, `Zeegat` → 5, `Ruimzee` → 9.

The single point of the case: the `Match` scrutinee — an `AttribuutRef`
reading `vaarwater` — spells its domain **`VAARWATER`**, in another case
than the declaration's `Vaarwater`. Every value label is repeated
exactly, in declaration order, so WF-088 is satisfied on its own terms;
the only difference is the case of the domain NAME.

Why the case exists: WF-089 (domain identity, `spec/IR-SPEC.md` ch. 2.4)
compares a use site's domain against the declaration's **ci**, following
WF-001's naming rule, and WF-023 does the same at every write position.
That ci clause was normative but unpinned — making the comparison
case-sensitive left the whole negatief corpus red and the whole golden
corpus clean, so nothing failed. This document is the falsifier: it is
verifier-clean only because the comparison is ci.

    Falsifiable: drop the ci normalisation from the domain-identity
    comparison in the verifier (compare the two domain names byte for
    byte) and this golden alone turns red — measured:

        FAIL: document corpus/golden/jutterswet-vaarwaterspelling/ir.json
        is not verifier-clean:
        WF-089 FAIL /resolvedRegels/0/waarde/scrutinee/type: attribute
        reference 'vaarwater' is typed as the enumeration domain
        'VAARWATER', but the declaration it reads from is the enumeration
        domain 'Vaarwater' (one namespace: a domain has one variant)

    while every negatief case keeps failing for its own declared rule.

Deliberately carries no `bron.rgs` and no `bron.catala_en`: the drift is
a wire-level spelling that neither frontend emits — both derive a
reference's type from the declaration — so a source file would have to be
round-tripped away. Hand-authored, in the Jutterswet's own vocabulary.
The two case files pin absolute derived values (5 and 9), measured with
the reference interpreter and confirmed by the ASP backend, so the
identity transpiler fails on it: `kadegeld` is absent from the input.
