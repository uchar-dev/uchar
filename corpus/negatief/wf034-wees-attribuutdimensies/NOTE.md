Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6 (p)): a **side-table key** of an
object type that names no declared member.

    WF-034 FAIL /objecttypen/Schouwveld/attribuutDimensies/opbrengsten: attribuutDimensies key 'opbrengsten' names no
    declared attribute of 'Schouwveld': the side table resolves against no declaration

**What the table is.** An object type declares its members in two places —
`attributen` (name to datatype) and `kenmerken` (a name list) — and hangs
eight per-member FACET maps beside them, each keyed by a member name. Nothing
required those keys to name a declared member, and they are references: every
one of them is looked up by member name and by nothing else, so a key naming
no member resolves against no declaration.

**The harm is a silent semantic change, not a missing value.** A key spelled `opbrengsten` where the declared attribute is `opbrengst` declares
a dimensioned attribute that does not exist — and leaves the declared one
DIMENSIONLESS, so WF-040's requirement that a cell read pin every declared
dimension exactly once has nothing left to require.

**Who reads this table** (measured 2026-09-05 across the four consumers):
WF-004 walks this table for its own law (non-empty, duplicate-free, resolving
dimension list), and WF-040's label/dimension cross inside `walk_expr` reads it
per attribute. The Drools loader parses it and no consumer reads it back;
`ref` and the ASP backend do not read it at all.

**Measured.** A spurious key added to this table on each golden object type that can carry
one — a dimension must be declared for the value to resolve, which **2** golden
object types offer: 2 of 2 stayed verifier-clean before this card, and neither
does now. The renaming instrument does NOT apply here and is recorded as such:
renaming either of the 2 live keys is already refused by the verifier of the
day before (0 of 2 clean), because the attribute's cell reads lose their
pinning and WF-040 reports them. The ADDED key is the shape no rule caught.

**Executor observability.** Not measurable through the executors: no golden carrying this table has a case
file, because a dimension-cell read is declined as a FORM by both OCaml
executors. This arm is verifier-only, exactly like the wave-3 cell arms it sits
beside (ch. 4.6 (g)).

*Measured breadth:* zero documents lose anything. All 9 `attribuutDimensies` keys
the four zones held before this card name a declared attribute.

A hand-broken variant of `corpus/golden/jutterswet-vormenkabinet/ir.json` — one key added, a one-line diff;
schema-valid, since the eight tables are `additionalProperties`-typed maps on
the v0 wire.

Falsifiable, arm-exact: delete the `orphan otn "attribuutDimensies" ...` line from
`wf034_zijtabelsleutels` (`spec/verifier/lib/checks.ml`) and this case, alone
in all four zones, turns clean — 522 documents compared, one verdict moved, no
report elsewhere.
