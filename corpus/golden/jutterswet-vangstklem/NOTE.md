# jutterswet-vangstklem

The **unit-bearing clamp** witness (`IR-SPEC.md` WF-099, ch. 4.6;
`RUNTIME-SEMANTICS.md` ch. 2.6 + ch. 2.7). `jutterswet-grensbedrag` pins the
clamp's *both-unitless* reading; this golden pins the other admitted one — a
bound in a **convertible sister unit** of the base — so the reading WF-099
turns on is carried by a gated document and not only by the verifier's
negatief witness.

One unit system, `Gewicht`: `kg` (canonical, factor 1) and `t` (canonical
`kg`, factor 1000). The base `ruwgewicht` is `kg` on every arm; the three
clamps place the sister unit in each of the three bound positions the
encoding has:

```
Het ondergewicht … als het ruwgewicht van de Vangst, met een minimum van 1 t.
Het bovengewicht … als het ruwgewicht van de Vangst, met een maximum van 2,50 t.
Het bandgewicht  … als het ruwgewicht van de Vangst, met een minimum van 1 t
                     en een maximum van 2500 kg.
```

* `ondergewicht` — min-only, bound in `t`.
* `bovengewicht` — max-only, bound in `t`, and **fractional**: the factor
  multiplies a two-decimal lexeme, not just an integer.
* `bandgewicht` — both bounds, **mixed**: the minimum in the sister unit,
  the maximum in the base unit. Its maximum (`2500 kg`) is the same weight
  `bovengewicht` reaches by restating (`2,50 t`), so the document checks its
  own conversion: the two arms must agree on 2500,00 or the restating is
  wrong.

## The restating, by hand (ch. 2.6, exact factors; ch. 2.7, minimum first)

The clamp compares and returns in the **base's** unit — ch. 2.10 gives a
composite position its effective type from the `base`, and `kg` is
canonical here, so each bound is restated by its own factor:

| bound     | as written | × factor | in `kg`     |
|-----------|------------|----------|-------------|
| minimum   | `1 t`      | × 1000   | **1000,00** |
| maximum   | `2,50 t`   | × 1000   | **2500,00** |
| maximum   | `2500 kg`  | × 1      | **2500,00** |

## Hand-computed absolutes (all at 2 decimals, all in `kg`)

`ondergewicht = max(x, 1000)`, `bovengewicht = min(x, 2500)`,
`bandgewicht = min(max(x, 1000), 2500)`.

| Vangst | ruwgewicht | ondergewicht | bovengewicht | bandgewicht |
|--------|------------|--------------|--------------|-------------|
| V1     | 250,00     | **1000,00**  | **250,00**   | **1000,00** |
| V2     | 4200,00    | **4200,00**  | **2500,00**  | **2500,00** |
| V3     | 1750,50    | **1750,50**  | **1750,50**  | **1750,50** |
| V4     | 999,99     | **1000,00**  | **999,99**   | **1000,00** |
| V5     | 1000,01    | **1000,01**  | **1000,01**  | **1000,01** |
| V6     | 2499,99    | **2499,99**  | **2499,99**  | **2499,99** |
| V7     | 2500,01    | **2500,01**  | **2500,00**  | **2500,00** |
| V8     | (leeg)     | leeg         | leeg         | leeg        |

V4/V5 bracket the restated **minimum** to the cent (999,99 clamps up to
1000,00; 1000,01 passes through), V6/V7 bracket the restated **maximum** the
same way. V8 pins the empty-base propagation: the base is unbound, so all
three clamps stay leeg.

## What the values falsify

An executor that took a bound as a bare number in the base's unit — the
failure the restating exists to prevent — would answer `max(250; 1) = 250,00`
for V1's `ondergewicht` and `min(4200; 2,50) = 2,50` for V2's `bovengewicht`.
Both are off by three orders of magnitude and are pinned here as absolutes,
so the mistake cannot pass any of the three executors.

## The differential square

`ref` ≡ `clingo` over every expectation above (gate 5 ≡ gate 6), anchored to
the hand-written absolutes and not read off any executor; the Drools backend
replays them (gate 12), so ref ≡ clingo ≡ Drools. NOOP-REFUTING: all
twenty-one derived values are computed and absent from the raw input, so a
do-nothing run mismatches every one of them (gate 7). Gate 13 twists this
golden's `kg` annotations like any other: the `Clamp` node's own
annotation is derived on both Drools legs and reaches only `ref` and
`clingo`. Until L2 the `AttribuutRef` positions probed all four legs; since
L2 a reference carries no annotation (B13).
