# Phase 5 — Catala as a frontend: the proposal

*Status: adopted (2026-08-21) — all six §8 decisions taken by the
maintainer in decision round 3; originally proposed per decision
round 2.
Companion to `PHASE4-DEFAULTS.md`, whose §2 fidelity map and §7
tree-versus-chain groundwork this document builds on. No code lands
with this proposal; §8 lists the decisions it needs. Paper references
are to Merigoux, Chataing & Protzenko, "Catala: A Programming Language
for the Law", ICFP 2021.*

## 1. Purpose and stance

Phase 5 proves the top of the hourglass. Phases 1–4 proved the bottom:
one IR, two semantically alien executors, meaning defined outside any
production runtime. What remains unproven is frontend-neutrality — that
the waist serves more than one *source* language. The Catala frontend
is the designated test (PLAN §6): a compiler plugin that emits our IR
JSON from Catala's normalised core, landing on the **core IR**, not the
RegelSpraak dialect.

The stance from PLAN §6 stands: full-Catala expressibility is the goal
(decided 2026-08-16; `CONSTRUCTS.md` rows 10/36), not a subset stance —
but the *road* there is staged, and the plugin's first milestone is
deliberately narrower than the goal (§6). Where the plugin cannot yet
express a Catala program, it refuses loudly, WF-044 style — honest
inexpressibility over silent approximation, the same rule the backends
live by.

## 2. The attachment point

Catala's pipeline: literate surface syntax → desugared →
**scopelang** (scopes explicit; per scope variable one normalised
default tree — the paper's D-Label, what PHASE4-DEFAULTS §2.1 calls
the surface desugaring; the trees exist from this level on) →
**dcalc** (the default calculus;
scopes compiled to functions, the caller's override thunked in as the
highest exception, C-Def) → lcalc (defaults compiled to exceptions) →
verified/extracted backends.

We attach at the **scopelang→dcalc boundary**: after desugaring and
normalisation — all sugar gone, types known, exception trees
normalised, exactly the three properties PLAN §6 names (they hold at
scopelang; attaching one step above the literal "default-calculus
level" is a deliberate refinement, confirmed as decision 1) — but
*before* the scope structure compiles away. The scope structure is
precisely what our mapping needs to see (§3): once scopes become
functions and caller overrides become synthetic exceptions (C-Def),
the structural information our object-type mapping needs — and the
override *polarity* that §3's decision-6 row must get right — is
gone, and we would be reverse-engineering it.

Two practical notes, honestly stated. First, the exact module and
plugin API against today's Catala compiler is an implementation-time
verification step (§6, milestone 5a opens with it) — the paper fixes
the *conceptual* level, the codebase names move. Second, the paper's
metatheory covers dcalc→lcalc only (PHASE4-DEFAULTS §2.1); attaching
above dcalc inherits design, not proof — our substitute is, as always,
the differential square over anchored cases.

## 3. The mapping, term by term

The right column is existing kernel unless marked otherwise.

| Catala (scopelang) | uchar core IR | Status |
|---|---|---|
| scope declaration | object type (one instance per scope invocation) | rows 9/21 — sugar over existing kernel |
| scope variable (internal/output) | attribute of that object type | existing |
| `input` variable (no own default; caller must supply) | attribute filled by the instantiation (member initialisation) | existing |
| one default tree per variable (D-Label) | prioritized defaults / a declared-unordered default group on that attribute | landed (Phase 4); **sub-root branching: see §5.1** |
| exception nesting (structural priority) | default order; root branching = unordered group | landed (Phase 4) |
| caller override of a `context` variable (C-Def: the caller's value as the **highest** exception — Reading B) | a **highest-priority default** above the scope's own defaults (the §4.1 hardship pattern is the landed precedent of Reading B *as program structure*) — **not** the Phase-4 input default, which is Reading A (computation displaces input) and runs the opposite direction; PHASE4-DEFAULTS §2.2 reserved this reconciliation for Phase 5 | **decision 6** (§8) |
| subscope call (instantiate, bind inputs, read outputs) | object creation + member initialisation (the bound inputs) + role navigation (reading outputs) | row 9 — real design work, but over existing kernel nodes |
| `ConflictError` (⊛) | the §9.5 channel fact, observable-not-fatal | landed; **tracked deviation** (row 8: Catala aborts, we continue) |
| `EmptyError` (∅), catchable | ordinary leeg; fall-through | landed; **tracked deviation** (row 8) |
| money, dates, durations, rationals | exact arithmetic + units (rows 26/27/34: no dedicated money type; a unit with declared decimals and explicit rounding) | existing |
| enum without payload | enumeration domain (row 27) | existing |
| enum with payload + exhaustive match | **new kernel** (row 36) — designed in this phase, spec-first | 5b |
| general folds, function values | **new kernel** (row 10) — designed in this phase, spec-first; DMN's BKM+invocation lands under the same design | 5b |
| assertions | consistency rule (§9.5 channel on violation) | existing (row 19) |
| law-text structure and versioning (source level) | source references (row 3) + our richer runtime time axis (row 11) | see §5.4 |

## 4. The first milestone, and the rights-clean corpus route

The exit criterion (PLAN §6): the tutorial example round-trips through
the plugin into valid, verifier-clean IR and executes on interpreter +
one engine. Two corpus routes exist; the proposal recommends the
second, and doing both in order.

**(a) The Catala tutorial statute** (the fictional tutorial law in the
Catala repository; the paper's running example is 26 USC §121). Good
as the plugin's own development test — it exercises exactly the
constructs Catala's authors consider central. But it enters *our*
published corpus only under its upstream licence with attribution,
which cuts against the corpus rule (rights-free **by construction**,
own-authored or art. 11 Auteurswet). Recommendation: the tutorial runs
in the plugin's test suite (frontend-side), not in `corpus/golden/`.

**(b) The Jutterswet in Catala.** Author a Jutterswet fragment —
art. 3/5 (penningrecht with the hardship defaults) is the natural first
candidate: it is small, already anchored (`jutterswet-hardheid`), and
exercises scopes, one default tree with an exception, and an input
fact — *in Catala*, ourselves. Rights-free by construction, and it
turns PLAN §6's frontend-diff exercise into something sharper than the
plan asked for: **the same article through two frontends**
(RegelSpraak via the toolchain, Catala via the plugin), diffed at the
IR level, with the existing anchors as the semantic referee. Where the
two frontends express the same law as different IR, an IR design flaw
is exposed — the exact feedback a second frontend exists to produce.

## 5. Frictions, honestly

**5.1 Sub-root branching** (PHASE4-DEFAULTS §7). Catala trees branch
at any depth; our prioritized defaults branch only at the root. The recorded
resolution direction is nested groups as kernel structure (option i);
the plugin's initial scope is option (iii): refuse programs whose
trees branch below the root, loudly. A flattening fallback is rejected
for the row-8 reason — it erases the structure amendments edit, and it
manufactures spurious conflicts (§7's worked example).

**5.2 Folds and function values** (row 10). Catala programs use list
folds and first-class functions sparingly but essentially (household
composition, per-child computations). These are kernel additions
designed spec-first in 5b — with verifier rules, negatiefs, and both
executors — before the plugin translates them. Until then: loud
refusal.

**5.3 Sum types with payload** (row 36). Same regime as 5.2: kernel
design first, plugin translation after. Payload-less enums already
map.

**5.4 Time.** The mismatch runs in our favour and needs no new kernel:
Catala handles law versions mainly at source level and computation
dates as ordinary values; our IR carries runtime timelines, vintages
and validity windows (row 11). A Catala program's date arithmetic maps
onto rows 26/27; its *statute* versioning maps onto source references
(row 3), not onto our time axis. What does not exist in Catala —
timeline-typed attributes — simply never arrives from this frontend.

**5.5 Evaluation model.** Catala is functional (a call graph, lazily
thunked defaults); we are fixpoint forward-chaining. Compiling
weak-to-strong is the good direction (PLAN §6): every scopelang
program has a dependency order (Catala's own topological sort), and a
dependency-ordered program is trivially stratifiable — WF-045/046
accept what Catala accepts. The converse direction (our recursion-rich
programs into Catala) is Phase 6's problem, deliberately deferred.

**5.6 The two landed deviations stand.** ConflictError
observable-not-fatal and EmptyError-as-ordinary-leeg are tracked
deviations (row 8), decided in Phase 4. The plugin does not re-open
them. Precision on ∅: fall-through to the enclosing default is the
*pervasive* core mechanism of the calculus (D-DefaultFalseNoExceptions)
and maps onto defaults fall-through directly — nothing to settle there.
The corner that does not map is **cross-scope** ∅-fallback (an
enclosing default in the *calling* scope catching a callee variable's
emptiness): that translates to explicit emptiness conditions where
structure allows, and is refused loudly where it does not — to be
settled during 5a against real tutorial code.

## 6. The staged path

- **5a — plugin skeleton + tutorial subset.** Verify the attachment
  point against the current Catala codebase; emit IR for scopes,
  scalar defaults (chains and root-branching trees), money/date
  arithmetic, assertions; refuse the rest loudly. Tutorial runs
  frontend-side; the Jutterswet-in-Catala art. 3/5 case lands in the
  corpus through the full gate set. Exit: PLAN §6's criterion on the
  subset — plugin IR is verifier-clean and executes on ref + clingo.
- **5b — the two kernel designs** (rows 10/36), spec-first in this
  repo: folds/function values, sum types with exhaustive match. Each
  with WF rules, negatiefs, worked examples, both executors, anchors —
  before the plugin uses them.
- **5c — nested groups** (option i of PHASE4-DEFAULTS §7), lifting the
  sub-root refusal.
- **5d — the frontend-diff exercise**, documented with resulting spec
  changes: the same Jutterswet articles through both frontends, IR
  diffs triaged into (i) legitimate dialect difference, (ii) IR design
  flaw → spec delta, (iii) frontend bug.

Each stage is cards with gates in the usual regime; 5a is the first
implementation card set after this proposal's decisions.

## 7. What this phase does not do

No Catala backend (Phase 6, deliberately last — PLAN §6 records why
compiling strong-to-weak teaches less and risks their core quietly
becoming ours). No Catala parser of our own — attaching below the
surface syntax is the point; we consume their normalisation. No proof
inheritance claims: their F* mechanisation covers a step we sit above.
No RegelSpraak-dialect features in the plugin's output — it emits core
IR only; if the plugin ever *needs* a dialect node, that is by
definition an IR design flaw for 5d's triage.

## 8. Decisions for the maintainer

1. **Attachment point.** A: the scopelang→dcalc boundary (§2 — scope
   structure and override polarity still visible); B: dcalc proper
   (PLAN §6's literal reading — simpler API surface, but scope
   structure must be reverse-engineered). Recommendation: A, with the
   implementation-time verification step opening 5a.
   **Decided (maintainer, 2026-08-21): A.**
2. **Corpus route.** A: tutorial frontend-side only, the
   Jutterswet-in-Catala article as the corpus-facing case (§4b — the
   same law through two frontends); B: the tutorial statute in the
   corpus under upstream licence with attribution (cuts against
   rights-free-by-construction). Recommendation: A.
   **Decided (maintainer, 2026-08-21): A.**
3. **Where the plugin lives.** A: `frontends/catala/` in this monorepo
   (a third sibling kind next to `ref/` and `backends/`; CI treats it
   like a backend); B: a separate repo pinned to `SPEC_REV` like the
   toolchain, if the Catala build dependency proves too heavy for this
   repo's CI. Recommendation: A first, B as the recorded escape hatch.
   **Decided (maintainer, 2026-08-21): A, escape hatch recorded.**
4. **Initial refusal scope.** A: option (iii) — sub-root branching,
   folds/function values, payload enums and cross-scope ∅-fallback all
   refuse loudly in 5a; B: block 5a on the 5b/5c designs.
   Recommendation: A (honest refusal ships earlier and the refusals
   are the WF-044 house style).
   **Decided (maintainer, 2026-08-21): A.**
5. **Stage order.** A: 5a → 5b → 5c → 5d as in §6, 5b interleaving
   with 5a once the refusals are in place; B: strictly serial.
   Recommendation: A.
   **Decided (maintainer, 2026-08-21): A.**
6. **The override polarity** (the §3 C-Def row; PHASE4-DEFAULTS §2.2
   reserved this for Phase 5). A caller override of a `context`
   variable is Reading B — the caller's value *beats* the scope's own
   computation (the highest exception) — which is the opposite of the
   landed Phase-4 input default (Reading A: computation displaces case
   input). A: the plugin compiles a context override as a
   **highest-priority default** above the scope's own defaults
   (Reading B as program structure, the §4.1 hardship-pattern shape;
   the input default remains reserved for the case dossier); B: refuse
   context overrides loudly in 5a and design the mapping in 5c.
   Recommendation: A — it is structure-preserving *because* the
   polarity is expressed as default order, and it keeps the two
   readings in the two distinct mechanisms
   PHASE4-DEFAULTS §2.2 demands.
   **Decided (maintainer, 2026-08-21): A.**

   **Executed (2026-08-24) — the generated highest-priority default
   landed, triple-green.**
   A Catala `context` (reentrant) subscope input now lowers to a generated
   highest-priority default above the callee's own defaults on that target
   (Reading B as default order, the §4.1 hardship-pattern shape). The plugin
   forces the reentrant target's own default to the **base** of the
   prioritized defaults (never a bare Berekening, so a default can sit above
   it), and at the call site translates the caller's override tree into
   highest-priority default(s) prepended onto those defaults: the default's
   guard (justification) and value (consequence) read the caller **navigated
   over the synthesised `verbindendeRol` back-link** (callee → caller) — no
   standing machinery, so the ch. 7.8 input default stays reserved for the
   case dossier (Reading A). A caller value feeding a callee target
   **without prioritized defaults**
   (a plain input, no own default) keeps the existing member-initialisation
   route — the WF-050 constraint PHASE5C-TUTORIAL §3 recorded. No verifier
   change was needed: navigated reads inside a default are verifier-clean
   and both executors evaluate them, the differential agreeing with `catala
   interpret` on both polarities (the highest-priority default decides vs
   falls through to the callee base). Witnesses: the frontend fixture
   `context.catala_en` (both polarities through verifier → ref → clingo,
   gate 9) and the own-authored
   corpus golden `jutterswet-hardheidsaftrek-catala` (art. 9bis — the
   discretionary hardship aftrek as a context override, both executors, gates
   2–6, plugin-regenerated byte-identically under gate 9). The old blanket
   context refusal **narrows** to recorded flanks that genuinely fall outside
   the decided attribute-value pattern: a context override on a
   **characteristic** (boolean) callee target (a decided attribution, not a
   value — `refuse_context.catala_en`), a **below-root branching** override
   tree, and a **root-unordered** own default all stay refused loudly.
