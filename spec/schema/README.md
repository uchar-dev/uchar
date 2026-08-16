# IR JSON schema

This zone holds the machine-checkable schema for the uchar IR wire format, plus
the gate that enforces it. It is a gated artifact zone: it must carry its full
meaning without reference to any external repository or machine (see
`../../scripts/check-self-contained.sh`).

## The `regelspraak-ir-v0` format

`regelspraak-ir-v0` is the first, mirror cut of the IR: the faithful,
engine-neutral JSON dump of the reference frontend's fully resolved model — every
declaration side-map, every resolved rule list, and the diagnostics, with the
original (Dutch) field names preserved verbatim. It mirrors first and diverges
later; each future deviation from this shape is a tracked delta, not a silent
rewrite. Rationale and method: `../IR-SPEC.md`; the construct inventory and
divergence log: `../CONSTRUCTS.md`.

Shape, in brief:

- A single top-level object with `"formaat": "regelspraak-ir-v0"` and the
  declaration registries (`objecttypen`, `domeinen`, `parameters`,
  `eenheidstelsels`, `feittypen`, `dimensies`, `tijdlijnen`, `dagsoorten`, …)
  followed by the resolved IR lists (`resolvedRegels`, `verdelingen`,
  `objectCreaties`, `feitCreaties`, `uniciteiten`, …) and `diagnostics`.
- Every node object carries a `"node"` discriminator holding its subtype name.
  The `Datatype`, `Expr` and `Conditie` families are modelled as discriminated
  unions on that field.
- Determinism: maps and lists keep declaration order; numbers are written in
  plain (non-exponential) form; non-ASCII and control characters are escaped as
  `\uXXXX`. This makes the dump byte-stable for a given input.

## `regelspraak-ir-v0.schema.json`

JSON Schema (draft 2020-12) for the format. It is strict: every object sets
`additionalProperties: false` and lists every always-emitted key as `required`;
enum-valued fields are constrained to their exact constant sets; fields are
nullable only where the format actually emits `null`. `$id`:
`https://uchar.dev/schema/regelspraak-ir-v0.schema.json`.

**Extension fields** (the decided wire-entry discipline, `../IR-SPEC.md`
§ Method): a v1 construct enters the wire additively, as an *optional*
property the v0 emitter never produces — currently `verbindendeRol` on
`ResolvedVerdeling`, `bronRol` on `Functie`, and the symbolic
fact-creation fields (`feittype`/`ankerRol`/`doelRol`/`joinRol`,
`Tussenstap.rolBinding`), all carrying the `RolBinding` node. When
present they are written whole, never null; the loaders admit exactly
these keys and nothing else. Mirror purity holds per file: an emitter
dump carries none of them, a hand-written anchor case may carry them,
and the same schema validates both. The `regelspraak-ir-v1` bump is
reserved for the non-additive cleanup (renames; dropping the superseded
bean fields).

## Gate: `../../scripts/check-ir-schema.sh`

The repo's second CI gate. It performs three checks:

1. **Metaschema** — the schema is itself a valid draft 2020-12 schema.
2. **Validation** — every `corpus/golden/**/ir.json` validates against the
   schema. The file count is reported; an empty corpus passes and says so.
3. **Round-trip** — each such file is parsed (preserving the original number
   lexemes) and re-emitted canonically; the result must be byte-identical to the
   input. This is what ties the schema-blessed form to the emitter's form: a
   drift in either surfaces as a byte diff.

The gate needs a `python3` with the `jsonschema` package (it falls back to
running it in an isolated environment via `uv` if one is available). It also
accepts explicit file paths as arguments, to check a dump that lives outside the
corpus.

Corpus IR files land under `corpus/golden/**/ir.json` and are picked up
automatically once they exist.

## The case schema (`uchar-case-v0.schema.json`)

A golden case's *data* half: `case*.json` beside the `ir.json` carries the
input world (labelled instances with attributes, label-based role links,
characteristics asserted true), the parameter values, the single
evaluation date (`asOfDate`, null for a time-free case), and the expected
outcomes — an attribute's value, an attribute explicitly empty, a
characteristic present or absent, or the consistency verdict. The
reference interpreter (`ref/`) consumes these files as its test suite;
gate 4 (`scripts/check-case-schema.sh`) validates them.

Conventions, deliberate:

- **Numbers are exact lexemes** (decimal-with-dot or `a/b` rational),
  carried as strings — never JSON floats. The comparison contract is
  exact numeric equality, scale-insensitive.
- **Values are unit-normalised** to the referenced declaration's unit
  before they enter the file; a case file carries no unit spellings.
- **Absence means empty** on input; only an expectation can *assert*
  emptiness (`attributeEmpty`). Characteristics start false; input can
  only assert them true.
- One instance per label; role links point at labels; a to-one role
  carries exactly one label.

Two deliberate narrowings relative to the donor conformance layer this
format was mined from: a case has **no rule scope** — evaluation is
whole-model (in the donor, scope changes nothing about what fires; it
only steers coverage bookkeeping) — and the evaluation date is a whole
**date**, never a datetime.

Division of labour: JSON Schema (gate 4) checks structure only.
Cross-item invariants — label uniqueness, role targets naming existing
labels, names resolving against the sibling `ir.json`, calendar
validity of dates beyond the lexical pattern — are the **`ref/` case
loader's** checks, fail-fast, like the verifier's for the IR itself.

The expectation values are pinned against the normative worked examples
of `spec/RUNTIME-SEMANTICS.md`; machine replay of the cases is the
reference interpreter's oracle guard and lands with `ref/`.
