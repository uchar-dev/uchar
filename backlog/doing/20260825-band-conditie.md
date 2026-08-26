# The band condition reaches ref and clingo

Goal: the re-measurement (backlog/done/20260825-toka-hermeting.md)
surfaced ONE scenario now stranding on a condition kind ref does not
carry — the "band condition" (it surfaced from behind the unit-literal
fault, so no exploration report covers it yet). INVESTIGATE FIRST:
identify the exact wire form (own-authored .rgs through the toolchain
resolver in /tmp — the band/"ligt tussen"-style comparison or whatever
the kind proves to be), the production semantics in
RegelSpraakRuntime/DrlTranspiler (boundary inclusivity, empty operands,
mixed types), and ref's exact fault site. Then close it spec-first,
narrowest form, v0-mirror: RUNTIME-SEMANTICS clause + worked example
(pin the boundary corners: on-boundary, off-boundary, empty operand),
ref evaluation, clingo translation (or a motivated loud fault + S19
entry if the form genuinely cannot ride the integer encoding — justify
it), witness golden with hand-computed cases on both sides of each
boundary through both executors, WF clause + negatief only if a
genuinely ill-formed variant exists. RIGHTS RULE: the investigation
happens in the toolchain and /tmp; everything landing in uchar is
own-authored Jutterswet material.

Acceptance:
- Red-first: the verifier-clean own-authored repro faults in today's
  ref (fault text quoted); after the change both executors replay the
  golden and agree (gates 5–6), boundary corners pinned.
- All nine gates green with counts.

May touch: `spec/RUNTIME-SEMANTICS.md`, `spec/IR-SPEC.md` (only if a
WF clause is needed), `spec/verifier/`, `ref/`, `backends/clingo/`,
`spec/ASP-TRANSLATION.md`, `corpus/golden/jutterswet-*`,
`corpus/negatief/` (only with a WF clause).
Out of scope: any other condition kind (narrowest change),
`spec/CONSTRUCTS.md` (serial at merge).
