# The findings of the reviews of four zones (2026-11-12)

Goal: the faults found in the RegelSpraak frontend (resolver and emitter), the Drools `cases/` package and the
toolchain service are fixed with a witness; the quality points (duplicated blocks, over-engineering, surprising
behaviour, dead code) are cleaned up where the cleanup makes the code shorter or clearer without moving an outcome.
Reports with their probes: the review folder of 2026-11-12 (outside the repository, named in the builders' working
agreement).

## Frontend, rule checker (`RuleChecker` and its helpers)

- A rule target that is not an attribute or dimension cell (a percentage of an attribute) reaches the emitter's net
  RGS-394 on line 0; it gets an error at the target.
- An allocation whose source is not an attribute (or has fewer than two attributes after recovery) disappears without
  a diagnostic; it gets an error.
- Threshold 0 in consistency criteria, a nested criterion and a compound subselection passes to the verifier
  (WF-041); `checkThreshold` runs there as on the condition side.
- A uniqueness over a withdrawn input-marked member passes with a warning (verifier WF-030): the withdrawal reads
  uniquenesses and their unions too.
- Two exception blocks with the same name get RGS-147 ("more than one version … this card"); they are a name clash
  (WF-020); the catalogue text of RGS-147 loses "this card".
- Diagnostics on line 0 while the source is known: RGS-205, RGS-214, RGS-175 (the latter with its file).
- An unresolved name under "Daarbij geldt" gets ERROR RGS-396 and also the sweep's warning that it stays as a marker
  on the wire; the sweep skips what RGS-396 refused.
- `ruleSubjectOf` has no arm for an allocation, so a condition "indien zijn …" on an allocation becomes Unresolved
  (ref EF-019).
- The carrier name of an exception is matched case-sensitively; name matching is case-insensitive (IR-SPEC ch. 2.1).
- Frontend diagnostics for the two verifier arms below (an allocation over non-Number attributes; the left operand of
  a percentage with a unit), at the place of the form.
- Quality: one traversal of the written forms instead of four lists that differ (the cause of the uniqueness fault);
  the operator methods once instead of once per grammar register; the condition typed on the built condition instead
  of rebuilt from the parse tree; the dependency-cycle check without emitting the whole document (the source of the
  RGS-394 class) where that is a contained change; `check*` methods with side effects renamed or returning their set;
  the small duplicates (`setReadError`, `formSites`, `stripArticle`, `withoutVersion`); names (`Clamp`, Dutch
  identifiers) and detached javadoc.

## Frontend, expression builder, decision tables, emitter, scenarios

- A decision-table column whose header contains "ongelijk" anywhere (also in an attribute name) turns its cells into
  an AND of ≠: only the end of the header decides.
- A cell "onwaar" under a characteristic condition, and a Boolean column, generate a rule that does not parse.
- An allocation with method COMPLEX disappears from the document with a warning on line 0; it gets an error with its
  reason.
- Forms the frontend lets through and the verifier refuses: the during-compound and during-comparison shapes beyond
  WF-055, the RGS-070 paths (now a warning), the prorata and day-count markers (WF-056/058), a bucket or target unit
  that does not fit, a variable-start timeline without a start rule (WF-016), two allocations to one target (WF-026).
  Each gets an error at its place, or the builder is restricted to the shapes the verifier admits.
- A scenario expectation without period on a time-dependent attribute ("is leeg", "is 7 €") or characteristic ("is
  niet K") is an error pointing to "is altijd leeg" or a period (see the executors section).
- A scalar scenario value on a parameter with a timeline writes a case the executors refuse; it is an error.
- A percentage per unit (`%/jr`) in a scenario cannot be written; the gap is closed while the unit can be declared.
- An empty condition cell in a decision table (and a row with fewer cells than the header) is an error, as RGS-301 is
  for an empty conclusion cell, unless the grammar's divergences record that an empty cell reads as n.v.t.
- Quality: one pair of functions for the version suffix " #n" (eight places, four patterns); one `norm`; `validity`
  and `writeValidity` agree on null; the small duplicates in `ExpressionBuilder`; `temporalAttributeOperand` uses
  `Expr.contains`.

## Drools `cases/`

- `CaseInput` reads `input.parameters` as strictly as ref (RS ch. 5.7 step 3) and binds the globals under the declared
  name; `setGlobalSafe` goes: a misspelt or undeclared parameter, a wrong type and a misspelt name no longer leave the
  global unset.
- A role key naming another relation falls back to the type pair only when it names a role opposite its own type;
  otherwise a case error.
- The case reader checks every value against its datatype as ref does: the number lexeme, an undeclared enumeration
  label, text on an enumeration, a value with `unit` or two variants, a segment without `value` or with null (RS ch.
  3.1), a missing section, a label with `::`, an empty `expected`, an unknown `format`; text on a number is a case
  error, not a crash.
- Case defects are case errors, not EXCLUDE (an unknown expectation kind, DROOLS-039; an unknown target label is
  EF-002, not DROOLS-048).
- A later EXCLUDE no longer hides an earlier FAIL in one golden.
- A parameter the case does not bind and a rule reads faults EF-014 (RS ch. 5.7b); the NOFAULT entry of gate 8 for
  `aanvoergeld-parameter-unbound` goes (proposal on `scripts/`).
- The EF code and parameters of a fault are public API (the service will carry them in its 422 answer).
- Small: the message for a missing case file; a golden whose case files all carry `expectedFault`; the mismatch line
  names the value kind.
- Quality: the session set-up blocks through one `withSession`; one FactType lookup; the `fireToFixpoint` chain and the
  double `playCase`; the test-only constructor of `Excluded`; the test harness of `SessionDump` out of production
  code where that is contained; Dutch identifiers (`emmers`, `documenthash`, `van`); detached javadoc and the stale
  usage text.

## Executors and verifier (ref, clingo, Drools lowering, verifier, spec)

- A scalar expectation (`attributeValue`, `attributeEmpty`, `characteristicPresent`, `characteristicAbsent`) on a
  time-dependent member is a case error without a code, as ch. 5.8c does for a dimensioned attribute: one sentence in
  RS ch. 5.7 step 3 and one in ch. 8.4; ref and clingo's case reader refuse it (Drools: the cases section).
- A comparison in a condition per period whose operand is a `sum` over members of a time-dependent attribute is
  refused by name by every executor, as a comparison of two own timelines is (ref/clingo's message, Drools
  DROOLS-309); one sentence in RS ch. 3.6: the double fold of `de som van` is the reading on a scalar target.
- Clingo plays a time-dependent characteristic in a scalar condition that ref and Drools refuse; it refuses it too.
- WF-025: amount, share, rest, weight and maximum of an allocation are declared as Number (new arm, negatives one per
  arm). The verifier now lets a sum-type pot, a Text pot and a Text weight through.
- WF-035: the left operand of `percentageOf` is unitless or carries exactly the unit `%` (new arm, negative).

## Toolchain service (in the toolchain, its own commits)

- Unknown keys at the top, in an instance and in a relation are refused (MALFORMED with the path), as they are inside
  a timeline; the old names (`attributen`, `peildatum`) give a 400.
- A number with an exponent or negative scale is refused (WRONG_TYPE) instead of written outside the case lexeme.
- A duplicate JSON key is refused (strict duplicate detection, also in the Quarkus adapter).
- The descriptor: a path used twice, the paths `info` and `playground`, a path with `/` and an unknown key at any
  level are refused at start with the path.
- A list in `relations[].roles` writes one link per id; a role value that is null, an object or a number is refused.
- A date parameter or segment bound outside the case lexeme is refused as on an attribute.
- `serve` exits 3 for a form without norm, as `check` does; an unresolved name stays exit 1. The README row of `ir`
  says why it exits 0.
- The detail flag is judged before the body, as ENGINE.md says.
- Quality: the bodies of the JDK and Quarkus adapters in one class; `prop`/`globalName` once; `Processor` takes
  `RequestOptions` (one production path); dead code (`typePerLabel`, `AnswerBuilder(model)`, `load(Path)`,
  `beanLoader`, the unused `fire` overloads, `percentUnit`); stale Dutch comments.

Not on this card (the maintainer decides first): reading the end world of Drools and ref side by side as a gate leg,
with one outcome reader in uchar for the service; the parameters and the outcome of the service read through uchar
only; the TOKA casus and undeclared names; whether `kind` leaves the service's 422 answer; a descriptor without
entrypoints; whether `RegelSpraakEngine` stays.

Acceptance: every item fixed with its witness, cleaned, or struck with the reason, one line each in the Result;
`make gates` green in uchar and in the toolchain; the toolchain pins the landing.

May touch: `frontends/regelspraak/`, `backends/`, `ref/`, corpus witnesses outside `corpus/golden/anchor`, this card.
Protected (the main session lands them): `spec/`, `scripts/`.
