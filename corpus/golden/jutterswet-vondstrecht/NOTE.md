# jutterswet-vondstrecht

The **tracked-delta witness** of the let-lowering card (2026-09-01): the
golden that pins the *behavioural* difference between the v0 inlining of
a §13.4.2.10 variabelendeel and its v1 lowering to a `Let`
(`RUNTIME-SEMANTICS.md` ch. 10.6, tracked delta 1).

## What it pins

```
Het vondstrecht van een Vondst moet berekend worden als
    het grondrecht van de Vondst plus de toeslag.
Daarbij geldt:
    de toeslag is de meerprijs van de Vondst.
```

`meerprijs` is a perfectly ordinary attribute that a case may leave
**absent**. The two routes then part company:

* **v0, inlined.** The variable's subtree is substituted at the use
  site, so the value is the bare `grondrecht + meerprijs`. A leeg
  operand of `plus` **coalesces to 0** (the ch. 1.2 coalesce table), so
  the target is written with `grondrecht`.
* **v1, lowered.** The variable is a `Let` and the use site is a
  `BinderRef`. Reading a binder whose value evaluates leeg raises the
  **leeg-barrier at the read** and makes the whole `Let` leeg, so the
  target is **not written**.

Measured on this very IR: replacing the `Let`/`BinderRef` pair with the
inlined subtree gives V2 `vondstrecht = 30`; the committed form gives
V2 leeg. That is the deliberate v1 divergence, and this golden is its
corpus witness.

## Hand-computed absolutes

| Vondst | grondrecht | meerprijs | vondstrecht                          |
|--------|------------|-----------|--------------------------------------|
| V1     | 30 €       | 12 €      | 30 + 12 = **42 €**                   |
| V2     | 30 €       | *absent*  | **leeg** (the barrier at the read)   |

V1 is the control: with the variable filled, the lowered form computes
exactly what the inlined form does. Only V2 separates them.

## The differential square

`ref` ≡ `clingo` over both expectations (gate 5 ≡ gate 6), anchored to
these hand-written absolutes; the Drools backend PLAYS it (gate 12), so
ref ≡ clingo ≡ Drools — all three agree that V2 stays leeg. Gate 10
re-emits `bron.rgs` to a byte-identical `ir.json`. NOOP-REFUTING: V1's
`vondstrecht` is computed and absent from the raw input, so a do-nothing
run mismatches it (gate 7) — the case does not ride on V2's leeg alone.
