Violates WF-066 (`spec/IR-SPEC.md` ch. 4.8): the `ListLiteral` declares an
element datatype of `Number` (2 decimals), but its second element is a
`Tekst` literal. Every element's static type MUST equal the literal's
declared element datatype.

A minimal hand-built document; the `ListLiteral` rides a `GEVULD` predicate
so the verifier's expression walk reaches it.
