# synth3-conditielezing

The **condition-is-a-read** witness of ch. 10.6 clause 2 + WF-077: an
`IfThenElse` *condition* operand is a **read** of the binder like any
other, so a binder whose value is leeg fires the barrier **there** —
before either branch is considered. Hand-written (synth3 family, no
`source.rgs`): it pins where an executor counts the read, which is a
kernel question.

## The shape

```
heffing := Let grondslag = <zijn grondslag>
           in (als (grondslag maal 1,00) groter dan 0,00 dan 5,00 anders 9,00)
```

The binder is read **only** in the condition — neither branch mentions
it. Two things make that the interesting case:

* the condition operand is a **coalescing** combinator (`maal`), so an
  executor that lowered it without honouring the read would compute
  `leeg maal 1,00 = 0,00` (the ch. 1.2 maal row), find `0,00 > 0,00`
  false, and *derive the else-branch*: **9,00**;
* the branches themselves are bare literals, so nothing else in the
  body can raise the barrier. If the condition read does not raise it,
  nothing does.

This is exactly the ASP-side risk the S18 per-branch read analysis
exists for: a translation that attributes the condition's binder read to
the branch bodies (or to one of them) still emits a stable model — it
just emits the **wrong** one. `synth3-takbarriere` pins the *other*
direction (an unread binder in the branch not taken must **not** empty
the `Let`); this golden pins that a condition read is never mistaken for
a branch read.

## Hand-computed absolutes

| Jutter | `grondslag` | condition                        | `heffing`                        |
|--------|-------------|----------------------------------|----------------------------------|
| J1     | *absent*    | never decided — the read fires the barrier | **leeg**                |
| J2     | 3,00        | 3,00 × 1,00 = 3,0000 > 0,00 → waar | **5,00** (the then-branch)     |

J2 is the filled control: it pins that the condition really does decide
and pick the then-branch, so J1's leeg is the barrier at the condition
read, not a rule that never fires. The mutant — the condition read
attributed to a branch — writes **9,00** for J1.

## The differential square

`ref` ≡ `clingo` over both expectations, anchored to the hand-written
absolutes above. NOOP-REFUTING: J2's `heffing` is derived and absent
from the raw input, so a do-nothing run mismatches it (gate 7).
