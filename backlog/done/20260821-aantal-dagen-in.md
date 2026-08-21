# AANTAL_DAGEN_IN: de zusterfold van tijdsevenredig-deel

Besluit maintainer 2026-08-21: gekozen in het werkmenu (veegbundel).

Goal: de AANTAL_DAGEN_IN-fold landt in beide executors op de
machinerie van tijdsevenredig-deel (granulariteitsfacet, condietijd-
lijn, bucketmuren), met een WF naar het model van WF-056, een anker
met absolute waarden en negatiefs.

Acceptance: anker door gates 5–6; negatiefs onder de nieuwe WF;
alle acht gates groen; twee lenzen.

May touch: `ref/lib/`, `backends/clingo/`, `spec/` (RS-hoofdstuk,
IR-SPEC WF), `spec/verifier/`, `corpus/`, `backlog/`, en (verruimd in
de verificatieronde) `scripts/check-noop.sh` — de reden-dragende
survivorlijst is gate-7's eigen ontwerp en een protected-path-onderdeel
van deze kaart.

Out of scope: overige dagsoort-machinerie.

Result: AANTAL_DAGEN_IN executes in both executors. ref:
`Timeline.aantal_dagen_in` (pro-rata's numerator, unitless whole-day
counts); clingo: the calendar and four-case-intersection machinery
factored into shared helpers (`emit_bucket_calendar`/`emit_true_days`)
with a day-count translation beside the pro-rata one (`adg%d`
counters, count fence, static decimals wrap-fence). WF-058 mirrors
WF-056's facets (gedurende over an own temporal characteristic, no
arguments, bucket == target granularity) with three negatiefs; the
anchor `jutterswet-dagentelling` pins the RS ch. 3.8 worked example
VERBATIM (jan 23, feb 13; year 36) plus the year-boundary split, the
empty case and the ch. 7.8 standing corner (two reason-carrying
gate-7 survivors: empty-stays-empty and input-stands). Red-first: the
kind faulted loudly in both executors. The breaker lens held on all
seven surfaces including the leap year, adjacent-period summing, the
far-calendar span, pro-rata co-existence (no counter collision) and a
v0-comparison against rsAantalDagenIn (same bucketing/summing
reading). Lens fixes: the ch. 7.8 parenthetical now names both
in-practice producers; the may-touch records the gate-7 survivor pin
as the protected-path member of this card. Deliberately left: the
anchored (variable-startpunt) form and richer gedurende shapes stay
loud faults, as at ch. 3.7; the samengesteld/mixed condition the v0
transpiler supports is a recorded narrower scope. All eight gates
green (117 cases, 85 negatiefs, 14 survivors).
