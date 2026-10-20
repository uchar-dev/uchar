# ref-gehele-periode-lege-grens: ref leest "ongelijk aan" tegen een lege grens onder "gedurende het gehele" als waar

*Gevonden op 2026-10-09 door de bouwer van stap 3 van `20261008-tms-verdeling-en-tijdlijnen` en door de hoofdsessie
nagemeten en vastgelegd volgens de afspraak dat een bestaande bug in ref een kaart krijgt. Gemeten met ref, clingo en
de verifier uit `_build` van main `ac0f9008` en de shaded jar van die kaartbranch (`58614e85`).*

## Aanleiding

RUNTIME-SEMANTICS ch. 3.9 (rond regel 2387): "A leeg comparison bound makes the test false, never a fault." Ref volgt
dat niet voor ongelijkheid: het leest "ongelijk aan" tegen een lege grens per dag met de regel van ch. 1.2 (precies één
leeg is waar) en laat de kwalificatie slagen.

Document (`TmsWholePeriodWitnessTest`, deel A3; verifier PASS): een premie "indien zijn tijdlijn gedurende het gehele
jaar ongelijk is aan zijn grens", met een lege grens.

| Uitvoerder | J5.ongelijkpremie |
|---|---|
| ref | 10 ("MISMATCH: J5.ongelijkpremie = 10, expected leeg", in alle volgordes) |
| clingo | vertaalfout: "a whole-period comparison against a bound other than a literal or a scalar parameter is not translated" |
| Drools, alle drie de takken | leeg (de runtimehulp `rsGeheelPeriode` volgt ch. 3.9) |

Geen golden en geen ref-test pinnen een lege grens onder de kwalificatie.

## Acceptatie (bij oppakken aanscherpen)

- (a) Ref volgt ch. 3.9 voor elke vergelijking, rood eerst met een ref-test. Kiest de eigenaar toch de lezing van ch. 1.2
  per dag (een normvraag), dan gaan ch. 3.9, `rsGeheelPeriode` en de tak met truth maintenance mee.
- (b) Een golden met een lege scalaire parameter als grens (die vertaalt clingo wel), met "gelijk aan" en "ongelijk aan".
- (c) Alle gates groen.

## Omvang

Klein.

May touch: `ref/`, `corpus/golden/<nieuwe golden>/`, kaartverplaatsing.
