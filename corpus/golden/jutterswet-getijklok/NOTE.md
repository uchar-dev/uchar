# jutterswet-getijklok — the day-kind step on a datum-tijd operand

The differential witness of the maintainer decision of 2026-09-19 that
**splits** the datum-tijd cross (`RUNTIME-SEMANTICS.md` ch. 3.8): the §6.11
STEP is carried over millisecond-precision operands, while the §6.10 count
between two instants is not. This golden pins the carried half in all four
consumers.

## The rule the decision states

> The day kind is judged **solely on the calendar-date part**; after the Nth
> matching day is found the **original local clock time stays unchanged**.
> Zero, leeg, negative and unsatisfiable day kinds follow the same rules as
> `datum ± N <dagsoort>`.

So there is nothing new in the *day-kind* arithmetic here — the point of the
golden is the clock time, and every case is chosen so that a backend which
dropped, floored or re-derived it would answer differently.

## Why a golden of its own, and not two more cases on `jutterswet-bergingsklok`

Because the two documents make two different claims and share no arithmetic.
`bergingsklok` declares day-precision attributes; carrying datum-tijd there
would mean re-typing them and re-pinning all 22 of its hand-computed values,
and its NOTE is written around the day-precision story (the "A itself does not
count" row, the signed/absolute pair). Splitting also keeps the channel
bookkeeping honest: this golden moves gate 12's two pins by one each, on its
own reason, instead of hiding inside another golden's row.

## The pins, hand-computed

Day kind `springtijdag` = a day whose day-of-month is 1 or 15 — the same
monthly recurrence `bergingsklok` uses, so the day arithmetic is checkable by
eye and the clock time is the only new variable.

`case1`:

| | meldtijdstip | termijn | sluittijdstip (plus) | openingstijdstip (min) |
|---|---|---|---|---|
| G1 | 2026-03-10T09:15:30 | 3 | **2026-04-15T09:15:30** | **2026-02-01T09:15:30** |
| G2 | 2026-03-15T23:59:59 | 1 | **2026-04-01T23:59:59** | **2026-03-01T23:59:59** |
| G3 | 2026-03-20T00:00:00 | 0 | **2026-03-20T00:00:00** | **2026-03-20T00:00:00** |

- **G1** is the plain forward and backward walk on the day part: from 11 March
  the matching days are 15 March, 1 April, 15 April; from 9 March backwards,
  1 March, 15 February, 1 February. 09:15:30 rides through both.
- **G2 is the row that catches a re-derived clock, and it is the MIN half
  that does the catching.** The melding falls *on* a springtijdag at
  **23:59:59**, one second before midnight. Forward the start does not count,
  so the step lands on 1 April, at 23:59:59 and not at 00:00:00. Backward is
  the discriminating direction: an implementation that rounded the instant UP
  to the next day boundary before stepping would start its walk from 16 March
  and land on **15 March**, itself a springtijdag — where judging the civil
  day part alone gives **1 March**. One second of clock time separates the
  two answers by a fortnight, which is why this row carries 23:59:59 and not
  a mid-day time.
- **G3** is the zero row at **midnight**: `+ 0` and `− 0` both return
  2026-03-20T00:00:00 unchanged, although 20 March is *not* a springtijdag.
  Midnight is the corner where a dropped time-of-day is invisible, which is
  why the other two rows carry non-zero clocks.

`case2` is leeg, and under this decision the leeg arms are **correct
behaviour, not a residue**: G4 supplies only a meldtijdstip, so the leeg count
coalesces to 0 and both results are the meldtijdstip **with its clock time**;
G5 supplies no meldtijdstip, so both results propagate leeg.

## The square

`ref`, the ASP backend and **both** Drools branches replay both cases on
exactly these values. The ASP backend reaches them through the pair term
`dt(day, ms-of-day)`: the day part is looked up in the day kind's extension
table and the ms-of-day rides through untouched, which is the encoding-level
form of the same sentence. The Drools backend was already doing this — its
`rsDatumTijdPlusDagsoort` steps the date part and re-attaches the time — and
this golden is what pins that, together with a unit test on the runtime static
itself; before 2026-09-19 nothing exercised either.
