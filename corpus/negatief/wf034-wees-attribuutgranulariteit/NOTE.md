Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6 (p)): a **side-table key** of an
object type that names no declared member.

    WF-034 FAIL /objecttypen/Vondst/attribuutGranulariteit/vondstdatum: attribuutGranulariteit key 'vondstdatum' names no
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

**The harm is a silent semantic change, not a missing value.** This is the sharpest of the eight. A key spelled `vondstdatum` where the
declared attribute is `vondstdag` says "this attribute is time-dependent" about
an attribute that does not exist, and leaves the declared one silently
NON-temporal. Temporality is not a decoration: it decides whether a value is a
timeline or a scalar, whether `gedurende` may be written over it, and whether a
pro-rata base is legal.

**Who reads this table** (measured 2026-09-05 across the four consumers):
The most-read of the eight. In the verifier: `attr_temporal`, WF-005's
named-timeline adoption, WF-043's whole-period qualifier inside `walk_expr`, the
temporal prologue of `expression_layer`, and WF-065. In `ref`: the case loader
and the evaluator. In the ASP backend: the translator, three times. In the
Drools backend: both transpiler branches, the contract validator, the fact-model
generator and the case runner. One orphan key switches all of them off at once.

**Measured.** A spurious key added to this table on each of the 205 golden object types in
turn: **205 of 205** stayed verifier-clean before this card, none does now. The
renaming instrument, over all **33** live golden keys: **20 of 33** clean
before (the other 13 were already refused, because the attribute's own reads
lose their temporal ground and WF-043 or WF-005 reports them), and 0 after.

**Executor observability.** Renaming each live key and replaying every case of its golden gave **99**
measurements, and they split three ways. **91** are loud in both OCaml executors
(`CASE ERROR: … a timeline value on a non-temporal attribute`, or an evaluation
fault on `gedurende`). **6** are silent in both — the golden replays to its
pinned expectations with the attribute's temporality simply gone. And **2**
DIVERGE: on `corpus/golden/synth2-prorata-breed` case4, renaming
`Ligplaats.seizoenbasis` or `Ligplaats.toeslagdeel` leaves `ref` replaying to its
pinned expectations while the ASP backend refuses to translate (`pro-rata base
"seizoenbasis" is not a temporal attribute (WF-056)`). A verifier-clean document
that one executor answers and another declines is the class an engine-neutral IR
must not admit.

*Measured breadth:* zero documents lose anything. All 74 `attribuutGranulariteit`
keys the four zones held before this card name a declared attribute.

A hand-broken variant of `corpus/golden/jutterswet-vondstverjaring/ir.json` — one key added, a one-line diff;
schema-valid, since the eight tables are `additionalProperties`-typed maps on
the v0 wire.

Falsifiable, arm-exact: delete the `orphan otn "attribuutGranulariteit" ...` line from
`wf034_zijtabelsleutels` (`spec/verifier/lib/checks.ml`) and this case, alone
in all four zones, turns clean — 523 documents compared, one verdict moved, no
report elsewhere.

**Why the spelling and not just the name** (measured 2026-09-06). The consumers
do not agree on how they look this facet up: the verifier, `ref` and the ASP
backend resolve the key case-insensitively, the bean-model executor does not —
it reaches this table through raw case-sensitive map lookups at **64** sites.
So a key differing from its declaration in capitalisation alone is found by
three consumers and missed by the fourth, which drops the document out of the
differential without a word. The witness for that flank is
`corpus/negatief/wf034-zijtabel-kapitalisatiewees`, on this table.
