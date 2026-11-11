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
