# wf037-aantalgevulde-leegwaarde

Violates WF-037 (`spec/IR-SPEC.md` ch. 4.3, ch. 4.6) in its `emptyFallback`
clause — "sum only". An `AANTAL_GEVULDE` carries an `emptyFallback`.

    WF-037 FAIL /rules/rules/0/versions/0/value: empty-value fallback
    on a non-sum aggregation

**Nothing was widened to get this, and that is the point.** The fallback
clause is written positively — it GRANTS the facet to a named kind — so a
new aggregation kind is outside it by construction and no letter of WF-037
had to move. What a positively written clause does not give is a failing
witness, and a MUST that outruns its witness is the spec-level "a green
subset is not green". This document is that witness for the new kind.

The semantics behind the refusal: a filled count over a source with no
filled values is 0, never leeg (`RUNTIME-SEMANTICS.md` ch. 3.4c and the
count row of ch. 1.2e). There is no empty to fall back FROM, so a stand-in
value would be an operand the node evaluates and then has no use for — and
the sum's fallback is evaluated unconditionally, so it would not even be
free.

The document. The single set source, the binding and the node type are the
well-formed shape; the only addition is an `emptyFallback` holding the literal
0 — the one fallback value the executors carry at all. The verifier reports
one fault.

Rood-eerst, measured on this document. With the wire enum landed and the
arms absent (verifier md5 `220741bf2cee253d71ca6b13b8ad1402`): the WF-037
line above PLUS a co-firing WF-096 on the source, because the set fence's
exemption still stopped at six value folds and this kind was not yet one of
them. With the arms (md5 `d970a571d0ecbc88de4bea5b14da9fc7`): the WF-037
line alone.

Falsifiable, arm-exact, until L2: take the `Aantal_gevulde` row back out of
`value_fold_name` in `spec/verifier/lib/checks.ml` — the six-to-seven
widening of WF-096's exemption, one line. Swept over golden + rood +
negatief + parity (695 documents) exactly four documents moved: this one and
its two facet siblings regained the co-firing WF-096, and
`wf093-aantalgevulde-wrat` fell back from WF-093 to WF-034. No golden, no
red case and no other negatief changed a verdict.

Since L2 WF-096 is retired (its count half went into WF-093), so this
mutant no longer reaches the witness. Measured in the L2 fix round
(2026-09-24) over golden, rood, negatief and the parity zone (718
documents): exactly one document moved, `wf093-aantalgevulde-wrat`, whose
WF-093 message then named the kind `AANTAL` instead of `AANTAL_GEVULDE`; no
document changed its verdict, and this one kept its WF-037 line, which the
kind-general facet arm of WF-037 carries. Later in the same round WF-093's
message became the sentence the executors share, which names no kind, so
no check reads the list any more: measured again, the mutant moves no
report of the 718.

Since L4 the node is an `Aggregation` or a `BuiltinCall` (delta B21), each
with only its own fields — `arguments`, and on an aggregation `elementType`,
`emptyFallback`, `dimensionSelection`, `sourceRole` and `sourcePath`, on a
builtin `granularity`; the report cites and names them. `expect.txt` pins the
arm.
