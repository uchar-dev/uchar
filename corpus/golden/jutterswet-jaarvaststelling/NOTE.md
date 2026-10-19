# jutterswet-jaarvaststelling — the yearly determination as a parameter set with two jaargangen

The corpus witness of the parameter set of the scenariotoetsen extension
(`frontends/regelspraak/grammar/DIVERGENCES.md`, "Scenariotoetsen"): one
`Parameterset` block with two jaargangen per parameter, chosen by two
scenarios whose peildatum lies one day apart and in different jaargangen.
The expected outcomes of the two scenarios differ by the jaargang alone:
both run on the same situation and against the same rules.

## The statute and the values

Art. 7 of the Jutterswet (`corpus/jutterswet/wet-v4.md`): the eilandraad
determines the vondstdrempel and the penningbedrag each year for the
coming penningjaar, and a missed determination does not carry over — the
statute has no substitute value. The values are the corpus data of
`corpus/jutterswet/versiegeschiedenis.md`:

| penningjaar | ground | vondstdrempel | penningbedrag |
|---|---|---|---|
| 2025 | transitional law of the Third amending regulation | 12 | € 250 |
| 2026 | determination under art. 7 of 25 November 2025 | 14 | € 275 |

Beside those two the set carries two parameters of this golden's own, not
statute text: `het fondspercentage`, the share of the penning that goes to
the eilandfonds (10 % in 2025, 8 % in 2026), and `de uitbetaaldag`, the day
the strandvoogd pays the penning of that year (12 December 2025, 11
December 2026). They give the set a percentage and a date beside a
threshold and an amount.

## The source (`source.rgs`)

Four parameters without granularity and one `Jutter` with a
characteristic and four attributes. Four rules:

- `penningrecht bij drempel` (art. 3): a Jutter is `penninggerechtigd` if
  his `aantal aangemelde vondsten` is at least the vondstdrempel;
- `uitkering van de penning` (art. 8): his `uitkering` is the
  penningbedrag if he is penninggerechtigd;
- `afdracht aan het eilandfonds`: his `afdrachtpercentage` is the
  fondspercentage if he is penninggerechtigd;
- `betaaldatum van de penning`: his `betaaldatum` is the uitbetaaldag if he
  is penninggerechtigd.

Gate 10 re-emits `ir.json` from `source.rgs` alone byte-identically; the
scenario route writes the same document, since the scenarios carry no
scope.

## The scenario document (`scenario.rgs`)

`Parameterset jaarlijkse vaststelling:` with eight rows, four per
jaargang: `van 01-01-2025 tot 01-01-2026` and `van 01-01-2026 tot
01-01-2027`, each from inclusive to exclusive. One situation, `twee
jutters`: J1 with 13 reported finds, J2 with 15. Two scenarios, each
`met parameterset jaarlijkse vaststelling` and based on that situation:
`oudejaarsdag 2025` with peildatum 31-12-2025 and `nieuwjaarsdag 2026` with
peildatum 01-01-2026. The expectations are written by hand in
`scenario.rgs`; the lowering transcribes them and writes the parameters
of the jaargang that holds the peildatum, and gate 16 regenerates both
case files and `ir.json` from the two sources byte-identically.

## Hand computation (absolute)

| | case1-oudejaarsdag-2025 (jaargang 2025: 12, € 250, 10 %, 2025-12-12) | case2-nieuwjaarsdag-2026 (jaargang 2026: 14, € 275, 8 %, 2026-12-11) |
|---|---|---|
| J1 (13 finds) | penninggerechtigd; uitkering **250**; afdrachtpercentage **10**; betaaldatum **2025-12-12** | not penninggerechtigd (13 < 14); uitkering, afdrachtpercentage and betaaldatum **leeg** |
| J2 (15 finds) | penninggerechtigd; **250**; **10**; **2025-12-12** | penninggerechtigd; **275**; **8**; **2026-12-11** |

Both cases end `consistent`. J1 shows the threshold of the jaargang, J2
the amount, the percentage and the date of the jaargang.

## Measured (2026-09-27)

- verifier: `verifier: PASS`.
- `ref`: `oracle: PASS (9 expectations)` for each case.
- ASP backend: `clingo: PASS (9 expectations)` for each case.
- Drools: `PLAYED 2` in the exec-model branch and `PLAYED 2` in the
  DRL-text branch.

## What it pins

The choice of the jaargang. Case 2 with the parameters of jaargang 2025
— what a lowering writes that takes the first jaargang instead of the one
that holds the peildatum — fails in all three executors on seven
expectations: `ref` answers `MISMATCH: J1 carries characteristic
"penninggerechtigd", expected absent` first, the ASP backend the same
line, and the Drools exec branch `FAIL case2-nieuwjaarsdag-2026.json:
kenmerk penninggerechtigd: verwacht afwezig, werkelijk aanwezig; …`.

The exclusive end of a period. The two peildata lie on either side of the
boundary 01-01-2026; read with an inclusive end, the two jaargangen of each
parameter would share that day, and the frontend refuses a set whose
jaargangen overlap.

A percentage of the penningbedrag is not in this golden: the ASP backend
refuses that product (`TRANSLATION FAULT: a unit-carrying multiplicative
operand is not translated`, measured on a draft of this source), so the
fondspercentage is written into an attribute of its own.

The refusals of the parameter set — overlapping jaargangen, no jaargang
on the peildatum, a parameter in the set and in the situation, a scenario
without peildatum — are pinned in the frontend suite
(`ScenarioParameterSetTest`), because a document that carries one writes no
case file.

## noop refutation (gate 7)

No derived attribute or characteristic is in the input world, so the
`--noop` run fails on both cases: case1 on eight expectations (`MISMATCH:
J1 lacks characteristic "penninggerechtigd"` first) and case2 on four
(`MISMATCH: J2 lacks characteristic "penninggerechtigd"` first). Not a
survivor.
