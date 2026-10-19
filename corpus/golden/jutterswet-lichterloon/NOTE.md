# jutterswet-lichterloon

The **sharing witness** of the let-lowering card (2026-09-01): the first
corpus golden whose `source.rgs` carries a §13.4.2.10 *variabelendeel*
(`Daarbij geldt:`) and whose `ir.json` therefore carries an
emitter-produced `Let` (`IR-SPEC.md` ch. 4.10).

## What it pins

The rule reads its one declared variable **twice**:

```
Het lichterloon van een Lichterbeurt moet berekend worden als
    het grondloon plus het grondloon maal het spoedtal van de Lichterbeurt.
Daarbij geldt:
    het grondloon is het vrachttal van de Lichterbeurt maal het eenheidsloon van de Lichterbeurt.
```

On the wire that is **one `Let` with two `BinderRef` reads** — not two
copies of the definition subtree. That is the whole point of the
lowering: the v0 route (which the pinned toolchain still takes)
substitutes the resolved subtree at every use site, so the definition is
duplicated and recomputed. Both reads are spelled `het grondloon`, with
the article the source language permits on a variable reference (§11);
the binder name on the wire is the declared name minus that article.

The variable is read in the value part only, so the rule's version has no
`condition` and the whole variabelendeel lives inside the `Let`.

## Hand-computed absolutes

`grondloon = vrachttal × eenheidsloon`, `lichterloon = grondloon +
grondloon × spoedtal` (the unit-identity of besluit 16Y types the
unitless × € product as €, so every operand of the sum is €).

| Lichterbeurt | vrachttal | eenheidsloon | grondloon | spoedtal | lichterloon        |
|--------------|-----------|--------------|-----------|----------|--------------------|
| L1           | 3         | 40 €         | 120 €     | 2        | 120 + 240 = **360 €** |
| L2           | 5         | 20 €         | 100 €     | 0        | 100 + 0  = **100 €** |

## The differential square

`ref` ≡ `clingo` over both expectations (gate 5 ≡ gate 6), anchored to
these hand-written absolutes; the Drools backend PLAYS it (gate 12), so
ref ≡ clingo ≡ Drools. Gate 10 re-emits `source.rgs` to a byte-identical
`ir.json`. NOOP-REFUTING: `lichterloon` is computed and absent from the
raw input, so a do-nothing run mismatches it (gate 7).
