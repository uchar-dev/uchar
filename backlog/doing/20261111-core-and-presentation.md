# Core and presentation, and the coherence of the spec text

Goal: the layer marks of IR-SPEC say what is true. Core is what the spec gives a meaning, which an executor evaluates
or refuses with a registered code and never skips; presentation is what no executor's answer depends on, and gate 13
proves that per field. Forms without a norm leave the wire. The contradictions the Fable review of 2026-11-11 found in
the spec text are resolved. The maintainer's norm decisions of 2026-11-11 in full:
`werk/review-candidates-triage/normbesluiten.md` beside the repository; the reviews: `werk/review-fable/spec-coherence/`,
`werk/review-fable/dialect-decisions/` (with `inventory.md`, the classification of every layer mark) and
`werk/review-fable/forms-without-norm/`. Small commits, each with its witness.

The definition and the inventory of layer marks:
- IR-SPEC r. 136–137 gets the definition of core and presentation in the wording the maintainer chose (in the decisions file). Ch. 1 and ch. 10
  exist only as lines of "Document structure"; the definition goes into the conventions, ch. 10 becomes a section.
- Ch. 10 lists every presentation field with its test: strip (`Dimension.preposition`, `origin`, the percentage tag,
  a non-recursive `RuleGroup`, the side tables: vocabularies, default names) or exchange (`Characteristic.kind`, the
  value of `presentation`). Gate 13 gets a leg per field that has none (one combined copy per golden keeps the cost
  down); the legs for vocabularies, origin and names exist.
- Core, marked as such: the rule kind in three states (presentation tag / `initialisation` / neither; r. 2713–2717,
  "absent is no tag" on r. 2636 goes), membership of a creation form in a recursive rule group, the cell forms of
  ch. 3, the date builtins that stay, `dayKind`, `dividedByAbs`, the dimension forms, `wholePeriod`, `filledCount`.
- A producent whose source has no equation/calculation distinction SHOULD write `calculation`; the RegelSpraak
  frontend writes `equation` on decision-table rows, the `stepsAfterSet` rule and the start-point anchor (9 versions),
  named in that sentence.
- WF-033 split: member resolution stays the verifier's; the "(dialect)" label (r. 3474, 10279) stays on the member
  clause only. RS r. 4453 and IR-SPEC r. 2738–2741 ("nothing reads the group's own") follow.
- PLAN: §9 decisions (4) and (5) amended with date (the wording in the dialect-decisions report), and the design
  principle at r. 109, 138–144, 342–345 and 501. CONSTRUCTS: the head (r. 15–16) and rows 14, 28, 29, 102, 103, 107,
  121, 131, 149, 159, 169. Not now: a core field for the rule kind (a candidate for the next wire epoch that comes for
  another reason).
- The OPEN of the percentage tag (IR-SPEC r. 1251–1253) closes: CONSTRUCTS row 27 closed it, and stripping the tag
  on all 7 goldens changes no outcome.

Forms without a norm:
- Off the wire: `elevenTest`, `hourOf`, `minuteOf`, `secondOf`, `millisecondOf`, and "is gevuurd" (`Regelversie` with
  `inconsistent: false`). Schema, IR-SPEC, the spelling table (`v1-naamtabel.json`), the frontend (a Unresolved with
  diagnostic, WF-113), Drools (execution and DROOLS-129 go; their tests follow), the gate-2 self-test rows. No
  existing document becomes invalid: a numbered landing, no epoch.
- `exactDigits` is normed: a clause with a worked example, ref's evaluation, a golden (a BSN-like text with exactly
  9 digits), a negative case for its verifier arm, and clingo's translation or a registered refusal.
- "Is inconsistent" stays and gets a golden, a negative case and a registered clingo refusal in place of the
  fallback CLINGO-011.
- The convention: a form is on the wire when RUNTIME-SEMANTICS (or IR-SPEC, for a form without
  evaluation) gives it a meaning with a worked example; a form with a clause but without a golden or a negative case
  stays, as OPEN in the WF index.
- The distribution: its message "onopgeloste naam" is wrong for a form taken off the wire; the toolchain session gets
  a text (not built here).

Coherence of the text:
- Strata: WF-046 and RS ch. 6.3 get the exception that inside an instance-ordered part every edge counts
  positive and the members share one stratum (ch. 6.2).
- WF-050's "no second defaults group, no rule" says what the rule already admits (several groups iff all
  `unordered`; "no rule" per level of the type chain).
- The worked example WV-6.4-j's "never withdrawn (ch. 5.3)" follows the decision of 2026-11-02 (an attribution that does not decide
  withdraws what it set); measure the example's outcome.
- Stale WF sentences in RS (the WF-030 union clause, WF-024 retired, the WF-030 OPEN decided), and the node kind
  "cell" in RS ch. 6.1.
- WF-008 and WF-010 are checked under the keys of WF-036 and WF-023/035; their OPEN and its reason go.
- References: the chapters IR-SPEC names but does not have, about 66 "ch. N" without a document name that mean RS,
  and the two that point nowhere (r. 520, r. 1823).
- The chapter-introduction counts (ch. 2: four exceptions; ch. 3: the cell forms; ch. 4: `filledCount`).
- Verifier arms without a negative case (141, two unreachable): the unreachable two go; the rest as OPEN per the convention above,
  filled by card `review-findings-2026-11-11` where the reviews named them.

Acceptance:
- Every item fixed or struck with the reason, one line each in the Result.
- Gate 13's new legs red on a mutant that reads the field, green on the landing.
- `make gates` green; the toolchain builds on the new pin.

May touch: this card, corpus witnesses outside `corpus/golden/anchor`, `ref/`, `backends/`, `frontends/`. Protected
(the main session lands them): `spec/IR-SPEC.md`, `spec/RUNTIME-SEMANTICS.md`, `spec/schema/`, `spec/verifier/`,
`spec/PLAN.md`, `spec/CONSTRUCTS.md`, `spec/proposals/`, `scripts/`.

Out of scope: the executor faults and norm decisions of card `review-findings-2026-11-11`; a wire epoch.

Date norms (maintainer, 2026-11-11), carried out on this card because they are spec text with small executor changes:
- Under the whole-period qualifier every date comparison is refused, equality and inequality too (RS ch. 3.9 names
  all date comparisons; ch. 3.12 says so already); ref refuses equality as well, Drools does already.
- `easterSunday` is defined from year 1; before it a fault with a code in ref and Drools (clingo refuses years before 1
  already). The formula gives a day that does not exist for 307 years between −5000 and 0.
- Year, month and day of a date-time are defined (IR-SPEC WF-038 "either precision"; RS ch. 3.8 follows); ref and
  clingo learn them, Drools computes them already.

Result (2026-11-11):

Definition and layer marks
- fixed: the definition of core and presentation stands in the conventions of IR-SPEC in the maintainer's wording;
  ch. 10 "Presentation" is a section with a table per field (chapter, what the schema requires, the test: strip or
  exchange) and names the core forms the source language spells. Layer marks in ch. 2, 3 and 4 follow; the mark is
  `layer: presentation`, the dialect mark is gone (schema descriptions too).
- fixed: the presentation fields and their test in gate 13's new presentation leg, one combined copy per golden, a
  difference assigned to a field by a copy per field: `Characteristic.kind` (exchanged), the percentage tag,
  `Dimension.preposition`, a non-recursive `RuleGroup`, the value of `RuleVersion.presentation` (exchanged),
  `Rule.statuteAnnotations`, `diagnostics` (exchanged) and the source reference `source` (stripped; exchanged on an
  `Unresolved`). The leg went red on a mutant per field that reads it, naming the field. A Drools refusal sentence
  that differs only in the quoted snippet counts as the same outcome.
- fixed: `source` became presentation in fact: the ch. 7.7 desugaring marks its defaults groups in the AST
  (`pd_desugared`) and clingo reads that field instead of the snippet text; a clingo test pins it.
- fixed: the rule kind in three states, recursive rule groups, the cell forms, the date builtins, `dayKind`,
  `exactDigits`, `dividedByAbs`, `filledCount` and the whole-period qualifier are core. The SHOULD `calculation`
  names the three places the RegelSpraak frontend writes `equation`. The OPEN of the percentage tag is closed.
- fixed: WF-033 in two clauses, (a) members resolve (presentation) and (b) recursion (core); the group name is read
  by WF-057, WF-118 and WF-121 (b), and both sentences say so. New negative `wf033-member-names-no-form` for (a).
- fixed: PLAN (r. 109, the design principle, Phase 1, Phase 5, §9 (4) with the candidate core field for the rule
  kind, §9 (5)) and CONSTRUCTS (head and rows 14, 28, 29, 102, 103, 107, 121, 131, 149, 159, 169; BinOp variants;
  side-table rows 41 and 42 to presentation).

Forms without a norm
- fixed: `elevenTest`, `hourOf`, `minuteOf`, `secondOf`, `millisecondOf` and "is gevuurd" are off the wire: schema,
  IR-SPEC, RS, ASP-TRANSLATION, the spelling table, the verifier (its two arms go; `inconsistent: false` is a load
  error), the frontend (an `Unresolved` with an RGS-092 warning, three new reasons), Drools (execution goes,
  DROOLS-129 RETIRED), the gate-2 self-test rows. 1221 documents scanned: none carried a removed form.
- fixed: the convention: a form is on the wire when the spec gives it a meaning with a worked example.
- fixed: `exactDigits` is normed (RS ch. 1.2, WV-1.2-exact in ref/test): ref and Drools evaluate it, clingo refuses
  with CLINGO-046; golden `jutterswet-registratienummer`, negative `wf036-exact-digits-operand`. N ≥ 1 under WF-036
  (`wf036-exact-digits-count`; the frontend reports a smaller count as RGS-205). Under the whole-period qualifier it
  is read per day: RS ch. 3.9 now opens with the per-day reading of every condition of ch. 1.2 (WV-3.9-exact);
  golden `jutterswet-registratienummer-gehele-jaar`; REF-008 retired (a `retired` kind in
  `scripts/check-exec-codes.sh`); Drools reads a text value on a timeline.
- fixed: "is inconsistent" keeps its node; golden `jutterswet-saldotoezicht` (the example of RS ch. 5.6), clingo
  refuses with CLINGO-047 in place of the fallback CLINGO-011; its consistent case is a gate-7 survivor with reason.
- open, for the toolchain: `UcharBuild` reports a sentence taken off the wire as "onopgeloste naam"; text in
  `werk/core-and-presentation/forms/result.md`, carried out at the pin.

Coherence of the text
- fixed: strata (WF-046, RS ch. 6.3), WF-050, WV-6.4-j (outcome measured, the ref comment follows), the stale WF
  sentences (WF-024, the WF-030 union, the WF-030 OPEN), the node kind cell in RS ch. 6.1, WF-008/WF-010 under the
  keys that check them, 68 references to RS named, the two that pointed nowhere.
- fixed: the two unreachable arms (WF-022 rule without target, WF-029 without assertion) are gone: the loader
  refuses those documents. 49 WF index rows carry `**OPEN** arms without a negative case:` with 135 keys, made by a
  script from the measurement; the head of the WF index no longer counts its OPENs.
- struck: the chapter-introduction counts were right already.

Date norms
- fixed: under the whole-period qualifier no comparison of dates has a reading, equality included: ref and clingo
  refuse equal/notEqual as they refuse earlier/later, Drools with DROOLS-246 (it used DROOLS-247).
- fixed: `easterSunday` before the year 1 faults with EF-007 in ref, clingo and Drools (red case
  `jutterswet-paasdag-voor-jaar-een`); the test `WV-3.8-d-bereik-paasdag-onder` now expects the year-1 sentence.
- fixed: year, month and day of a date-time in ref and clingo; golden `jutterswet-meldtijdstip-delen`.

Tests changed because behaviour changed: the Drools tests of the removed forms and DROOLS-129, `TypingSentenceTest`,
`StructureSentenceTest`, `ReaderSentenceBoundaryTest`, the builtin-kind count in the Drools tests, the ref test
`WV-3.8-d-bereik-paasdag-onder` (the year-1 sentence), the record literals of the ref, Catala and F-logic suites (the
new AST field). `EXPECTED_PLAYED` 373 → 377.
