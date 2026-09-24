# jutterswet-bergingsklok — the day-kind duration forms, executed

The first corpus document whose **day-kind predicate is evaluated**. Until
2026-09-19 a day kind could only be *carried* (`jutterswet-schouwrooster`
carries the unit spellings, `jutterswet-schouwkalender` two definitions), because
the two forms that would execute one — `tijdsduur … in hele <dagsoort>`
(§6.10) and `datum ± N <dagsoort>` (§6.11) — were refused by `ref` and by the
ASP backend. This golden is the witness that they are not any more, and that
all three executors answer the same.

## What it declares

One day kind, `springtijdag`: a day whose day-of-month is 1 or 15 — a
**monthly** recurrence, deliberately, so that the step form crosses month
ends and the count form spans several occurrences without needing a long
walk. The definition is a top-level `TEN_MINSTE 1` composite over two
`DAG_UIT` comparisons on `de dag`, which is the §9.8 shape of the source's
own Christmas-day example with the year dropped.

`springtijdagen` is the declared unit of `bergingsbestek`, `bergingsspanne`
and `bergingsafstand`, and the `plural` of the day kind
(`{ "node": "DayKind", "name": "springtijdag", "plural": "springtijdagen" }`),
so the spelling reaches evaluation through the day kind that carries it — the
spelling `jutterswet-schouwrooster` carries and no consumer there reads. Until
L3 of the v1 wire migration that plural stood as a key of a separate day-kind
registry (the L3 card, `backlog/done/20260924-l3-typesysteem.md`).

Four rules, one per corner of `RUNTIME-SEMANTICS.md` ch. 3.8's day-kind
section:

| attribute | form | what it pins |
|---|---|---|
| `uiterste bergingsdatum` | `meldingsdatum plus bergingsbestek` | §6.11 forward |
| `vroegste meldingsdatum` | `meldingsdatum min bergingsbestek` | §6.11 backward — the direction the source leaves unwritten |
| `bergingsspanne` | `tijdsduur van meldingsdatum tot opleverdatum` | §6.10 signed |
| `bergingsafstand` | `absolute tijdsduur van opleverdatum tot meldingsdatum` | §6.10 absolute |

## The pins, hand-computed

A springtijdag is the 1st or the 15th. `case1`:

| | meldings­datum | bestek | oplever­datum | uiterste | vroegste | spanne | afstand |
|---|---|---|---|---|---|---|---|
| P1 | 2026-03-10 | 3 | 2026-04-20 | **2026-04-15** | **2026-02-01** | **3** | **3** |
| P2 | 2026-03-15 | 1 | 2026-03-15 | **2026-04-01** | **2026-03-01** | **0** | **0** |
| P3 | 2026-03-20 | 0 | 2026-03-01 | **2026-03-20** | **2026-03-20** | **−1** | **1** |

- **P1 forward**: from 11 March the matching days are 15 March (1), 1 April
  (2), 15 April (3). **Backward**: from 9 March, 1 March (1), 15 February
  (2), 1 February (3). The count runs over (10 March, 20 April] — 15 March,
  1 April, 15 April — which is 3; the absolute variant of the reversed pair
  reads the same 3.
- **P2 is the "A itself does not count" row.** The melding falls *on* a
  springtijdag and the step still skips it: 15 March is not the answer,
  1 April is. Backward likewise: 1 March, not 15 March. And the count over
  (15 March, 15 March] is empty, so the equal-dates row reads 0 — not 1,
  which is what a closed interval would give.
- **P3 is the zero row and the reversed row.** `+ 0` and `− 0` both return
  20 March unchanged, although 20 March is **not** a springtijdag: the
  source's general zero rule wins over its "the result is always such a
  day" sentence. The span runs backwards — 20 March to 1 March — so the
  count is **−1** (15 March, negated by operand order) while the absolute
  variant of the mirrored phrase reads **1**.

`case2` is the leeg corner (ch. 1.2, unchanged by the day kind): P4 supplies
only a meldingsdatum, so the step **coalesces its leeg count to 0** and both
dates stand at 2026-03-10 while both counts read leeg; P5 supplies no
meldingsdatum, so all four attributes propagate leeg. A leeg tijdsduur
operand is leeg, never 0 — the rule that separates the two columns.

## The square

`ref`, the ASP backend and **both** Drools branches replay both cases on
exactly these values (gate 5, gate 6, gate 12 exec and `--drl`). The Drools
backend reaches them through the ported production runtime, which had carried
both duration forms all along with nothing exercising them — measured
2026-09-19: before this golden, `rsDatumPlusDagsoort` and
`rsTijdsduurDagsoort` were referenced by the two transpilers and by no test,
no script and no corpus document in any zone.

## What this golden deliberately leaves to the red zone and the suites

- **The reach fence.** Nothing here walks far; the day kind no calendar day
  satisfies is `corpus/rood/dagsoort-onvervulbare-wandeling`.
- **`de dag` outside its binding** is `corpus/rood/contextwaarde-de-dag-buiten-dagsoort`.
- **The §9.8 composition on a date other than the bound day** is
  `corpus/rood/dagsoortverwijzing-andere-dag`.
- **The other composite quantors and the §9.8 nested reference on the bound
  day** are in the two executors' own suites (`ref/test`,
  `backends/clingo/test`), where the eight arms were measured against each
  other on hand-computed dates.
- **Datum-tijd operands** are golden `jutterswet-getijklok`'s: the §6.11 step
  is carried there (the day kind judged on the civil day part, the clock time
  preserved), while the §6.10 count over two instants stays refused by name
  in both OCaml executors.
