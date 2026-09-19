# frontend-stille-markers: hoort een Onopgelost op een kwantorpositie luid?

Uit de niettemporele-vouw-kaart (2026-09-19): op de lidmaatschapsroute
van de kwantorvouw zijn vier weigervormen een STILLE
`Onopgelost`-marker — kale `DE`, een §8.1.7-rolcheck van het element,
een meerstapsbereik en een onbindbaar bereik — terwijl drie zusters
luid zijn (telwoord < 1, §8.2-gekwalificeerde matrix, niet-bindende
die/dat). Bestaand markergedrag, door die kaart niet toegevoegd; maar
een zin die als kwantificatie leest en zonder één diagnose tot een
marker vervalt, is precies de stilteklasse die de diedat-lijn elders
dichtzette.

De vraag is frontend-breed (de markerdiscipline: wanneer volstaat de
eerlijke `Onopgelost` en wanneer hoort er een diagnose naast) en dus
een maintainersbesluit — niet per vorm hier beslissen.

## Acceptatie (bij oppakken aanscherpen)

- (a) De inventaris: alle stille `Onopgelost`-producties van de
  frontend (grep + klassering: draagt de route een diagnose ja/nee),
  niet alleen de vier van de vouw.
- (b) De genummerde maintainersvraag: diagnoseplicht bij elke
  Onopgelost-productie (en met welke severity), of de stille marker
  als gedocumenteerde klasse met haar motivering.
- (c) De uitvoering volgt het besluit; getuigen per gewijzigde route;
  alle veertien gates groen.

May touch: `frontends/regelspraak/**`, kaartverplaatsing; spec-teksten
alleen als voorstel.

## Maintainersbesluit (2026-09-19): diagnoseplicht, WARNING

Elke `Onopgelost`-productie van de frontend krijgt een diagnose
ernaast met severity WARNING: het document blijft emitteerbaar — de
marker blijft de eerlijke wire-vorm — maar de schrijver ziet het.
Eerst de (a)-inventaris over de hele frontend, dan de uitvoering per
route met getuigen.

## Result (2026-09-19)

Besluit uitgevoerd. Elke `Onopgelost`-productie van de frontend draagt nu
een diagnose; de marker zelf is onveranderd, de severity is WARNING, en de
emissie faalt er niet op. Veertien gates groen, geen enkele corpus-golden
bewoog.

### (a) De inventaris — 32 productieplekken, 13 luid, 19 stil

Gezocht op `new Conditie.Onopgelost(` / `new Expr.Onopgelost(` plus de twee
`onopgelost(…)`-helpers van `ExpressieBouwer`; dat zijn alle plekken in de
frontend die een marker maken (`IrEmitter` serialiseert er alleen een,
`DefinitionResolver`, `Typering` en `BeslistabelDesugar` maken er geen).
Regelnummers van de eindtoestand.

**`resolver/RuleChecker.java` — 15 plekken, 2 luid, 13 stil**

| regel | route | was | nieuw |
|---|---|---|---|
| 1662 | `bouwVoorwaarde` valt door: geen §13.4.13-vorm | stil | "het voorwaardendeel heeft geen van de §13.4.13-vormen — geen samengestelde voorwaarde, geen elementaire voorwaarde met operator, geen regelpredicaat en geen periodevergelijking" |
| 1728 | `bouwSamengesteld`: bullets kolom-/tekeninconsistent (§13.4.14.6) | stil | "de bullets van de samengestelde voorwaarde staan op verschillende kolommen of dragen verschillende tekens, dus welke bullet onder welke hoort ligt niet vast (§13.4.14.6)" |
| 1825 | `bouwUitKnopen`: hernestte bullet is geen van beide vormen | stil | "deze bullet van de hernestte opsomming is geen geneste samengestelde voorwaarde en geen elementaire voorwaarde (§13.4.14)" |
| 1920 | `samengesteldeElementFilter`: tellende kwantor over een diepere geneste voorwaarde | stil | "een tellende kwantor over een deelselectie waarvan een bullet zelf nog een diepere samengestelde voorwaarde draagt … dan telt de selectie een andere verzameling dan de zin noemt" |
| 1969 | `bouwCriterium` valt door | stil | "dit geneste consistentiecriterium is geen samengesteld criterium en geen elementaire voorwaarde (§13.4.8)" |
| 2011 | `bouwGenest` valt door | stil | "deze bullet van de samengestelde voorwaarde is geen geneste samengestelde voorwaarde en geen elementaire voorwaarde (§13.4.14)" |
| 2028 | `bouwElementair` valt door | stil | "de elementaire voorwaarde heeft geen van de §13.4.15-vormen — geen expressie met operator, geen periodevergelijking en geen regelpredicaat" |
| 2124 | `uitToplevelKaal`: geabsorbeerde 'niet' op vergelijking/leeg/gevuld | stil | `REDEN_GEABSORBEERDE_NIET` |
| 2144 | `uitToplevelKaal` valt door: naam bindt geen kenmerk/rol | stil | `REDEN_GEEN_KENMERK_GEEN_ROL` |
| 2170 | `keurGeheelPeriode`: §8.2 over een statisch onderwerp | **luid** (ERROR) | `reden = null` |
| 2449 | `uitNonToplevelKaal`: geabsorbeerde 'niet' | stil | `REDEN_GEABSORBEERDE_NIET` |
| 2480 | `uitNonToplevelKaal` valt door | stil | `REDEN_GEEN_KENMERK_GEEN_ROL` |
| 2500 | `kenmerkPredicaat`: §8.2-kwalificatie over een rolcontrole | stil | "de controle 'gedurende de/het gehele <periode>' hoort bij een kenmerkcontrole over een tijdsafhankelijk onderwerp; over deze lidmaatschapsvorm … heeft zij geen omschreven betekenis (§8.2)" |
| 2516 | `negeer`: 'is niet een <naam>' bond niets | stil | "… bond geen kenmerk en geen rol van het onderwerp (§8.1.8.3), dus er is niets om te ontkennen" |
| 2974 | `dagsoortOperandFout` | **luid** (ERROR) | `reden = null` |

De twee gedeelde teksten: `REDEN_GEABSORBEERDE_NIET` = "de gulzige ontleding
trok de ontkenning 'niet' in het onderwerp; een vergelijking of een
leeg-/gevuld-controle kan haar daar niet uit terughalen, en de positieve vorm
emitteren zou de vraag omkeren (§8.1)". `REDEN_GEEN_KENMERK_GEEN_ROL` = "de
geschreven naam bindt geen kenmerk en geen rol van het onderwerp
(§8.1.7/§8.1.8), dus er is geen lidmaatschap om te toetsen". Twee registers,
één tekst per as.

**`resolver/ExpressieBouwer.java` — 17 plekken, 11 luid, 6 stil**

| regel | route | was | nieuw |
|---|---|---|---|
| 243 | `bouw(null)` | stil | "er is geen expressie om te bouwen — de ontleding leverde op deze plaats geen boom op" |
| 284 | `bouw` valt door | stil | "deze expressievorm wordt niet naar IR omgezet — zij is geen som, product, macht, percentage, afronding, begrenzing of primaire expressie (§13.4.16/.17)" |
| 347 | `bouwPrimair(null)` | stil | "er is geen primaire expressie om te bouwen — …" |
| 444 | `bouwPrimair` valt door | stil | "deze primaire expressie bindt geen literal, geen parameter, geen variabele, geen attribuut van een onderwerp en geen functieaanroep (§13.4.17)" |
| 518 | aggregatie over een ongedeclareerd lid | **luid** (WARNING) | `null` |
| 561 | dimensieselectie / 'gedurende de tijd dat' | **luid** (WARNING) | `null` |
| 650/679/688/703 | `keurVouwbron` (a) geneste set-stap, (b) rolanker, (b) tussenstap, (c) tegengesproken paarbinding | **luid** (4× WARNING) | `null` |
| 1124/1130/1155 | `aggregatieFilter`: samengestelde subselectie, getal-/kenmerkpredicaat, unietelling | **luid via de aanroeper** (`bouwFunctie` toetst op `Onopgelost` en waarschuwt) | `null` |
| 1719 | `keurRangtelwoord`: positie voorbij de bindingen | **luid** (ERROR) | `null` |
| 1733 | `keurRangtelwoord`: twee of meer bindingen | **luid** (WARNING) | `null` |
| 1750 | `bouwAttribuut` zonder attribuutnaam met lidwoord | stil | "de attribuutverwijzing noemt geen attribuutnaam met lidwoord (§13.4.2.9)" |
| 2279 | `naamRef`: onbindbare kale naam | stil | "de naam '<naam>' is geen gedeclareerde parameter, geen variabele uit een 'Daarbij geldt:'-deel en geen attribuut van het onderwerp (§11/§13.3.8)" |

De vier stille weigervormen uit de kaart staan hier NIET als productieplek: ze
zijn declines in `kwantorKenmerk`, die `null` teruggeeft en de marker van regel
2144/2480 laat staan. Ze kregen hun eigen meldingen (zie hieronder), naast de
drie zusters die al spraken.

### (b) De uitvoering — twee kanalen

**1. De reden reist mee, gemeld wordt wat overleeft.** `Conditie.Onopgelost` en
`Expr.Onopgelost` dragen een component `reden`; `Resolver.resolve` loopt na
`RuleChecker.check` de IR-wortels van het model af (`Conditie.verzamelOnopgelost`
en `Expr.verzamelOnopgelost`, beide een UITPUTTENDE switch over de sealed union,
dus een nieuwe knoop is een compilatiefout en geen stilte) en meldt elke marker
die er dán nog staat als WARNING met prefix `onopgelost: `.

Melden op de bouwplek was geen optie, en dat is de kern van het ontwerp: een
marker is daar niet definitief. `uitToplevelKaal` markert een voorwaarde die de
kwantorvouw vervolgens alsnog leest; `doelVoorwaarde` hervertaalt een gemarkeerde
feitcreatie-guard tegen het doel; de §13.4.17.53-hersplitsing bouwt een
gemarkeerde aggregatie-operand ter plaatse opnieuw op. Gemeten: de gelande
lidmaatschapsvouw loopt zelf door zo'n marker heen, dus een melding bij de
constructie zou elke golden met die vorm onterecht onopgelost noemen — en die
melding staat op de wire. Een `reden` van `null` betekent "deze route spreekt al
zelf"; de sweep herhaalt haar niet. Bijvangst: de constructor dwingt voortaan bij
elke nieuwe markerplek een reden af.

**2. De kwantorvouw meldt haar eigen weigeringen.** `meldVouwWeigering` in
`RuleChecker` geeft de vier stille declines dezelfde behandeling als hun drie
luide zusters: kale `DE`, §8.1.7-rolcontrole van het element, meerstapsbereik en
onbindbaar bereik. `GekwantificeerdOnderwerp` draagt daarvoor nu een `reden`
zoals `Deelselectie` dat al deed — `null` betekent "dit is de vorm helemaal
niet", een `reden` betekent "dit IS een gekwantificeerde ledenzin en zij wordt
geweigerd, om deze reden". De kale `DE` spreekt alleen bij een GEDECLAREERD
meervoud (`isGedeclareerdMeervoud`): "de reis van de passagier" is een gewone
één-op-één-navigatie, en die auteur vertellen dat een kwantorvouw weigerde zou
een lezing noemen die de zin nooit had.

**3. `--ir` drukt zijn waarschuwingen af.** Tot nu toe printte een geslaagde run
niets, dus elke WARNING was onzichtbaar op het enige pad dat een schrijver
gebruikt (`NietTemporeleVouwTest` noteerde dat al: "a WARNING the CLI does not
even print"). Nu gaan de niet-ERROR-diagnoses naar stderr; stdout houdt zijn
parseerbare samenvattingsregel, de exitcode verandert niet.

**Bewust niet gedaan.** Geen spec-tekst: de markerdiscipline is frontend-intern
en `IR-SPEC` ch. 2.4 zegt al "a marker with a reason, not a type" — dat is nu
waargemaakt in plaats van uitgebreid. Geen samensmelting van de twee kanalen: de
vouwweigering noemt een as die de sweep niet kan noemen, en de sweep dekt routes
die met vouwen niets te maken hebben. Geen gemakconstructor `Onopgelost(bron)`:
die zou precies de stilte terugbrengen die deze kaart wegneemt.

### De wire-controle — de bevinding

Diagnoses staan WEL op de v0-wire: `IrEmitter` schrijft een `diagnostics`-lijst
(`SetlezingZonderVouwTest.de_emissie_draagt_de_diagnose_op_de_wire` pint dat al).
Gemeten omvang:

- **corpus/golden: nul beweging.** Alle 58 `bron.rgs`-goldens dragen nul
  `Onopgelost`-knopen en nul diagnoses, dus er valt niets te melden. Gate 10 is
  byte-identiek groen zonder regeneratie van welke golden dan ook.
- **corpus/rood: nul beweging.** Geen enkele rood-casus heeft een `bron.rgs`;
  de twee WF-044-markerdocumenten zijn handgeschreven en worden niet
  ge-her-emitteerd.
- **`toka/casus/ir.json`: één diagnose erbij, zes regels.** De pariteitscasus
  draagt precies één `Onopgelost` — een bullet waarvan de geschreven naam noch
  een kenmerk noch een rol van zijn onderwerp bindt, dus route 2480 — en die
  krijgt nu zijn WARNING. Niets anders in het bestand en niets in de
  `cases/`-slices bewoog; de marker zelf is byte-identiek. Gate 11 vergelijkt
  tegen de vastgelegde staat, dus de pin is hernomen en de afwijking staat als
  vierde regel in `toka/README.md` onder "Deliberate divergences from the
  toolchain emission", naast het Tekst-lexeem-precedent van 2026-09-18. Dat is
  de gesanctioneerde route; een stille regeneratie is het niet.

Dat `toka/`-bestand is de enige wire-beweging van deze kaart. De `toka/`-tests
zelf (gate 11, 27 tests) veranderden niet: `CasusTest` toetst op ERRORs en
`ModelTest` op benoemde meldingen.

### Getuigen

Nieuw: `frontends/regelspraak/src/test/java/io/github/yhavinga/regelspraak/StilleMarkerTest.java`
(18 tests) en `…/MarkerRedenPinTest.java` (de klem, 1 test). De eerste elf zijn
rood-eerst gedraaid vóór de wijziging: van de elf toenmalige tests faalden er
acht met letterlijk `kreeg: []` — de lege diagnoselijst die de stilte ís — en
een negende (`een_luide_route_wordt_niet_twee_keer_gemeld`) op `expected: <1>
but was: <0>`, want de sweep bestond nog niet. De twee anti-tests waren meteen
groen.

- `een_naam_die_geen_kenmerk_en_geen_rol_bindt_wordt_gemeld` — route 2480, de
  vorm die de pariteitscasus loopt, in eigen Jutterswet-materiaal
- `de_vragende_vorm_van_dezelfde_weigering_wordt_ook_gemeld` — route 2144
- `een_ontkende_vorm_zonder_binding_wordt_gemeld` — route 2516 (`negeer`)
- `een_rolcheck_van_het_element_meldt_de_as_van_de_weigering` — vouwdecline
  §8.1.7, plus de marker
- `een_meerstapsbereik_meldt_de_gestapelde_verzameling` — vouwdecline
- `een_kale_de_over_een_meervoud_wordt_gemeld` — vouwdecline
- `een_onbindbaar_bereik_wordt_gemeld` — vouwdecline (`bronRol == null`)
- `een_onbindbare_naam_in_een_expressie_wordt_gemeld` — route 2279
- `een_gelande_vouw_meldt_niets` — de anti-test die het sweep-ontwerp draagt:
  een landende vouw maakt onderweg een marker en moet schoon blijven
- `een_luide_route_wordt_niet_twee_keer_gemeld` — precies één sweep-melding
  naast de bestaande telwoord-WARNING
- `een_schoon_document_krijgt_geen_enkele_melding` — de wire-klem: lege
  `diagnostics` blijft leeg

Erbij in de lensronde (zie hieronder):

- `inconsistente_bullets_melden_de_onduidelijke_nesting` — route 1728
- `een_tellende_kwantor_over_diepere_nesting_wordt_gemeld` — route 1920
- `een_geabsorbeerde_niet_op_een_vergelijking_wordt_gemeld` — route 2124
- `dezelfde_weigering_in_het_stellende_register` — route 2449
- `een_voorwaardendeel_zonder_vorm_wordt_gemeld` — route 1662, foutherstel
- `een_bullet_zonder_vorm_wordt_gemeld` — route 2011, foutherstel
- `een_ontbrekende_expressie_en_een_onbindbare_primaire_worden_gemeld` —
  routes 243 en 444, foutherstel
- `elke_null_reden_staat_op_de_gesanctioneerde_lijst` — de klem zelf

## Lensronde (2026-09-19) — drie correcties

De lens bevond het ontwerp houdbaar (de sweep uitputtend; het meldplek-argument
beslissend bewezen door plek 2144 bij constructie te laten melden, waarop vier
bestaande kwantorvouw-tests braken; geen wire-lek; de toka-herpinning feitelijk
juist) en wees twee gaten plus een onnauwkeurigheid aan.

### Klem op de null-conventie (`MarkerRedenPinTest`)

Gemeten gat: `RuleChecker:2480` op `null` muteren liet 308 van de 309 tests
groen. De compiler dwingt bij een nieuwe plek wel een argument af, maar niet dat
het een reden IS — een toekomstige plek die `null` schrijft was dus weer stil.

Gekozen: **de broncontrole**, niet de runtimeklem. De runtimevariant — een
overlevende `null`-marker in een model met een lege diagnoselijst is bewezen
stilte — is goedkoop maar heuristisch: één luide melding elders in het document
maskeert de stilte, en bij een syntaxfout maskeert zij ze allemaal. Juist de
foutherstelroutes hieronder zouden er structureel buiten vallen. De broncontrole
is exact en volgt het huispatroon van de gates: een redendragende lijst die
faalt als er één bijkomt én als er één verdwijnt. De prijs is onderhoud, en die
prijs is het punt — een `null` schrijven wordt daarmee een bewuste daad met een
opgeschreven belofte, zoals het optrekken van een gatevloer dat is.

De klem telt per (bestand, omvattende methode) en niet per regelnummer, want
regelnummers schuiven. Tekstliteralen en regelcommentaar worden eerst geblankt:
het woord "onopgelost" staat in het proza van zes meldingen. Zes lijstregels
dekken de dertien gesanctioneerde nullen; een vloer van 30 gevonden
bouwplekken (gemeten 32) zorgt dat een patroon dat niets meer matcht niet
stilzwijgend alles goedkeurt, en een lege reden (`""`) faalt apart.

Beide richtingen gemeten met een gesimuleerde mutatie, daarna teruggedraaid:
`negeer()` zijn reden afnemen geeft *"nieuwe reden==null-plek(ken) zonder
vermelding: [RuleChecker.java#negeer]"*, en `dagsoortOperandFout` een reden
géven geeft *"de lijst is verouderd — deze plek(ken) bouwen geen null-marker
meer: [RuleChecker.java#dagsoortOperandFout]"*.

### Getuigenstand: elke redendragende plek gedekt

De negentien redendragende plekken vallen in vier klassen. Zeven getuigen zijn
erbij gekomen; zeven plekken dragen nu een `GEEN GETUIGE`-commentaarzin met de
reden dat er geen invoer bestaat die hen bereikt.

| klasse | plekken | dekking |
|---|---|---|
| A — bereikbaar op een welgevormd document dat emitteert | RC 1728, 1920, 2124, 2144, 2449, 2480, 2516; EB 2279; plus de vier vouwweigeringen | JUnit-getuige (elf) |
| B — alleen via ANTLR-foutherstel; het document draagt dan een syntaxfout en emitteert nooit | RC 1662, 2011; EB 243, 444 | JUnit-getuige die de melding én de blokkerende fout vastlegt (drie tests) |
| C — structureel onbereikbaar: elke grammaticatak wordt hierboven afgehandeld | EB 284 (zeven takken), EB 347, EB 1750; RC 1825, 1969, 2028 | `GEEN GETUIGE`-commentaar bij de plek |
| D — wél geproduceerd, maar altijd vervangen voordat het model af is | RC 2500 | `GEEN GETUIGE`-commentaar: `keurGeheelPeriode` acht een `Onopgelost` nooit tijdsafhankelijk en zet er haar eigen marker mét ERROR voor in de plaats |

Twee bevindingen uit die ronde zijn het noteren waard. EB 243 (`bouw(null)`) is
wél bereikbaar — de lens vermoedde van niet — en zijn zuster EB 347
(`bouwPrimair(null)`) niet, omdat ANTLR de `primaireExpr`-tak pas kiest ná een
geslaagde match. En RC 2500 blijkt een reden te dragen die nooit gerapporteerd
kan worden; de reden blijft staan omdat zij de weigering van díe route
beschrijft, en het commentaar zegt waarom zij zwijgt.

RC 1728 en 1920 zijn beide bereikbaar met een zesregelige eigen bron. Bij 1920
meldt óók de aggregatie-aanroeper; dat is een bewust aanvaarde dubbeling van
twee verschillende uitspraken (die ene zet de aggregatie opzij, deze zegt waarom
de deelselectie niet te structureren was) — en op de vouwroute, waar dezelfde
marker het filter van een `Kwantorvouw` wordt, spreekt niemand anders.

De 1728-meldtekst is ingekort tot de kern: van ~55 woorden naar de
kolom-/tekenconstatering met haar §-verwijzing.

Geen bestaande test gewijzigd. De frontend-suite telt 317 tests (was 298),
allemaal groen; gate 10's vloer staat op 173 en hoeft niet omhoog.

### Gates (alle veertien, los gedraaid)

1 self-contained PASS · 2 ir-schema PASS (238 bestanden, vloer 192) ·
3 verifier PASS (238 golden schoon, 395 negatief, vloer 350) · 4 case-schema
PASS (568 cases) · 5 oracle PASS (ref-suite 643, vloer 489; 539 cases) ·
6 clingo PASS (419 checks, vloer 83; 539 cases) · 7 noop PASS (503 cases,
36 survivors) · 8 rood PASS (29 rode cases, vloer 26) · 9 catala PASS (201
checks; 12 goldens) · **10 regelspraak PASS (317 tests, vloer 173; 58
rgs-goldens byte-identiek, vloer 38)** · 11 toka-parity PASS (45 fragmenten
byte-identiek na herpinning; 29 cases door ref/clingo/Drools) · 12 drools PASS
(159 played, 478 JUnit-tests) · 13 reftype-derivation PASS (126 twists over 23
goldens) · 14 flogic PASS (130 checks, vloer 81).

Gate 1 en gate 12 vielen onderweg één keer om en zijn in dezelfde kaart
gerepareerd: gate 1 op het woord TOKA in mijn eigen testcommentaar (de
rechtenprikkel leest ook testproza — commentaar herschreven), gate 12 omdat
`backends/drools` de `ir`-bronnen van de frontend als source root meeneemt (de
"no copy"-opzet), zodat `IrLoader` de nieuwe component moest meegeven: `null`,
want de wire draagt geen `reden`.

### Gewijzigde bestanden

`frontends/regelspraak/src/main/java/io/github/yhavinga/regelspraak/ir/Conditie.java`,
`…/ir/Expr.java`, `…/resolver/RuleChecker.java`, `…/resolver/ExpressieBouwer.java`,
`…/resolver/Resolver.java`, nieuw `…/src/test/java/…/StilleMarkerTest.java` en
`…/src/test/java/…/MarkerRedenPinTest.java`,
`backends/drools/src/main/java/io/github/yhavinga/regelspraak/loader/IrLoader.java`,
`toka/casus/ir.json` (herpinning), `toka/README.md` (de afwijking opgeschreven).
`spec/CONSTRUCTS.md` en `spec/PLAN.md` onaangeroerd; geen spec-voorstel nodig.
