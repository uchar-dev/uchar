# conditions-per-period: voorwaarden per periode en de dagentelling (impliciete tijd, fase 1c en 2c)

*Opgesteld door de hoofdsessie op 2026-11-03 uit het Result van `backlog/done/20261010-impliciete-tijd.md` en het voorstel
`spec/proposals/IMPLICIT-TIME.md`, punt 3 tot en met 8. Na `20261103-timeline-arithmetic-per-period`. Besluiten van de eigenaar
2026-11-03: **per emmer** (4): op een regel met een tijdsafhankelijk doel is de gehele-periodekwalificator een
waarheidstijdlijn per emmer van zijn raster, de vouw blijft voor een scalair doel; **0** (7a): de dagentelling geeft 0 in een
emmer zonder ware dag; alle tijd (2). Besloten zonder normvraag: 3, 5, 6, 7b, 7c, 8. De kaart mag in tweeën: 1c-i (3, 4, 5, 8)
en 1c-ii (6, 7).*

## Stap A — spec, ref, clingo, verifier, frontend

Goal: een voorwaarde op een regel met een tijdsafhankelijk doel geldt per periode (de veralgemeende knip van ch. 3.10;
RegelSpraak §10.3), de kwalificator daarin per emmer, twee schrijvers van één tijdlijndoel per dag (fout alleen op een dag waar
beide beslissen en verschillen), een tijdsafhankelijk kenmerk binnen een venster met een dagprecieze datumexpressie als grens, de
dagentelling met 0 in een lege emmer, een dagsoorttoets op de getelde dag en dagen per rastereenheid als eenheid, en een fijnere
operand statisch geweigerd; in ref, clingo, de verifier en de frontend, met formulering A van de ankercasus als anker.

Acceptance:
- (a) Rood eerst: anker-golden `corpus/golden/anchor/jutterswet-verzuimrente-30-360` (beschermd: voorstel) met geval 1 en 2 van
  formulering A (IMPLICIT-TIME §4): rentedragend door de regel van datum tot datum, de twee schrijvers van de rentedagen, de
  maandrente, het totaal; per maand 16 2/3, 33 1/3, 33 1/3, 41 2/3, 16 2/3 en 25, 41 2/3, 5 5/9; totaal **141 2/3 €** en
  **72 2/9 €**. Op main de weigeringen van IMPLICIT-TIME §5 (frontend Z1, Z2; verifier Z3; ref Z4; clingo Z8).
- (b) De frontend schrijft "van zijn vervaldatum plus 1 dg tot en met zijn betaaldatum" (RGS-202 verruimd), crasht niet op een
  variabele met een samengestelde dagentelling (`IrEmitter.unknownType`, `IrEmitter.java:761`; document
  `werk/implicit-time/meting/zoek-crash/`), en schrijft de samengestelde voorwaarde met dagsoorttoets als filter in plaats van
  `Unresolved` (Z17).
- (c) Verifier (voorstel): WF-111 verruimd (VB 6.1), WF-058 verruimd (7b, 7c), een nieuwe WF-regel voor het raster van operand en
  voorwaarde (VB 8.1; nu laten frontend en verifier een operand per dag in een maanddoel door, `forms/p08`); een negatief per
  arm.
- (d) RS ch. 3.3, 3.8, 3.9, 3.10, 3.11, 3.12 en 5.7 (voorstel); de verwachtingen van de goldens met een dagentelling naar 7a: het
  anker `corpus/golden/anchor/jutterswet-dagentelling` (beschermd: voorstel), `jutterswet-dagentelling-rgs`,
  `jutterswet-vast-startpunt` en `jutterswet-pachtvenster`, elk met oud en nieuw in het Result; de rode casus
  `time-window-timeline-characteristic` wordt een golden.
- (e) clingo weigert of draagt een tijdsafhankelijk kenmerk in de voorwaarde van een regel met een tijdsafhankelijk doel, nooit
  stil een lege tijdlijn (nu wel, `forms/p03b`); ref schrijft geen dagentelling zonder eenheid in een doel met eenheid dg/mnd
  (nu wel, `forms/p07b`; ch. 2.6 regel 6).
- (f) De `ref/test`-rijen `WV-3.10-voorwaarde-per-periode`, `WV-3.9-per-emmer`, `WV-5.7-per-dag`, `WV-3.10-kenmerk-venster`,
  `WV-3.8-nul-en-dagsoort` en `WV-3.4-fijner-geweigerd` van het voorstel letterlijk.
- (g) `make gates` groen.

May touch: `ref/`, `backends/clingo/`, `spec/verifier/` (voorstel), `spec/RUNTIME-SEMANTICS.md`, `spec/IR-SPEC.md`,
`spec/ASP-TRANSLATION.md` (voorstel), `corpus/golden/anchor/` (voorstel), de goldens met een dagentelling (verwachtingen),
`corpus/negative/<nieuw>/`, `corpus/red/` (de overgang van `time-window-timeline-characteristic`), `frontends/regelspraak/**`,
`scripts/check-drools.sh`, `scripts/check-red.sh` (voorstel), kaartverplaatsing.

Out of scope: het totaal over een gedeeltelijke eenheid (`20261103-total-over-partial-unit`); verschuiven en eenmalig (punt 10);
tijdsafhankelijke feittypen (punt 13).

## Stap B — Drools (direct na de landing van stap A)

Acceptance: (a) rood eerst: de goldens van stap A geven op main DROOLS-102, DROOLS-103, DROOLS-114, DROOLS-271 of de vouw van
Z13 (IMPLICIT-TIME §5); (b) `PLAYED` op beide branches, `EXPECTED_PLAYED` omhoog, kanaalpins weg (voorstel), codes zonder plek
`RETIRED`; de goldens met een dagentelling spelen met hun nieuwe verwachtingen; (c) gate 17 gelijk, een getuige per nieuwe vorm in
elke regelvolgorde; (d) `make gates` groen.

May touch: `backends/drools/**`, `scripts/check-drools.sh` (voorstel), kaartverplaatsing.

## Omvang

Groot (stap A, eventueel in tweeën), middel tot groot (stap B).
