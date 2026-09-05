Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6 (p)): a **side-table key** of an
object type that names no declared member.

    WF-034 FAIL /objecttypen/Vondst/attribuutTijdlijnAantal/vondstdatum: attribuutTijdlijnAantal key 'vondstdatum' names no
    declared attribute of 'Vondst': the side table resolves against no declaration

**What the table is.** An object type declares its members in two places —
`attributen` (name to datatype) and `kenmerken` (a name list) — and hangs
eight per-member FACET maps beside them, each keyed by a member name. Nothing
required those keys to name a declared member, and they are references: every
one of them is looked up by member name and by nothing else, so a key naming
no member resolves against no declaration.

**The harm is a silent semantic change, not a missing value.** A key spelled `vondstdatum` where the declared attribute is `vondstdag` gives
the period count to an attribute that does not exist, and leaves the declared
one at the default of one period — a different timeline, not a missing one.

**Who reads this table** (measured 2026-09-05 across the four consumers):
WF-005 reads it twice: for its own count law (a facet count below 1 is
ill-formed) and as the adoption check of a named timeline. Both Drools
transpiler branches read it; `ref` and the ASP backend do not.

**Measured.** A spurious key added to this table on each of the 205 golden object types in
turn: **205 of 205** stayed verifier-clean before this card, none does now. The
renaming instrument, over the **1** live golden key: 1 of 1 clean before, 0
after.

**Executor observability.** Not measurable through the OCaml executors: no golden that carries this table
has a case file, so the one live key cannot be replayed. What the arm rests on
instead is WF-005's own reading — the count is half of the named-timeline
adoption check — and the two Drools transpiler branches that read it.

*Measured breadth:* zero documents lose anything. Both `attribuutTijdlijnAantal`
keys the four zones held before this card name a declared attribute.

A hand-broken variant of `corpus/golden/jutterswet-vondstverjaring/ir.json` — one key added, a one-line diff;
schema-valid, since the eight tables are `additionalProperties`-typed maps on
the v0 wire.

Falsifiable, arm-exact: delete the `orphan otn "attribuutTijdlijnAantal" ...` line from
`wf034_zijtabelsleutels` (`spec/verifier/lib/checks.ml`) and this case, alone
in all four zones, turns clean — 522 documents compared, one verdict moved, no
report elsewhere.
