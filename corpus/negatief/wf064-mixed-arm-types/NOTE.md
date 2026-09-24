Violates WF-064 (`spec/IR-SPEC.md` ch. 4.7): the `Afgewezen` arm body
is `Tekst`, while the `Match`'s result type (and the other arm bodies)
is `Number`. All arm bodies must agree on one result datatype — the
`Match`'s own `type`.

A minimal hand-built document.
