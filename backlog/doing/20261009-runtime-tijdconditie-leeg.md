# runtime-tijdconditie-leeg: `rsTijdConditie` leest een lege kant van een vergelijkingsknip als geen periode

*Gevonden op 2026-10-09 door de bouwer van stap 2 van `20261008-tms-verdeling-en-tijdlijnen` en door een lens op die
stap, en door de hoofdsessie nagemeten en vastgelegd volgens de afspraak dat een bestaande bug in de gedeelde runtime
een kaart krijgt. Gemeten met ref, clingo en de verifier uit `_build` van main `ac0f9008` en de shaded jar van die
kaartbranch (`c2005edf`).*

## Aanleiding

RUNTIME-SEMANTICS ch. 3.6 laat een totaal knippen op "de tijd dat" een vergelijking van een eigen tijdsafhankelijk
attribuut met een grens geldt, en past daarbij ch. 1.2 per periode toe. Het voorbeeld rond regel 1774 ("gedurende
'≠ 1000' where the tested timeline has leeg periods: leeg **satisfies** ≠ against a filled bound … 500,00") pint dat
voor ongelijkheid. Ch. 1.2 zegt verder dat bij een getal twee keer leeg gelijk is. De runtimehulp `rsTijdConditie`
(`backends/drools/.../runtime/RegelSpraakRuntime.java`) geeft bij een lege grens meteen geen perioden, en leest een gat
in de getoetste tijdlijn als geen periode. Beide oude Drools-takken gebruiken die hulp.

Document: een Jutter met vergoeding 10 per maand van januari tot en met juni 2024, controle 5 alleen in januari, en
"het totaal van zijn vergoeding gedurende de tijd dat zijn controle (on)gelijk is aan zijn grens". Verifier PASS.

| Geval | ref | oude exec-model- en DRL-teksttak |
|---|---|---|
| gelijk aan, grens leeg | 50 | 0 |
| gelijk aan, grens 5 | 10 | 10 |
| ongelijk aan, grens 5 | 50 | 0 |
| ongelijk aan, grens leeg | 10 | 0 |

Clingo vertaalt gelijkheid in een knip niet ("equality in a per-segment clip comparison is not translated (no corpus
case exercises it)"). De tak met truth maintenance weigert "gelijk aan" en "ongelijk aan" in een knip met naam tot de
hulp klopt. De documenten zijn uitgeschreven in de rijen `knip-gelijk` en `knip-ongelijk` van
`TmsTimelineRefusalWitnessTest`.

## Acceptatie (bij oppakken aanscherpen)

- (a) Een golden met de vier gevallen hierboven, eigen Jutterswet-casus; de verwachtingen zoals ref.
- (b) `rsTijdConditie` gerepareerd (of een eigen hulp in de tak met truth maintenance, als de oude takken dan al weg
  zijn), rood eerst; de tak met truth maintenance draagt de vorm dan in plaats van haar te weigeren.
- (c) Clingo vertaalt gelijkheid in een knip, of weigert haar met naam zoals nu.
- (d) Alle gates groen.

## Omvang

Klein.

May touch: `backends/drools/**` (de runtimehulp en de weigering in `transpiler/tms/`), `backends/clingo/`,
`corpus/golden/<nieuwe golden>/`, `scripts/check-drools.sh` (pins, voorstel), kaartverplaatsing.
