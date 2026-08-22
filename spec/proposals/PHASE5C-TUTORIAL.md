# PHASE5C-TUTORIAL — structs, subscope calls, nested groups, and the road to the tutorial

*Status: draft for maintainer ratification · 2026-08-22 · companion to
`PHASE5-CATALA.md` (adopted) and `PHASE5B-KERNEL.md` (both designs
landed). This document designs the remainder of the route to the formal
5a exit criterion — the Catala tutorial through the plugin, verifier-
clean and executed on ref + clingo. The numbered decisions in §7 go to
the maintainer before any spec work merges.*

*Working prose: may cite the private toolchain by name (WAY-OF-WORKING
§5). Nothing here enters an artifact zone.*

## 1. What the tutorial actually needs — the measured inventory

The tutorial is the catala-book's cumulative income-tax example
(`tutorial_end_2_1..2_4.catala_en`). The measured construct inventory,
against what already lands:

- **Already carried (5a + 5b):** scopes → object types; scalar default
  chains and root-unordered groups → ladders; money/date arithmetic;
  assertions; sum types with total match; lists of scalars/sums with
  `MapList`/`FilterList`/`Fold`/`ListLength`; `IfThenElse` inside
  lambda/function bodies; named functions (`functionDefs`/`Invoke`).
- **2-1 needs:** structs (`Individual`), decimals (`20%` arrives as the
  exact rational `1/5` — Catala desugars percentages before our cut),
  and the inline subscope call (`EScopeCall` is an *expression* at
  scopelang; a declared subscope collapses into the same node).
- **2-2 needs:** the branching exception tree — `article_3` carries
  *two* exceptions, so the tutorial's central ladder branches below the
  root: **nested groups are on the tutorial's critical path**, not an
  optional refinement.
- **2-3 needs:** `list of Individual` (a struct as list element), the
  declared subscope with input definitions, `let … in` (with an
  `if … then … else` inside the let body at rule level), rational
  casts/arithmetic (`decimal of n`, `Div_rat_rat`, …), and
  `Integer.sum` — a *stdlib module* function whose body is unreachable
  at the scopelang cut (module topdef bodies only materialise below
  it), so it must be recognised by qualified name and lowered directly.
- **2-4 adds:** multi-`state` variables and a subscope call *per list
  element* (`map each … to (output of … with {…}).household_tax`) —
  per-element instantiation, machinery nothing in the IR has.
- **The tutorial does *not* use context/reentrant inputs.** Decision
  6's generated top rung is therefore not on the tutorial's critical
  path; the context refusal stays (§5).

One latent defect surfaced by the survey, fixed under this design:
Catala's runtime rounds *money multiplication* to the cent
(`o_mult_mon_rat`), while the plugin maps `Mult_mon_rat`/`Mult_mon_int`
to a bare `Maal` — the `Afgerond {REKENKUNDIG, 2}` wrapper that the
money *divisions* already carry is missing there. The tutorial's own
numbers happen to terminate, which is why no fixture caught it.

## 2. Design 1 — structs as a record datatype (row: the 5c core)

**`StructType`** joins the datatype family as the third composite,
exactly in the `SumType`/`ListType` landing pattern (extension node,
hosted by `domeinen`, repeated whole at use sites):

```json
{ "node": "StructType", "domain": "individu",
  "fields": [ { "name": "inkomen",  "datatype": { "node": "Numeriek", "…": "…" } },
              { "name": "kindertal", "datatype": { "…": "…" } } ] }
```

with two expression nodes — `StructLiteral {type, fields: name→expr,
bron}` and `FieldAccess {type, base, field, bron}` — and a record value
in the value domain. The rules, mirroring the sum-type clauses:

- **Nominal identity** (the WF-061 sibling): two struct operands
  compare only when they name the same declaring domain; equality is
  fieldwise structural; structs join the equality-only set.
- **No holes**: a `StructLiteral` whose field evaluates leeg
  propagates — the whole literal evaluates leeg (the `Construct`/
  `ListLiteral` rule); a record value never carries a leeg field.
  `FieldAccess` on a leeg base propagates.
- **Field discipline** (WF): ci-unique field names; exactly the
  declared fields supplied, each type-matching; `FieldAccess` names a
  declared field; admissible field datatypes = the WF-060 set plus
  `StructType` itself (non-recursive — the acyclicity graph extends
  over struct fields, which is also the term-depth fence).
- **WF-065 widens**: `StructType` joins the admissible `ListType`
  element set — `list of Individual` is the tutorial's input shape and
  the whole point; the fence was written "loudly extensible" for
  exactly this.
- **Case format**: one `{ "struct": { field: value, … } }` variant in
  the value union; `list` already recurses, so list-of-struct parses
  with no further surface.
- **Executors**: ref adds a `Record` value with structural equality;
  clingo encodes a record as a functor term with **fixed
  declaration-order fields** — `st(F1,…,Fn)`, scale 0 on the term,
  each field at its own declared scale — and `FieldAccess` as
  positional projection by unification, the sum-type pattern. Flat
  records are depth 1; the acyclicity fence keeps compositions
  statically bounded under clingo's ~200 term-depth abort.

**Why not lowering onto object types** (the rejected direction, stated
for the record): instances carry identity and labels, structs are
anonymous values — value equality vs identity, ordered lists vs
unordered roles, and set-idempotent creation vs distinct records each
open silent-divergence surfaces; and `list of Individual` would drag
the tutorial's map/fold out of the 5b fold family (which operates on
list *values* only — "the two never merge in v1") into the instance-
aggregation world. Object-type lowering stays where it belongs:
subscopes (§3), which genuinely *are* entities.

## 3. Design 2 — subscope calls as a lowering onto existing kernel

No new kernel node. A subscope call lowers onto three landed pieces —
the PLAN §6 direction ("object type + object creation + navigation"),
executed:

1. the callee scope is (already) an object type; its rules and ladders
   translate as they do for any scope and run in the caller's fixpoint
   on the minted instance;
2. the call becomes one `ResolvedObjectCreatie` of the callee type,
   with the call's `args` bound as member initialisations
   (`Toekenningen`) on the callee's input attributes, and the
   `verbindendeRol` back-link connecting caller and callee instance;
3. reading an output (`EStructAccess` on the subscope instance — the
   field name *is* the callee's output-variable name at scopelang)
   becomes a navigated `AttribuutRef` over that back-link (ch. 4.1a).

The plugin performs the split: one Catala expression becomes a creation
form plus a navigated read, dataflow re-threaded. Distinct call sites
get distinct creation indices (no collision); *the same* declared
subscope is one call per caller subject, which is exactly the
creation-per-subject key. **Fences, all loud and recorded:** a subscope
call per list element (2-4's shape) needs per-element instantiation
that creation-per-subject cannot key — refused until designed; context/
reentrant inputs stay refused (the 5a refusal — decision 6's generated
top rung becomes *realisable* with this machinery but is not needed by
the tutorial; its WF-050 interaction — a caller value feeding a
laddered callee target must enter as a rung, never as a `MemberInit` —
is recorded here as the design constraint for that later card);
subscope outputs consumed as whole structs (rather than per-field
reads) follow design 1 once both land.

## 4. Design 3 — nested ladder groups (the ratified option i, executed)

The additive wire form: an optional field **`excepties` on `Rung`** —
`{ "ongeordend": bool, "leden": [ …sub-ladders… ] }` — making the
ladder a rose tree: a member is itself a rung chain that may nest. Sub-
ladders carry no target of their own (they inherit the root's);
`Rung.required` is untouched, so every existing document validates
unchanged.

Semantics (RUNTIME-SEMANTICS ch. 7, generalised — no new concepts): a
rung carrying `excepties` first reconciles its sub-group exactly as the
root group reconciles today (each member's effective decision;
all-equal → one decision; differing → the sub-group's own §9.5 conflict
fact and no decision); the sub-group's decision, if any, *is* the
rung's decision; an empty sub-group falls through to the rung's own
justification/consequence. The conflict-fact key gains a path suffix so
a nested conflict is distinguishable from a root conflict. The
initialisation rung and the ch. 7.8 input rung stay root-only. WF-050
recurses its shape checks; WF-051 needs zero change once `dep_forms`
folds every descendant read into the root ladder's negative edges (the
recorded "every descendant read is a negative edge toward the root
target").

**The binding acceptance test** is PHASE4-DEFAULTS §7's spurious-
conflict example, pinned absolutely: the tree `b ← x`, `x ← {y, z}`
with only `y` applicable and `x` justified must yield **`y`,
consistent** in both executors — the exact input on which flattening
manufactures a root conflict. The anchor family covers: only-y, only-z,
both-differing (sub-group conflict: fall-through to the parent, one
path-keyed inconsistent fact), both-equal (the equal-decisions rule at
sub-group level), neither (fall-through to `x`), base-only — in both
target kinds. The plugin's two flat parsers become one recursive tree
walk on the `just = true` (node) vs `just = false` (base-group)
discriminator scopelang already provides, and the sub-root refusal
narrows to what genuinely stays out (a shared non-trivial base under
all members — workable later by base-duplication under the
equal-decisions rule).

## 5. Design 4 — the small pieces (decision-free, stated for the record)

- **Decimals.** Catala decimals are exact rationals — precisely our
  exact-rational model. `TLit TRat` → the *unbounded* `Numeriek`
  precision form (`geheel=false, decimalen=0`, WF-007 form (c), exempt
  from the assignment gate); `LRat` literals carry the exact lexeme;
  the resolved rational operators (`Add/Sub/Mult/Div_rat_rat`,
  `Minus_rat`, `ToRat_int`, `ToRat_mon`) map onto the existing numeric
  operators. **`Mult_mon_rat`/`Mult_mon_int` get the
  `Afgerond {REKENKUNDIG, 2}` wrapper** the money divisions already
  carry (Catala rounds money products to the cent — the §1 latent
  defect; existing catala goldens regenerate under gate 9 if their
  bytes move). No `Percentage` node is involved — Catala pre-divides.
- **Stdlib.** The plugin flips `no_stdlib` and admits a named
  **allowlist** — `Integer.sum`, `Money.sum`, `Decimal.sum` —
  recognised by *module-qualified* name (the current base-name lookup
  drops the module path and would collide) and lowered directly to
  `Fold(+, 0/…)` over the argument; module topdef bodies are
  unreachable at the scopelang cut, so this is the only honest route.
  Everything else from modules keeps refusing loudly.
- **Sub-root refusal narrowing, `filled`-analysis recursion, and the
  `EStructAccess`-over-call plumbing** are plugin mechanics under the
  designs above.

## 6. Design 5 — `Let` and the rule-level conditional (the one new discipline)

The tutorial's 2-3 rule is `definition household_tax equals let … in
let … in if deduction > tax then $0 else tax - deduction` — a let
chain *at rule level* whose body is a conditional. Catala lowers
`let x = e1 in e2` to `EApp{f=EAbs…}`; the plugin currently refuses
both the shape and the rule-level `IfThenElse` (the 5b position
fence).

The design (option A of vraag 2): a **strict `Let` node** —
`{binder, value, body}`; the binder joins `BinderRef`; **a leeg value
propagates: the whole `Let` evaluates leeg** — the binder is therefore
never leeg inside the body, which is the load-bearing point. WF-069
widens: `IfThenElse` becomes admissible inside `Let` bodies as well,
under a **condition-operand discipline** that holds everywhere
`IfThenElse` is admissible: its condition may read only binders,
parameters and literals — an attribute read must pass through a strict
`Let` binding first. That discipline is exactly what keeps the clingo
encoding honest: the strict `Let` is the leeg-barrier (an underivable
value atom simply never binds — ref's leeg propagation and clingo's
underivability coincide by construction, the 5b lesson applied
forward), and conditions over bound-only operands keep the
exact-complement, NAF-free branch encoding. In clingo a `Let` is a
bound intermediate (value term + its atoms, body translated with
binder → term); in ref it is `locals`. The alternatives — inlining
(duplicates multi-use bindings like the tutorial's `deduction`/`tax`)
and unrestricted rule-level conditionals (reopens NAF-over-derived-
atoms and the leeg-divergence class) — are rejected in the question.

## 7. The decisions (numbered; to the maintainer before any spec work merges)

**Vraag 1 — de struct-representatie.** A: **eigen record-datatype**
(`StructType`/`StructLiteral`/`FieldAccess` + recordwaarde, §2 — het
SumType/ListType-landingspatroon; no-hole-leegte; nominale identiteit;
WF-065-verruiming met struct-elementen; acycliciteits-/dieptehek). B:
lowering op objecttypen — verworpen in §2 om de identiteit/waarde-,
orde/rol- en fold-familie-mismatches. **Aanbeveling: A.**

**Vraag 2 — `Let` en de rule-level-conditional.** A: **strikte
`Let`-knoop + WF-069-verruiming met de conditie-operand-discipline**
(§6 — binders zijn nooit leeg; het differentiaal blijft per
constructie gesloten). B: pure inlining (geen nieuwe knoop, maar
dupliceert multi-use-bindingen zoals `deduction`/`tax` en vergroot de
IR kwadratisch bij ketens). C: onbeperkte rule-level-conditionals
(heropent de NAF-/leeg-divergentieklasse die 5b juist dichtte).
**Aanbeveling: A.**

**Vraag 3 — het tutorial-exitdoel.** A: **`tutorial_end_2_3` als de
exit-getuige** — het bevat élke constructiefamilie (structs, decimals,
lijsten+folds, subscope-call, let-in, vertakkende exceptiebomen), en
2-4's twee restvormen (multi-`state`-variabelen; de
subscope-call-per-lijstelement) worden luide, geïnventariseerde
flanken met een eigen vervolgkaart. B: volledig 2-4 als doel — dan
komt per-element-instantiëring (echte nieuwe machinerie) nu op het
kritieke pad. **Aanbeveling: A.**

Beslissingsvrij vastgelegd (geen vraag): subscope-calls als lowering
op de bestaande kernel (creatie + init + genavigeerde read — de al in
PLAN §6 en mapping-rij 9 benoemde richting; context-inputs blijven
geweigerd en besluit-6's gegenereerde rung blijft de vastgelegde
vervolgconstructie met zijn WF-050-randvoorwaarde); geneste groepen
als `Rung.excepties` per de geratificeerde optie (i), met het
spurious-conflict-anker als bindende toets; de decimals-mapping op de
onbegrensde Numeriek-vorm; het `Afgerond`-omhulsel op
geld-vermenigvuldiging (het Div-precedent, tegen de shipping runtime
gemeten); de stdlib-allowlist per gekwalificeerde naam.

## 8. The implementation cards

Vier kaarten, serieel (elk: volledig regime, negen gates, herpin,
lensronde, eerlijk Result):

1. `fase5c-structs` — §2 end-to-end (WF-nummers vanaf WF-071), incl.
   de WF-065-verruiming en list-of-struct.
2. `fase5c-nested-groups` — §4 end-to-end, incl. het
   spurious-conflict-ankergezin en de plugin-boomwandeling.
3. `fase5c-let-decimals` — §5 + §6: de `Let`-knoop, de
   WF-069-verruiming, decimals, de geld-vermenigvuldigingsafronding,
   de stdlib-allowlist.
4. `fase5c-subscopes-tutorial` — §3 plus de tutorial-golden:
   `tutorial_end_2_3` door de plugin de corpus in, drievoudig
   geverifieerd — het formele 5a-exitcriterium valt hier, en PLAN §6
   Phase 5 krijgt zijn Met-markering voor dat criterium.
