# som-fallback-always-evaluated

The **unconditional stand-in** witness of `RUNTIME-SEMANTICS.md` ch. 5.11:
a som's `emptyFallback` is the node's LAST operand, and it is **evaluated**
whether or not its value is ever **read**.

## Why it needs saying

Ch. 5.11 orders the som as "the contributors, then the `emptyFallback`".
Read quickly, that suggests the stand-in only runs when the contributors
leave nothing — which is when its *value* is used. That is not this
witness's reading: the stand-in is an ordinary operand of the node and it
evaluates every time the node does. Its faults and its §9.5 deposits
therefore happen on documents where its value is discarded.

## The shape

```
vangstsom := de som van zijn deelvangst en zijn teller
             of zijn teller gedeeld door zijn noemer als die er niet zijn
```

with `deelvangst` = 5, `teller` = 1 and `noemer` = 0.

Both contributors are **filled**, so the som is 5 + 1 = 6 and the
stand-in's value is not needed. It is evaluated anyway, and `1 ÷ 0`
faults: the document has no answer.

| executor | pinned fragment | why |
|----------|-----------------|-----|
| ref | `division by numeric zero` | the stand-in evaluates after the contributors and faults |
| clingo | `a sum over listed operands is not translated` | the listed-operand som is not carried by the ASP backend at all — an honest refusal, not an order observation |

The clingo fragment moved on 2026-09-19, and the move is this case's own
doing. It used to read `expression kind not translated`, the translator's
generic fall-through, whose full sentence continues *"(no corpus case
exercises it)"* — a claim **this case** falsified the day it landed. The
form now has a named arm of its own, so the fragment names the form and
the fall-through's sentence is true again. Nothing about the document or
its outcome changed: the ASP backend still declines the form, in both the
old position and the new one, and the refusal is still the only thing
this executor contributes to the row. At L5 the fragment's wording moved
once more, with no change of form or outcome: the executors name the
aggregation kind as the wire spells it, `sum` where they wrote `som`.

## What it catches

Making the stand-in lazy — evaluating it only when no contributor is
filled — is the natural "optimisation", and it is a semantic change.
Measured against exactly that mutant:

```
EXPECTED FAULT DID NOT OCCUR: evaluation succeeded
```

The same mutant also kills `WV-5.11-som-leegwaarde-onvoorwaardelijk` in
`ref/test`. The order half of the same row — contributors *before*
stand-in — is pinned by the two `WV-5.11-som-…` example pairs there; it
cannot be pinned here, because a document whose contributors barrier and
whose stand-in would fault **answers**, and the ASP backend does not
carry the form, so it cannot be a golden either.

## Drools

Not carried: the reaching rule does not lower — a transpiler SKIP
(aggregation outside the backend's Phase-C scope) — so fire is never
reached. Listed with that reason in `scripts/check-rood.sh`.
