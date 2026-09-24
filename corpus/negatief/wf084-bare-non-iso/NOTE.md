# Violates WF-084 — bare day-first date lexeme, not ISO

The Gelijkstelling assigns the date literal `{"date": "31-01-2024"}`: the
Dutch day-first source form (`dd-mm-yyyy`), not the ISO `yyyy-mm-dd` the
executors parse. Because the executors read the lexeme as ISO, this was
silently misread as year 31, month 1, day 2024 — which rolls over to
`0036-07-16`, a differential-blind wrong answer that both ref and the ASP
backend agreed on. WF-084 makes the document ill-formed at verification time,
ahead of any executor.

Minimal own-authored document (Jutterswet vocabulary); the well-formed
counterpart is the ISO date literal in
`corpus/golden/jutterswet-vondstverjaring/ir.json`.

Since L4 the literal's value is typed (delta B22): the ill-formed string sits
in its `date` variant, which the rule reads, and the report cites `/value`.
`expect.txt` pins the arm.
