# ASP translation

*Status: the systematic construct-by-construct translation (Part I,
sections S1–S14, complete for the translated core) — the Phase-3
deliverable this document was always growing toward. Part I describes what `backends/clingo` provably does:
every claim is distilled from `backends/clingo/lib/translate.ml` and
witnessed by the gate set (gate 6, `scripts/check-clingo.sh`, replays
every golden case through the translation; gate 8,
`scripts/check-rood.sh`, pins the observable faults; the negatief
corpus under gate 3 pins what the verifier rejects before translation
is ever reached). The text normates what the code proves — semantics
itself is defined by `RUNTIME-SEMANTICS.md` and this document must
never drift from it; where a construct does not translate, Part I says
so loudly (S14), never silently. Part II (chapters 1–7) is the
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
S14 the complete inventory of loud non-translations — what the
backend does not translate is stated there, never silently absent.

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
| `val/3` | instance, attribute, scaled value |
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
`jutterswet-penningvintage` and `corpus/rood/vintage-uncovered-date`.

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

Not translated, loudly (S14 carries the full list): validity windows;
temporal targets outside the two ch. 3.7/3.8 fold shapes; case input
on non-standing computed targets; a `standing` target outside its own
domain.

### S4. Expression terms

Every value expression lowers to a triple ⟨body atoms, integer term,
scale⟩ (`expr_term`). **Literal**: the scaled constant, scale from its
lexeme's decimals. **Parameter reference**: body atom `param("p",P)`,
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
   fallback-0 facet, S14).

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
half-empty, both-empty). What remains loud: non-numeric scalar
equality (text/enum/boolean operands fault as unscalable before any
eqN rule is emitted) and equality in the per-segment clip comparison
(S14).

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
condition (ch. 1.2d). **Equality/inequality**: the eqN-helper
encoding of S5 — gelijk-aan through the helper atom, ongelijk-aan
through its negation (witness `jutterswet-sluitregister`). **Characteristic check** (kenmerk-check): `heeft(S,k)`, or
`not heeft(S,k)` under negation — stratification (WF-045/046) makes
this sound. The *navigated* check walks the `rol/3` hop as a POSITIVE
literal with only the characteristic under the negation: an absent
link fails the positive and the negated form alike — the deliberate
v0 asymmetry (ch. 5.8), encoded for free. **Composite**
(samengesteld, the ALLE and singular DE quantifiers): concatenation
of the parts' atoms — both quantifiers are conjunctions over their
parts. Other quantifiers and condition kinds fault (S14).

Witnesses: `jutterswet-hardheid`, `jutterswet-uitsluiting`,
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
it, S14). The *sum* is doubly fenced: each contributor at 46340 *and*
the contributor count at 46340 (`viol` when `46340 < #count{...}`),
so the accumulated sum provably stays inside 2^31. The bare count
carries no fence — a `#count` cannot approach 2^31 before grounding
itself becomes infeasible (recorded, not guarded; ch. 6c).

**Temporal total, clipped** (totaal ... gedurende, ch. 3.6): the
clip predicates a fold over the subject's *own* temporal attribute on
a condition timeline. Per clip N: the condition segments (the
`ktl/4` atoms read directly for the kenmerk shape, or — for the
per-segment comparison of an own temporal attribute — an
intermediate `clN_cond(S,V2,T2)` predicate), the four-way segment
intersection
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
by subject and target. Per member i, rung n:
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
`rol(ocI(S),subj_role,S)`). The 1:1 input-snapshot narrowing guards
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
allocation and misaligned scales fault (S14). Case input on the
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

### S14. What does not translate — the complete loud inventory

The standing rule (PLAN §4): the core may say "I cannot express this
(yet)"; it may not pretend. Every fault below is a `Value.fault` with
context — the translation stops, nothing degrades. Grouped; the
literal reason strings live in `backends/clingo/lib/translate.ml`.

*Semantics deliberately unencoded (recorded findings):*
non-numeric scalar equality (text/enum/boolean `Gelijk`/`Ongelijk` —
ref answers per ch. 1.2d's non-numeric branch, the translation faults
on the unscalable operand before any eqN rule is emitted; the
*numeric* asymmetry itself is encoded — S5) and equality in the
per-segment clip comparison (the clip's
empty corner is a different shape); runtime, negative and fractional
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
validity windows (rules, ladders, creations); temporal targets
outside the ch. 3.7/3.8 fold shapes; the negated gedurende clip (the
complement walk); gedurende condition shapes beyond WF-055; temporal
totals over case-supplied full timelines, and non-year total
granularity; `Begrensd` (clip/min-max) and rounded roots in term
positions; a temporal attribute read in a scalar slot (ch. 3.3);
non-numeric arithmetic operands and non-numeric literals in term
positions (ref faults date arithmetic — day numbers must not silently
compute); unscalable datatypes in the integer encoding;
characteristic attribution with a non-waar value;
non-proportional and conditional allocation; misaligned allocation
and rounding scales; som fallbacks other than 0; uniqueness key
lists with more than one temporal component, and ever-unique keys
served by a scalar; case input on non-standing computed targets
(scalar, temporal, and allocation targets alike — the ch. 6d
superset-model fence); to-many navigation in scalar slots (WF-053);
bean-field-only fact creations (the symbolic ch. 3.4 form is what
executes); the expression/condition catch-alls ("not translated — no
corpus case exercises it").

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
promise is kept: Part I (S1–S14) is the systematic mapping. The
constructs this chapter deferred have long since landed — hardheid in
the ladder anchors (S10), the temporalisation in the timeline
machinery (S9, historically ch. 6d), the quantitative corpus
throughout.*
