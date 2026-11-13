# A scenario for the folds over member timelines

Goal: `jutterswet-tempovouw` and `jutterswet-tempototaal`, the goldens whose rules fold over the timelines of members,
carry a `scenario.rgs` beside their `source.rgs`, so the fold can be run and explained with `bin/regelspraak scenario
--explanation` and in the playground. No golden with a scenario has such a fold today.

- Each scenario regenerates the golden's pinned case files exactly (gate 16: same names, none missing or extra,
  byte-identical), and the `ir.json` stays byte-identical. Where the pinned `case.json` cannot be written by a scenario
  as it stands (a form the scenario layer does not carry), the scenario is a case file of its own beside it, with the
  same situation and hand-checked expectations, and the reason is written in the golden's `NOTE.md`.
- A scenario layer gap found on the way is fixed in the frontend with a witness if it is small, or recorded with the
  measurement.

Acceptance: both goldens carry a scenario; gates 10, 16, 5, 6 and 12 green; `make gates` green.

May touch: `corpus/golden/jutterswet-tempovouw`, `corpus/golden/jutterswet-tempototaal`, `frontends/regelspraak/`,
this card. Protected (the main session lands them): `spec/`, `scripts/`.
