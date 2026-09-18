# drools-veelopveel-beanmodel: een relatie zonder ene-kant krijgt een drager

Rest van de bronpad-kaart (2026-09-18), door bouwer en lens gemeten:
§13.3.9 bergt een relatie in één scalair referentieveld aan de
ene-kant, dus een feittype dat beide rollen meervoudig declareert
krijgt aan geen van beide kanten een veld en elke consument weigert.
Drie corpusgoldens delen exact deze vorm: `anchor/jutterswet-bootberging`
en `jutterswet-muntketen` (hun middelste feittype `berging`, waardoor
hun bronPad-wandeling op stap 1 strandt) en `anchor/jutterswet-erflijn`
(de oorspronkelijke getuige van de weigering). Twee verwante gevallen
weigeren op de ándere, conservatieve arm — enkelvoudige elementrol met
meervoudige zusterrol (`jutterswet-gemeenteberging`,
`jutterswet-penninguitgifte`, besluit 2026-09-10) — en horen bij deze
kaart hooguit als afbakeningsgetuigen, niet als doel.

Vaststelling van de lens (read-only naslag): de oude toolchain-vorm van
de geneste aggregatie had in beide hops een ene-kant. Dit is dus geen
legacy-gat maar een uchar-eigen verbreding van het beanmodel.

Gemeten straal (lens, 2026-09-18): een collectierepresentatie raakt
`FactModelGenerator.leden()` (de enige beanrenderer), 18 echte
aanroepplekken van `heeftReferentieVeld`, 22 van `naarVeelRol`,
`ContractValidator`, de case-invoerkant van `CaseRunner` (die zo'n
relatie nu bij het schrijven weigert), beide transpilers en de
§9-creatie-idempotentiegarde (`subjectNaarVeelCreatie`). De javadoc van
`zelfpaarRollen` verwerpt collectievelden expliciet omdat ze
cardinaliteit aan elke `naarVeelRol`-consument zouden misrapporteren —
die afweging moet herzien, niet omzeild. Middel-tot-groot; eigen
gate-12-kanaalbeweging.

## Acceptatie (bij oppakken aanscherpen)

- (a) Rood-eerst: de drie goldens met hun huidige, één laag diepere
  weigering in beide takken.
- (b) Een collectiedragende representatie voor relaties zonder
  ene-kant, gedragen door beide transpilers, de case-invoer en de
  creatiepaden; de `naarVeelRol`/idempotentie-consumenten blijven
  correct (de herziene afweging gedocumenteerd).
- (c) De drie goldens spelen in beide takken met ongewijzigde
  verwachtingen; kanaalregels en `EXPECTED_PLAYED` bewegen mee
  (beschermd-padvoorstel); de conservatieve-arm-gevallen bewegen NIET.
- (d) Dual-branchgetuigen + mutatieproeven; alle veertien gates groen;
  registervoorstel.
