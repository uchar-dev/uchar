# ref-periodeconditie-arm: de laatste bodemzin-drager onder de condities

Gemeten bij kaart `v4-verre-rol-getuige` (2026-09-20): de voormalige
restarm over conditiesoorten in `ref/lib/eval.ml` vangt sindsdien alleen
nog `Periode_conditie`, als expliciete constructor gematcht, met de
ongewijzigde bodemzin `condition kind not implemented in this card` uit
de gate-8-lijst (de redenkolom in `scripts/check-rood.sh` regel ~145 is
bij die kaart al op deze werkelijke stand gebracht). De zin beschrijft
daarmee één bekende vorm in plaats van een restklasse — precies de
toestand die de bodemzinnen-discipline wil opheffen: de cure is een
benoemde arm, nooit een geherformuleerde bodem. Clingo's evenknie
(`condition kind not translated`, vóór de v4-kaart door dezelfde verre
vorm gedeeld) verdient dezelfde meting.

## Acceptatie (bij oppakken aanscherpen)

- (a) Hermeten wat de twee OCaml-uitvoerders en beide Drools-takken
  vandaag met een scalaire periodeconditie doen; de bestaande metingen
  liggen verspreid (de WF-097-rij voor de clips, het tijdelijke derde
  hek van WF-109 voor de feitcreatie-guard, de §8.2-notitie in de
  kenmerkcheck-kaarten).
- (b) Benoemde armen in `ref` en clingo — echte semantiek of een
  weigering per vorm — waarna de 36-lijst in `scripts/check-rood.sh`
  de betrokken bodemzin(nen) verliest of hun bron/reden bijstelt
  (beschermd pad, voorstel).
- (c) Samenhang benoemen met `backlog/new/20260920-drl-periode-zink.md`
  (de DRL-tak zinkt op een compile-fout in dezelfde familie): één
  periodefamilie-kaart mag beide nemen, samen met het vallen van het
  derde WF-109-hek.

May touch: `ref/**`, `backends/clingo/**`, `scripts/check-rood.sh`
(**voorstel**), `spec/**` (**voorstel**), `corpus/**`,
kaartverplaatsing.
