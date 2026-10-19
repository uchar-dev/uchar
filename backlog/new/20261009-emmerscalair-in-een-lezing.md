# emmerscalair-in-een-lezing: ref en clingo lezen een asOfDate-emmerscalair als geen tijdlijn waar ch. 8.4 een fout vraagt

*Gevonden op 2026-10-09 tijdens `20261008-tms-verdeling-en-tijdlijnen` (een lens op stap 2 en de bouwers van de
fixronde van stap 2 en van stap 3) en door de hoofdsessie nagemeten en vastgelegd volgens de afspraak dat een bestaande
bug in ref of clingo een kaart krijgt. Gemeten met ref, clingo en de verifier uit `_build` van main `ac0f9008` en de
shaded jar van die kaartbranch.*

## Aanleiding

RUNTIME-SEMANTICS ch. 8.4 houdt de twee invoervormen van een tijdsafhankelijk attribuut strikt apart. De
asOfDate-emmerscalair dient het totaal over de eigen tijdlijn, de volledige tijdlijn dient de lezingen, en: "An executor
that needs the one and finds the other faults — never guesses". Ref en clingo doen dat bij de knip, bij een lid van een
vouw, bij de ooit-sleutel en bij het eigen totaal over een volledige tijdlijn. In drie lezingen doen ze het niet: daar
lezen ze de emmerscalair als geen tijdlijn.

| Lezing (document, verifier PASS) | ref en clingo | tak met truth maintenance |
|---|---|---|
| de basis van een pro rata (`p1-scalaire-basis`: jaartarief 366 als scalair, rekendatum 2024-06-01, borgplichtig maart 2024 – maart 2025) | schrijven niets | fout: "the pro-rata base "jaartarief" needs the full timeline; "J1" supplies only the asOfDate-bucket scalar (ch. 3.7, ch. 8.4)" |
| de vergeleken tijdlijn van een knip ("het totaal van zijn vergoeding gedurende de tijd dat zijn controle …", controle 7 als scalair) | totaal 0 | fout: "the gedurende clip of "vergoeding" compares "controle" per segment and needs its full timeline; "J2" supplies only the asOfDate-bucket scalar (ch. 3.6, ch. 8.4)" |
| "gedurende het gehele jaar" over een emmerscalair langs een pad ("de havendagen van de werf van de Schuit") | de toets slaagt (werftoeslag 10) | fout: "the whole-period qualifier over "havendagen" needs the full timeline; the werf of "S1" supplies only the asOfDate-bucket scalar (ch. 3.9, ch. 8.4)" |

De oude Drools-takken antwoorden overal, omdat `CaseRunner` de emmerscalair als segment op de boon zet (bewust; zie de
javadoc van `temporeleInvoer`); bij de pro rata geven ze 306. De documenten: `/tmp/lens-tms-k3/proef/lens2/invoer/`, en
uitgeschreven in `TmsInvoervormWitnessTest` en `TmsGeheelPeriodeWitnessTest` (deel E).

## Acceptatie (bij oppakken aanscherpen)

- (a) Bevestigen dat ch. 8.4 ook voor deze drie lezingen geldt, of de spec preciseren (beschermd pad: voorstel). Ook
  het moment van de fout: de tak faalt zodra de pro rata wordt uitgerekend, ook als de voorwaarde geen ware periode
  heeft.
- (b) Ref en clingo gerepareerd, rood eerst, met een rood geval per lezing in `corpus/red/` (ref en clingo met hun zin).
- (c) Alle gates groen.

## Omvang

Klein.

May touch: `ref/`, `backends/clingo/`, `corpus/red/<nieuwe gevallen>/`, `spec/RUNTIME-SEMANTICS.md` (voorstel),
kaartverplaatsing.
