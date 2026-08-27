# frontends/regelspraak — the RegelSpraak frontend joins the monorepo (copy route)

Goal: maintainer decisions 2026-08-27 (exploration
wf_f70c043c-51e, all four ratification questions answered A). Take over
the proven-clean frontend half of the toolchain (path supplied at
session start: /Users/yeb/Developer/yhavinga/regelspraak-java, main ≥
b4ba108, READ-ONLY — the copy route touches nothing there) as
`frontends/regelspraak`:

1. **The slice** (verified by isolation compile): `grammar/*.g4` (2
   files), `ir/*` MINUS ResolvedScenario, `model/*`, `resolver/*` MINUS
   ScenarioResolver, `emit/IrEmitter.java`, and the two pure helpers
   `transpiler/kern/{Literalen,Tijdrekening}.java` (relocate to a
   frontend-own package) — 29 Java files + 2 grammars 1-op-1. Trim
   `resolver/Resolver.java` to `--ir`-only (drop drlModus/factsModus/
   graafModus, ±50 LOC); fix the dangling Parameterset javadoc @link.
2. **The sweep** (self-containedness + rights, the zone is gated from
   its first commit): remove the ~18 TOKA comment references in the
   source and the 16 toka-NN case names in the grammar comments; NO
   machine-local paths, NO private-repo names. The 344 bare §-number
   references to the RegelSpraak spec STAY (decision V3-A) — but first
   run the required spot check: sample ~20 of them and confirm they are
   bare annotations, not paraphrased spec prose; quote the sample in
   the Result. The spec markdown itself does NOT come along.
3. **Build**: jar-only Makefile in `frontends/regelspraak` (ANTLR
   4.13.2 complete jar + JUnit 5 standalone, the toolchain's pattern;
   parser generated build-time into a gitignored gen/), JDK 21+.
4. **Tests** (decision V2+V5-A, narrow): port the clean unit tests
   1-op-1 (IrEmitterTest as the wire layer, plus the TOKA-free
   resolver/kern tests — verify each is clean before porting;
   TyperingTest only after its comment sweep); do NOT import
   CasusTest/ModelTest/RuleCheckTest/ScenarioParseTest.
5. **Gate 10** (`scripts/check-regelspraak.sh`): build the frontend,
   run its JUnit suite, then re-emit every `corpus/golden/**/bron.rgs`
   with `--ir` and require byte-identity against the committed ir.json;
   an empty golden set or zero tests run is a failure, never silently
   green. Show it red first (e.g. a deliberately mutated emitter or
   missing build), then green.
6. **CI** (decision V4-A): an unconditional, blocking gates.yml job
   (actions/setup-java, cached ANTLR jar download) — unlike gate 9,
   hosted CI must catch a frontend regression.
7. **CLAUDE.md**: add gate 10 to the gate list (an invariant change —
   allowed for exactly that line).

Acceptance:
- Gate 10 red-first shown, then green: 19/19 goldens byte-identical
  re-emitted from uchar's own frontend, JUnit suite green with a
  non-zero test count.
- All ten gates green (the nine existing untouched); check-self-
  contained covers the new zone content (no TOKA, no private-repo
  names, no local paths).
- The §-spot-check documented in the Result.

May touch: `frontends/regelspraak/**` (new), `scripts/
check-regelspraak.sh` (new), `.github/workflows/gates.yml`,
`CLAUDE.md` (the gate list line only).
Out of scope: any toolchain change (copy route — read-only there), the
ladder-producing evolution (the registered v1 path, later card), the
dependency inversion (planned later upgrade per V1-A), ScenarioResolver/
ResolvedScenario, `spec/CONSTRUCTS.md` (serial at merge),
`WAY-OF-WORKING.md`.
