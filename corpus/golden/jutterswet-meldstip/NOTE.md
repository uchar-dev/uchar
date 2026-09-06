# jutterswet-meldstip — the first `DatumTijd` coverage of the differential

`DatumTijd` is a declared datatype of the wire, and until this golden **no
executor differential touched it**. The corpus carried `dateTime` values on
the case wire that were only ever read in, never compared: **zero**
`DatumTijd` comparisons and **zero** `DatumTijd` literals. One executor is
not a differential — any `DatumTijd` semantics that shifted would have
shifted unseen.

This golden closes that. It pins one `DatumTijd` attribute against one
`DatumTijd` **literal** in the canonical ISO lexeme, in both ordering
directions, at a **sub-day threshold** — and it is replayed by gate 5
(`ref`), gate 6 (clingo) and gate 12 (Drools) against the same
hand-computed values.

## Why the threshold is not midnight

A midnight threshold would hide exactly the fault the previous card
exposed: a reader that loses the time of day still answers correctly when
every instant sits on a day boundary. The threshold here is
`2026-03-15T12:00:00.000`, and the case values straddle it by **one
millisecond** in both directions, sit **exactly on** it, and straddle the
**civil day** either side — so neither the ms-of-day alone nor the civil
day alone can answer. A reader that dropped the time would put `A1` and
`A2` (a millisecond apart) on the same side; a reader that compared only
the time of day would invert `A6` and `A7`.

## The hand calculation

The millisecond layer counts whole milliseconds since
`1970-01-01T00:00:00`; the ASP translation carries the same instant as the
pair `dt(D, MS)` — the civil day beside the ms-of-day.

The civil day of 2026-03-15: 1970→2026 is 56 years × 365 = 20 440 days,
plus 14 leap days (1972, 1976, …, 2024) = **20 454** for 2026-01-01; plus
31 (January) + 28 (February 2026 — not a leap year) + 14 = **20 527**.

| lexeme | civil day D | ms-of-day MS | absolute ms |
|---|---|---|---|
| `2026-03-15T12:00:00.000` (the literal) | 20 527 | 43 200 000 | 1 773 576 000 000 |
| `2026-03-15T11:59:59.999` (A1) | 20 527 | 43 199 999 | 1 773 575 999 999 |
| `2026-03-15T12:00:00.001` (A2) | 20 527 | 43 200 001 | 1 773 576 000 001 |
| `2026-03-15T09:30:00.000` (A5) | 20 527 | 34 200 000 | 1 773 567 000 000 |
| `2026-03-14T23:59:59.999` (A6) | 20 526 | 86 399 999 | 1 773 532 799 999 |
| `2026-03-16T00:00:00.000` (A7) | 20 528 | 0 | 1 773 619 200 000 |

`MS` of 12:00:00.000 is 12 × 3 600 000 = 43 200 000; of 23:59:59.999 it is
23 × 3 600 000 + 59 × 60 000 + 59 × 1 000 + 999 = 86 399 999. The absolute
ms is `D × 86 400 000 + MS`.

The ordering is lexicographic on the pair — the civil day decides, the
ms-of-day breaks the tie:

| label | vs the literal | `tijdig` (EERDER) | `verlaat` (LATER) |
|---|---|---|---|
| A1 | same day, MS one lower | present | absent |
| A2 | same day, MS one higher | absent | present |
| A3 | same day, same MS | absent | absent |
| A4 | leeg (no `meldstip`) | absent | absent |
| A5 | same day, MS 9 000 000 lower | present | absent |
| A6 | day 20 526 < 20 527 (MS *higher*) | present | absent |
| A7 | day 20 528 > 20 527 (MS *lower*) | absent | present |

A3 pins that both orderings are **strict**: the instant exactly on the
threshold is neither earlier nor later. A4 pins the empty operand: no
`meldstip` makes both comparisons false (the ch. 1.2d date row — any empty
operand is false), so silence, not a fault.

## Why the wire is hand-written (no `bron.rgs`)

The RegelSpraak emitter cannot yet write this document. For a `DatumTijd`
literal it writes the day-first source snippet into `waarde` **and** into
`bron` (measured 2026-09-06: `15-03-2026 12:00:00.000` in both), so unlike
a day-precision date — ISO in `waarde`, the snippet in `bron` — the
datetime literal has no canonical/source split on the wire at all. The
emitter side is its own card (it needs a re-pin of the emitter goldens).
Until then this golden is hand-written wire, the pattern the Catala and
`jutterswet-getijdenpaar` goldens already follow, and it carries the
CANONICAL lexeme deliberately: that is the form the wire's own rule
(WF-095) names first and the one a consumer reads.

Both lexemes are admitted by WF-095 and by every reader, and they name the
same instant: `15-03-2026 12:00:00.000` in this document's literal replays
identically (measured). The canonical one is written here because the wire
rule exists to make the canonical form the one a new producer targets.
