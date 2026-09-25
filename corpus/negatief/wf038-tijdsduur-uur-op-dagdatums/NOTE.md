Violates WF-038 (`spec/IR-SPEC.md` ch. 4.3, the duration clause): a
sub-day counted unit demands millisecond-precision operands; here a
`duration` between two day-precision dates counts in `uur`. Broken from
`corpus/golden/anchor/jutterswet-jutleeftijd`: the rule `Berekening
jutleeftijd` counts in `uur` instead of `jr`, and the target attribute
`Jutter.jutleeftijd` is declared in `uur` as well, so that no other rule
(WF-023's unit clause) reports the pair.

    WF-038 FAIL /rules/rules/0/versions/0/value: sub-day duration unit on
    day-precision operands (millisecond precision required)

The sentence is spelled once, `duration_sub_day_on_day_dates` in
`spec/verifier/lib/checks.ml`, and names the builtin's own kind; `ref` and
the ASP backend speak it too — measured 2026-09-25 on this document:
`EVALUATION FAULT: sub-day duration unit on day-precision operands
(millisecond precision required)` and `TRANSLATION FAULT:` with the same
sentence. Until then `ref` said `sub-day duration needs datum-tijd
operands` and the ASP backend the same with `(a day-precision date has no
time-of-day)` behind it. `expect.txt` pins the arm.
