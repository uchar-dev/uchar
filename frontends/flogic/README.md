# `frontends/flogic` — a minimal F-logic frontend for the uchar IR

A second source language for the same kernel: an F-logic program in, a
`regelspraak-ir-v0` document out. Its point is not coverage but
**independence** — object-type subtyping, characteristics, fact types,
defaults groups and aggregations reach the wire from a language that has
none of RegelSpraak's grammar, and the resulting IR must be the same IR.
The corpus pair that pins this is `corpus/golden/jutterswet-wrakrecht`
(the RegelSpraak dialect) beside `corpus/golden/jutterswet-wrakrecht-flogic`
(this frontend), over one shared set of case expectations.

```
flogic_frontend <source.flogic> <out.json>
```

A refusal writes no document, prints its reason with a line number, and
exits 2.

## Why F-logic, as the third frontend

RegelSpraak is the target language itself. Catala was chosen as the
second language at the start of the project, before any of the
defaults work existed, because it is a complete, mature DSL for legal
rules; that its core construct is default logic with exceptions is
what later made it the natural second source for the
`prioritizedDefaults` node — the guard was a consequence of the
choice, not its motive. The same holds for the engines: Drools and
Clingo were picked at the start (Drools from the existing toolchain
practice, ASP because the corpus mirrors the British Nationality Act
experiment, which is logic-programming literature), not for any one
construct. F-logic is the first frontend chosen the other way around:
picked *for* the construct it had to guard. Class subtyping (`C::D`)
and inheritable class-level values (`@!{...} C[|a -> v|]`) are native
F-logic frames — the language was designed around them (Kifer, Lausen &
Wu, *JACM* 42(4), 1995) — where Catala has no subtyping at all. The
supertype field, chain-closed member resolution and the child-wins
defaults order reaching the wire from this frontend proves the
hierarchy kernel is language-neutral, and the pair of goldens named
above pins it: two sources, one set of case expectations.

The implementation language follows the same early choices. Choosing
Catala meant OCaml was in the project from the plan onward (Catala's
compiler and plugin API are OCaml), the other language in house — Java
— is the production-runtime side that `ref/` and the verifier must stay
structurally separate from (gate 1's no-share tripwire), and OCaml fits
a reference implementation well: typed ASTs with exhaustive matches, so
every kernel extension that widens a node union breaks the build here
instead of failing silently. That is why the verifier, `ref/`, the
clingo backend and this frontend are OCaml.

The **quantifier fold** is the second construct this frontend guards, and
it arrived the same way. `Kwantorvouw` had three independent executors and
no source language: RegelSpraak declines the non-temporal matrix, Catala
does not carry the node, and the corpus witnesses for it were hand-written
IR. Counting is native here — `count{…}` is an F-logic aggregate, and all
five quantifiers are one comparison of a count with a number — so the node
reaches the wire from a language with none of RegelSpraak's grammar, which
is the same argument the subtype kernel got.

Two further reasons. The slice is a syntactic subset of Flora-2 2.2, so
every accepted program also parses in an independently maintained
reasoner — the spelling is borrowed, not invented (with the three forms
listed under "The slice" excepted: argued, not measured). And F-logic can write
what RegelSpraak cannot: a second parent. That makes this frontend the
only place WF-106 (at most one supertype) has a refusing witness — on
the wire the rule is structural (`supertype` is one string), so no
verifier check can test it.

## The slice

The slice is a syntactic **subset of Flora-2 2.2**: every form below parses
in that reasoner. The rule the slice follows:

> where Flora-2 has a spelling, use Flora-2's; where it has none for
> something the wire format carries, use uchar's own metamodel word.

**Three forms are argued rather than measured.** The claim above was
checked in the reasoner itself for every form the slice carried up to
2026-09-12; the installation that check ran on is gone as of 2026-09-19,
so the forms added since have been reasoned against Flora-2's documented
grammar and **not run**. They are, exactly:

1. the **conjunctive comprehension body** `count{?V | ?V = ?X.rol, G, …}` —
   Flora-2's aggregate body is a comma-separated conjunction of literals,
   and the slice only restricts which conjuncts it reads;
2. the **isa head** `?X : k :- Body.` — a rule concluding class membership;
3. `<=` as the non-strict less-than comparison. It was in the slice's
   operator table before the fold arrived, but no program used it until
   `karig` and `zuinig` did; Prolog and XSB spell that comparison `=<`, and
   whether Flora-2 2.2 also accepts `<=` is the one form on this list whose
   answer could be "no". If it is, the fix is mechanical and local: `=<`
   replaces `<=` in the lexer's punctuation table, in the operator line
   below and in the three sources that use it.

That is the measuring list for the next session that has a reasoner. No
synonym has been added on speculation: the slice's rule is to use Flora-2's
spelling, not to accept two.

| form | reading |
|---|---|
| `C::D.` | `C`'s single direct supertype is `D` |
| `X : feittype.` | `X` is a fact type, not an object type |
| `C[\|a => numeriek(sign, scale)\|].` | an attribute declaration on `C` |
| `C[\|a => kenmerk(soort)\|].` | a characteristic declaration on `C` |
| `C[\|a => \integer \| \boolean \| \string \| \date\|].` | the four exactly-mappable primitives |
| `F[\|r {lo..hi} => C\|].` | a role of the fact type `F`, filled by `C` |
| `@!{'name'} C[\|a -> 5\|].` | the inheritable class-level default: a defaults **base** on `C.a` |
| `@!{'name'} ?X[a -> Expr] :- ?X : C.` | an unconditional definition of `C.a` |
| `@!{'name'} ?X[a -> Expr] :- ?X : C, Cond, ….` | a conditional definition: an exception |
| `@!{'name'} ?X : k :- ?X : C, Cond, ….` | a characteristic assignment: `C.k` holds where the body does |

Flora-2's rule id is optional and unavailable outside a rule; here it is
**required on every form that puts a name on the wire, and only on those**
— a Horn rule and a barred class-level *value*, both of which are
definitions. A barred *signature* declares a member and names nothing, so
an id on one refuses, as it does on `::`, on `:` and on the unbarred
frame. Ids must be distinct across both kinds. The same demands hold
downstream since 2026-09-12: WF-107 requires a present, non-blank name on
every named form and WF-020 keeps names unique across rules and defaults
groups in one namespace. The frontend refuses first, with a source line;
the verifier is the second net. Because Flora-2 has no per-item id, a
named barred statement carries exactly its one definition.

`sign` is `onbepaald`, `niet_negatief`, `negatief` or `positief`; `scale`
is `geheel` or a decimal count. `soort` is `bijvoeglijk`, `bezittelijk` or
`zelfstandig`. A cardinality whose upper bound is `*`, or greater than 1,
makes the role to-many.

Value expressions: decimal literals, path reads `?X.a`, infix `+ - * /`,
and the two aggregates over one role hop —
`sum{?V | ?V = ?X.role.attribute}` and `count{?V | ?V = ?X.role}`. Rule
bodies carry the subject binding `?X : C`, characteristic checks `?X : k`,
comparisons (`> >= < <= = !=`) and value bindings `?V = Expr`.

## The quantifier fold: a count compared with a threshold

Flora-2's aggregate body is a comma-separated conjunction of goals. A
`count` comprehension that carries goals beside the hop states a condition
of the members it counts, which is a **quantification** over them — the
`Kwantorvouw` of `IR-SPEC.md` ch. 4.5 (semantics `RUNTIME-SEMANTICS.md`
ch. 3.9a). The slice reads it in a rule body, compared with its threshold:

```
count{?V | ?V = ?X.role, F…, M} <op> <right>
```

The **first** goal is the role hop, the **last** is the matrix, and
everything between narrows the range — the die/dat subselection. That
position rule is the whole distinction, because F-logic has no relative
clause to attach a filter to the range with. Both are read per contributor,
with the member as subject, and the member reaches the wire as `?V`
whatever letter the author chose.

**A swapped filter and matrix are not a refusal but a different sentence**,
and the position rule is the only thing standing between them. Under the
counting quantifiers and GEEN the swap is invisible by arithmetic — those
read `|{m : F(m) ∧ M(m)}|`, which is symmetric in the two — but under ALLE
it is not: `count(F ∧ M) = count(F)` and `count(M ∧ F) = count(M)` are
different claims, and both `ref` and the ASP backend answer the swapped
document differently (measured). Writing the qualifying clause last is
therefore a real error the frontend cannot catch: any heuristic that could
would have to guess from the goals' shapes, and would refuse legitimate
programs to do it.

| form | quantifier |
|---|---|
| `… >= n`, n ≥ 1 | TEN MINSTE n |
| `… <= n`, n ≥ 1 | TEN HOOGSTE n |
| `… = n`, n ≥ 1 | PRECIES n |
| `… = 0` | GEEN |
| `… = count{?V \| ?V = ?X.role, F…}` — the same comprehension WITHOUT its last goal | ALLE |

All five are one comparison of a count with a number, which is why the
slice needs **no negation form** for them and the `\naf` / `\neg` refusals
below did not move when the fold arrived: `GEEN` is `count = 0` and `ALLE`
is `count = |contributors|`, which is what ch. 3.9a states. A comprehension
with no goal beside the hop states no matrix and stays the plain instance
fold; a quantifier over no condition would violate WF-108 (d).

Corpus witness: `corpus/golden/jutterswet-vuurtoren-flogic`, the second
source twin of the hand-written `corpus/golden/jutterswet-vuurtoren`, over
one shared set of case expectations.

## How priority is decided

F-logic's own way to order two definitions of one target is defeasible
reasoning, and that is refused. What orders them here is the **shape of
the rule**:

- a target whose only definition is unconditional is a **bare rule** — the
  standing norm of RUNTIME-SEMANTICS ch. 7.8 — and lowers to a
  `ResolvedRegel`;
- a target that has any conditional definition lowers to a
  `PrioritizedDefaults` group: the conditional definitions first, in source
  order, the unconditional one last as the base;
- the inheritable class-level default `C[|a -> v|]` is a base by the same
  reading, and always a base — an inheritable default *is* a default, so it
  never becomes a standing norm;
- two unconditional definitions of one target refuse: nothing in the slice
  orders them.

A **characteristic** target is outside that device, and stays a standing
norm with its condition as the rule's own `voorwaarde`. Two reasons, and
neither is "a characteristic cannot be over-determined" — on the wire it
can: `PrioritizedDefaults` carries a `kenmerk` target and a `Default`
carries `besluit`, so *attribute* and *do-not-attribute* are two competing
decisions a defaults stack does order (RUNTIME-SEMANTICS ch. 7). The
reasons are narrower:

- **this slice has no do-not-attribute form.** `?X : k :- …` concludes
  membership and nothing concludes its denial — the negation forms are
  refused. Within this language two definitions of one characteristic
  therefore cannot conflict, and a device that orders conflicts has
  nothing to order;
- **it is what the sibling frontend does.** The RegelSpraak frontend lowers
  a conditional characteristic assignment to a `ResolvedRegel` with that
  condition, not to a defaults group. Lowering the same sentence to a
  group here would make two frontends disagree about the shape of one
  meaning.

A second definition of one characteristic target refuses, for the same
reason a second unconditional attribute definition does: nothing in the
slice orders them, and inventing an order would be this frontend's
invention.

Inheritance itself is the kernel's, not the frontend's: the frontend only
writes `supertype`.

## What Flora-2 has no word for, and what this frontend does about it

- **Grammatical number and animacy.** RegelSpraak declares a plural for
  every object type and role and marks animate types; F-logic declares
  neither. The emitted `meervoud` is therefore `null` and `bezield` is
  `false` — not invented. A to-many role is named by the label its author
  wrote, which reads as a plural because a `{0..*}` role denotes a set;
  that label is the one a case document keys on.
- **An empty aggregate.** Flora-2 answers `0` for `sum{…}` and `count{…}`
  over an empty set (measured on 2.2 beta). The SOM's `leegwaarde` says
  exactly that in uchar's vocabulary, written at the summed attribute's
  declared scale; `count` needs no such field.
- **Instances.** uchar separates program from data, so ground facts are
  refused with a pointer to the `uchar-case-v0` case document.

## Refusals

Every refusal names its reason and cites the mandate document that fixed
the slice. One fixture per entry lives in `test/`; the suite checks both
the reason and the citation.

1. **two `::` facts for one class** — WF-106, an object type declares at
   most one direct supertype. On the wire that refusal is structural
   (`supertype` is one string), so no verifier check and no `corpus/negatief`
   entry can carry it: F-logic is the only source form in this repository
   that can actually *write* a second parent, so this fixture is the only
   place the rule can be tested. The message names the two known
   multiple-inheritance cases in the source data.
2. **the unbarred class-level value** `C[a -> v]` — that is the class
   object's own value, and uchar has no class-level attribute value for a
   type object; lowering it to an instance default would change its
   meaning. The inheritable form carries the bars.
3. **HiLog and meta-programming** — a variable in a name position.
4. **defeasible reasoning** — `\overrides`, the argumentation-theory rule
   descriptor `@{…}`.
5. **the negation forms** `\naf` and `\neg`.
6. **`setsemantics{inheritance=monotonic}`** — uchar's inheritance is
   non-monotonic by mandate decision 3, and the two modes are not
   interchangeable.
7. **everything else outside the slice**, each by name where the form has
   one: the Flora-1 arrow `*->` (which Flora-2 2.2 also rejects), `\decimal`
   (it states no decimal scale where uchar's `Numeriek` declares one), a
   predicate `p(…)` (the wire format has no predicate channel), an
   aggregate other than `sum`/`count`, a reference signature between object
   types (a reference is a role and belongs on a fact type), a role filled
   by an undeclared class, one member declared twice on one class, a ground
   instance fact, two unconditional definitions of one target, a definition
   with no id, an id on a form that names nothing, and two definitions
   sharing one id.
8. **the quantifier fold's own edges**, each by name: a strict `>` or `<`
   against a counting comprehension (the `drempel` on the wire is the number
   the source wrote, so the message names the direct spelling), a threshold
   of 0 under `>=` or `<=` (WF-041 pins the bound at 1, and `= 0` is the
   GEEN spelling), the count on the RIGHT of the comparison, a count
   compared with anything but a whole non-negative number or its matrix-free
   twin, an ALLE twin that is a different comprehension, a conditioned
   comprehension in a VALUE position (a quantification with no threshold to
   compare against — the counterpart of the set reading a fold source cannot
   host), a conditioned `sum` (the SOM carries no subselection), a
   conditioned comprehension over an attribute path instead of one role hop,
   a goal inside a comprehension that speaks of the outer subject instead of
   the counted member, a characteristic head naming something the subject
   does not declare, and two definitions of one characteristic target.

## Building and testing

The frontend is part of the workspace's dune build:

```
dune build frontends/flogic
dune build --force @frontends/flogic/runtest
```

The suite emits each test program, asserts it is verifier-clean, replays
its cases through the reference runner and the clingo backend, and drives
every refusal fixture. `scripts/check-flogic.sh` (gate 14, with an
unconditional CI leg) runs that suite and holds every corpus golden
carrying a `bron.flogic` to byte-identical re-emission.
