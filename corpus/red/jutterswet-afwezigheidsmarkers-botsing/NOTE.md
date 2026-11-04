# jutterswet-afwezigheidsmarkers-botsing

The document of `corpus/golden/jutterswet-afwezigheidsmarkers` (the same `ir.json`, the frontend's emission of that
golden's `source.rgs`), with the two cases in which a marker is decided against another value. A marker is a value
like any other (`spec/RUNTIME-SEMANTICS.md` ch. 9.5): two writers that decide one target with different values have no
fixpoint, and the session faults with EF-016 (ch. 5.7), per day on a timeline.

- Case 1: a Vondst that is onleesbaar and carries a schatting of 300,00. The rule "taxatie onbekend" writes
  `onbekend` and the rule "taxatie uit schatting" writes `waarde(300.00)` into the taxatie: EF-016, attribute
  `taxatie`, label `V1`.
- Case 2: a Vondst verkocht from 2025-02-01. In February 2025 the rule "opslag onbekend in de winter" writes
  `onbekend` and "opslag niet van toepassing na verkoop" writes `niet van toepassing` into the opslagkosten: EF-016,
  attribute `opslagkosten`, label `V1`.

The pins carry the attribute and the label, which ref, clingo and the Drools backend each report.
