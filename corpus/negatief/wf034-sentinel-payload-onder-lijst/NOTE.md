Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6): the v0 unknown-type sentinel on
a `SumType` constructor payload that is reached **through a container** — the
outer payload is a `ListType` whose element is a second sum type, and the
sentinel sits on that inner sum's payload (ch. 4.7, ch. 2.4).

    WF-034 FAIL /resolvedRegels/0/waarde/type: unknown-type sentinel on the
    payload of constructor 'Toegewezen'

Why this is a third case and not a repeat of the other two: the payload
descent is **mutually recursive**, and this document is the only artifact
that pins the direction container → sum. `wf034-sentinel-sumtype-payload`
pins sum → sum; `wf034-sentinel-lijstelement-payload` and
`wf034-sentinel-structveld-payload` pin sum → container. A walk that entered
containers but did not re-enter a sum found inside one would pass this
document, and a walk that recursed through sums but stopped at the first
container would pass it too. It is refused only when both halves of the
recursion are present.

The report names the innermost constructor and not the path that reached it.
That is deliberate and is what keeps the chain case's message byte-identical
across this widening: a nested sum re-enters the same walk it always did, so
the three sum-chain wordings the previous wave pinned did not move.

Nothing else stands in for it: a `Construct` node reads no declaration
(ch. 4.1), so nothing crosses a use site's inline constructor copy, and the
identity family defers on the sentinel rather than crossing it.

Measured before the closure, on this document against all four cases beside
`corpus/golden/synth3-besluitschakel/`:

    ref    : oracle: PASS (3 expectations)      [each of the four cases]
    clingo : TRANSLATION FAULT: unscalable datatype in the ASP translation

*Measured dracht:* zero, and structurally so — no `SumType` constructor
payload anywhere in the 153 golden + 22 rood + 30 parity documents or in the
257 negatief cases that predate this card is a container at all, so no
document in any zone changes verdict.

A hand-broken variant of `corpus/golden/synth3-besluitschakel/ir.json` — one
constructor payload replaced by a `ListType` over a one-constructor `SumType`
whose payload is `{"node": "Onbekend", "reden": "niet opgelost"}`, a one-node
diff; schema-valid, since every level of that nesting is a datatype the v0
wire admits.

Falsifiable, clause-exact: drop the `ListType` branch from the payload
descent and this case turns clean together with
`wf034-sentinel-lijstelement-payload`, while the struct case and the chain
case stay red — measured over all four zones, with every other document
keeping its verdict.
