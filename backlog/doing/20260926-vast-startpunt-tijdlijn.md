# vast-startpunt-tijdlijn: een tijdlijn met een vast startpunt buiten de kalenderperiode krijgt een veld op de wire

IR-SPEC ch. 2.8 (`Timeline`: `granularity`, `count`, `variableStart`) zegt
uitdrukkelijk dat de wire geen vast startpunt draagt: een tijdlijn met
`variableStart: false` ligt op het kalenderraster (RS ch. 3.2, "Fixed
starting points"). RegelSpraak kent wél tijdlijnen met een vast startpunt
buiten dat raster (§13.3.6.3–.7, "startend op …"); de frontend resolvet ze
(`RegelSpraakModel.vasteStartpunten`) en de emitter weigert ze met een
ERROR ("tijdlijn: het vaste startpunt '…' wijkt af van de kalenderperiode
en de wire heeft er geen veld voor — de zin wordt geweigerd in plaats van
het startpunt stil te laten vallen", `IrEmitter.wireWeigeringen`). De oude
toolchain droeg de vorm in het geheugen; de maintainer besloot op
2026-09-26 dat de wire haar nu draagt. Spec-first: dit is een deltakaart
(`spec/CONSTRUCTS.md`-rij door de hoofdsessie bij de landing).

## U-besluiten (hoofdsessie, 2026-09-26, bij de claim)

- U1 De bouwer meet eerst welke startpuntvormen §13.3.6.3–.7 kent (een
  absolute datum, een terugkerende dag-maand, iets anders; de
  specificatie is read-only en wordt niet geciteerd) en welke de frontend
  vandaag in `vasteStartpunten` zet; de kaart draagt ze alle op de wire.
- U2 IR-SPEC ch. 2.8: één optioneel veld op `Timeline` (naam **voorstel**
  `fixedStart`, vorm door de bouwer: een strikte ISO-datum voor een
  absolute start, een `--MM-DD`-achtige dag-maand voor een terugkerende
  start — de bouwer kiest één spelling en verantwoordt haar), met een
  nieuwe MUST (**voorstel** WF-1xx, nummer door de bouwer als eerstvolgende
  vrije): `fixedStart` alleen bij `variableStart: false`, welgevormd,
  passend bij de granulariteit (een dag-maand bij jaar/maand, geen dag bij
  een dagtijdlijn); verifier-arm met één zin, negatief rood-eerst,
  `expect.txt`, NOTE uit echte uitvoer; schema-bijwerking in canonieke
  sleutelvolgorde (gate 2).
- U3 RS ch. 3.2: het raster van zo'n tijdlijn is de reeks perioden van
  `count` granulariteitseenheden vanaf het startpunt (en dezelfde reeks
  terug in de tijd), worked example → `ref/test`. Alle drie de uitvoerders
  bucketen erop (ref, clingo, beide Drools-takken); de frontend emitteert
  het veld en `wireWeigeringen` verliest deze arm.
- U4 Golden `jutterswet-vast-startpunt` (**voorstel**): een tijdlijn
  "startend op" een datum buiten het kalenderraster met een attribuut
  erop, regels die per periode schrijven en lezen, cases rond de
  periodegrens; ref ≡ clingo ≡ Drools. Gate 13 (twist) en gate 7 per de
  vaste patronen; gate 12 +1 op beide takken.
- U5 Pariteitszone: alleen tellen; als de casusdump de vorm draagt, is de
  herpin in deze kaart (README-regel zonder inhoud uit de zone).
- U6 Klaar: alle veertien gates groen; naamtabel-toets rc 0; gelijke
  zinnen in verifier, ref, clingo en de Drools-lader zonder
  regelnummerstaart; L5-spelling.

## Acceptatie

- (a) U1–U6; (b) een document met het veld is verifier-schoon en speelt in
  de drie uitvoerders; een document met het veld op een `variableStart`-
  tijdlijn wordt geweigerd met de WF-zin; (c) `git grep "wire heeft er geen
  veld"` leeg.

May touch: `spec/IR-SPEC.md` ch. 2.8, `spec/RUNTIME-SEMANTICS.md` ch. 3.2,
`spec/schema/`, `spec/verifier/` (alle **voorstel**), `ref/**`,
`backends/clingo/**`, `backends/drools/**`, `frontends/regelspraak/**`,
`corpus/golden/jutterswet-vast-startpunt/`, `corpus/negatief/wf1xx-*`,
gatepins (**voorstel**), `toka/` alleen bij een herpin. Niet in deze kaart:
`spec/CONSTRUCTS.md` en `spec/PLAN.md` (hoofdsessie bij de landing).

## Result (2026-09-26, bouwer)

**Stand.** Basis `b88a8ccc`, claim `3c6e1ce6`, kop: de commit die deze noot toevoegt, direct op `24f54f1e`, werkboom schoon.
Niet gepusht, niet gemerged.

**Commits** (oud naar nieuw):
`91af3064` spec: het vaste startpunt van een tijdlijn krijgt een veld op de wire ·
`f82a3018` verifier, ref en clingo: WF-115 en het raster van een vast startpunt ·
`144685ab` frontend: het vaste startpunt wordt geemitteerd in plaats van geweigerd ·
`629ad051` drools en golden: een doel op een vast startpunt buckt op zijn anker ·
`91dc9274` frontend: een benoemde tijdlijn zonder vast startpunt geeft er geen door ·
`5817204f` dagtelling buckt op het raster van het doel, ook in de tekst ·
`8a6ae3c6` drl: alleen een variabel anker houdt de som over een verzameling buiten ·
`24f54f1e` getuigen: pro-rata over een vast startpunt is omheind in alle takken ·
en de commit met deze Result-noot.

### Per U-besluit

**U1 — de vormen, gemeten.** Gelezen: de grammatica van de frontend
(`grammar/RegelSpraak.g4`, producties `vastStartpuntDatum` §13.3.6.3, `vastStartpuntMaand` §13.3.6.4,
`vastStartpuntKwartaal` §13.3.6.5, `vastStartpuntJaar` §13.3.6.6, `meervoudigePeriode` §13.3.6.7 met een
datum), `DefinitionResolver` op de basis, en de specificatie v2.3.0 (read-only, niet geciteerd). Vier vormen:
een absolute datum (week en meervoudige periode), een dag van de maand, een dag van drie-maandelijkse maanden
(de specificatie kent drie maandgroepen, de grammatica laat elke vier getallen toe), een dag van een maand (jaar).
Vier posities: de benoemde tijdlijndefinitie en inline op attribuut, kenmerk en parameter. `vasteStartpunten`
registreerde op de basis elke datumvorm, maand met dag ≠ 1, kwartaal ≠ (dag 1; 1, 4, 7, 10), jaar ≠ (1, 1);
alle vier de vormen op alle vier de posities staan nu op de wire (`VastStartpuntTest`, 16 tests).

**U2 — het veld en WF-115.** `fixedStart` op `Timeline` (IR-SPEC ch. 2.8) en, per JSON-pointer naar
datzelfde veldschema, op de drie facetvormen (`TimelineFacet`, `CharacteristicTimelineFacet`,
`ParameterTimelineFacet`): de inline vormen noemen geen tijdlijn, en een kenmerk- of parameterfacet kán er geen
noemen (B1 hieronder). Twee lexemen, onderscheiden door hun eerste teken: een datum `yyyy-mm-dd` (WF-084's
lexeem) op een weekraster en een raster van count ≥ 2; een dag-maand `--mm-dd` (de `gMonthDay`-vorm van XML
Schema) op een maand-, kwartaal- of jaarraster van count 1, canoniek gespeld met de vroegste maand van het jaar
die op dag `dd` zelf een muur heeft (maand → `--01-dd`; dag 31 van maand 2, 5, 8 en 11 → `--05-31`); het
kalenderstartpunt `--01-01` wordt nooit geschreven. WF-115 (eerstvolgende vrije nummer op de basis) met drie
armen: (a) geen `fixedStart` op een variabele-starttijdlijn, (b) het lexeem dat het raster neemt, (c) een facet
dat een tijdlijn noemt draagt diens `fixedStart`. Schema: `fixedStart` na `count` (vóór `timelineName`), geen
patroon (B3). Rood eerst, gemeten vóór de arm bestond maar mét de lader die het veld leest: `verifier: PASS` op
alle drie de getuigen (`wf115-vast-startpunt-bij-variabel`, `-buiten-raster`, `-niet-overgenomen`, elk de golden
met één bewerking). Na de arm: elk weigert op zijn eigen arm (`expect.txt` pint het fragment). Mutatieproef: met
één arm uit de verifier gehaald wordt precies de eigen getuige `PASS`, de andere twee blijven op hun arm vallen
(voor (a), (b) en (c) gemeten, `checks.ml` daarna byte-gelijk teruggezet).

**U3 — het raster en de uitvoerders.** RUNTIME-SEMANTICS ch. 3.2 "Fixed starting points" herschreven: het
raster is de reeks perioden van `count` eenheden vanaf het startpunt en dezelfde reeks terug, elke muur direct uit
het startpunt (clamp van ch. 3.8), met zeven uitgewerkte voorbeelden die letterlijk in `ref/test` staan
(`WV-3.2-vast-*`). ref: één functie `Timeline.grid_bucket` voor de caselader (invoer, verwachting, parameter),
de asOfDate-bucket en de dagtelling; clingo's bucketkalender roept dezelfde functie aan; beide Drools-takken
geven een doel op een vast startpunt het constante anker aan de verankerde producenten van de variabele start;
de caserunner legt een casetijdlijn op het raster via de gedeelde `kern/VastStartpunt`. De frontend emitteert het
veld; `wireWeigeringen` had alleen deze arm en is met zijn twee aanroepen verdwenen.

**U4 — de golden.** `corpus/golden/jutterswet-vast-startpunt`: een benoemd stormjaar vanaf 1 oktober
(`--10-01`, door twee facetten overgenomen), een inline maandraster vanaf de 31e (`--01-31`), een kenmerk op
een weekraster vanaf `2024-09-30`; twee dagtellingen en een totaal met gedurende-knip. Vier cases, met de hand
berekend in de NOTE: de periodegrens van 1 oktober (3 en 2, kalender zou 5 geven), een heel stormjaar (365; 13
maandperioden; totaal 120), de schrikkelmaand (2 en 2; 4), geen wacht (lege tijdlijnen, 0). ref 4× `oracle:
PASS (4 expectations)`, clingo 4× `clingo: PASS (4 expectations)`, Drools exec-model `PLAYED 4`, DRL-tekst
`PLAYED 4`. Mutatieproef op de golden: zonder elke `fixedStart` is het document verifier-schoon en weigert ref
elke case bij het laden (`segment boundaries must sit on the declared granularity grid`, geciteerd in de NOTE).
Gate 12: `EXPECTED_PLAYED` 209 → 210 en `EXPECTED_DRL_PLAYED` 179 → 180, elk met redenblok; de golden speelt
in beide takken en staat dus in geen niet-spelerslijst (geen klassekop geraakt). Gate 7: geen overlever (alle vier
de cases weerleggen de no-op-uitvoerder). Gate 13: het document draagt geen eenheid, dus niets te twisten.

**U5 — de pariteitszone, alleen geteld.** De casusdump draagt de vorm niet (0 keer "startend op" onder
`toka/casus`); één testbestand van de zone heeft 7 zulke regels, alle op modelniveau (`Resolver.resolve`), die
groen blijven. 29 replay-slices: 0 leden op een variabele-starttijdlijn, 0 meervoudige facetten. Geen herpin;
gate 11 ongewijzigd (45 bronnen byte-gelijk, 27 tests, 28 slices verifier-schoon, 29 cases in ref, clingo,
Drools).

**U6 — klaar.** Alle veertien gates groen (tabel hieronder); naamtabel-toets rc 0 (een rij `fixedStart`
toegevoegd, 314 propertyrijen); de WF-115-zinnen zijn in verifier, ref en clingo één gedeelde functie
(`Checks.vast_startpunt_refusal` en zusters, door `Loader.document` bij een uitvoerder bij het laden
gesproken) en in de Drools-lader een letterlijke kopie (`kern/VastStartpunt`), gepind door
`VastStartpuntWitnessTest` (Drools) en `VastStartpuntKernTest` (frontend); geen regelnummerstaart; L5-spelling.

### Technische beslissingen (zelf genomen)

B1. Het veld staat ook op de facetten, met overname zoals granulariteit en telling (WF-115 (c)); anders kon een
inline startpunt op een kenmerk of parameter niet mee. B2. Eén spelling per vorm, canoniek (vroegste maand;
`--01-01` verboden), zodat één raster één spelling heeft; de frontend normaliseert een datum op een raster van
één maand, kwartaal of jaar naar zijn dag-maand en laat een datum op een dagraster van één eenheid weg (elke
daggrens is daar al een muur). B3. Geen schemapatroon: dat vroeg een rij in de patroontabel van het beschermde
gate-2-script; het precedent is `count` (vorm bij de verifier). B4. In het OCaml-AST rijdt de vaste start van
een lid mee in de verbrede telbare tabellen (`attribuut_tijdlijn_aantal : (string * tijdvak) list`), omdat de
Catala- en F-logic-frontends het `objecttype`-record heel bouwen en buiten "May touch" liggen. B5. Waar een
uitvoerder het raster van een lid leest, weigert hij een raster van meer eenheden en een variabele start luid;
ref en clingo namen daar eerder stil het kalenderraster aan. B6. Pro-rata op of vanaf een vast-startraster is
omheind in alle takken. B7. De gehele-periode-kwalificatie houdt haar eigen kalendereenheid (normvraag N1). B8.
De uitvoerders weigeren WF-115-bevindingen bij het laden. B9. Een parameter over meer perioden weigert de
frontend in de zin van WF-005: het model van een parameter heeft geen telling om te schrijven (was: bij een
datumstart geweigerd om het startpunt, via een benoemde tijdlijn stil tot 1 teruggebracht). B10.
`wireWeigeringen` verdwenen, niet leeg achtergelaten.

### De zinnen, letterlijk

WF-115 (a): `timeline '<naam>' has a variable start and a fixedStart '<s>': its periods run from the anchor its
start-point determination binds per object, and a timeline has one start`
WF-115 (b): `fixed start point '<s>' does not fit a <granularity> grid of count <n>: <reden>`, met als reden
één van: `a day grid has a wall at every day boundary and takes no start point` · `a week grid and a grid of
several units start on a date, written yyyy-mm-dd, the ISO form of a day of the calendar` · `a month, quarter or
year grid of one unit starts on a day-month, written --mm-dd, a day of month mm in a leap year` · `it lays the
calendar's own grid, which is written without a fixedStart` · `its canonical spelling is <--mm-dd>, with the
earliest month of the year that has a wall on that day`
WF-115 (c): `named-timeline adoption is partial: fixed start point <'s'|none> on the facet, <'s'|none> on
timeline '<naam>'`
Omheining (ref, clingo): `pro-rata onto or from a grid that does not lie on the calendar is fenced
(RUNTIME-SEMANTICS ch. 3.2)`; Drools-reden: `pro-rata op of vanaf een raster dat niet op de kalender ligt (vast
startpunt) is omheind — RUNTIME-SEMANTICS ch. 3.2`.
ref-raster: `a multi-period grid (<n> <granularity> periods) carries no operation (RUNTIME-SEMANTICS ch. 3.2,
OPEN in ch. 3.12)` · `a variable-start grid is laid per object from the anchor its start-point determination
binds, which this interpreter does not execute (ch. 3.2)`; caselader: `<pos>: the declared grid is <n>
<granularity> units, a multi-period grid, which carries no operation, a case input included (RUNTIME-SEMANTICS
ch. 3.2, OPEN in ch. 3.12): refused as a v0 limit, not as a defect of the case` · `<pos>: the declared grid is
laid per object from the anchor of a variable start, which this loader does not read (RUNTIME-SEMANTICS ch.
3.2): refused as a v0 limit, not as a defect of the case`.
Frontend (ERROR, Nederlands): `tijdlijn: het vaste startpunt '<bron>' (§13.3.6.3) draagt een tijdstip, en een
tijdvak begint op een dag` · `… (§13.3.6.3) noemt geen dag van de kalender` · `… (§13.3.6.4) noemt geen dag
van de maand` · `… (§13.3.6.5) noemt geen reeks kwartaalmaanden: die is 1, 4, 7 en 10, 2, 5, 8 en 11 of 3, 6,
9 en 12` · `… (§13.3.6.6) noemt geen maand` · `… (§13.3.6.6) noemt een dag die maand <m> niet heeft`; en
`parameter '<naam>': the timeline facet of parameter '<naam>' has count <n> > 1: a parameter's timeline is one
period while multi-period grids are open (RUNTIME-SEMANTICS ch. 3.12)` (WF-005's zin; `LezerszinGrensTest`
groen).

### Gates (eindronde op `24f54f1e`, logs `/tmp/golf/logs/start/final-*.log`; basis `base-*.log`)

| gate | uitkomst | tellingen, basis → eind |
|---|---|---|
| 1 self-contained | PASS | — |
| 2 ir-schema | PASS | 307 → 308 bestanden (vloer 192); 37935 → 38138 objecten in 788 → 792 documenten in sleutelvolgorde, 1 in geen subschema (ongewijzigd); patroon- en vormrijen ongewijzigd |
| 3 verifier | PASS | golden schoon 307 → 308; negatief 481 → 484 (vloer 350); op hun arm 324 → 327 (vloer 260); pointers 584 → 587 (vloer 547) |
| 4 case-schema | PASS | 752 → 756 casebestanden |
| 5 oracle | PASS | ref-suite 1047 → 1075 (vloer 489); 709 → 713 cases |
| 6 clingo | PASS | suite 1878 → 1883 (vloer 83); 709 → 713 cases |
| 7 noop | PASS | 665 → 669 weerleggen; 44 overlevers (ongewijzigd) |
| 8 rood | PASS | 43 rode cases, 86 fragmenten (ongewijzigd) |
| 9 catala | PASS | 241 checks, 12 goldens (ongewijzigd) |
| 10 regelspraak | PASS | 764 → 770 tests; 93 → 94 rgs-goldens (vloer 80) |
| 11 pariteitszone | PASS | 45 bronnen byte-gelijk, 27 tests, 29 slices, 28 verifier-schoon, 29 cases in ref, clingo en Drools (ongewijzigd) |
| 12 drools | PASS | played 209 → 210 (pin); DRL-tak 179 → 180 (pin); auto-excluded 52 (ongewijzigd); JUnit 814 → 817 |
| 13 reftype | PASS | 41 twists over 23 goldens (vloer 38), 106 replays, 22 in de DRL-tak (vloer 20), 0 overlevers; basis niet schoon gemeten (open punt 9) |
| 14 flogic | PASS | 150 checks, 2 goldens (ongewijzigd) |
| naamtabel-toets | rc 0 | propertyrijen 313 → 314 |

### Voorgestelde registertekst

**CONSTRUCTS, rij `Tijdlijn`** (achter de bestaande tekst): **Landed (2026-09-26, card
`vast-startpunt-tijdlijn`):** the wire carries the fixed start point — `fixedStart` on `Timeline` and on the
three timeline facets (IR-SPEC ch. 2.8): a date `yyyy-mm-dd` on a week grid or a grid of count ≥ 2, the
canonical day-month `--mm-dd` on a month, quarter or year grid of count 1, the calendar start point written as
none; WF-115 (fits its grid; none on a variable start; adopted by a facet that names the timeline); the grid in
`RUNTIME-SEMANTICS.md` ch. 3.2 (walls computed from the start point, periods before it as after it). The
RegelSpraak frontend emits every form it refused at emission and refuses by name only a source that names no
day; ref, clingo and both Drools branches lay case timelines on the fixed grid and bucket the day count on it
(golden `jutterswet-vast-startpunt`, negatief `wf115-*`); the pro-rata over a fixed-start grid is fenced in
all of them. **Delta (addition).**
**CONSTRUCTS, rij 11** (v0→v1-delta, één zin erbij): the fixed start point off the calendar is a `Timeline`
and facet field since 2026-09-26 (WF-115, RUNTIME-SEMANTICS ch. 3.2).
**PLAN §9** (in de golf-zin): "… and a fixed start point off the calendar grid as a `Timeline` field
(`vast-startpunt-tijdlijn`, landed 2026-09-26: `fixedStart` with WF-115, executed by all four executors)."
**CLAUDE.md**: geen regel — er kwam geen gate bij en geen gate veranderde van betekenis.

### Normvraag (standaardantwoord toegepast)

N1. "gedurende het gehele jaar" over een lid op het stormjaarraster: het kalenderjaar dat de kwalificatie
noemt, of de periode van het lid? Toegepast: het kalenderjaar (ch. 3.9 onveranderd; ch. 3.2 zegt het zo). Een
andere keuze raakt ch. 3.9, waar de parallelle kaart `gehele-periode-leeg-gevuld` werkt.

### Open punten

1. OCaml-AST (B4): de vaste start van een ledenfacet rijdt mee in de verbrede telbare tabellen
   (`attribuut_tijdlijn_aantal`, `kenmerk_tijdlijn_aantal`, elementtype `tijdvak`). Eigen velden
   `attribuut_vast_startpunt`/`kenmerk_vast_startpunt` vragen een kaart die `frontends/catala` en
   `frontends/flogic` mag raken (vier recordliteralen).
2. Buiten de letterlijke "May touch"-lijst, elk een gevolg van de kaart en elk een voorstel op een beschermd pad:
   IR-SPEC ch. 2.2 (één rij in de `TimelineFacet`-tabel) en de WF-indexrij; RUNTIME-SEMANTICS ch. 3.8 (één
   bijzin: de dagtelling buckt op het raster van het doel) en ch. 3.12 (de rij "Fixed start point");
   `spec/schema/README.md` (twee zinnen); `spec/proposals/v1-naamtabel.json` (één rij, nodig voor U6).
3. Variabele start: ref en clingo weigeren een variabel-startraster nu luid waar ze een raster lezen (caselader,
   asOfDate-bucket, dagtelling, pro-rata), waar ze eerder stil het kalenderraster namen; de Drools-backend voert de
   verankerde dagtelling en pro-rata voor zo'n doel uit (bestaand) en weigert een casetijdlijn erop nu ook. Geen
   corpusdrager (gemeten: de 29 slices van de pariteitszone zetten geen lid op een variabele-starttijdlijn,
   `jutterswet-vormenkabinet` heeft geen case). Het vierkant voor het variabele anker is een eigen kaart.
4. Meervoudige rasters: caseinvoer en bucketing weigeren count ≥ 2 in ref, clingo en de Drools-caserunner (ref en
   clingo negeerden de telling). Geen corpusdrager (gemeten: geen ledenfacet met count ≥ 2 in golden, rood of de
   pariteitszone).
5. De asOfDate-bucket op een vast raster (een scalaire casewaarde) heeft geen corpusdrager: voor een periode van
   één eenheid is het totaal gelijk aan dat van het kalenderraster (één hele eenheid), dus een golden onderscheidt
   ze niet. De periode zelf is gepind (`WV-3.2-vast-*`, `VastStartpuntKernTest`); de lijm in ref's collapse en de
   scalaire route van de Drools-caserunner draaien niet eind-tot-eind.
6. Pro-rata over een vast-startraster is omheind; opheffen vraagt een nestregel (het raster van de basis genest in
   dat van het doel) met een eigen getuige.
7. WF-115 is het eerstvolgende vrije nummer op `b88a8ccc`; een parallelle kaart kan hetzelfde nummer nemen — bij
   de unie nakijken.
8. Acceptatie (c) letterlijk: `git grep "wire heeft er geen veld"` treft nog twee grootboekregels, het
   probleemverhaal van deze kaart en de meting in `backlog/done/20260922-tijdlijn-startpunt-emissie.md`; buiten
   `backlog/` is de uitkomst leeg. Als geschiedenis gelaten.
9. De basislijn van gate 13 is aan het begin niet schoon gemeten: mijn gelijktijdige bewerkingen maakten de jar
   tijdens die run verouderd (geen rode basis, wel een onbruikbare meting); aan het eind groen.
10. De grammatica van de frontend laat elke vier getallen als kwartaalgroep toe; de resolver weigert een niet-groep
    bij naam (§13.3.6.5 kent er drie). De grammatica is ongewijzigd gelaten.
11. Het vaste startpunt van een kenmerk wordt gedragen maar door geen uitvoerder gelezen (de tijdlijn van een
    kenmerk is zijn ware perioden); vastgelegd in de NOTE van de golden.
12. Testbestanden van ref, clingo, de frontend en de Drools-backend zijn gewijzigd of toegevoegd; de kaart
    mandateert het (U3's worked examples, het herschrijven van de weigertest, de nieuwe getuigen).
