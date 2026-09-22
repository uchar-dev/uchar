# wf110-unie-tak-zonder-bronrol

Violates WF-110 (`spec/IR-SPEC.md` ch. 4.3), the instance-set union — the
§5.7 concatenation of subject expressions, landed 2026-09-20 as the source
of an instance count. Clause (e): every selection names the relation it ranges over.

    WF-110 FAIL /resolvedRegels/0/waarde/argumenten/0/branches/1/sourceRole:
    an instance-set union's selection MUST name the relation it ranges
    over: there is no bindingless fallback here, because every selection of
    one union shares one type pair (clause (d)) and a derived range would
    make them indistinguishable (ch. 4.3)

The document. Selection 0 names `vondstaanmelding`/`aangemelde vondst`;
selection 1 carries `"sourceRole": null` and nothing else is wrong with it —
its population is declared, its `bron` reads as written.

**Why there is deliberately no bindingless fallback, and why the reason is
this rule's own rather than borrowed.** WF-048 derives a connecting relation
from a TYPE PAIR — subject type and element type — and admits it when
exactly one fact type connects them. Clause (d) gives every selection of one
union the SAME type pair. So a derived range would give every bindingless
selection the SAME relation, and a union of two such selections is a
restatement of one of them: the document would be well-formed, would count
something, and would count the wrong thing silently. That is the misbinding
WF-102 closed for the multi-step range, arriving at a new position; the
answer is the same one, and it is stated here rather than inherited, because
WF-048's own text is about a fold and an allocation and says nothing about
this node.

The cost is nil: a written selection names its relation, and the frontend
resolves it before it can emit anything.

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
Arm-exact: the (e) arms report per selection and per field, so this document
names selection 1 and its `sourceRole` alone. The sister spelling — a
selection with no `elementType` — falls on the neighbouring arm, with its
own witness (`wf110-unie-tak-zonder-elementtype`).
