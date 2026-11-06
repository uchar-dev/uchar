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

## Result

*Gebouwd op 2026-11-06 op branch `card/text-output-foldfilter` (worktree `wt/clingo-round2`). Logs onder
`uchar-dev/werk/clingo-round2/logs/`, proefbestanden onder `uchar-dev/werk/clingo-round2/k2/`.*

Opnieuw gemeten met clingo 5.8.2 (de versie op deze machine): `a("x…x")` breekt onder `--outf=2` af (exit 134,
SIGABRT) bij een string van 1017, 2040, 3063 en 5109 tekens, niet bij 1016 en 1018; `--outf=0` bij geen ervan.
`heeft(oc0^n("W1"),"k")` breekt onder JSON af bij n = 201 en 202, de tekstuitvoer niet, ook niet bij n = 10 000.
De oorzaak is de lengte van het geschreven atoom in JSON, met escapes: 1024 + 1023k tekens (1017 + 7 = 1024 en
201 niveaus `oc0(` = 1020 + 4 escapes = 1024). De lengte van de keten die afbreekt hangt dus af van de namen: met de
namen van de proef (`Schakel<i>`, label `S1`, kenmerk `bereikt`) breekt een keten van 451 niet-recursieve
objectcreaties af (JSON-atoom 3070 tekens), 449, 450, 452 en 453 spelen (`k2/chain.py`, `k2/search-json.txt`).

- **(a)** `Solve.run_clingo` leest de tekstuitvoer, één atoom per regel (`--outf=0 --out-ifs='\n'`);
  `Model.models_of_text` en `Model.model_of_text` vervangen `model_of_outf2`, ook in `Solve.diagnostic` en in de
  runner. De keten van 451 speelt nu (`clingo: PASS`).
- **(b)** De nestgrens `oc_nest` is weg, met `nest/2`, haar `viol("term nesting bound reached …")` en de zin in
  RUNTIME-SEMANTICS ch. 5.5 (beschermd pad; de kaart noemt die tekst). De creatiediepte per stratum (`oc_budget`,
  ch. 5.5) blijft en begrenst de grounding. De README beschrijft de tekstuitvoer en de oorzaak onder "Run"; de twee
  beperkingen over de afbraak en de nestgrens zijn weg.
- **(c)** Rood eerst (`logs/k2-red-suite.log`, de suite op de oude code: 8 FAILURES): de zes lengtes 1017 … 8178
  via `Solve.run_clingo` en de modellezer (`clingo ended by signal`), de keten van 451 (`clingo killed`), en de
  recursieve keten 100 + 1 + 100 over drie strata, 201 niveaus diep (UNSAT op de nestgrens; dit frame vervangt het
  frame dat de nestgrens met `oc_nest` = 60 vastzette). Groen na de bouw: `test_clingo: 2769 checks, 0 failures`
  (`logs/k2-green-suite.log`).
- **(d)** Gates 6, 8 en 13 los gedraaid (`logs/k2-gates.log`); `make gates` aan het eind van de ronde.

### Vervolg: ref speelt de lange keten traag

Gevonden bij het meten van (c): ref speelde de keten van niet-recursieve creaties traag, n = 50 in 0 s, 100 in
2,4 s, 150 in 11,1 s, 451 na 25 minuten nog niet klaar (gestopt). Op verzoek van de hoofdsessie in deze branch
gemeten en opgelost. Een profiel (`sample` op de run van n = 150, `k2/sample150.txt`) legde 1727 van de 2925
monsters in `Ir_v0.Checks.is_sub`, en daaronder bijna alles in `find_ci` met `norm`: elke vraag S ⊑ T van ref
(`Eval.isa`) zoekt de gedeclareerde typen lineair af en normaliseert elke naam, en ref stelt die vraag per instantie
en vorm in elke generatie; met n typen, n instanties en n generaties groeit dat als n⁴. De reparatie onthoudt het
antwoord per document (`Eval.isa`, een tabel per fysiek document); het antwoord hangt alleen van de declaraties af,
dus het gedrag verandert niet. Tijden (zelfde machine, `k2/chain.py`, `logs/f2-ref-chain-after.log`):

| n | vóór | na |
|---|---|---|
| 100 | 2,4 s | 0,3 s |
| 150 | 11,1 s | 0,7 s |
| 200 | – | 1,6 s |
| 451 | > 25 min | 19,0 s |

Gedrag byte-gelijk: de runner vóór en na over elke case van `corpus/golden` en `corpus/red`, gewoon en (golden) met
`--trace`, 1216 en 1131 runs, 0 verschillen (`k2/compare_ref.sh`, `logs/f2-compare-ref.log`). Het suiteframe van
451 draait alleen clingo: ook 19 s is te veel voor één suitecheck. Gates 5, 6, 8 en 17 los gedraaid na de
reparatie, alle rc=0 (`logs/f-gates.log`).

Eindrun van de ronde: `make gates` op `2df79f6a3`, alle zeventien gates en `after-run` rc=0, red=0, 435 s
(`out/gates/20261106-164133/summary.txt` in de worktree, log `uchar-dev/werk/clingo-round2/logs/gates-final.log`).

### Rebase en DRL-pins

Op verzoek van de hoofdsessie gerebased op main `3df621a37` (de Drools-opruimronde). Conflicten alleen in de
boekhouding: `EXPECTED_PLAYED` wordt de som (main 355, plus de drie goldens van deze ronde = 358, alle
commentaarregels behouden), de RETIRED-lijst van gate 12 in `CLAUDE.md` noemt DROOLS-045/046, DROOLS-058 en
DROOLS-183, en in RUNTIME-SEMANTICS ch. 5.5 blijft main's nieuwe meting van `jutterswet-erflijn` staan zonder de
zin over de nestgrens. De `make gates`-runs hierboven (red=0) liepen vóór de rebase, op de toen geldende hashes.
`DrlTextPinWitnessTest` met `-Duchar.drlpin` schreef drie nieuwe pins, voor `jutterswet-tekst-van-een-waarde`,
`jutterswet-vouwfilter-buiten-de-beurt` en `jutterswet-kwantorvouw-en-unie`; geen bestaande pin veranderde (ook niet
door `rsTekst`: geen golden van main heeft een tekstdeel van een ander type dan tekst). Gate 12 met de suite na de
rebase: PASS, beide takken `PLAYED` 358, JUnit 1884 tests (`DrlTextPinWitnessTest` inbegrepen), `logs/f3-check-drools.log`.

### Landing (hoofdsessie, 2026-11-06)

Geland op main na `make gates` op de stand vóór de rebase (red=0, 436 s) en, na de rebase op `3df621a37`, gate 12 en de
Drools-suite groen; CONSTRUCTS en de gate-6-tekst van `CLAUDE.md` in de landingscommit. De volledige set draait de
hoofdsessie op main na de laatste landing van de dag.
