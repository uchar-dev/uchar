# PHASE5D-DIFF — the frontend-diff exercise: penningrecht through two frontends

*Status: draft for maintainer ratification · 2026-08-24 · companion to
`PHASE5-CATALA.md` (adopted) and `PHASE5C-TUTORIAL.md` (landed). This
document discharges the **second Phase-5 exit criterion** (`PLAN.md` §6:
"the frontend-diff exercise is documented with resulting spec changes") —
the exercise `PLAN.md` §6 prescribes: run the same law through both
frontends and diff the IR dumps, because "where two frontends express the
same semantics as different IR, an IR design flaw is exposed".*

*Working prose: may cite the private toolchain (`regelspraak-java`) by name
(WAY-OF-WORKING §5). Nothing here enters an artifact zone; the two dumps it
analyses are already checked-in corpus goldens.*

## 1. The chosen fragment — and why it is the right one

The law: **Jutterswet art. 3 jo. art. 5 (penningrecht / hardheid)** — the
canonical **defeasibility defaults group**, the Phase-4/5 core both frontends must
carry:

- base: geen recht (silence — no `penninggerechtigd` without a ground);
- **art. 3:** penninggerechtigd bij ten minste **twaalf** aangemelde
  vondsten (the threshold condition);
- **art. 5:** the hardheidsgrond reads "twaalf" as "**zes**" — an exception
  *on* art. 3 (the chain, not a second unordered ground), carried as a
  compound justification (hardheid ∧ ≥ 6).

The same law already lives in the corpus in **three** encodings, which is
what makes it the diagnostic fragment:

1. hand-written native uchar IR, a `PrioritizedDefaults` —
   `corpus/golden/anchor/jutterswet-hardheid`;
2. the **Catala** render through the plugin, a `PrioritizedDefaults` —
   `corpus/golden/jutterswet-penningrecht-catala`;
3. the **RegelSpraak-toolchain** render through the resolver, two flat
   `KENMERKTOEKENNING` rules — `corpus/golden/jutterswet-penningrecht`
   (the render this card authored, the missing frontend dump).

The pair this exercise diffs is **(3) vs (2)** — the two *frontend* dumps of
the same law.

**Why this fragment, and not a richer one.** It is **provably semantically
identical on every input**, not merely on the corpus cases. Both defaults
decide the same boolean (`waar` / attribute), so the ordered defaults
collapse to a plain disjunction and no default-conflict is reachable; the
toolchain — which has no defaults construct — renders the same law as two
flat same-valued attributions, and here disjunction ≡ the defaults group.
The pair therefore
isolates a **pure form diff**: identical meaning on all inputs, two IR
shapes, nothing else moving.

`tariefrecht-catala` was the richer contrast and was **deliberately
rejected**: its two *unordered* exceptions (`grootgezin` 10% / `kleine
beurs` 0%) raise a Catala `ConflictError` on the overlap input
(kindertal ≥ 4 ∧ inkomen ≤ 10000 → uchar §9.5 over-determination), which
flat, mutually-exclusive RegelSpraak rules cannot reproduce. Diffing that
pair would contaminate the form diff with a genuine *semantic* divergence
and defeat the purpose. Penningrecht's whole-input equality is the stronger
base for isolating form from meaning (see §6 for what this scoping costs).

## 2. The reproducible dump recipe (revisions pinned)

Both dumps are checked-in goldens; both are byte-identical to a fresh
regeneration.

- uchar side: branch `card/fase5d-frontend-diff` — the card claim at
  `ac391c2`, the checked-in pair-fragment goldens at `3285c58` (hashes
  refreshed after a rebase onto main orphaned the originals; the dumps
  regenerate byte-identically from the checked-in goldens regardless).
- toolchain (`regelspraak-java`) git HEAD: `332db4c`.
- toolchain `SPEC_REV` pin: `81e198e`.

**RegelSpraak side** (STRICT read-only toolchain; the dump call is the one
mutation allowed) — from the `regelspraak-java` checkout:

```
make ir FILE=<uchar>/corpus/golden/jutterswet-penningrecht/bron.rgs \
        OUT=<scratch>/rgs-penningrecht-ir.json
```

Byte-identical to the checked-in `ir.json` (confirmed against a fresh
resolve). Equivalent low-level form: `make resolver` once, then
`java -cp lib/antlr-4.13.2-complete.jar:out
io.github.yhavinga.regelspraak.resolver.Resolver --ir <out> <bron.rgs>`.

**Catala side** (the plugin) — from the uchar worktree, `eval $(opam env)`
first:

```
dune build
_build/default/frontends/catala/bin/catala_frontend.exe \
    corpus/golden/jutterswet-penningrecht-catala/bron.catala_en <raw.json>
```

then canonicalise with gate 2's serialiser slice (as `scripts/check-catala.sh`
does — one source of truth) to reach the checked-in `ir.json` shape; gate 9
regenerates it byte-identically.

**The structural read.** The classification below walks a **normalised**
form of each dump — every `bron` field stripped recursively and object keys
sorted (`json.dumps(..., sort_keys=True)`). The `bron` strip removes the
category-(i) source-string noise so structure is legible; the normalisation
is applied only to the scratch read-copies, never to the checked-in dumps.

## 3. The equality test — same cases, same absolute outcomes

Both goldens carry the **same five cases** (identical inputs modulo the
category-(i) naming, identical expected outcomes), and both pass gates 4–6:
the reference interpreter (gate 5) and clingo (gate 6) replay each case with
exact expectation match, so **ref ≡ clingo over both dumps, anchored to the
hand-written outcomes**.

| case | aantal vondsten | hardheid in input | outcome (both dumps, both executors) | ground |
|---|---|---|---|---|
| case1-standaard | 12 | no | `penninggerechtigd` present | art. 3 threshold |
| case2-hardheid | 6 | yes | `penninggerechtigd` present | art. 5 hardheid |
| case3-stilte | 6 | no | **absent** | silence — no ground |
| case4-hardheidsgrens | 4 | yes | **absent** | hardheid, below 6 |
| case5-beide-gronden | 12 | yes | present | both grounds |

The five cases pin the boolean function
`penninggerechtigd ⇔ (vondsten ≥ 12) ∨ (hardheid ∧ vondsten ≥ 6)`.

**Whole-input equivalence (the argument the five cases stand in for).** The
Catala defaults group is `[default: hardheid ∧ ≥6 → true] >
[default: ≥12 → true] > [base → false]`. It is a *characteristic* defaults
group, so every justified default decides (RUNTIME-SEMANTICS ch. 7.1); the
base default (always-true condition) decides `false` only when no higher
default fires. Its extension is therefore
exactly `(hardheid ∧ ≥6) ∨ (≥12)`. The toolchain's two flat
`KENMERKTOEKENNING` rules each attribute `penninggerechtigd = waar` under
one disjunct; a characteristic is monotone (ch. 5.3) and both rules write
the *same* value, so their joint extension is `(≥12) ∨ (hardheid ∧ ≥6)` —
the same set, order-free and conflict-free. The two forms coincide on every
input, not just the five. This is why the pair is a clean form diff.

## 4. The structural diff, classified

Walking the tree — declarations, top-level rule/defaults container,
condition/expression forms, types, source references — every difference
falls into one of the three prescribed categories: (i) administrative,
(ii) tracked v0↔v1 delta (confirmation, each verified against
`CONSTRUCTS.md`), (iii) a real design flaw.

### 4.1 Category (i) — administrative (naming, source strings, order)

- **Object-type name:** `Jutter` (toolchain) vs `Penningrecht` (Catala — the
  scope name becomes the object type). Naming.
- **Member identifiers:** `aantal aangemelde vondsten` / `hardheid toegepast`
  (toolchain, spaces) vs `aantal_aangemelde_vondsten` / `hardheid_toegepast`
  (Catala — identifiers cannot carry spaces). Naming.
- **`bron` / `onderwerpBron` strings:** RegelSpraak natural-language phrases
  (`"zijn aantal aangemelde vondsten groter of gelijk is aan 12"`,
  `"Een Jutter"`) vs Catala's terse tokens (`"de aantal_aangemelde_vondsten"`,
  `"penninggerechtigd"`). Source annotations; stripped for the structural
  read.
- **Serialisation order** of the `kenmerken` list: `["penninggerechtigd",
  "hardheid toegepast"]` (toolchain — RegelSpraak declaration order) vs
  `["hardheid_toegepast", "penninggerechtigd"]` (Catala). Order only; the
  `kenmerkSoorten` map carries the same two `BIJVOEGLIJK` entries either way.
- **`meervoud`:** `"Jutters"` (RegelSpraak declares a plural) vs `null`
  (Catala has none). Surface/authoring.
- **`teken` on the attribute:** `NIET_NEGATIEF` (the RegelSpraak source
  declares "niet-negatief geheel getal") vs `ONBEPAALD` (the Catala source
  leaves the sign unconstrained). This is an **authoring** difference between
  the two source texts, not an IR-forced one; it is benign on this corpus
  (every test value is non-negative, the `≥` comparisons are unaffected) —
  but it means the two sources are not character-for-character the same
  declaration (recorded honestly in §6).

None of these is a divergence the IR imposes; each is either a name the
frontend cannot spell identically, a source annotation, a list order, or a
sign refinement one source author wrote and the other did not.

### 4.2 Category (ii) — tracked v0↔v1 deltas (confirmation)

Three structural differences, each an *expected and wanted* delta whose
tracking in `CONSTRUCTS.md` was verified per case. Per the exercise's own
rule, **a tracked delta is confirmation, not a finding.**

**(ii-a) The core one — `PrioritizedDefaults` (Catala) vs two flat
`KENMERKTOEKENNING` `resolvedRegels` (toolchain).** This is the whole
top-level shape:

- Catala emits one `prioritizedDefaults` entry — subject `Penningrecht`,
  kenmerk `penninggerechtigd`, three `Default`s (`hardheid ∧ ≥6 → besluit
  true`, `≥12 → besluit true`, `base → besluit false`) — and
  `resolvedRegels: []`.
- The toolchain emits two `resolvedRegels` of `soort: KENMERKTOEKENNING`
  (`penninggerechtigd bij drempel`, `penninggerechtigd bij hardheid`), each
  a `doel` (`AttribuutRef` to the characteristic) + `waarde`
  (`Literal "waar"`) + `voorwaarde`, and **no `prioritizedDefaults` key at all**.

The pieces of this one delta:
- **container:** a first-class `PrioritizedDefaults` (ordered defaults,
  fall-through semantics) vs a flat `regels`/`resolvedRegels` list;
- **decision carrier:** `Default.besluit` (the boolean *is* the decision,
  ch. 7.1) vs `ResolvedRegel.waarde = Literal "waar"`;
- **target carrier:** the defaults group's `(onderwerp, kenmerk)` pair vs
  the rule's `doel` `AttribuutRef`;
- **the base default:** Catala materialises an explicit `besluit: false` base
  (empty-`ALLE` condition); the flat form has no base default — an unfired
  characteristic is simply absent;
- **priority:** the defaults group orders art. 5 *above* art. 3 (the exception
  defeats the threshold); the flat rules carry no order. Here the priority
  is present in one form and absent in the other but **inert**, because both
  defaults decide the same value — the very property that makes this a pure
  form diff (§3).

**Tracked:** `CONSTRUCTS.md` **row 8** (the adopted Catala default calculus:
"a first-class defaults group per target with Catala's fall-through", landed
as the `prioritizedDefaults` extension, IR-SPEC ch. 6.1 / WF-050/051; and
explicitly "across compositions a Catala frontend **must compile it into
defaults structure**" — the recorded Phase-5 reconciliation obligation);
**row 2** (KENMERKTOEKENNING → the target's member kind); **row 7**
(defaults). The v0 mirror produces the flat rules; a v1-native frontend
(Catala) produces the defaults group. Semantics reconciled: RUNTIME-SEMANTICS ch. 7, with the **v0
desugaring ch. 7.7** and the differential witness `jutterswet-waardering-v0vorm`.
Confirmed carried.

**(ii-b) Parse-stage `Regel` shells.** The toolchain emits a `regels` list
of two `Regel` stubs (`{node, naam, soort:"regel"}`) *beside* the resolved
forms; Catala emits `regels: []`. **Tracked:** `CONSTRUCTS.md` §2.3 —
`Regel (shell)` is `non-goal`, "superseded by `ResolvedRegel`; collapses
away". The v0-mirror toolchain carries the shell; a v1-native frontend does
not. Confirmed.

**(ii-c) The optional `prioritizedDefaults` extension field, present vs
never-produced.** Catala emits `prioritizedDefaults` populated; the toolchain
omits the key entirely (not `[]`) — while both emit the *required* core
`resolvedRegels`, empty (`[]`) on the Catala side. **Tracked:** the schema
description and the IR-SPEC wire-entry discipline (Method) — v1 extension
top-level lists (`prioritizedDefaults`, `functionDefs`) are optional and "the v0
emitter never produces any of them", whereas core lists are always-emitted
required keys. The present-vs-absent asymmetry is the designed convention,
not a divergence. Confirmed (this is the wire face of ii-a).

**A structural note that strengthens "pure form diff":** below the container,
the condition trees are **identical in shape**. The toolchain's hardheid
rule `voorwaarde` and the Catala default-1 `voorwaarde` are both
`Samengesteld{kwantor: ALLE}[ Kenmerk(hardheid…), Vergelijking(≥, AttribuutRef,
Literal "6") ]`; the drempel rule and default-2 are both
`Vergelijking(≥, AttribuutRef, Literal "12")`. Modulo names — and modulo
the category-(i) `teken` refinement of §4.1, which the emitter copies onto
every `AttribuutRef.type` inside the conditions (`NIET_NEGATIEF` vs
`ONBEPAALD`; lens-verified benign: negative, empty and boundary inputs
agree across both forms and both executors) — byte-for-byte the same
expression forms. Beyond that one copied field, the entire diff lives in
the top-level wrapper (defaults group vs flat rules).

### 4.3 Category (iii) — real design flaws

**None.** Every structural difference resolves into category (i) or a
tracked category-(ii) delta; no difference is an *expected-but-untracked*
divergence, and no difference is the same semantics given two IR forms for a
reason the spec does not already carry. The one corner the exercise
sharpens is treated in §5 — it is documentary, not a flaw.

## 5. What the exercise sharpens (besluit-free)

The exercise is prescribed to expose flaws; here it exposes a **corner worth
documenting**, which on examination is already covered and needs no maintainer
decision. Stated plainly so a later reader need not re-derive it:

**The defeasible characteristic has two admissible IR encodings, reconciled
only extensionally, and ch. 7.7's desugaring table does not name this
case.** ch. 7.7 desugars *at most one* computing rule (plus an
initialisation) into a defaults group, and explicitly excludes **multiple
computing rules on one target** because their v0 meaning is agenda order —
"exactly what the defaults group exists to replace". It also says a *single*
characteristic attribution is trivially its own one-default group,
"recorded, not transformed". The penningrecht toolchain dump is neither: it is **two**
`KENMERKTOEKENNING` rules on **one** characteristic target. So:

- it is **well-formed** — WF-026's single-writer rule binds *allocations*
  only (IR-SPEC ch. 3.2), and WF-050's "no other writer" binds a target that
  *carries defaults*; neither forbids two flat characteristic attributions,
  and the verifier (gate 3) passes the dump;
- it is **not desugared** into the defaults group (outside ch. 7.7's
  domain); it is left native and executed as two monotone same-valued
  attributions —
  covered by ch. 7.7's "outside the desugaring's domain the v0 forms are
  left as they are" catch-all, and *safe* precisely because the agenda-order
  exclusion rationale does not apply (same value, idempotent, confluent —
  ch. 5.1–5.3);
- it agrees with the defaults form on every input (§3), machine-witnessed on
  both executors (gates 5–6).

So the "two encodings, no canonical form" observation is **not** an
undocumented two-form problem: the multiplicity is inherent to
mirror-then-diverge (v0 has no defaults group; v1 adds one), the reconciliation
obligation is already recorded (row 8), and the extensional equivalence is
proven per-case. It is a *feature boundary*, not a flaw.

**Recommended spec consequence — one, documentary, decision-free.** The
maintainer *may* fold a single clarifying sentence into ch. 7.7 (or
`CONSTRUCTS.md` row 8) noting that **multiple same-valued characteristic
attributions on one target are left native, not desugared — order-free and
conflict-free by monotone idempotence (ch. 5.3), extensionally the
disjunctive one-default-equivalent of the defaults group a v1-native
frontend would emit.** This changes no semantics, adds no WF rule, and
closes the one silence this diff surfaced. Because ch. 7.7 / `CONSTRUCTS.md` are
maintainer-owned, even this documentary line lands as a maintainer edit, not
a session merge — but it carries **no decision**: there is no option to
weigh, only a fact to record. **No numbered maintainer question arises from
this exercise.**

## 6. Honest limits — what this exercise does not cover

- **v0-mirror form vs v1 forms, not v1 vs v1.** The toolchain is the *v0
  mirror* by construction; the Catala plugin targets the *v1 core*. The diff
  therefore measures mirror-vs-diverge, which is exactly where tracked deltas
  live — and tracked deltas are confirmation, not findings. It does **not**
  compare two independent v1-native frontends (that pairing exists since
  the F-logic landing, 2026-09-12 — `CONSTRUCTS.md` row 39).
- **One fragment is not completeness.** A single law — a characteristic
  defeasibility defaults group — proves nothing about attribute defaults,
  allocation,
  temporal writes, folds, structs, subscopes, or any construct family this
  pair does not touch. The absence of a flaw here is a data point, not a
  clearance.
- **The clean case was chosen on purpose; the sharp case is out of scope.**
  The fragment was selected *because* the two forms cannot diverge (both
  defaults decide the same value). The genuinely dangerous composition — two
  *unordered, differently-deciding* exceptions, where the defaults group
  faults on
  §9.5 over-determination and flat rules would silently last-writer-win — is
  precisely `tariefrecht`, deliberately excluded (§1). The penningrecht
  exercise itself **does not** exercise the divergent-exception path, which is
  where a real IR-form flaw could still hide. That comparison is supplied
  separately in **§8** — the divergence witness `tariefrecht` carries — so
  this limit is now closed rather than left open.
- **The two sources are not character-identical declarations.** The `teken`
  refinement (§4.1) and the plural differ between the source texts; they are
  benign on this corpus but mean "same law" holds at the level of *outcomes
  on the five cases and the provable boolean function*, not at the level of
  identical declared types.
- **Extensional, not intensional, equality.** The equality test pins that
  the two dumps produce the same outcomes (through ref and clingo). It does
  not claim the two IR trees are isomorphic — they are deliberately not
  (that is the whole diff).

## 7. Resulting spec changes (the exit-criterion ledger)

- **Normative changes: none.** No IR-SPEC / RUNTIME-SEMANTICS / schema /
  verifier change is required or proposed by this diff.
- **Category-(iii) findings: none.** No design flaw exposed.
- **Maintainer questions: none.** `besluit_plichtig` is empty.
- **One decision-free documentary recommendation** (§5): an optional
  one-line clarification in ch. 7.7 / `CONSTRUCTS.md` row 8 on multiple
  same-valued characteristic attributions. Maintainer-owned edit, no
  decision.
- **Exit criterion discharged:** with this document, `PLAN.md` §6's second
  Phase-5 exit criterion — "the frontend-diff exercise is documented with
  resulting spec changes" — is met. The PLAN §6 marking flip and the two
  opus lens rounds are the card-close steps that follow.

## 8. The divergence witness — closing the §6 grens (2026-08-24)

§6's third bullet named the one composition the penningrecht pair could not
carry: **two unordered, differently-deciding exceptions**, where Catala
raises a `ConflictError` and uchar takes the §9.5 channel. That composition
is `tariefrecht`, and it already lives in the corpus as a Catala golden
(`corpus/golden/jutterswet-tariefrecht-catala`) — a `PrioritizedDefaults` on
`tarief` whose kinderlast default (art. 2, 15 %) carries an **unordered
`Default.excepties` exception group** (art. 3): `grootgezin` (kindertal ≥ 4 → 10 %)
and `kleine beurs` (inkomen ≤ 10000 → 0 %). The three cases the golden
shipped with each fire **at most one** sub-exception, so the sharp corner was
present in the IR but unexercised. This section exercises it.

### 8.1 The overlap input, and what each side does with it

The witness input is the overlap the golden's cases avoided: **kindertal = 4
∧ inkomen = 8000** (`case4-conflict.json`). Both sub-exceptions are justified
at once and decide **different** values (10 % vs 0 %).

**Catala aborts.** Verified directly with `catala interpret` on a scratch
copy of `bron.catala_en` that adds one `#[test]` scope feeding the overlap
input (`clerk test` compiles the stdlib into `<scratch>/_build/libcatala`;
the interpret call it then runs is quoted below). The scope does not
evaluate to a value — the whole computation faults, pointing at the two
conflicting exception defaults:

```
$ catala interpret -I . tariefrecht-conflict.catala_en \
      --stdlib=_build/libcatala --scope=TariefConflictTest
┌─[ERROR]─
│
│  During evaluation: conflict between multiple valid consequences for
│  assigning the same variable.
│
├─➤ …:49.55-58:
│ 49 │     under condition kindertal >= 4 consequence equals 10%
│    │                                                       ‾‾‾
│
├─➤ …:51.59-61:
│ 51 │     under condition inkomen <= $10,000 consequence equals 0%
│    │                                                           ‾‾
└─
```

**uchar does not abort — it deposits one observable fact.** Both executors
replay `case4-conflict` (gates 5–6, exact match):

- `tarief` is **leeg**: the differing exception group propagates its conflict
  to the root default and the defaults group writes nothing
  (RUNTIME-SEMANTICS ch. 7.9);
- the §9.5 channel carries exactly one fact, **path-keyed** to the default
  that hosts the exception group: `Penningplichtige.tarief/0` —
  distinguishing a nested exception-group conflict from a root-level one
  (`…/tarief` bare), the row-8 Phase-5c form;
- and, the sharpest half of the contrast, the computation **continues**:
  `aanslag` still resolves — to **0.00**. `aanslag = inkomen × tarief`, and
  `MAAL` coalesces the blanked (leeg) rate to zero (the v0 mirror reading,
  ch. 1.2a; `ref/lib/arith.ml`'s `coalesce0`), so `belasting = 0`, the
  money floor `aftrek(1000) > belasting(0)` holds, and the then-branch pins
  `0.00`. Where Catala aborts the **entire scope** (no `aanslag` at all),
  uchar blanks the one over-determined target, records the conflict, and
  runs the rest of the program to a defined answer.

That is the tracked row-8 deviation made concrete: **Catala's ⊛-propagation
minus the fatality** — the conflict is an end-state observation on the §9.5
channel, never a program-aborting exception.

Rood-eerst: `case4-conflict` was first pinned to the naive
"priority/last-writer" reading `tarief = 0.1000` + `consistent`; both
executors rejected it (`ref`: `J1.tarief = leeg, expected 0.1` and
`the channel carries: Penningplichtige.tarief/0 × J1`; `clingo`: `J1.tarief
is underived (leeg)` and `the model carries 1 inconsistent fact(s)`), which
is the proof that neither executor silently picks a winner.

### 8.2 Can the v0-flat form express this? No — and the way it fails is the
finding

§6 speculated that, without prioritized defaults, "flat rules would silently
last-writer-win". The prediction was that the flat form loses the conflict;
the empirical reality is **sharper and worse than last-writer-win**. Built as
a scratch v0-flat IR — two flat `BEREKENING` rules on the same defaults-free
`tarief` target, no defaults, no exception group: `tarief bij grootgezin`
(kindertal ≥ 4 → 0.1000) and `tarief bij kleine beurs` (inkomen ≤ 10000 →
0.0000) — and replayed on the same overlap input:

- **verifier: `PASS`.** The flat form is *well-formed*. WF-050's
  single-writer rule binds a target that **carries defaults**; two flat
  computing writers on a defaults-free attribute violate no WF rule (the same
  reasoning as §5's characteristic case — but there the two writers were
  monotone and same-valued; here they differ).
- **`ref`: `EVALUATION FAULT: no fixpoint within 100 passes — divergence is
  a fault (ch. 5.7)`.** The two rules each overwrite the other every pass;
  the fixpoint oscillates 0.1000 ↔ 0.0000 and never converges. Not
  last-writer-win — **non-termination**.
- **`clingo`: a consistent stable model, no fault — but `J1.tarief =
  [0;1000]`.** The stable model carries **both** values at once and raises no
  §9.5 inconsistency fact, so against any single-value pin the runner reports
  `MISMATCH` (and against an `inconsistent` pin, a `CASE ERROR: … unknown
  rule`) — no expectation makes it emit a pinnable `PASS`. Not last-writer-win
  either — a **silently multi-valued** target.

So `ref` faults and `clingo` is silently-consistent-multivalued: **ref ≢
clingo**. Under the corpus rules a differential disagreement "routes to
adversarial triage, never to admission". The flat form is therefore
**un-pinnable in every zone**: not a golden (ref ≢ clingo, and ref does not
even terminate), not a `negatief` (verifier-clean), not a `rood` (clingo
raises no fault). It falls through the whole corpus by construction — which
is exactly why the `ongeordend` defaults group is *not* optional sugar over
two flat rules but **the only IR form** in which this law's over-determination
is expressible with the executors in agreement (one leeg target, one
path-keyed §9.5 fact, both backends).

The gate set is the backstop that makes the flat form's verifier-cleanliness
harmless: the differential square (gates 5–6) rejects it downstream even
though the verifier admits it. Whether a *defensive* WF rule should reject
two flat writers on one target ahead of the square was the latent design
question this witness surfaced. **Decided (maintainer, 2026-08-24): yes.**
It is now **WF-079** (`spec/IR-SPEC.md` ch. 3.1; `RUNTIME-SEMANTICS.md`
ch. 7.7): a defaults-free (type, attribute) target admits at most one
unconditional, version-free computing rule (Gelijkstelling/Berekening); two
or more are ill-formed. The rule is scoped to the soundly-detectable subset —
**unconditional** ("bare") writers, where over-determination is certain
without a satisfiability check — so it never rejects a valid program: the v0
emitter's single writer, its monotone same-valued KENMERKTOEKENNING pair
(exempt by kind), and the corpus's guarded case splits (each writer carries
its own `condition`, e.g. `anchor/jutterswet-rekenkamer-overzee`) all stay
clean. A conditional overlap that is only semantically over-determined stays
a differential-square catch. Negatief: `corpus/negatief/wf079-two-unconditional-writers/`.

### 8.3 Classification and ledger delta

- **Category (iii) — design flaws: still none.** The divergent-exception
  path — the one §6 flagged as where a flaw could hide — is walked here and
  exposes no untracked divergence. The Catala/uchar difference is the
  **already-tracked** row-8 deviation (over-determination → observable,
  non-fatal §9.5 channel fact, vs Catala's fatal `ConflictError`); the
  flat-vs-defaults difference is the row-8 defaults rationale itself
  (`Default.excepties`, RS ch. 7.9, WF-075), now shown to be load-bearing
  rather than cosmetic.
- **Normative changes: none.** The semantics this witness exercises
  (path-keyed §9.5 propagation from unordered exception groups) landed in
  Phase 5c; `case4-conflict` is a new *witness*, not a new rule.
- **Maintainer questions: none forced** (the one latent observation §8.2
  recorded was decided 2026-08-24 — the defensive rule landed as WF-079).
- **§6 limit closed:** the "sharp case is out of scope" bullet is
  superseded — the divergent-exception path is now witnessed end-to-end
  (Catala abort quoted, uchar §9.5 fact pinned in both executors, v0-flat
  inexpressibility shown empirically).
