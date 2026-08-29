# jutterswet-vergunningsduur — hand computation

Witnesses the **unit-normalisation lowering pass**
(PRIORITIZED-DEFAULTS-EVOLUTIE §8.1–8.2, Q9; spec/ASP-TRANSLATION.md S6)
with a **non-power-of-ten factor**: a `jaar`
literal (`= 12 maand`) added to a `maand`-declared attribute, written into a
`maand` target. The `×12` restatement proves the finest-unit canonical is a
genuine integer multiply, not a decimal shift. clingo refused it before the
pass and replays it exactly after (ref ≡ clingo).

## The rule

`De looptijd van een Bergingsvergunning` (**maand**, whole) :=
`2 jaar` (literal) **plus** `de basisduur` (**maand**, whole, case input).

`Termijn` stelsel: `maand` canonical (`factorNaarCanoniek = 1`, the **finest**
base unit), `jaar = 12 maand`. These are plain NUMERIC units on a Numeriek
attribute (no timeline, no granularity): the additive join runs through the
exact-rational unit algebra, never the calendar kernel.

## rood-eerst (clingo, pass DISABLED)

    TRANSLATION FAULT: non-identical units under the write to "looptijd"
    are not translated (ref restates with an exact conversion factor this
    integer encoding does not carry)

## The lowering (pass ENABLED)

The target `looptijd` is declared in the finest unit `maand`, so the pass
restates every `Termijn` literal in the value INTO `maand`:

- `2 jaar` → `2 × 12 = 24 maand`. The literal now reads unit `maand`.
- `de basisduur` is already `maand` — untouched.

Both operands carry the identical unit `maand`; the join and write translate
as raw integer arithmetic.

## Unit algebra — the reference value (RUNTIME-SEMANTICS ch. 2.6 / IR-SPEC 2.5)

ref over the SOURCE units:
- **plus**: left `2 jaar`; restate right `basisduur` into `jaar`:
  `3 maand × factor(maand)/factor(jaar) = 3 × 1/12 = 1/4 jaar` (exact, not
  0,0833…). Sum = `2 + 1/4 = 9/4 jaar`.
- **write** into the `maand` target (World.collapse): `9/4 jaar × 12 = 27
  maand`, fits 0 decimals.

clingo over the NORMALISED wire: `24 maand + basisduur` = `24 + 3 = 27 maand`.

Both executors land on the SAME absolute value.

## Case (absolute expected value, hand-computed)

**case1** — basisduur = 3 maand
- `24 + 3 = 27` → looptijd = **27** (maand).

## noop refutation (gate 7)

`looptijd` is a computed target absent from the input, so the `--noop` read is
empty and the 27 expectation mismatches — the case refutes a do-nothing
executor. Not a defeasible-standing survivor.
