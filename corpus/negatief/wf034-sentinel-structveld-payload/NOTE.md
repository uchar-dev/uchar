Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6): the v0 unknown-type sentinel on
a **field of a `StructType` that is a `SumType` constructor payload**, inside
the static type of a value node (ch. 4.7, ch. 4.9, ch. 2.4).

    WF-034 FAIL /rules/resolvedRegels/0/waarde/type: unknown-type sentinel on field
    'kern' inside the payload of constructor 'Toegewezen'

The record half of the container descent whose list half is
`wf034-sentinel-lijstelement-payload`. Both spellings need a witness because
they are two different branches of the walk: a list has one anonymous element
slot, a struct has a named field per position, and a descent that covers one
says nothing about the other. Together with the chain case
(`wf034-sentinel-sumtype-payload`) they pin the whole payload subtree: sum,
list, record.

The message names the field, which is not decoration — a record payload can
carry several fields and only one of them ill-formed, and a walk that
reported "somewhere under this payload" would leave the reader to find it.

Nothing else stands in for it: a `Construct` node reads no declaration
(ch. 4.1), so WF-062 crosses only the declared constructor list and never a
use site's inline copy; WF-072's struct-declaration cross keys on a declared
struct domain, which an inline payload need not name; and the identity family
defers on the sentinel instead of crossing it.

Measured before the closure, on this document against all four cases beside
`corpus/golden/synth3-besluitschakel/`:

    ref    : oracle: PASS (3 expectations)      [each of the four cases]
    clingo : TRANSLATION FAULT: unscalable datatype in the ASP translation

The reference interpreter replays a document the ASP backend refuses to
translate — two executors disagreeing about whether a verifier-clean document
exists.

*Measured dracht:* zero, and structurally so — no `SumType` constructor
payload anywhere in the 153 golden + 22 rood + 30 parity documents or in the
257 negatief cases that predate this card is a `StructType` or a `ListType`
at all. Over-rejection is impossible by measurement and the verifier's
verdict over all 462 of those documents is unchanged.

A hand-broken variant of `corpus/golden/synth3-besluitschakel/ir.json` — one
constructor payload replaced by a one-field `StructType` whose field datatype
is `{"node": "Onbekend", "reden": "niet opgelost"}`, a one-node diff. The
shape is schema-valid: a payload is `anyOf [datatype, null]`, a struct field
carries a datatype, and `Onbekend` is a datatype node on the v0 wire — which
is precisely why the verifier, not the schema, is the fence.

Falsifiable, clause-exact: drop the `StructType` branch from the payload
descent and this case alone turns clean, the two list cases and the chain
case staying red — measured over all four zones, with every other document
keeping its verdict.
