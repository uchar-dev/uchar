# jutterswet-sluitstip — the emitter's millisecond-precision date literal on the wire

The first `bron.rgs` golden that carries a date literal at millisecond
precision. Until
2026-09-22 the RegelSpraak emitter wrote the day-first source snippet into
a datum-tijd literal's `waarde`, and WF-095 admitted it as a second
spelling for that reason. The emitter now writes the canonical ISO lexeme
into the literal's value — since L4 the `dateTime` variant of `value` — and
keeps the snippet in `source`, as it already did for a day-precision date
(WF-084), and WF-095 admits the ISO lexeme alone. This
golden holds that route: gate 10 re-emits it from `bron.rgs`
byte-identically, gate 3 holds it WF-095-clean, and gates 5, 6 and 12
replay it on the values below.

## Three spellings, one instant

The source writes the depot's closing instant three ways, and every one
emits the same lexeme `2026-07-01T08:30:00.000`:

| rule | source spelling (`source.snippet`) | `value.dateTime` |
|---|---|---|
| `Sluitstip` (a write) | `dd. 1-7-2026 08:30:00.000` | `2026-07-01T08:30:00.000` |
| `Kenmerktoekenning tijdig` (`earlier`) | `01-07-2026 08:30:00.000` | `2026-07-01T08:30:00.000` |
| `Kenmerktoekenning verlaat` (`later`) | `1-07-2026 08:30:00.000` | `2026-07-01T08:30:00.000` |

The `dd. ` prefix and the one-digit day and month belong to the source
token (§13.2.19/.21) and never to the lexeme. The day comes first in the
source, so `1-7-2026` is the first of July; a reader that took it
month-first would put the threshold on 7 January. Gate 10's byte
comparison would catch such a misreading in the emitter first; among the
cases V7 is the threshold witness, sitting exactly on the misread instant.

## The hand calculation

Whole milliseconds since `1970-01-01T00:00:00`; the ASP translation
carries the same instant as the pair `dt(D, MS)`, civil day beside
ms-of-day. The civil day of 2026-01-01 is 20 454 (56 × 365 + 14 leap
days); 2026-07-01 adds 31 + 28 + 31 + 30 + 31 + 30 = 181, so **20 635**.
08:30:00.000 is 8 × 3 600 000 + 30 × 60 000 = **30 600 000** ms.

| lexeme | D | MS | absolute ms |
|---|---|---|---|
| `2026-07-01T08:30:00.000` (the literal) | 20 635 | 30 600 000 | 1 782 894 600 000 |
| `2026-07-01T08:29:59.999` (V1) | 20 635 | 30 599 999 | 1 782 894 599 999 |
| `2026-07-01T08:30:00.001` (V2) | 20 635 | 30 600 001 | 1 782 894 600 001 |
| `2026-07-01T08:30:00.000` (V3) | 20 635 | 30 600 000 | 1 782 894 600 000 |
| `2026-06-30T23:59:59.999` (V5) | 20 634 | 86 399 999 | 1 782 863 999 999 |
| `2026-07-02T00:00:00.000` (V6) | 20 636 | 0 | 1 782 950 400 000 |
| `2026-01-07T08:30:00.000` (V7) | 20 460 | 30 600 000 | 1 767 774 600 000 |

V4 carries no `afgiftestip`.

| label | vs the literal | `tijdig` (`earlier`) | `verlaat` (`later`) | `sluitstip` |
|---|---|---|---|---|
| V1 | same day, MS one lower | present | absent | the literal |
| V2 | same day, MS one higher | absent | present | the literal |
| V3 | same instant | absent | absent | the literal |
| V4 | leeg | absent | absent | the literal |
| V5 | day 20 634 < 20 635 (MS higher) | present | absent | the literal |
| V6 | day 20 636 > 20 635 (MS lower) | absent | present | the literal |
| V7 | day 20 460 < 20 635, same MS | present | absent | the literal |

V3 pins that both orderings are strict; V4 that an empty operand makes
both comparisons false (ch. 1.2d) while the unconditional write still
lands. V7 sits exactly on the instant a month-first reading of the source
would give, so under that misreading it would be neither earlier nor
later and `tijdig` would go absent. It is not the only case that
separates the two readings — under the misread threshold V1, V3 and V5
move too — but it is the one that lands on it exactly.

Case 1 carries V1–V4, case 2 carries V5–V7.

Own-authored Jutterswet material; `ir.json` is the frontend's output of
`bron.rgs`, not hand-written.
