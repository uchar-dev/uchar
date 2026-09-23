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
