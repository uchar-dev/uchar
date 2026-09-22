# wf110-unie-als-regelwaarde

Violates WF-110 (`spec/IR-SPEC.md` ch. 4.3), the instance-set union — the
§5.7 concatenation of subject expressions, landed 2026-09-20 as the source
of an instance count. Clause (a), at the sharpest position: the whole rule value.

    WF-110 FAIL /resolvedRegels/0/waarde: an instance-set union stands
    where nothing consumes it: the union of subject expressions denotes a
    set of INSTANCES, and the one node that folds instances is the instance
    count — so this node is admissible as the SINGLE argument of an AANTAL
    aggregation and at no other position (ch. 4.3)

The document. The union standing as a rule's entire `waarde`, assigned to a
whole-number attribute. Measured before the rule: verifier-clean. No type
rule catches it, and that is the point — the union carries no `type` key at
all (it denotes instances), and WF-034's type prologue exempts it for
exactly that reason, the way it exempts a Lambda. Without clause (a) the
typelessness that makes the node honest would also make it invisible.

This is the sibling of `wf096-setlezing-regelwaarde`, and deliberately so:
a collection standing where a scalar is written is the same defect whichever
collection it is, and each collection's own rule says it about its own
node.

## Rood-eerst, gemeten op dit document

The pre-card verifier cannot LOAD it — `LOAD ERROR at /resolvedRegels/0/waarde/node: unknown
expr node "InstanceSetUnion"` — because the node entered the wire with this
card. So the honest red-first record is in two steps, both measured:

1. **Node on the wire, rule not yet written** (`spec/verifier/lib/checks.ml`
   md5 `c6ba34b2f4ece55bc0e6dac19e086ef2`, binary
   `cb285f7baacdbb16d8363b8119eed78b`): `verifier: PASS` — no rule reached it.
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
Arm-exact: the rule-value position never sets the admissibility flag, so
this witness falls on the default and the count's own argument does not.
