# empty-write-decides-nothing: een lege waarde beslist niets, ook op een scalair doel

*Opgesteld door de hoofdsessie op 2026-11-04, na de kaart `writers-per-day-beside-input` (RUNTIME-SEMANTICS ch. 3.12 rij 10) en de
meting tegen ALEF (`werk/writers-and-input/verslag.md`, buiten de repo, geval 1c en 1d).*

## Aanleiding

Op een tijdlijndoel beslist een schrijver de dagen waarop zijn tijdlijn leeg leest niet (besluit 2026-11-04). Op een scalair doel hangt
het nog af van de vorm: een **staande** schrijver (RS ch. 7.8, besluit 2026-08-20: één versievrije rekenende regel als enige schrijver)
schrijft bij een lege waarde niets en de invoer blijft staan; elke andere schrijver (naast een tweede schrijver, of met een
versievenster) schrijft leeg over de invoer (RS ch. 1.2 Assignment, ch. 3.11 "Leeg in a write", ch. 5.7 "a leeg one included where the
writer is not standing"). ALEF's standaardevaluatie slaat een lege waarde altijd over (`merlin/lazy/MLazyProperty.java` r. 53–56):
gemeten met invoer 40 en twee schrijvers, waarvan één met leeg vuurt, geeft ALEF 40 en ref leeg. De specificatie zegt in §9.1 dat een
gelijkstelling aan leeg leeg oplevert, en zwijgt over invoer (§9.3).

**Besluit van de eigenaar (2026-11-04):** een schrijver die met een lege waarde vuurt, beslist niets, op elk doel. Een scalair is één
segment over alle tijd (ch. 3.1), dus dit is de regel van het tijdlijndoel.

## Goal

Op een scalair doel schrijft een schrijver die vuurt met een lege waarde niets: het doel houdt wat het hield, zijn invoer of leeg. Dat geldt
voor elke schrijver: een staande, een naast een tweede schrijver, een met een versievenster. Twee schrijvers waarvan één leeg en één een
waarde geeft, geven die waarde (geen EF-016); twee met verschillende waarden blijven EF-016. In ref, clingo en beide Drools-branches, en in
RS als één regel voor het scalaire en het tijdlijndoel. Waar het staande domein (`Desugar.standing_targets`) dan alleen nog dit punt
droeg, valt het weg; waar het nog iets anders draagt (de routering van invoer in clingo, de desugaring van ch. 7.7), blijft het met die
taak.

Ongewijzigd: een defaults-groep (leeg valt daar al door), een initialisatie (ch. 7.7, §9.6), een kenmerkdoel, een verdeling en een
toekenning bij het maken van een object; meet ze waar het mechanisme gedeeld is, en noem een raakvlak als vraag.

## Acceptance

- (a) Rood eerst, letterlijk: met invoer 40 (1) twee schrijvers, één vuurt met leeg en de ander vuurt niet → 40; (2) een schrijver met een
  versievenster die met leeg vuurt → 40; (3) twee schrijvers, één leeg en één 50 → 50 zonder fout. Elk faalt op main in ref, clingo en
  beide Drools-branches waar uchar nu anders leest, en speelt daarna. Per nieuwe bewaking een mutatie die een test rood maakt.
- (b) De corpusmeting: elke verwachting die verandert (goldens en rode casussen), met oud, nieuw en reden; `anchor/` als voorstel.
- (c) RS als huidige stand (voorstel): ch. 1.2 (Assignment), ch. 3.11 "Leeg in a write", ch. 5.7 de schrijversregel, ch. 7.8 de standing
  norm (het besluit van 2026-08-20 verbreed op 2026-11-04, in één zin), ch. 3.12 rij 10 weg; een uitgewerkt voorbeeld met absolute waarden
  dat `ref/test` letterlijk overneemt.
- (d) De no-op-lijst van gate 7 en de kanaallijsten van gate 6, 8 en 12 kloppen; een verschuiving staat met reden in het Result.
- (e) Voorgestelde tekst voor CONSTRUCTS (de rij van de standing norm en de delta tegen v0, die leeg over de invoer schreef) als huidige
  stand.
- (f) `make gates` groen.

May touch: `ref/`, `backends/clingo/`, `backends/drools/**`, `spec/verifier/` (alleen het staande domein; voorstel),
`spec/RUNTIME-SEMANTICS.md` (voorstel), `corpus/golden/` (alleen verwachtingen die het besluit verandert; `anchor/` als voorstel),
`corpus/red/`, `scripts/` (alleen lijsten die verschuiven; voorstel), kaartverplaatsing.

Out of scope: de dienst van de distributie; de vormen onder "Ongewijzigd"; een regel die een attribuut bewust leegmaakt of "onbekend"
schrijft (RegelSpraak heeft er geen vorm voor; kaart `20261104-flavours-of-empty`).

## Omvang

Middel.

## Result
