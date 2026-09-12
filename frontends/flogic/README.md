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

## The slice

The slice is a syntactic **subset of Flora-2 2.2**: every form below parses
in that reasoner. The rule the slice follows:

> where Flora-2 has a spelling, use Flora-2's; where it has none for
> something the wire format carries, use uchar's own metamodel word.

| form | reading |
|---|---|
| `C::D.` | `C`'s single direct supertype is `D` |
| `X : feittype.` | `X` is a fact type, not an object type |
| `C[\|a => numeriek(sign, scale)\|].` | an attribute declaration on `C` |
| `C[\|a => kenmerk(soort)\|].` | a characteristic declaration on `C` |
| `C[\|a => \integer \| \boolean \| \string \| \date\|].` | the four exactly-mappable primitives |
| `F[\|r {lo..hi} => C\|].` | a role of the fact type `F`, filled by `C` |
| `C[\|a -> 5\|].` | the inheritable class-level default: a defaults **base** on `C.a` |
| `@!{'name'} ?X[a -> Expr] :- ?X : C.` | an unconditional definition of `C.a` |
| `@!{'name'} ?X[a -> Expr] :- ?X : C, Cond, ….` | a conditional definition: an exception |

The rule id is optional in Flora-2 and **required** here: a rule reaches
the wire by name, and the names must be distinct. Neither is enforced
downstream — WF-020 filters null names out and never reaches a defaults
group's name — so a nameless or duplicated rule refuses at the frontend
or nowhere.

`sign` is `onbepaald`, `niet_negatief`, `negatief` or `positief`; `scale`
is `geheel` or a decimal count. `soort` is `bijvoeglijk`, `bezittelijk` or
`zelfstandig`. A cardinality whose upper bound is `*`, or greater than 1,
makes the role to-many.

Value expressions: decimal literals, path reads `?X.a`, infix `+ - * /`,
and the two aggregates over one role hop —
`sum{?V | ?V = ?X.role.attribute}` and `count{?V | ?V = ?X.role}`. Rule
bodies carry the subject binding `?X : C`, characteristic checks `?X : k`,
comparisons (`> >= < <= = !=`) and value bindings `?V = Expr`.

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
   that can actually *write* a second parent, which makes this fixture the
   rule's sharp witness. The message names the two known
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
   types (a reference is a role and belongs on a fact type), a ground
   instance fact, two unconditional definitions of one target, a rule with
   no id, and two rules sharing one id.

## Building and testing

The frontend is part of the workspace's dune build:

```
dune build frontends/flogic
dune build --force @frontends/flogic/runtest
```

The suite emits each test program, asserts it is verifier-clean, replays
its cases through the reference runner and the clingo backend, and drives
every refusal fixture. `scripts/check-flogic.sh` runs that suite and holds
every corpus golden carrying a `bron.flogic` to byte-identical
re-emission.
