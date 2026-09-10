Violates WF-059 (`spec/IR-SPEC.md`), second arm: a `kenmerkGranulariteit` entry
declares its characteristic time-dependent by the presence of its KEY but names
no grid in its VALUE.

A minimal hand-broken variant of `corpus/golden/jutterswet-tegenwacht/ir.json`:
one field on one node — `objecttypen.Jutter.kenmerkGranulariteit.aangesteld` goes
from `"MAAND"` to `null`.

Why it is a defect and not a shrug. Four readers decide "is this characteristic
temporal", and they do not decide it on the same thing: the verifier's clip
check, the reference interpreter and the ASP translator all read KEY EXISTENCE,
while the shared case loader reads the VALUE. A null value therefore declares
the characteristic temporal to three readers and non-temporal to the fourth.

Measured on this exact document before the rule existed — it was schema-valid
AND verifier-clean, and then:

  - a case DELIVERING `aangesteld`'s timeline died at load in both executors
    with "a characteristic timeline on a non-temporal characteristic", a message
    that contradicts the document that produced it;
  - a case OMITTING that timeline ran to completion and answered
    `tegenwachtsom = 60` and `stilwachtsom = 120` where the golden pins 30 and
    30 — silently wrong, and wrong IDENTICALLY in both executors, so no
    differential gate could have caught it. (`stilwachtsom`'s clip is negated:
    with no periods to complement, the clip degenerates to the whole span.)

Closed twice on purpose: `spec/schema/regelspraak-ir-v0.schema.json` no longer
admits `null` in the enum, so the wire refuses the shape, and this rule refuses
it in the verifier for any document that never met the schema. The negatief zone
is not schema-validated (gate 2 covers `corpus/golden` and `corpus/rood`), which
is what lets this document exist here to exercise the verifier arm.

`attribuutGranulariteit` — the same seam one side table over — was closed on
2026-09-10 as WF-059's THIRD arm, on its own measurement rather than on this
one's, because the fault it produces is a different class: no silent wrong
answer anywhere, but loud refusals that are misworded and that the two executors
phrase differently. Its witness is
`corpus/negatief/wf059-attribuut-zonder-granulariteit`, on this same golden.

The two arms match on the WF closure — schema enum and verifier — and NOT yet on
the message. That third arm also repaired its case-loader wording; this one's
was left as it is. Measured on THIS document with a case delivering
`aangesteld`'s timeline: both executors still say "a characteristic timeline on
a non-temporal characteristic", because the `characteristicTimelines` arm folds
"key present, value null" and "no key at all" into one catch-all. The wire no
longer carries the shape, so only a hand-built document meets that wording — the
same position the attribute side was in before its arm, and reported rather than
fixed because `kenmerkGranulariteit` was out of that card's scope.
