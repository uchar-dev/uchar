# v1-wiremigratie-inventaris: de brekende lijst compleet en besloten

Maintainersbesluit 2026-09-18: de brekende v0→v1-wiremigratie wordt
voorbereid met één inventarisatiekaart, uit te voeren na het
zes-kaarten-pariteitsprogramma en de tekstlexeem-canonicalisatie. De
versieklap zelf is een eigen vervolgprogramma; deze kaart maakt hem
beslisbaar. Additieve delta's (nieuwe knopen naast bestaande vormen)
vallen erbuiten — die breken niets en landen gewoon door.

Aanleiding: v1 groeit al maanden additief (prioritizedDefaults,
Kwantorvouw, bronPad, Let/BinderRef, hiërarchie, vouwen), maar de
brekende kern-herspelling is opgespaard. Eén klap wil je één keer doen:
elke brekende delta dwingt alle emitters (RegelSpraak, Catala, F-logic),
alle consumenten (ref, clingo, Drools-loader, verifier), het schema, de
corpusbestanden en de toolchain-pin tot meebewegen. Nu is er één afnemer
op de pin; wachten maakt de migratie alleen duurder.

## Wat de kaart oplevert

Een proposal-document (`spec/proposals/`, beschermd-padvoorstel) met de
complete brekende lijst, per delta: de wire-wijziging, de geraakte
partijen, de migratienota, en een besluit-klare genummerde vraag waar
een keuze open is. Bekende kandidaten om te verifiëren en aan te vullen:

- de inerte bean-spiegelbagage eruit (`zetVeld`/`joinVeld` en verwanten;
  de symbolische opvolgers zijn al de enige gelezen vorm — IR-SPEC
  ch. 3.4-migratienota, CONSTRUCTS rij 5);
- `bron`-string naar het gestructureerde `Bron`-record (rij 3);
- typed values (de tekstlexeem-canonicalisatie,
  `20260918-tekstlexeem-canonicalisatie.md`, is de voorbode);
- alles wat een mechanische sweep over "v0", "mirror", "inert",
  "baggage", "tracked delta" in `spec/` en `CONSTRUCTS.md` verder
  aanwijst als brekend-maar-onbesloten.

Plus het volgordebesluit als expliciete maintainersvraag: de
`toka/`-parityzone verliest bij de klap zijn directe byte-anker tegen de
toolchain — zone opruimen vóór de migratie (met de
geschiedenis-herschrijving die de publieke flip toch al eist), of
mee-regenereren en pas bij de flip opruimen.

## Acceptatie (bij oppakken aanscherpen)

- (a) De lijst is aantoonbaar compleet: registervergelijking tegen
  `spec/CONSTRUCTS.md` (elke rij geclassificeerd
  additief/brekend/gedaan) plus de mechanische sweep, beide verantwoord
  in het document.
- (b) Elke brekende delta draagt wire-diff-schets, geraakte partijen,
  migratienota en waar nodig een genummerde beslisvraag; geen enkele
  staat als "nog uitzoeken".
- (c) Deze kaart wijzigt geen code, schema, corpus of gates; alle
  veertien gates blijven onaangeroerd groen. Het document is een
  proposal; besluiten neemt de maintainer op de genummerde vragen.
- (d) De uitkomst benoemt het vervolgprogramma in kaartvorm (schatting
  van de kaartenreeks), inclusief herpin- en
  emitter-golden-consequenties voor de toolchain.

## Result

Geland als **voorstel** op `card/v1-wiremigratie-inventaris` (beschermd pad:
`spec/proposals/` — de hoofdsessie merget na review). Deliverable:
`spec/proposals/V1-WIREMIGRATIE.md`. Geen code, geen schema, geen corpus, geen
gate aangeraakt; alle veertien gates draaiden ter bevestiging en stonden groen
(exitcode 0, driemaal: voor de landing, na de landing, en na de fixronde).

**Fixronde na de lens (dezelfde branch).** De lens vond vier fouten met één
signatuur — een zoekterm, citaat of tabelaanname als meting gepresenteerd zonder
tegenproef — en die zijn alle vier hersteld, elk met de tegenproef erbij. De
zwaarste: het sweeppatroon `**Delta` miste drie spellingen, waaronder
`**pending delta**` op IR-SPEC r. 2516 — en dat is de `bron`-string naar
gestructureerd `Bron`-record, naar voorkomens (4987) de **grootste delta van de
hele lijst**, nu B26. §2.2 draagt de correctie en de les: een zoekterm is geen
meting, en elke telling in het document draagt voortaan haar meetmethode.

### (a) De lijst is aantoonbaar compleet — de tellingen

**Registervergelijking**, elke rij van `spec/CONSTRUCTS.md` geclassificeerd:
**14 brekend** (rijen 1, 2, 3, 4, 5, 11, 16, 17, 20, 22, 25, 26, 27, 30),
**9 additief geland** (7, 8, 9, 10, 24, 36, 38, 39, 40),
**7 gedaan** (6, 14, 15, 19, 21, 23, 29),
**10 n.v.t. voor v1** (12, 13, 18, 28, 31, 32, 33, 34, 35, 37) — samen 40.
(Rij 17 verschoof van gedaan naar brekend: de bindingloze verdeling valt onder
hetzelfde vervallen van WF-048 als de bindingloze vouw. Rij 21 verschoof van
additief naar gedaan: haar deltakolom zegt "mirrored as-is".)
De knoopinventaris §2.1–§2.4 (12 `Expr`-subtypen, 9 `Conditie`-subtypen, 13
top-level vormen, 20 modellaagknopen) leverde vier items zonder eigen rij: de
`Regel`-stub, `Severity`/`Diagnostic`, `Rationaal` (staat niet op de wire) en
`Methode.COMPLEX`.

**Mechanische sweep** over `spec/` en het schema, elf zoektermen (indicatief,
met meetmethode erbij). Dichtste bron zijn de deltamarkeringen in IR-SPEC, en
die telde de eerste doorgang verkeerd: het patroon `**Delta` vond er 60, maar
IR-SPEC spelt de markering op **vier** manieren en drie daarvan beginnen niet
met hoofdletter-`D` — `**pending delta**` (r. 2516, vormdragend → B26),
`**Staged delta**` (r. 672, uitgesteld) en `**delta, promotion**` (r. 2444,
promotie). Het gecorrigeerde patroon vindt er **63**: 37 promoties, 3 v1-norm./
tight./widen., 2 new wire law, 1 diverge-from-v0, 1 staged — samen 44 niet
vormdragend — en **19 vormdragende**, één-op-één afgehandeld. Vier brekende
punten vielen buiten élke markering: de `regels`-stubs, het vervallen van
WF-048, de referentiepositie-`type`, en de globale hernoeming.
`ASP-TRANSLATION.md` levert nul brekende punten; `RUNTIME-SEMANTICS.md`'s 27
mirror-noten en 10 tracked deltas zijn semantisch zonder wire-gevolg.

**Consumentensweep.** De 193 propertynamen van het schema gekruist tegen de vier
consumenten. Dit corrigeert de premisse van de kaart: **geen enkel veld heeft
nul lezers.** De gedeelde OCaml-loader leest alle 193 (strikte sleutelset), de
Drools-loader 172. De spiegelvelden zijn **niet inert** — de verifier kruist ze
(WF-028, WF-034 (m)/(o)/(q), WF-054) en `backends/drools` **voert ze uit** in
beide takken. De loader-javadoc noemt de v0-bonenvelden GEZAGHEBBEND; zijn
eerste alinea noemt vier genegeerde symbolische uitbreidingen, maar **zijn
tweede alinea beperkt dat** ("TWO extensions escape that dichotomy") en de code
bevestigt het. Genegeerd en her-afgeleid zijn er **drie**:
`AttribuutRef.navigatieBindingen`, `Kenmerk.navigatieBinding` en de
verdelings-`verbindendeRol`. De vouw-`bronRol` (`IrLoader` r. 448-449) en de
objectcreatie-`verbindendeRol` (r. 623-624) worden wél gelezen, via de
zijkanaal-kaart `Loaded#rolBindingen`; de laatste is per RUNTIME-SEMANTICS
ch. 5.5 zelfs GEZAGHEBBEND. P3 in het vervolgprogramma is daardoor een derde van
wat hij leek.

### (b) De brekende lijst — 28 deltas in acht families

Dragers: groen = anchor 40 + golden 159 + rood 26 = 225; alle zones = 650
`ir.json` (+ 395 negatief, + 30 toka), plus 566 casusbestanden.

| # | Delta | Fam. | groen | alle | Vraag |
|---|---|---|---:|---:|---|
| B1 | formaatnaam + schema-id | A | 225 | 650 | V1 |
| B2 | zes kaart-tabellen → lijsten, zelfnaam weg | A | 225 | 639 | V3 |
| B3 | inerte `regels`-stubs eruit | A | 205 | 472 | — |
| B4 | `diagnostics` eruit | A | 225 | 650 | V2 |
| B5 | `Objecttype`-zijkaarten → ledenknopen | A | 225 | 639 | — |
| B6 | hernoeming NL→EN (56 knopen, 155 velden) | B | 225 | 650 | V2 |
| B7 | feitcreatie-spiegelvelden | C | 5 | 16 | — |
| B8 | `scopeVeld` → `scopeRol` verplicht | C | 7 | 19 | — |
| B9 | rollidmaatschapsknopen samensmelten | C | 3 | 8 | V4 |
| B10 | `Kenmerk.eigenaar` | C | 1 | 8 | — |
| B11 | `AttribuutRef`-navigatievelden + `set` | C | 201 | 537 | — |
| B12 | WF-048 vervalt, binding verplicht | C | 16 | 60 | — |
| B13 | referentiepositie-`type` van de wire | D | 206 | 567 | V5 |
| B14 | `Percentage` lost op | E | 4 | 47 | — |
| B15 | `DatumInDagen` + `DatumTijd` → `Date(p)` | E | 19 | 89 | — |
| B16 | `Onbekend` uit de datatypesom | E | 25 (→0 ná B13) | 71 | V6 |
| B17 | exacte factor + canonieke-eenheid-wire | E | 30 | 143 | V7 |
| B18 | `Parameter` volledige tijdlijnfacet | E | 32 | 134 | — |
| B19 | versie-afvlakking keert om | F | 190 | 501 | — |
| B20 | `Soort` klapt in | F | 190 | 501 | — |
| B21 | `Functie` splitst | F | 61 | 170 | — |
| B22 | literalen → getypeerde waarden | G | 148 | 368 | V8, V9 |
| B23 | `Constante` splitst | G | 4 | 10 | V10 |
| B24 | `Onopgelost` krijgt een reden | G | 1 | 2 | V11 |
| B25 | `Regelversie` — blijft hij toelaatbaar? | G | 0 | 0 | V13 |
| B27 | uniciteit + uniciteitsunie samensmelten | F | 10 | 24 | — |
| B28 | `ResolvedStartpunt` → ankermetadata | F | 1 | 6 | — |
| **B26** | **`bron`-string → `Bron`-record** | **H** | **208** | **551** | **V16** |

B26 is naar voorkomens de grootste van de lijst (4987 voorkomens) en naar
betekenis een van de kleinste: geen enkele uitvoerder leest `bron`. Niemand
staat als "nog uitzoeken": elke delta draagt wire-diff, geraakte partijen,
gemeten omvang en migratienota, en waar de richting niet vastligt een
genummerde vraag.

### (c) De genummerde vragen — zeventien

**V14 staat vooraan in het document**, niet omdat hij het laagste nummer heeft
maar omdat hij bepaalt wat de klap kost en wat gate 3 daarna nog bewaakt.

- **V1** — heet het formaat `regelspraak-ir-v1`, `uchar-ir-v1` of `uchar-ir-1.0`? (aanbeveling: `uchar-ir-v1`)
- **V2** — één klap of twee epochen (opruiming los van hernoeming)? (aanbeveling: één klap, mits de Drools-porten vooraf landen) — met deelvraag: blijft `diagnostics`? (aanbeveling: blijven, mét WF-regel)
- **V3** — blijft de topcontainer vlak of nestelt hij declaraties onder `model`? (aanbeveling: nesten)
- **V4** — hoe smelten de rollidmaatschapsknopen samen zonder getuige? (aanbeveling: eerst een getuige maken)
- **V5** — verlaat alleen de referentiepositie-`type` de wire, of ook de afgeleide? (aanbeveling: alleen referentieposities; de vierde klasse van ch. 2.10 heeft geen afleidingsregel)
- **V6** — wat draagt de AANTAL-telbron als `Onbekend` verdwijnt? (aanbeveling: niets — B13 lost het op)
- **V7** — landt de canonieke-eenheid-wire in deze klap? (aanbeveling: alleen de exacte factorvorm; de canonicalisatie wacht op de kruis-stelsel-divergentiekaart)
- **V8** — het enumeratie-dekkingsgat in de frontend nu sluiten, of bij de klap? (aanbeveling: nu — maar de omvang is "delimiters **én** typering", geen enkele emitterregel, en de andersom-optie kost de intrekking van WF-039)
- **V9** — worden literalen echte JSON-waarden? (aanbeveling: ja, in de vormen die `uchar-case-v0` al gebruikt)
- **V10** — krijgt `de dag` een eigen knoopsoort? (aanbeveling: eerst de dagsoortsemantiekkaart, dan ja)
- **V11** — gaat de `Onopgelost`-reden mee in deze klap? (aanbeveling: nee — additief is genoeg)
- **V12** — toka-zone opruimen vóór de migratie, of mee-regenereren? (aanbeveling: opruimen, als laatste stap vóór de klap)
- **V13** — blijft `Regelversie` in v1? (aanbeveling: schrappen; nul dragers, geen producent)
- **V14** — krijgt elke vervallende WF-regel een opvolgergetuige? **De grootste post**: 171 van de 395 negatief-mappen zijn getuige van een regel die de klap raakt (aanbeveling: per getuige een stempel — herschreven, vervangen, of verwijderd-met-reden; en `EXPECTED_MIN_NEGATIEF` in dezelfde commit omlaag)
- **V15** — gate 13 intrekken of herzien als de type-annotatie van de wire gaat? (aanbeveling: herzien tot een afgeleide-positie-harnas; een tweede uitzondering op de gate-monotonie hoort expliciet in `CLAUDE.md`, naast die van gate 11)
- **V16** — gaat het `Bron`-record in deze klap mee? (aanbeveling: ja, met `snippet` verplicht en `file`/`line`/`column` optioneel — anders blokkeren de 40 handgeschreven ankers en de 395 negatief-getuigen, die geen bronbestand hebben)
- **V17** — in welke taal staan de proposals? Dit is het eerste Nederlandse naast tien Engelse (aanbeveling: naar Engels vertalen mét vastgelegde conventie, maar pas nadat de besluiten gevallen zijn)

### (d) Het vervolgprogramma in drie zinnen

**Fase 1** is elf voorbereidende v0-kaarten die de klap mechanisch maken in
plaats van riskant — drie porten in `backends/drools` (navigatiebindingen,
bonenveldafleiding, en de verdelingsbinding, die na de loader-meting een derde
is van wat hij leek), twee emitterreparaties (de enumeratieliteral, die zowel
gestript als getypeerd moet worden, en het datum-tijd-lexeem), vier openstaande
beslissingen die al gekaart zijn, de extractie van de v0→v1-naamtabel uit
IR-SPEC, en als laatste het opruimen van de toka-zone met de
geschiedenis-herschrijving die de publieke flip toch al eist. **Fase 2** is de
klap in **acht** kaarten langs de families — schema en container, de
spiegelbagage, de typeannotatie, het typesysteem, de regellaag, de
expressielaag, de **bronverwijzing als eigen nulmeting** (551 documenten die
geen enkele uitvoerder leest: beweegt gate 5, 6 of 12 tóch, dan is de aanname
weerlegd), en als laatste de zuiver mechanische hernoeming — waarbij de grootste
post niet de 225 groene documenten zijn maar de **171 negatief-mappen** waarvan
de regel wordt geraakt (72 alleen al aan de typeannotatie), en waarbij gate 12's
`EXPECTED_PLAYED` bij elke kaart opnieuw moet worden vastgesteld omdat het een
gelijkheid is en geen vloer. **Fase 3** is de toolchain: herpin van `SPEC_REV`,
de her-vendor van de frontend-slice (die `backends/drools` als bronmap
meecompileert, dus emitter en loader delen één verzameling Java-records), de
twintig emitter-goldens van `uchar-conformance`, en de toolchain-lokale
scenariolaag die met B19 en B20 meebeweegt.

### Voorgestelde registerrijen (voor de hoofdsessie)

Drie, alle drie uitgeschreven in §9 van het voorstel; alle drie zijn in de
fixronde bijgesteld.

1. **De knoopinventaris §2.1** (de regels van `Begrensd` en `TekstReeks`) — niet
   de matrixrijen: dáár staat het `sugar`-oordeel dat zich laat lezen als
   "verdwijnt van de wire", terwijl IR-SPEC ch. 4.2 `Clamp` en `TextComposition`
   uitdrukkelijk als v1-knoop aanhoudt en WF-099 sinds 2026-09-10 aan de eerste
   hangt. (Matrixrij 1 noemt `Begrensd` wél, maar alleen in de WF-099-passage
   over de eenheid van een klemgrens — niet als oordeel.)
2. **Rij 27** — de gemeten stand van de enumeratiewaarde, en die is anders dan
   de eerste doorgang schreef: de wire is **gehekt** (WF-039 kruist het label
   tegen het domein — verifier-tegenproef gedraaid, `WF-039 FAIL … not in the
   declared domain`; en de emitterroute strandt sowieso eerder op WF-034 (r),
   omdat de frontend de literal niet typeert). Geen divergentie dus, maar een
   **frontend-dekkingsgat**, en de zin in IR-SPEC ch. 4.1 over de delimiters
   beschrijft de frontend en niet de wire.
3. **Rijen 4/5/22** — de autoriteitsverhouding in de Drools-loader, mét de
   begrenzing die zijn tweede javadoc-alinea aanbrengt: genegeerd en her-afgeleid
   zijn er **drie** (`navigatieBindingen`, `navigatieBinding`, de
   verdelings-`verbindendeRol`); de vouw-`bronRol` en de
   objectcreatie-`verbindendeRol` worden wél gelezen, en de laatste is per
   RUNTIME-SEMANTICS ch. 5.5 gezaghebbend.

### Eerlijk gemeld

- **De eerste doorgang presenteerde vier zoektermen, citaten en tabelaannames
  als metingen.** Alle vier zijn in de fixronde hersteld mét tegenproef: het
  deltapatroon (drie gemiste spellingen, waarvan één de grootste delta van de
  lijst was), het loader-citaat (de tweede javadoc-alinea beperkt de eerste),
  het enum-verhaal (WF-039 hekt de vorm wél — verifier-tegenproef gedraaid), en
  de zelfnaamtelling van B2 (2498 was een schatting; gemeten op de
  declaratiepositie is het 2277, en `domeinen` blijkt een andere vorm te hebben
  dan de vijf WF-094-tabellen). De les staat in §2.2 van het voorstel.
- De sweep-subagent voor `spec/` liep in een timeout; die doorgang is daarna
  handmatig gedaan — en dat is waar de gemiste deltaspellingen ontstonden.
- **B24** (`Onopgelost`-reden) is het enige item dat niet besluit-klaar te maken
  was zónder eerst werk te doen: IR-SPEC ch. 4.4 markeert de taxonomie als OPEN
  "until the negatief corpus fixes the taxonomy's values", en met twee
  corpusdragers valt er weinig te meten. V11 beslist daarom over *of* het
  meegaat, niet over de waarden — de aanbeveling is het additief te houden.
- De **v1-naamtabel is niet geëxtraheerd.** IR-SPEC draagt elke Engelse naam met
  een `v0 name:`-regel eronder, maar verspreid over ch. 2–4 en niet
  machineleesbaar. Dat is kaart P10 van fase 1 en het is echt werk, geen
  formaliteit.
