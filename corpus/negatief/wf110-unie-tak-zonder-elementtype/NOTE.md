# wf110-unie-tak-zonder-elementtype

Violates WF-110 (`spec/IR-SPEC.md` ch. 4.3), the instance-set union — the
§5.7 concatenation of subject expressions, landed 2026-09-20 as the source
of an instance count. Clause (e): every selection names the population it ranges over.

    WF-110 FAIL /rules/resolvedRegels/0/waarde/argumenten/0/branches/1/elementType:
    an instance-set union's selection MUST name the population it ranges
    over (ch. 4.3)

The document. Selection 1 carries `"elementType": null` beside a resolving
`sourceRole`. The relation alone is not a selection: it gives the REACH, and
ch. 3.4a's contributors are reach ∩ POPULATION — an instance the relation
reaches whose type the population does not cover is not folded. Without the
population half there is no intersection to take and the count would be over
the reach, which is a different set and a different answer.

It is also what clause (d) compares. A selection with no population cannot
be crossed against selection 0's, so a union carrying one would slip past
the one-population rule as well; this arm is what keeps (d) total.

Its own witness rather than a shared one, on the house pattern: one witness
per wire spelling, not per field combination. The neighbouring spelling — a
selection with no `sourceRole` — is `wf110-unie-tak-zonder-bronrol`.

## Rood-eerst, gemeten op dit document

The pre-card verifier cannot LOAD it — `LOAD ERROR at /rules/resolvedRegels/0/waarde/argumenten/0/node: unknown
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
Arm-exact: a selection whose `elementType` is spelled but UNDECLARED draws
the neighbouring WF-034 arm as well (`instance-set selection element type
'Wrak' is not declared`, measured), so a MISSING name and an UNKNOWN name
never share a sentence.
