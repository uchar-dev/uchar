# wf037-aantalgevulde-granulariteit

Violates WF-037 (`spec/IR-SPEC.md` ch. 4.3, ch. 4.6) in its `granularity`
clause — the facet belongs to the two bucketing builtins
(tijdsevenredig-deel, aantal-dagen-in) and to nothing else. An
`AANTAL_GEVULDE` carries one.

    WF-037 FAIL /rules/resolvedRegels/0/waarde: granularity facet on a
    non-bucketing kind

The granularity says which period grid a builtin buckets over. A filled
count buckets nothing: it tests each contributor's value for presence and
adds up the answers, which is a question about a SET and not about time.
(A filled count over a temporal attribute has no defined moment to read
either, and `ref` refuses it by name at evaluation for that reason — ch. 3.6
gives the per-element timeline fold to som and totaal alone.)

The document. The well-formed shape with `granulariteit: "JAAR"` added and
nothing else touched. The verifier reports one fault.

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
documents): exactly one document moves, `wf093-aantalgevulde-wrat`, whose
WF-093 message then names the kind `AANTAL` instead of `AANTAL_GEVULDE`; no
document changes its verdict, and this one keeps its WF-037 line, which the
kind-general facet arm of WF-037 carries.
