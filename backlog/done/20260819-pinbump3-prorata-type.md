# The third emitter pin bump: the pro-rata node carries its base type

Goal: the conformance debt recorded at the TIJDSEVENREDIG_DEEL landing
is settled spec-first. The toolchain (already changed and pushed,
working pin `aade1b8`) now types the pro-rata node with its base's
value type instead of the unknown-type sentinel WF-034 forbids, with an
emitter regression test pinning the wire type. This card lands the
uchar side: the bakenmeester emitter golden — re-emitted at the new
pin, verifier-clean at last — enters `corpus/golden/` through
`scripts/admit.sh` with a case per executable edge, and the CONSTRUCTS
row that recorded the pin-bump candidate records the landing instead.

Acceptance: all eight gates green (`bash scripts/check-*.sh`); the
bakenmeester golden replays differentially (gates 5–6) and its ir.json
is byte-identical to the `aade1b8` emitter output; CONSTRUCTS row 92
names the new pin.

May touch: `corpus/golden/jutterswet-bakenmeester/`,
`spec/CONSTRUCTS.md`, `backlog/`.

Out of scope: any further toolchain change (the pin is pushed); other
emitter deltas; new operator work.

Result: settled spec-first. The toolchain types the pro-rata node with
its base's value type instead of the WF-034 unknown-type sentinel
(ExpressieBouwer.functieType gains the TIJDSEVENREDIG_DEEL arm),
working pin `f35d541` pushed: `aade1b8` carried the typing fix, and the
verification round then caught the first regression test asserting a
token the emitter never writes while matching the base argument's type
in a character window — vacuously green under a revert; the sharpened
test scopes to the node's OWN type field and was proven red on a revert
probe. The bakenmeester emitter golden entered `corpus/golden/` through
`scripts/admit.sh`, byte-identical to the fresh emitter output
(sha-verified by the lens), replaying exactly in both executors
(March 24, April 24 × 15/30 = 12, half-open). Recorded honestly, no
action needed for the unit-free corpus: on a RATE base the static type
is the PRE-restatement unit (the transpiler restates €/jr → €/mnd
before proration), benign because rate units are inter-convertible in
v0's assignment check; and the concrete type un-shortcircuits v0's
§6.1.2 precision check for a base declared finer than its target —
arguably correct, no fixture reaches it. The uchar executors fence
unit-carrying pro-rata loudly regardless.
