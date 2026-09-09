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

`attribuutGranulariteit` still admits `null` on the wire — the same seam one side
table over, deliberately left alone. Its reader split HAS been measured since
(2026-09-10) and turns out to be a different class from this one: no silent wrong
answer anywhere, but loud refusals that are misworded and that the two executors
phrase differently. Diagnostic hygiene rather than correctness, and so its own
card (`backlog/new/20260909-attribuutgranulariteit-null.md`, low priority) rather
than a third arm here.
