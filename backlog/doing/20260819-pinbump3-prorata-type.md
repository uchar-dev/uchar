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
