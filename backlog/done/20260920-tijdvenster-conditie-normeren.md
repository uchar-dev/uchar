# Wat er van de periodeconditie nog OPEN staat na ch. 3.10

Goal: de twee resterende helften van `RUNTIME-SEMANTICS.md` ch. 3.10 —
de grens-EXPRESSIE-vorm en de clip op een tijdlijndoel — uitvoerbaar maken
of als spec-besluit afsluiten. De scalaire poort zelf is KLAAR: ch. 3.10
normeerde haar al in zes clausules met worked example, en sinds 2026-09-20
voeren alle vier de uitvoerders haar uit (kaart `ref-periodeconditie-arm`,
golden `jutterswet-premievenster`).

**Hersneden 2026-09-20.** Deze kaart heette eerst "de TimeWindow-conditie
normeren" en stelde vier beslispunten. Drie daarvan waren al beslist en de
kaart berustte op een verkeerde lezing: ch. 8.2's "the ch. 3.10 period
condition" wijst naar ch. 3.10 van ditzelfde document (het document citeert
zijn eigen hoofdstukken als `ch. N`), niet naar IR-SPEC ch. 3.10. De
grenzen, de tijdvrije sessie en het temporele doel staan daar alle drie —
inclusief, exclusief, "tot en met" inclusief, een afwezige grens open, een
ongezette evaluatiedatum matcht nooit, een lege grens matcht nooit, en de
clip als het doel een tijdlijn is. Wat overblijft is hieronder.

## (1) De grens-EXPRESSIE

ch. 3.10's v0-mirror-note begrenst wat een grens mag ZIJN: een
datumliteral of een eigen scalair datumattribuut van het onderwerp. WF-042
laat elke datum-expressie toe. Alle vier de uitvoerders weigeren de rest —
`ref` en clingo bij naam met de fase in de zin, de exec-tak met een
transpiler-SKIP, de DRL-tak met een marker die de grens zelf noemt. Rode
getuige: `corpus/rood/periodegrens-parameterdatum` (een datumparameter).

Te beslissen: draagt v1 de volle expressie (dan vervalt de mirror-note en
wordt die rode map een golden), of legt de spec de tweevorm vast als de
norm (dan wordt het een WF-regel met een negatief in plaats van vier
weigeringen)? De eerste vraagt vertaalwerk in clingo, waar een
niet-onderwerpgebonden grens geen val-atoom heeft om aan te hangen.

## (2) De clip op een tijdlijndoel

ch. 3.10's tweede geval is **gespecificeerd en niet gebouwd**: bij een
tijdsafhankelijk doel snijdt het venster de geschreven tijdlijn in plaats
van de regel te poorten. Het hoofdstuk zegt "`ref/` is to implement them
first". Vandaag weigeren beide OCaml-uitvoerders de vorm bij naam, en met
opzet BIJ DE REGEL — de gevalsonderscheiding hangt aan het doel en een
conditie ziet er geen — zodat de scalaire poort nooit per ongeluk op een
tijdlijn wordt toegepast. Elke uitvoerder pint die weigering in zijn eigen
suite.

Te bouwen: `written(tl, window) = tl restricted to [vanaf, tot)` in `ref`,
daarna clingo, met een golden. Let op de aanpalende OPEN beslissing die
ch. 3.12 registreert: een geldigheidsvenster op een temporele schrijving
mengt de versie-as met de tijdlijn-as, en dat is de één-as-beslissing van
ch. 8 (PLAN §9, besluit 2). De clip zelf hangt daar niet aan, maar wie
beide tegelijk aanraakt wel.

## (3) Het venster onder een tellende kwantor

Sinds 2026-09-20 geweigerd door alle vier, met de grens gemeten in plaats
van gekozen: beide Drools-takken spelen onder `ALLE` en `DE` en weigeren
onder `GEEN`, `TEN_MINSTE`, `TEN_HOOGSTE` en `PRECIES`, waar de DRL-tak de
limiet zelf noemt. Rode getuige
`corpus/rood/periodevenster-tellende-kwantor`. De mirror-note van ch. 3.10
zegt precies dit ("refuse the condition under counting quantifiers"), dus
te beslissen is of v1 die begrenzing overneemt als norm (WF-regel +
negatief) of hem opheft (dan moet er een lezing komen voor wat het
COMPLEMENT van een vensterverdict betekent in een tijdvrije sessie — dat
is waar de vorm vandaag stil een onbenoemd antwoord gaf).

De kwantorVOUW (`Kwantorvouw`, ch. 3.9a) rijdt mee op dezelfde weigering
zonder eigen Drools-meting: dezelfde kwantoren, dezelfde kwantificatie
over instanties in plaats van delen. Die meting hoort bij deze kaart.

## Latent, nu onbereikbaar: de volgorde van de twee grensfouten

`ref`'s vensterarm evalueert `vanaf` vóór `tot` (ch. 5.11) en dwingt sinds
2026-09-20 ook de volgorde van de twee grens-TYPEcontroles af met een
`let` — OCaml legt de volgorde binnen een tuple niet vast. Vandaag is dat
onbereikbaar: WF-042 weigert een niet-datumgrens vóór elke uitvoerder, en
de twee gedragen grensvormen kunnen niet falen. Met de grens-expressie van
(1) wordt het scherp: dat is de eerste grensvorm die bij evaluatie KAN
falen, en dan is "welke grens faalt eerst" de waarneembare ch. 5.11-orde
waar de getuigentabel van dat hoofdstuk vandaag "none yet" bij zet.

## Samenhang

`backlog/new/20260920-drl-periode-zink.md` gaat over `GeheelPeriode` — de
§8.2-heelperiode-KWALIFICATIE op een feitcreatie-guard, waar de DRL-tak op
een compile-fout zinkt. Dat is een andere wireknoop en een andere positie
dan deze conditie; de kaarten delen alleen het woord "periode". Ze mogen
gebundeld worden, maar dan als bewuste keuze.

De statustabel van ch. 3.12 draagt sinds 2026-09-20 drie rijen voor deze
familie: de scalaire poort (uitgevoerd), de clip (gespecificeerd, niet
uitgevoerd) en de grensvorm (OPEN, backend-rijpheid).

## Acceptance (bij oppakken aanscherpen)

- (a) Per helft — grensvorm, clip, tellende kwantor — een besluit met
  reden, en de spec-tekst die het draagt (**voorstel**).
- (b) Wat uitvoerbaar wordt, wordt uitgevoerd in `ref` én clingo, met een
  golden met handberekende absoluten; wat geweigerd blijft, houdt zijn
  benoemde arm mét fase-staart en zijn getuige.
- (c) De lijsten bewegen mee: gate 8's Drools-uitsluitingsregel, de
  ch. 3.12-rijen en de gate-12-pins bij een kanaalwissel.
- (d) Alle veertien gates groen.

May touch: `spec/RUNTIME-SEMANTICS.md` en `spec/IR-SPEC.md`
(**voorstel**), `ref/**`, `backends/clingo/**`, `corpus/**`,
`scripts/check-rood.sh` en `scripts/check-drools.sh` (**voorstel**, de
lijsten en de pins).

## Besluiten en aangescherpte acceptatie (bij oppakken, 2026-09-23)

De drie besluiten zijn genomen door de hoofdsessie (2026-09-23,
maintainer-norm), hier verbatim vastgelegd:

1. **Grensvorm.** "De tweevorm (datumliteral of eigen scalair
   datumattribuut van het onderwerp) wordt de v1-norm als WF-regel met
   negatief; de volle grens-expressie blijft een OPEN wirebesluit in
   ch. 3.12. Motivering: er is geen corpusvraag naar de volle expressie,
   clingo heeft voor een niet-onderwerpgebonden grens geen val-atoom om aan
   te hangen, en het huisbeginsel legt gedragen gedrag normatief vast boven
   het openhouden van ongetuigd gedrag."
2. **Clip op een tijdlijndoel.** "Bouwen — de spec draagt het besluit al:
   ch. 3.10 specificeert de clip en zegt dat ref hem eerst uitvoert."
3. **Tellende kwantor.** "De mirror-note-begrenzing (weiger het venster
   onder GEEN/TEN_MINSTE/TEN_HOOGSTE/PRECIES) wordt de v1-norm als WF-regel
   met negatief; opheffen zou een lezing vergen voor het complement van een
   vensterverdict in een tijdvrije sessie, en die is er niet."

Acceptatie, aangescherpt:

- (1) Een nieuwe WF-regel (WF-111) in IR-SPEC (**voorstel**) met
  verifier-arm (**voorstel**), passerend op het bestaande corpus
  (`jutterswet-premievenster` e.a.), en `corpus/rood/periodegrens-parameterdatum`
  verplaatst naar `corpus/negatief/wf111-…` met herschreven NOTE; rood-eerst
  (rc 0 onder de oude verifier, afgewezen onder de nieuwe) en een
  mutatieproef (arm uit ⇒ negatief stil ⇒ gate 3 rood).
- (2) De clip `written(tl, window) = tl restricted to [vanaf, tot)` in `ref`
  en clingo, met een golden met handberekende absoluten, geen geldigheid op
  de temporele regel (de één-as-beslissing blijft onaangeroerd); de
  Drools-takken weigeren benoemd en de golden komt reden-dragend in beide
  gate-12-kanaallijsten, de gelijkheidspinnen bewegen niet. ch. 3.10 en de
  ch. 3.12-rij bewegen mee (**voorstel**).
- (3) Een nieuwe WF-regel (WF-112) met verifier-arm en negatief (de rode
  getuige `periodevenster-tellende-kwantor` verhuist, zelfde procedure);
  de kwantorVOUW-meting vastgelegd, met de keuze of dezelfde arm hem dekt.
- De gate-8-telling en de Drools-uitsluitingslijst bewegen mee
  (**voorstel**); alle veertien gates groen, elk los gedraaid.

## Handberekening van de clip-golden, vóór de eerste run (2026-09-23)

Golden `corpus/golden/jutterswet-pachtvenster`: een `Strandpachter` met een
jaarlijks `pachttarief`, twee maand-granulaire tijdlijnkenmerken
(`vergund`, `aanwezig`) en een scalaire `ingangsdatum`. Drie temporele
regels, elk met het venster als HELE voorwaarde en zonder geldigheid (de
één-as-beslissing blijft onaangeroerd):

| regel | producent | venster |
|---|---|---|
| `jaarpacht` (JAAR, geheel) | tijdsevenredig deel per jaar van het pachttarief gedurende `vergund` | `vanaf 1 januari 2025` (open boven) |
| `maandpacht` (MAAND, 2 dec.) | tijdsevenredig deel per maand, idem | `van 15 maart 2025 tot en met 30 april 2025` |
| `zomerdagen` (MAAND, geheel) | aantal dagen in de maand dat hij `aanwezig` is | `vanaf de ingangsdatum tot 1 september 2025` |

`case1-voorbeeld` (asOfDate **null**), P1: tarief 2024 = 10, 2025 = 12;
vergund [2024-01-01, 2026-01-01); aanwezig [2025-06-20, 2025-09-10);
ingangsdatum 2025-07-05.

- jaarpacht ongeknipt {2024: 10, 2025: 12} → **{[2025-01-01, 2026-01-01): 12}**
  — het worked example van ch. 3.10 letterlijk.
- maandpacht ongeknipt: 24 maandsegmenten (2024 elk 10, 2025 elk 12) →
  **[2025-03-15, 2025-04-01): 12; [2025-04-01, 2025-05-01): 12** — het
  maartsegment wordt gesneden en houdt zijn waarde, "tot en met 30 april"
  loopt tot 1 mei.
- zomerdagen ongeknipt: juni 11, juli 31, augustus 31, september 9 →
  venster [2025-07-05, 2025-09-01) → **[2025-07-05, 2025-08-01): 31;
  [2025-08-01, 2025-09-01): 31** — eigen-attribuutgrens, de 31 wordt niet
  herteld tot 27, september valt weg (`tot` exclusief).

`case2-lege-en-gemiste-vensters` (asOfDate **2025-03-20**, BINNEN het
maandpachtvenster), twee pachters:

- P2: tarief 2025 = 365; vergund [2025-01-01, 2025-04-16) (105 dagen in
  2025: 31 + 28 + 31 + 15); aanwezig [2025-06-01, 2025-07-01); GEEN
  ingangsdatum.
  - jaarpacht 365 × 105/365 = **{[2025-01-01, 2026-01-01): 105}**
  - maandpacht ongeknipt jan/feb/mrt 365, apr 365 × 15/30 = 182,5 →
    **[2025-03-15, 2025-04-01): 365; [2025-04-01, 2025-05-01): 182.5**
  - zomerdagen: lege grens → **leeg** (ongeknipt zou het juni 30 zijn)
- P3: tarief 2024 = 10; vergund [2024-01-01, 2025-01-01); aanwezig
  [2025-06-01, 2025-07-01); ingangsdatum 2025-10-01.
  - jaarpacht ongeknipt {2024: 10} → **leeg** (de gat-helft van het
    worked example)
  - maandpacht ongeknipt 2024 elk 10 → **leeg**. Een scalaire poort had
    hier op 2025-03-20 gevuurd en de 2024-maanden geschreven.
  - zomerdagen: venster [2025-10-01, 2025-09-01) heeft geen dag → **leeg**

**Bijstelling na de eerste run (2026-09-23, eerlijk vastgelegd).** De eerste
versie hierboven had twee grenzen midden in een maand (15 maart,
ingangsdatum 5 juli). ref's case-lezer weigerde de verwachting al bij het
laden: `segment boundaries must sit on the declared granularity grid`. Het
case-formaat legt dat vast in `spec/schema/uchar-case-v0.schema.json`
(`timelineValue`: "aligned to the declaration's granularity grid"), voor
invoer én verwachting, en dat pad valt buiten het mandaat van deze kaart.
Een gesneden bucket is dus niet als corpusverwachting uit te drukken. De
golden gebruikt daarom grenzen op het raster: maandpacht **van 1 maart tot
en met 30 april 2025** (P1: [03-01, 04-01): 12; [04-01, 05-01): 12. P2:
[03-01, 04-01): 365; [04-01, 05-01): 182.5), en P1's ingangsdatum is
**2025-07-01** (zomerdagen [07-01, 08-01): 31; [08-01, 09-01): 31). De
clausule "de restrictie snijdt segmenten en houdt hun waarde" staat in de
eigen suite van beide OCaml-uitvoerders, met een grens midden in een maand.
Het open punt voor het schema staat in het Result.

## Result (2026-09-23)

Alle drie helften zijn af. Twee zijn WF-regels geworden, de derde is
gebouwd in `ref` en clingo met een golden. Eén clausule van de clip kan
het corpus niet dragen, om een reden die buiten deze kaart ligt (open punt 1).

### (1) Grensvorm → WF-111

IR-SPEC ch. 4.6 heeft **WF-111** (**voorstel**): elke grens is een
datumliteral of een `AttributeRef` naar een scalair datumattribuut van het
eigen onderwerp van de vorm (ongenavigeerd, geen setlezing, `S ⊑ O`), en
heeft dagprecisie. Een positie zonder onderwerp (een dagsoortdefinitie)
laat dus alleen de literal toe. De volle grens-expressie staat als OPEN
wirebesluit in ch. 3.12, met de motivering van het besluit.
**Eigen technische toevoeging, ter beoordeling:** de dagprecisieclausule.
Beide OCaml-uitvoerders weigeren een datum-tijdgrens al, en ch. 3.10
spreekt alleen over kalenderdagen. Het besluit noemde de precisie niet.

Verifier-arm `wf111_bound` in `spec/verifier/lib/checks.ml`
(**voorstel**). Passerende golden: `jutterswet-premievenster` (vier
literals en de eigen `meldatum`). Negatief:
`corpus/negatief/wf111-periodegrens-parameterdatum`, verplaatst uit
`corpus/rood/periodegrens-parameterdatum`, zonder case-bestand en met een
herschreven NOTE. Een attribuutgrens op een SUPERtype valt al onder
WF-053: gemeten met een `Strandgoed ⊑`-variant van premievenster. Een
`objecttype` dat het supertype noemt wordt als navigatie geweigerd. Een
`objecttype` dat het onderwerptype noemt terwijl het attribuut op het
supertype staat, speelt in ref en clingo gelijk (6×2 PASS). Clingo hoefde
daarvoor niet te veranderen.

### (3) Tellende kwantor → WF-112

**WF-112** (**voorstel**): een venster zit nooit onder `GEEN` of een
tellende kwantor. Dat geldt als lid van een `Samengesteld` op elke diepte
en in `filter`/`toets` van een `Kwantorvouw`. Verifier-arm
`wf112_position` met een eigen diepe wandeling `cond_has_window`; er wordt
bewust geen code met ref gedeeld. Passerende golden: premievenster's
`borgpremie` (ALLE-lid). Negatief:
`corpus/negatief/wf112-periodevenster-tellende-kwantor`, dezelfde
procedure.

**De kwantorvouwmeting.** Gemeten op `jutterswet-schouwploeg`: de toets
van *bevoegd* vervangen door het kale venster, en apart door
`ALLE[gediplomeerd; venster]`, telkens onder alle vijf de kwantoren (case1,
asOfDate 2024-06-15):

| kwantor | oude verifier | nieuwe verifier | ref / clingo | exec-model | DRL-tekst |
|---|---|---|---|---|---|
| ALLE | rc 0 | schoon | PASS (6) | SKIP "matrix … een conditie van soort Periode" | PLAYED 1 |
| GEEN | rc 0 | WF-112 | weigering bij naam | SKIP | FAIL (antwoordt: afwezig) |
| TEN_MINSTE 1 | rc 0 | WF-112 | weigering | SKIP | PLAYED 1 |
| TEN_HOOGSTE 1 | rc 0 | WF-112 | weigering | SKIP | FAIL (antwoordt) |
| PRECIES 1 | rc 0 | WF-112 | weigering | SKIP | FAIL (antwoordt) |

De aanname "de vouw rijdt mee op dezelfde weigering" klopte voor de
DRL-tak NIET. Die tak weigerde de vouw niet, maar verlaagde het venster
onder alle vijf de kwantoren. Onder GEEN in een tijdvrije sessie
(asOfDate null) gaf hij `PLAYED 1` met *bevoegd* aanwezig: de
complementlezing die het besluit weigert. **Dezelfde WF-arm dekt de vouw.**
Een eigen arm is niet nodig, omdat de kwantoren dezelfde zijn en de
wandeling de vouw ingaat zoals ze een samenstelling ingaat. De regel dicht
daarmee een echt gat en herformuleert geen bestaande weigering.

### Rood-eerst en mutatie (WF-111/112)

    oude verifier  corpus/rood/periodegrens-parameterdatum/ir.json      verifier: PASS  rc=0
    oude verifier  corpus/rood/periodevenster-tellende-kwantor/ir.json  verifier: PASS  rc=0
    nieuwe         WF-111 FAIL /resolvedRegels/0/voorwaarde/vanaf: the vanaf bound of a time window is the parameter 'peildatum' — …  rc=1
    nieuwe         WF-112 FAIL /resolvedRegels/0/voorwaarde: a time window under GEEN or a counting quantifier: …  rc=1

    arm WF-111 uit:  FAIL: negatief case corpus/negatief/wf111-periodegrens-parameterdatum/ir.json passed the verifier (must violate WF-111)
    arm WF-112 uit:  FAIL: negatief case corpus/negatief/wf112-periodevenster-tellende-kwantor/ir.json passed the verifier (must violate WF-112)
    beide teruggedraaid, cmp ok

Corpusbrede nulmeting van de nieuwe verifier: alleen deze twee documenten
raken WF-111/112 (golden, rood en toka doorzocht).

De weigerarmen van `ref` en clingo blijven staan als faalsnel-hek achter de
verifier. Hun zin citeert nu WF-111/112. De suitegetuigen roepen eval en
translate rechtstreeks aan en gaan niet via de verifier, dus ze bleven
ongewijzigd groen (de naalden zitten in de nieuwe zinnen).

### (2) De clip → gebouwd

ch. 3.10 (**voorstel**) noemt nu de clausules. De evaluatiedatum doet niet
mee. De grenzen lezen zoals bij de poort, per onderwerp waar de grens een
eigen attribuut is. De restrictie snijdt SEGMENTEN en laat WAARDEN staan.
Een leeg venster (lege grens, `vanaf` na `tot`, of een venster dat de
tijdlijn mist) schrijft de lege tijdlijn, en een staand doel schrijft dan
niets (ch. 7.8). De zin "is to implement them first" is "implemented them
first" geworden. De ch. 3.12-rij beweegt mee. Twee posities blijven bij
naam geweigerd, bij de regel: het venster als samenstellings-LID op een
tijdlijndoel (OPEN, suite-gepind in beide) en een tijdlijn-KENMERKdoel
(rood `periodevenster-tijdlijnkenmerk`, nieuwe weigerzin en pins).

Uitvoering. `ref`: één grenslezer `venster_grenzen` voor poort én clip, en
`clip_venster` over het resultaat van pro-rata of dagtelling vóór de
ch. 7.8-toets. Clingo: `venster_grenzen` gedeeld met de poortarm, en
`schrijf_segment` knipt elk bucketsegment. Gringo kent geen min/max op
termen, dus elke grens levert twee regels, en het `eff`-atoom rijdt op
dezelfde bodies mee.

Golden `corpus/golden/jutterswet-pachtvenster` (handberekening hierboven,
vóór de eerste run; bijstelling naar rastergrenzen idem). Beide cases
speelden bij de eerste run in `ref` (4 en 7 verwachtingen) en in clingo.
Mutatieproeven, telkens teruggedraaid met `cmp`:

    M1 ref clip = identiteit      → 7 MISMATCH (o.a. P1.jaarpacht = [2024…)=10 [2025…)=12, P3.* niet leeg)
    M2 ref "tot en met" genegeerd → P1/P2.maandpacht [2025-04-01,2025-04-30) i.p.v. …05-01)
    M3 ref lege grens = open      → P2.zomerdagen = [2025-06-01,2025-07-01)=30, expected (empty)
    M4 ref venster ook als poort  → case1 (tijdvrij): P1.jaarpacht/maandpacht/zomerdagen carries no timeline
    C1 clingo clip uit            → P1.jaarpacht timeline = [19723,20089)=10 [20089,20454)=12 …
    C2 clingo "tot en met" uit    → P1.maandpacht [20179,20208) i.p.v. [20179,20209)

Drools, gemeten: exec `EXCLUDE transpiler SKIP: … periodevergelijking op
tijdsafhankelijk doel — §8.4.3 clip`, DRL `EXCLUDE drl-transpiler MARKER:
… TODO periodevergelijking op een tijdsafhankelijk doel (§8.4.3)`. De
golden staat in `EXPECTED_CHANNEL` en in `DRL_CHANNEL` (klasse C,
kop 10 → 11). `EXPECTED_PLAYED` (180) en `EXPECTED_DRL_PLAYED` (159)
bewegen NIET.

**Lensvondst, meteen gerepareerd.** Clingo schreef bij een
KENMERKTOEKENNING op een tijdsafhankelijk kenmerk stil een scalair
`heeft/2`-feit, waar `ref` weigert. Gemeten op het document van
`periodevenster-tijdlijnkenmerk` zonder venster: clingo `PASS`, ref
`EVALUATION FAULT: temporal characteristic "geborgen": temporal writes are
not implemented`. Clingo weigert nu in dezelfde woorden; er is een nieuwe
suitecheck. Geen golden of rode case bewoog.

### Tests (mandaat: de clip kantelt de weigergetuigen)

- `ref/test/test_ref.ml`: de drie clipweigeringschecks (attribuutdoel,
  fasestaart, kenmerkdoel) zijn vervangen door acht. Dat zijn het worked
  example, de evaluatiedatum die niet meedoet, de gesneden bucket die zijn
  waarde houdt, de snede over een bucketwand, het gemiste venster, de
  lidweigering met fasestaart, en het kenmerkdoel met de nieuwe zin.
  780 → 785.
- `backends/clingo/test/test_clingo.ml`: de twee clipweigeringschecks zijn
  vervangen door drie (de gesneden bucket, gelezen uit de `tl/5`-atomen,
  en de lidweigering met fasestaart), plus één nieuwe voor het kenmerkgat.
  480 → 482.

### Gates (elk los gedraaid, alle veertien groen)

1 PASS · 2 PASS (276 documenten = 277 − 2 rood + 1 golden) · 3 PASS (276
schoon; **433 negatief, +2**) · 4 PASS (635 casebestanden: −2 rood, +2
golden) · 5 PASS (785 bewerkte voorbeelden, +5; **593 cases, +2**) · 6 PASS
(482 suitechecks, +2; 593 cases) · 7 PASS (557 weerleggen de nietsdoener,
36 overlevers ongewijzigd) · 8 PASS (**42 rode cases, −2**; 84 fragmenten,
−4; Drools-been 9 fault-pinned, 33 niet-gedragen, −2) · 9 PASS (201
checks, 12 goldens) · 10 PASS (393 tests, 70 rgs-goldens) · 11 PASS
(29/29) · 12 PASS (played 180 = pin, DRL 159 = pin; kanaalvloeren 54 en
75, elk +1; JUnit 604) · 13 PASS (131 verdraaiingen over 27 goldens, 128
replays) · 14 PASS (130 checks, 2 goldens). De vloer van gate 8 (26) en
die van gate 3 (350) hoefden niet bij.

### Beschermd-pad-voorstellen

- `spec/IR-SPEC.md`: WF-111 en WF-112 (ch. 4.6), twee indexrijen, en de
  TimeWindow-rij in ch. 4.5.
- `spec/RUNTIME-SEMANTICS.md`: ch. 3.10 (welgevormdheid, vensterposities,
  grensvormen, de clipclausules), drie ch. 3.12-rijen, de ch. 5.11-rij en
  de getuigenrij van `Periode`.
- `spec/verifier/lib/checks.ml`: alleen de twee nieuwe armen.
- `scripts/check-rood.sh`: twee uitsluitingsregels weg, één reden
  bijgewerkt.
- `scripts/check-drools.sh`: +1 regel in elke kanaallijst en de kop van
  klasse C. Geen pin bewoog.

### Open punten en kaartvoorstellen

1. **Een gesneden bucket kan geen corpusverwachting zijn.**
   `uchar-case-v0.schema.json` (`timelineValue`) legt ook een verwachte
   tijdlijn op het raster. De clausule "snijdt segmenten, houdt waarden"
   is daarom alleen suite-gepind. Maintainerbesluit nodig: óf het schema
   laat verwachtingen van het raster af (schema en `ref/lib/case.ml`), óf
   v1 eist voor een clip grenzen op het raster van het doel (een WF-regel).
2. **Het venster als lid van een samenstelling op een tijdlijndoel.** OPEN
   en benoemd geweigerd. ch. 3.10 zegt niet wat de andere leden dan doen.
3. **Drools (kaartvoorstel, `backends/drools` viel buiten het mandaat).**
   (a) De exec-tak weigert het venster in een vouwtoets ook onder ALLE,
   waar ref, clingo en DRL spelen. Geen golden draagt dat vandaag.
   (b) De DRL-tak verlaagt een venster in een vouw onder GEEN en de
   tellende kwantoren. De verifier houdt zulke documenten nu tegen, maar
   een faalsnel-weigering bij naam in de tak zelf hoort erbij.
4. **Staand tijdlijndoel + clip + case-invoer.** Gebouwd in beide (de
   ch. 7.8-samenstelling), maar geen corpuscase draagt het.
5. De ch. 5.11-getuige "`vanaf` vóór `tot`" blijft "none yet". Hij komt met
   het open wirebesluit over de grens-expressie.
6. Niet gemeten: of de RegelSpraak-frontend een parametergrens emitteert.
   Doet hij dat, dan houdt de verifier het document voortaan tegen.

### Registervoorstel `spec/CONSTRUCTS.md` (niet aangeraakt; de hoofdsessie landt)

Aan **rij 11** toe te voegen, achteraan:

*"**Decided and landed (2026-09-23, card `tijdvenster-conditie-normeren`,
maintainer decisions of that day):** the §8.4.3 period condition's two
remaining halves. (1) BOUND SHAPE — WF-111: a bound is a date literal or a
scalar date attribute of the form's own subject, un-navigated, of day
precision; the general date expression is an OPEN wire decision
(RUNTIME-SEMANTICS ch. 3.12). Negatief `wf111-periodegrens-parameterdatum`
(until then red case `periodegrens-parameterdatum`). (2) POSITION — WF-112:
a window never sits under GEEN or a counting quantifier, as a composite
member at any depth or inside a quantifier fold's filter or test; negatief
`wf112-periodevenster-tellende-kwantor` (until then red). Measured that day:
the Drools DRL-text branch answered a window inside a fold under all five
quantifiers, the complement included, so the rule closes a live divergence.
(3) The CLIP on a temporal attribute target (RUNTIME-SEMANTICS ch. 3.10's
second case, the row-11/29 delta) is executed by `ref` and the ASP backend
over the ch. 3.7 pro-rata and ch. 3.8 day-count producers: the evaluation
date plays no part, segments are cut and values kept, an empty or leeg
window writes the empty timeline; golden `jutterswet-pachtvenster`, refused
by name in both Drools branches. Still refused by name: the window as a
composite member on a temporal target (OPEN) and a temporal characteristic
target (red case `periodevenster-tijdlijnkenmerk`; no temporal
characteristic is written, ch. 3.11). The segment-cutting clause is
suite-witnessed only: the case format pins expected timelines to the
declared grid."*

Aan **rij 29** (die de §8.4.3-clipdelta met rij 11 deelt), achteraan:

*"The §8.4.3 clip half of this row's delta is executed since 2026-09-23
(see row 11)."*
