# Step 2: both v0 Drools routes learn ever-unique temporal union keys

Goal: the maintainer decision of 2026-08-20 (uchar RS ch. 5.9:
temporal keys in uniqueness unions read EVER-uniqueness) lands in
`regelspraak-java` as a conformance card. Today the two Drools routes
disagree — the DRL transpiler skips a union containing a temporal key
entirely, the exec-model route checks it — and neither implements the
ever reading. Both routes must: claim every value a key timeline ever
carries (deduplicated per instance × source, so same-instance
redeclaration is no self-collision), collide across non-overlapping
periods, support at most one temporal component per compound source
key (refuse more, matching the uchar executors' loud edge), and
refuse a temporal key whose timeline is absent. Tests pin the
non-overlapping collision and the redeclaration non-collision in BOTH
routes; the working pin advances afterwards.

Acceptance: `make test` green with new fixtures exercising both
routes on the ever reading; the uchar anchor
`corpus/golden/anchor/jutterswet-nummerregister` semantics reproduced
(same verdicts on the same three case shapes).

May touch (toolchain repo): `DrlTranspiler`, `ExecModelTranspiler`,
`RegelSpraakRuntime` uniqueness helpers if shared, tests.

Out of scope: the same-moment tuple join for multiple temporal
components (loud in uchar too); scoped unions.
