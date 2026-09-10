# De verifier heeft geen hek op de geldigheidsgrens — `niet-een-datum` is een geldig venster

Goal: beslissen of de `vanaf`/`tot` van een `Geldigheid` een eigen WF-regel
krijgt (de zuster van WF-084, die exact deze grammatica al normeert voor het
datum-LITERAL) of dat het gate-2-patroon dat 2026-09-11 is voorgesteld de hele
handhaving blijft. De kaart levert het besluit; bij "WF-regel" de spec-tekst,
de verifiercontrole, een negatief-getuige en de golden die hem passeert.

## De stand (gemeten 2026-09-11, kaart `20260910-irloader-datumklem`)

`spec/verifier/lib/loader.ml:862` leest beide grenzen met `str_opt` — een
willekeurige string, ongecontroleerd. Er is geen WF-regel die de vorm dekt.
Gedraaid op een met de hand bewerkte kopie van
`corpus/golden/jutterswet-vensterheffing`, met de `vanaf` van de eerste
regelversie vervangen:

| lexeem | verifier |
| --- | --- |
| `+012026-01-01` | `PASS` |
| `-0500-01-01` | `PASS` |
| `2026-02-30` | `PASS` |
| `niet-een-datum` | `PASS` |

Alle vier. De vierde is de scherpste: een venster met de grens
`niet-een-datum` is voor de verifier een welgevormd document.

**Waarom dat meer is dan slordigheid.** `checks.ml:80-81` schrijft de aanname
op die eronder ligt — *"Interval overlap over ISO date strings … ISO dates
compare lexicographically"* — en `validity_overlaps` vergelijkt de grenzen dan
ook met `<=` op de string. WF-021 (overlappende regelversies) steunt daar
rechtstreeks op, en `ref` (`eval.ml:83`) en clingo (`translate.ml:311`) doen
in hun toepassingstest hetzelfde. Die ordening is alleen geldig BINNEN het
strikte lexeem: `2026-1-01` sorteert ná `2026-09-30`, en `+012026-01-01`
sorteert vóór elk viercijferig jaar. Drie lezers rekenen dus met een
voorwaarde die geen enkele lezer afdwingt.

**Wat er sinds 2026-09-11 wél staat**, en waarom dat de vraag niet sluit:

1. Een **schemapatroon** op `Geldigheid.vanaf`/`tot` (voorstel op de tak
   `card/irloader-datumklem`, beschermd pad). Dat is gate-2-handhaving: het
   ziet documenten die langs de gate komen, en het kan de kalender niet
   kennen — `2026-02-30` matcht.
2. Een **klem in de Drools-lezer** (`IrLoader.datumVanIrWire`). Die dekt één
   executor, op zijn eigen drempel.

`ref`, clingo en de verifier houden nog steeds geen eigen hek, en de verifier
is de plek waar een WF-regel hoort: hij weigert vóór elke executor.

**En er is een derde gat, dat op deze kaart kan meeliften: het schemapatroon
zelf wordt door niets bewaakt.** Gemeten 2026-09-11 — beide `pattern`-sleutels
uit `$defs/Geldigheid` verwijderd, het schema schoon geherserialiseerd — meldt
gate 2 `PASS (schema valid; 194 file(s) validate and round-trip, floor 192)` en
blijft de Drools-getuige 18/18 groen. Het patroon verwijderen is dus een stille
groen; zijn beet staat alleen in kaartproza. Dat is niet nieuw en niet
bijzonder voor deze twee sleutels: **de tien patronen van
`uchar-case-v0` staan in precies dezelfde positie**, dus dit is de bestaande
huistoestand van het hele schemapark, geen regressie van de kaart die het
patroon voorstelde.

**Let op welke bewaking hier NIET werkt**, want de voor de hand liggende is de
verkeerde. Een `corpus/negatief/`-document bewaakt het patroon niet: gate 2
valideert uitsluitend `corpus/golden` en `corpus/rood`
(`check-ir-schema.sh:50`) en ziet de negatief-zone nooit; zo'n document wordt
door gate 3 geweigerd, om zijn WF-regel, en het patroon verwijderen blijft dan
nog steeds een stille groen. De andere kant werkt evenmin: een golden- of
rood-document kan het patroon niet bewaken zonder er zelf buiten te vallen, en
dan faalt gate 2 altijd in plaats van alleen na verwijdering.

De patroonbewaking vergt dus een eigen mechanismekeuze, en die staat open:

- een **negatiefzone voor gate 2** — documenten die tegen het schema MOETEN
  falen. Gate 2 kent vandaag geen zone die moet falen (gates 3 en 8 wel), dus
  dit is een gate-uitbreiding en daarmee een voorstel op een beschermd script;
- een **zelftest in gate 2** die het patroon tegen een vaste lexeemlijst
  houdt, zonder corpusdocument;
- **niets**, met de reden opgeschreven — de huistoestand blijft dan gelijk
  voor alle elf patronen in het schemapark, wat verdedigbaar is zolang het
  expliciet is.

Beslis welke; het is geen reden om de keuze weg te laten.

## De vraag die beslist moet worden

WF-084 normeert deze grammatica al, letterlijk, voor de andere datumpositie op
deze wire (het `Literal` met `DatumInDagen`-type), inclusief de kalendercontrole
die een patroon niet kan doen (`lexeme_iso_date`, `checks.ml:229`). De vraag is
of de geldigheidsgrens een eigen ID krijgt of onder een verbrede WF-084 valt —
en of `ref` en clingo hun grens dan óók parseren of ondoorzichtig laten.

Let op de tweede helft daarvan: als de verifier de vorm afdwingt, is de
lexicografische vergelijking in `ref`/clingo/verifier *correct* en hoeft er
niets aan die drie lezers te veranderen. Dat is vermoedelijk het goedkoopste
antwoord, maar het moet gemeten en opgeschreven worden, niet aangenomen — een
lezer die een document binnenkrijgt dat niet langs de verifier is geweest,
heeft dan nog steeds geen hek, en dat is precies het argument waarmee de
case-wire-klemmen zijn neergezet.

## Acceptance

- Het besluit staat in het Result, met de meting hierboven hertest op de
  dan-geldende HEAD.
- Bij "WF-regel": spec-tekst met ID en grammatica in `spec/IR-SPEC.md` ch. 4.1
  naast WF-084, een verifiercontrole, minstens één passerende golden en één
  `corpus/negatief/<wf###-slug>/` die om de genoemde regel wordt geweigerd —
  de volle spec-discipline uit `CLAUDE.md`.
- Bij "geen WF-regel": de reden staat in het Result én als commentaar bij
  `validity_overlaps`, zodat de volgende lezer de onafgedwongen aanname niet
  opnieuw hoeft te ontdekken.
- De bewakingsvraag hierboven is expliciet beantwoord met één van de drie
  genoemde mechanismen, en bij "wel bewaken" laat het Result zien dat het
  verwijderen van het patroon dan rood wordt. Stilzwijgend overslaan telt als
  niet gedaan.
- Gates 3 en 2 groen; geen telling beweegt behalve de negatief-teller bij
  "WF-regel".

May touch: `spec/IR-SPEC.md`, `spec/verifier/**`, `corpus/negatief/**`,
`backlog/` — de eerste twee als voorstel, niet mergen.

## Out of scope

- De Drools-klem en het schemapatroon: die landden 2026-09-11.
- De datum-LITERAL-lezer zelf (WF-084/WF-095): die regels staan er; deze kaart
  gaat over de positie die ze niet dekken.
- `Literal.waarde` patroneren in het schema: polymorf per `type`, dus niet per
  positie te patroneren — gemeten in dezelfde sweep, en het is een andere vraag.

## Result (2026-09-11)

**Het besluit: een WF-regel, en het nummer is WF-101.** De geldigheidsgrens
krijgt een eigen ID naast WF-084, niet een verbrede WF-084. De reden is de
positie: WF-084 normeert een `Literal` met `DatumInDagen`-type, WF-101 een veld
van de `Geldigheid`-node — twee posities, één grammatica, en de regels vallen
apart. De grammatica wordt letterlijk gedeeld: de controle roept `lexeme_iso_date`
aan, dezelfde functie die WF-084 gebruikt, dus er is geen tweede
datumdefinitie in huis bijgekomen.

**Terugdraaibaarheid.** Drie commits dragen het besluit en zijn los terug te
draaien. `spec/verifier/lib/checks.ml` bevat één nieuwe functie (`wf101`) plus
één regel in `all`; terugdraaien is die functie schrappen en de regel
terugzetten, waarna de twee negatief-getuigen weer PASS geven en gate 3 rood
wordt op precies die twee mappen. `spec/IR-SPEC.md` draagt de clausule, de
registerrij en twee bijgewerkte verwijzingen (WF-021's rij en de
ch. 3.10-spiegelnotitie). De gate-2-zelftest staat in een eigen commit en hangt
niet aan WF-101: hij bewaakt het schemapatroon, niet de verifiercontrole.

### Hermeting op HEAD 998fc4f

De kaartmeting reproduceert exact — alle vier de lexemen zijn verifier-PASS —
en er is nu een tweede kolom, want het schemapatroon is inmiddels geland:

| lexeem | schema (gate 2) | verifier (gate 3) |
| --- | --- | --- |
| `+012026-01-01` | FAIL | PASS |
| `-0500-01-01` | FAIL | PASS |
| `2026-02-30` | **PASS** | PASS |
| `niet-een-datum` | FAIL | PASS |
| `2026-01-01` (controle) | PASS | PASS |

Dat is de laagverdeling in één tabel: het patroon vangt drie van de vier en kan
de vierde niet vangen, en de verifier ving er nul.

**Wat de andere lezers deden**, gemeten op `jutterswet-vensterheffing` met één
grens vervangen en verder niets aangeraakt, beide casus per rij, `ref` en de
ASP-backend rij voor rij gelijk:

| grens | casus BINNEN het venster | casus VÓÓR het venster |
| --- | --- | --- |
| `2026-01-01` (de golden) | 125 | leeg |
| `+012026-01-01` | 125 | **125** — fout |
| `-0500-01-01` | 125 | **125** — fout |
| `niet-een-datum` | **leeg** — fout | leeg |
| `2026-02-30` | 125 | leeg |

Geen enkele rij faalt ergens; elk fout antwoord is stil. `+` (0x2B) en `-`
(0x2D) sorteren onder elk cijfer, dus een tekenjaar is kleiner dan elk
viercijferig jaar en het venster opent stil aan het begin; `niet-een-datum`
begint met een letter en sorteert boven elke datum, dus het venster sluit over
zijn eigen casus. Daarmee is de tweede helft van de kaartvraag ook gemeten en
niet aangenomen: mét het hek is de lexicografische vergelijking in verifier,
`ref` en clingo *correct zoals ze staat*, en hoeft geen van de drie een grens te
parseren. Het residu staat in de clausule: een document dat een executor
bereikt zónder langs de verifier te zijn geweest, treft in die drie nog steeds
geen hek — alleen de Drools-backend parseert het lexeem, op zijn eigen drempel.

**Hertelling van de 848/839/9-telling** (de kaart mat 848/839/9 over 194
documenten; main is doorgelopen): **852** `vanaf`/`tot`-posities over **195**
documenten in `corpus/golden` plus `corpus/rood`, **843 null**, **9 met
waarde**, distinct `2025-01-01`, `2025-12-31`, `2026-01-01`, `2026-12-31`,
verdeeld over dezelfde drie documenten. Over de drie verifier-gepoorte zones
samen (golden 169, negatief 364, rood 26 — 559 documenten): **1814** posities,
**1800 null**, **14 met waarde**; twaalf daarvan gaan aan de regel vooraf en
spellen een toelaatbaar lexeem letterlijk, twee zijn de getuigen van deze
kaart. De pariteitszone voegt 128 posities over 30 documenten toe, allemaal
null. De regel breekt geen enkel bestaand document.

### Rood eerst, beide lagen

Twee negatief-getuigen, één per laag, elk met een eigen NOTE:

- `corpus/negatief/wf101-geldigheidsgrens-tekenjaar` — de VORM-arm,
  `vanaf: "+012026-01-01"`;
- `corpus/negatief/wf101-geldigheidsgrens-kalenderdag` — de KALENDER-arm,
  `tot: "2026-02-30"`, het lexeem dat het patroon toelaat.

Vóór de controle (commit `23f551a`):

```
wf101-geldigheidsgrens-tekenjaar       rc=0 verifier: PASS
wf101-geldigheidsgrens-kalenderdag     rc=0 verifier: PASS
```

Ná (commit `0275d54`):

```
WF-101 FAIL /resolvedRegels/0/geldigheid/vanaf: validity bound '+012026-01-01' is not the ISO form yyyy-mm-dd (fixed field widths, valid ranges — ch. 4.1)
WF-101 FAIL /resolvedRegels/0/geldigheid/tot: validity bound '2026-02-30' is not the ISO form yyyy-mm-dd (fixed field widths, valid ranges — ch. 4.1)
```

**Arm-exact, nul over-afwijzing.** De volle sweep over de 559 documenten van de
drie gepoorte zones, vóór tegen ná, verschilt in exact vier regels: de twee
`==`-koppen van de nieuwe mappen en hun twee meldingen. Geen enkele andere
rapportregel beweegt. De pariteitszone beweegt ook niet — daar is geen enkele
grens niet-null.

Beide getuigen zijn gebouwd op het skelet van `wf084-source-snippet` met het
datum-LITERAL terug in ISO-vorm, zodat het enige gebrek de grens is. De
passerende getuigen zijn bestaande goldens, niet nieuwe:
`jutterswet-vensterheffing` (twee gedateerde versies van één regelnaam, beide
grenzen geschreven, herspeeld door gates 5 en 6 op een casus binnen en een
casus vóór het venster — het document waarvan alle metingen hierboven twists
zijn) en `anchor/jutterswet-jutheffing` (twee aansluitende vensters, absolute
waarden per venster door beide executors). `jutterswet-vormenkabinet` draagt de
derde en vierde grens en blijft schoon.

### De clausule en de registerrij

- `spec/IR-SPEC.md` ch. 4.1: **"The validity bound's lexeme (WF-101)"**, direct
  na het WF-095-blok, zodat de vier lexeemregels (WF-081, WF-084, WF-095,
  WF-101) aaneen staan. De clausule noemt de negen dragers van een venster, zegt
  dat `null` de GEDECLAREERDE open grens is en dus toegelaten wordt (niet de
  lege helft van WF-095), draagt de twee meettabellen, en scheidt de twee lagen
  expliciet: een kalender is geen vorm, dus die laag hoort waar WF-084's al zit.
- Registerrij `| WF-101 | … | gate 3 | vensterheffing + anchor/jutheffing |
  de twee wf101-getuigen |`, na WF-100.
- **De onafgedwongen aanname is op drie plekken benoemd en op één plek
  afgedwongen.** In `checks.ml` is de comment bij `validity_overlaps`
  omgeschreven: de aanname is nu een eis met een nummer. WF-021's registerrij
  zegt nu dat zijn test op de RUWE grensstrings draait en alleen daarom
  kalendercorrect is. En ch. 3.10's v0-spiegelnotitie ("een misvormde grens is
  een frontendfout, dus een Geldigheid in geëmitteerde IR is altijd
  welgevormd") staat er nog, maar met erbij dat dat een belofte over één
  PRODUCENT was en nooit een wet van de wire — tot WF-101.

### De bewakingsvraag: mechanisme 2, met demonstratie

Gekozen: **een zelftest in gate 2**, niet een negatiefzone en niet "niets".
`scripts/check-ir-schema.sh` (VOORSTEL, beschermd pad) krijgt een check 1b: een
vaste lexeemtabel, in beide polariteiten, tegen `$defs/Geldigheid` gevalideerd
zonder enig corpusdocument, plus een **oppervlakteklem** die faalt zodra een
`pattern` opduikt op een positie die de tabel niet draagt. Reden voor dit
mechanisme boven een negatiefzone: gate 2 valideert alleen `corpus/golden` en
`corpus/rood`, dus een zone die moet falen is een gate-uitbreiding met een
eigen zoekpad, terwijl de vraag "bijt dit patroon nog?" zich zonder enig
document laat stellen. Het IR-schema draagt precies twee patronen, beide op
`Geldigheid`, dus deze tabel dekt het hele patroonoppervlak van dit schema —
dat is in het script vastgelegd in plaats van aangenomen.

De tabel pint ook de LAAGGRENS: `2026-02-30` staat erin als *geaccepteerd*, met
de reden erbij, zodat het patroon aanscherpen tot een kalendercontrole een
zichtbare, beargumenteerde wijziging wordt en geen drift.

**De demonstratie** (schema daarna teruggezet; `git status` op `spec/schema`
leeg):

```
# 1. beide patronen weg
FAIL: the schema's pattern surface moved and the self-test did not follow.
  self-test row with no pattern:  /$defs/Geldigheid/properties/tot
  self-test row with no pattern:  /$defs/Geldigheid/properties/vanaf
gate 2 rc=1

# 2. patroon verzwakt tot ^[0-9-]+$ (oppervlak intact)
FAIL: pattern self-test, Geldigheid.vanaf = '-0500-01-01': schema accepts it, expected refuse
FAIL: pattern self-test, Geldigheid.vanaf = '2026-1-1': schema accepts it, expected refuse
FAIL: pattern self-test, Geldigheid.vanaf = '26-01-01': schema accepts it, expected refuse
… (idem voor tot)
check-ir-schema: FAIL (6 pattern self-test row(s) bad)
gate 2 rc=1

# 3. een NIEUW patroon op een ongetabelleerde positie
FAIL: the schema's pattern surface moved and the self-test did not follow.
  pattern with no self-test row: /$defs/Literal/properties/waarde
gate 2 rc=1

# 4. schema teruggezet
check-ir-schema: PASS (schema valid; 24 pattern self-test row(s); 195 file(s) validate and round-trip, floor 192)
```

Vóór deze zelftest was elk van de eerste drie een stille groen.

**Wat de zelftest niet dekt, met de reden:** de tien patronen van
`uchar-case-v0` staan in exact dezelfde positie. Die horen bij gate 4, niet bij
gate 2, en een gate-4-zelftest is een eigen kaart. Dat staat als scopezin in
het script zelf, zodat het een keuze is en geen omissie.

### De nummerschuif

WF-101 was door één wachtende kaart als kandidaat genoemd. Deze kaart landt
eerder, dus `backlog/new/20260910-geneste-aggregatie-rolketen.md` is bijgewerkt
naar **WF-102**, met WF-101 toegevoegd aan haar lijst van inmiddels bezette
nummers. De dubbeling die die kaart noemde is weg: `20260910-reverse-rol-via-naam.md`
is geland met antwoord (ii) en heeft haar WF-kandidaat laten vervallen, dus de
geneste-aggregatiekaart is de enige wachter op het volgende nummer.

### Gates

```
scripts/check-self-contained.sh   check-self-contained: PASS
scripts/check-ir-schema.sh        PASS (schema valid; 24 pattern self-test row(s); 195 file(s) validate and round-trip, floor 192)
scripts/check-verifier.sh         PASS (195 golden clean; 364 negatief rejected for their declared rule, floor 350)
```

Gate 3's negatief-teller gaat van 362 naar 364; dat is de enige telling die
beweegt, precies zoals de acceptance eist. Gate 2's documenttelling (195) en
zijn vloer (192) staan stil — de negatief-zone wordt daar niet gevalideerd.
Gate 2's regel draagt er één getal bij (24 zelftestrijen); dat is een nieuwe
telling, geen verplaatste.

### Wat bewust niet bewoog

- **`ref/`, `backends/clingo/` en `backends/drools/`** — geen regel. De meting
  hierboven is het argument: met het hek is hun lexicografische vergelijking
  correct zoals ze staat. Een eigen grenslezer in die drie zou het gemeten
  residu (een document dat de verifier omzeilt) wél dichten, maar dat is een
  eigen besluit met eigen kosten, en het staat als residu in de clausule.
  `backends/drools` was bovendien buiten de opdracht: zusterbouwer.
- **`corpus/golden/`** — geen nieuw document. De kaart vroeg om een passerende
  getuige en het corpus heeft er drie; nieuwe goldens zouden gate 12's
  `EXPECTED_PLAYED`-gelijkheid raken zonder iets toe te voegen.
- **`spec/schema/`** — het patroon stond er al (kaart
  `20260910-irloader-datumklem`) en is niet aangeraakt. De laagverdeling is
  ongewijzigd: het patroon doet de vorm, WF-101 doet vorm *plus* kalender.
- **`spec/CONSTRUCTS.md` en `spec/PLAN.md`** — niet aangeraakt, conform
  `WAY-OF-WORKING.md` §4.
- **De `Literal.waarde`-patroonvraag** — buiten scope volgens de kaart en daar
  gelaten; hij is polymorf per `type` en dus niet per positie te patroneren.

### Voorstel voor `spec/CONSTRUCTS.md` (hoofdsessie, niet hier gedaan)

De rij die dit draagt is §2.3, `Geldigheid` — de node-rij, waarvan de kolom
"Delta / notes" vandaag een kale `—` is. Voorgestelde zin voor die kolom:

> **Landed (2026-09-11, WF-101, IR-SPEC ch. 4.1):** de grenzen dragen WF-084's
> strikte ISO-datumlexeem *inclusief kalender*, `null` is de gedeclareerde open
> grens; dat maakt de vergelijking op de RUWE string die de verifier, `ref` en
> de ASP-backend uitvoeren (ch. 8-versieselectie, WF-021's overlaptest)
> kalendercorrect zonder dat een van die drie een grens hoeft te parseren.
> Gemeten voordien: een tekenjaar-grens liet beide executors een regelversie
> toepassen buiten haar venster en `niet-een-datum` sloot het venster — stil, in
> beide gevallen. Getuigen `wf101-geldigheidsgrens-tekenjaar` (vorm) en
> `-kalenderdag` (kalender); het schemapatroon dekt alleen de eerste laag.

Rij 2 ("Assignment rules incl. versions with validity windows") draagt het
NIET: die rij gaat over de versieselectie als IR-structuur, en WF-101 zegt niets
over structuur — alleen over het lexeem van één veld. Eén zin op de node-rij is
de kleinste plek waar hij waar is.
