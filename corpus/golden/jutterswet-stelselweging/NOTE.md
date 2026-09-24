# jutterswet-stelselweging

The **cross-system unit** witness (`IR-SPEC.md` WF-023, WF-035, WF-036 and
WF-099, ch. 4.6; `RUNTIME-SEMANTICS.md` ch. 2.6). `jutterswet-vangstklem` pins
a bound in a convertible sister unit of the **same** unit system; this golden
pins the reading one step out — two units that are convertible by **canonical
name** while a **different declared unit system** carries each of them.

Convertibility here is a canonical-name law: two units are convertible when
their canonical names agree and their shape agrees. The declaring unit system
is not part of it. So a document may legitimately weigh a `Massa` ton against
a `Gewicht` kilogram, and an executor must restate it by the exact declared
factor like any other convertible pair.

## The two systems

```
Eenheidsysteem Gewicht
    het kilogram (mv: kilogrammen) kg

Eenheidsysteem Massa
    het kilogram (mv: kilogrammen) kg kgm
    de ton (mv: tonnen) t = 1000 kg
```

Both systems declare a kilogram, canonical `kg`, factor 1. A unit spelling
resolves against the first system that declares it, so `kg` and `kilogram`
reach **Gewicht**'s record, `t` reaches **Massa**'s, and the second
abbreviation `kgm` is what reaches **Massa**'s kilogram — a record identical
to Gewicht's in every field but `system`.

That gives the two cross-system pairs the document needs:

* **with a factor** — `t` (Massa, ×1000) against `kg` (Gewicht): one unit is
  restated into the other by an exact integer;
* **without a factor** — `kgm` (Massa, ×1) against `kg` (Gewicht): the same
  quantity in two records, exact conversion factor 1, nothing to restate.

## The four unit positions, each crossed

| rule | position | pair | needs |
|------|----------|------|-------|
| `ondergewicht` | WF-099 clamp bound | `1 t` (Massa) over a `kg` (Gewicht) base | factor ×1000 |
| `spiegelgewicht` | WF-023 write | `kg` (Gewicht) value into a `kgm` (Massa) target | factor 1 |
| `somgewicht` | WF-035 additive join | `kg` (Gewicht) plus `kgm` (Massa) | factor 1 |
| `toeslaggewicht` | WF-035 additive join | `kg` (Gewicht) plus `1 t` (Massa) | factor ×1000 |
| `lastgewicht` | WF-099 bound **and** WF-023 write | `2 t` (Massa) bound over a `kg` (Gewicht) base, written to a `kgm` (Massa) target | both at once |
| `keurgewicht` | WF-036 comparison | `kgm` (Massa) greater than `1000 kg` (Gewicht) | factor 1 |

`lastgewicht` is the one that crosses twice in a single rule: the target, the
base and the bound sit in three different records, two of them in the other
system from the target.

## Hand-computed absolutes (all at 2 decimals, all in kilograms)

With `x` the `ruwgewicht`:

```
ondergewicht   = max(x, 1000)          (1 t restated to 1000)
spiegelgewicht = x                     (factor 1, no restating)
somgewicht     = x + spiegelgewicht    = 2x
toeslaggewicht = x + 1000              (1 t restated to 1000)
lastgewicht    = min(x, 2000)          (2 t restated to 2000)
keurgewicht    = x when spiegelgewicht > 1000, else leeg
```

| Vangst | ruwgewicht | onder | spiegel | som | toeslag | last | keur |
|--------|-----------|-------|---------|-----|---------|------|------|
| V1 | 250,00 | **1000,00** | **250,00** | **500,00** | **1250,00** | **250,00** | leeg |
| V2 | 4200,00 | **4200,00** | **4200,00** | **8400,00** | **5200,00** | **2000,00** | **4200,00** |
| V3 | 1750,50 | **1750,50** | **1750,50** | **3501,00** | **2750,50** | **1750,50** | **1750,50** |
| V4 | 1000,00 | **1000,00** | **1000,00** | **2000,00** | **2000,00** | **1000,00** | leeg |
| V5 | 1000,01 | **1000,01** | **1000,01** | **2000,02** | **2000,01** | **1000,01** | **1000,01** |
| V6 | (leeg) | leeg | leeg | **0,00** | **1000,00** | leeg | leeg |

V4/V5 bracket the restated comparison threshold to the cent: at exactly
1000,00 the `groter dan` is false and `keurgewicht` stays leeg, one cent above
it fires. V3 is fractional on both sides of the doubling. V6 pins the empty
propagation of ch. 1.2a across the cross-system positions: a clamp over an
unbound base stays leeg, while the additive family coalesces — `somgewicht`
adds two empties to 0,00 and `toeslaggewicht` adds the restated 1000.

## What the values falsify

An executor that refused to restate across the system boundary and took the
bound as a bare number would answer `max(250; 1) = 250,00` for V1's
`ondergewicht` and `min(4200; 2) = 2,00` for V2's `lastgewicht`. One that
applied the ×1000 where no factor is due would answer 250000,00 for V1's
`spiegelgewicht`. Both mistakes are three orders of magnitude out and are
pinned here as absolutes.

## The differential square

`ref` ≡ `clingo` over every expectation above (gate 5 ≡ gate 6), anchored to
the hand-written absolutes and not read off any executor; the Drools backend
replays them on both transpiler branches (gates 12 and 13), so ref ≡ clingo ≡
Drools on the cross-system form. NOOP-REFUTING: every derived value is absent
from the raw input, so a do-nothing run mismatches it (gate 7). Gate 10
re-emits the source to this `ir.json` byte for byte.
