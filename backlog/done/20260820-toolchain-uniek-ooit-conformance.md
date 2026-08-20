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

Result: landed at toolchain `83d9f24` (pushed), 948 tests green. The
exploration lens sharpened the diagnosis first: the "checking" DRL
route was in fact BROKEN — it joined the List<Segment> key by mvel `==`
(Segment declares no equals), so the rule silently never fired on any
collision; the exec-model route skipped honestly. Both now call one
shared runtime predicate (single-runtime principle):
`rsOoitWaarden(List<Segment>) → Set<BigDecimal>` (every value the
timeline ever carries, stripTrailingZeros-normalised so 5 and 5.00
collide, deduplicated so same-instance redeclaration claims once; a
null or empty timeline claims nothing) and `rsOoitBotst(a, b)` — the
ever-value intersection, a VALUE intersection, never a time one. The
DRL route emits `rsOoitBotst(veld, $ki)` for a temporal key position;
the exec route replaces `gelijkWaarde` by the same predicate; scalar
positions keep their joins. The >1-temporal-component fence lands
RESOLVER-side (the union stays a consistentieregel-WARNING and never
reaches the IR — both routes identically silent, mirroring the loud
uchar edge in spirit; the emitter can therefore never produce the
shape uchar red-pins by hand). Pinned by: two DrlTest units (the
ooit-join lowering; the two-temporal WARNING), two DroolsExecutionTest
firings reproducing the uchar anchor verdicts (7-in-2024 vs 7-in-2026
collides across disjoint periods with the redeclared 9/9 and sole 8
unique; the compound (4,3)-tuple collides across moments while (9,3)
stays unique), and an ExecModelDifferentialTest fixture that guards
DRL ≡ exec on the reading permanently. The pre-existing SINGLE-form
temporal-uniqueness fixture (List-identity semantics,
parity-faithful) stays untouched and green — that form was outside
the decision. Working pin advanced to `83d9f24`; CONSTRUCTS row 20
records the conformance.
