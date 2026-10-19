# jutterswet-gehele-periode-leeg-rgs — the spellable whole-period leeg and gevuld tests, from source

The RegelSpraak twin of `jutterswet-gehele-periode-leeg`. That golden is
hand-written because two of its seven rules carry the inner negation of the
leeg and gevuld tests (`negated: true` under the qualifier), which
RegelSpraak cannot spell: the frontend's grammar reads `… gedurende het
gehele jaar leeg is` and `… niet gedurende het gehele jaar leeg is`, and
has no `niet` before `leeg` or `gevuld`. This twin carries the five rules
that can be spelled, in `source.rgs`, so gate 10 holds the frontend's
emission of the form: the leeg test, the gevuld test, the qualifier's own
negation, the month grid and a navigated operand.

## Source and emission

`source.rgs` declares `Werf` with the month-granular `havengeld`, `Schuit`
with the month-granular `bemanning` and five whole-number results, and the
fact type `ligplaats` (one `werf`, many `schuiten`). The five rules write
10, 20, 50, 60 and 70 under the conditions of the rules of the same name in
the hand-written golden:

| target | value | condition |
|---|---|---|
| `leegtoeslag` | 10 | de bemanning gedurende het gehele jaar leeg |
| `vultoeslag` | 20 | de bemanning gedurende het gehele jaar gevuld |
| `onderbrekingstoeslag` | 50 | de bemanning niet gedurende het gehele jaar leeg |
| `maandtoeslag` | 60 | de bemanning gedurende de gehele maand gevuld |
| `werftoeslag` | 70 | het havengeld van de werf gedurende het gehele jaar gevuld |

Measured 2026-09-26: each of the five emitted `Rule` objects is equal,
source snippets included, to the rule of the same name in
`jutterswet-gehele-periode-leeg/ir.json`, and so are the declarations the
twin shares with it. The hand-written golden is therefore the emission of
this source plus the two rules the source cannot write.

## Cases

The three cases of the hand-written golden, with the expectations on
`nietleegtoeslag` and `nietvultoeslag` taken out: a timeline that is empty
for the whole year (`case1-geheel-leeg`), one that is filled for the whole
year (`case2-geheel-gevuld`) and one that is filled for half of it
(`case3-gemengd`), eleven expectations each. The hand computation is in the
hand-written golden's NOTE.

## Executors (measured 2026-09-26)

- verifier: `verifier: PASS`.
- `ref`: `oracle: PASS (11 expectations)` on each case; the ASP backend
  `clingo: PASS (11 expectations)` on each.
- Drools: `PLAYED 3` in the exec-model branch and `PLAYED 3` in the DRL-text
  branch.
- An expectation twisted from 60 to 61 (`S1.maandtoeslag`, case3) fails in
  all four: `MISMATCH: S1.maandtoeslag = 60, expected 61` in `ref`,
  `MISMATCH: S1.maandtoeslag = [60], expected 61` in clingo, and
  `FAIL case3.json: attribuut maandtoeslag: verwacht 61, werkelijk 60` in
  both Drools branches.

## noop refutation (gate 7)

The `--noop` run fails all three cases: case1 and case2 on 8 expectations,
case3 on 5 (`MISMATCH: S1.leegtoeslag = leeg, expected 10` first on case1).
Not a survivor.
