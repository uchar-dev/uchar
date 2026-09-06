Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6 (p)): a **side-table key** of an
object type that names no declared member.

    WF-034 FAIL /objecttypen/Vondst/kenmerkTijdlijnAantal/verjaart: kenmerkTijdlijnAantal key 'verjaart' names no
    declared characteristic of 'Vondst': the side table resolves against no declaration

**What the table is.** An object type declares its members in two places —
`attributen` (name to datatype) and `kenmerken` (a name list) — and hangs
eight per-member FACET maps beside them, each keyed by a member name. Nothing
required those keys to name a declared member. Every key is a reference, and it
is more than that: it is the v0 DENORMALISATION of a facet that v1 carries on
the member node itself (`spec/CONSTRUCTS.md` row 11 — "each v0 map entry folds
into the corresponding member node"), so the key IS that member's identity,
written out beside the declaration instead of on it. A key naming no member
resolves against no declaration; and because an identity is carried VERBATIM
(ch. 2.1's carriage rule, not its matching rule), the key must be the member's
declared spelling, case and whitespace included.

**The harm is a silent semantic change, not a missing value.** A key spelled `verjaart` where the declared characteristic is `verjaard` gives
the period count to a characteristic that does not exist, and leaves the
declared one at the default of one period.

**Who reads this table** (measured 2026-09-05 across the four consumers):
WF-005 reads it for the count law (a facet count below 1 is ill-formed); both
Drools transpiler branches read it. `ref` and the ASP backend do not.

**Measured.** A spurious key added to this table on each of the 205 golden object types in
turn: **205 of 205** stayed verifier-clean before this card, none does now. The
renaming instrument has nothing to rename: **no golden carries a
`kenmerkTijdlijnAantal` key at all** — the one key the four zones hold sits in
the negatief zone. That is stated here rather than left to be found: this arm's
regression evidence is the added-key sweep and the four-zone comparison, not a
live golden key.

**Executor observability.** Not measurable through the executors, for the same reason: no golden carries the
table, so there is nothing to replay. The arm rests on WF-005's count law and on
the two Drools transpiler branches that read it.

*Measured breadth:* zero documents lose anything. The single `kenmerkTijdlijnAantal`
key the four zones held before this card names a declared characteristic.

A hand-broken variant of `corpus/golden/jutterswet-vondstverjaring/ir.json` — one key added, a one-line diff;
schema-valid, since the eight tables are `additionalProperties`-typed maps on
the v0 wire.

Falsifiable, arm-exact: delete the `orphan otn "kenmerkTijdlijnAantal" ...` line from
`wf034_zijtabelsleutels` (`spec/verifier/lib/checks.ml`) and this case, alone
in all four zones, turns clean — 523 documents compared, one verdict moved, no
report elsewhere.

**Why the spelling and not just the name** (measured 2026-09-06). The consumers
do not agree on how they look this facet up: the verifier, `ref` and the ASP
backend resolve the key case-insensitively, the bean-model executor does not —
it reaches this table through raw case-sensitive map lookups at **7** sites.
So a key differing from its declaration in capitalisation alone is found by
three consumers and missed by the fourth, which drops the document out of the
differential without a word. The witness for that flank is
`corpus/negatief/wf034-zijtabel-kapitalisatiewees`, on this table.
