Violates WF-089 (`spec/IR-SPEC.md` ch. 2.4) on the VARIANT half of the
identity: the attribute `beschikking` of `Jutter` is declared `SumType` of
domain `beschikking`, and the rule `uitkering := 5.00 indien beschikking =
Afgewezen` types both the `AttribuutRef` and the `Literal` as
`Enumeratie` of domain `beschikking` — the same name, the other keuzetype
variant. `domeinen` is one namespace, so a domain has one variant and the
reference lies.

    WF-089 FAIL /resolvedRegels/0/voorwaarde/links/type: attribute
    reference 'beschikking' is typed as the enumeration domain
    'beschikking', but the declaration it reads from is the sum-type
    domain 'beschikking' (one namespace: a domain has one variant)

Why it has to exist: with names compared alone the document was
verifier-clean — WF-088 calls an enumeration use site naming no declared
ENUMERATION domain "not ours", WF-036 sees two operands of one variant
and one domain, WF-061 walks declarations only. As a `Match` scrutinee the
same reference is caught by WF-063 ("not a declared enumeration domain");
only the comparison position leaked. Measured: ref faulted `mixed-type
equality: Afgewezen vs Afgewezen`, clingo faulted the translation — two
faults, no semantics, one verifier-clean document.

Falsifiable: make `keuzetype_same` compare names only (drop the variant
match) and this case passes together with `wf089-som-over-enum`, while
every same-variant case stays red.
