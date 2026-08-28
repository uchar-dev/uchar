# jutterswet-sleeprest — hand computation

Witnesses the **factor-1 unit admission** (ASP-TRANSLATION S6): a MAAL of a
unitless factor with a unit-carrying attribute, consumed by a restating
position (MIN) whose other side is a unitless literal that adopts the unit.
Every unit meeting here is factor-FREE — no `factorNaarCanoniek` is ever
applied — so the reference interpreter's exact-rational result equals the
raw integer arithmetic, and clingo replays it exactly (ref ≡ clingo ≡
Drools).

## The rule

`De resterende sleepafstand van een Berging` (km, 2 decimals) :=
`50` (unitless literal) **min** (`bergingsfactor` (unitless, 2 dec) **maal**
`sleepafstand` (km, whole)).

## Unit algebra (RUNTIME-SEMANTICS ch. 2.6)

- **maal**: `Unitless × Unit km → Unit km`, value unchanged (`Arith.mul`,
  `(Unitless, Unit u) -> Num(v, u)`; NO factor). The km attribute's declared
  `factorNaarCanoniek = 1000` is never exercised — km never meets a
  different unit.
- **min**: the unitless left literal adopts the right operand's unit
  (`restate_into`, `Unitless, Unit _ -> q`; value unchanged). Result unit km.
- **write** into the km target: `restate_into km q km` with `conv_factor
  km km = 1000/1000 = 1` — factor 1, value unchanged; fits 2 decimals.

## Cases (absolute expected values, hand-computed)

**case1** — bergingsfactor = 0,50 ; sleepafstand = 12 km
- maal: 0,50 × 12 = **6,00** km
- min:  50 − 6,00 = **44,00** km  → resterende sleepafstand = **44,00**

**case2** — bergingsfactor = 1,25 ; sleepafstand = 8 km
- maal: 1,25 × 8 = **10,00** km
- min:  50 − 10,00 = **40,00** km → resterende sleepafstand = **40,00**

## noop refutation (gate 7)

`resterende sleepafstand` is a computed target absent from the input, so the
`--noop` read is empty and the expectation (44,00 / 40,00) mismatches — the
case refutes a do-nothing executor. It is not a defeasible-standing survivor.

## The standing fence (loud-witness, not committed)

If the restating position met two DIFFERENT convertible units (e.g. a meter
literal min the km product, or a km literal min a meter product), the
reference interpreter would apply the real 1000 factor and produce a value,
while clingo refuses loudly (`non-identical units under … / the write …`).
Such an IR is neither golden (clingo cannot replay it) nor red (ref does not
fault), so it is demonstrated in the build log only.
