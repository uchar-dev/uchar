# ref/: timeline algebra (RUNTIME-SEMANTICS ch. 3) + timeline cases

Goal: implement ch. 3 in `ref/` — timeline values with `read(tl, d)`, the
cut-merge of §5.1.4 (period-coalescing) and the temporal aggregation of
§5.8.2 (contributor-skipping), anchored year buckets, and the evaluation-
date gate of ch. 3.10 — far enough that timeline-bearing corpus cases
replay under gate 5. The `jutterswet-penningjaar` golden currently holds
only declarations (timeline, year-granular attribute, two year-granular
parameters) and no rule: extend it with at least one timeline rule (e.g.
the drempel comparison per penningjaar) by re-emitting `bron.rgs` at the
pinned emitter commit, or add a sibling timeline golden; then author
`case*.json` files with `asOfDate` set, including a vintage-selection
case for the year-granular parameters (fail-fast on a gap, ch. 5.7/8).
Add the WV-1.2f examples and the ch. 3 worked examples (temporal total
186,00/60,00/…; pro-rata 21600/73) as `ref/` unit tests.

Acceptance: `sh scripts/check-oracle.sh` green with the penningjaar
golden replayed (no longer in the not-replayed list); `dune build
@ref/runtest` green with the ch. 3 examples; gates 1–4 stay green.

May touch: `ref/**`, `corpus/golden/jutterswet-penningjaar/**`,
`corpus/golden/README.md`, `backlog/**`; a new timeline golden directory
if chosen; `spec/IR-SPEC.md` (only the WF-046 golden-witness cell, which
this card's negation golden fills).

Out of scope: ch. 6 stratification; ch. 8 beyond what year-granular
parameter binding needs; dagsoorten (ch. 3.8) unless a case demands it.
