Violates WF-059 (`spec/IR-SPEC.md`), third arm: an `attribuutGranulariteit`
entry declares its attribute time-dependent by the presence of its KEY but
names no grid in its VALUE.

    WF-059 FAIL /objecttypen/Jutter/attribuutGranulariteit/wachtvergoeding per maand: attribute
    declared time-dependent carries no granularity: the key makes the temporal readers treat it as
    a timeline while the case loader reads the value and refuses that timeline

A minimal hand-broken variant of `corpus/golden/jutterswet-tegenwacht/ir.json`:
one field on one node — `objecttypen.Jutter.attribuutGranulariteit."wachtvergoeding
per maand"` goes from `"MAAND"` to `null`.

**Why this base.** The card measured three positions; this is the one where no
other rule reaches. On a pro-rata base (`anchor/jutterswet-borgdeel`,
`borgtarief`) the same nulling is caught already, but as WF-056 — "pro-rata base
'borgtarief' is not a temporal attribute" — which names a different defect than
the one present. `jutterswet-tegenwacht` uses its temporal attribute through an
ordinary `gedurende` clip, where WF-056 does not reach, so this document
isolates the arm: WF-059 is the only rule that reports it.

**Why it is a defect.** Four readers decide "is this attribute temporal", and
they do not decide it on the same thing: the verifier's `attr_temporal`, the
reference interpreter's evaluator and the ASP translator read KEY EXISTENCE,
while the shared case loader reads the VALUE. A null value therefore declares
the attribute temporal to three readers and non-temporal to the fourth.

**Measured on this exact document before the rule existed** (pre-arm verifier
md5 `f55fdba98ccd5ac68bb99492519aa580`): it was schema-valid AND verifier-PASS,
and then

  - a case DELIVERING the attribute's timeline
    (`corpus/golden/jutterswet-tegenwacht/case1-overlap.json`) died at load in
    both executors with "a timeline value on a non-temporal attribute" — a
    message that contradicts the document that produced it;
  - the same document with the attribute delivered SCALAR — the shape the loader
    does accept — died in both executors as well, but with two different
    diagnoses of one defect: `ref` "temporal attribute "wachtvergoeding per
    maand" without a granularity", the ASP backend "the gedurende clip of
    "wachtvergoeding per maand" needs the full timeline".

This is what separates the arm from its sister one side table over
(`wf059-kenmerk-zonder-granulariteit`): there, a case OMITTING the timeline ran
to completion and answered 60 and 120 where the golden pins 30 and 30 —
identically wrong in both executors, invisible to any differential gate. Here
nothing is silently wrong; every path dies. What is wrong is the diagnosis.

Closed twice on purpose. `spec/schema/regelspraak-ir-v0.schema.json` no longer
admits `null` in this enum, so the wire refuses the shape, and this rule refuses
it in the verifier for any document that never met the schema. The negatief zone
is not schema-validated (gate 2 covers `corpus/golden` and `corpus/rood`), which
is what lets this document exist here to exercise the verifier arm.

The case loader's message moved in the same change: with the key present and the
value null it now says "the attribute is declared time-dependent but its
granularity is null: the timeline has no grid to sit on (WF-059)". Both
executors share that loader, so they say it identically. The old wording stays
for the case it is true of — an attribute with no granularity key at all,
delivered as a timeline. The scalar-delivery divergence above is deliberately
left standing: both messages are true of the document and both are loud.
