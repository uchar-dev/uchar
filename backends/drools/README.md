# backends/drools — the Drools backend (IR consumer)

The Drools leg of the uchar differential square. It **consumes the uchar IR**: it loads a
golden's `ir.json` off the wire — exactly as `ref/` and `backends/clingo` do — builds a
Drools `KieBase` through its exec-model branch, which derives with truth maintenance, and
replays the `case*.json` files; its DRL-text branch renders the same analysis as DRL text. The RegelSpraak source-compile path is **never** the gate path (the maintainer
architecture anchor, 2026-08-27); a source path may serve only as an internal comparison
in tests.

## Layout

- `transpiler/tms/` — the analysis (`TmsTranspiler`), which derives with truth maintenance, with
  the fact classes it inserts, and its two renderings: `PatternDslWeergave`, Drools' executable
  model, for the exec-model branch (below, "The exec-model branch"), and `DrlWeergave`, DRL text,
  for the DRL-text branch (below, "The DRL-text branch").
- `transpiler/kern/` — parts of the lowering the analysis shares with the rest of the backend: names, the coverage manifest,
  the classification of standing targets, units, day kinds, the plans of fold sources.
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
returns a `Build` record: the model, the bean class loader, the analysis (`execModelTranspiler()`), on the DRL-text
branch its DRL text (`drl()`; null on the exec-model branch), the coverage manifest of the analysis and the `KieBase`
(`Branch` has two values, `EXEC_MODEL` and `DRL_TEXT`, the two renderings of the analysis). It is the whole prelude of the case
runner, which builds every golden through it, so gate 12 fires the KieBase a consumer gets, and it refuses in the
replay's sentences (`IrLoader.Refusal`, `IrLoader.Documentfout`, `CaseRunner.Excluded`). On the DRL-text branch it
also refuses a kind the rendering does not carry, an `Excluded` after the label `drl:` (section "The DRL-text
branch"). A DRL text that the Drools compiler rejects is no such refusal but a fault of the rendering: the build throws
an `IllegalStateException`, "drl: de DRL-tekst van de weergave compileert niet, een fout van de weergave: …". Until
card `drl-teksttak-op-truth-maintenance` the build gave such a text as an `Excluded`, "drl compile: …", so a consumer
that catches only the refusals now meets it as an exception. The analysis has one build,
and it takes every side table the loader returns beside the model: a build that left one out still produced a KieBase —
a defaults family firing nowhere, a navigated read answered as the subject's own — and no marker said so, which is why
there is no shorter way in. A document whose lowering holds two rules of one name in one package — two forms of one
name, which WF-020 forbids and a hand-written document can still carry — is refused on both branches before the KieBase
is built: the KieBase keeps one rule per name, and the destructive exec-model lowering played such a document with the
second rule in the place of the first (measured 2026-10-02: "kenmerk actief: verwacht aanwezig, werkelijk afwezig"). The
analysis refuses it in the words of Drools' DRL compiler, on both branches in one sentence: "exec-model: Duplicate rule
name: actief — …" (witness `DubbeleRegelnaamWitnessTest`).

**A session in four calls.** The replay makes them for every case (`CaseRunner`), so the gate fires a session as a
consumer does.

1. A fresh `KieSession` of `build.kieBase()`, with `build.beanLoader()` as the thread's context class loader while the
   beans are made and the session fires. The globals go on it before the fire: each parameter under its name as an
   identifier (`Namen.ident(name, false)`), the evaluation date as `rekendatum` (a `LocalDate`; a session without one
   sets none), and on the DRL-text branch every entry of `build.dagsoortGlobalen()`. A global the KieBase does not
   declare is one no rule reads; `setGlobal` refuses it, and the replay passes it over. A global keeps its value from
   the first `vuur` on: the branch states it as a fact once, at the first fire,
   so `vuur`, `lees` and `zetAfgeleideWaarden` refuse a session whose global changed after it.
2. `DroolsBackend.voegIn(build, session, invoer)`, with an `Invoer` that holds the beans, instances of the classes
   `build.beanLoader()` loads with their attributes, characteristics and reference fields set, and what the branch
   reads beside them: `emmerscalair(bean, attribute)` for every attribute on
   a timeline whose input the bean holds as the asOfDate-bucket scalar, the one segment of the period of the
   attribute's grid that holds the evaluation date (RUNTIME-SEMANTICS ch. 8.4), and `lijst(holder, role, members)`
   where the order of a to-many list is not the order of the beans, `role` being the role the holder plays, the name
   of the reference field each member holds toward it. `voegIn` states the facts the branch reads from these
   (`Emmerinvoer`, `Lijstplaats`), so a consumer states no fact class of the branch itself.
   `CaseInvoer.van(build, case)` makes that `Invoer` from a `uchar-case-v0` case, as the replay makes it for every
   case: it reads `asOfDate` and `input.instances` onto fresh beans, sets the attributes, characteristics and role
   links (RUNTIME-SEMANTICS ch. 5.7a: the written role by name, else by the type pair; on a one-to-one relation the
   far field as well; a to-many role in the far end's field; the reverse leg of a reciprocal role in a separate
   pass), and gives `beans()` by label, `typen()` and `invoer()`, every to-many list in the order the reference
   interpreter folds over. A consumer with a request of its own, such as a decision service, writes the request as
   the `instances` of a case with its ids as labels. A role link the bean model cannot hold is a
   `CaseInvoer.Rolweigering` (a `CaseRunner.Excluded`), a second, different target on a to-one field (WF-013) a
   `CaseInvoer.Rolbotsing` (an `IllegalArgumentException`); both name the link the case wrote, its instance, role key
   and target (`CaseInvoer.Rolfout`), and both come from `van`, before anything goes into a session (witness
   `CaseInvoerWitnessTest`). The parameters and the evaluation date stay the caller's, as globals (step 1).
3. `DroolsBackend.vuur(build, session)`, which fires to the fixpoint under a cap, a model with a creation in
   generations per stratum (below), and returns a `Vuring`: the firings, the cap, and `geenFixpunt`, the sentence of a
   run without a fixpoint, at the cap or at the creation budget, or null. The cap is `max(10 000, FACTOR × R × N0)`,
   with R the rules of
   the KieBase and N0 the facts just before the fire, computed again at the start of every creation phase and never
   lowered; FACTOR is 4, at least twice the largest number of firings per rule per fact measured over the played
   goldens and the scale model (`DroolsBackend.FACTOR`, with the measurement). The cap is a safeguard against a fault
   of the lowering, not a norm: the build refuses a cycle (`Kringen`), and a chain of creations ends at the creation
   budget. Nothing can be read of a run without a fixpoint: `lees` and `zetAfgeleideWaarden` refuse it.
   `DroolsBackend.vuur(build, session, waarnemer)` fires the same run and records what it fired in a
   `DroolsBackend.Waarnemer`, made with a function that gives the label of an input bean and null for any other
   object: `vuringen()`, per form of the document without its version suffix, how often its rules fired and the labels
   their matches bound, and `vormen()`, the exact names of the forms that fired, from which
   `DroolsBackend.dekking(build, vormen)` gives per manifest entry `GEVUURD` or `NIET_GERAAKT`. The scenario report
   (`CaseRunner.rapport`) is made from these two. A waarnemer belongs to one session and is refused at a `vuur` of
   another, and its listener is on the session only during that `vuur`, so parallel sessions of one build each record
   their own firings (witness `WaarnemerWitnessTest`).
4. `DroolsBackend.lees(build, session)`, or at the end `DroolsBackend.zetAfgeleideWaarden(build, session)`; or
   `lees(build, session, label)` and `zetAfgeleideWaarden(build, session, label)` with a function that gives the
   label of an input bean, and null for a made object, by which a fault then names its object as the reference
   interpreter does ("V, een Vondst"), where without labels it names the type and the identity. Both read only a
   session whose last `vuur` reached its fixpoint and in which nothing happened outside `vuur` since (below), so what
   `lees` gives is always a fixpoint, that of the input as the session knows it: an attribute or a characteristic set
   without `session.update` is not seen (below). After the write-back, `DroolsBackend.vergelijk(build, session, label,
   expected)` compares the `expected` of a `uchar-case-v0` case with the session and returns a
   `DroolsBackend.Vergelijking`: per expectation a `ScenarioResultaat.VerwachtingResultaat`, the diagnose of the
   scenario report, and `pass()`. `label` is a function as above, and an expectation names the bean to which it gives
   the expectation's label, so the labels may be the ids of a request rather than those of a case file. It reads the
   beans, which carry the fixpoint only after `zetAfgeleideWaarden`, and refuses a session that was not written back,
   one whose fixpoint carries evaluation faults, what `lees` refuses, a label given to two beans, an expectation on a
   member of a label no bean carries, and, as `CaseRunner.Excluded` in the replay's sentence, a form it does not
   compare. The replay and the scenario report compare through it (witness `VergelijkingWitnessTest`).

**One write-back per session.** The beans hold the input alone until the write-back.
`lees` reads what stands at the fixpoint without writing anything, as a `Fixpunt` (`transpiler/tms`): per object the
derived attribute values in the form the bean field takes them (a timeline as the bean model's `Segment`s, a leeg as
null), the derived characteristics, the objects the creations made with the creation and its subject, the links they
laid with the fact type and both roles, the evaluation faults that stand, as data, and the §9.5 deposits.
`zetAfgeleideWaarden` writes exactly that on the beans, read by the same code: each value and characteristic, and each
link a creation laid on the reference fields that hold it, so that a consumer that reads the beans reaches a made
object as it reaches one of the input. Then it closes the session with a stated mark, and `vuur` refuses it from then
on with a sentence that says what to do instead. `vuur` leaves a stated mark of its own after every run, of how the run
ended, and `lees` and `zetAfgeleideWaarden` refuse a session without one, fired with a plain `fireAllRules` or not at
all, a session whose last `vuur` ended at the cap, at the creation budget or with an exception, and a session in which
something happened outside `vuur` after its last run (below), each with a sentence that says what is missing. Both
branches do so alike: the DRL-text branch renders the same analysis, and its rules write no bean either.

**Firing again.** A consumer that fires one session more than once changes an attribute or a characteristic of a
bean `voegIn` put in between the fires, with `session.update(handle, bean)`, fires with `vuur`, and reads every answer
with `lees`; the write-back comes once, after the last fire. A derivation whose reads changed lapses, a fault arises or
lapses, and an initialisation fires again, since its target stayed empty on the bean (witness
`TmsHerhaaldVurenWitnessTest`). Two rules hold a session to that order. The entry keeps both with a watch on the
session (`Bewaking`), a listener that `vuur` takes off while it fires itself, so that Drools makes no event for it and
it costs a fire nothing per firing; what Drools reports by no event, a cleared agenda group and a call on a group that
waits for the next fire, the watch reads from Drools' agenda (`org.drools.core`, the one place the backend reads an
internal class of Drools). An action of another kind on the agenda's propagation list than an insert, update or delete
of a fact or one of those two calls on a group, which another version of Drools could queue there, makes the watch
refuse in a sentence that names its class, since it cannot tell what that action does to the agenda.

- `lees` and `zetAfgeleideWaarden` refuse a session in which something happened outside `vuur` after the last
  `vuur`: a fact inserted, updated or deleted, or a rule that fired, by a plain `fireAllRules` among others. Their
  sentence names what happened and the way on: fire again with `vuur`, or open a fresh session where `vuur` refuses
  this one (a model with a creation, a session written back, input that came or went). So an update shows only after
  the next `vuur`, and a plain fire never takes the place of one (witness `TmsAfnemerWitnessTest`).
- `vuur` refuses a session in which, since `voegIn` or the last `vuur`, more happened outside it than an update of an
  attribute or a characteristic of a bean `voegIn` put in, naming what happened, and sends the consumer to a fresh
  session with all beans in one `Invoer`; `lees` and `zetAfgeleideWaarden` then give that way too. That is: a fact
  inserted or deleted, a bean, a bucket input or a list place among them, or a fact of the branch, which the branch
  states itself; an update of a fact that is no such bean; an update that changed a relation, a reference field, since
  `voegIn` numbers the places of a list over the relations of one input, and a relation changed later keeps a place
  it no longer has or has none, so the first fault of a fold names the wrong member or the fold stops the fire; a
  plain `fireAllRules`, which fires without the cap and the generations; and a change of the agenda, `Agenda.clear`,
  `AgendaGroup.clear` or `AgendaGroup.setFocus`, which drops what the network had to fire or fires another group
  first. After the first `vuur` it refuses as well a global that changed, and a session from which a consumer took the
  watch, where what happened is not to be seen. `vuur` also repeats the reference check of `voegIn` over the beans of
  the session, which a reference field set without an update can fail, and refuses in the sentence of `voegIn`. It
  compares the reference fields of the beans `voegIn` put in with what they held then: a relation changed without an
  update makes no event, and Drools keeps reading the old relation, so `vuur`, `lees` and `zetAfgeleideWaarden` refuse
  such a session, naming the field and the two objects, and send the consumer to a fresh session. The links a creation
  lays on those fields at the write-back count as none (witness `TmsHerhaaldVurenWitnessTest`).

A session of a model with a creation fires once, and `vuur` refuses it at every later call: an object or a fact
creation is stated and never lapses, and so are its initialisations, its links and the fault of a failed
initialisation (RUNTIME-SEMANTICS ch. 5.5), so after an input change a second fire would keep them beside an input they
no longer follow from, a state no fresh evaluation gives. A consumer of such a model opens a fresh session per input.
Written back between the fires, a derived value would be read as input by the next fire and keep an initialisation
from firing; that is why the write-back closes the session. A changed relation asks a fresh session, since the places
`voegIn` numbered decide which member a fault names; another convention of a timeline input asks a fresh session too.
After
the write-back the beans hold derived values beside their input, so a fresh session takes beans that hold the input
alone.

**Faults.** An evaluation fault is a fact of the fixpoint, on both branches, not an exception out of the fire: `lees` gives it as data (`Fixpunt.fouten`, the subject, the rule and the exception), and `zetAfgeleideWaarden`
throws what stands before it writes anything, and then writes nothing: one fault as its own exception, a
`Breuk.DelingDoorNul` (an `ArithmeticException`) for a division by a numeric zero, a `Rekenfout` for a fault of a
computation or of the evaluation date, a `Schrijversconflict` (an `IllegalStateException`) for two writers of one
attribute that disagree (RUNTIME-SEMANTICS ch. 5.7) and an `Invoervormfout` for a timeline input in the other
convention (ch. 8.4); two or more as `Evaluatiefouten`, with each fault in `fouten()`. Any other exception is no
evaluation fault: a broken contract or a bug.

**Deposits, made objects and links.** The §9.5 deposits are the facts of the generated class `Inconsistentie`, with
the rule and the subject: `lees` gives them, and on both branches a consumer reads them from the working memory. A made
object is a bean in the working memory of the session; on the exec-model branch `lees` names it with its creation and
subject, and after the write-back the reference fields reach it.

**What a consumer can forget, and what happens.**

- A bucket scalar: the branch reads that input as a full timeline of one segment, which is valid input it cannot tell
  apart. A form that needs the full timeline, such as the base of a pro-rata part, then computes over that one segment,
  without a fault, where the stated bucket scalar faults with an `Invoervormfout`; a form that needs the bucket scalar,
  the subject's own total without a clip, faults with an `Invoervormfout`.
- A list: its members keep the order of the beans, which decides only which member a fault names.
- A member in a list it gives: `voegIn` refuses by name, before it inserts anything; so it refuses every statement that
  does not fit the beans or the model, but for the shape of a bucket scalar's timeline (next), a bean of another bean
  model, a bean twice, and a session that is not fresh.
- The one segment of a bucket scalar: `voegIn` checks that the attribute lies on a timeline, not that the bean holds it
  as one segment, the period of the evaluation date (RUNTIME-SEMANTICS ch. 8.4). A bucket scalar on an empty or a longer
  timeline, given so or set so by an update between two fires, stops the next `vuur` with an `IllegalStateException`
  in Drools' `ConsequenceException` ("een emmerscalair staat op de bean als het ene segment van de periode van de
  rekendatum, niet als …").
- `session.update` after a change of an attribute or a characteristic: Drools does not see a field set without an
  update, and neither does the entry, which compares only the reference fields with what `voegIn` put in. `vuur` then
  reaches the fixpoint of the old value, and `lees` and `zetAfgeleideWaarden` give it without a refusal; step 4 of card
  tms-omschakeling measured it with the second fire of `Schaalproef` and a mutant without the update.
- An object a bean refers to: `voegIn` refuses a reference field that holds an object outside the input, naming the
  bean, the field and the object's type, before it inserts anything. The branch derives nothing for an object outside
  the session, so every value read through the field would stay empty, and the link the field gives would still block
  a creation. `vuur` repeats the check over the beans of the session, so a relation that an update set to an object
  outside the session is refused there, in the same sentence.
- `voegIn` itself, with the beans inserted by `session.insert`: no bucket scalars, so every timeline input reads as a
  full timeline, and no places, so a rule that needs one stops the fire with an `IllegalStateException` ("… heeft geen
  Lijstplaats …").
- A fresh session for a new or a removed object, or a changed relation: `vuur` refuses a session into which a bean,
  a bucket input or a list place came, or from which one went, outside `voegIn`, one in which an update changed a
  reference field, naming it, after `voegIn` as between two fires, and, with `lees` and `zetAfgeleideWaarden`, one in
  which a reference field changed without an update.
- A fresh session for another global: `vuur`, `lees` and `zetAfgeleideWaarden` refuse a session whose global changed
  after the first `vuur`, naming the global with both values; without that refusal the next fire kept the old value
  (measured 2026-10-10: aanvoergeld 322.50 where a fresh session gives 332.50).
- The facts of the branch and the agenda: `vuur` refuses a session in which a consumer inserted, deleted or updated a
  fact of the branch, or cleared the agenda or a group of it, or moved the focus, naming it.
- `vuur`, with a plain `fireAllRules`: `lees` and `zetAfgeleideWaarden` refuse the session, on every model, with a
  sentence that names `vuur`; on a model with a creation the creations never fired.
- `vuur` after an update, or after a plain fire that follows a `vuur`: `lees` and `zetAfgeleideWaarden` refuse the
  session, naming the first fact that changed and the first rule that fired since the last `vuur`, and write nothing;
  after an update of an attribute or a characteristic alone the next `vuur` makes the session readable again, and
  after a plain fire `vuur` refuses the session too, so the way is a fresh session.
- The write-back: the beans hold the input alone, and `lees` holds the answer.
- `geenFixpunt`: a run at the cap or at the creation budget has no answer, and `lees` and `zetAfgeleideWaarden`
  refuse the session with the sentence of that run, as they refuse one whose `vuur` threw. Without a creation a further
  `vuur` that reaches the fixpoint makes the session readable again; with one the way is a fresh session.
- A second `vuur` on a model with a creation: refused, with the reason and the way, a fresh session per input.

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

`scripts/check-drools.sh` replays every corpus golden through both branches. The exec-model
branch is the square: a golden whose forms the loader+transpiler carry **must** match the
pinned expectations gates 5 (ref) and 6 (clingo) verify — a played pass is the square — and
`EXPECTED_PLAYED` pins the played count as an equality, 255 since card
`drl-teksttak-op-truth-maintenance` (2026-10-14), which brought five goldens of its own (250
since card `objectcreatie-zonder-connectingrole-binding`, 2026-10-11), every golden the
destructive exec-model lowering played among them. The DRL-text branch is swept after it and must give
every golden the status line of the exec-model branch, `PLAYED n` or the same refusal; it has
no list and no pin of its own (proposed with card `drl-teksttak-op-truth-maintenance`; until
then it had `EXPECTED_DRL_PLAYED`, 197, and a list of non-players, `DRL_CHANNEL`). Not-carried
forms never vanish silently: per-form auto-refusals print their reason, and a
reason-carrying value-divergence list in the script (the check-noop survivor pattern)
names each golden whose fired outcome differs from ref/clingo and fails when stale.
`TmsCorpusWitnessTest` holds the played count of the exec-model branch as a floor in the
suite and reports its refusals per family and form, which the gate does not; the pin of
the gate is the truth.

## The DRL-text branch

`DroolsBackend.Branch.DRL_TEXT`, the flag `--drl` of the CLI: the analysis of the exec-model
branch rendered as DRL text (`transpiler/tms/DrlWeergave`, through `TmsTranspiler.drl()`) and
compiled by the classic DRL build (`KieHelper`) in EQUALITY mode. It is the second rendering of
one analysis (card `drl-teksttak-op-truth-maintenance`, 2026-10-14): the same rules under the
same names, the same facts, the same truth maintenance and the same driver, so every entry of
a session treats it as the exec-model branch, and what this README says of that branch holds
for it. Every RegelSpraak comparison or computation is a Java function of the text, with the
source sentence above it; the structural constraints stay DRL. The predicates of the day kinds
are globals of the text (`Build.dagsoortGlobalen()`). It refuses three kinds of its own,
after the label `drl:` (`DrlWeergave.Weigering`): a member whose name Drools reads as
something else where a pattern spells it (a word of DRL or MVEL such as `in`, or a variable of
a consequence such as `drools`), a day kind whose global is no Java identifier, and a bean class
as the constant of a term; a text the compiler rejects is a fault of the rendering, an
`IllegalStateException`. Gate 12 holds it to the status
line of the exec-model branch on every golden; the price is the time of the fire, 1.8 to 2.3
times that of the executable model on the scale model.

Until that card the DRL-text branch was a lowering of its own (`DrlTranspiler`, with
`Stratificatie`): its rules wrote the beans as they fired, ordered by salience bands, the
design the exec-model branch had until card `tms-omschakeling`, without a defaults lowering.
Its limits measured on hand-written documents stood in the sections "Recorded: …" that
described both destructive lowerings; the history of this file keeps them.

## The exec-model branch

`DroolsBackend.Branch.EXEC_MODEL`, lowered by `transpiler/tms/TmsTranspiler` to Drools' executable model: the branch
gate 12's square replays and the distribution builds with. It derives with Drools' truth maintenance instead of writing
the beans, so it needs none of the measures a lowering that writes the beans takes against the engine's own reactivity
— `NO_LOOP` on a rule that writes its own subject, salience from the stratification, opaque class reactivity, `update`
only on a changed value, the `initEigen` guard of an initialisation. It began as the trial of the card
`drools-logische-afleiding` beside such a destructive exec-model lowering, and on its measurements the owner decided to
build it out into that lowering's replacement (PLAN §9, 2026-10-08), in four cards: `tms-aggregaties-creatie-en-fouten`
added aggregations, object and fact creation and evaluation faults as logical facts; `tms-expressievormen-en-rekendatum`
the value forms, the evaluation date, the faults of date arithmetic as facts, the condition forms and the writers of
one target; `tms-verdeling-en-tijdlijnen` allocations, timelines, the whole-period qualifier and day kinds; and
`tms-omschakeling` made it the exec-model branch on 2026-10-10 and removed the destructive lowering, the branch value
`TRUTH_MAINTENANCE` and the flag `--tms`. Each card's `Result:` gives its measurements. It plays 250 of the 295 goldens
with cases (since card `objectcreatie-zonder-connectingrole-binding`, 2026-10-11), every golden the destructive lowering
played among them. `TmsMeting` and `Schaalproef` in the test tree
measure it, their javadoc says how to run them, and the suite runs its witnesses (`Tms*WitnessTest`), among them
`TmsCorpusWitnessTest` (above, "Gate 12").

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
exception group below a displaced default settles no `Uitkomst` and raises no fault at the fixpoint, so no control
fact stands under a displaced default.

**The write-back.** After a fire that reached its fixpoint, `DroolsBackend.zetAfgeleideWaarden(build, session)` puts
the derivations on the beans: `setX(true)` per `HeeftKenmerk`, the attribute's setter per `Waarde`. Input on a
characteristic stands, since nothing writes false; a derived value replaces input on an attribute, a computed leeg
of a standing writer writes nothing, so the input stands (ch. 7.1, 7.8), and a computed leeg of a writer outside the
standing domain empties the attribute (ch. 1.2g). Two different values on one attribute of one object have no common
fixpoint: they stand as a fault (below), and the write-back never chooses. The replay passes the case labels to
`zetAfgeleideWaarden`, so that the fault names its object as the reference interpreter does, and `lees` takes the same
labels; a consumer without labels gets the type and the identity. It writes what `DroolsBackend.lees` reads, a link a
creation laid on the reference fields among it, once, after the last fire of the session, which it then closes: the
beans then hold values the network never saw arrive, and `DroolsBackend.vuur` refuses the session (above, "One
write-back per session").

**Evaluation faults are logical facts.** A division by a numeric zero (RUNTIME-SEMANTICS ch. 1.4, point 4) is a fault,
and so is every fault of `Rekenfout`: a date step or a duration count outside the range of ch. 3.8, a walk over the
days of a day kind that passes its reach (ch. 3.8), a context value
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
as a `DelingDoorNul`, named after the rule, in the sentence the destructive DRL-text lowering gave it out of its fire
until card `drl-teksttak-op-truth-maintenance` ("deling door nul in de regel "aandeel": 100 gedeeld door 0 — …"), one other fault as its `Rekenfout`, named
after the rule and in the reference interpreter's sentence wherever that executor names the fault ("fout in de regel
"meldtermijn": duration count … is out of range"), two or more as `Evaluatiefouten`, ordered by the name of the form and
then by the sentence, so the report does not depend on the order of the rules. That message opens with a head that names
the rule of every fault ("aan het fixpunt staan 2 evaluatiefouten, in de regels "aandeel", "verhouding""), and the CLI's
ERROR line bounds only the sentences after it, so no fault drops off the line. A consumer meets the fault at
`zetAfgeleideWaarden`, or as data in `DroolsBackend.lees`, not out of the fire. Witnesses `TmsFoutWitnessTest`,
`TmsVoorwaardefoutWitnessTest` and, for the date faults, `TmsDatumfoutWitnessTest`, among them a duration count that
lies outside the range only on an intermediate state, where the branch threw out of the fire until 2026-10-09. Other
exceptions are no evaluation fault and still leave the fire: a broken contract and a bug.

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
exact-digit tests compute through the runtime's helpers from bound values; the last three and the sub-day extractors,
dateWith and dateTimeWith, the reference interpreter declines (ch. 5.11), so only this backend computes them. The
evaluation date is the fact `Rekendatum`, put in working memory once per session from the global `rekendatum` the case
runner sets, with a null date in a time-free session. A version with a bounded validity gates on it with a pattern that
admits the date (ch. 8.2) and beside it a rule inserts the fault where the session carries no date, as the reference
interpreter faults when it evaluates the form, with or without a subject; a defaults group with a dated validity gates
its own chain the same way and faults per object of the stack's type, where the reference interpreter asks the group's
window. A `TimeWindow` on a scalar target and the context values `evaluationDate` and `evaluationYear` read the date as
a bound value. Witnesses `TmsLetWitnessTest`, `TmsRekenvormWitnessTest`, `TmsBuiltinWitnessTest` and
`TmsRekendatumWitnessTest`.

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
7.7, as `transpiler/kern/StaandeDoelen` classifies it for both branches) a computed leeg inserts nothing; outside
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
intermediate state goes again. It computes with `rsPlafond`, `rsAfronding` and `rsVerdeelOpVolgorde`: a share is the
quotient at 16 significant digits, capped, then floored, the v0 residual ch. 4.2 records beside the exact fractions. The
edge rows of ch. 4.4 write nothing for no recipients, for an empty pot and for eligible proportional weights that sum to
0, the last still with the deposit of an empty weight beside them. An empty pot is no distribution in every method, as
ch. 4.4 decides for v1: no share, no rest, no deposit, not even for an empty weight, and no fault, so an input on a
share stays and a pot derived later leaves nothing from its empty state. Four readings differ from the destructive
lowering this branch replaced, each as the spec has it: that empty pot in the equal-shares and the proportional method,
where the reference interpreter and the ASP backend keep the v0 hole and fault ("empty source amount in equal-shares
allocation (v0 mirror hole, ch. 4.4)", `viol("allocation: empty pot")`) and the destructive lowering threw a
NullPointerException; a negative weight of the proportional method counts as 0 with a deposit (ch. 4.4, decided for v1),
where the reference interpreter keeps the unclamped v0 hole, as the destructive lowering did, and the ASP backend
faults; an empty tie-break weight counts as 0 also where every weight of its group is empty, as the reference
interpreter and the ASP backend read it, where `rsVerdeelOpVolgorde` splits such a group in equal parts; and two
ordering keys of one value in two scales, 2019 and 2019.0, are one group, as those two executors compare them, where
that helper groups by `equals`. A version with a bounded validity gates on the evaluation date and faults a session
without one; a condition faults and deposits through rules of its own, on the source. A share or a rest replaces the
input on its target, and a recipient that receives nothing keeps its input. Witness `TmsVerdelingWitnessTest`.

**Timelines.** A timeline (RUNTIME-SEMANTICS ch. 3) is a value in a fact: an immutable `List<Tijdvak>`, where the
record `Tijdvak` is one segment `[van, tot)` with its value. Its equality is the segment-wise equality of ch. 3.1, as
the reference interpreter compares two timelines (`write_timeline`): 7 and 7.00 are one value, the same values on
another cut are another; its `hashCode` agrees with it, since the KieBase in EQUALITY merges equal logical facts. The
generated bean `Segment` has no value equality and is mutable, so the branch converts at two boundaries only: where it
binds a timeline off a bean, the input of an attribute or the true periods of a characteristic, and where
`zetAfgeleideWaarden` puts a derived timeline on the bean as its `Segment`s. `Tijdvak` has the getters and the
constructor the runtime's helpers ask of a segment class, so `rsTijdsevenredig`, `rsAantalDagenIn`, `rsTijdTotaal`,
`rsTijdKnip`, `rsTijdConditie` and `rsOoitWaarden` compute on it unchanged. A timeline is bound in the left-hand side
like any read, an attribute through its standing, derived or input, and read whole: by the temporal total of the
subject's own timeline, with the clip of ch. 3.6 by a characteristic, negated or not, by an `alle` of characteristics
or by an ordering of an own timeline against a value; by a sum or a total per member of a set (ch. 3.6, the
per-element fold), where each member's timeline collapses over time in its contribution rule and a member without one
contributes nothing; and as a uniqueness key, which reads ever-uniqueness (ch. 5.9): one key per value the timeline
ever carries. It is written whole, only by a pro-rata part (ch. 3.7) or a day count (ch. 3.8), on the calendar grid or
from a fixed start point (ch. 3.2). On a standing temporal target (ch. 7.8) an empty result writes nothing and leaves
the input whole; elsewhere it writes the empty timeline (ch. 1.2g), which the write-back puts on the bean as an empty
list, while a target never written stays null. A characteristic on a timeline is input only, read as its true
periods; the case reader refuses one given in the flat list of characteristics, for every branch. Witnesses
`TijdvakTest`, `TmsTijdlijnWitnessTest`, `TmsTijdlijnVouwWitnessTest`, `TmsOoitUniciteitWitnessTest`, and for the
refusals below `TmsTijdlijnWeigeringWitnessTest`.

**The two input conventions of a timeline.** The case gives an attribute on a timeline either as its full timeline or
as the asOfDate-bucket scalar, one value for the period of the attribute's grid that holds the evaluation date, and
RUNTIME-SEMANTICS ch. 8.4 keeps the two apart: the bucket scalar serves the subject's own total without a clip, the
full timeline every other read, and "an executor that needs the one and finds the other faults — never guesses". The
bean shows a bucket scalar as the one segment of its period, which a full timeline of one segment would be as well, so
on this branch the case runner states an `Emmerinvoer(bean, attribute)` beside the beans for each bucket scalar, the
attribute keyed by its declaring type as `ProducentConsument.elementVan` gives it. Two input rules of the attribute's
standing join that fact, under `exists` and under `not`, and the input stands as an `Emmerscalair` or as a
`List<Tijdvak>`; a timeline a form derives replaces it, as any input. A form that finds the other convention faults
where the evaluation reaches it, an `Invoervormfout` inserted logically as a `Fout`, in the reference interpreter's
words and naming the object, by its case label where the write-back has one: the own total without a clip over a full
timeline; and over a bucket scalar the clip of a total, the timeline a comparison clip compares, the base of a
pro-rata part, a member of a per-element fold (the first in fold order, after the filter faults, as a `Lidfout`), an
ever-unique key, on every instance of the type before the scope narrows the rows and up to an empty component before
it, and the timeline the whole-period qualifier folds (below). `DroolsBackend.voegIn` states the same facts for a
consumer, from the bucket scalars and the lists its `Invoer` names (above, "A session in four calls"): an `Emmerinvoer`
per bucket scalar, without which the branch reads every input timeline as a full timeline, and where a rule reads them
(`TmsTranspiler.lijstvelden`) a `Lijstplaats` per member of every to-many list, without which the fire stops with
Drools' `ConsequenceException` around an `IllegalStateException` ("… heeft geen Lijstplaats onder het veld …") as soon as
a rule needs the place of a member that has none. Witness `TmsInvoerWitnessTest`. Every quantifier fold over a
to-many role whose matrix compares or tests an attribute under the whole-period qualifier reads them, whether or not the
input holds a bucket scalar, as in golden `jutterswet-reddingsvloot`: that matrix can fault per member on a bucket
scalar, so the fold reads its members in order (`Lidtoets`, above); a characteristic check under the qualifier cannot
fault and asks no places. The reference interpreter and the ASP backend read a pro-rata base, a compared timeline and a
timeline under the whole-period qualifier given as a bucket scalar as no timeline today; this branch follows ch. 8.4
there, and they follow with a card. The own total without a clip in a session without an evaluation date has no bucket
to take and faults, as in the reference interpreter ("the temporal total of "bedrag" needs the evaluation date: the case
carries the asOfDate bucket's value"), where the evaluation reaches it. Witnesses `TmsInvoervormWitnessTest` and
`TmsTotaalRekendatumWitnessTest`.

**The whole-period qualifier.** "gedurende het gehele jaar" and "gedurende de gehele maand" (RUNTIME-SEMANTICS ch. 3.9)
fold a timeline that the left-hand side binds into a truth value: for every calendar year or month the timeline
touches, the test must hold on every day of it. The branch folds with the runtime's `rsGeheelPeriode`: for a
comparison, the subject's own timeline against a bound, which is any value of the rule,
restated into the timeline's unit and read as a decimal, the operator reversed where the timeline stands on the right;
for the leeg and the gevuld test, the timeline of an attribute of the subject or of the object a path of to-one roles
reaches; for a characteristic check, its true periods, of the subject or over such a path. An attribute is read
through its standing at the end of the path as well, so the fold reads it whole and faults on the asOfDate-bucket
scalar (above), naming the object through the path ("the werf of "S1""). The grid is the
qualifier's own, never the member's nor the session's: no rule of the qualifier binds the evaluation date, and a
session without one answers. A timeline without segments, and the null timeline of a path whose link is absent, touch
no bucket and pass, and the qualifier's own negation complements the fold. A leeg bound fails every day, never a
fault, the inequality included, as ch. 3.9 says; the reference interpreter answers "ongelijk aan" against a leeg bound
as true on every day (measured 2026-10-09; no golden has it). In a quantifier fold (ch. 3.9a) the qualified comparison
is the matrix, so each member's verdict is a contribution of the fold, with its short circuit, and the qualifier's
negation is read per member: a counting quantifier counts the members for which the fold fails. Witness
`TmsGeheelPeriodeWitnessTest`, over timelines that a rule derives later, in every order of the rules.

**Day kinds.** A day-kind definition (IR-SPEC ch. 3.7) reads nothing of working memory (WF-031), so its predicate over
a calendar day is a constant of the document: the backend's day-kind compiler (`kern/Dagsoorten`) compiles it the first
time a form steps or counts in its kind, and the value function of the form holds it. No rule binds anything for it,
and a definition is no form of the manifest. A date or a date-time plus or minus a count of days of a kind walks the
calendar (`rsDatumPlusDagsoort`, `rsDatumTijdPlusDagsoort`, ch. 3.8), and a duration in whole days of a kind between
two dates counts them (`rsTijdsduurDagsoort`), from the date and the count that the rule binds, a derived one through
its standing. A walk that passes the reach of ch. 3.8 without a day of the kind is an evaluation fault in the
reference interpreter's sentence ("day-kind walk: no day of the kind within 40000 calendar days of …"), a `Fout` that
counts only where it stands at the fixpoint. A definition that does not compile refuses nothing while no form uses
it, so `jutterswet-schouwkalender`, whose hoogschouwdag tests a day kind on another day than the bound day, plays.
Witness `TmsDagsoortWitnessTest`.

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
guards on the reference fields and the refusal sentences, is one decision of the bean model
(`FactModelGenerator.creatiekoppeling`, with `verbindendeFeittypen`): the creation's `connectingRole` binding, or
without one the derivation from the type pair of ch. 5.5, the one fact type that relates the subject's type and the
created type. The connecting relation of a derived pair is the fact type of the two derived roles, with the fillers
compared exactly, as `Kringen` derives the back-link for the strata (`TmsTranspiler.typenpaarrelatie`); golden
`jutterswet-penningbesluit`. Its consequence makes a new bean of the
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
without the 1:1 guard, as the reference interpreter evaluates the condition first, in every pass. The replay and a
consumer fire through `DroolsBackend.vuur`; `lees` and `zetAfgeleideWaarden` refuse a session that `vuur` did not fire
to its fixpoint, or in which something happened outside `vuur` since, and on a build with a creation `vuur` fires a
session once, since what a creation made is stated. The
write-back puts each link a creation laid on the reference fields that hold it, so a consumer that reads the beans
reaches a made object. Witnesses `TmsCreatieWitnessTest`, `TmsAfnemerWitnessTest` and `TmsHerhaaldVurenWitnessTest`.

**What it refuses**, from its manifest as `transpiler SKIP: <family>: <name> — <reason>` before the KieBase is built,
never as a silent fall:

- of dimensioned attributes, with no golden with cases: a read or a write outside a dimension cell and a fold over a
  dimension selection, and in those two an attribute that lies on a timeline, is read over a path or is written by a
  form. Witness `TmsTijdlijnWeigeringWitnessTest` for a dimensioned attribute on a timeline, by its cell and by its
  total;
- of timelines, each with no golden with cases unless one is named: a second writer of a timeline target, which ch.
  7.8 keeps fenced, and an initialisation of one; a time window in the condition of a rule with a timeline target,
  which clips the timeline (ch. 3.10), as the destructive DRL-text lowering refused it (golden `jutterswet-pachtvenster`); a
  characteristic on a timeline as a target; any value of a timeline target other than a pro-rata part or a day count,
  among them a sum of timelines over a set (ch. 3.5); the comparisons "gelijk aan" and "ongelijk aan" in a clip, where
  `rsTijdConditie` reads an empty side as no period and ch. 3.6 applies the comparison of ch. 1.2 per period; a rate
  over a grid whose unit the document does not declare, or declares in two different canonical chains, which the
  reference interpreter refuses to integrate, in its words; a rate whose denominator is not the unit of the grid, which
  `rsTijdTotaal` would weigh unit-blind; a sum or a total per member over a timeline in a fold over a created relation,
  whose made links have no place in the fold order that names the member given as a bucket scalar; the total without a clip
  over a timeline a form writes, which the reference interpreter refuses as well; every form that reads or writes a
  timeline on a variable start point (the `StartPointDetermination` itself is a declaration and refuses nothing, so a
  document that declares one and uses no timeline on its grid plays; witness `TmsStartpuntWitnessTest`); a
  time-dependent parameter, which the goldens with cases read in a `TemporalRead`
  that the loader refuses for every branch; and what the reference interpreter refuses as well: a member on a grid of
  more than one period, a fold over a member on a timeline other than a sum or a total, a uniqueness key with two
  components on a timeline, and a pro-rata part over a base with a unit or finer than the bucket, or onto or from a
  fixed start point. Witness `TmsTijdlijnWeigeringWitnessTest`;
- of the whole-period qualifier, with no golden with cases: the negation inside the qualifier of a characteristic
  check ("gedurende het gehele jaar niet …"), which the destructive DRL-text lowering refused as well (ch. 3.12); and what the
  reference interpreter does not fold either: a comparison over a timeline read over a path, between two timelines or
  with none, and a date ordering (ch. 3.9, 3.12);
- of day kinds: the dayKind predicate as a condition, which the reference interpreter and the ASP backend refuse
  outside a definition (ch. 3.12, 5.4); a duration in whole days of a kind between two date-times, which they refuse as
  well (ch. 3.8, 3.12); and a form that steps or counts in a day kind whose definition does not compile: one that tests
  a day kind on another day than the bound day, which no executor carries (red case `dagsoortverwijzing-andere-dag`),
  one that reads a parameter, which WF-031 admits and the other two executors read and this branch does not (golden
  `jutterswet-havenrooster`, which the destructive DRL-text lowering refused as well), and any other shape outside the compiler.
  Witness `TmsDagsoortWitnessTest`;
- what is no construct of the core or is ill-formed: "regelversie X is gevuurd", a predicate of the RegelSpraak dialect
  over the fixpoint (`spec/CONSTRUCTS.md` row 30), which the reference interpreter and the ASP backend do not evaluate
  either, and an instance-set union where a value is read (WF-110 (a)), in the sentence of the destructive DRL-text
  lowering;
- the creations it does not build yet: an object creation whose back-link is ambiguous, which ch. 5.5 makes a fault of
  the program and this branch refuses when it builds (red case `ambiguous-backlink`), an object creation without a
  `connectingRole` whose type pair reaches the connecting relation only through a supertype of the subject or of the
  created type, since the strata derive that relation from the exact types (`Kringen`) and would not see the creation
  write it, a created
  relation that is not one of two roles, a division or a square root in a member initialisation, whose §6.5 or §6.6
  deposit the branch does not make, a fold whose range of more than one step goes over a created relation, a fold
  whose filter can fault over a created relation and a quantifier fold whose matrix can fault or deposit over one,
  whose made links have no place in the fold order, and the scope of a uniqueness over a created relation;
- a binder of a `Let` around a fold, read in the fold's filter or matrix: the contribution rules compute the filter
  and the matrix per member and do not bind the `Let`'s value, which carrying it would bind, read on the fold's
  subject, in every contribution rule, with a leeg value ending the `Let` across the rule boundary (ch. 10.6). A
  binder that no `Let` encloses keeps its WF-064 refusal, and a `Let` inside the filter plays. Witness
  `TmsVouwbinderWitnessTest`;
- the shapes of a fold's range the bean model gives no field, refused as the destructive DRL-text lowering refused them (§13.3.9):
  a hop over a relation whose two roles are both to-many, and, until a card of its own measures it, a to-one member
  role whose sibling is to-many, over a relation no form creates (a created relation is read over its `Koppeling`
  facts);
- the value forms whose meaning the spec does not decide: a text part of a number, a truth value or a date in a text
  composition or a concatenation, where the reference interpreter ("5.5", "waar") and the runtime's `rsTekst`
  ("5.50", "true") differ; a power whose exponent is not a whole literal of at least 0, which reaches the real power
  that ch. 2.1 leaves OPEN; and a time window under `GEEN` or a counting quantifier, which has no reading (WF-112), in
  the sentence of the destructive DRL-text lowering;
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
- every cycle of the dependency graph that the verifier refuses, refused by `Kringen` with an `exec-model:` sentence
  over the verifier's own graph (`checks.ml`, the nodes of IR-SPEC ch. 5.1: attribute, characteristic, channel,
  population, relation): a cycle that does not pass through a creation member of a recursive rule group (WF-045), and a
  negative edge on any cycle (WF-046). Under truth maintenance a positive cycle sustains itself, an even negative cycle
  settles by model order, and an odd one changes until the fire cap. One edge more than the verifier draws, a rule that
  writes the channel of its own name, makes a known over-refusal: where a rule reads the channel of a computing rule
  that reads the reader's target, the verifier sees no cycle and the reference interpreter computes the document, while
  this branch refuses it. It refuses none of the goldens the loader loads (`TmsKringVerifierWitnessTest`).

The loader and the bean model refuse on both branches alike, before the analysis.

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
conditional ones reach the engine. On the exec-model branch each writer's value is a logical fact, and two different
values that stand on one object at the fixpoint are a fault, a `Schrijversconflict` that `lees` gives as data and the
write-back throws (above, "Writers of one target"): "geen fixpunt: twee schrijvers van één doel geven verschillende
waarden — de regel "melding op vinddag" schrijft 2026-03-04 en de regel "melding op aanspoeldag" schrijft 2026-03-01 in
het attribuut meldingsdatum van V, een Vondst; divergentie is een fout (RUNTIME-SEMANTICS ch. 5.7)", the writers in the
order of the document and the object by its case label. A difference on an intermediate state only is no fault. With
three writers the sentence names every writer that stands at the fixpoint, where the reference interpreter names the
two that its last passes saw change the attribute.

The DRL-text branch renders the same analysis and gives the same fault. Until card
`drl-teksttak-op-truth-maintenance` its rules wrote the beans as they fired, two such writers fired each other again
until the fire cap, and the replay named the writers that kept changing the attribute from their last writes. Both
branches fault on a computing rule that evaluates
leeg beside a conditional initialisatie outside the ch. 7.7 domain, where `ref` faults and the ASP backend derives its
`viol/2` witness. Writers whose conditions exclude each other, or that write the same value, play in both branches
(golden `jutterswet-melding-naar-aard`, red case `jutterswet-dubbele-melding`, witnesses
`OverlappendeSchrijversWitnessTest` and `TmsSchrijversWitnessTest`).

## Recorded: a division by a numeric zero

A division whose divisor is a numeric zero is a fault (RUNTIME-SEMANTICS ch. 1.4, point 4; the red case
`division-by-numeric-zero`): `ref` faults with "division by numeric zero" and clingo with its fail-fast guard on the
subject. `Breuk.delen` throws `Breuk.DelingDoorNul`, an `ArithmeticException` with the message it always had ("Breuk
deling door 0") and the dividend, and `Breuk.DelingDoorNul.inDeRegel` names the rule and the operands: "deling door nul
in de regel "verhouding": 60 gedeeld door 0 — een numerieke nul als deler is een fout, geen leeg (RUNTIME-SEMANTICS ch.
1.4, punt 4)". The replay (`ERROR DelingDoorNul: …`) reads that sentence on both branches, and the red path and the
scenario report, which play the exec-model branch, read it as well.

On both branches the fault is a logical fact of the fixpoint (above, "Evaluation faults are logical facts"). The
consequence that computes a value inserts the fault in place of the value, a condition that divides gets the rule
`<rule>: fout in de voorwaarde`, and a fold's filter that divides makes the form that reads the fold fault where its
evaluation reaches the fold, so a division by zero on an intermediate state, or in a filter no form reaches, is no
fault. At the fixpoint `lees` gives the fault as data and `zetAfgeleideWaarden` throws it before it writes anything,
named as above. A division in a default is refused by name (above, "What it refuses"). Witnesses `NuldelerWitnessTest`,
`NuldelerVoorwaardeWitnessTest`, `TmsFoutWitnessTest`, `TmsVoorwaardefoutWitnessTest` and `TmsFilterfoutWitnessTest`.

Until card `drl-teksttak-op-truth-maintenance` the fault left the fire of the DRL-text branch, in the cause chain of
Drools' `ConsequenceException`, and `DroolsBackend.benoem`, which `CaseRunner` calls where it fires a session, turned
that into a second `DelingDoorNul` named as above. `benoem` stays public for a consumer that fires a KieBase itself; on
the KieBases of both branches today no evaluation fault leaves the fire, so what comes to it is a broken contract or a
bug, and it hands that on unchanged.

The naming sat after the fire and not in a consequence handler on the KieBase. Drools takes such a handler by class
name (`ConsequenceExceptionHandlerOption`) and instantiates it in every new session; where the kie API and this
backend are loaded by different class loaders, the instance does not cast to the kie interface and no session opens.
Measured 2026-10-02, when this backend set such a handler, in the distribution's Quarkus test of its decision service
(`GoldenOverQuarkusTest`): `ClassCastException: class dev.uchar.regelspraak.cases.DroolsBackend$Nuldelingmelder cannot
be cast to class org.kie.api.runtime.rule.ConsequenceExceptionHandler`, at `DefaultAgenda.<init>`, on every request;
the packaged Quarkus service and the JDK service opened their sessions. The KieBases `DroolsBackend` builds therefore
keep Drools' default handler. A consumer meets the fault in `lees` or at `zetAfgeleideWaarden`, on either branch.
Witness `NuldelerAfnemerWitnessTest`.
