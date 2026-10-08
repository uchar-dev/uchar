# tms-omschakeling: de truth-maintenance-verlaging wordt de exec-modeltak, en de destructieve verlaging verdwijnt

*Vastgelegd door de hoofdsessie op 2026-10-08, na het besluit van de eigenaar
op het `Result:` van `20261007-drools-logische-afleiding` (PLAN §9,
2026-10-08): de truth-maintenance-tak vervangt de exec-modeltak, ook in de
toolchain. De vierde en laatste bouwkaart van de tak, na
`20261008-tms-verdeling-en-tijdlijnen`. Regelnummers op uchar `3619921b`.*

## Aanleiding

`DroolsBackend.Branch` heeft drie waarden (`cases/DroolsBackend.java` regel 75):
`EXEC_MODEL`, de destructieve verlaging in
`transpiler/model/ExecModelTranspiler.java` (9898 regels), die gate 12 speelt
(`EXPECTED_PLAYED=240`, `scripts/check-drools.sh` regel 1253) en de toolchain
gebruikt; `DRL_TEXT`; en `TRUTH_MAINTENANCE`, de tak achter `--tms` die de drie
bouwkaarten hiervoor uitbouwen. Het `Result:` van de proefkaart: "Ze groeit tot
de dekking van de huidige exec-modeltak (gate 12: 240) en vervangt die dan, ook
in de toolchain."

De toolchain bouwt met `DroolsBackend.Branch.EXEC_MODEL`
(regelspraak-toolchain
`toolchain/src/main/java/dev/uchar/regelspraak/toolchain/engine/RegelSpraakEngine.java`
regel 57, gemeten op 2026-10-07) en vuurt en leest zelf
(`service/.../Uitvoerder.java` regel 69, `CaseSessie`). Met truth maintenance
staan de afgeleide waarden als logische feiten in de sessie; ze komen pas op de
bonen via `DroolsBackend.zetAfgeleideWaarden(Build, KieSession)` (regel 270).
De proeftak riep die na één `fireAllRules` aan; een dienst die invoer in een
lopende sessie verandert en opnieuw vuurt, vraagt ook dat een veld leeg wordt
als zijn feit verviel (open punt 2 van de proefkaart).

Goal: `Branch.EXEC_MODEL` bouwt via de truth-maintenance-verlaging; de
destructieve verlaging, de waarde `TRUTH_MAINTENANCE` en de vlag `--tms`
verdwijnen; gate 12 speelt ten minste dezelfde goldens met dezelfde
verwachtingen; de ingang voor afnemers beschrijft het terugzetten; de toolchain
volgt met een eigen kaart.

## Wat er gebeurt

1. **Voorwaarde.** Een doorloop van beide takken over het hele corpus, op de
   commit waar deze kaart begint: de truth-maintenance-tak speelt elke golden
   die de exec-modeltak speelt, met dezelfde namen en zonder FAIL. Een golden
   die verschilt, gaat terug naar een bouwkaart, niet naar een lijst; gate 12
   mag niet teruglopen (`CLAUDE.md`, "Gate set").
2. **De omschakeling.** `EXEC_MODEL` bouwt via de truth-maintenance-verlaging.
   Het pakket `transpiler/tms/` heet dan niet meer de proeftak; de bouwer kiest
   of het pakket blijft of verhuist. `TRUTH_MAINTENANCE` en `--tms` verdwijnen
   zonder alias.
3. **Wat weggaat.** `ExecModelTranspiler` en wat alleen hij gebruikt; de
   getuigen die de destructieve maatregelen toetsen, of ze worden omgezet naar
   de nieuwe verlaging; het `Result:` noemt elke verwijderde getuige met de
   reden. `Stapels` is daarna de enige samenstelling van ch. 7.10 en geen kopie
   meer. `Stratificatie` blijft tot `20261008-drl-teksttak-op-truth-maintenance`,
   omdat `DrlTranspiler` haar nog gebruikt.
4. **Terugzetten bij herhaald vuren.** `zetAfgeleideWaarden` maakt een veld leeg
   waarvan het feit verviel, zodat een afnemer na elke `fireAllRules` dezelfde
   functie kan aanroepen. Getuige: twee keer vuren in één sessie, met tussendoor
   een invoerwijziging die een afleiding laat vervallen.
5. **Gates** (beschermd pad `scripts/`: **voorstel**). Gate 12: de lijsten van
   de exec-modeltak volgen de nieuwe verlaging; `EXPECTED_PLAYED` loopt niet
   terug; een golden die alleen om een fout van de destructieve verlaging op de
   lijst van waardeverschillen stond, gaat eraf. Gate 13 speelt het pad van gate
   12; zijn tellingen en vloeren gaan mee. De corpusgetuige `TmsCorpusWitnessTest`
   gaat op in gate 12, of blijft met een reden.
6. **README.** De sectie "The truth-maintenance trial branch" wordt de
   beschrijving van de exec-modeltak; "The entry for consumers" beschrijft het
   terugzetten na elke `fireAllRules`. Elke sectie "Recorded: …" die gedrag van
   de destructieve verlaging beschrijft (onder meer "two writers of one target"
   en "a reader after the writers of what it reads"), wordt herschreven voor de
   nieuwe verlaging of verdwijnt, met de reden.
7. **Meting.** `TmsMeting` over alle gespeelde goldens, oud tegen nieuw
   (vuringen, tijd van vuren en terugzetten, bouwtijd), als tabel in het
   `Result:`, voor de dienst van de toolchain. Geen drempel.
8. **De toolchain.** Het `Result:` geeft de tekst van een kaart voor de
   distributie (`uchar-dev/regelspraak-toolchain`): de pin verzetten en na elke
   `fireAllRules` het terugzetten aanroepen in `Uitvoerder` en `CaseSessie`. Die
   kaart landt daar, na deze.

## Aanvulling na `20261008-tms-aggregaties-creatie-en-fouten` (2026-10-09)

Uit het `Result:` van die kaart (waarneming 11, het advies, de open punten) en
het besluit van de eigenaar van 2026-10-08 (PLAN §9):

9. **Schaalproef met vloer** (besluit van de eigenaar). Eén Kustwacht, N
   Wachters met elk 10 Vondsten; (a) een regel per Vondst uit invoer maal een
   parameter; (b) een defaults-groep op de Vondst met een onvoorwaardelijke
   lagere default en een hogere die (a) leest; (c) een som en (d) een maximum per
   Wachter over (b); (e) een som over alle Wachters op de Kustwacht; (f) een
   consistentie per Wachter; (g) een recursieve creatieketen van diepte 3 per
   Wachter. Voor N ∈ {100, 1 000, 10 000}: vuringen tegen het plafond, tijd van
   vuren en terugzetten, piekaantal feiten en heap, bouwtijd, de verhouding met
   de exec-modeltak van vóór de omschakeling, en de helling in log-log; plus een
   tweede fire na één invoerwijziging tegen een verse sessie. **Vloer:** helling
   ten hoogste 1,2 en vuringen onder de helft van het plafond bij N = 10 000.
   Haalt de verlaging dat niet, dan eerst de joins indexeren (`alphaIndexedBy` op
   de lidsleutel, `betaIndexedBy` op het onderwerp, gelijke ids voor gelijke
   constraints, zodat Drools knopen deelt), en dan opnieuw meten. Gelezen door de
   adviseur op `42859692`: elke join is een ongeïndexeerde lambda, elke
   constraint heeft een eigen id (`id()` in `TmsTranspiler`), `min` en `max`
   rekenen bij een intrekking alles opnieuw (`supportsReverse()` is daar false in
   Drools 8.44), en het plafond van 100 000 vuringen
   (`CaseRunner.java` regel 576) is met ongeveer 5 000 objecten × 20 leden al
   bereikt.
10. **Een vuurplafond dat met de invoer meegroeit**, in plaats van de vaste
    100 000 (de driver `DroolsBackend.vuur` krijgt het plafond als argument).
11. **Herhaald vuren na het terugzetten.** Na `zetAfgeleideWaarden` staan de
    afgeleide waarden op de bonen, en de patronen op de bonen lezen ze bij een
    volgende fire als invoer: een initialisatie wordt dan geblokkeerd door een
    waarde "uit de invoer" die zij zelf schreef. Punt 4 lost dat op (niet
    terugzetten tussen twee fires, of de bonen alleen invoer laten dragen), en
    de getuige van punt 4 draait ook een initialisatie.
12. **Het contract voor afnemers**, in de README (punt 6) en in de kaarttekst
    voor de toolchain (punt 8): een model met creaties vuurt via de driver
    `DroolsBackend.vuur` (strata, generaties, de agendagroepen
    `TmsTranspiler.FEITCREATIE` en `OBJECTCREATIE`), die nog niet publiek is;
    deze kaart beslist hoe een afnemer hem aanroept (de javadoc van
    `DroolsBackend` verwijst al hierheen). Tot dan weigert
    `zetAfgeleideWaarden` een sessie van zo'n model die zonder de driver vuurde.
    Bij het vullen van een sessie komen de
    `Lijstplaats`-feiten mee als `TmsTranspiler.leestLijstplaatsen()` dat vraagt;
    `zetAfgeleideWaarden` gooit `DelingDoorNul` bij één fout en
    `Evaluatiefouten` bij twee of meer; en gecreëerde links staan nog niet op de
    referentievelden van de bonen (alleen als `Koppeling`-feit), wat de afnemer
    moet weten of wat deze kaart oplost. Nieuw publiek in het pakket van de tak
    sinds die kaart: `Evaluatiefouten`, `Lijstplaats`, `Creatiefase`, en in
    `TmsTranspiler` `FEITCREATIE`, `OBJECTCREATIE`, `creatieregels()`,
    `creatiestrata()`, `lidstratum(…)` en `leestLijstplaatsen()`.

## Acceptatie

- (a) De doorloop van punt 1, als tabel in het `Result:`.
- (b) Getuige van punt 4, rood eerst.
- (c) `grep -rnE 'NO_LOOP|SALIENCE|\.watch\(|drools\.update|initEigen'` over
  `backends/drools/src/main/java` raakt alleen nog de DRL-teksttak, of niets.
- (d) Gate 12 groen met `EXPECTED_PLAYED` ten minste 240 en dezelfde namen;
  `EXPECTED_DRL_PLAYED` gelijk; alle gates van `CLAUDE.md` groen.
- (e) De meting van punt 7 en de kaarttekst van punt 8 in het `Result:`.
- (f) De schaalproef van punt 9 als tabel in het `Result:`, met de vloer
  gehaald; punt 10 en 11 met een getuige, rood eerst; het contract van punt 12
  in de README en in de kaarttekst van punt 8.

## Raakvlakken

- `20261008-deling-door-leeg-onder-initialisatie`: landt die kaart eerder, dan
  verdwijnt haar deel in de exec-modeltak hier; de nieuwe verlaging gaf daar al
  het goede antwoord.
- `20261008-fout-op-tussenstand`: de Drools-takken van haar (c) zijn na deze
  kaart en de DRL-kaart de takken met truth maintenance.
- `20260918-drools-veelopveel-beanmodel` raakt alle takken. Na deze kaart en de
  DRL-kaart draagt hij er twee in plaats van drie.
- `20260923-drools-rolpaar-ontdubbeling`: de kopie in `ExecModelTranspiler`
  verdwijnt hier.
- Daarna: `20261008-drl-teksttak-op-truth-maintenance`.

## Review

Zoals in de eerste bouwkaart: een lens na elke bouwstap, een eindlens over de
hele diff, en een lensvondst alleen met een concreet faalscenario op echte
invoer of een getuige.

## Omvang

Middel. Het werk zit in het opruimen, de gates en de README; de verlaging zelf
is er dan al.

May touch: `backends/drools/**` behalve de DRL-teksttak,
`scripts/check-drools.sh` en `scripts/check-reftype-derivation.sh`
(**voorstel**), kaartverplaatsing. NB: serieel met andere Drools-kaarten.

Out of scope: de DRL-teksttak (`DrlTranspiler`, `Stratificatie`), de toolchain
zelf (eigen kaart, punt 8), `spec/`, `corpus/`.
