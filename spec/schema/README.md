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
