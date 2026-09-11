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

## The key this document does not carry

`Werf.kenmerkTijdlijnAantal` is **empty**, as every frontend-produced
document's is: the emitter writes that key only for a count other than 1
(`DefinitionResolver`), so no frontend-produced document carries it at 1.

Until 2026-09-11 that emptiness was load-bearing. Both Drools branches gated
the navigated-characteristic timeline read on the key's PRESENCE rather than
on "two or more", so adding `"bedrijvig": 1` — a wire-legal, verifier-clean,
semantically identical edit — flipped both branches from PLAYED to a refusal
and silently reduced this golden to a two-executor witness. The card
`backlog/done/20260911-kenmerktijdlijnaantal-poort.md` corrected that gate in
both branches to the multiplicity test it was meant to be (a period count
defaults to 1 — IR-SPEC §2.2/ch. 2.10 — so a raster starts at 2); it was found
because the presence gate mismeasured this very clause first.

The key stays out of the committed IR all the same: this document is shaped
like what the emitter produces, and a golden pins semantics rather than a
backend's gate. That the same document with `"bedrijvig": 1` plays all four
cases in BOTH branches is pinned in the backend's own suite
(`MeerPeriodiekePoortWitnessTest`), which builds it in memory and never
writes here.

One direction is still live, and it is the other one: a count of **two or
more** is a §13.3.6.7 multi-period raster whose producer semantics neither
Drools branch carries, so growing the key to 2 drops this golden out of the
played channel exactly as the presence gate used to do at 1. Gate 12's
`EXPECTED_PLAYED` would then fail by name, which is the correct report — the
form really is not carried.
