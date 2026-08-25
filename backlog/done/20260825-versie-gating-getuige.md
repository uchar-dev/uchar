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

Result: de afspelende versie-gating-getuige staat. Rood-eerst: vóór deze
kaart was het enige corpusdocument met geversioneerde regelnamen (`#N`)
het getuigenkabinet `jutterswet-vormenkabinet` — nul `case*.json`, en
zijn eigen NOTE legt vast dat het niet kán afspelen; de enige andere
begrensde vensters (`jutterswet-vensterheffing`) zitten op twee
*verschillende* regelnamen. RS hfst. 8.2-versieselectie op één regelnaam
werd dus door nul afspelende cases geraakt. Geland:
`corpus/golden/anchor/jutterswet-jutheffing` — twee gedateerde versies
van één regelnaam (`jutheffing naar jaartarief #1`, venster
2025-01-01..2025-12-31, aangiftewaarde+30; `#2`, 2026-01-01..2026-12-31,
aangiftewaarde+70) op hetzelfde doelattribuut, plus een ongevensterde
controleregel (registratiegetal = aangiftewaarde+1). Drie cases met
identieke input (200), alleen de datum verschilt: 2025→230, 2026→270,
en de buiten-elk-venster-flank 2027-02-01. Die flank is eerst
vastgesteld en toen exact gepind: ref fault NIET maar levert leeg
("MISMATCH: A1.jutheffing = leeg" op een opzettelijke waardeprobe),
clingo identiek ("underived (leeg)") — dus een golden-case met
`attributeEmpty`, geen rood-case; de controleregel vuurt er wél (201),
wat elke case een gate-7 noop-refuter maakt: `scripts/check-noop.sh`
onaangeraakt (33 survivors ongewijzigd). Alle negen gates groen
(ir-schema 128 round-trip; verifier 128 golden/144 negatief; case-schema
331; oracle en clingo elk 311 exact; noop 278 refuters; rood 20; catala
169 checks/11 byte-identiek). Bewust gelaten: de tabelrij in
`corpus/golden/anchor/README.md` (buiten May-touch; de NOTE.md in de
map draagt de beschrijving zolang), en de WF-021-rij in de WF-index van
`spec/IR-SPEC.md` die als golden-getuige nog het vormenkabinet noemt —
protected pad; de maintainer kan daar nu de afspelende getuige
`anchor/jutterswet-jutheffing` bijzetten.
