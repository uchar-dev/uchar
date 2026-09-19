# synth3-armvangst

The nested `Let` whose **own value reads the outer need**, with the body
reading that need through nothing else. Hand-written `ir.json`, no
`bron.rgs` (no source language emits an `IfThenElse`, IR-SPEC ch. 4.10).

This is the sharpest of the four branch documents, because the outer
need's read place is not visible in either arm: syntactically the arms
mention only `w`. Whether `z` is needed is decided one binder along, by
whether `w` is read — which is `RUNTIME-SEMANTICS.md` ch. 10.6 clause 3
applied twice over. Two rules again, the bare form and the same form under
an operator; the second is the scope pin (clause 2).

## The shape

```
heffing       := let k = zijn inkomen in
                 let z = k / zijn restpost in
                 let w = z + 1,00 in
                   if k > drempel then 0,00 else w

wikkelheffing := let k = zijn inkomen in
                 let z = k / zijn restpost in
                 (let w = z + 1,00 in
                    if k > drempel then 0,00 else w) + basisheffing
```

Compare the sibling shape in which the body *also* reads `z` directly: there
the need is wanted on every branch and eager placement is exact. Here it is
not, and the difference is not syntactic — both documents mention `z` in
exactly one place, the nested value.

## Why this discriminates

**Clause 3, twice.** When the `then` arm runs, `w` is unread, so `w`'s
value is never evaluated, so `z` is never read, so the division is never
evaluated. J1 (absent divisor) must therefore deposit no channel reason
and J2 (zero divisor) must raise no fault — and both must still answer
0,00. An executor that reads "the nested value mentions `z`" as "`z` is
needed either way" places `z` eagerly and fails both rows.

**Clause 2, the extent.** On J4 the `else` arm is entered, `w` is read,
`z` is read through it, its value is leeg, and the barrier belongs to the
`Let` that bound `z` — the whole expression. So `wikkelheffing` is leeg,
not 5,00. Note the `z + 1,00` inside `w`: the read of `z` **ends** the
`Let` rather than yielding leeg into the `plus`, so the 1,00 never
coalesces anything either.

## Hand-computed absolutes

`drempel = 10,00`, `basisheffing = 5,00`.

| case | Jutter | inkomen | restpost | z     | w     | arm  | heffing  | wikkelheffing        |
|------|--------|---------|----------|-------|-------|------|----------|----------------------|
| 1    | J1     | 40,00   | *absent* | —     | —     | then | **0,00** | 0,00 + 5,00 = **5,00** |
| 1    | J2     | 40,00   | 0,00     | —     | —     | then | **0,00** | **5,00**             |
| 1    | J3     | 5,00    | 2,00     | 2,50  | 3,50  | else | **3,50** | 3,50 + 5,00 = **8,50** |
| 2    | J4     | 5,00    | *absent* | leeg  | —     | else | **leeg** | **leeg** (not 5,00)  |

An em dash means the need was never evaluated at all — which is the claim,
not a gap in the table.

## The differential square

`ref` ≡ `clingo` over all eleven expectations (gate 5 ≡ gate 6); the Drools
exec-model branch PLAYS it (gate 12), so ref ≡ clingo ≡ Drools.
NOOP-REFUTING: every case-1 cell is computed and absent from the raw input
(gate 7).
