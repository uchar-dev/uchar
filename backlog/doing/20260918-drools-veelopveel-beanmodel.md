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

## Result

Branch `card/many-to-many`, gerebased op main `8e8fd6ee5` (tweede clingo-ronde). Commits: `0dadf06f2` (kaart naar
doing), `841a00553` (stap 1, case-invoer), `e5164c429` (stap 2 en 3, lezers en koppelingsstap in een pad), `743caf673`
(vervolgtaak: EF-039 en EF-040). Logs onder `uchar-dev/werk/many-to-many/logs/`, overdracht in
`uchar-dev/werk/many-to-many/OVERDRACHT.md`.

**(a) Rood eerst.** Gemeten op de jar van `0115ec5b5` (`logs/red-first.log`): `anchor/jutterswet-bootberging`,
`jutterswet-muntketen` en `jutterswet-ketenkwantor` weigeren in beide takken met DROOLS-067 (sourcePath-stap 1 heeft
geen beanveld, "both sides are many"), `anchor/jutterswet-gemeenteberging` met DROOLS-161, en de rode case
`to-one-second-filler` met EXCLUDE DROOLS-161. De suite op de bouw van stap 2 en 3 (`logs/suite-1.log`, 1885 tests, 12
rood) gaf de kantelende getuigen: de zes van het ontwerp en daarnaast
`BranchEqualityWitnessTest.the_drl_aggregation_join_refuses_a_join_without_field`,
`DefaultsConsequenceFoldWitnessTest.a_fold_without_a_bean_field_is_refused_with_the_form`, de twee
N:M-weigeringen van `SourceRoleFoldWitnessTest` en `StorageDirectionWitnessTest.a_multiple_self_pair_role_is_refused_loudly_with_the_true_reason`,
plus de vier ontbrekende DRL-pins. De getuige van stap 1 (`Input.link`) compileerde vóór de stap niet.

**(b) De representatie.** Geen collectieveld: een relatie van twee meervoudige rollen wordt over `RelationLink`-feiten
gelezen. `Input.link(factType, first, second)` is nieuw publiek API; `CaseInput` leest zo'n rolsleutel als één
koppeling, vanaf welke kant de case haar ook schrijft (ch. 5.7a); `DroolsBackend.insert` zet haar als `RelationLink`
van fase 0 in de sessie en weigert luid een koppeling die geen N:M-feittype noemt, een einde buiten de invoer of van
het verkeerde type, en een dubbele. `TmsTranspiler.linkRelations` = de gemaakte relaties ∪ elk niet-wederkerig
feittype van twee meervoudige rollen; de javadoc van `linkRelations` en van `RelationLink` noemt precies dat
criterium (een 1:N-invoerrelatie die geen vorm maakt, blijft een beanveld). Alle lezers die `createdRelations`
vroegen voor de koppelingsroute (`linkRules`, `recipientsJoin`, `oneStepRange`, `roleCheck`, `linkHop`, en beide
`SourcePath.parse`-aanroepen) vragen nu `linkRelations`; `createdRelations` blijft waar het om creatie gaat
(DROOLS-212, -234, -236, -259, de uniciteitsscope, `madeOnlyIn`). `SourcePath.Step` kent een derde soort hop, de
koppeling; met een koppelingsstap is elke tussenknoop een existentieel gebonden patroon en verbindt een
`RelationLink`-patroon de twee knopen (`TmsTranspiler.linkStep`). Het beanmodel, `ContractValidator`,
`hasReferenceField`/`toManyRole`, de §9-idempotentiegarde, de productieruntime en de JSON van de distributie zijn
niet aangeraakt; de herziene afweging staat in de javadoc van `FactModelGenerator.selfPairRoles`.

**(c) De goldens.** Op beide takken met hun ankerwaarden (`logs/replay-1.log`): bootberging PLAYED 2, muntketen
PLAYED 1, ketenkwantor PLAYED 4, gemeenteberging PLAYED 2; erflijn PLAYED 1 en penninguitgifte PLAYED 3 onveranderd.
Gate 12 vóór de rebase: 359 op beide takken, JUnit 1889 groen (`logs/gate12-1.log`). De conservatieve arm DROOLS-232
en `ObjectCreationWitnessTest` (32/32) blijven. Gate 8: `to-one-second-filler` geeft op de Drools-poot de gepinde
`FAULT EF-017 {"role":"bergende gemeente","label":"V1"}` (`logs/gate8-1.log`). Gate 17 groen (360 goldens vergeleken,
`DEVIATIONS` en `READINGS_ERROR_KNOWN` leeg, `logs/gate17-1.log`), gate 13 groen (`logs/gate13-1.log`).

**(d) Getuigen.** `ManyToManyWitnessTest` (7): de drie spellingen van ketenkwantor case 1 geven dezelfde koppelingen
en dezelfde waarden op beide takken; `insert` zet elke koppeling één keer in, fase 0; de vier weigerzinnen van
`insert`; en DROOLS-318 op beide takken in één zin voor een filter dat kan falen over één N:M-stap, over een
N:M-stap van een pad, en een kwantor met een falende matrix over een pad. De twaalf kantelende tests zijn positieve
getuigen geworden, elk met waarden die ref en clingo op hetzelfde document geven (gemeten,
`werk/many-to-many/docs/`, `ref-docs.sh`); één flank is nieuw: erflijn met invoerkoppelingen naast de gemaakte
(`StorageDirectionWitnessTest.a_link_of_the_input_beside_the_creations_of_the_same_relation_counts_once`: W1 2, W2 1,
W3 0, W4 3, in ref, clingo en beide takken). Mutatieproef (niet gecommit, `logs/mutation-a.log`): `first` en `second`
in `linkStep` verwisseld maakt ketenkwantor, bootberging en muntketen rood op beide takken.

**Register.** Nieuw DROOLS-318 `MANY_TO_MANY_FOLD_ORDER` (LIMIT, `factType,description`), NL en EN; DROOLS-078 en
DROOLS-161 `RETIRED`, genoemd in `CLAUDE.md` bij gate 12. De arm van DROOLS-078 (`noJoinFieldReason`) geeft voor wat
er nog komt, een feittype van meer rollen of een zusterrol die geen naam oplost, de zin van
`noReferenceFieldReason` (DROOLS-075), die geen kardinaliteit beweert; `step()` geeft voor twee meervoudige rollen van
een feittype van meer rollen DROOLS-158 ("een stap die geen rolpaar van twee rollen noemt"). Het deel
`PART.membership-both-multi-valued` blijft: het is nog bereikbaar voor een feittype van meer dan twee rollen
(WF-011 OPEN) en een wederkerige meervoudige rol (WF-013, ill-formed); de javadoc van `roleCheck` zegt het zo.

**Bewogen pins en vloeren.** `scripts/check-drools.sh`: vier `EXPECTED_CHANNEL`-regels weg, `EXPECTED_PLAYED` 355 →
359, na de rebase 358 → 362. `scripts/check-red.sh`: de uitsluiting van `to-one-second-filler` weg,
`EXPECTED_MIN_DROOLS_EXCLUDE_CODES` 15 → 14 (een bewuste verwijdering, in dezelfde commit, met reden).
`ReadingsExclusionWitnessTest.EXPECTED_MIN_EXCLUDED` 46 → 43 (vier goldens verlieten de uitgesloten set; gemeten 43).
DRL-pins: vier nieuwe (`anchor/jutterswet-bootberging`, `anchor/jutterswet-gemeenteberging`,
`jutterswet-ketenkwantor`, `jutterswet-muntketen`); geen bestaande pin veranderde, ook niet na de rebase.
`ScenarioReportWitnessTest` neemt `jutterswet-grens-per-jutter` (DROOLS-193) als document dat de backend niet draagt.

**Afwijkingen van het ontwerp.** Stap 2 en 3 staan in één commit (beide in `TmsTranspiler`). Twaalf kantelende tests
in plaats van zes (hierboven). DROOLS-078 en de deel-arm van `membershipStorage`: zie Register.

**Voorlopig besluit (normvraag, de eigenaar kan het omdraaien).** De volgorde van een N:M-bereik is per houder de
lijst van RUNTIME-SEMANTICS ch. 5.7a (geschreven leden in geschreven volgorde, daarna gematerialiseerde in
labelvolgorde), zoals ref al doet. De Drools-backend draagt die volgorde niet en weigert een geordende vouw over zo'n
bereik bij naam op beide takken (DROOLS-318); de vorm staat in de lijst van niet-gedragen vormen van
`backends/drools/README.md`. Geen golden heeft die vorm.

**Open, buiten deze kaart gelaten (technisch).** Een gemaakte relatie in een meerstapspad (DROOLS-236) en een
objectcreatie over een N:M-relatie (DROOLS-081 `both-many`) blijven geweigerd. Geen rij voor `spec/CONSTRUCTS.md` of
`spec/PLAN.md`: er is geen v1-afwijking van de draad.

### Vervolgtaak: EF-039 en EF-040 voor de case-invoer

Ch. 5.7b had geen code voor een dubbel label of een niet-gedeclareerde naam (EF-001 is een rolsleutel die twee rollen
oplost, EF-002 een koppeling naar een onbekend label). Nieuw, kanaal `case input`, naar het voorbeeld van EF-035:
EF-039 "an instance label occurs twice in the case input" (`label`) en EF-040 "the case input names an object type,
an attribute, a characteristic or a role the document does not declare" (`label`, `name`), met een zin in ch. 5.7
stap 3 en de gemeten parameters van de Drools-backend in ch. 5.7b. Ref en clingo delen de caselezer
(`ref/lib/case.ml`: `failc` op de vijf plekken; een rolsleutel die wel een rol noemt maar niet tegenover het type
blijft zonder code); Drools draagt beide als `CaseInput.NameFault` met een catalogusregel per soort (NL en EN).
Rode gevallen `corpus/red/duplicate-instance-label` en `corpus/red/undeclared-input-name`: gate 8 groen, 87 rode
gevallen, 40 foutsoorten, Drools-poot 71 FAULT met de gepinde code (`logs/gate8-2.log`).
`CaseNameRuleWitnessTest` pint de vier namen van EF-040 en EF-039 met code en parameters op beide takken;
`CaseInputWitnessTest` de rolsleutel. Een naam in een verwachting houdt zijn zin zonder code, in alle drie.
