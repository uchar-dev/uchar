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
