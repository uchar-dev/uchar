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

Result: the date-function class is closed in both executors. RS ch. 3.8
gained the extraction/Easter paragraph (civil components as unitless
whole numbers, leeg via ch. 1.2b, the Gauss/Anonymous algorithm written
out with the 2024/2026/2027/2038 pins); WF-038 gained the
day-precision-extractor date-operand clause, verifier arm and negatief
`wf038-extractor-text` (red-first shown: it was verifier-clean before,
faulting only at runtime). ref: Arith.maand_uit/dag_uit mirror
jaar_uit, Arith.eerste_paasdag is the Gauss chain (leeg propagates; a
non-whole or unit-carrying year faults — the ratified delta vs v0's
silent int-narrowing, noted in the RS paragraph); the catch-all stays
for datum-met/datumtijd-met/sub-day. clingo: the extractors read the
shared era decomposition, Easter is the Gauss chain into the
days_from_civil re-encode with a year-before-1 viol fence;
ASP-TRANSLATION S6a extended, S19 shrunk (S19 stays highest). Witness
golden `jutterswet-paasvondst`: maand-uit TEN_MINSTE-1 disjunction,
eerste-paasdag∘jaar-uit equality, jaar-uit Gelijkstelling — plus a
dag-uit Gelijkstelling beyond the card's letter, added so clingo's
DAG_UIT arm is differentially exercised, not dead code. Five
hand-computed cases on both sides of the Easter and month boundaries;
the leeg case pins paasvondst WAAR on a dateless Vondst (ch. 1.2d
numeric equality of two empties, both executors agree). All 14 worked
examples are verbatim ref tests (293 total). Nine gates green:
134 ir round-trip, 134/155 verifier, 354 cases, 334 oracle = 334
clingo, 300 noop refuters + 34 pinned, 20 rood, 169 catala checks.
Deliberately left: DAGSOORT/ResolvedDagsoort (follow-up card),
datum-met/datumtijd-met and the sub-day extractors (duration-class
cards), ONGELIJK-over-extractor with an empty date (kept out of the
golden entirely), the CONSTRUCTS.md row (serial at merge).
