# jutterswet-vaarwaterspelling — the ci witness of WF-088/WF-089 until L2

A `Kadeplaats` has a `vaarwater` of the declared enumeration domain
`Vaarwater` (`Wad`, `Zeegat`, `Ruimzee`) and a `kadegeld` a `Match` over
that attribute derives: `Wad` → 2, `Zeegat` → 5, `Ruimzee` → 9.

The single point of the case, until L2: the `Match` scrutinee — an
`AttribuutRef` reading `vaarwater` — spelled its domain **`VAARWATER`**, in
another case than the declaration's `Vaarwater`. Every value label was
repeated exactly, in declaration order, so WF-088 was satisfied on its own
terms; the only difference was the case of the domain NAME. Since L2 a
reference carries no type of its own (B13), so the drift has no wire
position left on the scrutinee: the spelling `VAARWATER` survives only in
the `Match`'s source snippet (`match VAARWATER`), measured 2026-09-25.

What the document pins since L2, measured 2026-09-25 with two verifier
mutants over the 293 golden and red documents: comparing domain names byte
for byte in WF-089 and WF-090 turns none of them red, this golden included,
so the ci clause below has no golden witness left; refusing every `Match`
over a declared enumeration turns exactly two red, this golden and
`jutterswet-vaarwatertoeslag-catala`, whose rule `berekening vaarwatertarief`
has the same shape — a `Match` over the enumeration attribute `vaarwater`
with three nullary arms. Every executor treats the two alike: `ref` and the
ASP backend replay both, and the Drools backend refuses both at load
(`Expr:Match`). This golden therefore pins nothing the catala golden does
not pin as well.

Why the case exists: WF-089 (domain identity, `spec/IR-SPEC.md` ch. 2.4)
compares a use site's domain against the declaration's **ci**, following
WF-001's naming rule, and WF-023 does the same at every write position.
That ci clause was normative but unpinned — making the comparison
case-sensitive left the whole negatief corpus red and the whole golden
corpus clean, so nothing failed. Until L2 this document was the falsifier:
it was verifier-clean only because the comparison is ci.

    Falsifiable: drop the ci normalisation from the domain-identity
    comparison in the verifier (compare the two domain names byte for
    byte) and this golden alone turned red — measured before L2:

        FAIL: document corpus/golden/jutterswet-vaarwaterspelling/ir.json
        is not verifier-clean:
        WF-089 FAIL /rules/resolvedRegels/0/waarde/scrutinee/type: attribute
        reference 'vaarwater' is typed as the enumeration domain
        'VAARWATER', but the declaration it reads from is the enumeration
        domain 'Vaarwater' (one namespace: a domain has one variant)

    while every negatief case keeps failing for its own declared rule.
    The report carries the pointer of the wire it was taken on; since L4 a
    rule's value sits at `/rules/<list>/<r>/versions/<v>/value` (delta B19),
    and since L5 that list is `rules` (delta B6).

Deliberately carries no `bron.rgs` and no `bron.catala_en`: the drift is
a wire-level spelling that neither frontend emits — both derive a
reference's type from the declaration — so a source file would have to be
round-tripped away. Hand-authored, in the Jutterswet's own vocabulary.
The two case files pin absolute derived values (5 and 9), measured with
the reference interpreter and confirmed by the ASP backend, so the
identity transpiler fails on it: `kadegeld` is absent from the input.
