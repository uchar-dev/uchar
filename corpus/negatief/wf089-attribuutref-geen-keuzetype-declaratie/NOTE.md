Violates WF-089 (`spec/IR-SPEC.md` ch. 2.4) on the one-sided arm the
mirror case `wf089-attribuutref-geen-domein` does NOT pin: the reference
names a keuzetype domain, the declaration names none. The rule `toeslag :=
Match(tarief)` reads the NUMERIC attribute `tarief` of `Berging` through
an `AttribuutRef` typed `Enumeratie` `Kust`, with arms `Noord`/`Zuid`/
`West` (WF-063 is satisfied against `Kust`).

    WF-089 FAIL /rules/resolvedRegels/0/waarde/scrutinee/type: attribute
    reference 'tarief' is typed with keuzetype domain 'Kust', but the
    declaration it reads from is number and names no keuzetype domain

Why it has to exist: the card's flank F15. Verifier-clean before WF-089;
ref faulted `match scrutinee is 4, not a constructor value`; clingo
faulted the translation (the text/enum/boolean operand fence); Drools
auto-excludes the `Match`. The two one-sided arms of
`wf089_domain_identity` are separate lines: silencing only `Some _, None`
(this direction) left gate 3 green before this case existed, because
`wf089-attribuutref-geen-domein` exercises only `None, Some _`.

Falsifiable: make the `Some _, None` arm alone return `[]` and this case
passes while `wf089-attribuutref-geen-domein` and every two-sided case
stay red.
