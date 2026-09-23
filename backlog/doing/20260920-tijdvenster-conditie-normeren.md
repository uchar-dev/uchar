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
