# jutterswet-kaletabel

The first golden whose `diagnostics` list is not empty (WF-113, `spec/IR-SPEC.md`
ch. 2): a document the producer did write, together with its own account of what
stayed out of it. The account comes from a real producer route, not a hand-made
list — `source.rgs` through the RegelSpraak frontend, `ir.json` its byte-identical
emission (gate 10).

## What the source does

`source.rgs` declares a Jutter with a `wrakkental`, one rule that computes the
`wrakkenloon` from it, and a decision table `Wrakkenbonus` that has a header row
and no data rows. A table without rows has nothing to lower, so the frontend
writes no rule for it and says so in a `warning` on line 10, the line where the
table starts:

> beslistabel 'Wrakkenbonus' heeft geen tabelinhoud (alleen een kop) — wordt
> niet naar IR/DRL omgezet

That `warning` is the whole of `diagnostics`. WF-113 (a) holds: severity
`warning`, a non-empty message, an integer line. The document carries no
`Unresolved` node, so (b) has nothing to account for here; the two red documents
under `corpus/red/wf044-unresolved-*` are its witnesses.

## Hand computation

The rule is `wrakkenloon = wrakkental × 15`; nothing writes `wrakkenbonus`,
because the only sentence that would have written it is the table the frontend
dropped. WF-113 (c): no executor reads `diagnostics`, so the list changes no
outcome.

| case | instance | `wrakkental` | `wrakkenloon` | `wrakkenbonus` |
|---|---|---|---|---|
| `case1.json` | J1 | 3 | 3 × 15 = 45 | empty |
| `case2.json` | J1 | 0 | 0 × 15 = 0 | empty |
| `case2.json` | J2 | 12 | 12 × 15 = 180 | empty |

Both cases also expect `consistent`: the document has no consistency rule.

The `wrakkenbonus` expectation is what ties the `warning` to an observable fact:
the sentence the frontend reports as not converted leaves its attribute empty
in every executor. The no-op guard (gate 7) fails on both cases, because the
input carries no `wrakkenloon`.
