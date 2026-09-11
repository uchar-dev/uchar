# synth-gehelejaarkenmerk — the §8.2 qualifier over a navigated characteristic

The corpus witness of `RUNTIME-SEMANTICS.md` ch. 3.9's
**characteristic-holds** test, and of its three vacuous passes. Hand-written
(synth family, no `bron.rgs`): the shape it pins is a kernel question about
where a timeline stops being a timeline, not a surface-language question.

Until 2026-09-11 no golden carried the form at all. The only `GeheelPeriode`
in the corpus sat on a *comparison*, in the verifier-only cabinet
`jutterswet-vormenkabinet`, which deliberately has no case — so the clause
had a written norm and no replayed witness, and two of the three executors
answered the opposite of it without a word.

## The shape

```
De toeslag van een Schuit is de lengte van de Schuit
  indien de werf van de Schuit gedurende het gehele jaar bedrijvig is
De korting van een Schuit is de lengte van de Schuit
  indien de werf van de Schuit niet gedurende het gehele jaar bedrijvig is
```

`bedrijvig` is a month-granular characteristic of `Werf`; the hop `werf` is
to-one. The **grid is the qualifier's** — "het gehele jaar" — not the
characteristic's declared granularity (ch. 3.9); the two are deliberately
different here so a reader that confuses them fails row d.

The two rules are the same qualifier in both directions, which is why every
case decides exactly one of the two attributes and leaves the other leeg.
That is also what makes each case refute a do-nothing executor (gate 7): a
case whose only pinned outcome were an `attributeEmpty` would survive
`--noop` and need a survivor entry; here every case pins a derived **value**.

## The four cases, by hand

Evaluation date 2026-03-15, `S1.lengte` = 8, `toeslag`/`korting` declared at
2 decimals. The year bucket is [2026-01-01, 2027-01-01) — 2026 is not a leap
year, so **365 days**.

| case | link | `bedrijvig` true-periods | touched bucket | satisfied / needed | toeslag | korting |
|---|---|---|---|---|---|---|
| case1-geen-koppeling | none | — | none (null timeline) | vacuous | **8** | leeg |
| case2-geen-tijdlijn | W1 | none | none (no segments) | vacuous | **8** | leeg |
| case3-heel-jaar | W1 | [2026-01-01, 2027-01-01) | 2026 | 365 / 365 | **8** | leeg |
| case4-half-jaar | W1 | [2026-01-01, 2026-07-01) | 2026 | 181 / 365 | leeg | **8** |

Row d's 181 = 31 + 28 + 31 + 30 + 31 + 30 (January through June 2026).

Rows a and b are the two vacuous passes ch. 3.9 states outright: "a timeline
with no segments at all touches no bucket and passes vacuously" and "a failed
to-one navigation contributes a null timeline, which passes vacuously". Row a
is the one that cannot be reached through the fold at all — it is about the
NAVIGATION — and it is where the qualifier **diverges deliberately** from the
unqualified navigated check, whose v0 asymmetry answers false for the
positive and the negated form alike (ch. 5.8). The `korting` column is
ch. 3.9's "negation complements the folded result", including "the
qualifier's negation then fails" on the two vacuous rows.

## The square

All three executors replay all four cases with these values: `ref` (gate 5),
the ASP backend (gate 6) and the Drools backend in **both** transpiler
branches (gate 12, PLAYED 4). The two OCaml executors reach the verdict by
two independent routes — an OCaml fold over the true-periods in `ref`, an ASP
derivation over the `ktl` facts and the pro-rata's bucket calendar in the
backend — so the agreement is a differential, not a shared implementation.

## One key this document must not grow

`Werf.kenmerkTijdlijnAantal` is **empty**, and that is load-bearing. The
frontend emits that key only for a count other than 1
(`DefinitionResolver`), so no frontend-produced document carries it at 1.
Both Drools branches gate the navigated-characteristic timeline read on
`containsKey` rather than on "two or more", so adding `"bedrijvig": 1` — a
wire-legal, verifier-clean, semantically identical edit — flips both branches
from PLAYED to a refusal and silently reduces this golden to a two-executor
witness. That over-rejection is recorded as its own card
(`backlog/new/20260911-kenmerktijdlijnaantal-poort.md`); it was found because
it mismeasured this very clause first.
