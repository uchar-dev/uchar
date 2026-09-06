Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6 (p)): a **side-table key** of an
object type that names no declared member.

    WF-034 FAIL /objecttypen/Vondst/attribuutTijdlijnNaam/vondstdatum: attribuutTijdlijnNaam key 'vondstdatum' names no
    declared attribute of 'Vondst': the side table resolves against no declaration

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

**The harm is a silent semantic change, not a missing value.** A key spelled `vondstdatum` where the declared attribute is `vondstdag` adopts a
named timeline for an attribute that does not exist, and leaves the declared one
with whatever its own facets say — the adoption, and WF-005's consistency check
on it, both aimed at nothing.

**Who reads this table** (measured 2026-09-05 across the four consumers):
WF-003 crosses the VALUE against the declared timelines; WF-005 crosses the
adopted granularity and period count against the timeline's own. The Drools
loader parses the map and no consumer reads it back; `ref` and the ASP backend
do not read it at all.

**Measured.** A spurious key added to this table on each of the 205 golden object types in
turn: **205 of 205** stayed verifier-clean before this card, none does now —
the witness here uses a `null` value deliberately, so that neither WF-003's
timeline reference nor WF-005's adoption check has anything to say and the
document carries exactly one violation. The renaming instrument does not
apply: renaming either of the **2** live golden keys is already refused before
this card (0 of 2 clean), because WF-005 then compares the orphan's absent
facets against the named timeline's.

**Executor observability.** Renaming the one live key that sits on a golden with a case file
(`corpus/golden/jutterswet-penningjaar`) and replaying it leaves `ref` and the
ASP backend printing a byte-identical status line: the adoption is resolved
before either of them looks. This arm is a wire fence.

*Measured breadth:* zero documents lose anything. Both `attribuutTijdlijnNaam`
keys the four zones held before this card name a declared attribute.

A hand-broken variant of `corpus/golden/jutterswet-vondstverjaring/ir.json` — one key added, a one-line diff;
schema-valid, since the eight tables are `additionalProperties`-typed maps on
the v0 wire.

Falsifiable, arm-exact: delete the `orphan otn "attribuutTijdlijnNaam" ...` line from
`wf034_zijtabelsleutels` (`spec/verifier/lib/checks.ml`) and this case, alone
in all four zones, turns clean — 523 documents compared, one verdict moved, no
report elsewhere.

**Why the spelling and not just the name** (measured 2026-09-06). The
bean-model executor's loader stores this table and reads it back nowhere, so
this table has no case-sensitive reader of its own; four of the eight do have
one (108 raw case-sensitive lookups between them), and the rule is uniform
across the eight because the key is a carried identity, not a matching site.
The witness for that flank is
`corpus/negatief/wf034-zijtabel-kapitalisatiewees`, on the most heavily
case-sensitively read of the characteristic tables.
