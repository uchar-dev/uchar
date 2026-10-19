# clamp-reads-bounds-in-order

The **`Clamp` operand-order** witness of `RUNTIME-SEMANTICS.md`
ch. 5.11: `base`, then `minimum`, then `maximum`.

## Why a RED case, not a golden

The order is observable only through what an earlier operand prevents.
For `Clamp` the natural green form — a barrier in the earlier
position suppressing a §9.5 deposit in the later one — cannot be a
golden, because the ASP backend **refuses a `Clamp` bound that is not
a ground literal** (a non-ground bound could be empty, and ref reads an
empty bound as *no* bound, which the bare join cannot express). Both a
division and a binder read carry atoms, so no green case with structured
bounds translates. What both executors *can* do on such a document is
fail observably — and which fault they report is decided by the order.

## The shape

```
begrenswaarde := Let grenswaarde = zijn grens
                 in (100 met een minimum van zijn teller gedeeld door zijn noemer
                         en een maximum van de grenswaarde)
```

with `teller` = 1, `noemer` = 0 and `grens` **absent**.

* `base` = 100 — evaluates first, uneventfully.
* `minimum` = `1 ÷ 0` — division by a **numeric zero**, which faults
  (ch. 1.2; the divisor is filled, so this is not the empty-divisor
  channel case).
* `maximum` = a read of `grenswaarde`, whose need is leeg — the ch. 10.6
  barrier, which would end the whole `Let` with **no** fault.

The two bounds therefore disagree about whether this document has an
answer, and the order decides.

## What each executor pins

| executor | pinned fragment | what decides it |
|----------|-----------------|-----------------|
| ref | `division by numeric zero` | the `minimum` is **evaluated** before the `maximum`, so the division is reached before the barrier |
| clingo | `a Clamp minimum bound that is not a ground literal` | the translator **visits** the `minimum` before the `maximum`, so it is the minimum's refusal that is reported |

Both flip under the reverse order, and both were measured that way:

* ref with the bounds swapped: `EXPECTED FAULT DID NOT OCCUR:
  evaluation succeeded` — the barrier fires first and the document
  quietly writes nothing;
* clingo with the bounds swapped: the report reads
  `a Clamp maximum bound …`, and the pinned fragment is absent.

So this one case pins the order in **both** executors — in ref by which
fault happens, in clingo by which bound is refused first.

## The Drools divergence this case exposes

Since the card `tms-omschakeling` (2026-10-10) the red run drives the
exec-model branch, which derives with truth maintenance. It faults on the
minimum's division as ref does: `deling door nul in de regel
"begrenswaarde van een jutter": 1 gedeeld door 0 — …`, pinned up to
`1 gedeeld door 0`, the operands of the minimum. The divergence below is
that of the destructive lowering, which the red run drove until then.

The Drools backend evaluates a `Clamp`'s **maximum before its
minimum** — the reverse of ch. 5.11 — and therefore runs this document
to a model without faulting: the barrier in the maximum fires before the
minimum's division is reached. Measured both ways on this exact shape:

| division in | ref | Drools |
|-------------|-----|--------|
| the `minimum` (this case) | **faults** | runs to a model |
| the `maximum` | writes 100, no fault | **faults**, `Breuk deling door 0` |

The two tables are mirror images, which is what makes this an *order*
divergence rather than a missing check — Drools does evaluate the
division, just from the other end. It was listed in
`scripts/check-red.sh`'s Drools exclusion list with that reason until
the card `tms-omschakeling`.
