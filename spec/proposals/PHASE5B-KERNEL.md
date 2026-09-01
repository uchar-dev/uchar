# PHASE5B-KERNEL — Sum types with payload (row 36) and folds/function values (row 10)

*Status: draft for maintainer ratification · 2026-08-21 · companion to
`PHASE5-CATALA.md` (adopted) and `PHASE4-DEFAULTS.md`. The two kernel
designs Phase 5b exists for, presented together so their shared
machinery is designed once. The numbered decisions in §6 go to the
maintainer before any spec work merges (the WF-059 precedent); the
implementation follows in two cards, sum types first. This revision
incorporates the first lens round (two independent reviews); the
findings and their resolutions are recorded on the card.*

*Working prose: this document may cite the private toolchain by name
(WAY-OF-WORKING §5). Nothing in it enters an artifact zone.*

## 1. Scope, method, and the fences

**What this is.** CONSTRUCTS rows 36 and 10 are the two "kernel (design
at Phase 5)" verdicts: payload-carrying variants with compile-time-total
match, and general folds plus function values (DMN's Business Knowledge
Model + invocation lands under the same design). Both are **core**
nodes, not dialect sugar; both get the full regime — IR-SPEC node forms
with WF rules and negatiefs, RUNTIME-SEMANTICS normative text with
worked examples, both executors, anchors through all nine gates — before
the plugin translates them (PHASE5-CATALA §5.2/5.3).

**The extension discipline decides the wire mechanics; nothing to
ratify there.** Both designs enter exactly as `prioritizedDefaults` and
`TemporalRead` did (IR-SPEC Method): new optional members of the
`datatype`/`expr` `oneOf`s and optional fields, never in `required`,
never emitter-produced, written whole or not at all. The toolchain
emitter has no source construct that could produce any of these forms
(its `Datatype` and `Expr` are sealed sets without payload variants,
match, folds, or arrows — verified read-only), it has no JSON-schema
validator and never reads an `ir.json` back, so the 11 emitter goldens
stay byte-identical and the only toolchain action is the mandatory
`SPEC_REV` re-pin once `spec/schema` moves. Tracked in the
CONSTRUCTS delta column in the row-6/row-8 stamp idiom.

**The fences.**

- **5c stays 5c.** No structs, no subscope calls, no nested defaults
  groups (PHASE4-DEFAULTS §7 option i). Catala constructors carry
  exactly one payload — the surface de-tuplifies n-ary payloads into
  one tuple and gives nullary constructors a unit payload
  (`name_resolution.ml:861`, `from_surface.ml:1228–1244`) — so
  single-payload sum types are Catala-isomorphic without touching
  structs. The tuple payload (a Catala n-ary constructor) is refused
  loudly until structs land. The argmin/argmax family uses tuples
  (`ETuple`, `from_surface.ml:365–445`) and is refused for the same
  reason.
- **The value domain stays first-order.** Function values never become
  runtime values: no arrow datatype in the `datatype` union, no
  closure in the value domain, nothing function-typed is ever storable
  or comparable. Lambdas exist only as operands of the fold-family
  nodes and as named top-level definitions — evaluated at the use
  site, never passed around. This keeps RUNTIME-SEMANTICS ch. 1.1, the
  write boundary, and timeline change-detection untouched by design 2.
- **The frozen empty discipline is not reopened.** Ch. 1.3's whitelist
  is exhaustive and match/fold are not on it: a leeg scrutinee
  propagates (whole match → leeg), a leeg list source propagates, and
  nothing coalesces. §2.3 and §3.2 record the consequences; no new
  three-valued form is introduced.
- **Rule-level defeasibility stays rule-level.** The value-level
  conditional of §3.1 exists only inside lambda and function bodies,
  where no rule structure is available; in rule expressions the IR's
  idiom remains conditions, prioritized defaults and voorwaarden. A value-level
  conditional in an ordinary rule expression stays refused (the 5a
  refusal, unchanged).

## 2. Design 1 — sum types with payload, compile-time-total match (row 36)

### 2.1 Declaration: a `SumType` datatype, hosted by `domeinen`

A new datatype node (English name, per the `TemporalRead`/
`PrioritizedDefaults` extension-node convention):

```json
{ "node": "SumType",
  "domain": "beschikking",
  "constructors": [
    { "name": "Toegekend", "payload": { "node": "Numeriek", "geheel": true,
        "decimalen": 0, "teken": "NIET_NEGATIEF", "eenheid": null } },
    { "name": "Afgewezen", "payload": null },
    { "name": "Aangehouden", "payload": null } ] }
```

- **Declared as a domain.** `domeinen` already maps a name to a
  datatype; adding `SumType` to the `datatype` `oneOf` admits it there
  with zero new top-level surface. The use site (an attribute's or
  parameter's `datatype`) repeats the full node, exactly as
  `Enumeratie` repeats `waarden` — with a WF agreement check between
  use site and declaration.
- **Nominal identity.** Two `SumType`s are compatible only when they
  name the same declaring domain — the WF-008 *principle*, applied to
  the new type with its **own WF number** (WF-008 itself is stated
  over `Enumeration` and its motivated OPEN stays open; the new rule
  is its sum-type sibling, witnessed by a cross-sum-type negatief).
- **Payload arity is 0 or 1.** `payload` is `datatype | null`; null =
  nullary. Admissible payload datatypes in v1: `Numeriek` (including
  unit-bearing — money), the date types, and `SumType` (nested,
  non-recursive). `Tekst`, `Booleaan` and `Enumeratie` payloads are
  refused by WF: the ASP value encoding carries none of the three
  (booleans are characteristics — the standing v0 discipline; text and
  enumeration values have no clingo encoding today and 5b does not add
  one), and no Catala source form produces them — the payloads that
  occur are unit, integer and money (`syntax_en.catala_en`, the
  `Optional` desugarings), and a Catala enum-typed payload arrives as
  `SumType` because *every* Catala enum maps to `SumType` (§2.7).
  Loud, recorded, extensible later.
- **No recursion.** The declaration graph over sum-type payloads must
  be acyclic (WF). Catala has no recursive types either; recursion
  would also reopen clingo's term-depth abort (its README pins the
  ~200-level SIGABRT; `oc_budget` exists for exactly this reason).
  Acyclicity is also what statically bounds constructor-term depth —
  the fact §3.3's accumulator rule leans on.
- **`Enumeratie` is untouched.** The row-27 enumeration stays exactly
  the v0 mirror form (RegelSpraak-side, still without a clingo value
  encoding — status quo, recorded). `SumType` is the Catala-side
  enum family in full, including all-nullary enums; the two forms
  never convert into each other on the wire.

> **Superseded (2026-09-01, keuzetype-familie card):** all-nullary now
> normalises to `Enumeratie` — WF-087; Match widens to enumeration
> scrutinees — WF-063. "Never convert" survives as its payload half
> only. The parenthetical "still without a clingo value encoding" is
> false as well: enumeration values ride the S2 symbolic operand and are
> replayed by gate 6 (`jutterswet-vaarwatertoeslag-catala`). Proposals
> are history: the paragraph above stands as written,
> and the ratified rules are IR-SPEC ch. 2.4 with CONSTRUCTS row 36.

### 2.2 Expressions: `Construct`, `Match`, `BinderRef`

Three new `expr` `oneOf` members (IR-SPEC ch. 4.7, new):

```json
{ "node": "Construct", "type": { "node": "SumType", "…": "…" },
  "constructor": "Toegekend", "argument": { "node": "Literal", "…": "…" },
  "bron": "…" }

{ "node": "Match", "type": "…result datatype…",
  "scrutinee": { "node": "AttribuutRef", "…": "…" },
  "arms": [
    { "constructor": "Toegekend", "binder": "bedrag",
      "body": { "node": "Binair", "…": "reads the binder…" } },
    { "constructor": "Afgewezen", "binder": null, "body": "…" },
    { "constructor": "Aangehouden", "binder": null, "body": "…" } ],
  "bron": "…" }

{ "node": "BinderRef", "name": "bedrag", "type": "…", "bron": "…" }
```

- `Construct` builds a sum value: `argument` is present iff the
  constructor is unary, and its static type must match the declared
  payload datatype (WF).
- `Match` deconstructs: the arms must cover **exactly** the declared
  constructors, each exactly once, no extras (WF — the compile-time
  totality of row 36; a verifier check in the WF-040 "pin every
  declared dimension exactly once" mould). A unary arm carries a
  `binder` name; a nullary arm carries `binder: null` (WF). All arm
  bodies must agree on one result datatype, the `Match`'s own type
  (WF).
- **`BinderRef` is a new node, deliberately distinct from the v0
  `VariabeleRef`.** It resolves lexically to the nearest enclosing
  binder of its name — a match arm's `binder`, and in design 2 a
  lambda or function parameter; an unbound `BinderRef` is ill-formed
  (WF). `VariabeleRef` (row 24, the rule-local let-binding whose
  definitions v0 does not serialize — the WF-034 caveat) is left
  entirely untouched: still accepted by the verifier as the motivated
  OPEN, still a runtime fault in ref. The first draft overloaded
  `VariabeleRef` for both roles; both lenses rejected that as
  non-additive (a resolution WF would newly reject legitimate v0
  let-refs, and the two roles would be indistinguishable on the wire
  forever). A distinct node is the strictly mirror-safe option, and
  narrowing to it is decision-free.
- Wildcards do not exist in the IR. Catala eliminates them before
  scopelang (the desugarer fans a wildcard out over the missing
  constructors), and RegelSpraak has no match at all; an IR wildcard
  would make totality a runtime property again.

### 2.3 Semantics (RUNTIME-SEMANTICS ch. 9, new)

- **Value domain.** Ch. 1.1's enumeration of the value domain gains
  "a constructor value: a declared constructor tag with, for a unary
  constructor, one payload value". A constructor value is *gevuld* by
  construction, and **never carries a leeg payload**: a `Construct`
  whose argument evaluates leeg propagates — the whole `Construct` is
  leeg (the ch. 1.3 strict default). This is what keeps payload
  equality out of the empty-asymmetry corners.
- **Match evaluation.** Evaluate the scrutinee; if it is a constructor
  value, evaluate the unique arm for its tag with the binder bound to
  the payload; the match's value is that body's value. **A leeg
  scrutinee propagates: the match evaluates leeg.** No implicit
  leeg-arm, no coalesce — the ch. 1.3 default, stated and
  anchor-witnessed. (An explicit leeg-arm would be a new three-valued
  form; no statute case wants it; recorded as a non-goal.)
- **Equality is structural and nominal.** Two sum values of the same
  declared domain are equal iff tags are equal and (when unary)
  payloads are equal under the existing value equality; equality
  across different sum types is statically ill-formed (the §2.1
  nominal rule). Non-numeric discipline applies: any leeg operand →
  false **in both directions** (Tabel 38 mirror — `Gelijk` and
  `Ongelijk` are deliberately not complements over leeg), no ordering
  (`<`/`>` faults), no coalescing. Structural equality feeds the three
  places that need it: idempotent writes, timeline segment
  change-detection ("numerically equal or both-leeg" generalises to
  "equal under ch. 9 equality"), and the defaults group's
  equal-decisions-are-no-conflict rule.
- **Prioritized defaults carry sum-typed attributes with zero new
  defaults semantics.** The decide-test is gevuld/leeg, tag-agnostic; a
  default wanting "decide only for constructor X" puts a match in its
  justification. The ch. 7.8 standing norm carries over unchanged.
- **Timelines.** A sum-typed temporal attribute supports the
  non-arithmetic operations only (read, `TemporalRead`, whole-period
  qualifier) — like text/enum timelines today. Timeline arithmetic and
  temporal totals stay numeric; nothing new to refuse, the existing
  refusals already key on numeric operands.

### 2.4 WF rules and negatiefs (numbers from WF-060; final assignment at implementation)

| Rule | Statement (sketch) | Negatief |
|---|---|---|
| WF-060 | SumType declaration: ≥1 constructor, unique names, payload datatypes admissible (§2.1) and resolvable, declaration graph acyclic | duplicate constructor; text payload; recursive sum |
| WF-061 | SumType use site: `domain` resolves to a declared domain whose datatype agrees with the use site; two sum-typed operands in one expression name the same domain (the WF-008 sibling) | undeclared domain; drifted constructor list; cross-sum-type equality |
| WF-062 | Construct: constructor declared; argument present iff unary; argument type matches the declared payload | nullary with argument; payload type mismatch |
| WF-063 | Match totality: scrutinee sum-typed; arms cover exactly the declared constructors, each once; binder present iff unary | missing arm; duplicate arm; alien constructor; binder on nullary |
| WF-064 | Binder discipline: every `BinderRef` resolves to an enclosing binder (match arm, lambda or function parameter); arm bodies agree on one result datatype | unbound BinderRef; mixed arm types |

Each with a verifier check (`checks.ml` `wf0NN` + the `all` list), at
least one passing golden, and its negatief per the
`corpus/negatief/wf###-slug` regime. `VariabeleRef` stays outside all
of these (§2.2); the row-24 OPEN is narrowed by nothing and closed by
nothing here.

### 2.5 Case format

One edit fans out everywhere: `$defs/value` in `uchar-case-v0` gains a
constructor variant — `{ "constructor": "Toegekend", "payload":
{ "number": "1250" } }`, payload absent for nullary — and because the
union is reused at all five sites, sum values work in input attributes,
parameters, timeline segments, `attributeValue` expectations and
timeline expectations with no further schema surface. The loaders
(ref's `case.ml`, clingo's case machinery) add the fail-fast
cross-checks JSON Schema cannot express: declared constructor, payload
against the declared payload datatype (recursing through the existing
`value_of_json`), the WF-059/WF-018 backstop widened to the new
datatype. The existing `enumeration` value form is untouched.

### 2.6 Executors

**ref** (touch list per the verkenning): `Value.t` gains
`Ctor of string * t option`; `eq_non_numeric`/`equal` gain structural
arms; `scalar_cmp` keeps faulting; `static_numeric` classifies
`SumType` non-numeric; `eval_expr` gains `Construct`/`Match`/
`BinderRef` arms (+ `expr_typ`, + the `vintage_read_params` walker
recursing into scrutinee and arms); `env` gains `locals` for binder
values; `case.ml` per §2.5. Worked examples land as `WV-9.*` blocks in
`ref/test/test_ref.ml` *and* as corpus anchors — the inline suite is
not gate-run, so gate coverage comes from the anchors (and the
`@ref/runtest` CI leg).

**clingo** — a genuinely new value capability, not free; the honest
work list:

- A sum value is a ground term — bare functor for nullary
  (`afgewezen`), unary as `toegekend(V)` with the payload scaled **by
  the payload's own declared datatype** (`scaled_of_value`/
  `decimals_of_datatype` gain a constructor arm; a money payload's
  cents come from the payload declaration, never from the target
  attribute).
- The match lowers to one rule per arm, unifying on the functor in
  the scrutinee's `val/3` atom — mutually exclusive by the functor
  itself, **no NAF** because totality is static, so the stratification
  posture is untouched and the one-stable-model invariant holds. A
  leeg scrutinee needs no leeg-arm encoding: no `val` atom binds, no
  arm rule fires, the target stays underived — propagation for free
  (the S5 point-1 pattern).
- Equality in condition position: `Gelijk` is positive ground-term
  unification (both operands bind and the terms are equal).
  **`Ongelijk` must not be encoded as the NAF complement** — over a
  leeg operand that diverges from Tabel 38 (ref: false in both
  directions). It is encoded positively as both-bound-and-different
  (`…, T1 != T2` over two bound value atoms), which is leeg-correct by
  the same underivability argument. This is the S5 non-complement
  warning, honoured in the encoding rather than merely cited.
- `translate_case` emits constructor terms for sum-typed input.
  `model.ml` needs real work, stated as such: `A_term` today holds the
  **flat unparsed lexeme** — a recursive term parser is new; and the
  expectation path routes around `scaled_expect` (which faults on any
  non-numeric target datatype) into a new constructor-expectation arm
  that scales payloads by their declared payload datatypes and
  compares structurally.
- New ASP-TRANSLATION section per the S-numbering rule in §5.

### 2.7 Plugin mapping

> **Superseded (2026-09-01, keuzetype-familie card):** all-nullary now
> normalises to `Enumeratie` — WF-087; Match widens to enumeration
> scrutinees — WF-063, so a two-formed scrutinee is no longer an
> objection. **Both** premises of the paragraph below are now false, not
> just the second: `Enumeratie` values *do* have a clingo encoding (the S2
> symbolic operand — the normalised quoted string), and the golden
> `jutterswet-vaarwatertoeslag-catala` plays the whole enum path through
> gate 6. Proposals are history: the paragraph below stands as
> written; the plugin's ratified mapping is `keuzetype_of` in
> `frontends/catala/lib/translate.ml`.

`TEnum` → `SumType`, built from `ctx_enums` — **uniformly, including
all-nullary enums** (the first draft mapped all-nullary onto
`Enumeratie`; that path could never reach the differential because
`Enumeratie` values have no clingo encoding, and it would have made
`Match` scrutinees two-formed. One family, one form). `EInj` →
`Construct` (unit payload → nullary; tuple payload → loud refusal,
structs-fence); `EMatch` → `Match`, with the plugin first replaying
the `from_scopelang.ml:223–249` exhaustiveness fold against
`ctx_enums` — **scopelang does not guarantee totality** (the check
lives one pass later), so a partial map is refused loudly with the
missing constructors named, and the IR-side WF stays the independent
gate. Case bodies arrive as one-argument `EAbs` over the payload (the
desugarer wraps them); the plugin unwraps the binder into the arm's
`binder` + `BinderRef` bodies. `domeinen` stops being hard-wired empty
in `translate.ml`/`emit.ml`.

### 2.8 Corpus anchors (sketch)

A Jutterswet article (own-authored, wet-v2.md extension if needed): a
`beschikking` sum type as above; a rule matching it into an uitkering
amount; cases per constructor pinned absolutely; negatiefs per §2.4; a
rood case only if a genuine runtime fault mode exists (none is
expected: totality is static, so the runtime residue is the ordinary
leeg/§9.5 machinery). Gate 7 honestly: the deciding cases fail
`--noop` as required; **the leeg-scrutinee corner is a no-op
survivor** (target absent, expectation empty — satisfied by a
do-nothing executor) and joins the reason-carrying `SURVIVORS` list in
`scripts/check-noop.sh`, exactly like the ch. 7.8 standing cases.

## 3. Design 2 — folds and function values (row 10)

### 3.1 The kernel, precisely

Seven additions, all core:

1. **`ListType`** — new datatype: `{ "node": "ListType", "element":
   datatype }`. Element datatypes: `Numeriek`, the date types,
   `SumType` (first-order, non-list; no lists-of-lists, no functions
   in v1 — loud WF, recorded flank). List-typed attributes and
   parameters are admissible (that is where corpus cases get their
   lists); list-typed *temporal* attributes are refused in v1.
2. **`ListLiteral`** — new expr: ordered element expressions, one
   shared element datatype (WF).
3. **`FunctionDef`** — new optional top-level list `functionDefs`
   (the `prioritizedDefaults` pattern): `{name, parameters: [{name,
   datatype}], result: datatype, body: expr}`. **Bodies are closed**:
   a body may reference its own parameters (`BinderRef`), document
   parameters (`ParameterRef`), literals, operators, and other
   functions via `Invoke`; free `AttribuutRef`/`Kenmerk` reads are
   ill-formed in v1 (WF). The call graph must be acyclic (WF) — no
   recursion, matching Catala and keeping termination and
   stratification trivial (closed bodies contribute no dependency
   edges, so WF-045/046 and ref's strata are untouched).
   This *is* DMN's BKM: named reusable logic, invoked by name.
4. **`Invoke`** — new expr: `{function, arguments}` with arity and
   types against the definition (WF).
5. **`Lambda`** — new expr form admissible **only** as the operator
   operand of the fold-family nodes (WF: a `Lambda` anywhere else is
   ill-formed): `{parameters: [{name, datatype}], body}` with the same
   closed-body rule. Never a value, never storable (§1 fence).
6. **`IfThenElse`** — new expr form admissible **only inside lambda
   and function bodies** (WF; §1's fence — rule expressions keep the
   defaults/voorwaarde idiom): `{condition: conditie, then: expr,
   else: expr}`, reusing the existing condition union in guard
   position, both branches one datatype (WF). This node is what makes
   Catala's reducer bodies expressible — `maximum`/`minimum` desugar
   to a `Reduce` whose lambda body is a conditional
   (`from_surface.ml:208–232`); without it the fold kernel would
   refuse the most common list reductions (a first-draft gap both
   lenses found). Inside closed bodies its conditions can only read
   binders, parameters and literals — comparisons over bound values,
   no characteristic reads — which is exactly what keeps its ASP
   encoding builtin-only (§3.3).
7. **`Fold` / `MapList` / `FilterList` / `ListLength`** — new expr
   nodes over list operands: `Fold {list, function: Lambda|name,
   init, …}` (two-parameter function `(acc, element)`; **the
   accumulator datatype is first-order and non-list** — scalar or
   `SumType`; sum-typed accumulators have statically bounded term
   depth because sum types are non-recursive (§2.1), and they are what
   the `Reduce` lowering needs (§3.5); a list-typed accumulator is
   refused in v1, the ASP term-depth fence); `MapList`/`FilterList`
   (one-parameter function; filter body boolean-valued — the one place
   a boolean-typed expression is admissible, mirrored on the
   aggregation `filter` precedent); `ListLength` (count). `Concat`
   joins if an anchor wants it; `Sort`/`Find`/`Map2`/argmin-argmax
   stay refused loudly in the plugin (recorded).

The existing closed `Functie` aggregation catalogue is **not**
reopened: `Som`/`Totaal`/etc. keep their instance-set semantics and
their empty-identity table; the fold family works over list *values*.
The two coexist — one is the RegelSpraak instance world, the other the
Catala list world; a later unification is a recorded non-goal for 5b.

### 3.2 Semantics (RUNTIME-SEMANTICS ch. 10, new)

- **List values** join ch. 1.1: an ordered, possibly empty sequence of
  element values. A list value is gevuld (also when empty — `[]` is a
  value, leeg is the absence of one; the distinction the case format
  and the fold identity both need). Element positions hold values,
  never leeg (WF on `ListLiteral`; the loader rejects leeg segments in
  list case input) — "a list with holes" is not a v1 notion. This is
  also why binders never see leeg inside fold bodies.
- **Fold**: `fold(f, init, [x1..xn]) = f(...f(f(init, x1), x2)..., xn)`,
  left-to-right, order normative. `fold(f, init, []) = init`.
  A leeg list source propagates leeg (ch. 1.3 default); so do
  `MapList`/`FilterList`/`ListLength`. Inside the body, the ordinary
  ch. 1.2 operator discipline governs — the fold node itself adds no
  empty policy (compositionality; the aggregation table stays the
  instance-world's own).
- **IfThenElse**: evaluate the condition under the ordinary ch. 1.2
  comparison discipline, then evaluate exactly the chosen branch —
  strict, no coalesce, the default-justification mirror.
- **Invoke**: evaluate arguments strictly (a leeg argument propagates
  leeg without entering the body — the strict-operator default),
  bind, evaluate the closed body. No recursion exists (WF), so no
  budget machinery is needed; a depth guard in ref is defensive only.
- **Worked examples**: sum-of-list via fold (the `combine all x among
  l in total initially 0 with total + x` shape), map-then-fold,
  filter-on-payload (composing design 1: a list of sum values matched
  per element), the max-via-`Reduce` lowering of §3.5, and the
  empty-list and leeg-list corners.

### 3.3 Both executors — the full IR kernel in the differential

**ref**: `Value.t` gains `Lijst of t list`; `locals` (shared with
design 1) binds lambda/function parameters via `BinderRef`; new eval
arms for the §3.1 nodes; `case.ml` gains the `{ "list": [ … ] }` value
variant against `ListType`; structural equality over lists for
idempotent writes and expectations (and `model.ml`'s term comparison
extends to list reifications on the clingo side).

**clingo**: list values are **ordered by construction**, which removes
the obstacle that makes general folds over instance sets hard (rol/3
joins are unordered). Encoding: reify a list value as indexed facts
`elem(ListId, I, V)` from `translate_case`/`ListLiteral`, then
index-recursion for fold —
`acc(L, I+1, F) :- acc(L, I, A), elem(L, I+1, V), F = ⟨f(A,V)⟩` —
with a first-order non-list accumulator the terms stay flat or
statically bounded (the §3.1 accumulator rule). **Every arithmetic
step in the recurrence carries the `viol/2` range guard of the S6/S8
fence discipline** — the running accumulator is grounding-time
arithmetic and would otherwise wrap silently past 2³¹ where ref's
exact rationals do not (a first-draft omission the lens round caught);
`MapList` output terms carry the same guard.
`MapList`/`FilterList` encode element-wise and flat
(`elem(out, I, f(V)) :- elem(in, I, V)`); `ListLength` is `#count`.
`IfThenElse` inside closed bodies encodes as one rule per branch with
**builtin comparison complements** (`A > V` / `A <= V`) — closed
bodies guarantee the condition reads only bound terms, so no NAF over
derived atoms enters and the stratification posture stays untouched.
`Invoke` of a `FunctionDef` inlines (acyclic, so inlining terminates)
or emits a parameterised helper predicate family (the defaults-group
pattern); chosen at implementation, semantics identical.

**The honest coverage claim** (the first draft overstated this): the
**IR-side differential is complete** — every §3.1 kernel form
translates and gates 5 ≡ 6 cover it; whatever corner implementation
reality still refuses lands as a loud fault in the ASP-TRANSLATION
inventory, never silent, never exercised by a golden until it
translates. The **frontend** coverage is narrower than the kernel:
closed bodies mean the plugin refuses Catala folds whose lambdas
capture enclosing scope variables (a real and common Catala shape) —
loudly, recorded, lifted only when a later phase opens attribute-
and scope-reading bodies (§4, question 2).

### 3.4 WF rules (sketch; numbers continue from design 1)

Declaration/type rules for `ListType` (admissible elements),
`ListLiteral` (one element type, no leeg elements), `FunctionDef`
(unique names, typed parameters/result, closed body, acyclic call
graph), `Invoke` (resolution, arity, argument types), `Lambda`
(position-restricted, closed body, parameter arity per host node),
`IfThenElse` (position-restricted to bodies, well-formed condition,
branch type agreement), `Fold`/`MapList`/`FilterList`/`ListLength`
(list-typed source, lambda arities, boolean filter body, first-order
non-list accumulator, result typing). Negatiefs per rule: recursive
function pair, attribute-read inside a body, lambda outside a fold,
if-then-else outside a body, list-typed accumulator, heterogeneous
list literal, unresolved invoke, arity mismatch.

### 3.5 Plugin mapping

`TArray` → `ListType`; `EArray` → `ListLiteral`; `EAppOp` `Fold` →
`Fold` (Catala's operand order `[f; init; list]`), `Map` → `MapList`,
`Filter` → `FilterList`, `Length` → `ListLength`. **`Reduce` lowers,
it does not map 1:1** (stated precisely because the first draft
hand-waved it): Catala's `Reduce(f, l)` produces an `Optional`-typed
result consumed by the wrapping `EMatch` the desugarer emits; the
plugin lowers it to `Fold` with an `Optional`-typed accumulator —
init `Construct None`, step `λ(acc, x). Match acc { None →
Construct Some(x); Some a → Construct Some(f(a, x)) }` — which is why
sum-typed accumulators are in the kernel and why **design 2 builds on
design 1** (the `Optional` enum the desugarer introduces is an
ordinary payload enum; `ConstructorCheck` on it is a `Match`). The
`sum`/`maximum`/`minimum` surface forms then translate
compositionally: `sum` as `Reduce(Add)`, `maximum`/`minimum` as
`Reduce` with an `IfThenElse` reducer body (§3.1 point 6). `EAbs` →
`Lambda` in fold-operand position (and only there); `EApp` of
`ELocation (ToplevelVar …)` → `Invoke`; `program_topdefs` whose expr
is an `EAbs` → `FunctionDef`, plain-value topdefs → refused until a
case wants them (recorded). The `no_stdlib` flip and `Integer.sum`
admission are 5c-adjacent plugin work (the tutorial route), out of
5b's cards. `Sort`/`Find`/`Map2`/argmin-argmax and scope-capturing
lambda bodies refuse loudly with row-10 messages replacing today's
generic operator refusals.

### 3.6 Corpus anchors (sketch)

A Jutterswet list case: a list-typed parameter (e.g. staffelbedragen)
folded into a total with a cap (exercising `IfThenElse` in the body);
a map/filter composition over a list of sum values (composing design
1); a max-via-`Reduce` case pinning the lowering; absolute expected
values; negatiefs per §3.4. Gate 7: deciding cases fail `--noop`; the
**empty-list and leeg-list corners are no-op survivors** and join the
reason-carrying `SURVIVORS` list (the §2.8 rule).

## 4. What deliberately stays out (all recorded, all loud)

Tuple/named-field payloads (structs, 5c); `TOption` as a native type
(the desugarer's `Optional` enum covers the reachable forms);
recursive sum types; text, boolean and `Enumeratie` payloads; lists of
lists; list-typed temporal attributes; list-typed fold accumulators;
first-class/stored function values; attribute- and scope-reading
lambda/function bodies (the closed-body fence — the frontend refuses
capturing folds loudly); value-level `IfThenElse` outside lambda and
function bodies (rule expressions keep the defaults idiom);
`Sort`/`Find`/`Map2`/argmin; plain-value topdefs; wildcards in the IR;
an explicit leeg-arm in match; unifying the `Functie` aggregation
catalogue with the fold family; any new clingo encoding for
`Enumeratie` values (status quo, recorded). Each lands as a WF
rejection or a loud plugin/executor refusal with its reason string,
inventoried in ASP-TRANSLATION's terminal section where
clingo-relevant.

## 5. Bookkeeping obligations bound to the implementation cards

The ASP-TRANSLATION new sections keep the loud-inventory terminal (the
inventory renumbers to stay the highest section — "the last section is
what does not translate" stays an invariant). RUNTIME-SEMANTICS gains
ch. 9 (sum types and match) and ch. 10 (folds and function values); the
stale status header and the "Chapters (planned)" trailer are updated in
the same commit. IR-SPEC gains ch. 4.7+ and the ch. 2.4 variant-table
rows; the WF index grows per §2.4/§3.4. CONSTRUCTS rows 36 and 10 get
their delta stamps (node names, chapters, WF ids, anchors, pin SHA).
Both cards force a toolchain re-pin (spec/schema and the normative
documents move); conformance stays 11/11 byte-identical. Gate 9
regenerates any `bron.catala_en` golden the plugin work adds.

## 6. The decisions (numbered; ratified by the maintainer 2026-08-22)

**Vraag 1 — payload-vorm van rij 36. Decided (2026-08-22): A — één
payload per constructor** (0 of 1; het Catala-isomorfe model — n-air
is daar al getupeld, nullair is unit; structs blijven volledig in 5c;
het tuple-geval weigert luid tot structs landen). Verworpen: B
(benoemde meerveldige payloads — dat haalt het structontwerp naar
voren).

**Vraag 2 — de rij-10-kerngrens. Decided (2026-08-22): A — de
gesloten kern van §3.1 mét de positiegebonden `IfThenElse` en
sum-getypeerde accumulatoren.** Het volledige IR-differentiaal
(ref ≡ clingo over de hele kern); de plugin draagt daarmee Catala's
`sum`/`maximum`/`minimum`/`combine all`-familie via de uitgespelde
`Reduce`-lowering; wat de frontend blijft weigeren (scope-vangende
body's, sort/find/argmin) weigert luid en staat in §4. Verworpen: B
(zonder `IfThenElse` — de bekendste Catala-lijstvormen zouden buiten
het differentiaal vallen) en C (bredere kern — het differentiaal zou
de kern niet meer dekken en de waarde-wereld van ch. 1 moest open).

Beslissingsvrij vastgelegd (geen vraag): de additieve wire-route (de
vastgelegde Method-discipline); leeg propageert door match, fold én
`Construct` (de bevroren ch. 1.3-default); **`BinderRef` als nieuwe
knoop** in plaats van de eerder overwogen VariabeleRef-herbestemming —
de lenzen toonden die niet-additief, en versmallen naar de
spiegelveilige optie is besluit-vrij (row 24 blijft onaangeroerd
OPEN); `SumType` gehost door `domeinen`; álle Catala-enums →
`SumType` en `Enumeratie` blijft de onaangeroerde v0-spiegelvorm; de
sum-type-nominaliteit krijgt een eigen WF-nummer (WF-008 blijft de
open Enumeration-regel); de no-op-leeg-hoeken als reden-dragende
survivors.

## 7. The implementation cards

Two cards, serial, sum types first (each: full regime, all nine gates,
re-pin, lens round, honest Result):

1. `20260821-fase5b-sumtypes.md` — §2 end to end, incl. the shared
   `locals`/`BinderRef` substrate.
2. `20260821-fase5b-folds.md` — §3 end to end, building on that
   substrate (the `Reduce` lowering consumes design 1's `Optional`
   machinery).

The plugin translation of each design lands inside its card (after the
kernel is gated), converting today's row-36/row-10 refusal fixtures
into passing E2E fixtures plus refusal fixtures for §4's fences.
