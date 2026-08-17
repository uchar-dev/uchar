# Emitter pin bump: the toolchain emits verbindendeRol and bronRol

Goal: the first pin bump of the wire-entry discipline. The toolchain
repo now emits two of the extension fields the anchors pioneered — the
allocation's `verbindendeRol` and the instance fold's `bronRol`
(emitted only when uniquely derivable from declared names; absent
otherwise, never null). This card lands the conformance side here:
re-emit the five emitter goldens on the new toolchain commit, show the
byte-diff is exactly the additive extension fields, and move the pin in
`spec/IR-SPEC.md` § Method (with the "the pinned v0 emitter never
produces it" sentence corrected to name what the pinned emitter now
produces).

Acceptance: all six gates green; `git diff` over the five emitter
goldens shows only added `verbindendeRol`/`bronRol` objects (no other
byte changes); IR-SPEC § Method names the new emitter commit;
`CONSTRUCTS.md` rows 4/17 record the landing.

May touch: `corpus/golden/{jutterswet-berging,jutterswet-penningdrempel,jutterswet-penningjaar,jutterswet-vondstnummer,jutterswet-vondsttelling}/ir.json`,
`spec/IR-SPEC.md` (§ Method pin note), `spec/CONSTRUCTS.md` (landing
notes), `backlog/`.

Out of scope: emitting the remaining extensions (`TemporalRead`,
ladders, symbolic creation fields — their surface syntax does not exist
in the toolchain yet; recorded, not attempted); any v1 format bump;
schema or verifier changes (the fields are already admitted).
