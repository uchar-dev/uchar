Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6): the v0 unknown-type sentinel on
the **element of a `ListType` that is a `SumType` constructor payload**,
inside the static type of a value node (ch. 4.7, ch. 2.4).

    WF-034 FAIL /rules/resolvedRegels/0/waarde/type: unknown-type sentinel on the
    list element inside the payload of constructor 'Toegewezen'

This is the **container half** of the descent `wf034-sentinel-sumtype-payload`
opened. That case closed the payload CHAIN — the fence in the expression
walk's type prologue matches the node's type at the top level only, and a sum
type carries its payloads as children — but the descent it added walked sums
and sums alone. A payload is a datatype like any other, so it may be a
`ListType`, and the sentinel then sits one further level down, past the end of
that walk. The descent now covers the whole payload subtree.

Nothing else stands in for it, for the same reason as in the chain case: a
`Construct` node reads no declaration at all (ch. 4.1), so WF-062, which
crosses the DECLARATION's constructor list, never sees a use site's inline
copy, and the identity family defers on the sentinel rather than crossing it.

Measured before the closure, on this document against all four cases beside
`corpus/golden/synth3-besluitschakel/`:

    ref    : oracle: PASS (3 expectations)      [each of the four cases]
    clingo : TRANSLATION FAULT: unscalable datatype in the ASP translation

That is the point, and it is the same disagreement the chain case pins: on a
verifier-clean document the two executors that carry sum types **disagree
about whether the document exists** — the reference interpreter replays it to
its pinned expectations, the ASP backend refuses to translate it.

*Measured dracht:* zero, and structurally so — no `SumType` constructor
payload anywhere in the 153 golden + 22 rood + 30 parity documents or in the
257 negatief cases that predate this card is a `ListType` or a `StructType`
at all, so the descent's new branches have nothing in the corpus to reach and
over-rejection is impossible by measurement. The verifier's verdict over all
462 of those documents is unchanged. That also means the witness could not be
produced by emptying a field: it is a payload datatype REPLACED by a
one-element container, the smallest edit that puts a sentinel where the
previous wave's walk stopped.

A hand-broken variant of `corpus/golden/synth3-besluitschakel/ir.json` — one
constructor payload replaced by
`{"node": "ListType", "element": {"node": "Onbekend", "reden": "niet opgelost"}}`,
a one-node diff. The shape is schema-valid: a payload is
`anyOf [datatype, null]`, `ListType.element` is a datatype, and `Onbekend` is
a datatype node on the v0 wire — which is precisely why the verifier, not the
schema, is the fence.

Falsifiable, clause-exact: drop the `ListType` branch from the payload
descent and this case turns clean together with
`wf034-sentinel-payload-onder-lijst` (which reaches its sentinel through the
same branch), while `wf034-sentinel-structveld-payload` and
`wf034-sentinel-sumtype-payload` stay red — measured over all four zones,
with every other document keeping its verdict.
