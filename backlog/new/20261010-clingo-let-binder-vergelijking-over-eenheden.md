# clingo-let-binder-vergelijking-over-eenheden: clingo vergelijkt een Let-binder met een waarde in een andere eenheid zonder herrekening

*Gevonden op 2026-10-10 door de bouwer van stap 2 van `20261008-tms-omschakeling` (bij de getuige
`TmsZoalsRefWitnessTest`) en door de hoofdsessie vastgelegd volgens de afspraak dat een bestaande bug in ref, clingo of
de spec een kaart krijgt. Gemeten in een kopie van de claim `331b1179` van die kaart, met de verifier, ref en clingo uit
`_build` en de shaded jar van de claim. Het document en de case staan in de repository: `backends/drools/src/test/resources/zoals-ref/let-kruiseenheid/` (`ir.json`,
`case.json`).*

## Aanleiding

Een vergelijking tussen twee waarden met een eenheid uit één stelsel herrekent de rechterkant naar de eenheid van de
linker (`spec/RUNTIME-SEMANTICS.md` ch. 2.6). Een consument leidt de eenheid af uit de declaraties, niet uit een
annotatie op de draad (ch. 2.10).

Het document `let-kruiseenheid`: objecttype Baan met `aanloop` (geheel getal, eenheid m; stelsel Afstand met m en km =
1000 m) en `uit` (getal met 2 decimalen), en één regel, "uit van een baan":

    Let a = aanloop in (als a groter dan 1 km dan 1,00 anders 2,00)

In de IR is de voorwaarde een `Comparison` `greater` met links de `BinderRef` `a` en rechts de literal 1 met eenheid
km. Case: B1 met aanloop 500; verwacht `uit` 2.00 en consistent, want 500 m is niet groter dan 1 km.

| Uitvoerder | Uitkomst |
|---|---|
| verifier | `PASS` |
| ref | `oracle: PASS (2 expectations)`: uit 2.00 |
| clingo | `MISMATCH: B1.uit = [100], expected 200`: uit 1.00, dus 500 > 1 zonder herrekening |
| Drools, de destructieve exec-model-tak (op de claim) | `EXCLUDE transpiler SKIP: regel: uit van …` (de meting kapt de zin af) |
| Drools, de tak met truth maintenance (sinds de omschakeling de exec-model-tak) | `PLAYED 1`: uit 2.00 |

De DRL-teksttak is op dit document niet gemeten. `LetDragerWandelingWitnessTest.het_cross_unit_hek_herrekent_en_kiest_de_tak_van_ref`
pint het antwoord van de exec-model-tak (2.00, consistent) op hetzelfde document.

Ter vergelijking: het eenheiddocument van `TmsRekenvormWitnessTest` vergelijkt zonder binder een attribuut in m met
een literal in km ("Een Sleepbaan is lang indien zijn sleeplengte groter is dan 3 km"), en dat document weigert clingo
met naam ("non-identical units under a comparison are not translated"; javadoc van die getuige, gemeten 2026-10-09).
Gevolgtrekking, niet gemeten: die weigering mist de route via een `BinderRef`, waar clingo de getallen kaal vergelijkt.

Geen golden heeft een Let-binder in een vergelijking met een waarde in een andere eenheid. Het verschil met
`20261009-clingo-let-binder-tussen-onderwerpen`: daar lekt de waarde van een binder naar een ander onderwerp in een
kwantorvouw onder een telling; hier gaat het om één onderwerp en de herrekening van een eenheid. Het zijn twee gebreken,
dus twee kaarten.

## Acceptatie (bij oppakken aanscherpen)

- (a) Een golden met deze vorm, eigen Jutterswet-casus, rechtenvrij, met `source.rgs`, en ten minste een case waarin de
  kale vergelijking de andere tak kiest (zoals 500 m tegen 1 km) en een waarin beide lezingen dezelfde tak kiezen.
- (b) Clingo gerepareerd, rood eerst: ref ≡ clingo ≡ Drools (exec-model-tak) op de golden, tegen absolute
  verwachtingen. Nagaan of dezelfde lacune bestaat voor een binder in een optelling met een waarde in een andere eenheid
  en voor een binder in andere vergelijkingsvormen; wat zo gevonden wordt, krijgt een case of een check in
  `backends/clingo/test`.
- (c) Gates 5, 6 en 12 groen; als de golden in een Drools-tak speelt, gaan de pins van gate 12 mee (beschermd pad:
  voorstel); alle gates van `CLAUDE.md` groen.

## Omvang

Klein tot middel.

May touch: `backends/clingo/**`, `corpus/golden/<nieuwe golden>/`, `scripts/check-drools.sh` (pins; voorstel),
kaartverplaatsing.

## Raakvlakken

- De naam van deze kaart staat al in de javadoc van `TmsZoalsRefWitnessTest` (document `let-kruiseenheid`, sinds
  `932229dc`) en van `LetDragerWandelingWitnessTest` (`het_cross_unit_hek_herrekent_en_kiest_de_tak_van_ref`); die
  verwijzingen kloppen zodra deze kaart onder deze naam in `backlog/new` staat, en hoeven bij de landing van de reparatie
  alleen te zeggen dat clingo nu 2.00 geeft.
- `20261009-clingo-let-binder-tussen-onderwerpen`: dezelfde backend en dezelfde knoop (`Let`), een ander gebrek; de
  twee kunnen in één sessie.
