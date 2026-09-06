# wf093-wratvouw-kale-som

Violates WF-093 (`spec/IR-SPEC.md` ch. 4.3): the row-1 wart is a COUNT
source, and this document makes it the source of a `SOM`.

    WF-093 FAIL /resolvedRegels/0/waarde/argumenten/0: the v0 instance-fold
    source (the row-1 wart, spelled with the unknown sentinel on the
    argument's own type) is a COUNT source: a fold source whose own type is
    the unknown sentinel yields the linked instances, which only AANTAL
    reads — so it cannot be the source of this SOM aggregation

The document. The **bare** wart spelling: a single-argument instance fold
over `elementObjecttype: "Vondst"` whose source is an `AttribuutRef` with
`attribuut: "vondsten"` (the declared plural of the role `vondst`),
`objecttype: "Jutter"` (the fold subject, so WF-083 (c)'s anchor cross is
satisfied), `set: false`, no `navigatieRol`, and the unknown sentinel on its
own `type`. Everything about it is the carried counting form of
`corpus/golden/jutterswet-vondsttelling` — the mutation is the fold's
`kind`, `AANTAL` to `SOM`, and the target attribute renamed with it. One
fault, nothing else: the verifier reports WF-093 and no other rule.

Rood-eerst, measured on this document. Under the branch-head verifier
before the rule (`spec/verifier/_build/default/bin/verifier.exe`,
md5 `924f0e3cfb69e9e8d9be7aa916a9bfe7`) it is `verifier: PASS`; under the
verifier that carries WF-093 (md5 `957d3eeb5667188e33d5468b80f21b71`) it is
the rejection above.

Executors, on this document (case: one Jutter linked to two Vondsten of
12 kg and 7 kg):

    ref         : EVALUATION FAULT: value aggregation over a bare role source
    ASP backend : TRANSLATION FAULT: expression kind not translated (no
                  corpus case exercises it)
    Drools      : ERROR ConstraintEvaluationException (a crash at fire)

`ref` is the norm and it names the defect exactly: a value aggregation
cannot read a bare role source. That fault is now unreachable on a
well-formed document — it stays as the second line of defence, never dead
code, and no corpus case pins it (measured).

Falsifiable, arm-exact: in `aggregation_source_check` (spec/verifier),
remove the WF-093 clause. Swept over golden + rood + negatief + parity
(527 documents) exactly the four `wf093-wratvouw-*` cases turn clean and
nothing else moves — not one verdict, not one report sentence.
