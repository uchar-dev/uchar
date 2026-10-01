# ronde-exec-vouwfilter-far-side: een vouwfilter dat via de rol van het element leest, een eigen getal links van een genavigeerd getal, een deling als consequentie van een default

*Ronde op drie vondsten van cluster G4+G5 (V6, V8, V4), opdracht van de
hoofdsessie van 2026-10-01; gebouwd vanaf uchar `2a0c6d60` op de tak
`ronde/g4-g5-exec`. Reproducties op Jutterswet-materiaal van V6 en V4 in
`/tmp/v2/probe/g4-g5-uchar/v6/` en `/tmp/v2/probe/g4-g5-uchar/v4/` (alleen
gelezen); V8 had geen reproductie en is in deze ronde op Jutterswet-materiaal
nagebouwd, de documenten staan in de getuige. V6 was een stil fout antwoord,
V8 en V4 waren luid (exit 3). Geen kaart: dit record legt de ronde vast,
zodat de eigenaar er zijn Keuring bij kan zetten.*

## Aanleiding

**V6, een vouwfilter dat via de rol van het element leest.**

    De eerste late vinddag van een Jutter moet berekend worden als de eerste van
    de vinddag van alle vondsten van de Jutter die aan de volgende voorwaarde
    voldoen: zijn vinddag is later dan de sluitdag van de jutter van de Vondst.
    De sluitdag van een Jutter moet gesteld worden op zijn startdag.

De subselectie van een vouw leest met het element als onderwerp
(RUNTIME-SEMANTICS ch. 3.4a); "de sluitdag van de jutter van de Vondst" is een
genavigeerde lezing over de rol `jutter` van het element. Gemeten op
`2a0c6d60` met J (startdag 01-03-2026) en vondsten op 15-02, 10-03 en
20-03-2026: `check` exit 0, verifier PASS, ref `oracle: PASS (2 expectations)`,
clingo `clingo: PASS (2 expectations)`, de exectak `FAIL … attribuut eerste
late vinddag: verwacht 2026-03-10, werkelijk leeg`. Dezelfde vorm als telling
`verwacht 2, werkelijk 0`, als laatste `werkelijk leeg`, als telling of kwantor
in een voorwaarde `attribuut laatklasse: verwacht 1, werkelijk leeg`; de
DRL-teksttak speelt de telling en de voorwaardevormen even stil fout. Een
keten van twee rollen in het filter en een datumrekensom met het verre lid
weigerde de exectak benoemd (`vouw EERSTE over een setbron die deze tak niet in
een waarde vouwt`). Oorzaak (dossier, sectie 1(e), nagelopen): `filterSource`
zet de toets op het elementpatroon; die draait als het element wordt
ingevoegd of bijgewerkt, niet als de jutter verandert.

**V8, een eigen getal links van een genavigeerd getal.**

    Een Vondst is zwaar
        indien zijn gewicht groter is dan de afgeleide diepte van de haven van
        de jutter van de Vondst.

Gemeten op `2a0c6d60`: ref en clingo `PASS (4 expectations)`, de exectak
`EXCLUDE transpiler SKIP: regel: zwaar — regel (voorwaarde buiten Phase-B
scope): zwaar`; de gespiegelde zin ("de afgeleide diepte van de haven van de
jutter van de Vondst kleiner is dan zijn gewicht") en "zijn gewicht maal 1
groter is dan …" `PLAYED 1`. Hetzelfde over één rol, onder groter of gelijk
en ongelijk, en in een gewone regel. Oorzaak: `leafPredicate` heeft een
getallenarm voor de genavigeerde lezing links van een eigen lid en geen voor
de omgekeerde volgorde.

**V4, een deling als consequentie van een default.**

    Regel verhouding
        De verhouding van een Vondst moet gesteld worden op 1,00.
    Afwijking van verhouding: gewogen
        De verhouding van een Vondst moet berekend worden als zijn gewogen
        gewicht gedeeld door zijn geschatte gewicht indien hij aan alle volgende
        voorwaarden voldoet: zijn gewogen gewicht is gevuld; zijn geschatte
        gewicht is groter dan 0.

Gemeten op `2a0c6d60`: ref en clingo `PASS (2 expectations)`, de exectak
`EXCLUDE transpiler SKIP: defaults: … kanaal-deponerende consequentie (deling)
op defaults-target`, ook met de deling in de basisregel en in een
`Uitzondering op` (die dezelfde groep op de draad geeft). Oorzaak:
`compileDefault` weigert elke consequentie waarin `foutplekSoort` een deling of
wortel vindt, omdat een groep geen begeleider draagt.

## Wat er landde

**V6 (exectak).** `ExecModelTranspiler.elementtoets` en `verreJoins`: waar de
subselectie van een vouw, of het filter of de matrix van een kwantorvouw, via
een rol van het element leest, krijgt de bron van de accumulate een join per
object dat die lezingen bereiken (bij een keten per object onderweg, met de
sleutels van `navJoins`), en staat de subselectie op het element dat daarna
opnieuw gebonden wordt (`from` de elementvariabele), de vorm die
`regelVoorwaarde` een voorwaarde geeft. Een wijziging van het bereikte object
toetst de bijdrage van het element opnieuw; de matrix van een kwantorvouw wordt
opnieuw gevraagd omdat de accumulate haar uitkomst opnieuw doorgeeft. De join
is op `Object`: een accumulate-bron draagt geen OR naast de join (drools-core
8.44 bouwt geen netwerk voor een OR binnen een subnetwerk; `GroupElementBuilder$OrBuilder`
gooit `Can't build a rete network with an inner OR group element`, gelezen in de
bytecode, niet beproefd), dus bindt de join het object
dat de rol bereikt, of het element zelf waar de rol niets bereikt; elk element
houdt één tupel en een lege rol leest leeg. Het verre lid wordt gelezen via het
element, door dezelfde lezers als vóór; de join bindt hetzelfde object en zorgt
dat de toets opnieuw draait. Geldt voor de vouw in een waarde en in een
voorwaarde, de kwantorvouw (filter en matrix) en de temporele setsom; een
keten en een datumrekensom met het verre lid lowert in een subselectie zoals in
een voorwaarde (`gejoindeLezingen`). Een unieselectie die zo leest weigert
benoemd (`aggregatieReden`: haar selecties delen één elementpatroon). Commits
`de0a8bd6` (getuige, rood), `b20ceb51` (fix).

**V6 (DRL-teksttak).** `DrlTranspiler.leestViaElementrol`: een subselectie (van
een vouw, een unieselectie of een temporele som) of een kwantormatrix die via
een rol van het element leest, weigert deze tak met de zin `een subselectie of
matrix die via een rol van het element leest: deze tak schrijft haar in het
elementpatroon, en een latere schrijving op het bereikte object toetst haar
niet opnieuw (IR-SPEC ch. 5.5)`. De join past in deze tak niet even klein (het
is een `from`-herbinding en een `Object`-patroon in de tekst van de
accumulate); de marker volgt het kanaal van deze tak, zoals haar andere
grenzen. Commits `6b58025f` (getuigerij, rood), `0bca28cc` (marker).

**V6 (golden).** `corpus/golden/jutterswet-late-vondst`: de eerste late
vinddag en het aantal late vondsten (de telling-vorm ernaast) over dezelfde
subselectie, de sluitdag uit een regel die na de vouwen staat; case 1 drie
vondsten rond de sluitdag, case 2 een sluitdag waar niets later is, een jutter
zonder sluitdag en een zonder vondsten. Handgerekend in `NOTE.md`. Commit
`63a87068`; gate-12-voorstel `35e24bb3`; README `19bc70d2`.

**V8.** `leafPredicate` wisselt een getallenvergelijking met een eigen lid
links en een genavigeerd lid rechts om en keert de operator om, zoals het al
deed voor een literal links; daarna valt de zin in de bestaande arm voor de
genavigeerde lezing links. Geldt in een voorwaarde, een kenmerktoekenning, een
subselectie en een kwantormatrix. Commits `4bed5123` (getuige, rood),
`238c72ec` (fix), `18dcbbde` (README).

**V4, de smalle stap.** `ExecModelTranspiler.plekIsDeConsequentie`: is de
deling of wortel de consequentie zelf, hooguit onder een afronding, en houden
haar operanden geen plek, dan lowert `compileDefault` haar als gewone deling of
wortel, zonder begeleider. Over een lege deler of een negatieve radicand is de
consequentie leeg, valt de default terug, en blijft het depot van een default
die terugvalt niet staan (RUNTIME-SEMANTICS ch. 7.1, de rij "speculative
default evaluation" van ch. 7.5); een default die beslist heeft geen depot.
Een plek onder een andere bewerking blijft geweigerd. Commits `8b1cd526`
(getuige, rood), `d2bf6685` (fix), `bf453803` (README).

## Metingen

Gemeten met de eigen uitvoerders van de worktree (`_build/default` van ref,
clingo en de verifier), de frontend van de distributie voor `check` en
`cases` (gelijk aan die van de worktree), en twee jars: vóór (de bouw van
`2a0c6d60`) en na (de bouw van de commit van de fix). Elke vorm is een eigen
Jutterswet-document; ref en clingo zijn de arbiter.

**V6, de vormen van het dossier** (`/tmp/v2/probe/g4-g5-uchar/v6/`; ref en
clingo PASS op elke vorm):

| vorm | exectak vóór | exectak na | DRL-tekst na |
|---|---|---|---|
| a, b (volgorde), g (onderwerpnaam), k (maskering vóór de schrijver), l (eigen voorwaarde) | FAIL `werkelijk leeg` | PLAYED 1 | EXCLUDE `TODO aggregatie EERSTE` (vóór idem) |
| i (laatste) | FAIL `werkelijk leeg` | PLAYED 1 | EXCLUDE `TODO aggregatie LAATSTE` (vóór idem) |
| h (telling) | FAIL `verwacht 2, werkelijk 0` | PLAYED 1 | EXCLUDE (de nieuwe zin; vóór FAIL `verwacht 2, werkelijk 0`) |
| m (telling in een voorwaarde) | FAIL `verwacht 1, werkelijk leeg` | PLAYED 1 | EXCLUDE (de nieuwe zin; vóór FAIL) |
| n (kwantor in een voorwaarde) | FAIL `verwacht 1, werkelijk leeg` | PLAYED 1 | EXCLUDE `TODO kwantorvouw … — <de nieuwe zin>` (vóór FAIL) |
| o (keten van twee rollen) | EXCLUDE `vouw EERSTE … niet in een waarde vouwt` | PLAYED 1 | EXCLUDE `TODO aggregatie EERSTE` |
| c (invoer), d en j (maskering na de schrijver), e (brug: kenmerk op het element), f (letterlijk) | PLAYED 1 | PLAYED 1 | EXCLUDE `TODO aggregatie EERSTE` |

**V6, de vormen van open punt 1 van het dossier en de flanken** (nieuw in deze
ronde, elk met ref en clingo PASS):

| vorm | exectak vóór | exectak na | DRL-tekst vóór → na |
|---|---|---|---|
| filter op een kenmerk van het verre object ("zijn jutter actief is", en "niet actief") | FAIL `verwacht 3, werkelijk 0` / `verwacht 0, werkelijk 3` | PLAYED 1 | FAIL → EXCLUDE |
| het verre lid uit een defaultsgroep (opgave gaat voor) | FAIL `werkelijk leeg` (twee keer) | PLAYED 1 | EXCLUDE (defaults) |
| het verre lid uit een verdeling (het bergingsaandeel van de vondstberger) | FAIL `verwacht 1, werkelijk 0` | PLAYED 1 | FAIL → EXCLUDE |
| het verre object uit een creatie (de melding van de Vondst) | PLAYED 1 | PLAYED 1 | PLAYED → EXCLUDE |
| som en maximum | FAIL `werkelijk leeg` (twee keer) | PLAYED 1 | FAIL → EXCLUDE |
| leeg-toets op het verre lid, in de waarde | FAIL `verwacht 0, werkelijk 3` | PLAYED 1 | FAIL → EXCLUDE |
| leeg-toets op het verre lid, in een voorwaardevouw | FAIL `verwacht leeg, werkelijk 1` | PLAYED 1 | FAIL → EXCLUDE |
| een rol die niets bereikt (haven zonder openingsdag, vondst zonder haven), leeg en ongelijk | FAIL `verwacht 2, werkelijk 4` | PLAYED 1 | FAIL → EXCLUDE |
| twee verre objecten (jutter en haven) | FAIL `verwacht 1, werkelijk 0` | PLAYED 1 | FAIL → EXCLUDE |
| matrix van een kwantor via de haven van het element, ten minste één en geen | FAIL `verwacht 1, werkelijk leeg` / `verwacht leeg, werkelijk 1` | PLAYED 1 | FAIL → EXCLUDE |
| matrix van een kwantor via de jutter van het element (het onderwerp zelf), ten minste één en geen | PLAYED 1 | PLAYED 1 | FAIL → EXCLUDE |
| filter van een kwantor via de haven | FAIL `verwacht 1, werkelijk leeg` | PLAYED 1 | FAIL → EXCLUDE |
| datumrekensom met het verre lid ("zijn vinddag min 7 dg") | EXCLUDE (benoemd) | PLAYED 1 | EXCLUDE `TODO aggregatie EERSTE` |
| keten waarvan de middelste relatie uit een feitcreatie komt | EXCLUDE (benoemd) | PLAYED 1 | EXCLUDE `TODO aggregatie EERSTE` |

Een matrix die via de rol naar het onderwerp zelf leest ("de sluitdag van zijn
jutter") speelde al: een bijwerking van het onderwerp geeft de accumulate
opnieuw door. De frontend weigert in een matrix "de sluitdag van de jutter van
de Vondst" (`is niet als navigatie gelegd`) en bindt "de sluitdag van zijn
jutter" (open punt 6).

**V8** (vormen in de getuige; ref en clingo `PASS (4 expectations)` op elk):
kenmerk twee rollen ver (groter, groter of gelijk, ongelijk), één rol ver,
gewone regel twee en één rol ver: exectak vóór EXCLUDE `voorwaarde buiten
Phase-B scope`, na PLAYED 1; de gespiegelde vorm en het product vóór en na
PLAYED 1. Dezelfde vergelijking in een subselectie: vóór EXCLUDE `aggregatie
buiten Phase-C scope`, na PLAYED. DRL-tekst ongewijzigd (twee rollen `TODO
voorwaarde`; één rol FAIL, de bekende grens van een genavigeerde toets in die
tak).

**V4** (`/tmp/v2/probe/g4-g5-uchar/v4/` en de vormen van de getuige):

| vorm | ref | clingo | exectak vóór | exectak na |
|---|---|---|---|---|
| a (deling in een afwijking), e (uitzondering, dezelfde groep), b (deling in de basisregel) | PASS (2) | PASS (2) | EXCLUDE `kanaal-deponerende consequentie (deling)` | PLAYED 1 |
| lege deler, met "de gegevens zijn consistent" | PASS (4) | MISMATCH `expected consistent, but the model carries 1 inconsistent fact(s)` | EXCLUDE | PLAYED 1 (= ref, ch. 7.1) |
| afronding op 2 decimalen | PASS (3) | PASS (3) | EXCLUDE | PLAYED 1 |
| afronding op 1 decimaal | PASS (4) | TRANSLATION FAULT `default value scale 10 does not match target scale 100` | EXCLUDE | PLAYED 1 |
| wortel, afgerond, met een negatieve radicand | PASS (4) | TRANSLATION FAULT `rounded root is not translated` | EXCLUDE `(wortel)` | PLAYED 1 |
| nuldeler | EVALUATION FAULT `division by numeric zero` | UNSATISFIABLE (fail-fast) | EXCLUDE | ERROR `ArithmeticException: Breuk deling door 0` |
| "(… gedeeld door …) plus 5,00" met een lege deler (open punt 6 van het dossier) | V3 5,00 en `the channel carries: Vondst.verhouding × V3` | idem (1 inconsistent fact) | EXCLUDE | EXCLUDE (blijft) |
| c (gewone regel), d (initialisatie en berekening), f (opgave-markering) | PASS | PASS | PLAYED 1 | PLAYED 1 |

**Getuigen.** `VouwfilterVerreKantWitnessTest` (23 rijen): rood vóór de fix
`Tests run: 22, Failures: 18, Errors: 2` (de twee flanken groen), de DRL-rij
rood vóór de marker (`Expected … Excluded to be thrown, but nothing was
thrown`); `VergelijkingEigenLinksWitnessTest` (6 rijen): rood `Errors: 5`
(de flank groen); `DefaultsDelingWitnessTest` (6 rijen): rood `Errors: 5`
(de weigering van de samengestelde vorm groen). Na de fixes alle drie groen;
de suite 1070 tests, 0 failures, 0 skipped. Elk document dat de klassen
spelen is uitgeschreven en gemeten: verifier PASS, ref PASS, clingo PASS,
behalve waar de tabellen hierboven het anders zeggen (V4: de lege deler en de
wortel).

**Sweep.** Over alle 290 goldens van `2a0c6d60`, elke golden met al zijn
cases, statusregel per golden en per tak (580 regels): de jar na V6
(`0bca28cc`), na V8 (`238c72ec`) en na V4 (`d2bf6685`) tegen de jar van
`2a0c6d60`: geen regel verschuift (exectak PLAYED 235, DRL-tak PLAYED 192). De
nieuwe golden speelt in de exectak (`PLAYED 2`) en weigert in de DRL-tak
(`TODO aggregatie EERSTE`).

**Pins** (`scripts/check-drools.sh`, voorstel): `EXPECTED_PLAYED` 235 → 236
met een redenblok; `DRL_CHANNEL` klasse (C) 13 → 14 regels met
`jutterswet-late-vondst|EXCLUDE|…`, de koppen en hertelzinnen bijgewerkt;
`EXPECTED_DRL_PLAYED` blijft 192.

**Gates.** Alle zestien in de volgorde van `gates.yml` op de kop `bf453803`
(de laatste codecommit; de commit met dit record raakt alleen `backlog/`),
`dune build` exit 0 vooraf, elk script onder zijn shebang; zeventien regels
`exit 0` en `DONE`:

     1. check-self-contained: PASS
     2. check-ir-schema: PASS (… 331 file(s) validate and round-trip, floor 192 …)
     3. check-case-schema: PASS (… 829 case file(s) validate)
     4. check-verifier: PASS (331 golden clean; 503 negatief rejected …)
     5. check-oracle: PASS (ref suite 1152 worked examples, floor 489; 787 cases replayed exactly, floor 427 …)
     6. check-clingo: PASS (backend suite 1988 checks, floor 83; 787 cases replay exactly through clingo …)
     7. check-noop: PASS (743 cases refute a do-nothing executor; 44 pinned survivors …)
     8. check-rood: PASS (42 red cases fault observably in ref and clingo, floor 26 …)
     9. check-catala: PASS (241 checks, floor 201; 12 catala golden(s) …)
    10. check-regelspraak: PASS (JUnit 1068 tests, floor 173; 118 rgs golden(s) re-emit byte-identically, floor 80)
    11. check-toka-parity: PASS
    12. check-drools: PASS (played 236, pin 236; DRL-text branch played 192, pin 192; DRL-text floor 99; JUnit 1070 tests, floor 239)
    13. check-reftype-derivation: PASS (41 unit annotations twisted over 23 goldens, floor 38 …)
    14. check-flogic: PASS (150 checks, floor 81 …)
    15. check-graaf: PASS (spec leg 254 of 291 goldens compared, 185 dependencies equal; route leg 118 goldens, 67 unit tests)
    16. check-scenario: PASS (25 scenario golden(s) regenerate 86 case file(s) …)

## Open punten

1. Clingo deponeert bij een default die terugvalt: met de lege deler meldt
   clingo één inconsistent feit, waar ref en RUNTIME-SEMANTICS ch. 7.1 geen
   depot laten staan. Laag `backends/clingo`; niet in deze ronde.
2. De algemene stap van V4 (dossier 5(f), open punt 6): een deling onder een
   omringende bewerking beslist met het depot; ref en clingo melden V3 5,00 en
   een inconsistentie, de exectak weigert benoemd. Een begeleider per default
   die het kanaal alleen vult als die default beslist, is niet gebouwd.
3. Een nuldeler faalt in de exectak met een `ArithmeticException` (ERROR), in
   een default zoals in de waarde van een regel; ref en clingo falen met naam
   ("division by numeric zero"). Luid in de drie, geen benoemde fout in de
   exectak.
4. Een unieselectie die via een rol van het element leest, weigert de exectak
   benoemd en de DRL-teksttak met haar marker; de join is voor een unie niet
   gebouwd.
5. De DRL-teksttak weigert ook vormen die daar toevallig goed uitkwamen (een
   creatie die het element bijwerkt; een kwantor waarvan het verre object het
   onderwerp is). Bewust conservatief: nooit stil.
6. Frontend: in de matrix van een kwantorvouw weigert de frontend "de sluitdag
   van de jutter van de Vondst" (`niet als navigatie gelegd`) en bindt zij "de
   sluitdag van zijn jutter". Luid; niet in deze laag.
7. De join op `Object` bereikt bij elke invoeging of bijwerking van een feit
   het joinknooppunt van elke zulke vouw; de kosten zijn lineair in het aantal
   elementen per wijziging. Niet gemeten op grote populaties.
8. De distributie-CLI draait op de pin `2a0c6d60` en kent deze ronde niet; de
   pin moet na het landen mee.
