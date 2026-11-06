# vouwfilter-resterende-vormen: een fout of deponering uit een vouw waar ref die vouw niet (zo) uitrekent, buiten wat drools-filterfout-buiten-de-lezende-regel repareert

*Gevonden op 2026-10-09 door de bouwer en de lens van
`20261009-drools-filterfout-buiten-de-lezende-regel` (bijvangst en
lensvondsten) en door de hoofdsessie vastgelegd volgens de afspraak dat een
bestaande bug een kaart krijgt. Gemeten op 2026-10-09 met ref, clingo en de
verifier uit `_build` van de hoofdcheckout (main `8beff67a`; ref en clingo
ongewijzigd sinds `54ba99f7`) en de shaded jar van `backends/drools` van main
`54ba99f7` ("oud") en van die kaartbranch op `9482ec1d` ("nieuw"). De documenten
staan onder `/tmp/lens-bugs/w2/c/` (gemaakt door `/tmp/lens-bugs/w2/c/maak.py`);
de uitkomsten in `/tmp/lens-bugs/w2/meting-resterend.txt`.*

## Aanleiding

Ref rekent een vouw en haar filter alleen uit waar een vorm de vouw bereikt
(`spec/RUNTIME-SEMANTICS.md` ch. 5.10 en ch. 7.1: een default die niet beslist,
wordt niet uitgerekend), en een kwantorvouw stopt zodra haar oordeel vaststaat.
De kaart `20261009-drools-filterfout-buiten-de-lezende-regel` repareert de
exec-modeltak en de DRL-teksttak voor een delend filter onder een voorwaarde die
niet geldt. Daarbuiten bleven vormen over, in clingo en in de Drools-takken
(alle documenten verifier-schoon; model als in die kaart: Jutters met
aangemelde Vondsten, V2 met deelnemers 0):

| Document | ref | clingo | exec oud → nieuw | DRL oud → nieuw |
|---|---|---|---|---|
| `g-buiten-bereik`, case 2: J1 niet actief naast een actieve J2 (de vorm van de golden `jutterswet-filterwacht` in één case) | `PASS` | `UNSAT viol("division by numeric zero","V2")` | fout → `PLAYED` | fout → `PLAYED` |
| `c8-buiten-het-bereik` en `g-buiten-bereik` case 1: een Vondst die bij geen Jutter hoort | `PASS` | `UNSAT … "V9"` | fout → `PLAYED` | fout → `PLAYED` |
| `c8b-pad-buiten-het-bereik` case 1: idem via een pad van twee stappen | `PASS` | `UNSAT … "B9"` | fout → `PLAYED` | fout → `PLAYED` |
| `c7b-defaults-hoger-beslist` case 1: de lagere default leest de delende vouw, de hogere beslist | `PASS` | `UNSAT … "V2"` | fout → fout (zie onder) | weigert (geen defaults) |
| `c6e-kwantorvouw-matrix-stop`: "ten minste één" waarvan V1 voldoet en de matrix bij V2 door nul deelt | `PASS` (stopt na V1) | `UNSAT … "V2"` | fout → fout | fout → fout |

De tak met truth maintenance weigert de laatste twee vormen (een deling in een
default, een kwantorvouw) en speelt de eerste drie.

**Clingo.** De foutguard voor een deling in het filter van een vouw
(`backends/clingo/lib/translate.ml` rond regel 6141–6155) draagt de voorwaarde van
de lezende regel, maar niet de join van het lid op het onderwerp
(`rol(S, …, X)` ontbreekt), niet de beslissing van de defaults-groep, en niet het
stoppen van de kwantorvouw. Zo vuurt hij voor elk lid met deelnemers 0 zodra een
onderwerp de voorwaarde haalt, ook voor een lid van een ander onderwerp of van
geen enkel. Daarom heeft de golden `jutterswet-filterwacht` twee losse cases in
plaats van één.

**Drools.** De kwantorvouw met een matrix die na het beslissende lid deelt
(`c6e`), in beide takken. De lagere default (`c7b`) in de exec-modeltak, tenzij
de fixronde van `20261009-drools-filterfout-buiten-de-lezende-regel` hem
oplost of luid weigert; wat daar niet lukt, valt onder deze kaart.

**Na de fixronde van die kaart (`4ada319d`) gemeten** (documenten in
`/tmp/lens-bugs/w2/f1/d/`, uitkomsten in `/tmp/lens-bugs/w2/meting-bijvangst-fix.txt`):
- `d1-begeleider`: de DRL-teksttak maakt de begeleider van een deling in de waarde
  (§6.5) zonder de vouw in de voorwaarde, en meldt `inconsistent (zwaarte @ J1)`
  waar ref en clingo consistent geven; vóór en na de fix.
- `a8-creatie-onder-vouwvoorwaarde`: clingo geeft geen model maar een fout van
  gringo, "unsafe variables" in de guard `viol("arithmetic range exceeded …")`
  van een objectcreatie met een vouw in een toekenning onder een
  vouwvoorwaarde (`C3` en `S` onveilig); ref speelt case 1 en 2.
- Een samenstelling onder "geen" of een tellende kwantor met een datumvouw als
  lid: de exec-modeltak faalt waar ref na "hij is actief" stopt; de DRL-teksttak
  weigert de vorm (`a9-*`).

**Gelezen, niet gemeten (lens).** Een negatieve wortel in het filter van een
vouw die wel bereikt wordt: ref geeft leeg en een §9.5-deponering
(`ref/lib/arith.ml` rond regel 223–253); `rsWortel` gaf leeg
(`RegelSpraakRuntime.java` rond regel 304–310; de functie staat niet meer in de runtime) en de Drools-foutbegeleiders
onderzoeken alleen de waarde van een regel, zodat de takken consistent kunnen
blijven waar ref een inconsistentie meldt. Eerst meten.

## Aanvulling (2026-10-09, avond): de deponering in clingo, na `20261008-tms-expressievormen-en-rekendatum`

Gemeten tijdens die kaart met ref, clingo en de verifier uit `_build` van haar worktree (documenten onder
`/tmp/lens-tms-k2/proef/clingo-filter/` en `/tmp/lens-tms-k2/eindfix/proef/`):

- **Het onderwerp van een deponering uit een vouwfilter.** Het `ir.json` van `jutterswet-filterwacht` met een case
  waarin V3 geen deelnemers heeft (filter "zijn gewicht gedeeld door zijn deelnemers is groter dan 2"): ref deponeert
  "zware vondsten × J2; drukte × J2", onder elke vorm die de vouw leest en op het onderwerp van de vouw; clingo
  deponeert op V3, het lid (gemeten met een verwachting `inconsistent` per regel en label). Volgens de bouwer van die
  kaart deponeert clingo ook waar de vouw niet bereikt wordt, en bij een unie op het lid ("unietelling × V3"). Ch. 5.6
  ("one error fact per rule × subject") en de filtertekst van IR-SPEC ch. 4.3 ("the element is the condition's
  subject") laten ruimte; ref en sinds `f9929a07` de tak met truth maintenance nemen het onderwerp van de lezende vorm.
  Een zin in ch. 5.6 die dat vastlegt, hoort bij deze kaart (voorstel).
- **De kwantorvouw.** Clingo heeft geen kortsluiting in de fout- en deponeerbewaking van de matrix van een kwantorvouw
  (gemeld door de bouwer van stap 2 van die kaart; zie ook `c6e-kwantorvouw-matrix-stop` hierboven).
- **Een uitzonderingsgroep onder een verdrongen default.** Clingo deponeert een deling door leeg uit een
  uitzonderingsgroep onder een default die een hogere positie verdrong, waar ref niets deponeert (ch. 7.1), en dan
  onder het label van een andere vorm ("wrakverklaring × V1"; zonder die regels "? × V1"); in de bereikte case
  deponeert ref "Vondst.heffing × V2" en clingo "wrakverklaring × V2" (`proef/diep1--deling/` en de andere
  `*--deling`-mappen, case `case-verdrongen.json`, nagemeten door de hoofdsessie: ref PASS, clingo 1 inconsistent
  feit). Hetzelfde gat als `c7b-defaults-hoger-beslist` hierboven: de bewakingen van clingo dragen de beslissing van de
  defaults-groep niet.
- De exec-modeltak en de DRL-teksttak deponeren in een vouwfilter niets; dat verdwijnt met
  `20261008-tms-omschakeling` en `20261008-drl-teksttak-op-truth-maintenance`.

## Acceptatie (bij oppakken aanscherpen)

- (a) De golden `jutterswet-filterwacht` krijgt de case met J1 en J2 samen (of
  een nieuwe golden), zodra clingo hem speelt; goldens of rode gevallen voor de
  andere rijen, met absolute verwachtingen.
- (b) Clingo: de guard volgt de evaluatie van ref (join van het lid op het
  onderwerp; alleen onder de beslissende default; niet na het beslissende lid
  van een kwantorvouw), rood eerst, met checks in `backends/clingo/test`.
- (c) Drools: de kwantorvouw met stoptrigger in beide takken, en wat van `c7b`
  overblijft; de negatieve wortel na meting. De tak met truth maintenance draagt
  de vormen pas met kaart 2 (`20261008-tms-expressievormen-en-rekendatum`); de
  fixes in de exec-modeltak en de DRL-teksttak vervallen met de omschakeling en
  de DRL-kaart, de goldens blijven.
- (d) Gates 5, 6, 8 en 12 groen (pins: voorstel); alle gates van `CLAUDE.md`
  groen.

## Omvang

Middel: één guard in clingo met drie voorwaarden, en twee kleine plaatsen in de
Drools-takken.

May touch: `backends/clingo/**`, `backends/drools/src/main/java/.../transpiler/tms/` (`TmsTranspiler`, `DrlRendering`), getuigen,
`corpus/golden/jutterswet-filterwacht/` en nieuwe goldens of rode gevallen,
`scripts/check-drools.sh` (pins; voorstel), kaartverplaatsing.

## Raakvlakken

Bouwt op `20261009-drools-filterfout-buiten-de-lezende-regel`; daarna.
`20261008-fout-op-tussenstand` gaat over een fout op een tussenstand, deze kaart
over een fout waar ref niet rekent.

## Aanvulling (2026-10-10): twee getuigen uit `20261008-tms-omschakeling`, en de stand na de omschakeling

Dit is dezelfde fout van clingo als in de eerste twee rijen van de tabel hierboven (`g-buiten-bereik` case 2 en
`c8-buiten-het-bereik`): de foutguard voor een deling in het filter van een vouw draagt de voorwaarde van de lezende
regel, maar niet de join van het lid op het onderwerp, en vuurt daardoor ook voor een lid van een ander onderwerp of van
geen enkel. De twee documenten hieronder zijn extra getuigen op een ander model; ze vragen geen ander werk.

Gevonden door de bouwer van stap 5a1 van `20261008-tms-omschakeling` (bijvangst 1 van zijn verslag), gemeten op
2026-10-10 met ref, clingo en de verifier uit `_build` van de worktree van die kaart. De twee cases zijn niet in de
repository bewaard; de tabel geeft hun invoer.

Het document is het vouwdocument van `TmsFilterFaultWitnessTest` (verifier PASS): Jutters met aangemelde Vondsten (feittype
`aanmelding`, rollen `aanmelder` en `vondst`), en één regel "zware vondsten" voor een actieve Jutter: het aantal vondsten
van de Jutter waarvoor "zijn gewicht gedeeld door zijn deelnemers is groter dan 2".

| Case | Invoer | ref | clingo |
|---|---|---|---|
| `a-eerst` | J1 actief met V1 (gewicht 10, deelnemers 2); V2 (gewicht 9, deelnemers 0) hoort bij geen Jutter | `oracle: PASS (2 expectations)`: J1 1, consistent | `UNSATISFIABLE — a fail-fast guard fired: viol("division by numeric zero","V2")` |
| `b-vers-j1` | J1 actief zonder vondsten; J2 niet actief met V1 (9/0) en V2 (4/0) | `oracle: PASS (2 expectations)`: J1 0, consistent | `UNSATISFIABLE`, de guard op V1 en V2 |

Drools: op de invoer van `a-eerst` bevat de lezing van de exec-model-tak alleen "J1.zware vondsten = 1" (de eerste
lezing van `TmsRepeatedFireWitnessTest.a_relation_that_arises_between_two_fires_refuses_fire`, hetzelfde document en
dezelfde invoer). Op `b-vers-j1` is Drools niet gemeten.

**De stand na de landing van `20261008-tms-omschakeling`:**
- De exec-model-tak is de tak met truth maintenance. `ExecModelTranspiler.java` bestaat niet meer; in May touch wordt
  dat `backends/drools/src/main/java/dev/uchar/regelspraak/transpiler/tms/TmsTranspiler.java`.
- (c) "De fixes in de exec-modeltak … vervallen met de omschakeling" is daarmee gebeurd. De tabel van deze kaart gaf
  voor de tak met truth maintenance: de eerste drie vormen spelen, `c7b-defaults-hoger-beslist` (een deling in een
  default) en `c6e-kwantorvouw-matrix-stop` (een kwantorvouw) weigert hij (gemeten 2026-10-09). Of dat na
  `20261008-tms-expressievormen-en-rekendatum` en `20261008-tms-verdeling-en-tijdlijnen` nog zo is, is niet opnieuw
  gemeten; meten bij het oppakken.
- Een deling in een default weigert de exec-model-tak tot `20261008-fout-op-tussenstand` (dekkingsverlies, punt 2 in het
  `Result:` van de omschakeling).

## Result

*Nagemeten op 2026-11-06 op branch `card/text-output-foldfilter` (worktree `wt/clingo-round2`; ref, clingo en de
verifier uit `_build` van de worktree, de shaded jar van `backends/drools` daar, na kaart 2). De documenten van
2026-10-09 in `/tmp` bestonden niet meer; elke rij is opnieuw gebouwd naar haar beschrijving, onder
`uchar-dev/werk/clingo-round2/k3/` (`probes/`, `rgs/`, `rgs2/`, `rgs3/`), uitkomsten in
`logs/k3-filterwacht-j1j2.log` en `logs/k3-probes-1.log` … `-4.log`. Alle documenten zijn verifier-schoon.*

Geen rij is nog fout: clingo's foutguard volgt de evaluatie van ref sinds `zero-division-channel` (2026-10-19) en
`fix/clingo-filter-guard-binding` (2026-10-22), en beide Drools-takken zijn één analyse met truth maintenance. Wat
nagemeten is, per rij van de tabel en de aanvullingen (ref | clingo | exec-model | DRL-tekst):

| Rij | Uitkomst | Nu vastgelegd in |
|---|---|---|
| `g-buiten-bereik` case 2: J1 niet actief naast een actieve J2 | PASS \| PASS \| PLAYED \| PLAYED | `jutterswet-filterwacht` case 3 |
| `c8`: een Vondst in geen enkel bereik | PASS in alle vier | `jutterswet-filterwacht` case 3 (V9) |
| `c8b`: idem over een pad van twee rollen | PASS in alle vier | `jutterswet-vouwfilter-buiten-de-beurt` (meldgewicht; S9/V9) |
| `c7b`: de lagere default leest de delende vouw, de hogere beslist | PASS in alle vier | `jutterswet-vouwfilter-buiten-de-beurt` (zware vondsten) |
| `c6e`: "ten minste één", V1 beslist, de matrix deelt bij V2 door nul | PASS in alle vier; bereikt (V2 eerst): `drukte × J1` in alle vier | `jutterswet-kwantorvouw-en-unie` (drukte) |
| `a8`: creatie met een vouw in een toekenning onder een vouwvoorwaarde | ref en clingo PASS (geen gringo-fout meer); beide takken weigeren met naam, `DROOLS-152` (een deling in een toekenning) | meting `k3/rgs/a8-…` |
| `d1-begeleider`: deling in de waarde, vouw in de voorwaarde | PASS in alle vier | meting `k3/rgs2/d1-companion` |
| `a9`: "geen" en "ten minste twee" met een datumvouw als lid | alle vier gelijk: onder "geen" na "hij is actief" geen deponering; onder "ten minste twee" wordt de datumvouw bereikt en deponeren alle vier `vlotheid × J1` | `jutterswet-vouwfilter-buiten-de-beurt` (rust) |
| negatieve wortel in het filter van een bereikte vouw | ref en beide takken: `zware vondsten × J1`; clingo weigert de afgeronde wortel bij de vertaling ("rounded root is not translated") | meting `k3/rgs2/negative-root-in-filter` |
| onderwerp van een deponering uit een vouwfilter | ref, clingo en beide takken op J2, onder elke lezende vorm; clingo precies twee feiten | golden `fold-filter-divides-by-zero` (al aanwezig) en meting `k3/probes/deposit-subject` |
| unietelling met een delende tak | alle vier `unietelling × J2`, clingo precies één feit | `jutterswet-kwantorvouw-en-unie` (unietelling) |
| uitzonderingsgroep onder een verdrongen default | PASS in alle vier, geen deponering; bereikt, met een deling door nul in de consequentie: alle vier `heffing` 2,00 en consistent (de speculatieve-deponeringsregel van ch. 7.1: een doorgevallen default deponeert niet) | meting `k3/rgs/exception-under-displaced` |

- **(a)** `jutterswet-filterwacht` krijgt case 3 (J1 niet actief naast een actieve J2, en V9 in geen bereik), via
  `scenario.rgs` (gate 16 regenereert hem); NOTE.md beschrijft de drie cases. Twee nieuwe goldens met
  handgeschreven absolute verwachtingen, elk met een case waarin geen deling bereikt wordt (consistent) en een
  controlecase waarin elke deling bereikt wordt (de EF-005-feiten exact, per lezende vorm op het onderwerp):
  `jutterswet-vouwfilter-buiten-de-beurt` (`source.rgs`: defaults, "geen", een pad) en
  `jutterswet-kwantorvouw-en-unie` (handgeschreven `ir.json`: de frontend schrijft geen deling in de matrix van een
  kwantorvouw en geen ander filter dan een kenmerk in een tak van een unie).
- **(b)** Clingo: geen reparatie nodig; de nieuwe goldens houden het gedrag vast (gate 6).
- **(c)** Drools: geen reparatie nodig; beide takken spelen de nieuwe goldens. `a8` weigeren ze met naam
  (`DROOLS-152`), geen stil fout antwoord.
- **(d)** Gates 2, 4, 5, 6, 7, 10 en 16 los gedraaid (`logs/k3-gates.log`); 12, 13, 15 en 17 in de afsluitende
  `make gates`. Gate 12 `EXPECTED_PLAYED` +2 (de twee nieuwe goldens spelen op beide takken; na de rebase op main `3df621a37` 356 → 358).

Een zin in ch. 5.6 over het onderwerp van een deponering uit een vouwfilter is niet nodig: ch. 1.4 punt 4 en ch. 5.6
zeggen al "on the rule × subject that evaluates it", en ref, clingo en beide takken nemen het onderwerp van de
lezende vorm.

Opgemerkt bij het meten, geen afwijking: het filter van een kwantorvouw wordt volledig uitgerekend vóór de matrix
kortsluit (ch. 3.9a, "the contributors are settled in full first"); een deling door nul in dat filter deponeert dus
ook na het beslissende lid, in alle vier.

Eindrun van de ronde: `make gates` op `2df79f6a3`, alle zeventien gates en `after-run` rc=0, red=0, 435 s
(`out/gates/20261106-164133/summary.txt` in de worktree, log `uchar-dev/werk/clingo-round2/logs/gates-final.log`).

### Vervolg: de afgeronde wortel in clingo krijgt een code

Bij de rij "negatieve wortel" weigerde clingo de afgeronde wortel bij de vertaling zonder registercode ("rounded
root is not translated (no corpus case)", `logs/k3-probes-3.log`), een fout die geen pin kan noemen. Op verzoek van
de hoofdsessie in deze branch opgelost: de weigering is `CLINGO-043 rounded-square-root` van de soort `refusal`
(`expr_term`; ref rondt de irrationele wortel exact af, ch. 2.2, en deze codering heeft daar geen gehele vorm voor).
Suitecheck "ch2.2 a rounded square root": verifier-schoon, ref 3 (√10 afgerond op 0 decimalen), clingo weigert met
`CLINGO-043` zonder parameters; `test_clingo: 2772 checks, 0 failures` (`logs/f1-suite.log`). Geen golden. De arm
erna, een kale `squareRoot` of `absoluteValue` buiten een afronding, weigert nog zonder code; geen golden bereikt
hem (`absoluteValue` staat in geen golden, en de frontend schrijft een wortel alleen onder een afronding).

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
