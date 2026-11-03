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

Gebouwd op main `95947ae0f` in de worktree `wt/ref-whole-period` (branch `card/ref-whole-period`), samen met
`20261009-ref-gehele-periode-over-gemaakte-koppeling`. De kaart blijft in `doing/`; de hoofdsessie landt. Proefdocumenten,
scripts en logs onder `/Users/yeb/Developer/uchar-dev/werk/ref-whole-period/` (`probe/`, `logs/`).

### Commits

- `400dd77ce` beide kaarten naar `doing/`
- `6fc4bb8cc` ref: een lege grens onder "gedurende het gehele" faalt elke dag (`ref/lib/eval.ml`; rijen
  `WV-3.9-empty-bound-*` en `WV-3.9-filled-bound-*` in `ref/test/test_ref.ml`)
- `54db2872b` clingo: een eigen attribuut als grens onder "gedurende het gehele" (`backends/clingo/lib/translate.ml`,
  `backends/clingo/test/test_clingo.ml`)
- `aec7a5f32` goldens `jutterswet-gehele-jaar-lege-grens` en `jutterswet-gehele-jaar-gemaakte-koppeling`;
  `EXPECTED_PLAYED` 316 → 318 (voorstel)
- `5f593f85c` Javadoc "Measured" van `TmsWholePeriodWitnessTest` beschrijft ref en clingo nu
- de commit met dit Result

### Wat er in ref veranderde

In `Eval.eval_cond_node`, de arm van een vergelijking met `time_period = Some _`, is de dagtoets nu
`boundary <> Value.Empty && Arith.compare_op …`. Een lege grens voldoet op geen dag, voor elke vergelijking. Daardoor
faalt elke geraakte emmer en houdt de negatie van de kwalificatie; een tijdlijn zonder segmenten raakt geen emmer en
slaagt, ook met een lege grens. Alleen "ongelijk aan" verandert van uitkomst: "gelijk aan" (precies één leeg is onwaar)
en de ordeningen tegen leeg waren per ch. 1.2 al onwaar. Het commentaar bij de arm zegt waarom.

### Wat er in clingo veranderde (voorlopig besluit 1)

`whole_period_fails` draagt naast een literal en een scalaire parameter een numeriek attribuut van het onderwerp zelf als
grens: `member_read ~subject:"S"` geeft `val(S,"<attribuut>",A)` in de hulpregel `_g`, waar S de S van de tijdlijn is,
dus het onderwerp. Een lege grens geeft geen `val/3`, dus geen dag voldoet en elke geraakte emmer faalt: dezelfde lezing
als ref en `rsGeheelPeriode`. Een grens met minder decimalen dan de tijdlijn blijft geweigerd, omdat de harmonisatie dan
de grens vermenigvuldigt en geen hek die waarde meet (het bestaande hek meet de segmenten, die een fijnere grens
vermenigvuldigt). CLINGO-010 blijft de weigering van elke andere vorm (een grens over een pad, een samengestelde
uitdrukking); de zin noemt nu de drie gedragen vormen. `backends/clingo/codes.tsv` verandert niet
(`check-exec-codes: PASS`).

### Golden

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

### Rood eerst

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

### Mutaties (niet gecommit; `probe/mutate.py` op een losse worktree, `logs/step18-mutation-*.log`)

| | recept | rood |
|---|---|---|
| R1 | ref: `boundary <> Value.Empty &&` weg | `WV-3.9-empty-bound-not-equal-whole-year` en `-negated`; in de clingo-suite de twee ref-rijen van `notEqual`; golden case1 "MISMATCH: J1.ongelijkpremie = 10, expected leeg \| MISMATCH: J1.afwijkpremie = leeg, expected 30" |
| C1 | clingo: de arm van het eigen attribuut uit (`&& false`) | 24 rijen "ch3.9 empty bound …" en "ch3.9 own bound …" (vertaalfout), golden alle 4 CLINGO-010 |
| C2 | clingo: het schaalhek omgedraaid (`<= sa`) | "ch3.9 own bound coarser than the timeline: expected a loud fault, got a model", de twee rijen "own bound finer than the timeline" |

### De getuige `TmsWholePeriodWitnessTest` (alle documenten van zijn `main`, elke volgorde, `logs/step13-witness-now.log`, `step14-witness-main.log`)

| deel | `95947ae0f` | nu |
|---|---|---|
| A (24 volgordes × 2 cases) | ref PASS; clingo CLINGO-010 | ref en clingo PASS |
| A3 (6 × 2) | ref "MISMATCH: J5.ongelijkpremie = 10, expected leeg"; clingo CLINGO-010 | ref en clingo PASS |
| E, `e-emmer-fout` en `e-emmer-speelt` (6 elk) | ref PASS; clingo CLINGO-010 | ref en clingo PASS (J1 trouwpremie 10 in beide, gemeten met een extra verwachting) |
| B, C, `e-emmer-pad` | ref en clingo PASS | gelijk |
| R1–R3 | gelijk | gelijk |

De Javadoc "Measured" van de klasse zegt dat nu (alleen Javadoc). E blijft de bekende afwijking van ch. 8.4 in ref en
clingo (het emmerscalair als geen tijdlijn); de truth-maintenance-tak faalt daar, zoals de Javadoc beschrijft.

### Pins en tellingen

- `EXPECTED_PLAYED` van gate 12: 316 → 318 (voorstel; de twee goldens van deze branch, met naam in het commentaar). Geen
  vloer verlaagd.
- Clingo-suite, een rij omgedraaid door de verandering: "ch3.9 bound that the subject reads" (oud: het eigen attribuut
  `toeslag` als grens, verwacht de weigering "other than a literal or a scalar parameter") → "ch3.9 bound over a path"
  (nieuw: `keur` van de werf als grens, verwacht "other than a literal, a scalar parameter or a numeric attribute of the
  subject's own"). Nieuw: "ch3.9 own bound coarser than the timeline" en 24 rijen ref + clingo. 2320 → 2369 checks.
- Ref-suite 2123 → 2150 voorbeelden.

### Voorgestelde tekst `spec/CONSTRUCTS.md`

Rij 29, achteraan: "**Een lege grens (2026-11-03, kaart `ref-gehele-periode-lege-grens`, RS ch. 3.9):** een lege
vergelijkingsgrens voldoet op geen dag, voor elke vergelijking, ongelijk aan inbegrepen; een tijdlijn zonder segmenten
slaagt ook dan. `ref` las de ongelijkheid per dag met ch. 1.2 en volgt nu de Drools-runtime; `backends/clingo` draagt naast
een literal en een scalaire parameter een numeriek attribuut van het onderwerp zelf als grens, op de schaal van de
tijdlijn of fijner. Golden `jutterswet-gehele-jaar-lege-grens`."

### Voorgestelde tekst `spec/PLAN.md`

Regel 1115–1118: "and of A3 only `J5.ongelijkpremie` remains (card `20261009-ref-gehele-periode-lege-grens`)" wordt "and
since the card `20261009-ref-gehele-periode-lege-grens` (2026-11-03) A3 too, a leeg bound failing every day".

### Voorlopige besluiten

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

### Open vraag aan de eigenaar: een lege grens in ch. 3.6 en ch. 3.9

Ch. 3.6 leest de vergelijking van de knip "gedurende de tijd dat" per periode met ch. 1.2 (het voorbeeld "gedurende ≠
1000": "leeg **satisfies** ≠ against a filled bound (the ch. 1.2 inequality asymmetry, applied per period)"), dus een
lege grens maakt "ongelijk aan" waar op elke gevulde periode. Ch. 3.9 maakt dezelfde vergelijking tegen een lege grens
op elke dag onwaar, en een leeg segment voldoet daar aan geen toets (besluit 2026-09-26). Eén document met beide vormen
over dezelfde grens (`probe/clip`, verifier PASS; `wachtvergoeding per maand` 10 over 2025, `wachtgrens` leeg):

| uitvoerder | `ongelijksom` (knip, ≠) | `gelijksom` (knip, =) | `ongelijkpremie` (gehele jaar, ≠) |
|---|---|---|---|
| ref (nu) | 120 | 0 | leeg |
| clingo | "TRANSLATION FAULT: equality in a per-segment clip comparison is not translated (no corpus case exercises it)" | idem | — |
| Drools, beide branches | "EXCLUDE DROOLS-222: … with 'unequal to' in the clip …: this branch does not carry this form yet; the calculation helper rsTijdConditie reads an empty side, a gap in the compared timeline, or an empty boundary as no period, whereas RUNTIME-SEMANTICS ch. 3.6 and the reference interpreter apply the comparison of ch. 1.2 per period: two empty sides are equal, and exactly one empty side is unequal; no golden with cases has it" | | |

Met een gevulde grens 20 geeft ref 120, 0 en 10. Een lezer van de spec ziet dus "zijn wachtvergoeding ongelijk aan
zijn wachtgrens" met een lege grens in de ene vorm elke maand gelden en in de andere geen enkele dag. Vraag: houdt
ch. 3.6 de lezing van ch. 1.2 per periode, ook voor een lege grens (en ch. 3.9 de zijne), of gaat een van beide mee met
de ander? Niet beslist; ref volgt beide hoofdstukken zoals ze er staan. De Drools-runtimehulp `rsTijdConditie` leest
al zoals ch. 3.9; de bouwer van `drools-clip-equality` werkt aan deze knip en heeft hetzelfde antwoord nodig.

### Gates

Zie onder; de samenvatting van `make gates` staat in de laatste commit.
