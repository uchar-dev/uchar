# PHASE4-DEFAULTS — completing the default calculus

*Status: PROPOSAL · 2026-08-20 · working prose, not normative. Nothing in
this document changes `IR-SPEC.md`, `RUNTIME-SEMANTICS.md`, the schema, the
verifier, an executor, or the corpus. It maps the landed Phase-4 machinery
onto Catala's default calculus with paper-precise citations, designs the
remaining Phase-4 arc, and ends in §10 with numbered decisions for the
maintainer. Adoption of any part is the maintainer's call; each adopted
part lands through its own card (protected-path changes as proposal
branches per `WAY-OF-WORKING.md` §3). This file lives outside every gate-1
zone and may therefore name the private toolchain; none of its text may be
folded into a zoned document without stripping those references first.*

*Primary external reference: V. Merigoux, N. Chataing, J. Protzenko,
"Catala: A Programming Language for the Law", ICFP 2021 — "the paper"
below. Figure and rule names follow the paper's own numbering (Fig. 1–12,
reduction rules D-\*, compilation rules C-\*).*

## 1. Where Phase 4 stands

The default calculus is not a future design — its core is landed and
machine-checked. `RUNTIME-SEMANTICS.md` ch. 7 declares both target kinds,
the declared-unordered group, and the v0 desugaring complete; the wire
form is `defaultLadders` (`IR-SPEC.md` ch. 6.1, an extension form, never
emitter-produced), guarded by WF-050 (shape, single writer, initialisation
rung lowest, group multiplicity only declared-unordered) and WF-051
(rung reads are negative edges; no cycle through a ladder). Four anchors
pin it through both executors:

| Anchor | What it pins |
|---|---|
| `jutterswet-uitsluiting` | the art. 4 characteristic ladder (worked example 7.6.1) |
| `jutterswet-vondstwaardering` | the attribute ladder: leeg fall-through, the initialisation rung, input left standing (`case5-input-blijft`), a deciding rung overwriting input (`case6-rung-overschrijft`) |
| `jutterswet-toelagegronden` | the declared-unordered group: differing decisions → no write + one §9.5 channel fact, equal decisions → one write, input kept standing under a conflict |
| `jutterswet-waardering-v0vorm` | the ch. 7.7 v0 desugaring, witnessed differentially: `ref/` replays the v0 forms natively, the ASP backend the desugared ladder |

Both executor encodings exist: `ref/`'s `run_ladder_group` (speculative
rung evaluation, fall-through, over-determination as one channel fact and
no write) and the ASP backend's `decides_m` lowering (rung outcomes
guarded by `not decides_m` for every higher rung; the defeasible input as
`inputval/3`, standing unless the group effectively writes). Stratification
already extends to displacement: every rung-justification and
attribute-consequence read is a `Dep_neg` edge in the shared dependency
graph (`spec/verifier/lib/checks.ml` `dep_forms`, the same graph `ref/`
derives its strata from — the two executors cannot drift there).

What remains of Phase 4 is therefore narrow and precise:

1. **Input-as-default.** Case input on a computed target is handled
   correctly only on ladder targets (`inputval/3`); on every other
   Berekening/Gelijkstelling target the ASP backend refuses it loudly
   (`ASP-TRANSLATION.md` ch. 6d, the computed-target input fence) and
   `ref/` overwrites v0-faithfully — and on allocation targets it is
   today neither handled nor fenced, a latent gap §4.4 records. §3–§4.
2. **The Drools lowering** — the one unmet exit criterion of `PLAN.md`
   Phase 4: "the salience mechanism in the Drools backend is generated
   from IR-level defaults". §5.
3. **Rule-readability of the conflict fact** — the single OPEN item
   ch. 7 itself carries. §6.
4. **Exception-tree expressibility** — the recorded Phase-5 reconciliation
   obligation, whose design groundwork belongs here. §7.

## 2. Fidelity: the landed ladder against the paper's calculus

### 2.1 The term-by-term map

The paper's core term (Fig. 3) is the default
`⟨ e₁, …, eₙ | e_just :- e_cons ⟩`: an exception list *left* of the bar, a
justification, a consequence. Evaluation (Fig. 6) is exceptions-first:
all exceptions evaluate; two or more non-empty → `⊛` (ConflictError,
D-DefaultExceptionsConflict); exactly one non-empty → its value
(D-DefaultOneException); none → the justification decides between the
consequence (D-DefaultTrueNoExceptions) and `∅` (EmptyError,
D-DefaultFalseNoExceptions). Priority is **purely structural** — an
exception is a default nested in its parent's exception slot; the paper
explicitly dropped the run-time pre-order `⩽` of prioritised default
logic and calls that the key simplification (§4.4). The surface
desugaring (Table 1, Fig. 2, rule D-Label) folds scattered definitions
and `exception` declarations into **one default tree per scope variable**.

| Catala (paper) | uchar (landed) | Where |
|---|---|---|
| default `⟨ e⃗ \| e_just :- e_cons ⟩` | the rung: (justification, consequence) | RS ch. 7.1; IS ch. 6.1 |
| exception nesting (structural priority) | rung order in one ladder — descending, highest first; no numeric priority anywhere | IS ch. 6.1 `rungs`; RS ch. 7.4 ("no salience-like escape hatch") |
| one default tree per scope variable (D-Label) | one ladder (or one declared-unordered group) per target — WF-050's single-writer lift of WF-026 | RS ch. 7.1; IS ch. 6.1 |
| `∅` (EmptyError): justification false → no value; catchable at the enclosing default | fall-through: a justified rung whose consequence evaluates leeg falls through; an undecided ladder writes nothing; under-determination is ordinary leeg | RS ch. 7.1/7.2 (tracked deviation, `CONSTRUCTS.md` row 8) |
| `⊛` (ConflictError): ≥ 2 non-empty exceptions, fatal under any context (D-ContextConflictError) | over-determination in a declared-unordered group: no write + one §9.5 channel fact, evaluation continues | RS ch. 7.2, ch. 5.6 |
| runtime, data-dependent conflict (paper §3.4: statically accepted, aborts at run-time) | same: the conflict is decided at the fixpoint on concrete facts, never statically | RS ch. 7.2 |
| topological sort rejects dependency cycles | WF-045/046/051: strata must exist; no cycle through a ladder | RS ch. 6, 7.4 |
| caller override of a context variable: C-Def compiles the caller's thunked value as the **single highest exception**, `let ℓ = ⟨ ℓ() \| true :- e ⟩` | case input is **not** a rung: it is the fact base the ladder's write meets; a deciding rung overwrites it | RS ch. 7.1; §4 below |

Metatheory, stated carefully: the paper mechanizes (in F*, not Coq) only
the final compilation step, default calculus → lambda calculus with
exceptions, proving type preservation and a convergence-style simulation
(diagram B of Fig. 11 — the textbook forward simulation provably fails
because exceptions compile to thunks). The scope-to-default translation
and the surface desugaring are not mechanized. We inherit the design, not
the proof; our substitute for the proof is the differential square over
the anchors.

### 2.2 Tracked deviations — two of them not yet named anywhere

Recorded already (`CONSTRUCTS.md` row 8; RS ch. 7.2): Catala's catchable
`∅` maps to ordinary leeg — within a ladder the fallback survives
structurally as fall-through, across compositions a Catala frontend must
compile enclosing-default fallback into ladder structure (the Phase-5
obligation §7 prepares).

Two further deviations exist in the landed semantics and are **not yet in
the row-8 delta record**; this proposal asks to track them (decision 5):

- **Equal decisions are no conflict.** The paper's
  D-DefaultExceptionsConflict fires on *any* two non-∅ exceptions —
  value equality does not save them. RS ch. 7.2 deliberately deviates:
  "Equal decisions are no conflict (idempotence, ch. 5.1)" — pinned by
  `jutterswet-toelagegronden/case4-gelijk`. The deviation is right for
  us (the ch. 5.1 equation reading makes agreeing writers idempotent,
  and legal grounds that agree are not in samenloop), but it is a
  deviation and belongs in the delta column.
- **The conflict is observable, not fatal.** The paper's `⊛` aborts under
  any context (D-ContextConflictError); ours deposits one channel fact
  and the computation degrades locally (RS ch. 5.6: "facts on a channel,
  not exceptions"). This is forced by two of our own invariants: the
  fixpoint session reports end-state observations, and the ASP anchor
  cannot express "no stable model" as a checkable expectation
  (`backends/clingo/lib/model.ml` demands SATISFIABLE with exactly one
  model; UNSAT is reserved for hard faults and the red corpus). The
  fatality difference matters for frontend work: a Catala program that
  *relies* on ⊛ aborting has no direct image here.

Not a deviation, but a polarity to keep straight (it drives §4): Catala's
"input as default" runs the **opposite direction** from ours. There, a
caller-supplied value is the *highest* exception (C-Def) — input beats
the scope's own definition. Here, a deciding rung *overwrites* case input
(`case6-rung-overschrijft`) — computation beats input, and input beats
only the initialisation rung. No contradiction: Catala's mechanism serves
**scope composition** (one scope invoking another, Phase-5 territory,
`CONSTRUCTS.md` row 9), ours serves **the fact base** (a case dossier the
rules compute over). The two will meet at Phase 5 and must not be
conflated before then.

## 3. The three findings that point at the remaining arc

All three come out of the adversarial rounds and the gate set — the
machinery telling us where the calculus is incomplete.

**(a) The computed-target input fence** (`ASP-TRANSLATION.md` ch. 6d;
`backends/clingo/lib/translate.ml` `case_supplies`). `ref/` — like v0 —
lets a computed target overwrite a case-supplied value or timeline
(`write_attr`/`write_timeline` replace); a monotone ASP program cannot
retract, so case fact and rule head would union into a superset model.
The translator refuses loudly whenever the case supplies input for a
Berekening/Gelijkstelling target. Ch. 6d names the principled home
itself: "overwrite semantics have no monotone encoding without the
defeasibility machinery (Phase 4's default calculus is the principled
home; ladder targets already carry it via `inputval/3`)."

**(b) The conditional-rule-plus-input flank.** The fence is
condition-blind: a rule whose condition is *false* over a case-supplied
target is refused by the ASP backend even though `ref/` executes it
perfectly well (no write; the input stands). The semantically
interesting case — a rule that *would* overwrite input but doesn't apply
to this subject — cannot enter the differential corpus at all. That is a
coverage hole exactly where legal defaults live.

**(c) The ladder's `inputval` already is the answer, locally.** On ladder
targets the case value enters as `inputval/3` and stands by
negation-as-failure unless the group effectively writes — a monotone
encoding of overwrite, differentially witnessed. Gate 7's survivor list
pins the semantics from the other side: `case5-input-blijft` (a
non-deciding ladder leaves the input standing) and the v0-form
`case3-input-blijft` (an initialisation leaves a filled input standing)
legitimately satisfy a do-nothing executor — defeasible standing *is*
what those cases test. The machinery exists; it is merely confined to
declared ladders.

## 4. Proposal A — the input rung: case input as a declared default

### 4.1 The two readings

A case input on a computed target is a default. The question is its
priority:

- **Reading A — computation beats input** (v0, `ref/`, and the landed
  ladder): a deciding writer displaces the input value; the input stands
  only when nothing decides; the initialisation fills only when even the
  input is absent. Priority order, top to bottom: ordinary rungs —
  input — initialisation rung.
- **Reading B — input beats computation** (Catala's C-Def): the supplied
  value is the highest-priority exception; the computation is the
  fallback for absent data.

This proposal recommends **Reading A as the v1 norm** (decision 1). The
grounds: it is what the anchors already pin (`case5`/`case6`); a case in
this corpus is a *dossier* — observed facts the rules compute over, not
an authoritative override of the law's own arithmetic; and Reading B's
proper home is the Phase-5 scope call, where the "caller" is another rule
context rather than a fact dossier. Reading B additionally cannot be
retrofitted per-target without new wire surface, while Reading A needs
none (§8).

### 4.2 The input rung, formalized

Restate RS ch. 7.1's "input facts are not a rung: they are the fact base"
as an equivalent, fully uniform reading — a **virtual input rung**:

> Every attribute ladder carries, between its lowest ordinary rung and
> its initialisation rung (when present), an implicit rung whose
> justification is "the target is **gevuld** in the input facts" and
> whose consequence is that input value.

This is the exact dual of the landed initialisation rung (justification:
input-emptiness — IS ch. 6.1). Like the initialisation rung's, its
justification reads the *input world*, not the evolving world, so it
contributes no dependency edges and cannot create a WF-051 cycle. It is
purely a re-description: with it, every landed outcome of
ch. 7.1/7.3/7.6.2 derives from one rule — "the highest rung that decides
wins" — with no side-text about fact bases:

| Case (anchor) | Derivation with the input rung |
|---|---|
| deciding rung over input 40 → computed (`case6`) | an ordinary rung decides above the input rung |
| no rung decides, input 40 → 40 (`case5`) | the input rung decides (gevuld) |
| no rung decides, no input, initialisation 25 → 25 (`case4`) | input rung falls through (leeg justification fails), initialisation decides |
| computed leeg, no input, initialisation 25 → 25 (`case3-doorval-init`) | ordinary rung falls through, input rung falls through, initialisation decides |

The virtue is not elegance but **generalization**: the reading extends
verbatim to targets that are not declared ladders.

### 4.3 Generalization: every computing target reads as a ladder

Extend the ch. 7.7 desugaring domain: a **bare version-free computing
rule** (Gelijkstelling/Berekening, with or without a beside-standing
initialisation) on one (type, attribute) target reads as a ladder — one
ordinary rung (justification: its condition, vacuously true when
unconditional; consequence: its value), the input rung, and the
initialisation rung when the v0 Initialisatie form is present. The
ch. 7.7 exclusions stay: conditional initialisations, versioned forms
(temporal, never defeasible — RS ch. 7.4), multiple computing rules on
one target (their v0 meaning is agenda order; ordering needs authorial
intent).

The semantic consequence, and the one behavioural delta with the v0
mirror, sits in a single corner: **a computed leeg over an
input-supplied value**. v0 (ch. 1.2g) writes the leeg and erases the
input; the ladder reading falls through and the input stands. Ch. 7.7
already pinned exactly this delta for the initialisation-paired case and
excluded the bare case *because* desugaring it would silently change its
meaning. This proposal asks to change it **openly** (decision 2): the v1
norm becomes

> a computed leeg never displaces an input-supplied value,

with the v0 behaviour recorded as a tracked delta in `CONSTRUCTS.md`
rows 8/18. Three arguments — stated honestly, because the verification
round corrected the first: once `inputval/3` lands, **both** readings
are monotonically encodable (§4.4: the effectiveness atom's body *is*
the choice — value bindings give standing, condition-only gives v0's
erase), so ch. 6d's "no monotone encoding" holds only for the legacy
`val/3` scheme and decision 2 is a genuinely normative choice, not one
forced by ASP. The grounds, then. First, **uniformity**: the ladder
already norms fall-through (RS ch. 7.1) — one calculus should not carry
two leeg-disciplines, displacing on declared ladders and erasing on bare
targets; and in the confluent domain (the rule evaluates gevuld or does
not fire) the two readings agree extensionally, so the norm decides only
the corner. Second, the neighbouring case is already fault-domain:
beside an initialisation, the v0 reading of a leeg-evaluating computation
has no fixpoint at all (`ref/` faults on the pass budget — RS ch. 7.7);
v1 would be *defining* behaviour where v0 diverges or is silent, not
contradicting settled semantics. Third, the legal reading: a computation
that produces no value should not silently destroy an observed fact —
absence of a derivation is not a derivation of absence (the same
principle that keeps a do-not-attribute decision a non-derivation,
RS ch. 7.1).

### 4.4 The ASP encoding, sketched

Generalize the existing `inputval/3` pattern from declared-ladder targets
to every desugarable computing target. The case emits `inputval/3`
instead of `val/3` for any attribute some rule writes; per computing rule
`r` on target `a`:

```
r_out(S,V) :- is_a(S,"t"), <condition atoms>, <value bindings V>.
r_eff(S)   :- is_a(S,"t"), <condition atoms>, <value bindings V>.
val(S,"a",V) :- r_out(S,V).
val(S,"a",V) :- inputval(S,"a",V), not r_eff(S).   % the input rung
```

The load-bearing subtlety: the body of `r_eff` **is the semantic
choice**. With the **value bindings** in it — "decides" as
justification ∧ gevuld, the ladder's `decides_m` shape (RS ch. 7.5) — a
leeg-evaluating consequence leaves `r_eff` underivable (absence
composes, exactly the ch. 6a coalescing machinery) and the input stands:
the fall-through norm of §4.3. With only the **condition** in it, a leeg
computation still derives `r_eff`, the input rung is blocked, no `val`
derives, and the target is leeg — which is precisely v0's erase-on-leeg
(ch. 1.2g), monotonically encoded. Both readings are therefore
expressible once `inputval/3` is general; ch. 6d's impossibility is a
statement about the legacy `val/3` scheme, and decision 2 picks the
value-binding body on the §4.3 grounds, not out of necessity. Timelines
take the same shape over `tl/5` with a per-rule effectiveness atom; the
initialisation rung stays NAF over the input fact as in RS ch. 7.5.

The fence (`case_supplies`) then **retires for desugarable targets**;
what stays fenced is the still-non-desugarable computing forms (multiple
computing rules on one target, versioned forms, conditional
initialisations). Two corrections from this card's own verification
round, recorded here deliberately: (i) the fence never covered
allocation — it is called only from the Berekening/Gelijkstelling arms,
so case input on an allocation `doelAttribuut` is today emitted as a
plain `val/3` fact beside the allocation's own head: an **unfenced
latent superset gap**, to be closed by the §9 executor card with a fence
of its own (an addition, not a narrowing); (ii) creation-initialised
members need no fence — minted instances live in the reserved `::`
namespace (RS ch. 5.5), so no case-supplied fact can ever collide with
them. Stratification is untouched: the new NAF is against `r_eff`, whose
body is the rule's own — no new edges beyond the WF-051 classes the
ladder already added.

### 4.5 The ref encoding, sketched

Two implementation routes, one semantics:

- **Desugar-route (recommended):** extend `Ir_v0.Desugar` to the bare
  computing rule and let `ref/` replay desugarable targets through
  `run_ladder_group` — one machinery, the fall-through and standing
  semantics already tested. The cost: `ref/` today replays v0 forms
  natively (the gate-6 differential deliberately witnesses the lowering
  by running desugared-vs-native); extending the desugar domain moves
  that witness line.
- **Write-path route:** keep native replay and add the one check in the
  computing-rule arm: a leeg-evaluating write over an input-supplied
  target is a no-op. Smaller diff, but a second encoding of the same
  semantics beside `run_ladder_group` — the kind of duplication ch. 7
  exists to remove.

Either way the change is executor work under a follow-up card with
red-first demonstration; the fixpoint model, `write_attr`'s idempotence
(`Arith.equal` old/new), and the pass ordering stay as they are.

### 4.6 What it retires, what it admits

Retired: the ch. 6d fence for desugarable targets; the recorded loud
asymmetry ("ref executes such cases, this backend refuses them, so they
cannot enter the differential corpus").

Admitted into the differential corpus, at last: (i) the
conditional-rule-plus-input flank — condition false, input stands, in
both executors; (ii) computed-over-input on a bare target — the overwrite
case, now monotonically encoded; (iii) the delta-pinning golden —
computed leeg over input, the input standing, which today no case can pin
(gate-6 refusal on one side, and no expectation could pin the v0 fault on
the other). Case (i) survives a no-op run by construction — the standing
*is* what it tests — so it joins gate 7's reason-carrying survivor list,
which is the sanctioned place for exactly this (`check-noop.sh`).

## 5. Proposal B — the Drools lowering: the unmet exit criterion

`PLAN.md` Phase 4 exits only when "the salience mechanism in the Drools
backend is generated from IR-level defaults". Precisely what the
toolchain does today (both Drools routes, deliberately identical):

- **Salience −1** is a hard-coded syntactic heuristic: a rule whose
  voorwaarde reads a *negated kenmerk* (`leestNegatieKenmerk`,
  `DrlTranspiler`/`ExecModelTranspiler`) sinks to the agenda bottom. It
  is the only non-default salience in the system — there is no priority
  lattice, no agenda groups. This is negation-stratification (our
  WF-045/046), not defaults.
- **Initialisatie** is an LHS `== null` guard plus the ordinary setter —
  operationally the ch. 5.4 semantics, i.e. exactly the initialisation
  rung's lowering (RS ch. 7.5 row 4 names it: "the empty-guard of
  ch. 5.4"). It is conformant behaviour that is not yet *derived from*
  the declared rung.
- **Conflicting ordinary writers are silently unarbitrated**: two rules
  writing different values ping-pong under the idempotence guard until
  the `fireAllRules` cap, with no Inconsistentie deposited —
  "one-writer-per-attribute" is a stated assumption, not an invariant.
  RS ch. 7.5 row 5 specifies what the ladder replaces this with (§9.5
  channel insert, no write) — "silent last-writer-wins" retired.
- The beslistabel ladder is emergent: per-row guarded rules with no
  default branch; standing input is the absence of a write.

The criterion cannot be met wholesale today because no frontend produces
ladders (`defaultLadders` is never emitter-produced) and RegelSpraak has
no ladder authoring surface. The honest, staged path (decision 6):

1. **The derivation card (pin-bump, satisfies the criterion on the
   v0-expressible fragment):** the Drools backends compute the WF-045/046
   dependency graph (RS ch. 6 — the same graph both our executors share)
   and *derive* the salience from declared strata instead of the
   `leestNegatieKenmerk` heuristic; behaviour-identical where the
   heuristic applies, and correct where it under-approximates (the
   heuristic sees only directly-negated kenmerk reads; the graph also
   carries the regelversie channel test and, later, ladder edges). The
   initialisation `== null` guard is documented and conformance-tested as
   the derived lowering of the ch. 7.3 rung. Deliverable: same emitted
   behaviour, mechanism now derived — which is exactly what the exit
   criterion asks ("becomes a derivable lowering of a specified
   requirement instead of the bearer of it", PLAN §6).
2. **Full ladder service in Drools** (generated displacement guards from
   the higher rungs' decides-conditions, the group's channel insert)
   waits until a frontend emits ladders — realistically the Phase-5
   Catala plugin. Building it against hand-authored IR earlier is
   possible but tests no production path.

Note for the pin-bump after decision 2 (§4.3): the computed-leeg-over-input
delta then also separates v1 from the toolchain's v0 behaviour
(an unguarded Drools modify writes the null and erases the input). That
is a *tracked* delta like any other in `CONSTRUCTS.md` — the toolchain
mirrors v0 until its own conformance card; nothing breaks meanwhile
because no shared corpus case can exercise the corner (the fence blocks
it today; after adoption the new goldens pin the v1 side only).

## 6. Proposal C — close conflict-fact readability for v1

The one OPEN item ch. 7 carries: may a rule *read* the §9.5 conflict fact
a declared-unordered group deposits? Recommendation (decision 4):
**close it — not readable in v1.**

- **Fidelity:** the paper makes `⊛` deliberately uncatchable
  (D-ContextConflictError — fatal under any context). A readable conflict
  fact would make our conflicts *more* catchable than Catala's, in the
  direction of meta-defeasible rules ("if conflicted, then …") that the
  adopted model rejects by design. We already deviate by keeping the
  computation alive (§2.2); letting rules *react* to the conflict would
  compound the deviation.
- **The statute pattern that seems to want it doesn't.** Dutch samenloop
  provisions ("bij samenloop van aanspraken geldt de hoogste") look like
  conflict handlers but are priority declarations: the samenloop rule is
  the *ordering*, hence an ordered ladder (or a rung whose justification
  compares the grounds), not a reader of a conflict channel. Modelling
  samenloop as conflict-then-repair would re-introduce two-phase
  evaluation through the back door.
- **Cost of the alternative:** a readable conflict fact adds a negative
  edge from every group member to every reader (the conflict is an
  end-state observation, RS ch. 7.2), extending WF-051 for no motivating
  corpus case.
- **Price of closing: zero.** The channel key already makes it
  unreachable — the ch. 5.6 regelversie read tests rule names, the
  conflict fact is keyed by the group's target form; no existing
  condition can name it. Closing is recording, not building.

Closing lands as one sentence in RS ch. 7.2 and the removal of the OPEN
marker in IS ch. 6.1 (adoption card; protected paths). Reopening later is
additive if a real statute case ever demands it.

## 7. Phase-5 groundwork: trees versus chain-plus-group

What Catala's normalization produces (Fig. 2, D-Label): per variable one
**tree** — a base default whose exception slots hold defaults, which may
themselves hold exceptions, to any depth and with any branching. What our
IR expresses today: a **chain** (one ladder; the unary tree) and
**root-level branching** (a declared-unordered group of ladders on one
target, siblings conflicting on differing simultaneous decisions —
faithfully D-DefaultExceptionsConflict, modulo the equal-values deviation
of §2.2).

The gap, stated exactly: **branching below the root**. A group member is
a chain and cannot itself branch — the group's one branching point is
fixed at the root target. So a tree that branches at the root compiles:
a base `b` with unordered exceptions `{x, y}` where `x` carries an
exception `z` becomes a group of two chains, `[z, x]` and `[y]` — `z`
displacing `x` inside member 1 is D-DefaultOneException, and member 1
against member 2 is the root-level D-DefaultExceptionsConflict (what
does *not* carry over is a shared non-trivial base `b` under all
members — a root-level limitation, workable by duplicating `b` as each
member's lowest rung under the equal-decisions rule). But a tree that
branches **below** the root does not: `b ← x`, `x ← {y, z}` — a chain
from the root, then an unordered pair as exceptions *to `x`* — has no
image. Flattening it to the group `{[y, x], [z, x]}` is wrong, not just
lossy: when only `y` applies and `x`'s justification holds, member 1
decides `y` while member 2 decides `x` — a **spurious conflict** where
the paper's calculus yields `y` (the `z` slot is `∅`; the sole non-empty
exception wins). And compiling the branch away into justification DNF is
exactly what row 8 forbids ("the compiled DNF erases exactly the
structure amendments edit").

Options for the Phase-5 design (decision 7 records the choice direction,
the design itself stays at Phase 5 with rows 9/10/36):

- **(i) Nested groups as kernel structure** — a rung's exception slot may
  hold a sub-group. Faithful, one concept recursively applied; the WF-051
  edge classes extend mechanically (every descendant read is a negative
  edge toward the root target). Recommended direction.
- **(ii) Flattening with derived conflict predicates** — encode depth in
  generated displacement conditions. No new IR surface, but erases
  amendment structure (the row-8 argument) — rejected for the same reason
  the ladder stayed first-class.
- **(iii) Frontend restriction** — the Catala plugin refuses programs
  whose trees branch below the root, loudly (honest inexpressibility,
  WF-044 style). Acceptable as the *initial* plugin scope even under (i).

Until Phase 5 this is a recorded obligation, not work: no RegelSpraak
construct produces sub-root branching, so v1 loses nothing.

## 8. Wire form and well-formedness: what does not change

Proposal A adds **zero wire surface**. The input rung is virtual — a
semantic reading, implicit per target, never serialized; the extension
discipline (IS Method: additive, optional, never null, tracked in the
`CONSTRUCTS.md` delta column) is not exercised because there is nothing
to carry. No new WF rule is needed: the norm of §4.3 is evaluation
semantics (RS ch. 7 text — a new ch. 7.8 plus the ch. 7.7 domain
extension and a delta note at ch. 1.2g), not document well-formedness;
WF-050/051 are untouched (the generalization creates no IR ladders, so
the single-writer rule and the edge classes stand as they are). Should
adoption want authorial opt-out (a rule that *demands* v0's
erase-on-leeg), that would be the moment for an extension field — this
proposal deliberately omits it: no statute case wants it, and the
extension discipline says fields enter when a construct needs the wire,
not in advance. The first new WF number, if any later part needs one, is
**WF-057** (WF-056 is the highest in use).

## 9. Migration and reclassification

| Artifact | Change on adoption | Card shape |
|---|---|---|
| `RUNTIME-SEMANTICS.md` ch. 7 (+ ch. 1.2g note) | ch. 7.8 input rung; ch. 7.7 domain extension; conflict-readability closed (§6); the §2.2 deviations named | proposal branch (protected) |
| `CONSTRUCTS.md` row 8 (+ row 18) | delta entries: equal-values conflict; observable-not-fatal conflict; computed-leeg-over-input standing | ordinary card |
| `backends/clingo` | `inputval/3` generalized (§4.4); `case_supplies` retired for desugarable targets, retained for non-desugarable computing forms; a **new** fence on allocation-target input (the latent gap of §4.4); ch. 6d note updated | ordinary card, red-first |
| `ref/` | fall-through on desugarable computing targets (§4.5, desugar-route) | ordinary card, red-first |
| corpus | new goldens: the flank, computed-over-input, the leeg-standing delta pin; gate-7 survivor list grows with reasons | through `scripts/admit.sh` |
| toolchain | nothing until the §5 derivation card / next pin-bump; v0 delta tracked meanwhile | pin-bump conformance card |

Nothing existing reclassifies: no current golden, negatief, or rood case
changes meaning — the fence guarantees the delta corner is empty of cases
today, which is exactly why the change is cheap now and dearer later.

## 10. Open decisions

1. **The input-rung reading** — A: computation beats input (the landed
   anchors' reading; recommended); B: input beats computation (Catala's
   C-Def polarity). *Recommendation: A. B's home is the Phase-5 scope
   call; adopting it for case input would flip `case6-rung-overschrijft`
   and put the dossier above the law's arithmetic.*
2. **Norm the fall-through for bare computing targets** — "a computed
   leeg never displaces an input-supplied value", extending the ch. 7.7
   desugaring to the bare computing rule and retiring the ch. 6d fence
   for desugarable targets; v0's erase-on-leeg becomes a tracked delta.
   The alternative is honestly available: with `inputval/3` general,
   v0's erase is *also* monotonically encodable (condition-only
   effectiveness, §4.4), so the mirror could be kept — at the price of
   two leeg-disciplines in one calculus. *Recommendation: yes,
   fall-through. Uniformity with the landed ladder, the observed-fact
   argument, the fault-domain neighbour — and the corner is
   fence-blocked today, so no case can disagree; it is the last obstacle
   between the flank cases and the differential corpus.*
3. **Scope of decision 2** — scalars and timelines together (one
   machinery, §4.4) or scalar-first. *Recommendation: together, scalar
   demonstrated first in the executor card.*
4. **Conflict-fact readability** — close for v1 (§6). *Recommendation:
   close; reopening is additive.*
5. **Track the two unrecorded Catala deviations** (§2.2) in
   `CONSTRUCTS.md` row 8. *Recommendation: yes — the row-8 delta column
   is the contract that we never deviate silently.*
6. **The Drools path** (§5) — schedule the derivation card at the next
   pin-bump (satisfying the exit criterion on the v0-expressible
   fragment), defer full ladder service to the first ladder-producing
   frontend. *Recommendation: exactly that split; hand-authored-IR-only
   Drools ladder service tests no production path.*
7. **Exception-tree direction** (§7) — record nested groups (option i)
   as the intended Phase-5 kernel shape, with frontend restriction
   (option iii) as the admissible initial plugin scope.
   *Recommendation: yes, as a recorded direction, no design now.*

## Appendix — one rule with input, paper execution in both encodings

Own-authored Jutterswet-flavoured example (illustrative only; becomes a
golden after decision 2 through `admit.sh`). Object type `Vondst`,
attribute `geschatte waarde` (numeriek, 2 decimals). One conditional
computing rule:

> **Regel Waardestelling**: De geschatte waarde van een Vondst moet
> gesteld worden op de taxatiewaarde van de Vondst, **indien** de Vondst
> gemeld is.

Case input: instance `v1`, `geschatte waarde = 40.00`. Desugared reading
(§4.3): rung 1 = (gemeld, taxatiewaarde); rung 2 = the input rung; no
initialisation.

ASP program fragment (§4.4 sketch, scale 2):

```
is_a(v1,"vondst").
inputval(v1,"geschatte waarde",4000).
r1_out(S,V) :- is_a(S,"vondst"), heeft(S,"gemeld"), val(S,"taxatiewaarde",V).
r1_eff(S)   :- is_a(S,"vondst"), heeft(S,"gemeld"), val(S,"taxatiewaarde",_).
val(S,"geschatte waarde",V) :- r1_out(S,V).
val(S,"geschatte waarde",V) :- inputval(S,"geschatte waarde",V), not r1_eff(S).
```

| Scenario (facts beside the input) | ref, ladder reading | ASP stable model | v0 mirror today |
|---|---|---|---|
| gemeld, taxatiewaarde 50.00 | rung 1 decides → **50.00** | `r1_eff(v1)` derives; `val(v1,…,5000)` from `r1_out`; input displaced by NAF | same (overwrite) — but the ASP side is fence-refused |
| niet gemeld | rung 1 justification fails → input rung decides → **40.00** | `r1_eff` underivable → `val(v1,…,4000)` from `inputval` | ref same; ASP fence-refused (the §3b flank) |
| gemeld, taxatiewaarde leeg | rung 1 falls through (consequence leeg) → input rung decides → **40.00** | `val(v1,"taxatiewaarde",_)` underivable → `r1_eff` underivable → input stands | **the delta**: v0/ref write leeg, input erased (ch. 1.2g) — encodable too, via a condition-only `r1_eff` (§4.4); decision 2 chooses standing |

The three rows are the whole proposal in miniature: row 1 is the
confluent domain where every reading agrees and only the encoding was
missing; row 2 is the flank the fence needlessly refuses; row 3 is the
one corner where a norm must be chosen — decision 2 chooses standing, and
the table's middle column becomes the pinned expectation in both
executors. Read through the paper's reduction rules, the Reading-A term —
the computation as the sole exception over the input as enclosing
default, `⟨ ⟨ | gemeld :- taxatiewaarde ⟩ | true :- 40 ⟩` — reproduces
the table exactly: a non-empty exception wins (row 1, 50); a false
justification or an empty consequence makes the exception `∅`, caught at
the exception slot, and the enclosing default supplies 40 (rows 2–3).
That term is **ours**, not the one Catala itself would build: C-Def
compiles a supplied input as the single *highest exception*, yielding 40
in all three rows — decision 1's Reading B, declined for dossiers. The
polarity is the entire difference.
