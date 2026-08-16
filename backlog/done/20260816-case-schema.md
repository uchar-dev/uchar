# Corpus case schema: inputs and expected outcomes (uchar-case-v0)

Goal: the golden-case data format the reference interpreter consumes
(`CONSTRUCTS.md` row 37): `spec/schema/uchar-case-v0.schema.json` — an
engine-neutral case file with the evaluation date, input instances
(labelled; attributes, label-based role links, asserted
characteristics), parameter values, and the six expectation kinds
(attribute value, explicitly empty, characteristic present/absent,
consistent/inconsistent). Donor: the toolchain's scenario layer — its
engine-neutral half only; values are unit-normalised to the declared
unit and numbers are exact lexemes, never JSON floats. Case files for
three existing goldens with expectations derived from the normative
equations (RUNTIME-SEMANTICS ch. 4-5) and v0-pinned test values. Gate 4
(`scripts/check-case-schema.sh`): metaschema + every
`corpus/golden/**/case*.json` validates and sits beside an `ir.json`;
wired into CI; documented in `spec/schema/README.md` and
`corpus/golden/README.md`; row-37 note updated.

Acceptance: gate 4 green, shown red first; gates 1-3 stay green; at
least three case files validate; expectation values traceable to
normative worked examples.

May touch: `spec/schema/` (protected — merge mandate), `scripts/`
(protected), `.github/` (protected), `CLAUDE.md` (protected; gate
list), `corpus/golden/` (case files + README), `spec/CONSTRUCTS.md`
(row-37 note only), `backlog/`.

Out of scope: `ref/`; machine replay of expectations (the oracle guard
lands with `ref/`); the scenario surface syntax.

Result: uchar-case-v0 landed: the schema (six expectation kinds,
exact-lexeme numbers, unit-normalised values, explicit-empty as an
expectation only), five case files over three goldens (penningdrempel
per the ch. 5.7 trace; berging per the ch. 4.6 recurrence; vondstnummer
consistent/date-violation/duplicate-number), gate 4 red-then-green with
CI job, docs in the schema and golden READMEs, row-37 note updated.
Two deliberate narrowings vs the donor recorded (no rule scope;
date-only evaluation date); cross-item invariants assigned to the ref/
case loader. Verified by two adversarial reviewers (0 blockers, 5
minors - all fixed). Deliberately left: machine replay (the ref/
oracle guard); case files for vondsttelling and penningjaar (need
temporal/aggregation replay semantics first).
