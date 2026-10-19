Violates WF-114 (`spec/IR-SPEC.md` ch. 4.1): a `Literal` whose value variant
is not the one its type names — the value `{"date": "2026-03-15"}` under a
`Date` of millisecond precision, which names `dateTime`.

    WF-114 FAIL /rules/rules/0/versions/0/condition/right/value:
    literal value is a date value but its type is a millisecond-precision Date:
    the variant is the one the type names

**Why the date string is not the defect.** `2026-03-15` is a valid day, so
WF-084, which reads the string of a `date` variant, has nothing to refuse, and
WF-095 reads only a `dateTime` variant. What is wrong is the pairing: a
millisecond-precision `Date` is a point inside a day, and its value is written
as a `dateTime`. The precision is the one place where one datatype node names
two variants, and this witness pins that the cross reads it.

**Every executor refuses it at load.** The shared OCaml loader, when an
executor loads the document, refuses the literal with this same sentence; the
verifier's loader loads it so that the rule can cite it.

**The document.** `corpus/golden/jutterswet-meldstip/ir.json` with one value
changed at the right operand of the first rule's condition:
`{"dateTime": "2026-03-15T12:00:00.000"}` → `{"date": "2026-03-15"}`.

Falsifiable, arm-exact: see the sister `wf114-literal-variant-against-type`; the
two witnesses are the two verdicts that move when the WF-114 report is removed.
