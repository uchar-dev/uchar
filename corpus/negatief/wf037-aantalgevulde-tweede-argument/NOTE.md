# wf037-aantalgevulde-tweede-argument

Violates WF-037 (`spec/IR-SPEC.md` ch. 4.3, ch. 4.6) in the filled count's
ARITY arm: `AANTAL_GEVULDE` takes exactly one argument, its set source, and
this node carries two.

    WF-037 FAIL /rules/rules/0/versions/0/value: the filled count takes
    exactly ONE argument, its set source, and this node carries 2: there is no
    listed form of this kind — the source language spells it nowhere, so there
    is no articulated operand list to admit (ch. 4.3)

**The arm is new of DIRECTION and that is why it needs a witness.** WF-037's
other arity clause says a value fold MUST carry two or more operands — the
listed form of §5.8.3, "de eerste van A en B". Here the law is the opposite.
The listed form exists because the source language spells those folds over
listed operands; it spells the filled count nowhere at all
(`RUNTIME-SEMANTICS.md` ch. 3.4c), so there is no written form for a listed
reading to mirror and the shape would have to be invented rather than
admitted.

The document. The same Jutterswet model as its siblings, written in the
listed shape: two ordinary reads of the subject's own kg attributes
(`vondstgewicht`, `wrakgewicht`), no `elementType`, no `sourceRole` —
exactly how a `MINIMUM` over two operands is spelled. Every other clause
holds; both operands resolve against their declarations, and the ordering
family clause of WF-038 says nothing here because this kind hands no element
through (it counts). The arity is the only defect and the verifier reports
one fault.

Rood-eerst, measured on this document. With the wire enum landed and the
arms absent (verifier md5 `220741bf2cee253d71ca6b13b8ad1402`):
`verifier: PASS`. With the arms (md5 `d970a571d0ecbc88de4bea5b14da9fc7`):
the single rejection above.

Falsifiable, arm-exact: turn the arity guard off (`else if arity <> 1 then`
→ `else if false then` in `spec/verifier/lib/checks.ml`) and leave
everything else untouched. Swept over golden + rood + negatief + parity
(695 documents) this document is the ONLY one that moves, and — recorded
rather than smoothed — it does not turn clean: with the first arm of the
three-arm chain gone it falls to the element-type arm, which is also true of
it, so the mutant moves its SENTENCE rather than its verdict. The arm is
measured by the sentence it owns; no other document moves a line.

Since L4 the node is an `Aggregation` or a `BuiltinCall` (delta B21), each
with only its own fields — `arguments`, and on an aggregation `elementType`,
`emptyFallback`, `dimensionSelection`, `sourceRole` and `sourcePath`, on a
builtin `granularity`; the report cites and names them. `expect.txt` pins the
arm.
