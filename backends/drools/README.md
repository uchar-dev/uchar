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
Two limits remain, measured 2026-09-28 with `ref` and clingo as the arbiters. The DRL-text branch still writes such
a test into the subject's pattern, so there the rule never fires on a value derived later on the reached object, or
fires on the value the object held at insertion (`anchor/jutterswet-kustwacht`, pinned in gate 12's DRL list). A test
that holds on an empty value, such as a leeg test on a derived attribute, fires on the transient leeg when its rule
is declared before the rule that writes the value, and the write stays; this holds for an own operand, one read over a
role and one over a chain of roles alike (IR-SPEC ch. 5.5, the order-dependent form), while a negated characteristic
check is stratified and is not affected. The same test inside a fold, in the subselection of a fold or the matrix of a
quantifier, no longer fires on the transient in the exec-model branch since 2026-09-30: such a rule is placed below the
writers of what the test reads (the section on a fold whose subselection tests a value derived later); a test outside a
fold keeps this limit.
A condition that reads through a chain of two or more to-one roles takes a far-side join on every object the chain
passes, in order, and its test goes on the subject bound again after the last join (golden `jutterswet-havenketen`,
witness `KetenVoorwaardeWitnessTest`, measured 2026-10-01): a relation in the middle of the chain that a fact creation
sets while the rules run is seen, which a join on the reached object alone does not see; a rule's value that reads a
chain takes the same joins. A comparison, a leeg or gevuld test and a date computation lower over a chain as over one
role, and a navigated operand of a date computation — a date plus or minus a duration, also a duration parameter, one
role away or more — lowers as it does without navigation. A chain lowers only where the condition's joins cover it: in
the matrix or the subselection of a fold it keeps the fold's named refusal, and a consistency rule's assertion over a
chain is refused (`schending buiten Phase-D scope`) where one over one role plays. A comparison this branch does not
lower over one role either, such as an own number on the left of a navigated one or a date computation against a date
parameter, is refused over a chain in the same words. A characteristic check over two roles does not reach this branch:
the wire's navigated check carries one to-one hop (IR-SPEC ch. 4.5), and the frontend leaves the check unresolved,
which ref and clingo refuse too. The DRL-text branch refuses a condition over two or more roles with its marker
`TODO voorwaarde`, and joins a chain in a rule's value at the reached object only, so there a value over a chain whose
middle relation a fact creation sets later stays leeg.

## Recorded: a defaults justification that counts or reads further than one role, and a consequence that folds

The exec-model branch lowers a justification that is no subject-local predicate member by member
(`ExecModelTranspiler.rechtvaardiging`): an aggregation (count, sum, minimum, maximum, with or without a filter)
becomes an accumulate on the group's reduce-rule whose result the justification reads when the rule fires, and a
comparison or a leeg/gevuld test whose operand reads a chain of roles or a numeric parameter reads that operand as a
rule's value does. A consequence that folds over a set source — the earliest or the latest date, a count, a sum, a
minimum or a maximum, on its own, inside a computation or as an operand of a listed fold — lowers as a rule's value
does (the section on a fold over a set source inside a rule's value): each fold becomes an accumulate on the same
reduce-rule, and the consequence reads its result from a slot (`ExecModelTranspiler.compileDefault`, `vouwslot`), in
the base and the exceptions of a tree and in the group the opgave marker generates. A group takes the lowest salience
among the producers of what it reads, on its own type, beyond a role or in a fold, and the producers they wait on in
turn, and is added to the model after the groups among them (`groepVolgorde`), so it decides after them
(RUNTIME-SEMANTICS ch. 7.4); an aggregation's accumulate follows its elements. It is not placed strictly below them; a
rule that reads the group's characteristic under negation is placed strictly below the group instead, since the
stratification of the rules does not see a group as a producer and an ordinary rule fires first in a band it shares
with a group. A group carries no far-side join: with the order in place none of the measured forms needed one, and a
producer outside the rules and the groups (an allocation) could not be measured, see the open points of the card.
Goldens `jutterswet-premiestaffel` and `jutterswet-sluitdag-gemeld`, witnesses `DefaultsRechtvaardigingWitnessTest` and
`DefaultsConsequentieVouwWitnessTest`. Still refused, each by name with its condition: a quantifier fold, an
aggregation inside an arithmetic expression, a parameter that is not a number, a period comparison and a rule-version
predicate in a justification. A consequence that does not lower is refused with the form and the consequence
(`consequentie niet vertaald`), such as a fold over a relation whose two roles are both plural (§13.3.9), and a group
whose aggregations, folds and globals bind more variables than the consequence takes (nine, the subject among them) by
that count. The DRL-text branch has no defaults lowering at all.

## Recorded: a condition that puts no test on the subject

The exec-model branch lowers a quantifier fold over a role, an aggregation comparison and a role check whose link is
stored on the other end into rule items after the subject pattern (an accumulate with its test, an exists), so such a
condition puts no test on that pattern, and a pattern without a test reacts to no update of the subject. The rule fired
once, when the condition first held, and did not read again an operand of its value, or an own operand the condition
compares in its accumulate, that a rule declared after it or a defaults group wrote later: the value stayed empty, or
the empty operand counted as 0 (RUNTIME-SEMANTICS ch. 1.2), where `ref` and clingo answer the fixpoint.
`ExecModelTranspiler.regel` now gives such a pattern the opaque always-true test (`zonderToets`), so the rule is
evaluated again on every update of the subject (RUNTIME-SEMANTICS ch. 5.1); golden `jutterswet-sluitdag`, witness
`KwantorvoorwaardeOperandWitnessTest`. A fold over a characteristic was affected less often: the stratification reads
its matrix as a negated read and places the rule below band 0, so a writer in band 0 fired first, and only a writer in
the rule's own band that fired after it was missed. Three limits were measured 2026-09-30 with `ref` and clingo as
the arbiters, and the exec-model branch has closed the first since the same day. A fold whose verdict holds while a
derived value is still empty, such as "de bergdag van geen van de vondsten van de Berging gevuld is", fired on that
empty value when its rule was declared before the writer, and the write stayed: the leeg test of the section on a
condition that reads through a role, here in the matrix of a fold; the branch now places such a rule below the writer
(the section on a fold whose subselection tests a value derived later). A fold under a counting composite ("ten
minste één van de volgende voorwaarden") is refused in both branches, in the exec-model branch with the generic
sentence `voorwaarde buiten Phase-B scope`. The DRL-text branch, whose rules watch what they read, plays these forms
except two, and refuses those with a defaults group: an aggregation comparison whose own threshold is derived later
does not fire, and an operand one role away under a fold that holds at insertion is read before its writer ran.

## Recorded: a fold over a set source inside a rule's value

The exec-model branch lowers a fold over a set source anywhere in a rule's value — the earliest or the latest date
over a role ("de eerste van de vinddag van alle vondsten van de Jutter"), with a condition on the elements or over a
path of roles, and a count, sum, minimum or maximum as an operand of a computation — to an accumulate beside the
subject pattern (`ExecModelTranspiler.vouwslot`). The consequence hands each fold's result to the value through the
map `scalarWaarde` already threads, so every operator of the scalar value applies around it: date arithmetic, a
duration between two folds, a percentage, a clamp, a navigated operand, a Let, and the empty operands of
RUNTIME-SEMANTICS ch. 1.2. An ordering fold over a date — eerste, laatste, and the minimum and maximum a hand-written
document spells — orders its members through `Min`/`MaxAccumulateFunction` over a `Comparable` (ch. 1.2e), and an
ordering fold inside a value binds a count, so an empty source reads leeg instead of leaving the rule unfired. Golden
`jutterswet-vroegste-vondst`, and `jutterswet-vondstvenster` and `jutterswet-vondstbereik` play since; witnesses
`DatumvouwOverRolWitnessTest` and `DatumSetvouwWitnessTest`. Until 2026-09-30 the branch refused the date folds
(`waarde buiten scope`, and the member-type sentence of `kern.Vouwwaarde` for the minimum and maximum) and lowered a
fold inside a computation through an evaluator of its own, `lowerVal`, which refused date arithmetic, a navigated read
and a clamp, and answered leeg for a percentage of a sum and for the minimum or maximum of an empty source plus 1,
where `ref` and clingo answer. Three limits remain, measured 2026-09-30 with `ref` and clingo as the arbiters. An
ordering fold over a date in a condition is refused by name (`voorwaarde met een ordenende vouw over een datumbron`):
a condition compares the accumulate's result as a number. A fold beside a division or a root under one Let or
IfThenElse (ch. 4.10) is refused by name, since the path-probe companion of such a §6.5 or §6.6 site evaluates that
node without the rule's accumulates; a fold bound by a Let alone plays. An object creation still lowers a fold in a
toekenning through `lowerVal`: a percentage of a sum answers leeg, and over an empty source the minimum plus 1 creates
no object. The DRL-text branch markers the eerste/laatste fold over a set (`TODO aggregatie EERSTE`), refuses the
minimum and maximum over a date with the `Vouwwaarde` sentence, which names that branch, and answers leeg for the
minimum or maximum of an empty source plus 1 and for a date plus the maximum of an empty source.

## Recorded: a fold whose subselection tests a value derived later

A fold whose subselection ("die aan de volgende voorwaarde voldoen: zijn afgeleide dag is leeg") tests a value that
another rule or a defaults group writes later. `ref` and clingo evaluate the fold after that writer where the
subselection reads the value negatively (a leeg or gevuld test, a negated characteristic, the range of a quantifier
other than ten minste één; IR-SPEC ch. 5.5, WF-046), and reach the fixpoint of a comparison by iterating. The exec-model
branch answered from the value as it stood before the writer, measured 2026-09-30. In a rule's value the fold is an
accumulate that sees an element leave its subselection, but Drools passes that revision on with the subject's last
propagation context, and NO_LOOP drops an activation whose last context came from the rule's own consequence: when the
rule's own write was the subject's last change, the value kept the maximum that still counted the element (two roles
away, where the fold's members have the subject's type, the freshness expr of that own read made the subject pattern see
the write; one role away an own condition did). An ordering fold whose subselection emptied read leeg, which decides
nothing on a standing target (RUNTIME-SEMANTICS ch. 7.8), so the value over the transient source stayed, over a case
input too, and a characteristic granted on it stayed. In a condition the rule fired on the transient, and a rule does
not write back. Now a rule whose value holds a fold with a subselection carries no NO_LOOP (`ExecModelTranspiler.regel`;
the idempotent `werkBij` ends the recompute after one more firing), and `groepVolgorde` places a rule below the band of
every rule and group that writes what a fold in its condition or value reads negatively, in the verifier's polarity
(`vouwOntkend`). Each half alone leaves measured forms wrong: the order alone a comparison whose element leaves the
subselection after the rule fired (an initialisatie, then a berekening), the revision alone the condition, the emptied
source and what reads the value. Golden `jutterswet-zwaarste-vondst`, witness `VouwfilterLatereAfleidingWitnessTest`.
Limits that remain, measured 2026-09-30 with `ref` and clingo as the arbiters: a leeg or gevuld test outside a fold, see
the section on a condition that reads through a role; a quantifier over two roles with a subselection, which the loader
refuses in both branches (`Kwantorvouw.bronPad`) and the RegelSpraak frontend does not read. Not measured: a writer
outside the rules and the groups (an allocation, a creation), which the order does not see, and a sum over a temporal
attribute of a set with a subselection, which keeps NO_LOOP. The DRL-text branch, whose rules watch what they read,
revises a fold in a value, but fires a condition on the transient (`verwacht leeg, werkelijk 1`), keeps a maximum whose
subselection emptied (`verwacht leeg, werkelijk 20`), and refuses a document with a defaults group.
