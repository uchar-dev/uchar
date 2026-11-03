# runtime-tijdconditie-leeg: `rsTijdConditie` leest een lege kant van een vergelijkingsknip als geen periode

*Gevonden op 2026-10-09 door de bouwer van stap 2 van `20261008-tms-verdeling-en-tijdlijnen` en door een lens op die
stap, en door de hoofdsessie vastgelegd volgens de afspraak dat een bestaande bug in de gedeelde runtime een kaart
krijgt. Nagemeten op `95947ae0f` (2026-11-03), met ref, clingo en de verifier uit `_build` van de worktree
`wt/drools-clip-equality` en de shaded jar van die worktree.*

## Aanleiding

RUNTIME-SEMANTICS ch. 3.6 laat een totaal knippen op "de tijd dat" een vergelijking van een eigen tijdsafhankelijk
attribuut met een grens geldt, en leest die vergelijking per periode met de regels van ch. 1.2. Bij een getal is twee
keer leeg gelijk en precies één keer leeg ongelijk, en een gat in de getoetste tijdlijn leest leeg (ch. 3.1). Het
voorbeeld van 500,00 in ch. 3.6 pint dat voor ongelijkheid.

De oude exec-model- en DRL-teksttak zijn weg. De lowering met truth maintenance (`transpiler/tms`: exec-model en
DRL-tekst, twee weergaven van één analyse) rekent de vergelijkingsknip met de runtimehulp `rsTijdConditie`
(`backends/drools/.../runtime/RegelSpraakRuntime.java`, via `StaticFunction.RS_TIME_CONDITION`), die er op
`95947ae0f` nog staat. Die hulp loopt alleen de segmenten van de getoetste tijdlijn langs, zodat een gat geen periode
is, en geeft bij een lege grens geen enkele periode. Voor de ordeningen klopt dat met ch. 1.2; voor "gelijk aan" en
"ongelijk aan" niet, en daarom weigert de tak die twee met DROOLS-222.

Document (de golden `jutterswet-peilwacht` van deze kaart): een Jutter met een wachtvergoeding per maand van 10 van
januari tot en met juni 2024, een peilstand van 5 alleen in januari, en "het totaal van zijn wachtvergoeding per maand
gedurende de tijd dat zijn peilstand (on)gelijk is aan zijn drempelstand". Verifier PASS.

| Geval | met de hand (ch. 3.6, 1.2) | ref | tak met truth maintenance | die tak zonder de weigering | clingo |
|---|---|---|---|---|---|
| gelijk aan, grens leeg | 50 | 50 | `EXCLUDE DROOLS-222` | 0 | `TRANSLATION FAULT` |
| gelijk aan, grens 5 | 10 | 10 | `EXCLUDE DROOLS-222` | 10 | `TRANSLATION FAULT` |
| ongelijk aan, grens 5 | 50 | 50 | `EXCLUDE DROOLS-222` | 0 | `TRANSLATION FAULT` |
| ongelijk aan, grens leeg | 10 | 10 | `EXCLUDE DROOLS-222` | 0 | `TRANSLATION FAULT` |

Letterlijk, op beide branches gelijk: `EXCLUDE DROOLS-222: transpiler SKIP: regel: peilsom van een jutter — a total
over its own timeline ('het totaal van zijn wachtvergoeding per maand gedurende de tijd dat zijn peilstand gelijk is
aan zijn drempelstand') with 'equal to' in the clip ('gedurende de tijd dat zijn peilstand gelijk is aan zijn
drempelstand'): this branch does not carry this form yet; the calculation helper rsTijdConditie reads an empty side, a
gap in the compared timeline, or an empty boundary as no period, whereas RUNTIME-SEMANTICS ch. 3.6 and the reference
interpreter apply the comparison of ch. 1.2 per period: two empty sides are equal, and exactly one empty side is
unequal; no golden with cases has it`. Clingo: `TRANSLATION FAULT: equality in a per-segment clip comparison is not
translated (no corpus case exercises it)`, exit 1. Ref: `oracle: PASS` op alle drie case-bestanden, met de waarden
die met de hand uit ch. 3.6 en ch. 1.2 volgen. De kolom "zonder de weigering" is de tak met alleen de weigering
weggehaald en de oude hulp: `FAIL case1-grens-leeg.json: attribuut peilsom: verwacht 50, werkelijk 0; attribuut
afwijksom: verwacht 10, werkelijk 0; …`, op beide branches gelijk.

## Acceptatie

- (a) Een golden met de vier gevallen hierboven, eigen Jutterswet-casus, met absolute verwachtingen volgens ch. 3.6 en
  ch. 1.2, met de hand nagerekend (ref is niet de maat).
- (b) De tak met truth maintenance draagt "gelijk aan" en "ongelijk aan" in een knip op beide branches, rood eerst;
  DROOLS-222 `RETIRED` als geen plek haar meer noemt; de rijen `knip-gelijk` en `knip-ongelijk` van
  `TmsTimelineRefusalWitnessTest` spelende getuigen of naar de golden.
- (c) Clingo vertaalt gelijkheid in een knip, of weigert haar met naam zoals nu.
- (d) Drools-suite groen zonder skips, gates 5, 6, 12 en 17 groen op de golden, `make gates` groen.

## Omvang

Klein.

May touch: `backends/drools/**` (de runtimehulp en de weigering in `transpiler/tms/`), `backends/clingo/`,
`corpus/golden/<nieuwe golden>/`, `scripts/check-drools.sh` (pins, voorstel), kaartverplaatsing.

## Result

Branch `card/drools-clip-equality` op main `95947ae0f`, niet gemerged en niet gepusht; `scripts/check-drools.sh` is een
beschermd pad, dus de branch is een voorstel.

**Commits.** `b552c9ea4` kaart naar `doing/`; `b9679d0db` aanleiding nagemeten (de meting op main staat hierboven);
`7264f709e` drools; `86015dabf` clingo; `b7239015b` golden en `EXPECTED_PLAYED`; daarna dit Result.

**Meting op main.** Zie de aanleiding: ref gaf op alle drie case-bestanden de met de hand gerekende waarden, beide
Drools-branches `EXCLUDE DROOLS-222`, clingo `TRANSLATION FAULT`. Een correctie op de opdracht: `rsTijdConditie` stond
op `95947ae0f` nog in de runtime (`RegelSpraakRuntime.java:853`) en rekende de knip van de ordeningen; alleen de
gelijkheden weigerde de tak.

**Tak met truth maintenance (exec-model en DRL-tekst, één analyse).** `rsTijdConditie(reeks, toets, grens, within,
segCls)` leest de vergelijking per periode met de regels van ch. 1.2 voor een getal (een ordening houdt op geen lege
kant, "gelijk aan" op twee, "ongelijk aan" op precies één) en voegt, waar een lege kant aan de vergelijking voldoet,
de gaten van `reeks` binnen de segmenten van `within` toe, elk zonder waarde (via `rsTijdKnipNiet`). `comparisonClip`
geeft de geknipte tijdlijn als `within` mee en weigert niets meer om de operator. De lezing (ch. 5.12, gate 17) blijft
"de segmenten van de getoetste tijdlijn waar de vergelijking geldt": dezelfde hulp met `within` null, gesneden op de
knippen van de geknipte tijdlijn; een gat heeft geen segment en staat er dus niet in, zoals in ref
(`Timeline.clip tested periods`). Een onbekende toetscode gooit nu in plaats van stil onwaar te geven. DROOLS-222 is
`RETIRED`; geen plek noemt de code. `StaticFunction.RS_TIME_CONDITION`, `ClosureRendering`, de javadoc van `clip` en
`backends/drools/README.md` beschrijven de nieuwe stand.

**Clingo.** Vertaalt "gelijk aan" en "ongelijk aan" in de knip, gekozen omdat gate 6 geen weiger- of divergentielijst
heeft: elke golden moet door clingo spelen (de opdracht noemde zo'n lijst; die bestaat niet). De gaten van de getoetste
tijdlijn zijn de maximale stukken van een segment van de geknipte tijdlijn die geen getoetst segment dekt, met de twee
kandidaatregels van de ontkende kenmerkknip; een helper `…_bound(S)` zegt of de grens bindt. Gelijk: gevulde segmenten
`==` plus de gaten als de grens niet bindt. Ongelijk: gevulde segmenten `!=`, alle gevulde segmenten als de grens niet
bindt, de gaten als hij wel bindt. Een datumkant weigert de vertaling met naam ("equality in a per-segment clip
comparison over dates is not translated (no corpus case exercises it)"), omdat de datumrij van ch. 1.2 twee lege
datums niet gelijk noemt; getuige in `test_clingo.ml` (2320 → 2321 checks). `compare_sym` krijgt voor een gelijkheid
een interne fout: geen aanroeper geeft haar die nog.

**Ref.** Ongewijzigd; gaf op main al de met de hand gerekende waarden.

**Golden `jutterswet-peilwacht`** (RegelSpraak-bron, IR via de frontend; wachtvergoeding per maand 10 van januari tot
en met juni 2024). Drie case-bestanden, vijftien verwachtingen:
`case1-grens-leeg` (J1 peilstand 5 in januari: peilsom 50, afwijksom 10; J2 zonder peilstand: 60 en 0),
`case2-grens-gevuld` (drempelstand 5; J1: 10 en 50; J2 zonder peilstand: 0 en 60; J3 peilstand 5 in januari en 7 in
februari–maart: 10 en 50), `case3-ch36-voorbeeld` (het voorbeeld van 500,00: wachtvergoeding 100, peilstand en
drempelstand 1000: peilsom 100, afwijksom 500). Elke waarde met de hand uit ch. 3.6 en ch. 1.2, een gat als leeg
(ch. 3.1). Speelt in ref, clingo en beide Drools-branches (`PLAYED 3`); de lezingen van beide branches zijn gelijk
aan de trace van ref.

**Rood.** Op main: `EXCLUDE DROOLS-222` op beide branches (letterlijk in de aanleiding), clingo `TRANSLATION FAULT`,
ref zonder `MISMATCH`. Met alleen de weigering weggehaald en de oude hulp: `FAIL case1-grens-leeg.json: attribuut
peilsom: verwacht 50, werkelijk 0; attribuut afwijksom: verwacht 10, werkelijk 0; attribuut peilsom: verwacht 60,
werkelijk 0 | case2-grens-gevuld.json: attribuut afwijksom: verwacht 50, werkelijk 0; attribuut afwijksom: verwacht
60, werkelijk 0; attribuut afwijksom: verwacht 50, werkelijk 20 | case3-ch36-voorbeeld.json: attribuut afwijksom:
verwacht 500, werkelijk 0`, op beide branches. Mutaties op `b7239015b` in een losse worktree, niet gecommit:
- M1, geen gaten in `rsTijdConditie`: deel F faalt, "attribuut gelijke wachtsom: verwacht 50, werkelijk 0; attribuut
  afwijksom: verwacht 60, werkelijk 10; …".
- M2, twee lege kanten niet gelijk: deel F faalt, "… gelijke wachtsom: verwacht 50, werkelijk 0; … gelijke wachtsom:
  verwacht 500, werkelijk 0".
- M3, de lezing met de gaten: de lezingen wijken op de golden af van ref, bijvoorbeeld `"J1" 2 Comparison []` tegen
  `"J1" 2 Comparison [[2024-02-01 2024-07-01 empty]]` (gate 17 vergelijkt die regels letterlijk).
- M4, clingo zonder de gatregel van "gelijk aan": `MISMATCH: J1.peilsom = [0], expected 50` en `J2.peilsom = [0],
  expected 60`.
- M6, clingo zonder de regel "gevuld segment, lege grens" van "ongelijk aan": `MISMATCH: J1.afwijksom = [0],
  expected 10`.
- M7, clingo zonder de gatregel van "ongelijk aan": `MISMATCH` op J1, J2 en J3 van case2 (0, 0 en 20) en op case3
  (0 waar 500).
- M5, clingo zonder de datumweigering: `FAIL gedurende clip, gelijk-aan over a date timeline: expected a loud fault,
  got a model`.

**Tests, oud en nieuw.** `TmsTimelineRefusalWitnessTest` had de rijen `knip-gelijk` en `knip-ongelijk`, die de weigering
met de zin van DROOLS-222 pinden; ze zijn weg, met `MODEL_CLIP`, `clipSentence` en hun alinea. Nieuw:
`TmsTimelineWitnessTest`, deel F, `a_clip_by_equality_reads_a_gap_and_a_leeg_bound_by_ch_1_2_on_both_branches_in_every_order`:
dezelfde documenten in één, met de drie regels in elke volgorde op beide branches, de oude waarden erin (J1 gelijke
wachtsom 50, J2 10, J3 afwijksom 500, het voorbeeld van ch. 3.6 met de grens als literal) en daarnaast 10, 60, 50, 60,
500 en 100. Gemeten: verifier, ref en clingo halen alle zes volgordes.

**Codes en pins.** DROOLS-222 `REFUSAL` → `RETIRED` (301 codes, 14 retired). `EXPECTED_PLAYED` 316 → 317, met naam.
Drools-suite 1774 tests, nul skips (één test erbij, deel F; geen weg). Clingo-suite 2320 → 2321 checks (gemeten).
Gate 17 vergelijkt 317 goldens, de nieuwe erbij.

**Voorgestelde tekst, CLAUDE.md gate 12**, in de opsomming van de retired codes na "since the card
`drools-exact-arithmetic` (2026-11-02) DROOLS-296": ", since the card `runtime-tijdconditie-leeg` (2026-11-03)
DROOLS-222".

**Voorgestelde tekst, `spec/CONSTRUCTS.md` rij 11** (achteraan): "**Landed (2026-11-03, card
`runtime-tijdconditie-leeg`):** de vergelijkingsknip van ch. 3.6 met `gelijk aan` en `ongelijk aan`, per periode
gelezen met de vergelijking van ch. 1.2 (een gat in de getoetste tijdlijn en een lege grens zijn leeg), speelt in `ref`,
`backends/clingo` en beide Drools-branches (golden `jutterswet-peilwacht`). Delta ten opzichte van de geporteerde
productieruntime: `rsTijdConditie` las een gat en een lege grens als geen periode; de hulp neemt nu de gaten binnen de
geknipte tijdlijn mee (DROOLS-222 retired). Clingo weigert een datumkant bij naam (de datumrij van ch. 1.2)."

**Voorgestelde tekst, `spec/IR-SPEC.md` ch. 4.3, de rij `filter`** (beschermd): "The negated clip (the complement) and
the Gelijk/Ongelijk comparison clip execute in the reference interpreter only — the ASP side faults loudly on both."
wordt "The negated clip (the complement) and the Gelijk/Ongelijk comparison clip execute in both executors
(`jutterswet-tegenwacht`, `jutterswet-peilwacht`); the ASP side refuses an equality over a date timeline by name.".

**Voorlopige besluiten.**
1. De runtimehulp gerepareerd in plaats van een eigen hulp in de tak: de oude takken zijn weg, de ordeningen
   gebruikten dezelfde hulp al, en één implementatie blijft één. De nieuwe parameter en hulpfunctie zijn Engels
   (`within`, `comparisonHolds`); de bestaande runtimenamen blijven. De distributie krijgt de veranderde hulp bij haar
   volgende pin.
2. Clingo vertaalt, omdat gate 6 een golden anders rood maakt; een datumkant blijft met naam geweigerd.
3. De twee weigerrijen zijn een spelende getuige geworden (deel F) en tegelijk door de golden gedekt; de literalgrens
   van het voorbeeld van ch. 3.6 staat alleen in F.
4. De lezing van een vergelijkingsknip laat de gaten weg, naar de letter van ch. 5.12 en zoals ref.

**Open vragen.**
1. *Ch. 3.6 tegen ch. 3.9.* Dezelfde vergelijking leest een lege dag in de twee collapses omgekeerd. In de knip
   (ch. 3.6) geldt ch. 1.2 per periode: een gat voldoet aan "ongelijk aan 5", en tegen een lege grens geldt "ongelijk
   aan" op elke gevulde dag. Onder "gedurende het gehele jaar" (ch. 3.9) voldoet een gatdag aan geen enkele toets,
   ongelijkheid inbegrepen ("although ch. 1.2d makes a scalar inequality against leeg true"), en maakt een lege grens
   de toets onwaar. Gemeten op een eigen document (`werk/drools-clip-equality/scratch/vraag/`, verifier PASS;
   wachtvergoeding per maand 10 over 2024): K1, peilstand 7 alleen in januari 2024 en drempelstand 5: knip "ongelijk
   aan" 120 in ref en Drools; "gedurende het gehele jaar ongelijk aan" onwaar in ref en Drools. K2, peilstand 7 over
   heel 2024 en drempelstand leeg: knip "ongelijk aan" 120 in ref en Drools, knip "gelijk aan" 0; "gedurende het gehele
   jaar ongelijk aan" in ref **waar** (`MISMATCH: K2.afwijktoeslag = 1, expected leeg`), in beide Drools-branches
   onwaar (`verwacht 1, werkelijk leeg`), terwijl ch. 3.9 onwaar zegt; clingo weigert dat document (CLINGO-010, een
   attribuut als grens onder het gehele jaar). De afwijking van ref ligt op het terrein van `ref-whole-period` en is
   hier niet gerepareerd. De vraag: is het bedoeld dat de twee collapses een lege dag van dezelfde vergelijking
   tegengesteld lezen? Zo ja, dan kan ch. 3.6 dat naast zijn voorbeeld van 500,00 zeggen, met een verwijzing naar
   ch. 3.9; zo nee, dan verschuift een van de twee, en daarmee de goldens van dat hoofdstuk.
2. *De lezing van een knip die alleen op gaten geldt.* Bij J1 van case1 houdt "gelijk aan" februari–juni (peilsom 50),
   maar de lezing van de vergelijking is `Comparison []`, omdat ch. 5.12 "the segments of the compared timeline where
   it holds" leest en een gat geen segment heeft. Wie de lezingen leest, ziet een vergelijking die nergens geldt en
   een totaal van 50. Is dat de bedoelde lezing, of hoort een gat waar de vergelijking geldt er als periode zonder
   waarde in (wat ref en Drools dan beide moeten veranderen)?
3. *Waarneming buiten de kaart.* De klassedoc van `TmsTimelineRefusalWitnessTest` zegt "Every document is
   verifier-clean" en "Measured 2026-10-09 …: the verifier passes all fifteen"; op `95947ae0f` vallen drie van de nu
   veertien documenten op de verifier (`dimensiecel-op-tijdlijn` en `totaal-over-dimensies` op WF-125.timeline,
   `vreemde-noemer` op WF-023.precision-unbounded). Niet aangeraakt.

**`make gates`** (run `20261103-093907` op `b7239015b`, 410 s):

```
01-self-contained rc=0 2s
02-ir-schema rc=0 159s
04-case-schema rc=0 7s
03-verifier rc=0 46s
05-oracle rc=0 23s
06-clingo rc=0 328s
07-noop rc=0 13s
08-red rc=0 175s
09-catala rc=0 5s
10-regelspraak rc=0 284s
11-toka-parity rc=0 275s
12-drools rc=0 410s
13-reftype rc=0 358s
14-flogic rc=0 2s
15-graph rc=0 57s
16-scenario rc=0 49s
17-readings rc=0 300s
after-run rc=0 0s the three builds fresh, no product newer than the end of the build step
end 2026-11-03 09:45:57 410s red=0
```

Gate 12: "played matches the pinned 317", "DRL-text branch played 317", "JUnit 1774 tests, floor 1573". Gate 17:
"on both branches 317 goldens and 890 case files compared, … deviations 0".

**Samenvoegen.** `EXPECTED_PLAYED` overlapt met `ref-whole-period`; mijn regel staat onder die van
`static-precision-gate` en verhoogt 316 → 317. Wie later landt, telt zijn goldens erbij op.
