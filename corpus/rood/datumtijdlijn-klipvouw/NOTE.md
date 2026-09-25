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

**Where this arm parts from its own-subject sibling, and why it is not
harmless here.** The refusal is about the DOCUMENT, so a case with NO
value timeline, or with a value the clip removes entirely, is declined
too. In the own-subject arm `ref` answers leeg there, so the refusal is
the harmless direction. In THIS arm it is not: measured 2026-09-23 on
this document, `ref` answers `K1.wachtsom = 0` both without a value
timeline and with the value clipped away — the empty clipped sum, a
NUMBER in a date slot that no chapter defines. Before this card the ASP
backend answered the same empty sum (read back as 1970-01-01), and the
Drools exec branch CRASHES on the case without a value
(`ClassCastException: Cannot cast java.math.BigDecimal to
java.time.LocalDate`) — the DRL-text branch declines it by name. The
arbiter's own 0 is out of this card's scope (`ref` is not touched); it
is carded as `20260923-ref-klipvouw-nul`, together with a
well-formedness rule that the operand of `total`/`sum`/pro rata is numeric.
