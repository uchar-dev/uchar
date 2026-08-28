# clingo term encodings: quote escaping, Begrensd, Maximum/Minimum over a set, som without the 0 fallback

Goal: four pure encoding gaps in backends/clingo/lib/translate.ml close in one
card — ref is the meaning throughout and is NOT touched; every semantics below
is already executed by ref (which replays 30/30 on the toka slices).
(1) The ASP string encoding escapes embedded double quotes and backslashes
    (quote helper, translate.ml:70-76) instead of faulting — consistently on
    BOTH the literal side (symbolic_literal_term) and the value side
    (symbolic_value_term), so the equality join stays sound; control chars
    <0x20 keep faulting loudly.
(2) Begrensd (clamp) gains a term-position lowering (min-only, max-only,
    min+max) via integer min/max identities on the harmonised scale; an empty
    base propagates empty through the bare join; bound/base scale and unit
    harmonisation is explicit, and any position needing a real unit
    conversion factor != 1 keeps faulting loudly.
(3) Functie MAXIMUM/MINIMUM over a set-typed AttribuutRef gains the S8
    aggregation arm (#max/#min mirroring the Som arm at translate.ml:1129)
    with a presence guard: empty contributors are skipped, an all-empty or
    empty set derives nothing (ref: Empty) — #sup/#inf must never leak out.
(4) SOM without a leegwaarde fallback gains a presence guard so an empty set
    yields empty (ref agg_som None -> Empty, ref/lib/arith.ml:425-428), not
    #sum's 0; the existing 0-fallback behaviour is unchanged and the
    "fallback other than 0" arm keeps faulting.

Four rights-free, own-authored Jutterswet goldens (hand-computed absolute
expected values, noop-refuting) witness each form through the full gate set.
The six toka pins toka-20, toka-21, toka-24 and toka-46-woonregio-factor-1/2/3
move FAULT->PASS in toka/cases/parity.manifest (clingo 16 -> 22 of 30).
spec/ASP-TRANSLATION.md gains/extends the S-section text per encoding and S19
shrinks by exactly the sentences these forms covered — S19 stays the highest
section and shrinks only by translation, never by silence.

Acceptance:
- Red-first: BEFORE the change, the clingo runner on each of the six toka
  ir/case pairs exits non-zero with the pinned manifest fragment (evidence in
  the Result — do NOT quote fault text that embeds casus spellings; the
  manifest fragments themselves are fine).
- After: bash scripts/check-toka-parity.sh is green with the six pins moved
  to PASS|- and the fragment tally shrunk accordingly.
- New goldens validate through gates 2-8 (schema round-trip, verifier, case
  schema, ref oracle, clingo differential, noop, rood untouched); gate 12
  either plays them or carries a reason-carrying, evidence-backed exclusion
  (never silent, never a bare FAIL).
- All twelve gates green in the worktree.
May touch: backends/clingo/**, corpus/golden/** (new goldens only),
toka/cases/parity.manifest, spec/ASP-TRANSLATION.md, scripts/check-drools.sh
(ONLY for a reasoned exclusion of a new golden), scripts/check-noop.sh (ONLY
if a survivor entry is unavoidable — prefer case design that refutes noop),
backlog/.
Out of scope: ref/, spec/IR-SPEC.md, spec/RUNTIME-SEMANTICS.md, spec/schema/,
spec/verifier/, frontends/**, backends/drools/** (code), the remaining fault
classes (allocation case input, datum-tijd fold, unit algebra / Onbekend),
spec/CONSTRUCTS.md, spec/PLAN.md, any toolchain commit.

## Result — clingo term encodings (closed 2026-08-28)

Landed on `card/clingo-term-encoderingen` (7 commits: the four encoding
commits, the goldens+pins commit, the ASP-TRANSLATION commit, the gate-12
record, and this review-fix round). All twelve gates green in the worktree.
Nothing outside the may-touch set changed.

### What landed
Four pure encoding gaps closed in `backends/clingo/lib/{translate,model}.ml`:
1. **Quote/backslash escaping** on BOTH the literal (`symbolic_literal_term`)
   and value (`symbolic_value_term`) sides; the expectation-parser (`model.ml`
   `split_top_commas`/`unescape_str`) made escape-aware and symmetric; control
   chars <0x20 keep faulting.
2. **Begrensd (clamp)** term arm (min-only / max-only / min+max) via the integer
   identities `max=(a+b+|a−b|)/2`, `min=(a+b−|a−b|)/2` on the harmonised scale;
   an empty base propagates; unit harmonisation explicit; a real unit factor ≠1
   or a non-ground bound faults loudly.
3. **MAXIMUM/MINIMUM over a set**-typed AttribuutRef (`#max`/`#min`) with a
   MANDATORY presence guard so empty/all-empty derives nothing (`#sup`/`#inf`
   never leak).
4. **SOM without a leegwaarde**: presence guard so an empty set yields leeg (ref
   `agg_som None → Empty`), not `#sum`'s 0; the 0-fallback path unchanged; a
   non-0 fallback still faults.

Four rights-free own-authored Jutterswet IR-only goldens witness each form (each
with a hand-computed NOTE.md, all noop-refuting): `jutterswet-keurmerk` (quote),
`jutterswet-grensbedrag` (clamp, 3 positions + empty base), `jutterswet-vondstpiek`
(max/min set), `jutterswet-buitsom` (som-no-fallback). Six toka pins moved clingo
FAULT→PASS in `toka/cases/parity.manifest` (clingo 16→22 of 30). `spec/ASP-
TRANSLATION.md` extended S2/S6/S8 and S19 shrank by exactly those sentences.

### Red-first evidence (before the change, manifest fragments only)
The clingo runner exited non-zero on each of the six toka ir/case pairs with its
pinned manifest fragment: toka-20 `som without the 0 fallback`; toka-21 + toka-24
`expression kind not translated`; toka-46-woonregio-factor-1/2/3 `carries a
character the ASP string encoding cannot quote`.

### Gate outcomes (all exit 0, re-run this round in the worktree)
1 self-contained · 2 ir-schema (148) · 3 verifier (148 golden / 161 negatief) ·
4 case-schema (371) · 5 oracle (349) · 6 clingo (349) · 7 noop (316 refute / 33
survivors, script unchanged) · 8 rood (22) · 9 catala (169 checks, 11 catala
goldens, default opam switch) · 10 regelspraak (130 tests, 19 rgs) · 11 toka-
parity (clingo 22/30, 0 silent diverge; 8 residual = allocation ×6 + fold/datum-
tijd ×1 + non-numeric ×1) · 12 drools (played 49, auto-excluded 72, value-
divergence 4, no-case 1).

### Review round — how each lens finding was handled
Both adversarial lenses returned NO critical/major (both "merge-klaar: JA"); six
minors total, handled as:
- **[fixed] Lens1-m4 doc-nit** — the S2 literal-side cross-ref pointed at S3;
  corrected to S4, where the literal's `symbolic_term`→S2 lowering is defined.
- **[fixed in comment] Lens1-m1 leegwaarde-on-min/max asymmetry** — verified
  real: ref eagerly evaluates and DISCARDS the leegwaarde for min/max, so a
  benign leegwaarde agrees and only a leegwaarde whose OWN eval faults diverges
  (no v0 witness, no frontend path onto min/max). A code fault-on-present would
  instead diverge on a benign leegwaarde, so "ignore" stays the ref-faithful
  default; the incomplete code comment is corrected to record the bounded
  residual. No behavioural change (all gates green after).
- **[verified, witness deferred] Lens2-m1 backslash escaping unwitnessed** —
  empirically proven correct: clingo 5.8.2 round-trips `val(a,"…\\…")`, the
  equality join with an embedded backslash matches, and `model.ml`'s escape-aware
  parser unescapes symmetrically. A permanent golden witness necessarily lands in
  `keurmerk` (the only text-carrying golden), whose gate-12 exclusion evidence is
  deliberately quote-specific; adding a backslash would muddy that reason-carrying
  exclusion for negligible incremental assurance. Documented, not added.
- **[maintainer note] Lens1-m2 keurmerk breaks the differential square** — card-
  sanctioned (a reason-carrying, evidence-backed gate-12 exclusion of a new golden
  is explicitly permitted by this card). The exclusion sits in `scripts/check-
  drools.sh` EXCLUDE_KNOWN with proof (the quote-stripped byte-identical variant
  PLAYS+PASSES through the same jar; the stale-guard fails if keurmerk ever plays).
  This is the one point wanting conscious maintainer confirmation: admitting a
  *synthesized* golden that breaks Drools≡ref≡clingo (unlike the hand-written
  anchor precedents). v1-delta candidate: escape embedded quotes/backslashes in
  the Drools exec-model text equality.
- **[rejected, no defect] Lens1-m3 all-zero som corner unwitnessed** — the som
  presence-guard tests presence, not value, so an all-present-zero contributor set
  correctly derives som 0 (present), not leeg, by construction; buitsom's J3
  already pins the all-empty→leeg edge. A contrived all-zero case adds marginal
  assurance only.
- **[rejected, no defect] Lens2-m2 Constante bound / min-max filter sub-paths** —
  both are ref-faithful, guarded extensions of the same mechanism (an atom-bearing/
  non-ground bound faults loudly; the filter is inherited verbatim from the som
  arm). No fault path demonstrated; no contrived witness added.

### Deliberately left (out of scope)
The 8 residual clingo faults are the card's declared out-of-scope classes:
allocation case input (×6 toka), the datum-tijd fold / navigated datum-tijd flank
(×1), the non-numeric arithmetic operand (×1). `ref/`, the schema, the verifier,
the frontends, and the Drools backend code were untouched. `scripts/check-noop.sh`
was not modified — all four goldens refute a do-nothing run, so no survivor entry
was needed.
