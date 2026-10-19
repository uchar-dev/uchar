Violates WF-097 (`spec/IR-SPEC.md`): a `gedurende` clip member carries a
whole-period qualifier. The clip already ranges over the whole timeline, so the
qualifier names no reading the clip has.

This is the **day count (ch. 3.8, the WF-058 clip)** — one of the three clips WF-097 fences. The rule lives in
ONE shared helper per artifact, reached from two or three call sites depending on
how that artifact discriminates the clips: this witness is what stops being
rejected if the reach to THIS clip is lost — in the verifier that means narrowing
the kind-keyed branch that covers both folds, elsewhere dropping the call
outright.

A minimal hand-broken variant of
`corpus/golden/anchor/jutterswet-dagentelling/ir.json`: one field on one node
— `rules[0].versions[0].value.filter.wholePeriod` goes from absent to an
affirmative `WholePeriod`. The characteristic is TIME-DEPENDENT, so WF-043's
non-time-dependent arm cannot fire here — which is exactly why the qualifier
evaporated in silence at this position. Since L5 the qualifier is a
`WholePeriod {granularity, negated}` (delta B6); the report names it
`wholePeriod`, and `expect.txt` pins the arm.

Only the AFFIRMATIVE polarity is pinned per clip. The negated one is not a
separate arm but a branch inside the shared one, and it is pinned twice already
at the temporal total (`wf097-clip-characteristic-period-negated`); a third and
fourth copy here would fail and pass together with those, testing nothing new.
What these two witnesses DO test that the total's cannot is the per-clip call
site.

Before the rule reached this clip the document was verifier-clean and both
executors replayed every one of the anchor's cases with the untwisted answers.
