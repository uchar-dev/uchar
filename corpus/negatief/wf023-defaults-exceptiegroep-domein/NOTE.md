Violates WF-023 (`spec/IR-SPEC.md` ch. 3.1) at its third write position,
**inside an exception group**: the `PrioritizedDefaults` on
`Berging.vaarwater` (declared `Vaarwater`) has a truthful root
consequence (`Wad`), and one exception group whose default — guarded by
the characteristic `uitzonderlijk` — writes the `Literal` `Noord` of
domain `Kust`. A rule reads `vaarwater` faithfully through a `Match`.

    WF-023 FAIL
    /rules/prioritizedDefaults/0/defaults/0/exceptions/members/0/defaults/0/consequence:
    a value of the enumeration domain 'Kust' is assigned to a target of
    the enumeration domain 'Vaarwater'

Why it has to exist: `wf023-defaults-domein` and `wf023-defaults-soort`
both sit on the ROOT consequence, so the recursion into exception groups
— which inherit the root target (ch. 7.9) — was normative in the clause
and in the rule index but pinned by nothing. ref faults at the reader
(`match has no arm for enumeration value "Noord"`); clingo faults the
translation (`unscalable datatype`, the pre-existing fence on enumeration
defaults, which the faithful twin hits identically).

Falsifiable: make the exception-group recursion in `wf023` return `[]`
(keep the root consequence) and this case alone turns clean, while
`wf023-defaults-domein` and `wf023-defaults-soort` stay red.
