Violates WF-007 (`spec/IR-SPEC.md` ch. 2.4): a whole number carrying 2
decimals — no legal precision form — on a rule TARGET's `type`.

    WF-007 FAIL /resolvedRegels/0/doel/type: whole number with 2 decimals:
    not one of the three precision forms

Why it has to exist: WF-007 is a SHAPE law on a datatype — every `Number`
in the document, wherever it sits — but its two halves reached only the
declarations (`wf007_model`: attribute, domain and parameter datatypes)
and the types the expression walk enters (`walk_expr`'s type prologue).
The target lvalue is WF-022's and the walk deliberately never enters it,
so the form check never ran on `/resolvedRegels/*/doel/type`: measured on
2026-09-03, the shape below is verifier-`PASS` while the identical form on
any walked expression type is refused (`.../voorwaarde/rechts/type`,
`WF-007 FAIL`). The clause added by card `20260903-wf007-schrijfdoel`
closes it.

The window the gap is OBSERVABLE in is narrower than the gap itself, and
that is measured, not assumed. Where the target's attribute declaration
resolves, the identity family already crosses the annotation facet by
facet (WF-023 position 0 → WF-091), and a WF-007-clean declaration
necessarily differs from a WF-007-dirty annotation in a numeric facet:
put the same lie on the attribute target of `jutterswet-vondsttarief` and
the document is refused — but as `WF-091 … decimals mismatch: rule target
'bergingstarief' is typed 2 decimals, but the attribute declaration it
writes is 0 decimals`, a COMPARISON message with the wrong rule number,
which says nothing about the precision form and vanishes the moment the
declaration is dropped or the comparison defers.

Hence a CHARACTERISTIC target here: `resolved_attr_datatype` yields None
for a characteristic (it is not in `attributen`), so the identity family
defers by construction and this clause is the only thing standing between
the wire and a nonsense datatype. That is not a corner: golden + rood carry
54 characteristic targets over 34 documents, each of which the fix now
covers and nothing covered before.

A hand-broken variant of `corpus/golden/jutterswet-penningdrempel/ir.json`,
one field deep: `/resolvedRegels/0/doel/type` — `{"node": "Booleaan"}` —
replaced by a `Numeriek` with `geheel: true` and `decimalen: 2`, which is
schema-valid on the v0 wire and round-trips byte-identically.

Falsifiable, clause-exact: drop the rule-target form clause from
`expression_layer` and this case alone turns clean, every other `wf007-*`
case staying red.
