# synth3-operatortak

`synth3-takhek`'s form with **an arithmetic operator above the branch
node**. Hand-written `ir.json`, no `source.rgs` (no source language emits an
`IfThenElse`, IR-SPEC ch. 4.10).

The two documents differ by one `plus`, and that one `plus` is the point:
a branch node that is a rule's whole value can be lowered by splitting the
rule head per arm, while a branch node **under an operator** cannot — it
needs a lowering of its own. Without this golden the corpus would pin the
ch. 10.2 arm discipline on one of those two routes and leave the other
resting on the backends' own suites.

## The shape

```
heffing := let k = zijn inkomen in
             (if k > drempel then basisheffing
                             else k / zijn restpost) + basisheffing
```

## Why this discriminates

Same §6.5 place and same numeric-zero place as `synth3-takhek`, in the
`else` arm only — so J1 and J2 again refute any executor that evaluates
both arms. What J4 adds here, and what makes this document more than a
copy, is the **coalesce**: the entered arm evaluates leeg, and the `plus`
above it reads that leeg as 0 (the ch. 1.2 coalesce table), so the answer
is a *filled* 5,00 **with** the channel reason beside it. An executor that
let the arm's emptiness escape upward past the operator answers leeg here
and fails; one that suppressed the reason fails too.

## Hand-computed absolutes

`drempel = 10,00`, `basisheffing = 5,00`.

| case | Jutter | inkomen | restpost | arm  | heffing                                  |
|------|--------|---------|----------|------|------------------------------------------|
| 1    | J1     | 40,00   | *absent* | then | 5,00 + 5,00 = **10,00**, no reason       |
| 1    | J2     | 40,00   | 0,00     | then | **10,00**, no fault                      |
| 1    | J3     | 5,00    | 2,00     | else | 2,50 + 5,00 = **7,50**                   |
| 2    | J4     | 5,00    | *absent* | else | leeg + 5,00 = **5,00**, *with* the reason |

## The differential square

`ref` ≡ `clingo` over all six expectations (gate 5 ≡ gate 6); the Drools
exec-model branch PLAYS it (gate 12), so ref ≡ clingo ≡ Drools.
NOOP-REFUTING: J1, J2, J3 and J4 all carry computed values absent from the
raw input, so a do-nothing run mismatches every row (gate 7).
