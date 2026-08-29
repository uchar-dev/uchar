# jutterswet-sleepbaan — hand computation

Witnesses the **unit-normalisation lowering pass** (LADDER-EVOLUTIE §8.1–8.2,
Q9; spec/ASP-TRANSLATION.md S6): a genuine **factor≠1** conversion — a
`km` literal added to an `m`-declared attribute, written into an `m` target —
that clingo refused before the pass and replays exactly after it, matching the
absolute value ref computes over the source units (ref ≡ clingo).

## The rule

`De sleeplengte van een Sleepbaan` (**m**, whole) :=
`2,5 km` (literal) **plus** `de aanloop` (**m**, whole, case input).

`Afstand` stelsel: `m` canonical (`factorNaarCanoniek = 1`, the **finest**
base unit), `km = 1000 m`. Finest-unit canonical ⇒ every restatement is
`× 1000`, multiply-only, integer-encodable.

## rood-eerst (clingo, pass DISABLED)

    TRANSLATION FAULT: non-identical units under the write to "sleeplengte"
    are not translated (ref restates with an exact conversion factor this
    integer encoding does not carry)

The additive join `km + m` and the write into the `m` target both crossed the
`units_identical_ok` fence; the write boundary reports first.

## The lowering (pass ENABLED)

The target `sleeplengte` is declared in the finest unit `m`, so the pass
restates every `Afstand` literal in the value INTO `m`:

- `2,5 km` → `2,5 × 1000 = 2500 m` (the decimal scale adjusts: 1 decimal in
  `km` becomes 0 decimals in `m`). The literal now reads unit `m`.
- `de aanloop` is already `m` — untouched.

Both operands now carry the identical unit `m`; the join and the write both
translate as raw integer arithmetic.

## Unit algebra — the reference value (RUNTIME-SEMANTICS ch. 2.6 / IR-SPEC 2.5)

ref over the SOURCE units:
- **plus**: left `2,5 km`; restate right `aanloop` into `km`:
  `300 m × factor(m)/factor(km) = 300 × 1/1000 = 0,3 km`.
  Sum = `2,5 + 0,3 = 2,8 km`.
- **write** into the `m` target (World.collapse): `2,8 km × 1000 = 2800 m`,
  fits 0 decimals.

clingo over the NORMALISED wire: `2500 m + aanloop` = `2500 + 300 = 2800 m`.

Both executors land on the SAME absolute value.

## Case (absolute expected value, hand-computed)

**case1** — aanloop = 300 m
- `2500 + 300 = 2800` → sleeplengte = **2800** (m).

## noop refutation (gate 7)

`sleeplengte` is a computed target absent from the input, so the `--noop`
read is empty and the 2800 expectation mismatches — the case refutes a
do-nothing executor. Not a defeasible-standing survivor.
