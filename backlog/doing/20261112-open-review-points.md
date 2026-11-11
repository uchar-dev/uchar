# The open points of the reviews of 2026-11-11

Goal: the points the card `review-findings-2026-11-11` left open are fixed with a witness or struck with the reason
(its Result, "not done here", and the Open line of the empty dividend).

- ref (and the other executors as measured): the division per period still evaluates the divisor timeline over all
  periods, also in a period where the dividend is leeg (RS ch. 1.4 point 4: an empty dividend does not read the
  divisor). Measure first in ref, clingo and Drools; a fault where the divisor faults only in such a period is the
  witness.
- frontend: four resolver gaps that surface as RGS-394 (an undeclared attribute in a `Daarbij geldt` variable; an
  allocation without a unique relation after a subtype; a granularity key without a member; a version suffix on a
  form with one version) get a diagnostic of their own; a rule writing a whole dimensioned attribute gets a frontend
  diagnostic (WF-125) instead of reaching the verifier.

Acceptance: every item fixed with its witness or struck with the reason, one line each in the Result; `make gates`
green.

May touch: `ref/`, `backends/`, `frontends/regelspraak/`, corpus witnesses outside `corpus/golden/anchor`, this card.
Protected (the main session lands them): `spec/`, `scripts/`.

The toolchain `Verbaliser` points (`readRaw`, `GEEN`, "-0,00") are fixed in the toolchain itself.
