# uchar

The **uchar IR**: a normative, engine-neutral intermediate representation
for machine-executable law — plus its corpus, reference interpreter, and
engine backends. **The specification is the product; implementations are
its proof.** The programme, phases, and rationale live in `spec/PLAN.md`.

```
spec/     the plan, the IR specification, runtime semantics,
          construct matrix, JSON schemas, and the OCaml verifier
corpus/   golden corpus (source → IR → evaluation cases), negatief
          corpus, and the rights-free Jutterswet casus material
ref/      the reference interpreter — the executable definition of
          the runtime semantics
backends/ engine backends (Clingo first; Phase 3)
```

Every change runs the gate set (`scripts/check-*.sh`, mirrored in CI):
self-containedness, schema + round-trip, the verifier over golden and
negatief corpora, case-file validation, and the oracle — the reference
interpreter replaying every corpus case exactly.

## License

- **Code** — the verifier (`spec/verifier/`), the reference interpreter
  (`ref/`), backends (`backends/`), and the gate scripts (`scripts/`):
  Apache License 2.0, see `LICENSE`.
- **Specification and corpus text** — the documents under `spec/` and
  the corpus material under `corpus/`: Creative Commons Attribution 4.0
  International, see `LICENSE-CC-BY-4.0`.
