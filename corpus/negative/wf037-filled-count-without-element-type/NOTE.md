# wf037-filled-count-without-element-type

Violates WF-037 (`spec/IR-SPEC.md` ch. 4.3, ch. 4.6) in the filled count's
INSTANCE-FOLD arm: `filledCount` is an instance fold and must carry an
element type, and this node carries none.

    WF-037 FAIL /rules/rules/0/versions/0/value: the filled count is an
    INSTANCE fold and MUST carry an element type: without one its argument is no
    fold source at all, and the count has no contributors whose values it could
    test for presence (ch. 4.3)

**Why the generic clause could not carry this.** WF-037's existing
element-type fence is spelled as "a value fold MUST carry ≥ 2 arguments" and
fires on an aggregation with no element type and fewer than two operands —
a sentence that is exactly wrong for this kind, whose law is exactly ONE
argument. So the kind is excluded from that clause and answers to its own,
and the exclusion is itself measured (below).

The document. The count keeps its single argument but drops
`elementType` and, with it, the `sourceRole` — a binding without an
instance fold is WF-047's defect and would have masked this one. The
argument is an ordinary read of the subject's own kg attribute, which
resolves against its declaration and is clean on its own account. The
missing element type is the only defect and the verifier reports one fault.

Rood-eerst, measured on this document. With the wire enum landed and the
arms absent (verifier md5 `220741bf2cee253d71ca6b13b8ad1402`):
`verifier: PASS`. With the arms (md5 `d970a571d0ecbc88de4bea5b14da9fc7`):
the single rejection above.

Falsifiable, arm-exact, twice. (1) Return `[]` from the arm's `None, _` case
in `spec/verifier/lib/checks.ml`: swept over golden + red + negative +
parity (695 documents) exactly this document moves — it turns clean again.
(2) Drop the kind's exclusion from the generic "fewer than two operands"
clause (`&& f.kind <> Aantal_gevulde`): exactly this document moves again,
and it moves by GAINING a second citation — `value fold with fewer than two
operands`, the sentence that reads the kind's arity law backwards. One
malformation, one rule: that is what the exclusion buys and what the mutant
measures.

Since L4 the node is an `Aggregation` or a `BuiltinCall` (delta B21), each
with only its own fields — `arguments`, and on an aggregation `elementType`,
`emptyFallback`, `dimensionSelection`, `sourceRole` and `sourcePath`, on a
builtin `granularity`; the report cites and names them. `expect.txt` pins the
arm.
