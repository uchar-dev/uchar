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
  `if … then … else` inside the let body at rule level), and rational
  casts/arithmetic (`decimal of n`, `Div_rat_rat`, …). Its summation
  is the deprecated surface form `sum integer of`, which desugars
  *above* the scopelang cut to the `Reduce(Add)`+`Optional`-match
  shape — **already carried by the ratified 5b lowering**; no stdlib
  machinery is on the critical path (the first draft wrongly routed
  this through `Integer.sum` — the lens round corrected it; the plugin
  must merely tolerate catala's deprecation warning on the fixture).
- **2-4 adds:** multi-`state` variables and a subscope call *per list
  element* (`map each … to (output of … with {…}).household_tax`) —
  per-element instantiation, machinery nothing in the IR has.
- **The tutorial does *not* use context/reentrant inputs.** Decision
  6's generated top rung is therefore not on the tutorial's critical
  path; its implementation is deferred, not refused as policy (§3).
- **The rights fence:** the tutorial text is upstream-licensed, so per
  the ratified decision 2 it never enters `corpus/golden/` — the exit
  witness is a **frontend-side E2E fixture** (gate 9's suite: plugin →
  verifier → ref → clingo); the corpus-facing witnesses remain the
  own-authored Jutterswet-in-Catala articles.

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
- **WF-067/WF-069 widen**: `FieldAccess` (and `StructLiteral`) join
  the closed-body whitelist — the tutorial's map-over-struct-field
  (`λ individual. individual.kindertal`) is otherwise ill-formed
  inside a lambda body (a lens catch: the whitelist is exhaustive and
  the first draft never extended it).
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

**Executed (2026-08-24) — §3 landed, the subscope call triple-green.**
The lowering runs end to end: the plugin splits `EScopeCall` into a
`ResolvedObjectCreatie` of the callee type (args as `Toekenningen` on
the input attributes, the `verbindendeRol` back-link) plus a navigated
`AttribuutRef` per output read, and the callee rules run in the
caller's fixpoint. Witnesses: the corpus subscope golden
`anchor/jutterswet-rekenkamer` and the Catala golden
`jutterswet-rekenkamerheffing-catala` through the full gate set;
`subscope.catala_en` and `tutorial_end_2_3` through gate 9 (plugin →
verifier → ref → clingo). **Two follow-on maintainer decisions, taken
at execution (2026-08-24, numbered questions, both A):** (1) **WF-027
relaxed — the B1-A widening.** A characteristic initialisation in an
`ObjectCreatie` may carry a boolean-*typed* EXPRESSION, not only the
boolean constant, so a caller boolean read (the tutorial's
`overseas_territories`) binds a callee kenmerk over the
`verbindendeRol`; the constant stays the v0 emitter form (additive on
the wire — the expression form is the v1 relaxation the frontend uses),
the tracked delta is `CONSTRUCTS.md` rows 9/21, spec `IR-SPEC.md`
ch. 3.3, semantics `RUNTIME-SEMANTICS.md` ch. 5.5, negatief
`wf027-nonboolean-characteristic-init`. This lifts B1 — the
boolean-subscope-input blockade — on every layer (verifier, ref,
clingo, plugin). (2) **The two clingo flanks land in this card — the
B2/B3-A decision.** A date literal in a term position translates to its
civil day-number (B2), and a `Let`-bound bare value (`ListLength`,
`Fold`, attribute read) read in a term position binds through a
self-contained per-subject helper rule (B3); both were clingo-leg
limits on the tutorial's own exception tree and money product, and both
are now differentially green (ref ≡ clingo over the corpus,
`spec/ASP-TRANSLATION.md` S4/S18). The remaining §3 fences stay loud
and recorded — per-element calls (2-4), multi-`state`, context/
reentrant inputs (decision 6 deferred under the WF-050 constraint),
whole-struct output consumption, and the creation-key collision
backstop. **The frontend-diff exit obligation is not discharged here**
(that is card 5d); §3's landing discharges only the
round-trips-and-executes half of the 5a exit criterion (PLAN §6).

### 3a. The per-element subscope call — the functie-route. Decided (2026-08-24): 1-A, executed.

The 2-4 fence above ("a subscope call per list element … refused until
designed") is now lifted for the pure-function case, and the design
below is ratified and executed.

**The dual lowering, named explicitly.** A subscope call has TWO
lowerings, chosen by how the caller consumes the callee:

- **whole-instance call → object creation** (§3, landed 2026-08-24): a
  *declared* subscope, one call per caller subject, collapses to a
  `ResolvedObjectCreatie` of the callee type plus a `verbindendeRol`
  back-link, its outputs read as navigated `AttribuutRef`s. The callee
  is an *entity*; the per-subject creation key carries it.
- **per-element call → function** (§3a, this decision): an *inline*
  subscope call `(S of {…}).f` appearing INSIDE a map lambda body — the
  2-4 shape, which collapses at scopelang to a `MapList` over the list
  VALUE carrying the inline `EScopeCall` in the lambda body,
  `map (λg → (S of {grondslag=g}).bedrag) grondslagen`. When the callee
  `S` is a **pure function** it lowers to a `FunctionDef` (the row-10
  machinery, DMN's BKM) and the map body to
  `MapList(list, λg. Invoke(S, …))` — for multiple outputs a
  `FieldAccess` on a `StructLiteral` result. **No instance, no creation
  key: the per-element identity IS the list position** (clingo's existing
  `lelem` index reification). The gate is `in_map_body` — the ratified
  form is an inline `EScopeCall` that is DIRECTLY the *map lambda body*.
  It is NOT the broader `in_body` (which also holds inside a fold-reducer,
  function, or match body, and inside arithmetic): only the direct map
  body has a clingo image, because its `Invoke` lowers through
  `body_branches`, whereas a call nested elsewhere in a closed body would
  land as a `closed_term` the backend cannot translate (an `Invoke` has no
  `closed_term` case). So a bare inline call in a rule value (the
  tutorial-2-1 shape), a call inside a fold reducer, and a call nested in
  arithmetic all stay refused — each loudly
  (`refuse_perelement_foldcall`, the lens-round fixture for the
  fold-reducer case).

**Pure function, defined.** A callee `S` is pure iff: every output is a
chain / plain-ladder expressible as a closed `IfThenElse`/`Let`/`Match`
body; it makes **no own object creation** (no subscope of its own); it
performs **no non-input reads** (every read resolves to one of `S`'s
inputs, mapped to the `FunctionDef` parameter binder); and it has a
**total base**. The body walk maps an input read to its parameter
`BinderRef` and refuses any other scope read.

**The weiger-grens (ratified), each a loud refuse-fixture.** Refused per
element: a callee with an **own creation** (`refuse_perelement_creation`),
a **non-input read** (`refuse_perelement_noninput`), an
**unordered/conflict-group output** (`refuse_perelement_conflict`), and —
the deferred flank — **multiple outputs** (`refuse_perelement_multioutput`),
whose `FieldAccess`-on-`StructLiteral` form has no clingo closed-body
image yet (a closed-body field-access-over-`Invoke` is outside clingo's
grammar). **Direction B — real per-element instances (a callee that
itself mints sub-instances) — is formally deferred.**

**Two measured walls, honoured.** (1) A caller output attribute stored as
a **list** fails clingo (`val/3` carries scalars); the covered form is
`map → fold/length` to a **scalar**, and the list-stored form is the
documented flank. (2) **Non-additive arithmetic inside a closed body**
fails clingo: its `closed_term` admits ONLY the additive binary operators
`Plus` and `Min` (unary negation lowers to `0 - a`). Note — measured in
the lens round — that this fence EXCLUDES binary subtraction: `a - b`
lowers to `Binair{Verminderd_met}`, which clingo's `closed_term` rejects
exactly as it rejects multiplication and division (though the SAME
subtraction in a plain, non-closed rule is fine — clingo's general
translator handles `Verminderd_met`; the gap is specific to a closed
callee body). So the fence is `{Plus, Min}`, not a loose "`+/-`": a
per-element callee whose body carries a binary subtraction,
multiplication, or division is REFUSED per element (the `+/- fence` is now
enforced by a body walk, not merely a documented flank —
`refuse_perelement_subtraction`, the lens-round fixture). Fixture bodies
stay **additive**; both walls are why the golden folds an additive `map`
to a scalar.

**Witnesses.** The corpus Catala golden `jutterswet-kamerheffing-catala`
(art. 14, own-authored, rights-free: `Kamerbedrag` a pure `grondslag +
100`, `Kamerheffing` folding the per-element `Invoke` to a scalar)
through the full gate set; `subscope_perelement.catala_en` through gate 9
(plugin → verifier → ref → clingo); the five refuse-fixtures above; and
the honest attempt at the real book page `refuse_tutorial_end_2_4` — its
`shares_of_household_tax` maps to a **whole** callee output (a `list of`
instances, Direction B), refused at the whole-struct read, upstream of
its callee's own creation, non-input read, multiplicative body and
multi-output (all enumerated in that fixture's header).

**Refinement (2026-08-25) — fusion, nested Invoke, multi-output; the
weiger-grens moves, staying inside besluit 1-A.** The `refuse_tutorial_
end_2_4` walls above are now lifted for the pure-function case, and the
book page lands green as `tutorial_end_2_4.catala_en`. Still **no
instances, no new kernel node, list-position identity** — the four
existing FunctionDef/Invoke pieces carry it:

- **FUSION (deforestation).** A whole-struct per-element map
  `map each x among source to (output of Callee with {…})` landing in an
  **INTERNAL** — a `list of Callee` **consumed only per field** (field
  reads/folds on the stored list) — never exists on the wire: at each
  per-field consumption it fuses to
  `MapList(source, λx. Invoke(Callee <field>, args))`. The intermediate
  struct list is inlined away; the internal produces no rule and no
  attribute. A whole-struct map that must exist as a list — declared an
  **OUTPUT**, or read other than per field — stays **loudly refused**
  (Direction B: a caller could read the whole list; no clingo image).
- **NESTED INVOKE (function composition).** A **declared subscope inside
  a pure-function callee whose own callee also passes the purity
  analysis** lowers as a nested `Invoke` — the sub-callee becomes a
  `FunctionDef` too (recursive synthesis under a cycle guard; **WF-067**
  is the acyclic-callgraph gate). A subscope-output read
  (`income_tax_computation.income_tax`) is then simply that Invoke's
  value. This narrows the old "own object creation" wall: **real
  creation** — a sub-callee that needs a *standing instance* (roles,
  ladders on the instance, a §9.5 **conflict channel**), or that fails
  purity another way (a non-input read) — stays **refused, Direction B**.
- **MULTI-OUTPUT.** A callee with more than one output declares **one
  FunctionDef per read output field**, named `<callee> <field>`
  (single-output callees keep the bare `<callee>` name — byte-identity
  for the existing goldens). The old `FieldAccess`-on-`StructLiteral`
  deferral is gone: each field is its own scalar function.
- **FENCE NARROWING.** The frontend closed-body fence narrows to exactly
  what clingo's `closed_term` carries after card `clingo-bodymult`:
  **`Maal`, a constant-divisor `Gedeeld_door`, and the money×rate cent
  product** (`Afgerond` over a `Maal`) now pass; **binary subtraction
  `Verminderd_met`, a runtime divisor, the abs-division, a power and a
  percentage** stay refused per element (no closed-body image). The
  boundary was determined by running clingo, and is documented in the
  refusal text.

**Refined witnesses.** `tutorial_end_2_4.catala_en` through gate 9
(plugin → verifier → ref → clingo), three cases pinned against `catala
interpret` (a two-person household $21,750 / cap $8,250; the cap flank
$11,500 / $8,500; the exempt-rate edge $25,000 / $0); the corpus Catala
golden `jutterswet-kamerhuishouden-catala` (art. 15, own-authored,
rights-free: the fused `aandelen` internal, a two-output `JutterAandeel`
with a nested pure `Vangstheffing`, the `$1.000·(1+n/4)` product and the
deduction cap on the states) through the full gate set;
`subscope_perelement_multi.catala_en` (direct-map-body multi-output)
through gate 9; and the refined refuse-set — `refuse_perelement_creation`
(a nested sub-callee with a conflict channel — échte creatie),
`refuse_perelement_nested_noninput` (a nested sub-callee with a non-input
read), `refuse_perelement_wholestruct_output` (a whole-struct map as an
OUTPUT), plus the retained `refuse_perelement_noninput` /
`_conflict` / `_foldcall` / `_subtraction`.

## 4. Design 3 — nested ladder groups (the ratified option i, executed)

The additive wire form: an optional field **`excepties` on `Rung`** —
`{ "ongeordend": bool, "leden": [ …sub-ladders… ] }` — making the
ladder a rose tree: a member is itself a rung chain that may nest. Sub-
ladders carry no target of their own (they inherit the root's);
`Rung.required` is untouched, so every existing document validates
unchanged.

Semantics (RUNTIME-SEMANTICS ch. 7, generalised): a rung carrying
`excepties` first reconciles its sub-group exactly as the root group
reconciles today (each member's effective decision; all-equal → one
decision); the sub-group's decision, if any, *is* the rung's decision;
an empty sub-group falls through to the rung's own justification/
consequence. **A rung may also be a pure group-rung** — `excepties`
present, no own justification/consequence — which is what carries a
*multi-member base group* (the exit witness's own root has one:
article 2 old/new is a two-member `just = false` base group; the lens
round caught that the first draft had no home for it): the base
position of a chain is then a group-rung whose sub-group holds the
base members, unordered, with the ordinary conflict reading if they
overlap — no disjointness assumption. **What a differing sub-group
does is vraag 5**: propagate to the root (no write, one path-keyed
§9.5 fact — uniform with the root reading and Catala's ⊛-propagation
minus fatality; the recommendation) or decide-nothing-and-fall-through
to the parent rung. Either way the conflict-fact key gains a path
suffix so a nested conflict is distinguishable from a root conflict,
and the choice extends the tracked row-8 deviation record. The
initialisation rung and the ch. 7.8 input rung stay root-only. WF-050
recurses its shape checks (a group-rung carries `excepties` iff it
carries no own consequence/besluit); WF-051 needs zero change once
`dep_forms` folds every descendant read into the root ladder's
negative edges (the recorded "every descendant read is a negative edge
toward the root target"). On the ASP side the nested lowering is real
design work, stated as such: the flat group's effectiveness/conflict
atoms become a per-path predicate family, and a rung whose decision
comes from a sub-group needs value-priority between the sub-group's
outcome and its own consequence — card 2 designs it, the anchor family
proves it.

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

- **Decimals — the ref side is decision-free, the clingo side is
  vraag 4.** Catala decimals are exact rationals — precisely our
  exact-rational model, and `LRat` literals carry the exact lexeme;
  the resolved rational operators (`Add/Sub/Mult/Div_rat_rat`,
  `Minus_rat`, `ToRat_int`, `ToRat_mon`) map onto the existing numeric
  operators; no `Percentage` node is involved — Catala pre-divides
  (`20%` arrives as `1/5`). But the first draft's mapping onto the
  *unbounded* `Numeriek` form was refuted by the lens round: clingo
  scales every value by `decimalen`, so the unbounded form is
  integer-only there — `tax_rate = 0.21` faults, and the money-product
  `Afgerond` would be a silent no-op at scale 1. The workable route
  (vraag 4, option A): decimal-typed declarations carry a **pinned
  finite scale** `decimalen = S` (S chosen at implementation against
  the tutorial, recorded); ref stays exact-rational and both executors
  fault loudly and *consistently* on a value that does not terminate
  at S (ref's bounded-write gate, clingo's existing does-not-fit
  fence) — the differential holds everywhere it runs.
- **Money multiplication** gets the `Afgerond {REKENKUNDIG, 2}`
  wrapper the money divisions already carry (Catala rounds money
  products to the cent — the §1 latent defect). No existing catala
  golden contains a money multiplication (lens-verified), so the fix
  moves no existing bytes; it lands with the first fixture that does.
- **No stdlib machinery.** The first draft's `Integer.sum` allowlist
  was misfounded (the tutorial uses the deprecated `sum <type> of`
  surface form, desugared to the 5b-covered `Reduce`+`Optional` shape
  above the cut — lens-verified with a live `catala scopelang` run);
  `no_stdlib` stays true, modules keep refusing loudly, and the
  base-name collision in the topdef lookup is fixed defensively. The
  plugin tolerates catala's deprecation warning on the fixture.
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

## 7. The decisions (numbered; ratified by the maintainer 2026-08-22 — all five as recommended)

**Vraag 1 — de struct-representatie. Decided (2026-08-22): A — eigen record-datatype**
(`StructType`/`StructLiteral`/`FieldAccess` + recordwaarde, §2 — het
SumType/ListType-landingspatroon; no-hole-leegte; nominale identiteit;
WF-065-verruiming met struct-elementen; acycliciteits-/dieptehek). B:
lowering op objecttypen — verworpen in §2 om de identiteit/waarde-,
orde/rol- en fold-familie-mismatches.

**Vraag 2 — `Let` en de rule-level-conditional. Decided (2026-08-22): A — strikte `Let`-knoop + WF-069-verruiming met de conditie-operand-discipline**
(§6 — binders zijn nooit leeg; het differentiaal blijft per
constructie gesloten). B: pure inlining (geen nieuwe knoop, maar
dupliceert multi-use-bindingen zoals `deduction`/`tax` en vergroot de
IR kwadratisch bij ketens). C: onbeperkte rule-level-conditionals
(heropent de NAF-/leeg-divergentieklasse die 5b juist dichtte).

**Vraag 3 — het tutorial-exitdoel. Decided (2026-08-22): A — `tutorial_end_2_3` als de exit-getuige** (als frontend-side E2E-fixture — het corpus blijft per
het geratificeerde besluit 2 rechtenvrij) — het bevat élke
constructiefamilie (structs, decimals, lijsten+folds, subscope-call,
let-in, vertakkende exceptiebomen), en 2-4's restvormen
(multi-`state`-variabelen; de subscope-call-per-lijstelement; de
directe attribuut-guard) worden luide, geïnventariseerde flanken met
een eigen vervolgkaart. Verworpen: B (volledig 2-4 — per-element-
instantiëring zou nu op het kritieke pad komen).

**Vraag 4 — de clingo-decimaalschaal** (door de lensronde uit de
beslissingsvrije lijst gelicht). **Decided (2026-08-22): A —**
**decimal-declaraties dragen een gepinde eindige schaal
`decimalen = S`** (S bij implementatie tegen de tutorial gekozen en
vastgelegd); ref blijft exact-rationaal; beide executors faulten luid
en consistent op een waarde die niet op S termineert (refs
begrensde-schrijf-poort, clingo's bestaande past-niet-hek) — het
differentiaal geldt overal waar het draait. Verworpen: B (een
rationale-getallen-encodering in clingo — zwaar, raakt elke
rekenroute).

**Vraag 5 — de geneste-conflictsemantiek** (de tweede lensvondst; een
verruiming van het getrackte rij-8-deviatierecord). **Decided
(2026-08-22): A —** **een verschillend-beslissende sub-groep
propageert naar de root** — geen schrijf, één pad-gesleuteld
§9.5-feit; uniform met de root-lezing (conflict = geen schrijf) én
Catala-getrouw (⊛ propageert door de boom) minus de fataliteit die we
al eerder observeerbaar maakten. Verworpen: B (doorvallen naar het
parent-rung — wijkt vérder van het paper af en maakt "conflict" op
twee niveaus twee verschillende dingen).

Beslissingsvrij vastgelegd (geen vraag): subscope-calls als lowering
op de bestaande kernel (creatie + init + genavigeerde read — de al in
PLAN §6 en mapping-rij 9 benoemde richting; besluit-6's gegenereerde
rung blijft geratificeerd en wordt met deze machinerie realiseerbaar —
5c stelt de implementatie uit omdat de tutorial hem niet nodig heeft,
met de WF-050-randvoorwaarde vastgelegd); geneste groepen als
`Rung.excepties` per de geratificeerde optie (i), incl. de
groep-rung-vorm voor meervoudige basisgroepen, met het
spurious-conflict-anker als bindende toets; het `Afgerond`-omhulsel op
geld-vermenigvuldiging (het Div-precedent, tegen de shipping runtime
gemeten; byte-neutraal voor het bestaande corpus); géén
stdlib-machinerie (de som-vormen zijn 5b-gedekt); de
WF-065/067/069-verbredingen met struct-vormen.

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
4. `fase5c-subscopes-tutorial` — §3 plus de tutorial als
   frontend-side E2E-fixture (`tutorial_end_2_3` door de plugin,
   verifier-clean en op ref + clingo — het corpus blijft rechtenvrij,
   besluit 2), naast een eigen-geschreven Jutterswet-subscope-golden
   dóór het corpus met de volle gate-set — het formele
   5a-exitcriterium valt hier, en PLAN §6 Phase 5 krijgt zijn
   Met-markering voor dat criterium.
