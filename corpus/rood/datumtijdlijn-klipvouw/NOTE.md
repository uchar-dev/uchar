# datumtijdlijn-klipvouw

The gedurende-clipped sibling of `datumtijdlijn-totaalvouw`: the same
defect in the other temporal-total arm. The document is
`golden/jutterswet-erfwacht` — `het totaal van zijn wachtvergoeding per
maand gedurende de tijd dat hij wachtplichtig is` — with the clipped
attribute and the target retyped to `Datum in dagen`. The verifier
passes it.

The case supplies a full value timeline (2023-07-01 to 2023-09-01, value
2024-01-15) and a characteristic period of exactly one month
(2023-07-01 to 2023-08-01), so the clipped span is one whole grid unit.
Measured 2026-09-23: the ASP backend answered **19737**, the day number
times one; a two-month clip would have answered twice that. `ref` faults
on the multiplication: `maal: expected a number, got 2024-01-15`.

The clip arm now asks the same declaration question as the own-subject
arm, before the per-instance full-timeline check and before anything
grounds, and names itself as the gedurende-clipped total.

Drools: the exec branch's case loader refuses the date-valued segment
before fire; the DRL-text branch, which failed the whole file on compile
(`Unable to Analyse Expression if (rsTijdTotaal(rsTijdKnip(…`), markers
the form through the shared timeline reason. Listed in gate 8's
exclusion list with that reason.
