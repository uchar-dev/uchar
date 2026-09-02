Violates WF-089 (`spec/IR-SPEC.md` ch. 2.4) at its **aggregation
position**, the ordering half of the family: `MINIMUM(kust van alle
schuiten van de Berging)` carries a node `type` naming the enumeration
domain `Vaarwater` while its operand — `kust` of `Schuit` — names `Kust`.
The document is the `EERSTE` case (`wf089-aggregatie-doorgeefluik-enum`)
with the kind swapped, and it pins that the clause covers the whole
element-passing family `EERSTE`/`LAATSTE`/`MINIMUM`/`MAXIMUM`, not just
the two positional folds.

    WF-089 FAIL /resolvedRegels/0/waarde/type: the MINIMUM aggregation is
    typed with keuzetype domain 'Vaarwater', but operand 0 names domain
    'Kust'

Why it has to exist: with one contributor `MINIMUM` performs no
comparison whatsoever — it returns the single element unchanged — so the
ordering kinds launder an alien label exactly like the positional ones.
Verifier-clean before the rule; ref faulted `match has no arm for
enumeration value "Noord"` at the truthful reader, clingo refused the
translation on its own carrier fence for enum operands.

Note for the reader: whether an ordering fold over an equality-only
keuzetype should be ill-formed *by itself* (ch. 2.4 gives Enumeration
and SumType no ordering) is a separate question, recorded as an open
point on the card; this case pins only the domain identity.

Falsifiable: shrink `passthrough_folds` in the verifier to `EERSTE`/
`LAATSTE` and this case alone turns clean while
`wf089-aggregatie-doorgeefluik-enum` stays red; make the whole
passthrough cross return `[]` and both turn clean together.
