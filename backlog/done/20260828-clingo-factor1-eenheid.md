# clingo: the factor-1 unit admission — the Onbekend sentinel and unitless-adopts-unit

Goal: the last clingo fault class on the toka replay closes with the
NARROWEST sound admission (maintainer decision 2026-08-28, option (a)):
translate only what provably needs NO unit-conversion factor; every
position that would need a real factor != 1 keeps faulting loudly. ref is
the meaning and is NOT touched (ref computes full unit algebra in exact
rationals; RUNTIME-SEMANTICS ch. 2.2/2.6). Two fences narrow:
(1) numeric_ok (translate.ml, pre-card-1 lines 585-590) currently refuses
    the Datatype_onbekend type sentinel outright. It now admits an
    Onbekend-typed expression ONLY when the expression is provably numeric
    — e.g. a Binair whose operands themselves pass the numeric checks
    recursively. The sentinel is the v0 frontend's typing inability, not an
    actual non-numeric value; genuinely non-numeric arithmetic (Tekst,
    Boolean, enum, date through the numeric path) keeps faulting with the
    existing fragment.
(2) unit_free_ok / units_identical_ok (pre-card-1 lines 640-662) currently
    refuse every unit-carrying multiplicative operand and every non-identical
    restating pair. They now admit EXACTLY the factor-free cases ref computes
    without applying any conversion:
    - multiplicative: unitless x unit-carrying — the product adopts the unit,
      the numeric value is unchanged (Arith.mul, ref/lib/arith.ml:96-109:
      (Unitless, Unit u) -> Num(Q.mul lq rq, u); no factor);
    - restating positions (MIN/PLUS/comparison/...): a unitless side adopts
      the other side's unit, value unchanged (restate_into,
      ref/lib/arith.ml:52-60: Unitless, Unit _ -> q).
    Unit x unit products, divisions/rates, and any restating pair of two
    DIFFERENT units (a real factorNaarCanoniek application) keep faulting
    loudly — that is the standing fence until the ladder design round
    decides the waist question below.
    Scale harmonisation stays exact: the adopting side follows the existing
    decimal-scale machinery unchanged; no new rounding may appear.

One rights-free, own-authored Jutterswet golden (hand-computed absolute
values, noop-refuting) witnesses the admitted shape: a MAAL of a unitless
factor with a unit-carrying attribute, consumed by a restating position
(MIN or a comparison) whose other side is unitless or same-unit — the
toka-45 shape in own material. A LOUD-witness is demonstrated (not
committed — ref would pass it, so it can be neither golden nor rood): an IR
where two DIFFERENT convertible units meet in a restating position still
faults with the units fragment; shown in the build log and summarised in
the Result.

The pin scenario-toka-45-belasting-op-afstand moves FAULT->PASS in
toka/cases/parity.manifest: clingo 30 of 30, the fault-class list EMPTY
(verify scripts/check-toka-parity.sh renders the zero-fault state cleanly —
the script itself should need no change; if it does, that is a finding to
report, not to silently patch). spec/ASP-TRANSLATION.md: S6 gains the
factor-1 admission paragraph with the ref anchors; the S19 sentences narrow
precisely — the non-numeric-operand sentence to genuinely non-numeric
operands, the unit-algebra sentence to the factor-carrying residue. S19
stays the highest section and shrinks only by translation. NO toka IDs in
the spec text.

Registered for the ladder design round (recorded here, NOT built): the
waist question — universal unit conversion as an IR-level decision, LLVM/
MLIR-style: a canonical-unit core where units are static dimension tags,
frontends convert at emission, and the upper dialect keeps source units for
verbalisation/traceability; versus a specified normalisation lowering pass
per backend. Maintainer words (2026-08-28): "(a) nu en wie weet pakken we
universele eenheid conversie in de ir aan samen met het ladder ontwerp."

Acceptance:
- Red-first: BEFORE the change, the clingo runner on the toka-45 pair exits
  non-zero matching "non-numeric arithmetic operand", and the new golden
  faults on the same fence chain (evidence summarised without casus
  content).
- After: bash scripts/check-toka-parity.sh green with the toka-45 pin at
  PASS|- — clingo 30 PASS / 0 faults / 0 silent diverge.
- The loud-witness (two different units in a restating position) still
  faults, demonstrated in the log.
- The new golden passes gates 2-8; gate 12 plays it or carries a
  reason-carrying, evidence-backed exclusion (never silent).
- All twelve gates green in the worktree.
May touch: backends/clingo/**, corpus/golden/** (new golden only),
toka/cases/parity.manifest, spec/ASP-TRANSLATION.md, scripts/check-drools.sh
(ONLY for a reasoned exclusion of the new golden), backlog/.
Out of scope: ref/, spec/IR-SPEC.md, spec/RUNTIME-SEMANTICS.md, spec/schema/,
spec/verifier/, frontends/**, backends/drools/** (code), any conversion with
factor != 1 (the ladder-round waist question), scripts/check-noop.sh
survivor entries (design the case to refute noop), scripts/check-toka-parity.sh
(findings reported, not patched), spec/CONSTRUCTS.md, spec/PLAN.md, any
toolchain commit.

## Result

The factor-1 unit admission landed and its adversarial review round is
handled. All twelve gates are green in the worktree, committed on branch
`card/clingo-factor1-eenheid` (no push, no merge). The root checkout was
only read from (jar copy), never modified.

### The admission (backends/clingo/lib/translate.ml)

1. `numeric_ok` made recursive — admits the `Datatype_onbekend` typing-
   inability sentinel only when the node is a provably-numeric arithmetic
   `Binair` (operands numeric recursively); a resolved non-numeric operand
   still faults. Witnessed by toka-45 (gate 11).
2. The rule-level `Maal` fence admits the factor-free unitless × unit-
   carrying product (adopts the unit, value unchanged; `Arith.mul`
   `(Unitless, Unit u) -> Num(v, u)`, no factor). `Percentage_van`,
   closed-body `Maal`, divisions/rates and `Tot_de_macht` stay strict.
3. `eff_unit` gains a `Maal` branch so the adopted product unit flows into
   restating positions — this is what keeps the loud-witness firing.

### Review round — one CRITICAL fixed (the narrowest sound fix)

Lens 1 found a card-introduced silent divergence: the first `mul_units_ok`
faulted only when BOTH operands had a *visible* unit (`eff_unit = Some`).
Because `eff_unit` returns `None` for nodes it cannot analyse (a `Let`, a
field access, a division, an Onbekend declaration), a unit hidden behind
such a node on the "unitless" side made a real unit×unit product compute
the raw integer silently. Reproduced on the branch before the fix, on a
verifier-clean IR `MAAL(sleepafstand[km], Let g = sleepafstand[km] in g)`:
ref FAULTS `incompatible units: km vs km*km`, clingo silently PASSED the
raw product 144.

Fix: a new `provably_unitless` predicate (mirrors `eff_unit`'s resolved
forms but PROVES the absence of a unit — a declaration counts only when its
datatype is explicitly Numeriek/Percentage with `eenheid` null; an Onbekend
or unanalysable node is never proven). `mul_units_ok` now admits the
one-visible-unit case ONLY when the other side is `provably_unitless`, else
it faults loudly. After the fix the same witness FAULTS loudly ("a
multiplicative operand … carries no statically resolvable unit …"); the
golden and toka-45 are unaffected (both unitless sides are proven-free leaf
declarations). `ref/`, `eff_unit`'s existing behaviour, and every other
fence are untouched — the change only tightens the new admission.

Lens 1's minor (the `numeric_ok` Onbekend-recursion leaf-gap) is a motivated
non-fix: it is unreachable in the corpus (WF-034 rejects an Onbekend sentinel
inside a golden, gate 3) and is the pre-existing `None -> ()` untyped-operand
policy surfacing, not new corpus-reachable exposure; tightening it would
regress every untyped-numeric case. The over-strong S19 clause it flagged is
softened. Lens 2's minor (fence-1 has no rights-free golden witness — only the
private toka-45) is recorded as a durability item for the public flip: a golden
CANNOT witness the Onbekend sentinel because the verifier (correctly) rejects
that sentinel in golden material, so no corpus artifact can pin fence-1 while
the toka zone exists.

### Loud-witness (demonstrated, not committed)

A verifier-clean IR with two DIFFERENT convertible units (m vs km) in a
restating position: the golden shape with the MIN's left literal carrying
`m` instead of unitless. ref PASSES it (applies the real 1000 factor →
−5,95 km); clingo still FAULTS with the units fragment ("non-identical
units under the write …"). So it is neither golden nor rood.

### New golden `corpus/golden/jutterswet-sleeprest/`

Own Jutterswet strandjutten material (no TOKA): a unitless `bergingsfactor`
MAAL a km `sleepafstand`, consumed by a unitless-adopting MIN; hand-computed
44,00 / 40,00 in `NOTE.md`; noop-refuting; plays ref ≡ clingo ≡ Drools.
`toka/cases/parity.manifest`: toka-45 clingo column FAULT→PASS.

### Registered for the ladder design round (not built)

The waist question — universal unit conversion as an IR-level decision
(LLVM/MLIR-style canonical-unit core with static dimension tags, frontends
converting at emission, versus a per-backend normalisation lowering pass).
Maintainer (2026-08-28): "(a) nu en wie weet pakken we universele eenheid
conversie in de ir aan samen met het ladder ontwerp."

### Gates (all twelve green in the worktree)

1 self-contained PASS · 2 ir-schema PASS (151) · 3 verifier PASS
(151 golden / 161 negatief) · 4 case-schema PASS (377) · 5 oracle PASS
(355) · 6 clingo PASS (355, ref ≡ clingo) · 7 noop PASS (322 refute /
33 survivors) · 8 rood PASS (22) · 9 catala PASS (169 checks, 11 goldens) ·
10 regelspraak PASS (130 tests, 19 rgs) · 11 toka-parity PASS (clingo
30 PASS / 0 fault / 0 diverge; fault-class list EMPTY; script needed no
change — no finding) · 12 drools PASS (52 played incl. jutterswet-sleeprest,
0 exclusion added).
