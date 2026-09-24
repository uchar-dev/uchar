Violates WF-023 (`spec/IR-SPEC.md` ch. 3.1) on the mirror one-sided arm
of its domain clause: the attribute `bedragen` of `Jutter` is declared
`list of number`, and the rule writes a `ListLiteral` typed `list of
oordeel` with two `Construct`s. The family clause is silent (list = list);
only the value names a keuzetype domain.

    WF-023 FAIL /rules/resolvedRegels/0/versions/0: a value of the sum-type
    domain 'oordeel' is assigned to a list target naming no keuzetype domain

Why it has to exist: the silent flank — no reader faults, nothing is
underived: ref wrote the constructors into the numeric list
(`J1.bedragen = [Goedgekeurd(10), Verworpen]` where numbers were expected),
a wrong value with no fault; clingo faulted the translation (`unscalable
datatype`). The arm pins the direction in which the declaration, not the
value, is the side without a domain.

Falsifiable: make the two one-sided arms of `wf023_assignment` return `[]`
and this case passes together with
`wf023-schrijf-lijst-geen-domein-waarde`.
