Violates WF-023 (`spec/IR-SPEC.md` ch. 3.1) on its domain clause, sum-type
variant — the card's F13b flank: the attribute `beschikking` of `Jutter`
is declared `SumType` of domain `beschikking`, and the rule writes a
`Construct` of the constructor `Verworpen` of the OTHER declared sum type
`oordeel` (WF-062 checks the Construct against its own domain, which is
consistent). A second rule reads `beschikking` faithfully through a
`Match` whose arms cover `beschikking`.

    WF-023 FAIL /rules/rules/0/versions/0: a value of the sum-type
    domain 'oordeel' is assigned to a target of the sum-type domain
    'beschikking'

Why it has to exist: WF-023 compared families only (sumtype = sumtype,
clean); ref wrote the alien constructor and then faulted `match has no arm
for constructor "Verworpen"` at the faithful reader; clingo left
`J1.uitkering` underived with a non-leeg scrutinee. Drools does not carry
the sum type (auto-exclusion).

Falsifiable: make the two-sided domain-mismatch branch of
`wf023_assignment` return `[]` and this case passes.
