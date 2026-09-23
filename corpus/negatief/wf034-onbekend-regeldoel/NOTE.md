Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6): the v0 `Onbekend` unknown-type
sentinel on a rule TARGET's `type`.

    WF-034 FAIL /rules/resolvedRegels/1/doel/type: unknown-type sentinel on a
    rule target

Why it has to exist: the target's `type` was the last place the sentinel
survived **outside the documented row-1 fold-source wart** — and, as the
fix round measured, outside the `SumType` constructor payload. The
expression walk refuses it on every value node it enters (`unknown-type
sentinel inside a structured node`) and WF-003 refuses it on a declaration
(`unresolved datatype (v0 'Onbekend' sentinel: …)`), but the walk
deliberately never enters the target — the lvalue is WF-022's — so the
sentinel there passed (measured, card `20260903-identiteitsarmen-getuigen`:
verifier `PASS`, and all three executors replay the document unchanged,
because each writes through the target's DECLARED datatype).

The qualification is measured, not hedged. Injecting the sentinel at every
type position of every golden — 2159 injections — leaves 21 verifier-clean:
9 on fold-/aggregation argument types (`…/argumenten/N/type`: 8
element-passing aggregation operands, 1 true instance-fold source — next
door to the v0 instance-fold wart the WF-034 index row names as an
exception, whose 18 by-construction sentinels are a separate set) and 12
on `SumType` constructor payloads (`…/type/constructors/N/payload`). None
anywhere else, and none on a rule target once this clause stands. The
payload position is open, unfenced and unclaimed here; closing it is a
decision of its own.

That no executor reads the field is exactly why it needed a fence rather
than a shrug: the target `type` is a use site the identity family crosses
(WF-091's rule-target arm, `wf091-schrijfdoel-eenheid`), and that family
defers on the sentinel. An emitter writing `Onbekend` there therefore
switched the unit, precision and domain checks off its own write targets
and stayed verifier-clean — a self-served exemption, not a design. The shape
is carried by zero documents outside this witness (152 golden + 22 rood +
30 parity documents, plus every negatief that predates this card,
measured), so the widening cannot over-reject.

A hand-broken variant of `corpus/golden/jutterswet-aanvoergeld/ir.json`:
`/rules/resolvedRegels/1/doel/type` — declared `Numeriek`, 2 decimals, `EUR` —
replaced by `{"node": "Onbekend", "reden": "niet opgelost"}`, which is
schema-valid on the v0 wire.

Falsifiable, clause-exact: drop the rule-target sentinel clause from
`expression_layer` and this case alone turns clean, every other `wf034-*`
case staying red.
