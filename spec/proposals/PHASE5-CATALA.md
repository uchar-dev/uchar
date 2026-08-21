# Phase 5 — Catala as a frontend: the proposal

*Status: proposal (2026-08-21), per the maintainer's decision round 2.
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
default tree, the paper's D-Label) → **dcalc** (the default calculus;
scopes compiled to functions, the caller's override thunked in as the
highest exception, C-Def) → lcalc (defaults compiled to exceptions) →
verified/extracted backends.

We attach at the **scopelang→dcalc boundary**: after desugaring and
normalisation — all sugar gone, types known, exception trees
normalised, exactly the three properties PLAN §6 names — but *before*
the scope structure compiles away. The scope structure is precisely
what our mapping needs to see (§3): once scopes become functions and
caller overrides become synthetic exceptions (C-Def), the structural
information our object-type mapping and the Phase-4 input rung already
express natively is gone, and we would be reverse-engineering it.

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
| input variable | attribute filled by case input, or by the calling scope (below) | existing |
| one default tree per variable (D-Label) | one ladder / declared-unordered ladder group on that attribute | landed (Phase 4); **sub-root branching: see §5.1** |
| exception nesting (structural priority) | rung order; root branching = unordered group | landed (Phase 4) |
| caller override of a context variable (C-Def: highest exception) | the Phase-4 input rung — case/caller input as the defeasible fact the ladder's write meets | landed (RS ch. 7.8); the mapping is *structure-preserving*, not re-encoded |
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
art. 3/5 (penningrecht with the hardship ladder) is the natural first
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
at any depth; our ladders branch only at the root. The recorded
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
them; a Catala program whose *correctness* depends on catching
EmptyError (rare, discouraged upstream) translates to explicit
emptiness conditions or is refused loudly — to be settled during 5a
against real tutorial code.

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

1. **Attachment point**: the scopelang→dcalc boundary (§2), with the
   implementation-time verification step in 5a. Proposed: yes.
2. **Corpus route**: tutorial frontend-side only; the
   Jutterswet-in-Catala article as the corpus-facing case (§4b).
   Proposed: yes.
3. **Where the plugin lives**: `frontends/catala/` in this monorepo (a
   third sibling kind next to `ref/` and `backends/`; CI treats it
   like a backend — builds, runs its suite, feeds the differential),
   unless the Catala build dependency proves too heavy for this repo's
   CI, in which case a separate repo pinned to `SPEC_REV` like the
   toolchain. Proposed: monorepo first, escape hatch recorded.
4. **Initial refusal scope**: option (iii) — sub-root branching,
   folds/function values, payload enums, catchable-EmptyError
   dependence all refuse loudly in 5a. Proposed: yes.
5. **Stage order**: 5a → 5b → 5c → 5d as in §6, with 5b's two designs
   allowed to interleave with 5a's plugin work once the refusals are
   in place. Proposed: yes.
