# synth3-armwissel

The same nested `Let` as `synth3-armverdeling`, with **the branch condition
reading the nested binder**. Hand-written `ir.json`, no `bron.rgs` (no
source language emits an `IfThenElse`, IR-SPEC ch. 4.10).

One character of difference in the condition, and the two documents need
different reasoning to reach the same law. Where `armverdeling`'s nested
binding may simply be handed into both arms, this one may not: a condition
reading `w` stays outside both arms and would lose its binding. The
`RUNTIME-SEMANTICS.md` ch. 10.6 clause-3 claim is identical, the route to
it is not — which is why the corpus carries both.

## The shape

```
heffing := let k = zijn inkomen in
           let z = k / zijn restpost in
           let w = k in
             if w > drempel then 0,00 else z
```

`w` is a second name for `k`, so the condition is the same test
`synth3-armverdeling` makes; what moved is that the test now runs through
a binder bound *between* the need and the branch node. WF-077 is satisfied
either way — `w` is a binder.

## Why this discriminates

`z` is read in the `else` arm only, so under call-by-need J1 and J2 never
evaluate the division: 0,00, no channel reason, no fault. An executor that
gives up on the read analysis when a nested binding carries the condition
falls back to eager placement, and then J1 deposits a reason the case
forbids while J2 breaks the run off.

J4 is the mirror: the arm is entered, `z` is read, its value is leeg, and
the barrier empties the whole `Let` — `heffing` is leeg and the channel
reason is present. Suppressing the places instead of placing them
correctly fails here.

## Hand-computed absolutes

`drempel = 10,00`, `basisheffing = 5,00` (the parameter is declared for
symmetry with the sibling documents; this rule does not read it).

| case | Jutter | inkomen | restpost | w     | arm  | heffing                        |
|------|--------|---------|----------|-------|------|--------------------------------|
| 1    | J1     | 40,00   | *absent* | 40,00 | then | **0,00**, no channel reason    |
| 1    | J2     | 40,00   | 0,00     | 40,00 | then | **0,00**, no fault             |
| 1    | J3     | 5,00    | 2,00     | 5,00  | else | 5,00 / 2,00 = **2,50**         |
| 2    | J4     | 5,00    | *absent* | 5,00  | else | **leeg**, *with* the reason    |

## The differential square

`ref` ≡ `clingo` over all six expectations (gate 5 ≡ gate 6); the Drools
exec-model branch PLAYS it (gate 12), so ref ≡ clingo ≡ Drools.
NOOP-REFUTING: J1, J2 and J3 are computed and absent from the raw input
(gate 7).
