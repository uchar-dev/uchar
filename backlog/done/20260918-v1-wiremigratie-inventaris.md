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
`spec/proposals/V1-WIREMIGRATIE.md`, 1783 regels. Geen code, geen schema, geen
corpus, geen gate aangeraakt; alle veertien gates draaiden ter bevestiging en
stonden groen (exitcode 0, tweemaal: voor en na de landing).

### (a) De lijst is aantoonbaar compleet — de tellingen

**Registervergelijking**, elke rij van `spec/CONSTRUCTS.md` geclassificeerd:
**13 brekend** (rijen 1, 2, 3, 4, 5, 11, 16, 20, 22, 25, 26, 27, 30),
**10 additief geland** (7, 8, 9, 10, 21, 24, 36, 38, 39, 40),
**7 gedaan** (6, 14, 15, 17, 19, 23, 29),
**10 n.v.t. voor v1** (12, 13, 18, 28, 31, 32, 33, 34, 35, 37) — samen 40.
De knoopinventaris §2.1–§2.4 (12 `Expr`-subtypen, 9 `Conditie`-subtypen, 13
top-level vormen, 20 modellaagknopen) leverde vier items zonder eigen rij: de
`Regel`-stub, `Severity`/`Diagnostic`, `Rationaal` (staat niet op de wire) en
`Methode.COMPLEX`.

**Mechanische sweep** over `spec/` en het schema, twaalf zoektermen. Dichtste
bron: de **60 `**Delta`-markeringen in IR-SPEC**, uitputtend geclassificeerd —
36 `(promotion)` (WF-verscherping, geen wire-vorm), 3 `(v1 normalisation/
tightening/widening)`, 2 `(new wire law)` en 1 `(diverge-from-v0)` zijn geland
en niet brekend; **18 vormdragend** en één-op-één afgehandeld. Vier brekende
punten vielen buiten die markeringen en kwamen alleen uit het register en de
`inert`/`baggage`/`retires`-sweep: de `regels`-stubs, het vervallen van WF-048,
de referentiepositie-`type`, en de globale hernoeming. `ASP-TRANSLATION.md`
levert nul brekende punten; `RUNTIME-SEMANTICS.md`'s 27 mirror-noten en 10
tracked deltas zijn semantisch zonder wire-gevolg.

**Consumentensweep.** De 193 propertynamen van het schema gekruist tegen de vier
consumenten. Dit corrigeert de premisse van de kaart: **geen enkel veld heeft
nul lezers.** De gedeelde OCaml-loader leest alle 193 (strikte sleutelset), de
Drools-loader 172. De spiegelvelden zijn **niet inert** — de verifier kruist ze
(WF-028, WF-034 (m)/(o)/(q), WF-054) en `backends/drools` **voert ze uit** in
beide takken. De loader-javadoc zegt het zelf: de v0-bonenvelden zijn
GEZAGHEBBEND, en vier symbolische uitbreidingen (`navigatieBindingen`,
`navigatieBinding`, een vouw-`bronRol`, een verdelings-`verbindendeRol`) worden
genegeerd en uit het model her-afgeleid.

### (b) De brekende lijst — 25 deltas in zeven families

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
| B17 | exacte factor + canonieke-eenheid-wire | E | 31 | 143 | V7 |
| B18 | `Parameter` volledige tijdlijnfacet | E | 32 | 134 | — |
| B19 | versie-afvlakking keert om | F | 190 | 501 | — |
| B20 | `Soort` klapt in | F | 190 | 501 | — |
| B21 | `Functie` splitst | F | 61 | 170 | — |
| B22 | literalen → getypeerde waarden | G | 148 | 368 | V8, V9 |
| B23 | `Constante` splitst | G | 4 | 10 | V10 |
| B24 | `Onopgelost` krijgt een reden | G | 1 | 2 | V11 |
| B25 | `Regelversie` — blijft hij toelaatbaar? | G | 0 | 0 | V13 |

Niemand staat als "nog uitzoeken": elke delta draagt wire-diff, geraakte
partijen, gemeten omvang en migratienota, en waar de richting niet vastligt een
genummerde vraag.

### (c) De genummerde vragen

- **V1** — heet het formaat `regelspraak-ir-v1`, `uchar-ir-v1` of `uchar-ir-1.0`? (aanbeveling: `uchar-ir-v1`)
- **V2** — één klap of twee epochen (opruiming los van hernoeming)? (aanbeveling: één klap, mits de Drools-porten vooraf landen) — met deelvraag: blijft `diagnostics`? (aanbeveling: blijven, mét WF-regel)
- **V3** — blijft de topcontainer vlak of nestelt hij declaraties onder `model`? (aanbeveling: nesten)
- **V4** — hoe smelten de rollidmaatschapsknopen samen zonder getuige? (aanbeveling: eerst een getuige maken)
- **V5** — verlaat alleen de referentiepositie-`type` de wire, of ook de afgeleide? (aanbeveling: alleen referentieposities; de vierde klasse van ch. 2.10 heeft geen afleidingsregel)
- **V6** — wat draagt de AANTAL-telbron als `Onbekend` verdwijnt? (aanbeveling: niets — B13 lost het op)
- **V7** — landt de canonieke-eenheid-wire in deze klap? (aanbeveling: alleen de exacte factorvorm; de canonicalisatie wacht op de kruis-stelsel-divergentiekaart)
- **V8** — enum-delimiters nu als v0-kaart, of bij de klap? (aanbeveling: nu, naar het tekstlexeem-precedent)
- **V9** — worden literalen echte JSON-waarden? (aanbeveling: ja, in de vormen die `uchar-case-v0` al gebruikt)
- **V10** — krijgt `de dag` een eigen knoopsoort? (aanbeveling: eerst de dagsoortsemantiekkaart, dan ja)
- **V11** — gaat de `Onopgelost`-reden mee in deze klap? (aanbeveling: nee — additief is genoeg)
- **V12** — toka-zone opruimen vóór de migratie, of mee-regenereren? (aanbeveling: opruimen, als laatste stap vóór de klap)
- **V13** — blijft `Regelversie` in v1? (aanbeveling: schrappen; nul dragers, geen producent)

### (d) Het vervolgprogramma in drie zinnen

**Fase 1** is elf voorbereidende v0-kaarten die de klap mechanisch maken in
plaats van riskant — drie porten in `backends/drools` (navigatiebindingen,
bonenveldafleiding, vouw- en verdelingsbinding), twee lexeemcanonicalisaties
(enumeratiewaarde, datum-tijd), vier openstaande beslissingen die al gekaart
zijn, de extractie van de v0→v1-naamtabel uit IR-SPEC, en als laatste het
opruimen van de toka-zone met de geschiedenis-herschrijving die de publieke flip
toch al eist. **Fase 2** is de klap in zeven kaarten langs de families — schema
en container, de spiegelbagage, de typeannotatie, het typesysteem, de regellaag,
de expressielaag, en als laatste de zuiver mechanische hernoeming — waarbij de
grootste post niet de 225 groene documenten zijn maar de **136 negatief-mappen**
waarvan de regel zijn onderwerp verliest (alleen al 57 aan de typeannotatie),
en waarbij gate 12's `EXPECTED_PLAYED` bij elke kaart opnieuw moet worden
vastgesteld omdat het een gelijkheid is en geen vloer. **Fase 3** is de
toolchain: herpin van `SPEC_REV`, de her-vendor van de frontend-slice (die
`backends/drools` als bronmap meecompileert, dus emitter en loader delen één
verzameling Java-records), de twintig emitter-goldens van `uchar-conformance`, en
de toolchain-lokale scenariolaag die met B19 en B20 meebeweegt.

### Voorgestelde registerrijen (voor de hoofdsessie)

Drie, alle drie uitgeschreven in §9 van het voorstel: (1) rij 1 en rij 31 —
het `sugar`-oordeel op `Begrensd`/`TekstReeks` betreft de brontaal, niet de
wire (IR-SPEC ch. 4.2 houdt `Clamp` en `TextComposition` als v1-knoop aan);
(2) rij 27 — de gemeten enumeratiewaarde-divergentie: de RegelSpraak-emitter
schrijft `'label'`, de Catala-plugin en elke handgeschreven wire schrijven
`label`, alle vier consumenten vergelijken verbatim, en nul van de 650
documenten dragen de gedelimiteerde vorm; (3) rijen 4/5/22 — de gemeten
autoriteitsverhouding in de Drools-loader, die de v0-bonenvelden gezaghebbend
noemt en vier symbolische uitbreidingen negeert.

### Eerlijk gemeld

- De sweep-subagent voor `spec/` liep in een timeout; die doorgang is daarna
  handmatig gedaan (de twaalf-termen-tabel en de uitputtende classificatie van
  de 60 `**Delta`-markeringen in §2.2 van het voorstel zijn het resultaat).
- **B24** (`Onopgelost`-reden) is het enige item dat niet besluit-klaar te maken
  was zónder eerst werk te doen: IR-SPEC ch. 4.4 markeert de taxonomie als OPEN
  "until the negatief corpus fixes the taxonomy's values", en met twee
  corpusdragers valt er weinig te meten. V11 beslist daarom over *of* het
  meegaat, niet over de waarden — de aanbeveling is het additief te houden.
- De **v1-naamtabel is niet geëxtraheerd.** IR-SPEC draagt elke Engelse naam met
  een `v0 name:`-regel eronder, maar verspreid over ch. 2–4 en niet
  machineleesbaar. Dat is kaart P10 van fase 1 en het is echt werk, geen
  formaliteit.
