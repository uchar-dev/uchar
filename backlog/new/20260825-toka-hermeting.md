# TOKA re-measurement — the stress score after the decision wave

Goal: yesterday's stress test (`backlog/done/20260825-toka-stresstest.md`,
method described there) scored the hourglass on 30 Drools-verified
real-world scenarios: ref ≡ Drools 18/30, clingo ≡ Drools 4/30, zero
silent divergences, all other outcomes loud refusals. Since then the
decision wave landed: WF-081 + the emitter unit-literal fix (the one
genuine outcome divergence), and clingo translations for non-numeric
scalars, validity windows, the counting composites (decision-table
disjunction) and the full allocation family. Repeat the measurement with
the SAME method on the new state (uchar pin ≥ 0240766, toolchain ≥
68cf8ce): the same 30 scenarios, the same case derivation from the
scenario expectations, the verifier on every dump, ref and clingo per
scenario. Report per executor: PASS count, refusal classes with counts,
the delta against yesterday per class, and — most important — any NEW
silent divergence (a differing value without a fault), which routes to
adversarial triage, never to acceptance.

RIGHTS RULE, absolute: no TOKA material enters uchar — all scenario work
happens in the toolchain checkout and /tmp; this card's Result carries
only aggregated counts and class names.

Acceptance:
- The card lands in done/ with a Result carrying the new score
  (ref x/30, clingo y/30), the per-class delta table against
  yesterday's counts, and either "zero silent divergences" or the
  explicit triage list.
- Nothing outside `backlog/` touched in uchar; no toolchain commits.

May touch: `backlog/` only (uchar). The toolchain and /tmp are working
space, read-only for the repo.
Out of scope: fixes (findings become card candidates), corpus additions,
toolchain commits, spec edits.
