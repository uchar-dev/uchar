# wf110-unie-onder-een-builtin

Violates WF-110 (`spec/IR-SPEC.md` ch. 4.3), the instance-set union — the
§5.7 concatenation of subject expressions, landed 2026-09-20 as the source
of an instance count. Clause (a): the union is admissible under the instance count and nowhere else.

    WF-110 FAIL /rules/resolvedRegels/0/versions/0/value/arguments/0: an
    instance-set union stands where nothing consumes it: the union of subject
    expressions denotes a set of INSTANCES, and the one node that folds
    instances is the instance count — so this node is admissible as the SINGLE
    argument of an AANTAL aggregation and at no other position (ch. 4.3)

The document. The two-selection union as the single operand of
`ABSOLUTE_WAARDE` — a single-argument BUILTIN, so the arity clause of
WF-037 is satisfied and this position's only defect is the position.

Why a builtin and not a fold. Under `SOM` the same document draws a second
citation, measured: `WF-037 FAIL … value fold with fewer than two operands`,
because a sum with no element type and one argument is already ill-formed
for a reason of its own. The builtin isolates clause (a): there is nothing
wrong with the node's arity, with its own type, or with the union — only
with what stands above it.

What the clause says, positively. The union denotes INSTANCES. Every other
consumer in the IR reads a VALUE, and there is no value here to read: a
selection names a population and a relation, not an attribute. The one node
that folds instances rather than values is the instance count (ch. 1.2e,
ch. 3.4a), so the union's admissible position is its single argument. That
is the same shape of rule WF-096 drew for the set reading until L2 — one node
consumes a collection, and the collection is ill-formed anywhere else.

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
Arm-exact: admissibility is carried DOWN one level by an optional argument
with a false default, so it reaches the argument the count hands over and no
descendant of it — a union nested inside a selection's filter is at a wrong
position again and says so.

Since L4 the node is an `Aggregation` or a `BuiltinCall` (delta B21), each
with only its own fields — `arguments`, and on an aggregation `elementType`,
`emptyFallback`, `dimensionSelection`, `sourceRole` and `sourcePath`, on a
builtin `granularity`; the report cites and names them. `expect.txt` pins the
arm.
