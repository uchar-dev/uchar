# ASP translation

*Status: the systematic construct-by-construct translation (Part I,
sections S1–S19, complete for the translated core) — the Phase-3
deliverable this document was always growing toward. Part I describes what `backends/clingo` provably does:
every claim is distilled from `backends/clingo/lib/translate.ml` and
witnessed by the gate set (gate 6, `scripts/check-clingo.sh`, replays
every golden case through the translation; gate 8,
`scripts/check-red.sh`, pins the observable faults; the negative
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
`Let`, guarded consequences and the split money multiplication;
S19 the complete inventory of loud non-translations — what the backend
does not translate is stated there, never silently absent.

### S1. Program shape and conventions

One case, one ASP program, one stable model. The translation of a
`uchar-ir-v1` document plus a `uchar-case-v0` case is a single
ASP-Core-2 program; WF-045/046 make every admissible document
stratifiable, so the program has exactly one stable model and the
model checker (`backends/clingo/lib/model.ml`) *faults* on anything
else — zero models and multiple witnesses are both translation bugs,
never answers. The runner asks clingo for two models (`-n 2`), so a
second one is seen instead of cut off behind the first, and a program
with none or with more than one is refused with CLINGO-030: the form a
document the verifier rejects takes when the runner is used on its own.
The document is desugared first (`Ir_v0.Desugar`, the
ch. 7.7 init+berekening → defaults rewrite): this translator evaluates
the desugared kernel while the reference interpreter replays the v0
forms natively, so gate 6's differential witnesses the desugaring
itself.

The fixed predicate vocabulary:

| predicate | carries |
|---|---|
| `is_a/2` | instance → object type |
| `val/3` | instance, attribute, scaled (or S2 symbolic) value |
| `has/2` | instance, characteristic |
| `role/3` | holder, canonical role name, filler |
| `tl/5` | instance, attribute, from, to, scaled value (timeline segment) |
| `ktl/4` | instance, characteristic, from, to (condition timeline) |
| `param/2`, `param_tl/4` | parameter values, scalar and vintaged |
| `inputval/3`, `inputtl/5`, `inputrole/3` | case input that enters *defeasibly* (S10) |
| `inconsistent/2` | the §9.5 channel: rule-or-form name, subject |
| `viol/2` | a hard-fault witness: reason, subject |
| `depth/2` | creation-budget bookkeeping (S11) |

Two failure channels, deliberately distinct. `inconsistent/2` **is
part of the model**: §9.5 facts coexist with the answer, exactly as
ref reports an inconsistency beside its values (`RUNTIME-SEMANTICS.md`
ch. 1.4). `viol/2` is fenced by the global constraint `:- viol(_,_).`
— any `viol/2` derivation makes the program UNSAT, which is the
encoding of ref's *hard faults*; the runner then re-runs relaxed,
shows the `viol/2` witnesses, and gate 8 reads the fault kind and its
parameters off the witness (`CODE EF-014 {…}`, RUNTIME-SEMANTICS ch. 5.7b) and
compares them with `expectedFault {code, params}`. Nothing ever degrades silently: a value the
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
witnesses `corpus/red/input-overprecision`,
`corpus/red/input-overprecision-param`).

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
decided at translation, decision for decision (`applies`, which since
2026-09-23 calls ref's `Eval.window_admits` — one shared gate). A rule, a defaults
member, an object creation or a fact creation whose window excludes
the date **does not apply**: nothing of it is emitted — no head, no
helper predicate, no `viol/2` fence (ref never evaluates an excluded
form, so a zero divisor or range hazard inside one must not fault
here either). Window bounds are read through the strict date
reader and compared as day numbers, both ends inclusive, by ref's own
gate; a non-date bound faults naming position and lexeme, and a
non-date `asOfDate` is refused by the shared case loader; a *bounded* window in a
time-free session (`asOfDate` null) faults with ref's own reason
(`a validity-bounded form needs an evaluation date; this session is
time-free`), so the two executors refuse identically. Windowed forms
never collide with the defeasible-input machinery: the ch. 7.7
desugaring and the ch. 7.8 standing norm both demand *version-free*
forms, so an excluded rule's target simply keeps its case input as
the plain `val/3` fact — with one seam handled explicitly: a defaults
*group* whose members are all excluded still emits the unconditional
pass-through `val(S,a,V) :- inputval(S,a,V), is_a(S,ot).`, because
the defeasible entry keys on the unfiltered defaults list (S10/S17).
Windows on the forms the gate does not carry — allocation,
consistency checks, uniqueness — fault loudly (S19). Witness:
`jutterswet-vensterheffing` (one rule inside its window, one before
it, replayed at an `asOfDate` on each side).

### S2. Case input: facts and types

The case loader's world enters as ground facts. Per instance: `is_a`
with its label; each scalar attribute value as `val/3` (scaled); each
characteristic as `has/2`; each attribute timeline segment as
`tl/5`; each condition timeline as `ktl/4`; each role filling as
`role/3`. Parameters enter as `param/2`, vintaged (granular) parameters
as `param_tl/4` segments; a *scalar* value on a granular parameter is
refused at load by the shared case loader (both executors identically
— the fence that keeps an unread slot from splitting the
differential). A granular parameter is only ever read temporally
(S4/S9): the scalar-slot read faults.

A numeric value is scaled by its declaration's decimals. The
**unbounded form** (IR-SPEC ch. 2.4 (c): `whole: false`, `decimals:
0`, a "getal" declared without decimals) fixes no scale, and this
encoding holds it as a whole number (the limit RUNTIME-SEMANTICS
ch. 2.4 records). A case input on it that is not whole — an attribute
or parameter, scalar or timeline segment, a struct field, a list
element, a constructor payload — is refused at translation in that
form's own words, `input V1.sleepafstand = 1/6 is not a whole number,
and this encoding holds a number declared without decimals (the
unbounded form) as a whole number`, where ref carries it exactly
(RUNTIME-SEMANTICS ch. 2.4 point 3). Until 2026-09-26 it was refused
as a value that "does not fit 0 decimals", a ceiling the declaration
does not have.

The ch. 1.2d **non-numeric scalars** ride ground *symbolic* terms in
the same `val/3` slot, beside the integer encoding
(`symbolic_value_term`): a **text** value is its verbatim quoted
string (ref compares `String.equal` — spelling and case are
load-bearing), an **enum** label its NORMALISED quoted string (ref
compares norm-equal, so both executors normalise at the same point),
and a **boolean** the bare symbol `true`/`false`. An embedded
**double-quote** or **backslash** in a text or enum value is lowered to
its gringo escape (backslash-quote, doubled-backslash) — identically on
this value side and on the literal side (`symbolic_literal_term`, S4,
where the literal lowers through `symbolic_term` to its S2 ground term),
so the equality join stays sound; the expectation-check parser reads the
same escaped shape back, escape-aware, and a control character < 0x20,
which has no escape, still faults (S19). A boolean input
additionally keeps its closed-world characteristic reading — `true` →
`has/2`, `false` → absence (the `bool_atoms` read of S11) — because
the equality read needs the three-way distinction (true / false /
empty) that `has/2` alone cannot carry: `false` is a *value* in the
`val/3` carriage, distinct from absence. Day-precision **dates** are
not symbolic: a date is its civil day number in the integer encoding
(S4). Date *ordering* and *inequality* ride the numeric rules on that
day number, but date *equality* follows RS ch. 1.2d's date row — any
empty operand false, in particular both-empty (v0's Tabel-38 guard on
the date shape; S7 carries the routing). A `dateTime` value stays
refused — milliseconds since epoch exceed clingo's integer range.

The standing-target routing is the one semantic decision made at
input time (RS ch. 7.8; S10 carries the detail): if the target attribute of
a defeasible producer — a defaults target, or a bare single-writer
version-free computing rule (the standing norm) — receives case
input, that input enters as `inputval/3` (scalar) or `inputtl/5`
(timeline) instead of `val/3`/`tl/5`, so the producer can defeat it;
everything else enters as plain facts. Case input on a *non-standing*
computed target is refused loudly (ref overwrites; a monotone
encoding would union — the hunted superset model of ch. 6d). Role
fillings are doubled as `inputrole/3` when object creation is present
(the ch. 5.5 input-snapshot narrowing, S11). With recursive creation,
every input instance gets `depth(L,0)` under the creation budget.

Witnesses: every golden case exercises this section (gate 6);
standing routing by the defaults anchors, `jutterswet-waardestelling`
(the scalar standing norm) and `jutterswet-borgdeel` (the temporal
arm); vintages by `jutterswet-aanmeldquotum`,
`jutterswet-penningvintage` and `corpus/red/vintage-uncovered-date`;
the symbolic carriage by `jutterswet-vondstetiket` (text),
`jutterswet-keurstaat` (enum, including the case2 norm-insensitive
spelling), `jutterswet-borgvlag` (boolean — case2 pins `onwaar` as a
value distinct from case3's absence), `jutterswet-jutdatum` (the
date-as-day-number sibling) and `jutterswet-keurmerk` (the quote-escape
witness — an embedded double-quote on both the value and literal sides).

### S3. Rules and write targets

Each version of a `Rule` translates by kind, which the wire does not
write: a version with a `presentation` tag computes, one with
`initialisation: true` initialises, and one with neither attributes the
characteristic its `target` names. **Characteristic attribution**
(value `{"boolean": true}`): `has(S,k) :- is_a(S,ot),
⟨condition atoms⟩.` — negation-as-failure gives the absent case for
free, and WF-045/046 stratification makes the negated reads sound.
**Computation/equation** (`calculation`/`equation`) to a scalar
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

**Initialisation** (`initialisation: true`). Inside the ch. 7.7
desugaring domain — one unconditional, version-free initialisation
beside at most one version-free computing rule — the pair is read as a
defaults group before translation and the initialisation becomes the
initialisation default (S10). Outside it — a conditional or a windowed
initialisation, which ch. 7.7 leaves to the native reading — it
translates natively, as ref replays it (RS ch. 5.4):

```
val(S,a,⟨term⟩) :- isa(S,ot), ⟨exclusions⟩, ⟨condition⟩, not inputfilled(S,a)
                   [, not comp_decides(S,a)], ⟨bindings⟩, ⟨fit guards⟩.
```

`inputfilled/2` is a case fact beside the untouched input `val/3`: the
guard reads the input (RS ch. 5.4), so a supplied value stands, the
initialisation's own write never switches the guard off, and the head is
not its own negative premise. Beside one computing rule that writes a
concrete type of the initialisation's chain as a rule too, that rule
derives `comp_decides(S,a)` with its own val body and `comp_fires(S,a)`
with its base and condition; the head needs `not comp_decides(S,a)`, so
the computed value wins. Where `comp_fires` holds without
`comp_decides`, v0 erases what the initialisation writes and the
initialisation writes it again, and ref faults on its pass budget; there
`viol("no fixpoint: a computing rule evaluating leeg erases what the
initialisation of a writes, and the initialisation writes it again
(ch. 5.7)",S)` fires as well, unless the rule is a standing one (S10),
which writes nothing on leeg. The value's fences carry the write guard
(`ctx.rule_guard`): ref evaluates the value only where it writes. Where
the chain of a concrete type declares a defaults group on the target, the
rule is folded into that type's stack as its initialisation default,
condition and all (RS ch. 7.10, S10), and `⟨exclusions⟩`
(`not is_a(S,t)` per such type) keeps the rule's own head off it.
Witnesses: `jutterswet-voorwaardelijke-initialisatie` (the three
branches of the guard), `jutterswet-wrakinitialisatie` (the folded
initialisation beside the rule on the supertype's own instances) and
`jutterswet-initialisatievolgorde` (a value that a rule written later
computes, conditional through this arm and unconditional through the
desugaring); the co-writer and divergence shapes are pinned in the
backend's own suite.

A **text/enum scalar target** takes the sum/struct-target route: the
value — a text/enum literal or an own-subject text/enum attribute
read, lowered by `symbolic_term` (S2's ground terms) — rides straight
into the `val` head, no scale, no unit, no fit. A leeg source read
binds no atom, so the rule does not fire and the target is leeg
(propagation for free). A case-supplied text/enum *computed* target
has no monotone encoding here (the sum-target sibling) and is fenced
loudly. A **boolean computed target** is refused with its reason: a
boolean is a characteristic in v0, so rule forms *read* it
closed-world through `has/2` (the WF-027 `bool_atoms` guard, S11),
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
domain; boolean computed targets (above); the initialisation shapes the
native arm has no guard for (above).

### S4. Expression terms

Every value expression lowers to a triple ⟨body atoms, integer term,
scale⟩ (`expr_term`). **Literal**: the scaled constant, scale from its
lexeme's decimals. A **rational literal** (IR-SPEC ch. 4.1: `a/b`, the
sign on the numerator) has no decimals to read, so it takes the smallest
power-of-ten scale that holds its value exactly: `3/2` and `-1/2` at one
decimal, `3/4` at two (golden `jutterswet-breukliteral`). Until
2026-09-26 it was read at scale 1 and faulted `value 3/2 does not fit 0
decimals`. A rational that no scale up to 10^9 holds — `1/3`, which has
no finite decimal expansion, or `1/1024`, which needs ten decimals — is
refused by name, `rational literal 1/3 has no finite decimal expansion
within 9 decimals`, where ref computes it exactly (S19). A **date literal** (`|2000-01-01|`) is its civil day
number at scale 1 — the *same* encoding as a date attribute read
(`val/3`), the point read and the evaluationDate context value, so a date
comparison harmonises day numbers directly and needs no second date
encoding (B2). Date *arithmetic* rides the same day numbers through
the S6a calendar chains (`numeric_ok` still gates the multiplicative
family and verminderd-met on both operands — a date must not silently
multiply); a text/enum/boolean literal in a
NUMERIC term position still faults (S19) — in the positions that
carry the non-numeric scalars (an equality operand, a text/enum write
value) it lowers through `symbolic_term` to its S2 ground term, never
through `expr_term`. **Parameter reference**: body atom `param("p",P)`,
term `P`; granular parameters are refused in scalar slots (ch. 3.3/8).
**Attribute reference**: body atom `val(S,a,A)`; a *navigated*
reference (ch. 5.8) chains one `role/3` atom per to-one hop before the
final `val` — the shared variable is the join, and an absent link
simply leaves the body underivable. The hops are the reference's role
bindings (`path`, since L2 the only spelling of a hop), each
resolved through the reference interpreter's own `Eval.bound_role`, so
both executors resolve navigation identically. A reference without
bindings is the subject's own read (ch. 5.8 rule 4): no `role/3` atom is
emitted at all and the `val` atom reads the subject itself — the
unconditional own read, structurally (witness
`jutterswet-aanmelderkrediet`) — and its `subjectType` must be the subject
type itself, anything else refused by name (WF-053). *Until L2* the
translator also resolved written hop names (`Eval.connecting_role`,
`Eval.nav_single_step`), and the zero-hop self-reference was a written
step that resolved to an empty hop list. **evaluationDate** (ch. 8.2): the session's evaluation date
as a compile-time civil day number; a case without `asOfDate` faults.
**Temporal point read** (ch. 3.3): a `tl/5`/`param_tl/4` atom with
`F <= D, D < T` — the gap and the empty-index corner are the
underivable body, for free (more in S9). **Unresolved**: translation
faults with "Unresolved reached" — the WF-044 unsolved-state guard;
the rule fails observably (gate-8 witness
`corpus/red/wf044-unresolved-reached`).

Witnesses: navigation by `synth-navigatiepremie`,
`jutterswet-vindersloon`, `jutterswet-borgdeel`; evaluationDate and point
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
date corner, where ongelijk-aan on one leeg is waar — the
asymmetry pinned side by side). The **date** row (RS ch. 1.2d, v0's
Tabel-38 guard on the date shape) straddles the two: date gelijk-aan
takes the bare positive harmonised join — any empty operand false, in
particular both-empty, where the eqN both-unbound arm would read waar
— while date ongelijk-aan stays on the eqN complement, whose cells
(exactly-one-empty waar, both-empty onwaar) are the date row's own.
Anchored by `jutterswet-paasvondst` case5 (both-empty gelijk-aan
onwaar: a dateless subject gains no kenmerk) beside
`jutterswet-jutdatum` case3 (the one-empty cells). What remains loud:
equality in the per-segment clip comparison (S19).

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
  no division anywhere. Both operands coalesce. Percentage-van's left
  operand also admits a provably pure-% unit (that same /100 is its bridge
  out of %-space; the fold is unchanged — see the units admission below).
- **Division** (gedeeld-door): the three §6.5 edges ride beside the
  value — empty dividend → 0 *before* the divisor is read; empty
  divisor under a filled dividend → the rule-keyed channel fact and
  no value; numeric zero divisor under a filled dividend → the same,
  with the code EF-005 beside it (`inconsistent_code/3`) — v1 routes this
  case into the §9.5 channel (RUNTIME-SEMANTICS ch. 1.4 point 4; executed
  2026-10-19 in both executors at once, until then the v0-mirror hard
  fault `viol("division by numeric zero")`). The bare form translates only EXACT outcomes:
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
  table); golden `corpus/golden/division-abs-by-numeric-zero` (a red
  case until 2026-10-19).
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
- **Clamp** (begrensd, ch. 2.7): minimum first (raise to at least the
  lower bound), then maximum (lower to at most the upper bound), in any
  of the three positions (min-only, max-only, min+max), via the integer
  identities max(a,b) = (a+b+|a−b|)/2 and min(a,b) = (a+b−|a−b|)/2 on the
  harmonised scale — a+b±|a−b| is always even, so the /2 is exact on
  either sign. An empty base propagates (the bare join). ref restates
  each bound into the base's unit by an exact factor; the integer
  encoding carries none, so only identical units or a unitless side pass
  (the additive-join fence). A bound must be non-empty by construction:
  ref reads an empty bound as NO bound, which the bare join cannot
  express, so only an atom-free ground literal/constant bound translates
  — a read that could be empty faults loudly (S19).

The fence table — every emitted operation carries `viol/2` range
guards on its operands ("arithmetic range exceeded (clingo grounds in
machine integers)"):

| position | bound on the scaled operand |
|---|---|
| product factor (maal, percentage-van) | 46340 |
| `#sum` contributor and contributor count | 46340 each (46340² < 2³¹) |
| additive operand | 1073741823 / harmonisation factor |
| clamp base/bound (begrensd) | 1073741823 / harmonisation factor |
| division operand | 536870912 / its exact-quotient factor |
| comparison / upscale / write fit | 2147483647 / harmonisation factor |
| power base (exponent e) | ⌊(2³¹−1)^(1/e)⌋ (46340 for e=2, 1290 for e=3) |
| result scale | ≤ 10^9, compile-time |

The safe side is pinned: `synth-grensrand` replays the largest
translatable product (46340² = 2147395600) and `jutterswet-taxatie`
case5 the largest translatable powers, so the guards provably do not
fire early. The clamp is witnessed by `jutterswet-grensbedrag` (the
three positions and the empty-base propagation).

**Units — the factor-1 admission (narrowed 2026-08-28).** The
translation admits exactly the cases ref computes *without applying
any conversion factor* (ch. 2.6):

- **maal** admits a unitless operand times a unit-carrying operand:
  the product adopts the unit with the numeric value unchanged
  (`Arith.mul`, `(Unitless, Unit u) -> Num(v, u)` — no factor), and
  `eff_unit` carries that adopted unit onward so a restating position
  consuming the product still sees the real unit. The operand admitted
  AS the unitless side must be **provably** unit-free (`provably_unitless`
  — a declaration whose datatype is a `Number` without a `unit`, or a
  sum/product of such; until L3 also a v0 `Percentage` without an `eenheid`,
  which since L3 carries the unit `%`), never merely one whose unit `eff_unit`
  cannot resolve: a unit hidden behind an unanalysable node (a `Let`, a
  field access, a division) would otherwise become a silent unit×unit
  product with no downstream unit to fence it, so it stays loud. A
  product of TWO unit-carrying operands (a rate cancellation `€/jr × jr`,
  an opaque product unit `€ × m`) composes units / applies factors the
  integer encoding does not carry, so it stays loud.
- **percentage-van** admits a **provably pure-%** left operand
  (PRIORITIZED-DEFAULTS-EVOLUTIE §8.3 / Q10). `pct_of` computes `(l/100)·r` and discards the left unit
  entirely (the result carries the RIGHT unit), so a `%` on the left is
  arithmetically inert; and the `/100` bridge out of %-space is *already* the
  `sl·sr·100` scale fold — no conversion factor is applied, the encoding is
  unchanged. The proof obligation mirrors `provably_unitless` but proves the
  unit rather than its absence (`provably_pure_pct`): the effective unit,
  derived from the literal/attribute/parameter declaration, is the singleton
  `%` — `abbreviation` and `canonical` both `%`, `factorToCanonical` exactly 1
  (a rational whose `num` equals its `den`), no denominator. A hidden or unknown unit is NOT proven pure-% and stays a
  loud fault; any other unit on the left (e.g. `km`), and any `%`-spelling
  whose factor is not 1, keeps faulting with the unit-carrying multiplicative
  message. The **right** operand rule is unchanged — every unit-carrying right
  operand stays refused (its ch. 2.6 right-unit adoption has no `eff_unit`
  witness and no corpus case yet). Anchors `jutterswet-vindersloon` and
  `synth2-afrondingscombinaties` pin the admitted shape (a `20%` literal times
  a unitless attribute), the frontend-faithful spelling
  `jutterswet-vondsttarief` witnesses.
- **Restating positions** — the additive join, the clamp bounds, the
  comparison, the eerste/laatste fold, the write boundary — accept a
  unitless side (it adopts the other unit, value unchanged,
  `restate_into`, `Unitless, Unit _ -> q`) or identical units. In a write
  rule's VALUE expression, two different declared units may first be
  reduced to one by the finest-canonical normalisation pre-step (below);
  everything the pass does not reduce — including every CONDITION and
  comparison position, which the pass never descends — keeps the loud
  fence outright (the hunted 1 m + 50 cm = 51 of ch. 6c).

The unit is derived recursively from declarations (`eff_unit`), never
read from a node's static annotation. Golden `jutterswet-sleeprest`
pins the admitted factor-free shape (a unitless factor × a unit-carrying
attribute, consumed by a unitless-adopting restating position,
hand-computed absolute values).

**Units — the finest-canonical normalisation pre-step
(PRIORITIZED-DEFAULTS-EVOLUTIE §8, Q9; `backends/clingo/lib/normalise.ml`).** The wire keeps its source units;
the clingo leg runs a wire→wire lowering BEFORE emission (composed with the
ch. 7.7 desugaring at the `translate` entry) that canonicalises each declared
unit system (`UnitSystem`) to its **finest base unit** — the declared unit
`u_min` of smallest `factorToCanonical`. Restating INTO the finest MULTIPLIES (2,5 km ⇒
2500 m, ×1000; 2 jaar ⇒ 24 maand, ×12): exact and integer-encodable, whereas
restating to a coarser unit would divide, possibly non-terminating — the
property clingo's integer encoding cannot hold. The rule is normative for the
clingo leg:

- **Scope.** For a numeric Berekening/Gelijkstelling whose TARGET attribute is
  declared in its stelsel's finest unit, every literal of that stelsel in the
  value expression is restated into the finest unit — its value multiplied by
  the exact integer `factor(u → u_min)`, its decimal scale adjusting with the
  factor, its unit annotation set to the finest. The additive join then reads
  one identical unit and translates as raw integer arithmetic (witnessed:
  `jutterswet-sleepbaan`, `jutterswet-vergunningsduur`); the descent also
  covers clamp bounds and ordering folds — code-covered and fault-safe but
  corpus-unwitnessed as yet, a registered hunt-wave witness. Conditions and
  comparison positions are NEVER descended: a mixed-unit comparison keeps the
  loud refusal. The write boundary writes the finest unit into a finest
  target.
- **Proof obligations, fault-loud.** Each declared factor is the exact
  rational the wire writes (`Arith.factor` reads `{num, den}` as that rational,
  the reading `ref` uses, so no binary double stands between the declaration
  and the scale; until L3 the factor was a JSON number that needed an exact
  short-decimal reading, and a binary-double tail faulted); and every declared
  unit must restate into the finest by a positive INTEGER (a unit system with
  sibling factors like 2 and 3 admits no such finest and faults — never a
  silently divide-down canonical).
- **What still refuses, loudly and unchanged.** A target NOT in the finest unit
  (the restated finer value has no divide-down write-back the encoding can
  hold); a coarser operand carried by an attribute/parameter rather than a
  literal (its value rides the unit-blind case and is read in source units by
  ref — restating it in clingo alone would split the executors); a rate unit
  (noemer set — its numerator/denominator normalise per their own stelsels,
  PRIORITIZED-DEFAULTS-EVOLUTIE §8.3, not built); the multiplicative positions
  (they demand a unit-FREE operand, or pure-% for percentage-van, which restating cannot
  satisfy); and cross-stelsel or opaque product units. Each keeps its existing
  `units_identical_ok` / `unit_free_ok` fault.
- **The differential is the proof — no new harness.** Gate 6 already demands
  clingo-over-(normalised)-wire hit the SAME absolute expectations gate 5 pins
  for ref-over-source, which restates natively on the write boundary
  (`World.collapse`). Goldens `jutterswet-sleepbaan` (km/m, ×1000, a
  decimal-scale interaction) and `jutterswet-vergunningsduur` (jaar/maand, ×12,
  a non-power-of-ten factor) are refused by clingo before the pass and replay
  exactly after — ref ≡ clingo ≡ Drools (both PLAY in gate 12, the production
  runtime converting units natively). The pass is the identity on every
  factor-1 wire (it only touches a coarser-than-finest literal of the target's
  stelsel, which no already-translating rule carries), so the existing corpus
  translates unchanged — gate 6 green over every prior case is the no-drift
  proof.

### S6a. The calendar chains — date ± duration, tijdsduur, part extraction and Easter (ch. 3.8)

Dates are civil day numbers (the case loader's encoding, the S4 date
literal's B2 convention), so day-precision calendar arithmetic is pure
ASP term arithmetic — no calendar-as-data, no grounding over dates.
The counted unit resolves by SPELLING through the shared table
(`Ir_v0.Checks.time_unit_of_spelling` — one table for verifier, ref
and this backend); declared factors and stelsel never enter.

- **Day and week are linear**: `date + n` days is `(T)+(N)`; weeks
  `(T)+(7*(N))`; the week tijdsduur is `((TB)-(TA))/7`. gringo's `/`
  truncates toward zero on both signs — exactly the ch. 3.8 `quot`.
- **tijdsduur in months/quarters/years** is the packed-month rule as a
  binding chain per operand: the era decomposition (shift by 719468,
  era = Z/146097, then yoe/doy/mp/d as in the civil algorithm) ends in
  the proleptic month `PM = 12·(yoe + 400·era) + mp + 2` — one uniform
  formula, no month-conditional — and `packed = PM·32 + D`. Months are
  `(packed(B) − packed(A))/32`, quarters `/3`, years `/12`, all
  truncating toward zero (the negative-span case of
  `jutterswet-jutleeftijd` pins the toward-zero reading
  differentially). The absolute variant wraps gringo's `|t|`.
- **date ± months/quarters/years** emits one helper predicate per site
  (`dpmN`), derived by the **leap/month-length rule pair**: the
  non-February arm computes `30 + ((M + M/8) mod 2)`, the February arm
  `28 + L` with the leap indicator `L = (4 − Y mod 4)/4 − (100 − Y mod
  100)/100 + (400 − Y mod 400)/400` (each term is 1 exactly on
  divisibility; operands non-negative, so truncation is floor). The
  day clamps by the exact integer min `(D + ML − |D − ML|)/2`, and the
  re-encode is `days_from_civil` with the `(14 − M)/12` March shift.
- **The day-precision part extractors** (jaar/maand/dag-uit) bind the
  same era decomposition once and read the civil components off it —
  `PM/12`, `(PM mod 12) + 1` and `D` — as unitless whole terms at
  scale 1 (ch. 3.8: the month 1..12, never a March-based index).
- **eerste-paasdag** is the ch. 3.8 Gauss/Anonymous chain as binding
  atoms — the spec's `quot`/`mod` are gringo's `/` and `\` verbatim —
  ending in the `days_from_civil` re-encode (the `(14 − MO)/12`
  March-shift term is 0 because the month is always 3 or 4; written
  anyway so the file has one re-encode shape). Its own fence:
  `easterSunday of a year before 1 is not translated` — below year 1
  the chain's intermediates go negative and gringo's toward-zero
  division stops being the floor the era re-encode needs; ref computes
  those years, so the fence is a loud narrowing, never a silent wrong
  date. Since 2026-10-22 a second fence, `easterSunday of a year after
  5000000 is not translated`: beyond that year the day number leaves
  clingo's integers, and until then the chain wrapped silently (year
  6 000 000 gave day −2104231732 where ref gives 6000000-04-02).
- **Emptiness**: the date operand PROPAGATES by its bare join (an
  unbound chain leaves the rule — or the `dpmN` helper — underivable,
  leeg as absence); the duration COALESCES through `coalesce0` (leeg
  counts 0: `+0` days, or `add_months 0` = the date unchanged, since a
  day never exceeds its own month's length). A fractional count
  truncates toward zero through its scale division before the calendar
  step. An extractor or Easter operand propagates like the date operand
  — the ch. 1.2b leeg rule for free.
- **Fences**: every packed decomposition carries the pre-epoch
  `viol/2` witness (`date before 0000-03-01 in calendar arithmetic`) —
  the shift must stay non-negative for truncation to equal floor — and
  the month step adds the year-zero fence (`date arithmetic reaches a
  year before 0000-03`, PMT < 2) guarding the re-encode's divisions.
  Both fire only where the rule fires (rule-guard discipline, S13).
- **The datum-tijd millisecond layer — the pair term** (RUNTIME-SEMANTICS
  ch. 3.8; the A4 pair-term encoding, 2026-08-28, lifting the embargo).
  A millisecond-precision date rides as `dt(D, MS)` — the civil day `D`
  beside the ms-of-day `MS` with `0 <= MS < 86 400 000` — so a datetime's
  absolute ms value (>= 1.7e12 for any modern date) NEVER enters clingo's
  machine-int range whole: only the bounded parts and bounded differences
  do. A datum-tijd input emits `val(l,a,dt(D,MS))` (the case loader's ms
  split by floor division); a datum-tijd expression lowers to
  `(atoms, D, MS)` through `dt_term`.
  - **datetime ± duration**: a **sub-day** unit is linear on the ms value
    with the carry across the day boundary — `TOT = MS + N·u`, then the
    ms-of-day re-splits by floor division (`FMS = ((TOT \ P)+P) \ P`,
    `ND = D + (TOT − FMS)/P`, `P = 86 400 000`) so `MS` never leaves
    `[0, P)`; a **calendar** unit steps the civil day part exactly as a
    day-precision date (linear days/weeks, the month-end clamp via the
    shared `dpm` helper) and reattaches the ms-of-day unchanged — the S6a
    day kernel.
  - **tijdsduur van A tot B**: a **sub-day** unit counts linearly on the
    bounded ms difference `T = (DB−DA)·P + (MSB−MSA)`, quot the unit's ms
    size (toward zero); a **day-based** unit carries the **borrow rule** —
    the borrow-adjusted B-day `DB' = DB + (−1 when DB>DA & MSB<MSA, +1 when
    DB<DA & MSB>MSA, else 0)` fed to the day-precision whole-unit count,
    WITHOUT forming the whole ms difference (so a month/year span stays
    unbounded), exactly ref/'s `whole_units_between_ms`.
  - **eerste/laatste over datum-tijd operands** (ch. 1.2e ordering fold):
    the extreme instant over ≥ 2 datum-tijd operands, lowered
    **lexicographically** because the pair cannot linearise into one integer
    (`D·86 400 000+MS` overflows clingo's int range) — stage 1 takes the
    extreme civil day `DE = #min/#max { D : dtfN(S,D,_) }`, stage 2 the
    extreme ms-of-day among that day's operands
    `ME = #min/#max { MS : dtfN(S,DE,MS) }`, the result `dt(DE,ME)` (`#min`
    for eerste, `#max` for laatste). One helper `dtfN(S,D,MS) :- is_a(S,ot),
    ⟨operand atoms⟩` per operand and a `dtfN_def(S)` definedness atom: an
    empty operand derives no tuple and drops out (the empty-skip as the
    join), all-empty leaves the pair underivable (leeg). Both stages range
    over already-fenced pair components (each `D` and `MS` inside 2^31 by the
    pair-term layer), so no product forms and no fence is added; the fold
    returns the winning VALUE, so a civil-day tie is value-neutral (ref's
    `agg_eerste`/`agg_laatste` over `scalar_cmp`). A non-datum-tijd or
    mixed-granularity operand faults loudly (ref faults the unordered
    comparison); a set-typed datum-tijd fold, a navigated datum-tijd read,
    a datum-tijd **parameter**
    read and a **`Let`/`BinderRef`-bound** datum-tijd (with the residual
    `VariableReference`) stay the dt_term catch-all's loud flank. The last two
    were measured on 2026-09-06 and are each a live shape, not a
    hypothetical: a document whose ordering reads a declared
    millisecond-precision `Date` parameter (v0 `DatumTijd`), and one whose
    left operand is a `Let` binding the datum-tijd
    attribute and reading it back as a `BinderRef`, are both
    `verifier: PASS` and both replay in ref (`oracle: PASS`), while clingo
    answers `TRANSLATION FAULT` at this catch-all. That is this backend
    declining a form the wire admits — one executor short of a
    differential, recorded rather than hidden.
  - **the datum-tijd ORDERING comparison** (ch. 1.2e, 2026-09-06). Before
    it, a datetime ordering carrying no literal at all — two
    millisecond-precision `Date` attributes (v0 `DatumTijd`) under `earlier` —
    faulted `unscalable datatype in the ASP translation` while ref replayed
    the same document exactly: the comparison arms harmonise INTEGER SCALES,
    and a millisecond-precision date has none.
    It has a pair instead, so the comparison is **lexicographic** over it,
    and a disjunction cannot stand in an ASP body, so the two cases become
    two rules for one helper: `dtcmpN(S) :- is_a(S,ot), ⟨operand atoms⟩,
    D₁ ⊙d D₂.` and `dtcmpN(S) :- …, D₁ == D₂, MS₁ ⊙m MS₂.`, where `⊙d` is
    the STRICT day symbol (`<` for the eerder family, `>` for the later
    family) and `⊙m` the operator's own ms symbol (`<`, `<=`, `>`, `>=`).
    Both rules are positive joins over the operands' own binding atoms, so
    an empty operand fails to bind and the comparison is false — the
    ch. 1.2d date row, for free, exactly as the day-precision ordering
    below gets it. Mixed precision faults (WF-036: one instant, one
    representation).
  - **the datum-tijd LITERAL** (2026-09-06, WF-095). A `Literal` typed
    `Date` with `precision: millisecond` (v0 `DatumTijd`) lowers to the GROUND
    pair `(⟨⟩, D, MS)` — no atoms — its
    lexeme read by ref's shared datum-tijd reader and split by the same
    `split_datetime` a case value takes. It is never an absolute ms
    integer, and the measurement is why: `2026-03-15T12:00:00.000` is
    1 773 576 000 000 ms, which gringo grounds as **−245 493 248** —
    it wraps at 32 bits, silently — and two instants 27.7 days apart then
    compare BACKWARDS (measured 2026-09-06: `1775968976896 < 1773576000000`
    derives). The pair keeps `D` and `MS` each inside 2^31 by construction,
    so this arm adds no fence at all. The WIRE lexeme is the canonical
    ISO form alone since 2026-09-22 (WF-095, IR-SPEC ch. 4.1), and since
    2026-09-23 the shared reader reads that form alone too: a day-first
    source snippet in `value` faults at translation time, quoting the
    lexeme (`malformed datum-tijd lexeme "…": not the canonical ISO
    form …`), before any program is grounded.
  - **Fences**: the sub-day paths carry a `viol/2` range guard so the
    bounded ms sum/difference stays inside 2^31 (a sub-day span beyond ~23
    civil days faults loudly — ref, on 63-bit ints, reaches further; the
    ch. 3.8 out-of-range fault as `viol/2`); the calendar path inherits the
    packed decomposition's pre-epoch and year-zero fences. Emptiness is
    unchanged: the datetime operand PROPAGATES by its bare `dt/2` join, the
    duration COALESCES through `coalesce0`.
- **Day-kind (dagsoort) durations** are translated since 2026-09-19
  (RUNTIME-SEMANTICS ch. 3.8). This backend does NOT walk: the reach fence
  is 40 000 days and a ground per-day walk would materialise that many
  days per step and per subject. It uses WF-031's purity clause instead —
  a day-kind condition reaches no fact base and no session value, and the
  translation is per case, so the predicate can be EVALUATED here, at
  translation time, over a window; what reaches the solver is the day
  kind's EXTENSION, `dkt<i>(D, R)`, its matching days each with their
  rank. **What this backend evaluates is a NAMED SUBSET of what WF-031
  admits**, and the difference is a backend limit rather than a semantic
  one: comparisons, composites with any quantifier, leeg/gevuld tests, nested
  day-kind tests on the bound day, and as operands the bound day, a
  literal, a parameter, the date-part extractors and eerste-paasdag. A
  condition built from anything else WF-031 lets through — arithmetic on
  the day (`Binary`), a rounding, a clamp, an `IfThenElse`, a `Let`, a
  `Match` — is verifier-clean and `ref` evaluates it, while this backend
  REFUSES it by name ("a day-kind condition carries an expression this
  backend does not evaluate"). The boundary is a straightforwardly
  extensible one: each missing form is one arm in the same evaluator.
  A parameter the case does not bind is a FAULT here, in `ref`'s own words
  ("parameter … is unbound in this case"), never an empty value quietly
  shipped into the table. §6.10 is then one `#count` over the matching days in the interval
  and §6.11 a rank lookup, since "the Nth day of the kind after A" IS "the
  day whose rank is rank(A) + N". The reach fence is read off the same
  table (no match within the reach of the start, or a gap wider than the
  reach along the way) and fires as `viol/2`. Every fence of a step
  carries, beside the condition, the reach of its place, as the §9.5
  deposits do: in a quantifier fold's matrix the fold's stop, so a member
  never reached walks no fence (RUNTIME-SEMANTICS ch. 3.9a; since
  2026-10-21, golden `jutterswet-keurtermijn`), in a fold member its
  range, in a default the defaults above it that did not settle. In a
  document with an object or fact creation the fences keep firing for
  every member, reached or not (loud; `backends/clingo/README.md` [V5]).
  The step's value does not wait for the reach: the stop is read off the
  matrix's own atoms. One limit is this backend's
  alone and loud, the S6a pattern: the window covers the case's dates
  extended by the reach on both sides, and a query that leaves it is
  refused here where ref computes it — never answered from a table that
  does not reach. The DATUM-TIJD step form is carried (the day kind is
  judged on the civil day part of the pair term and the ms-of-day rides
  through unchanged); the datum-tijd `duration` stays refused, naming the
  same form `ref` names and giving the same reason for it.
- **The date construction** (`dateWith`/`dateTimeWith`, RUNTIME-SEMANTICS
  ch. 3.8; since 2026-10-22, golden `date-with-parts`): each part is an
  unscaled whole term (a part that is not statically a whole unitless
  number is refused at translation), and the civil day number is
  `days_from_civil` with the March shift, the Easter chain's re-encode;
  `dateTimeWith` adds the ms-of-day ((h·60 + min)·60 + s)·1000 + ms as the
  pair term's second component. Parts that name no calendar day (a month
  outside 1..12, a day below 1 or beyond the month's length by the
  February/non-February rule pair of the month step) or no clock time
  derive a `viol/2` witness, never a rolled-over or clamped date; its
  first argument is a tuple of the sentence and the parts,
  `viol(("dateWith: the parts name no calendar day",Y,M,D),S)`, so that
  the runner reads the fault kind EF-032 and its parameters off the ground
  atom (`viol_code`). Every witness joins all parts, so a leeg part leaves
  the value leeg and fires none, as in ref. The year is fenced to
  1..5 000 000 by two further witnesses that refuse loudly with this
  backend's own code, CLINGO-029 (a limit): below 1 the shifted year turns
  negative and gringo's truncation stops being the floor, beyond 5 000 000
  the day number leaves clingo's integers. Every instant outside the
  representable millisecond range lies beyond those years, so where ref
  and Drools fault with EF-007 this backend refuses with CLINGO-029, and
  gate 8 lists the red case `date-time-with-outside-range` in its
  divergence list.
- **Loud flanks**: the sub-day part extractors
  (`uur/minuut/seconde/milliseconde-uit`), and datum-tijd
  **(on)gelijk-aan** still fault at translation — outside the arithmetic,
  the fold ordering and (since 2026-09-06) the scalar ordering the pair
  term carries, no corpus case. The equality row is a different shape, not
  a tighter one: the numeric arm's `eqN` helper harmonises a scale the pair
  term does not have, and Tabel 44's one-empty-true cell needs the negation
  route over it. Day-kind shapes ref refuses at evaluation too.

Witnesses: anchor `jutterswet-jutleeftijd` (the three ch. 3.8 worked
examples — leap child, clamp-not-inverse, signed truncation — plus the
leeg corner) and golden `jutterswet-vervaltermijn` (month-end clamp in
and out of a leap year, the empty-duration standstill, the quarter
step, fractional truncation), each hand-computed and replayed by
gates 5–6. The datum-tijd millisecond layer rides golden
`jutterswet-getijdenpaar` (the ch. 3.8 borrow corner — tijdsduur in hele
uren = 34 and hele dagen = 1, and their negatives — beside datetime +
26 uur across the leap day and + 1 maand preserving the ms-of-day,
hand-computed and replayed by gates 5–6 as the pair-term square). The
datum-tijd ordering fold rides golden `jutterswet-hoogwaterpaar`
(`first`/`last` over a datum-tijd pair — the ms-of-day tie-break on one
civil day, the civil-day-first ordering where the earlier day carries the
later ms-of-day, and the empty-skip and all-empty corners; hand-computed
from ch. 3.8, ref ≡ clingo). The datum-tijd scalar ORDERING and the
datum-tijd LITERAL ride golden `jutterswet-meldstip` (`earlier` and `later`
against one canonical-lexeme literal at a SUBDAG threshold — a
millisecond either side of it, the instant exactly on it, and a whole day
either side, so neither the ms-of-day alone nor the civil day alone can
answer; hand-computed, ref ≡ clingo ≡ Drools). The
extraction/Easter slice rides golden
`jutterswet-paasvondst` (a maand-uit disjunction through the cqN
counting composite, an eerste-paasdag∘jaar-uit date equality through
the positive date-equality join of S5/S7 — any empty operand false, so
a dateless subject is paasvondst-onwaar: the RS ch. 1.2d date row,
v0's Tabel-38 guard, pinned in its leeg case — and a jaar-uit/dag-uit
Gelijkstelling pair), hand-computed on both sides of the Easter and
month boundaries across the 2024, 2026, 2027 and 2038 pins.

### S7. Conditions

`cond_atoms` lowers a condition to body atoms. **Comparison**
(vergelijking): harmonised scaled terms joined by `>=`, `>`, `<`,
`<=`; both operands are bare joins, so a leeg operand fails the
condition (ch. 1.2d). **Equality/inequality**: statically routed
on the operands' declared kind, exactly as ref routes
(`static_numeric`/`static_date`): numeric operands take the eqN-helper
encoding of S5 — gelijk-aan through the helper atom, ongelijk-aan
through its negation (witness `jutterswet-sluitregister`);
text/enum/boolean operands take the positive symbolic-term join of S5
— `==`/`!=` over both bound terms, any empty operand false in both
directions (witnesses `jutterswet-vondstetiket`,
`jutterswet-keurstaat`, `jutterswet-borgvlag`); date-typed gelijk-aan
takes the bare positive harmonised join (any empty operand false —
the RS ch. 1.2d date row, witness `jutterswet-paasvondst` case5)
while date-typed ongelijk-aan stays on the eqN complement, whose
cells match the date row (witness `jutterswet-jutdatum` case3). **Characteristic check** (kenmerk-check): `has(S,k)`, or
`not has(S,k)` under negation — stratification (WF-045/046) makes
this sound. The *navigated* check walks the `role/3` hop as a POSITIVE
literal with only the characteristic under the negation: an absent
link fails the positive and the negated form alike — the deliberate
v0 asymmetry (ch. 5.8), encoded for free. **Role membership**
(`RoleMembership`, RUNTIME-SEMANTICS ch. 5.8a): the subject's links are
read over the OTHER role of `factType` (a reciprocal fact type's one role
is its own other end), so the positive form is a bare `role(S,r,N)` join
keyed on that role's canonical name, the far variable free — N
groundings collapse onto one head, so the check is existence, never a
count, and the multiplicity of either role is no condition. A written
`counterpartType` adds `isa(N,ct)`, the program's extensional membership
closure (ch. 2.2), so an instance of a subtype satisfies it; absent, no
literal is added. The negated form derives a per-subject helper
`rmN(S) :- isa(S,ot), role(S,r,N)` (plus the `isa(N,ct)` literal where a
counterpart is written) and reads it negated, the subject kept bound by
the helper's `isa` base — the plain complement, which *flips*, unlike
the navigated characteristic check. The pair is resolved as the verifier
resolves a role binding (the declared role name; a plural names no
role), and the translator refuses by name what ref refuses: a subject
type that does not fill `role`, and a `counterpartType` that is not
declared or not ⊑ the other role's declared filler (WF-034). Witnesses
`jutterswet-wachtindeling`, `jutterswet-postbezetting`,
`near-side-role-check-to-many`. *Until L2* the near-side `Rollidmaatschap`
was a join over the connecting role derived from the type pair
(`Eval.connecting_role`, shared with ref), and a to-many connecting
reference was v0's marker, refused loudly; the far-side
`VerreRollidmaatschap` was the separate exists-join.
**Composite** (samengesteld, the `all` and singular `the` quantifiers):
concatenation of the parts' atoms — both quantifiers are conjunctions
over their parts. Other condition kinds fault (S19).

**Composite, and the short circuit** (RUNTIME-SEMANTICS ch. 5.10). A
composite's members are read left to right and evaluation stops once the
outcome stands, so a member the outcome no longer reads must raise no
fault and deposit no §9.5 fact. A conjunction of body atoms is
order-free, so the ordering has to be carried by the *guards*: while a
member is translated it runs with `rule_guard` extended by the atoms
that say "the outcome does not stand yet", and the fences, `viol`s and
`inconsistent(…)`s the member emits pick those up in their bodies (S6,
S13). The returned atom list is unchanged, so the composite's **truth
value** is untouched — only its fault surface is.

*Which emissions, exactly.* "Every fence" is a claim, so it is swept
rather than asserted. Of the emission sites reachable from `cond_atoms`:

- **Subject-keyed, and all of them carry `rule_guard`** — the date
  pre-epoch fence and its year `viol`; the min/max folds; the
  eerste/laatste fold; the date-difference and date-arithmetic fences;
  the rounding fences and their negative-operand `viol`; the division's
  range fence, its empty-divisor `inconsistent(…)` and both its `viol`s;
  the scalar range fence; and the numeric equality and ordering arms.
  The last three were **added in the fix round of 2026-09-07**: the
  equality arm, the ordering arm and the eerste/laatste fold built their
  fence bodies without the guard, which made a range `viol` fire out of a
  member the outcome had already settled away — measured as a ref ≡
  clingo split on an `all` and on a `none` composite alike.
- **Element-keyed, and deliberately not** — the closed-body family
  (`closed_term` under `body_branches`: the fold/lambda/function-def
  bodies). These are keyed by a per-element `witness` and carry their own
  `guard_body` chain, not the subject. Threading a subject-keyed prefix
  into an element-keyed body would be a scoping error, not a fix. The
  shape is reachable in the IR type structure (a fold or an invocation
  inside a member's operand), but **no corpus golden carries one inside a
  composite member** (measured 2026-09-07: 0), and the one source-level
  route that reaches it — an aggregation over a computed expression —
  is refused by this translation outright (`expression kind not
  translated`, S19), so it cannot produce a silent divergence. If that
  refusal is ever lifted, the prefix question must be settled with it.
- **The `gedurende` clip — in scope, and it was not** (fixed 2026-09-07).
  RUNTIME-SEMANTICS ch. 5.10's one boundary exempts the clip's **own
  members**; it does not exempt the clip as an **operand**, and a totalled
  clip is an expression that can sit inside a member of an ordinary
  composite. Both of its fences — the comparison fence and the segment
  fence `|W| > 46340` — were keyed on the clip helper's own free `S`
  instead of the rule subject, and carried neither the `is_a` base nor
  `rule_guard`. The first left the fence grounded over a cross product of
  every clip subject with every bound subject; together they fired a range
  `viol` out of a member the outcome had already settled away, and out of
  a rule whose own condition was false. Measured ref PASS vs clingo
  UNSATISFIABLE from ordinary frontend source; witness
  `jutterswet-klipwacht`. Both now key on the subject and carry the base
  and the guard.

Sites outside `cond_atoms` — the write boundary's fit fence, the
to-one-filler `viol`, and the allocation's `viol`/`inconsistent` family —
are not part of this claim and carry no prefix.

For **`all` / `the`** the guard needs nothing new: member *i* is read exactly
where members 1…*i*−1 all held, so the accumulated **prefix atoms are the
guard**. Translating member *i* with `rule_guard = ⟨saved⟩ ∪ atoms(1…i−1)`
turns the §11.1 pattern into ASP for free — `ploegomvang ≠ 0` in member 1
appears in the body of the divide-by-zero `viol` that member 2 emits, so
the constraint cannot fire on the very instances member 1 excluded.
Witness `jutterswet-ploegaandeel`.

**Composite, counting** (samengesteld — `none`, `atLeast`,
`atMost`, `exactly`; the decision-table desugar's disjunctive row is
`atLeast` 1): ref counts the members that evaluate true (its hits)
and compares against the threshold — `none` hits = 0, `atLeast`
hits >= d, `atMost` hits <= d, `exactly` hits = d. Encoded per
composite: each member *i* becomes its own helper rule
`cqK_m<i>(S) :- is_a(S,ot), ⟨member atoms⟩` — derivable exactly where
ref counts a hit, so a leeg member operand counts false through the
unbound positive join (Tabel 38 for free) — and a **prefix chain**
carries the hit count link by link:

```
cqK_h0(S,0)  :- is_a(S,ot).
cqK_h<i>(S,H1) :- cqK_h<i-1>(S,H0), cqK_m<i>(S),     H1 = H0+1.
cqK_h<i>(S,H0) :- cqK_h<i-1>(S,H0), not cqK_m<i>(S).
```

The composite itself contributes only the plain atoms `cqK_h<n>(S,H)`
plus the threshold comparison, so it serves every position `cond_atoms`
serves — aggregate elements included, where a body aggregate literal
could not appear.

The chain replaces the single `#count` aggregate this branch used
before, for the short circuit's sake: member *i*'s guard is the ch. 5.10
stop triggers **negated** over the prefix count, i.e. `cqK_h<i-1>(S,H)`
plus ground comparisons on `H` (`H == 0` for `none`; `H < d, H ≥ d−r` for
TEN MINSTE; `H ≤ d, H ≥ d−r+1` for TEN HOOGSTE; `H ≤ d, H ≥ d−r` for
`exactly`, with *r* = *n* − (*i*−1) the unread rest, ground at translation
time). TEN HOOGSTE's guard is **two-sided** since its true-side trigger
landed (maintainer decision 2026-09-07): member *i* is read only while
the hits have not exceeded *d* **and** the unread rest can still push
them past it — `H + r > d`, written `H ≥ d − r + 1`. At *i* = 1 with
*d* ≥ *n* that second half already fails against `H` = 0, so a vacuously
true at-most emits nothing at all.

**Member 1 is guarded too**, against the chain's own base `cqK_h0(S,0)`.
It is tempting to special-case it as always-read, and this translation
did until the fix round of 2026-09-07 — wrongly: at TEN MINSTE and
`exactly` with a threshold ABOVE the member count the out-of-reach trigger
fires on the EMPTY prefix (ch. 5.10), so ref reads nothing while a
translation that skipped the guard read member 1 and emitted its fence.
Measured as a ref ≡ clingo split on `TEN MINSTE 5` over two members and
on `PRECIES 3` over two, nested and unnested, and reachable from ordinary
source — the grammar does not tie the numeral to the bullet count. With
`H` bound to 0 the ordinary comparison decides it: `H ≥ d − n` holds
exactly when *d* ≤ *n*.

Reading that count over one shared `cq(S,I)`
predicate would put `cq` on both ends of the chain's negative link and
close a predicate-level loop; naming every member and every link after
its index makes each guard edge point at a **strictly smaller** index, so
the dependency graph is acyclic by construction and the ground program
stays stratified. The counted value is identical either way — the chain
over `cqK_m<i>` counts exactly what `#count` over `cq(S,I)` counted.

Silencing a settled member cannot move the verdict: *h* only grows and
*h* + *r* only shrinks, so any trigger that fired already decides the
comparison, and a member the guard silences only ever loses a hit the
verdict no longer reads (the ch. 5.10 stability argument, in the
translation's own terms). The non-monotone quantifiers (`none` / `atMost`
/ `exactly` behave like negation) stay sound because the verifier's
WF-045/046 dependency walk conservatively marks their members as negative
edges. A counting composite without members faults, and every member
kind keeps its own fault (S19). Witness `jutterswet-wachtdeling` (`none`
and TEN MINSTE 1 over the same two members: a stop that fixes the
outcome false and one that fixes it true).

Witnesses (gate 6): `jutterswet-rubriektabel` (TEN MINSTE 1 — the
disjunctive decision-table row over text equalities — and TEN MINSTE
2 over mixed kenmerk/comparison members, threshold boundary
included), `jutterswet-vrijgavetoets` (`none`, with the
leeg-member-counts-false corner), `jutterswet-sluimerwacht` (TEN
HOOGSTE, hits 1/2/3 around threshold 1), `jutterswet-tweevoudkeur`
(`exactly`, hits 2 against 3). Further: `jutterswet-hardheid`,
`jutterswet-uitsluiting`,
`jutterswet-vormenkabinet`; the WF-053 negatiefs pin the navigation
well-formedness the verifier enforces before translation.

### S8. Aggregations and folds

**Count** (aantal): `V = #count { X : role(S,role,X), ⟨filter
atoms⟩ }`, scale 1 — the role is the node's `bronRol` (since L2
required on a count over one source argument, IR-SPEC ch. 4.3), and the
count keys that role's **canonical** name — never the raw written
lexeme, which as a `role/3` key silently counted 0 whenever it differed
from the declared plural that keys the facts (the written singular of a
to-many role, or an undeclared synthesized plural; hardened 2026-08-26,
witnessed by `jutterswet-kaveltelling`). A count source whose written
name is a member of the element type (an attribute or a characteristic)
is refused by name (`the source of this count aggregation reads '…', a
member of the element type '…'`, WF-093 (b)), and so is a count that
carries a `sourcePath` (WF-102 (c): a count source's range is one step).
*Until L2* a count without a binding resolved its written source name
against the declared roles (name or plural, ci) or fell back to the
unique connecting fact type. The `die/dat` filter is lowered by
`cond_atoms` over the element variable. An element the filter's body
cannot derive contributes no tuple: the contributor skip is the join,
for free. A value in the filter that is itself an aggregate — a listed
eerste/laatste, an instance fold — is an assignment `V = #agg { … }`, and
clingo admits no aggregate inside an aggregate element; since 2026-10-22
(card `clingo-nested-aggregate-in-fold-filter`, goldens
`fold-filter-nested-aggregate` and
`fold-filter-inner-fold-via-element-role`) each such atom moves into a
helper predicate per element, `aghN(X,…,V) :- isa(X,T), …, V = #agg { …
}.`, in the filter of every set fold and in a quantifier fold's filter and
matrix alike. The helper's head carries the element variable, the
variables the aggregate shares with an earlier atom of the condition (that
atom copied into the helper's body) and the result. An aggregate that
reads a variable of the enclosing rule — its subject, its guard, a `Let`
binder — or that is no assignment is refused by name (CLINGO-028), never
hoisted into a helper that would read a variable of its own in its place.
The forms a filter and a matrix take in the pass before a creation
(`register_pre`, clones of their own atoms) pass through the hoist where
they enter the element of the quantifier fold's `#count` in that pass
(clingo suite, `tr-kv-listed`).
The **self-denoting** source (WF-083, RS ch. 3.4a) is, since L2, a
plain count source: the count carries its `sourceRole` and translates as
every other count. *Until L2* the written role stood in the argument's
`navigatieRol`, which translated identically — the field was
presentation, not navigation — behind the ref mirror's
`self_denoting_role` check, and an away-pointing role kept the loud
translation fault.
**Sum** (som): contributor body `role(S,role,X), val(X,a,G), ⟨filter⟩`
and `V = #sum { G,X : ⟨body⟩ }` at the element attribute's scale —
an instance without the attribute contributes no tuple, and the empty
tuple set is clingo's native 0, which the **fallback-0** facet reads
directly (ref `agg_som (Some 0)`). A som WITHOUT an `emptyFallback` adds a
**presence guard** — a per-subject helper `sompN(S)` that holds iff at
least one contributor exists, carried by the reading rule — so the empty
set leaves the term underivable and the result is leeg, not 0 (ref
`agg_som None → Empty`; `jutterswet-buitsom`). A fallback OTHER than 0
stays loud (S19). A set source whose ELEMENT ATTRIBUTE is declared a
DAY-TYPED date (`Datum in dagen`) is refused BY NAME, naming the kind,
the source and the member datatype, before anything grounds: the
additive fold is arithmetic and the day number this encoding gives a
date is a POSITION, not an amount, so `#sum` would answer a definite
number no calendar reads back (measured 2026-09-19 on a
verifier-clean document: 19737 + 19783 = 39520, where `ref` faults on
the addition itself). The arm matches that ONE datatype and not dates
in general, because that is its whole reach: a datum-tijd set source
is routed through `dt_term` one node earlier and refused there
(measured 2026-09-20), so a wider match would claim reach this
position does not have. The question is asked of the DECLARATION on
the element type (§2.10), so it does not depend on what a case
supplies: a set with FEWER THAN TWO filled contributors is refused
too, where `ref` answers without ever performing an addition — the
single member itself at one contributor, and leeg (som) or 0 (totaal)
at none. Those two edges are where the two executors part; both are
recorded in `corpus/negative/wf091-set-source-total-date/NOTE.md`. Since
WF-091 (IR-SPEC ch. 4.1) demands a Number as the operand of `sum` and
`total`, the verifier rejects such a document: its witnesses are
`corpus/negative/wf091-set-source-sum-date` and its `total` twin
`wf091-set-source-total-date`, and this translation's own refusal, kept as
a second line, is witnessed by the backend's suite
(`backends/clingo/test/test_clingo.ml`). The *sum* is doubly fenced: each
contributor at 46340 *and* the contributor count at 46340 (`viol` when
`46340 < #count{...}`), so the accumulated sum provably stays inside
2^31. The bare count carries no fence — a `#count` cannot approach 2^31
before grounding itself becomes infeasible (recorded, not guarded;
ch. 6c).
**Minimum / Maximum / Eerste / Laatste** (set-form, ch. 1.2e, ref
`agg_min`/`agg_max`/`agg_eerste`/`agg_laatste`):
the SAME contributor gathering as the som over its set source (since
L2 the argument of a fold that carries a range; until L2 a set-typed
`AttributeReference`), aggregated with `V = #min { G : ⟨body⟩ }` (resp.
`#max`) at the element attribute's scale. An empty contributor is
skipped (the join is the skip, as for the som), and the extreme IS one
contributed value — no accumulation, so no range fence — with a
**mandatory presence guard** `extpN(S)`: `#min`/`#max` over the empty
set yield `#sup`/`#inf`, which must never escape into a value, so an
all-empty or empty set derives nothing (ref → Empty). An `emptyFallback` on
min/max is ignored, mirroring ref. **Eerste and laatste ride this
very arm** (card eerste-laatste-setvouw-kloof, 2026-09-19): they are
the same ordering fold over the contributed VALUES — ch. 1.2e, and
the normative source reads earliest/latest of the values — so over day
numbers eerste is `#min` and laatste `#max`, with the same
empty-skip, the same presence guard and the same temporal refusal (the
four kinds refuse as one ranking class). Witnessed by
`jutterswet-vondstvenster`, whose member-order-reversed case falsifies
a positional pick in both directions. Unlike the listed eerste/laatste
fold there is one attribute, one scale, one unit — no harmonisation.
Witnessed by `jutterswet-vondstpiek` (both extremes, the presence guard
and the empty-skip corners). A DATE element attribute is carried here
and refused at the som: these folds RETURN one contributed value, so
a day number goes in and the same day number comes out, and the
ordering over day numbers IS the ordering over dates. Witness
`jutterswet-vondstbereik` (hand-computed absolute dates; the same two
cases as `jutterswet-vondstvenster`, whose `first`/`last` are the
same fold under another name).

Every fold join closes with `isa(X,el)` on the ELEMENT variable — the
ch. 2.2 population, joined against the reach the `role/3` atoms deliver.
It is emitted unconditionally, including for a `sourcePath` (where it lands
on the last step only, never on an intermediate), and it is the same
chain-closed `isa/2` the allocation's `vdN_ontv` joins on. On a document
without a supertype edge the atom is satisfied by every instance the
role yields, so the encoding is unchanged.

**Temporal total, clipped** (totaal ... gedurende, ch. 3.6): the
clip predicates a fold over the subject's *own* temporal attribute on
a condition timeline. Per clip N: the condition segments (the
`ktl/4` atoms read directly for the kenmerk shape, or — for the
per-segment comparison of an own temporal attribute — an
intermediate `clN_cond(S,V2,T2)` predicate; a FLAT composite `all`
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

**The negated clip member** (`negated: true` on a characteristic
check, either as the whole condition or as a member of the composite
`all` — carried since 2026-09-09, maintainer decision; before that the
form was refused here while ref answered, which was a live ref ≢
clingo on a verifier-clean document): the complement of the
characteristic's periods within the clip horizon. ref takes that
horizon to be the clipped attribute's *own* value timeline and walks
the gaps per value segment (ch. 3.6), so the encoding derives one
`clN_not(S,X,Y)` atom per maximal uncovered stretch inside a single
`tl/5` segment. A gap edge can only be a value-segment edge or the
edge of a covered period — ref's walk emits no other number — so two
candidate predicates supply them:

    clN_not_lo(S,X) :- tl(S,"a",X,_,_).
    clN_not_lo(S,X) :- ktl(S,"k",V,X), V < X.
    clN_not_hi(S,Y) :- tl(S,"a",_,Y,_).
    clN_not_hi(S,Y) :- ktl(S,"k",Y,T), Y < T.
    clN_not_cov(S,X,Y) :- clN_not_lo(S,X), clN_not_hi(S,Y), X < Y,
                          ktl(S,"k",V,T), V < Y, X < T.
    clN_not(S,X,Y) :- tl(S,"a",SV,ST,_), clN_not_lo(S,X),
                      clN_not_hi(S,Y), SV <= X, X < Y, Y <= ST,
                      not clN_not_cov(S,X,Y).

`clN_not` then feeds `clN_and` and `clN_seg` like any other condition
predicate. Maximality needs no aggregate: a left edge is the segment
start or the end of a period that began before it, a right edge is the
segment end or the start of a period that runs past it, so extending
an uncovered pair in either direction always meets a period and
`clN_not_cov` refuses it; value-timeline segments are disjoint, so no
other segment's edge sits strictly inside `(SV,ST)` to offer a
shorter, non-maximal pair. The characteristic with **no timeline at
all** is the sharp case: no `ktl/4` atom exists, nothing is covered,
and the complement is the whole horizon — a clip that totals 0 in its
positive form totals the entire span when negated. The negation is
stratified: `clN_not_cov` is defined by `ktl/4` case facts and `tl/5`
and never reads this helper back. A document that could break that is
refused before it reaches the backend — a negated characteristic check
inside a filter is a NEGATIVE edge in the dependency walk
(`RUNTIME-SEMANTICS.md` ch. 6.1), so a cycle through it fails WF-046
(`corpus/negative/wf046-negated-clip-characteristic-cycle`). The complement's
edges are exactly the endpoints the `clN_wu` cutset already carried, so
the calendar facts are unchanged by the negation.

**Temporal total, unclipped**: the asOfDate-bucket convention — the
case supplies the bucket scalar as a plain `val/3` and the total
reads it (×year factor; only year granularity is exercised); the
total over a case-supplied *full* timeline faults, loudly.

**The articulated eerste/laatste fold** (RS ch. 1.2e: the ordering
fold over ≥ 2 listed operands; dispatched by argument shape within
v0's listed family minimum/maximum/eerste/laatste only — the vestigial
`elementType` v0 stamps on a listed fold names no instance
source, while a multi-operand som/totaal/aantal keeps its loud fault
in both executors *where that stamp is present*, v0's accumulate
consuming the element there; the
numeric operands are RS ch. 1.2e's v1 widening, ratified 2026-09-04):
one helper
predicate per fold site. Each operand's
body derives its value, harmonised to the common scale, into the
shared head — `flN(S,Tᵢ') :- is_a(S,ot), ⟨operand atoms⟩` — and the
fold reads `flN_def(S), V = #min { X : flN(S,X) }` (eerste; laatste
takes `#max`). An empty operand derives nothing and drops out of the
aggregate set — the empty-skip discipline as the join, for free; the
definedness atom keeps the fold's body underivable when *no* operand
derives, so all-empty stays leeg as absence and no `#sup`/`#inf`
ever escapes into a value. Fences: one comparable family only (all
operands statically dates or none — ref faults the unordered mixed
comparison at runtime, and a day number must not silently order
against a scaled numeric; text/boolean/enum operands fault; a
datum-tijd operand faults in THIS numeric arm — the datum-tijd fold
rides `dt_term`'s two-stage lexicographic pair term instead (S6a), not
`expr_term`'s integer encoding), identical (or absent,
adopting) units — ref
restates into the first operand's unit with exact factors this
integer encoding does not carry — and the S6 range bound on any
harmonisation product. Minimum/maximum over listed operands would
take the same encoding; no corpus case exercises them yet, so they
keep the loud S19 fault.

*Where the stamp is ABSENT, the som parts company with that row*
(measured 2026-09-19 on `corpus/red/sum-fallback-always-evaluated`,
which is exactly this shape): `ref` CARRIES the listed som — its
contributor arm reads the operand list whenever `elementType` is
absent, whatever the kind — while this backend does not, since every
som arm it has ranges a `#sum` over an instance source a listed fold
never supplies. That refusal is now a NAMED one (`a sum over listed
operands is not translated …`) rather than the expression fall-through,
for the reason the catch-all inventory below records. Its guard takes
TWO wire fields, not one: an absent `elementType` means listed
*or* dimension cell, because WF-037 grants the DIMENSION som an
explicit arity exemption (one operand, a `dimensionSelection`, no stamp),
and that form is not listed — `ref` refuses it earlier and on its own
ground, `dimension selection not implemented in this card`. So the
named arm demands an absent `dimensionSelection` too, and the dimension
som keeps the fall-through.

Witnesses (gate 6): count by `jutterswet-erflijn`,
`jutterswet-maatschap`, `jutterswet-kustregister`, the self-denoting
source by `jutterswet-aanmeldertelling`; sum by
`jutterswet-bergingstoezicht`, `jutterswet-gemeenteberging`,
`jutterswet-penninguitgifte`, `synth2-budgetgrens-onder`; the clip by
`jutterswet-getijdenwacht`, `jutterswet-penningjaar`,
`synth2-clip-over-prorata`, `synth2-clip-cond-over-prorata`; the
negated clip by `jutterswet-tegenwacht` (both forms from one source —
the bare negated check and the `all` composite with one negated member
— over six cases: the overlap, the disjoint pair, the characteristic
with no timeline at all, the split periods, a value timeline with a
gap and two values which pins the per-segment walk, and the ch. 3.6
negated worked example of `RUNTIME-SEMANTICS.md`, whose 115 is now
pinned differentially rather than in `ref/` alone); the
WF-055 negatiefs pin the clip's admissible condition shapes; the
articulated eerste/laatste fold by `jutterswet-peildatum` (a date pair
and a numeric pair, the ordering-vs-positional pin, the empty-skip and
all-empty corners); the datum-tijd sibling of that fold rides S6a's
pair term instead (`jutterswet-hoogwaterpaar`).

### S9. Timelines

A timeline is data, not state: `tl/5` segments (instance, attribute,
from, to, scaled value) and `ktl/4` condition segments, with civil
day numbers as endpoints. Three readers and two producers translate.

**Point read** (ch. 3.3): `tl(S,a,F,T,R), F <= D, D < T` — and for a
granular parameter `param_tl("p",F,T,R)` likewise. A gap or an empty
date: the body is underivable, leeg for free. An uncovered *vintage*
is the opposite — the shared `parameterset_check` fail-fast, run
before emission (gate-8 witness
`corpus/red/vintage-uncovered-date`). A point read needs the
full timeline in the case; the asOfDate-bucket scalar cannot serve it
and faults (the ch. 4.3a convention: executors fault, never guess).

**Pro-rata** (tijdsevenredig-deel, ch. 3.7): per fold N, the bucket
calendar as data (`prN_b(bs,bn,days)` — every bucket in the span of the case's
endpoints, widened by one period of every fixed start point's grid around
each end (RUNTIME-SEMANTICS ch. 3.2); the calendar is condition-blind), the true-day
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

**Prioritized defaults** (`prioritizedDefaults`, RS ch. 7): the members
are grouped by subject and target. A member whose validity window excludes the
`asOfDate` leaves the member set before translation (ref's
`member_decision` returns Undecided before a single default is
evaluated — S1's validity gate); a group whose members are *all*
excluded decides nothing and emits only the unconditional input
pass-through. Per member i, default n:
`dlI_decides_n(S) :- is_a(S,ot), ⟨default condition⟩[, ⟨consequence
bindings⟩].` — for *attribute* defaults the consequence's bindings
sit inside `decides`, so an empty consequence cannot decide (the
filled-gate for free); higher defaults displace lower ones by `not
dlI_decides_m` for every higher m. The decision projects into
`dlI_dec(S,b)` (characteristic arm) or `dlI_out(S,term)` (attribute
arm), and the head fires under the group's conflict guard:
`has(S,k) :- dlI_dec(S,1), not grpG_conflict(S), ...` /
`val(S,a,V) :- dlI_out(S,V), not grpG_conflict(S), ...`. Equal-priority
disagreement between members derives
`inconsistent("onderwerp.target",S) :- dlI_dec(S,X), dlJ_dec(S,Y),
X != Y` — the §9.5 channel, never an arbitrary pick (a singleton
group elides the conflict machinery entirely). An `initialisation`
default additionally requires `not grpG_has_input(S)`: initialisation
yields to case input by definition (RS ch. 7.3, and the virtual
input default of ch. 7.8 that sits above it). An initialisation that the
fold of RS ch. 7.10 took into a stack keeps its condition, whose atoms
join that body. Every default of a composed stack is translated on the
level that declared it: an own read in it names that type, not the
stack's owner (RS ch. 7.10; `jutterswet-wrakinitialisatie`).

**Standing input** (RS ch. 7.8): case input on a defeasible target
enters as `inputval/3` (S2) and re-enters the model through
`val(S,a,V) :- inputval(S,a,V), is_a(S,ot), not grpG_eff(S).` — the
producer defeats the input exactly where it *decides*. The
effectiveness atom's body **is** the semantic choice: it carries
condition *and* value bindings, so a producer that fires but derives
nothing (an empty defaults outcome, an empty computing rule) leaves the
input standing — ref's no-write-on-empty. A condition-only body
would encode v0's erase instead. The standing norm extends this to
bare single-writer version-free computing rules (S3) and to the
temporal producers (S9, whole-timeline).

Witnesses: `jutterswet-hardheid` (the §4.1 hardship pattern),
`jutterswet-toelagegronden`, `jutterswet-uitsluiting`,
`jutterswet-vondstwaardering` (conflict), `jutterswet-waardestelling`
(scalar standing), `jutterswet-borgdeel` (temporal standing); the
WF-050/051 negatiefs pin defaults well-formedness; gate 7's
reason-carrying no-op survivors pin exactly the cases where standing
input must survive an ineffective producer.

### S11. Creation

**Object creation** (ch. 3.3/5.5): the created instance is a
deterministic function term `ocI(S)` — set semantics gives
idempotence for free (re-deriving the same head is the same atom).
`is_a(ocI(S),new) :- ⟨body⟩.`, member initialisations as
`val(ocI(S),m,⟨term⟩)`/`has(ocI(S),m)`, and the back-link doubled
in both directions (`role(S,new_role,ocI(S))` and
`role(ocI(S),subj_role,S)`). A **characteristic initialisation** deposits
`has(ocI(S),m)`: the two boolean constants are the v0 form — `waar`
an unconditional head, `onwaar` no head at all — and the WF-027
relaxation (besluit 1) admits a boolean-typed **expression**, which
*guards* the head on the SUBJECT (`has(ocI(S),m) :- ⟨body⟩,
⟨bool atoms⟩`): a boolean is a characteristic in v0, so an own-subject
boolean-attribute read is `has(S,a)` — present iff true, absent iff
false, so the deposit fires exactly on true and the mark stays unset on
false (closed-world, ref's reading; `translate_case` carries a boolean
input attribute the same way — `has(label,a)` when true, absence when
false). The 1:1 input-snapshot narrowing guards
the creating body with `not inputrole(S,new_role,_)` — creation reads
the *input* world, not the growing one (ch. 5.5). Recursive creation
is budgeted: `depth(ocI(S),D+1) :- ⟨body⟩, depth(S,D), D <
oc_budget.` with `viol("generation budget exhausted",X) :-
depth(X,oc_budget).` — the runaway chain is UNSAT with its reason,
exactly where ref's pass budget faults. The back-link pair
resolution reuses `Eval.connecting_pair`; an ambiguous back-link
faults in both executors (`corpus/red/ambiguous-backlink`).

**Fact creation** (ch. 3.4): the symbolic form walks its join chain
— one positive `role/3` atom per step — and asserts the new filling
in both directions. The to-one guard is a `viol/2` witness when a
second filler appears on a to-one role
(`corpus/red/to-one-second-filler`). Bean-field-only fact creations
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

**Allocation** (verdeling, ch. 4): all three methods translate as
instances of the ch. 4.2 recurrence, per allocation I rooted in the
gate `vdI_src(B)` — `is_a` plus the voorwaarde's condition atoms
(S7), so a **conditional** allocation distributes only where its
condition holds, and every semantic guard and §9.5 deposit below
rides the same gate (ref evaluates the condition before it touches
pot or recipients). The **scale layer** admits misaligned
declarations: shares are floored at the declared rounding scale
d ≤ t by the integer division itself — the pot upscaled by 10^(d−p)
into the numerator, or the divisor by 10^(p−d) when the pot is finer
(non-negative operands make integer division exactly floor_d; the
weight scale cancels in the ratio) — the written share upscales to
the target scale t, and the remainder lives at its own scale
r ≥ max(p, d): `val(B,ra,A·10^(r−p) − T·10^(r−d))` over the
allocation's OWN share atoms. Without declared rounding ref writes
the EXACT rational share and faults a non-fitting write; that is
mirrored as an exactness viol at the target scale — the previously
"aligned" unrounded form floored silently, now it viols.
**Proportional** (naar rato): weights `vdI_gewicht(X,W)` over the
gate, the weight sum with `S != 0`, raw shares `vdI_raw`, the cap
split share = min(floor_d raw, floor_d cap) — the floor commutes
with min, the declared cap floors to the rounding scale — as the
R≤C / R>C / uncapped cases. **Equal shares**: one group,
`vdI_n(B,N)` recipients, share = floor_d(A/n), no weights and (by
WF-025) no cap. **Ordered** (op volgorde): `vdI_grank` ranks the
distinct key values by counting predecessors (ascending `<`,
descending `>`), the keyless group at the filled-group count — an
empty key sorts LAST in both directions; `vdI_pot(B,R,P)` IS the
recurrence: rank R divides the pot as it stands at entry (the
weighted tie-break `vdI_tw`: an empty weight a real 0 + a §9.5
deposit, a negative one 0 silently, a 0-sum tie group assigning 0 to
every member), shares are capped, floored and clamped at 0
(`vdI_sh0`/`vdI_share`), and `max(0, P−ΣS)` carries forward — the
recursion through the assignment aggregates grounds because the rank
strictly increases under the `vdI_ranks` bound. Negative raws (a
negative pot) clamp to the real 0 ref assigns, so ordered needs no
negative-pot viol, and an empty ordered pot is Nothing (§9.7.6),
never a fault. The §9.5 channel carries the ch. 4.4 holes — a
weightless proportional recipient, an empty ordered tie weight, a
negative or absent cap — keyed by the allocation's name, per
subject. `viol/2` fences: negative pot and weight and empty pot
(equal/proportional only — ref faults the empty pot as the ch. 4.4
v0 mirror hole), the exactness viol, and the wrap bounds on pot,
weight, cap and recipient count, which shrink by exactly the scale
multipliers a misaligned form introduces (46340² < 2^31; equal
scales keep the historical 46340 everywhere). The wrap fences stay
UNGATED by the voorwaarde — grounding computes the scaled products
whether or not the condition turns out true — so clingo refuses
loudly where ref would merely skip the source. **Case input on the
allocation's target** enters defeasibly (S10): the value rides
`inputval/3`, a per-allocation `vdI_eff(X)` fires with the SAME body
as each val head — so it holds exactly where the allocation writes
recipient X — and `val(D,da,V) :- inputval(D,da,V), is_a(D,dt), not
vdI_eff(D)` re-admits the input on any target instance the allocation
does not reach: a non-recipient, or a recipient the method excludes
(a weightless proportional share — ref leaves those standing, never
0). ref overwrites where it writes and leaves the input where it does
not (`World.write_attr`, ch. 4); the layer is exactly that. It is
engaged only where the case supplies the target, and fenced loudly
where that target attribute is READ anywhere else (a feedback into the
pot, weights, criteria or condition has no stratified `not vdI_eff`
encoding — the read set is the verifier's own, `dep_forms`) or WRITTEN
by a second form (the overlap's overwrite order is unpinned) — the
narrowest acyclic form.

**Uniqueness** (ch. 5.9): a collision is a pairwise join —
`inconsistent("name",S1) :- is_a(S1,ot), is_a(S2,ot), S1 != S2,
⟨key atoms S1⟩, ⟨key atoms S2⟩, ⟨scope⟩.` — with scalar keys read
via `val/3`, at most one temporal key component read *ever* via
`tl(L,k,_,_,K)`, and the WF-054 scope hop as a shared `role/3`
parent. The **union** shares one key predicate `uuI_key(S,J,⟨K…⟩)`
across its sources (positions harmonised to the max scale, each
upscale fenced), colliding by `2 <= #count { O,J : uuI_key(O,J,⟨V…⟩)
}` — the source index J rides along because v0 sums per-source
counts. An ever-unique key served by a bucket scalar faults; so do
two temporal components (the same-moment tuple join has no corpus
case).

Witnesses: proportional allocation by `jutterswet-bergingstoezicht`,
`jutterswet-berging`, and the exact (unrounded) proportional share
under a maximum no finer than its target by `jutterswet-buitplafond`
(cdec ≤ tdec, so the clip writes a fitting integer and the surplus
falls into the undivided rest, the raw shares themselves exact);
equal shares by `jutterswet-gelijkdeling`
(floor + remainder, the recipientless source writing nothing);
ordered by `jutterswet-rangdeling` (depletion over year groups, the
empty key last, the negative pot clamping to real 0s),
`jutterswet-rangmaximering` (tie weights + cap + floor + remainder —
the ch. 4.6 full recurrence — plus the 0/0 tie group and the empty
tie weight's §9.5 deposit) and `jutterswet-rangaflopend`
(descending, empty key still last); conditional by
`jutterswet-voorwaardedeling` (the false and leeg condition arms
leaving targets leeg); misaligned scales by `jutterswet-schaaldeling`
(whole-euro pot and cap into a cent target under 2-decimal rounding)
and `jutterswet-centdeling` (cent pot under whole-euro rounding into
a cent target); uniqueness by `jutterswet-kustregister`,
`jutterswet-strandregister`, `jutterswet-postwacht`,
`jutterswet-vondstnummer`; the union by `jutterswet-nummerregister`
(per-subject collision pins); red: `uniqueness-temporal-key-scalar`,
`uniqueness-two-temporal-keys`, `ever-unique-temporal-key-scalar`,
`ever-unique-two-temporal-keys`.

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
| default-group conflict (equal-priority disagreement) | `subject.target` |
| allocation: weightless recipient, empty ordered tie weight, cap holes | the allocation's name |

A channel head embedded in a rule's *value expression* — the
division row — threads the enclosing rule's condition atoms (S3): a
subject the condition excludes deposits nothing. The standalone forms
(consistencies, uniqueness, default-group conflicts) have no
enclosing voorwaarde to thread; their own bodies are the guard. An
allocation's deposits ride its `vdI_src` gate (S12): a source its
voorwaarde excludes deposits nothing, and a deposit out of the
condition itself (a division edge) keys the allocation's name. The
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
The anchor's default — `uitkering` := match on `beschikking`:
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
`has(S,bevoorschot) :- …, val(S,beschikking,V), V == aangehouden.`
and `has(S,afgehandeld) :- …, val(S,beschikking,V), V != aangehouden.`

On the check side (`model.ml`), a constructor term is parsed
**recursively** (the earlier flat lexer could not parse a bare nullary
symbol); a sum-typed `attributeValue` expectation is built by the *same*
`ctor_value_term` emission machinery and parsed by the *same* term
parser, then compared structurally — one source of truth, no independent
scaling path to drift. A sum-typed target that stays leeg reports as
underived (the emptiness test sees any `val/3`, not only numeric ones).

**The enumeration scrutinee** (the keuzetype family's other wire
variant, IR-SPEC ch. 2.4; ch. 9.2's enum arm) reaches the **same
guarantees** by a *different* route, and the difference is worth stating
plainly because the two rules do not look alike. The sum arm puts the
constructor pattern **inside** the `val/3` atom, so ASP's own
term unification selects the arm and a unary arm's `P` is bound by that
atom. The enum arm cannot: the scrutinee rides the S2 symbolic operand
(`symbolic_term`), which binds the label **out** of the `val/3` atom into
a fresh variable and carries that family's fences (temporal
text/enum/boolean carriage among them), and the arm's label — a ground
symbolic term in S2 (`str`, the normalised quoted string both executors
compare under) — is then matched against that variable by a **builtin
equality**. Two shapes, one per variant:

```
matchN(S,⟨arm term⟩) :- is_a(S,ot), val(S,scrut,ctor_k(P…)), ⟨arm body⟩.   (sum)
matchN(S,⟨arm term⟩) :- is_a(S,ot), val(S,scrut,V), V = "label_k", ⟨arm body⟩.   (enum)
```

What *is* shared is everything the semantics rests on: one rule per arm,
mutually exclusive ground patterns, **no NAF**, and a leeg scrutinee
binding no `val/3` atom so `matchN` stays underived — propagation for
free, exactly as above. The enum form is ch. 9.2's **comparison chain**
written out: the chain needs no encoding of its own because mutually
exclusive ground equalities *are* the chain. WF-063 guarantees the arms
are the declared values exactly, each once, and every arm nullary — so no
arm binds anything, and no `P` variable enters the guard. And the domain
identity of IR-SPEC ch. 2.4 guarantees, for both variants, every half of
what the scrutinee is: WF-089 (read half) that the domain a use site's
`type` names is the domain of the source it takes its value from — the
declaration a reference *reads from*, and the operand an
element-passing fold (`first`/`last`/`minimum`/`maximum`) hands
through — and WF-023/WF-067 (write half) that every value **entering**
that declaration — at a rule target, an object-creation member, a
defaults consequence, a function body — names that domain too. Together
they make an underived `matchN` on a verifier-clean document mean a
**leeg scrutinee and nothing else**. No half does that alone, measured
in three rounds: a scrutinee typed with a second, internally consistent
domain left every arm's pattern unmatched and `matchN` silently
underived (leeg for the wrong reason, and on a list-typed source a
folded 0 — a wrong answer where ref faulted); with only that half
repaired a value of a second domain *written into* the truthfully-read
declaration produced the same underived `matchN` with a non-leeg
scrutinee; and with both repaired an element-passing fold — whose own
`type` claimed one domain while its truthfully typed operand named
another — still handed an alien label to the scrutinee, as did a fold
source, the one reference position the expression walk hands off. Those
last two reached ref's fault but not this encoding, because the carrier
refuses an enum operand in a fold ahead of translation (the fence named
below); the guarantee above rests on the verifier refusing all of them,
not on that fence. The verifier now refuses every one of these documents
before translation.

The route difference costs a **narrower carrier**, and the narrowing is
pre-existing rather than new: `symbolic_term` carries only a literal or
an **own-subject attribute read**, so an enumeration-typed *parameter*
used as a match scrutinee is refused loudly by that fence (`a
text/enum/boolean operand here is an own-subject attribute read or a
literal (no corpus case for more)`) where the sum route's fallback would
have evaluated it to a bound term. No corpus case exercises it; it is
listed with S14's other uncarried shapes in the S19 inventory.

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
      (`Rounding{halfAwayFromZero,2}` over a 2-decimal money times a
      4-decimal rate or, with both operands unit-free, a rate of one to three
      decimals, the money on either side, `money_mult_shape`) is the split
      encoding of S18(c) —
      the shared `money_mult_rate`/`money_mult_fences`/`money_mult_body`, with
      the four fences (two negative-operand, two range) and, for a rate below
      four decimals, the range fence on the rate brought to scale 10⁴, all
      keyed by the list-id, and the `A`/`C`
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
      two-factor form; the empty-operand §9.5 edges (empty dividend → 0, empty
      divisor → the channel) cannot arise on always-bound binders, so only the
      numeric zero does, whose quotient is leeg with the deposit EF-005 (golden
      `jutterswet-kinderdeler`, `acc + (grondslag / x)`, `60/[2,3,4,5] → 77.00`,
      every quotient exact; golden `closed-runtime-division-by-zero`, `[2,0]` →
      30,00 with the deposit, in both executors). A closed body has no absence,
      so the translation carries that leeg only as an operand of plus, minus,
      times or percentage-of, where it reads as 0; elsewhere a zero divisor in a
      closed body derives a named `viol/2` witness, a limit of this backend
      (`backends/clingo/README.md` [V4]). Until 2026-10-19 the zero divisor was
      a fault in both executors, *"division by numeric zero"*.
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
`val`/`has`/`inconsistent`/`tl`), so a numeric fold result and a
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
these forms — `has(S,dubbel) :- …, val(S,dossier,B), B == dossier(12050,3).`
and `has(S,afwijkend) :- …, val(S,dossier,B), B != dossier(12050,3).`

The **gevuld gate** (`Predicate` kind `filled`, ref: value ≠ leeg) is the
operand's binding atoms as a **positive join**: a struct operand rides
`struct_term`'s `val/3` atom, any other rides `expr_term`; either way an
absent value leaves the atoms unbound, so the gated rule does not fire —
leeg-on-absence for free (the anchor's `premie` rule reads
`filled(dossier)`).

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

The flat defaults chain of S10 is the unary case of a rose tree (RS
ch. 7.9, the ratified option i). A default's `exceptions` field
generalises the `decides_m` lowering recursively; the translation is a real extension of
S10, not an application of it.

**Chains and groups.** A *chain* — a default list, either a top-level
member or an exception group — lowers to two predicates: an outcome
`T_out(S,V)` (the highest *settling* default's decision: the 0/1 besluit on
characteristic defaults, the scaled consequence term on attribute defaults)
and a conflict flag `T_cfl(S)`. A *group* — a default's `exceptions` field,
or the root member set — reconciles its member chains exactly as S10's root
group reconciles:

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

**The default.** A default carrying `exceptions` reads its group first:

```
tN_dec(S,V) :- gU_out(S,V).                                        % adopt the sub decision
tN_conf(S)  :- gU_cfl(S).                                          % propagate the sub conflict
tN_dec(S,c) :- ⟨own justification⟩, ⟨own bindings⟩, not gU_any(S). % form-a fall-back only
```

A *group-default* (no own besluit/consequence — the multi-member base
group of RS ch. 7.9, e.g. art. 6's B1/B2 basisgrond) omits the third rule
and merely carries its group as a chain position. A default *settles* the
walk when it decides or conflicts (`tN_stl :- tN_dec(_)`;
`tN_stl :- tN_conf`), and the chain's outcome takes the highest settling
default, higher defaults displacing lower ones by `not tM_stl` — the S10
displacement, now carrying conflicts too, so a higher deciding default
displaces a lower default's exception-group conflict (vraag 5-A). The form-a
fall-back's `not gU_any` restricts the own justification/consequence to the
case where the exception groups decide *nothing*: a deciding or conflicting
exception group takes precedence.

**Vraag 5-A — propagation to the root.** `T_cfl` climbs the tree to
`grpG_conflict`, which guards *every* write head and the effectiveness
atom `grpG_eff`: differing exception groups therefore write nothing on the
target and leave any case input standing (S10's `inputval` path), while
the sole §9.5 fact is the `inconsistent` its *own* group deposits under a
**path key** — `subject.target` plus the default-index path
(`Jutter.heffing/0` for the exception groups of root default 0,
`Jutter.heffing/1` for default 1's base group). The root's own differing
decisions keep the bare key `Jutter.heffing` (S10). One conflict, one fact, one place —
uniform with the root reading and Catala's ⊛-propagation minus the
fatality we already made observable.

**Stratification.** Every negation points strictly *down* the tree
(`not gU_cfl`, `not gU_any`) or at strictly *higher* defaults of the same
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
the sole-exception route, case3-verschillend the propagated exception-group
conflict (`heffing` leeg + `Jutter.heffing/0`), case4-gelijk the
equal-decisions rule *at exception-group level*, case5-terugval the form-a
fall-back to `40.00`, case6-basis and case7-basisconflict the group-default
base group and its `Jutter.heffing/1` conflict, case8-niets the
all-undecided no-op survivor; `anchor/jutterswet-nestkenmerk` (the
characteristic arm, art. 7) — case1-y-wint the sub-besluit `false`
displacing the root `true` (vrijgesteld absent, consistent),
case2-conflict the `Jutter.vrijgesteld/0` key, case3-terugval the
fall-back to `true` when the exception groups decide nothing,
case4-nz-zonder-nx (2026-09-23, the carrier-condition repair) the two
inputs without `nx` on which nothing decides — `nz` alone stays absent
and `ny` with `nz` raises no conflict — beside a contrast instance with
`nx` and `nz` that is attributed. The seven
nested-group negatiefs pin what the verifier rejects before translation
ever runs:
`wf050-half-marked-group`, `wf050-subdefault-wrong-shape`,
`wf075-contentless-default`, `wf075-empty-leden`,
`wf075-groupdefault-with-justification`, `wf075-init-in-subdefault`,
`wf075-two-members-unmarked`.

### S18. The `Let`, guarded consequences, and the split money multiplication

Three capabilities land here (design `PHASE5C-TUTORIAL.md` §5–6, the
ratified vragen 2-A and 4-A), all sharing one discipline: **the `Let`'s
leeg barrier is an underivable value atom that simply never binds** — so
ref's leeg propagation and clingo's underivability coincide *by
construction*, the S5 lesson carried into the value position. Since the
O3 unification (2026-09-01) the barrier sits at the **read**, which this
section's encoding realises in two places: an unread binder is not
translated at all, and a read binder's value atoms guard exactly the
branches that read it.

**(a) `Let` as a self-contained bound intermediate.** A
`Let { binder; value; body }` translates the `value` to
`(atoms, term, scale)` and binds the binder in the binder environment
(the S14/S15 regime, WF-064). The value's atoms also thread into the rule
guard, so a leeg value (an underivable read) leaves every rule carrying
the reading body underivable — exactly ref's `Let`, where the barrier
fires at the read (RUNTIME-SEMANTICS ch. 10.6 clause 2). But the
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

**(a′) Call-by-need: the unread binder and the per-branch guard.** Two
refinements carry ch. 10.6's clauses 1–3 into the encoding.

*Clause 3 — an unread binder evaluates nothing.* Before translating a
`Let`, the backend asks whether the `body` reads the binder at all
(`reads_binder`). If it does not, the `value` is **not translated**: no
value atoms, no `letN` helper, no range fence and — the observable part
— no §9.5 `inconsistent/2` deposit and no `viol/2` divisor fence. The
body's translation *is* the `Let`'s. Witness: `Let q = 100,00 ÷ 0,00 in
42,00` writes **42,00** in both executors, where the strict lowering
faulted in both (ref *"division by numeric zero"*, clingo the `viol/2`
UNSAT).

*Clause 2 — the barrier at the read, hence per branch.* `value_branches`
carries, per branch, whether that branch reads a given binder, and the
`Let` arm prefixes the value atoms to **only the reading branches**. An
`IfThenElse` condition is evaluated on every branch, so a binder the
condition reads counts as read on both; a binder read only in `then`
guards only the `then` rule. A branch that never reads the binder
therefore survives a leeg value — the shape the strict lowering could not
express. Witness: `Let k = kindertal in Let x = leegwaarde in if k > 1
then 7,00 else x maal 2,00` with `kindertal` = 2 and `leegwaarde` absent
writes **7,00** in both executors (strict: nothing derived).

`reads_binder` refines the verifier's `mentions_binder` on exactly the
shape the skip creates — a nested `Let` whose own binder is unread never
evaluates its value, so a mention buried there is not a read — which
makes the analysis agree with the translation that consults it. Below a
node it does not descend through (a `Match` arm, a fold body) it falls
back to the syntactic mention. That fallback is conservative in the safe
direction: an extra guard atom can only make clingo derive **less**,
never a different value, so a divergence would show up as a missing
write under gate 6, never as a silent agreement on a wrong one. Two
residues stay recorded rather than hidden: the value's own side
conditions (its §9.5 deposit, its fences) are emitted once, under the
rule guard, so on a **partially** read binder they fire on branches that
do not read it; and ref's within-branch evaluation order (a deposit made
*before* the read that fires the barrier) has no order in ASP. Neither
has a corpus case; both are the same class as the S19 inventory —
report-and-fail territory the moment a witness exists.

*Clause 2 again — the rewrite, not a guard.* Where the barrier's place is
decided by the ARM rather than by the branch prefix, no amount of guard
threading suffices: each lowering translates a `Let`'s value before it
knows the body's branches, so the arm guard does not yet exist when that
value's fences, `viol/2` guards and §9.5 deposits are emitted. A `Let`
whose need is read in exactly one arm is therefore MOVED into that arm
(`let_into_arm`), and the branch node is lifted out of any pure arithmetic
wrapper standing between them so the barrier's extent stays the whole
expression rather than the arm — a coalescing wrapper would otherwise read
the emptied arm as 0. A nested `Let` between the two is handled by
distributing it over the arms, or, where the condition reads its own
binder, by exchanging the two `Let`s; and where the nested value is the
only reader of the outer need, the rewrite asks where the nested `Let`
lands and then CHECKS that the outer need is left read in exactly one arm,
rather than inferring it from a mention. Where that check fails the
rewrite declines and eager placement stays — measured both on a need
genuinely read on both branches (where eager is exact) and on a landing
that is not a bare branch node. Corpus witnesses: `synth3-takhek` and
`synth3-operatortak` (the arm fences, on the rule-head route and under an
operator), `synth3-armverdeling`, `synth3-armwissel` and
`synth3-armvangst` (the nested `Let`, over all three routes) ;
`synth3-armverdeling` and `synth3-armvangst` pin, on the entered arm, that
the barrier empties the whole expression and not just the arm,
`synth3-takhek` and `synth3-armwissel` carry no wrapper to test, and
`synth3-operatortak` pins the complementary edge — an operator wrapper
coalesces the emptied arm to 5,00 precisely because only a `Let` is a
barrier.

**(b) Guarded consequences.** A rule value now reaches an `IfThenElse`
through `Let` bodies (WF-069 widened). At value position it lowers to
**one guarded outcome rule per branch** (`value_branches`): the
`Let` value atoms sit in the bodies of the branches that READ the binder
((a′) above), the condition is a builtin
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
`corpus/red/decimal-nonterminating`, `1,0000 / 3,0000` at four decimals:
ref's *"does not fit the declared 4 decimals"* and clingo's *"non-exact
division (an explicit rounding is mandated before precision is lost)"*).
The rational operators and the `ToRat` casts ride the existing numeric
family unchanged (a cast is a pass-through re-typing — the value is
already exact-rational).

The one new arithmetic encoding is the money product Catala rounds to the
cent (`Mult_mon_rat`; the plugin wraps it in `Rounding {halfAwayFromZero, 2}`).
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
makes the product 0, not leeg); the `Let` barrier that makes a
leeg *money* leeg lives one level up, in the peeled value atoms, never in
these direct operands — a coalescing operand swallows the read's own
`letN` atom, so the peel is where the read-place barrier survives the
product. `Let`s around the product are peeled with their binders in
scope (an unread one is skipped whole, (a′)). Negative operands are not encoded (ref rounds
either sign; the tutorial's money and rate are ≥ 0) — a `viol/2` fence,
S19.

**A rate of fewer decimals, and the money on the right.** The product is
routed to the split on the decimals its terms carry, derived the way the
translation derives a term's scale — a literal from its lexeme, a
reference from its declaration, a binder from its binding, a join by the
rule its arm applies — and never on a composite's own annotation
(RUNTIME-SEMANTICS ch. 2.10). The money may stand on either side of the
product; with two 2-decimal operands the left one is the money. That route
is what admits the RegelSpraak twin of `anchor/ww-herleving-inkomen`: the
frontend annotates the literal `0,8750` at three decimals, its smallest
scale, while its lexeme keeps four. A route that read the annotation took
the split for a four-decimal rate only, so it sent the product to S6,
whose operand fence fires at a maandloon of 2400,00 (240000); on the
terms' decimals the product takes the split with the rate term as it
stands (k = 1). When both operands are unit-free, a rate written or
declared at one to three decimals — the literal `0,875`, an attribute at
three — takes the split as well: at scale 10⁴ it is the same rational (875
at 10³ is 8750 at 10⁴), so its term is multiplied by k = 10^(4−d) first,
under a `viol/2` range fence on that term, `|R| ≤ max_int31/k` — a wrapped
rate would be a plausible wrong answer, not a loud one. The rate's sign
fence reads the term before it is multiplied: past the range fence the
multiplied term has wrapped, and its sign would name a negative operand the
document does not have. Two kinds of product below four decimals keep the
S6 product, because the split would carry less there: one whose operands
are not both unit-free — the split refuses a unit, S6 admits the
factor-free product — and one with a whole-number rate (`Mult_mon_int`),
whose cent rounding is the identity, so S6 carries a negative operand the
split refuses. Below four decimals the split's sign fence costs one flank:
a negative operand beside a negative or zero other operand is refused,
where the S6 rounding carried the non-negative product at small magnitude.
Witnesses: `ww-herleving-inkomen-rgs`, ten cases (ref ≡ clingo), for the
route on the terms' decimals (k = 1); in the corpus only `synth3-letbeslag`
(a rate of 0,10, k = 100) takes the rescale. The backend suite pins both
routes apart on the program text, the rescale fence and the sign read
before it, both orders, the derived route and the flank in its two forms.

Witnesses: `anchor/jutterswet-tariefstelling` — **case1-elsetak** the
else branch (`aanslag` 3700.00 from belasting 4200.00 − aftrek 500.00,
the split money product €20.000 × 21% = €4.200 exact), **case2-dantak**
the then branch (aftrek 750.00 > belasting 210.00 → `aanslag` 0.00),
**case3-leegbarriere** the double leeg barrier (inkomen absent → both
`aanslag` and `gewogen` leeg through the `Let` (every binder in the
chain is read), while `factor`
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
thereby ref-only behaviour); ref's unit-conversion algebra where a
REAL factor survives the finest-canonical normalisation pre-step (S6,
`Normalise`, PRIORITIZED-DEFAULTS-EVOLUTIE §8, Q9): a product of TWO
unit-carrying operands (a rate cancellation, an opaque product unit), division/rate
units, and a restating position (additive join, clamp bound, comparison,
ordering fold, write boundary) over two DIFFERENT units the pass could
NOT reduce to one — a target not in its stelsel's finest unit, a coarser
operand carried by an attribute/parameter rather than a literal, a
condition/comparison position (never descended by the pass), or a
cross-stelsel pair. The finest-canonical restatements now translate — the
pass multiplies each coarser literal into the finest base unit by an exact
integer (2,5 km ⇒ 2500 m, 2 jaar ⇒ 24 maand; goldens
`jutterswet-sleepbaan`, `jutterswet-vergunningsduur`) — as do the
FACTOR-FREE cases: a unitless operand times a unit-carrying one (the
product adopts the unit, value unchanged) and a unitless side adopting a
restating sibling's unit (S6, golden `jutterswet-sleeprest`); negative operands
under non-richting-nul rounded forms (runtime `viol/2` fences);
non-exact bare division and non-fitting write results (runtime
`viol/2` — an explicit rounding is mandated before precision is
lost) — inside the write of the unbounded form (S2) both name that
form instead, `result of <target> is not a whole number, and this
encoding holds a number declared without decimals (the unbounded form)
as a whole number` and `a division into a number declared without
decimals is not whole, and this encoding holds the unbounded form as a
whole number`, since the spec mandates no rounding there and ref writes
the exact value (RUNTIME-SEMANTICS ch. 2.4 point 3); a non-whole case
input on that form (S2); a rational literal that no power-of-ten scale up to 10^9 holds
exactly (S4, `rational literal 1/3 has no finite decimal expansion
within 9 decimals`) — ref computes it exactly and both Drools branches
play it (measured 2026-09-26 on "zijn taxatie maal 1/3 naar beneden
afgerond op 2 decimalen" over a taxatie of 10: ref `oracle: PASS` on
3.33, the exec-model and the DRL-text branch `PLAYED 1`), while this
encoding holds a number only as an integer at a power-of-ten scale. And
one decided delta, executed 2026-10-19 in both executors at once:
ch. 1.4 point 4 routes numeric-zero division into the §9.5 channel in
v1 (goldens `division-by-numeric-zero`, `division-abs-by-numeric-zero`),
where it was the v0-mirror hard fault until then.

*Constructs and shapes without a corpus case (fault at translation):*
validity windows on the forms OUTSIDE the ch. 8.2 gate — allocation,
consistency checks, uniqueness and the uniqueness union (`… validity
windows are not translated (no corpus case)`; before the gate landed
for rules, defaults members and creations these forms silently ignored
their window, so the fault is new and load-bearing: an unwindowed
translation would apply outside its window) — while windows on rules,
defaults members and both creation forms now translate (S1's validity
gate, `jutterswet-vensterheffing`), including the time-free refusal
both executors share; temporal targets
outside the ch. 3.7/3.8 fold shapes; the ten-minste-1 composite union (v0
clips to the union of member regions; loud in both executors); the
static-comparison composite member (v0 partitions it into a
value-side guard; loud in both executors) and the nested composite
bullet (v0 markers it as Unresolved; loud in both executors);
gedurende condition shapes beyond WF-055; temporal
totals over case-supplied full timelines, and non-year total
granularity; rounded roots in term positions (`Clamp`
clip/min-max now translates — the integer min/max identities on the
harmonised scale, S6, `jutterswet-grensbedrag` — save a bound needing a
real unit conversion factor or a non-ground, possibly-empty bound, which
stay loud); a temporal attribute read in a scalar slot (ch. 3.3);
genuinely non-numeric arithmetic operands (text/enum/boolean, or a
date reaching the numeric path) — narrowed 2026-08-28 for the v0
**`Onbekend` typing-inability sentinel** (a composite unit the
frontend could not name, not an actual non-numeric value), which then
translated when the node was a PROVABLY-numeric arithmetic `Binary`
whose operands passed the numeric checks recursively; that arm was
retired again on 2026-08-29 with the unit-identity typing fix
(decisions 16Y and 17b), and since L2 `Onbekend` is no datatype at all (B16): a
composite whose `type` is `null` — the spelling an untypable derived
position has since L2 — faults here like any other non-numeric operand;
and narrowed
2026-08-25: the
**day-precision date family now translates** (date ± duration and
tijdsduur/absolute-tijdsduur in dagen/weken/maanden/kwartalen/jaren,
the S6a calendar chains; the day-precision part extractors and
eerste-paasdag joined 2026-08-25 — the shared era decomposition and
the Gauss chain, S6a, `jutterswet-paasvondst`, with eerste-paasdag's
own year-before-1 `viol/2` fence; the **datum-tijd millisecond layer**
joined 2026-08-28 — the **pair-term encoding** `dt(D, MS)` lifted the
former datum-tijd embargo, so datetime ± duration and sub-day/day-based
tijdsduur over datetimes now translate WITHOUT a datetime's absolute ms
value entering clingo's integer range whole, S6a, golden
`jutterswet-getijdenpaar` — the ch. 3.8 borrow corner as the differential
square, ref ≡ clingo) and, since 2026-09-19, the day-kind (dagsoort)
durations and tijdsduren (the extension table above, goldens
`jutterswet-bergingsklok` and `jutterswet-getijklok`, the latter over
datum-tijd operands); what stays loud in the date family is the day-kind
`duration` over DATUM-TIJD operands, the sub-day part extractors
(uur/minuut/seconde/milliseconde-uit, still under the catch-all;
outside ref too — the extractor slice is not the ms layer's
arithmetic), and a datum-tijd operand in an EQUALITY comparison
(the pair term carries the arithmetic, since 2026-08-28 the
eerste/laatste fold ordering and since 2026-09-06 the scalar ORDERING
and the datum-tijd LITERAL — S6a, golden `jutterswet-meldstip` — but not
the `eqN` harmonisation the equality row rides; no
corpus case) — plus a
**text/enum/boolean literal in
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
closed-world `has/2` read and a derived `val/3` would split — S3);
unscalable datatypes in the integer encoding (a
datatype-less *declaration* is since 2026-08-21 rejected by WF-059
with the shared loader refusing its input as backstop — the fault
here remains the last line for a **dateTime** reaching `expr_term`'s
scalar integer encoding (a slot OUTSIDE the pair-term arithmetic of
S6a: an equality comparison, or a datum-tijd operand reaching
`expr_term`'s numeric eerste/laatste arm, whose ms value has no bounded
scalar form; the tijdsduur, datetime ± duration arithmetic and the
eerste/laatste fold over datum-tijd operands ride `dt_term`'s pair term
instead, since 2026-08-28, and the scalar ORDERING plus the datum-tijd
literal joined them 2026-09-06 through the `dtcmpN` helper),
and for text/enum/boolean **parameters**, which ref executes but the
ASP parameter emission scales through `decimals_of_datatype` —
ref-only until a corpus case demands a symbolic parameter carriage,
and with a **millisecond-precision `Date` parameter** (v0 `DatumTijd`) as
their sibling: a declared datum-tijd parameter that the case SUPPLIES A
VALUE FOR faults in the
same parameter emission with the same words even when **no rule reads
it at all** (measured 2026-09-06: `verifier: PASS`, ref
`oracle: PASS (6 expectations)`, clingo
`TRANSLATION FAULT: unscalable datatype in the ASP translation`; drop
the case value from the very same pair and clingo replays all six).
So the emission scales what the case gives it, not what the rules use
— the declaration alone is free. **Consequence for the message:**
`unscalable datatype in the ASP translation` now has two senses, and
the report does not distinguish them — a datum-tijd reaching
`expr_term`'s scalar encoding (above) and a datum-tijd parameter
VALUE reaching the parameter emission (here). Named rather than
papered over: the two are found by where the fault fires, not by
what it says);
characteristic attribution with a non-waar value;
the allocation's remaining scale-shape fences — since 2026-08-25
all three methods, the voorwaarde and misaligned pot/target/cap/rest
scales translate (S12), and what stays loud is rounding declared
FINER than the target scale, a remainder declared coarser than the
pot or rounding scale, and an unrounded (exact-share) allocation
carrying a maximum — narrowed 2026-08-28: the **proportional** method
with a cap no finer than its target now translates, because a cap at
cdec <= tdec floors to an exact integer at the target scale, so a
clipped write always fits; the exactness viol then fires on the RAW
proportional share whenever it is inexact, independent of the cap, so
where the cap clips an inexact raw to a fitting integer clingo refuses
LOUDLY though ref writes the fitting cap and passes — clingo is
stricter in that corner, a conservative loud refusal, never a silent
floor (no corpus golden witnesses it); what stays loud is the ORDERED
method's exact+maximum and a cap FINER than its target (whether the
exact share or the clipped cap is the written minimum then decides
ref's write fault, no corpus case) — while the unrounded aligned form,
which used to floor silently where ref faults a non-fitting write, now
carries the runtime exactness viol instead of a silent divergence;
a som fallback other than 0 — a NON-zero `emptyFallback` literal (the
fallback-less som now translates via its presence guard, S8, and the
fallback-0 facet rides `#sum`'s native 0); uniqueness key
lists with more than one temporal component, and ever-unique keys
served by a scalar; case input on non-standing SCALAR and TEMPORAL
computed targets (the ch. 6d superset-model fence) — an ALLOCATION
target now enters defeasibly instead (S10/S12, `jutterswet-veilinginvoer`),
so what stays loud there is only the residual: a target attribute READ
anywhere else (a feedback into the pot, weights, criteria or condition,
which `not vdI_eff` cannot stratify) or WRITTEN by a second form (an
unpinned overwrite order); to-many navigation in scalar slots (WF-053);
bean-field-only fact creations (the symbolic ch. 3.4 form is what
executes); the expression/condition catch-alls ("not translated — no
corpus case exercises it") — narrowed 2026-08-25: the *scalar*
counting composites (`none` / `atLeast` / `atMost` / `exactly`, the
decision-table desugar's disjunctive condition kinds among them) now
translate through the cqN count encoding (S7:
`jutterswet-rubriektabel`, `jutterswet-vrijgavetoets`,
`jutterswet-sluimerwacht`, `jutterswet-tweevoudkeur`) and left this
catch-all, as did the articulated eerste/laatste fold (S8,
`jutterswet-peildatum`; minimum / maximum / eerste / laatste over the
SET-typed source now translate too — the `#min`/`#max` aggregation
arm, S8, `jutterswet-vondstpiek` and `jutterswet-vondstvenster` — while
minimum/maximum over LISTED operands stay under this catch-all: the
same listed-fold encoding would serve, no corpus case; the `count` set
source stayed under it too until L2, and from 2026-09-19 it was the one
entry in this inventory that would never leave the catch-all by being
translated — the maintainer took option (ii) of
`backlog/done/20260909-eerste-laatste-setvouw-kloof.md` §6 and the WIRE
refused the form (`IR-SPEC.md` WF-096, negatiefs
`wf096-aantal-setbron*`), so this backend's refusal, which was the
strictest of the three readings, is what the verifier came round to.
Since L2 the form has a named arm instead: WF-096 retired with the set
flag, and a count whose source names a member of the element type is
refused as WF-093's (`the source of this count aggregation reads '…', a
member of the element type '…'`; negative
`wf093-count-over-attribute-source`), on the same ground: a
count folds the contributors, and such a source hands it values). The LISTED
som left this catch-all on 2026-09-19 WITHOUT being translated: it has
a named arm of its own (`a sum over listed operands is not translated
…`) and is refused exactly as before. The reason it needed a name is
the catch-all's own sentence:
`corpus/red/sum-fallback-always-evaluated` fell here, so "no corpus
case exercises it" was false while that witness stood — the same
untruth WF-044's condition arm had just removed from the condition
half. The form is `sum` with an absent `elementType` AND an
absent `dimensionSelection`: the first rules out the instance folds
(every som arm this backend carries demands the stamp present), and the
second is needed because absence of the stamp alone means listed *or*
dimension cell — WF-037 exempts the dimension som from the arity rule,
and `ref` refuses that form on its own ground before asking where the
contributors come from. Its three boundaries — the kind, the stamp and
the dimension selection — are pinned in the backend's own suite and
cannot be pinned by the corpus: a red witness for any of them would
re-falsify the sentence by exercising the catch-all again. What keeps
the catch-all keeps it truthfully: `total`'s temporal subject-own arms
carry an absent `elementType`, so absence does not mean "listed"
there; the `count` listed form is no som; and no document in any zone
carries a `dimensionSelection` on a `sum` (measured 2026-09-19 over
corpus and parity, when the field was `dimensieSelectie` on a `Functie`,
and again 2026-09-25: the only carrier since is the negative witness
`wf037-filled-count-dimension-selection`, on a `filledCount` that WF-037
refuses);
what remains under it for composites is the member-less
counting list (`a counting composite without members is not
translated (no corpus case)`) and every member kind that keeps its
own fault (a TimeWindow member — the vormenkabinet's peiling rule, still
refused by both executors — an Unresolved bullet, a
comparison carrying a periode, the negated gevuld predicate; the role
check rides the S7 `role/3` join, above, with no to-many exception since
L2 — until L2 the near-side `Rollidmaatschap`'s **to-many connecting
reference** faulted loudly in both executors (`the rolcheck's
connecting reference to … is the to-many role …`; the red case
`near-side-role-check-to-many`, since L2 a golden, RUNTIME-SEMANTICS
ch. 5.8a)). The **day-kind (`dayKind`) predicate** — a
reference to a declared day kind used as a condition — is itself a loud
non-translation here: clingo reaches this condition catch-all and ref
refuses the predicate by name, so a reference to a day kind faults loudly
in both executors, never silently, even though a day kind's own definition
is well-formed (WF-031). Since 2026-09-19 that holds of the predicate as a
RULE condition only: INSIDE a day-kind condition the nested reference is
the §9.8 composition and both executors evaluate it, which is what WF-085's
reference graph is about.

*Sum types (S14) — carriage and shapes without a corpus case:*
**temporal sum carriage** is a recorded flank — the value model admits a
sum-typed temporal attribute and the case format can write one, but no
case exercises it, so the ASP case emission (and any sum scrutinee or
operand over an own temporal attribute) faults with the literal reason
`temporal sum carriage is a recorded flank; no corpus case` rather than
silently omitting the constructor-valued segments (RUNTIME-SEMANTICS
ch. 9.4). A **non-literal `Construct` payload** meets SIX distinct places
in the translator: THREE carry it — the positions that can hold the payload's
binding atoms — and THREE refuse it, each for a different reason. A SEVENTH
place, the fold-init source fence, refuses a shape that need not contain a
`Construct` at all; it is inventoried with them below because a fold `init`
reaches it and (iii) alike. Before 2026-09-03 this inventory described one of the
refusals and gave it a reason that was not the true one, and it counted the two
closed-body arms as one, so the boundary it stated was wrong in both
directions. Carried:

- **(i) at RULE level** an own-subject attribute read rides its `val` atom —
  the anchor's `herbeschikking` rule
  `val(S,herbeschikking,toegekend(V)) :- …, val(S,borg,V)`.
- **(ii) in a BRANCHING closed body** — a fold STEP, a function body — the
  payload goes through `body_branches` and EACH branch is wrapped in the
  constructor functor (the reduce lowering's `present(⟨body⟩)` step, above).
  This is the broadest of the three: four goldens' folds build a constructor
  value in the step — `jutterswet-penningstaffel-catala`,
  `jutterswet-kamerhuishouden-catala`, `jutterswet-meldplichtspoor`, and
  `jutterswet-meldtermijnen` itself (`Openstaand(⟨resterend⟩ - ⟨dagen⟩)` under
  an `IfThenElse`).
- **(iii) in a NON-branching closed-body term** the payload lowers RECURSIVELY
  through the closed-body grammar and its binding atoms travel out with the
  term, so the caller's rule body binds them. The pinned witness is a document
  parameter in a fold's `init` — golden `jutterswet-meldtermijnen`, rule
  `standaardpositie van een meldplicht`, whose fold base rule carries the
  payload's `param("standaardtermijn",P)` atom in its body; the same arm lowers
  any other closed-body payload shape, and those shapes have no corpus case.

(ii) and (iii) are separate arms in separate functions, and a fold STEP never
reaches (iii). That is why (iii) needed a witness of its own: while this
inventory merged the two, the step's arm passed for the init's and (iii) had no
artifact at all.

Refused, each with its own true reason:

- a **rule-level payload that is neither a literal nor an own-subject
  attribute read** — a document parameter read, a `Match`, a composite.
  No corpus case; the rule-level `Construct` arm carries only the two.
- a **`Construct` with a bound payload in a ground-term position** — a
  sum-typed comparison operand, or a sum-typed operand inside a closed
  body. Those positions return a term and NO body atoms, so a payload
  that must be bound has nowhere to hang its atom.
- an **attribute read at the LEAF of a closed-body term**, reached
  THROUGH forms the closed body does carry. `Openstaand(<attr> + 1)` as a
  fold `init` is not itself an attribute read, so it takes the closed
  route of (iii); the constructor arm lowers recursively, the additive arm
  lowers its operands, and the leaf asks for a subject in a scope that has
  none. The route between (i) and (iii) is chosen on the init's SHAPE — an
  attribute read, bare or directly under a constructor — and this refusal
  states that rule as its reason. Widening the shape test to "reads the
  subject anywhere" would only move forms from one loud refusal to
  another, except for the semantically inert ones (an unread `Let` around
  a carried init), which it would newly carry with no witness; the shape
  test is therefore deliberate. `ref` plays every such form; the carrier
  refuses it and names the boundary.

Beside those three stands a fourth refusal that is NOT about the payload: the
**fold-init source fence**. It fires on the init's SHAPE — an attribute read,
bare or directly under a constructor — so a BARE attribute read as init trips
it with no `Construct` anywhere in the document. A subject-dependent init takes
the rule-level route, and that route is sound only over a SUBJECT-KEYED source.
The fold's accumulator and result facts are keyed by the LIST ID alone. For an
own attribute list that id is `alist(S,"attr")` and the subject rides in it, so
the init read joins on the same `S` as the head — safe, and this is the carried
case (golden `jutterswet-meldtermijnen`: two duties, one register shape,
different own terms, opposite outcomes). For a list PARAMETER the id is
`plist(p)` and the subject drops out of the head: two subjects' init facts
would collide on that one key and the recursion would derive EVERY subject's
outcome for EVERY subject. Measured before the fence existed, on two instances
with terms 30 and 10: both came back carrying both outcomes. This is refused
because the answer would be WRONG, not because it would be missing. A DERIVED
source — a mapped list over an own attribute list — keeps the subject in its id
(`lm(alist(S,"attr"))`, `list_source`), so the collision this refusal describes
does not apply to it; the carrier nevertheless admits exactly ONE source shape,
and the refusal says so rather than claiming a collision that shape does not
have. A widening nothing asks for is a widening no gate can see.

All four refusals are unobservable from any corpus document (a
refusal has no golden), so each is pinned by a fault fixture asserting
its literal text in `backends/clingo/test/test_clingo.ml`, which gate 6
runs: a text that drifts or a refusal that quietly disappears fails the
suite. Note that empty propagation is NOT among the reasons — an empty
init derives no base fact and the whole fold is thereby empty, which is
the intended reading (RUNTIME-SEMANTICS ch. 10.1/10.2) and needs no extra
machinery; an earlier version of this inventory and of the reason string
claimed otherwise and was wrong. Also refused: a sum-typed equality operand other than an own-subject
attribute read or a `Construct`, and a match scrutinee other than an
own-subject attribute read or a `Fold` result (the reduce extraction —
navigated sum reads await a case); a
match arm whose result scale disagrees with the match's declared scale;
a constructor name that does not normalise to a valid ASP functor symbol
(`ctor_functor`, the sum-side quote backstop). A **scalar sum-typed
parameter** is ref-only: ref accepts and executes it, but the ASP case
emission scales every *scalar* parameter through `decimals_of_datatype`
and so faults on a `SumType` datatype with the literal reason
`unscalable datatype in the ASP translation` (a constructor value has no
integer encoding). *Scalar* is load-bearing and the word the struct
sibling below already uses: the case emission's parameter loop takes its
`ListType` branch FIRST and reifies a list parameter as `lpres`/`lelem`
through `element_scale_of` (S15), which carries a `SumType` element as a
ground constructor term at scale 0 — so a list-of-`SumType` parameter
never reaches the scalar scaling and translates. Witness:
`corpus/golden/jutterswet-meldplichtspoor`, whose `meldreeks` is a
`ListType` of a declared `SumType` and whose eight cases all replay
exactly through clingo. Two
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
translation is reached. The keuzetype family's **enumeration** match
translates at rule level (S14's enum arm) but **not inside a closed
lambda/function body**: a closed body has no subject to read the
scrutinee's `val/3` atom from and the S2 symbolic operand has no
closed-body carrier, so `body_branches` faults with `an enumeration match
inside a closed lambda/function body is not translated (the symbolic
operand has no closed-body carrier; no corpus case)` — verifier-clean
input (WF-063 admits it, WF-069 admits a `Match` there) that ref executes
and clingo refuses loudly; a named executor asymmetry, never a wrong
answer, lifted when a case wants it. At rule level the enum arm's carrier
is likewise **narrower than the sum arm's**, by the same pre-existing
`symbolic_term` fence: an enumeration match scrutinee is an own-subject
attribute read or a literal, so an **enumeration-typed parameter** as
scrutinee refuses loudly (`a text/enum/boolean operand here is an
own-subject attribute read or a literal (no corpus case for more)`) where
the sum route's `expr_term` fallback carries a `Fold`. No corpus case;
recorded here rather than fixed, because the fence predates the enum arm
and firing it is a loud refusal, never a wrong answer.

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
WF-064-ill-formed (`backlog/done/20260825-closed-body-enclosing-binder.md`);
until it merges the fence is the clean backstop in place of gringo's grounding
crash. A **named FunctionDef
as a `Fold`/`MapList` operand** is no longer a shape-without-a-case: it is
**WF-069-rejected** before translation (the operand MUST be an inline
`Lambda`; a named function reaches a fold through a lambda body that
`Invoke`s it — `wf069-named-function-operand`), so the clingo fault
(`a named FunctionDef as a fold operand is refused (the operand must be
an inline lambda)`) survives only as an unreachable backstop on
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
default inside an exception group** faults (`an initialisation default
inside an exception group is ill-formed`) — the initialisation and
ch. 7.8 input defaults stay root-only, and WF-075 rejects the shape first,
so the fault survives only as an unreachable backstop on WF-clean input; a
**default consequence scale** disagreeing with the target scale faults at
every depth (`default value scale … does not match target scale …`), an
exception group inheriting the root target's declared scale (RS ch. 7.9);
a validity window on a defaults group gates per top-level member since the
ch. 8.2 landing (S1's validity gate; an exception group carries no window
of its own in the IR, so nothing nested is silently gated). The
**shared non-trivial base under all members** stays a frontend refusal
(PHASE5C-TUTORIAL §4), never a clingo shape: a group-default carries a
multi-member base group here with no special encoding, so nothing new is
refused for it.

*The `Let`, guarded consequences and the split money
multiplication (S18) — shapes and fences without a corpus case:* a
**value-position `IfThenElse` condition** that is not a comparison
(`a value-position IfThenElse condition is a comparison over bound
operands (no corpus case for more)`) — a predicate or composite
condition has no corpus case. Its comparison operands may READ binders,
parameters and literals, and arithmetic combinators (`Binary`,
`Rounding`, `Clamp`, `TextComposition`) over those are admissible:
`value_condition` lowers each operand through `expr_term` and harmonises
the two operand scales exactly as the closed-body comparison route does,
so the operand backstop (`… must read only binders, parameters or
literals`) refuses only the attribute/field/characteristic reads
WF-077 already rejects before translation — an unreachable guard on
WF-clean input (the guard mirrors the verifier's `bound_only_expr`). A
**`Let` inside a closed body** (a fold, function or lambda body) **now
translates** — `closed_term`/`body_branches` inline the value and bind the
binder self-contained (S15, golden `jutterswet-kinderlet`); the only closed-body
`Let` reading refused is a binder bound **outside** the body (an ill-formed
shape both executors reject, the parallel verifier card's WF-064 gap). One
`Let`-body shape an executor carries only in the bare rule-value position still
faults elsewhere: a **`Let` whose
body is an `IfThenElse` outside the bare rule value** (a default, a
creation member initialisation, a sum- or struct-target value —
`expr_term` peels the `Let` but has no `If_then_else` arm) faults
`expression kind not translated (no corpus case exercises it)`. ref
carries it everywhere (ch. 4.10); on the ASP side only `value_branches`
reaches an `IfThenElse` at the rule value (a closed-body `IfThenElse` reducer
does lower — `body_branches`). **Negative operands
under the money multiplication** (ref rounds either sign; runtime `viol/2`, the
non-richting-nul rounding sibling — below four decimals this includes a
negative operand beside a negative or zero other operand, whose
non-negative product the S6 rounding carried at small magnitude; the
rate's sign is read before the rate is brought to scale 10⁴) and the three
split range fences (`M·Rh`, `M·Rl`, and a rate of one to three decimals
brought to scale 10⁴; runtime `viol/2` — the tutorial's magnitudes clear
them); a **money multiplication other than a 2-decimal money times a
4-decimal rate or, with both operands unit-free, a rate of one to three
decimals** — `Mult_mon_int`, a product below four decimals whose money or
rate carries a unit, and other scale pairs are not split, they ride the S6
multiplicative fence (the 46340 operand bound, which
faults at large magnitude) — while the split's own scale backstop (`the
split money multiplication expects a 2-decimal money and a rate of at most
4 decimals …`) is unreachable: `money_mult_shape` routes on the scales the
translation derives for the terms, the same scales the backstop tests. The
negative-operand fences of the rounding, the rounded division and the split
read the sign where no multiplication or sum can have wrapped it: a
product's sign from its operands (negative where their signs differ); a
sum's from its value where no range fence of the subject has fired, and
from its operands where their signs agree; a division's from its dividend
and divisor before their pre-scaling; and through a coalesce or `Let`
binding from atoms of that binding's own rule. A term past its range fence
therefore meets that fence alone, and a negative product or sum that wraps
to a positive term is still named. The strict
`Let` itself has no fault of its own: a leeg value is underivability, not
a fault (the S18 coincidence). It does, however, **materialise its binder
at a finite integer scale** (the value's out-scale, RUNTIME-SEMANTICS
ch. 2.4), and that is where the afrondingsnorm (RUNTIME-SEMANTICS ch. 2.9,
vraag 3-A: intermediate values are exact-rational, the scale binds
declarations and write boundaries, never a step) becomes a **loud backend
limit**. The minimised synthesis find: a `Let` binding the
**non-terminating** quotient `100 ÷ 3 = 33,333…`, read at an endpoint
`Rounding` to 2 decimals, `halfAwayFromZero`. ref carries the exact rational through the
`Let` and the single endpoint `Rounding` collapses it once to 33,33; the
ASP side translates the binder's value as a **bare** division (the
`Rounding`-over-division fold of this section fires only when the round
wraps the division *directly*, not through a binder read), whose
`non-exact division (an explicit rounding is mandated before precision is
lost)` fence trips because `100/3` terminates at **no** finite scale. So
clingo, which cannot represent an exact-rational intermediate, refuses the
form loudly — a documented limit **conform to the norm**: the two
executors diverge here in *expressiveness*, never in *value*, the
exact-to-the-rounding-point rule made visible on the backend that must
round every step.

*Initialisations outside the desugaring domain (S3) — the shapes the
native arm has no guard for:* **two initialisations that write one
concrete type as rules** (`… initialisations write "a": the first to
fire writes and the others find the target filled, an order among
writers that no guard on the fact base reproduces (no corpus case)`) —
WF-079's initialisation arm refuses such a pair first, flat and through
the chain, so on verifier-clean input the fault is a backstop, while two
initialisations the fold takes into one stack are no pair (the stack's
block order ranks them, RS ch. 7.10); an initialisation beside **two
computing rules** on its target (`the initialisation of "a" sits beside
… computing rules on its target: their v0 meaning is the order in which
they fire (no corpus case)`) — a guarded case split WF-079 admits,
whose firing order this arm has no guard for; an initialisation beside
**an allocation, a creation-time write or a defaults group of its own
level** (`the initialisation of "a" shares its target with an
allocation, an object creation or a defaults group of its own level:
ch. 5.4 places an initialisation beside ordinary rules only (no corpus
case)`) — the last is WF-050's single-writer lift, refused first, and a
defaults group on another level of the chain is no co-writer because the
fold takes the initialisation into that stack; a target that is
**neither a number nor a date** (`the initialisation of "a" writes a
target that is neither a number nor a date, and only those are
translated here (no corpus case)`) — text, enumeration, boolean, sum,
struct and date-time targets, which ref replays; and a **temporal
target**, refused in ref's words (`temporal attribute "a" as a target:
temporal writes are not implemented`).

*Environment and input fences:* a **control character < 0x20** in a
name or label — the one character class the ASP string encoding cannot
escape (an embedded double-quote or backslash now escapes to
backslash-quote / doubled-backslash, S2; WF-057's backstop, sole cover
for case labels); values that do
not fit their declared decimals or exceed 2^31−1; scales beyond 10^9;
unknown object types, attributes, parameters; malformed or missing
`asOfDate` under evaluationDate; granular parameters in scalar slots and
their timelines missing from the case.

Gate 8 keeps the observable-fault subset honest
(`corpus/red/`), and the catch-alls guarantee the list can only
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
   ch. 2.6, WF-047/048) and `backends/clingo` reads the `role/3` join
   role from it; until L2 it derived the role for bindingless v0
   dumps, and since L2 the binding is required and nothing is derived
   (B12).
2. **The top-level-only restriction is a Drools artefact** (row 5,
   confirmation). The existential lives in any rule body at any depth.
3. **The single-layer default is one NAF over a declared contrary**
   (rows 7/8). No priority or salience mechanism appears anywhere; the
   only ordering notion is stratification, which here holds because the
   contrary is an input. When contraries become *derived* (the art. 4
   defaults of wet-v2), layering will need the standard
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

## 6. Hand exercise 2: the art. 4 exception defaults

Source: `corpus/jutterswet/wet-v2.md` — art. 3 (penningrecht), art. 4
(uitsluiting, gelaagd: "dit geldt niet indien …; dat laatste geldt weer
niet indien …"), art. 4bis (schorsing). The test case is the Marrit
dossier (`corpus/jutterswet/dossiers.md`) — the only dossier that walks
the whole chain — plus four synthetic single-difference variants. In IR
terms this exercises:

| IR construct | Where it appears here | `CONSTRUCTS.md` row |
|---|---|---|
| Negation over a derived characteristic | "recht" negatively depends on the derived "uitgesloten" | 6 |
| Exception layering (exception-on-exception) | the art. 4 defaults themselves | 8 |
| Stratification as a WF condition | the four-stratum negative dependency chain below | 6 |
| Parallel exclusion grounds | art. 4bis "evenmin" next to the art. 4 defaults | 8, 19 |
| Numeric comparison in a condition | "ten minste twaalf vondsten" | 1 |

### 6.1 The translation, construct by construct

- **The defaults are layered negation-as-failure over *derived* exception
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
- **The compiled representation** flattens the defaults into classical
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
- **Art. 4bis is a parallel ground, not a default.** "Evenmin"
  attaches a second, independent exclusion next to art. 4; it is
  translated as a separate `not geschorst_raapvergunning` in the art. 3
  rule, deliberately *not* as a case of `uitgesloten` — a suspension is
  not subject to the herroeping defaults.
- **"Ten minste twaalf vondsten"** is a plain arithmetic guard
  (`N >= 12`); the art. 5 hardheid variant (twaalf → zes) is out of
  scope here (ch. 7).

### 6.2 The programs

`defaults.lp` — the layered representation:

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
% De gecompileerde lezing van art. 4: de defaults platgerekend tot twee
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

`varianten.lp` — the defaults, default by default:

```prolog
% De defaults laagsgewijs: vier synthetische varianten van hetzelfde dossier.
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

**Run 1 — Marrit:** `clingo defaults.lp marrit.lp`

```
Answer: 1
recht_op_penning(marrit)
SATISFIABLE
```

Recht — the weigering would exclude, the herroeping lifts the exclusion,
and of verval nothing is established: the layer-1 exception stands. The
v0 statute (flat art. 4) would have excluded her; the layered v2 text
exists to decide this dossier correctly, and does.

**Run 2 — the defaults, default by default:** `clingo defaults.lp varianten.lp`

```
Answer: 1
recht_op_penning(wh) uitgesloten(w) uitgesloten(whv)
SATISFIABLE
```

`w` (weigering alone) → uitgesloten, no recht; `wh` (herroepen) →
recht; `whv` (herroeping vervallen) → uitgesloten again, no recht; `s`
(geschorst, art. 4bis) → no recht *without* being `uitgesloten` — the
parallel ground blocks art. 3 directly and never touches the defaults.

**Run 3 — the difference test:**
`clingo defaults.lp gecompileerd.lp marrit.lp varianten.lp`

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
3. **What the defaults demand of the Phase-4 default model** (row 8): a
   priority *chain* (rule < exception < exception-on-exception), not
   just pairwise defaults — Catala's exception trees express exactly
   this nesting, and the stable-model reading here gives the same
   answers. No conflict arises anywhere in the chain (the layers are
   linearly ordered), so the §9.5 inconsistency channel is not
   exercised; over-determination needs *unordered* simultaneous
   exceptions, which this statute deliberately avoids.
4. **Parallel grounds compose by conjunction of NAFs** (rows 8/19).
   "Evenmin" (art. 4bis) is a second independent blocker, not one of the
   defaults: `not uitgesloten, not geschorst`. Keeping it out of
   `uitgesloten` preserves the defaults' meaning (a suspension cannot be
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
inconsistent("<rule name>",S) :- <full body>, (R) == 0.
```

An empty dividend collapses to 0 before the divisor is read (also under
a zero or empty divisor — ref's match order); an empty divisor under a
filled dividend deposits the §9.5 channel fact — keyed by the ENCLOSING
RULE'S NAME, exactly as ref keys its channel (harmonised 2026-08-20 so
the case format's rule pin reads both executors identically; the reason
text stays executor-internal) — and derives no value
(leeg as absence); a numeric zero divisor under a filled dividend does
the same (golden `corpus/golden/division-by-numeric-zero`; until
2026-10-19 a `viol/2` witness, UNSAT where ref hard-faulted). Each
`inconsistent/2` rule has an `inconsistent_code/3` twin with the same
body that carries the code, EF-031 and EF-005. Inside a fold filter, a
quantifier element or a closed body the deposit is keyed by the subject
of the form whose evaluation reaches it, joined through its reach, as ref
keys it (since 2026-10-19; before, by the member). The true quotient
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
rule-keyed channel fact beside an absent value; numeric zero → the
same, with its own code). Anchored absolutely by
`corpus/golden/anchor/jutterswet-taxatie` (the RS ch. 2.2 example row
on both signs — 10 ÷abs 3 = 3,33333 and −10 ÷abs 3 = −3,33333 — plus
the exact and empty corners) and by the golden
`corpus/golden/division-abs-by-numeric-zero` in both executors (a red
case until 2026-10-19).

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
unit-compatibility clause skipped Verminderd_met entirely (negative
`wf035-reduced-with-incompatible-units` pins the rejection; v0's
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
machinery (Phase 4's default calculus is the principled home; defaults
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
the defaults anchors (S10), the temporalisation in the timeline
machinery (S9, historically ch. 6d), the quantitative corpus
throughout.*
