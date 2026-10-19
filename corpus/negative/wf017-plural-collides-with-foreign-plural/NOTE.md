# wf017-plural-collides-with-foreign-plural

Violates WF-017 (`spec/IR-SPEC.md` ch. 2.9), arm (b): a day kind's `plural`
that is also a spelling of another day kind — here the other day kind's
plural, from which it differs in case alone.

    WF-017 FAIL /model/dayKinds/1/plural: the plural 'SchouwDagen' of day kind
    'stormvloeddag' is the plural 'schouwdagen' of day kind 'schouwdag': one
    spelling names two day kinds

A duration of `5 schouwdagen` could then count the days of either kind, and a
consumer resolving the spelling has no second reading to fall back on.
Spellings are compared under the ch. 2.1 identity (collapse whitespace, lower
case), the one a consumer applies when it resolves a unit spelling, so
`SchouwDagen` and `schouwdagen` are one spelling. This witness pins that
identity: a byte-exact comparison passes it. Until L3 the identity was pinned
from the positive side, by entries of the separate spelling registry of
`corpus/golden/jutterswet-vormenkabinet` that bound to their own day kind only
through it; those entries left the wire with the registry.

Until L3 this collision could not be seen at all: a registry held one day kind
per spelling, so the losing registration was simply absent. Since L3 each day
kind carries its own `plural`, and both halves of the collision are on the
wire.

The document is `corpus/golden/jutterswet-schouwrooster` with the plural of
`stormvloeddag` respelled `SchouwDagen`. `expect.txt` pins the arm.
