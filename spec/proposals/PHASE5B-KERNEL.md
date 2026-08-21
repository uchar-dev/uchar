# PHASE5B-KERNEL — Sum types with payload (row 36) and folds/function values (row 10)

*Status: draft for maintainer ratification · 2026-08-21 · companion to
`PHASE5-CATALA.md` (adopted) and `PHASE4-DEFAULTS.md`. The two kernel
designs Phase 5b exists for, presented together so their shared
machinery is designed once. The numbered decisions in §6 go to the
maintainer before any spec work merges (the WF-059 precedent); the
implementation follows in two cards, sum types first.*

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
ratify there.** Both designs enter exactly as `defaultLadders` and
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

- **5c stays 5c.** No structs, no subscope calls, no nested ladder
  groups (PHASE4-DEFAULTS §7 option i). Catala constructors carry
  exactly one payload — the surface de-tuplifies n-ary payloads into
  one tuple and gives nullary constructors a unit payload
  (`name_resolution.ml:861`, `from_surface.ml:1228–1244`) — so
  single-payload sum types are Catala-isomorphic without touching
  structs. The tuple payload (a Catala n-ary constructor) is refused
  loudly until structs land.
- **The value domain stays first-order.** Function values never become
  runtime values: no arrow datatype in the `datatype` union, no
  closure in the value domain, nothing function-typed is ever storable
  or comparable. Lambdas exist only as operands of fold/map/filter
  nodes and as named top-level definitions — evaluated at the use
  site, never passed around. This keeps RUNTIME-SEMANTICS ch. 1.1, the
  write boundary, and timeline change-detection untouched by design 2.
- **The frozen empty discipline is not reopened.** Ch. 1.3's whitelist
  is exhaustive and match/fold are not on it: a leeg scrutinee
  propagates (whole match → leeg), a leeg list source propagates, and
  nothing coalesces. §2.3 and §3.3 record the consequences; no new
  three-valued form is introduced.

## 2. Design 1 — sum types with payload, compile-time-total match (row 36)

### 2.1 Declaration: a `SumType` datatype, hosted by `domeinen`

A new datatype node (English name, per the `TemporalRead`/
`DefaultLadder` extension-node convention):

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
  name the same declaring domain — the WF-008 rule, inherited verbatim.
  Match and sum-value equality are the first wire sites that put two
  enum-family operands in one expression, so WF-008 finally gets its
  golden and negatief here (closing that motivated OPEN).
- **Payload arity is 0 or 1.** `payload` is `datatype | null`; null =
  nullary. Admissible payload datatypes in v1: `Numeriek`, the date
  types, `Enumeratie`, and `SumType` (nested, non-recursive). `Tekst`
  and `Booleaan` payloads are refused by WF: the ASP value encoding
  carries neither text values nor boolean values (booleans are
  characteristics, the standing v0 discipline), and no Catala source
  form needs them (the payloads that occur are unit, integer, money —
  `syntax_en.catala_en`, the `Optional` desugarings). Loud, recorded,
  extensible later.
- **No recursion.** The declaration graph over sum-type payloads must
  be acyclic (WF). Catala has no recursive types either; recursion
  would also reopen clingo's term-depth abort (its README pins the
  ~200-level SIGABRT; `oc_budget` exists for exactly this reason).
- **Payload-less sum types vs `Enumeratie`.** A `SumType` whose
  constructors are all nullary is semantically the row-27 enumeration.
  The plugin maps an all-nullary Catala enum onto the existing
  `Enumeratie`; `SumType` is used only when at least one constructor
  carries payload. The two forms stay distinct on the wire (unifying
  them would rewrite `Enumeratie`, a non-additive move).

### 2.2 Expressions: `Construct` and `Match`

Two new `expr` `oneOf` members (IR-SPEC ch. 4.7, new):

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
- **Binder reference rides `VariabeleRef`.** The arm body reads the
  payload through the existing v0 node `VariabeleRef`, which v1 hereby
  gives its resolution semantics: a `VariabeleRef` resolves to the
  nearest enclosing binder (a match arm's `binder`; design 2 adds
  lambda and function parameters). This is additive: v0 dumps carry
  `VariabeleRef` only for rule-local variables whose definitions are
  not serialized (the WF-034 caveat), those remain unresolvable and
  ref's existing fault is unchanged. A `VariabeleRef` with no
  enclosing binder of that name is ill-formed (WF). One mechanism
  serves match binders, fold binders, and function parameters.
- Wildcards do not exist in the IR. Catala eliminates them before
  scopelang (the desugarer fans a wildcard out over the missing
  constructors), and RegelSpraak has no match at all; an IR wildcard
  would make totality a runtime property again.

### 2.3 Semantics (RUNTIME-SEMANTICS ch. 9, new)

- **Value domain.** Ch. 1.1's enumeration of the value domain gains
  "a constructor value: a declared constructor tag with, for a unary
  constructor, one payload value". A constructor value is *gevuld* by
  construction.
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
  across different sum types is statically ill-formed (WF-008).
  Non-numeric discipline applies: any leeg operand → false (Tabel 38
  mirror), no ordering (`<`/`>` faults), no coalescing. Structural
  equality feeds the three places that need it: idempotent writes,
  timeline segment change-detection ("numerically equal or both-leeg"
  generalises to "equal under ch. 9 equality"), and the ladder's
  equal-decisions-are-no-conflict rule.
- **Ladders carry sum-typed attributes with zero new ladder
  semantics.** The decide-test is gevuld/leeg, tag-agnostic; a rung
  wanting "decide only for constructor X" puts a match in its
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
| WF-061 | SumType use site: `domain` resolves to a declared domain whose datatype agrees with the use site | undeclared domain; drifted constructor list |
| WF-062 | Construct: constructor declared; argument present iff unary; argument type matches the declared payload | nullary with argument; payload type mismatch |
| WF-063 | Match totality: scrutinee sum-typed; arms cover exactly the declared constructors, each once; binder present iff unary | missing arm; duplicate arm; alien constructor; binder on nullary |
| WF-064 | Match arm agreement: all bodies one result datatype; every `VariabeleRef` resolves to an enclosing binder | mixed arm types; unbound VariabeleRef |
| WF-008 | (existing OPEN, now anchored) nominal identity for enum-family operands | cross-sum-type equality |

Each with a verifier check (`checks.ml` `wf0NN` + the `all` list), at
least one passing golden, and its negatief per the
`corpus/negatief/wf###-slug` regime.

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
`SumType` non-numeric; `eval_expr` gains `Construct`/`Match` arms
(+ `expr_typ`, + the `vintage_read_params` walker recursing into
scrutinee and arms); `env` gains `locals` and the dead `Variabele_ref`
arm resolves against it; `case.ml` per §2.5. Worked examples land as
`WV-9.*` blocks in `ref/test/test_ref.ml` *and* as corpus anchors —
the inline suite is not gate-run, so gate coverage comes from the
anchors (and the `@ref/runtest` CI leg).

**clingo**: a sum value is a ground term — bare functor for nullary
(`afgewezen`), unary as `toegekend(V)` with the payload scaled by its
own declared datatype (`scaled_of_value`/`decimals_of_datatype` gain a
constructor arm). The match lowers to one rule per arm, unifying on
the functor in the scrutinee's `val/3` atom — mutually exclusive by
the functor itself, **no NAF** because totality is static, so the
stratification posture is untouched and the one-stable-model invariant
holds. A leeg scrutinee needs no leeg-arm encoding: no `val` atom
binds, no arm rule fires, the target stays undived — propagation for
free (the S5 point-1 pattern). Sum-value equality in condition
position takes a non-numeric path: ground-term unification, *not* the
numeric eqN asymmetry (S5's warning). `translate_case` emits
constructor terms for sum-typed input; `model.ml` breaks `A_term`'s
deliberate opacity for expectations with a structural term comparison
(the parser already yields the nested structure). New ASP-TRANSLATION
section per the S-numbering rule in §5.

### 2.7 Plugin mapping

`TEnum` → `SumType` (all-nullary → `Enumeratie`), built from
`ctx_enums`; `EInj` → `Construct` (unit payload → nullary; tuple
payload → loud refusal, structs-fence); `EMatch` → `Match`, with the
plugin first replaying the `from_scopelang.ml:223–249` exhaustiveness
fold against `ctx_enums` — **scopelang does not guarantee totality**
(the check lives one pass later), so a partial map is refused loudly
with the missing constructors named, and the IR-side WF-063 stays the
independent gate. Case bodies arrive as one-argument `EAbs` over the
payload (the desugarer wraps them); the plugin unwraps the binder into
the arm's `binder` + `VariabeleRef` bodies. `domeinen` stops being
hard-wired empty in `translate.ml`/`emit.ml`.

### 2.8 Corpus anchors (sketch)

A Jutterswet article (own-authored, wet-v2.md extension if needed): a
`beschikking` sum type as above; a rule matching it into an uitkering
amount; cases per constructor incl. the leeg-scrutinee corner pinned
absolutely; a rood case only if a genuine runtime fault mode exists
(none is expected: totality is static, so the runtime residue is the
ordinary leeg/§9.5 machinery); negatiefs per §2.4. The no-op guard:
expected differs from raw input, so the anchor fails `--noop` as
required.

## 3. Design 2 — folds and function values (row 10)

### 3.1 The kernel, precisely

Six additions, all core:

1. **`ListType`** — new datatype: `{ "node": "ListType", "element":
   datatype }`. Element datatypes: the §2.1 admissible set (scalar,
   first-order; no lists-of-lists, no functions in v1 — loud WF,
   recorded flank). List-typed attributes and parameters are
   admissible (that is where corpus cases get their lists); list-typed
   *temporal* attributes are refused in v1.
2. **`ListLiteral`** — new expr: ordered element expressions, one
   shared element datatype (WF).
3. **`FunctionDef`** — new optional top-level list `functionDefs`
   (the `defaultLadders` pattern): `{name, parameters: [{name,
   datatype}], result: datatype, body: expr}`. **Bodies are closed**:
   a body may reference its own parameters (`VariabeleRef`),
   document parameters (`ParameterRef`), literals, operators, and
   other functions via `Invoke`; free `AttribuutRef`/`Kenmerk` reads
   are ill-formed in v1 (WF). The call graph must be acyclic (WF) — no
   recursion, matching Catala and keeping termination and
   stratification trivial (closed bodies contribute no dependency
   edges, so WF-045/046 and ref's strata are untouched).
   This *is* DMN's BKM: named reusable logic, invoked by name.
4. **`Invoke`** — new expr: `{function, arguments}` with arity and
   types against the definition (WF).
5. **`Lambda`** — new expr form admissible **only** as the operator
   operand of the three fold nodes (WF: a `Lambda` anywhere else is
   ill-formed): `{parameters: [{name, datatype}], body}` with the same
   closed-body rule. Never a value, never storable (§1 fence).
6. **`Fold` / `MapList` / `FilterList` / `ListLength`** — new expr
   nodes over list operands: `Fold {list, function: Lambda|name,
   init, …}` (two-parameter function `(acc, element)`; **the
   accumulator datatype is scalar** — a list-typed accumulator is
   refused in v1, the ASP term-depth fence); `MapList`/`FilterList`
   (one-parameter function; filter body boolean-valued — the one
   place a boolean-typed expression is admissible, mirrored on the
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
  list case input) — "a list with holes" is not a v1 notion.
- **Fold**: `fold(f, init, [x1..xn]) = f(...f(f(init, x1), x2)..., xn)`,
  left-to-right, order normative. `fold(f, init, []) = init`.
  A leeg list source propagates leeg (ch. 1.3 default); so do
  `MapList`/`FilterList`/`ListLength`. Inside the body, the ordinary
  ch. 1.2 operator discipline governs — the fold node itself adds no
  empty policy (compositionality; the aggregation table stays the
  instance-world's own).
- **Invoke**: evaluate arguments strictly (a leeg argument propagates
  leeg without entering the body — the strict-operator default),
  bind, evaluate the closed body. No recursion exists (WF), so no
  budget machinery is needed; a depth guard in ref is defensive only.
- **Worked examples**: sum-of-list via fold (the `Integer.sum` body
  shape: `combine all x among l in total initially 0 with total + x`),
  map-then-fold, filter-on-payload (composing design 1: a list of sum
  values matched per element), the empty-list and leeg-list corners.

### 3.3 Both executors, full differential — no fenced fold

**ref**: `Value.t` gains `Lijst of t list`; `locals` (shared with
design 1) binds lambda/function parameters; new eval arms; `case.ml`
gains the `{ "list": [ … ] }` value variant against `ListType`;
structural equality over lists for idempotent writes and expectations.

**clingo**: list values are **ordered by construction**, which removes
the obstacle that makes general folds over instance sets hard (rol/3
joins are unordered). Encoding: reify a list value as indexed facts
`elem(ListId, I, V)` from `translate_case`/`ListLiteral`, then
index-recursion for fold —
`acc(L, I+1, F) :- acc(L, I, A), elem(L, I+1, V), F = ⟨f(A,V)⟩` —
with a scalar accumulator the terms stay flat (no nesting, no
term-depth risk; the WF scalar-accumulator rule is what buys this).
`MapList`/`FilterList` encode element-wise and flat
(`elem(out, I, f(V)) :- elem(in, I, V)`); `ListLength` is `#count`.
Closed lambda bodies translate with parameters as bound ASP variables —
no attribute reads inside bodies means no join surface beyond the
`elem` atoms. `Invoke` of a `FunctionDef` inlines (acyclic, so
inlining terminates) or emits a parameterised helper predicate family
(the ladder-group pattern); chosen at implementation, semantics
identical. Expected list values compare structurally in `model.ml`
via the same `A_term`/indexed-fact route as design 1. **Everything in
§3.1 translates — the differential (gates 5 ≡ 6) covers the whole
fold kernel; nothing is fenced.** Whatever partial form implementation
reality still refuses (e.g. a corner the encoding cannot carry) lands
as a loud fault recorded in the ASP-TRANSLATION inventory — never
silent, and never exercised by a golden until it translates.

### 3.4 WF rules (sketch; numbers continue from design 1)

Declaration/type rules for `ListType` (admissible elements),
`ListLiteral` (one element type, no leeg elements), `FunctionDef`
(unique names, typed parameters/result, closed body, acyclic call
graph), `Invoke` (resolution, arity, argument types), `Lambda`
(position-restricted, closed body, parameter arity per host node),
`Fold`/`MapList`/`FilterList`/`ListLength` (list-typed source, lambda
arities, boolean filter body, scalar accumulator, result typing).
Negatiefs per rule: recursive function pair, attribute-read inside a
body, lambda outside a fold, list-typed accumulator, heterogeneous
list literal, unresolved invoke, arity mismatch.

### 3.5 Plugin mapping

`TArray` → `ListType`; `EArray` → `ListLiteral`; `EAppOp` `Fold` →
`Fold` (Catala's operand order `[f; init; list]`), `Map` → `MapList`,
`Filter` → `FilterList`, `Length` → `ListLength`; `Reduce` and the
`Optional`-wrapped `sum`/`maximum`/`minimum` desugarings translate
compositionally — the `Optional` enum the desugarer introduces is an
ordinary payload enum, so design 1 carries it (`ConstructorCheck` →
a `Match` on it); `EAbs` → `Lambda` in fold-operand position (and only
there); `EApp` of `ELocation (ToplevelVar …)` → `Invoke`;
`program_topdefs` whose expr is an `EAbs` → `FunctionDef`, plain-value
topdefs → refused until a case wants them (recorded). The `no_stdlib`
flip and `Integer.sum` admission are 5c-adjacent plugin work (the
tutorial route), out of 5b's cards. `Sort`/`Find`/`Map2`/
`ConstructorCheck`-outside-`Optional` refuse loudly with row-10
messages replacing today's generic operator refusals.

### 3.6 Corpus anchors (sketch)

A Jutterswet list case: a list-typed parameter (e.g. staffelbedragen)
folded into a total with a cap; a map/filter composition over a list
of sum values (composing design 1); absolute expected values; the
empty-list vs leeg-list corner pinned in both executors; negatiefs per
§3.4. No-op-guard behaviour as in §2.8.

## 4. What deliberately stays out (all recorded, all loud)

Tuple/named-field payloads (structs, 5c); `TOption` as a native type
(the desugarer's `Optional` enum covers the reachable forms);
recursive sum types; text/boolean payloads; lists of lists; list-typed
temporal attributes; list-typed fold accumulators; first-class/stored
function values; attribute-reading function bodies;
`Sort`/`Find`/`Map2`/argmin; plain-value topdefs; wildcards in the IR;
an explicit leeg-arm in match; unifying the `Functie` aggregation
catalogue with the fold family. Each lands as a WF rejection or a loud
plugin/executor refusal with its reason string, inventoried in
ASP-TRANSLATION's terminal section where clingo-relevant.

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

## 6. The decisions (numbered; to the maintainer before any spec work merges)

**Vraag 1 — payload-vorm van rij 36.** A: **één payload per
constructor** (0 of 1; het Catala-isomorfe model — n-air is daar al
getupeld, nullair is unit; structs blijven volledig in 5c; het
tuple-geval weigert luid tot structs landen). B: benoemde
meerveldige payloads (mini-structs) nu al in de kern — breder, maar
het haalt het structontwerp naar voren en vergroot elke executor- en
schema-stap. **Aanbeveling: A.**

**Vraag 2 — de rij-10-kerngrens.** A: **de gesloten kern van §3.1**
— lijstwaarden met scalaire elementen, benoemde niet-recursieve
functies met gesloten body's, lambdas uitsluitend als fold-operand,
scalaire accumulatoren — en dáárdoor het volledige differentiaal
(ref ≡ clingo over de hele fold-kern; niets afgeschermd). B: een
bredere kern (eerste-klasse functiewaarden, attribuut-lezende body's,
lijst-accumulatoren) waarvan de ASP-kant substantiële delen luid
weigert — expressiever op papier, maar het differentiaal dekt de kern
dan niet meer en de waarde-wereld van ch. 1 moet open.
**Aanbeveling: A.**

Beslissingsvrij vastgelegd (geen vraag): de additieve wire-route (de
vastgelegde Method-discipline); leeg propageert door match en fold (de
bevroren ch. 1.3-default); `VariabeleRef` als het ene
binder-mechanisme (additieve v1-semantiek van een dode v0-knoop);
`SumType` gehost door `domeinen`; WF-008 eindelijk verankerd;
Enumeratie blijft de payload-loze vorm.

## 7. The implementation cards

Two cards, serial, sum types first (each: full regime, all nine gates,
re-pin, lens round, honest Result):

1. `20260821-fase5b-sumtypes.md` — §2 end to end, incl. the shared
   `locals`/`VariabeleRef` substrate.
2. `20260821-fase5b-folds.md` — §3 end to end, building on that
   substrate.

The plugin translation of each design lands inside its card (after the
kernel is gated), converting today's row-36/row-10 refusal fixtures
into passing E2E fixtures plus refusal fixtures for §4's fences.
