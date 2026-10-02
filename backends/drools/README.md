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

What a defaults group reads is public per default too, since 2026-10-01: `ProducentConsument.lezingen(model, groep,
navigatie)` gives, per default of the group in the order of the group and each default before those of its exception
groups, its path in the tree (`[i]`, `[i, g, j]`, …), the default itself, and what its justification and its consequence
read. A consumer that names the default where a read falls outside a scope reads it there, with the source text the
default and its parts carry. `vormen()` takes the group's reads from it, so their union is exactly the group's
`Vorm.leest()`; `GraafDefaultLezingTest` holds that on every defaults group of the corpus and on the forms a group
takes: a member and a characteristic check in a justification, a member in a consequence, one and two roles away, a
nested exception group, a member of `Uitzonderingen ongeordend op` and the group-default of `<basis> voor <subtype>`.

## Recorded: two writers of one target

Two rules that write one attribute of one object and whose conditions both hold there have no common fixpoint when
they write different values (RUNTIME-SEMANTICS ch. 5.1, 5.7); WF-079 refuses two unconditional writers, so two
conditional ones reach the engine. In the exec-model branch the two fire each other again until the replay's fire
cap. The replay keeps the last writes of the session (`cases.Schrijfconflict`, an agenda listener on every case's
session, the red path's and the witness seams' included) and at the cap names the writers that kept changing one
attribute of one object to different values, in document order, with their values: "geen fixpunt: twee schrijvers
van één doel geven verschillende waarden — de regel "melding op vinddag" schrijft 2026-03-04 en de regel "melding op
aanspoeldag" schrijft 2026-03-01 in het attribuut meldingsdatum van V, een Vondst; divergentie is een fout
(RUNTIME-SEMANTICS ch. 5.7)". Before this, it reported "geen fixpunt (kringverwijzing?)", a cycle the document does not
contain; a run that reaches the cap otherwise, such as a creation that keeps minting, keeps that sentence. With three
writers the engine can let two of them fire each other while the third waits on the agenda, so the sentence names at
least two, where `ref` names all three. In the DRL-text branch a computing rule watches what its condition and value
read and not its own target, so each writer fired once and the one that fired last kept its value without a word; a
computing rule on a target that another rule writes (a computing rule or an initialisatie) now watches that target as
well (`DrlTranspiler.gedeeldDoel`), so both branches reach the cap and give the same sentence. The same holds for the
corner this section used to record: a computing rule that evaluates leeg beside a conditional initialisatie outside
the ch. 7.7 domain, where `ref` faults and the ASP backend derives its `viol/2` witness, and where the DRL-text branch
answered the initialisatie's value. Writers whose conditions exclude each other, or that write the same value, play in
both branches (golden `jutterswet-melding-naar-aard`, red case `jutterswet-dubbele-melding`, witness
`OverlappendeSchrijversWitnessTest`).

## Recorded: a division by a numeric zero

A division whose divisor is a numeric zero is a fault (RUNTIME-SEMANTICS ch. 1.4, point 4; the red case
`division-by-numeric-zero`): `ref` faults with "division by numeric zero" and clingo with its fail-fast guard on the
subject. Since 2026-10-02 this backend names it as well. `Breuk.delen` throws `Breuk.DelingDoorNul`, an
`ArithmeticException` with the message it always had ("Breuk deling door 0") and the dividend. Out of a consequence it
leaves the fire in the cause chain of Drools' `ConsequenceException`, and `DroolsBackend.benoem`, which `CaseRunner`
calls where it fires a session, turns that into a second `DelingDoorNul` naming the rule and the operands, with what
the consequence threw as its cause: "deling door nul in de regel "verhouding": 60 gedeeld door 0 — een numerieke nul
als deler is een fout, geen leeg (RUNTIME-SEMANTICS ch. 1.4, punt 4)". So on both branches, in a rule's value and in a
default's justification or consequence alike, the replay (`ERROR DelingDoorNul: …`), the red path and the scenario
report read the sentence; a defaults group carries the name this backend gives it (`defaults A:Vondst.verhouding`). Any
other `ConsequenceException` passes `benoem` unchanged. The red path keeps its pinned fragments, since the cause chain
still carries "Breuk deling door 0". A division in a condition is evaluated outside a consequence and still leaves the
fire as Drools' `ConstraintEvaluationException`, which on the exec-model branch names neither the rule nor the
constraint (the DRL-text branch names both). Witness `NuldelerWitnessTest`.

The naming sits after the fire and not in a consequence handler on the KieBase. Drools takes such a handler by class
name (`ConsequenceExceptionHandlerOption`) and instantiates it in every new session; where the kie API and this
backend are loaded by different class loaders, as in the distribution's decision service under Quarkus, the instance
does not cast to the kie interface and no session opens (measured 2026-10-02, when this backend set such a handler:
`ClassCastException: class dev.uchar.regelspraak.cases.DroolsBackend$Nuldelingmelder cannot be cast to class
org.kie.api.runtime.rule.ConsequenceExceptionHandler`, at `DefaultAgenda.<init>`, on every request). The KieBases
`DroolsBackend` builds therefore keep Drools' default handler. A consumer that fires a KieBase itself gets Drools'
`ConsequenceException`, which names the rule and carries `DelingDoorNul` as its cause, and can name it the same way:
`DroolsBackend.benoem(e)` is public for that.

## Recorded: a condition that reads through a role

The exec-model branch tests a condition that reads through a to-one navigation — an attribute or a characteristic
of the reached object — after a far-side join on that object (`ExecModelTranspiler.regelVoorwaarde`), so a value
another rule derives there later is seen; the companions that mirror a rule's condition, the object creations and
the fact creations take the same test (golden `jutterswet-meldingsketen`, witness `GenavigeerdeVoorwaardeWitnessTest`).
Two limits remain, measured 2026-09-28 with `ref` and clingo as the arbiters. The DRL-text branch still writes such
a test into the subject's pattern, so there the rule never fires on a value derived later on the reached object, or
fires on the value the object held at insertion (`anchor/jutterswet-kustwacht`, pinned in gate 12's DRL list). A test
that holds on an empty value, such as a leeg test on a derived attribute, fired on the transient leeg when its rule
was declared before the rule that writes the value, and the write stayed; this held for an own operand, one read over a
role and one over a chain of roles alike (IR-SPEC ch. 5.5, the order-dependent form), while a negated characteristic
check is stratified and was not affected. The exec-model branch places such a rule after the writers of what the test
reads: inside a fold, in the subselection of a fold or the matrix of a quantifier, since 2026-09-30 (the section on a
fold whose subselection tests a value derived later), and anywhere in the condition since 2026-10-01 (the section on a
reader after the writers of what it reads). The DRL-text branch keeps this limit.
A condition that reads through a chain of two or more to-one roles takes a far-side join on every object the chain
passes, in order, and its test goes on the subject bound again after the last join (golden `jutterswet-havenketen`,
witness `KetenVoorwaardeWitnessTest`, measured 2026-10-01): a relation in the middle of the chain that a fact creation
sets while the rules run is seen, which a join on the reached object alone does not see; a rule's value that reads a
chain takes the same joins. A comparison, a leeg or gevuld test and a date computation lower over a chain as over one
role, and a navigated operand of a date computation — a date plus or minus a duration, also a duration parameter, one
role away or more — lowers as it does without navigation. A chain lowers only where joins cover it: in a condition,
and since 2026-10-01 in the subselection of a fold and the matrix of a quantifier fold (the section on a fold whose
subselection tests a value derived later); a consistency rule's assertion over a chain is refused (`schending buiten
Phase-D scope`) where one over one role plays. A date computation against a date
parameter, which this branch does not lower over one role either, is refused over a chain in the same words. An own
number on the left of a navigated one ("zijn gewicht groter is dan de afgeleide diepte van de haven van de jutter van de
Vondst") was refused over one role and over a chain alike, where its mirror and "zijn gewicht maal 1" lowered; since
2026-10-01 the leaf swaps the operands and reverses the operator, as it does for a literal on the left (witness
`VergelijkingEigenLinksWitnessTest`). A characteristic check over two roles does not reach this branch:
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
(`consequentie niet vertaald`), such as a fold over a relation whose two roles are both plural (§13.3.9). Since
2026-10-01 a consequence binds any number of variables (`consequenceOf`, which passes the DSL's `BlockN` beyond the
nine its typed blocks spell out): a group binds what every default of its stack reads, and a tree on an inheritance
line with counts and a sum in its justifications, date folds in its consequences and two parameters binds thirteen; a
rule's value and a creation's toekenningen with five folds bind eleven. Until then such a group, rule or creation was
refused by that count (`te veel gebonden variabelen`). Witness `BindingVoorbijNegenWitnessTest`. A consequence with a
§6.5 division or a §6.6 root was refused (`kanaal-deponerende consequentie … op defaults-target`), since a group carries
no companion that fills the channel. Since 2026-10-01 a division or a root that is the consequence itself, under at
most a rounding, lowers as an ordinary division or root (`plekIsDeConsequentie`): over a leeg divisor or a negative
radicand its value is leeg, the default falls through, and the deposit of a default that falls through does not persist
(RUNTIME-SEMANTICS ch. 7.1); a default that decides has no deposit. A site under another operator ("(… gedeeld door …)
plus 5,00" decides 5,00 with the deposit, `ref` and clingo report it) stays refused. A numeric zero divisor faults at
fire (`ArithmeticException`), as it does in a rule's value, where `ref` and clingo fault on "division by numeric zero".
Witness `DefaultsDelingWitnessTest`. The DRL-text branch has no defaults lowering at all.

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

## Recorded: a fold over a set source inside a rule's value or a creation's toekenning

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
`DatumvouwOverRolWitnessTest` and `DatumSetvouwWitnessTest`. The toekenning of an object creation lowers its folds the
same way since 2026-10-01, see below. Until 2026-09-30 the branch refused the date folds
(`waarde buiten scope`, and the member-type sentence of `kern.Vouwwaarde` for the minimum and maximum) and lowered a
fold inside a computation through an evaluator of its own, `lowerVal`, which refused date arithmetic, a navigated read
and a clamp, and answered leeg for a percentage of a sum and for the minimum or maximum of an empty source plus 1,
where `ref` and clingo answer. Two limits were measured 2026-09-30 with `ref` and clingo as the arbiters. An
ordering fold over a date in a condition was refused by name (`voorwaarde met een ordenende vouw over een datumbron`),
since a condition compared the accumulate's result as a number; it plays since 2026-10-01 (the next section). A fold
beside a division or a root under one Let or
IfThenElse (ch. 4.10) is refused by name, since the path-probe companion of such a §6.5 or §6.6 site evaluates that
node without the rule's accumulates; a fold bound by a Let alone plays. The DRL-text branch refuses the same rule in
the same sentence since 2026-10-01. The DRL-text branch markers the eerste/laatste fold over a set (`TODO aggregatie
EERSTE`), refuses the minimum and maximum over a date with the `Vouwwaarde` sentence, which names that branch, and
answers leeg for the minimum or maximum of an empty source plus 1 and for a date plus the maximum of an empty source.

A §6.5 division or a §6.6 root in a rule's value whose operand holds a fold — a sum or a minimum as the divisor, a
fold as the dividend or inside a compound operand, the radicand of a root — gets a companion in both branches since
2026-10-01 (`ExecModelTranspiler.vouwCompanion`, `DrlTranspiler.vouwCompanion`). The companion lowers the operands as
the rule's value does, each fold an accumulate of its own with the count bound, so an empty source reads leeg, and
raises the Inconsistentie where a filled dividend meets a leeg divisor or the radicand is negative. Before, a
companion read only an own or to-one navigated attribute, and the case read consistent where `ref` and clingo deposit
(`verwacht inconsistent, werkelijk consistent`). A divisor that is 0 stays a fault at fire in every executor (ref:
"division by numeric zero"). Witness `VouwBegeleiderWitnessTest`.

The toekenning of an object creation ("Een Jutter heeft een afrekening met heffing gelijk aan 10% van de som van het
gewicht van alle vondsten van de Jutter") goes the same way since 2026-10-01 (`ExecModelTranspiler.objectCreatie`):
each toekenning is a `scalarWaarde`, the folds of all toekenningen are accumulates on the creation's rule after its
guard, and the value is restated into the declared unit of the member it is written to (RUNTIME-SEMANTICS ch. 2.4),
which a toekenning without a fold did not get either (`verwacht 2000, werkelijk 2`). Golden `jutterswet-afrekening`,
witness `ObjectcreatieVouwWitnessTest`. Until that day the creation lowered a fold through `lowerVal`: a percentage of
a sum answered leeg, a minimum or maximum over an empty source left the creation unfired, so a constant beside it read
leeg too, and a date fold, a clamp and a navigated operand were refused. A division or a root in a toekenning plays:
the creation evaluates its toekenningen once, when it mints, and the probe every §6.5/§6.6 site reports to rides that
evaluation, so the creation raises the Inconsistentie for itself and its subject where `ref` deposits (ch. 5.5, 5.6);
for one round of the same day it was refused by name (`kanaal-deponerende toekenning`). Five folds, eleven bound
variables, were refused by that count until a consequence bound more than nine (see the defaults section above). A
date fold in the creation's condition plays as in a rule's condition (the next section); until 2026-10-01 it was
refused by name, in the sentence of a rule's condition.

## Recorded: a date fold as an operand of a condition or a justification

Since 2026-10-01 the exec-model branch lowers a comparison of two dates, and a leeg/gevuld test of a date, whose operand
folds a date over a set source ("de laatste van de bergdag van alle vondsten van de Jutter later is dan zijn
meldtermijn"; eerste, laatste, and the minimum and maximum a hand-written document spells), with the fold on either
side, inside date arithmetic with a literal duration or a parameter, one role away or over a path. Each fold becomes an
accumulate with its count, as in a rule's value (`vouwslot`), so an empty source, or one whose contributors have no
date, reads leeg, and the comparison is the one of a date operand elsewhere (`datumVergelijkTemporal`, the empty
operand of RUNTIME-SEMANTICS ch. 1.2). In the condition of a rule or a creation (`ExecModelTranspiler.datumvouwVoorwaarde`)
the condition is one test after the accumulates: a chain of from-declarations carries the subject's globals and the
folds' values to it, one variable per declaration, so no constraint reads two globals and there is no ceiling on their
number; a composite with such a member is that one test under any quantifier, its subject-local members read from the
subject. A date parameter beside the fold plays there. In a justification (`rechtvaardigingZijde`) the reduce-rule
reads the fold from the slots its consequences use. A member a rule declared later derives is read after that rule
(`groepVolgorde`), also under a leeg test. Witness `DatumvouwVoorwaardeWitnessTest`; the rows that pinned the refusal in
`DatumvouwOverRolWitnessTest` and `ObjectcreatieVouwWitnessTest` play since. Refused by name: a date fold elsewhere in a
condition, such as in the matrix of a quantifier fold or a subselection (`… op een plek die deze tak niet vouwt …`), and
a date parameter in a justification (`de parameter '…', die deze tak in een rechtvaardiging alleen als getal leest`),
as before and with or without a fold. The DRL-text branch markers such a condition (`TODO voorwaarde`) and has no
defaults lowering.

A creation reads stratum-strict (RUNTIME-SEMANTICS ch. 6.1) and mints once. Since 2026-10-01 the object and the fact
creations are nodes of `groepVolgorde` beside the rules and the groups: a creation reads its condition, its
toekenningen, the population of its subject and the links its folds, quantifiers and navigations follow, and writes
its toekenningen on the created type, that type's population and its link; a fact creation reads its condition and the
links of its hops and writes its link. A creation goes below every node it waits on that would otherwise fire after it
— a defaults group, a rule the stratification places lower, a creation declared after it, a fact creation — and a
recursive creation stays on its generations (ch. 5.5). Before, it fired at salience 0 before such a writer and wrote
leeg or 0 (`verwacht 15, werkelijk leeg`, `verwacht 1, werkelijk 0`). Witness `CreatieVolgordeWitnessTest`; no golden
changes its salience. A reader of a creation's output that writes irrevocably, such as a characteristic assignment over
a count of the created objects or over the links a fact creation lays, and a leeg test on what a creation writes, fired
before the creation (`kenmerk onafgerekend: verwacht afwezig, werkelijk aanwezig`); since the same day such a reader
waits on the creation (the section on a reader after the writers of what it reads).

The DRL-text branch, unchanged otherwise: it orders no creation (a later creation and a fact creation give the same 0;
it refuses a document with a defaults group), markers a date fold in a toekenning (`TODO aggregatie EERSTE`, `TODO
datum-rekenkunde`), leaves the creation unfired over an empty source where a toekenning holds a minimum or maximum
(`verwacht 1, werkelijk leeg`), writes a toekenning without restating it into the member's unit (`verwacht 2000,
werkelijk 2`), and reads a division by leeg in a toekenning as consistent.

## Recorded: a reader after the writers of what it reads

A read under a leeg or gevuld test is a negative edge in either polarity, and every read but a characteristic's by a
form whose write is irrevocable — a characteristic assignment, a consistency rule, a creation — is stratum-strict
(RUNTIME-SEMANTICS ch. 6.1): such a reader evaluates strictly above the producers of what it reads. Measured 2026-10-01
with `ref` and clingo as the arbiters, the exec-model branch fired such a reader on the transient when its writer came
later on the agenda — a rule declared after it, a defaults group, a creation, an allocation (all added to the model
after the ordinary rules) — and the write stayed, for an own operand, one role away and over a chain alike: an attribute
written where `ref` and clingo answer leeg (`verwacht leeg, werkelijk 2026-09-15`); a characteristic attributed over a
leeg test, over a comparison with a value derived later, or over the number of created objects or of the links a fact
creation lays (`kenmerk gemarkeerd: verwacht afwezig, werkelijk aanwezig`); a violation a consistency rule reported over
a value a group or a creation writes, and the §6.5 companion of a rule under a leeg test (`verwacht consistent,
werkelijk inconsistent`). `ExecModelTranspiler.groepVolgorde` now places such a rule after every writer of what it reads
negatively or stratum-strict, or in its condition, and every node that writer waits on: strictly below it, or level with
it and added before the rule, since Drools fires one salience in the order the rules were added. The writers are the
ordinary rules, the defaults groups, the object and fact creations and the allocations; an allocation keeps its own
place. A consistency rule takes the same placement, and the companions of a rule (§6.5 division, §6.6 root, leeg
startpunt, gevuurd) take the salience of their rule, since they repeat its condition. A leeg or gevuld test on what a
writer on a cycle with the rule writes, its own target included, has no order (WF-046) and the rule is refused by name
(`een leeg- of gevuld-toets op … een kring zonder volgorde (WF-046)`); a stratum-strict read on a cycle through a
recursive creation keeps its place, its generations order it (ch. 5.5). Witness `LezerOnderSchrijverWitnessTest`. No
golden changes its status; ten get a lower salience for such a reader, the rules and the consistency rules the opgave
marker generates among them.

A comparison in the condition of a rule that writes an attribute reads positively (ch. 6.1), and the exec-model branch
orders it after the writers of what it reads all the same, a finer order than the graph asks for that reaches the same
fixpoint; on a cycle such a rule keeps its place, without a refusal. Before, with two operands derived later ("…
ongelijk is aan zijn afgeleide controledag") the branch tested the condition after the first writer and before the
second and wrote the vinddag where `ref` and clingo answer leeg; with a comparison that holds on an empty operand ("…
ongelijk is aan 01-03-2026", "het aantal bonnen van de Jutter gelijk is aan 0") before its writer it now answers leeg
with clingo, where `ref` writes on the transient (the recorded residual of ch. 6.1). What remains, measured the same
day: an allocation whose source a defaults group writes faults in `ref` (`empty source amount`) and in this branch (a
ConsequenceException on the empty source) where clingo divides. The DRL-text branch keeps its own order: a leeg test, a
comparison in a characteristic assignment and one in the condition of any rule, declared before its writer, act on the
transient; a test one role away does not see the far side (the section on a condition that reads through a role); a
count of created objects or of the links a fact creation lays reads 0, since it orders no creation; a leeg test on what
an allocation writes fires first; a condition over two roles is refused by its marker and a document with a defaults
group at the door. A consistency rule over a creation's link plays there.

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
Limits that remain, measured 2026-09-30 with `ref` and clingo as the arbiters: a quantifier over two roles with a
subselection, which the loader refuses in both branches (`Kwantorvouw.bronPad`) and the RegelSpraak frontend does not
read. A leeg or gevuld test outside a fold takes the same order since 2026-10-01, and the creations (2026-10-01) and the
allocations (the same day) are writers the order sees: "het aantal bergers van de Berging die aan de volgende voorwaarde
voldoen: zijn bergingsaandeel is leeg groter is dan 0" waited for the allocation only since then (`verwacht leeg,
werkelijk 1` before); see the section on a reader after the writers of what it reads. Not measured: a sum over a
temporal attribute of a set with a subselection, which keeps NO_LOOP. The DRL-text branch, whose rules watch what they
read, revises a fold in a value, but fires a condition on the transient (`verwacht leeg, werkelijk 1`), keeps a maximum
whose subselection emptied (`verwacht leeg, werkelijk 20`), and refuses a document with a defaults group.

A subselection, or the matrix of a quantifier fold, that reads through a role of the element ("die aan de volgende
voorwaarde voldoen: zijn vinddag is later dan de sluitdag van de jutter van de Vondst") reads another object than the
element. The exec-model branch tested it on the element's pattern, which runs when the element is inserted or updated
and not when the reached object is, so a value another rule, a defaults group or an allocation wrote there later was not
seen: measured 2026-10-01 with `ref` and clingo as the arbiters, the earliest such vinddag read leeg, a count 0, a sum and
a maximum leeg, a leeg test on the far member counted every vondst, and a count or a quantifier in a condition never
fired; a chain of two roles and a date computation with the far member in a subselection were refused by name. Now the
source of the accumulate takes a join per object those reads reach (for a chain, per object it passes), and the
subselection goes on the element bound again after those joins (`ExecModelTranspiler.elementtoets`, `verreJoins`), the
shape a condition over a role has; a quantifier fold collects its elements over the same joins, so its matrix is asked
again when a reached object changes. The join is on `Object` and binds the element itself where the role reaches
nothing, since the source of an accumulate carries no OR beside the join; every element keeps one tuple, and a read
over an absent relation is leeg. A union selection that reads through a role of the element is refused by name: its
selections share one element pattern. Golden `jutterswet-late-vondst`, witness `VouwfilterVerreKantWitnessTest`. A rule
that updates the element after the writer (a characteristic on the element, an object creation that links the reached
object to it) played before as well. The DRL-text branch writes such a test into the element pattern of its accumulate
or quantifier and answered with the members it had at insertion (`verwacht 2, werkelijk 0`); it refuses the form by name
(`een subselectie of matrix die via een rol van het element leest`), also where the answer happened to hold.
