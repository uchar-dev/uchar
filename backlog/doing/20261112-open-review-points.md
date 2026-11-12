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

Result (2026-11-12):
- fixed: an empty dividend does not read the divisor per period either, in ref, clingo and Drools (measured: all
  three gave an inconsistency for a zero divisor in a quarter where the dividend is leeg). ref deposits only in
  periods where the dividend is filled and does not compute the divisor where the dividend is leeg on every day;
  clingo carries the periods a node is read in onto its deposits, reach fences and guards; Drools records per period
  whether the dividend is filled while computing the divisor, and both renderings compute the dividend first (the
  DRL text now writes a function per division: the pins of `jutterswet-rekenen-per-dag`,
  `jutterswet-verzuimrente-30-360-afgerond` and `anchor/jutterswet-verzuimrente-30-360` follow). Witnesses
  `WV-3.4-empty-dividend-per-period-*` (ref), "an empty dividend per period" (clingo suite),
  `EmptyDividendPerPeriodWitnessTest` (Drools, outcome and reading against ref's trace); golden
  `jutterswet-leeg-deeltal-per-periode`; `EXPECTED_PLAYED` 377 → 378. Provisional: with a dividend leeg on every
  day an unbound parameter or bucket scalar in the divisor does not fault either.
- struck: Drools' `isPeriodArithmetic` not counting a `Construct` of a literal changes no outcome: every place that
  tests the producing value tests `constructorOfLiteral` apart; the two left are refused already (the verifier,
  WF-091, and Drools under another code).
- fixed, frontend: the resolver gaps that surfaced as RGS-394 have a diagnostic at the place of the fault: an
  undeclared read or an unresolved name under "Daarbij geldt" and a characteristic read as a value there (new
  RGS-396); an allocation whose recipients name no role and no declared type (RGS-185, a new reason); versions of
  one rule that land in different lists (new RGS-395); a whole write or a bare read of a dimensioned attribute
  (RGS-364 per dimension). The granularity key without a member was an incomplete withdrawal: a withdrawn input
  marking (RGS-153) now takes its timeline entries along. A sweep cutting every golden `source.rgs` as
  SemanticTokensRecoveryTest does: RGS-394 from 29 to 0. `DependencyCycleTest.a_model_the_emitter_refuses_carries_the_refusal`
  now triggers RGS-394 by a sum over an undeclared attribute in a variable (its old trigger is now diagnosed).
- struck: an undeclared recipient attribute of an allocation stays without a frontend diagnostic: the TOKA casus
  names `treinmiles`, which only the replay slices declare, and gate 11 pins the casus' diagnostics; this is the
  boundary `grammar/DIVERGENCES.md` records ("An undeclared attribute read"); the verifier refuses the form
  (WF-034.undeclared-allocation-attribute).
- fixed, in the toolchain (its own commits, pushed): `Verbaliser.readRaw` fails with context instead of returning
  null; GEEN reads "niet A en niet B"; `nlNumber` writes "0,00" for a negative value that rounds to zero; TEN HOOGSTE,
  PRECIES and TEN MINSTE above 1 name their threshold and each read member with its own verdict.
