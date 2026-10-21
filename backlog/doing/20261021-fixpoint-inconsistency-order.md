# fixpoint-inconsistency-order: de inconsistenties van het fixpunt in een vaste volgorde

*Vondst van de toolchain-kaart `explanation-from-readings` (2026-10-21), nagelopen door de hoofdsessie op uchar
`15f39170`. Geen fout antwoord: de verzameling klopt, alleen de volgorde wisselt.*

## Aanleiding

`TmsTranspiler` (de lezing van het fixpunt, r. ±955–967) sorteert de fouten op de naam van de vorm en de zin, maar geeft
de §9.5-deposito's door in de volgorde van het werkgeheugen: `session.getObjects(…)` over de beans van `Inconsistentie`,
zonder sortering. `Fixpoint.inconsistencies` belooft geen volgorde. De dienst van de toolchain schrijft die lijst
ongewijzigd in het antwoord (`AnswerBuilder`, `inconsistencies` en `inconsistencyDetails`). Gemeten in de toolchain: op
dezelfde productiebuild gaf `jutterswet-keurspelling` tussen twee verzoeken een andere volgorde, en over de 231 goldens
van de replay verschilde de volgorde in 13 tussen twee sessies. De replay-leg van de uitleg vergelijkt de
inconsistenties daarom zonder volgorde.

## Doel

`Fixpoint.inconsistencies` in een vaste volgorde, zoals `faults`: op de regel, dan op het onderwerp (het label van een
invoerobject, de naam van een gemaakt object zoals de dump hem schrijft). De javadoc van `Fixpoint` noemt de volgorde.
`CaseRunner.inconsistencies` en `SessionDump` volgen dezelfde volgorde of laten zien waarom ze er niet van afhangen.
Nagaan of `objects` en `links` hetzelfde probleem hebben voor een gebruiker die ze in volgorde uitschrijft; zo ja, in
dezelfde kaart, met dezelfde toets.

## Acceptatie

- (a) Rood eerst: een getuige die een golden met meer dan één inconsistentie twee keer vuurt in verse sessies, met
  verschillende invoegvolgorde van de invoerobjecten, en dezelfde lijst eist; rood op `15f39170`.
- (b) De Drools-suite en gate 12 groen; de productiebuild verder ongewijzigd.
- (c) In de toolchain, bij de volgende pin-bump: de replay-leg vergelijkt de inconsistenties weer in volgorde.

May touch: `backends/drools/src/main/java/dev/uchar/regelspraak/transpiler/tms/` (`TmsTranspiler`, `Fixpoint`),
`cases/CaseRunner.java`, een getuigetest, deze kaart.

Out of scope: de productieruntime; de volgorde van de waarden en kenmerken (die gaan naar de beans en het antwoord
leest ze per object).

## Result (2026-10-21)

Tak `card/fixpoint-order-and-refusal-reading`, vanaf `0f45965f`, samen met `reading-after-refusal`. Geraakt:
`backends/drools` (`TmsTranspiler`, `Fixpoint`, een javadoc in `CaseRunner`, een getuige), de kaart. Acceptatie (c) is de
toolchain (een andere bouwer).

- `0cb1525c` de kaart naar `doing/`.
- `e4eecf8d` de getuige `FixpointOrderWitnessTest` (acceptatie (a)), rood.
- `bd74da98` de vaste volgorde van `objects`, `links` en `inconsistencies`, en de javadoc van `Fixpoint`.

**Rood eerst.** `FixpointOrderWitnessTest`: elke case van `jutterswet-keurcriteria` (11 deposito's op vier objecten in
`case2-over-de-drempels`), `jutterswet-afrekening`, `jutterswet-havenketen`, `jutterswet-premiepot` en
`jutterswet-dubbelverbinding` (gemaakte objecten en koppelingen), op beide takken in verse sessies met de invoerobjecten
in de volgorde van de case, omgekeerd en een plaats geroteerd; de drie lijsten, in hun volgorde uitgeschreven met de
caselabels (een gemaakt object naar zijn creatie en de naam van zijn onderwerp), moeten tussen die sessies gelijk zijn.
Vloeren (het meeste in een case, gemeten 2026-10-21): `EXPECTED_MIN_MOST_INCONSISTENCIES=11`,
`EXPECTED_MIN_MOST_OBJECTS=7`, `EXPECTED_MIN_MOST_LINKS=7`; 66 sessies, 1,0 s. Rood op `e4eecf8d`
(`u4/logs/red-order-1.log`): 24 verschillen, zes cases per tak, elk in beide andere volgordes
(`keurcriteria/case2`, `afrekening/case1` en `case2`, `premiepot/case1`, `dubbelverbinding/case1` en `case3`), op alle drie
de lijsten. Het eerste:

```
EXEC_MODEL jutterswet-keurcriteria/case2-over-de-drempels.json: eerst Lists[objects=[], links=[], inconsistencies=[
  keuring naar een samengesteld criterium @ "V6" [EF-025], keuring naar een samengesteld criterium @ "V4" [EF-025],
  keuring naar precies een criterium @ "V5" [EF-025], …]] | met de invoer in een andere volgorde Lists[objects=[],
  links=[], inconsistencies=[keuring naar ten minste twee criteria @ "V5" [EF-025], keuring naar ten hoogste een
  criterium @ "V6" [EF-025], …]]
```

Groen sinds `bd74da98`.

**De volgorde.** `TmsTranspiler.readFixpoint` sorteert op namen van de objecten (`orderNames`):

- een invoerobject: zijn caselabel, gequoteerd zoals de dump quoteert, in de lezing met labels
  (`DroolsBackend.fixpointOf(build, session, label)`);
- een gemaakt object: zijn naam in de dump, `«"creatie"»(onderwerp)#k`, met k het aantal fasen waarin die creatie voor
  dat onderwerp een object maakte; een onderwerp dat zelf gemaakt is, heet al zo uit een eerdere fase (een creatie
  neemt geen onderwerp van haar eigen fase; een merk waarvan het onderwerp nog geen naam heeft, wordt luid geweigerd);
- `objects` op de naam van het object; `links` op feittype, eerste rol, naam van het eerste object, tweede rol, naam van
  het tweede; `inconsistencies` op regel, naam van het onderwerp, dan de EF-codes (deze backend zet één feit per
  deponering, dus één regel × onderwerp kan twee feiten met verschillende codes hebben); `faults` zoals voorheen.

Waarom stabiel tussen sessies: geen van de sleutels leest een identiteit of een invoegvolgorde. Het caselabel is de naam
die de afnemer het object geeft, uniek per case. Creatie en onderwerp van een gemaakt object volgen uit de invoer, en de
fase is de generatie van de driver, die volgt uit het fixpunt van de eerdere fasen en niet uit de volgorde van de agenda:
`BranchDumpsCorpusWitnessTest` houdt de dumps, die gemaakte objecten zo noemen, gelijk tussen twee takken met een
verschillende agenda. Zonder labels (`fixpointOf(build, session)`) heet een invoerobject naar type en identiteit en
verschilt de volgorde van twee invoerobjecten tussen sessies, zoals de zin van een fout daar; de javadoc zegt het.

`objects` en `links` hadden hetzelfde probleem (de getuige was er rood op: `afrekening`, `premiepot`, `dubbelverbinding`)
en zijn in deze kaart meegenomen. `values` en `characteristics` blijven ongeordend (buiten de kaart; een afnemer leest ze
per object). De schrijfhandelingen van de terugzetting staan in de volgorde van vóór de kaart; alleen de lijsten zijn
gesorteerd. De javadoc van `Fixpoint` noemt de volgorde per lijst.

**Wie een andere volgorde ziet.**

- `SessionDump` (de dump, gate 12's `BranchDumpsCorpusWitnessTest`): niets; zij sorteert elke regel van de sectie `read`.
- De replay (`CaseRunner`, gates 12 en 13): niets; `CaseRunner.inconsistencies` leest de sessie zelf, `consistency` zoekt
  een passend feit en `renderIncs` sorteert wat het toont (nu in de javadoc van `inconsistencies`).
- De CLI (`DroolsBackendMain`): niets; de gewone uitvoer komt van de replay, `--readings` van `SessionDump.readingLines`
  (gesorteerd), dus gate 17 ongewijzigd.
- De tests die `Fixpoint`-lijsten lezen, sorteren ze of zoeken erin; `TmsConsumerWitnessTest` vergelijkt twee lezingen
  zonder labels van één sessie (dezelfde namen), groen.
- Een afnemer van `DroolsBackend.fixpointOf` of `TmsTranspiler.readOf` die de lijsten in volgorde uitschrijft: die ziet nu
  één volgorde. In de toolchain (gelezen, niet geraakt): `FixpointGivens` sorteert objecten, koppelingen, fouten en
  inconsistenties zelf en ziet dus niets; `AnswerBuilder` schrijft `outcome.inconsistencies()`, en die komt uit
  `Executor.readInconsistencies(session)`, de sessie zelf, niet uit het fixpunt. Voor (c) moet de toolchain-kaart die
  lijst dus uit `fixpoint.inconsistencies()` nemen; dan pas verdwijnen de 13 verschillen van de replay-leg.
- Geen golden en geen gepinde tekst verschuift: gate 12 `--fast` played 261, gate 17 dezelfde tellingen, de suite groen.

**Acceptatie (b).** De Drools-suite groen (1648 tests); gate 12 `--fast` groen; de volledige gate 12 door de
hoofdsessie. De productiebuild verder ongewijzigd: de plannen en de DRL-tekst raakt deze kaart niet, alleen de lezing
van het fixpunt.

#### Voorgestelde tekst voor `CLAUDE.md`

Geen: gate 12 en 17 eisen niets nieuws. Voor `spec/CONSTRUCTS.md` en `spec/PLAN.md`: niets.

#### Voor de toolchain (acceptatie (c))

`Executor` geeft `AnswerBuilder` de inconsistenties van `readInconsistencies(session)`; met de pin-bump naar deze tak
kan het `fixpoint.inconsistencies()` geven (dezelfde verzameling, nu in vaste volgorde, met labels gelezen), en kan de
replay-leg van de uitleg de inconsistenties weer in volgorde vergelijken.

#### Gates

De volledige set door de hoofdsessie op `bd74da98` (`werk/defaults-decider/u4/gates-1`): alle zeventien `rc=0`,
`EXIT 0` (gate 12 798 s, played 261 op beide takken, JUnit 1648; gate 13 228 s; gate 17 430 s, afwijkingen 0, geen
`readings ERROR`). Gebouwd door een Opus-bouwer op één tak met de kaart `reading-after-refusal`; de hoofdsessie draaide de
set, landde het voorstel voor `scripts/check-readings.sh` en de `CLAUDE.md`-tekst, en schreef dit Result af.

De toolchain-kant (acceptatie (c)) zit in de toolchain-kaart `explanation-fold-members`: `AnswerBuilder` leest de
inconsistenties uit `fixpoint.inconsistencies()`, en na de pin-bump vergelijkt de replay-leg ze weer in volgorde.
