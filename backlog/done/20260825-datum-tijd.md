# A2 — the datum-tijd millisecond layer in ref; clingo under embargo

Goal: the remaining date-time scenario (datetime ± duration,
millisecond precision). Maintainer decision (2026-08-25, ratified):
ref implements the ms layer; clingo keeps its loud fault under an
EMBARGO — no datum-tijd corpus golden lands until a pair-term encoding
exists (registered as its own future card); ref behaviour is pinned by
worked-example unit tests instead. REQUIRES card A1 merged (the
calendar kernel and spelling table). Mirror of rsDatumReken/rsTijdsduur
on LocalDateTime: sub-day units linear on the ms value; calendar units
act on the date part preserving time-of-day; whole-unit counts carry
the borrow rule (time-of-day counting back — the classic silent-error
corner, pin it with worked examples); empty rules per ch. 1.2 (empty
duration → datetime unchanged; empty datetime → empty).

Acceptance:
- Red-first: the verifier-clean datetime repro faults in today's ref
  (quoted); after the change ref evaluates it and the worked examples
  run as tests (including the borrow corner and a negative span).
- RUNTIME-SEMANTICS ch. 3.8 gains the datetime paragraph (time-of-day
  preservation, the borrow rule, sub-day linearity); the clingo embargo
  is recorded in ASP-TRANSLATION S19 (loud fault stays; the pair-term
  encoding named as the registered future work) — S19 stays highest.
- No datum-tijd corpus golden (the embargo); all nine gates green.

May touch: `spec/RUNTIME-SEMANTICS.md` (ch. 3.8 datetime paragraph),
`ref/` (arith.ml, eval.ml, test_ref.ml worked-example tests),
`spec/ASP-TRANSLATION.md` (S19 embargo note).
Out of scope: any clingo code change, any corpus golden for datetime,
the pair-term design (future card), `spec/CONSTRUCTS.md` (serial),
`spec/IR-SPEC.md` (no WF change needed — WF-035/038 land in A1).

Result: the datum-tijd millisecond layer executes in ref — sub-day
units linear on the ms value, calendar units on the civil day part
preserving the time-of-day (floor-division split, also pre-epoch),
whole-unit tijdsduur with the borrow rule (strictly-before, mirrored
on the negative side) — the rsDatumReken/rsTijdsduur LocalDateTime
mirror. Empty rules ch. 1.2 unchanged; mixed tijdsduur precision
stays a loud fault. Red-first held: the verifier-clean repro faulted
at all three sites in the old ref (quoted in the ms-layer commit)
and replays exactly after (oracle PASS, 4 expectations, including
the borrow corner 34 uren / 1 dag). RUNTIME-SEMANTICS ch. 3.8
carries the datetime paragraph; its worked examples run verbatim in
test_ref.ml (20 new checks, 318 total) — the pin, since the embargo
admits no corpus golden. ASP-TRANSLATION S6a/S19 record the embargo:
clingo keeps its loud fault (verified on the repro: "unscalable
datatype in the ASP translation"), no datum-tijd golden until the
pair-term encoding; S19 stays highest. Deliberately left: the
pair-term encoding card itself is not cut here (outside May touch —
for the maintainer to register); sub-day extractors and
datum-met/datumtijd-met stay unexecuted, loud; clingo's internal
reason string "the datum-tijd layer is not executed" is now stale
about ref but clingo code was out of scope. All nine gates green.
