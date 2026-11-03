# total-over-partial-unit: het totaal telt een gedeeltelijke eenheid naar rato (impliciete tijd, punt 12)

*Opgesteld door de hoofdsessie op 2026-11-03 uit het Result van `backlog/done/20261010-impliciete-tijd.md` en het voorstel
`spec/proposals/IMPLICIT-TIME.md`, punt 12. Na `20261103-timeline-arithmetic-per-period` (de kalender van punt 11). Besluiten van
de eigenaar 2026-11-03: **naar rato** (12a): het totaal van ch. 3.6 telt een gedeeltelijke eenheid naar rato van haar werkelijke
lengte (dagen gedeeld door de dagen van de omsluitende kalendereenheid; ALEF `timesDuration`, `partialLength`), een gevolgde
afwijking van de vloer per fragment van de gespiegelde runtime; **de gewone operand houden** (12b), gelezen als "per eenheid van
het raster", een gevolgde afwijking van RegelSpraak §7.1. Besloten (spec spreekt, 12c): een afwezige tijdlijn telt 0.*

Goal: ch. 3.6 en alle uitvoerders tellen een gedeeltelijke eenheid naar rato, lezen een gewone operand per rastereenheid en
tellen een afwezige tijdlijn 0, met beide afwijkingen in `spec/CONSTRUCTS.md` rij 11.

Acceptance:
- (a) Rood eerst met de voorbeelden `WV-3.6-naar-rato` van het voorstel: 140 €, 100/61 € en 100 €, waar de vloer 0 geeft.
- (b) Golden `jutterswet-vast-startpunt` case 1: `toelagesom` wordt 100 × 3/366 + 100 × 2/365 = 6090/4453; dat past niet in het
  gehele doel (EF-006), dus het doel krijgt de onbegrensde vorm of een afronding (noem de keuze en oud en nieuw). De `ref`-test
  `verify-clip-per-fragment-floor` gaat mee, met oud en nieuw. Meet elke andere golden met een totaal over een geknipte tijdlijn
  en noem elke verandering.
- (c) RS ch. 3.6 en 3.12 (voorstel), het residu "afwezige tijdlijn telt leeg" vervalt (12c).
- (d) ref, clingo en beide Drools-branches; gate 17 gelijk.
- (e) `make gates` groen.

May touch: `ref/`, `backends/clingo/`, `backends/drools/**`, `spec/RUNTIME-SEMANTICS.md` (voorstel),
`corpus/golden/jutterswet-vast-startpunt/` en andere goldens met een totaal over een geknipte tijdlijn (verwachtingen),
`scripts/check-drools.sh` (voorstel), kaartverplaatsing.

Out of scope: de kalenderconventie zelf (in `20261103-timeline-arithmetic-per-period`).

## Omvang

Middel.
