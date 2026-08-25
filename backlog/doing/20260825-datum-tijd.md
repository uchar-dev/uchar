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
