# A4 — the pair-term datum-tijd encoding in clingo lifts the embargo

Goal: the registered future work ASP-TRANSLATION S19 names for the
datum-tijd embargo (maintainer decision 2026-08-25, ratified): encode
a millisecond-precision date in ASP as a pair term — the civil day
beside the ms-of-day, `dt(D, MS)` with 0 ≤ MS < 86 400 000 — so
datetime ± duration and sub-day tijdsduur translate without a
datetime's ms value ever entering clingo's integer range whole.
Mirror ref's ms layer exactly (RUNTIME-SEMANTICS ch. 3.8): sub-day
linearity with the carry across the day boundary, calendar units on
the day part preserving the ms-of-day (the S6a day kernel), the
borrow rule on whole-unit counts, the ch. 3.8 range fault. Sweep
clingo's internal reason string "the datum-tijd layer is not
executed" (translate.ml, stale about ref since 2026-08-25) while
there. REQUIRES the datum-tijd card (A2) merged.

Acceptance:
- Red-first: the datum-tijd repro faults at translation today
  ("unscalable datatype in the ASP translation", quoted); after the
  change clingo agrees with ref on it — the differential square.
- The embargo lifts: the first datum-tijd corpus golden lands through
  both executors (the ch. 3.8 borrow corner 34 uren / 1 dag among the
  expectations); S6a and S19 record the lifted embargo; the stale
  reason string is gone. S19 stays highest.
- All nine gates green.

May touch: `backends/clingo/`, `spec/ASP-TRANSLATION.md` (S6a, S19),
`corpus/golden/`, `ref/` (only if the differential exposes a defect).
Out of scope: sub-day part extractors and datum-met/datumtijd-met
(unexecuted in both executors — their own debt),
`spec/CONSTRUCTS.md` (serial at merge).
