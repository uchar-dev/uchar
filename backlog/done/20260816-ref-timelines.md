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

Result: delivered, with one deliberate deviation from the card text.
ref/lib/timeline.ml carries the ch. 3 algebra — read (order-independent),
calendar and anchored buckets with the month-end clamp, cut-merge
(period-coalescing), temporal summation (contributor-skipping, pre-fold
unit restatement), the temporal total with clipping — and the ch. 3
worked examples land verbatim as unit tests (WV-1.2f, 3.4, 3.5 incl.
the 720 cross-unit fold, 3.6: 186/60/3600/71/115/500, 3.2 anchored
walls). The penningjaar golden gained three rules (re-emitted
byte-canonically at the pin): the temporal total — the one v0-mirrored
timeline-to-scalar bridge, rsTijdTotaal at the authority pin, confirmed
marker-free in the generated DRL — the drempel comparison over that
scalar, and the negated aanmeldplichtig reading, which fills the WF-046
golden-witness cell. Its case observes the collapse values, the
absent-timeline-totals-to-leeg rule, and the non-transient negation
outcomes; gate 5 is fully green (7 cases, no not-replayed list).
Deviation, forced by verification: the card's vintage-selection case is
NOT deliverable without a silent mirror divergence — at the pin both
backends refuse a temporal attribute or granular parameter in a scalar
condition (§8.2 marker family), and the point-read is v1-OPEN (row 11,
maintainer's decision). The first golden shape (drempel over the
temporal attribute + year parameter, oracle-pinned green) was exactly
that divergence; two verification lenses caught it with pin-level
evidence and the golden was rebuilt through the collapse. ref/ now
faults on every unmirrored timeline/scalar crossing (temporal
attribute/characteristic/granular parameter in scalar slots, temporal
write targets). Vintage reading and parameter-set data move to ch. 8;
multi-segment timeline case data needs a case-schema extension (later
card). Verification: 3 lenses (1 blocker: the divergence; 1 major:
ungated temporal writes; minors: clip fragmentation undercount,
ch. 3.10 mis-citation) + 1 recheck lens (confirmed the rebuild on the
pin; 1 doc restant) — all addressed.
