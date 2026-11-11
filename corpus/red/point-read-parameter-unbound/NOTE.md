# point-read-parameter-unbound

A point read of a **time-dependent parameter the case does not bind**. The
timeline of a time-dependent parameter is its binding (`RUNTIME-SEMANTICS.md`
ch. 3.4), so a read of it in a case without one reads an unbound parameter,
and binding is fail-fast: EF-014 with the parameter's name (ch. 5.7b).

## The shape

`corpus/golden/anchor/jutterswet-aanmeldquotum`'s document verbatim
(`ir.json` is a byte copy): the parameter `penningbedrag` is declared with a
yearly timeline, and one rule reads it on a day.

```
De penning bij aanmelding van een Jutter moet berekend worden als
    het penningbedrag op zijn aanmelddatum.
```

The case is the anchor's own case with an empty `parameters` object: J1
(aanmelddatum 2025-06-01), J2 (2026-07-01) and J3 (no aanmelddatum).

## Why it faults (by hand)

The rule has no condition. For J1 the date operand is filled, so the read
reaches the parameter's timeline, which the case does not supply: `parameter
"penningbedrag" is unbound in this case (binding is fail-fast, ch. 5.7)`,
EF-014 `{parameter: penningbedrag}`. J2 reaches it the same way. For J3 the
date is leeg, the read is leeg and the parameter is not reached (ch. 3.3); J3
alone would not fault.

## Executors

ref raises EF-014 at the read. The ASP backend carries it as the fail-fast
guard of an unbound parameter, under the reach of the read and a filled
date; the program is unsatisfiable and the diagnosed witness names the
parameter. The Drools backend does not carry a point read (`TemporalRead`)
and refuses the document at load with DROOLS-023, so it stands in gate 8's
Drools exclusion list with that code.

## The green twin

`corpus/golden/anchor/jutterswet-aanmeldquotum` binds `penningbedrag` (4 in
2025, 5 in 2026) and pins penning bij aanmelding 4 for J1, 5 for J2 and leeg
for J3.
