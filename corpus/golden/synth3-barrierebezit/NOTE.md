# synth3-barrierebezit

The **barrier-ownership** witness of ch. 10.6 clause 2: *"the barrier
belongs to the `Let` that bound the binder that was read: a nested `Let`
empties **itself**, never an enclosing one"* — and, read the other way,
a read of an **enclosing** binder empties the **enclosing** `Let`, right
through any nested one in between. Hand-written (synth3 family, no
`source.rgs`): the shape is a kernel-level question, not a source-language
one.

## The shape

```
heffing := Let grondslag = <zijn grondslag>
           in ((Let opgehoogd = grondslag plus 1,00 in opgehoogd) plus 100,00)
```

The read of `grondslag` sits **inside the inner `Let`'s value**. When
`grondslag` is absent, that read raises the **outer** `Let`'s barrier.
The inner `Let` must not catch it: the binder that was read is the
outer one, so it is the outer `Let` that goes leeg — and with it the
whole rule value.

The `plus 100,00` is the **detector token**. An executor that let the
inner `Let` swallow the outer barrier would end up with a leeg inner
`Let`, and `leeg plus 100,00` **coalesces** (the ch. 1.2 plus row) to
`100,00`. So a mis-owned barrier does not merely propagate differently
— it produces a *different number*, which the pinned expectation
catches.

## Hand-computed absolutes

| Jutter | `grondslag` | `opgehoogd`   | `heffing`                                  |
|--------|-------------|---------------|--------------------------------------------|
| J1     | *absent*    | never binds   | **leeg** — the read fires the OUTER barrier |
| J2     | 3,00        | 3,00 + 1,00 = 4,00 | 4,00 + 100,00 = **104,00**            |

J2 is the filled control: it pins that the nesting computes the ordinary
answer, so J1's leeg is the barrier and not a broken chain. A shared or
global barrier token gives J1 **100,00** instead of leeg — the mutant
this golden is here to catch, in `ref` (gate 5), in clingo (gate 6) and
in Drools (gate 12).

## The differential square

`ref` ≡ `clingo` over both expectations, anchored to the hand-written
absolutes above. NOOP-REFUTING: J2's `heffing` is derived and absent
from the raw input, so a do-nothing run mismatches it (gate 7) — the
case does not ride on J1's leeg alone.
