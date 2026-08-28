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
