# tms-verdeling-en-tijdlijnen: de truth-maintenance-tak draagt verdelingen, tijdlijnen en dagsoorten

*Vastgelegd door de hoofdsessie op 2026-10-08, na het besluit van de eigenaar
op het `Result:` van `20261007-drools-logische-afleiding` (PLAN §9,
2026-10-08). De derde van vier bouwkaarten, na
`20261008-tms-expressievormen-en-rekendatum`; geen haalbaarheidsproef.
Tellingen uit de corpusdekking van de proeftak in dat `Result:` (bij (e)).*

## Aanleiding

Na de eerste twee bouwkaarten blijven drie groepen weigeringen over die de
exec-modeltak speelt:

- **verdeling** (12): `Allocation` naar rato en op volgorde, met de degradaties
  van RUNTIME-SEMANTICS ch. 4.3 en 4.4 op het kanaal;
- **tijdlijnen**: een tijdsafhankelijk attribuut (9), `wholePeriod` in een
  `CharacteristicCheck`, `Comparison` of `Predicate` (3), tijdsafhankelijke
  kenmerken en parameters, en `StartPointDetermination`;
- **dagsoorten**: een duur in dagsoorten (3), `DayKindDefinition` (2) en
  `BoundDay`.

Het `Result:` noemt een verdeling een berekening over een gebonden
verzameling, en een tijdlijn een waarde in een standfeit. De tijdlijnalgebra
van ch. 3 staat al in de runtime, en de staande tijdlijndoelen van ch. 7.8 in
`transpiler/kern/StaandeDoelen` (`staandTemporeel`).

Goal: de tak draagt verdelingen, tijdlijnen en dagsoorten; daarna weigert hij
geen golden meer die de exec-modeltak speelt, zodat
`20261008-tms-omschakeling` kan beginnen.

## Wat de tak erbij krijgt

1. **Verdeling**: de verdeling als berekening over de verzameling die de LHS
   bindt (de ontvangers met hun gewichten, ook afgeleide); de aandelen als
   logische feiten; de degradaties als feit op het kanaal.
2. **Tijdlijnen**: een tijdlijn als waarde in een standfeit; afgeleide
   tijdsafhankelijke attributen en kenmerken; `wholePeriod`; de staande
   tijdlijndoelen van ch. 7.8; `StartPointDetermination`; tijdsafhankelijke
   parameters.
3. **Dagsoorten**: `DayKindDefinition` met `BoundDay`, en een duur in
   dagsoorten.

## Acceptatie

- (a) Rood-eerst getuigen, elk in elke volgorde van de betrokken regels in het
  model:
  - een verdeling naar rato en een op volgorde, met een gewicht dat later
    afgeleid wordt;
  - twee verdelingen met één gedeelde ontvanger (de vorm van
    `20260923-drl-verdeling-dubbelbezet`, die de exec-modeltak draagt);
  - een degradatie op het kanaal;
  - een tijdsafhankelijk attribuut dat uit tijdsafhankelijke invoer wordt
    afgeleid, en een waarvan de invoer blijft staan (ch. 7.8);
  - `wholePeriod` in een voorwaarde;
  - een dagsoortdefinitie met een duur in die dagsoort.
- (b) De corpusgetuige heeft geen FAIL; de vloer gaat naar het gemeten aantal.
  Een doorloop van beide takken over het hele corpus toont dat de tak elke
  golden speelt die de exec-modeltak speelt (240 bij het schrijven van deze
  kaart), met dezelfde namen; het `Result:` geeft die doorloop en noemt elke
  golden die nog verschilt, met de reden.
- (c) De grep van de eerste kaart over `transpiler/tms/` geeft niets.
- (d) De exec-modeltak en de DRL-teksttak zijn ongewijzigd: gate 12 groen met
  beide pins gelijk; alle gates van `CLAUDE.md` groen.

## Raakvlakken

- Bouwt op de eerste twee bouwkaarten (aggregaties, fouten als feit,
  schrijvers, rekendatum).
- `20260923-drl-verdeling-dubbelbezet` is een fout in de destructieve
  DRL-teksttak; die gaat op in `20261008-drl-teksttak-op-truth-maintenance`.
  Hier dient zijn vorm alleen als getuige.
- Een golden die na (b) nog verschilt, gaat niet naar een lijst: de omschakeling
  begint pas als er geen verschil meer is.

## Review

Zoals in de eerste bouwkaart: een lens na elke bouwstap, een eindlens over de
hele diff, na elke stap een doorloop van het hele corpus, en een lensvondst
alleen met een concreet faalscenario op echte invoer of een getuige.

## Omvang

Middelgroot. De runtime heeft de verdeling en de tijdlijnalgebra al; het werk
zit in het binden van de verzamelingen en tijdlijnen in de LHS.

May touch: `backends/drools/src/main/java/dev/uchar/regelspraak/transpiler/tms/`,
`cases/CaseRunner.java` en `cases/DroolsBackend.java` voor zover het terugzetten
van tijdlijnen dat vraagt, tests onder `backends/drools/src/test/java/`, de
sectie over de tak in `backends/drools/README.md`, kaartverplaatsing. NB:
serieel met andere Drools-kaarten.

Out of scope: de exec-modeltak en de DRL-teksttak, `transpiler/kern/`,
`scripts/`, `corpus/`, `spec/`, de toolchain.
