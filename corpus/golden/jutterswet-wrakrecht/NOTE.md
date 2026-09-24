# jutterswet-wrakrecht — object-type inheritance, authored in RegelSpraak

The **source twin** of the hand-written anchor
`corpus/golden/anchor/jutterswet-strandrechtketen`: the same three object
types, the same chain, the same fact type and the same four cases with the
same absolute values — but with an `ir.json` the RegelSpraak frontend
**emitted** from a `bron.rgs`. The anchor pins that all three executors agree
on inheritance; this golden pins that the dialect frontend produces exactly
that IR, so `Objecttype.supertype` is emitter-borne and not only hand-written.
The four `case*.json` files are byte-identical to the anchor's on purpose: one
set of expectations, replayed against a hand-written and against an emitted
document.

## Source (`bron.rgs`)

The uchar-dialect head form (`frontends/regelspraak/grammar/DIVERGENCES.md`
§5) declares the single direct supertype:

```
Objecttype het Wrakrecht is een Strandrecht (mv: Wrakrechten)
    de bergingsdiepte Numeriek (niet-negatief geheel getal);
```

`Wrakrecht` adds one own attribute and inherits `grondslag`, `uitkering`,
`registratiekosten` and the characteristic `betwist`. The aggregating
`Kustgemeente` registers `Strandrechten`; its two attributes are deliberately
**not** named after the role, which would put the case on top of the ch. 2.6
name-sharing shape this golden is not meant to test.

Four rule shapes, one per thing to prove:

- a **bare rule on the supertype** — `registratiekosten = 5,00`, the ch. 7.8
  standing norm, which must fire for subtype instances too;
- a **defaults group on `Strandrecht.uitkering`** — base `= de grondslag`,
  exception `= 99,00 indien het Strandrecht betwist is`;
- a **defaults group on `Wrakrecht.uitkering`** — base `= de grondslag maal 2`,
  own exception `= 55,00 indien zijn bergingsdiepte groter is dan 20`;
- two **aggregations on the Kustgemeente**, which range extensionally: the
  `Wrakrecht` counts as a `Strandrecht`.

## Lowering

Two `PrioritizedDefaults` groups, one per subject, each with the exception
above the base. The base rules carry no `indien`, so their defaults take the
vacuously true justification (`ALLE` over no parts, "altijd") WF-050 requires.
Both aggregations carry a `sourceRole` naming the one relation. Since L2 every
range is written and none is derived (`spec/proposals/V1-WIREMIGRATIE.md`
B12). Until then this mattered here and not in a flat golden: WF-048's
uniqueness was chain-closed, so a subtype was exactly what could make a
bindingless navigation ambiguous.

## Case derivations (hand-computed, absolute)

`grondslag = 10,00` throughout. The effective `Wrakrecht` stack for
`uitkering` is, highest first: its own 55-exception, the inherited
99-exception, its own base 20 (which replaced the inherited base).

| case | instance | betwist | bergingsdiepte | uitkering | what it proves |
|------|----------|---------|----------------|-----------|----------------|
| case1-basis | S1 Strandrecht | no  | —  | **10,00** | the supertype's own base |
| case1-basis | W1 Wrakrecht   | no  | 5  | **20,00** | base override: specific beats general |
| case2-geerfde-uitzondering | S1 | yes | —  | **99,00** | the supertype's own exception |
| case2-geerfde-uitzondering | W1 | yes | 5  | **99,00** | the inherited exception outranks the subtype base |
| case3-eigen-uitzondering | W1 | no  | 25 | **55,00** | the subtype's own exception over its own base |
| case4-laaggrensvoorrang  | W1 | yes | 25 | **55,00** | both exceptions applicable: the child's definition wins |

`registratiekosten` is **5,00** on every instance, subtype included — the
supertype's bare rule firing for a subtype. `uitkeringstotaal` and
`registertal` count both instances (30,00 / 2 in case1, 198,00 / 2 in case2,
55,00 / 1 in cases 3 and 4): a non-extensional reading would give 10,00 / 1
and fail loudly. All four cases are `consistent`; case4 is an ordinary write,
not a conflict.

Cases 1 and 2 together are the penguin test, and they are the two rows a flat
lowering cannot produce at once: flattening gives 20,00 for case2's W1 or
99,00 for case1's W1, never both.
