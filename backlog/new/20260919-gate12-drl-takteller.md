# gate12-drl-takteller: de DRL-tekst-tak telbaar maken

Gemeten spanning (asp-ch102-bouwer, 2026-09-19): gate 12's
`EXPECTED_PLAYED`-gelijkheid en kanaallijst tellen uitsluitend het
exec-model-kanaal. De vijf nieuwe ch.-10.2/10.6-goldens spelen alleen
daar; dat de DRL-tekst-tak ze om drie pre-existente redenen niet
speelt, staat alleen in het reden-commentaarblok van de bump — en
commentaar is documentatie, geen check. Het vierkant is op die vormen
groen door de takkeuze van de gate, niet door takdekking. De
DRL-PLAYED-stand (nu 132, na de twee Let-kaarten van 2026-09-19) werd in het pariteitsprogramma handmatig in
Results bijgehouden; niets hekt hem tegen stille krimp behalve gate
13's twist-vloer (DRL-PLAYED ≥ 13, alleen over twist-goldens).

Voorstel om te wegen: een eigen `EXPECTED_DRL_PLAYED` in
`scripts/check-drools.sh` (gelijkheid, zoals de exec-pin, met
reden-blok bij elke beweging) plus een reden-dragende
niet-spelerslijst voor de DRL-tak, of — lichter — alleen de
gelijkheidspin. Kosten: elke kaart die een golden in de DRL-tak laat
spelen of verliest raakt voortaan het beschermde script (dat is bij de
exec-pin al zo en bleek werkbaar).

## Acceptatie (bij oppakken aanscherpen)

- (a) Besluit gelijkheidspin / vloer / beide, met de reden erbij.
- (b) De gate draait de DRL-sweep zelf (nu draait hij de replay alleen
  exec) — meet de looptijdkosten en weeg ze.
- (c) Beschermd-padvoorstel voor `scripts/check-drools.sh`; alle
  veertien gates groen; de bestaande exec-pins bewegen niet.

May touch: `scripts/check-drools.sh` (voorstel — beschermd pad),
kaartverplaatsing. Besluit aan de maintainer.

## Maintainersbesluit (2026-09-19): de volle vorm

Besluit (a): een `EXPECTED_DRL_PLAYED`-gelijkheidspin mét een
reden-dragende niet-spelerslijst voor de DRL-tak; de gate draait de
DRL-sweep zelf en de looptijdkosten worden gemeten en gemeld in het
Result. Uitvoerbaar met dit mandaat (beschermd pad via de
gebruikelijke lens- en maintainersmerge-route).
