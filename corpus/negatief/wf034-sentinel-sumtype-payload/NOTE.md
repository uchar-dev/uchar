Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6): the v0 unknown-type sentinel on a
`SumType` **constructor payload**, inside the static type of a value node.

    WF-034 FAIL /rules/resolvedRegels/0/waarde/type: unknown-type sentinel on the
    payload of constructor 'Toegewezen'

Why the existing sentinel fence missed it: that fence is a match on the node's
type at the TOP level — `Some (Datatype_onbekend _)` — so it sees the sentinel
only when the whole type is the sentinel. A datatype is a tree, and a
`SumType` carries its constructors' payload datatypes as children; one level
down the fence was not there. `enum_use_site` (WF-088) descends through those
same payloads for exactly this reason; the sentinel walk now does too.

Nothing else stands in for it here. WF-062, the constructor rule, reads the
DECLARATION (`sum_decl`) and never the use site's inline constructor copy —
the sum-type twin of the drift flank WF-072/WF-088 close for structs and
enumerations, and a recorded open flank of WF-061. What DOES catch the
sentinel in most payload positions is a neighbour: of the 128 constructor
payloads the goldens carry outside a list element, 116 are refused, 55 of them
by the identity family's residual-structure cross (WF-091) and the rest by the
declaration and binder rules (WF-060/061/062/064/068/069/070). The twelve that
survive are positions where every one of those stands down — a `Construct`
node reads no declaration at all (ch. 4.1) — and this witness sits on one of
them.

Measured before the closure, on this document against
`corpus/golden/synth3-besluitschakel/case1-toegewezen.json`:

    ref    : oracle: PASS (3 expectations)
    clingo : TRANSLATION FAULT: unscalable datatype in the ASP translation
    Drools : EXCLUDE loader: form not carried by the v0 Drools backend:
             Datatype:SumType (unknown datatype node)

The first two lines are the point: on a verifier-clean document the two
executors that both carry sum types **disagree about whether the document
exists** — the reference interpreter replays it to its pinned expectations,
the ASP backend refuses to translate it. The Drools line says nothing either
way: that backend does not carry `SumType` at all, and prints the same
exclusion for the unbroken golden.

*Measured dracht:* zero outside this witness — no sentinel sits on any
constructor payload in 152 golden + 22 rood + 30 parity documents or in the
238 negatief cases that predate this card. A sweep injecting the sentinel at
all 2650 datatype-valued positions of the goldens one at a time left 36
verifier-clean survivors, of which 15 are no-ops (the position already holds
the sentinel: the documented row-1 fold-source wart, where a count's source
names a role) — so 21 real ones: **12 on constructor payloads**, which this
clause closes, and 9 on fold-/aggregation argument types, which stay open (the
aggregation branch replaces the generic walk for those arguments). Over-
rejection is therefore impossible by measurement.

A hand-broken variant of `corpus/golden/synth3-besluitschakel/ir.json` — one
payload datatype replaced by `{"node": "Onbekend", "reden": "niet opgelost"}`,
a one-field diff. The shape is schema-valid: a constructor payload is
`anyOf [datatype, null]` and `Onbekend` is a datatype node on the v0 wire —
which is precisely why the verifier, not the schema, is the fence.

Not closed here, and measured with the same sweep: **WF-007** has the identical
blind spot at the identical twelve positions — a `{"geheel": true,
"decimalen": 2}` payload stays verifier-clean at all twelve. Its shape check
is a per-datatype predicate, not a walk, and its blind spot spans list
elements and struct fields as well (recorded OPEN by
`backlog/done/20260903-wf007-schrijfdoel.md`), so sharing this descent is not
one movement and would need its own witnesses per container.

Falsifiable, clause-exact: drop the payload descent from the sentinel check in
`walk_expr`'s type prologue and this case alone turns clean, every other
`wf034-*` case staying red.
