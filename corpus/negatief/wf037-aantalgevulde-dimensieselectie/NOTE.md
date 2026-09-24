# wf037-aantalgevulde-dimensieselectie

Violates WF-037 (`spec/IR-SPEC.md` ch. 4.3, ch. 4.6) in its
`dimensionSelection` clause — "sum/min/max only". An `AANTAL_GEVULDE`
carries one.

    WF-037 FAIL /rules/rules/0/versions/0/value: dimension selection on
    a kind other than sum/min/max

**Postponed, not rejected** — and the distinction is normative. The
dimension fold is itself a dialect form, and a combination of two dialect
forms without a casus is one nobody writes and nobody checks; that is the
ground for keeping it off the wire today, not a claim that a dimensioned
filled count is meaningless. A later card with a real casus finds the door
closed and not locked, which is why the clause says so in words rather than
leaving a reader to infer it from an absence.

As with the fallback sibling, the clause is written positively and so needed
no amendment: it grants the facet to an enumerated set. The witness is what
turns that silence into a checked MUST.

The document. The well-formed shape plus a declared dimension `kustvak`
(labels `noord`, `zuid`) and a `dimensionSelection` over both labels — the
labels resolve, so WF-040 has nothing to say and the facet's presence is the
only defect. The verifier reports one fault.

Rood-eerst, measured on this document. With the wire enum landed and the
arms absent (verifier md5 `220741bf2cee253d71ca6b13b8ad1402`): the WF-037
line above PLUS a co-firing WF-096 on the source, the set fence's exemption
still standing at six. With the arms (md5
`d970a571d0ecbc88de4bea5b14da9fc7`): the WF-037 line alone.

Falsifiable, arm-exact, until L2: the same one-line mutant as its facet siblings —
take the `Aantal_gevulde` row out of `value_fold_name` in
`spec/verifier/lib/checks.ml`. Swept over golden + rood + negatief + parity
(695 documents) exactly four documents moved, this one among them, and no
golden, red case or other negatief changed a verdict.

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
