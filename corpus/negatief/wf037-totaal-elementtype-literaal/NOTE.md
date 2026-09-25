# wf037-totaal-elementtype-literaal

Violates WF-037 (`spec/IR-SPEC.md` ch. 4.3) in its fold-source arm: a
`total` without a range is the subject's own read (ch. 4.3, the reading of
the source, (iv)), so its one argument, its fold source, is an
`AttributeReference` of the subject. This `total` carries the element type
`Jutter` — the subject, as WF-083 (c) demands of a total without a range —
no range, and as its one argument a `Literal`, 2.00 kg: a value, and no
read at all.

    WF-037 FAIL /rules/rules/0/versions/0/value: a total aggregation
    carries elementType "Jutter" and a Literal operand: the source of an
    instance fold is an AttributeReference

The document is that of `wf037-som-elementtype-zonder-operand`: a `Jutter`
whose `aangemeld gewicht` (kg, two decimals) is the target, a `Vondst`
with a kg `gewicht`, and the fact type `vondstaanmelding`. The node's type
is the target's.

Before the arm the document was verifier-clean — `verifier: PASS`, exit 0,
and gate 3 reported the case `passed the verifier (must violate WF-037)` —
while no executor ran it. Measured before the arm with a case outside the
corpus (the population of golden `jutterswet-vondsttelling`): `ref` and
the ASP backend refused in their bottom sentences, the Drools exec-model
branch excluded the rule (transpiler SKIP) and the DRL-text branch left a
marker (`TODO aggregatie TOTAAL`). A well-formed document no executor can
run: across the four zones every one of the 30 totals without a range
carries an `AttributeReference`, so the refusal costs no existing
document. Since the arm `ref`, the ASP backend and the Drools loader
refuse it in the verifier's sentence.

The rood-eerst measurement, the verifier checksums and the sweeps with
their mutants are recorded once, in
`corpus/negatief/wf037-aantal-elementtype-literaal/NOTE.md`.

`expect.txt` pins the arm.
