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
defaults-producing evolution (the registered v1 path, later card), the
dependency inversion (planned later upgrade per V1-A), ScenarioResolver/
ResolvedScenario, `spec/CONSTRUCTS.md` (serial at merge),
`WAY-OF-WORKING.md`.

Result: landed. `frontends/regelspraak` carries the toolchain's frontend
half on the copy route (the source repo untouched): 2 grammars + 30 Java
files 1-op-1 (the card said 29; the slice's own MINUS-rules yield 30 —
`ir/ResolvedDagsoort.java` postdates the card's count), kern helpers
relocated to `io.github.yhavinga.regelspraak.kern`, `Resolver.java`
trimmed to `--ir`-only, the dangling Parameterset `@link` inlined.
Jar-only Makefile (ANTLR 4.13.2 complete + JUnit 5 standalone, curl into
gitignored `lib/` — a cache, not a source, which is the shape the
self-containedness gate verdraagt: nothing tracked references a machine
or a private repo). Tests: 12 clean, slice-only files ported 1-op-1
(IrEmitterTest; resolver: Typering/KenmerkMatcher/MeervoudMatcher/
BeslistabelDesugar; kern: Literalen/Tijdrekening; plus the equally clean
IrTest, BronTest, ParametersetTest, RationaalTest, RecursieTest) —
130 tests green; BeslistabelTabelTest stayed behind (reads example
fixtures that do not come along), Rangtelwoord/StartpuntTest stayed
behind (import DrlTranspiler), the four card-excluded integration tests
not imported. Gate 10 shown red first (mutated emitter key: 0 identical,
19 diverging, exit 1) then green: 130 tests, 19/19 rgs goldens re-emit
byte-identically. CI job unconditional and blocking (setup-java temurin
21, cached jar downloads); CLAUDE.md gained exactly the gate-10 entry.
All ten gates green at close (1: PASS; 2: 143; 3: 143+161; 4: 365;
5: 343; 6: 343; 7: 308+35; 8: 22; 9: 169 checks + 11 goldens; 10: 130
tests + 19 goldens).

The sweep, complete before the first zone commit: all TOKA comment
references and toka-NN names removed or neutrally reworded (incl. the
TyperingTest comment sweep and its assertion message), the private
working-document names (`RegelSpraak-spec-for-ANTLR-v2.3.0.md`,
`specification/v2.3.0/`, `spec-antlr-04`, `spec-main-133-139`,
`main-spec L325`, a `per CLAUDE.md` pointer) stripped; no machine-local
paths, no private-repo names.

The §-spot check (decision V3-A): the zone carries 308 distinct
§-numbers over 1287 occurrences on 1117 lines (the card's 344 counted
the pre-trim slice incl. the scenario files that stayed behind).
Deterministic sample, every 47th §-bearing line (24 hits), judged
annotation-vs-paraphrase — all 24 are bare annotations or own-words
engineering commentary with a § citation; none is a paraphrased spec
prose block, so nothing was stripped beyond the sweep above. Sample:
RegelSpraak.g4:407 `// §13.3.6.6` (bare tag); g4:590 `// §13.4.2.10`
(bare tag); g4:820 §8.1 Tabel 16 NEGATED-form note (own commentary on
surface syntax the grammar must parse); g4:961 `// §13.4.17.51` (bare);
Lexer:103 `// §13.4.16` (bare); Lexer:185 `// §13.4.2.10` (bare);
Conditie.java:45 "§5.11-5.16 one-sided predicates" (label + own
explanation); Expr.java:187 "must pin EVERY dimension (§3.6)" (own-words
contract + citation); ResolvedVerdeling.java:25 "(§9.7.3, naar-rato
only…)" (own semantics note); RegelSpraakModel.java:21 "the §6" (bare);
RegelSpraakModel.java:569 "the §9.9 machinery" (bare);
BeslistabelDesugar.java:183 diagnostic text citing "(§12)" (own error
message); DefinitionResolver.java:369 "§13.3.6 the Granulariteit…" (own
javadoc); ExpressieBouwer.java:272 "§6.1.2 then rightly demands" (own);
ExpressieBouwer.java:625 "a §7.1 gedurende comparison" (bare);
ExpressieBouwer.java:1029 the "§4.17"/"§4.16" strings in a diagnostic
(annotation in output); ExpressieBouwer.java:1541 "§3.6 the bare name
may be…" (own); RuleChecker.java:491 "§13.4.8 samengestelde
consistentieregel" (label); RuleChecker.java:1038 "§6.8: the deriving
expression must be Datum in dagen" (one-clause own-words contract, plus
a five-word quoted term of art at §9.9 — not a prose block);
RuleChecker.java:1563 "the spec requires a §13.2.3 <geheelgetal>" (own);
RuleChecker.java:1866 "the §8.1.7 rolcheck" (bare); Typering.java:130
"§5 comparison" (banner); IrTest.java:154 "(§3.6 …)" (test note);
RegelSpraak.g4:48 '§9.10 recursie / regelgroep' (heading name inside a
docs pointer).

Deliberately left: the ~20 `docs/grammar-divergences.md` /
`docs/SCENARIOTOETSEN.md` citations remain (not in the card's sweep
list, gate-clean; those documents live in the toolchain — a follow-up
card could relocate or strip them); the scenario token inventory stays
in the grammar (grammar comes 1-op-1) though ScenarioResolver/
ResolvedScenario did not come along; the branch is left unmerged as the
protected-paths proposal (scripts/, .github/, CLAUDE.md).
