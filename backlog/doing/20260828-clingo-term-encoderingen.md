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
