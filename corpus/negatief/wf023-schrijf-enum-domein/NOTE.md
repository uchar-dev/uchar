Violates WF-023 (`spec/IR-SPEC.md` ch. 3.1) on its domain clause — the
write half of the domain identity WF-089 reads: the rule target is the
attribute `vaarwater` of `Berging`, declared `Enumeratie` of domain
`Vaarwater`, and the value is a `Literal` `Noord` typed `Enumeratie` of
domain `Kust` — a second, internally consistent domain (the inline copy is
faithful to `Kust`, so WF-088 is silent). A second rule reads `vaarwater`
faithfully (scrutinee typed `Vaarwater`, arms `Wad`/`Zeegat`/`Ruimzee`),
so WF-089 is rightly silent too.

    WF-023 FAIL /rules/resolvedRegels/0/versions/0: a value of the enumeration
    domain 'Kust' is assigned to a target of the enumeration domain 'Vaarwater'

Why it has to exist: before the domain clause, WF-023 compared FAMILIES
only (enumeration = enumeration, clean) and nothing crossed the written
value's domain against the declaration it enters — so the label `Noord`
reached the faithful reader, and ref faulted `match has no arm for
enumeration value "Noord"` on a verifier-clean document while the message
claimed unreachability; clingo derived nothing (`B.tarief is underived
(leeg), expected 3` — leeg for the wrong reason, with a non-leeg
scrutinee). The read side alone (WF-089) never sees this: the reader is
truthful. Drools does not carry the reader (`Match` auto-exclusion).

Falsifiable: make the two-sided domain-mismatch branch of
`wf023_assignment` (`Some a, Some b` after `keuzetype_same`) return `[]`
and this case passes, together with every other `wf023-*-domein` case,
while the soort cases stay red.
