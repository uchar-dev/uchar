# wf110-unie-naast-vouwbron

Violates WF-110 (`spec/IR-SPEC.md` ch. 4.3), the instance-set union — the
§5.7 concatenation of subject expressions, landed 2026-09-20 as the source
of an instance count. Clause (c): the count over a union names no source of its own.

    WF-110 FAIL /resolvedRegels/0/waarde: a count over an instance-set
    union names no source of its own, and this node carries
    elementObjecttype: the union's selections carry the population, the
    relation and the subselection, one set per selection, so a source here
    is a second claim on one node (ch. 4.3)

The document. A well-formed two-selection union under a count that ALSO
carries `elementObjecttype: "Vondst"`. The union already says which
population each selection ranges over; the field on the count says it a
second time, for the whole node, and nothing decides which of the two a
consumer should read.

This is the setbron decision's ground (2026-09-19) applied at the node that
consumes the union: the wire may not say two things at once. It is the same
sentence the opgesomde-telling card wrote about an `elementObjecttype`
beside an operand list — "a second claim on one node" — and it is why the
arm names every field it found rather than the first.

**Measured, the whole clause and not just this spelling.** The arm reports
`elementObjecttype`, `bronRol`, `bronPad` and `filter` by name, joined with
`+`, so a count carrying several is one report naming all of them. This
document carries the one field that trips NOTHING else: a `bronRol` beside a
union additionally draws WF-047 (`a fold-source role binding on a node that
is not an instance fold over a relation`), measured, so it would have made
the witness a two-rule document.

## Rood-eerst, gemeten op dit document

The pre-card verifier cannot LOAD it — `LOAD ERROR at /resolvedRegels/0/waarde/argumenten/0/node: unknown
expr node "InstanceSetUnion"` — because the node entered the wire with this
card. So the honest red-first record is in two steps, both measured:

1. **Node on the wire, rule not yet written** (`spec/verifier/lib/checks.ml`
   md5 `c6ba34b2f4ece55bc0e6dac19e086ef2`, binary
   `cb285f7baacdbb16d8363b8119eed78b`): `verifier: PASS`.
2. **With WF-110** (source md5 `675c051a56e94f4c1e96abcda0b6a6ca`, binary
   `5fca029498f72359e648b5ea2a0f63b1`): the single rejection above.

## Falsifiable, and swept

Sweep over golden + rood + negatief + parity, 725 documents, the pre-card
verifier against this one: the ONLY documents whose output differs are the
seven `wf110-*` witnesses, which the pre-card binary cannot load. Not one
pre-existing document moves a verdict or a report line — the node is new, so
no document could carry it, and the one arm this card added to an EXISTING
rule (WF-034's type prologue exempts the union, which carries no `type` by
construction, exactly as it exempts a Lambda) is unreachable without one.
Arm-exact: the clause is keyed on the argument BEING a union, so it cannot
reach an ordinary count — where `elementObjecttype` beside `bronRol` is
exactly the right spelling and 109 corpus documents carry it.
