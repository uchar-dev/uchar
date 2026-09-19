# rood-fragment-catchall-tripwire: een pin mag geen bodemzin citeren

Gemeld door de som-catchall-kaart (2026-09-19, "Gemeld, niet gekaart"):
de kaart genas een rode case die de catch-all-bodemzin van de
ASP-vertaling als `expectedFault`-fragment pinde — waarmee de
geen-corpusgeval-claim in die zin live onwaar was. Niets verhindert de
recidive: een toekomstige rode case kan opnieuw een catch-all-zin
pinnen, en niemand merkt het tot een lens erover struikelt.

De meting die de kaart meegaf: een wegwerp-scan van alle 29 rode
fragmenten tegen de vier bekende catch-all-zinnen van `ref` en clingo
geeft op de huidige stand NUL treffers — en had er vóór die kaart
precies één gegeven. De tripwire is dus vandaag goedkoop en had het
defect gevangen.

## Acceptatie (bij oppakken aanscherpen)

- (a) Inventariseer de catch-all-/bodemzinnen van beide uitvoerders
  (de zinnen die "niet vertaald/niet gedragen" zeggen zonder de vorm
  te noemen) en leg ze als lijst vast op één plek.
- (b) Een check — in `scripts/check-rood.sh` (voorstel, beschermd pad)
  of in de suite van de backend — die faalt wanneer een
  `expectedFault`-fragment een substring van zo'n bodemzin is, met de
  naam van de rode case en de zin in de foutmelding.
- (c) Mutatieproef: een tijdelijke rode pin op een bodemzin laat de
  check falen; teruggedraaid. Alle veertien gates groen.

May touch: `scripts/check-rood.sh` (voorstel) of `backends/*/test/**`,
kaartverplaatsing. NB: raakt geen kanaallijsten; parallelveilig naast
DrlTranspiler-kaarten zolang niemand check-rood.sh anders raakt.
