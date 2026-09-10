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

The two arms match on the WF closure — schema enum and verifier — and, since
2026-09-11, on the message as well. The third arm repaired its case-loader
wording; this one's followed one card later, because the
`characteristicTimelines` arm folded "key present, value null" and "no key at
all" into one catch-all and so refused this document for being non-temporal
while it declares the characteristic temporal. Measured on THIS document with a
case delivering `aangesteld`'s timeline, identically in both executors:

  CASE ERROR: J1.aangesteld: the characteristic is declared time-dependent but
  its granularity is null: the timeline has no grid to sit on (WF-059)

The old wording stays for the case it is true of — a characteristic with no
granularity key at all, delivered as a timeline. The two sentences answer two
different documents:

  (1) the NULL-VALUE document, this witness. Both the schema enum and the
      verifier refuse it, so an IR carrying it is hand-built by definition, and
      it is the new sentence it draws.
  (2) the KEY-ABSENT document, which is no IR defect at all: an IR with an empty
      `kenmerkGranulariteit` is schema-valid AND verifier-PASS as long as no
      `gedurende` reads the characteristic (measured 2026-09-11 on exactly the
      document the third pin uses). There the CASE is what oversteps, by
      delivering a timeline the declaration never promised.

Neither SENTENCE can be pinned in a corpus zone, for the one reason they share:
the refusal falls at LOAD time and exits 2, so the case that draws it fits in no
case-replaying zone — `corpus/golden` replays cases for their expected values,
and `corpus/rood` reaches its expected-fault branch only AFTER the case has
loaded, which is why gate 8 reads a load-time exit as a failure rather than as
the pinned fault. Gate 2 adds a second, independent bar, but for (1) alone: it
schema-gates golden and rood, so THAT IR cannot sit there either. Document (2)
is welcome in any zone; only its case is not. Both sentences are therefore
pinned in the ASP backend's own suite, the only place that can hold them.
