# wf053-geleste-vouwoperand-onverbonden-hop

Violates WF-053 (`spec/IR-SPEC.md` ch. 4.1a) in a **listed value fold's
operand**: a `MAXIMUM` over two listed operands whose second operand reads
`bakenloon` on `Baken` from a `Vondst` subject, while **no fact type
relates the two types at all**. The hop names no route and none is
derivable.

    WF-053 FAIL /rules/resolvedRegels/0/waarde/argumenten/1: bindingless
    navigation: no fact type relates 'Vondst' to 'Baken'

The document. Three object types — `Jutter`, `Vondst`, `Baken` — and one
fact type, `bewaring`, relating Jutter to Vondst. `Baken` is declared, and
so is `bakenloon` on it: every declaration cross in the document is
satisfied. What is missing is the relation the read walks over.

## Why the position is the point

The operand is not a fold SOURCE. With two or more arguments the node is
the listed value fold (§5.8.3) and its operands are ordinary reads, which
is why WF-083's anchor and navigation clauses are scoped to the
single-argument shape. But the fold branch of the walk REPLACES the
generic expression walk for these arguments, so until the WF-053
resolution was repeated there, a nested operand was the one read position
in the whole IR whose hop resolved against nothing.

The symmetry is exact — the same read, moved out of the fold, has always
been rejected:

    inside the fold  : WF-053 FAIL /rules/resolvedRegels/0/waarde/argumenten/1
    as the rule value: WF-053 FAIL /rules/resolvedRegels/0/waarde

same rule, same message, different path. A well-formedness rule that holds
for a read and stops holding because the read is listed beside another one
is not a rule about reads.

## What the executors do with it

    ref         : EVALUATION FAULT: no fact type relates "Vondst" to "Baken"
    ASP backend : TRANSLATION FAULT: expression kind not translated
    Drools      : EXCLUDE transpiler SKIP (waarde buiten scope)

`ref` faults with the verifier's own sentence, which is the strongest form
this kind of witness takes: the rule does not invent a restriction, it
lifts a runtime fault to well-formedness where the fault is unavoidable.

Hand-authored, from the temporal-total goldens' declarations plus one
unrelated object type.

Falsifiable, arm-exact: in `spec/verifier/lib/checks.ml`, drop the WF-053
repetition from the fold branch of `walk_expr` (the block guarded by
`List.length f.argumenten <> 1`, beside the WF-089/WF-092 repetitions that
are there for the same reason). Measured over golden + rood + negatief +
parity — 510 documents — exactly one report changes and this case, alone,
turns clean.
