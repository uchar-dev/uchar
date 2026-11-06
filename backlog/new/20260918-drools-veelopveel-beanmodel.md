# drools-veelopveel-beanmodel: een relatie zonder ene-kant krijgt een drager

Rest van de bronpad-kaart (2026-09-18), door bouwer en lens gemeten:
§13.3.9 bergt een relatie in één scalair referentieveld aan de
ene-kant, dus een feittype dat beide rollen meervoudig declareert
krijgt aan geen van beide kanten een veld en elke consument weigert.
Drie corpusgoldens delen exact deze vorm: `anchor/jutterswet-bootberging`
en `jutterswet-muntketen` (hun middelste feittype `berging`, waardoor
hun sourcePath-wandeling op stap 1 strandt) en `anchor/jutterswet-erflijn`
(de oorspronkelijke getuige van de weigering). Twee verwante gevallen
weigeren op de ándere, conservatieve arm — enkelvoudige elementrol met
meervoudige zusterrol (`jutterswet-gemeenteberging`,
`jutterswet-penninguitgifte`, besluit 2026-09-10) — en horen bij deze
kaart hooguit als afbakeningsgetuigen, niet als doel.

Vaststelling van de lens (read-only naslag): de oude toolchain-vorm van
de geneste aggregatie had in beide hops een ene-kant. Dit is dus geen
legacy-gat maar een uchar-eigen verbreding van het beanmodel.

Gemeten straal (lens, 2026-09-18): een collectierepresentatie raakt
`FactModelGenerator.members()` (de enige beanrenderer), 18 echte
aanroepplekken van `hasReferenceField`, 22 van `toManyRole`,
`ContractValidator`, de case-invoerkant van `CaseInput` (die zo'n
relatie nu bij het schrijven weigert), beide transpilers en de
§9-creatie-idempotentiegarde (`subjectToManyCreation`). De javadoc van
`selfPairRoles` verwerpt collectievelden expliciet omdat ze
cardinaliteit aan elke `toManyRole`-consument zouden misrapporteren —
die afweging moet herzien, niet omzeild. Middel-tot-groot; eigen
gate-12-kanaalbeweging.

## Ontwerp en voorlopig besluit (Fable-advies 2026-11-06)

*Het volledige ontwerp, met een proef in een kloon (100 regels, `proef.diff`), het stappenplan, de zes tests die
kantelen en wat niet mag veranderen: `uchar-dev/werk/norm-advice-2026-11-06/many-to-many/ADVIES.md` (§5 is het
stappenplan, §6 de bewaking).*

Geen collectieveld op de bean. Een feittype met twee meervoudige rollen wordt altijd als koppelingsfeiten gelezen
(`RelationLink`, fase 0 voor de invoer), via de route die de tak al gebruikt voor een relatie die een vorm maakt
(`linkRange`, `linkHop`, `roleCheck`, `recipientsJoin`); nieuw is alleen de koppelingsstap in een meerstaps-
`sourcePath`. Het beanmodel, de consumenten van `hasReferenceField` en `toManyRole`, `ContractValidator`, de
productieruntime en de JSON van de distributie blijven onaangeraakt. In de proef spelen `anchor/jutterswet-bootberging`,
`jutterswet-muntketen`, `jutterswet-ketenkwantor` en `jutterswet-gemeenteberging` in beide takken met hun
ankerwaarden, de lezingen zijn gelijk aan de trace van ref, en de rest van het corpus beweegt niet. DROOLS-161 en
DROOLS-078 worden `RETIRED`; de conservatieve arm DROOLS-232 en `anchor/jutterswet-penninguitgifte` (PLAYED 3) blijven.

*Voorlopig besluit (normvraag, de eigenaar kan het omdraaien):* de volgorde van een N:M-bereik is per houder de lijst
van RUNTIME-SEMANTICS ch. 5.7a (geschreven leden in geschreven volgorde, daarna gematerialiseerde in labelvolgorde),
zoals ref al doet. De Drools-backend draagt die volgorde in deze kaart niet en weigert een geordende vouw over zo'n
bereik (een filter die kan falen, een deposito, een geordende kwantor) bij naam op beide takken, met een nieuwe code;
geen golden heeft die vorm. De hoofdsessie maakt daarvoor geen vervolgkaart (doel van de eigenaar: een kleinere
backlog): de weigering staat in de lijst van niet-gedragen vormen van `backends/drools/README.md`.

*Naamgeving (hoofdsessie, 2026-11-06):* `RelationLink` blijft heten zoals het heet: het is één koppeling van een
relatie, welke kardinaliteit ook. Het criterium welke relaties over koppelingen gelezen worden, staat in de
verzameling `linkRelations` (nu `createdRelations`): gemaakt door een vorm, of beide rollen meervoudig. De javadoc van
`RelationLink` en van `linkRelations` noemt precies dat criterium; een 1:N-invoerrelatie die geen vorm maakt, blijft
een beanveld.

## Acceptatie (bij oppakken aanscherpen)

- (a) Rood-eerst: de drie goldens met hun huidige, één laag diepere
  weigering in beide takken.
- (b) Een collectiedragende representatie voor relaties zonder
  ene-kant, gedragen door beide transpilers, de case-invoer en de
  creatiepaden; de `toManyRole`/idempotentie-consumenten blijven
  correct (de herziene afweging gedocumenteerd).
- (c) De drie goldens spelen in beide takken met ongewijzigde
  verwachtingen; kanaalregels en `EXPECTED_PLAYED` bewegen mee
  (beschermd-padvoorstel); de conservatieve-arm-gevallen bewegen NIET.
- (d) Dual-branchgetuigen + mutatieproeven; alle veertien gates groen;
  registervoorstel.

## Na `20261008-tms-omschakeling` (2026-10-11)

Na `20261008-tms-omschakeling` (2026-10-10) speelt `anchor/jutterswet-erflijn` in de exec-model-tak (gate 12, 248) en
weigert alleen de DRL-teksttak hem nog; `anchor/jutterswet-bootberging` en `jutterswet-muntketen` weigeren in beide
takken, in de exec-model-tak met 'sourcePath-stap 1 (Boot -> Vondst/Munt) heeft geen beanveld'. De kaart raakt twee
takken, `transpiler/tms/` (`TmsTranspiler`) en `DrlRendering`.

## Na `20261010-objectcreatie-zonder-connectingrole-binding` (2026-10-11)

`anchor/jutterswet-penninguitgifte` speelt sinds die kaart in de exec-model-tak (PLAYED 3, met de absolute
ankerwaarden):
- Zijn twee creaties maken `penningbezit`, en een gemaakte relatie leest de tak over koppelingsfeiten, niet over het
  gedeelde beanveld `Penning.houder`.
- De §13.3.9-weigering van deze kaart geldt in de exec-model-tak dus alleen nog voor een invoerrelatie die geen vorm
  maakt. De getuige daarvan is `ObjectCreationWitnessTest.a_count_assertion_over_a_shared_field_is_refused_loudly`,
  op de golden zonder haar creaties.
- De DRL-teksttak weigert het anker nog.
