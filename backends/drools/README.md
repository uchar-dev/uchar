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
returns a `Build` record: the model, the bean class loader, the transpiler of the chosen branch (`EXEC_MODEL`,
`DRL_TEXT`, or the trial `TRUTH_MAINTENANCE` below), its coverage manifest and the `KieBase`. It is the whole prelude of the case runner, which builds
every golden through it, so gate 12 fires the KieBase a consumer gets, and it refuses in the replay's
sentences (`IrLoader.Refusal`, `IrLoader.Documentfout`, `CaseRunner.Excluded`). Each transpiler has one build,
and it takes every side table the loader returns beside the model: a build that left one out still produced a
KieBase — a defaults family firing nowhere, a navigated read answered as the subject's own — and no marker said
so, which is why there is no shorter way in. A document whose lowering holds two rules of one name in one package —
two forms of one name, which WF-020 forbids and a hand-written document can still carry — is refused on both branches
before the KieBase is built: the KieBase keeps one rule per name, and the exec-model branch played such a document
with the second rule in the place of the first (measured 2026-10-02: "kenmerk actief: verwacht aanwezig, werkelijk
afwezig"). The DRL-text branch's compiler refuses it with "Duplicate rule name: actief", the exec-model branch with
"exec-model: Duplicate rule name: actief — …" (witness `DubbeleRegelnaamWitnessTest`).

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

## The truth-maintenance trial branch

A third branch, `DroolsBackend.Branch.TRUTH_MAINTENANCE`, lowered by `transpiler/tms/TmsTranspiler` and chosen with
`--tms` on the CLI (`CaseRunner.viaTruthMaintenance` in the replay). It began as the trial of the card
`drools-logische-afleiding`: can Drools' truth maintenance replace the measures the exec-model branch takes against
the engine's own reactivity — `NO_LOOP` on a rule that writes its own subject, salience from the stratification,
opaque class reactivity, `update` only on a changed value, the `initEigen` guard of an initialisation — and what does
that cost in rules, firings and control facts. The measurements are in that card's result; `TmsMeting` in the test
tree produces them, and its javadoc says how to run it. On them the owner decided to build the branch out into the
replacement of the exec-model branch (PLAN §9, 2026-10-08), in four cards. The first,
`tms-aggregaties-creatie-en-fouten`, added aggregations, object and fact creation and evaluation faults as logical
facts; its `Result:` gives the measurements and the count of the corpus per family. The second,
`tms-expressievormen-en-rekendatum`, adds the value forms, the evaluation date, the faults of date arithmetic as
facts, the condition forms and the writers of one target (below). The third, `tms-verdeling-en-tijdlijnen`, adds
allocations (below) and, in its later steps, timelines and day kinds. It is no gate path yet: gate 12
plays the exec-model and the DRL-text branch, and the toolchain builds with `EXEC_MODEL`. The suite runs its
witnesses (`Tms*WitnessTest`), among them `TmsCorpusWitnessTest`, which demands that every golden with cases either
plays or is refused, that none plays a wrong value, and that at least 216 play (`EXPECTED_MIN_PLAYED`).

**Derivations are logical facts.** A derived characteristic is a `HeeftKenmerk`, a derived attribute value a `Waarde`
with its writer, both inserted with `insertLogical`. Case input stays on the beans, and while the session fires the
branch writes no bean and calls no `update`. Every read a derived value depends on is a bound pattern in the
left-hand side, and a consequence computes only from bound variables, so truth maintenance withdraws whatever rested
on a read that changed. A read of a derived member goes through a total standing fact, exactly one per object and
member (`KenmerkStand`, `Stand`, and `PadStand` for a read one role away), which carries the derivation or else the
input; a read of an input member or of a role binds the bean, except a relation a form creates (below), and a
parameter is read through its `ParameterStand`. A
condition is a predicate over bound values; the negations sit in the generic standing rules, in the guard of an
initialisation and in the control facts of a defaults group. The KieBase runs in `EqualityBehaviorOption.EQUALITY`,
where a stated fact takes over an equal logical one; equal logical facts merge into one fact with a justification per
rule, which rests on the `equals` and `hashCode` of the fact classes, the subject compared by identity.

**Initialisations, consistencies, defaults.** An initialisation fires where the target is empty in the input on the
bean and no other writer holds a `Waarde` on it (beside a writer outside the standing domain, a written leeg leaves
the target empty: below); its guard excludes its own write, since a guard that counted its own `Waarde` would undo the
write it justified. A consistency inserts a logical `Inconsistentie`, and "regelversie X is
inconsistent" reads the total standing fact `KanaalStand`. A defaults group decides with control facts (`Stuurfeit`).
In the simple form, one chain without exception groups, a deciding default inserts `Beslist` where a lower default
has a rule, and every lower default carries `not Beslist` over the ones above it. In the general form, with
exception groups or more than one group at the root of the effective stack (RUNTIME-SEMANTICS ch. 7.10), every
default settles into an `Uitkomst`, and the member chains are reconciled by the rules of ch. 7.2 and 7.9. A higher
decision displaces a lower one through truth maintenance alone and gives it back when it goes. The rules of a member
chain also wait on the positions above each default whose exception group the chain is, at any depth, as the
reference interpreter reaches the exception groups of a default only where that default is reached (ch. 7.9): an
exception group below a displaced default settles no `Uitkomst` and raises no fault at the fixpoint. The loose control
facts the measurement (`TmsMeting`) counts apart are therefore none.

**The write-back.** After a fire that reached its fixpoint, `DroolsBackend.zetAfgeleideWaarden(build, session)` puts
the derivations on the beans: `setX(true)` per `HeeftKenmerk`, the attribute's setter per `Waarde`. Input on a
characteristic stands, since nothing writes false; a derived value replaces input on an attribute, a computed leeg
of a standing writer writes nothing, so the input stands (ch. 7.1, 7.8), and a computed leeg of a writer outside the
standing domain empties the attribute (ch. 1.2g). Two different values on one attribute of one object have no common
fixpoint: they stand as a fault (below), and the write-back never chooses. The replay passes the case labels to
`zetAfgeleideWaarden`, so that the fault names its object as the reference interpreter does; a consumer without labels
gets the type and the identity. On the other branches the call does nothing, so a consumer that fires a KieBase
itself can call it after every fire. The session must not fire again afterwards: the beans then hold values the
network never saw arrive.

**Evaluation faults are logical facts.** A division by a numeric zero (RUNTIME-SEMANTICS ch. 1.4, point 4) is a fault,
and so is every fault of `Rekenfout`: a date step or a duration count outside the range of ch. 3.8, a context value
or a validity window in a session without an evaluation date (ch. 8.1, 8.2), an `Unresolved` node the evaluation
reaches (ch. 5.7, WF-044), an argument a date builtin cannot take (easterSunday of a year that is not whole,
dateWith over parts that name no day), and an irrational square root without a rounding (ch. 2.2), where the
reference interpreter computes a rational one exactly. A fault, like a value, belongs to the fixpoint (ch. 5.2; PLAN §9,
2026-10-09, "Faults and deposits count at the fixpoint"). This branch has no strata and passes intermediate states
the reference interpreter does not see, such as a divisor a lower default set to 0 before a higher one decides, and a
fault thrown out of the fire there could not be withdrawn when the state passes. So no consequence throws an
evaluation fault: the consequence that computes a value inserts the fault logically as a `Fout`, in place of the value,
and the fault goes when a read it rested on changes. A condition is tested in the left-hand side, where Drools cannot
take an exception as a fact; there the test answers that the condition does not hold where it faults, and a rule of the
form's own, `<rule>: fout in de voorwaarde`, with the same left-hand side and the test that the condition faults,
inserts the `Fout`. The write-back reports the faults that stand at the fixpoint before it writes anything: one division
as the `DelingDoorNul` the exec-model branch's fire gives through `DroolsBackend.benoem`, with the same sentence
("deling door nul in de regel "aandeel": 100 gedeeld door 0 — …"), one other fault as its `Rekenfout`, named after
the rule and in the reference interpreter's sentence wherever that executor names the fault ("fout in de regel
"meldtermijn": duration count … is out of range"), two or more as `Evaluatiefouten`, ordered by the name of the form
and then by the sentence, so the report does not depend on the order of the rules. That message opens with a head that
names the rule of every fault ("aan het fixpunt staan 2 evaluatiefouten, in de regels "aandeel", "verhouding""), and
the CLI's ERROR line bounds only the sentences after it, so no fault drops off the line. A consumer that fires a
KieBase of this branch itself meets the fault at `zetAfgeleideWaarden`, not out of `fireAllRules`. Witnesses
`TmsFoutWitnessTest`, `TmsVoorwaardefoutWitnessTest` and, for the date faults, `TmsDatumfoutWitnessTest`, among them a
duration count that lies outside the range only on an intermediate state, where the branch threw out of the fire
until 2026-10-09. Other exceptions are no evaluation fault and still leave the fire: a broken contract and a bug.

**Value forms and the evaluation date.** A `Let` binds its binder as a need (RUNTIME-SEMANTICS ch. 10.6): the value is
evaluated at the first read and kept for every further one, a read of a leeg value ends that `Let` alone with leeg,
and a binder the body does not read evaluates nothing, so it faults and deposits nothing. An `IfThenElse` evaluates
the branch its condition chooses and nothing of the other (ch. 10.2). Both bind every read of both branches and of a
binder's value in the left-hand side all the same, so truth maintenance follows each value the evaluation may need.
Which §6.5 or §6.6 place deposits therefore depends on the evaluation itself: every division of a filled dividend by
a leeg divisor and every square root of a negative number that the evaluation reaches deposits, as in the reference
interpreter (ch. 1.2, 5.6). The consequence of a computing rule puts a probe (`Sonde`) in its value array, and every
such place reports to it where it runs; a condition deposits through a rule of the form's own, `<rule>: deponering in
de voorwaarde`, with the same left-hand side and a probe in its test, so a member of a composite that its verdict no
longer reads deposits nothing (ch. 5.10). Witness `TmsDeponeringWitnessTest`. Rounding,
clamp, the builtins outside the folds, a text composition of texts, percentages and `percentageOf`, a power with a
whole exponent, the restatement of units at every operand boundary (ch. 2.6), a dimension cell and the eleven and
exact-digit tests compute through the runtime's helpers, as the exec-model branch computes them, from bound values;
for the last three and the sub-day extractors, dateWith and dateTimeWith, which the reference interpreter declines
(ch. 5.11), the exec-model branch is the only other executor. The evaluation date is the fact `Rekendatum`, put in
working memory once per session from the global `rekendatum` the case runner sets, with a null date in a time-free
session. A version with a bounded validity gates on it with a pattern that admits the date (ch. 8.2) and beside it a
rule inserts the fault where the session carries no date, as the reference interpreter faults when it evaluates the
form, with or without a subject; a defaults group with a dated validity gates its own chain the same way and faults
per object of the stack's type, where the reference interpreter asks the group's window. A `TimeWindow` on a scalar
target and the context values `evaluationDate` and `evaluationYear` read the date as a bound value. Witnesses
`TmsLetWitnessTest`, `TmsRekenvormWitnessTest`, `TmsBuiltinWitnessTest` and `TmsRekendatumWitnessTest`.

**Aggregations.** A fold over a set source (`count`, `filledCount`, `sum` with its `emptyFallback`, a `total` over
members that are not time-dependent, `minimum`, `maximum`, `first`, `last`) is an accumulate in the left-hand side of
the rule that reads it, over `Bijdrage` facts: one per member of the range that passes the fold's filter, inserted
logically by a contribution rule of the fold. That rule binds the fold's subject, the member joined to it through the
range (`sourceRole`, or `sourcePath` on the plan of `transpiler/kern/Bronpad`, with the intermediates in an `exists`),
the member's value and every read of the filter, with the member as the filter's subject; a member whose value or
filter reading changes gives up its contribution and makes the new one, and the accumulate follows. A value fold skips
a leeg member, a count counts every member, and over no contribution a sum is leeg or its fallback, a total 0, a count
0 and an ordering fold leeg, since the accumulate always binds the count beside the folded value; `first` and `last`
are the least and the greatest value. Equal contributions merge into one fact, so a member that a multi-step range
reaches along two routes, or that two selections of an `InstanceSetUnion` select, contributes once (RUNTIME-SEMANTICS
ch. 3.4b, 3.4d). When the result of an accumulate changes, the activation that read it fires again and Drools
withdraws the logical facts its earlier firing inserted and did not insert again: one `Waarde` stays for the target.
A listed fold ("de eerste van A en B") is computed from its operands' bound values. Witness
`TmsAggregatieWitnessTest`.

A filter that divides by a numeric zero for a member inserts a `Filterfout` for that member, and no fault: the
reference interpreter evaluates a filter only where the evaluation of a form reaches the fold, so not where the form's
condition does not hold, nor in a member of a composite that its verdict no longer reads (RUNTIME-SEMANTICS ch. 5.10),
and there per member in fold order, stopping at the first fault (ch. 5.11). The bean model keeps a to-many role as a
reference field on each member and no order, so for a model that reads it (`TmsTranspiler.leestLijstplaatsen`) the
case runner states the place of every member of a to-many list as a `Lijstplaats`, in the list the reference
interpreter holds: the members the case writes under the holder, in the order it writes them, then the members the
loader mirrors from their own link, in ascending label order (ch. 5.7a). The rule that inserts a `Filterfout` joins the
member per route, with the intermediates of a path bound rather than in an `exists`, binds the place at every to-many
step, and gives the fact its place in the fold order, the number of the selection of a union and then those places.
Two routes to one member stay two facts, so the least is the member's first reach, as the dedup of the reference
interpreter keeps it (ch. 3.4b). The rule that reads the fold binds the least `Filterfout` on its subject beside the
contributions, the first in fold order, and where its own evaluation reaches the fold while one stands, that
evaluation faults as a division in it would; the `Fout` is that rule's, inserted by its consequence or by its fault
rule, named after it, and withdrawn when the `Filterfout` goes. A member whose place the input did not state makes the
rule fail loudly. Witness `TmsFilterfoutWitnessTest`.

A filter that deposits for a member (a division by leeg, a negative root) inserts a `Filterdeponering` for that member
in the same way, through a rule beside the contribution rule with a probe in its test, and no inconsistency: the
reference interpreter evaluates the filter with the collector of the form that reads the fold, so the deposit is that
form's, on its subject, and only where its evaluation reaches the fold (RUNTIME-SEMANTICS ch. 5.11, "keyed by (rule ×
subject)"). The rule that reads the fold counts these facts on its subject beside the contributions and reports to its
own probe where its evaluation reaches the fold while one stands; the filter of each selection of an
`InstanceSetUnion` does the same. Witness `TmsFilterdeponeringWitnessTest`.

**Condition forms.** A quantifier fold (RUNTIME-SEMANTICS ch. 3.9a) has the contributions of an aggregation over its
one-step range: a contribution rule per member that passes the filter binds the member's reads of the filter and of
the matrix, the condition the fold asks of each member, and inserts a `Bijdrage` with 1 or 0 for the member's verdict,
so the rule that reads the fold counts the contributors and the hits with one accumulate, and all, none and the three
counting quantifiers compare those; over no contributor all and none hold. A member whose derived value changes takes
its contribution along. Where the matrix can fault or deposit, the order of the members shows: the reference
interpreter reads the matrix in the contributors' order and stops at the first one after which the verdict stands
(ch. 5.10), so a member it does not reach neither faults nor deposits. The contribution then carries the matrix's
outcome and the member's place in the list of the fold's subject (`Lidtoets`, the place a `Lijstplaats` gives, as for
a filter fault), and the reading rule collects them and replays the stop triggers of ch. 5.10 in that order, the empty
prefix included; over a created relation, whose made links have no place, such a matrix is refused. The filter is
settled for every member before the matrix, with its faults and deposits as for any fold. A role check (ch. 5.8a) is
an existence question bound as a value, so it stands in a composite like any condition: over a relation a form
creates, an accumulate over the relation's `Koppeling` facts that hold the subject at its end; over an input
relation, the subject's own reference field where the other role is to-one, and else an accumulate over the beans
whose field names the subject, as the bean model stores the link (`FactModelGenerator.lidmaatschapsopslag`); a
counterpart type is a class test, which a subtype passes. A uniqueness (ch. 5.9), over one source or more: per source a
rule inserts each instance's key (`Uniciteitssleutel`), its components bound in the left-hand side, a derived one
through its standing, and for a scoped form its parent, where every component is gevuld and the parent is there; the
form's rule counts per key the keys that collide with it, and where more than one does, marks the instance
(`Uniciteitsschending`), which a rule of the form puts on the §9.5 channel once per instance. A key belongs to its
source, so an instance whose keys in two sources of a union are equal duplicates against itself. Everything is
logical, so a duplicate that arises on an intermediate state is gone at the fixpoint, where the reference interpreter
evaluates the form. Witnesses `TmsKwantorvouwWitnessTest`, `TmsRolcontroleWitnessTest` and
`TmsUniciteitWitnessTest`.

**Writers of one target.** A computing rule is an equation (RUNTIME-SEMANTICS ch. 5.1), and a target may have any
number of them, the versions of one form among them. On a standing target (ch. 7.8, and the desugaring domain of ch.
7.7, as `transpiler/kern/StaandeDoelen` classifies it for all three branches) a computed leeg inserts nothing; outside
it the rule inserts a `Waarde` that carries leeg (ch. 1.2g), which takes the input standing away and is a value
another writer can disagree with. Where two writers stand on one object with different values, a rule per attribute
on which two can stand (two computing rules, or one outside the standing domain beside an initialisation; an
accumulate over the attribute's `Waarde` facts on the object) inserts a logical
`Fout` with a `Schrijversconflict`: "geen fixpunt: twee schrijvers van één doel geven verschillende waarden — de regel
"melding op vinddag" schrijft 2026-03-04 en de regel "melding op aanspoeldag" schrijft 2026-03-01 in het attribuut
meldingsdatum van V, een Vondst; divergentie is een fout (RUNTIME-SEMANTICS ch. 5.7)", the writers in the order of the
document and the object by its case label, as the reference interpreter names them, so the Drools pin of
`corpus/rood/jutterswet-dubbele-melding` matches it. Like every fault it counts at the fixpoint, so a difference on an
intermediate state only is no fault. With three writers the sentence names every writer that stands, where the
reference interpreter names the two its last passes saw change the attribute. The writers are keyed per object, so per
concrete type (WF-079, ch. 7.10): rules on sister subtypes never meet, and a rule on a supertype meets one on a subtype
on the subtype's instances. An initialisation's guard reads the target as the other writers leave it, as the
reference interpreter's guard reads the running fact base: a value another writer places switches it off, and a leeg
that a writer outside the standing domain writes leaves the target empty, input or not, so the initialisation fills it
and the two writers disagree, where the reference interpreter faults on the pass budget with the same two writers.
Witnesses `TmsSchrijversWitnessTest` and, for a member an object creation sets and a division in an initialisation
beside a writer on a sister subtype, `TmsCreatieInvoerWitnessTest`.

**Allocations.** An allocation (RUNTIME-SEMANTICS ch. 4) is a computation over the recipients that the left-hand
side binds. A rule of the allocation inserts, per source and per recipient, an `Ontvanger` logically: an instance of
`targetType` that the connecting relation links to the source, through the bean field of an input relation or the
`Koppeling` facts of a relation a form creates, so a recipient a creation adds counts as well, with its weight,
ordering key and cap bound in that rule's left-hand side, a derived one through its standing. The allocation's own
rule binds the source, its pot, the reads of its condition and, through an accumulate, the list of the source's
`Ontvanger` facts; its consequence computes the distribution from those values alone (`Verdeling.bereken`, a public
function of plain values, so that a rendering as DRL text can call it too). Every share and the rest is a `Waarde`
with the allocation as its writer, and every degradation of ch. 4.3 and 4.4, an empty weight, a negative proportional
weight, an empty or negative cap, is the allocation's §9.5 deposit on that recipient. All of it is logical, so a
recipient that arrives or leaves, a weight, key, cap or pot that a rule derives later or changes, and a condition that
stops holding take the source's whole distribution along, the rest included (ch. 4.3), and a deposit made on an
intermediate state goes again. It computes as the exec-model branch does, with `rsPlafond`, `rsAfronding` and
`rsVerdeelOpVolgorde`: a share is the quotient at 16 significant digits, capped, then floored, the v0 residual ch. 4.2
records beside the exact fractions. The edge rows of ch. 4.4 are the reference interpreter's: no recipients, an empty
pot of the ordered method and eligible proportional weights that sum to 0 write nothing, the last still with the
deposit of an empty weight beside them, and an empty pot of the equal-shares or the proportional method with
recipients is that executor's fault, a `Fout` on the source ("empty source amount in equal-shares allocation (v0
mirror hole, ch. 4.4)"), which like every fault counts at the fixpoint only, so a pot derived later leaves none. Three
readings differ from the exec-model branch, each as the spec has it: a negative weight of the proportional method
counts as 0 with a deposit (ch. 4.4, decided for v1), where the reference interpreter and
the exec-model branch keep the unclamped v0 hole and the ASP backend faults; an empty tie-break weight counts as 0 also
where every weight of its group is empty, as the reference interpreter and the ASP backend read it, where
`rsVerdeelOpVolgorde` splits such a group in equal parts; and two ordering keys of one value in two scales, 2019 and
2019.0, are one group, as those two executors compare them, where that helper groups by `equals`. A version with a
bounded validity gates on the evaluation date and faults a session without one; a condition faults and deposits
through rules of its own, on the source. A share or a rest replaces the input on its target, and a recipient that
receives nothing keeps its input. Witness `TmsVerdelingWitnessTest`.

**Creation is stated, and fires in generations.** An object creation and a fact creation (RUNTIME-SEMANTICS ch. 5.5)
fire only at a fixpoint of the rule layer, and what they make is stated and never lapses, as in the reference
interpreter, which keeps a minted instance under a fixed label. A logical object could keep itself standing through
its own output once the state that made it had passed, and its initialisations would follow later changes, where
ch. 5.5 evaluates them when the object is made. The attribute values an object creation sets are the input of the
object it makes, as the reference interpreter reads them: they go on the new bean before it enters working memory, so
no rule sees the object without them and no bean in the session changes; a writer of the member displaces them as it
displaces case input, a leeg written outside the standing domain erases them, and an initialisation finds the member
filled. So the creation rules sit in two agenda groups of their own,
`TmsTranspiler.FEITCREATIE` and `TmsTranspiler.OBJECTCREATIE`, and the driver `DroolsBackend.vuur` fires the session
in generations, outside the rules: the rule layer to rest, which leaves the creation rules pending; then, per
evaluation stratum in ascending order (below), one phase per generation. A phase inserts a new `Creatiefase` with its
number and its stratum, puts both groups on the focus stack, the fact creations on top, and fires; that fires the fact
creations that hold at the fixpoint, then the object creations, and once both groups are empty takes the rule layer to
rest again in the same fire. The rule layer does not fire between the two groups, so every creation of a generation
reads the same settled world, as in the reference interpreter (RUNTIME-SEMANTICS ch. 5.5, Generations): an object
creation does not read what a fact creation of its own generation links. Until 2026-10-09 a generation was a phase of
fact creations and a phase of object creations with the rule layer at rest between them, and an initialisation that
counts the maatgevers of a Wachter, over a relation a fact creation in the same recursive group lays, gave 1 where the
reference interpreter gives 0. A stratum ends at its first generation that makes nothing, and the creation budget
(below) ends a chain of mints that does not stop, at its mint at depth 101 ("creation budget exhausted"), the bound
that `corpus/rood` `finite-chain-over-budget` and `corpus/golden/synth2-budgetgrens-onder` pin from both sides for
every executor. An agenda filter cannot
keep the creations back while the rule layer fires: Drools 8.44 drops a match the filter rejects from the agenda for
good. The groups do: Drools evaluates a rule lazily, when the agenda takes it from the group that has the focus, so a
creation rule gets no match on an intermediate state of the rule layer. Auto-focus on a creation rule would make it
eager and give it a match there.

**Creations fire per stratum.** A creation writes irrevocably, so every value it reads that is not a characteristic is
a strict read, and the reference interpreter puts the creation in an evaluation stratum above the producers of what it
reads; it evaluates stratum k to its fixpoint, within its budget, before a form of stratum k+1 fires (RUNTIME-SEMANTICS
ch. 6.1 and 6.3, `ref/lib/eval.ml`). With the creations of every stratum in one phase (until 2026-10-08), a creation
that reads what another creation of the same generation makes read the state before it: "maak een afrekening" with a
penningtelling counting the penningen of the Jutter fixed the count at 0 beside "geef een penning", where the reference
interpreter gives 1, and a condition over a sum that the other creation raises held on the lower sum. So every creation
rule gets the stratum the reference interpreter gives its form: `Kringen.strata` computes it as `eval.ml` does, over the
verifier's graph with its positive, strict and negative edges (an edge counts as positive along a cycle, which WF-045
admits only through a recursive creation), and without the one edge this branch draws beyond the verifier, a rule
writing the channel of its own name. The rule binds a `Creatiefase` of its own stratum or one above, as a pass of the
reference interpreter over stratum k runs every form of stratum k and below, and `TmsTranspiler.creatiestrata` gives
the driver the strata in ascending order. The stratum is a field of the phase and a test in the rule rather than an
agenda group per stratum: that keeps the two groups and their order within a generation, and stratum k needs no
focus on the groups of the strata below it. The budget holds per stratum, as the reference interpreter counts the
depth of a mint in every stratum apart. Only the creations are ordered by stratum, since only their output is
stated; the rule layer stays under truth maintenance without strata and reaches its fixpoint after every phase. Over
the corpus the strata equal those the reference interpreter's algorithm gives on the verifier's `--dependency-edges`
for every creation of a document the loader loads (83 creations in 68 documents, 7 of them in stratum 1, among them
`anchor/jutterswet-erflijn`, whose recursive "erf de lijn door" now walks the direct links in a later stratum, and the
three wrakboekingen). Witness `TmsCreatiestratumWitnessTest`.

**The budget counts the depth of a chain of mints.** Every object a creation makes has a creation depth in the stratum
of that creation, one more than its subject's depth there, where an object of the input and one a lower stratum made
stand at 0, and a mint at depth 101 is the fault, in the reference interpreter, in the ASP backend (`oc_budget`) and
here (RUNTIME-SEMANTICS ch. 5.5). The `Gemaakt` mark of an object carries its stratum and depth; the creation rule binds
the marks of its subject through an accumulate (`TmsTranspiler.Lezingen.herkomst`), computes the depth of what it makes
(`TmsTranspiler.diepte`) and, past `TmsTranspiler.CREATIEBUDGET`, throws `Creatiebudget` before it makes anything; the
driver ends the run with its sentence, which opens with the fragment of the other two executors, "creation budget
exhausted". A mint that waits generations for a link or a value keeps its subject's depth, and a fact creation, which
makes no object, has no budget; the generations need no bound of their own, since under the budget a stratum makes
finitely many objects and links. A subject that a higher stratum made cannot occur, since a creation reads its
subject's population strictly; `TmsTranspiler.diepte` throws on one as a fault of the transpiler. The rule layer has no
budget either; truth maintenance takes it to rest within the fire, and the fire cap bounds that. Until 2026-10-09 the
driver counted generations: at first as passes of the reference interpreter of then, a pass in which only the rule layer
changed included (`TmsTranspiler.lidstratum`, since removed), then at most 100 generations per stratum that make
something, which ended a chain whose every mint waits a generation for a link, and a fact creation that lays a closure
of 102 hops, at their 101st generation. Witnesses in `TmsCreatiestratumWitnessTest`: one chain at 99, 100 and 101
mints, 50 + 1 + 50 over three strata, document L with the kopie, the erflijn over 103 Wachters, a chain that waits for
links to depth 100 and 101, and a forged subject of a higher stratum.

An object creation's rule binds the subject, the reads of its condition and of its initialisations, the `Creatiefase`,
the absence of its own `Gemaakt` mark for the subject (one object per creation per subject) and the absence of a
`Gemaakt` that made the subject in the current phase, and on the 1:1 shape the empty reference field of the subject,
which is the input world, since the branch writes no bean. Which roles link the subject and the new object, with the
guards on the reference fields and the refusal sentences, is one decision shared with the exec-model and the DRL-text
branch (`FactModelGenerator.creatiekoppeling`, with `verbindendeFeittypen`). Its consequence makes a new bean of the
created type and inserts, stated, the bean, its `Gemaakt` with the phase, the `Koppeling` of the connecting relation and
the initialisations as `Waarde` and `HeeftKenmerk`, computed from the values the rule binds. A fact creation's rule
walks its path, over the reference fields of an input relation and over the `Koppeling` facts of a created one, none of
the current phase, and inserts the `Koppeling` it makes, stated, where no link between anchor and target exists yet: the
creation is idempotent per link. What a phase made carries its number, and a creation rule takes no object or link of
the phase it fires in, so a recursive chain advances one generation per phase, after the rule layer has reached a
fixpoint over what the previous phase made. Every relation a form creates is read through `Koppeling` facts, the links
of the case input included, which a rule per to-one end inserts logically from the bean fields: a navigation through a
`PadStand` over a chain of patterns, the range of a fold through an `exists` over the link. A second distinct filler of
a to-one role of such a relation is a `Fout` at the fixpoint ("een tweede vulling is een fout, geen overschrijving", ch.
5.5). A condition of a creation that can fault has an ordinary fault rule in the rule layer, without the mark and
without the 1:1 guard, as the reference interpreter evaluates the condition first, in every pass. The replay fires
through `DroolsBackend.vuur`, which is not public; a consumer call is for the card `tms-omschakeling`, and
`zetAfgeleideWaarden` refuses a session of a build with a creation that was fired without a `Creatiefase`. Witness
`TmsCreatieWitnessTest`.

**What it refuses**, from its manifest as `tms-transpiler SKIP: <family>: <name> — <reason>` before the KieBase is
built, never as a silent fall:

- the forms outside the trial, among them day kinds (a duration counted in a day kind and the dayKind
  predicate among them), timelines (a total over the subject's own timeline or over a time-dependent member, the
  bucketing builtins timeProportionalPart and dayCount, wholePeriod, and a uniqueness key on a timeline, which reads
  ever-uniqueness, among them), and a dimensioned attribute over a path or one a form writes;
- what is no construct of the core or is ill-formed: "regelversie X is gevuurd", a predicate of the RegelSpraak dialect
  over the fixpoint (`spec/CONSTRUCTS.md` row 30), which the reference interpreter and the ASP backend do not evaluate
  either, and an instance-set union where a value is read (WF-110 (a)), in the sentence of the other two branches;
- the creations it does not build yet: an object creation without a `connectingRole`, whose relation would follow from
  the pair of types, or with a back-link that is ambiguous (the exec-model branch faults there at fire), a created
  relation that is not one of two roles, a division or a square root in a member initialisation, whose §6.5 or §6.6
  deposit the branch does not make, a fold whose range of more than one step goes over a created relation, a fold
  whose filter can fault over a created relation and a quantifier fold whose matrix can fault or deposit over one,
  whose made links have no place in the fold order, and the scope of a uniqueness over a created relation;
- a binder of a `Let` around a fold, read in the fold's filter or matrix: the contribution rules compute the filter
  and the matrix per member and do not bind the `Let`'s value, which carrying it would bind, read on the fold's
  subject, in every contribution rule, with a leeg value ending the `Let` across the rule boundary (ch. 10.6). A
  binder that no `Let` encloses keeps its WF-064 refusal, and a `Let` inside the filter plays. Witness
  `TmsVouwbinderWitnessTest`;
- the shapes of a fold's range the bean model gives no field, refused as the exec-model branch refuses them (§13.3.9):
  a hop over a relation whose two roles are both to-many, and, until a card of its own measures it, a to-one member
  role whose sibling is to-many;
- the value forms whose meaning the spec does not decide: a text part of a number, a truth value or a date in a text
  composition or a concatenation, where the reference interpreter ("5.5", "waar") and the runtime's `rsTekst`
  ("5.50", "true") differ; a power whose exponent is not a whole literal of at least 0, which reaches the real power
  that ch. 2.1 leaves OPEN; and a time window under `GEEN` or a counting quantifier, which has no reading (WF-112), in
  the sentence of the other two branches;
- a parameter whose Java name is `rekendatum` beside a form that reads the evaluation date, since the case runner sets
  the date under that name;
- what the design cannot carry: two initialisations on one target that apply to one instance, on one type or on a
  type and its ancestor, which nothing ranks (ch. 5.4, WF-079); a rule beside a defaults group of its type on its
  target (WF-050); and a division in a default, or in the condition or the value of an initialisation whose target
  has another writer on an instance it writes (per concrete type: a writer on a sister subtype does not count), which
  ch. 7.7 reads as the lowest default beside it: the branch computes every default whose reads stand, also one another
  default displaces, so a division by zero there is a fault on an intermediate state, which the branch withdraws once
  the default is displaced; whether such a fault counts waits on a decision of the norm (ch. 5.2, 5.4, 7.1, 7.7; card
  `fout-op-tussenstand`, where the reference interpreter gives the fault or the value of an initialisation beside
  another writer depending on the order of the rules); and a square root in a default, whose §6.6 deposit a default
  makes only where it decides (ch. 7.1);
- of an allocation: another writer of its share target or its rest on an instance it writes (per concrete type, so
  not a writer on a sister subtype), a second allocation (WF-026) or a defaults group (WF-050), both ill-formed, and a
  computing rule or an initialisation, which this branch does not carry beside an allocation yet; a pot, share, rest,
  weight or cap that is no number; a to-one recipient role whose sibling is to-many, and a join the bean model gives
  no field, as a fold over such a role is refused; and the combinations of method and fields that WF-025 refuses;
- every cycle of the dependency graph that the verifier refuses, refused by `Kringen` with a `tms:` sentence over the
  verifier's own graph (`checks.ml`, the nodes of IR-SPEC ch. 5.1: attribute, characteristic, channel, population,
  relation): a cycle that does not pass through a creation member of a recursive rule group (WF-045), and a negative
  edge on any cycle (WF-046). Under truth maintenance a positive cycle sustains itself, an even negative cycle settles
  by model order, and an odd one changes until the fire cap. One edge more than the verifier draws, a rule that writes
  the channel of its own name, makes a known over-refusal: where a rule reads the channel of a computing rule that
  reads the reader's target, the verifier sees no cycle and the reference interpreter computes the document, while
  this branch refuses it. It refuses none of the goldens the loader loads (`TmsKringVerifierWitnessTest`).

The loader and the bean model refuse on this branch as on the other two.

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
als deler is een fout, geen leeg (RUNTIME-SEMANTICS ch. 1.4, punt 4)". So the replay (`ERROR DelingDoorNul: …`), the
red path and the scenario report read the sentence, in a rule's value on both branches and in a default's
justification or consequence on the exec-model branch (the DRL-text branch has no defaults lowering); a defaults group
carries the name this backend gives it (`defaults A:Vondst.verhouding`). Any other `ConsequenceException` passes
`benoem` unchanged. The red path keeps its pinned fragments, since the cause chain still carries "Breuk deling door 0".
Witness `NuldelerWitnessTest`.

A division in a condition is tested in a constraint and not in a consequence, and leaves the fire as Drools'
`ConstraintEvaluationException`. On the exec-model branch that exception named neither the rule nor the condition
("Error evaluating constraint '' in  and in more rules", measured 2026-10-02), because a lambda carries no
`PredicateInformation`. Since then the predicates of a condition's constraints carry their rule and the condition's
source text (`transpiler.model.BenoemdeToets`: a rule's condition, a counting composite, a comparison with a parameter,
an aggregation compared, a fold's subselection, a quantifier fold, a consistency's assertion, a fact creation's guard
on its target), so Drools' message reads "Error evaluating constraint 'zijn gewogen gewicht gedeeld door zijn
geschatte gewicht groter is dan 1' in [Rule "zwaar" in dev.uchar.regelspraak.generated]", for any fault in the test.
A division by a numeric zero there is named by the predicate itself, after its rule and with its operands, and
`DroolsBackend.benoem(ConstraintEvaluationException)`, which `CaseRunner` calls beside the consequence arm, hands that
`DelingDoorNul` on: the replay, the red path and the scenario report read "deling door nul in de regel "zwaar": 60
gedeeld door 0 — …", and a consumer that fires a KieBase itself gets Drools' exception with the named division in its
cause chain. The DRL-text branch throws Drools' MVEL exception, which names the rule and the condition in DRL, as
before; for a subselection that can fault it names no rule (the section on a subselection that can fault). Witness
`NuldelerVoorwaardeWitnessTest`.

The naming sits after the fire and not in a consequence handler on the KieBase. Drools takes such a handler by class
name (`ConsequenceExceptionHandlerOption`) and instantiates it in every new session; where the kie API and this
backend are loaded by different class loaders, the instance does not cast to the kie interface and no session opens.
Measured 2026-10-02, when this backend set such a handler, in the distribution's Quarkus test of its decision service
(`GoldenOverQuarkusTest`): `ClassCastException: class dev.uchar.regelspraak.cases.DroolsBackend$Nuldelingmelder cannot
be cast to class org.kie.api.runtime.rule.ConsequenceExceptionHandler`, at `DefaultAgenda.<init>`, on every request;
the packaged Quarkus service and the JDK service opened their sessions. The KieBases `DroolsBackend` builds therefore
keep Drools' default handler. A consumer that fires a KieBase itself gets Drools' `ConsequenceException`, which names
the rule and carries `DelingDoorNul` in its cause chain, and can name it the same way: `DroolsBackend.benoem(e)` is
public for that. Witness `NuldelerAfnemerWitnessTest`.

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

## Recorded: a subselection that can fault

The subselection of a fold ("die aan de volgende voorwaarde voldoen: zijn gewicht gedeeld door zijn deelnemers is
groter dan 2") is evaluated by `ref` only where the evaluation of a form reaches the fold: where the form's condition
holds, within a composite up to the member after which its verdict stands (RUNTIME-SEMANTICS ch. 5.10), and per member
of that subject's range (ch. 3.4a). A fault there ends the evaluation (a division by a numeric zero, ch. 1.4, point 4;
the day-kind walk past its reach, ch. 3.8), and nowhere else does it happen. Both branches tested the subselection on
the element's pattern, which Drools tests for every element of the type as it is inserted, whether or not a form reaches
the fold: measured 2026-10-09, each faulted where a rule's condition did not hold, where a composite had settled before
the fold, in the condition and in the initialisation of an object creation, in the filter of a quantifier fold, and for
an element in no fold's range, one step or a path away, where `ref` answers.

A subselection that holds an operation of the closed list of `kern.Faalbaar` goes on the element bound again in the
source of the accumulate, after the join to the subject and after the walk of a path whose intermediates are an
`exists` (`accBron` puts the element's tests after it): it is tested per tuple that reaches the accumulate, for the
elements joined to that tuple's subject. The list holds every runtime operation a condition lowers to that throws on
some value of its operands: a division, a power, date arithmetic (a step over a day kind included), a date built from
parts, and the Rekenjaar. The exec-model branch uses the shape a subselection that reads through a role already had
(`ExecModelTranspiler.elementtoets`); the DRL-text branch writes `Elem( … ) from $e` in `accumulate`, in `kwantorvouw`
for the subselection and the matrix, and in `tijdreeksSomAlleSet`. A subselection without such an operation stays on
the element's pattern, so every golden without one keeps its rules (a sweep over all goldens in both branches,
2026-10-09: no report line moved). The element pattern keeps reacting to every update of its element: on the
DRL-text branch it carries `java.util.Objects.nonNull(this)`, since with its join alone it passed on no update of a
divisor another rule writes on the element and the fold kept the input's value (`verwacht 2, werkelijk 0`, measured on
the first form of this change, where the test on the element's own pattern had seen the update; an `@watch( * )` on
that pattern did not change it); on the exec-model branch an element pattern left without a test gets the opaque
always-true one. Where the fold is reached the fault still happens: on the exec-model branch a division is named after
the rule, on the DRL-text branch it is Drools' exception for the constraint, which there names no rule (on the element's
pattern it named every rule whose accumulate shared the constraint).

The members of an `alle` composite are read in their order, and a member after one that does not hold is never read
(ch. 5.10). Both branches put the tests of every member on the subject's pattern and a member's separate items (the
accumulate of a fold and its comparison, an exists) after it, so a test of a member after a fold ran before the fold:
it faulted where the fold had already settled the composite ("het aantal … is groter dan 0; zijn saldo gedeeld door
zijn noemer is groter dan 1": `ref` answers), and the fold was not evaluated where that test did not hold ("het aantal
… is groter dan 0; hij is actief" on a Jutter that is not actief: `ref` faults). The members after the first one with
separate items now go on the subject bound again from its variable, after those items (`ExecModelTranspiler.conjunctie`;
`DrlTranspiler.gesplitsteConjunctie`, `Type( … ) from $subject`, for the condition of a rule, of an object creation and
of a fact creation's anchor). The DRL-text branch also put the clauses of a rule's value, and of an object creation's
initialisations, before those of its condition, so a fold in the value whose subselection faults ran where a fold in
the condition did not hold; the condition's clauses now come first. Its §6.5/§6.6 companions read the condition's
inline constraint and joins as before: where members moved, a companion takes the condition lowered without the move.

A defaults group is one rule that folds every fold of the group on its left-hand side, for every subject, before its
consequence reads the defaults by priority, while `ref` reads a lower default only where no higher one decides (ch. 7.1)
and a justification only up to the member after which its verdict stands. A lower default whose fold divides by a
numeric zero therefore faulted where a higher one decides (measured 2026-10-09: `ref` answers 1, the exec-model branch
`DelingDoorNul: deling door nul in de regel "defaults A:Jutter.zwaarte"`), in a justification and in a consequence.
The exec-model branch refuses, by name, a defaults group with a fold over a set source whose subselection can fault; no
golden has one. A group whose folds cannot fault is lowered as before (the DRL-text branch refuses every defaults
group).

A composite under `geen` or a counting quantifier with a member that folds a date over a set source is lowered on the
exec-model branch as one test that reads the members in their order and stops where the verdict stands, after the
accumulates of all its folds (`ExecModelTranspiler.datumvouwVoorwaarde`). Drools folds those accumulates for every
subject before the test, so the subselection of a fold in a member the composite never reaches was evaluated all the
same: on "hij aan ten minste 1 van de volgende voorwaarden voldoet: hij is actief; de eerste van de vinddatum van alle
vondsten … die {een deling} … is later dan zijn peildatum" `ref` stops after "hij is actief" and answers, where the
branch divided by zero (measured 2026-10-09, under each of the four quantifiers and nested in an `alle` composite). The
exec-model branch refuses, by name, such a composite when a fold whose subselection can fault sits where `ref` can leave
it unread (`ExecModelTranspiler.overgeslagenVouwfout`): in a member before which a stop trigger can fire, whatever
number of the members before it hold, or, by the same rule, within a member that is always read and is itself a
composite. That is every member after the first under `geen`, and under `alle` in a nested composite; the first member
only where the threshold settles the composite before any member (`ten hoogste` d over at most d members, `ten minste`
or `precies` d over fewer: shapes WF-041 admits, as it asks d ≥ 1 only). Every other such composite is lowered as before
the refusal, since `ref` reads each of its folds wherever it reads the composite: a fold as the only member under
`geen`, a fold before "hij is actief" under `ten minste 1`, and a fold after "hij is actief" under `precies 1` of two
members, where no trigger can fire before it (measured 2026-10-09: the branch plays these as `ref` answers, and faults
where `ref` faults). No golden has a fold that can fault in such a composite. The DRL-text branch refuses a fold of
dates in a condition. The other routes of such a composite evaluate no fold `ref` does not read: both branches refuse a
fold of numbers in it, unless it is the single member of a composite under `geen` or `ten minste 1`, which `ref` reads
wherever it reads the composite.

Golden `jutterswet-filterwacht`, red case `vouwfilter-deelt-door-nul`, witness `FilterfoutWitnessTest`: the four members
"hij is actief", a fold that divides by the ploeg, "zijn saldo gedeeld door zijn noemer is groter dan 1" and a
quantifier fold that divides by the deelnemers in all 24 orders on four cases, so that each order pins which division
faults first and a branch that evaluates the two folds in the other order fails; a fold beside a fold of dates, beside a
member that reads through a role and in a fact creation's anchor condition, each in both orders; a fold in a rule's
value and in a creation's initialisation under a fold in the condition; the companion of a division by leeg under a
composite whose member moved; a divisor another rule writes on the element, under a fold and under a quantifier fold, in
both rule orders; the refused and the lowered defaults group; the refused and the lowered composite under each other
quantifier, the fold in a member that is always read and in one that a trigger can skip; and each other operation of
the list in a subselection.

Limits that remain, measured 2026-10-09 with `ref` as the arbiter:

- The refusal of a composite under `geen` or a counting quantifier reads the form, not the data: it counts every
  member before a fold as able to hold and not to hold. Where members cannot ("hij is actief" and "hij is niet
  actief" under `precies 1`, before the fold), it refuses a fold that `ref` reads wherever it reads the composite.
- A quantifier fold whose matrix faults on a contributor after an earlier one settled the fold: `ref` stops in the
  contributors' order (ch. 3.9a); both branches evaluate the matrix of every contributor and fault.
- Where the fold is reached: date arithmetic past the year 999 999 999 and the eerste paasdag of such a year fault in
  `java.time` where `ref` answers; zero to a negative power stops `ref` with an uncaught OCaml `Division_by_zero`.
- A negative root in a subselection: `ref` reads leeg and deposits on the §9.5 channel, this backend reads leeg and
  deposits nothing.
