Violates WF-097 (`spec/IR-SPEC.md`): a `gedurende` clip member carries a
whole-period qualifier. The clip already ranges over the whole timeline, so the
qualifier names no reading the clip has.

This is the **day count (ch. 3.8, the WF-058 clip)** — one of the three clips WF-097 fences. The rule is one
shared helper called from three sites in each of the verifier, the reference
interpreter and the ASP backend; this witness is what fails if that clip's call
site is dropped.

A minimal hand-broken variant of
`corpus/golden/anchor/jutterswet-dagentelling/ir.json`: one field on one node —
`resolvedRegels[0].waarde.filter.periode` goes from `null` to an affirmative
`GeheelPeriode`. The characteristic is TIME-DEPENDENT, so WF-043's
non-time-dependent arm cannot fire here — which is exactly why the qualifier
evaporated in silence at this position.

Only the AFFIRMATIVE polarity is pinned per clip. The negated one is not a
separate arm but a branch inside the shared one, and it is pinned twice already
at the temporal total (`wf097-clip-kenmerk-periode-ontkend`); a third and
fourth copy here would fail and pass together with those, testing nothing new.
What these two witnesses DO test that the total's cannot is the per-clip call
site.

Before the rule reached this clip the document was verifier-clean and both
executors replayed every one of the anchor's cases with the untwisted answers.
