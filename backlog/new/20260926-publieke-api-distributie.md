# publieke-api-distributie: één facade voor het gate-pad, en wat de distributie van uchar leest

De opvolger van yhavinga/regelspraak-java (uchar-dev/regelspraak-toolchain,
een distributie met uchar als git-submodule, pakket
`dev.uchar.regelspraak.toolchain.*`) leest uchar uitsluitend via de
publieke API. Het onderzoek van 2026-09-26 (rapport in
`../research/toolchain-verhuizing/onderzoek-2026-09-26.md`, §2 G1 en §4)
mat wat daar nu misgaat: de dienst, de scenariorunner en de engine-facade
van de oude toolchain bouwen met `ExecModelTranspiler.build(model, cl)`, en
die overload delegeert in uchar met LEGE zijtafels (defaults, rolbindingen,
lets, bronpaden, navigatiebindingen), waar de kopie in de oude toolchain de
defaults zelf uit het model las. Gemeten: de uitsluiting-tweeling noemt via
HTTP een uitgesloten jutter penninggerechtigd (4 van 4 fout, status 200,
geen marker). Geen van de zes aanroepers in uchar zelf gebruikt die
overload (`CaseRunner:269` en vijf getuigen geven alle zijtafels door). De
prelude van `CaseRunner.build` (lader → rekenjaarcheck → wederkerigheids-
checks → `FactModelGenerator` → `BeanCompiler` → `ContractValidator` →
`ExecModelTranspiler.build` met alle zijtafels → dekkingscheck → KieBase; de
DRL-tak ernaast) is privé, zodat een consument haar alleen kan nabouwen.
De scenariolaag (resolver, runner, `ScenarioResultaat`) en de regelgraaf
horen in uchar (maintainer, 2026-09-26): de scenariolaag komt met een eigen
kaart als frontend-extensie, de graaf wordt in een eigen kaart samengevoegd
en gegate. Deze kaart raakt ze niet.

## U-besluiten (hoofdsessie, 2026-09-26, bij de claim)

- U1 Eén publieke facade in `backends/drools` voor het gate-pad, van
  wire-document tot KieBase: invoer het geladen document (`JsonNode` of
  pad), uitvoer een record met model, beanlader, `ExecModelTranspiler`
  (of DRL-tak, per vlag), dekking en KieBase; dezelfde weigeringen als de
  replay (`Excluded` met dezelfde zinnen). `CaseRunner.build` gebruikt de
  facade — één prelude, geen tweede kopie — zodat gate 12 haar draait.
  Naam en vorm kiest de bouwer; javadoc noemt de consument (de toolchain).
- U2 De overload `ExecModelTranspiler.build(model, cl)` verdwijnt (nul
  aanroepers in uchar): stil weglaten van zijtafels moet een compilefout
  zijn, geen runtimegok. Elke andere overload die een zijtafel weglaat die
  het document kan dragen, gaat mee of weigert luid; hetzelfde voor
  `DrlTranspiler.build`. De getuigentests die een kortere overload
  gebruiken, geven de zijtafels expliciet (lege lijsten waar hun document
  er geen heeft).
- U3 `engine/ScenarioResultaat`, `graaf/` en de scenariolaag blijven
  onaangeroerd (eigen kaarten). Wat de distributie verder van de frontend
  leest — `Resolver.resolve`, `IrEmitter.emit`, de diagnoses — is al
  publiek; de bouwer meet met `jdeps` welke typen de facade naar buiten
  brengt en houdt die lijst in het Result.
- U4 (vervallen: de graaf blijft in uchar.)
- U5 Klaar: alle veertien gates groen; gate 12 speelt 209 en de DRL-tak
  179 (geen pin verschuift); de getuigentest voor U1: op elke golden met
  een `bron.rgs` geeft de facade dezelfde regeltelling in de KieBase als
  de replay (of: de replay ís de facade — dan een test dat de facade op
  `jutterswet-uitsluiting-rgs` twee regels in de KieBase zet, één regel en
  één defaults-familie); naamtabel-toets rc 0.
- U6 Geen spec-wijziging; `backends/drools/README.md` krijgt één alinea
  over de facade als de ingang voor consumenten. Zinnen: bestaande
  weigerzinnen ongewijzigd, geen regelnummerstaarten, L5-spelling.

## Acceptatie

- (a) U1, U2, U5 en U6 volledig; (b) `git grep -n "build(model, cl)"`
  leeg; (c) Result met de gekozen namen en de signatuur van de facade.

May touch: `backends/drools/**`, `frontends/regelspraak/src/**`. Niet in
deze kaart: spec, scripts, corpus (behalve dat gate 12's pins gelijk
blijven), `CLAUDE.md` (de hoofdsessie werkt "Repo relationships" bij de
landing bij).
