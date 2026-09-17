# uchar — a normative, engine-neutral IR for machine-executable law

*uchar* is short for **universal characteristic** — Leibniz's *characteristica
universalis*, scaled down from "all human reasoning" to a domain where it can
actually be built: legislation as executable rules.

The architecture is an hourglass. Legislation is written in domain-specific
languages (RegelSpraak, Catala, F-logic today); each translates into one
intermediate representation (IR) with a well-defined, engine-neutral meaning;
from that IR we execute on multiple engines and solvers (Drools and Clingo
today, plus a reference interpreter). *n* languages and *m* engines meet in
one waist: *n+m* translators instead of *n×m*.

**The IR specification is the product.** Parsers, transpilers, and runtimes
are its proof.

```
   RegelSpraak   Catala   F-logic       frontends (characteristica)
          \        |        /
           \       |       /
          ┌─────────────────┐
          │     uchar IR    │           the waist (spec/ + corpus/)
          └─────────────────┘
           /       |       \
          /        |        \
      Drools    Clingo   reference      backends (calculus ratiocinator)
                         interpreter
```

The three backends are independent implementations of the same
specification, and that is the point: every corpus case must produce the
same hand-computed answer in all three (the differential square,
ref ≡ Clingo ≡ Drools). Agreement between backends is never accepted as
truth by itself — a hand-written anchor subset pins absolute expected
values.

## Layout

| Path | Role |
|---|---|
| `spec/` | **The product**: `PLAN.md` (the constitution), `IR-REVIEW.md` (the founding review), `IR-SPEC.md` (the normative IR spec, every MUST carrying a `WF-###` id), `RUNTIME-SEMANTICS.md` (evaluation semantics with worked examples that run verbatim as tests), `CONSTRUCTS.md` (the v0→v1 delta register), the JSON schemas, the OCaml verifier |
| `corpus/` | `golden/` (cases with expected values, rights-free by construction — the Jutterswet casus), `negatief/` (documents the verifier must reject, one per WF rule), `rood/` (cases that must fail observably in every executor) |
| `ref/` | The reference interpreter (OCaml): the executable definition of the semantics |
| `backends/` | `clingo/` (translation to ASP-Core-2) and `drools/` (an IR loader plus two transpiler branches — DRL text for fast development and testing, the executable model for compiling services — over a shared runtime) |
| `frontends/` | `regelspraak/` (Java parser/resolver, emits the IR), `catala/` (a Catala plugin), `flogic/` (OCaml, a second independent source language) |
| `scripts/` | The gate set: fourteen checks that run on every change, locally and in CI |
| `toka/` | **Temporary.** Third-party © parity fixtures (Belastingdienst TOKA material) used to prove the RegelSpraak frontend byte-identical to the original toolchain. Not covered by the licenses below; the repository stays private while this zone exists |

## Status

The spec regime is in force and the corpus exercises it end to end. The IR
covers the RegelSpraak core: an object model with single-supertype
inheritance and chain-closed member resolution, typed attributes and
characteristics with units, dimensions and timelines, fact types and roles,
rules with conditions including quantified folds over linked instance sets
(ALLE / GEEN / TEN MINSTE / TEN HOOGSTE / PRECIES n, with die/dat
subselection), aggregations, prioritized defaults, distribution rules, and
temporal arithmetic.

Fourteen gates guard it. Structural: self-containedness, schema validation
with byte-identical round-trip, the verifier over golden and negatief
corpora, case-file validation. Semantic: the reference interpreter replays
every case exactly; Clingo and Drools must reproduce the same pinned
answers; a no-op guard proves an identity transpiler fails; the red corpus
must fail in every executor. Per frontend: Catala, RegelSpraak and F-logic
sources re-emit their committed IR byte-identically. Plus an
annotation-indifference harness (consumers must derive units from
declarations, not wire annotations) and the temporary TOKA parity gate.
The gate set grows monotonically; a green subset is not green, an empty
input set is a failure, and counts carry explicit floors so shrinkage
cannot pass silently.

Start with `spec/PLAN.md`; the review that motivates it is
`spec/IR-REVIEW.md`. Day-to-day process is `WAY-OF-WORKING.md`; work items
live in `backlog/`.

The first consumer is `regelspraak-java` (separate repository): it vendors
the RegelSpraak frontend at a pinned `SPEC_REV`, byte-identical, and builds
an HTTP service, scenario tests and a CLI on top. Changes flow spec-first:
a delta lands here (spec + corpus + schema), then a pin bump lands there.

## Provenance of the casus

The Jutterswet (island of Scholde) is original, rights-free fiction by the
maintainer. It mirrors, article by article, the British Nationality Act
experiment (Sergot, Sadri, Kowalski, Kriwaczek, Hammond & Cory, *CACM*
29(5), 1986) — which gives the corpus four decades of logic-programming and
ASP literature to compare against. The same lineage picked the backend:
Clingo was chosen as the semantically alien executor because the BNA
tradition is logic programming, so the corpus and the ASP backend answer
to one body of literature.

## License

- **Code** — the verifier (`spec/verifier/`), the JSON schemas
  (`spec/schema/`, functional artifacts implementers embed), the reference
  interpreter (`ref/`), engine backends (`backends/`), frontends
  (`frontends/`), and the gate scripts (`scripts/`): Apache License 2.0,
  see `LICENSE`.
- **Text** — the specification documents (the Markdown under `spec/`) and
  the corpus material under `corpus/` (casus text, RegelSpraak sources,
  and their IR and case data): Creative Commons Attribution 4.0
  International, see `LICENSE-CC-BY-4.0`.
- **Exception** — `toka/` contains third-party © material (see Layout) and
  is covered by neither license.
