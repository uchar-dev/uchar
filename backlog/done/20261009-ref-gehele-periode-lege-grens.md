# ref-gehele-periode-lege-grens: ref leest "ongelijk aan" tegen een lege grens onder "gedurende het gehele" als waar

*Gevonden op 2026-10-09 door de bouwer van stap 3 van `20261008-tms-verdeling-en-tijdlijnen` en door de hoofdsessie
nagemeten en vastgelegd volgens de afspraak dat een bestaande bug in ref een kaart krijgt. Eerst gemeten met ref, clingo
en de verifier uit `_build` van main `ac0f9008` en de shaded jar van die kaartbranch (`58614e85`).*

*Nagemeten op `95947ae0f` (2026-11-03) met ref, clingo en de verifier uit `_build` van de worktree `wt/ref-whole-period`
en de shaded jar van die worktree; de oude Drools-takken zijn sindsdien vervangen door de lowering met truth maintenance
(`transpiler/tms`). Regelnummers, foutzinnen en de tabel hieronder zijn van die meting.*

## Aanleiding

RUNTIME-SEMANTICS ch. 3.9 (regel 2833–2834): "A leeg comparison bound makes the test false, never a fault." Ref volgde
dat niet voor ongelijkheid: het las "ongelijk aan" tegen een lege grens per dag met de regel van ch. 1.2 (precies één
leeg is waar) en liet de kwalificatie slagen (`ref/lib/eval.ml` regel 2461, `Arith.compare_op` per segment).

Document (`TmsWholePeriodWitnessTest`, deel A3, uitgeschreven met de `main` van de testklasse; verifier PASS): een premie
"indien zijn wachtdagen gedurende het gehele jaar ongelijk is aan zijn drempel", met een lege drempel bij J5.

| Uitvoerder | J5.ongelijkpremie (6 volgordes, 2 cases) |
|---|---|
| ref | 10: "MISMATCH: J5.ongelijkpremie = 10, expected leeg" in alle 6 volgordes en beide cases |
| clingo | vertaalfout: "TRANSLATION FAULT: CLINGO-010: a whole-period comparison against a bound other than a literal or a scalar parameter is not translated: the bound rides inside the per-segment rule, which binds only the timeline's own subject (no corpus case)" |
| Drools, exec-model en DRL-tekst | leeg, `PLAYED 2` in elke volgorde (de runtimehulp `rsGeheelPeriode` volgt ch. 3.9: `segmentVoldoet` geeft onwaar bij een lege grens, `RegelSpraakRuntime.java` regel 1045) |

Geen golden en geen ref-test pinden een lege grens onder de kwalificatie.

## Acceptatie (bij oppakken aanscherpen)

- (a) Ref volgt ch. 3.9 voor elke vergelijking, rood eerst met een ref-test. Kiest de eigenaar toch de lezing van ch. 1.2
  per dag (een normvraag), dan gaan ch. 3.9, `rsGeheelPeriode` en de tak met truth maintenance mee.
- (b) Een golden met een lege scalaire parameter als grens (die vertaalt clingo wel), met "gelijk aan" en "ongelijk aan".
- (c) Alle gates groen.

## Omvang

Klein.

May touch: `ref/`, `corpus/golden/<nieuwe golden>/`, kaartverplaatsing.

## Result:

Gebouwd in de worktree `wt/ref-whole-period` (branch `card/ref-whole-period`), samen met
`20261009-ref-gehele-periode-over-gemaakte-koppeling`, in twee rondes. De eerste ronde volgde ch. 3.9 zoals het er stond
(hieronder als vorige stand). De tweede ronde volgt het besluit van de eigenaar over punt 4b van
`spec/proposals/IMPLICIT-TIME.md` (2026-11-03) en is gebouwd na een rebase op main `a9d572d59`. De kaart blijft in
`doing/`; de hoofdsessie landt. Proefdocumenten, scripts en logs onder `/Users/yeb/Developer/uchar-dev/werk/ref-whole-period/`
(tweede ronde: `probe4b/`, `logs4b/`; eerste ronde: `probe/`, `logs/`).

### Tweede ronde: de lege dag per ch. 1.2 (besluit 4b, 2026-11-03)

Het besluit: onder "gedurende de/het gehele …" leest een vergelijking elke dag met de leeg-regels van ch. 1.2, zoals de
knip van ch. 3.6. Een ordening is onwaar op een lege kant, "gelijk aan" bij een getal waar op twee lege kanten,
"ongelijk aan" waar op precies één; een gatdag, een dag onder een expliciet leeg segment en een lege grens zijn lege
kanten. Welke emmers de kwalificator raakt en de leeg- en gevuld-toetsen veranderen niet. Daarmee valt acceptatie (a)
in de tweede tak ("kiest de eigenaar toch de lezing van ch. 1.2 per dag"): ch. 3.9, `rsGeheelPeriode` en clingo gaan
mee, en ref komt terug op de lezing die het vóór de eerste ronde had, nu ook voor de gatdag.

#### Commits

- `fd4e3a2d5` ref: de lege dag volgt ch. 1.2 (`ref/lib/eval.ml`, `ref/lib/timeline.ml`, `ref/test/test_ref.ml`)
- `5461ccf5c` clingo: "ongelijk aan" onder de kwalificator leest een lege dag (`backends/clingo/lib/translate.ml`,
  `backends/clingo/test/test_clingo.ml`)
- `69e4bcd62` drools: `rsGeheelPeriode` leest een lege dag met ch. 1.2 (`RegelSpraakRuntime.java`, Javadoc in
  `TmsTranspiler.java`, `TmsWholePeriodWitnessTest`, `EmptyNegatedWitnessTest`)
- `92a039d72` golden `jutterswet-gehele-jaar-lege-grens` herrekend, twee cases erbij; het commentaar bij
  `EXPECTED_PLAYED` in `scripts/check-drools.sh` (voorstel; de waarde 319 blijft)
- `00b94ba63` RS ch. 3.9 (voorstel)
- de commit hierna: de Results van beide kaarten

#### Wat er veranderde

- **ref.** `Timeline.whole_period_value` telt per geraakte emmer de gatdagen bij de voldane dagen waar het predicaat
  op `Empty` geldt (`pred Empty`); een expliciet leeg segment valt vooraf weg en is dus ook een gatdag. In
  `Eval.eval_cond_node` is `boundary <> Value.Empty &&` van de eerste ronde weg: de dagtoets is weer
  `Arith.compare_op` met de statische vlaggen van de vergelijking, dus ch. 1.2 met de numerieke en de datumrij.
- **clingo.** `whole_period_fails` houdt voor de ordeningen en "gelijk aan" de ene faalregel (voldane dagen tegen de
  kalenderlengte): daar beslist een gatdag geen uitkomst, omdat een ordening er nooit op geldt en "gelijk aan" alleen
  tegen een lege grens, waar de gevulde dag die elke geraakte emmer heeft faalt. Voor "ongelijk aan" splitst de regel
  op de gedefinieerdheid van de grens (`gpvN_bound(S)`, uit de atomen van de grens): bij een gevulde grens faalt een
  emmer waar een gedekte dag niet voldoet (`GD < TD`, de gatdagen voldoen); bij een lege grens faalt hij waar hij een
  gatdag heeft (`TD < Tot`, elke gedekte dag voldoet). De drie gedragen grensvormen (literal, scalaire parameter, eigen
  numeriek attribuut, eerste ronde) blijven; `backends/clingo/codes.tsv` verandert niet.
- **Drools, beide branches.** Beide roepen `ComputeHelpers.wholePeriod` → `RegelSpraakRuntime.rsGeheelPeriode` aan.
  `segmentVoldoet` leest een lege waarde of grens nu met ch. 1.2 (toets 4: beide leeg; toets 5: precies één; een
  ordening onwaar), en de vouw telt per geraakte emmer de dagen zonder waarde mee waar `segmentVoldoet(toets, null,
  grens)` geldt, behalve bij de kenmerktoets 7. De leeg-toets (8) valt daarmee onder dezelfde drempel days(B): een
  gatdag haalt hem, dus dezelfde uitkomst als de oude drempel van de geraakte dagen. De lowering in `TmsTranspiler`
  verandert niet, alleen haar Javadoc.
- **RS ch. 3.9** (voorstel, `spec/RUNTIME-SEMANTICS.md`): de eerste bullet (een gatdag of lege dag faalt een ordening,
  de gevuld- en de kenmerktoets; een vergelijking leest hem met ch. 1.2), de lege grens in de derde bullet ("a leeg side
  of the comparison on every day, read with ch. 1.2"), het lege segment binnen de periode (alleen nog "onwaar onder
  `> 40`"), de alinea over het expliciete lege segment ("≠ 40" over 50 in de eerste helft en leeg in de tweede: nu
  **waar**, 365 van 365, en `> 40` onwaar), en een nieuwe alinea "A leeg day in a comparison" met het besluit, de zin
  dat de knip van ch. 3.6 en de kwalificator een lege dag gelijk lezen, uitgewerkte voorbeelden (gatdagen tegen een
  gevulde en een lege grens) en het voorbeeld `WV-3.9-lege-dag` met K1 en K2 als tabel.

#### Golden `jutterswet-gehele-jaar-lege-grens`

Met de hand herrekend (2025, 365 dagen; `=`/`≠` de voldane dagen). Oud is de lezing van de eerste ronde.

| case | `wachtdagen` | `wachtgrens` | `=` / `≠` | gelijk / ongelijk / afwijk oud | nieuw |
|---|---|---|---|---|---|
| case1-lege-grens | 20 heel 2025 | leeg | 0 / 365 | leeg / leeg / 30 | leeg / **10** / **leeg** |
| case2-lege-grens-geen-tijdlijn | geen | leeg | geen emmer | 20 / 10 / leeg | gelijk |
| case3-gevulde-grens-gelijk | 20 heel 2025 | 20 | 365 / 0 | 20 / leeg / 30 | gelijk |
| case4-gevulde-grens-ongelijk | 25 heel 2025 | 20 | 0 / 365 | leeg / 10 / leeg | gelijk |
| case5-gatdagen-gevulde-grens (nieuw, K1) | 7 in januari | 5 | 0 / 31 + 334 | (leeg / leeg / 30) | leeg / 10 / leeg |
| case6-gatdagen-lege-grens (nieuw) | 7 in januari | leeg | 334 / 31 | (leeg / leeg / 30) | leeg / leeg / 30 |

Case 6 verandert niet van uitkomst, maar houdt twee foute lezingen tegen: een lege grens die "ongelijk aan" elke dag laat
gelden (ook op een gatdag) en een gatdag die "gelijk aan" tegen een lege grens de emmer laat halen. Ref, clingo en
beide Drools-branches: 6 keer `oracle: PASS`, 6 keer `clingo: PASS`, `PLAYED 6` en `PLAYED 6`
(`logs4b/s13-golden-green.log`). `NOTE.md` is herschreven naar het besluit. Geen datumcase (voorlopig besluit 1).

#### Rood eerst

- Golden met de nieuwe verwachtingen op de binaries van vóór de reparatie (`logs4b/s03-golden-red.log`): case1 en case5
  in ref "MISMATCH: J1.ongelijkpremie = leeg, expected 10 | MISMATCH: J1.afwijkpremie = 30, expected leeg", in clingo
  "MISMATCH: J1.ongelijkpremie is underived (leeg), expected 10 | MISMATCH: J1.afwijkpremie = [30], expected leeg", op
  beide Drools-branches "FAIL case1-lege-grens.json: attribuut ongelijkpremie: verwacht 10, werkelijk leeg; verwacht
  leeg, werkelijk 30 | case5-gatdagen-gevulde-grens.json: …" (dezelfde zin); case 2, 3, 4 en 6 PASS.
- Ref-suite met de nieuwe rijen vóór de reparatie (`logs4b/s05-reftest-red.log`): "8 failure(s)": `WV-3.9-vgl-gat-ongelijk`,
  `-gat-ongelijk-niet`, `-leeg-segment-ongelijk-is-een-gat`, `WV-3.9-empty-bound-not-equal-whole-year`, `-negated`,
  `WV-3.9-filled-bound-not-equal-half-year`, `WV-3.9-lege-dag-k1-ongelijk`, `-k2-ongelijk`. Daarna: "test_ref: all 2164
  worked examples pass" (`logs4b/s16-reftest-green.log`).
- Clingo-suite vóór de reparatie (`logs4b/s07-clingotest-red.log`): "test_clingo: 2398 checks, 12 FAILURES", zes rijen in ref
  en clingo: "ch3.9 empty bound notEqual whole year", "… negated", "ch3.9 own bound not-equal over the gap", "ch3.9 value
  not-equal over the gap", "…, negated", "temporal: a whole-period not-equal parameter bound over the gap". Daarna
  "test_clingo: 2398 checks, 0 failures" (`logs4b/s11-clingotest-green.log`).
- `TmsWholePeriodWitnessTest` vóór de reparatie (`logs4b/s08-witness-red.log`): "Tests run: 7, Failures: 1", de A3-test in
  alle 12 regels (6 volgordes × 2 cases) "attribuut ongelijkpremie: verwacht 10, werkelijk leeg" voor J5 en J7, op beide
  branches gelijk (`playBoth`). Daarna beide klassen groen, "Tests run: 15, Failures: 0, Errors: 0, Skipped: 0"
  (`logs4b/s14-witness-green.log`). De omgedraaide rij van `EmptyNegatedWitnessTest` is rood onder mutatie D1 hieronder,
  de stand van vóór de reparatie voor dat stuk.

#### Tests die omgingen (oud → nieuw)

- ref `WV-3.9-vgl-leeg-segment-ongelijk-is-een-gat` (op main sinds 2026-09-26): 50 op de eerste helft van 2026 en
  expliciet leeg op de tweede, `≠ 40`: oud **onwaar** (181 van 365) → nieuw **waar** (365 van 365).
- ref `WV-3.9-empty-bound-not-equal-whole-year` (eerste ronde): oud onwaar → nieuw **waar**;
  `WV-3.9-empty-bound-not-equal-negated`: oud waar → nieuw **onwaar**. De andere `WV-3.9-empty-bound-*`-rijen houden
  hun waarde; de lijst draagt de verwachting nu per operator.
- clingo "ch3.9 empty bound notEqual whole year" (eerste ronde): oud onwaar → nieuw waar; "ch3.9 empty bound notEqual
  negated": oud waar → nieuw onwaar.
- Drools `EmptyNegatedWitnessTest.an_empty_segment_also_touches_no_bucket_for_a_comparison` (op main sinds
  2026-09-26): de tijdlijn 50 op de eerste helft van 2026 en leeg op de tweede, toets 5 tegen 40: oud `assertFalse`
  ("≠ 40: 181 of 365 days") → nieuw `assertTrue` ("≠ 40: 181 days 50 and 184 leeg days, 365 of 365"); erbij
  `assertFalse` voor toets 2 (`> 40`, 181 van 365). De Javadoc zegt de lezing nu.
- Drools `TmsWholePeriodWitnessTest` A3 (op main): de test `an_empty_bound_fails_every_day_also_for_not_equal_to` heet nu
  `a_leeg_day_is_read_with_the_comparison_of_chapter_1_2_in_every_order_on_both_branches`; J5 (geen drempel, heel
  2024 wachtplichtig): `ongelijkpremie` oud leeg → nieuw **10**. Erbij J7 (drempel 20, alleen januari: 10 / leeg) en
  J8 (geen drempel, alleen januari: leeg / leeg), en elke volgorde draait nu op beide branches (`playBoth`) in plaats
  van alleen exec-model. Javadoc van A, A3 en "Measured" bijgewerkt.

Nieuw: ref `WV-3.9-vgl-gat-ongelijk`, `-gat-gelijk`, `-gat-ongelijk-niet`, `-gat-ongelijk-op-de-grens`,
`WV-3.9-filled-bound-not-equal-half-year`, `-equal-half-year`, `WV-3.9-lege-dag-k1-ongelijk`, `-k1-gelijk`,
`-k2-ongelijk`, `-k2-gelijk`; clingo (elk in ref en clingo) "ch3.9 empty bound ⟨op⟩ half year" voor de zes
operatoren, "ch3.9 own bound not-equal over the gap", "…, on the bound", "ch3.9 own bound equal over the gap", "ch3.9
value not-equal over the gap", "…, negated", "…, on the bound", "ch3.9 value equal over the gap", "temporal: a
whole-period not-equal parameter bound over the gap". Per arm van `whole_period_fails` een rij: literal, scalaire
parameter, eigen attribuut gevuld en eigen attribuut leeg.

#### Mutaties (niet gecommit; `probe4b/mutate4b.py`, één losse worktree, `logs4b/s17-mutation-*.log`)

| | recept | rood |
|---|---|---|
| R1 | ref: `gap_satisfies = false` | ref: `WV-3.9-vgl-gat-ongelijk`, `-gat-ongelijk-niet`, `-leeg-segment-ongelijk-is-een-gat`, `WV-3.9-filled-bound-not-equal-half-year`, `WV-3.9-lege-dag-k1-ongelijk`; clingo-suite de ref-kant van vier gatrijen; golden case5 in ref |
| R2 | ref: `boundary <> Value.Empty &&` terug | ref: `WV-3.9-empty-bound-not-equal-whole-year`, `-negated`, `WV-3.9-lege-dag-k2-ongelijk`; clingo-suite de ref-kant van twee rijen; golden case1 in ref |
| C1 | clingo: "ongelijk aan" weer op de algemene regel | zes clingo-rijen (beide grenzen, literal, parameter); golden case1 en case5 in clingo |
| C2 | clingo: de regel bij een gevulde grens uit (`1 = 0`) | "ch3.9 own bound not-equal on the bound", "… over the gap, on the bound", "ch3.9 value not-equal over the gap, on the bound"; golden case3 in clingo |
| C3 | clingo: de regel bij een lege grens uit | "ch3.9 empty bound notEqual half year"; golden case6 in clingo |
| D1 | Drools: `gatVoldoet = false` | `EmptyNegatedWitnessTest` (de omgedraaide rij), de A3-test; golden case5 op beide branches |
| D2 | Drools: toets 5 onwaar bij een lege grens | de A3-test; golden case1 op beide branches |

#### Andere goldens

Geen golden verandert. Gemeten over `corpus/golden`, `corpus/red`, `corpus/negative` en `toka/` (script
`/private/tmp/claude-501/-Users-yeb-Developer-uchar-dev-uchar/0f057885-693c-417d-8351-b7bfac514f31/scratchpad/scan_wp.py`):
de enige "gelijk aan" en "ongelijk aan" onder de kwalificator staan in deze golden; elke andere vergelijking eronder
(`jutterswet-vast-startpunt`, `-keurploeg`, `-sleepvloot`, `-reddingsvloot`, `-ploegpremie`) is "groter dan" een
literal, waar een gatdag in beide lezingen niet voldoet. De gates 5, 6, 12 en 17 bevestigen het (hieronder).

Het proefdocument van `drools-clip-equality` (`werk/drools-clip-equality/scratch/vraag/`, `logs4b/s13-golden-green.log`): alle vier
de uitvoerders geven nu K1.afwijktoeslag 1 en K2.afwijktoeslag 1, de lezing van het besluit (IMPLICIT-TIME: K1 en K2
"ongelijk aan" waar). Zijn twee casefiles pinnen de oude lezingen (`case.json` ch. 3.9 zoals het er stond voor K1 en K2,
`case-k2-een.json` dat voor K1) en falen daarom nu in alle vier, zoals verwacht.

#### De getuige `TmsWholePeriodWitnessTest` in ref en clingo (`probe4b/witness_docs.py`, `logs4b/s15-witness-docs.log`)

A3 (6 volgordes × 2 cases, 9 verwachtingen) en A (24 × 2, 21 verwachtingen): ref en clingo PASS in elke volgorde, de
verifier PASS; 150 PASS-regels. De Javadoc "Measured" zegt dat.

#### Voorgestelde tekst `spec/CONSTRUCTS.md`

Rij 29, achter "(decision point 4b; RegelSpraak §8.2 with typeringen §5.3 and §5.6; card
`20261009-ref-gehele-periode-lege-grens`)": "**Landed (2026-11-03, card `ref-gehele-periode-lege-grens`):** decision
point 4b in all four executors. `ref` counts a gap day, a day under a leeg segment included, as satisfying where the
comparison holds on leeg; `backends/clingo` reads "ongelijk aan" by the definedness of the bound and carries, beside a
literal and a scalar parameter, a numeric attribute of the subject's own as the bound (at the timeline's scale or
finer); the Drools runtime fold `rsGeheelPeriode`, which both branches call, reads a leeg moment and a leeg bound with
ch. 1.2. Golden `jutterswet-gehele-jaar-lege-grens` (both bounds, gap days, the qualifier's negation); no other golden
changes, since every other comparison under the qualifier in the corpus is "groter dan" a literal." Dit vervangt de
tekst die de eerste ronde voorstelde (hieronder).

#### Voorgestelde tekst `spec/PLAN.md`

Regel 1116–1118: "and of A3 only `J5.ongelijkpremie` remains (card `20261009-ref-gehele-periode-lege-grens`)" wordt "and
since the card `20261009-ref-gehele-periode-lege-grens` (2026-11-03, decision point 4b of
`spec/proposals/IMPLICIT-TIME.md`) A3 too, where `J5.ongelijkpremie` is 10 in all four executors". Dit vervangt het
voorstel van de eerste ronde.

#### Voorlopige besluiten

1. **Geen datumcase in de golden.** Beide Drools-branches weigeren een datumkant onder de kwalificator
   (`PERIOD_NON_NUMERIC_COMPARE`, "a side that is no number", en `WHOLE_PERIOD_DATE_COMPARISON` voor een ordening) en
   clingo weigert een datumordening, dus een datumcase haalt het vierkant niet. Bovendien geeft de datumrij hier
   dezelfde uitkomsten als de numerieke: ze verschillen alleen op een dag met twee lege kanten, en een emmer met zo'n dag
   heeft ook een gevulde dag, die "gelijk aan" tegen een lege grens in beide rijen faalt. RS ch. 3.9 zegt dat.
2. **Clingo krijgt geen regel voor gatdagen onder "gelijk aan".** Zo'n regel verandert geen uitkomst (zie boven) en
   geen mutatie kan hem meten; het commentaar bij de faalregels zegt waarom.
3. **Drools: de leeg-toets onder dezelfde drempel.** De aparte drempel van de geraakte dagen voor toets 8 is vervangen
   door het meetellen van de gatdagen, wat voor elke tijdlijn dezelfde uitkomst geeft (een gatdag haalt de leeg-toets);
   zo is er één regel voor elke toets, de regel van ch. 3.9.
4. **De namen van de tests.** De ref-rij `WV-3.9-vgl-leeg-segment-ongelijk-is-een-gat` houdt haar naam (het expliciete
   lege segment is nog steeds een gat); de Drools-test van A3 kreeg een naam die de nieuwe lezing zegt. Uit de
   Javadoc van `EmptyNegatedWitnessTest` viel de zin over de stand van vóór 2026-09-26 weg, omdat de alinea opnieuw
   geschreven is.
5. **`scripts/check-drools.sh`** (beschermd): alleen het commentaar bij `EXPECTED_PLAYED`; de waarde 319 van de eerste
   ronde blijft een voorstel.

#### Gates

`make gates` op `00b94ba63` (de commits van de tweede ronde; daarna verandert alleen de kaart),
`out/gates/20261103-112911/`, `logs4b/gates-1.log`:

```
start 2026-11-03 11:29:11 00b94ba63 card/ref-whole-period
ocaml rc=0 0s ocaml-build: rebuilt (12 source path(s) newer than the stamp, e.g. backends/clingo/lib; 0s)
frontend rc=0 0s frontend: fresh (out/ newer than every source, the tool jars present)
jar rc=0 0s drools-jar: fresh (jar newer than 547 source files)
01-self-contained rc=0 2s
02-ir-schema rc=0 126s
04-case-schema rc=0 7s
03-verifier rc=0 39s
05-oracle rc=0 22s
06-clingo rc=0 268s
07-noop rc=0 14s
08-red rc=0 142s
09-catala rc=0 6s
10-regelspraak rc=0 228s
11-toka-parity rc=0 212s
12-drools rc=0 324s
13-reftype rc=0 285s
14-flogic rc=0 2s
15-graph rc=0 48s
16-scenario rc=0 41s
17-readings rc=0 242s
after-run rc=0 0s the three builds fresh, no product newer than the end of the build step
end 2026-11-03 11:34:35 324s red=0
```

Gate 5: ref-suite 2164 voorbeelden, 1031 cases; gate 6: clingo-suite 2398 checks, 1031 cases; gate 12: played 319 op
beide branches, JUnit 1775 tests; gate 17: 319 goldens en 901 case files vergeleken, deviations 0.

### Eerste ronde (vorige stand): ch. 3.9 zoals het er stond

Gebouwd op main `95947ae0f` en gerebased op main `27cfd3dfa` (de kaart `runtime-tijdconditie-leeg`) in de worktree
`wt/ref-whole-period` (branch `card/ref-whole-period`), samen met
`20261009-ref-gehele-periode-over-gemaakte-koppeling`. Proefdocumenten, scripts en logs onder
`/Users/yeb/Developer/uchar-dev/werk/ref-whole-period/` (`probe/`, `logs/`). De hashes hieronder zijn die na de rebase op
`a9d572d59`; de lezing van de lege grens, de rijen en de verwachtingen van deze ronde zijn door de tweede vervangen
(zie "Tests die omgingen"), de clingo-uitbreiding met een eigen attribuut als grens blijft.

#### Commits

- `33d2f4182` beide kaarten naar `doing/`
- `7fd246eb2` ref: een lege grens onder "gedurende het gehele" faalt elke dag (`ref/lib/eval.ml`; rijen
  `WV-3.9-empty-bound-*` en `WV-3.9-filled-bound-*` in `ref/test/test_ref.ml`)
- `76ea79e07` clingo: een eigen attribuut als grens onder "gedurende het gehele" (`backends/clingo/lib/translate.ml`,
  `backends/clingo/test/test_clingo.ml`)
- `fcf37d2e1` goldens `jutterswet-gehele-jaar-lege-grens` en `jutterswet-gehele-jaar-gemaakte-koppeling`;
  `EXPECTED_PLAYED` 317 → 319 (voorstel)
- `7262c89ee` Javadoc "Measured" van `TmsWholePeriodWitnessTest` beschrijft ref en clingo nu
- `b40eb971b` en de commits hierna: dit Result

#### Wat er in ref veranderde

In `Eval.eval_cond_node`, de arm van een vergelijking met `time_period = Some _`, is de dagtoets nu
`boundary <> Value.Empty && Arith.compare_op …`. Een lege grens voldoet op geen dag, voor elke vergelijking. Daardoor
faalt elke geraakte emmer en houdt de negatie van de kwalificatie; een tijdlijn zonder segmenten raakt geen emmer en
slaagt, ook met een lege grens. Alleen "ongelijk aan" verandert van uitkomst: "gelijk aan" (precies één leeg is onwaar)
en de ordeningen tegen leeg waren per ch. 1.2 al onwaar. Het commentaar bij de arm zegt waarom.

#### Wat er in clingo veranderde (voorlopig besluit 1)

`whole_period_fails` draagt naast een literal en een scalaire parameter een numeriek attribuut van het onderwerp zelf als
grens: `member_read ~subject:"S"` geeft `val(S,"<attribuut>",A)` in de hulpregel `_g`, waar S de S van de tijdlijn is,
dus het onderwerp. Een lege grens geeft geen `val/3`, dus geen dag voldoet en elke geraakte emmer faalt: dezelfde lezing
als ref en `rsGeheelPeriode`. Een grens met minder decimalen dan de tijdlijn blijft geweigerd, omdat de harmonisatie dan
de grens vermenigvuldigt en geen hek die waarde meet (het bestaande hek meet de segmenten, die een fijnere grens
vermenigvuldigt). CLINGO-010 blijft de weigering van elke andere vorm (een grens over een pad, een samengestelde
uitdrukking); de zin noemt nu de drie gedragen vormen. `backends/clingo/codes.tsv` verandert niet
(`check-exec-codes: PASS`).

#### Golden

`corpus/golden/jutterswet-gehele-jaar-lege-grens` (`source.rgs`, `ir.json` uit de frontend, `NOTE.md`), 4 cases, elk
consistent: `wachtdagen` (maand) van J1 over 2025 tegen zijn `wachtgrens`, met "gelijk aan" (`gelijkpremie` 20),
"ongelijk aan" (`ongelijkpremie` 10) en "niet gedurende het gehele jaar ongelijk aan" (`afwijkpremie` 30).

| case | `wachtdagen` | `wachtgrens` | gelijk / ongelijk / afwijk |
|---|---|---|---|
| case1-lege-grens | 20 | leeg | leeg / leeg / 30 |
| case2-lege-grens-geen-tijdlijn | geen | leeg | 20 / 10 / leeg |
| case3-gevulde-grens-gelijk | 20 | 20 | 20 / leeg / 30 |
| case4-gevulde-grens-ongelijk | 25 | 20 | leeg / 10 / leeg |

Ref, clingo en beide Drools-branches: 4 keer `oracle: PASS`, 4 keer `clingo: PASS`, `PLAYED 4` en `PLAYED 4`. De
lezingen van gate 17 (ref `--trace` tegen `--readings` van beide branches) zijn letterlijk gelijk.

#### Rood eerst

- `ref/test`, de nieuwe rijen vóór de reparatie (`logs/step10-reftest-red.log`): `FAIL WV-3.9-empty-bound-not-equal-whole-year`,
  `FAIL WV-3.9-empty-bound-not-equal-negated`, "2 failure(s)". Na de reparatie: "test_ref: all 2150 worked examples pass".
- De golden op de binaries van `95947ae0f` (`logs/step19-goldens-base-main.log`; nu `step19-goldens-now.log`): ref case1
  "MISMATCH: J1.ongelijkpremie = 10, expected leeg | MISMATCH: J1.afwijkpremie = leeg, expected 30", cases 2–4 PASS;
  clingo alle 4 "TRANSLATION FAULT: CLINGO-010: a whole-period comparison against a bound other than a literal or a
  scalar parameter is not translated: …"; Drools `PLAYED 4` op beide branches.
- De clingo-suite met de nieuwe rijen tegen de code van `95947ae0f` (`logs/step15-clingotest-red.log`): "test_clingo:
  2369 checks, 28 FAILURES" — de 18 rijen "ch3.9 empty bound … : clingo pins the values" (vertaalfout), daarvan twee ook
  "ref pins the values" (`notEqual whole year`, `notEqual negated`), de 6 rijen "ch3.9 own bound …", en de twee
  weigeringsrijen op hun nieuwe zin.

#### Mutaties (niet gecommit; `probe/mutate.py` op een losse worktree, `logs/step18-mutation-*.log`)

| | recept | rood |
|---|---|---|
| R1 | ref: `boundary <> Value.Empty &&` weg | `WV-3.9-empty-bound-not-equal-whole-year` en `-negated`; in de clingo-suite de twee ref-rijen van `notEqual`; golden case1 "MISMATCH: J1.ongelijkpremie = 10, expected leeg \| MISMATCH: J1.afwijkpremie = leeg, expected 30" |
| C1 | clingo: de arm van het eigen attribuut uit (`&& false`) | 24 rijen "ch3.9 empty bound …" en "ch3.9 own bound …" (vertaalfout), golden alle 4 CLINGO-010 |
| C2 | clingo: het schaalhek omgedraaid (`<= sa`) | "ch3.9 own bound coarser than the timeline: expected a loud fault, got a model", de twee rijen "own bound finer than the timeline" |

#### De getuige `TmsWholePeriodWitnessTest` (alle documenten van zijn `main`, elke volgorde, `logs/step13-witness-now.log`, `step14-witness-main.log`)

| deel | `95947ae0f` | nu |
|---|---|---|
| A (24 volgordes × 2 cases) | ref PASS; clingo CLINGO-010 | ref en clingo PASS |
| A3 (6 × 2) | ref "MISMATCH: J5.ongelijkpremie = 10, expected leeg"; clingo CLINGO-010 | ref en clingo PASS |
| E, `e-emmer-fout` en `e-emmer-speelt` (6 elk) | ref PASS; clingo CLINGO-010 | ref en clingo PASS (J1 trouwpremie 10 in beide, gemeten met een extra verwachting) |
| B, C, `e-emmer-pad` | ref en clingo PASS | gelijk |
| R1–R3 | gelijk | gelijk |

De Javadoc "Measured" van de klasse zegt dat nu (alleen Javadoc). E blijft de bekende afwijking van ch. 8.4 in ref en
clingo (het emmerscalair als geen tijdlijn); de truth-maintenance-tak faalt daar, zoals de Javadoc beschrijft.

#### Pins en tellingen

- `EXPECTED_PLAYED` van gate 12: 317 → 319 (voorstel; de twee goldens van deze branch, met naam in het commentaar,
  na de 317 van `jutterswet-peilwacht`; de rebase loste het conflict op die regel zo op). Geen vloer verlaagd.
- Clingo-suite, een rij omgedraaid door de verandering: "ch3.9 bound that the subject reads" (oud: het eigen attribuut
  `toeslag` als grens, verwacht de weigering "other than a literal or a scalar parameter") → "ch3.9 bound over a path"
  (nieuw: `keur` van de werf als grens, verwacht "other than a literal, a scalar parameter or a numeric attribute of the
  subject's own"). Nieuw: "ch3.9 own bound coarser than the timeline" en 24 rijen ref + clingo. 2321 → 2370 checks (main
  `27cfd3dfa`: 2321).
- Ref-suite 2123 → 2150 voorbeelden.

#### Voorgestelde tekst `spec/CONSTRUCTS.md` (vervangen door de tweede ronde)

Rij 29, achteraan: "**Een lege grens (2026-11-03, kaart `ref-gehele-periode-lege-grens`, RS ch. 3.9):** een lege
vergelijkingsgrens voldoet op geen dag, voor elke vergelijking, ongelijk aan inbegrepen; een tijdlijn zonder segmenten
slaagt ook dan. `ref` las de ongelijkheid per dag met ch. 1.2 en volgt nu de Drools-runtime; `backends/clingo` draagt naast
een literal en een scalaire parameter een numeriek attribuut van het onderwerp zelf als grens, op de schaal van de
tijdlijn of fijner. Golden `jutterswet-gehele-jaar-lege-grens`."

#### Voorgestelde tekst `spec/PLAN.md` (vervangen door de tweede ronde)

Regel 1115–1118: "and of A3 only `J5.ongelijkpremie` remains (card `20261009-ref-gehele-periode-lege-grens`)" wordt "and
since the card `20261009-ref-gehele-periode-lege-grens` (2026-11-03) A3 too, a leeg bound failing every day".

#### Voorlopige besluiten

1. **De grens van de golden is een attribuut, en clingo draagt dat.** (b) vraagt een lege scalaire parameter, maar die is
   in een case niet te schrijven: het case-schema heeft geen lege waarde (`spec/schema/uchar-case-v0.schema.json`,
   `$defs/value`), en een scalaire parameter die de case niet bindt, is in ref EF-014 ("parameter … is unbound in this case
   (binding is fail-fast, ch. 5.7)") en in clingo dezelfde fail-fast (het commentaar bij `whole_period_fails` zei het al).
   Een attribuut weigerde clingo met CLINGO-010 en gate 6 kent geen weigeringskanaal, dus zonder de kleine uitbreiding
   in clingo kon geen golden met een lege grens het vierkant halen. Het alternatief, een lege waarde in het case-formaat,
   raakt `spec/schema/` (beschermd) en heb ik niet gekozen.
2. **"The test" van ch. 3.9 is de dagtoets.** Een tijdlijn zonder segmenten slaagt dus ook tegen een lege grens (case 2).
   Zo lezen `rsGeheelPeriode` (`if (tijdlijn == null) return true`, en geen geraakte emmer) en de ASP-join; de lezing
   "de gevouwen uitkomst is onwaar" zou case 2 omdraaien en alle drie de uitvoerders.
3. **Het schaalhek van clingo**: een grens met minder decimalen dan de tijdlijn blijft geweigerd (zie boven).
4. **De Javadoc van `TmsWholePeriodWitnessTest`** in `backends/drools` is bijgewerkt, omdat zij de stand van ref en
   clingo beschrijft; buiten de "may touch" van de kaart, alleen tekst.

#### Vraag, bij de hoofdsessie: een lege grens in ch. 3.6 en ch. 3.9 (beantwoord: besluit 4b)

Ch. 3.6 leest de vergelijking van de knip "gedurende de tijd dat" per periode met ch. 1.2 (het voorbeeld "gedurende ≠
1000": "leeg **satisfies** ≠ against a filled bound (the ch. 1.2 inequality asymmetry, applied per period)"), dus een
lege grens maakt "ongelijk aan" waar op elke gevulde periode. Ch. 3.9 maakt dezelfde vergelijking tegen een lege grens
op elke dag onwaar, en een leeg segment voldoet daar aan geen toets (besluit 2026-09-26). Eén document met beide vormen
over dezelfde grens (`probe/clip`, verifier PASS; `wachtvergoeding per maand` 10 over 2025, `wachtgrens` leeg;
`logs/step22-after-rebase.log`):

| uitvoerder | `ongelijksom` (knip, ≠) | `gelijksom` (knip, =) | `ongelijkpremie` (gehele jaar, ≠) |
|---|---|---|---|
| ref, clingo, beide Drools-branches, na de rebase op `27cfd3dfa` | 120 | 0 | leeg |

Met een gevulde grens 20: 120, 0 en 10, ook in alle vier. Op `95947ae0f`, vóór `runtime-tijdconditie-leeg`, weigerde
clingo de knip ("TRANSLATION FAULT: equality in a per-segment clip comparison is not translated (no corpus case exercises
it)") en Drools ook ("EXCLUDE DROOLS-222: … rsTijdConditie reads an empty side, a gap in the compared timeline, or an
empty boundary as no period, whereas RUNTIME-SEMANTICS ch. 3.6 and the reference interpreter apply the comparison of
ch. 1.2 per period …"). De uitvoerders volgen nu dus allebei de hoofdstukken zoals ze er staan, en een lezer ziet "zijn
wachtvergoeding ongelijk aan zijn wachtgrens" met een lege grens in de ene vorm elke maand gelden en in de andere geen
enkele dag. Het document van de bouwer van `drools-clip-equality` (`/Users/yeb/Developer/uchar-dev/werk/drools-clip-equality/scratch/vraag/`,
verifier PASS; peilstand 7 over 2024, drempelstand leeg) meet hetzelfde (`logs/step23-vraag.log`): `case.json`, met
`afwijktoeslag` leeg volgens ch. 3.9, gaf op `95947ae0f` in ref "MISMATCH: K2.afwijktoeslag = 1, expected leeg" en in clingo
CLINGO-010, en is nu in ref en clingo PASS; `case-k2-een.json`, dat de lezing van ch. 1.2 pint (`afwijktoeslag` 1), faalt
nu in ref, clingo en beide Drools-branches.

Of ch. 3.6 en ch. 3.9 een lege dag of grens bewust tegengesteld lezen, legt de hoofdsessie neer bij het voorstel
impliciete-tijd (bericht van 2026-11-03); deze branch volgt ch. 3.9 zoals het er staat en beslist het niet.

#### Gates

`make gates` op `d19b72012` (gerebased op `27cfd3dfa`; daarna verandert alleen de kaart), `out/gates/20261103-095725/`:

```
start 2026-11-03 09:57:25 d19b72012 card/ref-whole-period
ocaml rc=0 0s ocaml-build: fresh (stamp newer than 229 source files)
frontend rc=0 0s frontend: rebuilt (0s)
jar rc=0 0s drools-jar: fresh (jar newer than 547 source files)
01-self-contained rc=0 2s
02-ir-schema rc=0 131s
04-case-schema rc=0 7s
03-verifier rc=0 41s
05-oracle rc=0 23s
06-clingo rc=0 333s
07-noop rc=0 15s
08-red rc=0 148s
09-catala rc=0 5s
10-regelspraak rc=0 254s
11-toka-parity rc=0 241s
12-drools rc=0 385s
13-reftype rc=0 343s
14-flogic rc=0 2s
15-graph rc=0 50s
16-scenario rc=0 41s
17-readings rc=0 272s
after-run rc=0 0s the three builds fresh, no product newer than the end of the build step
end 2026-11-03 10:03:50 385s red=0
```

Gate 5: ref-suite 2150 voorbeelden, 1028 cases; gate 6: clingo-suite 2370 checks, 1028 cases; gate 12: played 319 op
beide branches, JUnit 1774 tests; gate 17: 319 goldens en 898 case files vergeleken, deviations 0. Main `b3feb3882`
(de twee frontendkaarten over tijd) is na deze run geland; volgens de hoofdsessie neemt zij die rebase bij de landing.
