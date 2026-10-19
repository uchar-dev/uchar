# synth3-armverdeling

A **nested `Let` between the need and the branch node**, in the shape where
the nested binder's value is independent of the outer need. Hand-written
`ir.json`, no `source.rgs` (no source language emits an `IfThenElse`,
IR-SPEC ch. 4.10).

Two rules over one object type, deliberately: the bare form and the same
form under an operator. The second is the **scope pin** — the one row that
separates a barrier belonging to the `Let` (`RUNTIME-SEMANTICS.md`
ch. 10.6 clause 2) from a barrier shrunk to the arm.

## The shape

```
heffing       := let k = zijn inkomen in
                 let z = k / zijn restpost in
                 let w = 1,00 in
                   if k > drempel then w else z

wikkelheffing := let k = zijn inkomen in
                 let z = k / zijn restpost in
                 (let w = 1,00 in
                    if k > drempel then w else z) + basisheffing
```

`z`'s only read sits in the `else` arm, and `w` stands between it and the
branch node. The condition reads `k` and a parameter, so it forces neither
`z` nor `w` (WF-077 is satisfied without touching either).

## Why this discriminates

Two independent claims, one per row of case 1 and one per row of case 2.

**Clause 3, through an intermediate binder.** `z` is unread when the
`then` arm runs, so its value — a division by an absent (J1) or zero (J2)
divisor — is never evaluated: no channel reason, no fault, and the answer
is `w`. An executor whose read analysis stops at the nested `Let` places
`z` eagerly and answers leeg on J1, or breaks off on J2.

**Clause 2, the extent.** On J4 the `else` arm *is* entered, `z` *is*
read, its value is leeg, and the barrier belongs to the `Let` that bound
`z` — which is the **whole** expression, the `+ basisheffing` included. So
`wikkelheffing` is leeg, not 5,00. An executor that empties only the arm
lets the coalescing `plus` read it as 0 and answers **5,00** here; that is
the single most informative cell in this document, because it fails
exactly the "obvious" rewrite that moves the binding inward instead of
lifting the branch node outward.

## Hand-computed absolutes

`drempel = 10,00`, `basisheffing = 5,00`.

| case | Jutter | inkomen | restpost | arm  | heffing   | wikkelheffing        |
|------|--------|---------|----------|------|-----------|----------------------|
| 1    | J1     | 40,00   | *absent* | then | **1,00**  | 1,00 + 5,00 = **6,00** |
| 1    | J2     | 40,00   | 0,00     | then | **1,00**  | **6,00**             |
| 1    | J3     | 5,00    | 2,00     | else | **2,50**  | 2,50 + 5,00 = **7,50** |
| 2    | J4     | 5,00    | *absent* | else | **leeg**  | **leeg** (not 5,00)  |

Case 1 asserts `consistent` over all three; case 2 asserts one channel
reason per rule on J4, so both rules are pinned to speak.

## The differential square

`ref` ≡ `clingo` over all eleven expectations (gate 5 ≡ gate 6); the Drools
exec-model branch PLAYS it (gate 12), so ref ≡ clingo ≡ Drools.
NOOP-REFUTING: every case-1 cell is computed and absent from the raw input
(gate 7).
