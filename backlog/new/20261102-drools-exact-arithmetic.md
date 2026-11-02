# drools-exact-arithmetic: de Drools-backend rekent overal exact, ook waar geen gewoon bedrag het ziet

*Vastgelegd door de hoofdsessie op 2026-11-02 bij de landing van `20261009-drools-schrijfgrens` (zie daar "Snede" en het
Result, "Voor `drools-exact-arithmetic`"). Onderzoek en metingen: `/Users/yeb/Developer/uchar-dev/research/precisie-en-afronding-2026-11-01/`
(`notes/drools-backend.md` par. 8). Geen haast: de corpus en de gewone bedragen zien het niet.*

Goal: RUNTIME-SEMANTICS ch. 2.1 (exacte breuken, twee collapspunten) geldt in Drools op elke plaats waar het nu een
`MathContext` of een `BigDecimal`-schaal tussen de waarde en de lezer zet.

## Wat er nog afwijkt

- `rsPlus`, `rsMin`, `rsMaal` ronden af op `DECIMAL128` (`(10^31 + 0,001) − 10^31` geeft `0.00`).
- Een vouw (`Contribution`) sommeert decimalen; daarom weigert de backend een vouw over een onbegrensd attribuut met naam
  (DROOLS-296). `ExactSum` in beide renderingen en `Contribution` zonder collapse heffen die weigering op.
- Het tijdsevenredig deel staat op 16 cijfers; met ch. 2.4 op de tijdlijn in ref én Drools (ref kent daar nu geen grens,
  clingo wel; RUNTIME-SEMANTICS ch. 3.7 en 2.4 noemen dit OPEN).
- De wortel: `toBd` geeft het radicand als `DECIMAL64`; eenmaal correct onder zijn afronding.
- Ch. 5.12 schrijft een niet-eindigende waarde op "16 cijfers half to even", zodat 1/3 en 3333333333333333/10^16 gelijk lezen;
  voorstel `t/n`.
- De gebroken exponent: "eenmaal correct afgerond", met een weigering als benoemde limiet.
- Een onbegrensd attribuut dat een objectcreatie initialiseert, gaat als decimaal op de nieuwe bean (de creatie collapset bij
  de constructie), dus een lezer ziet daar 16 cijfers.
- De grens van de kwalificatie over de gehele periode (`ComputeHelpers.wholePeriod`) leest een `Breuk` via `toBd`.

Los, elk klein, mag mee of apart: clingo's EF-006 op `x/3 maal 3`, waar de waarde past (een `CLINGO-###` soort limit); de zinnen
ch. 2.5 "default when no mode is written" en ch. 2.3 min/max; de javadoc van `Breuk` ("impossible-by-resolver-guard" klopt niet).

Acceptance (bij oppakken aanscherpen):
- (a) Per punt een witness in de Drools-suite, eerst rood (clingo's bereik houdt deze getallen uit de differentiële corpus).
- (b) DROOLS-296 `RETIRED`, met een golden of witness voor een som over een onbegrensd attribuut (drie keer 1/3 is 1).
- (c) `make gates` groen; `EXPECTED_PLAYED` gelijk of hoger, met naam.

May touch: `backends/drools/**`, `ref/` (alleen voor ch. 2.4 op de tijdlijn), `spec/RUNTIME-SEMANTICS.md` (voorstel),
`scripts/check-drools.sh` (pin, voorstel).
