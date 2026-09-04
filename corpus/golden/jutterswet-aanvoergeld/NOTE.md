# jutterswet-aanvoergeld

The **`ParameterRef` unit witness** of the reference-type identity rule
(`IR-SPEC.md` ch. 4.1, WF-091). Until this golden the `ParameterRef` arm
of that rule had **no positive witness anywhere in the corpus**: 44
parameter references over golden + rood, not one of them unit-bearing, so
a verifier that OVER-rejected at that arm left every gate green. This
document closes that half.

## What it pins

Two unit-bearing parameters, in two different unit systems, read at the
two positions a parameter reference can occupy:

```
Parameter de aanvoerdrempel : Numeriek (niet-negatief getal met 2 decimalen) met eenheid kg
Parameter het aanvoertarief : Numeriek (getal met 2 decimalen) met eenheid €

Een Jutter is aanvoerplichtig
indien zijn aangevoerd gewicht groter of gelijk is aan de aanvoerdrempel.   <- condition

Het aanvoergeld van een Jutter moet berekend worden als
    zijn aanvoerwaarde plus het aanvoertarief.                              <- value
```

Each `ParameterRef` on the wire carries a `type` that is a byte-copy of
its parameter's declared datatype, unit sub-object included (`kg` of
system `Gewicht`, factor 1 to `kg`; `EUR` of system `Munt`, factor 1 to
`EUR`). WF-091 crosses exactly that copy against the declaration, facet
by facet, and the unit facet is the one no other rule can reach — a
kilogram/tonne copy is *compatible* (same canonical unit, factor 1000),
so WF-035's and WF-023's convertibility crosses admit it by construction.

**Two-sided, measured.** The document is verifier-clean, and it is a
POSITIVE witness: a mutant that fires at the `ParameterRef` arm on every
unit-bearing use site turns this document — and, before this golden
landed, no document at all — red. The refusal half is pinned the other
way round on a mutated copy of this same `ir.json`: retyping the
`aanvoerdrempel` reference `t` (canonical `kg`, factor 1000) while the
declaration stays `kg` is refused, arm-exact:

```
WF-091 FAIL /resolvedRegels/0/voorwaarde/rechts/type: unit mismatch:
  parameter reference 'aanvoerdrempel' is typed 't' of system 'Gewicht'
  (factor 1000 to 'kg'), but the declaration it reads from is 'kg' of
  system 'Gewicht' (factor 1 to 'kg')
```

(The permanent form of that refusal is the negatief
`corpus/negatief/wf091-parameterref-eenheid`; what this golden adds is
the non-over-rejection the negatief can never pin.)

## Hand-computed absolutes

Parameters: `aanvoerdrempel` = 500,00 kg, `aanvoertarief` = 12,50 €.
Both comparison operands are in `kg`, both addition operands in `EUR`, so
no restatement happens anywhere; the sums are exact at two decimals and
the target carries two decimals, so the ch. 2.4 assignment gate rounds
nothing.

| Jutter | aangevoerd gewicht | ≥ 500,00 kg? | aanvoerplichtig | aanvoerwaarde | aanvoergeld |
|--------|--------------------|--------------|-----------------|---------------|-------------|
| J1     | 640,25 kg          | 640,25 ≥ 500,00 | **present**  | 310,00 €      | 310,00 + 12,50 = **322,50** |
| J2     | 499,99 kg          | 499,99 < 500,00 | **absent**   | 87,40 €       | 87,40 + 12,50 = **99,90** |
| J3     | 500,00 kg          | 500,00 ≥ 500,00 (the boundary) | **present** | 0,00 € | 0,00 + 12,50 = **12,50** |

J3 is the `groter of gelijk` boundary: equality holds, so the
characteristic is present. No consistency rule fires → `consistent`.

## The differential square

`ref` (gate 5) ≡ `clingo` (gate 6) ≡ Drools (gate 12) on the seven
expectations above, anchored to the hand computation, never to parity.
NOOP-REFUTING: `aanvoergeld` and `aanvoerplichtig` are absent from the
raw input world, so a do-nothing run mismatches all six derived
expectations (gate 7). Gate 10 re-emits `bron.rgs` byte-identically.

## Measured boundary of the form

Two richer shapes of the same witness were built and rejected by
measurement, not by taste — each would have turned gate 6 red:

* a `t`-declared parameter compared against a `kg` attribute (truthful
  types on both sides, a genuine conversion) —
  `TRANSLATION FAULT: non-identical units under a comparison are not
  translated (ref restates with an exact conversion factor this integer
  encoding does not carry)`;
* a composite-unit rate (`€/kg`) multiplied by the weight, which cancels
  to `€` — `TRANSLATION FAULT: non-identical units under the write to
  "aanvoergeld" are not translated (…)`, the ASP backend's unit-free
  requirement on multiplicative operands.

Both forms are carried by `ref` and by Drools; only the ASP encoding
declines them. A golden must pass every gate, so the witness stays inside
the identically-united additive/comparison subset all three executors
carry.
