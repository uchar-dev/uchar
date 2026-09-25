# kenmerkinvoer-en-invoer-boven-berekening: wat `niet` tegenover invoer betekent, en een oppervlak voor "opgegeven, niet afgeleid" (vorm 3)

Besluit 7 van de onderzoekskaart dialect-defaults-niet-en-nesting
(maintainersbesluit 2026-09-23): een eigen kaart. Bronnen: het rapport
buiten de repo
(`../research/dialect-defaults-niet-en-nesting/onderzoek-niet-in-uitzonderingen.md`,
§4.4 flank 2, §8.4, §9 besluit 7) en het voorstel
`spec/proposals/DIALECT-DEFAULTS-NIET-EN-NESTING.md` §6 (kaartbranch).

Gemeten (ref ≡ clingo ≡ Drools-exec): een `besluit: false` neemt een
doelkenmerk dat in de casusinvoer staat nergens weg. ref zet
invoerkenmerken bij laden en kent geen intrekking; clingo zendt ze als kale
feiten `heeft(label,k).` uit, ook op een defaults-doel
(`backends/clingo/lib/translate.ml:8797`; invoer*attributen* op een
defaults-doel gaan wél via `inputval/3`, r. 8793); de Drools-groepsregel
schrijft bij `false` niets (`ExecModelTranspiler.java:7649`). Dat is
consistent met ch. 7.1 ("not a retraction") en met §3.5 van de spec
(invoer mag een kenmerk zetten, regels voegen toe), maar ch. 7.8's virtuele
invoer-default is voor kenmerkdoelen in de onwaar-richting leeg, terwijl hij
voor attributen wél door een beslissende default wordt verdrongen
(ch. 7.6.2). Daarnaast wil de werkbank afgeleide waarden als invoer kunnen
aannemen en een afleiding laten overrulen (Catala's `context`, Reading B,
PHASE5-CATALA besluit 6); dat is vandaag alleen via een subscope-aanroep
bereikbaar, niet vanuit de casusinvoer.

## Te beslissen door de maintainer (de kaart levert de analyse en een voorstel)

- (7-i) Vastleggen wat is: ch. 7.8 krijgt de zin dat de virtuele
  invoer-default op een kenmerkdoel door geen besluit wordt verdrongen,
  omdat de groep nooit `onwaar` schrijft; de ankercase
  `case7-invoer-staat-tegen-blokkade` (kaart
  anker-ww-herleving-dragervoorwaarde) pint het. Nul code.
- (7-ii) Kenmerkinvoer defeasibel maken (Reading A ook voor kenmerken):
  clingo `heeft(S,k) :- inputchar(S,k), not grpG_eff(S)` op defaults-doelen;
  ref houdt invoerkenmerken van defaults-doelen apart en laat ze alleen
  staan als de groep niet beslist; Drools idem. Een getrackte delta
  (ch. 7.8, `CONSTRUCTS.md`), drie uitvoerders, de ankercase krijgt
  verwachting afwezig.
- (7-iii) Een oppervlak voor Reading B: een markering op het doel ("mag
  opgegeven worden") die uit de casusinvoer een hoogste default genereert,
  zoals het Catala-frontend dat voor `context` via de subscope-aanroep doet;
  met een uitlegkanaal ("opgegeven, niet afgeleid") en een keuze wat er
  gebeurt als feiten en opgave elkaar tegenspreken (een §9.5-feit, of stil
  de opgave). Per attribuut of per groep.

(7-ii) en (7-iii) staan los van elkaar; (7-i) is de minimale vastlegging.

## Acceptatie (bij oppakken aanscherpen)

- (a) Een voorstel (sectie in `DIALECT-DEFAULTS-NIET-EN-NESTING.md` of eigen
  bestand in `spec/proposals/`) met de drie opties, de wisselwerking met
  ch. 7.8 en PHASE5-CATALA besluit 6, de spelling van de markering voor
  (7-iii), en de gemeten stand per uitvoerder.
- (b) De maintainersbesluiten vastgelegd; de zin voor ch. 7.8 als
  **voorstel** (beschermd pad); de bouwkaarten voor de gekozen opties
  gesneden.
- (c) Geen code; alle gates ongewijzigd groen.

May touch: `spec/proposals/**`, `spec/RUNTIME-SEMANTICS.md` (**voorstel**,
één zin), `backlog/**`.

Niet in deze kaart: de uitvoering van (7-ii) of (7-iii).

## Correcties bij de claim (2026-09-25, hoofdsessie)

De kaart is op 2026-09-23 op de L1-wire geschreven; de repo staat op de
L5-wire (`uchar-ir-v1` compleet, main 2f47d80e).

- Regelnummers (`translate.ml:8797`, `ExecModelTranspiler.java:7649`) zijn
  van 2026-09-23; zoek op `inputval`/`inputchar` en `compileDefault`.
- Wire-namen: `besluit` → `Default.decision`; `Default {decision?,
  justification?, consequence?, initialisation?, exceptions?}`;
  `PrioritizedDefaults {name, subject, characteristic | attribute,
  unordered?, defaults}`; `ALLE` → `Compound {quantifier: "all"}`; `bron` →
  `source {snippet}`. De ASP-predicaten (`inputval/3`, `heeft/2`) en de
  Drools-namen zijn geheugennamen, geen wire-namen.
- Het voorstel DIALECT-DEFAULTS-NIET-EN-NESTING.md is geland op main; §6
  (r. 920–990) geeft de richtingen; §10 besluit 1 BEHIELD "een negatieve
  beslissing verwijdert nooit een invoerkenmerk" (RegelSpraak §3.5
  invoer-of-toekenning).
- De ankercase `case7-invoer-staat-tegen-blokkade` bestaat
  (`corpus/golden/anchor/ww-herleving-inkomen/`, tien cases).
- Het rapport buiten de repo: §4.4 r. 105–128, §8.4 r. 410–419, §9
  r. 420–435 van
  `../research/dialect-defaults-niet-en-nesting/onderzoek-niet-in-uitzonderingen.md`.
- Het voorstel komt in een EIGEN bestand
  `spec/proposals/KENMERKINVOER-EN-INVOER-BOVEN-BEREKENING.md` (niet in
  DIALECT-DEFAULTS-NIET-EN-NESTING.md: dat bewerkt de kaart
  dialect-uitzondering-op-uitzondering tegelijk).

## U-besluiten (hoofdsessie, 2026-09-25; standaardantwoorden, aan de maintainer gemeld)

- U1 **(7-i) JA.** RS ch. 7.8 krijgt één zin (**voorstel**): de virtuele
  invoer-default op een KENMERKdoel wordt door geen beslissing verdrongen —
  een `decision: false` schrijft niets (ch. 7.1: geen intrekking; RegelSpraak
  §3.5) — anders dan op een attribuutdoel, waar een beslissende default de
  invoerwaarde verdringt (ch. 7.6.2); `case7-invoer-staat-tegen-blokkade`
  pint het. Geen D-/U-nummers, geen procestaal.
- U2 **(7-ii) NEE.** Kenmerkinvoer blijft niet-defeasibel: dat is precies wat
  besluit 1 van 2026-09-23 behield. Het voorstel legt de asymmetrie en haar
  reden vast en noemt (7-ii) als verworpen alternatief, met wat het zou
  kosten (ch. 7.8-delta, drie uitvoerders, de ankerverwachting omgekeerd).
- U3 **(7-iii) VOORSTEL, geen bouw.** Suiker over het hardheidspatroon
  (PHASE4-DEFAULTS §4.1; de `context`-lowering van het Catala-frontend,
  PHASE5-CATALA besluit 6): een markering op de attribuutdeclaratie in het
  dialect (spelling voorstellen, met de inertheidsvraag), die de frontend
  lowert tot een hoogste default die de casusinvoer via een tweede attribuut
  `opgegeven <naam>` leest — géén wire-verandering, géén ch. 7.8-wijziging;
  uitleg = de naam van het beslissende blok; tegenspraak feiten/opgave = de
  gematerialiseerde berekende waarde plus een consistentieregel (§9.5-feit).
  Het voorstel weegt ook de semantische variant (een invoer-default bóven de
  gewone defaults, per doel) en zet haar als alternatief neer. De bouwkaart
  voor de suikervorm staat als TEKST in het Result (de hoofdsessie snijdt
  hem; bouwers maken geen kaartjes); of hij vóór of ná de pin-bump komt is
  het sein van de maintainer.
- U4 **Gemeten stand** per uitvoerder (`ref`, clingo, Drools exec en DRL) op
  het WW-anker case7 en op een klein eigen proefdocument (L5-wire) met een
  `decision: false` tegenover een invoerkenmerk, commando's en uitkomsten in
  het voorstel; proefbestanden buiten de repo.
- U5 Nul code; gate 1 en de naamtabel-toets als sanity; de unie draait alle
  veertien.
