# uchar — a normative, engine-neutral IR for machine-executable law

*uchar* is short for **universal characteristic** — Leibniz's *characteristica
universalis*, scaled down from "all human reasoning" to a domain where it can
actually be built: legislation as executable rules.

The idea is an hourglass. Legislation is written in domain-specific languages
(RegelSpraak today, Catala and others tomorrow); each translates into one
intermediate representation (IR) with a well-defined, engine-neutral meaning;
from that IR we generate execution on multiple engines and solvers (Drools
today, Clingo next). *n* languages and *m* engines meet in one waist: *n+m*
translators instead of *n×m*.

**The IR specification is the product.** Parsers, transpilers, and runtimes
are its proof.

```
     RegelSpraak    Catala    …        frontends (characteristica)
           \          |
            \         |
          ┌─────────────────┐
          │     uchar IR    │          the waist (spec/ + corpus/)
          └─────────────────┘
            /         |        \
           /          |         \
       Drools      Clingo    reference  backends (calculus ratiocinator)
                             interpreter
```

## Layout

| Path | Role |
|---|---|
| `spec/` | **The product**: `PLAN.md` (the constitution), `IR-REVIEW.md` (the founding review), `IR-SPEC.md`, `RUNTIME-SEMANTICS.md`, `CONSTRUCTS.md`, the JSON schema, the verifier |
| `corpus/` | The golden corpus — rights-free by construction (the Jutterswet casus) — plus the negatief-corpus |
| `ref/` | The reference interpreter: the executable definition of the semantics (Phase 2) |
| `backends/` | Engine generators; first `clingo/` (Phase 3) |

## Status

Pre-Phase-0. Start with `spec/PLAN.md`; the review that motivates it is
`spec/IR-REVIEW.md`. The first working frontend/backend pair —
`regelspraak-java` (RegelSpraak parser/resolver, typed IR, two Drools
back-ends, decision service) — lives in its own repository and consumes this
one through a pinned `SPEC_REV`.

## Provenance of the casus

The Jutterswet (island of Scholde) is original, rights-free fiction by the
maintainer, from the *calculemus* course material. It mirrors, article by
article, the British Nationality Act experiment (Sergot, Sadri, Kowalski,
Kriwaczek, Hammond & Cory, *CACM* 29(5), 1986) — which gives the corpus four
decades of logic-programming and ASP literature to compare against.

## License

Not yet chosen — tracked as an open decision in `spec/PLAN.md` §9. Until a
license lands: all rights reserved.
