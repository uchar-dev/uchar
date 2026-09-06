# wf093-wratvouw-kale-eerste

Violates WF-093 (`spec/IR-SPEC.md` ch. 4.3): the row-1 wart is a COUNT
source, and this document makes it the source of an `EERSTE` — the
POSITIONAL half of the value folds, where the arithmetic sibling
(`wf093-wratvouw-kale-som`) is the other.

    WF-093 FAIL /resolvedRegels/0/waarde/argumenten/0: the v0 instance-fold
    source (the row-1 wart, spelled with the unknown sentinel on the
    argument's own type) is a COUNT source: it names a role and yields the
    linked instances, never a value per contributor — so it cannot be the
    source of this EERSTE aggregation

The document. The **bare** wart spelling, identical to the SOM sibling
except for the fold kind: one argument, `attribuut: "vondsten"` (the
declared plural of the role `vondst`), `objecttype: "Jutter"` (the fold
subject), `set: false`, no `navigatieRol`, the unknown sentinel on its own
`type`, `elementObjecttype: "Vondst"`. Derived from the carried counting
form of `corpus/golden/jutterswet-vondsttelling` by changing `kind` alone.
One fault; the verifier reports WF-093 and no other rule.

Rood-eerst, measured on this document. Pre-rule verifier
(md5 `924f0e3cfb69e9e8d9be7aa916a9bfe7`): `verifier: PASS`. With WF-093
(md5 `945642482a4183057a3c5194a11749ce`): the rejection above.

Executors, on this document (case: one Jutter linked to two Vondsten of
12 kg and 7 kg):

    ref         : EVALUATION FAULT: value aggregation over a bare role source
    ASP backend : TRANSLATION FAULT: expression kind not translated
    Drools      : EXCLUDE, transpiler SKIP — the rule's value is out of the
                  backend's scope, so the form is refused per form and never
                  fires

The positional kinds are the one place where the third executor does not
even reach a fault: it refuses the shape at lowering. All three roads are
therefore dead ends, which is the whole argument for refusing the document
at the wire instead of at three different places.

Falsifiable, arm-exact: remove the WF-093 clause from
`aggregation_source_check` (spec/verifier). Swept over golden + rood +
negatief + parity (527 documents) exactly the four `wf093-wratvouw-*` cases
turn clean and nothing else moves.
