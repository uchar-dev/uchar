Violates WF-034 (`spec/IR-SPEC.md` ch. 2.4 name resolution) at the quantifier
fold's `filter` position (ch. 4.5): the §5.6 subselectie reads `stilliggend`,
which is a declared characteristic of the fold's SUBJECT type `Keurploeg` and
not of its element type `Keurmeester`.

The rule this pins is the re-anchoring, not the spelling. ch. 4.5 evaluates
`filter` PER CONTRIBUTOR with the ELEMENT as subject, so every name in it
resolves against the element — the same re-anchoring the instance fold's filter
gets, reached through the same walk. A verifier that read the subselectie
against the fold's own subject would let this document through, and it would
then quantify over a set selected by a characteristic no contributor can carry.

The positive twin is `corpus/golden/jutterswet-keurploeg`, whose filter reads
`aangemeld` — a characteristic of the Keurmeester — and plays in all three
executors.

Verifier: `WF-034 FAIL /rules/rules/0/versions/0/condition/filter: 'stilliggend' is
not a declared characteristic of 'Keurmeester'`.
