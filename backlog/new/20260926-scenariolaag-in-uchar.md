# scenariolaag-in-uchar: scenariotoetsen als frontend-extensie, gelowerd naar casebestanden

De "scenariotoetsen"-extensie van de oude toolchain (Situatie-definities en
Scenario-blokken in RegelSpraak-stijl; `docs/SCENARIOTOETSEN.md` in
uchar-dev/regelspraak-toolchain, 264 regels: doel, oppervlak, semantiek,
runner, gates) hoort in uchar (maintainer, 2026-09-26): het is semantiek
over de IR, geen presentatie. uchars grammatica parseert de blokken al
(`RegelSpraak.g4` r. 101–191: `situatieDefinitie`, `scenario`,
`scenarioScope`, `scenarioPeildatum`, `scenarioBasis`, `situatieBlok`,
`situatieWaarde`); de resolver negeert ze. In de oude toolchain:
`ScenarioResolver` (785 regels, resolverpakket; gebruikt zeven
package-private leden van de resolver), `ResolvedScenario` (102),
`ScenarioRunner` (571), `ScenarioRunnerCli` (274), `ScenarioResultaat`
(61; staat al in uchar, ongebruikt), `Verbalisator` (433, uitlegtekst),
`ParametersetLader` (326, parametersets per peildatum voor de dienst),
`RegelSpraakEngine` (187, compile→session→fire); tests
`ScenarioTypeGateTest`, `ScenarioParseTest`, `ScenarioMutatieTest`,
`ParametersetScenarioTest`, `ScenarioRunnerTest`, `ScenarioOrakelTest`,
`ScenarioMutatieRunTest`. uchars tegenhanger van een scenario is het
caseformaat (`spec/schema/uchar-case-v0.schema.json`: `asOfDate`,
`input.instances`, `input.parameters`, verwachtingen `attributeValue`,
`attributeEmpty`, `attributeTimeline`, `characteristicPresent`,
`characteristicAbsent`, `consistent`, `inconsistent`), dat de drie
uitvoerders al spelen (gates 5, 6, 12). Ontwerp: de scenariolaag wordt een
frontend-extensie met een LOWERING naar casebestanden; de replay is de
runner. Rechten: de code komt uit de mirror (Apache-2.0, eigen werk); de
fixtures van de oude toolchain (`examples/toka-scenarios`) zijn ©-materiaal
en komen nergens in uchar — alle getuigen zijn Jutterswet.

## U-besluiten (hoofdsessie, 2026-09-26, bij de claim)

- U1 `ScenarioResolver` en `ResolvedScenario` komen in `frontends/
  regelspraak` (resolver- en ir-pakket, op uchars grammatica en records);
  hun package-private toegang is daarmee gewoon. De resolver-diagnoses
  volgen het regime: Engelse lezerszinnen waar de verifier een zin heeft,
  anders eigen zinnen van de extensie, vastgelegd in `grammar/
  DIVERGENCES.md` (nieuwe sectie "Scenariotoetsen"), waar ook
  `SCENARIOTOETSEN.md` in opgaat (of ernaast als eigen document — bouwer).
- U2 Lowering `ResolvedScenario` → casedocumenten (`uchar-case-v0`, in de
  canonieke sleutelvolgorde van gate 2): situatie → `input` (instanties met
  attributen, kenmerken, rollen; parameters), peildatum → `asOfDate`,
  verwachtingen → `expectations`; `gebaseerd op de situatie` wordt
  uitgevouwen; de scenarionaam bepaalt de bestandsnaam
  (`case<n>-<slug>.json`, bouwer legt de slugregel vast). Een
  `scenarioScope` (toetst de regel/regels/regelgroep) lowert naar het
  IR-document: de scope bepaalt welke regels het geëmitteerde `ir.json`
  draagt — geen wijziging van het caseformaat, geen uitvoerder verandert.
  Wat niet in het caseformaat past, is een WEIGERING met zin, nooit stil
  verlies; de bouwer meldt elke vorm uit `SCENARIOTOETSEN.md` die zo
  weigert in het Result.
- U3 `Resolver` krijgt een modus die naast `--ir` de casebestanden schrijft
  (`--cases <map>`, bouwer). De frontendsuite (`make test`) draagt de
  geporteerde scenariotests op Jutterswet-fixtures: parse, typegate,
  mutatie (verkeerd type, onbekend attribuut, dubbele situatie, kringen
  in `gebaseerd op`), elk rood-eerst.
- U4 Nieuwe gate (**voorstel**, `scripts/check-scenario.sh`, protected —
  de hoofdsessie landt haar en schrijft de CLAUDE.md-regel): elke golden
  met een scenariodocument (`scenario.rgs` naast `bron.rgs`, bouwer legt
  de naam vast) regenereert haar casebestanden byte-identiek en, bij een
  scope, haar `ir.json`; floor `EXPECTED_MIN_SCENARIO_GOLDENS`; een lege
  verzameling faalt. Gate 4 blijft de schemapoort; gates 5/6/12 blijven de
  replay.
- U5 Getuigen: scenariodocumenten voor ten minste vijf bestaande goldens,
  waaronder de drie dialect-tweelingen (`ww-herleving-inkomen-rgs`,
  `jutterswet-uitsluiting-rgs`, `jutterswet-nestkenmerk-rgs`), waarvan de
  lowering de GEPINDE casebestanden byte-identiek reproduceert (het
  sterkste bewijs: de handgeschreven verwachtingen zijn het anker); elke
  verwachtingssoort van het caseformaat komt ten minste eenmaal voor; één
  golden met een scope.
- U6 De runner: `ScenarioRunner`, `ScenarioRunnerCli`, `RegelSpraakEngine`
  komen NIET naar uchar (de replay is de runner; de distributie bouwt haar
  CLI op frontend-lowering + de replay-facade van de kaart
  publieke-api-distributie). `ScenarioResultaat` blijft in
  `backends/drools` als het rapportrecord dat de replay vult (status per
  verwachting, vuringen, dekking) — de bouwer meet wat de oude runner
  rapporteert boven wat `CaseRunner` al berekent en port dát;
  `Verbalisator` (uitleg) en `ParametersetLader` zijn presentatie en
  dienst-infrastructuur: distributie. Markerexit en trace: Result.
- U7 Klaar: alle gates groen inclusief de nieuwe; gate 10 byte-identiek
  op alle bestaande goldens; naamtabel-toets rc 0; geen D-/U-nummers in
  code, scripts, DIVERGENCES; de pariteitszone alleen tellen.

## Acceptatie

- (a) U1–U7; (b) `make ir` en de casemodus op de vijf getuigen: `cmp` leeg
  tegen de gepinde bestanden; (c) een scenario met een niet-lowerbare vorm
  eindigt in een diagnose met zin.

May touch: `frontends/regelspraak/**`, `corpus/golden/*/scenario.rgs`
(nieuw, bouwer), `scripts/check-scenario.sh` (**voorstel**),
`backends/drools/**` (alleen `ScenarioResultaat` en het rapport van de
replay), `spec/schema/` alleen als een verwachtingssoort ontbreekt
(**voorstel**, met verifier/ref/clingo/Drools-dekking — liever niet).
Niet in deze kaart: `CLAUDE.md`, `WAY-OF-WORKING.md`, `spec/PLAN.md`
(hoofdsessie bij de landing).
