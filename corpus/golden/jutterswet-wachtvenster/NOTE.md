# jutterswet-wachtvenster — the time window inside an `all` quantifier fold

The **period condition** (`RUNTIME-SEMANTICS.md` ch. 3.10, wire `TimeWindow`)
at the three positions a quantifier fold and an aggregation give it, each
read per member with the MEMBER as the window's subject:

```
Een Schouwploeg is paraat   indien alle wachten van de Schouwploeg
                                   vanaf hun aanstelling in dienst zijn.
Een Schouwploeg is bevoegd  indien alle wachten van de Schouwploeg die
                                   vanaf hun aanstelling in dienst zijn,
                                   gediplomeerd zijn.
Het dienstwachttal van een Schouwploeg moet berekend worden als het
                                   aantal wachten van de Schouwploeg die
                                   vanaf hun aanstelling in dienst zijn.
Het wachttal van een Schouwploeg moet berekend worden als het aantal
                                   wachten van de Schouwploeg.
```

| rule | wire position of the window |
|---|---|
| `paraat` | the `test` of an `all` `QuantifierFold` (ch. 3.9a) |
| `bevoegd` | the `filter` of an `all` `QuantifierFold`; the `test` is `gediplomeerd` |
| `dienstwachttal` | the die/dat `filter` of a `count` (ch. 3.4a) |
| `wachttal` | none — the control, the unfiltered count of the same range |

The bound is `vanaf de aanstelling van de wacht`: the member's own scalar
date attribute, one of the two bound shapes WF-111 admits, and the window
sits only under `all`, the position WF-112 admits. The window is open
above.

## Why this document exists

Until 2026-09-23 the Drools exec-model branch refused all three positions
("een conditie van soort Periode", "een die/dat-subselectie die deze tak
niet op de source kan binden", "aggregatie buiten Phase-C scope"), while
the verifier, `ref`, the ASP backend and the DRL-text branch carried them.
Card `drools-venster-vouwtoets` made the exec branch carry them; this
golden is what gate 12 counts for it. Measured on this document with the
Drools jar of the card's base, before the change:

    exec: EXCLUDE transpiler SKIP: regel: paraat bij aanstelling — kwantorvouw ALLE over Strandwacht (een niet-temporele matrix die de per-subject bladvertaling niet draagt: een conditie van soort Periode): …
    drl:  PLAYED 3

and after it both branches report `PLAYED 3`.

## Hand-written IR: there is no `source.rgs`

The RegelSpraak frontend emits none of the three positions. Measured
2026-09-23 on this repository's frontend (`make ir`), with the sentences
above in `source.rgs` form:

- a window as the quantified predicate ("indien alle wachten van de
  Schouwploeg aan de volgende voorwaarde voldoen: het is de periode vanaf de
  aanstelling van de wacht") is a syntax error: `mismatched input 'het'
  expecting {'..', BULLET, '-'}`;
- a window in the die/dat subselection of the fold is refused ("de
  'die/dat'-deelselectie … wordt niet omgezet en zou stil wegvallen");
- a window in the subselection of the count becomes an `Unresolved` marker
  ("'het is de periode' blijft als marker op de wire staan").

So the document is written against the schema, as `jutterswet-schouwploeg`
was before its source arrived, and gate 10 has nothing to re-emit.

## The instances

One population in all three cases; only the evaluation date moves. Each
Schouwploeg carries an `aanstelling` of its OWN, with the same name as the
member's, on the other side of the evaluation date from what its members
need — so a reading that took the window's bound from the fold's subject
instead of from the member compiles and gives another answer (see the last
table).

| ploeg (own aanstelling) | wacht | aanstelling | gediplomeerd |
|---|---|---|---|
| S1 (2025-06-01) | W1 | 2023-01-01 | ja |
| | W2 | 2024-06-15 | nee |
| S2 (2020-01-01) | W3 | 2024-09-01 | nee |
| | W4 | 2023-01-01 | ja |
| S3 (none) | W5 | none | nee |
| | W6 | 2023-01-01 | ja |
| S4 (2020-01-01) | — | | |

## The three cases, computed by hand before any executor ran

A wacht is **in dienst** on day D iff its own aanstelling is filled and on
or before D: `from` is inclusive, the window is open above, a leeg bound
never matches, and an unset evaluation date never matches (ch. 3.10).

**`case1-medio-2024`, D = 2024-06-15.** In dienst: W1, W2 (on D itself),
W4, W6. Not: W3 (after D), W5 (no aanstelling).

| ploeg | in dienst | paraat (`all` in dienst) | bevoegd (`all` in-dienst gediplomeerd) | dienstwachttal | wachttal |
|---|---|---|---|---|---|
| S1 | W1, W2 | **ja** | nee (W2) | 2 | 2 |
| S2 | W4 | nee (W3) | **ja** | 1 | 2 |
| S3 | W6 | nee (W5) | **ja** | 1 | 2 |
| S4 | — | **ja** (vacuous) | **ja** (vacuous) | 0 | 0 |

**`case2-begin-2025`, D = 2025-01-01.** W3 is in dienst now as well; W5
never is.

| ploeg | in dienst | paraat | bevoegd | dienstwachttal | wachttal |
|---|---|---|---|---|---|
| S1 | W1, W2 | **ja** | nee (W2) | 2 | 2 |
| S2 | W3, W4 | **ja** | nee (W3) | 2 | 2 |
| S3 | W6 | nee (W5) | **ja** | 1 | 2 |
| S4 | — | **ja** | **ja** | 0 | 0 |

**`case3-tijdvrij`, no evaluation date.** Nobody is in dienst: `paraat`
holds only for the empty ploeg, `bevoegd` holds everywhere because the
filter leaves no contributor (the vacuous reading ch. 3.12 records for this
position), and the count is 0 beside a `wachttal` of 2.

| ploeg | paraat | bevoegd | dienstwachttal | wachttal |
|---|---|---|---|---|
| S1 | nee | **ja** | 0 | 2 |
| S2 | nee | **ja** | 0 | 2 |
| S3 | nee | **ja** | 0 | 2 |
| S4 | **ja** | **ja** | 0 | 0 |

All three cases replayed on the first run: `ref` and the ASP backend
`PASS (17 expectations)` each, both Drools branches `PLAYED 3`.

## The readings the cases exclude

Each row is a wrong reading and the first cell of `case1` where it gives
another answer.

| wrong reading | case1 answer | pinned answer |
|---|---|---|
| the bound read on the PLOEG (its own aanstelling) | S1 paraat nee, bevoegd ja, dienstwachttal 0; S2 paraat ja, bevoegd nee, dienstwachttal 2 | S1 ja, nee, 2; S2 nee, ja, 1 |
| the window ignored (always in dienst) | S2 bevoegd nee, S3 bevoegd nee, dienstwachttal = wachttal | ja, ja, 1 and 1 |
| `from` exclusive | S1 paraat nee, bevoegd ja, dienstwachttal 1 | ja, nee, 2 |
| a leeg bound read as open | S3 paraat ja, bevoegd nee, dienstwachttal 2 | nee, ja, 1 |
| the filter dropped from the fold | S2 and S3 bevoegd nee | ja |
| an unset evaluation date read as a match | `case3` S1 and S2 paraat ja, dienstwachttal 2, 2, 1 for S1–S3 | nee, and 0 everywhere |

`case2` adds the one cell that only the evaluation date moves: W3 enters
the window, which turns S2 `paraat` and turns its `bevoegd` off in the same
run.
