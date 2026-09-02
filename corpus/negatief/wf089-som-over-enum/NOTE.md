Violates WF-089 (`spec/IR-SPEC.md` ch. 2.4) on the VARIANT half of the
identity, mirror direction: the attribute `vaarwater` of `Berging` is
declared `Enumeratie` of domain `Vaarwater`, and the comparison
`vaarwater = Wad` types both the `AttribuutRef` and the `Literal` as a
`SumType` of domain `Vaarwater` with three nullary constructors.

    WF-089 FAIL /resolvedRegels/0/voorwaarde/links/type: attribute
    reference 'vaarwater' is typed as the sum-type domain 'Vaarwater', but
    the declaration it reads from is the enumeration domain 'Vaarwater'
    (one namespace: a domain has one variant)

Why it has to exist: verifier-clean with names compared alone (the same
gaps as `wf089-enum-over-som`); ref faulted `literal of this datatype not
implemented: "Wad"`, clingo faulted the translation (`a sum-typed operand
here is an own-subject attribute read or a Construct`).

Falsifiable: make `keuzetype_same` compare names only and this case passes
together with `wf089-enum-over-som`.
