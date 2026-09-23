# wf093-wratvouw-set-som

Violates WF-093 (`spec/IR-SPEC.md` ch. 4.3) in the **set** spelling of the
row-1 wart — the spelling the WF-083 anchor move newly admitted, and the
sharpest of the four: here one executor ANSWERS while the other two refuse.

    WF-093 FAIL /rules/resolvedRegels/0/waarde/argumenten/0: the v0 instance-fold
    source (the row-1 wart, spelled with the unknown sentinel on the
    argument's own type) is a COUNT source: a fold source whose own type is
    the unknown sentinel yields the linked instances, which only AANTAL
    reads — so it cannot be the source of this SOM aggregation (the set
    spelling reads it no differently: the sentinel decides)

The document. A `SOM` over `elementObjecttype: "Vondst"` whose single
source argument carries `set: true`, `objecttype: "Vondst"` (= the
`elementObjecttype`, so WF-083 (c)'s anchor cross is satisfied for a set
read), `navigatieRol: "aanmelder"` (a role the fold SUBJECT `Jutter` itself
fills, so WF-083 (b)'s self-denotation clause is satisfied too), and the
unknown sentinel on its own `type`. Every other WF-083 clause holds; only
the KIND law refuses it. Built from the carried set-source `SOM` of
`corpus/golden/jutterswet-vondsttelling` by putting the sentinel on the
operand's `type`, adding the subject-denoting `navigatieRol`, and dropping
the `bronRol` the wart spelling has no room for (WF-047's fold-source
detector does not recognise a navigating source, so a binding there would
be a second, unrelated fault). One fault; the verifier reports WF-093 and
no other rule.

Rood-eerst, measured on this document. Pre-rule verifier
(md5 `924f0e3cfb69e9e8d9be7aa916a9bfe7`): `verifier: PASS`. With WF-093
(md5 `957d3eeb5667188e33d5468b80f21b71`): the rejection above.

Executors, on this document (case: one Jutter linked to two Vondsten whose
gewicht is 12 kg and 7 kg):

    ref         : EVALUATION FAULT: value aggregation over a bare role source
    ASP backend : TRANSLATION FAULT: expression kind not translated (no
                  corpus case exercises it)
    Drools      : PLAYED — it fires and answers 19.00

`ref`'s first fold-source arm matches the sentinel BEFORE its set arm and
regardless of `set`, so the set spelling is not a second reading: it lands
on the same instances and the same refusal. The third executor derives its
member set from the fold kind and its own role derivation, reads each
member's `gewicht`, and sums 12 + 7. Under a verifier-clean document that
is one answer against two refusals — refusing the document at the wire is
what keeps the three from being asked. The whole 2026-09-05 measurement
re-verified here on 2026-09-06: sum 19.00, maximum 12.00, minimum 7.00.

Sibling measurement, same spelling, other `attribuut`: with the role plural
(`attribuut: "vondsten"`) in place of the element attribute, `ref` and the
ASP backend report exactly the same two faults and Drools crashes
(`ERROR ConstraintEvaluationException`) instead of answering. Same rule,
same arm, same verdict — the `attribuut` slot does not enter WF-093, only
the sentinel does.

Falsifiable, arm-exact: remove the WF-093 clause from
`aggregation_source_check` (spec/verifier). Swept over golden + rood +
negatief + parity (527 documents) exactly the four `wf093-wratvouw-*` cases
turn clean and nothing else moves. Removing only the `set` half of the
MESSAGE changes this case's report text and no verdict anywhere — the two
spellings run through ONE arm.
