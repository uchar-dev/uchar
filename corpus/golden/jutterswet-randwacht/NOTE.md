# jutterswet-randwacht

The **fence-carriage witness** of `ASP-TRANSLATION.md` S7: the claim that
*every* fence a composite member emits carries the ch. 5.10 prefix. Two
of them did not, and nothing in the corpus noticed.

A member the outcome has already settled away must emit no fault — not
only no *division* fault, but no **range** fault either. The clingo
translation fences arithmetic against machine-integer wrap, and two of
those fences (the numeric **equality** arm and the **ordering** arm) built
their bodies without the prefix. The verdicts were right, so every gate
stayed green; only the fault surface was wrong.

## What it pins

```
Het gelijkteken van een Randwacht moet gesteld worden op 1
indien hij aan alle volgende voorwaarden voldoet:
- zijn ploegomvang is gelijk aan 0
- zijn grootgetal is gelijk aan zijn bedrag.        <- de gelijkheidsarm

Het ordeteken van een Randwacht moet gesteld worden op 1
indien hij aan alle volgende voorwaarden voldoet:
- zijn ploegomvang is gelijk aan 0
- zijn grootgetal is groter dan zijn bedrag.        <- de orderingsarm
```

`grootgetal` is a whole number and `bedrag` carries two decimals, so
comparing them harmonises by ×100 and the fence bound drops to
`max_int31 / 100`. `R1` puts 30 000 000 above that bound **and** makes
member 1 false, so the fence sits behind a stop: it must not fire.

## Hand-computed absolutes

| Randwacht | ploegomvang | grootgetal | bedrag | member 1 | `gelijkteken` | `ordeteken` |
|-----------|-------------|-----------|--------|----------|---------------|-------------|
| R1 | 1 | 30 000 000 | 1,00 | **false** (1 ≠ 0) | **leeg** | **leeg** |
| R2 | 0 | 5 | 1,00 | true | 5 = 1,00 → false → **leeg** | 5 > 1,00 → true → **1** |
| R3 | 0 | 1 | 1,00 | true | 1 = 1,00 → true → **1** | 1 > 1,00 → false → **leeg** |

`R1` is the stop; member 2 is never read, so its range fence must stay
silent. `R2` and `R3` are the controls: they read member 2 with values
inside the bound and let it decide each rule in both directions — which
is what keeps `R1`'s two leegs from being explainable by a composite that
never evaluates its second member.

## The differential square

`ref` ≡ `clingo` over all six expectations (gate 5 ≡ gate 6), anchored to
the hand-written absolutes; gate 10 re-emits `source.rgs` byte-identically.
NOOP-REFUTING: `R2`'s `ordeteken` and `R3`'s `gelijkteken` are derived and
absent from the raw input (gate 7). Mutation-wise this golden is the
artifact that kills the two "fence without the prefix" mutants: with
either arm's guard removed, `R1` turns clingo `UNSATISFIABLE` with
`viol("arithmetic range exceeded …")` while ref still passes.
