# wf045-erfattribuut-spelling-cyclus

Violates **WF-045** (`spec/IR-SPEC.md` ch. 4.6, `RUNTIME-SEMANTICS.md`
ch. 6.2): `uitkering` is computed from `grondslag` and `grondslag` from
`uitkering`. Both attributes are declared once, on `Strandrecht`; the cycle
is invisible only because the two rules SPELL their owner differently — one
anchors at `Wrakrecht`, the other at `Strandrecht`.

    WF-045 FAIL /rules/resolvedRegels/0/versions/0: dependency cycle through no
    recursive creation: bereken de uitkering van een wrakrecht -> bereken de
    grondslag van een strandrecht -> bereken de uitkering van een wrakrecht

The spelling arm, beside `wf045-cyclic-attribute-dependency` (one type, one
spelling, caught since the graph existed) and `wf045-subtypecreatie-cyclus`
(the population arm). The three fall independently: a graph keyed on the
written owner name catches the first and neither of the other two.

Measured before the repair (2026-09-12) on this document: verifier PASS, and
`ref` on a single `Wrakrecht` `EVALUATION FAULT: no fixpoint within 100
passes — divergence is a fault (ch. 5.7)`. Same shape as the population arm:
loud at the last line of defence, under a report that names neither rule.

**What is deliberately still clean** is the sibling shape, and it is why the
repair walks ⊑-COMPARABILITY rather than keying both sides on the declaring
type. Give `Strandrecht` a second, incomparable subtype and let each subtype
compute one of the two attributes from the other: no instance is of both
types, so nothing depends on anything, and `ref` replays it to a clean PASS
(measured the same day). The declaring-type key would have merged those two
into one node and refused a document that runs.

Falsifiable in one edit: spell the first rule's owner `Strandrecht` too, and
it becomes the plain one-type cycle the older witness already carries — still
refused, but by the arm that never needed the chain. Drop `Wrakrecht`'s
`supertype` instead and the document is clean, because then the two rules
really are about two unrelated populations.

Minimal own-authored document (Jutterswet vocabulary), on the type pair of
`corpus/golden/anchor/jutterswet-strandrechtketen`.
