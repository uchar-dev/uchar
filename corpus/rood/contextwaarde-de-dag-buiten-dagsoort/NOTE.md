# contextwaarde-de-dag-buiten-dagsoort

`de dag` **outside a day-kind definition**. Since the L4 landing it is its
own node, `BoundDay` (`IR-SPEC.md` ch. 4.1; until then the third context
value under WF-098), and it denotes the BOUND DAY of a day-kind voorwaarde
— the day the defining predicate is being applied to. Anywhere else it
denotes nothing, and this case is what makes that observable.

## The shape

No day kind at all. One rule reads the bound day (`BoundDay`) in an ordinary
calculation:

```
Het jaartal van een Baken moet berekend worden als het jaar uit de dag.
```

The document is **verifier-clean**: a `BoundDay` node is well-formed wherever
it stands (WF-098 governs the two session-value names, not the bound day),
and the clause is deliberately FLAT — it carries no position rule.
The position half that DID land with the day-kind semantics card is its
mirror: WF-031 refuses a SESSION value *inside* a day-kind voorwaarde
(negatief `wf031-sessiewaarde-in-dagsoort`). What `de dag` means outside one
is left to the executors, and they agree.

## Why a RED case, not a negatief

Because the question is what the node MEANS, not whether the wire is
well-formed. Both readings that would let it pass quietly are wrong: leeg
(which would make an unbound day look like a missing input) and the
evaluation date (which is a different context value with a different
meaning — a day-kind definition is about an arbitrary day, a session is
about one particular one). A static rule would be the stronger answer and is
recorded as open; the runtime refusal is what this card measured and pinned.

## What each executor pins

| executor | phase | pinned fragment |
|---|---|---|
| `ref` | evaluation | `the bound day of a day-kind definition` |
| clingo | translation | same, verbatim |

The two refuse in the SAME WORDS, which is the point: before this card
`de dag` reached each executor's generic fall-through (`expression kind not
implemented in this card` / `not translated`), a message that names no
defect — measured with WF-098 on 2026-09-10 and recorded there.

The Drools backend does not reach fire: its transpiler SKIPs the rule
("waarde buiten scope"), because the v0 constant reader carries no `de dag`
outside `kern.Dagsoorten`, its day-kind predicate compiler. That is listed
with its reason in gate 8's Drools exclusion list rather than pinned here.
