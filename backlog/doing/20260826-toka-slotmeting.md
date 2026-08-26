# TOKA slotmeting — the ref-100 goal witness

Goal: the maintainer's goal is ref at 100% on the stress measurement.
Yesterday's re-measurement (`backlog/done/20260825-toka-hermeting.md`)
scored ref 19/30 with eleven refusals: navigation 3, date/duration
family 7, band condition 1. The ref-100 wave has since landed (uchar
pin ≥ 83d48da, toolchain ≥ b4ba108): the day-precision calendar kernel,
date-part extraction + Easter, the datetime ms layer, both fold forms,
the narrowed self-reference, the type-pair fallback, the near-side
rolcheck, and the WF-084 ISO date-literal pin with its emitter fix.
Repeat the measurement with the SAME method (described in
`backlog/done/20260825-toka-stresstest.md`; artefacts from both prior
runs under /tmp/toka and /tmp/toka-hermeting): the same 30
Drools-verified scenarios, same case derivation, verifier on every
dump, ref and clingo per scenario. NB the emitter moved (WF-081/084):
regenerate the dumps with the CURRENT toolchain. Report per executor:
PASS count, refusal classes with counts, delta per class against
yesterday, and any NEW silent divergence (routes to triage, never to
acceptance). The goal condition: ref 30/30 exact agreement with the
Drools-anchored expectations — report honestly if any scenario falls
short, with its class.

RIGHTS RULE, absolute: no TOKA material enters uchar — all scenario
work in the toolchain checkout and /tmp; the card Result carries only
aggregated counts and class names.

Acceptance:
- The card lands in done/ with a Result carrying the final score
  (ref x/30, clingo y/30), the per-class delta table, and either "zero
  silent divergences" or the explicit triage list.
- Nothing outside `backlog/` touched in uchar; no toolchain commits.

May touch: `backlog/` only (uchar); toolchain and /tmp read-only work
space.
Out of scope: fixes (findings become card candidates), corpus
additions, toolchain commits, spec edits.
