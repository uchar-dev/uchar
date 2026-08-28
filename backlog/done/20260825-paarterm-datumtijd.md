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

---

Result (2026-08-28, landed on `card/paarterm-datumtijd`, NOT merged):

The pair term `dt(D, MS)` lands. `backends/clingo/lib/translate.ml`:
`dt_term` lowers a datum-tijd expression to `(atoms, D, MS)` — an
own-subject read (`val(l,a,dt(D,MS))`) and datetime ± duration;
`datetime_plus_subday` (linear ms + carry via the floormod re-split) and
`datetime_plus_calendar` (the civil-day step through the shared `dpm`
helper, ms-of-day reattached) are the two arms; the tijdsduur handler
gained a datetime branch — sub-day on the bounded ms difference `T`,
day-based via the borrow-adjusted `DB' = DB + borrow` WITHOUT forming `T`
(so a month/year span stays unbounded). Range fences as `viol/2`
(ch. 3.8 out-of-range). Case input emits the pair; a datum-tijd computed
target writes it; `model.ml` compares the dt term from the one-split
source of truth. Verified: no absolute ms value (>= 1e11) appears in the
emitted ASP.

Golden `corpus/golden/jutterswet-getijdenpaar` (own-authored Jutterswet,
hand-computed): case1-borghoek pins the borrow corner — tijdsduur in hele
uren = 34, hele dagen = 1 — beside hersteltijd = aankomst + 26 uur =
2024-03-01T01:30:00 (sub-day carry across the leap day) and ijkdatum =
aankomst + 1 maand = 2024-03-28T23:30:00 (calendar, ms-of-day preserved);
case2-negatief the mirrored −34 / −1. Both refute noop; verifier-clean.

Red-first: the repro faulted `datum-tijd tijdsduur is not translated (day
precision only)` before; after, clingo agrees with ref (gate 6 PASS). The
card's quoted "unscalable datatype" fault was the datetime-attribute-read
path; the tijdsduur dispatch faulted first — both are swept. The stale
"the datum-tijd layer is not executed" string is gone; the partial "day
precision only" messages were reworded for what genuinely stays loud.
S6a gained the pair-term paragraph + witness; S19 records the lift and
narrows the loud flank; S19 stays highest.

Gates: ALL TWELVE green (the card text said nine — stale). 1 self-cont,
2 ir-schema (144, golden round-trips), 3 verifier (144 clean), 4 case-schema
(367), 5 oracle (345), 6 clingo (345), 7 noop (312+33), 8 rood (22),
9 catala (169 checks), 10 regelspraak (130 tests), 11 toka (30 cases,
clingo 16 PASS/14 loud fault, zero silent divergences), 12 drools
(played 46).

Extra scope, reported honestly:
- The card predicted gate 12 auto-EXCLUDES the datetime golden (played
  stays 45). It does NOT: the Drools exec-model carries datetime tijdsduur
  (ChronoUnit.between) and datetime ± duration (rsDatumReken), so
  `jutterswet-getijdenpaar` PLAYS and matches — played 45 → 46, the full
  ref ≡ clingo ≡ Drools square (stronger than an exclusion). No toolchain
  change; the count is honestly 46.
- `toka/cases/parity.manifest` (outside "May touch", forced by gate 11):
  clingo now translates two datum-tijd toka slices differently — toka-25
  (vertrek + duur) is now clingo-PASS (matches ref), toka-22 (a datum-tijd
  eerste fold, a loud flank) keeps a loud fault with its new fragment. Two
  pins updated; zero silent divergences.

Leftover for the maintainer: RUNTIME-SEMANTICS ch. 3.8 (protected, NOT in
"May touch") still reads "Executed in ref only" / "the ASP backend keeps
its loud fault under the datum-tijd embargo … no datum-tijd corpus golden
lands until a pair-term encoding exists" — now stale on both counts. It
needs a follow-up edit to record that the ASP backend executes the
millisecond layer since this card and `jutterswet-getijdenpaar` pins it.
