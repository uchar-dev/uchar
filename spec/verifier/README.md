# IR verifier

Machine enforcement of the well-formedness rules (`WF-###`) of
`spec/IR-SPEC.md`, over IR documents in the `uchar-ir-v1` wire format, in
its L5 shape (`spec/schema/uchar-ir-v1.schema.json`).

The project is an OCaml dune build in two parts:

- `lib/` — a typed AST of the wire format (`ast.ml`) with a strict
  loader (`loader.ml`, yojson-based: unknown or missing fields are load
  errors, never silently dropped) and the WF checks (`checks.ml`). The
  library exists to be reused: the Phase-2 reference interpreter (`ref/`),
  the ASP backend and the two OCaml frontends link the same AST, and the
  first two the same loader, so spec and executors cannot drift apart at
  the parsing layer. Since M1 the AST keeps the in-memory shape its readers
  were written against: the model lists as name-keyed association lists,
  and an object type's members as name-keyed facet tables that the loader
  builds from the Attribute, Characteristic and TimelineFacet nodes (the
  header of `ast.ml` says what that implies). Since L3 the datatype keeps
  its v0 constructors as well: the loader reads a `Number` whose unit is `%`
  as a percentage, any other `Number` as a plain number, and a `Date` by its
  precision, so every reader keeps its typing families; a unit's factor is
  an exact rational in memory too. Since L5 the wire spells every node,
  key and enumeration value with its IR-SPEC name while the AST keeps its
  v0 constructors and fields: the loader reads the one onto the other, the
  enumeration values through one translation table (`spelling.ml`), which
  the two OCaml emitters use to write them. The library is still named
  `uchar-ir-v0`; renaming it is a change to every reader.
- `bin/` — the `verifier` executable: `verifier <ir.json>` exits 0 when
  the document is well-formed, non-zero with one `WF-### FAIL <path>:
  <reason>` line per violation otherwise. `<path>` is a JSON pointer into
  the document (`/model/objectTypes/0/attributes/1/timeline/count`,
  `/rules/rules/2/versions/0/condition`), as a load error's is; below a
  node a check may add a segment of its own reading, such as a struct
  field by its name.

Which rules are implemented, and which are still open, is tracked in one
place only: the WF index at the end of `spec/IR-SPEC.md`. Checks are
written as exhaustive matches over the AST's node kinds, so extending the
format without deciding its checks fails at compile time.

Build and run (needs OCaml with dune and yojson, e.g. `opam install dune
yojson`):

```
dune build --root spec/verifier
spec/verifier/_build/default/bin/verifier.exe corpus/golden/<case>/ir.json
```

Gate 3 (`scripts/check-verifier.sh`) runs the verifier over the corpus:
every `corpus/golden/**/ir.json` must be clean, every
`corpus/negatief/<wf###-slug>/ir.json` must be rejected, citing the rule
its directory name declares (incidental extra violations are allowed; a
case that passes, or fails only for other rules, turns the gate red).
