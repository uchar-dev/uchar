# ref-macht-van-nul: ref crasht op nul tot een negatieve macht in plaats van een benoemde fout te geven

*Gevonden op 2026-10-09 door de bouwer van de fixronde van
`20261009-drools-filterfout-buiten-de-lezende-regel` (bijvangst) en door de
hoofdsessie vastgelegd en nagemeten volgens de afspraak dat een bestaande bug een
kaart krijgt. Gemeten op uchar main (`_build` van de hoofdcheckout, ref
ongewijzigd sinds `54ba99f7`); het document staat in
`/tmp/lens-bugs/w2/f1/d/c3-macht/` (`source.rgs`, `ir.json`, `case2.json`).*

## Aanleiding

"Tot de macht" met een negatieve gehele exponent keert de basis om
(`ref/lib/arith.ml`, `pow_`, rond regel 275–300: `Q.inv (p lq (-e))`). Is de
basis 0, dan gooit Zarith `Division_by_zero`, die niemand opvangt: de runner
stopt met `Fatal error: exception Division_by_zero`, zonder benoemde fout, zonder
regelnaam en zonder het "EVALUATION FAULT"-formaat waaraan de rode gevallen en
de gates een fout herkennen. Nul tot een negatieve macht is een deling door een
numerieke nul (`spec/RUNTIME-SEMANTICS.md` ch. 1.4, punt 4) en hoort de fout
"division by numeric zero" te geven.

Document: een vouw "het aantal vondsten van de Jutter die aan de volgende
voorwaarde voldoen: zijn gewicht tot de macht zijn deelnemers naar beneden
afgerond op 0 decimalen is groter dan 2"; case 2: J1 actief met V1 (gewicht 0,
deelnemers −1).

| Uitvoerder | case 2 |
|---|---|
| verifier | `PASS` |
| ref | `Fatal error: exception Division_by_zero` |
| clingo | `TRANSLATION FAULT: a runtime exponent is not translated …` (weigert de vorm) |
| Drools, DRL-teksttak | `ERROR ConstraintEvaluationException … rsAfronding(rsMachtReeel …` |

## Acceptatie (bij oppakken aanscherpen)

- (a) Ref deponeert EF-005, een deling door een numerieke nul (sinds `zero-division-channel` een soort van het
  §9.5-kanaal, RUNTIME-SEMANTICS ch. 5.6), rood eerst, met een voorbeeld in `ref/test/`.
- (b) Een golden met een `inconsistent`-verwachting die code EF-005 vastzet, gespeeld door ref en, waar ze de vorm
  dragen, Drools; clingo weigert de vorm al bij de vertaling (een regel in `EXPECTED_CHANNEL` van gate 6).
- (c) Nagaan of andere plaatsen in `ref/lib/arith.ml` een Zarith-exceptie
  ongevangen laten (`Q.inv`, `Q.div`, `Z.div` op een nul).
- (d) Gates 5 en 8 groen; alle gates van `CLAUDE.md` groen.

## Omvang

Klein. May touch: `ref/lib/arith.ml`, `ref/test/`, `corpus/red/<nieuw geval>/`,
kaartverplaatsing.

## Result

Gebouwd op branch `card/drools-frontend-cleanup` (opruimronde 2026-11-06). Logs onder
`uchar-dev/werk/java-cleanup/logs/`, proefbestanden onder `uchar-dev/werk/java-cleanup/probe/k3/`.

- **Wat er werkelijk gebeurde.** Zarith werpt bij `Q.inv 0` niets: het geeft `+inf`. Pas een afronding deelt
  daarna door de noemer 0 (`Z.fdiv` → `Division_by_zero`); zonder afronding rekende ref stil met oneindig verder
  (`0 tot de macht −1 groter dan 2` zou waar zijn). Bij een gebroken exponent gaf `root_exact` op `+inf` weer `+inf`.
- **(a)** `ref/lib/arith.ml`: `pow_` en `pow_rounded` krijgen `~err`; een numerieke nul onder een negatieve
  exponent (geheel of gebroken, met of zonder afronding) is leeg met één §9.5-depositie EF-005, zoals 1 ÷ 0
  (`inverts_zero`, `inverted_zero`). `eval.ml` geeft `~err` door. Voorbeelden in `ref/test/test_ref.ml`:
  `ch2.2-pow-zero-negative`, `-fraction`, `-rounded`, `ch2.2-pow-zero-zero` (1, geen depositie) en
  `ch2.2-pow-zero-positive` (0, geen depositie); de bestaande aanroepen van `pow_` geven `~err:no_err`. Rood:
  `FAIL ch2.2-pow-zero-negative` (`k3-red-reftest.log`) en op het document `Fatal error: exception
  Division_by_zero` (`k3-red-golden.log`); groen 2538 voorbeelden (`k3-green-reftest.log`). Spec: ch. 2.2 kreeg de
  alinea met het uitgewerkte voorbeeld, de rij EF-005 van ch. 5.7b noemt de macht van nul naast de deling.
- **Drools, naburig gat.** Beide takken faalden op `0 tot de macht -1` met `ArithmeticException: Breuk met noemer 0`
  (de inverse in `rsMacht` en in `powerRadicand`). Nu zet de lowering voor de basis van een macht met een negatieve
  literaire exponent een `Term.Division` met de nieuwe functie `INVERTED_BASE` (`ComputeHelpers.invertedBase`): een
  numerieke nul is leeg met de depositie EF-005, en zo'n macht is een depositieplaats (`divisionSite`). Een
  gebroken macht onder een afronding brak bovendien de bouw met lezingen (de knoop had geen term, en de term van de
  exponent was niet die van de aanroep); de knoop is nu niet bereikt, zoals ref hem leest, en `EXACT_POWER` en
  `ROUNDED_POWER` krijgen de exponent als één constante. Getuige `PowerOfZeroWitnessTest` op het document
  `backends/drools/src/test/resources/power-of-zero` (rood `k3-red-drools.log`, groen `k3-green-drools*.log`): beide
  takken spelen beide cases, en de lezingen van case 1 zijn regel voor regel de trace van ref.
- **(b) niet geland: de golden wacht op één clingo-code.** De golden `jutterswet-macht-van-nul` staat klaar in
  `uchar-dev/werk/java-cleanup/golden-ready/jutterswet-macht-van-nul/` (bron, IR, twee cases, NOTE.md; verifier
  PASS, ref PASS, beide Drools-takken PLAYED, lezingen gelijk aan de trace). Clingo weigert een negatieve exponent
  (`const_pow_exponent`, `backends/clingo/lib/translate.ml`) met `TRANSLATION FAULT: negative exponent -1 is not
  translated …` zonder registercode, en gate 6 aanvaardt in `EXPECTED_CHANNEL` alleen een weigering met een code van
  `backends/clingo/codes.tsv`. Die bestanden zijn van de clingo-bouwer (opdracht: niet aanraken). Bij de landing:
  (1) een code voor die weigering (soort `refusal`, ook voor de runtime-exponent ernaast), (2) de golden naar
  `corpus/golden/`, (3) in `scripts/check-clingo.sh` de regel `jutterswet-macht-van-nul|CLINGO-0xx|the encoding
  translates only a whole non-negative literal exponent: a negative one inverts its base, a division, which the
  scaled integers do not hold; ref and both Drools branches play the golden`, (4) in `scripts/check-drools.sh`
  `EXPECTED_PLAYED` +1, (5) de rij EF-005 van ch. 5.7b noemt de golden.
- **(c)** Nagelopen in `arith.ml`: op een document dat de verifier aanvaardt, laat geen andere plaats een
  Zarith-exceptie of een oneindige waarde door. `round_z`/`round_q`: de noemer van een eindig getal is positief;
  `Z.pow 10 d` met een negatief aantal decimalen werpt `Invalid_argument`, maar zo'n afronding weigert de verifier
  (WF-007 `decimals-below-zero` en WF-091 op het type; met type `null` WF-034; proef `probe/k3/negdec`).
  `root_exact`/`Z.root`: de wortelgraad is ≥ 1 en een negatieve radicand gaat eerst naar `None`; `sqrt_`/
  `sqrt_rounded` toetsen het teken vooraf. `rational`/`factor_power`: het schema eist teller en noemer ≥ 1.
  `Z.to_int` vangt `Overflow` overal af (exponent, paasdag, datumdelen, duur); de kalender is sinds `319628d89`
  begrensd.
- **(d)** Gate 5 groen (2538 voorbeelden, 1117 cases), gate 8 groen, gate 12 groen (350 gespeeld, 1872 tests),
  gate 1 groen (`k3-gate05.log`, `k3-gate08.log`, `k3-gate12.log`).
