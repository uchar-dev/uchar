# writers-per-day-beside-input: invoer en schrijvers per dag op een tijdlijndoel

*Opgesteld door de hoofdsessie op 2026-11-04, na de herschrijving van RUNTIME-SEMANTICS ch. 3 (ch. 3.12 rij 10) en een meting tegen
ALEF (`werk/writers-and-input/verslag.md`, buiten de repo).*

## Aanleiding

Een dag van een tijdlijndoel met invoer uit de case leest nu de invoer of leeg, afhankelijk van het aantal schrijvers:

- één schrijver: de geschreven tijdlijn vervangt het doel helemaal, ook op dagen waar zij leeg leest (RS ch. 3.11 "Leeg in a write",
  ch. 5.7 "On a target of one writer …", ch. 7.8 "the timeline arm", besluit van 2026-08-21: invoer en berekende segmenten mengen nooit);
- twee of meer schrijvers: een schrijver beslist alleen de dagen waarop zijn tijdlijn een waarde leest, en op een dag die geen schrijver
  beslist, staat de invoer (ch. 5.7 "Two writers per day", IMPLICIT-TIME punt 5, VB 5.2).

ALEF voegt elke schrijfactie per moment samen met de huidige waarde, de invoer inbegrepen, ook bij één schrijver. Gemeten: invoer 10 over
2025 en één regel die 20 schrijft in maart–mei geeft in ALEF 10 in januari en juli; uchar geeft daar leeg. De specificatie zwijgt (§9.3).

**Besluit van de eigenaar (2026-11-04):** per dag, ook bij één schrijver (`spec/proposals/IMPLICIT-TIME.md`, punt 5).

## Goal

Op een tijdlijndoel beslist elke schrijver, ook een enige, alleen de dagen waarop zijn tijdlijn een waarde leest. Op een dag die geen
schrijver beslist, staat wat het doel vóór de schrijvers hield: zijn invoer of leeg. Op een dag die een schrijver beslist, staat zijn
waarde; twee schrijvers die een dag verschillend beslissen, blijven EF-016. Dat geldt in ref, clingo en beide Drools-branches, en in RS als
één regel in plaats van drie.

Ongewijzigd: een scalair doel (ch. 1.2, de standing norm van ch. 7.8 voor een scalair), een kenmerkdoel (ch. 3.12 rij 7: een
case-tijdlijn van een kenmerk dat een regel schrijft, is niet gedefinieerd), de ASP-afschermingen die er niet over gaan.

## Acceptance

- (a) Rood eerst, letterlijk: het gemeten geval (invoer 10 over 2025, één schrijver 20 op maart–mei) faalt op main in ref, clingo en
  beide Drools-branches met de verwachting 10, 20, 10 en speelt daarna; hetzelfde met de waarde 10 (de schrijver schrijft wat de invoer
  heeft). Per nieuwe bewaking een mutatie die een test rood maakt.
- (b) Het tweede uitgewerkte voorbeeld van het anker `jutterswet-borgdeel` (`case6-berekend-over-input`: een vouw die jan–feb 12 beslist
  over een invoer met een segment in maart) verandert door het besluit: de invoer staat dan in maart. De nieuwe verwachting staat als
  voorstel in het anker, met oud en nieuw in het Result. Elke andere golden waarvan een verwachting verandert, staat met oud, nieuw en
  reden in het Result; een golden die alleen door de oude regel slaagde of faalde, wordt genoemd.
- (c) RS als huidige stand (voorstel): ch. 3.11 "Leeg in a write" één regel voor het tijdlijndoel; ch. 5.7 de zinnen over één en twee
  schrijvers; ch. 7.8 "the timeline arm" per dag (het besluit van 2026-08-21 herzien op 2026-11-04, in één zin); ch. 3.12 rij 10 weg of
  teruggebracht tot wat open blijft; een uitgewerkt voorbeeld met absolute waarden dat `ref/test` letterlijk overneemt.
- (d) De no-op-lijst van gate 7 en de kanaallijsten van gate 6, 8 en 12 kloppen; een verschuiving staat met reden in het Result.
- (e) Voorgestelde tekst voor CONSTRUCTS (de rij die de standing norm en de tijdlijnschrijver draagt, en rij 11) als huidige stand.
- (f) `make gates` groen.

May touch: `ref/`, `backends/clingo/`, `backends/drools/**`, `spec/RUNTIME-SEMANTICS.md` (voorstel), `corpus/golden/` (alleen verwachtingen
die het besluit verandert, met reden; `anchor/` als voorstel), `corpus/red/`, `scripts/` (alleen lijsten die verschuiven; voorstel),
kaartverplaatsing.

Out of scope: een defaults-groep op een tijdlijndoel (meet hoe ze nu leest en of het corpus er een heeft; geen wijziging, vraag aan de
hoofdsessie als het besluit haar raakt); de dienst van de distributie (die neemt geen tijdsafhankelijk attribuut als invoer).

## Omvang

Middel.

## Result
