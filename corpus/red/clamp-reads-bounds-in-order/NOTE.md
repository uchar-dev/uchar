# clamp-reads-bounds-in-order

The **`Clamp` operand-order** witness of `RUNTIME-SEMANTICS.md`
ch. 5.11: `base`, then `minimum`, then `maximum`.

## Why a RED case, not a golden

The order is observable only through what an earlier operand prevents.
For `Clamp` the natural green form — a barrier in the earlier
position suppressing a §9.5 deposit in the later one — cannot be a
golden, because the ASP backend **refuses a `Clamp` bound that is not
a ground literal** (a non-ground bound could be empty, and ref reads an
empty bound as *no* bound, which the bare join cannot express). What the
executors *can* do on such a document is fail observably — and which
fault they report, or whether they fault at all, is decided by the order.

## The shape

```
begrenswaarde := Let grenswaarde = zijn grens
                 in (100 met een minimum van het Rekenjaar
                         en een maximum van de grenswaarde)
```

with `grens` **absent**, in a session with no evaluation date
(`asOfDate` null).

* `base` = 100 — evaluates first, uneventfully.
* `minimum` = the `evaluationYear` context value, which needs an
  evaluation date the case does not carry: a hard fault, kind EF-011
  (ch. 8.2).
* `maximum` = a read of `grenswaarde`, whose need is leeg — the ch. 10.6
  barrier, which would end the whole `Let` with **no** fault.

The two bounds therefore disagree about whether this document has an
answer, and the order decides.

## What each executor reports

| executor | code | what decides it |
|----------|------|-----------------|
| ref | EF-011 | the `minimum` is **evaluated** before the `maximum`, so the year is read before the barrier |
| clingo | EF-011 | the translator **visits** the `minimum` before the `maximum`, so the year's translation fault comes before the refusal of a `maximum` bound that is not a ground literal (CLINGO-005) |
| Drools (exec-model) | EF-011 | the `minimum` is evaluated before the `maximum`, as in ref |

Under the reverse order ref would read the barrier first and quietly write
nothing, and the clingo translator would refuse the non-ground bound
(CLINGO-005) before it reached the year.

## History

Until 2026-10-19 the `minimum` was `zijn teller gedeeld door zijn noemer`
with `teller` 1 and `noemer` 0: a division by a numeric zero, fault kind
EF-005. The card `zero-division-channel` routed that division into the
§9.5 channel (ch. 1.4 point 4), where it deposits and does not end the
evaluation, so it could no longer show the order; the evaluation year is
another hard fault of v1 at the same position, and `teller` and `noemer`
left the model. With the division, clingo refused the `minimum` bound
itself (CLINGO-005), which stood in gate 8's divergence list; with the
year its own fault is the case's code and the entry went. Measured with the
division: ref with the bounds swapped wrote nothing (`EXPECTED FAULT DID NOT
OCCUR`), and clingo then reported `a Clamp maximum bound …`. The Drools
backend's destructive lowering (until the card `tms-omschakeling`,
2026-10-10) evaluated the `maximum` first and ran this document to a model.
