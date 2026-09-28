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
  parses `uchar-ir-v1` JSON in its L5 shape — every node, key and list under `rules` in
  its IR-SPEC name, every enumeration value read through the one translation table
  `ir/Spelling.java` that the emitter writes with — into the frontend IR record types the
  transpiler consumes, which keep their v0 form. A form the backend does not carry
  (another `format`, the `functionDefs` section, a node of the schema's expression or
  datatype union with no record here — a v1 struct/list/match, a temporal read, …) is a
  **loud per-form `Refusal`**; a document that is not well-formed — a node name outside
  the schema's union at its position, a v0 node name among them, or a v0 key, list or
  enumeration value — is a `Documentfout` naming its JSON pointer, in the shared OCaml
  loader's words, never a `Refusal`.
- `cases/CaseRunner.java`, `cases/DroolsBackendMain.java` — the case runner and the
  jar-only CLI the gate drives.

The shared IR record types (`model/`, `ir/`, `kern/`) are **not** copied here — the pom
compiles them in-tree from `frontends/regelspraak/src/main/java` (the ANTLR-coupled
`resolver`/`emit` packages are excluded). This is the card's "no second copy".

## The entry for consumers

`cases/DroolsBackend.java` is the backend's public entry, and the one a consumer builds with — the toolchain
distribution (CLI, decision service, scenario runner) that builds this repository as a git submodule.
`DroolsBackend.build(document, branch)` takes a loaded `uchar-ir-v1` document (a `JsonNode`, or a path) and
returns a `Build` record: the model, the bean class loader, the transpiler of the chosen branch (`EXEC_MODEL` or
`DRL_TEXT`), its coverage manifest and the `KieBase`. It is the whole prelude of the case runner, which builds
every golden through it, so gate 12 fires the KieBase a consumer gets, and it refuses in the replay's
sentences (`IrLoader.Refusal`, `IrLoader.Documentfout`, `CaseRunner.Excluded`). Each transpiler has one build,
and it takes every side table the loader returns beside the model: a build that left one out still produced a
KieBase — a defaults family firing nowhere, a navigated read answered as the subject's own — and no marker said
so, which is why there is no shorter way in.

## Build (the shaded runtime uber-jar — decision V2-B)

The gate runs **jar-only** (the gate-10 pattern). Build the shaded uber-jar once; rebuild
it whenever any backend source or the frontend IR records change:

```sh
mvn -o -f backends/drools/pom.xml package
# -> backends/drools/target/uchar-drools-backend-*-shaded.jar
#    (Main-Class: dev.uchar.regelspraak.cases.DroolsBackendMain)
```

`-o` builds offline against the local Maven repository cache. The `package` phase also runs the
backend's JUnit suite, the loader round-trip witness (`RoundTripWitnessTest`) among it.
`scripts/check-drools.sh` runs that same offline `package` on every run, whether or not a jar
is there — the suite must pass with no skips and at least the test count the gate's floor
pins, and the replay never runs a jar older than the source — and then replays jar-only.

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

## The rule graph and gate 15

`graaf/RegelGraaf` draws which form of the rule layer writes and which reads which attribute or characteristic:
the rules, object creations, allocations, consistencies, uniquenesses, fact creations and defaults groups of
IR-SPEC ch. 5.1, a member keyed on the type that declares it (`ProducentConsument.elementVan`), so a write on a
supertype and a read on a subtype meet on one node. It is built from the records the loader rebuilds from
`ir.json`, and each route to a model supplies what rides beside it: the defaults groups (`model.prioritizedDefaults`
of a resolver's model, `Loaded.prioritizedDefaults()` beside a loaded one) and the type a navigated characteristic
check reaches (`ProducentConsument.bronNavigatie(model)` from the role the record names,
`ProducentConsument.wireNavigatie(loaded)` from the binding beside the model). `scripts/check-graaf.sh` holds it:
over every golden the loader reads, the graph's dependencies (`GraafProjectie`) must equal the verifier's ch. 5
edges (`verifier --dependency-edges`) over an attribute or characteristic node, whatever forms they join, and the
graph of every golden with a `bron.rgs` must render the same from the source as from the wire. Outside the
comparison, and reported per class by the gate: the edges over a population, a relation or a channel, nodes the
graph does not draw, and the edges of a golden the loader refuses. Two readings differ by design, recorded in the
gate beside its list of deliberate differences: keyed on the declaring type, the graph draws a dependency between
two sibling subtypes sharing an inherited member, and from an object creation assigning an inherited member to a
read of it on a strict subtype of the created type, where ch. 5.1 closes the write over the instances it reaches.
The tests that need the frontend's resolver live under `src/bronroute/`, outside this build, which carries no
parser; the gate compiles them against the frontend's classes and this jar.

## Recorded: the DRL-text branch answers where the others refuse

One corner, measured 2026-09-25 and deliberately not built. A computing rule and a conditional
initialisation write one target (outside the ch. 7.7 desugaring, RUNTIME-SEMANTICS ch. 5.4), the
computing rule's condition holds and its value evaluates leeg. In v0 the computing rule erases what the
initialisation writes and the initialisation writes it again: `ref` faults on its pass budget
(`no fixpoint within 100 passes — divergence is a fault (ch. 5.7)`), the ASP backend derives its
`viol/2` witness for the same body, and the exec-model branch reaches the shared fire cap
(`geen fixpunt (kringverwijzing?)`). The DRL-text branch answers the initialisation's value instead
(`verwacht leeg, werkelijk 25.00`): its computing rule is property-reactive to what its condition and
value read (`@watch`), not to its own target, so after the initialisation writes nothing re-fires the
erase and the session stops before the cap. No corpus case can hold the corner — `ref` faults, so it is
no golden, and the red zone pins `ref` and clingo only.

## Recorded: a condition that reads through a role

The exec-model branch tests a condition that reads through a to-one navigation — an attribute or a characteristic
of the reached object — after a far-side join on that object (`ExecModelTranspiler.regelVoorwaarde`), so a value
another rule derives there later is seen; the companions that mirror a rule's condition, the object creations and
the fact creations take the same test (golden `jutterswet-meldingsketen`, witness `GenavigeerdeVoorwaardeWitnessTest`).
Three limits remain, measured 2026-09-28 with `ref` and clingo as the arbiters. The DRL-text branch still writes such
a test into the subject's pattern, so there the rule never fires on a value derived later on the reached object, or
fires on the value the object held at insertion (`anchor/jutterswet-kustwacht`, pinned in gate 12's DRL list). A test
that holds on an empty value, such as a leeg test on a derived attribute, fires on the transient leeg when its rule
is declared before the rule that writes the value, and the write stays; this holds for an own and a navigated operand
alike (IR-SPEC ch. 5.5, the order-dependent form), while a negated characteristic check is stratified and is not
affected. A condition over two or more hops is refused by name, except the whole-period leeg and gevuld test.
