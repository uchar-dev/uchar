# drools-exec-defaults-consequentie-vouw: een vouw als consequentie van een default speelt niet in de exec-tak

*Vondst van een regelsetbouwer, 2026-09-30 (skelet-vervolg: de laatste brug
K1 die bleef), bevestigd door de hoofdsessie op uchar `b0aab696` via de
distributie (`bin/regelspraak check|scenario|cases`) en de eigen
uitvoerders; reproductie op Jutterswet-materiaal in
`/tmp/v2/probe/defaults-consequentie/` (bron.rgs, bron-gewoon.rgs,
scenario.rgs, cases/; alleen lezen). Luid (exit 3), geen fout antwoord; wel
het laatste gat dat een regelset dwingt tot een hulplid voor een datum die
een `Afwijking van` of de opgave-markering boven zich heeft. Op verzoek van
de eigenaar geschreven (2026-09-30), tweeling van
drools-exec-defaults-justificatie-vouw.*

## Aanleiding

De kaart drools-exec-defaults-justificatie-vouw liet de rechtvaardiging
van een default tellen en ver lezen; de consequentie van een default bleef
daarbuiten. Zodra de basisregel van een `Afwijking van`-boom (of van de
groep die `(mag opgegeven worden)` genereert) haar waarde als vouw over een
rol berekent — "de laatste van de bergdag van alle vondsten van de Berging"
— markeert de exec-tak de hele groep: "defaults (Berging.sluitdag):
niet-reduceerbare consequentie (far-side/aggregatie): <default>". Dezelfde
vouw in een gewone regel speelt sinds drools-exec-datumvouw-over-rol (exit
0). Een regelset die een datum als de vroegste of laatste over haar gronden
afleidt en daar een uitzondering of een opgave boven zet, schrijft daarom
de vouw in een gewone regel op een hulplid en laat de default dat lid
lezen.

## Minimale reproductie

`b0aab696`, distributie `bin/regelspraak check|scenario|cases`; ref via
`_build/default/ref/bin/runner.exe`, clingo via
`_build/default/backends/clingo/bin/clingo_runner.exe`.

    Objecttype de Berging (mv: Bergingen)
        de gemelde sluitdag Datum in dagen;
        de sluitdag Datum in dagen;
    Objecttype de Vondst (mv: Vondsten)
        de bergdag Datum in dagen;
    Feittype BergingBetreftVondst
        de berging Berging
        de vondst (mv: vondsten) Vondst
    Eén berging betreft meerdere vondsten

    Regel sluitdag
        geldig altijd
            De sluitdag van een Berging moet berekend worden als de laatste van de bergdag van alle vondsten van de Berging.

    Afwijking van sluitdag: gemelde sluitdag gaat voor
        geldig altijd
            De sluitdag van een Berging moet gesteld worden op zijn gemelde sluitdag
            indien zijn gemelde sluitdag gevuld is.

    Scenario 1: Berging B met vondsten van 03-03-2026 en 09-03-2026, geen gemelde sluitdag;
    verwacht sluitdag 09-03-2026. Scenario 2: B met gemelde sluitdag 12-03-2026 en één vondst;
    verwacht 12-03-2026.

    R1  bron.rgs, zoals hierboven.
        nu: check "niet uitvoerbaar in uchar (exit 3): transpiler SKIP: defaults: defaults
        A:Berging.sluitdag — defaults (Berging.sluitdag): niet-reduceerbare consequentie
        (far-side/aggregatie): <default>" (resolved=0); scenario MARKER voor beide scenario's; ref
        `oracle: PASS (1 expectations)` en clingo `clingo: PASS (1 expectations)` voor beide cases.
    R2  bron-gewoon.rgs: alleen `Regel sluitdag`, zonder de afwijking.
        nu: check exit 0 zonder meldingen (de vouw in een gewone regel speelt).
    R3  dezelfde vorm met `(mag opgegeven worden)` op de sluitdag in plaats van de afwijking (meting
        van de regelsetbouwer, `/tmp/v2/probe/skelet-2/`, A5): dezelfde weigering op
        `berekende sluitdag`, met één vouw en met twee vouwen als operanden.

## Acceptatie

- (a) Meet de vormruimte in beide Drools-takken en in ref en clingo: de
  consequentie van de basisregel, van een uitzondering en van de gegenereerde
  groep van de markering als vouw over een rol (eerste, laatste, minimum,
  maximum, som, aantal), als vouw binnen een berekening (vouw plus een duur),
  en twee vouwen als operanden van één minimum of maximum; met een
  rechtvaardiging die telt of ver leest erbij (de vorm van
  drools-exec-defaults-justificatie-vouw); één en twee rollen ver.
- (b) De exec-tak speelt een default waarvan de consequentie een vouw over
  een rol is, met dezelfde lowering als de gewone regel (de vouw als eigen
  accumulate, de consequentie leest haar uitkomst) en met dezelfde uitkomst
  als ref en clingo, ook in de gegenereerde groep van de markering; wat niet
  meegaat blijft een benoemde weigering met de vorm in de zin; rood-eerst
  JUnit-getuige met R1 en R3.
- (c) Golden (werknaam `jutterswet-sluitdag-gemeld`), handgerekend, in ref ≡
  clingo ≡ Drools-exec; `EXPECTED_PLAYED` (230) en `EXPECTED_DRL_PLAYED`
  (191) in `scripts/check-drools.sh` als **voorstel**; de DRL-teksttak volgens
  haar kanaal (zij weigert defaultsgroepen).
- (d) Corpussweep ongewijzigd voor de bestaande goldens; alle zestien gates
  groen (bash waar de shebang dat zegt). `backends/drools/README.md`: de
  alinea over de rechtvaardiging noemt nu ook de consequentie;
  `spec/proposals/PRIORITIZED-DEFAULTS-EVOLUTIE.md` krijgt de registertekst
  van de hoofdsessie.

May touch: `backends/drools/**`, `corpus/golden/<nieuwe golden>/`,
`scripts/check-drools.sh` (**voorstel**), kaartverplaatsing. Niet: `ref/`,
`backends/clingo/`, `spec/`, `frontends/`. NB: serieel met de andere
Drools-kaarten (zelfde transpiler); na de landing kan het laatste K1-lid van
de regelset weg.

## Result (2026-09-30/10-01, bouwer)

Tak `card/drools-exec-defaults-consequentie-vouw` in de worktree van de kaart, afgetakt van main `3fad946f`; niet
gerebased, niet gepusht, niet gemerged. Gemeten met de eigen uitvoerders van deze repo:
`_build/default/ref/bin/runner.exe`, `_build/default/backends/clingo/bin/clingo_runner.exe`,
`_build/default/spec/verifier/bin/verifier.exe`, de frontend (`Resolver --ir … --cases … bron.rgs scenario.rgs`) en de
Drools-jar (`java -jar <jar> ir.json case…`, `--drl` voor de teksttak). "Vóór" is de jar van de claim `2a9ba859`
(backend gelijk aan main), "na" de jar van de kop. Elke vorm is een eigen Jutterswet-document (bron.rgs + scenario.rgs),
door een generator geschreven, door de frontend naar ir.json en cases verlaagd en door een meetscript gemeten; beide
staan buiten de repo (`/tmp/v2/probe/dcv-bouw/vorm/maak.py`, `/tmp/v2/probe/dcv-bouw/meet.sh`). Alle 72 documenten
zijn verifier-schoon (`verifier: PASS`).

### De reproductie op deze kop

- R1, `bron.rgs` van de kaart (de basis van een `Afwijking van` vouwt): ref `oracle: PASS (1 expectations)` en clingo
  `clingo: PASS (1 expectations)` voor beide cases; exectak vóór `EXCLUDE transpiler SKIP: defaults: defaults
  A:Berging.sluitdag — defaults (Berging.sluitdag): niet-reduceerbare consequentie (far-side/aggregatie): <default>`,
  na `PLAYED 2`; DRL-tak `EXCLUDE drl: ch. 7 prioritizedDefaults — the DRL-text branch has no defaults lowering`.
- R2, `bron-gewoon.rgs` (alleen de gewone regel), met het eerste scenario: exectak vóór en na `PLAYED 1`; DRL-tak
  `EXCLUDE drl-transpiler MARKER: regel: sluitdag — TODO aggregatie LAATSTE: de laatste van de bergdag van alle vondsten
  van de Berging`.
- R3 letterlijk gelezen, de markering op de sluitdag in plaats van de afwijking (`r3-letterlijk`): speelt al vóór de
  fix, `PLAYED 2`. De frontend verlegt een gewone regel naar `berekende sluitdag` als gewone regel (DIVERGENCES §7, stuk
  2), en de gegenereerde groep leest alleen `opgegeven sluitdag` en `berekende sluitdag`. De weigering "op `berekende
  sluitdag`" ontstaat pas als de markering boven een afwijkingsboom staat waarvan de basis vouwt: dan verlegt zij de
  boom als groep naar `berekende sluitdag` (`r3-boven-afwijking`): vóór `EXCLUDE transpiler SKIP: defaults: defaults
  A:Berging.berekende sluitdag — defaults (Berging.berekende sluitdag): niet-reduceerbare consequentie
  (far-side/aggregatie): <default>`, na `PLAYED 3`. Die vorm, met één en met twee vouwen, is R3 in de getuige. De
  gegenereerde groep zelf krijgt een vouw als consequentie via een initialisatie van het gemarkeerde attribuut, die de
  markering als laagste default in haar groep tilt (DIVERGENCES §7, stuk 3; vormen `a-mark-*`).

### Oorzaak

`ExecModelTranspiler.compileDefault` verlaagde een consequentie met `scalarWaarde`, zonder de vouwslots die de waarde
van een gewone regel sinds drools-exec-datumvouw-over-rol heeft. Een vouw over een setbron is geen waarde die de
consequentie kan uitrekenen maar een accumulate op de linkerkant van de regel; `scalarWaarde` gaf daarom `null` terug,
en `compileDefault` weigerde de hele groep in één verzamelzin (`niet-reduceerbare consequentie (far-side/aggregatie):
<default>`; `<default>` omdat een default op de draad geen brontekst draagt).

### Fix en de gekozen lowering

`ExecModelTranspiler` (+41, −15 regels, commentaar inbegrepen); `Stratificatie`, de loader, de beangeneratie en de
DRL-tak zijn niet aangeraakt.

- `compileDefault` verlaagt een consequentie met de vouwslots aan: elke vouw over een setbron wordt een accumulate
  (`vouwslot`, dezelfde `aggregatie` als in de waarde van een regel; naast een ordenende vouw en een som is de telling
  gebonden, zodat een lege bron een rij geeft en leeg leest), gebonden aan het onderwerp van de groep (`RechtvaardigingBouw.subject`), en de consequentie leest haar
  uitkomst uit een slot in de map die elke `WaardeFn` doorgeeft. Rond de vouw geldt zo elke operator van de scalaire
  waarde: datumrekenkunde, een gelijste vouw over twee vouwen, een keten van rollen, de lege operanden van ch. 1.2.
- De slots staan per groep in `RechtvaardigingBouw.vouwslots`, met sleutels die in de groep uniek zijn. `defaultsGroep`
  zet hun accumulates op de reduce-regel na die van de rechtvaardigingen, bindt per vouw telling en uitkomst na de
  globals, en vult de slots (`vulVouwslots`) voordat `DefaultsRun` beslist. Een lege consequentie valt door naar de
  volgende default (ch. 7.1), zoals voorheen.
- Wat niet vertaalt, weigert met de vorm en de consequentie: `consequentie niet vertaald, <reden van de vouw of de
  navigatie>: <consequentie>`. De zin voor te veel gebonden variabelen noemt nu ook de vouwen.
- De volgorde van de groepen hoefde niet te veranderen: `groepVolgorde` las de consequentie al (`verzamelLezingen`,
  sinds drools-exec-datumvouw-over-rol ook de leden en de subselectie van een eerste/laatste-vouw op het elementtype),
  dus een groep wacht op de schrijvers van wat haar vouw leest. Mutatieproef: met de consequentie uit die lezingen
  faalt de getuigenrij over een subselectie die een latere groep vult (`een groep, getoetst op leeg ==> … <case:
  attribuut sluitdag: verwacht 2026-03-12, werkelijk 2026-03-05>`).

Verworpen: een eigen evaluator voor de consequentie naast `scalarWaarde` (de weg die `lowerVal` voor de gewone regel
ging en die drools-exec-datumvouw-over-rol weghaalde); de vouw in de rechtvaardigingslijst `gebonden` opnemen in plaats
van eigen slots (dan leest de consequentie haar niet via de waardemachinerie en valt de operator rond de vouw weg).

### (a) Vormruimte, vóór en na

Situatie van de gegenereerde vormen (één case, `vijf bergingen`): B1 (kustwacht K1) met V1 (bergdag 09-03-2026,
meldingsdag 05-03-2026, gewicht 4), V2 (03-03-2026, 06-03-2026, 7) en V3 (geen bergdag, 12-03-2026, 2), in die
volgorde; B2 (K1, gemelde sluitdag 12-03-2026, gemeld gewicht 30) met V4 (01-03-2026, 02-03-2026, 5); B3 (K2) zonder
vondsten; B4 (gemelde sluitdag 20-03-2026, gemeld gewicht 25) zonder vondsten; B5 (K2) met V5 zonder waarden; bij de
markering B6 met een opgave (sluitdag 15-03-2026, maatgewicht 40). Strandvoogd S1 (sluitingsdatum 25-03-2026) beheert
K1, S2 K2; K1 heeft sluitingsdatum 22-03-2026, K2 een gemelde sluitdag 18-03-2026, K3 een gemeld gewicht 11.

Legenda. a-<positie>-<soort>: positie `basis` (de basis van een `Afwijking van`-boom; de afwijking stelt de gemelde
waarde indien gevuld), `uitz` (een `Uitzondering op` met de vouw, "indien zijn gemelde … leeg is", boven "gesteld op
zijn gemelde …"), `mark` (de gegenereerde groep van de markering: een initialisatie met de vouw als laagste default
onder `opgegeven X` en `berekende X`), `r3` (de markering boven een afwijkingsboom waarvan de basis vouwt: de vouw in
de groep op `berekende X`); soort eerste en laatste (bergdag), minimum, maximum en som (gewicht), aantal (vondsten).
b-*: een vouw in een berekening, "(de laatste van …) plus 14 dagen", "(de eerste van …) min 7 dagen", "(de som van
het gewicht …) plus 1". c-*: twee vouwen als operanden, "de laatste van (de laatste van de bergdag …) en (de laatste
van de meldingsdag …)", de eerste daarvan, "de maximale waarde van (de som van het gewicht …) en (het aantal …)" en de
minimale. d-*: met een rechtvaardiging die telt of ver leest: `basis-telt` (de afwijking "indien het aantal vondsten
van de Berging groter is dan 0"), `uitz-telt` (de uitzondering vouwt "indien het aantal … groter is dan 1"), `uitz-som`
("indien de som van het gewicht … groter is dan 4"), `uitz-een-rol` ("de sluitingsdatum van de kustwacht van de
Berging gevuld"), `uitz-twee-rollen` en `basis-twee-rollen` ("de sluitingsdatum van de strandvoogd van de kustwacht van
de Berging gevuld"), `basis-vouw-en-keten` (de laatste van de vouw en een keten van twee rollen), `r3-telt` (de
markering boven een tellende afwijking). e-*: de vouw twee rollen ver, een Kustwacht over de vondsten van al zijn
bergingen (laatste, som). f-*: `afgeleid-lid` (het lid schrijft een regel na de groep), `filter-afgeleid` (de
subselectie leest een later afgeleid kenmerk), `lezer` (een regel leest het doel van de groep en staat ervoor).
g-gewoon-max-van-twee: de gewone regel naast c-basis-max-van-twee. h-*: tegenvormen: een geneste uitzondering, een
ongeordend blok, een lid uit een latere groep, een subselectie die een initialisatie en daarna een gelijkstelling
vullen, een subselectie op leeg die een latere groep vult, een subtype (de instanties Strandberging), vijf vouwen in één
consequentie, een vouw over een veel-op-veelrelatie, en een deling in de subselectie (ook door een leeg gewicht, en de
gewone regel daarnaast). "SKIP verzamelzin" is `EXCLUDE transpiler SKIP: defaults: defaults A:<doel> — defaults
(<doel>): niet-reduceerbare consequentie (far-side/aggregatie): <default>`; "EXCLUDE defaultszin" is `EXCLUDE drl: ch.
7 prioritizedDefaults — the DRL-text branch has no defaults lowering`.

| vorm | ref | clingo | exec vóór | exec na | DRL-tekst |
|---|---|---|---|---|---|
| a-basis-aantal | oracle: PASS (5 expectations) | clingo: PASS (5 expectations) | SKIP verzamelzin (Berging.maatgewicht) | PLAYED 1 | EXCLUDE defaultszin |
| a-basis-eerste | oracle: PASS (5 expectations) | clingo: PASS (5 expectations) | SKIP verzamelzin (Berging.sluitdag) | PLAYED 1 | EXCLUDE defaultszin |
| a-basis-laatste | oracle: PASS (5 expectations) | clingo: PASS (5 expectations) | SKIP verzamelzin (Berging.sluitdag) | PLAYED 1 | EXCLUDE defaultszin |
| a-basis-maximum | oracle: PASS (5 expectations) | clingo: PASS (5 expectations) | SKIP verzamelzin (Berging.maatgewicht) | PLAYED 1 | EXCLUDE defaultszin |
| a-basis-minimum | oracle: PASS (5 expectations) | clingo: PASS (5 expectations) | SKIP verzamelzin (Berging.maatgewicht) | PLAYED 1 | EXCLUDE defaultszin |
| a-basis-som | oracle: PASS (5 expectations) | clingo: PASS (5 expectations) | SKIP verzamelzin (Berging.maatgewicht) | PLAYED 1 | EXCLUDE defaultszin |
| a-mark-aantal | oracle: PASS (6 expectations) | clingo: PASS (6 expectations) | SKIP verzamelzin (Berging.maatgewicht) | PLAYED 1 | EXCLUDE defaultszin |
| a-mark-eerste | oracle: PASS (6 expectations) | clingo: PASS (6 expectations) | SKIP verzamelzin (Berging.sluitdag) | PLAYED 1 | EXCLUDE defaultszin |
| a-mark-laatste | oracle: PASS (6 expectations) | clingo: PASS (6 expectations) | SKIP verzamelzin (Berging.sluitdag) | PLAYED 1 | EXCLUDE defaultszin |
| a-mark-maximum | oracle: PASS (6 expectations) | clingo: PASS (6 expectations) | SKIP verzamelzin (Berging.maatgewicht) | PLAYED 1 | EXCLUDE defaultszin |
| a-mark-minimum | oracle: PASS (6 expectations) | clingo: PASS (6 expectations) | SKIP verzamelzin (Berging.maatgewicht) | PLAYED 1 | EXCLUDE defaultszin |
| a-mark-som | oracle: PASS (6 expectations) | clingo: PASS (6 expectations) | SKIP verzamelzin (Berging.maatgewicht) | PLAYED 1 | EXCLUDE defaultszin |
| a-r3-aantal | oracle: PASS (6 expectations) | clingo: PASS (6 expectations) | SKIP verzamelzin (Berging.berekende maatgewicht) | PLAYED 1 | EXCLUDE defaultszin |
| a-r3-eerste | oracle: PASS (6 expectations) | clingo: PASS (6 expectations) | SKIP verzamelzin (Berging.berekende sluitdag) | PLAYED 1 | EXCLUDE defaultszin |
| a-r3-laatste | oracle: PASS (6 expectations) | clingo: PASS (6 expectations) | SKIP verzamelzin (Berging.berekende sluitdag) | PLAYED 1 | EXCLUDE defaultszin |
| a-r3-maximum | oracle: PASS (6 expectations) | clingo: PASS (6 expectations) | SKIP verzamelzin (Berging.berekende maatgewicht) | PLAYED 1 | EXCLUDE defaultszin |
| a-r3-minimum | oracle: PASS (6 expectations) | clingo: PASS (6 expectations) | SKIP verzamelzin (Berging.berekende maatgewicht) | PLAYED 1 | EXCLUDE defaultszin |
| a-r3-som | oracle: PASS (6 expectations) | clingo: PASS (6 expectations) | SKIP verzamelzin (Berging.berekende maatgewicht) | PLAYED 1 | EXCLUDE defaultszin |
| a-uitz-aantal | oracle: PASS (5 expectations) | clingo: PASS (5 expectations) | SKIP verzamelzin (Berging.maatgewicht) | PLAYED 1 | EXCLUDE defaultszin |
| a-uitz-eerste | oracle: PASS (5 expectations) | clingo: PASS (5 expectations) | SKIP verzamelzin (Berging.sluitdag) | PLAYED 1 | EXCLUDE defaultszin |
| a-uitz-laatste | oracle: PASS (5 expectations) | clingo: PASS (5 expectations) | SKIP verzamelzin (Berging.sluitdag) | PLAYED 1 | EXCLUDE defaultszin |
| a-uitz-maximum | oracle: PASS (5 expectations) | clingo: PASS (5 expectations) | SKIP verzamelzin (Berging.maatgewicht) | PLAYED 1 | EXCLUDE defaultszin |
| a-uitz-minimum | oracle: PASS (5 expectations) | clingo: PASS (5 expectations) | SKIP verzamelzin (Berging.maatgewicht) | PLAYED 1 | EXCLUDE defaultszin |
| a-uitz-som | oracle: PASS (5 expectations) | clingo: PASS (5 expectations) | SKIP verzamelzin (Berging.maatgewicht) | PLAYED 1 | EXCLUDE defaultszin |
| b-basis-min-duur | oracle: PASS (5 expectations) | clingo: PASS (5 expectations) | SKIP verzamelzin (Berging.sluitdag) | PLAYED 1 | EXCLUDE defaultszin |
| b-basis-plus-duur | oracle: PASS (5 expectations) | clingo: PASS (5 expectations) | SKIP verzamelzin (Berging.sluitdag) | PLAYED 1 | EXCLUDE defaultszin |
| b-basis-som-plus-1 | oracle: PASS (5 expectations) | clingo: PASS (5 expectations) | SKIP verzamelzin (Berging.maatgewicht) | PLAYED 1 | EXCLUDE defaultszin |
| b-mark-plus-duur | oracle: PASS (6 expectations) | clingo: PASS (6 expectations) | SKIP verzamelzin (Berging.sluitdag) | PLAYED 1 | EXCLUDE defaultszin |
| b-mark-som-plus-1 | oracle: PASS (6 expectations) | clingo: PASS (6 expectations) | SKIP verzamelzin (Berging.maatgewicht) | PLAYED 1 | EXCLUDE defaultszin |
| b-r3-plus-duur | oracle: PASS (6 expectations) | clingo: PASS (6 expectations) | SKIP verzamelzin (Berging.berekende sluitdag) | PLAYED 1 | EXCLUDE defaultszin |
| b-r3-som-plus-1 | oracle: PASS (6 expectations) | clingo: PASS (6 expectations) | SKIP verzamelzin (Berging.berekende maatgewicht) | PLAYED 1 | EXCLUDE defaultszin |
| b-uitz-plus-duur | oracle: PASS (5 expectations) | clingo: PASS (5 expectations) | SKIP verzamelzin (Berging.sluitdag) | PLAYED 1 | EXCLUDE defaultszin |
| b-uitz-som-plus-1 | oracle: PASS (5 expectations) | clingo: PASS (5 expectations) | SKIP verzamelzin (Berging.maatgewicht) | PLAYED 1 | EXCLUDE defaultszin |
| c-basis-eerste-van-twee | oracle: PASS (5 expectations) | clingo: PASS (5 expectations) | SKIP verzamelzin (Berging.sluitdag) | PLAYED 1 | EXCLUDE defaultszin |
| c-basis-laatste-van-twee | oracle: PASS (5 expectations) | clingo: PASS (5 expectations) | SKIP verzamelzin (Berging.sluitdag) | PLAYED 1 | EXCLUDE defaultszin |
| c-basis-max-van-twee | oracle: PASS (5 expectations) | TRANSLATION FAULT (expression kind not translated) | SKIP verzamelzin (Berging.maatgewicht) | PLAYED 1 | EXCLUDE defaultszin |
| c-basis-min-van-twee | oracle: PASS (5 expectations) | TRANSLATION FAULT (expression kind not translated) | SKIP verzamelzin (Berging.maatgewicht) | PLAYED 1 | EXCLUDE defaultszin |
| c-mark-laatste-van-twee | oracle: PASS (6 expectations) | clingo: PASS (6 expectations) | SKIP verzamelzin (Berging.sluitdag) | PLAYED 1 | EXCLUDE defaultszin |
| c-mark-max-van-twee | oracle: PASS (6 expectations) | TRANSLATION FAULT (expression kind not translated) | SKIP verzamelzin (Berging.maatgewicht) | PLAYED 1 | EXCLUDE defaultszin |
| c-r3-laatste-van-twee | oracle: PASS (6 expectations) | clingo: PASS (6 expectations) | SKIP verzamelzin (Berging.berekende sluitdag) | PLAYED 1 | EXCLUDE defaultszin |
| c-r3-max-van-twee | oracle: PASS (6 expectations) | TRANSLATION FAULT (expression kind not translated) | SKIP verzamelzin (Berging.berekende maatgewicht) | PLAYED 1 | EXCLUDE defaultszin |
| c-uitz-laatste-van-twee | oracle: PASS (5 expectations) | clingo: PASS (5 expectations) | SKIP verzamelzin (Berging.sluitdag) | PLAYED 1 | EXCLUDE defaultszin |
| c-uitz-max-van-twee | oracle: PASS (5 expectations) | TRANSLATION FAULT (expression kind not translated) | SKIP verzamelzin (Berging.maatgewicht) | PLAYED 1 | EXCLUDE defaultszin |
| d-basis-telt | oracle: PASS (5 expectations) | clingo: PASS (5 expectations) | SKIP verzamelzin (Berging.sluitdag) | PLAYED 1 | EXCLUDE defaultszin |
| d-basis-twee-rollen | oracle: PASS (5 expectations) | clingo: PASS (5 expectations) | SKIP verzamelzin (Berging.sluitdag) | PLAYED 1 | EXCLUDE defaultszin |
| d-basis-vouw-en-keten | oracle: PASS (5 expectations) | clingo: PASS (5 expectations) | SKIP verzamelzin (Berging.sluitdag) | PLAYED 1 | EXCLUDE defaultszin |
| d-r3-telt | oracle: PASS (6 expectations) | clingo: PASS (6 expectations) | SKIP verzamelzin (Berging.berekende sluitdag) | PLAYED 1 | EXCLUDE defaultszin |
| d-uitz-een-rol | oracle: PASS (5 expectations) | clingo: PASS (5 expectations) | SKIP verzamelzin (Berging.sluitdag) | PLAYED 1 | EXCLUDE defaultszin |
| d-uitz-som | oracle: PASS (5 expectations) | clingo: PASS (5 expectations) | SKIP verzamelzin (Berging.sluitdag) | PLAYED 1 | EXCLUDE defaultszin |
| d-uitz-telt | oracle: PASS (5 expectations) | clingo: PASS (5 expectations) | SKIP verzamelzin (Berging.sluitdag) | PLAYED 1 | EXCLUDE defaultszin |
| d-uitz-twee-rollen | oracle: PASS (5 expectations) | clingo: PASS (5 expectations) | SKIP verzamelzin (Berging.sluitdag) | PLAYED 1 | EXCLUDE defaultszin |
| e-twee-rollen-laatste | oracle: PASS (3 expectations) | clingo: PASS (3 expectations) | SKIP verzamelzin (Kustwacht.sluitdag) | PLAYED 1 | EXCLUDE defaultszin |
| e-twee-rollen-som | oracle: PASS (3 expectations) | clingo: PASS (3 expectations) | SKIP verzamelzin (Kustwacht.maatgewicht) | PLAYED 1 | EXCLUDE defaultszin |
| f-afgeleid-lid | oracle: PASS (5 expectations) | clingo: PASS (5 expectations) | SKIP verzamelzin (Berging.sluitdag) | PLAYED 1 | EXCLUDE defaultszin |
| f-filter-afgeleid | oracle: PASS (5 expectations) | clingo: PASS (5 expectations) | SKIP verzamelzin (Berging.sluitdag) | PLAYED 1 | EXCLUDE defaultszin |
| f-lezer | oracle: PASS (5 expectations) | clingo: PASS (5 expectations) | SKIP verzamelzin (Berging.sluitdag) | PLAYED 1 | EXCLUDE defaultszin |
| g-gewoon-max-van-twee | oracle: PASS (5 expectations) | TRANSLATION FAULT (expression kind not translated) | PLAYED 1 | PLAYED 1 | PLAYED 1 |
| h-filter-deling-door-leeg-regel | oracle: PASS (5 expectations) | UNSATISFIABLE (non-exact division, V2 en V4) | PLAYED 1 | PLAYED 1 | MARKER TODO aggregatie LAATSTE |
| h-filter-deling-door-leeg | oracle: PASS (5 expectations) | UNSATISFIABLE (non-exact division, V2 en V4) | SKIP verzamelzin (Berging.sluitdag) | PLAYED 1 | EXCLUDE defaultszin |
| h-filter-deling | oracle: PASS (5 expectations) | UNSATISFIABLE (non-exact division, V2 en V4) | SKIP verzamelzin (Berging.sluitdag) | PLAYED 1 | EXCLUDE defaultszin |
| h-geneste-uitzondering | oracle: PASS (5 expectations) | clingo: PASS (5 expectations) | SKIP verzamelzin (Berging.sluitdag) | PLAYED 1 | EXCLUDE defaultszin |
| h-lid-uit-groep | oracle: PASS (5 expectations) | clingo: PASS (5 expectations) | SKIP verzamelzin (Berging.sluitdag) | PLAYED 1 | EXCLUDE defaultszin |
| h-ongeordend | oracle: PASS (5 expectations) | clingo: PASS (5 expectations) | SKIP verzamelzin (Berging.sluitdag) | PLAYED 1 | EXCLUDE defaultszin |
| h-subselectie-init-dan-berekening | oracle: PASS (5 expectations) | clingo: PASS (5 expectations) | SKIP verzamelzin (Berging.sluitdag) | PLAYED 1 | EXCLUDE defaultszin |
| h-subselectie-leeg-groep | oracle: PASS (5 expectations) | clingo: PASS (5 expectations) | SKIP verzamelzin (Berging.sluitdag) | PLAYED 1 | EXCLUDE defaultszin |
| h-subtype | oracle: PASS (5 expectations) | clingo: PASS (5 expectations) | SKIP verzamelzin (Berging.sluitdag) | PLAYED 1 | EXCLUDE defaultszin |
| h-veel-op-veel | oracle: PASS (2 expectations) | clingo: PASS (2 expectations) | SKIP verzamelzin (Berging.sluitdag) | SKIP consequentie niet vertaald (zin 1) | EXCLUDE defaultszin |
| h-vijf-vouwen | oracle: PASS (5 expectations) | clingo: PASS (5 expectations) | SKIP verzamelzin (Berging.sluitdag) | SKIP te veel gebonden variabelen (zin 2) | EXCLUDE defaultszin |
| r1-kaart | oracle: PASS (1+1 expectations) | clingo: PASS (1+1 expectations) | SKIP verzamelzin (Berging.sluitdag) | PLAYED 2 | EXCLUDE defaultszin |
| r2-gewoon | oracle: PASS (1 expectations) | clingo: PASS (1 expectations) | PLAYED 1 | PLAYED 1 | MARKER TODO aggregatie LAATSTE |
| r3-boven-afwijking | oracle: PASS (1+1+1 expectations) | clingo: PASS (1+1+1 expectations) | SKIP verzamelzin (Berging.berekende sluitdag) | PLAYED 3 | EXCLUDE defaultszin |
| r3-letterlijk | oracle: PASS (1+1 expectations) | clingo: PASS (1+1 expectations) | PLAYED 2 | PLAYED 2 | EXCLUDE defaultszin |

Zin 1 (h-veel-op-veel, na): `EXCLUDE transpiler SKIP: defaults: defaults A:Berging.sluitdag — defaults
(Berging.sluitdag): consequentie niet vertaald, de aggregatie-join heeft geen beanveld — de rol 'kennende berging' op
Melding is meervoudig, dus haar opslag is het veld aan de overzijde, en ook daar leidt het beanmodel er geen af (beide
zijden meervoudig) (§13.3.9 opslagrichting): de laatste van de meldingsdatum van alle meldingen van de Berging`.
Zin 2 (h-vijf-vouwen, na): `EXCLUDE transpiler SKIP: defaults: defaults A:Berging.sluitdag — defaults
(Berging.sluitdag): te veel gebonden variabelen (aggregaties, vouwen en globals)`. De clingo-cellen voluit:
`TRANSLATION FAULT: expression kind not translated (no corpus case exercises it)` en `clingo: UNSATISFIABLE — a
fail-fast guard fired: viol("non-exact division (an explicit rounding is mandated before precision is lost)","V2");
viol("non-exact division (an explicit rounding is mandated before precision is lost)","V4")`.

Samengevat over de 72 documenten (76 cases): ref `oracle: PASS` op alle 76 cases; clingo `clingo: PASS` op 67, een
`TRANSLATION FAULT` op de zes documenten met het minimum of maximum van twee getalvouwen (ook in de gewone regel,
g-gewoon-max-van-twee) en `UNSATISFIABLE` op de drie flanken met een niet-exacte deling in de subselectie (V2 en V4: 7/2
en 5/2, of 12/7 en 12/5). Exectak vóór: 4 PLAYED (r2-gewoon, r3-letterlijk, g-gewoon-max-van-twee en
h-filter-deling-door-leeg-regel), 68 SKIP verzamelzin; na: 70 PLAYED, 2 met naam geweigerd (zin 1 en 2). Elke PLAYED
is met de verwachtingen die ref bevestigt, ook waar clingo faalt.
DRL-tak vóór en na gelijk: elk document met een defaultsgroep `EXCLUDE defaultszin`; R2 en de gewone regel met een
deling `MARKER TODO aggregatie LAATSTE`; g-gewoon-max-van-twee `PLAYED 1`.

### (b) Getuige

`DefaultsConsequentieVouwWitnessTest` (exectak, veertien rijen, documenten in de klasse op Jutterswet-materiaal, met de
waarden van ref en clingo; de rij over twee getalvouwen alleen met die van ref):
`r1_de_basis_van_een_afwijking_vouwt` (R1 met de twee cases van de kaart, en de DRL-tak weigert met de defaultszin),
`r3_de_opgave_boven_een_afwijking_waarvan_de_basis_vouwt` en `r3_met_twee_vouwen_als_operanden` (R3 met één en met
twee vouwen, zes bergingen, met een opgave), `de_gegenereerde_groep_initialiseert_met_een_vouw`,
`een_uitzondering_vouwt_onder_een_rechtvaardiging_die_telt`, `de_getalvouwen_in_de_consequentie` (som, minimum,
maximum, aantal), `een_vouw_in_een_berekening` (plus 14 dagen, plus 1 over een lege som),
`twee_getalvouwen_als_operanden`, `de_vouw_twee_rollen_ver`, `een_rechtvaardiging_twee_rollen_ver_boven_een_vouw`,
`de_vouw_wacht_op_de_groep_die_haar_lid_schrijft`, `de_subselectie_leest_een_later_afgeleide_datum` (een initialisatie
en dan een gelijkstelling; een groep, getoetst op leeg), en twee rijen die een benoemde weigering pinnen:
`een_vouw_zonder_beanveld_weigert_met_de_vorm` en `vijf_vouwen_in_een_consequentie_binden_te_veel`.

Rood op de claim (`6d624af5` tegen de transpiler van `2a9ba859`, dertien rijen):

    Tests run: 13, Failures: 2, Errors: 11, Skipped: 0
    elf rijen » Excluded transpiler SKIP: defaults: defaults A:<doel> — defaults (<doel>): niet-reduceerbare consequentie (far-side/aggregatie): <default>
      (<doel> Berging.sluitdag, Berging.berekende sluitdag, Berging.maatgewicht of Kustwacht.sluitdag)
    een_vouw_zonder_beanveld_weigert_met_de_vorm:250 transpiler SKIP: defaults: defaults A:Berging.sluitdag — defaults (Berging.sluitdag): niet-reduceerbare consequentie (far-side/aggregatie): <default> ==> expected: <true> but was: <false>
    vijf_vouwen_in_een_consequentie_binden_te_veel:268 transpiler SKIP: defaults: defaults A:Berging.sluitdag — defaults (Berging.sluitdag): niet-reduceerbare consequentie (far-side/aggregatie): <default> ==> expected: <true> but was: <false>

De veertiende rij (`90f5370e`) rood tegen dezelfde transpiler (`Tests run: 1, Failures: 0, Errors: 1`):
`twee_getalvouwen_als_operanden:160->speel:545->speel:557 » Excluded transpiler SKIP: defaults: defaults
A:Berging.maatgewicht — defaults (Berging.maatgewicht): niet-reduceerbare consequentie (far-side/aggregatie):
<default>`. Op de kop: `Tests run: 14, Failures: 0, Errors: 0, Skipped: 0`; de suite na de fix 950 tests, 0
failures, 0 skipped (937 op de claim), op de kop 951 (gate 12: `JUnit 951 tests, floor 239`).

Mutatieproef (een proefjar buiten de tak): zonder de lezingen van de consequentie in `verzamelLezingen` faalt precies
één rij, `de_subselectie_leest_een_later_afgeleide_datum:227 een groep, getoetst op leeg ==> expected: <null> but was:
<case: attribuut sluitdag: verwacht 2026-03-12, werkelijk 2026-03-05>`: de groep besliste op de voorlopige stand
waarin elke vondst nog een lege uiterste bergdag had, en een toegekende waarde gaat bij een lege herziening niet weg.

### (c) Golden `jutterswet-sluitdag-gemeld` (`93359b14`)

Uit `bron.rgs` en `scenario.rgs`, handgerekend in NOTE.md: de sluitdag van een Berging (de laatste bergdag, onder de
gemelde sluitdag), de uiterste meldingsdag met de opgave-markering (de laatste meldingsdag plus 14 dagen, onder een
afwijking die telt en de sluitdag van een andere groep leest; de markering verlegt de boom naar `berekende uiterste
meldingsdag`), de afrekendag met de opgave-markering (de laatste van twee vouwen als initialisatie in de gegenereerde
groep, onder `berekende afrekendag`), het vrachtgewicht (een som in een uitzondering) en de sluitdag van een Kustwacht
(de laatste bergdag twee rollen ver). Geen getalvouw als operand van een gelijste vouw: de ASP-backend vertaalt die
niet. De IR uit `--ir bron.rgs` alleen is byte-gelijk aan die met het scenario.

| case | verwachtingen | ref | clingo | exec vóór | exec na | DRL-tekst | no-op |
|---|---|---|---|---|---|---|---|
| case1-drie-bergingen | 20 | `oracle: PASS (20 expectations)` | `clingo: PASS (20 expectations)` | EXCLUDE (golden) | PASS | EXCLUDE (golden) | 13 MISMATCH |
| case2-opgave-en-een-lege-bijdrager | 12 | `oracle: PASS (12 expectations)` | `clingo: PASS (12 expectations)` | idem | PASS | idem | 8 MISMATCH |

Per golden: exectak na `PLAYED 2`; exectak vóór `EXCLUDE transpiler SKIP: defaults: defaults A:Kustwacht.sluitdag —
defaults (Kustwacht.sluitdag): niet-reduceerbare consequentie (far-side/aggregatie): <default>`; DRL-tak `EXCLUDE drl:
ch. 7 prioritizedDefaults — the DRL-text branch has no defaults lowering`. Toegelaten via `scripts/admit.sh` met het
gate-12-voorstel in de werkboom: `admit: ADMITTED corpus/golden/jutterswet-sluitdag-gemeld (2 case(s); verifier-clean,
ref = clingo, no-op refuted, gates green)`.

### Gate-12-voorstel (`c22a709d`, `scripts/check-drools.sh`, beschermd pad)

- `EXPECTED_PLAYED`: 230 → 231, redenblok "230 -> 231 (2026-10-01, the consequence of a default that folds over a
  role)" na dat van jutterswet-zwaarste-vondst.
- `DRL_CHANNEL` klasse (A): `"jutterswet-sluitdag-gemeld|EXCLUDE|ch. 7 prioritizedDefaults: no defaults lowering in
  this branch"` op naamvolgorde (na jutterswet-schorsing); kop "38 lines" → "39 lines", "sitting 39 below" → "sitting
  40 below", "37 of the 38" → "38 of the 39", "the 38th" → "the 39th", "All 37" → "All 38", "The 39 is 43 exec-only
  players — those 37" → "The 40 is 44 exec-only players — those 38", en een hertelzin "(231 - 191); the six class heads
  (A 39, B 37, C 11, D 2, E 3, F 3 = 95 lines)"; lijstkop "eighty-eight of the ninety-four" → "eighty-nine of the
  ninety-five".
- `EXPECTED_DRL_PLAYED` blijft 191, `EXPECTED_MIN_TESTS` blijft 239, `EXPECTED_CHANNEL` en `EXCLUDE_KNOWN` ongewijzigd.

### (d) Sweep

Over alle 285 goldens van de claim, elke golden met al zijn cases, statusregel per golden en per tak, jar van de claim
(`2a9ba859`) tegen jar van de kop (`90f5370e`, de code van `8f60f208`): alle 570 regels gelijk (exectak PLAYED 230,
DRL-tak PLAYED 191). Geen bestaande golden draagt een vouw in een consequentie. De nieuwe golden: exectak vóór EXCLUDE,
na `PLAYED 2`; DRL-tak EXCLUDE. De beangeneratie is niet aangeraakt.

### README (`9004ada7`)

`backends/drools/README.md`, de sectie "Recorded: a defaults justification that counts or reads further than one
role, and a consequence that folds" (kop verlengd): een vouw in een consequentie wordt een accumulate op de
reduce-regel en de consequentie leest haar uit een slot, in de basis en de uitzonderingen van een boom en in de
gegenereerde groep van de opgave-markering; golden en getuige erbij; wat niet vertaalt weigert met de vorm en de
consequentie. De oude zin "an aggregation in a consequence keeps the consequence's own refusal" is weg.

### Gates

Alle zestien in de volgorde van `gates.yml` met `/tmp/v2/uchar-gates12/run.sh` op de kop `a6aa37b5` (de laatste
codecommit; de commit met dit Result raakt alleen de kaart), met een schone werkboom: `eval $(opam env)`, `dune build`
(exit 0), dan elk script onder zijn shebang (bash voor catala, regelspraak, toka-parity, drools, flogic, graaf en
scenario; sh voor de negen andere). Catala aanwezig. Logs in `/tmp/v2/uchar-gates12/`. Per gate de exitcode en de
PASS-regel:

     1. check-self-contained (sh): exit 0 — check-self-contained: PASS
     2. check-ir-schema (sh): exit 0 — check-ir-schema: PASS (schema valid; 79 pattern self-test row(s); 504 shape self-test row(s); 325 file(s) validate and round-trip, floor 192; 42948 object(s) in 826 document(s) in the schema's key order, 1 in no subschema)
     3. check-case-schema (sh): exit 0 — check-case-schema: PASS (schema valid; 92 pattern self-test row(s); 817 case file(s) validate)
     4. check-verifier (sh): exit 0 — check-verifier: PASS (325 golden clean; 501 negatief rejected for their declared rule, floor 350; 344 on their declared arm (expect.txt), floor 260; 605 cited pointers resolve in their documents, floor 547)
     5. check-oracle (sh): exit 0 — check-oracle: PASS (ref suite 1152 worked examples, floor 489; 777 cases replayed exactly, floor 427; NOT replayed, no case file yet: corpus/golden/jutterswet-seizoenvondst corpus/golden/jutterswet-seizoenvouw corpus/golden/jutterswet-vormenkabinet)
     6. check-clingo (sh): exit 0 — check-clingo: PASS (backend suite 1988 checks, floor 83; 777 cases replay exactly through clingo — ref = clingo over the corpus)
     7. check-noop (sh): exit 0 — check-noop: PASS (733 cases refute a do-nothing executor; 44 pinned survivors pin defeasible standing)
     8. check-rood (sh): exit 0 — check-rood: PASS (40 red cases fault observably in ref and clingo, floor 26; 80 pinned fragments cite none of the 36 listed bottom sentences)
     9. check-catala (bash): exit 0 — check-catala: PASS — frontend suite green (241 checks, floor 201); 12 catala golden(s) regenerate byte-identically (floor 12)
    10. check-regelspraak (bash): exit 0 — check-regelspraak: PASS — JUnit suite green (1008 tests, floor 173); 113 rgs golden(s) re-emit byte-identically (floor 80)
    11. check-toka-parity (bash): exit 0 — check-toka-parity: PASS
    12. check-drools (bash): exit 0 — check-drools: PASS
    13. check-reftype-derivation (sh): exit 0 — check-reftype-derivation: PASS (41 unit annotations twisted over 23 goldens, floor 38; 106 executor replays reproduce the untouched outcome, 22 of them PLAYED by the DRL-text branch, floor 20; 0 pinned survivors)
    14. check-flogic (bash): exit 0 — check-flogic: PASS — frontend suite green (150 checks, floor 81); 2 flogic golden(s) re-emit byte-identically (floor 1)
    15. check-graaf (bash): exit 0 — check-graaf: PASS
    16. check-scenario (bash): exit 0 — check-scenario: PASS — 20 scenario golden(s) (floor 7) regenerate 76 case file(s) (floor 31) and their ir.json byte-identically

Gate 12 verder, regels uit zijn samenvatting:

    played 231  (loaded off the wire, wholly lowered, matched the pinned ref≡clingo expectations)
    channel floor 55 non-PLAYED goldens pinned by name; played matches the pinned 231
    DRL-text branch played 191  (its OWN count — the narrower branch, not the square; pin 191)
    DRL-text floor 95 non-PLAYED goldens pinned by name; sweep cost 138s wall clock
    JUnit 951 tests, floor 239  (the backend's own suite, green with a non-zero count)

Gate 15 ook `check-graaf: route leg — the graph of the wire
equals the graph of the source on all 113 goldens with a bron.rgs (floor 98); 23 unit tests green (floor 23)`. Gate 11
ook `re-emission: 1 casus dump(s), 45 .rgs re-emitted byte-identical, floor 45 (…); TOKA suite 27 tests, floor 27`.

Eerder groen: op de claim `2a9ba859` (alle zestien; gate 12 played 230, DRL-text 191, JUnit 937) en in de toelating
van de golden (`scripts/admit.sh`, alle gates met de golden en het gate-12-voorstel in de werkboom). Een run op
`9004ada7` heb ik na gate 5 (alle vijf PASS) gestopt om het commentaar van één getuigenrij te herstellen (`a6aa37b5`);
de run hierboven is die daarna.

### Afwijkingen van de kaart

1. R3 letterlijk gelezen (de markering in plaats van de afwijking) weigerde niet: `PLAYED 2` al vóór de fix, omdat de
   frontend een gewone regel als gewone regel naar `berekende sluitdag` verlegt. De weigering "op `berekende
   sluitdag`" die de kaart noemt, ontstaat met de markering bóven een afwijkingsboom waarvan de basis vouwt; die vorm
   is R3 in de getuige (met één en met twee vouwen) en in de golden. De meting van de regelsetbouwer
   (`/tmp/v2/probe/skelet-2/`) viel buiten wat deze sessie mocht lezen.
2. De weigerzin van een consequentie die niet vertaalt is voor elke oorzaak veranderd, niet alleen voor een vouw: van
   `niet-reduceerbare consequentie (far-side/aggregatie): <default>` naar `consequentie niet vertaald, <reden van de
   vouw of de navigatie, anders "een waarde die deze tak niet leest">: <consequentie>`; de zin over te veel gebonden
   variabelen noemt nu "(aggregaties, vouwen en globals)". Geen gate of getuige pinde de oude zin; de NOTE.md van
   `jutterswet-wrakinitialisatie` citeert hem als meting van vroeger en blijft zo.
3. De vorm "twee vouwen als operanden van één minimum of maximum" staat in de golden alleen met datums (de laatste van
   twee vouwen): het minimum of maximum van twee getalvouwen vertaalt de ASP-backend niet (`TRANSLATION FAULT:
   expression kind not translated (no corpus case exercises it)`, ook in een gewone regel), dus die vorm kan geen
   golden zijn; zij is een getuigenrij met de waarden van ref.
4. De getuige heeft meer rijen dan R1 en R3: de posities, soorten en flanken van (a) die de lowering raken, en twee
   rijen die een benoemde weigering pinnen.
5. Geen bestaand testbestand en geen bestaande golden gewijzigd.

### Voorgestelde registertekst (voor de hoofdsessie)

- `spec/proposals/PRIORITIZED-DEFAULTS-EVOLUTIE.md`, na de alinea "Landed 2026-09-30 (exec-model branch): a
  justification that counts, …" (§5.1): "Landed 2026-10-01 (exec-model branch): a consequence that folds over a set
  source — the earliest or the latest date, a count, a sum, a minimum or a maximum, on its own, inside a computation or
  as an operand of a listed fold — lowers as a rule's value does: an accumulate on the reduce-rule whose result the
  consequence reads, in the base and the exceptions of a tree and in the group the opgave marker generates. A
  consequence that does not lower is refused with the form and the consequence. Witnesses:
  `DefaultsConsequentieVouwWitnessTest`, the golden `jutterswet-sluitdag-gemeld`."
- `spec/CONSTRUCTS.md`: geen rij; de spec schrijft de vouw in een consequentie al voor (RUNTIME-SEMANTICS ch. 7.1,
  ch. 1.2e), de exectak voldoet er nu aan.

### Open punten

1. De ASP-backend vertaalt het minimum of maximum van twee getalvouwen niet (`TRANSLATION FAULT: expression kind not
   translated (no corpus case exercises it)`), in een gewone regel en in een consequentie; ref antwoordt, de exectak
   speelt met de waarden van ref. Buiten deze kaart (`backends/clingo/`).
2. Een niet-exacte deling in de subselectie van een vouw (7/2, 12/7): ref antwoordt, de ASP-backend geeft
   `UNSATISFIABLE — a fail-fast guard fired: viol("non-exact division (an explicit rounding is mandated before
   precision is lost)",…)`, in een gewone regel en in een consequentie. Gemeten als flank, niet uitgezocht; ref en
   clingo verschillen daar.
3. Een groep waarvan de vouwen, aggregaties en globals meer dan negen variabelen binden (het onderwerp meegeteld),
   weigert met naam (h-vijf-vouwen); `consequenceOf` bindt ten hoogste negen variabelen (`Block10`).
4. Een vouw over een relatie waarvan beide rollen meervoudig zijn, weigert met naam (§13.3.9), zoals in de waarde van
   een regel.
5. Niet gemeten: een schrijver buiten de regels en de groepen (een verdeling, een creatie) van wat een vouw in een
   consequentie leest; `groepVolgorde` ziet zo'n schrijver niet (open punt 5 van drools-exec-defaults-justificatie-vouw).
6. De DRL-teksttak heeft geen defaults-lowering (ongewijzigd).
7. De distributie-CLI draait op een oudere pin en kent deze wijziging niet; de pin moet na het landen mee. Daarna kan
   het laatste K1-lid van de regelset weg (NB van de kaart).

### Commits

    2a9ba859 claim: drools-exec-defaults-consequentie-vouw
    6d624af5 DefaultsConsequentieVouwWitnessTest: vouw in de consequentie, rood
    8f60f208 Exectak: een vouw in de consequentie van een default wordt een accumulate
    90f5370e DefaultsConsequentieVouwWitnessTest: twee getalvouwen als operanden
    93359b14 golden jutterswet-sluitdag-gemeld: defaults met een vouw als consequentie
    c22a709d voorstel gate 12: jutterswet-sluitdag-gemeld speelt in de exectak
    9004ada7 backends/drools/README: een consequentie die vouwt
    a6aa37b5 DefaultsConsequentieVouwWitnessTest: V3 gaat ook door de subselectie

en de commit die de kaart met dit Result naar `backlog/done/` verplaatst. Niet gepusht, niet gemerged.

## Keuring (hoofdsessie, 2026-10-01)

Eigen run van de zestien gates op `55184584` in de volgorde van `gates.yml`
(bash waar de shebang dat zegt), `dune build` vooraf; logs in
`/tmp/v2/uchar-gates12/`. Alle zestien exit 0: gate 10 JUnit 1008, 113
rgs-goldens; gate 12 `check-drools: PASS`, played 231 (pin 231), DRL 191
(pin 191), JUnit 951; gate 16 20 scenario-goldens, 76 casebestanden. De fix
gelezen: `compileDefault` verlaagt de consequentie met de vouwslots aan,
per groep in `RechtvaardigingBouw.vouwslots`; hun accumulates staan op de
reduce-regel na die van de rechtvaardigingen en de slots zijn gevuld vóór
`DefaultsRun` beslist; `groepVolgorde` las de consequentie al, dus de
volgorde blijft (mutatieproef gemeten). Afwijking 1 aanvaard: de markering
boven een afwijkingsboom waarvan de basis vouwt is de vorm die de
weigering gaf, en die staat in de getuige en de golden. Afwijking 2
aanvaard: één weigerzin met vorm en consequentie voor elke oorzaak; geen
gate of getuige pinde de oude zin. De voorgestelde registertekst is in dit
landingscommit gezet in `spec/proposals/PRIORITIZED-DEFAULTS-EVOLUTIE.md`
§5.1, na de alinea van de rechtvaardiging. Twee commitkoppen van 73
tekens aanvaard (ff-only, geen rebase). Open punten 1–7 gelezen: geen
kaart. 1 en 2 zijn grenzen van de ASP-vertaling (het minimum of maximum
van twee getalvouwen; een niet-exacte deling in een subselectie, waar ref
antwoordt en clingo een guard laat vuren) en staan hier als aantekening; 3
(negen variabelen) en 4 (§13.3.9) zijn benoemde weigeringen; 5 blijft open
punt 5 van de justificatie-kaart; 6 is het kanaal van de DRL-teksttak; 7
is de pin die op deze landing volgt.

## Vervolgronde graaf en defaults (2026-10-01, bouwer)

Tak `vervolg/graaf-en-defaults` in de worktree `wt/vervolg-graaf-en-defaults`, afgetakt van uchar main `b9d3a5e6`; niet
gerebased, niet gepusht, niet gemerged. Gemeten met de eigen uitvoerders van deze repo: `_build/default/ref/bin/runner.exe`,
`_build/default/backends/clingo/bin/clingo_runner.exe`, `_build/default/spec/verifier/bin/verifier.exe`, de frontend
(`Resolver --ir … --cases … bron.rgs scenario.rgs`) en de Drools-jar (`java -jar <jar> ir.json case…`, `--drl` voor de
teksttak). "Vóór" is de jar en de frontend van de claim `afe0c361` (code gelijk aan main), "na" die van de kop
`bdedf518`. Elke gemeten vorm is een eigen Jutterswet-document (bron.rgs + scenario.rgs), door een generator geschreven
en door een meetscript gemeten; generatoren, meetscripts, tabellen en sweep staan buiten de repo onder
`/tmp/v2/probe/gd-bouw/` (`maak_c.py`, `maak_d.py`, `maak_d_flank.py`, `meet.sh`, `alles.sh`, `tabel.py`, `sweep.sh`,
`graaftest.sh`; per vorm `vorm/<naam>/`; metingen `meting-*.txt`, sweep `sweep-voor.txt`/`sweep-na.txt`). De reproducties
van de regelsetbouwer zijn alleen gelezen en daarvoor gekopieerd naar `/tmp/v2/probe/gd-bouw/repro/`; niets daarvan staat
in uchar.

### (c) Meer dan negen gebonden variabelen

**Reproductie op deze kop.** Eigen vorm `c-erflijn` (Jutterswet): `Regel meldtermijn` op Jutter (vinddag plus de
meldduur, indien vinddag gevuld en het aantal wrakhout-vondsten groter dan 0), op Strandjutter `Afwijking van
meldtermijn: meldtermijn na de laatste berging` (de laatste bergdag van de lading-vondsten plus de meldduur, onder een
telling), daaronder `Uitzondering op …: zware lading` (de eerste bergdag plus 1 mnd, indien de som van het gewicht
groter is dan de gewichtsdrempel) en `Afwijking van meldtermijn: meldtermijn tot het vergunningseinde` (onder een
telling). De stapel van een Strandjutter bindt dertien variabelen: het onderwerp, vier tellingen, een som met haar
telling, twee datumvouwen met hun telling en twee parameters. Vóór: `EXCLUDE transpiler SKIP: defaults: defaults
A:Strandjutter.meldtermijn — defaults (Jutter.meldtermijn): te veel gebonden variabelen (aggregaties, vouwen en
globals)`; na: `PLAYED 1`; ref `oracle: PASS (7 expectations)`, clingo `clingo: PASS (7 expectations)`. De reproducties
`a2e-volledig-variabele` en `a2-k1-erflijn` (alleen gedraaid): vóór dezelfde zin, na `PLAYED 14`; ref geeft op 8 van de 14
scenario's `EVALUATION FAULT: function kind not implemented in this card` en clingo op alle 14 `TRANSLATION FAULT: a
Clamp minimum bound that is not a ground literal could be empty; …` (zie open punt 1).

**Oorzaak.** `ExecModelTranspiler.consequenceOf` schreef de getypeerde blokken van de DSL uit tot `Block10` (negen
variabelen) en gaf daarboven `null`. Zeven plekken weigerden daarop: de defaultsgroep (`te veel gebonden variabelen
(aggregaties, vouwen en globals)`), de waarde van een regel (`regel (THEN-waarde: te veel gebonden globals en vouwen)`),
het tijdlijntotaal, de objectcreatie (`objectcreatie (te veel gebonden variabelen: globals en vouwen)`), de begeleider en
de sonde (een IllegalStateException), en een uniciteit over meer dan acht bronnen, die daarom haar tellingen paarsgewijs
optelde via `DSL.from`. De DSL zelf heeft getypeerde blokken tot 24 variabelen, en `ConsequenceImpl` draagt een `BlockN`
van elke ariteit, die de engine aanroept met de Drools-handle en de gebonden feiten.

**Fix** (`d41ffee6`). `consequenceOf` houdt de getypeerde blokken tot negen en bindt daarboven via `GebondenN`, een
subklasse van `ConsequenceBuilder.AbstractValidBuilder` die hetzelfde `BlockN` zet als elke getypeerde `execute`
(`usingDrools`, het blok over `Drools` en de feiten). Er is geen plafond meer, dus de zeven weigeringen zijn weg, en de
paarsgewijze telling van de uniciteit ook (die bestond alleen voor die grens; een flank-rij pint negen bronnen, groen
vóór en na). Verworpen: de getypeerde blokken tot 24 uitschrijven (een plafond blijft), en de slot-uitkomsten via een
keten van `from`-declaraties bundelen (meer knopen, zelfde resultaat).

**Vormruimte, vóór en na** (`meting-c-na.txt`; ref en clingo PASS op alle negen):

| vorm | gebonden | vóór | na |
|---|---|---|---|
| c-erflijn (de vorm hierboven) | 13 | SKIP te veel gebonden variabelen | PLAYED 1 |
| c-groep-k4 (groep, vier datumvouwen als gelijste vouw) | 9 | PLAYED 1 | PLAYED 1 |
| c-groep-k4-param (idem, plus de meldduur) | 10 | SKIP te veel gebonden variabelen | PLAYED 1 |
| c-groep-k5 / c-groep-k5-param | 11 / 12 | SKIP te veel gebonden variabelen | PLAYED 1 |
| c-regel-k4 (gewone regel) | 9 | PLAYED 1 | PLAYED 1 |
| c-regel-k4-param, c-regel-k5, c-regel-k5-param | 10–12 | SKIP regel (THEN-waarde: te veel gebonden globals en vouwen) | PLAYED 1 |

DRL-teksttak vóór en na gelijk: `EXCLUDE drl: ch. 7 prioritizedDefaults …` voor de groepen, `MARKER TODO aggregatie
LAATSTE` / `TODO datum-rekenkunde` voor de regels.

**Getuige.** `BindingVoorbijNegenWitnessTest` (nieuw, `d3902c82` rood): `een_afwijkingsboom_op_de_erflijn_bindt_dertien_variabelen`,
`vijf_vouwen_in_de_waarde_van_een_regel` en de flank `een_uniciteit_over_negen_bronnen`. Rood op de claim: `Tests run:
2, Failures: 0, Errors: 2` (`… te veel gebonden variabelen (aggregaties, vouwen en globals)` en `… regel (THEN-waarde:
te veel gebonden globals en vouwen): meldtermijn`). Twee bestaande rijen die de weigering pinden verwachten nu de waarden
van ref en clingo: `DefaultsConsequentieVouwWitnessTest.vijf_vouwen_in_een_consequentie` (was
`…_binden_te_veel`) en `ObjectcreatieVouwWitnessTest.vijf_vouwen` (was `vijf_vouwen_binden_te_veel`), rood op de claim
met de zin van de groep resp. `objectcreatie (te veel gebonden variabelen: globals en vouwen)`. Elk document is
verifier-schoon en ref ≡ clingo met de verwachtingen van de rij (gemeten op een dump van de documenten).

### (d) Een datumvouw in een voorwaarde en een rechtvaardiging

**Reproductie op deze kop.** Eigen vormen (`maak_d.py`), bijvoorbeeld `d-p1-regel-kenmerk-c01-laatste-later` ("Een
Jutter heeft de late melding indien de laatste van de bergdag van alle vondsten van de Jutter later is dan zijn
meldtermijn"): vóór `EXCLUDE transpiler SKIP: regel: late melding — regel (voorwaarde met een ordenende vouw over een
datumbron (eerste, laatste, minimum of maximum over een rol), die deze tak alleen als waarde vouwt (van een regel, de
consequentie van een default of de toekenning van een creatie)): late melding`, na `PLAYED 1`; en
`d-p3-dflt-uitzondering-c03-plus-dagen` ("Uitzondering op meldplicht … geen meldplicht indien (de laatste van …) plus 14
dg later is dan zijn meldtermijn"): vóór `… defaults (Jutter.meldplicht): rechtvaardiging niet vertaald, een operand die
deze tak niet als waarde leest: …`, na `PLAYED 1`. De reproducties `a4-n2-rechtvaardiging` en `a4b-zonder-rekensom`:
vóór de zin van de rechtvaardiging, na `PLAYED 2`, ref en clingo `PASS (1 expectations)` op beide cases;
`a6-datumvouw-gewone-regel` (alleen een bron): vóór de zin van de voorwaarde, na lowert hij (`PLAYED 1` op een lege case).

**Oorzaak.** (1) EERSTE en LAATSTE zijn geen aggregatiesoort (`isAggregatieKind`), dus zo'n operand kwam in
`scalarWaarde`, en daar geeft een vouw alleen een waarde als er slots open staan (`vouwslots`), wat alleen in de waarde van
een regel, de consequentie van een default en de toekenning van een creatie zo was. (2) Het minimum en maximum over een
datum zijn wel een aggregatiesoort en gingen naar de arm die de uitkomst als getal vergelijkt (`aggregatieVoorwaarde`,
`aggregatieRechtvaardiging`); `aggregatie(…, waarde = false)` weigerde ze daar. (3) De voorwaarde van een regel is
constraints op patronen; een vouw zonder accumulate kon daar niet staan.

**Fix** (`0ad715c4`).
- Voorwaarde van een regel of creatie: een vergelijking van twee datums of een leeg/gevuld-toets van een datum met een
  operand die een datum vouwt (`isDatumvouwBlad`), en een samengestelde voorwaarde met zo'n lid (`bevatDatumvouwBlad`),
  gaat naar `datumvouwVoorwaarde`, vóór de aggregatie-arm. Elke vouw wordt een accumulate met haar telling, zoals in de
  waarde (`vouwslot`); de operanden zijn `scalarWaarde` met de slots open; `slotToets` maakt er één test van over
  onderwerp en naam-waardekaart (`datumVergelijkTemporal` met de leegregels van ch. 1.2, een samengestelde voorwaarde met
  `vastgesteld`/`telVerdict` onder elke kwantor, een onderwerp-lokaal lid via `subjectConditie`). `toetsNa` zet die test
  na de accumulates: een keten van `from`-declaraties draagt de waarden in één houder (`Waarden`, geen Iterable, nooit
  null, omdat een `from` elementen van een lijst of array als eigen rijen geeft) naar de test, één variabele per
  declaratie, zodat geen constraint twee globals leest (`nGlobalGate`) en er geen plafond is. Een numerieke parameter, de
  rekendatum en een datumparameter gaan mee.
- Rechtvaardiging: `rechtvaardiging` stuurt een vergelijking met een datumvouw naar `scalaireRechtvaardiging`, en
  `rechtvaardigingZijde` opent voor een operand met een datumvouw de slots van de groep (`bouw.vouwslots`), die de
  reduce-regel al vulde voor de consequenties; een datumvouw valt buiten de weigering "een aggregatie binnen een
  rekenkundige uitdrukking".
- Zinnen: `voorwaardeSkipReden` zegt nu waar een datumvouw nog geweigerd wordt (`… op een plek die deze tak niet vouwt:
  alleen als operand van een vergelijking van twee datums of van een leeg/gevuld-toets van een datum`), de verdediging in
  `aggregatie` noemt de getalplek, `meet()` zet `vouwReden` terug, en `regel`/`objectCreatie` geven `vouwReden` door. De
  weigerzin van de DRL-teksttak in `kern.Vouwwaarde` noemt de drie plekken waar de exectak de vouw draagt (`bdedf518`).
- Verworpen: de voorwaarde in de consequentie toetsen (de regel vuurt dan ook als zij niet geldt, wat `regelversie …
  is gevuurd` en de dekking zou raken), en een predicaat met vaste ariteit op het laatste accumulate (plafond 13, en
  globals moeten dan vóór de accumulates gebonden worden).

**Vormruimte, vóór en na** (`meting-d-voor.txt`, `meting-df-na-final.txt`). 74 vormen uit `maak_d.py` in zes posities
(voorwaarde van een kenmerkregel en van een attribuutregel, rechtvaardiging van een uitzondering, van de basis en van een
afwijking, voorwaarde van een creatie) en 21 varianten van de voorwaarde (P = PLAYED met de waarden van ref en clingo,
FE = de frontend weigert de vorm):

| voorwaarde | regel-kenmerk | regel-attribuut | dflt-uitzondering | dflt-basis | dflt-afwijking | creatie |
|---|---|---|---|---|---|---|
| c01 de laatste … later dan zijn meldtermijn | P | P | P | P | P | P |
| c02 idem, de vouw rechts | P | P | P | P | P | P |
| c03 (de laatste …) plus 14 dg | P | P | P | P | P | P |
| c04 rechts, (de eerste …) plus 14 dg | P |  | P |  |  |  |
| c05 tegen een datumliteraal | P |  | P |  |  |  |
| c06 plus de meldduur (parameter) | P | P | P | P | P | P |
| c07 met een subselectie | P |  | P |  |  |  |
| c08 / c09 maximale / minimale waarde | FE | FE | FE | FE | FE | FE |
| c10 twee vouwen (eerste gelijk aan laatste) | P |  | P |  |  |  |
| c11 / c12 / c13 gelijk / ongelijk / later of gelijk | P |  | P |  |  |  |
| c14 / c15 gevuld / leeg | P | P (c14) | P | P (c14) | P (c14) | P (c14) |
| c16 alle / c17 ten minste één / c18 geen van | P | P (c16, c17) | P | P (c16, c17) | P (c16, c17) | P (c16, c17) |
| k01–k03 twee rollen ver (Kustwacht), ook plus en rechts | P |  | P |  |  |  |

Vóór weigerde de exectak elk van de 66 vormen die de frontend verlaagt: de voorwaarde van een regel en van een creatie
met de zin hierboven, een rechtvaardiging met `rechtvaardiging niet vertaald, een operand die deze tak niet als waarde
leest` (een minimum of maximum met `… een vergelijking van een aggregatie met iets anders dan een getal`). Na: 66 `PLAYED
1`, met ref ≡ clingo op alle 66; 8 FE: de frontend
weigert "de maximale/minimale waarde van" over een datum (`maximale waarde van vereist numerieke operanden, kreeg Datum
in dagen`); die vormen staan met de hand geschreven in de getuige (WF-038 laat ze toe; ref, clingo en de exectak spelen
ze). Elke situatie heeft een lege bron (J2, K3), bijdragers zonder datum (V4) en een lege andere operand (J5, K4).
Negen flanken (`maak_d_flank.py`, vóór alle negen geweigerd): een lid dat een latere regel afleidt (later dan, leeg,
ongelijk, en leeg in een rechtvaardiging), een genavigeerde andere operand (in een voorwaarde en een rechtvaardiging) en
een datumparameter: `PLAYED 1` met de waarden van ref en clingo, behalve de datumparameter in een rechtvaardiging, die met
de bestaande zin geweigerd blijft (`de parameter 'peildatum', die deze tak in een rechtvaardiging alleen als getal leest`; open punt 2).
Eén flank verdeelt ref en clingo: een attribuutregel "… gesteld op zijn meldtermijn indien de eerste van de meldingsdag
… ongelijk is aan zijn meldtermijn" met de meldingsdag afgeleid door een latere regel. ref schrijft J4 01-04-2026 (het
werkte op de tijdelijke lege meldingsdag), clingo en de exectak leeg. Dat is de vastgelegde rest van RUNTIME-SEMANTICS
ch. 6.1 ("conditions that hold on absence"; een attribuutregel leest een vergelijking positief); open punt 3. DRL-teksttak
vóór en na gelijk (`TODO voorwaarde`, of de defaultszin).

**Getuige.** `DatumvouwVoorwaardeWitnessTest` (nieuw, `61d7106d` rood): twaalf rijen, 27 documenten, elk verifier-schoon
en ref ≡ clingo met de verwachtingen van de rij: links en rechts, datumrekensom met een getal en een parameter, gelijk,
ongelijk, gevuld en leeg, het minimum en maximum over datums, onder alle, ten minste één en geen van, een regel die een
attribuut schrijft, twee rollen ver, tegen een datumparameter, een lid dat een latere regel afleidt (ook in een
rechtvaardiging), de rechtvaardiging van een uitzondering (ook plus 14 dg, onder ten minste één, met het maximum), van de
basis en van een afwijking, en twee rollen ver in een rechtvaardiging. Rood op de claim: `Tests run: 10, Failures: 0,
Errors: 10` met de zinnen hierboven; de twee rijen over een datumparameter en een later afgeleid lid kwamen na de fix en
geven op de jar van de claim dezelfde zinnen (alle 27 documenten gemeten). Twee bestaande rijen die de weigering in een
voorwaarde pinden spelen nu: `DatumvouwOverRolWitnessTest.een_datumvouw_in_een_voorwaarde` (was
`…_weigert_met_de_vorm`; J4 vroeg) en `ObjectcreatieVouwWitnessTest.een_datumvouw_in_de_voorwaarde_van_de_creatie` (was
`…_weigert_met_de_vorm`; 5, leeg, leeg, 5).

### (a) Wat elke default leest

**Oorzaak.** `ProducentConsument.vormen` gaf per defaultsgroep één verzameling `leest`; de wandeling per default
(`Lezer.dflt`) was privé en voegde rechtvaardiging, consequentie en uitzonderingen samen.

**Fix** (`202626e4`). `public static List<DefaultLezing> lezingen(RegelSpraakModel model, PrioritizedDefaults groep,
Navigatie navigatie)` met `public record DefaultLezing(List<Integer> pad, Default dflt, Set<ElementSleutel>
rechtvaardiging, Set<ElementSleutel> consequentie)`, zoals `dienst-scope.md` vraagt: in de volgorde van de groep, per
default eerst de default en dan die van zijn uitzonderingsgroepen (pre-order), pad `[i]`, `[i, g, j]`, …, elk tegen het
onderwerp van de groep, met dezelfde `Lezer`. `vormen()` neemt de lezingen van een groep nu daaruit (`leesDefaults`),
zodat de vereniging per constructie `Vorm.leest()` is; `Lezer.dflt` is weg. De verzamelingen zijn onwijzigbaar, het pad
ook.

**Getuige.** `GraafDefaultLezingTest` in `src/bronroute` (nieuw, `f9969364` rood: de suite compileerde niet zonder
`lezingen`): `de_lezingen_samen_zijn_wat_de_groep_leest_op_het_corpus` (een dynamische test per golden met een
defaultsgroep die de lader leest: 40 van de 41, wire en waar een `bron.rgs` is ook bron; vloer 40; de lader weigert
`jutterswet-kamerhuishouden-catala` om haar functiedefinities), `rechtvaardiging_en_consequentie_een_en_twee_rollen_ver_en_genest`
(een kenmerkcheck en een attribuut in de rechtvaardiging, een attribuut in de consequentie, ook van een uitzondering die
geen basis is, één en twee rollen ver, een genavigeerde kenmerkcheck, een geneste uitzonderingsgroep op pad `[1, 0, 0]`),
`de_groepdefault_van_een_basis_voor_een_subtype` (`[0] R[] C[]`, `[0, 0, 0]` de afwijking),
`de_leden_van_uitzonderingen_ongeordend_op` (`[0, 0, 0]`, `[0, 1, 0]`) en `het_pad_wijst_de_default_aan`, alle op beide
routes. Gate 15: `67 unit tests green (floor 23)` (was 23).

Punt (b), de scope van een scenario over de erflijn, staat onder dezelfde kop in de done-kaart
`20260930-frontend-afwijking-op-een-subtype.md`.

### Corpussweep

Over alle 287 goldens met cases, elk met al zijn cases, de statusregel per golden en per tak, jar van de claim tegen de
jar van de kop (`sweep-voor.txt`, `sweep-na.txt`, 574 regels): exectak `PLAYED` 235 → 235, `EXCLUDE` 52 → 52; DRL-tak
`PLAYED` 192 → 192, `EXCLUDE` 93 → 93, `FAIL` 2 → 2. Eén regel verandert, terecht:
`jutterswet-vondstbereik drl`, de weigerzin van `kern.Vouwwaarde`, oud "… de exec-modeltak (in de waarde van een regel)
dragen haar …", nieuw "… de exec-modeltak (als waarde, in een voorwaarde en in een rechtvaardiging) dragen haar …";
de status (EXCLUDE MARKER) blijft. Geen golden draagt een datumvouw in een voorwaarde of rechtvaardiging of een groep
boven negen variabelen; geen pin verschuift, geen nieuwe golden (de getuigen pinnen elke vorm).

### README's

`backends/drools/README.md`: de defaults-sectie (een consequentie bindt elk aantal variabelen; `7cca4b5e`), de alinea
over de creatie, een nieuwe sectie "Recorded: a date fold as an operand of a condition or a justification" en de
verwijzing in de sectie over vouwen in de waarde (`84fef9c6`), en `lezingen` in de sectie over de regelgraaf
(`fcfe5f72`).

### Gates

Alle zestien met `/tmp/v2/uchar-gates21/run.sh` op de kop `bdedf518` (de laatste codecommit; de commit met dit Result raakt
alleen de twee done-kaarten), `eval $(opam env)`, `dune build` exit 0, elk script onder zijn shebang; logs in
`/tmp/v2/uchar-gates21/`. Alle zestien exit 0:

     1. self-contained: check-self-contained: PASS
     2. ir-schema: PASS (… 330 file(s) validate and round-trip, floor 192 …)
     3. case-schema: PASS (schema valid; 92 pattern self-test row(s); 827 case file(s) validate)
     4. verifier: PASS (330 golden clean; 503 negatief rejected …; 346 on their declared arm …)
     5. oracle: PASS (ref suite 1152 worked examples, floor 489; 785 cases replayed exactly, floor 427; …)
     6. clingo: PASS (backend suite 1988 checks, floor 83; 785 cases replay exactly through clingo …)
     7. noop: PASS (741 cases refute a do-nothing executor; 44 pinned survivors …)
     8. rood: PASS (42 red cases fault observably in ref and clingo, floor 26; 84 pinned fragments …)
     9. catala: PASS — frontend suite green (241 checks, floor 201); 12 catala golden(s) …
    10. regelspraak: PASS — JUnit suite green (1068 tests, floor 173); 117 rgs golden(s) re-emit byte-identically (floor 80)
    11. toka-parity: PASS (45 .rgs re-emitted byte-identical, floor 45; TOKA suite 27 tests, floor 27)
    12. drools: PASS — played 235 (pin 235); channel floor 55; DRL-text played 192 (pin 192); DRL-text floor 98; JUnit 1035 tests, floor 239
    13. reftype-derivation: PASS (41 unit annotations twisted over 23 goldens, floor 38; 106 executor replays …)
    14. flogic: PASS — frontend suite green (150 checks, floor 81); 2 flogic golden(s) …
    15. graaf: PASS — spec leg 253 of 290 goldens compared, 183 dependencies equal (floors 234 / 116), 3 listed differences; route leg 117 goldens; 67 unit tests green (floor 23)
    16. scenario: PASS — 24 scenario golden(s) (floor 7) regenerate 84 case file(s) (floor 31) and their ir.json byte-identically

### Afwijkingen

1. Vier bestaande testrijen gewijzigd (de gebruikersregel "geen testbestanden wijzigen zonder te vragen" stond tegenover
   de opdracht, die precies de weigeringen wegneemt die deze rijen pinden; zonder wijziging zou de suite rood zijn): de
   twee rijen over vijf vouwen (c) en de twee rijen over een datumvouw in een voorwaarde (d), elk van "weigert met naam"
   naar "speelt met de waarden van ref en clingo", met de documenten ongewijzigd; de klasse-javadoc zegt het. Graag
   bevestiging van de eigenaar.
2. (c) De paarsgewijze telling van een uniciteit over meer dan acht bronnen is weg (bestond alleen voor de grens van
   negen); flank-rij erbij, groen vóór en na.
3. (d) Meer dan de vraag: ook de voorwaarde van een creatie (dezelfde `voorwaarde`), elke kwantor, een datumparameter in
   een voorwaarde, en het minimum en maximum over datums; en de weigerzin van de DRL-teksttak in `kern.Vouwwaarde`.
4. (a) `vormen()` leest de groepen nu via `lezingen` en `Lezer.dflt` is weg, in plaats van een tweede wandeling naast de
   oude: zo is de gelijkheid met `Vorm.leest()` een constructie en niet alleen een test. Gate 15 ongewijzigd groen.
5. Geen golden en geen pinvoorstel: geen golden verschuift en de getuigen pinnen elke vorm.

### Voorgestelde registertekst (voor de hoofdsessie)

- `spec/IR-SPEC.md` ch. 5.5: geen tekst. De flanken daar gaan over volgorde en leeg/gevuld; de datumvouw in een voorwaarde
  stond er niet (zij was een benoemde weigering, de tweede houding van ch. 5.5).
- `spec/CONSTRUCTS.md` rij 39, achter de Landed-alinea van 2026-10-01: "**Widened (2026-10-01, the scope of a scenario).**
  A scenario whose scope names the base of a tree carries the groups `<base> voor <subtype>` that take that base, and a
  scope that names such a group carries its base and the other groups that take it, read off the groups (subject,
  target, a default with a decision or consequence of its own) and not off their names; RUNTIME-SEMANTICS ch. 7.10
  composes a subtype's stack from both, and a scope that dropped the subtype's group gave a subtype instance the base's
  value in all three executors (measured). A subtype's own base stays a tree of its own. Witness
  `ScenarioErflijnScopeTest` (frontend); the scenario goldens regenerate byte-identically (gate 16)."
- `spec/CONSTRUCTS.md` rij 6, optioneel, achter de conformance-flank: "A date folded over a set source in a condition
  (since 2026-10-01 lowered in the exec-model branch) reads its members through the same order: a characteristic
  assignment over a fold of a member a later rule derives waits for that rule, also under a leeg test (witness
  `DatumvouwVoorwaardeWitnessTest`)."
- `spec/proposals/PRIORITIZED-DEFAULTS-EVOLUTIE.md` §5.1, na de alinea over de consequentie die vouwt: "Landed
  2026-10-01 (exec-model branch): a justification that compares a date folded over a set source, or tests it for leeg,
  reads the fold from the slots the group's consequences use, on either side and inside date arithmetic; and a group binds
  any number of variables (the folds, aggregations and globals of every default of its stack). Witnesses
  `DatumvouwVoorwaardeWitnessTest`, `BindingVoorbijNegenWitnessTest`."
- `spec/RUNTIME-SEMANTICS.md` ch. 6.1, optioneel, bij "The same classes also leave the reference interpreter
  order-dependent OFF a cycle": een tweede meting van dezelfde klasse, "de eerste van de meldingsdag … ongelijk is aan
  zijn meldtermijn" in een attribuutregel met de meldingsdag afgeleid door een latere regel: ref schrijft, clingo en de
  Drools-exectak antwoorden leeg (open punt 3).

### Open punten

1. De reproducties `a2e`/`a2` spelen in de exectak met de waarden van de regelsetbouwer, maar ref en clingo kunnen ze daar
   niet toetsen: ref weigert 8 van de 14 scenario's (`EVALUATION FAULT: function kind not implemented in this card`, de
   datum uit jaar, maand en dag) en clingo het document (`TRANSLATION FAULT: a Clamp minimum bound that is not a ground
   literal could be empty …`, de begrenzing met een parameter als minimum). Buiten deze laag (`ref/`, `backends/clingo/`).
2. Een datumparameter in een rechtvaardiging of consequentie weigert de exectak met naam, met en zonder vouw (`de
   parameter '…', die deze tak in een rechtvaardiging alleen als getal leest`); de reduce-regel bindt alleen numerieke
   globals en de rekendatum. Klein in dezelfde laag, maar het verandert een bestaande weigering zonder vouw; niet gedaan.
3. Een attribuutregel met "ongelijk" (of een andere vorm die op afwezigheid waar is) over een vouw van een lid dat een
   latere regel afleidt: ref werkt op de tijdelijke waarde, clingo en de exectak niet. Dezelfde rest als ch. 6.1 al
   vastlegt; normvraag voor de eigenaar.
4. De frontend weigert "de maximale/minimale waarde van" over een datum (`… vereist numerieke operanden, kreeg Datum in
   dagen`), terwijl WF-038 het toelaat en de drie uitvoerders het spelen; buiten deze ronde (`frontends/regelspraak`,
   typering).
5. Een datumvouw in de matrix of het filter van een kwantorvouw, in de subselectie van een vouw of in een periodegrens
   blijft een benoemde weigering (`voorwaardeSkipReden`); niet gemeten.
6. De DRL-teksttak markeert een voorwaarde met een datumvouw (`TODO voorwaarde`) en heeft geen defaults-lowering;
   ongewijzigd.
7. (a) De dienst kan een default nu bij pad en brontekst noemen; bij de naam van zijn blok ("late berging") kan zij hem
   pas noemen als `Default` die naam draagt, een keuze over de IR.
8. De distributie draait op haar eigen submodulepin en kent deze ronde pas als die pin deze kop bevat.

### Commits

    afe0c361 claim: vervolg graaf en defaults
    d3902c82 Getuige: meer dan negen gebonden variabelen spelen, rood
    d41ffee6 Exectak: een consequentie bindt meer dan negen variabelen
    7cca4b5e backends/drools/README: een consequentie bindt voorbij negen
    61d7106d Getuige: een datumvouw in een voorwaarde en een rechtvaardiging, rood
    0ad715c4 Exectak: een datumvouw in een voorwaarde en een rechtvaardiging
    84fef9c6 backends/drools/README: een datumvouw als operand van een toets
    f9969364 Graaf: getuige voor wat elke default leest, rood
    202626e4 Graaf: ProducentConsument.lezingen, wat elke default leest
    fcfe5f72 backends/drools/README: wat elke default van een groep leest
    3ce39c7a ScenarioErflijnScopeTest: de scope over een erflijn, rood
    646603ed ScenarioResolver: een scope draagt de groepen van een erflijn mee
    ffcfd73a DIVERGENCES: de scope van een scenario over de erflijn
    bdedf518 Vouwwaarde: de exectak draagt een datumvouw ook in voorwaarden

en de commit "Vervolgronde graaf en defaults: Result", die alleen de twee done-kaarten raakt. Niet gepusht, niet gemerged.

## Keuring vervolgronde graaf en defaults (hoofdsessie, 2026-10-01)

Eigen run van de zestien gates op `76ece9f7` in de volgorde van `gates.yml`
(bash waar de shebang dat zegt), `dune build` vooraf; logs in
`/tmp/v2/uchar-gates21/`. Alle zestien exit 0: gate 10 JUnit 1068, 117
rgs-goldens; gate 12 `check-drools: PASS`, played 235 (pin 235), DRL 192
(pin 192), JUnit 1035; gate 16 24 scenario-goldens, 84 casebestanden. De fix
gelezen: (c) de consequentie van een regel, een groep en een creatie bindt
voorbij negen variabelen via één gebonden structuur (`Waarden`, `toetsNa`);
(d) een datumvouw in een voorwaarde of rechtvaardiging wordt een accumulate
op de regel resp. de reduce-regel en de toets leest de uitkomst uit het slot
als datum (`datumvouwVoorwaarde`, `slotToets`, `vouwOperand`); (a)
`ProducentConsument.lezingen` per default met `vormen()` daarop gebouwd, zodat
de gelijkheid een constructie is; (b) `ScenarioResolver.metStapel` draagt
`<basis> voor <subtype>` met haar basis mee, uit de groepen gelezen. Afwijking
1 aanvaard: vier bestaande getuigenrijen pinden precies de weigeringen die
deze ronde wegneemt en verwachten nu de waarden van ref en clingo met de
documenten ongewijzigd. Afwijkingen 2–5 aanvaard. De registertekst is in dit
landingscommit gezet in `spec/CONSTRUCTS.md` rij 39 (Widened) en
`spec/proposals/PRIORITIZED-DEFAULTS-EVOLUTIE.md` §5.1; de optionele teksten
voor CONSTRUCTS rij 6 en RUNTIME-SEMANTICS ch. 6.1 niet. Open punten: 2 (een
datumparameter in een rechtvaardiging; klein, zelfde laag, volgende ronde
in deze tak), 3 ("ongelijk" over een later afgeleid lid: ref op de
tijdelijke waarde, clingo en exec niet; het residu van ch. 6.1, normvraag
eigenaar), 4 (de frontend weigert "de maximale/minimale waarde van" over een
datum terwijl WF-038 het toelaat; frontend-typering, volgende frontendronde),
7 (de naam van een blok op `Default` is een IR-keuze van de eigenaar); 1, 5,
6 en 8 gelezen, geen kaart. Commitkoppen tot 69 tekens aanvaard.
