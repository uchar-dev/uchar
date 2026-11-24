# concurrent-open-reads: de posities van een document zijn af voordat iemand ze leest

*Melding van de werkbank (een andere sessie), 2026-11-24; de bouw in een eigen worktree, de landing en de pin-bump van
de toolchain door de hoofdsessie.*

Goal: twee threads die tegelijk de posities van één geladen document lezen (`NodePositions.subtree`), krijgen hetzelfde
antwoord als één thread na de ander. De gedeelde tabellen van een geladen document en van een build zijn volledig
opgebouwd voordat de lader of de build ze teruggeeft; geen ervan wordt bij het eerste gebruik gevuld.

Acceptance: een test in de Drools-backend die op een vers geladen document veel threads tegelijk (vanaf één
`CyclicBarrier`) `subtree` laat vragen en elk antwoord met dat na elkaar vergelijkt, is rood vóór het herstel en groen
erna; de faalkans vóór en het aantal runs na staan in het `Result:`. De toolchain-test van dezelfde race
(`?openInputs=true` op een strikte Jutterswet-regelset, veel verzoeken tegelijk aan een verse dienst) is rood op
`62c393c94`/`f9919e9e3` en groen met dit herstel. Gates 12 en 17 groen, en de andere gates die de wijziging raakt, elk
apart.

May touch: `backends/drools/src/main/java/dev/uchar/regelspraak/loader/NodePositions.java` en `IrLoader.java`,
`backends/drools/src/main/java/dev/uchar/regelspraak/transpiler/tms/TmsTranspiler.java` (de pointers van de defaults),
een nieuwe test onder `backends/drools/src/test/java/dev/uchar/regelspraak/loader/`, deze kaart.

Out of scope: de spec, ref, clingo, het corpus; de toolchain (die krijgt het herstel als pin-bump, met haar eigen test).

## De melding

Bij vier gelijktijdige verzoeken met `?openInputs=true` aan een strikte dienst gaf één van 1334 verzoeken een 500:
`IllegalStateException: de motor liet /rules/rules/40/versions/0 op … open, en haar lezing noemt geen open lid` in
`OpenReads.of` van de toolchain, bij de eerste verzoeken na de start. Na elkaar gaf hetzelfde verzoek 200.

## Diagnose

`OpenReads` (toolchain) nummert de knopen van een vorm met `document.positions().subtree(root)` en zoekt met die
nummers de lezing van uchar op. `NodePositions.subtree` bouwt de preorde van het document lui, bij de eerste aanroep:

```java
if (inOrder == null) {
    inOrder = new ArrayList<>(nodes.keySet());   // het veld wijst al naar de ongesorteerde lijst
    inOrder.sort(Comparator.comparingInt(n -> nodes.get(n).order()));
}
```

Thread A zet `inOrder` op een lijst in de volgorde van de `IdentityHashMap` en sorteert haar daarna ter plekke; thread B
ziet `inOrder != null` en zoekt binair in een lijst die nog niet of maar half gesorteerd is. De zoektocht landt naast de
wortel, de lus breekt bij de eerste knoop die niet onder de wortel ligt, en `subtree` geeft een lege of te korte lijst.
De knopen van de voorwaarde hebben dan geen nummer, `Nodes.of` vindt geen lezing, niets is onbepaald, en `OpenReads.of`
werpt precies de fout van de melding.

Waarom alleen in de dienst en bij de eerste verzoeken: de dienst laadt het document voor de uitleg apart
(`Verbaliser.Document.of(IrLoader.loadWithPositions(tree))` in `DecisionCore`), en dat exemplaar van `NodePositions`
roept bij de start niemand aan; de build met lezingen leest haar eigen exemplaar al tijdens de build. De eerste
`subtree` op het exemplaar van de uitleg valt dus in een verzoek, en vallen twee verzoeken daar tegelijk, dan race.
Daarna staat de lijst en is er niets meer aan de hand.

Zusterplek op hetzelfde pad (de lezers van een sessie): `TmsTranspiler.pointerOf` vult de `IdentityHashMap` `pointers`
ook bij de eerste aanroep, zonder synchronisatie, en `SessionReaders.decisionsOf` roept haar per sessie aan. Vandaag
vult de build haar al (de compositie van elke stapel in `run()`), zodat ze in een sessie niet meer leeg is; dat rust op
een toevallige aanroep. Andere luie velden op dit pad: `TmsTranspiler.drl()` en zijn zusters zijn `synchronized`, de
caches van de runtime (`Accessors`, `RegelSpraakRuntime`) en `DrlRendering.NAME_PARTS` zijn `ConcurrentHashMap`s,
`PatternDslRendering.FIELDS` wordt alleen in een static-initialisatie gevuld, `Watch` en `Composite` bestaan per sessie
of per evaluatie.

## Herstel

- `NodePositions` krijgt haar preorde aan het eind van het laden (`IrLoader.loadWithPositions`), als onveranderlijke
  lijst; `subtree` bouwt niets meer, en een knoop toevoegen nadat de tabel af is, wordt geweigerd.
- `TmsTranspiler.pointers` wordt in de constructor gebouwd, uit de defaults die de lader gaf, en is daarna `final`.

## Result

Diagnose bevestigd zoals hierboven: het veld `NodePositions.inOrder` werd bij de eerste `subtree` gezet op de
ongesorteerde lijst en daarna ter plekke gesorteerd; elke request-thread van de dienst die op hetzelfde exemplaar (het
document van de uitleg, apart geladen in `DecisionCore`) tegelijk `subtree` vroeg, zocht in die lijst. De toolchain-test
geeft precies de melding van de werkbank (`de motor liet /rules/rules/2/versions/0 op Vondst open, en haar lezing noemt
geen open lid`, ook op een default en op een regel van de Jutter).

Commits (uchar, branch `card/concurrent-open-reads`, op `f9919e9e3`):

- `ee2666a9c` en `6be34dc9f`: deze kaart en haar claim;
- `4720652f6`: `NodePositions.complete()` maakt de preorde als onveranderlijke lijst; `IrLoader.loadWithPositions` roept
  haar aan voordat het de tabel teruggeeft; `subtree` vóór `complete` en een knoop of versie erna worden geweigerd. De
  zusterplek `TmsTranspiler.pointers` wordt in de constructor gevuld en is `final`; met de getuige
  `PositionsAcrossThreadsWitnessTest`.

Toolchain (branch `card/concurrent-open-reads`, op `3bb7941ce`): `3dbdc3e63`, `ConcurrentOpenInputsTest` in `service`:
per ronde een verse kern van `OpenValuesSource` (strikte Jutterswet-regelset), zestien verzoeken met `openInputs` vanaf
één `CyclicBarrier`, elk antwoord vergeleken met dat na elkaar; 200 rondes, 2,8 s.

De tests, rondes met een afwijkend antwoord:

| test | vóór het herstel | na het herstel |
|---|---|---|
| `PositionsAcrossThreadsWitnessTest` (8 threads, 200 rondes) | 174 van 200 | 0 in 5 runs (1000 rondes) |
| `ConcurrentOpenInputsTest` op uchar `f9919e9e3` | 36, 45 en 39 van 200 (3 runs) | — |
| `ConcurrentOpenInputsTest` op uchar `62c393c94` (de pin) | 42 van 200 | — |
| `ConcurrentOpenInputsTest` op uchar `4720652f6` | — | 0 in 5 runs (1000 rondes), en groen in de reactor |

De zusterplek `pointers` heeft geen rode test: de build vult haar vandaag al (de compositie van elke stapel in `run()`),
zodat ze in een sessie niet meer leeg is; het herstel haalt alleen het luie vullen weg.

Gates (uchar, elk apart in de worktree):

- 1: `check-self-contained: PASS`
- 12: `check-drools: PASS`, `played 389`, `DRL-text branch played 389`, `JUnit 2073 tests, floor 1573`
- 17: `check-readings: PASS (ref traced 1201 golden case files, …; on both branches 390 goldens and 1075 case files
  compared, 8091 readings, 25857 node lines …)`
- 15: `check-graph: PASS`
- 8: `check-red: PASS (101 red cases fault observably in ref and clingo, …)`
- 13: `check-reftype-derivation: PASS (129 unit annotations twisted over 42 goldens, …; 242 executor replays reproduce
  the untouched outcome, …)`

Toolchain, stap 1 tot en met 5 van `make gates-no-docker` apart, met de submodule lokaal op `4720652f6` (niet
gecommit): 1 `pin` rood, zoals verwacht (`de pin 4720652f6 staat niet op uchars main`); 2 `reactor` groen,
testgevallen per module 212/646/68; 3 `twins` groen (`3 tweelingen, 18 cases via HTTP …`); 5 `manifest` groen (`32
regels, 29 ok, 3 uitgesloten, 0 mismatch`); 4 `replay` rood op `380 goldens speelden, EXPECTED_REPLAY_PLAYED pint 379`:
de golden `jutterswet-keurder-strikt` van `f9919e9e3` (fold-order-in-readings) speelt nu door de dienst. Lokaal met
380 en 1062 in het script (daarna teruggezet): `check-service-replay: PASS`, `380 goldens (pin 380), 1062 cases (pin
1062)`, rode leg `67 cases met hun foutsoort (pin 67)`.

Open punten:

- De hoofdsessie landt `4720652f6` op uchar-main en bumpt daarna de pin van de toolchain; bij die bump gaan
  `EXPECTED_REPLAY_PLAYED` naar 380 en `EXPECTED_REPLAY_CASES` naar 1062 in `scripts/check-service-replay.sh` (de golden
  van fold-order-in-readings, niet van deze kaart), en komt `3dbdc3e63` mee, dat op de huidige pin rood is.
- Niet aangeraakt, want onschadelijk: `DecisionCore.document()` in de toolchain emitteert het document lui in een
  `volatile` veld; twee threads kunnen het dubbel emitteren, maar publiceren elk een volledige, onveranderlijke `String`.
- Niet herhaald met de regelset van de werkbank; de toolchain-test geeft dezelfde fout op een eigen regelset.
