# De Drools-backend leest de eenheid van de annotatie, niet van de declaratie

Goal: de Drools-backend de **effectieve-typewet** laten volgen op de
attribuutlees-positie — de eenheid van een `AttribuutRef` komt uit de
**declaratie** waar hij uit leest, niet uit de `type`-annotatie die naast hem
op de wire staat (`spec/RUNTIME-SEMANTICS.md` §2.10). Vandaag speelt de
backend een document dat de verifier, de referentie-interpreter én de
ASP-backend alle drie weigeren, en levert het de "juiste" getallen — omdat hij
de mismatch nooit ziet.

## De meting (2026-09-06, pre-existent — deze kaart introduceert niets)

Startpunt is de ongewijzigde golden `corpus/golden/jutterswet-schouwrooster`.
Daarin telt `schouwplicht = vaste schouwlast + extra schouwlast`, beide
gedeclareerd met `eenheid schouwdagen`; de `AttribuutRef`s in de berekening
dragen dezelfde eenheid als `type`-annotatie.

**De mutant:** alleen de **declaratie** van één attribuut wordt verdraaid
(`objecttypen.Strandmeester.attributen["extra schouwlast"].eenheid`), de
annotaties op de leesposities blijven onaangeraakt. Twee sterktes gemeten,
allebei dezelfde uitkomst:

| mutant | verifier | ref | clingo | Drools |
|---|---|---|---|---|
| declaratie → `dagen` (factor 1) | **WF-091 FAIL** | **FAULT** (case1) | **FAULT** (beide) | **PLAYED 2** |
| declaratie → `uur` (factor 3600 naar `seconde`) | **WF-091 FAIL** | **FAULT** (case1) | **FAULT** (beide) | **PLAYED 2** |

Letterlijk:

- verifier: `WF-091 FAIL /resolvedRegels/0/waarde/rechts/type: unit mismatch:
  attribute reference 'extra schouwlast' is typed 'schouwdagen' (factor 1 to
  'schouwdagen'), but the declaration it reads from is 'uur' (factor 3600 to
  'seconde')`
- ref: `EVALUATION FAULT: incompatible units: schouwdagen vs uur`
- clingo: `TRANSLATION FAULT: non-identical units under an additive join are
  not translated`
- Drools: `PLAYED 2` — beide cases spelen, met **exact** de gepinde
  verwachtingen van de ongemuteerde golden (15 / 24 / 20 en 9).

Dat de tweede mutant — een eenheid uit een heel ander stelsel, met factor 3600
— dezelfde getallen oplevert als de eerste, is het beslissende punt: de
backend consulteert de declaratie op deze positie **niet**, ook niet om te
converteren. Hij neemt de wire-annotatie. Ref bewijst het spiegelbeeld: die
noemt in zijn foutmelding juist beide kanten (`schouwdagen vs uur`), dus ref
leest de declaratie wél.

Waarom `ref case2` in beide rijen PASS geeft: in `case2` is de tweede operand
leeg, dus de optelling zet nooit twee gevulde eenheden naast elkaar en de
vergelijking wordt nooit bereikt. Dat is de coalescing-helft van `plus`
(`RUNTIME-SEMANTICS.md` ch. 1.2), geen zwakte in de meting — clingo weigert
ook die case, omdat hij op vertaaltijd oordeelt.

## Waarom geen bestaande gate dit ziet

- **Gate 3** (verifier) draait niet over mutanten; hij ziet alleen de
  welgevormde corpusdocumenten, en die zijn WF-091-schoon.
- **Gate 12** (Drools) speelt alleen die welgevormde documenten. Een document
  dat de verifier afwijst komt er nooit langs.
- **Gate 13** (annotatie-onverschilligheid) kan het **per constructie** niet
  zien: die houdt de declaraties byte-identiek en verdraait uitsluitend
  `type`-annotaties, op documenten die welgevormd blijven. Precies de omgekeerde
  richting van dit lek. Een gate-13-twist en deze mutant zijn elkaars spiegel:
  de eerste vraagt "reproduceert de consument de uitkomst als de annotatie
  liegt?", de tweede "merkt de consument het als de annotatie en de declaratie
  elkaar tegenspreken?".

## Wat deze kaart moet opleveren

- De Drools-backend leidt de eenheid van een attribuutlees-positie af uit de
  declaratie (de effectieve-typewet, `RUNTIME-SEMANTICS.md` §2.10), zoals ref
  dat doet — in **beide** transpilerbranches (de exec-model-tak die gate 12
  speelt en de klassieke DRL-tekst-tak).
- Een backend-eigen witness-test (JUnit, additief) die de mutant hierboven
  vastlegt: een document met een tegensprekende declaratie/annotatie MOET niet
  stilzwijgend de "juiste" waarde opleveren. Of de backend dan faalt, weigert,
  of markert is de ontwerpkeuze van deze kaart — **maar niet: stil doorspelen**.
  Schrijf de keuze op.
- Gate 12 blijft groen met ongewijzigde speelvloer en ongewijzigde
  divergentielijst: de corpusdocumenten zijn WF-091-schoon, dus een correcte
  afleiding mag daar niets bewegen. Beweegt er wél iets, dan is dat de echte
  vondst en hoort ze in de kaart.
- Gate 13 blijft groen met ongewijzigde tellingen.

## Acceptance

- De twee mutanten hierboven, opnieuw gebouwd uit de dan-geldende golden,
  worden door de Drools-backend **niet** stil gespeeld; het rapport noemt de
  reden.
- De backend-JUnit-suite groen met een hoger, niet-nul testaantal en nul
  skips.
- Alle dertien gates groen, met de tellingen genoteerd.

May touch: `backends/drools/**`, `backlog/`.

## Out of scope

- **De verifier.** WF-091 doet zijn werk: hij wijst beide mutanten af met de
  positie en beide eenheden erbij. Er is hier niets aan de regel te repareren.
- Ref en clingo: die weigeren al, elk in hun eigen register.
- Elke uitbreiding van gate 13. De blinde vlek hierboven is een *eigenschap*
  van die gate (declaraties byte-identiek houden is haar hele opzet), geen
  fout erin. Of er een aparte harnas moet komen die de omgekeerde richting
  verdraait, is een eigen vraag en een eigen kaart.
