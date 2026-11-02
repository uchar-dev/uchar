# drools-exact-arithmetic: de Drools-backend rekent overal exact, ook waar geen gewoon bedrag het ziet

*Vastgelegd door de hoofdsessie op 2026-11-02 bij de landing van `20261009-drools-schrijfgrens` (zie daar "Snede" en het
Result, "Voor `drools-exact-arithmetic`"). Onderzoek en metingen: `/Users/yeb/Developer/uchar-dev/research/precisie-en-afronding-2026-11-01/`
(`notes/drools-backend.md` par. 8). Geen haast: de corpus en de gewone bedragen zien het niet.*

Goal: RUNTIME-SEMANTICS ch. 2.1 (exacte breuken, twee collapspunten) geldt in Drools op elke plaats waar het nu een
`MathContext` of een `BigDecimal`-schaal tussen de waarde en de lezer zet.

## Wat er nog afwijkt

- `rsPlus`, `rsMin`, `rsMaal` ronden af op `DECIMAL128` (`(10^31 + 0,001) − 10^31` geeft `0.00`).
- Een vouw (`Contribution`) sommeert decimalen; daarom weigert de backend een vouw over een onbegrensd attribuut met naam
  (DROOLS-296). `ExactSum` in beide renderingen en `Contribution` zonder collapse heffen die weigering op.
- Het tijdsevenredig deel staat op 16 cijfers; met ch. 2.4 op de tijdlijn in ref én Drools (ref kent daar nu geen grens,
  clingo wel; RUNTIME-SEMANTICS ch. 3.7 en 2.4 noemen dit OPEN).
- De wortel: `toBd` geeft het radicand als `DECIMAL64`; eenmaal correct onder zijn afronding.
- Ch. 5.12 schrijft een niet-eindigende waarde op "16 cijfers half to even", zodat 1/3 en 3333333333333333/10^16 gelijk lezen;
  voorstel `t/n`.
- De gebroken exponent: "eenmaal correct afgerond", met een weigering als benoemde limiet.
- Een onbegrensd attribuut dat een objectcreatie initialiseert, gaat als decimaal op de nieuwe bean (de creatie collapset bij
  de constructie), dus een lezer ziet daar 16 cijfers.
- De grens van de kwalificatie over de gehele periode (`ComputeHelpers.wholePeriod`) leest een `Breuk` via `toBd`.

Los, elk klein, mag mee of apart: clingo's EF-006 op `x/3 maal 3`, waar de waarde past (een `CLINGO-###` soort limit); de zinnen
ch. 2.5 "default when no mode is written" en ch. 2.3 min/max; de javadoc van `Breuk` ("impossible-by-resolver-guard" klopt niet).

Acceptance (bij oppakken aanscherpen):
- (a) Per punt een witness in de Drools-suite, eerst rood (clingo's bereik houdt deze getallen uit de differentiële corpus).
- (b) DROOLS-296 `RETIRED`, met een golden of witness voor een som over een onbegrensd attribuut (drie keer 1/3 is 1).
- (c) `make gates` groen; `EXPECTED_PLAYED` gelijk of hoger, met naam.

May touch: `backends/drools/**`, `ref/` (alleen voor ch. 2.4 op de tijdlijn), `spec/RUNTIME-SEMANTICS.md` (voorstel),
`scripts/check-drools.sh` (pin, voorstel).

## Result

Bouwer, 2026-11-02, op main `3b75ac8e0` (gerebased; de branch begon op `e284bab9a`). Niet gemerged, niet gepusht. Het enige
beschermde pad dat de kaart raakt, `spec/RUNTIME-SEMANTICS.md`, is voorstel. Buiten "May touch", op grond van de
opdracht: `backends/clingo/**` (de afbeelding van het tijdsevenredig deel en CLINGO-031), `corpus/red/<nieuw geval>`, en in
`ref/` behalve de schrijfgrens op een tijdlijn ook `arith.ml`/`eval.ml` (de gebroken macht) en `reading.ml` (t/n), zie de
voorlopige besluiten.

### Commits

- `f6d52cbfb` kaart naar doing
- `a9f7f6771` drools: rekent exact waar een lezer de waarde ziet
- `3506efd0b` ref: schrijfgrens per segment, gebroken macht en t/n in de lezing
- `759896017` tijdlijn: de schrijfgrens geldt per segment, in Drools en clingo
- `e6475ee07` clingo: een niet-exacte deling binnen een waarde is een limiet
- `fc1b3e800` drools: een creatie zet een onbegrensde waarde exact in het werkgeheugen
- `fc8d149f1` spec: exact rekenen en de schrijfgrens op een tijdlijn (voorstel)
- `f7c87d481` ref en clingo: getuigen van de gebroken macht en het tijdsevenredig deel
- en de commit van dit Result

### Wat veranderde, per punt

Witnesses in de Drools-suite staan, tenzij anders genoemd, in `cases/ExactArithmeticWitnessTest` (9 tests, beide branches).

1. **`rsPlus`, `rsMin`, `rsMaal` exact.** De `MathContext.DECIMAL128` is weg (`RegelSpraakRuntime`). Witness
   `plus_min_and_times_are_exact_past_thirty_four_digits`: (10^31 + 0,001) − 10^31 = 0,001 en 100000000000000001² met alle 35
   cijfers; ref `WV-2.1-exact-a`, `-b`.
2. **`ExactSum` en `Contribution` zonder collapse; DROOLS-296 `RETIRED`.** Drie accumulatiefuncties van de branch
   (`ExactSumFunction`, `ExactMinimumFunction`, `ExactMaximumFunction`): de exec-modelweergave neemt ze als klasse, de DRL-tekst
   via `import accumulate … exactSum|exactMin|exactMax`. De som houdt de `BigDecimal`-som (met haar schaal) zolang er geen
   `Breuk` bij komt, zodat geen bestaande pin beweegt. `ComputeHelpers.contribution` draagt de exacte waarde; `rsDimSom`,
   `rsDimMin`, `rsDimMax` rekenen en vergelijken exact. De twee weigeringen van DROOLS-296 in `TmsTranspiler` zijn weg.
   Witnesses: `RationalAtRestWitnessTest.a_fold_over_values_of_the_unbounded_form_adds_and_compares_them_exactly` (drie keer 1/3
   is 1; 1/3, 1, 2/3 gemengd is 2; minimum en maximum maal 3 zijn 1 en 1 of 3) en
   `DimensionCellWitnessTest.a_fold_over_written_cells_of_the_unbounded_form_adds_their_exact_values` (drie cellen "zijn som
   gedeeld door 3", de som over alle seizoenen is 1). Geen golden: clingo houdt de onbegrensde vorm als geheel getal.
3. **Het tijdsevenredig deel exact, ch. 2.4 op de tijdlijn in ref, clingo en Drools.** Ref: `World.write_timeline_by` (de
   write van een vorm) toetst elk segment met de toets van een scalaire write; `release` (terugzetten wat het doel had) toetst
   niet. Drools: `ComputeHelpers.proRata` rekent het deel exact per kalenderbucket en toetst elk segment
   (`WriteBoundaryFault`, EF-006 met `place`, `value`, `decimals` als ref); `rsTijdsevenredig` is uit de runtime weg. Een deel in
   een tijdlijn van de onbegrensde vorm weigert Drools op naam: DROOLS-301 (limit). Clingo: zijn zin "non-exact pro-rata part"
   heet nu `pro_rata_misfit` en wordt EF-006; voor de onbegrensde vorm een eigen zin zonder code (limiet). Gemeten vóór alles:
   gate 5 met de toets in ref, 987 cases PASS, dus geen golden faalt. Rood geval
   `corpus/red/jutterswet-tijdsevenredig-past-niet` (februari 15 × 18/28 = 135/14 in 2 decimalen), `expectedFault {EF-006,
   {}}`: ref en Drools EF-006 `{place L1.ligdeel, value 135/14, decimals 2}`, clingo EF-006 `{}`. Witnesses
   `a_pro_rata_segment_passes_the_write_boundary_of_its_target` en `a_pro_rata_part_into_the_unbounded_form_is_refused_by_name`;
   ref `WV-3.7-schrijfgrens-maand` (18 en 4,50), `-jaar` (EF-006, 684/365) en `-onbegrensd` (684/365); clingo-suite "a pro-rata
   part that does not fit: the witness is EF-006".
4. **De wortel eenmaal correct.** De afronding over een wortel is één aanroep `ROUNDED_SQUARE_ROOT(RADICAND(radicand), d,
   mode)`: `ComputeHelpers.roundedRoot` beslist in gehele getallen zoals ref's `sqrt_rounded`; `rsWortel` is uit de runtime weg.
   Witness `a_square_root_under_its_rounding_is_the_true_root_rounded_once` (√(1/9) op 20 naar beneden, √2 op 40 naar beneden en
   naar boven, √2 op 2 rekenkundig); ref `WV-2.2-u`, `-v`.
5. **Ch. 5.12 op `t/n`.** `RecordingRendering.given` collapset niet meer; `SessionDump.canonical` schrijft een `Breuk` als
   `t/n`; ref's `Reading.decimal` schrijft `Q.to_string`. Witness `a_reading_writes_a_value_that_does_not_terminate_as_its_fraction`
   (`-1/3`); ref `WV-5.12-ploegaandeel` (`3/7`). Gate 17 groen op beide branches.
6. **De gebroken exponent.** Drools: een exponent als literal mag nu negatief en gebroken zijn; `EXACT_POWER` geeft de exacte
   wortel of de fout van ref, `ROUNDED_POWER` onder een afronding de echte macht eenmaal afgerond (`roundedRoot` met graad q);
   `rsMacht` keert een negatieve exponent om ook bij een decimale basis. Teller of noemer boven 1000: DROOLS-300 (limit). Een
   exponent die geen literal is blijft DROOLS-173 (zin aangepast). Ref: `Arith.pow_rounded` en in `eval.ml` een arm van
   `Rounding` voor een `Power` met een gebroken literal als exponent, naast die van de wortel. Witnesses
   `a_fractional_exponent_is_an_exact_root_or_the_true_power_rounded_once` (8^(2/3) = 4, (1/3)^−1 = 3, 10^(1/3) op 10 naar
   beneden 2,1544346900 en naar boven 2,1544346901, 2^0,5 op 20 richting nul) en
   `an_exponent_past_the_bound_of_this_branch_is_refused_by_name`; ref `WV-2.2-p` tot `-t` en `WV-2.2-w` (in een sessie).
7. **Een onbegrensd attribuut dat een objectcreatie initialiseert.** Is de creatie de enige schrijver van dat lid (in de
   producentenindex), dan voegt zij de exacte waarde in als `Value` van het nieuwe object (`Consequence.Insert.Exact`, beide
   weergaven) in plaats van haar decimaal op de bean te zetten; de lezer leest via de stand, de write-back zet de decimaal op de
   bean. Witness `a_value_of_the_unbounded_form_initialises_a_new_object_exactly` (een Aandeel met deel 1/3; geheel = deel maal
   3 = 1, gelezen over de rol als controle = 1).
8. **De grens van de gehele periode.** `ComputeHelpers.wholePeriod` en `rsGeheelPeriode`/`rsTijdConditie` vergelijken de grens
   exact (`exactCompare`); de `COLLAPSE` om de grens van `RS_TIME_CONDITION` is weg. Witness
   `the_bound_of_a_whole_period_comparison_is_compared_exactly` (een segment 0,3333333333333333 is niet ten minste 1/3).
9. **Los.** Clingo: een niet-exacte deling binnen de geschreven waarde van een regel of een creatie-initialisatie is CLINGO-031
   (limit, `codes.tsv`), een deling die zelf de waarde is houdt EF-006; clingo-suite (x/3 maal 3: CLINGO-031; x/3: EF-006).
   Ch. 2.5: geen standaardmanier (de draad eist `mode`). Ch. 2.3 min/max: niet aangeraakt, dat verandert
   `static-precision-gate` in de typering. Javadoc van `Breuk` (en van `toBd`) beschrijft de huidige drager.

**Spec (voorstel, eigen alinea's).** Ch. 2.1 v0-noot: de exacte semantiek is normatief voor elke uitvoerder, een limiet op naam
waar een uitvoerder het niet kan, met voorbeeld (`WV-2.1-exact-a`, `-b`). Ch. 2.2: rij gebroken exponent en wortel, en een
alinea met de regel van de eenmaal-correcte afronding en voorbeelden (`WV-2.2-p` tot `-v`). Ch. 2.4: de Drools-zin bij de
onbegrensde vorm (vouwen exact, creatie exact, weigering van rationele invoer en van een deel in een onbegrensde tijdlijn) en een
eigen alinea "On a timeline target the gate holds per segment". Ch. 2.5: geen standaardmanier. Ch. 2.8 en 3.7: de OPEN-zin wordt
de norm, met een uitgewerkt voorbeeld (`WV-3.7-schrijfgrens-*`) en het rode geval. Ch. 5.7b: het rode geval bij EF-006. Ch. 5.12:
`t/n`, het voorbeeld 3/7.

### Rood eerst

Rode stand op `e284bab9a` (detached worktree, de nieuwe en veranderde testklassen en het rode geval erin gekopieerd, niet
gecommit; `logs/stap7-red-1.log`): 11 van 11 rood. Plus/min/maal "y: verwacht 0.001, werkelijk 0.00; p: werkelijk
1.000000000000000020000000000000000E+34"; wortel "r: werkelijk 0.33333333333333331666; s en t: werkelijk
1.4142135623730950488016887242096980000000"; gebroken exponent en grens: EXCLUDE DROOLS-173; creatie: `WriteBoundaryFault`
"de waarde 9999999999999999/10000000000000000 past niet" op geheel; gehele periode "verwacht leeg, werkelijk 7"; lezing
`0 Binary -0.3333333333333333`; tijdsevenredig deel NOFAULT; onbegrensd tijdlijndoel PLAYED; de twee vouwen EXCLUDE
DROOLS-296. Ref op `e284bab9a`: het rode geval "EXPECTED FAULT DID NOT OCCUR"; `10 tot de macht 1/3` afgerond: "fractional
power 10^1/3 is not exactly representable"; de trace van ploegaandeel `"J3" 6 Binary 0.4285714285714286`. Clingo op
`e284bab9a`: het rode geval een `viol` zonder code ("EXPECTED FAULT MISMATCH"); x/3 maal 3 EF-006 (nu CLINGO-031). Proefdocumenten
in `werk/drools-exact-arithmetic/probes/`.

Mutaties in een detached worktree van `e5fe54e4a` (de stand vóór de rebase; dezelfde code), elk teruggezet, niet gecommit
(`scratch/mutate.py`, `logs/stap10-mutations-1.log`):
M1 `DECIMAL128` terug in plus en min, M2 de wortel naar boven zonder +1, M3 de bijdrage collapset, M4 `ExactSum` telt een breuk
als decimaal, M5 het tijdsevenredig deel zonder toets, M6 de creatie zet de decimaal op de bean, M7 de lezing collapset, M8 de
grens van de gehele periode als decimaal, M9 de grens op de exponent 100 000 in plaats van 1000, M10 de dimensiesom collapset,
M11 geen weigering van een onbegrensd tijdlijndoel: elk maakt zijn witness rood. Ref: M12 de tijdlijn zonder schrijfgrens →
`WV-3.7-schrijfgrens-jaar` rood; M13 de gebroken macht zonder eigen arm → `WV-2.2-w` rood; M14 `t/n` verwisseld →
`WV-5.12-ploegaandeel` rood. Clingo: M15 elke deling binnen de waarde → "x/3 as the value" (en "unbounded form, a non-exact
division into it") rood; M16 de zin van het tijdsevenredig deel zonder code → "a pro-rata part that does not fit" rood. In een
eerste ronde vielen M9, M12, M13 en M15 verkeerd uit (M9 groen door de voorrang van `&&` en `||`, de andere drie compileerden
niet op een ongebruikte variabele); de tweede ronde (`logs/stap10-mutations-2.log`) heeft ze goed gemaakt.

### Aangepaste verwachtingen, pins en vloeren (oud → nieuw)

- `RationalAtRestWitnessTest.a_fold_over_a_value_of_the_unbounded_form_is_refused_by_name` (EXCLUDE DROOLS-296) →
  `a_fold_over_values_of_the_unbounded_form_adds_and_compares_them_exactly` (gespeeld); de kaart draait dit om.
- `DimensionCellWitnessTest.a_fold_over_a_written_cell_of_the_unbounded_form_is_refused_by_name` (EXCLUDE DROOLS-296) →
  `a_fold_over_written_cells_of_the_unbounded_form_adds_their_exact_values` (gespeeld).
- `FilterFaultWitnessTest.a_filter_that_fails_otherwise_than_by_a_division`: de zin van DROOLS-173 ("een macht met een exponent
  die geen geheel getal van ten minste 0 als literal is …: de reële macht is OPEN …") → "een macht met een exponent die geen
  literal is, of een geheel literal buiten ±999999999 …: deze tak rekent een macht alleen met een exponent die het document
  schrijft".
- `TemporalFixpointWitnessTest.two_unanchored_temporal_writers_reach_a_fixpoint`: jaartarief 36 → 366 (36 × 60/366 = 360/61
  past niet in 2 decimalen en is nu EF-006; de getuige gaat over het fixpunt, niet over de waarde).
- `FormsPlan` (vormfragmenten en handplan): `"sumBD("`, `"min("`, `"max("` → `"exactSum("`, `"exactMin("`, `"exactMax("`,
  `"import accumulate "`; `"ComputeHelpers.squareRoot("` → `"ComputeHelpers.radicand("`; `StaticFunction.SQUARE_ROOT` →
  `RADICAND`. Voorbeeld in de DRL-tekst: `$s : sumBD($v)` → `$s : exactSum($v)` met bovenaan `import accumulate
  dev.uchar.regelspraak.transpiler.tms.ExactSumFunction exactSum;`. Geen plandump en geen andere DRL-tekstpin bewoog (de
  som houdt haar `BigDecimal`-schaal zonder breuk; een tijdlijnplan heeft `PRO_RATA` waar `RS_TIME_PROPORTIONAL` stond, en geen
  test pinde dat).
- `ref/test` `WV-5.12-ploegaandeel`: `"6 Binary 0.4285714285714286"` → `"6 Binary 3/7"`.
- Vloeren: geen veranderd. `EXPECTED_PLAYED` blijft 315 (geen nieuwe golden; beide branches 315). De suite van de backend
  telt 1773 tests (op main 1764 + 9 nieuw), ref 2116 voorbeelden, clingo 2295 checks, gate 8 72 rode gevallen.

### Codes

- `RETIRED`: DROOLS-296 (`FOLD_OVER_UNBOUNDED_VALUE`).
- Nieuw: DROOLS-300 `POWER_EXPONENT_PAST_BOUND` (limit, `exponent,bound,snippet`), DROOLS-301 `PRO_RATA_UNBOUNDED_TARGET`
  (limit, `description,attribute,objectType`), CLINGO-031 `nested-non-exact-division` (limit, `viol_code`). Catalogi nl en en.

### Voorgestelde tekst voor de hoofdsessie

`spec/CONSTRUCTS.md` rij 2, achter de tekst van `drools-schrijfgrens`: "**Exact arithmetic everywhere a reader sees a value
(2026-11-02, card `drools-exact-arithmetic`):** RUNTIME-SEMANTICS ch. 2.1 is normative for every executor: +, − and × exact on
every number, a square root and a power with a fractional exponent under their rounding rounded once on the true value (ch. 2.2;
`ref` `Arith.pow_rounded`, the Drools backend `ComputeHelpers.roundedRoot`), an executor that cannot compute a form exactly
refuses it by name as a limit (DROOLS-300, CLINGO-031). The Drools backend folds exact values (`ExactSumFunction`,
`ExactMinimumFunction`, `ExactMaximumFunction` in both renderings; DROOLS-296 retired) and holds a value of the unbounded form
that an object creation initialises exactly in working memory. The write boundary holds per segment of a timeline target in all
three executors (ch. 2.4, 3.7; red case `jutterswet-tijdsevenredig-past-niet`); the Drools backend refuses a pro-rata part into
a timeline of the unbounded form (DROOLS-301). The canonical text of a reading writes a value that does not terminate as `t/n`
(ch. 5.12)."

`spec/CONSTRUCTS.md` rij 28: in "Still refused by name" vervalt "a fold over a written cell of the unbounded form in Drools
(DROOLS-296, card `20261102-drools-exact-arithmetic`)".

`CLAUDE.md` gate 12: DROOLS-296 bij de retired codes ("since the card `drools-exact-arithmetic` (2026-11-02) DROOLS-296").
Gate 8: geen floor verandert (`EXPECTED_MIN_CLINGO_CODES` 27, gemeten 31). `spec/PLAN.md`: geen scopewijziging.

### Voorlopige besluiten

1. **Ref buiten de tijdlijn.** De opdracht beperkt ref tot de schrijfgrens op een tijdlijn; de gebroken exponent "eenmaal
   correct afgerond" en `t/n` in ch. 5.12 vragen ook `arith.ml` en de arm van `Rounding` in `eval.ml` (naast die van de wortel)
   en `reading.ml`, anders spreken de norm en ref elkaar tegen. Smal gehouden: de arm geldt alleen voor een literal die geen
   geheel getal is.
2. **Een wortel en een gebroken macht onder hun afronding hebben geen lezing van hun knoop** (`unreached`), zoals ref ze nu
   leest: hun waarde vóór de afronding is irrationeel. Geen golden heeft er een (clingo weigert beide).
3. **Een negatieve basis met een gebroken exponent is een fout**, ook bij een oneven noemer, zoals ref's `pow_` al deed.
4. **De grens van DROOLS-300 is 1000** voor teller en noemer; een exponent die geen literal is blijft DROOLS-173.
5. **Een deel in een tijdlijn van de onbegrensde vorm weigert Drools statisch (DROOLS-301)**, ook waar het deel eindigt: een
   `Period` houdt een `BigDecimal`, en een weigering tijdens de fire heeft geen route.
6. **De creatie zet de exacte `Value` alleen waar zij de enige schrijver van het lid is**; schrijft een andere vorm het lid ook,
   dan blijft de waarde de invoer van het nieuwe object, als voorheen.
7. **CLINGO-031 alleen in de write van een regel en van een creatie-initialisatie.** Elders (een defaults-groep, een gesloten
   lichaam) houdt een niet-exacte deling de zin van EF-006, zodat geen rood geval verschuift.
8. **De toets op een tijdlijn in ref zit in `write_timeline_by`**, de write van een vorm; `release`, dat teruggeeft wat het doel
   had vóór een vorm schreef, toetst niet (na de rebase op `written-value-at-fixpoint`).
9. **Ch. 2.5: geen standaardmanier**; de draad eist `mode` en RegelSpraak §6.1.3 ook.
10. **`ExactSum` houdt de `BigDecimal`-som met haar schaal zolang er geen breuk bij komt**, zodat geen bestaande pin beweegt.

### Wat niet lukte of buiten de kaart bleef

- Ch. 2.3 min/max: niet aangeraakt; `static-precision-gate` verandert dat in de typering.
- Een golden voor de exacte vouw: niet mogelijk, clingo houdt de onbegrensde vorm als geheel getal; witnesses in de suite.
- Werkbank niet gemeten (niet gevraagd).

### make gates

Na de rebase op `3b75ac8e0`, één run op `f7c87d481` (`logs/stap9-gates-1.log`, `out/gates/20261102-225107`), alle regels groen:

```
start 2026-11-02 22:51:07 f7c87d481 card/drools-exact-arithmetic
ocaml rc=0 0s ocaml-build: rebuilt (29 source path(s) newer than the stamp, e.g. backends/clingo; 0s)
frontend rc=0 0s frontend: fresh (out/ newer than every source, the tool jars present)
jar rc=0 6s drools-jar: rebuilt (36 source path(s) newer than the jar, e.g. backends/drools/src/main/java/dev/uchar/regelspraak/cases; 6s)
01-self-contained rc=0 2s
02-ir-schema rc=0 127s
04-case-schema rc=0 7s
03-verifier rc=0 38s
05-oracle rc=0 22s
06-clingo rc=0 276s
07-noop rc=0 13s
08-red rc=0 168s
09-catala rc=0 5s
10-regelspraak rc=0 236s
11-toka-parity rc=0 226s
12-drools rc=0 337s
13-reftype rc=0 289s
14-flogic rc=0 2s
15-graph rc=0 48s
16-scenario rc=0 41s
17-readings rc=0 254s
after-run rc=0 0s the three builds fresh, no product newer than the end of the build step
end 2026-11-02 22:56:50 343s red=0
```

Gate 12: 315 gespeeld op beide branches, JUnit 1773. Gate 17: 315 goldens, 885 case files op beide branches. Gate 8: 72 rode
gevallen in ref en clingo, de Drools-leg 52 met de code van het geval. Een eerdere run vóór de rebase op `e5fe54e4a`
(`logs/stap8-gates-1.log`) was ook geheel groen.

**Landing (hoofdsessie, 2026-11-02).** Zelf nagemeten: het rode geval `jutterswet-tijdsevenredig-past-niet` gaf op ref en de jar van `3d4676b88` geen fout ("EXPECTED FAULT DID NOT OCCUR", `NOFAULT`) en geeft nu in ref en Drools EF-006 met `place` `L1.ligdeel`, `value` `135/14`, `decimals` 2. Nagelopen: de invoer 36 → 366 in `TemporalFixpointWitnessTest` houdt de bedoeling van die getuige (het fixpunt), en de nieuwe norm per segment heeft een eigen rood geval. Gerebased op `3b75ac8e0`, ff; `make gates` groen op `f7c87d481`, daarna alleen het Result. In dezelfde landing: CONSTRUCTS rij 2 en 28, CLAUDE.md gate 12 (DROOLS-296 retired). Ch. 2.3 min/max blijft bij `static-precision-gate`.
