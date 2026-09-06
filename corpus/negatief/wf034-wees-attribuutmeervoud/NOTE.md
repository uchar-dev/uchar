Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6 (p)): a **side-table key** of an
object type that names no declared member.

    WF-034 FAIL /objecttypen/Vondst/attribuutMeervoud/vondstdatum: attribuutMeervoud key 'vondstdatum' names no
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

**The harm is a silent semantic change, not a missing value.** A key spelled `vondstdatum` where the declared attribute is `vondstdag` gives the
plural to an attribute that does not exist, and leaves the declared one without
one. No consumer complains: every reader looks the facet up BY the declared
member's name, finds nothing, and takes the default.

**Who reads this table** (measured 2026-09-05 across the four consumers):
WF-057 crosses the VALUE for target-text-safety (the plural is a declared name
like any other). The Drools loader parses the map into its object-type record
and no transpiler, runtime or case-runner code reads it back; `ref` and the ASP
backend do not read it at all.

**Measured.** A spurious key added to this table on each of the 205 golden object types in
turn: **205 of 205** stayed verifier-clean under the verifier of the day before
this card, and none does now. The complementary instrument — renaming a LIVE
key, so the declared member silently loses its plural — was measured on all
**6** golden `attribuutMeervoud` keys: 6 of 6 clean before, 0 after.

**Executor observability.** Nothing sees it. Renaming each of the 6 live keys and replaying every case of
its golden gave **8** measurements, and in all 8 `ref` and the ASP backend print
a byte-identical status line before and after. The plural is presentation to
them; this arm is a wire fence, and it is the only fence there is.

*Measured breadth:* zero documents lose anything. All 924 side-table keys the
four zones held before this card resolve against a declared member — 24 in this
table — and the only orphans anywhere are this wave's own eight witnesses.

A hand-broken variant of `corpus/golden/jutterswet-vondstverjaring/ir.json` — one key added, a one-line diff;
schema-valid, since the eight tables are `additionalProperties`-typed maps on
the v0 wire.

Falsifiable, arm-exact: delete the `orphan otn "attribuutMeervoud" ...` line from
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
