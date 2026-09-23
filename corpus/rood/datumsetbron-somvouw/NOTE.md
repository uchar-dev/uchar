# datumsetbron-somvouw

The additive fold over a DATE-valued set source, as a red case. The
document is `golden/jutterswet-vondstvenster` with one change — `EERSTE`
becomes `SOM` — and everything else is untouched: the same element type,
the same source role, the same `Datum in dagen` on both the member and
the target. The verifier passes it, so the refusal has to come from the
executors.

The reason is arithmetic and not a capacity. A date is a calendar
POSITION; the sum of two positions is not a position, and neither
`RUNTIME-SEMANTICS.md` nor the normative RegelSpraak specification gives
the addition a meaning over dates. `ref` says so at the addition itself
— `plus: expected a number` — and that is this case's arbiter reading.

What the case guards against is a DEFINITE WRONG NUMBER, which is worse
than a fault and is exactly what stood here. Measured 2026-09-19 and
remeasured on this card's head: on this shape with members 2024-01-15 and
2024-03-01, the ASP backend answered **39520** — the sum of the day
numbers 19737 and 19783 — under a verifier-clean document, because the
day number is the only non-numeric encoding its scale table hands a scale
at all. The translation now declines the form by name, before it grounds
anything.

Its ordering siblings are NOT refused and must not be: `#min`/`#max`
return one contributed value, so a day number goes in and the same day
number comes out. `golden/jutterswet-vondstbereik` pins that half with
absolute dates, and `golden/jutterswet-vondstvenster` pins `EERSTE` and
`LAATSTE`, which are the same ordering fold under another name.

**The two members carry the SAME date on purpose.** The pinned `ref`
fragment runs to the value (`got 2024-01-15`), and `add` reads its two
operands in an order this case must not depend on — with two different
dates the tail would be whichever operand the compiler evaluates first.
Equal members make the tail the same whichever it is, and nothing else
about the case changes: two filled contributors still reach the fold.

The Drools backend refuses the form in both branches, one phase earlier:
the fold lowers to a `BigDecimal` accumulate and a date-valued member has
no place in it. That refusal is listed with its reason in gate 8's
exclusion list rather than fragment-pinned: a transpiler refusal is an
EXCLUDE, and that leg pins a fragment only on a FAULT.
