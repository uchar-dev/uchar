# IR verifier

Machine enforcement of the well-formedness rules (`WF-###`) of
`spec/IR-SPEC.md`, over IR documents in the `regelspraak-ir-v0` wire
format (`spec/schema/regelspraak-ir-v0.schema.json`).

The project is an OCaml dune build in two parts:

- `lib/` — a typed AST of the wire format (`ast.ml`) with a strict
  loader (`loader.ml`, yojson-based: unknown or missing fields are load
  errors, never silently dropped) and the WF checks (`checks.ml`). The
  library exists to be reused: the Phase-2 reference interpreter (`ref/`)
  links the same AST and loader, so spec and interpreter cannot drift
  apart at the parsing layer.
- `bin/` — the `verifier` executable: `verifier <ir.json>` exits 0 when
  the document is well-formed, non-zero with one `WF-### FAIL <path>:
  <reason>` line per violation otherwise.

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
