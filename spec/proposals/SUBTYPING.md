# Object-type subtyping — the design decisions

*Status: PROPOSAL · 2026-09-11 · working prose, not normative. This is
the deliverable of the research card
`backlog/new/20260910-subtype-onderzoek.md` and answers its seven
numbered questions in decided form, so that
`20260910-subtype-kernel`, `20260910-subtype-regelspraak-dialect` and
`20260910-flogic-frontend-minimaal` can be executed without open
design questions. The maintainer's mandate
`OPDRACHT_OBJECTTYPE_HIERARCHIE_20260910.md` is BINDING throughout, in
particular its decision 2 (single inheritance) and its decision 3
(inheritance is defaults-group inheritance with base override, one
priority axis). That mandate is the maintainer's own document and is
deliberately left untracked on this branch: it uses one of the terms
the 2026-08-29 prioritized-defaults rename retired, so gate 1's rename
tripwire goes red the moment it is tracked, and neither that tripwire
nor the mandate is in this card's may-touch set (§11 point 6).
Nothing here changes `IR-SPEC.md`,
`RUNTIME-SEMANTICS.md`, the schema, the verifier, an executor, a
frontend or the corpus; every repo claim below carries the file and
line it was measured at, on this branch. Like
`PRIORITY-RELATION.md`, this document lives outside every gate-1 zone.
External reference: Kifer, Lausen & Wu, "Logical Foundations of
Object-Oriented and Frame-Based Languages", JACM 42(4):741–843, 1995
— "the JACM paper"; and its implementation lineage Flora-2, whose
inheritance semantics is Yang & Kifer (ODBASE 2002; Journal on Data
Semantics 2800:69–97, 2003), measured here on a real installation
(§8).*

## 1. The question, precisely

Today an object type is a flat record: a name, an animacy flag, a
plural, attributes and characteristics (`IR-SPEC.md` ch. 2.2). There
is no relation between object types other than the fact types that
link their instances. The mandate's §1 records why that is now a
defect rather than a simplification: the canonical data model the
first real corpus is drawn from uses inheritance functionally — the
2026-09-10 measurement over all 747 entity responses of release
2026-08-13 found **157 entities with a direct parent, 44 chains
deeper than one level, and exactly 2 entities with two direct
parents** (`RechtOpUitkeringWazo` and `RechtUitkeringZw`, both with
parents {`RechtOpUitkering`, `RechtUitkeringZwWazo`}, both outside
the WW scope). Relations hang on the supertype *almost always* — the
mandate's own qualification, kept here — and some subtypes carry no
own attributes at all and exist purely as classification.
That measurement is the only fact base this document uses about that
model; nothing about it is invented here.

The IR must therefore carry a supertype relation as a first-class
kernel notion (mandate decision 1: no sugar lowering as the delivered
form). Seven questions have to be settled before the kernel card can
be executed. They are §3 to §9 below, in the card's order, each
ending in a **Decided** paragraph.

## 2. What the repo has today — the measured inventory

Every row was re-measured on this branch for this card.

| Fact | Where |
|---|---|
| `ObjectType` carries no parent: `name`, `animate`, `plural`, `attributes`, `characteristics` | `IR-SPEC.md` ch. 2.2 |
| the verifier's AST record has 13 fields, none of them a parent | `spec/verifier/lib/ast.ml:302–316` |
| the wire node `Objecttype` is **closed** (`additionalProperties: false`) with 14 required keys | `spec/schema/regelspraak-ir-v0.schema.json` |
| `ref/` compares the type name **literally** in at least eleven places, not one: `of_type`, the case loader's instance, role-filler and expectation arms, the navigation endpoint, the channel's declaration lookup and four name guards | `ref/lib/world.ml:52–57`; `ref/lib/case.ml:277–281, 535, 554, 569, 576, 600`; `ref/lib/eval.ml:480, 627, 803, 1108, 1159, 2508` |
| the instance record holds `otype : string` — one name, no chain | `ref/lib/world.ml:15–29` |
| clingo emits `is_a(Label,"Type")` as a **fact** at one site and joins on it everywhere else: 52 lines / 53 occurrences of `is_a(`, of which 2 are comments and 1 is the fact emitter — **49 lines / 50 occurrences** are join sites | `backends/clingo/lib/translate.ml`; emitter at `:6457`, comments at `:1256` and `:3818` |
| Drools generates ONE JavaBean per object type, emitted as `public class <Name> {` — no `extends` | `backends/drools/src/main/java/io/github/yhavinga/regelspraak/schema/FactModelGenerator.java` (`genereer`) |
| the exec-model transpiler matches by Java class identity: `DSL.declarationOf(factClass, "$subject")`, 65 `declarationOf` sites | `backends/drools/src/main/java/io/github/yhavinga/regelspraak/transpiler/model/ExecModelTranspiler.java:348` |
| a case names an instance's type in one wire key: `"objectType": "Jutter"` | `corpus/golden/anchor/jutterswet-aanspraaktelling/case1-gemengd.json` |
| the model for a new acyclicity check already exists: colour DFS with a named cycle path | `spec/verifier/lib/checks.ml:2859–2915` (WF-085, day-kind cycle) |
| a new WF rule costs: one `wfNNN ctx` function, one line in the `all` concat, one negatief directory, one normative sentence | `spec/verifier/lib/checks.ml` (`all`), `spec/verifier/bin/verifier.ml` |
| the highest rule **in the index on this branch's base** is WF-101; WF-102 does NOT appear there — it is claimed by the parallel aggregation scope-path card (branch `card/geneste-aggregatie-bronpad`), which merges today | `IR-SPEC.md` WF index; the parallel card |

Two properties of that inventory shape everything below. First,
**type membership is decided through one concept per executor but not
through one site**: `ref/` compares the type name literally at the
dozen places listed above, ASP has one predicate that 49 join sites
read, and Drools has one class identity that the engine itself
resolves. The number of sites is what separates the three lowering
answers in §5 and §6, so it is measured rather than assumed.
Second, **the wire node is closed**, so the supertype field cannot
arrive silently: it is an additive optional property under the
Method's extension-field discipline (`IR-SPEC.md` § Method: never
`required`, never null, written whole or not at all), and gate 2
fails on anything else.

## 3. Question 1 — the wire form and resolution through the chain

### 3.1 The field

`ObjectType` gains one optional field:

| Field | Type | Meaning |
|---|---|---|
| `supertype` | string? | the single direct parent, by declared name; absent = a root type |

Wire key `supertype` (the word is the same in both languages, so the
English-metamodel/Dutch-wire seam does not bite here). It is an
**extension field** per `IR-SPEC.md` § Method: optional, never
`required`, never null, written whole or not at all; the kernel card
lands it emitter-free with hand-written anchor witnesses, and the
dialect card then makes it emitter-borne — exactly the route `Let`
and `BinderRef` took at the let-lowering card (2026-09-01), which is
the standing precedent for an extension node that acquires a
producer.

**Single parenthood is true by shape, not by rule.** The field is one
string. A second direct parent is therefore not a document the
verifier rejects; it is a document that cannot be written, and gate 2
refuses the array form on the closed node. This is the same move
WF-050 makes for priority — "explicit or explicitly absent, never
implicit" — applied one level down: the parent is single by
construction, so no rule can be violated and no rule is needed.

The cost of that choice is real and is stated rather than hidden:
**there is no kernel negatief for "a second direct parent"**, because
there is no well-formed-but-wrong document to put in one. The
mandate's decision 2 requires the two known multiple-inheritance
cases to be a documented, loud refusal; under this shape that
refusal lives in three places instead of one — the schema (gate 2),
the RegelSpraak dialect frontend and the F-logic frontend, each with
its own fixture (§9). The kernel card as originally written asks for
a `corpus/negatief/` witness with a reserved WF number for this case;
that request cannot be met under the single-string shape, and §11
lists the alternative for the maintainer to overrule if he wants the
refusal inside the kernel.

### 3.2 Resolution through the chain

Define `⊑` as the reflexive-transitive closure of `supertype`. The
**effective member set** of a type T is the union of the attributes
(respectively characteristics) declared on every S with T ⊑ S. Member
resolution walks the chain **upward only**: a name is looked up on
the type, then on its parent, and so on to the root; the first
declaration found answers.

Three consequences, each of which is a restatement of an existing
rule under the new definition of "resolves", not a new rule:

- **WF-034**'s member-existence cross (`IR-SPEC.md` ch. 2.2/4.6 (p))
  now crosses against the chain-closed set. A member declared on a
  strict *sub*type, read at the supertype, still does not resolve —
  resolution never walks downward, because not every instance of the
  supertype has that member. That refusal is the new reachable shape
  and needs a negatief (§7.2).
- **WF-002** (member names unique per kind within one object type)
  keeps its meaning unchanged for a single declaration; §3.3 lifts it
  to the chain.
- **WF-003** (every model-layer name reference resolves) covers the
  new reference itself, but the supertype reference gets its own rule
  because its failure mode is specific and its witness must name it.

### 3.3 The collision decisions

Two different collisions get two different answers, and conflating
them is the error this section exists to prevent.

**(a) Shadowing — a subtype redeclares a name its chain already
declares. REFUSED.** One instance would then see two declarations of
one name. Which one a reader finds decides the datatype, the
dimensions and the timeline facet of every read of that name, and
readers differ in which they find first — exactly the failure class
`IR-SPEC.md` ch. 2.2 already fences for the side-map keys ("an orphan
key is not a missing value but a silent change of meaning"). The
measured consumer consequence is sharper than "it would not compile":
`FactModelGenerator.leden` **deduplicates silently, first-wins** — it
guards every member with `gezien.add(...)` and keeps the first
derivation (attribute before characteristic before reference). A
shadowed name therefore produces a bean that generates perfectly and
is quietly **missing one member**, which is the same silent
change-of-meaning failure one layer down. Refusing is
therefore both the safe and the cheap answer. Note what is *not*
refused: a subtype may freely declare names its chain does not
declare, including names a *sibling* declares.

**(b) Sibling collision — two incomparable subtypes of one supertype
each declare the same member name. ADMITTED, and not a rule at all.**
Under single inheritance no instance is of two incomparable types, so
no read is ambiguous: each resolves on its own chain. This is the
shape the mandate's **§3** names in the real data (the two
termination codes hanging off one right-to-benefit entity, in the
kernel part's own list of things to decide), and refusing it would
reject the data the whole exercise exists to express. The consequence
that must be stated rather than discovered: a read of that name
anchored at the **common supertype** does not resolve — WF-034
refuses it — and the author must either lift the member to the
supertype or read it at each subtype. A golden pins the admission; no
negatief is needed for the collision itself.

The kernel card's acceptance text says "botsend sibling-attribuut"
among the negative witnesses. Read against this section that is
case (a) — the collision one instance can see — and the card is
sharpened accordingly.

### 3.4 Decided (question 1)

**`ObjectType.supertype: string?`, an additive optional extension
field naming one direct parent; single parenthood holds by wire
shape, not by a well-formedness rule. Member resolution walks the
chain upward only, and the effective member set is the chain-closed
union. Shadowing a name declared anywhere on the chain is refused
(WF-105); two incomparable subtypes declaring the same name are
admitted without a rule, and a supertype-anchored read of such a name
is refused by the existing WF-034 cross. Reserved for this question:
WF-103 (the supertype reference resolves to a declared object type),
WF-104 (the supertype graph is acyclic, self-reference included) and
WF-105 (no shadowing along the chain).**

## 4. Question 2 — the semantics of mandate decision 3

Decision 3 is binding: a subtype inherits, per target, the whole
defaults stack of the supertype; subtype-own ordinary rules replace
the inherited base at the bottom of that stack; inherited exceptions
stay above it; there is no second priority axis; clashing exceptions
across the layer boundary form an unordered group unless explicitly
ordered, and a clash is a visible ch. 7.2 conflict fact. This section
works that out against the machinery that exists.

### 4.1 The stack, bottom-up

`RUNTIME-SEMANTICS.md` ch. 7.1/7.3/7.8 already fix the shape of a
target's stack. Bottom-up: the initialisation default (ch. 7.3,
root-only), the virtual input default (ch. 7.8, implicit), the
**base** — defined here as *the lowest ordinary default* — and above
it the exceptions in descending priority, the wire's `defaults` list
being highest-first.

Inheritance resolves **bottom-up along the chain**: the effective
stack of a type for a target is computed from its parent's effective
stack, so a three-level chain composes twice, associatively. Four
rules, covering the shapes decision 3 speaks about:

1. **The parent's stack is an ordered chain and the subtype declares
   its own ordinary rule or defaults group on the target.** The
   subtype's own base replaces the inherited base. The inherited
   exceptions stay above; the initialisation and input defaults stay
   below and are never replaced — they are not the base.
2. **The parent's lowest ordinary default is a group-default**
   (ch. 7.9: `exceptions`, no own decision or consequence). Nothing
   is replaced; the subtype's base is inserted *below* it and becomes
   the new lowest ordinary default. Discarding a group-default would
   discard its nested exception groups, which is exactly the
   structure amendments edit, so the conservative insert is the only
   defensible reading. **This rule is a filling-in, not a mandate
   reading**: decision 3 says "the subtype's rule replaces the
   inherited base" and is silent about a base that has no own
   consequence to replace. It is the recommendation, flagged so the
   maintainer can overrule it without re-opening decision 3.
3. **The parent's stack is a declared-unordered group**
   (`ongeordend`). There is no "lowest", so base override has nothing
   to bite on. The subtype's own rule **joins the group as one more
   unordered member**, and it should itself be declared unordered —
   WF-050's "priority is explicit or explicitly absent, never
   implicit" read across the layer boundary. Differing decisions
   route to ch. 7.2 exactly as today. **An enforcement gap, named
   rather than papered over:** WF-050's multiplicity check keys on
   the declared subject type (`checks.ml:7392–7394`), so it sees
   `Strandrecht.uitkering` and `Wrakrecht.uitkering` as two targets
   and will not demand the marking across the boundary. Making it
   demand it means lifting that key to the chain-closed set, which is
   exactly what §4.2 argues against. Either this obligation gets its
   own rule with a boundary-scoped check, or it stays an unchecked
   SHOULD; this document does not decide it, and the kernel card must.
4. **The parent writes the target with no defaults at all** (a bare
   computing rule under the ch. 7.8 standing norm). Its rule is the
   base; case 1 applies.

This answers the card's edge "what if the supertype carries only
exceptions and no base" directly: in an **ordered** group the lowest
ordinary default *is* the base by definition, so the case does not
arise; the genuine no-base case is the unordered group, and rule 3
governs it.

**What the four rules do not cover.** They speak about the writer
forms decision 3 speaks about — ordinary computing rules and defaults
groups. Three further writer forms can also appear on two levels of
one chain and get no treatment here: an **allocation** (ch. 3.2,
fenced by WF-026), an **object-creation initialisation** (ch. 3.3,
named in WF-050's no-other-writer clause), and a **characteristic
attribution** as the plain non-defaults form. For the first two the
conservative reading is that they are *not* bases and do not
participate in override — a subtype allocation beside an inherited
allocation on one target stays two allocations and stays refused —
but that is a reading, not a decision, and the kernel card should
either carry a witness for it or refuse the combination loudly.

### 4.2 The single-writer fences are read on the effective stack

This is the load-bearing consequence for the verifier, and the risk it
guards against runs the **opposite** way from the obvious one. Getting
that direction right matters, so it is measured rather than argued.

**Today the fences do not fire on a supertype/subtype pair at all.**
All three key on the **declared object-type name**:

- WF-026 (one allocation per target) compares declared type *and*
  attribute pairwise (`checks.ml:1295–1298`);
- WF-079 (one unconditional bare writer) compares `ot1 = ot2 && a1 =
  a2` (`checks.ml:7581`), and its defaults lookup matches
  `norm l.pd_onderwerp = norm otn` (`:7558`);
- WF-050 builds its multiplicity key as `"A:" ^ norm
  l.pd_onderwerp ^ ":" ^ norm tn` (`checks.ml:7392–7394`), and each
  of its three no-other-writer lifts — rule, object creation,
  allocation — compares against `l.pd_onderwerp` likewise
  (`:7423`, `:7442`, `:7469`).

So `Strandrecht.uitkering` and `Wrakrecht.uitkering` are **two
distinct keys today**, and the §7 anchor cannot trip over them: the
fences never look at the pair. The earlier claim that the anchor would
trip its own fences was wrong, and it is withdrawn here.

**The real hazard is the naive repair.** A session implementing
extensional quantification will be tempted to lift the target key to
the chain-closed set, so that a group on `Strandrecht.uitkering` is
seen as also targeting `Wrakrecht.uitkering`. That lift makes the
fences fire on exactly the shape decision 3 legitimises: a supertype
base plus a subtype base on one target would become "two writers" and
be refused, and the whole construct would be unreachable.

**Decided, and the decision is unchanged: the single-writer rules are
evaluated on the per-type effective stack, after inheritance
resolution, never on the raw declaration set and never on a
chain-closed target key.** In practice that means the fences keep
their present per-declared-type keying for the *collision* question,
and any chain-awareness added to them must ask "do these two writers
survive into one instance's effective stack" rather than "do these two
targets close over the same member". This is a **new obligation on
the verifier**, not merely a restatement — it constrains a change that
is otherwise the obvious one — and it is the sentence the kernel card
must land in `IR-SPEC.md` ch. 6.1 and `RUNTIME-SEMANTICS.md` ch. 7.

The ch. 7.8 standing norm follows from the same reading. Where the
supertype carries a bare single-writer rule and the subtype replaces
it with its own bare rule, each type's effective stack is again a
single bare writer, and the standing norm applies per type, unchanged
— a leeg evaluation writes nothing and the input stands. Where the
subtype's base sits under inherited exceptions, the effective stack is
an ordinary defaults group and ch. 7.1 governs, not ch. 7.8. Nothing
about the norm changes; only the object it is read on does.

### 4.3 May a subtype refine or replace an inherited exception?

**No, and deliberately.** Addressing an inherited exception requires
per-default identity: today only the *group* carries an optional
`name` (`IR-SPEC.md` ch. 6.1), a `Default` carries none. Giving
defaults names so that a subtype can say "replace exception 2 of my
parent's group" is the free-superiority road that
`PRIORITY-RELATION.md` closed as decision C2-1: priority that
references other rules by identity. Decision 3 says only the base is
overridden, and this document does not widen it. A subtype therefore
*adds* exceptions; it never edits inherited ones. Where an author
genuinely needs the inherited exception not to apply to the subtype,
the expressible form is a subtype-own exception whose justification
excludes it, under the layer-boundary ordering of §4.4 — visible in
the document, not implied by the type chain.

### 4.4 Inherited and subtype-own exceptions in one stack

Inherited exceptions keep their inherited relative order among
themselves; subtype-own exceptions keep their declared relative order
among themselves. Between the two blocks there is **no implicit
order**. Per decision 3 the composition is an unordered group unless
the document says otherwise, and WF-050's principle forbids leaving
that implicit. One field carries the declaration:

| Field | Type | Meaning |
|---|---|---|
| `inheritedExceptions` | enum | wire `geerfdeUitzonderingen`: `boven` \| `onder` \| `ongeordend` — where the inherited exception block sits relative to this group's own exceptions |

Three values, one field, on the subtype's `PrioritizedDefaults` node.
This is **not** a second priority axis: the result is still exactly
one chain (or one unordered group) per target, and the field declares
a block position, not a relation between two named rules. It is the
direct analogue of `ongeordend`, which declares the absence of order
in the same way.

*This field is a **recommendation**, not a settled decision.* It is
new wire surface with no producer until the dialect card gives it
one, and decision 3's phrase "unordered group unless explicitly
ordered" is what asks for the escape. §11.2 puts the alternative —
drop `boven`/`onder` and make the layer boundary always unordered —
to the maintainer, and the kernel card must not treat the field as
ratified.

- `boven` — the inherited exceptions outrank the subtype's own.
- `onder` — the subtype's own exceptions outrank the inherited ones.
- `ongeordend` — no order; two members that decide differently are
  over-determination, and ch. 7.2 applies verbatim: **no write** (the
  target keeps what the fact base holds) and **one §9.5 conflict
  fact**, keyed rule × subject with the group's target form as the
  rule name. Equal decisions are no conflict — ch. 7.2's idempotence
  deviation carries over untouched.

**The conflict key names the type that declares the ATTRIBUTE**, not
the instance's concrete type and not the type whose defaults group
happened to clash. In §7.2's anchor the target `uitkering` is declared
on `Strandrecht`; the clash in row 6 arises between an exception
inherited from `Strandrecht`'s group and an exception in
`Wrakrecht`'s own group, on a `Wrakrecht` instance — and the fact is
keyed **`Strandrecht.uitkering`**, because `Strandrecht` is where the
attribute is declared. The target is declared once; keying on the
concrete type would make one target report under as many names as it
has subtypes, and the ch. 5.6 channel already keys rule × subject, so
the subject distinguishes the instances.

**Nested exception groups (ch. 7.9) ride along unchanged.** An
inherited default's `exceptions` subtree is part of that default and
is inherited with it; the subtype never reaches inside it, for the
same no-addressing reason as §4.3. A layer-boundary clash is between
two **root-level** members and therefore keys as a root conflict
(`Subject.target`), not with ch. 7.9's path-keyed form
(`Subject.target/<index path>`). That distinction has to be pinned by
a case or a session will guess it.

### 4.5 Decided (question 2)

**Inheritance composes effective defaults stacks bottom-up along the
chain by the four rules of §4.1: base override against an ordered
parent stack, conservative insertion below a group-default, joining as
an unordered member against an unordered parent group, and the bare
rule as its own base (rule 2 is a flagged filling-in, and three other
writer forms are left open — §4.1). The initialisation and virtual
input defaults are never replaced. The single-writer fences (WF-026,
WF-050, WF-079) are read on the per-type effective stack, never on a
chain-closed target key — measured: today they key on the declared
type name and do NOT fire on a supertype/subtype pair, so the hazard
is the naive lift that would make them fire on the legitimate form,
and this is a new obligation on the verifier rather than a
restatement (§4.2). A subtype never edits an inherited exception.
**Recommended**, not ratified (§11.2): the layer boundary carries one
declaration field `inheritedExceptions` ∈ {boven, onder, ongeordend},
required whenever there is an inherited exception block to place, with
WF-106 reserved for it; unordered clashes go to the existing ch. 7.2
channel with no new machinery, keyed on the type that declares the
attribute and at the root.**

## 5. Question 3 — the Drools lowering, decided on the differential

The card demands this be argued from the differential and not from
taste. The measurement, taken on this branch, decides it.

### 5.1 What the backend actually does

The Drools backend does **not** use `declare` blocks, Drools
Prototypes or a generic map-backed fact. It generates **one Java
source file per object type** — a plain bean with one private field
and a getter/setter pair per member — and compiles them all in one
`javac` invocation into a child-first class loader
(`FactModelGenerator.genereer`, `engine/BeanCompiler.java`). Type
identity at runtime **is the Java class**: there is no type-name
field and no discriminator anywhere. Both transpiler branches match
on class identity — the exec-model branch through
`DSL.declarationOf(factClass, "$subject")`
(`ExecModelTranspiler.java:348`, 65 `declarationOf` sites), the
classic DRL-text branch by emitting the class name into the pattern.
Aggregation is an `accumulate` over a pattern on the element class, so
the type filter *is* the pattern.

### 5.2 What each option would cost

**Native `extends`.** The generated subtype bean becomes
`public class Wrakrecht extends Strandrecht`, and `leden(model, ot)`
must emit only the type's **own** members instead of the chain-closed
set. Everything downstream is then already correct, because Drools
resolves a class pattern by `instanceof`:

- a pattern on the supertype matches subtype facts with no transpiler
  change — extensional quantification (§6) arrives for free, which is
  precisely the semantics the other two executors have to be taught;
- `accumulate` over the supertype includes subtype instances for the
  same reason;
- the contract validator's *lookup* already walks the hierarchy — it
  resolves getters through `c.getMethods()`
  (`schema/ContractValidator.java:90–94`), which returns inherited
  public methods — but its **expectations follow `leden`**: it
  iterates `FactModelGenerator.leden(model, ot)`
  (`ContractValidator.java:34`). So the moment `leden` becomes
  own-members-only, the validator checks own members only, and the
  inherited ones are checked when their declaring type is validated.
  That is the correct behaviour, and it arrives as a **consequence**
  of the `leden` change rather than for free: the validator is one of
  the three files that move;
- the typed accessor path is **already subtype-safe, by anticipation**
  — `runtime/Accessors.java` guards every typed getter and setter with
  `owner.isInstance(o)` and falls back to the reflective bridge
  otherwise, with the comment naming the very case: "only possible if
  a future objecttype specialisatie / a relation field broader than
  the navigation target ever feeds a SUBTYPE/sibling here"
  (`Accessors.java:117–120`, and the setter arm at `:144`).

The work is concentrated in three files: the bean generator (the
`extends` clause and own-members-only), the IR loader (carry the
supertype), and the contract validator's expectations. No pattern
site, no aggregation site, no join predicate changes.

**Flat emission with a discriminator.** Every object type stays a
root bean and gains a type-name field. Then *every* pattern anchored
at a type that has subtypes must be rewritten from a class pattern to
a class pattern plus a membership constraint over the transitive
closure computed at transpile time — 65 `declarationOf` sites in the
exec-model branch and the corresponding pattern emissions in the DRL
branch, each needing a closure lookup it does not have today.
Accumulates need the same. The bean's reference fields, which are
typed by the navigation target
(`FactModelGenerator.leden`, the §13.3.9 to-one reference fields),
would have to be widened to a common supertype bean that no longer
exists — so the flat option does not merely cost more sites, it
**loses static typing on exactly the fields the consumer note of
`IR-SPEC.md` ch. 2.6 is already careful about**.

### 5.3 Why this is a differential argument and not a preference

The gate-12 contract is that a played golden must reproduce **exactly
the expectations gates 5 and 6 pin for `ref` and clingo**. The
semantic content that must be reproduced is extensional quantification
(§6): a supertype pattern must see subtype instances. Native `extends`
reproduces it by the engine's own subsumption; the flat option
reproduces it by a closure the transpiler must compute and apply at 65
sites, each of which is an opportunity for a divergence that the gate
would then have to catch case by case. The option with fewer places
where the differential can split is the one the differential selects.
Two further facts make the choice one-sided rather than merely
preferable: the backend already contains the subtype guard
(`Accessors.java`), so the hierarchy path was anticipated by its
author; and the flat option is not even reachable without widening the
typed reference fields, which is a regression in the one dimension
(static bean typing) the backend spent work on.

The honest residue: `extends` makes the two CGM multiple-inheritance
cases unrepresentable in this backend too, since Java has single
class inheritance. That is not a cost here — it is mandate decision 2
holding at a third layer.

### 5.4 Decided (question 3)

**Native class-hierarchy subtyping: the generated fact bean of a
subtype `extends` its supertype's bean, and `leden` emits own members
only. Both transpiler branches, every pattern site and every
accumulate need no change, because Drools resolves class patterns by
`instanceof` and the typed accessor path already carries an
`owner.isInstance` guard written in anticipation of this case; the
work is three files — the bean generator, the IR loader, and the
contract validator, whose expectations follow `leden`
(`ContractValidator.java:34`) and therefore move with it. Flat
emission with a discriminator is rejected: it costs a
transitive-closure rewrite at 65 pattern sites plus every accumulate,
and it forces the typed to-one reference fields to be widened, losing
static typing the backend deliberately has. The gate-12 obligation —
reproduce the ref/clingo expectations exactly — is met by the engine's
own subsumption instead of by transpiler-computed closure at 65
independent sites.**

## 6. Question 4 — extensional quantification and role compatibility

### 6.1 Quantification is extensional, everywhere, uniformly

The extension of a type T is `{ i : typeOf(i) ⊑ T }`. Every position
that ranges over a type ranges over that set, with no exceptions:

- the instance fold's `elementType` (`IR-SPEC.md` ch. 4.3);
- a rule's subject type — so a supertype rule fires for subtype
  instances, which is the card's first proof moment;
- a defaults group's `subject` (ch. 6.1);
- a characteristic check's owner type (ch. 4.5);
- the uniqueness scope (ch. 3.6) and the consistency rule's subject.

**There is no exact-type quantifier in v1.** The mandate records
that subtypes with zero own attributes exist purely as
classification; a non-extensional reading would make those inert, and
the relation-hangs-on-the-supertype pattern would stop working the
moment a subtype appeared. An "only the plain ones" selection is
expressible today without a new quantifier — as a characteristic, or
as a condition — and adding a second selection axis would cut against
decision 3's one-axis stance for the same reason.

The executor consequences are **located but not small**, and the
earlier draft understated them. `World.of_type`
(`ref/lib/world.ml:52–57`) is where quantification is decided, but it
is not the only literal type comparison in `ref/`, and the §7 anchor
does not even **load** until the others move too. Measured:

- `ref/lib/case.ml:277–281` — the instance loader resolves each
  supplied attribute with `find_ci ot.attributen name` against the
  instance's **own** declared type. `case1` supplies `grondslag` on a
  `Wrakrecht`, where it is inherited: without a chain walk the case
  fails at load.
- `ref/lib/case.ml:554, 569, 576, 600` — the expectation loader
  refuses an attribute or characteristic not declared on the
  instance's own type ("expected attribute %S is not declared on %S").
  Every inherited expectation in all four cases hits this.
- `ref/lib/case.ml:535` — the role-filler check is literal type
  equality (`norm target.otype <> norm expect`), so a `Wrakrecht`
  cannot fill a `Strandrecht` role: §6.2's upper-bound rule lives
  here.
- `ref/lib/eval.ml:480` — the navigation endpoint check is literal
  equality against the declared endpoint.
- `ref/lib/eval.ml:2508`, plus the name guards at `:627`, `:803`,
  `:1108`, `:1159` — declaration lookups keyed on the instance's own
  type.

That is at least eleven sites, and they are the honest cost of the
`ref` leg. clingo has a genuine choice — expand the type set in OCaml
before emission, or emit `isa_sub("Sub","Super").` facts plus a
transitive closure rule and route the **49 join lines (50
occurrences)** of `is_a(` through it. *Recommended*: the ASP closure,
because it then lives in the model the solver sees, which keeps the
clingo leg an independent reimplementation rather than a second copy
of the OCaml walk. That is a recommendation on design grounds; the
effort argument runs the other way and the choice is the maintainer's
(§11.3). Drools needs nothing (§5).

### 6.2 Role compatibility is upward only

A role declaring filler type T accepts an instance of any S ⊑ T.
There is exactly one direction: the declared filler type is an upper
bound.

The asymmetry that matters: **a read through a role may resolve
members upward from the role's declared filler type, never downward.**
Reading an attribute declared on a strict subtype through a role
typed at the supertype is unsound — not every filler has it — and is
refused by the chain-aware WF-034 cross (§3.2). This is the one new
reachable refusal in the navigation layer and it needs its own
negatief.

Two existing rules change reachability without changing text:

- **WF-047** ("the bound role's filler type and the opposite role's
  filler type MUST agree with the types the node declares") becomes a
  ⊑-compatibility check rather than an equality check. This is a
  semantic widening of "agree" and must be written down as such; it
  is the rule that lets a fact type declared on the supertype carry
  subtype instances, which is the CGM's dominant shape.
- **WF-048** (bindingless navigation is well-formed only if exactly
  one fact type relates the type pair) is computed over the
  **chain-closed** pair set. Consequence, stated so it is not
  discovered later: *adding a subtype can break a previously
  well-formed bindingless navigation*, because a pair that was
  uniquely connected may become doubly connected once the chain
  closes. The rule is unchanged and the refusal stays loud; the
  practical answer is that the dialect frontend should emit
  `verbindendeRol` / `bronRol` bindings whenever it emits a subtype.
  That belongs in the dialect card.

### 6.3 Instantiation names the concrete type

The case wire already has the discriminator position: each instance
carries `"objectType": "<name>"`
(`corpus/golden/anchor/jutterswet-aanspraaktelling/case1-gemengd.json`).
**Decided: that name is the CONCRETE (most specific) type, there is no
second discriminator field, and the member set the loader accepts is
the chain-closed one.** An instance declared at the supertype is a
plain supertype instance, not an under-specified subtype instance.

Object creation (`IR-SPEC.md` ch. 3.3) creates an instance of exactly
the type it names — **creation is not polymorphic**. This is recorded
as an explicit non-refusal so that nobody adds a rule for it: there is
nothing to refuse, only a semantics to state.

### 6.4 Decided (question 4)

**Quantification is extensional at every type-ranging position, with
no exact-type quantifier in v1. `ref/` changes at eleven measured
sites, not one — `World.of_type` decides quantification, but the case
loader's instance, expectation and role-filler arms, the navigation
endpoint and four name guards all compare the type name literally,
and the anchor does not load until they move. For clingo the
*recommendation* is to carry the closure in ASP (an `isa_sub/2` fact
set plus a transitive rule, with the 49 `is_a(` join lines routed
through it) rather than expanding the type set in OCaml, so the
clingo leg stays an independent implementation; the effort argument
runs the other way and the call is the maintainer's (§11.3). A role's
declared
filler type is an upper bound: S ⊑ T fills a T-role, member resolution
through a role walks upward only, and the downward read is refused by
the chain-aware WF-034. WF-047's "agree" widens to ⊑-compatibility and
WF-048's uniqueness is computed over the chain-closed pair set, which
can turn a previously well-formed bindingless navigation into a loud
refusal — accepted, and an argument for the dialect to emit role
bindings. A case instance names its concrete type in the existing
`objectType` key; object creation is not polymorphic.**

## 7. Question 5 — the minimal Jutterswet use case

### 7.1 The model

Own-authored Jutterswet material, in the corpus's conventions. Two
object types in a chain, one aggregating holder:

Datatype spellings are the corpus's own — `Numeriek (…)`, measured
across every `bron.rgs` (137 occurrences of `Numeriek`, no `Bedrag`
and no `Getal` anywhere) — and the characteristic form is the
declared `is <naam> kenmerk (bijvoeglijk);`.

```
Objecttype het Strandrecht (mv: Strandrechten)
    de grondslag           Numeriek (niet-negatief getal met 2 decimalen);
    de uitkering           Numeriek (niet-negatief getal met 2 decimalen);
    de registratiekosten   Numeriek (niet-negatief getal met 2 decimalen);
    is betwist kenmerk (bijvoeglijk);

Objecttype het Wrakrecht is een Strandrecht (mv: Wrakrechten)
    de bergingsdiepte      Numeriek (niet-negatief geheel getal);

Objecttype de Kustgemeente (mv: Kustgemeenten) (bezield)
    het uitkeringstotaal   Numeriek (niet-negatief getal met 2 decimalen);
    het registertal        Numeriek (niet-negatief geheel getal);

Feittype strandrechtregistratie
    de Kustgemeente registreert meerdere Strandrechten
```

The two Kustgemeente attributes are deliberately **not** named after
the role: an attribute called "aantal strandrechten" beside the role
`strandrechten` would put the use case on top of the ch. 2.6
role/member name-sharing shape, which the Drools bean cannot hold and
which this anchor is not meant to test. `uitkeringstotaal` and
`registertal` keep the two questions apart.

`Wrakrecht` adds one own attribute and inherits everything else —
which is also the shape that proves the classification-only subtype
the mandate records is expressible, since removing `bergingsdiepte`
would leave a legal, empty subtype.

### 7.2 The rules and the six cells

- a bare rule on the supertype: `de registratiekosten van een
  Strandrecht = 5` — no defaults, the ch. 7.8 standing norm;
- a defaults group on `Strandrecht.uitkering`: base `= de grondslag`,
  exception `= 99, indien het Strandrecht betwist is`;
- a defaults group on `Wrakrecht.uitkering`: base `= de grondslag × 2`,
  exception `= 55, indien de bergingsdiepte groter is dan 20`,
  `geerfdeUitzonderingen: ongeordend`;
- on the Kustgemeente: `het uitkeringstotaal = de som van de
  uitkeringen van alle strandrechten van de Kustgemeente` and `het
  registertal = het aantal strandrechten van de Kustgemeente`.

With `grondslag = 10` the six cells separate, which is what makes the
case a proof rather than an illustration. "Inherited" below is said
from the *`Wrakrecht`* side; on a `Strandrecht` instance rows 1 and 2
are simply that type's own base and own exception:

| # | instance | betwist | bergingsdiepte | `uitkering` | what it proves |
|---|---|---|---|---|---|
| 1 | Strandrecht | no | — | **10** | the supertype's own base |
| 2 | Strandrecht | yes | — | **99** | the supertype's own exception |
| 3 | Wrakrecht | no | 5 | **20** | base override: specific beats general |
| 4 | Wrakrecht | yes | 5 | **99** | the inherited exception still outranks the subtype base |
| 5 | Wrakrecht | no | 25 | **55** | the subtype's own exception over its own base |
| 6 | Wrakrecht | yes | 25 | **no write + one §9.5 fact** | the unordered layer boundary |

Rows 3 and 4 together *are* the penguin test in the mandate's own
formulation, and they are the two rows a flat lowering cannot produce:
flattening gives 20 for row 4 (the subtype base wins) or 99 for row 3
(no override at all), never both.

### 7.3 The cases

`corpus/golden/anchor/jutterswet-strandrechtketen/` — hand-written,
absolute expected values, permanently pinned. **Four** case files, so
that every one of the six cells is covered; the earlier three-case
sketch left row 5 — the subtype's own exception over its own base —
with no witness at all:

- `case1-basis.json` (**cells 1 and 3**) — one `Strandrecht`
  (grondslag 10, not betwist) and one `Wrakrecht` (grondslag 10,
  bergingsdiepte 5, not betwist), both registered by one
  `Kustgemeente`. Expected: uitkering **10** and **20**;
  registratiekosten **5** on *both* (the supertype rule firing for a
  subtype instance — proof moment 1); uitkeringstotaal **30** and
  registertal **2** (extensional quantification — proof moment 2; a
  non-extensional reading gives 10 and 1, so the case fails loudly
  against a flat implementation); `consistent`.
- `case2-geerfde-uitzondering.json` (**cells 2 and 4**) — the same
  two, both betwist, bergingsdiepte still 5. Expected: **99** and
  **99**, uitkeringstotaal **198** (proof moment 3, the penguin
  test's second half).
- `case3-eigen-uitzondering.json` (**cell 5**) — one `Wrakrecht`,
  not betwist, bergingsdiepte 25. Expected: uitkering **55**,
  registratiekosten **5**, uitkeringstotaal **55**, registertal
  **1**; `consistent`. This is the cell that separates "the subtype
  has an exception at all" from "the inherited exception wins": with
  only cases 1–2 a lowering that silently dropped the subtype's own
  exception would still pass.
- `case4-laaggrensconflict.json` (**cell 6**) — a `Wrakrecht` that is
  betwist and has bergingsdiepte 25. Expected: `uitkering` **not
  written** and one §9.5 conflict fact keyed `Strandrecht.uitkering`
  (the type that declares the attribute, §4.4) on that subject;
  `inconsistent` (proof moment 4).

Negatiefs, one per reserved rule, in the repo's Dutch slug convention:

- `corpus/negatief/wf103-onbekend-supertype`
- `corpus/negatief/wf104-supertype-cyclus` (and the self-reference as
  a second directory under the same rule, `wf104-supertype-zelfverwijzing`)
- `corpus/negatief/wf105-hershadowd-attribuut`
- `corpus/negatief/wf106-ongeordende-laaggrens`
- `corpus/negatief/wf034-neerwaartse-lezing` — a member declared on
  `Wrakrecht` read through a `Strandrecht`-typed role

Follow-on goldens, named here so the three cards agree: the dialect
card's emitted golden is `corpus/golden/jutterswet-wrakrecht` (carrying
`bron.rgs`, gate 10's byte-identical re-emission), and the F-logic
card's is `corpus/golden/jutterswet-wrakrecht-flogic` (the `-flogic`
suffix follows the existing `-catala` convention). Those two are the
frontend-diff pair.

### 7.4 Decided (question 5)

**One anchor, `corpus/golden/anchor/jutterswet-strandrechtketen`, with
`Strandrecht` → `Wrakrecht` and an aggregating `Kustgemeente`, **four**
case files covering all six cells of the §7.2 table as absolute
expectations. It carries all four proof moments the kernel card
demands; cells 3 and 4 are exactly what a flat lowering cannot
produce simultaneously, and cell 5 is what catches a lowering that
drops the subtype's own exception. Five negatief directories as
listed. The
dialect and F-logic cards re-express the same use case as
`jutterswet-wrakrecht` and `jutterswet-wrakrecht-flogic`, the
frontend-diff pair.**

## 8. Question 6 — F-logic: the JACM line, and Flora-2 measured

Decision 3 makes uchar's inheritance a form of non-monotonic
inheritance, so the comparison with the F-logic tradition is no longer
optional context; it is a check on whether we have quietly
contradicted a well-studied semantics.

### 8.1 The installation attempt, and its result

**Flora-2 is installable and was installed.** Outside the repo tree,
at `/tmp/flora2-proef` (nothing was placed inside the repository).
What was done, in order:

1. XSB Prolog 5.0 source (`XSB-5.0.tar.gz`, from the project's
   SourceForge release area) built from source with the stock
   `./configure && ./makexsb`. One file, the Windows-only
   `build/MSVC.sh`, collided on the case-insensitive macOS filesystem
   during extraction and was skipped; nothing else failed. The result
   reports itself as `XSB Version 5.0.0 (Green Tea) of May 15, 2022
   [aarch64-apple-darwin24.6.0 64 bits; mode: optimal; engine:
   slg-wam; scheduling: local] [Build date: 2026-09-11]`.
2. The Flora-2 2.2 beta distribution ships only a Windows `.exe` and a
   Linux `.run`, with no macOS artifact. The `.run` is a **Makeself
   2.1.5** self-extracting archive, so it extracts on macOS with
   `--noexec --keep --nox11 --target <dir>`; it contains a bundled XSB
   source tree and the Flora-2 sources.
3. The distribution's own installer script handles Darwin explicitly.
   Its documented path — build the bundled XSB, then
   `./floraconfig <xsb> selfextract` — ran to completion and ended
   with `+++++ All is well: run Flora-2 using the script
   ./flora2/runflora`.

The banner: `Flora-2/ErgoLite Reasoner 2.2 beta (Dimocarpus longan) of
2022-08-05 (rev: 1203)`. Total elapsed time for the whole install,
including both XSB builds, was a few minutes. ErgoAI 3.0 is the
successor and ships in the same release area; it was not needed and
was not installed (the F-logic card puts ErgoAI licence work out of
scope).

### 8.2 What the semantics actually is — and it is not the JACM one

An important correction to the framing, taken from the shipped manual
rather than assumed. Flora-2 does **not** implement the JACM 1995
inheritance semantics. The manual states it directly: "Behavioral
inheritance has a different (and better) semantics in Flora-2 compared
to [10]", [10] being the JACM paper; and, on the semantics itself: "A
semantics that favors the second interpretation was proposed in [10].
This approach is based on a fixpoint computation of non-monotonic
behavioral inheritance. However, this semantics is unsatisfactory in
many respects, including because it is procedural in nature. Flora-2
uses a different, more cautious semantics for inheritance". The
implemented semantics is Yang & Kifer (ODBASE 2002; JoDS 2003). Its
three stated principles:

1. "Method definitions in subclasses override the definitions that
   appear in the superclasses."
2. "In case of a multiple inheritance conflict, the result of
   inheritance is false."
3. "Inheritance from the same source through different paths is not
   considered a multiple inheritance conflict."

The syntax note that matters for the F-logic frontend card: in
Flora-2 the *inheritable* (class-level, default) value is written
`c[|attr -> v|]`, not `c[attr -> v]`. The latter is the class object's
own non-inheritable value. The JACM paper's `*->` is Flora-1 syntax
and is rejected by the 2.2 parser.

### 8.3 The measurements

All runs below are transcripts from the installed system, with one
stated edit: on the two **membership** queries Flora-2 also answers
with its three built-in classes `\callable`, `\object` and `\symbol`,
which are noise here, and those three lines are elided — marked at
each occurrence. The solution counts are given unelided so the
elision is visible. No value answer is edited anywhere.

**(a) The penguin test.** Source: `vogel[|vliegt -> ja|].
pinguin::vogel. pinguin[|vliegt -> nee|]. tweety:pinguin. opus:vogel.`

```
?- tweety[vliegt->{?V}]          ?V = nee
?- opus[vliegt->{?V}]            ?V = ja
?- pinguin[|vliegt->{?V}|]       ?V = nee
?- tweety : ?C                   ?C = pinguin      ?C = vogel
                                 (5 solutions; \callable, \object,
                                  \symbol elided)
```

Specific beats general, and membership is transitive. This agrees with
decision 3's base override and with §6.1's extensional quantification.

**(b) uchar's four-cell table, with the supertype exception as a
rule.** Source: `vogel[|hoogte -> 10|]. pinguin::vogel.
pinguin[|hoogte -> 20|]. ?O[hoogte -> 99] :- ?O:vogel, ?O[gewond ->
ja].` over four objects.

```
?- v1[hoogte->{?V}]   ?V = 10      ?- p1[hoogte->{?V}]   ?V = 20
?- v2[hoogte->{?V}]   ?V = 99      ?- p2[hoogte->{?V}]   ?V = 99
```

**Cell for cell identical to §7.2's rows 1–4.** The inherited
exception outranks the subtype base in Flora-2 too — for a different
reason (in F-logic any locally derivable value cancels inheritance for
that attribute), but with the same answer. Decision 3 does not
contradict the tradition on the discriminating case.

**(c) The clashing-exceptions edge.** Adding a subtype-own exception
with a different value — `?O[hoogte -> 55] :- ?O:pinguin, ?O[gewond ->
ja].` — to a penguin that is `gewond`:

```
?- p3[hoogte->{?V}]
?V = 55
?V = 99
2 solution(s)
```

Flora-2 returns **both values, silently**. There is no conflict
signal, and there cannot be one: the manual states "Flora-2 does not
automatically enforce type constraints", which a direct test confirms
— declaring `vogel[|hoogte {0..1} => \integer|]` parses, is inherited
to the instance (`?L = 0 ?H = 1`), and is never checked; the
multi-valued answer stands unchanged. uchar writes nothing and
deposits one §9.5 conflict fact instead. **This is the sharpest
divergence, and it is in uchar's favour for a legal IR.**

**(d) The same exception modelled as a class instead of a rule.**
`gewondeVogel::vogel. gewondeVogel[|hoogte -> 99|]. p4:pinguin.
p4:gewondeVogel.`

```
?- p4[hoogte->{?V}]      (no answer)
?- p4 : ?C               ?C = gewondeVogel   ?C = pinguin   ?C = vogel
                         (6 solutions; \callable, \object,
                          \symbol elided)
```

Membership holds, but the value is **blocked**: `pinguin` and
`gewondeVogel` are incomparable, so principle 2 fires and inheritance
yields false. uchar answers 99 here, because its inherited exception
sits above the subtype base by stack position and not by lattice
specificity. **This is the structural divergence**: F-logic resolves
by specificity on the class lattice — a genuine second selection axis
computed from the type order — while decision 3 keeps one axis and
lets the type chain decide only which members enter the stack and
which is the base.

**(e) Inheritance meeting deduction.** The manual's own case
`b[|m->c|]. a:b. a[m->d] :- a[m->c].`

```
?- a[m->{?X}]     ?X = c  - undefined     ?X = d  - undefined
```

Flora-2 answers in the third truth value of the well-founded
semantics. uchar has no third value at the value layer; its
honest-inexpressibility marker is `Onopgelost` (WF-044), a *document*
marker that faults at evaluation under gate 8 and never becomes a
value. Related stance, different mechanism: uchar refuses at the
document, Flora-2 answers `undefined` at the query.

### 8.4 The comparison, summarised

Agreements: specific-beats-general on the base (b, principle 1);
transitive extensional membership (a); an inherited exception
outranking a subtype base on the discriminating four-cell table (b);
and a shared refusal to arbitrate silently between genuinely
competing sources — Flora-2 blocks, uchar reports, neither picks.

Divergences: **(i)** the origin of priority — lattice specificity
versus stack position, which is exactly mandate decision 3's "no
second priority axis", and which produces different answers on case
(d); **(ii)** clash reporting — Flora-2 accumulates or blocks
silently and has no constraint enforcement at all, uchar deposits a
visible conflict fact and writes nothing; **(iii)** the unit of
override — F-logic cancels inheritance for the whole attribute the
moment anything local is derivable, whereas uchar replaces only the
base and keeps the inherited exceptions, so decision 3 is *finer
grained* than F-logic overriding on this axis; **(iv)** three-valued
`undefined` versus a document-level refusal.

### 8.5 Is Flora-2 usable as an external comparison executor?

**Partly, and the limits are structural rather than practical.**

It can: reproduce the penguin test and the four-cell table exactly, as
§8.3 shows, which is enough to certify that decision 3 does not
silently contradict the JACM line on its own canonical cases.

It cannot: (1) express the §9.5 conflict case at all — there is no
conflict channel and no constraint enforcement, so the comparison is
asymmetric by construction at precisely the cell that distinguishes
uchar; (2) carry units, declared decimal scales, timelines,
allocation or the arithmetic channel, so only the inheritance skeleton
of the use case is comparable, never the values; (3) be a gate — it
requires a from-source XSB build per machine and ships no macOS
artifact, so no CI runner would have it, and the gate set does not
admit conditional legs beyond the one the maintainer already carved
out for catala.

**Decided: admit Flora-2 as a documented external comparison on the
inheritance skeleton only — the penguin test and the four-cell table
— recorded as a note, never a gate and never a fourth leg of the
differential square.** The F-logic card's "externe-executor-notitie"
is the right instrument and should be scoped to exactly that.

### 8.6 A finding the F-logic card must absorb

The mandate's §3 fixes the frontend's minimal slice as `c::d`, `o:c`,
`c[attr=>type]`, `o[attr->v]` and Horn rules. **That slice cannot
express the use case that is the card's own acceptance criterion**,
because inheritable class-level defaults are written with the
instance-level bars — `c[|attr->v|]` and `c[|attr=>type|]` — which
the slice as written does not contain. The finding is a measurement;
the remedy is not this document's to take. **Recommended: widen the
slice by exactly those two forms**, because the widening is
semantically the right one — `c[|attr -> v|]` lowers to a uchar
defaults **base** on the class's target, which is decision 3's shape
exactly — and **make the unbarred class-level form `c[attr->v]` a
refusal**, since uchar has no class-level attribute value for a type
object and lowering it silently to an instance default would change
its meaning. The mandate fixed the slice, so widening it is a mandate
amendment and a maintainer decision (§11.4); the alternative, if he
declines, is that the F-logic card's use case must be something other
than the §7 one.

### 8.7 Decided (question 6)

**Flora-2 2.2 beta is installable and was installed and exercised on
this machine, outside the repo; its inheritance semantics is Yang &
Kifer, not the JACM 1995 fixpoint semantics, which the shipped manual
states explicitly. On the penguin test and on uchar's discriminating
four-cell table it agrees with decision 3 cell for cell. It diverges
on three points that matter: priority by lattice specificity rather
than stack position (which blocks where uchar answers), no conflict
channel and no constraint enforcement at all (it returns both
clashing values silently), and attribute-wide cancellation of
inheritance rather than base-only replacement — so decision 3 is
finer grained than F-logic overriding, not a weaker version of it.
Flora-2 is admitted as a documented external comparison on the
inheritance skeleton, never as a gate and never as a fourth leg of the
differential square. Measured finding, with the remedy left to the
maintainer (§11.4): the F-logic frontend's minimal slice as the
mandate fixes it cannot express its own use case, because inheritable
defaults in Flora-2 carry the bars; the recommendation is to widen it
by `c[|attr->v|]` and `c[|attr=>type|]` and to make the unbarred
`c[attr->v]` a refusal.**

## 9. Question 7 — the three refusal lists

### 9.1 Kernel

1. a **second direct parent** — unrepresentable on the wire; gate 2
   refuses the array form on the closed `Objecttype` node. The two
   known cases are named and tagged here so the refusal is documented
   rather than merely mechanical: `RechtOpUitkeringWazo` and
   `RechtUitkeringZw`, each with parents {`RechtOpUitkering`,
   `RechtUitkeringZwWazo`}, permalink form
   `cgm:entity:<Tag>@<release>` at release 2026-08-13, both outside
   the WW scope (mandate §1, §5). No kernel negatief — see §3.1 and
   §11.
2. an **unknown supertype** — WF-103.
3. a **cycle**, self-reference included — WF-104.
4. a **shadowed member name** along the chain — WF-105.
5. an **unplaced inherited exception block** — WF-106.
6. a **downward member read** (a member declared on a strict subtype,
   read at a supertype subject or through a supertype-typed role) —
   the chain-aware WF-034.
7. a **bindingless navigation whose type pair is no longer uniquely
   connected** once the chain is closed — WF-048, unchanged text, new
   reachability.
8. a **defaults group on a temporal member** — WF-086, unchanged;
   inheritance cannot make a target temporal.

Explicit **non-**refusals, recorded so nobody adds a rule: a sibling
name collision (§3.3b); object creation naming a supertype (§6.3,
creation is not polymorphic); a target carrying rules on two levels —
that is decision 3's whole subject, and it replaces the earlier
blanket ban the mandate itself retired.

### 9.2 RegelSpraak dialect (`is een`)

Head form, fixing the position against the existing annotations. The
production, measured, is
`frontends/regelspraak/grammar/RegelSpraak.g4:311`:

```
objectTypeDefinition
    : OBJECTTYPE bepaaldLidwoord? objectTypeNaam
      ( MV_START meervoudsvorm RPAREN )? BEZIELD?
      objectTypeMember*
```

So the annotation order is **already fixed by the grammar** — the
`mv:` group precedes `(bezield)` and there is no alternative — and the
corpus heads `Objecttype de Jutter (mv: Jutters) (bezield)` are that
order, not a free choice. The extension therefore inserts one optional
element after `objectTypeNaam` and before the `mv:` group:

```
Objecttype het Wrakrecht is een Strandrecht (mv: Wrakrechten)
```

**No new lexer token.** A whole-phrase `IS_EEN : 'is' WSP 'een'` is
*not* safe here, and the `IS_VAN_HET_TYPE` precedent does not carry
over. Lexing is not head-local: under maximal munch such a token would
fire wherever the phrase occurs, and it already occurs in **rule
text** — `corpus/golden/jutterswet-schouwrooster/bron.rgs:15` and
`:22` ("Een dag is een schouwdag"),
`corpus/golden/jutterswet-postwacht/bron.rgs:66`, plus seven `toka/`
sources. That would break gate 10's byte-identical re-emission on
documents that have nothing to do with subtyping. `IS_VAN_HET_TYPE`
(`RegelSpraakLexer.g4:187`) is safe only because its phrase occurs
nowhere else. The solution is at the **parser** level: the existing
`IS` (`RegelSpraakLexer.g4:284`) and `EEN` (`:378`) tokens in sequence
at the head position, where the surrounding context disambiguates.
Registered in `frontends/regelspraak/grammar/DIVERGENCES.md` beside
the prioritized-defaults extension, under the same
inert-on-official-documents argument.

Refusals, each with a fixture:

1. two `is een` clauses on one head, or `is een A en B` — the second
   parent, naming the two CGM cases in the message;
2. self-reference (`X is een X`);
3. an unknown supertype (resolution is document-global and order-free,
   so a forward reference is fine; only an undeclared name is
   refused);
4. `is een` naming something that is not an object type (a domain, a
   parameter);
5. a shadowed member — the frontend refuses before the verifier does,
   since it has the chain at hand.

Explicit non-refusal: a target that carries rules on two levels. The
earlier blanket ban is gone; the kernel's group inheritance governs,
and the kernel witnesses are the semantic referee.

### 9.3 F-logic frontend

1. **multiple parents** — two `::` facts giving incomparable parents
   for one class, naming the two CGM cases;
2. **HiLog** and meta-programming;
3. **defeasible reasoning** — argumentation theories, `\overrides`,
   `\neg`;
4. **negation forms** — `\naf` and `\neg`;
5. **monotonic inheritance mode** —
   `setsemantics{inheritance=monotonic}`; uchar's inheritance is
   non-monotonic by decision 3 and the two modes are not
   interchangeable;
6. **the unbarred class-level value** `c[attr->v]` (§8.6);
7. anything outside the slice as widened by §8.6.

Every refusal cites `OPDRACHT_OBJECTTYPE_HIERARCHIE_20260910.md`, per
the F-logic card.

### 9.4 Decided (question 7)

**The three lists above, with the two CGM multiple-inheritance cases
named and tagged in all three of them; the kernel's second-parent
refusal is carried by the schema and the two frontends rather than by
a WF rule, and the explicit non-refusals are recorded so that no
session adds a rule for them.**

## 10. WF-number reservations

**WF-101 is the highest rule in the WF index on this branch's base.**
WF-102 does **not** appear in that index; it is claimed by the
parallel aggregation scope-path card (branch
`card/geneste-aggregatie-bronpad`), whose merge follows today — so the
number is spoken for by that card, not by the document. Subtyping
therefore reserves **from WF-103**:

| Reserved | Statement | Negatief |
|---|---|---|
| WF-103 | `supertype`, when present, names a declared object type | `wf103-onbekend-supertype` |
| WF-104 | the supertype graph is acyclic; self-reference is the depth-1 cycle | `wf104-supertype-cyclus`, `wf104-supertype-zelfverwijzing` |
| WF-105 | no attribute or characteristic name is declared twice along one chain (ci, the WF-002 discipline lifted) | `wf105-hershadowd-attribuut` |
| WF-106 | a defaults group inheriting a non-empty exception block declares its layer-boundary ordering | `wf106-ongeordende-laaggrens` |

**WF-107 is held in reserve** for whatever the kernel card's own lens
round surfaces — the §4.1 rule-3 marking obligation is the most likely
claimant, since it currently has an obligation and no check. Most of
the rest of this document adjusts existing rules under the chain-aware
definition of resolution (WF-034, WF-047, WF-048) and takes no new
number — those get goldens and, where a new shape becomes reachable, a
negatief under the existing number. The single-writer fences (WF-026,
WF-050, WF-079) are the exception to that framing: §4.2 puts a new
*constraint* on them rather than restating them, and it needs a
witness even though it needs no number.

These numbers are **reserved, not fixed**: the main session assigns
the final numbers serially when the kernel card lands, per the card
protocol in `CLAUDE.md`. WF-104's model implementation is
`spec/verifier/lib/checks.ml:2859–2915` (WF-085's colour DFS with a
named cycle path).

## 11. Open points only the maintainer can settle

1. **The second-parent refusal has no kernel witness.** §3.1 chooses
   `supertype: string?`, which makes single parenthood true by shape
   and leaves the refusal to gate 2 and the two frontends. The
   alternative is `supertypes: [string]` with a WF rule "at most one",
   which buys a real `corpus/negatief/` witness and a place for the
   two CGM cases inside the kernel, at the price of a wire shape that
   can represent what the design forbids. The kernel card as written
   assumes the second; this document recommends the first. **This is
   one of two places where the proposal departs from a card's stated
   acceptance text, and both are flagged rather than quietly
   executed.** The other is the kernel card's "botsend
   sibling-attribuut" negatief: §3.3 reinterprets it as the
   *shadowing* case (a), because the literal sibling case (b) is the
   shape the real data needs and must be admitted, not refused. If the
   maintainer reads the card's phrase the other way, §3.3's admission
   is what has to change, not the negatief.
2. **The layer-boundary ordering field.** `inheritedExceptions` ∈
   {boven, onder, ongeordend} is one field and one enum, but it is
   still new wire surface with no producer until the dialect card
   gives it one. If the maintainer prefers the smaller boundary,
   dropping `boven`/`onder` and making the layer boundary *always*
   unordered is a coherent alternative: it costs the ability to
   express "the subtype's exception outranks the inherited one" and
   makes every such clash a conflict fact. Decision 3's phrase
   "ongeordende groep tenzij expliciet geordend" reads as requiring
   the escape, which is why it is proposed.
3. **Where clingo carries the closure.** §6.1 recommends the ASP-side
   `isa_sub/2` closure over an OCaml-side type-set expansion, on the
   ground that an independent reimplementation is worth more than a
   smaller diff. The opposite choice is defensible on effort grounds
   (52 join sites) and is a maintainer call, not a design one.
4. **Whether the F-logic slice may be widened** by `c[|attr->v|]` and
   `c[|attr=>type|]` (§8.6). The mandate fixed the slice; this
   document's finding is that the slice as fixed cannot express the
   card's own use case. Widening is a mandate amendment, not a card
   decision.
5. **Depth.** Mandate decision 2 says chains are unbounded. Nothing
   here needs a bound, and the measured data has chains deeper than
   one level in 44 cases, so no bound is proposed — recorded only so
   that the absence of a limit is a decision rather than an oversight.
6. **The mandate document cannot be committed as it stands.** It uses
   one of the terms the 2026-08-29 prioritized-defaults rename
   retired, and gate 1's rename tripwire runs `git grep` over tracked
   files, so tracking the mandate turns gate 1 red. This card left the
   copy untracked rather than edit the mandate or the tripwire, both
   of which are outside its may-touch set. Two ways out at landing:
   rephrase the one word in the mandate, or add the mandate to the
   tripwire's reason-carrying allowance list in
   `scripts/check-self-contained.sh`. Both are maintainer moves. The
   same constraint applies to this document, which is why it describes
   the retired term instead of quoting it.

## 12. Summary of positions — proposed, not ratified

Nothing below is ratified. This document is a proposal; the
maintainer's reading round is the gate, and §11 lists the six points
he alone can settle.

1. `ObjectType.supertype: string?`, an additive optional extension
   field; single parenthood by wire shape, not by rule (§3.4).
2. Chain resolution is upward only; the effective member set is the
   chain-closed union; shadowing is refused, sibling collision is
   admitted (§3.4).
3. Inheritance composes effective defaults stacks by the four rules of
   §4.1; the initialisation and virtual input defaults are never
   replaced. Rule 2 is a flagged filling-in, rule 3 carries a named
   enforcement gap, and three other writer forms are left open (§4.5).
4. The single-writer fences are read on the per-type effective stack
   and never on a chain-closed target key. Measured: today they key on
   the declared type name and do not fire on a supertype/subtype pair,
   so the hazard is the naive lift, not the anchor — a new obligation
   on the verifier, not a restatement (§4.2).
5. A subtype never edits an inherited exception; the layer boundary
   carries one declaration field (recommended, §11.2), and unordered
   clashes use the existing ch. 7.2 channel, keyed on the type that
   declares the attribute, at the root (§4.4).
6. Drools lowers to native class-hierarchy subtyping; flat emission
   with a discriminator is rejected on the differential. Three files
   move, the contract validator among them (§5.4).
7. Quantification is extensional everywhere, with no exact-type
   quantifier; roles are upper bounds; creation is not polymorphic.
   `ref/` changes at eleven measured sites; the clingo closure
   placement is a recommendation, not a decision (§6.4, §11.3).
8. The anchor is `jutterswet-strandrechtketen` with the six-cell table
   and **four** cases; five negatiefs; the dialect and F-logic goldens
   are `jutterswet-wrakrecht` and `jutterswet-wrakrecht-flogic`
   (§7.4).
9. Flora-2 is installable, was installed and exercised, agrees with
   decision 3 on the penguin test and the four-cell table, diverges on
   three named points, and is admitted as a documented external
   comparison on the inheritance skeleton only — never a gate (§8.7).
10. The three refusal lists of §9, with the two CGM
    multiple-inheritance cases named in each; the dialect's `is een`
    is a parser-position extension over existing tokens, never a new
    lexer token (§9.2, §9.4).
11. WF-103 to WF-106 reserved, WF-107 held; WF-101 is the highest in
    the index on this base and WF-102 is claimed by a parallel card;
    the main session fixes the numbers at the kernel landing (§10).
