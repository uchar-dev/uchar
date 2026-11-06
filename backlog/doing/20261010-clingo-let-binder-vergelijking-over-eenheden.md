# clingo-let-binder-vergelijking-over-eenheden: clingo vergelijkt een Let-binder met een waarde in een andere eenheid zonder herrekening

*Gevonden op 2026-10-10 door de bouwer van stap 2 van `20261008-tms-omschakeling` (bij de getuige
`TmsLikeRefWitnessTest`) en door de hoofdsessie vastgelegd volgens de afspraak dat een bestaande bug in ref, clingo of
de spec een kaart krijgt. Gemeten in een kopie van de claim `331b1179` van die kaart, met de verifier, ref en clingo uit
`_build` en de shaded jar van de claim. Het document en de case staan in de repository: `backends/drools/src/test/resources/like-ref/let-cross-unit/` (`ir.json`,
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

De DRL-teksttak is op dit document niet gemeten. `LetCarrierWalkWitnessTest.the_cross_unit_fence_recomputes_and_chooses_the_arm_of_ref`
pint het antwoord van de exec-model-tak (2.00, consistent) op hetzelfde document.

Ter vergelijking: het eenheiddocument van `TmsComputeFormWitnessTest` vergelijkt zonder binder een attribuut in m met
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

- De naam van deze kaart staat al in de javadoc van `TmsLikeRefWitnessTest` (document `let-kruiseenheid`, sinds
  `932229dc`) en van `LetCarrierWalkWitnessTest` (`the_cross_unit_fence_recomputes_and_chooses_the_arm_of_ref`); die
  verwijzingen kloppen zodra deze kaart onder deze naam in `backlog/new` staat, en hoeven bij de landing van de reparatie
  alleen te zeggen dat clingo nu 2.00 geeft.
- `20261009-clingo-let-binder-tussen-onderwerpen`: dezelfde backend en dezelfde knoop (`Let`), een ander gebrek; de
  twee kunnen in één sessie.

## Result

Gebouwd 2026-11-06 op branch `card/allocation-letbinder-dates`, in één commit met de golden van
`20261009-clingo-let-binder-tussen-onderwerpen` erna. Logs onder `werk/allocation-letbinder-dates/logs/`.

- (a) Twee goldens, eigen Jutterswet-casus (een Strook met een aanlooplengte in m, "1 km" ertegenover):
  `corpus/golden/jutterswet-loopgrens` met `source.rgs` (de binder onder een begrenzing "met een minimum van 1 km" en
  in een optelling "plus 1 km"; case 1, 500 m: de kale lezing kiest de andere grens, 500 in plaats van 1000; case 2,
  1500 m: beide lezingen kiezen dezelfde, 1500) en `corpus/golden/jutterswet-loopgrens-keuze` zonder bron (de vorm
  van de kaart, "als de loop groter is dan 1 km dan 1 anders 2"; de frontend schrijft geen `IfThenElse`, dus de
  waarde is met de hand in het document van de frontend gezet; case 1 500 m → 2, case 2 2000 m → 1, beide lezingen 1).
- (b) Rood eerst, met clingo van main `17f956d92` (`k2-red-main.log`, `k2-golden-loopgrens.log`,
  `k2-golden-keuze.log`): schouwlengte 500 (verwacht 1000), looplengte 501 en 1501 (verwacht 1500 en 2500),
  schouwklasse 1 (verwacht 2), `let-cross-unit` uit 1.00 (verwacht 2.00). Groen: ref ≡ clingo ≡ beide Drools-takken
  op beide goldens. Drie oorzaken, één reparatie in `backends/clingo/lib/translate.ml` en `normalise.ml`:
  `eff_unit` kreeg een arm voor `BinderRef` (de eenheid van de binder, uit de scope van een `Let` of uit het
  datatype in `ctx.binders`, dat voor een `Let` de effectieve eenheid van de waarde draagt: `binder_type`) en voor
  `Let`; de voorwaarde van een `IfThenElse` (op regelniveau en in een gesloten lichaam) vermenigvuldigde nooit met de
  factoren van `comparison_factors` en doet dat nu; de normalisatiestap daalt in de waarde en het lichaam van een
  `Let`, zodat "1 km" onder een binder naar 1000 m herschreven wordt. Nagegaan: de optelling (was stil 501, nu goed),
  de begrenzing (was stil 500, nu goed), de schrijfgrens (een binder in m naar een doel in km schreef stil 500 km,
  nu luid geweigerd zoals de vorm zonder binder: check in `backends/clingo/test`), een product van een binder met
  eenheid (werd geweigerd als verborgen eenheid, speelt nu: check), de vergelijking in een vouwfilter en in de test
  van een kwantorvouw met `greater` en `equal` (was stil fout: `jutterswet-grens-per-jutter`, regels "lange" en
  "passende vondsten"). Het minimum/maximum van opgesomde waarden weigert clingo al luid (CLINGO-008). Een binder in
  een kwantorvouw in een regelvoorwaarde bestaat niet: een `Let` omspant geen voorwaarde, en een
  `IfThenElse`-voorwaarde mag geen vouw lezen (WF-077). De javadoc van `TmsLikeRefWitnessTest` en
  `LetCarrierWalkWitnessTest` zegt nu dat clingo 2.00 geeft; `ASP-TRANSLATION.md` (S6) zegt dat een binder de eenheid
  van zijn waarde draagt.
- (c) Gate 12: beide goldens `PLAYED 2` op beide takken; `EXPECTED_PLAYED` 348 → 350 met regel. Gerichte gates
  groen (`logs/k23b/summary.txt`).
