Violates WF-063 (`spec/IR-SPEC.md` ch. 4.7), the enumeration arm of the
rule: the `beschadigd` arm of the `Match` over the `keurklasse`-typed
`staat` carries a binder `schade`. Enumeration alternatives are nullary
by construction (ch. 2.4 — that is what makes them the nullary wire
form), so an enumeration arm's `binder` is ALWAYS null; there is no
payload for it to name.

A minimal hand-built document.
