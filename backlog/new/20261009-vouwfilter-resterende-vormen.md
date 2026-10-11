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
  van een objectcreatie met een vouw in de initialisatie onder een
  vouwvoorwaarde (`C3` en `S` onveilig); ref speelt case 1 en 2.
- Een samenstelling onder "geen" of een tellende kwantor met een datumvouw als
  lid: de exec-modeltak faalt waar ref na "hij is actief" stopt; de DRL-teksttak
  weigert de vorm (`a9-*`).

**Gelezen, niet gemeten (lens).** Een negatieve wortel in het filter van een
vouw die wel bereikt wordt: ref geeft leeg en een §9.5-deponering
(`ref/lib/arith.ml` rond regel 223–253); `rsWortel` geeft leeg
(`RegelSpraakRuntime.java` rond regel 304–310) en de Drools-foutbegeleiders
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

May touch: `backends/clingo/**`, `backends/drools/src/main/java/.../transpiler/model/ExecModelTranspiler.java`,
`backends/drools/src/main/java/.../transpiler/DrlTranspiler.java`, getuigen,
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

Het document is het vouwdocument van `TmsFilterfoutWitnessTest` (verifier PASS): Jutters met aangemelde Vondsten (feittype
`aanmelding`, rollen `aanmelder` en `vondst`), en één regel "zware vondsten" voor een actieve Jutter: het aantal vondsten
van de Jutter waarvoor "zijn gewicht gedeeld door zijn deelnemers is groter dan 2".

| Case | Invoer | ref | clingo |
|---|---|---|---|
| `a-eerst` | J1 actief met V1 (gewicht 10, deelnemers 2); V2 (gewicht 9, deelnemers 0) hoort bij geen Jutter | `oracle: PASS (2 expectations)`: J1 1, consistent | `UNSATISFIABLE — a fail-fast guard fired: viol("division by numeric zero","V2")` |
| `b-vers-j1` | J1 actief zonder vondsten; J2 niet actief met V1 (9/0) en V2 (4/0) | `oracle: PASS (2 expectations)`: J1 0, consistent | `UNSATISFIABLE`, de guard op V1 en V2 |

Drools: op de invoer van `a-eerst` bevat de lezing van de exec-model-tak alleen "J1.zware vondsten = 1" (de eerste
lezing van `TmsHerhaaldVurenWitnessTest.een_relatie_die_ontstaat_tussen_twee_fires_weigert_vuur`, hetzelfde document en
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
