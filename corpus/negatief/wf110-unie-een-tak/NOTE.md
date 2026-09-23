# wf110-unie-een-tak

Violates WF-110 (`spec/IR-SPEC.md` ch. 4.3), the instance-set union — the
§5.7 concatenation of subject expressions, landed 2026-09-20 as the source
of an instance count. Clause (b): a union carries at least TWO selections.

    WF-110 FAIL /rules/resolvedRegels/0/waarde/argumenten/0: an instance-set
    union carries at least TWO selections and this one carries 1: a single
    selection IS the ordinary instance count, spelled with the fold's own
    elementObjecttype and bronRol, so one meaning keeps one spelling
    (ch. 4.3)

The document. A `Jutter` who counts her `aantal strandvondsten` over a
union with exactly ONE selection — the aangemelde vondsten, the relation
`vondstaanmelding`. Everything else holds: the count carries no source of
its own (clause (c)), the selection names a declared population and a
resolving relation (clause (e)), and the node sits at its admissible
position (clause (a)).

Why the form is refused rather than read as the count it obviously means.
One selection has a spelling already: `elementObjecttype` beside `bronRol`
on the fold itself, which every one-source count in the corpus carries and
all four executors read. Admitting it here would give one meaning two
spellings, which is the discipline WF-102 (b) draws for the one-step range
("the one-step range IS bronRol") and WF-087 for the nullary keuzetype. The
cost of the refusal is nil: a producer that has one selection writes the
form that already exists.

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
Arm-exact: the (b) arm reads `branches` and nothing else, so it cannot
reach a two-or-more union; the empty union falls on the same arm (measured,
same sentence with `0`).
