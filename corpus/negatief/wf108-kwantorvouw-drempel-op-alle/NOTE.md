Violates WF-108 (e) (`spec/IR-SPEC.md` ch. 4.5): the quantifier fold's `kwantor`
is `ALLE` and it carries a `drempel` of 2. `ALLE` asks every contributor to
satisfy the matrix and reads no threshold, so the number is a quantification the
`kwantor` field contradicts — and a reader is entitled to trust either one.

The other arm of the same rule (`wf108-kwantorvouw-drempel-ontbreekt`) refuses the
missing threshold; together they make the field present EXACTLY where it is read.
Admitting an ignored number here would put this node back where `Samengesteld`
still is — the threshold always written, a zero nobody consults — which is the
"a field nobody reads is a field that can say anything" defect of WF-083 (c),
and this node is new enough not to have to inherit it. It matters beyond
tidiness: a consumer widening its fold to the counting quantifiers, reading the
threshold before the quantor, would answer "at least 2" for a document that
wrote "alle".

The schema also refuses this document — its `if`/`then`/`else` on `kwantor`
forbids `drempel` under ALLE and GEEN — so it is deliberately schema-invalid as
well. The negatief zone is gate 3's, not gate 2's: a rule whose only enforcement
were the schema would have no citable violation.

Verifier: `WF-108 FAIL /rules/rules/0/versions/0/condition/threshold: quantifier fold with
a threshold (2) under a NON-counting quantifier: the threshold is read only by the
counting quantifiers, and a number no reader consults states a quantification the
quantifier contradicts`.
