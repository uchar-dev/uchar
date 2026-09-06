# wf093-wratvouw-set-maximum

Violates WF-093 (`spec/IR-SPEC.md` ch. 4.3) in the **set** spelling of the
row-1 wart under a RANKING kind, where the arithmetic sibling
(`wf093-wratvouw-set-som`) carries the same spelling under a computing one.

    WF-093 FAIL /resolvedRegels/0/waarde/argumenten/0: the v0 instance-fold
    source (the row-1 wart, spelled with the unknown sentinel on the
    argument's own type) is a COUNT source: it names a role and yields the
    linked instances, never a value per contributor — so it cannot be the
    source of this MAXIMUM aggregation (the set spelling reads it no
    differently: the sentinel decides)

The document. A `MAXIMUM` over `elementObjecttype: "Vondst"` whose single
source argument carries `set: true`, `objecttype: "Vondst"` (= the
`elementObjecttype`: WF-083 (c) satisfied), `navigatieRol: "aanmelder"` (a
role the fold subject `Jutter` fills: WF-083 (b) satisfied), and the
unknown sentinel on its own `type`. No `leegwaarde` — that field is
sum-only. Built from the carried set-source fold of
`corpus/golden/jutterswet-vondsttelling` exactly as the `SOM` sibling is.
One fault; the verifier reports WF-093 and no other rule.

Rood-eerst, measured on this document. Pre-rule verifier
(md5 `924f0e3cfb69e9e8d9be7aa916a9bfe7`): `verifier: PASS`. With WF-093
(md5 `945642482a4183057a3c5194a11749ce`): the rejection above.

Executors, on this document (case: one Jutter linked to two Vondsten whose
gewicht is 12 kg and 7 kg):

    ref         : EVALUATION FAULT: value aggregation over a bare role source
    ASP backend : TRANSLATION FAULT: expression kind not translated
    Drools      : PLAYED — it fires and answers 12.00

The ranking kinds behave no differently from the computing ones here, which
is the point of the rule being keyed on the sentinel and not on the kind
family: the same source, the same two refusals, the same third answer. The
minimum leg of the same shape answers 7.00 (measured 2026-09-06 on the
identical document with `kind: "MINIMUM"`), completing the 19/12/7 triple
this rule makes unreachable.

Falsifiable, arm-exact: remove the WF-093 clause from
`aggregation_source_check` (spec/verifier). Swept over golden + rood +
negatief + parity (527 documents) exactly the four `wf093-wratvouw-*` cases
turn clean and nothing else moves.
