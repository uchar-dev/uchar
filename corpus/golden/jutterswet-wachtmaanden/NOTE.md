# jutterswet-wachtmaanden

The witness for the whole-period qualifier in the condition of a rule with
a timeline target (`RUNTIME-SEMANTICS.md` ch. 3.9, per bucket; owner's
decision 2026-11-03, `spec/proposals/IMPLICIT-TIME.md` point 4): there the
qualifier is a truth per bucket of its own grid, and the rule writes in
the buckets where it holds. On a scalar target the qualifier still folds
to one verdict (`synth-gehelejaarkenmerk`, `jutterswet-ploegpremie`).

A `Jutter` has the daily characteristic `wachtplichtig` and a monthly
`wachtloon`. Three rules on monthly targets:

| target | value | condition |
|---|---|---|
| `toeslagdagen` | het aantal dagen in de maand dat hij wachtplichtig is | hij niet gedurende de gehele maand wachtplichtig is |
| `wachtpremie` | 50 | hij gedurende de gehele maand wachtplichtig is |
| `jaarpremie` | 20 | zijn wachtloon gedurende het gehele jaar groter is dan 10 |

"Niet gedurende de gehele maand" holds in every month that is not whole,
also in a month without a single wachtplichtig day; there the day count
writes 0 (ch. 3.8; IMPLICIT-TIME 7a), over all time: before the first month
with a wachtplichtig day and after the last as one open segment each. The
expectations were computed by hand.

## What each case pins

- `case1-wacht-over-de-jaarwisseling`: wachtplichtig 2025-11-16 up to
  2026-03-13, the whole months December 2025, January and February 2026.
  toeslagdagen 0 until 2025-11-01, 15 in November, 12 in March and 0 from
  2026-04-01; wachtpremie 50 on
  [2025-12-01, 2026-03-01), one segment. wachtloon 15 on
  [2025-01-01, 2026-07-01): every day of 2025 has 15 > 10 and 2026 has
  gap days, so jaarpremie is 20 on 2025 only. The fold of a scalar target
  would have read the qualifier once, false for this timeline, and so
  written all five months of the count.
- `case2-schrikkeljaar`: wachtplichtig 2028-02-11 up to 2028-04-05, the
  whole month March 2028. toeslagdagen 0 until 2028-02-01, 19 in February
  (2028 is a leap year), 4 in April and 0 from 2028-05-01; wachtpremie 50
  in March. No wachtloon, so every
  day of every year is a gap day, no ordering holds, and no jaarpremie.
- `case3-geen-wacht`: no wachtplichtig periods: no whole month, and
  toeslagdagen 0 on all time; wachtloon 30 on 2024, so jaarpremie 20 on
  2024.
