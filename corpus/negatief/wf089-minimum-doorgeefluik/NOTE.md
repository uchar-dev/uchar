Violates WF-089 (`spec/IR-SPEC.md` ch. 2.4) at its **aggregation
position**, the ordering half of the family: `minimum(kust van alle
schuiten van de Berging)` carries a node `type` naming the enumeration
domain `Vaarwater` while its operand — `kust` of `Schuit` — names `Kust`.
The document is the `first` case (`wf089-aggregatie-doorgeefluik-enum`)
with the kind swapped, and it pins that the clause covers the whole
element-passing family `first`/`last`/`minimum`/`maximum`, not just
the two positional folds.

    WF-089 FAIL /rules/rules/0/versions/0/value/type: the minimum
    aggregation is typed with keuzetype domain 'Vaarwater', but operand 0 names
    domain 'Kust'

Why it has to exist: with one contributor `minimum` performs no
comparison whatsoever — it returns the single element unchanged — so the
ordering kinds launder an alien label exactly like the positional ones.
Verifier-clean before the rule; ref faulted `match has no arm for
enumeration value "Noord"` at the truthful reader, clingo refused the
translation on its own carrier fence for enum operands.

Note for the reader, answered since: whether an ordering fold over an
equality-only keuzetype is ill-formed *by itself* (ch. 2.4 gives
Enumeration and SumType no ordering) was recorded here as an open point.
It is closed — WF-038's ordering-fold operand clause (2026-09-05) refuses
it, so this document now carries two independent defects. This case
still pins only the domain identity; `wf038-minimum-enum-setbron` pins
the other one on a document whose domains are truthful throughout.

Falsifiable: shrink `passthrough_folds` in the verifier to `first`/`last` and
this case turns clean while `wf089-aggregatie-doorgeefluik-enum` stays red.
Re-measured after the ordering-fold clause, which reads the same list
(2026-09-05): that mutant now frees three documents together — this one and the
two set-source ordering-fold negatiefs `wf038-minimum-enum-setbron` and
`wf038-maximum-somtype-setbron` — so it is a shared-machinery mutant, not an
arm-exact one. The arm-exact mutant for this document is the other sentence:
make the whole passthrough cross return `[]` and the WF-089 line disappears here
and on `wf089-aggregatie-doorgeefluik-enum` (and the WF-090 line on
`wf090-doorgeefluik`), while neither of the two enumeration cases turns clean,
both being ordering folds over an equality-only contributor.

**Migrated at L2 (2026-09-24), stamp: herschreven (m).** The document is the
migration's translation (`spec/proposals/v1-migratie/m2.py`): the operand
references lost their own `type` (B13), and the aggregation's annotation, a
derived position, keeps its (V5 (a)); operand 0 is now typed by the
attribute it reads. The rule still fires on the same arm, and `expect.txt`
pins it.
