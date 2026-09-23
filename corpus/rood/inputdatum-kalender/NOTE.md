# inputdatum-kalender

A case INPUT date that the case schema admits and the calendar does not:
`P4.meldingsdatum` is `2026-02-30`. The `date` pattern of `uchar-case-v0`
checks digits and field widths (`^[0-9]{4}-[0-9]{2}-[0-9]{2}$`), not the
calendar, so gate 4 lets the file through; calendar validity beyond the
lexical pattern is the case readers' own check (`spec/schema/README.md`,
division of labour). All three executors refuse the value at load, each
naming the field and the value:

    ref, clingo  P4.meldingsdatum "2026-02-30" is not the uchar-case-v0 date lexeme yyyy-mm-dd (day out of range in date lexeme "2026-02-30")
    Drools       malformed date '2026-02-30' on 'meldingsdatum': the field widths are right but the calendar is not (Text '2026-02-30' could not be parsed: Invalid date 'FEBRUARY 30')

The document is the `jutterswet-bergingsklok` golden verbatim. The case is
that golden's `case2.json` with this one value changed: `P5` still carries
a well-formed date and a number, so the refusal names the one bad field and
not the instance or the input as a whole. ref and the ASP backend share one
case loader, so their fragment is one sentence by construction; for load
faults this zone is a regression fence for that loader, not a differential.

## Why this file could not be red before 2026-09-23

Until the card `ref-caselabel-datumfout` the date reader's refusal left the
shared case loader as an EVALUATION fault. Both runners read an
`expectedFault` pin at load only for case errors, so the pin in this file
was never consulted and the file failed gate 8 whatever it said. Measured
on the loader of that time:

    ref     EVALUATION FAULT: day out of range in date lexeme "2026-02-30"     exit 1
    clingo  TRANSLATION FAULT: day out of range in date lexeme "2026-02-30"    exit 1

No field, no load phase, and no way to pin it. Since the card the refusal
is a case error at load (`oracle: expected fault observed at load (…)`,
exit 0 on the red path), with the field and the value in it. Every other
input position (list element, struct field, constructor payload, timeline
boundary and segment value, characteristic period, parameter, expected
value) refuses through the same template, pinned position by position in
the reference interpreter's own suite.

## The Drools pin

The Drools red-path runner reports a runtime exception raised while it loads
the case inputs as a FAULT, and its case reader refuses the value by name:
`FAULT IllegalArgumentException: malformed date '2026-02-30' on
'meldingsdatum': …`. The pinned `drools` fragment is the backend's own
sentence up to the parenthesis; the parenthesised tail is the Java
platform's parse message, whose wording belongs to the JDK rather than to
the backend, so it is left out of the pin.
