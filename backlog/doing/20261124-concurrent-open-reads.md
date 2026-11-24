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
