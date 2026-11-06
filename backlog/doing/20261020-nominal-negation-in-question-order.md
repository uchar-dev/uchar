# nominal-negation-in-question-order: "niet een <kenmerk>" in de vragende volgorde

*Gevonden in de fixronde na de review van frontend-kwantor-over-een-pad (2026-10-20), gemeten op `bed92c3f`;
proeven `uchar-dev/werk/kwantor-pad/fixronde/n1-subject-niet-een` en `n6-subject-de-niet-een` (alleen lezen).
Luid, geen fout antwoord.*

Tabel 16 (§8.1.8.3) ontkent een zelfstandig kenmerk met "is niet een <k>". In de stellende volgorde ("hij is niet
een wrakstuk") heeft de grammatica daar een tak voor (`nonToplevelOperator`, `NIET EEN kaleNaam`), en die speelt
(proef `n9-subject-stellend-niet-een`: PASS in ref, clingo en beide Drools-takken). In de vragende
volgorde van een voorwaarde niet: in "indien de Vondst niet een wrakstuk is" komt `niet` in het onderwerp, de
EEN-vorm zoekt de naam "niet wrakstuk", die bindt niets, en de voorwaarde blijft een marker met de WARNING
"onopgelost: de geschreven naam bindt geen kenmerk en geen rol van het onderwerp" (EF-019 in ref en clingo, een
evaluatiefout in beide Drools-takken). "indien hij niet een wrakstuk is" is een syntaxfout ("no viable alternative
at input 'nieteen'"). Onder een kwantor leest de frontend de vorm sinds de fixronde van 2026-10-20 wel
(`RuleChecker.elementCharacteristicCheck`); "geen wrakstuk" speelt in elke gemeten vorm (proeven n2, n7–n9,
f2-pad-geen).

Acceptatie:
- (a) Rood eerst: een JUnit-getuige met beide zinnen.
- (b) "indien <onderwerp> niet een <zelfstandig kenmerk> is" lowert naar de ontkende kenmerktoets, gelijk aan
  "geen <k>", voor het onderwerp zelf, "hij" en een navigatie naar één; na "niet een" blijft een ander soort
  kenmerk of een naam die niets bindt luid geweigerd.
- (c) DIVERGENCES (§13.4.15.45 of de tabel van vorm 1 in §4); alle zestien gates groen.

May touch: `frontends/regelspraak/**`.

## Result

Gebouwd op branch `card/drools-frontend-cleanup` (opruimronde 2026-11-06). Logs onder
`uchar-dev/werk/java-cleanup/logs/`, proeven onder `uchar-dev/werk/java-cleanup/probe/k6/`.

- **(a)** Getuige `NominalNegationInQuestionOrderTest`. Rood op `890519e8d` (`k6-red.log`): "indien de Vondst niet
  een wrakstuk is" liet een marker met de WARNING "onopgelost …", en "indien hij niet een wrakstuk is" was de
  syntaxfout "no viable alternative at input 'nieteen'".
- **(b)** De grammatica kreeg in `toplevelOperator` het alternatief `gehelePeriodevergelijking? NIET EEN kaleNaam
  ( IS | ZIJN )`, de zuster van de stellende tak, vóór het `EEN? kaleNaam`-alternatief. `RuleChecker` leest het
  zoals de stellende tak (de positieve EEN-vorm, omgedraaid; een rol krijgt de WARNING RGS-214) en geeft de `NIET`
  door aan de kwantorvouw. Neemt het onderwerp `niet` als laatste woord ("de Vondst niet", "zijn gemelde vondst
  niet"), dan leest de EEN-vorm eerst de geschreven naam "niet <k>" en daarna de naam na `niet`, ontkend, voor het
  onderwerp zelf (`characteristicCheckOn`) en over een navigatie naar één (`navigationCharacteristicCheck`). Groen
  (`k6-green3.log`): de drie vormen (onderwerp, "hij", "zijn gemelde vondst") geven dezelfde ontkende kenmerktoets
  als "geen wrakstuk", op de brontekst na; "niet een zwaar" (bijvoeglijk) en "niet een drijfhout" (bindt niets)
  blijven een marker met hun WARNING. `QuantifierOverPathTest` en `NotInExceptionTest` blijven groen. Geen bron in
  het corpus of de pariteitszone schrijft "niet een" (gezocht), dus geen document verandert.
- **(c)** DIVERGENCES: de alinea "niet een" onder een kwantor eindigt niet meer op de marker en de syntaxfout; een
  nieuwe alinea "'niet een' in the question order" beschrijft de lezing. Gates 10, 11 en 16 groen (`k6-gate10.log`,
  `k6-gate11.log`, `k6-gate16.log`); de hele set in de eindrun.
