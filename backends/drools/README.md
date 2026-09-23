# backends/drools — the Drools backend (IR consumer)

The Drools leg of the uchar differential square. It **consumes the uchar IR**: it loads a
golden's `ir.json` off the wire — exactly as `ref/` and `backends/clingo` do — builds a
Drools `KieBase` through the ported exec-model back-end, and replays the `case*.json`
files. The RegelSpraak source-compile path is **never** the gate path (the maintainer
architecture anchor, 2026-08-27); a source path may serve only as an internal comparison
in tests.

## Layout

- `transpiler/`, `transpiler/model/`, `transpiler/kern/` — the RegelSpraak → Drools
  exec-model transpiler (the production lowering), ported from the reference toolchain.
- `runtime/` — the production runtime (`RegelSpraakRuntime`, `Breuk`, `Accessors`, …). It
  lives **only here**; `ref/` and `spec/verifier/` share no code with it (gate 1's
  no-share tripwire enforces this).
- `schema/`, `graaf/`, `engine/` — the fact-model contract + bean generation, the rule
  graph, and the fire-layer support (in-memory bean compiler, parameterset loader).
- `loader/IrLoader.java` — **the heart**: the inverse of the frontend's `IrEmitter`. It
  parses `uchar-ir-v1` JSON (the M1 shape) into the frontend IR record types the
  transpiler consumes, with a **loud per-form `Refusal`** for every form the v0 backend
  does not carry (another `format`, an unknown section of the root, the model or the
  rules, an unknown `Expr`/`Conditie`/`Datatype` node — a v1 struct/list/Let/fold, …).
- `cases/CaseRunner.java`, `cases/DroolsBackendMain.java` — the case runner and the
  jar-only CLI the gate drives.

The shared IR record types (`model/`, `ir/`, `kern/`) are **not** copied here — the pom
compiles them in-tree from `frontends/regelspraak/src/main/java` (the ANTLR-coupled
`resolver`/`emit` packages are excluded). This is the card's "no second copy".

## Build (the shaded runtime uber-jar — decision V2-B)

The gate runs **jar-only** (the gate-10 pattern). Build the shaded uber-jar once; rebuild
it whenever any backend source or the frontend IR records change:

```sh
mvn -o -f backends/drools/pom.xml package
# -> backends/drools/target/uchar-drools-backend-*-shaded.jar
#    (Main-Class: io.github.yhavinga.regelspraak.cases.DroolsBackendMain)
```

`-o` builds offline against the local Maven repository cache. The `package` phase also runs the
loader round-trip witness (`RoundTripWitnessTest`). `scripts/check-drools.sh` rebuilds the
jar automatically (offline) if it is absent, then runs jar-only.

## Run one golden

```sh
java -jar backends/drools/target/uchar-drools-backend-*-shaded.jar \
  corpus/golden/anchor/jutterswet-jutheffing/ir.json \
  corpus/golden/anchor/jutterswet-jutheffing/case*.json
# PLAYED <n>  (exit 0) | EXCLUDE <reason> (exit 2) | FAIL <detail> (exit 1) | ERROR … (exit 3)
```

## Gate 12

`scripts/check-drools.sh` replays every corpus golden. A golden whose forms the
loader+transpiler carry **must** match the pinned expectations gates 5 (ref) and 6
(clingo) verify — a played pass is the square. Not-carried forms never vanish silently:
per-form auto-refusals print their reason, and a reason-carrying value-divergence list
in the script (the check-noop survivor pattern) names each golden whose fired outcome
differs from ref/clingo and fails when stale.
