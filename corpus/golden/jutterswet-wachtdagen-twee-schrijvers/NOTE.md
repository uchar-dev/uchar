# jutterswet-wachtdagen-twee-schrijvers

The witness for two writers of one timeline target read per day
(`RUNTIME-SEMANTICS.md` ch. 5.7; `spec/proposals/IMPLICIT-TIME.md` point
5): each writer decides the days of its own segments, and together they
fill the timeline; two that decide one day with different values are a
fault (red case `corpus/red/jutterswet-wachtdagen-botsing`).

A `Jutter` has the daily characteristic `wachtplichtig` and the monthly
`wachtdagen`, written by two rules: 30 gedurende de tijd dat hij gedurende
de gehele maand wachtplichtig is, and het aantal dagen in de maand dat hij
wachtplichtig is gedurende de tijd dat hij niet gedurende de gehele maand
wachtplichtig is (formulation A of the proposal's anchor casus, without
units and without the leap-day test). The two conditions are each other's
complement, so no day has two writers. Each writer's segments stand as it
wrote them. The expectations were computed by hand.

## What each case pins

- `case1-wacht-over-de-jaarwisseling`: wachtplichtig 2025-11-16 up to
  2026-03-13: 15 in November 2025, 30 on [2025-12-01, 2026-03-01) (one
  segment, the first writer's), 12 in March 2026.
- `case2-schrikkeljaar`: wachtplichtig 2028-02-11 up to 2028-04-05: 19 in
  February, 30 in March, 4 in April.
- `case3-alleen-gedeeltelijke-maanden`: wachtplichtig 2024-01-10 up to
  2024-01-20 and 2024-03-05 up to 2024-04-03: no whole month, so only the
  count writes, 10, 27 and 2.
