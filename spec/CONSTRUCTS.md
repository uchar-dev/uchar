# CONSTRUCTS — the scope matrix

*Status: seeded, not complete. Phase 0 deliverable (`PLAN.md` §6): one row per
construct, mined from four sources — the `regelspraak-java` IR and its
coverage manifest (including the `Onopgelost` inventory), Catala's default
calculus, LegalRuleML's requirements list, and optionally OpenFisca and DMN.
This matrix forces the v1 definition of "universal" before anything is
serialized, and its delta column carries the v0→v1 divergences that double as
the Phase 0–4 roadmap.*

Columns:

- **Verdict (v1)** — `core` | `dialect` (RegelSpraak-specific, lowers to
  core) | `sugar` (desugars in the frontend) | `later(…)` | `non-goal`.
- **Kernel or sugar?** — the GHC-Core question: does the construct need its
  own kernel node, or is it expressible in existing ones?
- **v0→v1 delta** — what changes relative to the faithful dump of today's
  `regelspraak-java` IR; "—" = mirrored as-is.

| # | Construct | Origin | Verdict (v1) | Kernel or sugar? | v0→v1 delta | Notes |
|---|-----------|--------|--------------|------------------|-------------|-------|
| 1 | Typed expression trees (arithmetic, comparison, functions, aggregation) | RegelSpraak §13.4.16/.17 | core | kernel | — | mirrored |
| 2 | Assignment rules incl. versions with validity windows | §13.4.4/.9, §13.4.2.5 | core | kernel | merge BEREKENING/GELIJKSTELLING into one node + presentation tag | `PLAN.md` §9 (4) |
| 3 | Source references | — | core | kernel | `String bron` → `Bron(file, line, column, snippet)`; `// Bron:` statute annotations pass the lexer | Phase 0 |
| 4 | Fact types and role navigation | §13.3.9, §13.4.1 | core | kernel | bean-field names (`zetVeld`/`joinVeld`/`scopeVeld`/`backref`) → symbolic `RolBinding(feittype, rol)` | Phase 3 forcing move |
| 5 | Distant role membership (far-side exists) | §8.1.7 | core | kernel | drop the top-level-only restriction (a Drools pattern artefact) | Phase 3 |
| 6 | Negation over derived characteristics | §8.1.8 | core | kernel | salience −1 → declared stratification requirement (WF rule) | Phases 3–4 |
| 7 | Initialisation-only-if-empty | §9.6 | core | kernel | leeg-guard → explicit defeasible default | Phase 4 |
| 8 | Exception trees with priorities + conflict detection | Catala default calculus | later(Phase 4) | kernel | new: adopted default model; conflicts → the §9.5 inconsistency channel | the hard dependency for Catala interop |
| 9 | Scopes with in/out variables, rebound subscope inputs | Catala | later(Phase 5) | sugar (over object types + creation + navigation) | — | real design work |
| 10 | General folds / function values | Catala | non-goal (v1) | — | — | subset stance |
| 11 | Timelines (granularity, variable starting points) + parameter vintages | §13.3.6, §9.9 | core | kernel | temporality into the type system (timeline-of-element), out of the side tables | review improvement 5 |
| 12 | Second time axis (knowledge time / retroactivity) | LegalRuleML; review | later(v2) | kernel | v1 names the single axis and the extension point | `PLAN.md` §9 (2) |
| 13 | Jurisdiction | LegalRuleML | non-goal (v1) | — | — | standing LegalRuleML test |
| 14 | Rule groups / recursion marker | §9.10 (documented extension) | dialect | — | stays out of core | `PLAN.md` §9 (5) |
| 15 | Decision tables | §12 | sugar | — | already desugared by the frontend | keep |
| 16 | `Onopgelost` (honest inexpressibility) | `regelspraak-java` | core | kernel | specified as a permanent part of the contract | `PLAN.md` §9 (3) |
| 17 | Allocation (equal / pro-rata / ordered + maximum, rounding, remainder) | §9.7, §13.4.10 | core | kernel | semantics as recurrence equations (`RUNTIME-SEMANTICS.md` ch. 4), not helper-defined | |
| 18 | Empty-value discipline | §6 / divergences | core | — | reopened as a design decision before freezing | `PLAN.md` §9 (7) |

**TODO (Phase 0):** complete the inventory — every `Resolved*` form and every
`Expr`/`Conditie` node from `regelspraak-java`; every MARKER/SKIP reason in
the coverage manifest; the remaining LegalRuleML rows; DMN hit policies.
