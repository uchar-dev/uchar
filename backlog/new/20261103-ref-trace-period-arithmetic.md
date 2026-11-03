# ref-trace-period-arithmetic: de lezing van ref leest rekenen per periode, en Drools kopieert het gebrek niet meer

*Opgesteld door de hoofdsessie op 2026-11-03 uit de architectuurreview van Fable (`werk/review-fable-2026-11-03/advies.md`, §3 "Drools" en
"Per-dag-lifting", advies 3 en 4) en open punt 1 van stap B van `backlog/done/20261103-timeline-arithmetic-by-rule.md`.*

## Aanleiding

1. De trace van ref (`ref/lib/reading.ml`) leest de knopen van een waarde van rekenen per periode zonder voorwaarde als `unreached`: hij
   rekent via `run_pro_rata` en ziet geen knoop, terwijl RUNTIME-SEMANTICS ch. 5.12 hun waarden vraagt. Drools bootst dat na
   (`TmsTranspiler.unreached`, de boekhouding in `writeRule`) om gate 17 groen te houden. Een backend die een gebrek van de referentie
   nabouwt, is het omgekeerde van wat gate 17 moet bewijzen.
2. `run_rule` in `ref/lib/eval.ml` houdt een eigen pad voor een venster dat de hele voorwaarde is (`clip`, met `window_of` dat `(0, 0)`
   geeft bij een lege grens) naast de `Time_window`-arm van `cond_periods`, die `[]` geeft. Twee implementaties van één regel.

## Goal

Ref leest de knopen van een waarde van rekenen per periode volgens ch. 5.12 (de knopen hun tijdlijnen, de wortel de geschreven tijdlijn,
zoals voor een regel met een voorwaarde per periode al gebeurt); Drools leest ze ook zo, en `TmsTranspiler.unreached` met zijn pins
verdwijnt. Het `clip`-pad van `run_rule` verdwijnt ten gunste van `cond_periods`, zonder dat een uitkomst verandert.

## Acceptance

- (a) Rood eerst: een readings-getuige op een golden van rekenen per periode zonder voorwaarde (`jutterswet-maandloon`) die op main
  `unreached` geeft (letterlijk).
- (b) Gate 17 vergelijkt de nieuwe lezingen op beide branches zonder afwijking; floors die bewust stijgen met oud en nieuw.
- (c) Het `clip`-pad weg; gates 5, 6, 12 en 17 onveranderd op elke golden (geen semantiek beweegt); de ref-tests die het pad pinnen
  blijven groen of worden met reden herschreven.
- (d) `make gates` groen.

May touch: `ref/lib/reading.ml`, `ref/lib/eval.ml`, `ref/test/`, `backends/drools/**`, `scripts/check-readings.sh` (voorstel), kaartverplaatsing.

## Omvang

Klein tot middel.
