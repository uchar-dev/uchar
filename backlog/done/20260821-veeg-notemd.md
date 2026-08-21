# Veeg: NOTE.md-precisering bij division-by-numeric-zero

Besluit maintainer 2026-08-21 (besluitenronde 2, punt 3).

Goal: corpus/rood/division-by-numeric-zero/NOTE.md preciezer over
ch. 1.4 punt 4 / PLAN §9 besluit 7: de hard fault is de v0-mirror
die het besluit zelf als spiegel handhaaft; v1 routeert naar het
§9.5-kanaal (de tracked delta die ASP-TRANSLATION S6/S14 nu draagt).
Ook division-abs-by-numeric-zero meenemen als die een NOTE mist.

Acceptance: de NOTE's dragen de juiste attributie; gate 1 en 8
groen.

May touch: `corpus/rood/division-by-numeric-zero/NOTE.md`,
`corpus/rood/division-abs-by-numeric-zero/`, `backlog/`.

Out of scope: al het overige.

Result (2026-08-21): de NOTE van division-by-numeric-zero attribueert
de hard fault nu precies — de v0-mirror die ch. 1.4 punt 4 (besluit 7)
als spiegel handhaaft, met de v1-kanaalroute als getrackte delta en de
notitie dat de flip te zijner tijd in beide executors tegelijk gaat;
division-abs-by-numeric-zero kreeg zijn eigen NOTE (deelt div_gen/het
rounded-division-pad, pint dat de abs-arm de rand zelf draagt). Gates
1 en 8 groen; de overige zes raken deze bestanden niet.
