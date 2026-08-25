# Date-part extraction and the Easter builtin reach ref and clingo

Goal: the stress-test date-function class (2 scenarios): MAAND_UIT,
DAG_UIT and EERSTE_PAASDAG in function position fall through ref's
eval_functie catch-all (`ref/lib/eval.ml:867` "function kind not
implemented in this card") while JAAR_UIT already works
(eval.ml:858-861, arith.ml:438-441 via civil_from_days). Close the
class spec-first, narrowest form (the exploration design, verified
file:line): (1) RUNTIME-SEMANTICS ch. 3.8 gains one paragraph — month
and day extraction from a day-precision date yield the proleptic-
Gregorian civil components as unitless integers (month 1..12), leeg
propagates (ch. 1.2b); Easter Sunday of a whole unitless year per the
Gauss/Anonymous algorithm, written out normatively; worked examples
become ref tests verbatim (maand uit 2026-11-03 = 11; dag uit = 3;
Easter 2026-04-05, 2027-03-28, 2038-04-25 beside the existing 2024
pin). The calendar sentence (proleptic Gregorian, no timezones) is
part of the paragraph. (2) A WF-038 clause: day-precision extractors
MUST take a date operand — with negatief `wf038-extractor-text` (today
a text operand only faults at runtime). (3) ref: Arith.maand_uit /
dag_uit (mirrors of jaar_uit) and Arith.eerste_paasdag (Gauss, mirror
of the production algorithm; non-integer or unit-carrying year faults;
Empty propagates; result is a Date via days_from_civil), three arms
before the catch-all — which stays for Datum_met/Datumtijd_met/sub-day
forms (no silent widening). (4) clingo: the matching expr_term arms;
ASP-TRANSLATION updated, S19 shrinks, S19 stays highest. (5) Witness:
one golden with a MAAND_UIT disjunction (counting composite), an
EERSTE_PAASDAG∘JAAR_UIT date equality, and a Gelijkstelling
`vondstjaar = jaar uit vondstdatum` (JAAR_UIT's first golden, and it
makes every case a noop refuter); hand-computed cases on both sides of
each boundary plus the leeg corner. This golden doubles as the missing
passing WF-038 Easter witness.

Acceptance:
- Red-first: the verifier-clean repro faults in today's ref with the
  quoted catch-all text; after the change both executors replay the
  golden (gates 5–6) and the negatief is rejected citing WF-038.
- All nine gates green with counts.

May touch: `spec/RUNTIME-SEMANTICS.md` (ch. 3.8 paragraph),
`spec/IR-SPEC.md` (WF-038 clause + index row), `spec/verifier/`,
`ref/` (arith.ml, eval.ml, test_ref.ml worked-example tests),
`backends/clingo/`, `spec/ASP-TRANSLATION.md`,
`corpus/golden/jutterswet-*`, `corpus/negatief/wf038-extractor-text/`.
Out of scope: the DAGSOORT predicate and ResolvedDagsoort evaluation
(follow-up card; rd_geldigheid stays untouched and its silence stays
recorded), Datum_met/Datumtijd_met/sub-day extractors and
Absolute_tijdsduur (the duration-class cards), ONGELIJK-over-extractor
with an empty date (keep it out of the golden or pin it — do not leave
it half-exercised), `spec/CONSTRUCTS.md` (serial at merge).
