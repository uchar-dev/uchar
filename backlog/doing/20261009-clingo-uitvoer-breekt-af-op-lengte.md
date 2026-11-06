# clingo-uitvoer-breekt-af-op-lengte: de ASP-runner breekt af op een atoom met een string van bepaalde lengtes

*Gevonden op 2026-10-09 door de bouwer van de laatste fixronde van
`20261009-creatiebudget-ref-en-clingo` (bijvangst, gemeten) en door de
hoofdsessie vastgelegd volgens de afspraak dat een bestaande bug een kaart krijgt.
Gemeten met clingo 5.8.2 en `backends/clingo` op die branch en op main
`e0d67d6a` (zelfde gedrag); de proefbestanden en het genereerscript staan in
`/tmp/lens-bugs/w9/fix-850W/nest/` (`gen.py <n>`).*

## Aanleiding

De runner van de ASP-backend leest het model van clingo uit de JSON-uitvoer
(`--outf=2`). Clingo 5.8.2 breekt die uitvoer af (de runner meldt `clingo
killed`) op een atoom met een string van bepaalde lengtes, los van hoe diep een
term genest is:

- `a("x…x")` breekt af bij een string van 1 017, 2 040, 3 063, 5 109, 7 155 en
  8 178 tekens, en bij geen andere lengte van 1 tot 8 999;
- `heeft(oc0^n("W1"),"k")` breekt alleen af bij n = 201 (195–200 en 202–229
  gaan goed); `val(…)` alleen bij n = 202;
- atomen zonder string, en de tekstuitvoer (`--outf=0`), breken bij geen van die
  lengtes af.

Op een verifier-schoon document: een keten van niet-recursieve objectcreaties
over verschillende objecttypen, waarvan de laatste een kenmerk zet, breekt in
clingo alleen af bij n = 187 (`clingo killed`); n = 200 en 201 spelen; ref speelt
ze allemaal. Een gemaakt object is in clingo een term die per creatie een niveau
dieper genest is, zodat de lengte van zijn string met de keten meegroeit.

De nestgrens `oc_nest` = 190 die `20261009-creatiebudget-ref-en-clingo` in stap 1
invoerde, rust op de lezing "clingo breekt af bij ~201 niveaus" en dekt dit dus
niet: hij weigert ketens die clingo kan afdrukken, en laat de keten van 187 door.

## Acceptatie (bij oppakken aanscherpen)

- (a) De runner leest de tekstuitvoer van clingo (voorkeur van de bouwer; raakt
  de runner en ~37 plaatsen in de suite), of benoemt de afbraak luid met de
  oorzaak, zodat een gebruiker niet `clingo killed` krijgt.
- (b) De nestgrens `oc_nest` wordt herzien: weg als (a) de afbraak oplost, of
  vervangen door een grens die de werkelijke oorzaak dekt.
- (c) Checks in `backends/clingo/test` voor een lengte die afbrak (de keten van
  187), rood eerst.
- (d) Gate 6 groen; alle gates van `CLAUDE.md` groen.

## Omvang

Klein tot middel. May touch: `backends/clingo/**`, de spectekst over de
nestgrens in RUNTIME-SEMANTICS ch. 5.5 als die er dan staat (voorstel),
kaartverplaatsing.

## Raakvlakken

Na `20261009-creatiebudget-ref-en-clingo` (de nestgrens en haar tekst).
