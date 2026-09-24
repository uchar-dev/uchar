Violates WF-023 (`spec/IR-SPEC.md` ch. 3.1) at its first write position
in the third **rule kind**: an initialisation, a version with
`initialisation: true`, writes the `Literal` `Noord` of domain `Kust` into
`Berging.vaarwater`, declared `Vaarwater`.
A second rule reads `vaarwater` faithfully through a `Match`. The
document is `wf023-schrijf-enum-domein` with the rule kind swapped.

    WF-023 FAIL /rules/resolvedRegels/0/versions/0: a value of the enumeration
    domain 'Kust' is assigned to a target of the enumeration domain 'Vaarwater'

Why it has to exist: write position 1 admits the two presentations
`GELIJKSTELLING` and `BEREKENING` and the initialisation (a characteristic
attribution, a version with neither, is WF-022's, whose value is the boolean
constant), but every existing write case used one of the first two — dropping
the initialisation from the rule-kind set left gate 3 green. ref faults at the
reader (`match has no arm for enumeration value "Noord"`); clingo faults the
translation (`unscalable datatype`, the pre-existing enum fence).

Falsifiable: drop `Initialisatie` from the rule-kind set of write
position 1 in `wf023` and this case alone turns clean, while every
`GELIJKSTELLING`/`BEREKENING` write case stays red.

Since L4 the initialisation is a version with `initialisation: true` and no
`presentation` (delta B20). `expect.txt` pins the arm.
