# C3 — the replaying version-gating witness

Goal: LRML-audit candidate C3 (`spec/proposals/LEGALRULEML-AUDIT.md` §7):
WF-021 version windows are verifier-witnessed only — the witness cabinet
`jutterswet-vormenkabinet` cannot replay by its own NOTE, so the RS
ch. 8.2 *runtime* gating (the evaluation date selects the applicable
dated version) is exercised by zero replaying cases. REQUIRES the
clingo-vensters card merged (clingo must carry window gating first).
Build a hand-authored anchor golden — two dated versions of one rule
name with non-overlapping windows and *different* computed outcomes —
plus an `asOfDate` case per window pinning absolute expected values
(and, if ref supports it, a case outside every window pinning the
no-version outcome per RS ch. 8.2 — investigate what ref does there and
pin exactly that; if ref faults, that flank belongs in `corpus/rood/`
with `expectedFault` for both executors, or is honestly left and noted).
Both executors must gate identically (gates 5–6).

Acceptance:
- Red-first, shown in the Result: demonstrate the pre-existing blind
  spot (no replaying window case: quote the vormenkabinet NOTE and show
  the corpus grep), then the new anchor golden replaying green through
  ref AND clingo with per-window distinct pinned values.
- Gates 1–9 all green (incl. gate 7: the cases must be noop-refuters —
  a version-gated computation is; justify any survivor).

May touch: `corpus/golden/anchor/jutterswet-*` (one new anchor golden),
`corpus/rood/` (only for the outside-every-window flank if it faults),
`scripts/check-noop.sh` (survivor list only, with reasons).
Out of scope: `ref/`, `backends/clingo/` (machinery landed by the
clingo-vensters card — if it proves insufficient, report and stop rather
than patch it here), spec documents, `spec/CONSTRUCTS.md` (do NOT
touch), the second time axis (PLAN §9 decision 2 — later v2).
