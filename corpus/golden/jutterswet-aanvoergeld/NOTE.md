# jutterswet-aanvoergeld

Until L2 the **`ParameterRef` unit witness** of the reference-type identity
rule (`IR-SPEC.md` ch. 4.1, WF-091). Before this golden the `ParameterRef`
arm of that rule had **no positive witness anywhere in the corpus**: 44
parameter references over golden + rood, not one of them unit-bearing, so a
verifier that OVER-rejected at that arm left every gate green. This document
closed that half. Since L2 a parameter reference carries no type of its own
(`spec/proposals/V1-WIREMIGRATIE.md` B13), so WF-091 has no copy to cross at
that arm. What the document pins since then is that two unit-bearing
parameters, read at the two positions a parameter reference can occupy,
take their declared units and give the hand-computed values in every
executor.

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

Since L2 a `ParameterReference`'s type is its parameter's declared datatype,
unit sub-object included (`kg` of system `Gewicht`, factor 1 to `kg`; `EUR` of
system `Munt`, factor 1 to `EUR`), read from the declaration
(`RUNTIME-SEMANTICS.md` §2.10). Until L2 each `ParameterRef` on the wire carried
a byte-copy of that datatype, and WF-091 crossed exactly that copy against the
declaration, facet by facet. The unit facet was the one no other rule could
reach: a kilogram/tonne copy is *compatible* (same canonical unit, factor 1000),
so WF-035's and WF-023's convertibility crosses admit it by construction.

**Two-sided, measured until L2.** The document was verifier-clean, and it
was a POSITIVE witness: a mutant that fired at the `ParameterRef` arm on
every unit-bearing use site turned this document — and, before this golden
landed, no document at all — red. The refusal half was pinned the other way
round on a mutated copy of this same `ir.json`: retyping the
`aanvoerdrempel` reference `t` (canonical `kg`, factor 1000) while the
declaration stayed `kg` was refused, arm-exact:

```
WF-091 FAIL /rules/resolvedRegels/0/voorwaarde/rechts/type: unit mismatch:
  parameter reference 'aanvoerdrempel' is typed 't' of system 'Gewicht'
  (factor 1000 to 'kg'), but the declaration it reads from is 'kg' of
  system 'Gewicht' (factor 1 to 'kg')
```

Since L2 that mutant has no wire form: a `type` on a `ParameterReference` is an
unknown field and is refused at load (the L2 load table in
`backends/clingo/test/test_clingo.ml`), and the negatief
`wf091-parameterref-eenheid` left the corpus with that field
(`corpus/negatief/README.md`).

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
