# weergaven-als-getuige: plan, executable model en DRL-tekst van het corpus vastgezet in de repository

*Vastgelegd door de hoofdsessie bij de landing van `20261008-drl-teksttak-op-truth-maintenance` (2026-10-14), uit
besluit 41 van die kaart (Fable-advies 4, §1). Een vervolg op die kaart, geen fout.*

## Aanleiding

Tijdens die kaart bewaakten drie tekstdumps over het hele corpus de analyse en haar twee weergaven: de planvorm (het
plan per regel, 250 goldens, 1851 regels), de modelvorm (het executable model, 21558 regels) en de drlvorm (de
DRL-tekst, 256 teksten, 41836 regels), elk tegen een nulmeting in `/tmp`. Fable-advies 4 bracht zeventien mutaties in;
vier glipten door het gedragsharnas en alleen een tekstdump zag ze:
- W1, de volgorde van de feiten binnen één gevolg, en W5, een bredere catch om het creatiebudget: geen semantiek, maar
  een verandering van de tekst;
- W4 en A1, de `collapse` van een breuk in rust weggelaten in de weergave of in het plan (sinds de landing heeft dat een
  gedragsgetuige).

Na de landing bestaan die nulmetingen niet meer. Een verandering in een weergave is dan alleen nog zichtbaar waar ze
het gedrag verandert.

## Acceptatie (bij oppakken aanscherpen)

- (a) Kies de vorm: de dumps zelf in de repository (groot), of per golden een hash met een hulpmiddel dat de dump
  opnieuw maakt en het verschil toont. Weeg de omvang tegen de leesbaarheid van een review.
- (b) Een getuige in de suite vergelijkt de dumps van de huidige code met wat vastligt, en faalt met de naam van de
  golden en de eerste afwijkende regel; een bewuste wijziging zet de vastgelegde vorm in dezelfde commit opnieuw.
- (c) Rood eerst met een mutatie als W1 van Fable-advies 4.

May touch: `backends/drools/**` (testboom, testbronnen).

Out of scope: `scripts/` (geen nieuwe gate; de suite draait in gate 12).

## Result

Bouwer drools-residue, 2026-11-06, branch `card/drools-residue`. Logs in `werk/drools-residue/logs/`.

**(a) De vorm: de DRL-tekst zelf, per golden een bestand.** Gemeten op deze branch: de DRL-tekst van de 355 goldens die
de exec-model-tak bouwt (`DrlRenderingWitnessTest -Duchar.drlvorm`, `logs/card4-drlvorm.log`) is 61 789 regels, 3,0 MB
(242 KB gzip); het corpus is 23 MB, het pakbestand van de repository 85 MB. De tekst ligt vast in
`backends/drools/src/test/resources/drl-text/<golden>.drl` (de map van een golden als map, `anchor/…`). Gekozen boven
een hash per golden: een review leest de wijziging van een weergave dan als diff van de commit, en een getuige kan de
eerste afwijkende regel met beide teksten noemen; een hash kan alleen zeggen dat er iets veranderde. Alleen de
DRL-tekst: die schrijft het plan regel voor regel uit, dus een wijziging van het plan staat erin; het executable model
heeft geen tekst van zichzelf (de "modelvorm" van de kaart `drl-teksttak-op-truth-maintenance` bestaat niet meer) en
blijft aan dezelfde fixpunten gehouden door `BranchDumpsCorpusWitnessTest`. Geen boodschap uit een catalogus staat in
de tekst (alleen EF-codes), dus een wijziging van een catalogus raakt de pins niet.

**(b) De getuige.** `DrlTextPinWitnessTest` (suite, gate 12): per golden de tekst van `DrlRendering` tegen zijn pin,
faalt met de golden en de eerste afwijkende regel met beide regels, een golden zonder pin en een pin zonder golden
falen bij naam; vloer `EXPECTED_MIN_PINS=355`. Met `-Duchar.drlpin=src/test/resources/drl-text` (vanuit
`backends/drools`) schrijft de getuige alle teksten opnieuw en verwijdert pins van verdwenen goldens, vóór hij
vergelijkt; een bewuste wijziging zet zo de pins in dezelfde commit opnieuw. `DrlRenderingWitnessTest.goldens()` is
daarvoor package-private. Twee keer gedraaid in een verse JVM, groen (`logs/card4-groen.log`): de tekst is gelijk
tussen runs.

**(c) Rood eerst.** Zonder pins faalt de getuige op elke golden ("no pin", `logs/card4-rood-zonder-pins.log`). Met
mutatie W1 (niet gecommit: de feiten van een besluit van een defaultsstack in omgekeerde volgorde in de DRL-tekst,
`x.facts().reversed()` in `DrlRendering`) faalt hij met de golden en de regel, bijvoorbeeld
`anchor/jutterswet-hardheid.drl: line 38: the pin has «… new HasCharacteristic(…)», the text «… new Decides(…)»`;
`BranchDumpsCorpusWitnessTest` blijft groen op dezelfde mutatie (`logs/card4-rood-W1.log`), zoals Fable-advies 4 het
van W1 mat. Daarna hersteld; `git diff` van `DrlRendering` leeg.

**Voor de hoofdsessie.** Een kaart die een golden toevoegt of de analyse of een weergave verandert, schrijft de pins
opnieuw: `cd backends/drools && mvn -o test -Dtest=DrlTextPinWitnessTest -Duchar.drlpin=src/test/resources/drl-text`.
Een golden die een parallelle branch toevoegt (de clingo-bouwer) faalt na de rebase als "no pin" tot die stap.

**Rebase op main `bc3249090` (2026-11-06).** De pins opnieuw geschreven met de aanroep hierboven: twee nieuwe,
`jutterswet-keurquotient.drl` en `jutterswet-macht-van-nul.drl`, en geen bestaande tekst veranderd (357 pins;
`logs/rebase-pins.log`). Gate 12 daarna groen (`logs/rebase-gate12.log`: 355 gespeeld op beide takken, JUnit 1882), gate 1 groen.
