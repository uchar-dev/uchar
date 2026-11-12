# The findings of the afternoon reviews (2026-11-12)

Goal: the faults the second review round found are fixed with a witness, and the simplifications it measured are
made without losing a function. Reports with probes: the review folder `review-2026-11-12b` (outside the repository,
named in the builders' working agreement). Only what the round classed as a real fault or a measured simplification
is on this card.

## Frontend, declarations and typing (`DefinitionResolver`, `Typing`, `Designations`, matchers)

- Two attributes of one name in one head: the second replaces the first without a word (WF-002).
- Duplicate names within a declaration reach the verifier: attributes that differ in case only (RGS-011 is a
  warning), characteristics, role names, enumeration values, dimension labels (WF-002, WF-012, WF-006, WF-014), and
  two timelines of one name, also identical ones (WF-015, IR-SPEC WF-001: "even an agreeing re-declaration is
  ill-formed"). One case-insensitive check per list, an ERROR.
- `Extensie van objecttype` naming its type in other letters creates a second object type (WF-001); `Designations`
  follows.
- A "meerdere" segment of a cardinality rule that binds no role disappears, so a one-to-many relation goes to the wire
  as one-to-one; it is an ERROR.
- A percentage plus or minus a percentage (and equality with one) is refused while verifier and ref take it: both
  operands Percentage give a Percentage.
- A comparison of a date with a date-time in days reaches the verifier (WF-036); an ERROR in the frontend.
- A domain and a conversion target are looked up case-sensitively (WF-003, WF-009).
- A dimensioned attribute declared on a timeline reaches the verifier (WF-125.timeline); an ERROR at the declaration.
- A time-dependent value in a scalar position (a parameter `voor elk jaar` or a timeline attribute assigned to a
  scalar target) passes the frontend and the verifier; RUNTIME-SEMANTICS ch. 3.3 "Scalar contexts" refuses it: an
  ERROR.
- Simplification: one `CharacteristicMatcher.bind` for a characteristic name with its negation instead of the two
  sequences in `ExpressionBuilder` and `RuleChecker`; `DefinitionResolver` uses `RegelSpraakModel.timelineByName`.

## Frontend, phases (`RuleChecker`, `ExpressionBuilder`, `IrEmitter`, `Resolver`)

- A characteristic check through a navigation ("indien de standpost van de Jutter bemand is") binds its role against
  the rule's subject again in the emitter: a silent wrong binding where the subject has a role of that name, RGS-394
  on line 0 where it has none. The check binds against the context's subject; a written subject that is not it is an
  ERROR at the line.
- One emission at the end of `Resolver.build`, after `AbsenceMarkers.lower`: the cycle check reads that text, `--ir`
  writes it, and a refusal of the emitter carries the source of its node or the name of its form. The emission in
  the middle of `RuleChecker` goes.
- RGS-001 ("verdeling wordt niet geschreven") is dead since RGS-401 if a measurement confirms it; then it goes with
  the `complexAllocations` flag and its register and catalogue rows.
- Binding where the node is built, as `QuantifierFold` does: a `CharacteristicCheck`, a role membership, the range of
  a fold call and the path of an attribute reference carry their `RoleBindingRef` (or the reason it failed) from the
  builder; the emitter derives nothing, its derivations from `currentSubject` and the public statics the resolver
  asks go. Every step keeps the 216 goldens with a `source.rgs` byte-identical.

## Frontend, scenarios and tokens (`ScenarioResolver`, `CaseEmitter`, `SemanticTokens`)

- `Resolver.tokens` (and `bin/regelspraak tokens`) crashes on a unit line without a word after recovery
  ("de (mv: dagen) dg"); a target without a declaration is written unlinked. A recovery row with one token left out
  joins `SemanticTokensRecoveryTest`.
- A scenario accepts an inconsistency rule the case reader does not know (a member without a defaults group, a rule
  outside the scope); it checks against the forms the document carries and the targets of its defaults groups.
- The two scenario gates do not see day-kind definitions and start-point determinations, and a scalar input on a
  time-dependent attribute without an evaluation date passes; they see and refuse them.
- A token after `zijn`/`haar` takes the member before a parameter of the same name.
- uchar's scope reading (`forms` of scope names, `withStack`) becomes public API, so the service stops copying it.
- Simplification: `ScenarioResolver.member` uses the cell choice of `ExpressionBuilder.dimensionCell` (one function).

## Executors, verifier and Drools `cases/`

- A part of a date construction that carries a unit is refused statically (a WF-038 arm with a negative); EF-046
  stays for the non-whole value (Drools played it).
- WF-025: amount, rest and maximum reduce to the unit of the share, the weight is free (an arm with a negative, and a
  frontend diagnostic beside RGS-423); a maximum in % or kg now passes both and faults without a code in all three.
- RUNTIME-SEMANTICS ch. 5.12a: where the session has no evaluation date or the grid lays no bucket, the asOfDate
  scalar stands as the case gave it; ref's end-state writer follows.
- Ch. 3.12's opening says a kind that depends on the case is `fault` (EF-041, EF-042); `ref/lib/world.ml` follows.
- The negative base of a fractional power against the square root of a negative number: as the norm advice of the
  card says.
- The Drools case reader refuses an inconsistency expectation naming an unknown rule as a case error, and an
  expectation without `label` as a case error instead of a NullPointerException.
- `EndState.channel` is built from `Fixpoint.inconsistencies` and `compare` passes the inconsistencies of its reading
  to the comparison (two scans and a swallowed `ClassNotFoundException` go); `EndState` carries the faults and
  whether a creation laid a link.

## Service (toolchain, its own commits)

- A date-time in the answer keeps its seconds (the formatter of `caseValue`), the schema states that pattern instead
  of `format: date-time`, and the violation schema names `{field, kind, error}`.
- A parameter set reads its values as the descriptor defaults do (`NameReader`, `caseValue`); its own decoder and the
  unused `parameterNames` go.
- `scenario` on a source without a Scenario block exits 1; `graph` checks its output directory; `scenario` exits 2
  for a source it cannot read.
- After the pin: `FormIndex` reads a scope through uchar's public scope reading (its copy goes, and with it the rows
  it carried that uchar does not); the service reads one record (`standingOf`) and `Fixpoint` leaves its main code;
  an inconsistency stands once per rule × subject (RUNTIME-SEMANTICS ch. 5.6), not once per deposit.

Not on this card (the maintainer decides): whether the OpenAPI document is served or the README stops calling it the
contract (`OpenApiEmitter` has no production caller).

Acceptance: every item fixed with its witness, done, or struck with the reason; `make gates` green in uchar and in
the toolchain; the toolchain pins the landing.

May touch: `ref/`, `backends/`, `frontends/regelspraak/`, corpus witnesses outside `corpus/golden/anchor`, this card.
Protected (the main session lands them): `spec/`, `scripts/`, `CLAUDE.md`.

Result (2026-11-12):
- Declarations and typing: duplicate names within a declaration list are one case-insensitive ERROR (RGS-431;
  attributes, characteristics, roles, enumeration values, dimension labels), two timelines of one name RGS-008 (also
  identical ones); RGS-011 and RGS-019 are gone. An extension in other letters extends the type of its head. A
  "meerdere" segment that names no role is an ERROR (RGS-432). A percentage plus or minus a percentage is a
  Percentage (golden `jutterswet-percentage-plus-percentage`, `EXPECTED_PLAYED` 381). Earlier/later between a date in
  days and a date-time is an ERROR (RGS-430); a domain and a conversion target are looked up case-insensitively; a
  dimensioned attribute on a timeline (RGS-433), a time-dependent value in a scalar position (RGS-434) and an amount,
  rest or maximum of an allocation whose unit does not reduce to the share's (RGS-435) are ERRORs. `DefinitionResolver`
  uses `timelineByName`.
- Phases: a characteristic check navigating from another subject than the rule's is an ERROR (RGS-440), no longer a
  silent wrong binding; one emission at the end of `Resolver.build`, after `AbsenceMarkers.lower`, and RGS-394 carries
  the line of its node and the form; RGS-001 was dead (53 models without a method in the suite each had an ERROR
  already) and is gone with `complexAllocations`; a characteristic check and a role membership carry the binding the
  resolver built (RGS-441 for a name without a unique role; the emitter and the graph's source route derive nothing
  for them); one `CharacteristicMatcher.bind`. Struck: binding the range of a fold call and the path of an attribute
  reference where they are built — `RuleChecker.opSubtype` moves the base rule's conjuncts to the subtype's subject,
  so their binding would have to be recomputed there; a subtype rule that reached RGS-394 now writes the base's
  binding, the reading the author wrote on the supertype.
- Scenarios and tokens: a unit line without its word after recovery no longer crashes the tokens; a token after
  `zijn` names the subject's member before a parameter of its name; a scenario names only inconsistency rules the case
  readers know (RGS-270); the parameter gate reads day-kind definitions (RGS-451) and a value without period on a
  time-dependent attribute without a peildatum is RGS-450; `ScenarioResolver.scopeForms` is public API (a table row
  recognised by its origin, group names case-insensitive); one cell choice (`ExpressionBuilder.chooseCell`). The
  fuzzer that leaves one token out finds 0 crashes over the production and scenario goldens. Struck: `haar` (the
  grammar has no such token); start-point determinations and day kinds in the gates (already seen or refused).
- Executors: a date part with a unit is refused statically (`WF-038.date-part-unit`, negative); WF-025 holds amount,
  rest and maximum to the share's unit (`WF-025.unit-not-share`, two negatives); ch. 5.12a writes the asOfDate scalar
  as the case gave it where the session has no evaluation date or the grid lays no bucket (ref broke on a quarter
  grid); ch. 3.12's opening names the fault kinds that depend on the case; the Drools case reader refuses an
  inconsistency expectation with an unknown rule and an expectation without `label` as case errors (the latter was a
  NullPointerException); `EndState.channel` comes from `Fixpoint.inconsistencies`, `compare` passes them on (two scans
  and a swallowed `ClassNotFoundException` gone), `EndState` carries `faults` and `Link.made`. A negative base under a
  fractional exponent is leeg with a §9.5 deposit EF-044, as a negative radicand under the root (maintainer
  decision); the red case `fractional-power-negative-base` is a golden (`EXPECTED_PLAYED` 380, clingo CLINGO-041).
  `UnitConflictWitnessTest`'s twist drops a percentage presentation, so it reaches the unit fence on a percentage.
- Service (toolchain, own commits): a date-time keeps its seconds in the answer and the schema states the case
  lexeme; the violation schema names `{field, kind, error}`; a parameter set reads its values as the defaults do (its
  decoder and `parameterNames` gone, 335 → 179 lines); `scenario` without a Scenario block exits 1, on an unreadable
  source 2, `graph` into a missing directory 2. The items after the pin follow at the toolchain pin.
- `make gates` green.
