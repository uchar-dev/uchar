# ASP translation

*Status: the systematic construct-by-construct translation (Part I,
sections S1–S19, complete for the translated core) — the Phase-3
deliverable this document was always growing toward. Part I describes what `backends/clingo` provably does:
every claim is distilled from `backends/clingo/lib/translate.ml` and
witnessed by the gate set (gate 6, `scripts/check-clingo.sh`, replays
every golden case through the translation; gate 8,
`scripts/check-rood.sh`, pins the observable faults; the negatief
corpus under gate 3 pins what the verifier rejects before translation
is ever reached). The text normates what the code proves — semantics
itself is defined by `RUNTIME-SEMANTICS.md` and this document must
never drift from it; where a construct does not translate, Part I says
so loudly (S19), never silently. Part II (chapters 1–7) is the
historical record: the two hand exercises and the encoding notes in
the order they landed, kept in situ with their original chapter
numbers because results, cards and code comments cite them (ch. 6a–6d
especially). Where Part I and Part II describe the same machinery,
Part I is the authoritative formulation.*

## Part I — The systematic translation

Sections S1–S7 cover program shape, facts and types, rules and write
targets, expression terms, emptiness, arithmetic and conditions; S8
aggregations and folds, S9 timelines, S10 defeasibility and standing,
S11 creation, S12 allocation and uniqueness; S13 the §9.5 channel;
S14 sum types and total match; S15 folds and function values; S16
structs (the record datatype); S17 nested exception groups; S18 the
strict `Let`, guarded consequences and the split money multiplication;
S19 the complete inventory of loud non-translations — what the backend
does not translate is stated there, never silently absent.

### S1. Program shape and conventions

One case, one ASP program, one stable model. The translation of a
`regelspraak-ir-v0` document plus a `uchar-case-v0` case is a single
ASP-Core-2 program; WF-045/046 make every admissible document
stratifiable, so the program has exactly one stable model and the
model checker (`backends/clingo/lib/model.ml`) *faults* on anything
else — zero models and multiple witnesses are both translation bugs,
never answers. The document is desugared first (`Ir_v0.Desugar`, the
ch. 7.7 init+berekening → ladder rewrite): this translator evaluates
the desugared kernel while the reference interpreter replays the v0
forms natively, so gate 6's differential witnesses the desugaring
itself.

The fixed predicate vocabulary:

| predicate | carries |
|---|---|
| `is_a/2` | instance → object type |
| `val/3` | instance, attribute, scaled (or S2 symbolic) value |
| `heeft/2` | instance, characteristic |
| `rol/3` | holder, canonical role name, filler |
| `tl/5` | instance, attribute, from, to, scaled value (timeline segment) |
| `ktl/4` | instance, characteristic, from, to (condition timeline) |
| `param/2`, `param_tl/4` | parameter values, scalar and vintaged |
| `inputval/3`, `inputtl/5`, `inputrol/3` | case input that enters *defeasibly* (S10) |
| `inconsistent/2` | the §9.5 channel: rule-or-form name, subject |
| `viol/2` | a hard-fault witness: reason, subject |
| `depth/2` | creation-budget bookkeeping (S11) |

Two failure channels, deliberately distinct. `inconsistent/2` **is
part of the model**: §9.5 facts coexist with the answer, exactly as
ref reports an inconsistency beside its values (`RUNTIME-SEMANTICS.md`
ch. 1.4). `viol/2` is fenced by the global constraint `:- viol(_,_).`
— any `viol/2` derivation makes the program UNSAT, which is the
encoding of ref's *hard faults*; the runner then re-runs relaxed,
shows the `viol/2` witnesses, and gate 8 pins the reason fragment in
`expectedFault.clingo`. Nothing ever degrades silently: a value the
encoding cannot carry exactly becomes a `viol/2` witness, never a
truncated number.

**Scaling.** clingo grounds arithmetic in machine integers. A value of
an attribute declared with d decimals is carried as the integer value
× 10^d; every arithmetic emission tracks the scale of its term
statically and harmonises scales by exact integer multiplication.
Because machine integers wrap silently (the hunted 50000×50000 <
0 of ch. 6c), *every* path into the integer encoding is fenced — the
bounds table is in S6. Scales themselves are capped at 10^9
(`pow10`); input values are range-checked at load (`scaled_of_value`:
"does not fit N decimals", "exceeds clingo's integer range" — gate-8
witnesses `corpus/rood/input-overprecision`,
`corpus/rood/input-overprecision-param`).

**Names.** Declared names and case labels land between double quotes
in the generated program. WF-057 (IR-SPEC ch. 2.1) rejects documents
whose declared names could not be quoted; the emitter's `quote` fence
remains as the last-line backstop and alone covers case *labels*,
which are outside WF-057's reach. Attribute and rule names are
normalised (`norm`) on both sides of every comparison; rule names in
the channel are matched version-stripped by both executors' checkers
(`RUNTIME-SEMANTICS.md` ch. 5.6).

**The validity gate** (IR-SPEC ch. 3.10 Validity; RUNTIME-SEMANTICS
ch. 8.2 gates it at runtime against the session's `asOfDate`). One
case, one program also fixes the evaluation date: the case's
`asOfDate` is a translation-time constant, so ref's *runtime* gate is
decided at translation, decision for decision (`applies`, the literal
mirror of ref's `applies` in `ref/lib/eval.ml`). A rule, a ladder
member, an object creation or a fact creation whose window excludes
the date **does not apply**: nothing of it is emitted — no head, no
helper predicate, no `viol/2` fence (ref never evaluates an excluded
form, so a zero divisor or range hazard inside one must not fault
here either). Window bounds compare as ISO strings, both ends
inclusive, exactly as ref compares them; a *bounded* window in a
time-free session (`asOfDate` null) faults with ref's own reason
(`a validity-bounded form needs an evaluation date; this session is
time-free`), so the two executors refuse identically. Windowed forms
never collide with the defeasible-input machinery: the ch. 7.7
desugaring and the ch. 7.8 standing norm both demand *version-free*
forms, so an excluded rule's target simply keeps its case input as
the plain `val/3` fact — with one seam handled explicitly: a ladder
*group* whose members are all excluded still emits the unconditional
pass-through `val(S,a,V) :- inputval(S,a,V), is_a(S,ot).`, because
the defeasible entry keys on the unfiltered ladder list (S10/S17).
Windows on the forms the gate does not carry — allocation,
consistency checks, uniqueness — fault loudly (S19). Witness:
`jutterswet-vensterheffing` (one rule inside its window, one before
it, replayed at an `asOfDate` on each side).

### S2. Case input: facts and types

The case loader's world enters as ground facts. Per instance: `is_a`
with its label; each scalar attribute value as `val/3` (scaled); each
characteristic as `heeft/2`; each attribute timeline segment as
`tl/5`; each condition timeline as `ktl/4`; each role filling as
`rol/3`. Parameters enter as `param/2`, vintaged (granular) parameters
as `param_tl/4` segments; a *scalar* value on a granular parameter is
refused at load by the shared case loader (both executors identically
— the fence that keeps an unread slot from splitting the
differential). A granular parameter is only ever read temporally
(S4/S9): the scalar-slot read faults.

The ch. 1.2d **non-numeric scalars** ride ground *symbolic* terms in
the same `val/3` slot, beside the integer encoding
(`symbolic_value_term`): a **text** value is its verbatim quoted
string (ref compares `String.equal` — spelling and case are
load-bearing), an **enum** label its NORMALISED quoted string (ref
compares norm-equal, so both executors normalise at the same point),
and a **boolean** the bare symbol `waar`/`onwaar`. A boolean input
additionally keeps its closed-world characteristic reading — `true` →
`heeft/2`, `false` → absence (the `bool_atoms` read of S11) — because
the equality read needs the three-way distinction (waar / onwaar /
leeg) that `heeft/2` alone cannot carry: `false` is a *value* in the
`val/3` carriage, distinct from absence. Day-precision **dates** are
not symbolic: a date is its civil day number in the integer encoding
(S4), and date equality is *numeric* (the adopted reading of
`ref/lib/value.ml`). A `dateTime` value stays refused — milliseconds
since epoch exceed clingo's integer range.

The standing-target routing is the one semantic decision made at
input time (RS ch. 7.8; S10 carries the detail): if the target attribute of
a defeasible producer — a ladder target, or a bare single-writer
version-free computing rule (the standing norm) — receives case
input, that input enters as `inputval/3` (scalar) or `inputtl/5`
(timeline) instead of `val/3`/`tl/5`, so the producer can defeat it;
everything else enters as plain facts. Case input on a *non-standing*
computed target is refused loudly (ref overwrites; a monotone
encoding would union — the hunted superset model of ch. 6d). Role
fillings are doubled as `inputrol/3` when object creation is present
(the ch. 5.5 input-snapshot narrowing, S11). With recursive creation,
every input instance gets `depth(L,0)` under the creation budget.

Witnesses: every golden case exercises this section (gate 6);
standing routing by the ladder anchors, `jutterswet-waardestelling`
(the scalar standing norm) and `jutterswet-borgdeel` (the temporal
arm); vintages by `jutterswet-aanmeldquotum`,
`jutterswet-penningvintage` and `corpus/rood/vintage-uncovered-date`;
the symbolic carriage by `jutterswet-vondstetiket` (text),
`jutterswet-keurstaat` (enum, including the case2 norm-insensitive
spelling), `jutterswet-borgvlag` (boolean — case2 pins `onwaar` as a
value distinct from case3's absence) and `jutterswet-jutdatum` (the
date-as-day-number sibling).

### S3. Rules and write targets

A `ResolvedRegel` translates by kind. **Characteristic attribution**
(`KENMERKTOEKENNING`, value `waar`): `heeft(S,k) :- is_a(S,ot),
⟨condition atoms⟩.` — negation-as-failure gives the absent case for
free, and WF-045/046 stratification makes the negated reads sound.
**Computation/equation** (`BEREKENING`/`GELIJKSTELLING`) to a scalar
attribute target:

```
val(S,a,⟨term⟩) :- is_a(S,ot), ⟨condition⟩, ⟨bindings⟩, ⟨fit guards⟩.
```

where `⟨term⟩` is the value expression's scaled term (S4–S6) and the
rule's condition atoms are threaded into *every* `viol/2` or
`inconsistent/2` head the value expression emits (`ctx.rule_guard`):
ref evaluates the value only under a true voorwaarde, so an excluded
subject must never fault or deposit a channel fact (pinned by
`jutterswet-vindersloon` case4 — a zero divisor under a false
condition stays consistent).

**The write boundary** (`fitted`): the term's scale is fitted to the
target's declared scale. Equal — unchanged; coarser — exact upscale
under a range guard; finer — downscale guarded by runtime
divisibility, and a value that does *not* fit the declared decimals
derives `viol("result of ... does not fit its declared decimals")`
rather than truncating (a recorded loud asymmetry: ref writes the
exact rational). Non-aligned scales fault at translation time. Units
at the write boundary must be identical (S6's unit fence).

A **standing** scalar target (S10) additionally emits an
effectiveness atom carrying the full rule body, and the defeasible
input rule `val(S,a,V) :- inputval(S,a,V), is_a(S,ot), not
⟨eff⟩(S).` — the producer wins exactly where it decides; an empty
result derives no effectiveness and the input stands (ref's
no-write-on-empty, RS ch. 7.8).

A **text/enum scalar target** takes the sum/struct-target route: the
value — a text/enum literal or an own-subject text/enum attribute
read, lowered by `symbolic_term` (S2's ground terms) — rides straight
into the `val` head, no scale, no unit, no fit. A leeg source read
binds no atom, so the rule does not fire and the target is leeg
(propagation for free). A case-supplied text/enum *computed* target
has no monotone encoding here (the sum-target sibling) and is fenced
loudly. A **boolean computed target** is refused with its reason: a
boolean is a characteristic in v0, so rule forms *read* it
closed-world through `heeft/2` (the WF-027 `bool_atoms` guard, S11),
which a derived `val/3` would not feed — the two encodings would
split, and no corpus case computes a boolean attribute. Witnesses:
`jutterswet-vondstetiket` (the literal write `rubriek` and the copy
`afschrift`), `jutterswet-keurstaat` (the enum copy `oordeel` and the
conditional literal `stempel`).

A rule's **validity window** rides S1's validity gate: an excluded
rule emits nothing and its target's case input stands untouched; an
in-window rule translates as any version-free rule — including the
ch. 6d fence when the case supplies its (never-standing) computed
target. Witness: `jutterswet-vensterheffing`.

Not translated, loudly (S19 carries the full list):
temporal targets outside the two ch. 3.7/3.8 fold shapes; case input
on non-standing computed targets; a `standing` target outside its own
domain; boolean computed targets (above).

### S4. Expression terms

Every value expression lowers to a triple ⟨body atoms, integer term,
scale⟩ (`expr_term`). **Literal**: the scaled constant, scale from its
lexeme's decimals; a **date literal** (`|2000-01-01|`) is its civil day
number at scale 1 — the *same* encoding as a date attribute read
(`val/3`), the point read and the Rekendatum constant, so a date
comparison harmonises day numbers directly and needs no second date
encoding (B2). ref faults date *arithmetic* (`numeric_ok` gates the
additive/multiplicative families on both operands), so a date literal
reaches comparison operands only; a text/enum/boolean literal in a
NUMERIC term position still faults (S19) — in the positions that
carry the non-numeric scalars (an equality operand, a text/enum write
value) it lowers through `symbolic_term` to its S2 ground term, never
through `expr_term`. **Parameter reference**: body atom `param("p",P)`,
term `P`; granular parameters are refused in scalar slots (ch. 3.3/8).
**Attribute reference**: body atom `val(S,a,A)`; a *navigated*
reference (ch. 5.8) chains one `rol/3` atom per to-one hop before the
final `val` — the shared variable is the join, and an absent link
simply leaves the body underivable. Role resolution reuses the
reference interpreter's own binding helpers (`Eval.bound_role`,
`Eval.connecting_role`), so both executors resolve navigation
identically. **Rekendatum** (ch. 8.2): the session's evaluation date
as a compile-time civil day number; a case without `asOfDate` faults.
**Temporal point read** (ch. 3.3): a `tl/5`/`param_tl/4` atom with
`F <= D, D < T` — the gap and the empty-index corner are the
underivable body, for free (more in S9). **Onopgelost**: translation
faults with "Onopgelost reached" — the WF-044 unsolved-state guard;
the rule fails observably (gate-8 witness
`corpus/rood/wf044-onopgelost-reached`).

Witnesses: navigation by `synth-navigatiepremie`,
`jutterswet-vindersloon`, `jutterswet-borgdeel`; Rekendatum and point
reads by `jutterswet-penningvintage`, `jutterswet-aanmeldquotum`.

### S5. Emptiness: the three encodings

Leeg is *absence*: an underivable `val/3` atom. Everything else
follows from where a rule's body may or may not bind (RS ch. 1.2 is
the norm; the encoding buys most of it for free):

1. **Bare join — propagation.** An operand position whose emptiness
   propagates (verminderd-met's left, the power's base, a rounding's
   basis, every comparison operand) is simply the bound variable in
   the body: leeg leaves the rule underivable, which is exactly Empty
   propagation. The ch. 1.2d ordering rule (a comparison over leeg is
   false) costs nothing: the unbound join fails.
2. **`coalesce0` — the whitelist.** A COALESCING operand position
   (ch. 1.2a: both sides of plus/min/maal/percentage-van, the right
   of verminderd-met) reads leeg as 0 through a default-0 helper:
   `coN(S,⟨term⟩) :- ⟨body⟩.`, `coN_def(S) :- ⟨body⟩.`,
   `coN(S,0) :- is_a(S,ot), not coN_def(S).`
3. **Default results on emptiness.** Where the *result* of a whole
   construct is 0 on emptiness, the encoding is per construct: the
   division's empty-dividend edge is a default head under NAF on the
   quotient predicate (`qN(S,0) :- is_a(S,ot), not qN_ldef(S).` —
   note the dividend is *not* a coalesce0 position; the edge lives on
   the construct's head), while the empty sum's 0 is clingo's native
   `#sum` over an empty tuple set (translated only with the
   fallback-0 facet, S19).

The one empty rule with no *free* encoding — the **numeric** equality
asymmetry of ch. 1.2d (leeg = leeg is true under gelijk-aan, false
under ordering) — is encoded (2026-08-21) through a per-comparison
helper: `eqN(S)` holds iff both operand bodies bind and the
harmonised values match, *or neither binds*; ongelijk-aan is
negation-as-failure over the helper — its exact complement, which
ch. 1.2d grants **numerically only** (the non-numeric branch makes
*any* empty operand false in both directions, so there the two are
not complements). Sound for
the same reason `coalesce0`'s definedness negation is: the helper
reads only the operand bodies. Anchored by
`jutterswet-sluitregister` (all four corners: equal, different,
half-empty, both-empty). The **non-numeric** branch (text/enum/boolean
`Gelijk`/`Ongelijk`, since 2026-08-25) needs no helper at all: both
operands lower through `symbolic_term` to positively-bound ground
terms (S2), `Gelijk` tests `==`, `Ongelijk` tests `!=` — never `not
gelijk` — and *any* empty operand fails the join in both directions,
which is exactly Tabel 38 (the sum/struct S14/S16 pattern on the
scalar datatypes). Anchored by `jutterswet-vondstetiket`,
`jutterswet-keurstaat` and `jutterswet-borgvlag` (each with the
both-directions-false leeg corner beside `jutterswet-jutdatum`'s
*numeric* date corner, where ongelijk-aan on leeg is waar — the
asymmetry pinned side by side). What remains loud: equality in the
per-segment clip comparison (S19).

Witnesses: `jutterswet-leegrekening`, `synth2-leegcomposities`,
`jutterswet-vindersloon` (every empty cell), `jutterswet-taxatie`
case3 (empty dividend → 0 beside empty base → leeg).

### S6. Arithmetic and its fences

All arithmetic is exact integer arithmetic on scaled terms; the
encoding never computes a quotient it cannot prove exact or a
rounding it cannot compute in one integer step.

- **Additive** (plus, min, verminderd-met): scales harmonised to
  max(sl,sr) by exact multiplication; emptiness per the whitelist.
- **Multiplicative** (maal, percentage-van): scaled product at scale
  sl·sr; percentage-van folds its /100 into the scale (sl·sr·100) —
  no division anywhere. Both operands coalesce.
- **Division** (gedeeld-door): the three §6.5 edges ride beside the
  value — empty dividend → 0 *before* the divisor is read; empty
  divisor under a filled dividend → the rule-keyed channel fact and
  no value; numeric zero divisor → `viol("division by numeric
  zero")` — the v0-mirror hard fault, which ch. 1.4 point 4 keeps as
  the mirror while v1 routes this case into the §9.5 channel (a
  decided, tracked delta both executors will follow together when the
  v1 divergence lands). The bare form translates only EXACT outcomes:
  a remainder
  derives a `viol/2` witness, never a truncated value. The
  rounded form (afgerond over a division) computes the mode's integer
  rounding in one step — floor T/N, ceiling (T+N−1)/N,
  half-away-from-zero (2T+N)/(2N) — exact for non-negative operands,
  the negative range fenced (richting-nul alone matches clingo's
  native truncation on both signs and needs no fence).
- **Abs-division** (gedeeld-door-abs, ch. 2.2): the rounded-division
  path at (richting-nul, 5) — the defined truncation point, both
  signs, no extra fence. Anchor `jutterswet-taxatie` (full sign
  table); red `corpus/rood/division-abs-by-numeric-zero`.
- **Power** (tot-de-macht): `((t)**e)` at compile-time scale sl^e for
  a compile-time non-negative integer exponent; the base operand is
  fenced at the largest value whose e-th power stays inside 2^31 —
  the wrap would happen during *grounding*, before any guard on the
  result could see it. Signed bases translate unfenced in value
  (gringo's `**` matches the exact integer power, including x^0 = 1
  and 0^0 = 1). Runtime, negative and fractional exponents fault.
- **Rounding** (afgerond, ch. 2.5): scale surplus divided away by the
  mode's formula; an empty basis propagates; a finer target upscales
  exactly.

The fence table — every emitted operation carries `viol/2` range
guards on its operands ("arithmetic range exceeded (clingo grounds in
machine integers)"):

| position | bound on the scaled operand |
|---|---|
| product factor (maal, percentage-van) | 46340 |
| `#sum` contributor and contributor count | 46340 each (46340² < 2³¹) |
| additive operand | 1073741823 / harmonisation factor |
| division operand | 536870912 / its exact-quotient factor |
| comparison / upscale / write fit | 2147483647 / harmonisation factor |
| power base (exponent e) | ⌊(2³¹−1)^(1/e)⌋ (46340 for e=2, 1290 for e=3) |
| result scale | ≤ 10^9, compile-time |

The safe side is pinned: `synth-grensrand` replays the largest
translatable product (46340² = 2147395600) and `jutterswet-taxatie`
case5 the largest translatable powers, so the guards provably do not
fire early. **Units:** operands of the multiplicative family must be
effectively unit-free, and positions where ref *restates* one operand
into the other's unit (the additive join, the comparison, the write
boundary) accept only identical units or a unitless side — ref's
conversion factors have no integer encoding (the hunted 1 m + 50 cm =
51 of ch. 6c). The unit is derived recursively from declarations
(`eff_unit`), never read from a node's static annotation.

### S7. Conditions

`cond_atoms` lowers a condition to body atoms. **Comparison**
(vergelijking): harmonised scaled terms joined by `>=`, `>`, `<`,
`<=`; both operands are bare joins, so a leeg operand fails the
condition (ch. 1.2d). **Equality/inequality**: statically routed
on the operands' declared kind, exactly as ref routes
(`static_numeric`): numeric operands take the eqN-helper encoding of
S5 — gelijk-aan through the helper atom, ongelijk-aan through its
negation (witness `jutterswet-sluitregister`); text/enum/boolean
operands take the positive symbolic-term join of S5 — `==`/`!=` over
both bound terms, any empty operand false in both directions
(witnesses `jutterswet-vondstetiket`, `jutterswet-keurstaat`,
`jutterswet-borgvlag`). **Characteristic check** (kenmerk-check): `heeft(S,k)`, or
`not heeft(S,k)` under negation — stratification (WF-045/046) makes
this sound. The *navigated* check walks the `rol/3` hop as a POSITIVE
literal with only the characteristic under the negation: an absent
link fails the positive and the negated form alike — the deliberate
v0 asymmetry (ch. 5.8), encoded for free. **Composite**
(samengesteld, the ALLE and singular DE quantifiers): concatenation
of the parts' atoms — both quantifiers are conjunctions over their
parts. Other condition kinds fault (S19).

**Composite, counting** (samengesteld — GEEN, TEN MINSTE, TEN
HOOGSTE, PRECIES; the decision-table desugar's disjunctive row is
TEN MINSTE 1): ref counts the members that evaluate true (its hits)
and compares against the threshold — GEEN hits = 0, TEN MINSTE
hits >= d, TEN HOOGSTE hits <= d, PRECIES hits = d. Encoded per
composite: each member i becomes its own indexed helper rule
`cqN(S,i) :- is_a(S,ot), ⟨member atoms⟩` — derivable exactly where
ref counts a hit, so a leeg member operand counts false through the
unbound positive join (Tabel 38 for free) — and one aggregate rule
`cqN_n(S,H) :- is_a(S,ot), H = #count { I : cqN(S,I) }` binds the
hit count per subject (0 included). The composite itself contributes
only the plain atoms `cqN_n(S,H)` plus the threshold comparison, so
it serves every position `cond_atoms` serves — aggregate elements
included, where a body aggregate literal could not appear. The
non-monotone quantifiers (GEEN / TEN HOOGSTE / PRECIES behave like
negation) stay sound because the verifier's WF-045/046 dependency
walk conservatively marks their members as negative edges, keeping
the ground program stratified. A counting composite without members
faults, and every member kind keeps its own fault (S19).

Witnesses (gate 6): `jutterswet-rubriektabel` (TEN MINSTE 1 — the
disjunctive decision-table row over text equalities — and TEN MINSTE
2 over mixed kenmerk/comparison members, threshold boundary
included), `jutterswet-vrijgavetoets` (GEEN, with the
leeg-member-counts-false corner), `jutterswet-sluimerwacht` (TEN
HOOGSTE, hits 1/2/3 around threshold 1), `jutterswet-tweevoudkeur`
(PRECIES, hits 2 against 3). Further: `jutterswet-hardheid`,
`jutterswet-uitsluiting`,
`jutterswet-vormenkabinet`; the WF-053 negatiefs pin the navigation
well-formedness the verifier enforces before translation.

### S8. Aggregations and folds

**Count** (aantal): `V = #count { X : rol(S,role,X), ⟨filter
atoms⟩ }`, scale 1 — the role read via the node's binding or the
declared role name, the `die/dat` filter lowered by `cond_atoms` over
the element variable. An element the filter's body cannot derive
contributes no tuple: the contributor skip is the join, for free.
**Sum** (som): contributor body `rol(S,role,X), val(X,a,G), ⟨filter⟩`
and `V = #sum { G,X : ⟨body⟩ }` at the element attribute's scale —
an instance without the attribute contributes no tuple, and the empty
tuple set is clingo's native 0, which is why only the fallback-0
facet translates (a som without it would need an explicit non-empty
guard to distinguish leeg from 0; faults until a corpus case demands
it, S19). The *sum* is doubly fenced: each contributor at 46340 *and*
the contributor count at 46340 (`viol` when `46340 < #count{...}`),
so the accumulated sum provably stays inside 2^31. The bare count
carries no fence — a `#count` cannot approach 2^31 before grounding
itself becomes infeasible (recorded, not guarded; ch. 6c).

**Temporal total, clipped** (totaal ... gedurende, ch. 3.6): the
clip predicates a fold over the subject's *own* temporal attribute on
a condition timeline. Per clip N: the condition segments (the
`ktl/4` atoms read directly for the kenmerk shape, or — for the
per-segment comparison of an own temporal attribute — an
intermediate `clN_cond(S,V2,T2)` predicate; a FLAT composite ALLE
condition over characteristic members intersects them pairwise
through `clN_and` helper predicates, the same four-case max/min as
below — v0 nests per-member clips over such flat lists, and
intersection is associative), the four-way
segment intersection
(`clN_seg(S,V,T,W)` with V = max of starts, T = min of ends), the
calendar as *data* (`clN_wu(V,T,N)`: whole units per clipped span),
and the total `Q = #sum { W*N,V,T : clN_seg(S,V,T,W), clN_wu(V,T,N)
}`. An absent or clipped-away timeline sums over the empty set: the
clip's presence turns absence into 0 (the mirrored v0 nuance, RS
ch. 3.6). Fences: |W| ≤ 46340, the endpoint span at 46340 whole
units (segments are disjoint slices of one timeline, so |ΣW×N| ≤
46340² holds), and the comparison harmonisation bounds of S6.
**Temporal total, unclipped**: the asOfDate-bucket convention — the
case supplies the bucket scalar as a plain `val/3` and the total
reads it (×year factor; only year granularity is exercised); the
total over a case-supplied *full* timeline faults, loudly.

Witnesses (gate 6): count by `jutterswet-erflijn`,
`jutterswet-maatschap`, `jutterswet-kustregister`; sum by
`jutterswet-bergingstoezicht`, `jutterswet-gemeenteberging`,
`jutterswet-penninguitgifte`, `synth2-budgetgrens-onder`; the clip by
`jutterswet-getijdenwacht`, `jutterswet-penningjaar`,
`synth2-clip-over-prorata`, `synth2-clip-cond-over-prorata`; the
WF-055 negatiefs pin the clip's admissible condition shapes.

### S9. Timelines

A timeline is data, not state: `tl/5` segments (instance, attribute,
from, to, scaled value) and `ktl/4` condition segments, with civil
day numbers as endpoints. Three readers and two producers translate.

**Point read** (ch. 3.3): `tl(S,a,F,T,R), F <= D, D < T` — and for a
granular parameter `param_tl("p",F,T,R)` likewise. A gap or an empty
date: the body is underivable, leeg for free. An uncovered *vintage*
is the opposite — the shared `parameterset_check` fail-fast, run
before emission (gate-8 witness
`corpus/rood/vintage-uncovered-date`). A point read needs the
full timeline in the case; the asOfDate-bucket scalar cannot serve it
and faults (the ch. 4.3a convention: executors fault, never guess).

**Pro-rata** (tijdsevenredig-deel, ch. 3.7): per fold N, the bucket
calendar as data (`prN_b(bs,bn,days)` — every bucket in the corpus
endpoints' span; the calendar is condition-blind), the true-day
intersection (`prN_d(S,B,KV,KT,D)`: days of `ktl` overlap per
bucket), and the head `tl(S,a,B,N,⟨W·TD/Tot⟩)` under a `TD > 0`
gate — the *head*, not the calendar, restricts to touched buckets:
an untouched bucket derives no segment.
The quotient translates only exactly: a non-exact pro-rata part
derives `viol("non-exact pro-rata part (an explicit rounding is
mandated before precision is lost)")`. WF-056 fences the shape at
translation time (temporal base, own temporal characteristic under
gedurende, bucket = target granularity, base not finer than the
bucket). **Day count** (aantal-dagen-in, ch. 3.8): the same bucket
and true-day machinery (shared emit helpers) with the head carrying
`TD` itself — pro-rata's numerator as the value, unitless; WF-058
mirrors WF-056's facets. The 46340 contributor-count guard is
shared; the 366-headroom bound differs per producer — pro-rata
fences the weight |W|, the day count fences the target's decimal
scale (a compile-time fault).

Both producers carry the ch. 7.8 **standing arm** (S10): a per-fold
effectiveness atom (`prN_eff`/`adgN_eff`) with the full bucket body —
for pro-rata including its exactness guard; the day count has no
quotient, so its eff body is the plain bucket body — and the
defeasible input rule
`tl(S,a,F,T,V) :- inputtl(S,a,F,T,V), is_a(S,ot), not prN_eff(S).`
An empty fold derives no effectiveness and the input timeline stands
**whole**, never mixed per bucket (the maintainer-ratified
whole-timeline reading, RS ch. 7.8).

Witnesses: point reads and vintages by `jutterswet-aanmeldquotum`,
`jutterswet-penningvintage`; pro-rata by `jutterswet-borgdeel`,
`jutterswet-bakenmeester`, `synth2-prorata-breed`; day count by
`jutterswet-dagentelling` (the RS ch. 3.8 worked example verbatim);
the WF-049/056/058 negatiefs pin the shapes.

### S10. Defeasibility and standing

The Phase-4 default calculus translates as layered
negation-as-failure — stratification (WF-045/046) is what makes every
layer sound.

**Ladders** (`defaultLadders`, RS ch. 7): ladder members are grouped
by subject and target. A member whose validity window excludes the
`asOfDate` leaves the member set before translation (ref's
`member_decision` returns Undecided before a single rung is
evaluated — S1's validity gate); a group whose members are *all*
excluded decides nothing and emits only the unconditional input
pass-through. Per member i, rung n:
`dlI_decides_n(S) :- is_a(S,ot), ⟨rung condition⟩[, ⟨consequence
bindings⟩].` — for an *attribute* ladder the consequence's bindings
sit inside `decides`, so an empty consequence cannot decide (the
filled-gate for free); higher rungs displace lower ones by `not
dlI_decides_m` for every higher m. The decision projects into
`dlI_dec(S,b)` (characteristic arm) or `dlI_out(S,term)` (attribute
arm), and the head fires under the group's conflict guard:
`heeft(S,k) :- dlI_dec(S,1), not grpG_conflict(S), ...` /
`val(S,a,V) :- dlI_out(S,V), not grpG_conflict(S), ...`. Equal-rung
disagreement between members derives
`inconsistent("onderwerp.target",S) :- dlI_dec(S,X), dlJ_dec(S,Y),
X != Y` — the §9.5 channel, never an arbitrary pick (a singleton
group elides the conflict machinery entirely). An `initialisatie`
rung additionally requires `not grpG_has_input(S)`: initialisation
yields to case input by definition (RS ch. 7.3, and the virtual
input rung of ch. 7.8 that sits above it).

**Standing input** (RS ch. 7.8): case input on a defeasible target
enters as `inputval/3` (S2) and re-enters the model through
`val(S,a,V) :- inputval(S,a,V), is_a(S,ot), not grpG_eff(S).` — the
producer defeats the input exactly where it *decides*. The
effectiveness atom's body **is** the semantic choice: it carries
condition *and* value bindings, so a producer that fires but derives
nothing (an empty ladder outcome, an empty computing rule) leaves the
input standing — ref's no-write-on-empty. A condition-only body
would encode v0's erase instead. The standing norm extends this to
bare single-writer version-free computing rules (S3) and to the
temporal producers (S9, whole-timeline).

Witnesses: `jutterswet-hardheid` (the §4.1 hardship pattern),
`jutterswet-toelagegronden`, `jutterswet-uitsluiting`,
`jutterswet-vondstwaardering` (conflict), `jutterswet-waardestelling`
(scalar standing), `jutterswet-borgdeel` (temporal standing); the
WF-050/051 negatiefs pin ladder well-formedness; gate 7's
reason-carrying no-op survivors pin exactly the cases where standing
input must survive an ineffective producer.

### S11. Creation

**Object creation** (ch. 3.3/5.5): the created instance is a
deterministic function term `ocI(S)` — set semantics gives
idempotence for free (re-deriving the same head is the same atom).
`is_a(ocI(S),new) :- ⟨body⟩.`, member initialisations as
`val(ocI(S),m,⟨term⟩)`/`heeft(ocI(S),m)`, and the back-link doubled
in both directions (`rol(S,new_role,ocI(S))` and
`rol(ocI(S),subj_role,S)`). A **characteristic initialisation** deposits
`heeft(ocI(S),m)`: the two boolean constants are the v0 form — `waar`
an unconditional head, `onwaar` no head at all — and the WF-027
relaxation (besluit 1) admits a boolean-typed **expression**, which
*guards* the head on the SUBJECT (`heeft(ocI(S),m) :- ⟨body⟩,
⟨bool atoms⟩`): a boolean is a characteristic in v0, so an own-subject
boolean-attribute read is `heeft(S,a)` — present iff true, absent iff
false, so the deposit fires exactly on true and the mark stays unset on
false (closed-world, ref's reading; `translate_case` carries a boolean
input attribute the same way — `heeft(label,a)` when true, absence when
false). The 1:1 input-snapshot narrowing guards
the creating body with `not inputrol(S,new_role,_)` — creation reads
the *input* world, not the growing one (ch. 5.5). Recursive creation
is budgeted: `depth(ocI(S),D+1) :- ⟨body⟩, depth(S,D), D <
oc_budget.` with `viol("generation budget exhausted",X) :-
depth(X,oc_budget).` — the runaway chain is UNSAT with its reason,
exactly where ref's pass budget faults. The back-link pair
resolution reuses `Eval.connecting_pair`; an ambiguous back-link
faults in both executors (`corpus/rood/ambiguous-backlink`).

**Fact creation** (ch. 3.4): the symbolic form walks its join chain
— one positive `rol/3` atom per step — and asserts the new filling
in both directions. The to-one guard is a `viol/2` witness when a
second filler appears on a to-one role
(`corpus/rood/to-one-second-filler`). Bean-field-only fact creations
fault: the symbolic form is what executes.

Both creation forms sit under S1's validity gate: an excluded
creation does not apply — nothing is minted, no link lands, no
budget or to-one guard fires.

Witnesses: `jutterswet-penninguitgifte`, `jutterswet-wachtersketen`,
`synth2-budgetgrens-onder` (creation under the budget);
`jutterswet-erflijn`, `jutterswet-gemeenteberging`,
`jutterswet-postwacht` (fact creation); red:
`ambiguous-backlink`, `to-one-second-filler`,
`finite-chain-over-budget`, `runaway-recursion`.

### S12. Allocation and uniqueness

**Proportional allocation** (verdeling naar rato, ch. 4.2): the
recurrence over the group translates as an aggregate cascade per
allocation I — weights `vdI_gewicht(X,W)`, the weight sum
`vdI_wsum(B,S) :- ..., S = #sum {W,X: ...}, S != 0`, raw shares
`vdI_raw(X,(A*W)/S)` (integer division on non-negative operands is
exactly the declared floor), the cap split (share = min(raw, cap)
encoded as the R≤C / R>C / uncapped cases), and the remainder
`val(B,ra,A−T)` with T the share sum. The §9.5 channel carries the
ch. 4.4 holes: a weightless recipient, a negative or absent cap —
keyed by the allocation's name, per subject. `viol/2` fences:
negative pot, negative weight, empty pot, and the 46340 bounds on
weight, pot and recipient count — plus, when a remainder attribute
is declared, on each share the remainder sums (the weight sum, the
pot×weight product and the share sum then provably stay inside
2^31; without a remainder the shares are bounded by construction). Only the proportional method translates; conditional
allocation and misaligned scales fault (S19). Case input on the
allocation's target attribute is refused — the ch. 6d
superset-model fence.

**Uniqueness** (ch. 5.9): a collision is a pairwise join —
`inconsistent("name",S1) :- is_a(S1,ot), is_a(S2,ot), S1 != S2,
⟨key atoms S1⟩, ⟨key atoms S2⟩, ⟨scope⟩.` — with scalar keys read
via `val/3`, at most one temporal key component read *ever* via
`tl(L,k,_,_,K)`, and the WF-054 scope hop as a shared `rol/3`
parent. The **union** shares one key predicate `uuI_key(S,J,⟨K…⟩)`
across its sources (positions harmonised to the max scale, each
upscale fenced), colliding by `2 <= #count { O,J : uuI_key(O,J,⟨V…⟩)
}` — the source index J rides along because v0 sums per-source
counts. An ever-unique key served by a bucket scalar faults; so do
two temporal components (the same-moment tuple join has no corpus
case).

Witnesses: allocation by `jutterswet-bergingstoezicht`,
`jutterswet-berging`; uniqueness by `jutterswet-kustregister`,
`jutterswet-strandregister`, `jutterswet-postwacht`,
`jutterswet-vondstnummer`; the union by `jutterswet-nummerregister`
(per-subject collision pins); red: `uni-temporal-key-scalar`,
`uni-two-temporal-keys`, `uu-temporal-key-scalar`,
`uu-two-temporal-keys`.

### S13. The §9.5 channel

`inconsistent/2` facts are model citizens, never fatal (the tracked
Catala deviation of `CONSTRUCTS.md` row 8): "consistent" in a case
expectation means the model carries *no* `inconsistent/2` atom, and a
pinned inconsistency names its producer. The key is always the
producing form's name, version-stripped at match time by both
executors — the case format's rule pin reads both identically:

| producer | key |
|---|---|
| division: empty divisor under filled dividend | the enclosing rule's name |
| consistency assertion failure | the consistency's name |
| uniqueness / uniqueness-union collision | the uniqueness form's name |
| ladder conflict (equal-priority disagreement) | `onderwerp.target` |
| allocation: weightless recipient, cap holes | the allocation's name |

A channel head embedded in a rule's *value expression* — the
division row — threads the enclosing rule's condition atoms (S3): a
subject the condition excludes deposits nothing. The standalone forms
(consistencies, uniqueness, ladder conflicts, allocations) have no
enclosing voorwaarde to thread; their own bodies are the guard. The
loader
validates a pinned rule name against the closed set of channel-capable
names and rejects unknown pins with a candidate list (ch. 5.6). Gross
"inconsistent"-without-a-rule expectations are gone from the corpus:
collisions pin per subject (`jutterswet-nummerregister` precedent).

### S14. Sum types and total match

A `SumType` value (RUNTIME-SEMANTICS ch. 9, CONSTRUCTS row 36) is a
**ground term**: a nullary constructor is the bare normalised functor
(`afgewezen`), a unary one is `functor(payload)`. The payload is scaled
by the constructor's **own declared payload datatype** — never by any
target attribute (`ctor_value_term` reads the payload's decimals from
the `SumType` declaration; a money payload's cents come from the
payload, not the write site). The functor is an ASP *symbol*, not a
string: the constructor name must normalise (`Ir_v0.Checks.norm`
lowercases and collapses whitespace) to a valid symbolic constant
`[a-z][a-z0-9_]*`, or the emitted program would be syntactically broken
unfenced — `ctor_functor` faults on a name that does not, the quote
fence's kin for the sum world (WF-060 admits the declaration; this is
the ASP-side backstop). `translate_case` emits a sum-typed input
attribute as `val(label, attr, term)`.

**Total match** (`Match`, ch. 9.2) lowers to **one rule per arm** into a
`matchN(S,Result)` helper — the scrutinee's `val/3` atom carries the
constructor **pattern**, so the functor itself selects the arm:

```
matchN(S,⟨arm term⟩) :- is_a(S,ot), val(S,scrut,ctor_k(P…)), ⟨arm body⟩.
```

**No negation-as-failure** — WF-063 makes the arms cover the declared
constructors exactly, so the arms are mutually exclusive by their
functors and the stratification posture is untouched (the one-stable-
model invariant holds). A unary arm's pattern binds a payload variable
`P`; that variable rides both the binder environment (so a `BinderRef`
in the arm body resolves to it, at the payload's declared scale) and the
rule guard (so any `viol/2` the arm body emits carries the scrutinee
atom that binds `P` — else `P` would be an unsafe variable in that guard
rule). **A leeg scrutinee** binds no `val/3` atom: no arm rule fires,
`matchN` stays underived, the target is leeg — propagation for free (the
S5 point-1 pattern; ch. 9.2's leeg propagation, no implicit leeg-arm).
The anchor's rung — `uitkering` := match on `beschikking`:
`Toegekend(bedrag)` → `bedrag` + parameter `vaste voet`, `Afgewezen` →
0.00, `Aangehouden` → 12.00 — emits three `match1` rules; the payload
`P` and the parameter share a scale (both 2 decimals), so the addition
reuses the ordinary additive machinery (S6).

**Sum equality** (ch. 9.3) is **positive ground-term unification**, never
the NAF complement. Both operand terms bind — an own-subject attribute
read via its `val/3` atom, a `Construct` as a bare ground term — and:

- `Gelijk`: `⟨T1⟩ == ⟨T2⟩` beside the two binding atoms;
- `Ongelijk`: `⟨T1⟩ != ⟨T2⟩` — **both-bound-and-different**, positively,
  **never `not gelijk`**.

The reason is Tabel 38's non-numeric branch: a leeg operand makes **both**
`=` and `≠` false, so the two are *not* complements over leeg. An
attribute read that is leeg does not bind its `val/3` atom, so the
positive join fails in both directions alike — leeg-correct for free. The
NAF complement would instead make `Ongelijk` **true** on leeg (the S5
non-complement warning, honoured here rather than merely cited). The
anchor's two characteristic rules are exactly these forms —
`heeft(S,bevoorschot) :- …, val(S,beschikking,V), V == aangehouden.`
and `heeft(S,afgehandeld) :- …, val(S,beschikking,V), V != aangehouden.`

On the check side (`model.ml`), a constructor term is parsed
**recursively** (the earlier flat lexer could not parse a bare nullary
symbol); a sum-typed `attributeValue` expectation is built by the *same*
`ctor_value_term` emission machinery and parsed by the *same* term
parser, then compared structurally — one source of truth, no independent
scaling path to drift. A sum-typed target that stays leeg reports as
underived (the emptiness test sees any `val/3`, not only numeric ones).

Witnesses: the anchor `jutterswet-beschikkingsom` — case1 `Toegekend`
(the unary arm, payload + parameter → 1275.50), case2 `Afgewezen` (0.00)
and case3 `Aangehouden` (12.00) (the nullary arms and the two
characteristic rules), case4 leeg (the whole match and both equality
directions go leeg/false — the both-directions-false corner, pinned
absolutely); the WF-060..064 negatiefs pin what the verifier rejects
before translation.

### S15. Folds and function values

A **list value** (RUNTIME-SEMANTICS ch. 10, CONSTRUCTS row 10) is
reified as indexed facts keyed by a **list-id term** `L`: `lelem(L,I,V)`
carries the element `V` at its 1-based position `I`, and `lpres(L)`
marks the list *present*. The two predicates are separate on purpose —
a present-but-empty list carries `lpres` and no `lelem`, which is
exactly the value/absence distinction ch. 10.1 needs: `[]` is a value
(`lpres`, no elements), leeg is the absence of one (no `lpres`). Element
`V` is scaled by the **element** datatype (`element_scale_of`): a
numeric element by its own decimals, a `SumType` element as its ground
constructor term (S14's `ctor_value_term`). Order is load-bearing and
kept (`translate_case` emits `I` = 1, 2, … in list order). The list-id
term is `plist(name)` for a list parameter, `alist(Subject,attr)` for an
own list-typed attribute — the subject variable rides the id, so one set
of derivation rules serves every subject at once, and distinct subjects'
lists never collide.

**Fold** (`Fold`, ch. 10.2) is **index recursion** over `L`:

```
faN(L,0,⟨init⟩)  :- lpres(L).
faN(L,I,⟨f(A,V)⟩) :- faN(L,I-1,A), lelem(L,I,V), ⟨body⟩.   (one rule per branch)
frN(L,F)          :- lpres(L), N = #count { I : lelem(L,I,_) }, faN(L,N,F).
```

The base case fires on `lpres`, so an **empty list yields the init**; the
result reads the accumulator at the list length (`#count` over `lelem`);
an **absent list derives no `lpres`**, so nothing is reachable and the
target stays leeg — propagation for free. The recurrence's contiguous
`I-1`/`I` chain requires **contiguous indices**, which a parameter,
attribute, `MapList` (index-preserving) or `ListLiteral` source
provides; a `FilterList` leaves gaps, so a `Fold` over a `FilterList` has
no encoding and no corpus case (S19).

**MapList** and **FilterList** derive their own `lelem`/`lpres`, keyed by
a fresh functor over the inner list-id (`lmN(⟨inner⟩)`,
`lfN(⟨inner⟩)`), element-wise and flat:

```
lelem(lmN(In),I,⟨f(V)⟩) :- lelem(In,I,V), ⟨body⟩.   lpres(lmN(In)) :- lpres(In).
lelem(lfN(In),I,V) :- lelem(In,I,V), ⟨cond over V⟩.  lpres(lfN(In)) :- lpres(In).
```

`MapList`'s body is the fold-body machinery (below); `FilterList`'s
condition keeps the element under a **positive** builtin — a sum-typed
`Ongelijk` is `V != ⟨Construct⟩`, both-bound-and-different, never
`not gelijk` (the S14 non-complement rule; the element is always bound,
so Tabel 38's leeg corner cannot arise). **ListLength** (`ListLength`) is
`llN(L,C) :- lpres(L), C = #count { I : lelem(L,I,_) }` — an empty list
counts 0, an absent list derives no count (leeg); `#count` tolerates a
`FilterList`'s index gaps, so no re-indexing is needed there. When this
count is bound to a `Let` binder and read in a term position, the S18(a)
self-contained helper keeps `C` safe inside any coalesce or range fence
built from local atoms (B3).

**Closed bodies** (lambda and function bodies, WF-067/069) lower to a
list of *branches* (`body_branches`): guard atoms, a result term and its
scale.

- **IfThenElse** (ch. 10.2) splits into two branches — the `then` under
  the condition's positive builtin, the `else` under its exact
  complement (`cmp_sym`/`cmp_complement`). The condition reads only bound
  terms (binders, parameters, literals), so it is a **builtin comparison
  over harmonised scales** and the complement is exact — **no NAF over
  derived atoms**, the stratification posture untouched (the S7
  closed-body point, mirrored here).
- **Match on a bound term** (ch. 10.4): the scrutinee is a bound binder
  (a lambda/function parameter or a match payload), not an attribute, so
  each arm lowers to a **functor-unification builtin** — `V = ctor(P)`
  for a unary arm (binding the payload `P`), `V = ctor` for a nullary
  one — one branch per arm, no NAF (WF-063 totality). This is S14's total
  match on a term already bound rather than read from a `val/3` atom.
- **Invoke** (ch. 10.2) inlines the acyclic (WF-067) FunctionDef body at
  the call site, binding its parameters to the argument terms — the
  aanspraaktelling map is `Invoke aanspraakwaarde` over each element,
  whose body is exactly the bound-term match above.
- Any other closed term (`closed_term`) is a bound binder, a literal, a
  document parameter, a `+`/`-`/`×`/`percentage-of` combination of those, a
  **division** (bare or abs), a **power by a constant integer exponent**, a
  `Construct`, or a **`Let`**. **Every emitted arithmetic step carries a
  `viol/2` range fence** grounded by the recurrence body and witnessed by the
  list-id — the running accumulator is grounding-time arithmetic and would
  otherwise wrap silently past 2³¹ where ref's exact rationals do not (the
  S6/S8 fence discipline, on the fold interior). The arithmetic forms reuse the
  rule-level encodings verbatim (one encoding, never a second), only swapping
  the subject-keyed grounding for the recurrence body and the list-id witness,
  so every fence moves with them:
    - **`Maal`** is the scaled product `(tl*tr)` at scale `sl·sr` with the
      46340 product fence on each operand — the S6 multiplicative family
      without the leeg coalesce (a closed-body binder never goes leeg, the
      additive-family posture of `closed_term`). **`Percentage_van`** is the
      same product with the `/100` folded into the scale (`sl·sr·100`, no
      division) — the rule-level `(Maal | Percentage_van)` arm carried into the
      body (golden `jutterswet-kinderprocent`, `acc + (x% van grondslag)`,
      `[10,25]/200 → 70.00`).
    - **The money × rate product** rounded to the cent
      (`Afgerond{REKENKUNDIG,2}` over a 2-decimal money × 4-decimal rate,
      `money_mult_shape`) is the split encoding of S18(c) — the shared
      `money_mult_fences`/`money_mult_body`, with the four fences (two
      negative-operand, two range) keyed by the list-id and the `A`/`C`
      assignments emitted *inline* as body atoms over fresh variables rather
      than a subject-keyed helper predicate, so the fold recurrence binds the
      per-element index directly.
    - **A `Gedeeld_door` by a compile-time positive constant** is the
      gcd-reduced form of S18 (the shared `const_div_reduce`): the numerator
      range fence and the non-exact `viol/2` ride the list-id, and the value
      is guarded **exact** (`numt \ dent == 0`), so a non-exact quotient
      derives no value and its viol makes the program UNSAT — the remainder
      never truncates silently. The exactness test runs at the out-scale
      `max(s_L, s_R, write_scale)` — the same target the rule-level division
      uses — so it is the precision the fold WRITE stores (the fold's target
      attribute scale, carried into the body by `write_scale`), not merely the
      operand scales: a bare-integer divisor whose exact quotient needs more
      decimals than `max(s_L, s_R)` offers (e.g. `x / 4` into a 2-decimal
      attribute → `0.25`) is still exact against that stored scale. An integer
      dividend under the division is thereby widened to the shared out-scale
      (the `decimal of n` ToRat pass-through), needing no separate node. A
      **`Gedeeld_door_abs`** rides the same `closed_division` with
      `rounding = Some (Richting_nul, 5)`: the quotient truncated toward zero at
      the 5-decimal collapse point (ch. 2.2), which gringo's integer `/` computes
      on both signs, so no exactness guard and no negative fence — the
      round_z-scale-5 form carried whole into the body (golden
      `jutterswet-kinderkap`, `acc + (x /abs 3)`, `[10,25] → 11.66666`). A
      **runtime (bound-binder) divisor** for the bare division keeps the
      two-factor form and rides a numeric-zero `viol/2`; the empty-operand §9.5
      edges (empty dividend → 0, empty divisor → the channel) cannot arise on
      always-bound binders, so only the zero fault does (golden
      `jutterswet-kinderdeler`, `acc + (grondslag / x)`, `60/[2,3,4,5] → 77.00`,
      every quotient exact; red `closed-runtime-division-by-zero`, `[2,0]` → both
      *"division by numeric zero"*).
    - **`Tot_de_macht`** by a compile-time non-negative integer exponent unfolds
      to **repeated `Maal`** under the same 46340 fence — one factor per step, the
      running partial product bounded so it never wraps (a conservative
      `t^(e-1) ≤ 46340` bound, tighter than the rule-level base bound but always
      loud). The exponent reading (`const_pow_exponent`) is shared with the
      rule-level power; a runtime, negative or fractional exponent faults with its
      reason, ref-only behaviour (golden `jutterswet-kindermacht`, `acc + x^3`,
      `[2,3] → 35`).
    - A **`Let` inside the body** (ch. 10.6) inlines its value — a closed-body
      term — binding the binder to that term with **empty atoms** (the
      recurrence-bound convention, so a nested read is self-contained) and
      threading the value's binding atoms into every read site. No subject-keyed
      helper: that is the rule level's shape, where a leeg value must break the
      body, and a closed-body binder is never leeg (golden `jutterswet-kinderlet`,
      `acc + (let bijdrage = grondslag maal x in bijdrage)`, `[2,3]/5.00 →
      25.00`). A binder bound by a `Let` **outside** the closed body carries
      binding atoms `closed_term` would have to drop; that reading is refused with
      a clean fault (the sharp afbakening, S19), the parallel verifier card making
      the shape WF-064-ill-formed.
- A **`Construct` with a computed payload** inside a body (the reduce
  lowering's `Some(⟨body⟩)` step) wraps *each* payload branch in the
  constructor functor: `body_branches ⟨payload⟩` splits an `IfThenElse`
  payload into its two branches (the max/min reducer), each becoming a
  `present(⟨term⟩)`. The constructor value carries no numeric scale (0);
  its payload rides the declared payload scale, range-fenced like any
  arithmetic step.

**The reduce lowering** (`sum`/`maximum`/`minimum`, RUNTIME-SEMANTICS
ch. 10.4/§3.5) folds over an **`Optional` accumulator** (a `SumType`,
scale 0 by `value_scale`): the init is `absent`, and the step is a
`Match` on the accumulator binder rebuilding `present(…)` — so
`faN(L,I,present(V)) :- …, A = absent` (the first element) and
`faN(L,I,present(⟨f(P,V)⟩)) :- …, A = present(P)` (the combine, one rule
per reducer branch). The wrapping extraction `Match` (Absent → the
empty-list default, Present b → b) runs over the **fold result** rather
than an attribute: `translate_match` binds the `frN(L,V)` term once and
each arm is a builtin `V = absent` / `V = present(P)` — the same
functor-unification as a bound-term match, over a term produced by a
`Fold` rather than read from a `val/3` atom.

On the check side, `model.ml` reads the fold family's scalar results as
ordinary `val/3` — the reified lists are internal (`#show` carries only
`val`/`heeft`/`inconsistent`/`tl`), so a numeric fold result and a
`ListLength` count are plain numeric expectations, and a leeg target
(absent list) reports underived through the same `val_present` path. A
**list-valued** `attributeValue` expectation has no corpus case and
faults loudly (S19) rather than growing a half-built list comparison.

Witnesses: `anchor/jutterswet-staffelvergoeding` — the capped-sum
`Fold` over the list *parameter* `staffelbedragen` with an `IfThenElse`
body (case1 `[12.50, 30.00, 7.25]` → **19.75**, the left-to-right order
load-bearing; case2 `[]` → **0.00**, the empty list a value; case3 the
parameter absent → leeg, a no-op survivor). `anchor/jutterswet-aanspraaktelling`
— `erkende som` := `Fold(+, 0.00)` over `MapList(aanspraken, Invoke
aanspraakwaarde)` and `aantal erkend` := `ListLength(FilterList(aanspraken,
a ≠ Vervallen))`, composing S14 (case1 `[Erkend(10.00), Vervallen,
Erkend(2.50)]` → **12.50** and **2**; case2 `[]` → **0.00** and **0**;
case3 the list attribute absent → both leeg). `jutterswet-penningstaffel-catala`
(the plugin-generated golden) — `hoogste` := `maximum … or if list empty
then $0.00`, the reduce lowering end to end (the `Optional` fold, the
`IfThenElse` reducer, the extraction `Match` over the fold result: case1
`[12.50, 30.00, 7.25]` → **30.00**, case2 `[]` → **0.00** via the Absent
arm, case3 a tie `[8.00, 25.50, 25.50, 3.00]` → **25.50**), beside
`totaal` as a plain numeric `Fold`. The WF-065..070 negatiefs pin what
the verifier rejects before translation.

### S16. Structs (the record datatype)

A **record value** (RUNTIME-SEMANTICS ch. 11, CONSTRUCTS the Phase-5c
row) is a **ground term** whose functor is the **normalised declaring
domain** (`ctor_functor`, the sum-world fence reused) and whose
arguments are the declared fields in **fixed declaration order**, each
scaled by its **own declared field datatype** — the struct
`dossier {waarde: 2-dec, telling: 0-dec}` value `{120.50, 3}` is
`dossier(12050,3)`. The term itself carries no numeric scale
(`value_scale`/`element_scale_of` map a struct to 0, the `SumType`
sibling); each *field* rides its own decimals. A struct always declares
at least one field (WF-071), so the term is never the bare functor —
no collision with a nullary constructor of the same domain name.
`translate_case` emits a struct-typed input attribute as
`val(label, attr, ⟨struct term⟩)` (`struct_value_term`), and a
struct-typed **list element** reifies through the same builder inside
`reify_list` (`lelem(alist(…),I,dossier(1000,1))`) — `list of dossier`
is depth 1, and the WF-071 acyclicity fence keeps any composition
statically bounded under clingo's term-depth abort.

**FieldAccess** (ch. 11.1) is **positional projection by unification**.
The base binds its record term; the declared field's 1-based position
carries a fresh variable and an anonymous variable fills the rest, so
the field is read by unifying the term against the domain functor:

```
val(S,dossier,B), B = dossier(W,_)      % read own attribute .waarde → W
```

Three base shapes land, all `struct_base`/`project_field`:

- an **own-subject struct attribute read** (the rule-expression route —
  `premie := dossier.waarde + opslag`) binds `B` via `val(S,dossier,B)`,
  then `B = dossier(W,_)`; a **leeg base** binds no `val/3` atom, so the
  projection is underivable — propagation for free;
- a **bound struct binder** (the closed-body route —
  `λ d. d.waarde` mapping over `list of dossier`) resolves to the ASP
  variable the enclosing `lelem` join bound, then `V = dossier(W,_)`
  binds the field — the **lelem route**: `MapList` over a struct-field
  projection lowers to
  `lelem(lmN(In),I,W) :- lelem(In,I,V), V = dossier(W,_)`, so the mapped
  list carries the projected field and an ordinary numeric `Fold`
  totals it (the anchor's `totaalwaarde`). The result scale is always
  the **field's** declared scale, never the base's;
- a **struct-typed field access** (the **struct-in-struct** route —
  `heffing := waardering.kern.grondslag`, a field access whose base is
  itself a struct-typed field access) resolves **recursively**:
  `struct_base` re-enters on the inner base, so each level of nesting
  adds **one** projection unification. The intermediate struct field is
  read at its own scale — `value_scale` maps a `StructType` to 0 — so
  the recursion carries **no** numeric rescaling until it reaches the
  leaf, where `project_field` returns the **leaf field's** declared
  scale. The two-level read `waardering.kern.grondslag`
  (`waardering{toeslag, kern{eenheden, grondslag}}`, `grondslag`
  a money field) lowers to
  `val(S,waardering,B), B = waardering(_,K), K = kernwaarde(_,G)` — `B`
  projects the outer term to the intermediate `K` (position 2, scale 0),
  then `K` projects to the leaf `G` (position 2, `grondslag`'s money
  scale). A **leeg anywhere in the chain** binds nothing, so the whole
  projection is underivable at every depth — propagation for free. The
  outer record is either computed (a nested `StructLiteral` write,
  `waardering(A1,kernwaarde(A2,A3))`) or an own struct attribute; the
  read grounds the same recursive way regardless.

**StructLiteral** in value position (`samenvatting := {waarde: premie,
telling: ListLength(dossiers)}`) builds the term from its **field
terms with their atoms** — the 5b computed-`Construct` pattern:
`struct_literal_term` translates each declared field in **declaration
order** (each field's static scale must equal its declared scale) and
wraps the results in the domain functor,
`val(S,samenvatting,dossier(A,L)) :- …, val(S,premie,A), llN(…,L)`. A
field evaluating **leeg** binds no atom, so the whole literal is
underivable and the target is leeg — **NO-HOLE** propagation, the
`Construct`/`ListLiteral` sibling (a record value never carries a leeg
field). A case-supplied struct **computed** target has no monotone
encoding here (the sum-target sibling), fenced loudly (S19).

**Struct equality** (ch. 11.2) is **positive ground-term unification**,
the S14 sum pattern exactly — never the NAF complement:

- `Gelijk`: `⟨T1⟩ == ⟨T2⟩` beside the two binding atoms;
- `Ongelijk`: `⟨T1⟩ != ⟨T2⟩` — both-bound-and-different, positively,
  **never `not gelijk`**.

Both operands bind their record term — an own attribute read via its
`val/3` atom, a `StructLiteral` as its computed/ground term
(`struct_term`). **Nominal identity is carried by the functor itself**
(= the declaring domain), so two operands compare only when they name
the same domain; WF-072 precludes a cross-domain comparison statically.
**Structural equality is the term equality** clingo's `==`/`!=` compute
over the fixed-order arguments. A **leeg operand** binds no term, so the
positive join fails in **both** directions — Tabel 38's non-numeric
branch (leeg makes `=` *and* `≠` false), the same both-false corner the
sum world honours (never the NAF complement, which would make `Ongelijk`
true on leeg). The anchor's `dubbel` / `afwijkend` rules are exactly
these forms — `heeft(S,dubbel) :- …, val(S,dossier,B), B == dossier(12050,3).`
and `heeft(S,afwijkend) :- …, val(S,dossier,B), B != dossier(12050,3).`

The **gevuld gate** (`Predicaat` soort `Gevuld`, ref: value ≠ leeg) is the
operand's binding atoms as a **positive join**: a struct operand rides
`struct_term`'s `val/3` atom, any other rides `expr_term`; either way an
absent value leaves the atoms unbound, so the gated rule does not fire —
leeg-on-absence for free (the anchor's `premie` rule reads
`GEVULD(dossier)`).

On the check side, `model.ml` builds a record-valued `attributeValue`
expectation by the **same** `struct_value_term` emission (each field
scaled by its declared field datatype, declaration order) and parses it
with the **same** recursive term parser, then compares structurally —
one source of truth, no independent scaling path to drift (the
case1 `dossier` input pin exercises exactly this — the
expectation-arm witness). A struct target that stays leeg reports
underived through the same `val_present` path.

Witnesses: the anchor `anchor/jutterswet-vondstdossier` — **case1
gevuld** exercises every route at once: `premie` (FieldAccess over the
own struct attribute, plus a parameter → 125.50), `totaalwaarde`
(the `MapList`-over-struct-field `Fold` — the lelem route — over
`[{10.00,1},{2.50,2}]` → 12.50), `samenvatting` (the computed
`StructLiteral` → `{125.50, 2}`), `dubbel` present (struct `==`),
`afwijkend` absent (struct `!=`), and the `dossier` **input** itself
pinned as `{120.50, 3}` (the recursive-term-parser scaling); **case2
leeg-lijst** (`dossiers = []` → `totaalwaarde` 0.00 by the fold base,
`samenvatting` `{125.50, 0}`); **case3 afwezig** (both struct inputs
absent → `premie`/`totaalwaarde`/`samenvatting` all leeg — the
build-side no-hole witness: a leeg `premie` propagates through the whole
`StructLiteral` — and `dubbel` **and** `afwijkend` both absent — the
Tabel-38 both-false corner — a no-op survivor). The WF-071..074
negatiefs pin what the verifier rejects before translation.

The **struct-in-struct recursion** rides its own golden,
`jutterswet-vangstwaardering` — a `Vangst` computes the nested record
`waardering{toeslag, kern{eenheden, grondslag}}` from its scalars and
reads the deepest **money** field back: **case1 gevuld**
(`heffing := waardering.kern.grondslag + waardering.toeslag` → 125.75,
the additive combination of the two-level read and the one-level read;
`grondslagkopie := waardering.kern.grondslag` → 100.25, the bare
two-level read — the scale-per-datatype witness, `grondslag` at money
scale behind the integer `eenheden`); **case2 leeg-grondslag**
(`grondbedrag` absent → the nested `StructLiteral` is leeg by no-hole →
`waardering` leeg → `heffing` **0.00** by the ch. 1.3 additive
coalesce, `grondslagkopie` **leeg** by bare-read propagation — the two
leeg corners of the recursion pinned against ref, ref ≡ clingo). The
outer record here is **computed**, so no nested record **value** is
emitted.

The **recursive struct value** — a nested record as a case **input** or
**expectation** — rides `jutterswet-waarderingsinvoer`: a `Vangst` whose
`waardering{toeslag, kern{eenheden, grondslag}}` is a direct **input**
(not computed), so the nested record enters through `struct_value_term`,
which **recurses** — one nested functor per level, each field at its own
declared scale — emitting
`val(V,waardering,waardering(2550,kernwaarde(3,10025)))` (the value-side
mirror of the `struct_base` field-access recursion above; the WF-071
acyclicity fence bounds the depth statically). The same recursive emission
and the model's recursive term parser build and compare the struct-valued
`attributeValue` **expectation** — one source of truth, no drift.
**case1 genest** echoes `waardering` itself, reads the deepest field back
(`grondslagkopie := waardering.kern.grondslag` → 100.25, the `struct_base`
recursion over the input record) and sums the two-level and one-level
reads (`heffing := waardering.kern.grondslag + waardering.toeslag` →
125.75) — the value shape and the field-access shape witnessed together,
ref ≡ clingo. A nested **sum-** or **list-**typed field value still has no
flat-term encoding (S19).

### S17. Nested exception groups

The flat ladder of S10 is the unary case of a rose tree (RS ch. 7.9, the
ratified option i). A rung's `excepties` sub-group generalises the
`decides_m` lowering recursively; the translation is a real extension of
S10, not an application of it.

**Chains and groups.** A *chain* — a rung list, either a top-level member
or a sub-ladder — lowers to two predicates: an outcome `T_out(S,V)` (the
highest *settling* rung's decision: the 0/1 besluit on a characteristic
ladder, the scaled consequence term on an attribute ladder) and a
conflict flag `T_cfl(S)`. A *group* — a rung's sub-group, or the root
member set — reconciles its member chains exactly as S10's root group
reconciles:

```
gU_diff(S) :- mI_out(S,X), mJ_out(S,Y), X != Y, not mK_cfl(S)…   % I<J, every K
gU_cfl(S)  :- mK_cfl(S).                                          % conflict-first
gU_cfl(S)  :- gU_diff(S).
gU_out(S,V) :- mK_out(S,V), not gU_cfl(S).                        % clean decision
gU_any(S)  :- gU_out(S,_).   gU_any(S) :- gU_cfl(S).
inconsistent("onderwerp.target/⟨path⟩",S) :- gU_diff(S).
```

Equal decisions collapse to one `gU_out` atom (idempotence — the ch. 5.1
reading, RS ch. 7.2); differing ones raise `gU_diff` and are suppressed.
A member conflict propagates *before* differing decisions are read
(`not mK_cfl` — the conflict-first ordering of ch. 7.9), so a deeper key
wins over a shallower one.

**The rung.** A rung carrying `excepties` reads its group first:

```
tN_dec(S,V) :- gU_out(S,V).                                        % adopt the sub decision
tN_conf(S)  :- gU_cfl(S).                                          % propagate the sub conflict
tN_dec(S,c) :- ⟨own justification⟩, ⟨own bindings⟩, not gU_any(S). % form-a fall-back only
```

A *group-rung* (no own besluit/consequence — the multi-member base group
of RS ch. 7.9, e.g. art. 6's B1/B2 basisgrond) omits the third rule and
merely carries its group as a chain position. A rung *settles* the walk
when it decides or conflicts (`tN_stl :- tN_dec(_)`;
`tN_stl :- tN_conf`), and the chain's outcome takes the highest settling
rung, higher rungs displacing lower ones by `not tM_stl` — the S10
displacement, now carrying conflicts too, so a higher deciding rung
displaces a lower rung's sub-group conflict (vraag 5-A). The form-a
fall-back's `not gU_any` restricts the own justification/consequence to a
sub-group that decides *nothing*: a deciding or conflicting sub-group
takes precedence.

**Vraag 5-A — propagation to the root.** `T_cfl` climbs the tree to
`grpG_conflict`, which guards *every* write head and the effectiveness
atom `grpG_eff`: a differing sub-group therefore writes nothing on the
target and leaves any case input standing (S10's `inputval` path), while
the sole §9.5 fact is the `inconsistent` its *own* group deposits under a
**path key** — `onderwerp.target` plus the rung-index path
(`Jutter.heffing/0` for the sub-group of root rung 0, `Jutter.heffing/1`
for rung 1's base group). The root's own differing decisions keep the
bare key `Jutter.heffing` (S10). One conflict, one fact, one place —
uniform with the root reading and Catala's ⊛-propagation minus the
fatality we already made observable.

**Stratification.** Every negation points strictly *down* the tree
(`not gU_cfl`, `not gU_any`) or at strictly *higher* rungs of the same
chain (`not tM_stl` — disjoint subtrees). No conflict atom depends on the
write it guards, so the program stays stratified and clingo keeps its one
stable model — the soundness S10 has, extended by construction. The
`#defined tN_cfl/1` and `#defined gU_diff/1` declarations keep a
conflict-free chain or a single-member group free of undefined-atom
warnings.

Witnesses: `anchor/jutterswet-nestuitzondering` (the attribute arm,
art. 6) — case1-alleen-y is **the binding spurious-conflict test**
(flattening `{y,x}∥{z,x}` manufactures a root conflict; the nested
reading yields `10.00`, consistent — PHASE4-DEFAULTS §7), case2-alleen-z
the sole-exception route, case3-verschillend the propagated sub-group
conflict (`heffing` leeg + `Jutter.heffing/0`), case4-gelijk the
equal-decisions rule *at sub-group level*, case5-terugval the form-a
fall-back to `40.00`, case6-basis and case7-basisconflict the group-rung
base group and its `Jutter.heffing/1` conflict, case8-niets the
all-undecided no-op survivor; `anchor/jutterswet-nestkenmerk` (the
characteristic arm, art. 7) — case1-y-wint the sub-besluit `false`
displacing the root `true` (vrijgesteld absent, consistent),
case2-conflict the `Jutter.vrijgesteld/0` key, case3-terugval the
empty-sub-group fall-back to `true`. The seven nested-group negatiefs
pin what the verifier rejects before translation ever runs:
`wf050-half-marked-group`, `wf050-subrung-wrong-shape`,
`wf075-contentless-rung`, `wf075-empty-leden`,
`wf075-grouprung-with-justification`, `wf075-init-in-subrung`,
`wf075-two-members-unmarked`.

### S18. The strict `Let`, guarded consequences, and the split money multiplication

Three capabilities land here (design `PHASE5C-TUTORIAL.md` §5–6, the
ratified vragen 2-A and 4-A), all sharing one discipline: **the strict
`Let` is the leeg barrier, and an underivable value atom simply never
binds** — so ref's leeg propagation and clingo's underivability coincide
*by construction*, the S5 lesson carried into the value position.

**(a) `Let` as a self-contained bound intermediate.** A
`Let { binder; value; body }` translates the `value` to
`(atoms, term, scale)` and binds the binder in the binder environment
(the S14/S15 regime, WF-064). The value's atoms also thread into the rule
guard, so a leeg value (an underivable read) leaves every rule carrying
the body underivable — exactly ref's strict `Let`, where a leeg value
propagates and the body never runs (RUNTIME-SEMANTICS ch. 10.6). But the
rule guard alone does not reach every position the binder is read: a
coalesce (S5), a range fence (S6) and the money-product partials build
their helper rules from the operand's *local* atoms only, so a binder
whose value is a bare term — a `ListLength` count, a `Fold` result, an
attribute read — and whose sole binding lived in the guard grounds
**unsafe** inside one such helper (`co7(S,(L2+C6)) :- …, is_a(S,"jutter")`
with the count variable `L2` unbound — the B3 defect). The binder is
therefore made **self-contained**: a value carrying atoms is bound
through a per-subject helper `letN(S,term) :- is_a(S,ot), ⟨value atoms⟩`
(for a `ListLength` value that is the `#count` aggregate over the S15
`lpres`/`lelem` reification), and the binder read carries the single
`letN` atom — so wherever it lands, in a helper body or the enclosing
rule, the value is bound before use. Because the helper is still a
*positive* join over the value's atoms, the leeg-propagation above is
unchanged. An atom-free value (a literal) is already ground and needs no
helper. The binder, once bound, is **never leeg** — the load-bearing
point that keeps the branch complement (below) honest. `expr_term`,
`value_branches` and the money-multiplication peel bind `Let`s the same
way; the anchor's `aanslag` rule threads three (`ink`, `aftrek`,
`belasting`), and `jutterswet-postenheffing` pins a `ListLength`-valued
binder read inside a product (`heffing = 100.00 · (aantal_posten +
number of posten)`, 600.00 / 0.00, ref ≡ clingo).

**(b) Guarded consequences.** A rule value now reaches an `IfThenElse`
through `Let` bodies (WF-069 widened). At value position it lowers to
**one guarded outcome rule per branch** (`value_branches`): the shared
`Let` value atoms sit in *both* branch bodies, the condition is a builtin
comparison over the branches' bound terms, and the else-branch carries
its *exact complement* (`cmp_complement`) — no NAF over derived atoms.
WF-077 is what makes this sound: a value-position `IfThenElse` condition
reads only binders, parameters and literals, never a bare attribute read,
so its operands are total (never leeg) and the positive/complement pair
partitions cleanly (the S7 closed-body argument, now at rule level). Each
branch emits its own `val(S,attr,·)` head; on a standing target (S10)
each branch also emits the effectiveness atom — the rule decides if *any*
branch fires — and the input-stands rule is emitted once.

**(c) Decimals and the split money multiplication.** Catala decimals are
exact rationals with a **pinned finite scale S = 4** (vraag 4-A): a
4-decimal declaration scales by 10^4, ref stays exact-rational, and a
value that does not terminate at four decimals faults loudly and
*consistently* in both executors — ref's bounded-write gate and clingo's
does-not-fit / non-exact-division fence (the red witness
`corpus/rood/decimal-nonterminating`, `1,0000 / 3,0000` at four decimals:
ref's *"does not fit the declared 4 decimals"* and clingo's *"non-exact
division (an explicit rounding is mandated before precision is lost)"*).
The rational operators and the `ToRat` casts ride the existing numeric
family unchanged (a cast is a pass-through re-typing — the value is
already exact-rational).

The one new arithmetic encoding is the money product Catala rounds to the
cent (`Mult_mon_rat`; the plugin wraps it in `Afgerond {REKENKUNDIG, 2}`).
The naive scale-6 product **wraps clingo's machine integers at tutorial
magnitude** — €20.000 × 21% is 2000000 × 2100 = 4.2·10⁹ > 2³¹ — so a
2-decimal money `M` times a 4-decimal rate `R` is encoded **split**, the
rate in two scale-2 halves `Rh = R/100`, `Rl = R\100`:

    M·R/10⁴  =  M·Rh/10²  +  M·Rl/10⁴

The rounded cent is `C = qA + (100·rA + M·Rl + 5000)/10⁴` with `A = M·Rh`,
`qA = A/100`, `rA = A\100` — demonstrably `round(M·R/10⁴)` half-up (ref's
`round_z Rekenkundig`) for non-negative operands, each partial product
fenced by a `viol/2` witness so no term wraps silently: `A = M·Rh` by
`|M| ≤ max_int31/(Rh+1)` (Rh ≥ 0, so the +1 divisor never faults on
zero), and `M·Rl` (with `Rl < 100`) by `|M| ≤ (max_int31−20000)/99`,
keeping the rounded numerator `100·rA + M·Rl + 5000` inside 2³¹. The
operands coalesce leeg → 0 exactly as ref's `Arith.mul` (a leeg rate
makes the product 0, not leeg); the strict-`Let` barrier that makes a
leeg *money* leeg lives one level up, in the peeled value atoms, never in
these direct operands. Strict `Let`s around the product are peeled with
their binders in scope. Negative operands are not encoded (ref rounds
either sign; the tutorial's money and rate are ≥ 0) — a `viol/2` fence,
S19.

Witnesses: `anchor/jutterswet-tariefstelling` — **case1-elsetak** the
else branch (`aanslag` 3700.00 from belasting 4200.00 − aftrek 500.00,
the split money product €20.000 × 21% = €4.200 exact), **case2-dantak**
the then branch (aftrek 750.00 > belasting 210.00 → `aanslag` 0.00),
**case3-leegbarriere** the double leeg barrier (inkomen absent → both
`aanslag` and `gewogen` leeg through the strict `Let`, while `factor`
2.0000 still writes — the case that pins *why* the discipline exists:
without the `Let`, the bare `maal` would coalesce a leeg inkomen to 0.00,
ch. 1.2a). Gate 6 replays all three (ref ≡ clingo); gate 8 the red
witness.

**A computed-rate magnitude limit (the tutorial fixture).** When the
4-decimal rate is itself computed by an exact division — the Catala
tutorial's `1.0 + decimal of number_of_children / 2.0`
(`frontends/catala/test/tutorial_end_2_3.catala_en`) — the exact scaled
quotient of `L / R` at out-scale `m` is `L·(s_R·m) / (R·s_L)` (`s_L`,
`s_R` the operand scales). At `S = 4` the numerator is pre-scaled by the
divisor's own scale `s_R = 10⁴`, so a 4-decimal divisor rides a
scale-`s_R·m = 10⁸` intermediate — `n·10⁸` for `n / 2.0000` — which clears
clingo's int31 grounding range only for `n ≤ 5`.

The divisor here is a **compile-time constant** `c` (`2.0000` →
`c = 20000`), and that lets the whole *constant* fraction `(s_R·m)/(c·s_L)`
reduce by its **full gcd** before grounding, folding the divisor's scale
out of the numerator: with `k = gcd(s_R·m, c·s_L)` the encoding emits
`(n·(s_R·m/k)) / (c·s_L/k)` — for `n / 2.0000` that is `n·5000 / 1`, so the
scale-10⁸ intermediate never forms and the dividend fence loosens from
`≤ 5` to `≤ 107374`. **The fold is exact.** The unreduced and reduced
forms denote the *same rational* `n·(s_R·m)/(c·s_L)`; clingo's `/` is its
floor and `\ == 0` its integrality test, and floor and integrality are
invariants of the rational, not of the representation — so `⌊·⌋`, the
non-exact `viol/2`, and the zero-divisor fence are all **unchanged**; only
the range bound moves (the value 3.0000 for `n = 4`, the non-exact viol
for `1 / 3.0000`, every existing division outcome is bit-identical). This
is the sole change — the split product's own rounding proof above stands
untouched.

The folded denominator `c·s_L/k` is itself the **ground constant** the
rounding formulas feed unguarded into `2·dent` and `2·numt+dent`, so the
fold applies only while it rides the same int31 fence a divisor *term*
would: past `536870912` (`2·dent` would leave 2³¹ and wrap **silently** —
a ref≠clingo split, and a ground guard on the already-wrapped literal
cannot fire). A larger constant divisor — one past `~10⁹` — therefore
drops back to the two-factor form, whose `(t_R, 536870912/kden)`
`range_guard` fences the divisor term and fires the loud `arithmetic range
exceeded` `viol/2` UNSAT, exactly as every constant divisor did before the
fold existed. No corpus case reaches it (the tutorial fold keeps
`c·s_L/k = 1`); ref computes such a quotient exactly, clingo cannot ground
it, so the fence is the honest verdict, never a silent value.

With the division no longer the binding constraint, that constraint is now
the split product's `A = M·Rh` partial (S18(c)): for the tutorial's
`$10,000 · (1 + n/2)` it carries `n ≤ 40` and fires the range `viol/2` at
`n ≥ 41`, where `M·Rh` genuinely exceeds 2³¹ — the fence stays loud for
exactly what does not fit, and ref computes every case exactly. Golden
`jutterswet-kinderweging` pins `n = 20` (well past the old 6) and `n = 6`
(the old boundary) through both executors, ref ≡ clingo. A **runtime**
(non-constant) 4-decimal divisor keeps the two-factor form and its
scale-10⁸ intermediate — no corpus case exercises one, so its dividend
fence stays at the conservative bound. The fixture's cases keep `n ≤ 5`
for their own overseas and computed-rate flanks, no longer a backend cap.

### S19. What does not translate — the complete loud inventory

The standing rule (PLAN §4): the core may say "I cannot express this
(yet)"; it may not pretend. Every fault below is a `Value.fault` with
context — the translation stops, nothing degrades. Grouped; the
literal reason strings live in `backends/clingo/lib/translate.ml`.

*Semantics deliberately unencoded (recorded findings):*
equality in the per-segment clip comparison (the clip's
empty corner is a different shape — non-numeric scalar equality
itself is since 2026-08-25 encoded, S5/S7, like the numeric
asymmetry before it); runtime, negative and fractional
power exponents (the runtime-exponent empty corner 4^leeg = leeg is
thereby ref-only behaviour); ref's unit-conversion algebra (all
restating positions refuse non-identical units); negative operands
under non-richting-nul rounded forms (runtime `viol/2` fences);
non-exact bare division and non-fitting write results (runtime
`viol/2` — an explicit rounding is mandated before precision is
lost). And one decided *future* delta, recorded rather than encoded:
numeric-zero division is today the v0-mirror hard fault in both
executors (`corpus/rood/division-by-numeric-zero`,
`division-abs-by-numeric-zero`); ch. 1.4 point 4 routes it into the
§9.5 channel in v1 — a tracked delta both executors will take
together.

*Constructs and shapes without a corpus case (fault at translation):*
validity windows on the forms OUTSIDE the ch. 8.2 gate — allocation,
consistency checks, uniqueness and the uniqueness union (`… validity
windows are not translated (no corpus case)`; before the gate landed
for rules, ladder members and creations these forms silently ignored
their window, so the fault is new and load-bearing: an unwindowed
translation would apply outside its window) — while windows on rules,
ladder members and both creation forms now translate (S1's validity
gate, `jutterswet-vensterheffing`), including the time-free refusal
both executors share; temporal targets
outside the ch. 3.7/3.8 fold shapes; the negated gedurende clip (the
complement walk — also as a composite member; ref carries both, the
standing executor asymmetry); the ten-minste-1 composite union (v0
clips to the union of member regions; loud in both executors); the
static-comparison composite member (v0 partitions it into a
value-side guard; loud in both executors) and the nested composite
bullet (v0 markers it as Onopgelost; loud in both executors);
gedurende condition shapes beyond WF-055; temporal
totals over case-supplied full timelines, and non-year total
granularity; `Begrensd` (clip/min-max) and rounded roots in term
positions; a temporal attribute read in a scalar slot (ch. 3.3);
non-numeric arithmetic operands (ref faults date arithmetic — day
numbers must not silently compute) and a **text/enum/boolean literal in
a NUMERIC term position** (a **date literal** translates as its civil
day number — B2, the S4 date-comparison sibling — and since
2026-08-25 the text/enum/boolean literals and own-subject reads
translate in their OWN positions: equality operands and text/enum
write values, through `symbolic_term`, S2/S3/S5/S7 — so what faults
here is those datatypes reaching `expr_term`'s integer encoding:
arithmetic, ordering comparisons, the gevuld-predicate operand and
every other numeric slot); the symbolic-term fences of S2/S3: a
text/enum/boolean operand that is neither an own-subject attribute
read nor a literal (navigated reads await a case), temporal
text/enum/boolean carriage (a recorded flank, the sum/struct
sibling), a case-supplied text/enum *computed* target (the ch. 6d
superset-model fence), and a **boolean computed target** (the
closed-world `heeft/2` read and a derived `val/3` would split — S3);
unscalable datatypes in the integer encoding (a
datatype-less *declaration* is since 2026-08-21 rejected by WF-059
with the shared loader refusing its input as backstop — the fault
here remains the last line for **dateTime** operand positions,
whose millisecond values exceed clingo's integer range in any slot,
and for text/enum/boolean **parameters**, which ref executes but the
ASP parameter emission scales through `decimals_of_datatype` —
ref-only until a corpus case demands a symbolic parameter carriage);
characteristic attribution with a non-waar value;
non-proportional and conditional allocation; misaligned allocation
and rounding scales; som fallbacks other than 0; uniqueness key
lists with more than one temporal component, and ever-unique keys
served by a scalar; case input on non-standing computed targets
(scalar, temporal, and allocation targets alike — the ch. 6d
superset-model fence); to-many navigation in scalar slots (WF-053);
bean-field-only fact creations (the symbolic ch. 3.4 form is what
executes); the expression/condition catch-alls ("not translated — no
corpus case exercises it") — narrowed 2026-08-25: the *scalar*
counting composites (GEEN / TEN MINSTE / TEN HOOGSTE / PRECIES, the
decision-table desugar's disjunctive condition kinds among them) now
translate through the cqN count encoding (S7:
`jutterswet-rubriektabel`, `jutterswet-vrijgavetoets`,
`jutterswet-sluimerwacht`, `jutterswet-tweevoudkeur`) and left this
catch-all; what remains under it for composites is the member-less
counting list (`a counting composite without members is not
translated (no corpus case)`) and every member kind that keeps its
own fault (a Periode member — the vormenkabinet's peiling rule, still
refused by both executors — Rollidmaatschap, an Onopgelost bullet, a
comparison carrying a periode, the negated gevuld predicate).

*Sum types (S14) — carriage and shapes without a corpus case:*
**temporal sum carriage** is a recorded flank — the value model admits a
sum-typed temporal attribute and the case format can write one, but no
case exercises it, so the ASP case emission (and any sum scrutinee or
operand over an own temporal attribute) faults with the literal reason
`temporal sum carriage is a recorded flank; no corpus case` rather than
silently omitting the constructor-valued segments (RUNTIME-SEMANTICS
ch. 9.4); a `Construct` payload other than a literal or an own-subject
attribute read (a bound own-attribute read in write position rides its
`val` atom, the anchor's `herbeschikking` rule
`val(S,herbeschikking,toegekend(V)) :- …, val(S,borg,V)`; a `Match` or
composite payload still has no corpus case); a sum-typed equality operand other than an own-subject
attribute read or a `Construct`, and a match scrutinee other than an
own-subject attribute read or a `Fold` result (the reduce extraction —
navigated sum reads await a case); a
match arm whose result scale disagrees with the match's declared scale;
a constructor name that does not normalise to a valid ASP functor symbol
(`ctor_functor`, the sum-side quote backstop). A **sum-typed parameter**
is ref-only: ref accepts and executes it, but the ASP case emission
scales every parameter through `decimals_of_datatype` and so faults on a
`SumType` datatype with the literal reason `unscalable datatype in the
ASP translation` (a constructor value has no integer encoding). Two
payload **values** WF-060 admits but the ASP value emission does not yet
carry uniformly: a **nested-sum payload** (a declared-`SumType` payload)
faults the same way — `ctor_value_term` scales the payload through
`decimals_of_datatype`, which yields no number for a constructor value
(`unscalable datatype in the ASP translation`); a **day-precision date
payload** does translate — `ctor_value_term` emits `functor(dayNumber)`
at scale 0 (the day integer) — so there the gap is only that no corpus
case exercises it. Nullary-with-argument and
unary-without-argument constructs, an alien or missing arm, a binder on a
nullary arm and its converse are rejected by WF-062..064 before
translation is reached.

*Folds and function values (S15) — shapes without a corpus case:* a
**`ListLiteral`** list source (the reified-list encoding covers it, but
no corpus case builds a list in an expression rather than the case —
`list_source` faults); a **`Fold` over a `FilterList`** — the index
recurrence needs contiguous positions and a filter leaves gaps, so the
`Fold` translation walks its source chain (through index-preserving
`MapList`s) and faults loudly with `a Fold over a filtered list is not
translated (index gaps need re-indexing; no corpus case)` rather than
silently deriving nothing; `ListLength` over a filter stays fine, it
counts; an **IfThenElse
condition** other than a comparison over bound terms, a **closed-body
term** other than a bound binder / literal / parameter /
`+`,`-`,`×`,`percentage-of` combination / division (bare or abs) / money×rate
split / power by a constant integer exponent / `Construct` / `Let`,
and a **`FilterList` condition** other than a
sum-typed (in)equality over the bound element (the closed-body grammar is
narrowed to what the corpus exercises); a **list-valued
`attributeValue` expectation** in `model.ml` (the anchors expect only
scalars and leegte; a list comparison is not built until a case wants
one). The **closed-body arithmetic forms now translate** (S15, goldens
`jutterswet-kinderfoldheffing` and the `jutterswet-kinder*` rest-form
witnesses; reds `closed-nonexact-division`, `closed-runtime-division-by-zero`),
each reusing its rule-level encoding and every fence in the fold interior:
`Maal` and `Percentage_van`, the money×rate split rounded to the cent, a
division by a compile-time positive constant **or a runtime (bound-binder)
divisor** (the latter with a numeric-zero fence, the empty-operand §9.5 edges
gone because a closed-body binder is never leeg), `Gedeeld_door_abs` (the
richting-nul truncation at the 5-decimal collapse point), `Tot_de_macht` by a
constant integer exponent (repeated `Maal`), and a `Let` **inside** the body
(its value inlined, the binder self-contained). The one closed-body `Let`
reading still refused is a binder bound **outside** the body: `closed_term`
would drop its binding atoms into an unsafe rule, so it faults cleanly (`the
closed body reads binder "…" bound by a Let OUTSIDE it …`) — ref faults there
too (`unbound binder`), and the parallel verifier card makes the shape
WF-064-ill-formed (`backlog/new/20260825-closed-body-enclosing-binder.md`);
until it merges the fence is the clean backstop in place of gringo's grounding
crash. A **named FunctionDef
as a `Fold`/`MapList` operand** is no longer a shape-without-a-case: it is
**WF-069-rejected** before translation (the operand MUST be an inline
`Lambda`; a named function reaches a fold through a lambda body that
`Invoke`s it — `wf069-named-function-operand`), so the clingo fault
(`a named FunctionDef as a fold operand is refused (WF-069: the operand
must be an inline lambda)`) survives only as an unreachable backstop on
WF-clean input.

*Structs (S16) — carriage and shapes without a corpus case:*
**temporal struct carriage** is a recorded flank — the value model admits
a struct-typed temporal attribute and the case format can write one, but
no case exercises it, so the ASP case emission (and any struct field-access
base over an own temporal attribute) faults with the literal reason
`temporal struct carriage is a recorded flank; no corpus case` rather than
silently omitting the record-valued segments (the sum-carriage sibling).
A **struct-typed parameter** is ref-only, the sum-typed-parameter
sibling: ref accepts and executes it, but the ASP case emission scales
every scalar parameter through `decimals_of_datatype`, which yields no
number for a `StructType` datatype and so faults with the literal reason
`unscalable datatype in the ASP translation` (a record value has no
integer encoding) — no corpus case exercises it. A **nested struct field
value** WF-071 admits now **does** translate — `struct_value_term`
recurses, one nested functor per level, the value-side mirror of the
`struct_base` field-access recursion (S16, golden
`jutterswet-waarderingsinvoer`, both the case-input and expectation arms).
What remains loud is a **sum-typed field** or a **list-typed field** — a
constructor or list value has no integer encoding in the flat record term,
so `struct_value_term` faults with `a nested sum/list field value has no
integer encoding in the ASP struct term (no corpus case)` (a day-precision
date field *does* translate — `decimals_of_datatype` scales it at 0). A **case-supplied struct computed target** has no monotone
encoding (the case fact and the rule head would union into a superset
model — the ch. 6d fence): `a case-supplied struct-typed computed target
"…" has no encoding here (no corpus case)`. A **struct-in-struct field
access** now **does** translate — `struct_base` recurses on a
struct-typed field-access base, one projection unification per level
(S16, golden `jutterswet-vangstwaardering`) — so what remains loud is a
**struct field-access base** (and its **struct equality operand** kin)
that is a **navigated** struct read, or otherwise neither an own-subject
attribute read, a bound binder, a struct-typed field access, nor — for
equality — a `StructLiteral`: `a struct field-access base here is an
own-subject attribute read, a bound binder or a struct-typed field
access (no corpus case for more)` (navigated struct reads await a case);
a **StructLiteral field scale** disagreeing with its declared scale faults
`a StructLiteral field "…" scale … does not match its declared scale …`.
The struct-side record-valued `attributeValue` expectation in `model.ml`
*is* checked (the anchor pins it); only a **list-valued** one still
faults (above). Missing/extra/leeg record fields, cross-domain
comparison, and inadmissible field datatypes are rejected by WF-071..074
before translation is reached.

*Nested exception groups (S17) — the backstops:* an **initialisation
rung inside a sub-ladder** faults (`an initialisation rung inside a
sub-ladder is ill-formed (WF-075)`) — the initialisation and ch. 7.8
input rungs stay root-only, and WF-075 rejects the shape first, so the
fault survives only as an unreachable backstop on WF-clean input; a
**rung consequence scale** disagreeing with the target scale faults at
every depth (`rung value scale … does not match target scale …`), a
sub-ladder inheriting the root target's declared scale (RS ch. 7.9);
a validity window on a ladder gates per top-level member since the
ch. 8.2 landing (S1's validity gate; a sub-ladder carries no window of
its own in the IR, so nothing nested is silently gated). The
**shared non-trivial base under all members** stays a frontend refusal
(PHASE5C-TUTORIAL §4), never a clingo shape: a group-rung carries a
multi-member base group here with no special encoding, so nothing new is
refused for it.

*The strict `Let`, guarded consequences and the split money
multiplication (S18) — shapes and fences without a corpus case:* a
**value-position `IfThenElse` condition** that is not a comparison
(`a value-position IfThenElse condition is a comparison over bound
operands (WF-077; no corpus case for more)`) — a predicate or composite
condition has no corpus case. Its comparison operands may READ binders,
parameters and literals, and arithmetic combinators (`Binair`,
`Afgerond`, `Begrensd`, `Tekst_reeks`) over those are admissible:
`value_condition` lowers each operand through `expr_term` and harmonises
the two operand scales exactly as the closed-body comparison route does,
so the operand backstop (`… must read only binders, parameters or
literals (WF-077)`) refuses only the attribute/field/characteristic reads
WF-077 already rejects before translation — an unreachable guard on
WF-clean input (the guard mirrors the verifier's `bound_only_expr`). A
**`Let` inside a closed body** (a fold, function or lambda body) **now
translates** — `closed_term`/`body_branches` inline the value and bind the
binder self-contained (S15, golden `jutterswet-kinderlet`); the only closed-body
`Let` reading refused is a binder bound **outside** the body (an ill-formed
shape both executors reject, the parallel verifier card's WF-064 gap). One
`Let`-body shape an executor carries only in the bare rule-value position still
faults elsewhere: a **`Let` whose
body is an `IfThenElse` outside the bare rule value** (a ladder rung, a
creation member initialisation, a sum- or struct-target value —
`expr_term` peels the `Let` but has no `If_then_else` arm) faults
`expression kind not translated (no corpus case exercises it)`. ref
carries it everywhere (ch. 4.10); on the ASP side only `value_branches`
reaches an `IfThenElse` at the rule value (a closed-body `IfThenElse` reducer
does lower — `body_branches`). **Negative operands
under the money multiplication** (ref rounds either sign; runtime `viol/2`, the
non-richting-nul rounding sibling) and the two split range fences (`M·Rh`
and `M·Rl`, runtime `viol/2` — the tutorial's magnitudes clear them); a
**money multiplication other than a 2-decimal money times a 4-decimal
rate** — `Mult_mon_int` and other scale pairs are not split, they ride
the S6 multiplicative fence (the 46340 operand bound, which faults at
large magnitude) — while the split's own scale backstop (`the split money
multiplication expects a 2-decimal money and a 4-decimal rate …`) is
unreachable once `money_mult_shape` has gated the decimals. The strict
`Let` itself has no fault of its own: a leeg value is underivability, not
a fault (the S18 coincidence). It does, however, **materialise its binder
at a finite integer scale** (the value's out-scale, RUNTIME-SEMANTICS
ch. 2.4), and that is where the afrondingsnorm (RUNTIME-SEMANTICS ch. 2.9,
vraag 3-A: intermediate values are exact-rational, the scale binds
declarations and write boundaries, never a step) becomes a **loud backend
limit**. The minimised synthesis find: a strict `Let` binding the
**non-terminating** quotient `100 ÷ 3 = 33,333…`, read at an endpoint
`Afgerond op 2 rekenkundig`. ref carries the exact rational through the
`Let` and the single endpoint `Afgerond` collapses it once to 33,33; the
ASP side translates the binder's value as a **bare** division (the
`Afgerond`-over-division fold of this section fires only when the round
wraps the division *directly*, not through a binder read), whose
`non-exact division (an explicit rounding is mandated before precision is
lost)` fence trips because `100/3` terminates at **no** finite scale. So
clingo, which cannot represent an exact-rational intermediate, refuses the
form loudly — a documented limit **conform to the norm**: the two
executors diverge here in *expressiveness*, never in *value*, the
exact-to-the-rounding-point rule made visible on the backend that must
round every step.

*Environment and input fences:* names the ASP string encoding cannot
quote (WF-057's backstop, sole cover for case labels); values that do
not fit their declared decimals or exceed 2^31−1; scales beyond 10^9;
unknown object types, attributes, parameters; malformed or missing
`asOfDate` under Rekendatum; granular parameters in scalar slots and
their timelines missing from the case.

Gate 8 keeps the observable-fault subset honest
(`corpus/rood/`), and the catch-alls guarantee the list can only
shrink by *translation*, never by silence.

## Part II — The hand exercises and encoding notes (historical)

*The chapters below predate Part I and keep their original numbers —
results, cards and code comments cite them. Ch. 1–5: exercise 1,
Jutterswet art. 1 (verkrijging) and the art. 2 default
(strandvondeling). Ch. 6: exercise 2, the wet-v2 layered exclusion,
where stratification became load-bearing. Ch. 6a–6d: the encoding as
it landed, including the two adversarial rounds. Findings fed the
delta column of `CONSTRUCTS.md` as they were made.*

## 1. Source and IR view

The two articles (`corpus/jutterswet/wet-v0.md`) exercise, in IR terms:

| IR construct | Where it appears here | `CONSTRUCTS.md` row |
|---|---|---|
| Characteristic attribution | "erkend jutter" as derived characteristic | 2, 6 |
| Fact type + role navigation | ouderschap: kind ↔ ouder | 4, 5 |
| Existential quantification | "ten minste één ouder die …" | 1, 5 |
| Defeasible default | art. 2 "zolang het tegendeel niet blijkt" | 7, 8 |
| The two negations | NAF vs aangetoond tegendeel | 6, 16 |
| Temporal qualification | art. 1 "op dat tijdstip" | 11 |

## 2. The translation, construct by construct

- **Model layer → predicates.** An object type's characteristic becomes a
  unary predicate over its instances; a fact type becomes a plain relation
  (`ouder(Kind, Ouder)`). Nothing of the v0 reference-field encoding
  (`zetVeld`/`joinVeld`/`backref`) has any counterpart: a role is a
  predicate argument position, navigation is a shared variable in a rule
  body.
- **Characteristic attribution → derivation rule.** One rule per
  acquisition ground; multiple grounds are simply multiple rules with the
  same head (art. 1 and art. 2 both conclude `erkend_jutter/1`).
- **Existential "ten minste één ouder" → body variable.** The existential
  is an unbound body variable — available at any nesting depth. The v0
  top-level-only restriction on far-side role membership has no ASP
  counterpart to translate *to*; there is nothing it could even mean here.
- **Positive recursion is native.** Art. 1 qualifies a parent through the
  parent's own (possibly derived) status — `kwalificeert(O) :-
  erkend_jutter(O)` — the classic British-Nationality-Act recursion. The
  minimal stable model is the forward-chaining fixpoint; no ordering
  mechanism is declared anywhere.
- **The art. 2 default → one NAF over a declared contrary.** The repaired
  reading (versiegeschiedenis, v1) translates as a single
  negation-as-failure over a positive contrary predicate:
  `not aangetoond_geen_ouder_kwalificeert(K)`. The contrary is an *input*
  predicate (closed-world at stratum 0), so the program stays stratified.
  The naive reading — a double NAF over derivability of art. 1 — is also
  expressible (`not not voldoet_aan_art_1(K)`) and reproduces the
  documented inversion: it demands positive derivability instead of
  yielding to a proven contrary (run 3 below).
- **Open vs closed predicates.** The corpus's predicate register (world
  facts open, organisational concepts closed) maps onto which predicates
  are inputs; `#defined` marks the inputs that may legitimately be absent.
  Full open-world treatment of world facts (unknown ≠ false) is *not*
  captured by this closed-world program — that is the row-18
  empty/unknown design question surfacing on the ASP side, noted, not
  solved here.

## 3. The program

`jutterswet.lp` — the rules:

```prolog
% Jutterswet, art. 1 + art. 2 (v1-lezing) — ASP-Core-2 / clingo.

% Art. 1 (verkrijging): erkend jutter is degene die na inwerkingtreding op
% Scholde geboren is uit ten minste een ouder die op dat tijdstip erkend
% jutter of vast ingezetene is.
erkend_jutter(K) :-
    geboren_op_scholde(K),
    na_inwerkingtreding(K),
    ouder(K, O),
    kwalificeert(O).

kwalificeert(O) :- erkend_jutter(O).
kwalificeert(O) :- vast_ingezetene(O).

% Art. 2 (strandvondeling), gerepareerde lezing: het vermoeden wijkt pas
% voor een aangetoond tegendeel (NAF over een contrair predicaat).
erkend_jutter(K) :-
    strandvondeling(K),
    not aangetoond_geen_ouder_kwalificeert(K).

#show erkend_jutter/1.

% Invoerpredicaten (mogen leeg zijn zonder waarschuwing).
#defined geboren_op_scholde/1. #defined na_inwerkingtreding/1.
#defined ouder/2. #defined vast_ingezetene/1.
#defined strandvondeling/1. #defined aangetoond_geen_ouder_kwalificeert/1.
```

`dossiers.lp` — dossier facts (Ade and Norre from
`corpus/jutterswet/dossiers.md`; Douwe is a synthetic second generation
added to exercise the recursion):

```prolog
% Ade: geboren op Scholde na inwerkingtreding; moeder vast ingezetene.
geboren_op_scholde(ade). na_inwerkingtreding(ade).
ouder(ade, tsjerkje). vast_ingezetene(tsjerkje).

% Norre: als pasgeborene aangetroffen op het strand; ouders onbekend.
strandvondeling(norre).

% Douwe: kind van Ade (positieve recursie: kwalificatie via een afgeleide
% status, niet via een vastgesteld feit).
geboren_op_scholde(douwe). na_inwerkingtreding(douwe).
ouder(douwe, ade).
```

`tegendeel.lp` — the proven-contrary variant. Deliberately *not* the
corpus dossier "Norre, twee jaar later": that dossier asks whether an
already acquired status is lost when the presumption falls afterwards;
this variant only puts the contrary on the table at evaluation time, so
the default never fires:

```prolog
% Tegendeel aangetoond op het evaluatiemoment: geen ouder kwalificeert.
aangetoond_geen_ouder_kwalificeert(norre).
```

`art2-naief.lp` — the designed pitfall, as a separate head so both
readings can run side by side:

```prolog
% De ontworpen valkuil: art. 2 als dubbele negation-as-failure.
% "zolang het tegendeel niet blijkt" naief vertaald: niet [niet voldoet-aan-art-1].
erkend_jutter_naief(K) :-
    strandvondeling(K),
    not not voldoet_aan_art_1(K).

voldoet_aan_art_1(K) :-
    geboren_op_scholde(K),
    na_inwerkingtreding(K),
    ouder(K, O),
    kwalificeert(O).

#show erkend_jutter_naief/1.
```

## 4. Runs and expected results

Executed with the clingo 5.8.2 binary (`clingo <files>`; any clingo ≥ 5
works). The program is stratified, so each run has exactly one stable
model (`Models: 1`). Answer sets verbatim; solver statistics and timing
lines trimmed — timings vary run to run and pin nothing.

**Run 1 — base:** `clingo jutterswet.lp dossiers.lp`

```
Answer: 1
erkend_jutter(norre) erkend_jutter(ade) erkend_jutter(douwe)
SATISFIABLE
```

Ade via art. 1 on established facts; Douwe via art. 1 through Ade's
*derived* status (the recursion); Norre via the art. 2 default.

**Run 2 — proven contrary:** `clingo jutterswet.lp dossiers.lp tegendeel.lp`

```
Answer: 1
erkend_jutter(ade) erkend_jutter(douwe)
SATISFIABLE
```

The default yields to the proven contrary: Norre's acquisition ground
falls away. (Whether an *already acquired* status is lost retroactively is
the deliberately open herziening question of the corpus — not asked here.)

**Run 3 — the naive reading alongside the repaired one:**
`clingo jutterswet.lp art2-naief.lp dossiers.lp`

```
Answer: 1
erkend_jutter(norre) erkend_jutter(ade) erkend_jutter(douwe)
SATISFIABLE
```

`erkend_jutter_naief(norre)` is **absent** while `erkend_jutter(norre)` is
present: the double NAF demands positive derivability of art. 1, which a
foundling with unknown parents cannot supply — the documented inversion of
the statute's intent, reproduced under stable-model semantics.

## 5. Findings (fed into `CONSTRUCTS.md`)

1. **A fact type is a predicate** (row 4, confirmation). Role navigation
   is variable sharing; the v0 reference-field vocabulary has no
   translation target. The symbolic `RolBinding` delta is not just
   cleaner — for this backend it is the only thing that *can* exist.
   *Landed (2026-08-16):* the binding rides the wire (`IR-SPEC.md`
   ch. 2.6, WF-047/048) and `backends/clingo` reads the `rol/3` join
   role from it, deriving only for bindingless v0 dumps.
2. **The top-level-only restriction is a Drools artefact** (row 5,
   confirmation). The existential lives in any rule body at any depth.
3. **The single-layer default is one NAF over a declared contrary**
   (rows 7/8). No priority or salience mechanism appears anywhere; the
   only ordering notion is stratification, which here holds because the
   contrary is an input. When contraries become *derived* (the art. 4
   ladder of wet-v2), layering will need the standard
   exception-predicate pattern and stratification becomes the load-bearing
   WF condition (row 6) — that is the natural next hand exercise.
4. **"Op dat tijdstip" did not translate** (row 11 — since decided). The
   parent must qualify *at the child's birth* ; this program collapses
   that to an atemporal `kwalificeert/1`, exactly as the boolean corpus
   dossiers do. The timeline machinery is required to say it honestly —
   the first corpus-driven demand on row 11, decided 2026-08-16:
   `read(tl, d)` becomes a core IR node (`RUNTIME-SEMANTICS.md` ch. 3.3);
   in ASP the node is a plain join against the segment relation.
   *Landed:* `backends/clingo` translates `TemporalRead` exactly so —
   `tl(Inst, A, From, To, V), From <= D, D < To` (and `param_tl/4` for
   the vintage read); a gap or an empty index is an unbound join, leeg
   as absence.
5. **Closed-world inputs vs open world facts** (row 18, boundary noted).
   `#defined` documents which inputs may be absent, but absence still
   means false; the corpus's "world facts are open" register needs the
   row-18 empty/unknown decision before this translation can respect it.

## 6. Hand exercise 2: the art. 4 exception ladder

Source: `corpus/jutterswet/wet-v2.md` — art. 3 (penningrecht), art. 4
(uitsluiting, gelaagd: "dit geldt niet indien …; dat laatste geldt weer
niet indien …"), art. 4bis (schorsing). The test case is the Marrit
dossier (`corpus/jutterswet/dossiers.md`) — the only dossier that walks
the whole ladder — plus four synthetic single-difference variants. In IR
terms this exercises:

| IR construct | Where it appears here | `CONSTRUCTS.md` row |
|---|---|---|
| Negation over a derived characteristic | "recht" negatively depends on the derived "uitgesloten" | 6 |
| Exception layering (exception-on-exception) | the art. 4 ladder itself | 8 |
| Stratification as a WF condition | the four-stratum negative dependency chain below | 6 |
| Parallel exclusion grounds | art. 4bis "evenmin" next to the art. 4 ladder | 8, 19 |
| Numeric comparison in a condition | "ten minste twaalf vondsten" | 1 |

### 6.1 The translation, construct by construct

- **The ladder is layered negation-as-failure over *derived* exception
  predicates** — the standard abnormality pattern. Each textual layer
  becomes one rule; each "geldt niet indien" becomes one `not` over the
  next layer's head:

  - layer 0: excluded on a refused raapvergunning, *unless* the layer-1
    exception holds;
  - layer 1: the exception holds when the refusal is herroepen, *unless*
    the layer-2 exception holds;
  - layer 2: the herroeping is vervallen (an input fact; deeper layers
    would continue the same pattern).

  Contrast exercise 1: there the single-layer art. 2 default negated an
  *input* predicate, and stratification was trivially given. Here the
  negated predicates are **derived**, so stratification does real work.
- **The negative dependency chain is stratified by construction**:
  `recht_op_penning` ← ¬`uitgesloten` ← ¬`uitzondering_art4` ←
  ¬`uitzondering_herroeping`, four strata, no cycle through negation.
  This is exactly the condition `CONSTRUCTS.md` row 6 promotes to a WF
  rule: the text's "geldt niet / geldt weer niet" nesting *is* the
  stratification order, read off the statute.
- **The compiled representation** flattens the ladder into classical
  cases over input facts (refused-and-not-herroepen, or
  refused-and-herroepen-and-vervallen). Over complete inputs the two are
  equivalent by construction: unfolding layer 1 into layer 0 gives
  `uitgesloten ⟺ geweigerd ∧ ¬(herroepen ∧ ¬vervallen)`, which
  distributes to `(geweigerd ∧ ¬herroepen) ∨ (geweigerd ∧ vervallen)` —
  logically equivalent to the two compiled cases (the compiled second
  case adds the `herroepen` conjunct, disjoint-normalising the same
  Boolean function; the extra region is absorbed by the first case).
  The equivalence is additionally *shown* under clingo by a difference
  test (run 3): any subject on which the two disagreed would surface as
  `verschil/1`.
- **Art. 4bis is a parallel ground, not a ladder rung.** "Evenmin"
  attaches a second, independent exclusion next to art. 4; it is
  translated as a separate `not geschorst_raapvergunning` in the art. 3
  rule, deliberately *not* as a case of `uitgesloten` — a suspension is
  not subject to the herroeping ladder.
- **"Ten minste twaalf vondsten"** is a plain arithmetic guard
  (`N >= 12`); the art. 5 hardheid variant (twaalf → zes) is out of
  scope here (ch. 7).

### 6.2 The programs

`ladder.lp` — the layered representation:

```prolog
% Jutterswet wet-v2, art. 3 + art. 4 (gelaagd) + art. 4bis — clingo.

% Art. 3 (penningrecht): recht op de jutterspenning heeft de erkende jutter
% van onbesproken juttersgedrag met ten minste twaalf aangemelde vondsten.
recht_op_penning(P) :-
    erkend_jutter(P),
    onbesproken_juttersgedrag(P),
    aantal_vondsten(P, N), N >= 12,
    not uitgesloten(P),
    not geschorst_raapvergunning(P).

% Art. 4 (uitsluiting, gelaagd) — de tekststructuur een-op-een:
% hoofdregel:            geen recht bij een geweigerde raapvergunning,
% uitzondering:          dit geldt niet indien de weigering is herroepen,
% uitzondering daarop:   dat laatste geldt weer niet indien de herroeping
%                        is vervallen.
uitgesloten(P) :-
    raapvergunning_geweigerd(P),
    not uitzondering_art4(P).

uitzondering_art4(P) :-
    weigering_herroepen(P),
    not uitzondering_herroeping(P).

uitzondering_herroeping(P) :-
    herroeping_vervallen(P).

#show recht_op_penning/1.
#show uitgesloten/1.

% Invoerpredicaten (mogen leeg zijn zonder waarschuwing).
#defined erkend_jutter/1. #defined onbesproken_juttersgedrag/1.
#defined aantal_vondsten/2. #defined raapvergunning_geweigerd/1.
#defined weigering_herroepen/1. #defined herroeping_vervallen/1.
#defined geschorst_raapvergunning/1.
```

`gecompileerd.lp` — the compiled representation plus the difference test:

```prolog
% De gecompileerde lezing van art. 4: de ladder platgerekend tot twee
% klassieke gevallen over invoerfeiten (weigering zonder herroeping, of
% weigering met vervallen herroeping).
uitgesloten_c(P) :-
    raapvergunning_geweigerd(P),
    not weigering_herroepen(P).
uitgesloten_c(P) :-
    raapvergunning_geweigerd(P),
    weigering_herroepen(P),
    herroeping_vervallen(P).

% Divergentietoets: een verschil tussen de gelaagde en de gecompileerde
% representatie zou hier zichtbaar worden.
verschil(P) :- uitgesloten(P), not uitgesloten_c(P).
verschil(P) :- uitgesloten_c(P), not uitgesloten(P).
#show verschil/1.
#defined uitgesloten/1. #defined uitgesloten_c/1.
```

`marrit.lp` — the corpus dossier:

```prolog
% Marrit (corpus/jutterswet/dossiers.md): erkend jutter; veertien vondsten;
% onbesproken juttersgedrag uitdrukkelijk vastgesteld; twee jaar geleden een
% raapvergunning geweigerd; vorig jaar de weigering herroepen; over verval
% van de herroeping is niets vastgesteld.
erkend_jutter(marrit).
onbesproken_juttersgedrag(marrit).
aantal_vondsten(marrit, 14).
raapvergunning_geweigerd(marrit).
weigering_herroepen(marrit).
```

`varianten.lp` — the ladder rung by rung:

```prolog
% De ladder laagsgewijs: vier synthetische varianten van hetzelfde dossier.
% w: alleen weigering.               wh: weigering, herroepen.
% whv: weigering, herroepen, vervallen.   s: geschorst (art. 4bis).
erkend_jutter(w).   onbesproken_juttersgedrag(w).   aantal_vondsten(w, 14).
raapvergunning_geweigerd(w).

erkend_jutter(wh).  onbesproken_juttersgedrag(wh).  aantal_vondsten(wh, 14).
raapvergunning_geweigerd(wh). weigering_herroepen(wh).

erkend_jutter(whv). onbesproken_juttersgedrag(whv). aantal_vondsten(whv, 14).
raapvergunning_geweigerd(whv). weigering_herroepen(whv).
herroeping_vervallen(whv).

erkend_jutter(s).   onbesproken_juttersgedrag(s).   aantal_vondsten(s, 14).
geschorst_raapvergunning(s).
```

### 6.3 Runs and expected results

Executed with the clingo 5.8.2 binary; stratified, so every run has
exactly one stable model. Answer sets verbatim, timing trimmed (ch. 4
convention).

**Run 1 — Marrit:** `clingo ladder.lp marrit.lp`

```
Answer: 1
recht_op_penning(marrit)
SATISFIABLE
```

Recht — the weigering would exclude, the herroeping lifts the exclusion,
and of verval nothing is established: the layer-1 exception stands. The
v0 statute (flat art. 4) would have excluded her; the layered v2 text
exists to decide this dossier correctly, and does.

**Run 2 — the ladder rung by rung:** `clingo ladder.lp varianten.lp`

```
Answer: 1
recht_op_penning(wh) uitgesloten(w) uitgesloten(whv)
SATISFIABLE
```

`w` (weigering alone) → uitgesloten, no recht; `wh` (herroepen) →
recht; `whv` (herroeping vervallen) → uitgesloten again, no recht; `s`
(geschorst, art. 4bis) → no recht *without* being `uitgesloten` — the
parallel ground blocks art. 3 directly and never touches the ladder.

**Run 3 — the difference test:**
`clingo ladder.lp gecompileerd.lp marrit.lp varianten.lp`

```
Answer: 1
recht_op_penning(wh) recht_op_penning(marrit) uitgesloten(w) uitgesloten(whv)
SATISFIABLE
```

`verschil/1` is absent over all five subjects: the layered and compiled
representations agree, as the ch. 6.1 unfolding argument says they must.

### 6.4 Findings (fed into `CONSTRUCTS.md`)

1. **Exception-on-exception is layered NAF over derived exception
   predicates** (rows 6/8). One rule per textual layer, one `not` per
   "geldt niet"; the pattern extends to any depth without touching
   earlier layers. The compiled form needs the full DNF recomputed at
   every amendment — the layered form is the amendable one, which is
   precisely why wet-v2 is written that way.
2. **Stratification is now load-bearing** (row 6). Exercise 1's only
   negation sat on an input predicate; here a four-stratum negative
   chain over *derived* predicates decides every outcome. The WF
   condition row 6 promises ("negation over derived characteristics
   requires stratification") is checkable on the IR as acyclicity of the
   negative dependency graph — the statute's own nesting supplies the
   strata.
3. **What the ladder demands of the Phase-4 default model** (row 8): a
   priority *chain* (rule < exception < exception-on-exception), not
   just pairwise defaults — Catala's exception trees express exactly
   this nesting, and the stable-model reading here gives the same
   answers. No conflict arises anywhere in the ladder (the layers are
   linearly ordered), so the §9.5 inconsistency channel is not
   exercised; over-determination needs *unordered* simultaneous
   exceptions, which this statute deliberately avoids.
4. **Parallel grounds compose by conjunction of NAFs** (rows 8/19).
   "Evenmin" (art. 4bis) is a second independent blocker, not a ladder
   rung: `not uitgesloten, not geschorst`. Keeping it out of
   `uitgesloten` preserves the ladder's meaning (a suspension cannot be
   "herroepen" away).
5. **The two representations diverge in structure, not in models** —
   and only the layered one mirrors the statute's amendment history. For
   the IR this is an argument to make exception layering first-class
   (row 8) rather than compiling it away in the frontend: the compiled
   form erases exactly the structure a later amendment needs.

## 6a. The empty-coalescing encoding (backends/clingo, 2026-08-18)

The ch. 1.2a whitelist — leeg reads as 0 on both sides of plus and min
and on the right of verminderd-met, while verminderd-met's left
propagates — lands in the systematic backend as two dual encodings. A
**propagating** position is the bare unbound join: an underivable
operand body leaves the whole rule underivable, which IS Empty
propagation. A **coalescing** position moves the operand's body atoms
into a helper:

```
coN(S,V)   :- is_a(S,"t"), <operand body binding V>.
coN_def(S) :- is_a(S,"t"), <operand body>.
coN(S,0)   :- is_a(S,"t"), not coN_def(S).
```

and the operator's rule joins `coN(S,V)` instead of the raw body — the
value when the operand derives, 0 by negation-as-failure when it does
not. An atom-free operand (a literal) is never empty and stays inline.
Sound on WF-045/046-clean documents: the negation reads exactly what
the positive operand read, so a cycle it sits on is a creation cycle
whose values are grounded in the minimal model — witnessed by the
recursive anchors (rank+1 on the wachtersketen chain) staying stable
under the encoding, and pinned absolutely by
`corpus/golden/anchor/jutterswet-leegrekening`.

## 6b. The multiplicative encoding (backends/clingo, 2026-08-19)

The integer-scaling convention (a value with d declared decimals is the
integer value × 10^d) extends to the multiplicative family without any
division for two of the three operators. **Maal** coalesces both
operands (ch. 1.2a) and multiplies the scaled integers: the product of
a scale-sl and a scale-sr term is the true product at scale sl·sr —
v0's static decimal count dl+dr, exactly. **Percentage-van** is
(l/100)·r with both sides coalescing; the /100 folds into the scale
(sl·sr·100), so no quotient is ever computed and no truncation can
diverge from ref's exact rationals. The write site fits a coarser scale
to the target's declared scale by exact upscaling; a finer scale
downscales under a runtime divisibility guard — ref writes any exact
rational that fits the target's declared decimals (1.5 × 2.0 = 3.0
lands on a 1-decimal target), and a value that does NOT fit derives a
viol/2 witness, never a silently truncated value (a recorded loud
asymmetry: ref would write the exact value).

Every viol/2 or inconsistent/2 head a value expression emits carries
the enclosing rule's condition atoms: ref evaluates the value only
under a true voorwaarde, so a subject the condition excludes must
never fault or deposit a channel fact — without the guard, a
conditional division would turn a satisfiable program UNSAT on the
excluded subjects (pinned by the vindersloon anchor's case4: a zero
divisor under a false condition stays consistent).

**Gedeeld-door** carries the §6.5 edges (arith.div_gen) beside its
value:

```
qN_ldef(S) :- is_a(S,"t"), <dividend body>.
qN_rdef(S) :- is_a(S,"t"), <divisor body>.
qN(S,0)    :- is_a(S,"t"), not qN_ldef(S).
inconsistent("<rule name>",S)
           :- is_a(S,"t"), qN_ldef(S), not qN_rdef(S).
viol("division by numeric zero",S) :- <full body>, (R) == 0.
```

An empty dividend collapses to 0 before the divisor is read (also under
a zero or empty divisor — ref's match order); an empty divisor under a
filled dividend deposits the §9.5 channel fact — keyed by the ENCLOSING
RULE'S NAME, exactly as ref keys its channel (harmonised 2026-08-20 so
the case format's rule pin reads both executors identically; the reason
text stays executor-internal) — and derives no value
(leeg as absence); a numeric zero divisor derives a `viol/2` witness —
UNSAT with the reason named, exactly where ref hard-faults (pinned by
`corpus/rood/division-by-numeric-zero`). The true quotient
(L/sl)/(R/sr) at out-scale m is L·(sr·m) / (R·sl), where m is
max(sl, sr) widened to the write target's scale — ref writes the exact
rational, so a quotient that fits the target's declared decimals
exactly (10/4 = 2.50 on a 2-decimal target) must translate; the bare
division derives its value only when the integer division at m is
EXACT, and a remainder derives a `viol/2` witness instead of a
truncated value. This is a recorded loud asymmetry: ref computes the
exact rational, so a corpus case whose division result fits no
representable scale *without an explicit rounding* cannot be admitted
(the differential would split). The **Afgerond-over-division**
form computes the mode's integer rounding of the true quotient in one
step — floor T/N, ceiling (T+N−1)/N, half-away-from-zero (2T+N)/(2N) —
exact for non-negative operands; the negative range is fenced by a
`viol/2` witness (richting-nul alone matches gringo's native truncation
on both signs and needs no fence). A standalone `Afgerond` over a
scaled term divides the scale surplus away with the same formulas.
Unit-carrying multiplicative operands are refused at translation time:
ref's unit algebra applies exact conversion factors (rate cancellation
under maal, convertible-unit division) that this encoding does not
carry. Anchored absolutely by
`corpus/golden/anchor/jutterswet-vindersloon` (all three operators,
every empty cell, the channel edge) and differentially by the emitter
golden `corpus/golden/jutterswet-bergloon`.

**Gedeeld-door-abs** (2026-08-21) is the same division path collapsed
at (richting-nul, 5): ch. 2.2 defines the variant as the quotient
truncated toward zero at 5 decimals, which is exactly the
rounded-division emission at out-scale 10^5 — and since gringo's
native integer division itself truncates toward zero on both signs,
this is the one rounded mode that needs no negative-range fence. The
three §6.5 edges ride along unchanged (empty dividend → 0 before the
divisor is read; empty divisor under a filled dividend → the
rule-keyed channel fact beside an absent value; numeric zero → a
`viol/2` witness). Anchored absolutely by
`corpus/golden/anchor/jutterswet-taxatie` (the RS ch. 2.2 example row
on both signs — 10 ÷abs 3 = 3,33333 and −10 ÷abs 3 = −3,33333 — plus
the exact and empty corners) and observably red by
`corpus/rood/division-abs-by-numeric-zero` in both executors.

**Tot-de-macht** (2026-08-21) translates for a compile-time
non-negative integer exponent: the scaled base L at scale sl grounds
as `L**e` at result scale sl^e, a compile-time constant fenced at
10^9. gringo's `**` matches the exact integer power on the exercised
range, signed bases included ((−3)\*\*3 = −27; x\*\*0 = 1 including
0\*\*0), so signed bases translate unfenced in value. The base
operand carries a `viol/2` fence at the largest value whose e-th
power stays inside 2^31 — the wrap would occur during *grounding*,
before any guard on the result term could observe it (46340 for e=2,
1290 for e=3; `jutterswet-taxatie` case5 pins the largest
translatable powers replaying exactly, so the guard provably does not
fire early). An empty base propagates through the bare body join
(ch. 1.2: the power has no coalesce). Three shapes fault loudly, by
design: a runtime exponent (the result scale would be
exponent-dependent, and the empty/negative runtime corners have no
integer encoding), a negative exponent (ref inverts the exact power —
a division), and a fractional exponent (an exact root — the ch. 2.1
OPEN). The runtime-exponent empty corner (4^leeg = leeg, ch. 1.2)
therefore remains ref-only behaviour: recorded here, never a silent
SKIP.

## 6c. The three fences of the first adversarial round (2026-08-19)

The first hacker–fixer round (PLAN §8) found no crack in the empty-value
algebra — every composition probed agreed between the executors — but
exposed three ways the integer encoding could diverge SILENTLY or crash
unfenced. Each is now a loud fence:

**The machine-integer fence.** clingo grounds arithmetic in machine
integers and wraps silently on overflow: 50000×50000 grounded negative,
and a comparison threshold upscaled by the scale harmoniser wrapped
negative and flipped a rule's condition — the one divergence class the
differential gate cannot see coming, witnessed live by the round. Every
multiplication emitted by the expression translation now carries a
`viol/2` range guard on its operands: product factors ≤ 46340 (note the
bound is on the SCALED integer, so a 2-decimal multiplication operand
is refused above 463.40 — conservative, and loud); additive, rounding
and division operands bounded with headroom for their formulas; the
clip's comparison harmonisation and its W×N segment products included
(the wu factor is a compile-time constant, bounded at emission). Input
values beyond 2^31−1 fault in `scaled_of_value`, expectation values in
`Model.scaled_expect` (both also fencing the formerly raw `Z.Overflow`
crash on 10^20), and scales beyond 10^9 fault in `pow10`. A comparison
guard deliberately omits the enclosing rule's condition: ref evaluates
a voorwaarde on every subject, so fencing it for every subject mirrors
that reach. The safe side is pinned by `corpus/golden/synth-grensrand`:
the largest translatable product (46340² = 2147395600) replays exactly,
so the guard provably does not fire early. The remainder recorded by
round 1 is closed (2026-08-19): the allocation recurrence bounds pot
and weights at 46340 with at most 46340 recipients (the weight sum,
the pot×weight product and the share sum then provably stay inside
2^31 — shares never exceed the pot under the non-negative-weight
guard), the uniqueness-union harmonisation fences each upscaled key at
max_int31 over its compile-time factor, and every `#sum` accumulation
is bounded — the Som aggregation, the weight sum and the share sum by
contributor ≤ 46340 plus contributor count ≤ 46340 (46340² < 2^31);
the clip total, whose contributors are W×N products, by the |W| ≤
46340 segment guard plus a compile-time bound of 46340 whole units on
the FULL clipped endpoint span (segments are disjoint slices of one
timeline, so ΣN never exceeds the span and |ΣW×N| ≤ 46340² holds).
`#count` aggregates carry no fence: a count cannot approach 2^31
before grounding itself becomes infeasible — recorded, not guarded.

**The unit fence.** ref restates operands across units with exact
conversion factors (1 m plus 50 cm = 150 cm); the integer encoding
carries no factor and grounded 1+50 = 51 — a silent split the round
caught in the additive join, the comparison, and the write boundary
(kg-target written from a t-typed expression). All positions where ref
would restate now refuse NON-IDENTICAL units; identical units (factor
1) and a unitless side (which adopts, value unchanged) translate,
pinned from the safe side by the identical-unit join and comparison in
`synth-grensrand`. The unit read is the EFFECTIVE unit — derived
recursively from attribute and parameter declarations with the ch. 2.6
join rules — never the node's own static annotation: a composite
annotated `eenheid:null` still carries its operands' units at runtime,
and the annotation-based first cut let a comparison of two plus
composites slip through (caught by the verification round of this very
fix). The companion verifier gap is closed: WF-035's additive-family
unit-compatibility clause skipped Verminderd_met entirely (negatief
`wf035-verminderd-met-incompatible-units` pins the rejection; v0's
resolver enforces the same, so the widening is mirror-founded).

**The name-quoting fence.** A declared name or case label containing a
double quote, backslash or control character passed the verifier and
broke the generated program's syntax — an unfenced parser crash.
Emission now faults with the name and reason. Whether the *spec* should
constrain declared names (a WF rule, which would also protect other
backends) is an open recommendation for the maintainer; until then this
is a recorded loud asymmetry (ref executes such names as data).

## 6d. The second adversarial round: the timeline machinery (2026-08-20)

Round 2 hunted the youngest code — the pro-rata fold, the timeline
checkers, the day-precise characteristic loader and the clip — and
found two real classes, each independently confirmed by two hunters:

**The derived-wall fix.** A gedurende clip over a DERIVED value
timeline (a pro-rata target) inherits the fold's interior bucket
walls, which the case's endpoint set does not carry: the clipped
segments found no `wu` fact, the `#sum` ranged over nothing, and the
total silently grounded 0 where ref computed the exact value. The
clip's cutset now enriches with the granularity walls of every derived
attribute the clip touches — the clipped source AND a
condition-comparison attribute alike (this fix's own verify round
caught the condition side reintroducing the zero) — compile-time, and
sufficient because a derived segment's bounds are bucket walls by
construction. Pinned by `corpus/golden/synth2-clip-over-prorata` (the
clipped side: fold Feb+Mar → clip totals 24) and
`corpus/golden/synth2-clip-cond-over-prorata` (the condition side: a
derived timeline under the comparison, total 20), both the rounds'
minimized witnesses admitted through the protocol.

**The computed-target input fence.** ref — like v0 — lets a computed
target OVERWRITE a case-supplied input value or timeline
(`write_attr`/`write_timeline` replace); a monotone ASP program cannot
retract, so the case fact and the rule head would union into a
superset model — a silent split both hunters reproduced symmetrically.
The translator now refuses loudly when the case supplies input for a
Berekening/Gelijkstelling target (scalar or temporal): overwrite
semantics have no monotone encoding without the defeasibility
machinery (Phase 4's default calculus is the principled home; ladder
targets already carry it via `inputval/3`). A recorded loud asymmetry:
ref executes such cases, this backend refuses them, so they cannot
enter the differential corpus.

Hunted and held (no finds): bucket-wall edges of the fold itself, base
gaps at bucket starts, out-of-range condition periods, multi-rule
sharing of condition facts, the overlapping/unsorted ctl loader
rejections, reverse-ordered timeline expectations, and the pro-rata in
operand/creation positions (loud faults on both sides).

*Postscript (2026-08-20, the Phase-4 adoption).* The computed-target
input fence retires for the RS ch. 7.8 **standing targets** — the case
value enters as `inputval/3`, standing by NAF against an effectiveness
atom that carries condition **and** value bindings (the defeasibility
machinery this section named as the principled home; a condition-only
atom would encode v0's erase instead — the atom's body is the semantic
choice). The fence remains on non-standing computing targets
(multi-writer, versioned) — the temporal arm followed with the
temporal-standing card (2026-08-21): a standing temporal target's
input timeline enters as `inputtl/5`, standing **whole** by NAF
against the fold's per-bucket effectiveness atom (`prN_eff` carries
the full bucket body incl. the exactness guard, so an empty fold
derives no eff and the input stands — ref's no-write on an empty
result, whole-timeline reading). The adoption round also found and
closed the neighbouring gap this section missed: an allocation
`doelAttribuut` supplied by the case was **unfenced** — the case
`val/3` fact and the allocation's own head unioned into a superset
model (observed: `[999;60]` on one attribute, ref replaying green over
it), silent wherever no expectation pinned the attribute. It now
refuses loudly, exactly like this fence.

## 7. Deliberately out of scope

*(Historical.)* Art. 5 (hardheid), art. 6, the v3 temporalisation,
quantitative machinery beyond the art. 3 threshold, and any systematic
core-IR-to-ASP mapping — all Phase-3 work. This document grows with
each hand exercise until that systematic mapping replaces it. *That
promise is kept: Part I (S1–S19) is the systematic mapping. The
constructs this chapter deferred have long since landed — hardheid in
the ladder anchors (S10), the temporalisation in the timeline
machinery (S9, historically ch. 6d), the quantitative corpus
throughout.*
