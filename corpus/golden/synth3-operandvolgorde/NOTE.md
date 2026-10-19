# synth3-operandvolgorde

The **operand-order** witness of `RUNTIME-SEMANTICS.md` ch. 5.11: a
node's operands are evaluated **left to right in wire order**, and that
order is observable because it decides which §9.5 channel facts come
into being. Hand-written (synth3 family, no `source.rgs`): the shape is a
kernel-level question, not a source-language one.

## The shape

```
vangstsom := Let deel = zijn deelvangst
             in (het deel  plus  zijn teller gedeeld door zijn noemer)
```

Two operand positions, each with an observable effect of its own:

* `left` — a read of `deel`. When `deelvangst` is absent the need is
  leeg, so the read raises the ch. 10.6 **barrier** and the whole `Let`
  is leeg. A barrier ends the evaluation of everything after it.
* `right` — a division whose dividend is filled and whose divisor is
  absent. That is a §9.5 **producer** (ch. 5.6, ch. 1.2's `gedeeld door`
  row): it yields leeg *and* deposits a channel fact.

So the order decides whether the deposit happens at all — and the value
cannot tell you, because `leeg plus …` coalesces either way.

## Hand-computed absolutes

`case1-barriere.json`

| Vangst | `deelvangst` | `teller` | `noemer` | `vangstsom` | channel |
|--------|--------------|----------|----------|-------------|---------|
| V1 | *absent* | 4 | *absent* | **leeg** | **none** — `left` barriers, `right` is never reached |
| V3 | 5 | 4 | 2 | 5 + (4 ÷ 2) = **7** | none |

Pinned: `consistent` — the model carries **no** channel fact at all.
That absence is the order pin. Evaluate `right` first and V1's
division is reached before the barrier: it deposits, the model is
inconsistent, and the pin breaks.

`case2-companion.json` — the control that proves the deposit is real

| Vangst | `deelvangst` | `teller` | `noemer` | `vangstsom` | channel |
|--------|--------------|----------|----------|-------------|---------|
| V2 | 5 | 4 | *absent* | 5 + leeg = **5** (the ch. 1.2 `plus` coalesce) | **one** fact (`vangstsom van een vangst`, V2) |
| V3 | 5 | 4 | 2 | **7** | none |

Without this case, `case1`'s `consistent` would also be satisfied by a
division that never deposits under any circumstances. With it, the two
cases differ in exactly one thing — whether the barrier stands between
the evaluation and the division — and the channel follows.

V3 rides in both cases so that each case pins a **positive** value: a
no-op transpiler cannot produce 7, which is what makes both cases fail
the ch.-noop guard (gate 7) rather than surviving it on the empty
expectation alone.

## The mutant this golden catches

Swapping the two `let`s in the reference interpreter's `Binary` arm —
`rechts` before `links` — leaves every value in the corpus untouched and
breaks exactly this case:

```
MISMATCH: expected consistent, but the channel carries:
          vangstsom van een vangst × V1
```

## The differential square

ref (gate 5) and clingo (gate 6) agree on both cases. The agreement is
not coincidence but it is not the same mechanism either, and that is
worth recording: ref stops at the barrier **positionally** (a local
exception raised at the read), while the ASP encoding guards the whole
`Let` body — including the division's `inconsistent` edge — with the
`Let` value's atoms, which are underivable when the need is leeg
(`ASP-TRANSLATION.md` S18). On this shape the two coincide. They part
company on the mirrored shape (deposit **before** barrier in wire
order), where ref's already-made deposit stands and clingo's never
arises; that shape is deliberately **not** in the corpus, and ch. 5.11
records it as a boundary instead.
