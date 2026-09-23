# wf110-unie-ongelijke-elementtypes

Violates WF-110 (`spec/IR-SPEC.md` ch. 4.3), the instance-set union — the
§5.7 concatenation of subject expressions, landed 2026-09-20 as the source
of an instance count. Clause (d): the selections range over ONE population.

    WF-110 FAIL /rules/resolvedRegels/0/waarde/argumenten/0/branches/1/elementType:
    the selections of an instance-set union range over ONE population:
    selection 0 names 'Vondst' and this one names 'Kavel' (ch. 4.3)

The document. The union of "aangemelde vondsten van de Jutter" (element
type `Vondst`, relation `vondstaanmelding`) and "duinkavels van de Jutter"
(element type `Kavel`, relation `kaveltoewijzing`). Both selections are
well-formed on their own; what the wire may not say is that one count
ranges over two populations.

**The boundary, and the two different reasons for it.** The v0 toolchain
refuses the same written shape and its own test says why: one Drools
accumulate has ONE source pattern, so operands over different element types
cannot be unioned without silently dropping the foreign set. That is a
property of a code generator. uchar refuses it on a ground of its own: a
count answers a cardinality over one DECLARED type — `elementObjecttype` is
the population everywhere else in the IR (ch. 2.2) — and with a population
per selection the membership test differs per member. Whether two selections
can then share an instance is a question about the subtype lattice
(ch. 2.6, where a role filler is an UPPER bound and `Munt ⊑ Vondst`), and
deduplication on instance identity would have to answer it. This rule does
not, and says so rather than picking an answer by side effect.

The door is closed and not locked. A later card with a real casus may norm
the multi-population union; it would be a clause here, with its own dedup
statement across the lattice, not an executor's choice.

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
Arm-exact: the (d) arm compares each selection against selection 0 and
reports at the selection that differs, so the equal-population union — the
form the goldens carry — is untouched by construction.
