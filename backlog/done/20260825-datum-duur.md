# A1 — day-precision tijdsduur and date±duration reach ref and clingo

Goal: the heart of the stress-test date/duration family (2 tijdsduur
scenarios; the date±duration forms ride the same scenarios). The wire
forms are verifier-clean today; both executors refuse loudly (ref:
arith.ml:457 "non-empty tijdsduur is ch. 3 scope" — an outdated scope
claim, RUNTIME-SEMANTICS ch. 3.8 r863-908 already promises the
semantics with worked examples; ABSOLUTE_TIJDSDUUR falls through the
eval.ml:867 catch-all; date±duration routes into the numeric path and
faults "plus: expected a number, got <date>"). Close it spec-first as
the v0 mirror (exploration verified against RegelSpraakRuntime.java):

1. SPEC (RUNTIME-SEMANTICS ch. 3.8, protected): the whole-unit
   definition made precise — the packed-month rule, with three pinned
   worked examples: leap child 29-02-2004 → 28-02-2026 = 21 whole
   years, → 01-03-2026 = 22; clamp-not-inverse: 31-01-2024 + 1 maand =
   29-02-2024 but tijdsduur 31-01→29-02 = 0 whole months;
   signed/truncation: −7 months in quarters = −2 (toward zero). Plus:
   "hele" only types the result (the runtime always counts whole
   units); the normative time-unit spelling table (nine units +
   abbreviations + plurals, keyed on SPELLING — declared factors and
   stelsel are irrelevant to calendar arithmetic, stelsel may be null);
   operand order (PLUS both sides, MIN only date-left; date±date
   ill-formed); precision rules (both operands equal precision; sub-day
   units require datum-tijd — datum-tijd itself is card A2); empty
   rules already in ch. 1.2 (empty duration → date UNCHANGED, Tabel 15;
   empty date → empty; tijdsduur with an empty operand → empty, never
   0); fractional counts truncate toward zero; the calendar is
   proleptic Gregorian, zoneless civil time (one fixing sentence).
   Ch. 2.2's operator inventory gains the two rows; ch. 1.2's date row
   becomes Executed.
2. WF: implement the OPEN WF-035 date±duration clause (the index says
   literally "the date±duration clause is OPEN"); extend WF-038:
   tijdsduur operands both dates of equal precision, the node-type
   unit a recognised time unit. Negatiefs: wf035-datum-plus-datum,
   wf035-duur-min-datum, wf035-uur-op-dagdatum,
   wf038-tijdsduur-gemengde-precisie, wf038-tijdsduur-zonder-tijdseenheid.
3. ref: the calendar kernel in arith.ml on the existing civil helpers
   (value.ml:54/64); eval.ml routes Binair on static date type and
   Tijdsduur/Absolute_tijdsduur on the node-type unit; the spec worked
   examples become tests verbatim; the spelling table as a shared
   function reachable from both verifier and ref.
4. clingo: full day-precision translation (dates are civil day numbers
   per B2): day/week linear; year/month/quarter via the pure-ASP civil
   decomposition chain with viol/2 range guards (all intermediates
   non-negative so gringo truncation = floor; a negative-span witness
   pins the toward-zero division); month-end clamp via a leap/month-
   length rule pair. ASP-TRANSLATION gains its section; S19 shrinks,
   stays highest.
5. Witnesses: anchor `jutterswet-jutleeftijd` (hand-computed, leap
   corner + empty corner + negative span) and golden
   `jutterswet-vervaltermijn` (date + 2 weeks; month-clamp case), both
   through ref AND clingo.

Acceptance: red-first per form (quoted faults from today's executors on
the verifier-clean repro), then gates 5–6 agree on the new
goldens; all five negatiefs rejected citing their rule; all nine gates
green with counts.

May touch: `spec/RUNTIME-SEMANTICS.md`, `spec/IR-SPEC.md`,
`spec/verifier/`, `ref/`, `backends/clingo/`, `spec/ASP-TRANSLATION.md`,
`corpus/golden/**`, `corpus/negatief/wf035-*`, `corpus/negatief/wf038-*`.
Out of scope: datum-tijd (card A2), dagsoort-as-unit forms and
DATUM_MET/DATUMTIJD_MET (recorded, stay loud), the date-literal lexeme
pin (card A3), the timeline/grid machinery (this class is scalar — do
NOT route tijdsduur through timelines), `spec/CONSTRUCTS.md` (serial).

Result: the day-precision date/duration family executes end to end.
RUNTIME-SEMANTICS ch. 3.8 carries the packed-month rule with the three
pinned worked examples, the nine-unit spelling table, operand order,
precision rules, toward-zero truncation and the proleptic-Gregorian
fixing sentence; ch. 2.2 gained the two rows, ch. 1.2's date rows are
Executed. The OPEN WF-035 date±duration clause and the WF-038 tijdsduur
extension are implemented in the verifier on the shared spelling table
(Ir_v0.Checks.time_unit_of_spelling — one source for verifier, ref and
clingo); all five negatiefs reject citing their rule (red-first: all
five passed the pre-change verifier). ref carries the calendar kernel
in arith.ml on the value.ml civil helpers, eval.ml routes Binair on the
static date type and Tijdsduur/Absolute_tijdsduur on the node-type
unit; the worked examples are ref tests verbatim (280 pass). clingo
carries the pure-ASP calendar chains (ASP-TRANSLATION S6a: day/week
linear, months via the era decomposition and packed difference,
date-plus-months via the dpmN leap/month-length rule pair with the
exact-min clamp; pre-epoch and year-zero viol/2 fences); S19 shrank and
stays highest. Witnesses: anchor jutterswet-jutleeftijd (leap corner,
leeg corner, negative span -2 kwartalen toward zero, clamp-not-inverse)
and golden jutterswet-vervaltermijn (2 weken, month-end clamp both
leap-nesses, min date-left, leeg-duration standstill, fractional
truncation, commuted plus), replayed exactly by ref AND clingo. All
nine gates green (133 ir round-trip / 133+152 verifier / 349 cases /
329 oracle / 329 clingo / 295+34 noop / 20 rood / 169 catala checks).
Deliberately left, loud: dagsoort-as-unit forms, DATUM_MET/DATUMTIJD_MET,
the datum-tijd sub-day layer (A2), the date-literal lexeme pin (A3);
timelines untouched — tijdsduur is scalar, as mandated.
