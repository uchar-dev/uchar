# De kenmerkTijdlijnAantal-poort is containsKey, niet "twee of meer"

Goal: de Drools-poort op de genavigeerde kenmerktijdreeks (§8.2/ch. 3.9)
weigert vandaag elk document dat `kenmerkTijdlijnAantal` voor dat kenmerk
*draagt*, ook bij de waarde **1** — een wire-legale, verifier-schone,
semantisch enkelvoudige count. Bedoeld is de meerdere-tijdlijnen-poort; wat
er staat is een aanwezigheidspoort. Latente over-afwijzing: vandaag
onzichtbaar omdat de frontend de sleutel bij 1 nooit schrijft, maar elke
handgebouwde of door een andere producent geschreven IR loopt erin.

## De vindplaats

Beide takken, identiek:

- `backends/drools/src/main/java/…/transpiler/DrlTranspiler.java`,
  `navigatieKenmerkTijdreeks`
- `backends/drools/src/main/java/…/transpiler/model/ExecModelTranspiler.java`,
  `navigatieKenmerkTijdreeks`

```java
if (doel == null || !doel.kenmerkGranulariteit.containsKey(k.kenmerk())
        || doel.kenmerkTijdlijnAantal.containsKey(k.kenmerk())) return null;
```

De derde disjunct is de poort. `null` betekent hier: de vorm wordt niet
gelowerd — de exec-model-tak levert een `transpiler SKIP (voorwaarde buiten
Phase-B scope)`, de DRL-tak een `MARKER TODO §8.2 gehele-periode`. Beide
luid, geen van beide onjuist bij een écht meervoudig raster; allebei
onterecht bij `count: 1`.

Waarom het niet opvalt: de emitter schrijft de sleutel alleen bij een andere
waarde dan 1 —
`frontends/regelspraak/src/main/java/…/resolver/DefinitionResolver.java`:

```java
if (aantal != 1) ot.kenmerkTijdlijnAantal.put(naam, aantal);
```

Een frontend-document draagt hem dus nooit bij 1, en het corpus evenmin. De
poort is daarmee onbereikbaar via de toolchain en volledig bereikbaar via de
hand — precies de asymmetrie die een latente fout oplevert.

## Hoe het gemeten is (2026-09-11)

Gevonden doordat het een meting op kaart `leegrelatie-ch39-divergentie`
verkéérd liet uitvallen: de handgebouwde fixture droeg
`"kenmerkTijdlijnAantal": {"bedrijvig": 1}`, beide Drools-takken weigerden,
en dat is als "Drools draagt de kenmerkvorm niet" gerapporteerd. Het
tegendeel is waar.

Meetrecept (reproduceert in twee minuten):

1. Neem `corpus/golden/synth-gehelejaarkenmerk/ir.json` — vier cases, beide
   takken PLAYED 4.
2. Zet in `objecttypen.Werf` de sleutel `kenmerkTijdlijnAantal` op
   `{"bedrijvig": 1}`. Verder niets.
3. `scripts/check-verifier.sh <ir>` → PASS (de sleutel is welgevormd).
4. `ref` en clingo → onveranderd PASS op alle vier de cases (de semantiek is
   identiek; beide lezen `kenmerkGranulariteit` en niets anders).
5. `java -jar backends/drools/target/uchar-drools-backend-*-shaded.jar <ir>
   <cases>` en dezelfde aanroep met `--drl` → beide slaan om naar EXCLUDE.

Eén sleutel, één waarde, geen semantisch verschil, twee uitvoerders minder.

## Wat er te beslissen valt

1. Is de poort bedoeld als `>= 2`? Dan is het een eenregelige correctie in
   beide takken, plus een getuige (de fixture uit stap 2 als tweede case-set,
   of een unit-test in de backend-suite).
2. Of is `aantal` bij 1 überhaupt geen wire-vorm? Dan hoort de *schema*- of
   WF-kant hem te weren, niet de backend stil. Let op de spiegelvraag: WF-005
   draagt `aantal` als modellaagveld, en ch. 3.2 verklaart elk multi-period
   raster (`count` >= 2) OPEN — dus "1" is juist de enige gedefinieerde
   waarde, wat voor lezing 1 pleit.
3. Dezelfde `containsKey`-vorm bestaat op de *attribuut*-kant
   (`attribuutTijdlijnAantal`) — meenemen in dezelfde meting, niet aannemen.

## Acceptance

- Een uitspraak welke van de twee lezingen geldt, met de meting erbij.
- Bij lezing 1: beide takken gecorrigeerd, met een getuige die rood was en
  groen wordt; gate 12 groen met `EXPECTED_PLAYED` ongewijzigd (de bestaande
  golden speelt al).
- Bij lezing 2: de weigering verhuist naar schema/verifier, met een
  negatief-case, en de backends verliezen hun stille tak.
- Het corpus mag de sleutel bij 1 daarna niet stil dragen: wat de beslissing
  ook is, één van beide kanten moet hem opmerken.

May touch: `backends/drools/`, `spec/schema/`, `spec/verifier/` (voorstel),
`corpus/negatief/`, `backlog/`.

## Out of scope

- De ch. 3.9-semantiek zelf: geland op `leegrelatie-ch39-divergentie`
  (2026-09-11), met `corpus/golden/synth-gehelejaarkenmerk` als getuige in
  alle drie de uitvoerders.
- Multi-period rasters (`count` >= 2) semantiek geven: ch. 3.2/3.12 OPEN, een
  eigen kaart.
