Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6): an **allocation** (ch. 3.2)
that names no source attribute. The amount an allocation distributes lives in
`sourceAttribute` on the source type; with the field `null` there is no
declaration to read it from.

    WF-034 FAIL /rules/verdelingen/0/versions/0/sourceAttribute: allocation
    without a source attribute: it resolves against no declaration

Why this needs an arm of its own, away from the expression walk: an
allocation is **not an expression**, so `walk_expr` never reaches it, and its
two endpoint attributes are the one pair of references the whole expression
layer is blind to. Every rule that does read them sits behind `Some a` —
WF-025's orderable-type check on the ordering key, WF-026's same-target
comparison, WF-050's overlap with a defaults group, and the dependency
graph's read edge from the allocation to the source attribute — so they all
fall silent together on one empty field. Measured: **11 of 11** golden
allocations stayed verifier-clean with this field emptied, and 0 do after
this clause.

Measured before the closure, on this document against the two cases beside
`corpus/golden/jutterswet-gelijkdeling/`:

    ref    : EVALUATION FAULT: missing bronAttribuut in IR node
    clingo : TRANSLATION FAULT: missing bronAttribuut

Both executors refuse, loudly and at run time, on a document the verifier had
already called well-formed.

*Measured dracht, stated exactly because it is not zero:* the 153 golden, 22
rood and 30 parity documents carry no allocation with an empty
`bronAttribuut`, but **four** negatief cases that predate this card do —
`wf020-crossform-collision`, `wf024-complex` (removed at L4, where `COMPLEX` became a load error), `wf025-max-without-ratio` and
`wf026-two-allocations-one-target`, each a minimal hand-written document that
spells only the fields its own rule needs. They were already refused, they
stay refused, and they still cite the rule their directory name declares;
what changes is that the report now names this defect too, which is true of
them. No document in any zone changes verdict.

*What this arm does NOT close,* measured the same day and written down so the
silence is not read as coverage: an allocation endpoint that names an
attribute **no declaration carries** is still verifier-clean
(`bronAttribuut: "bestaatniet"` passes, while `ref` faults and clingo reports
`unknown attribute`), and so are empty `bronType` and `doelType` fields
(`ref` and clingo both fault with `missing bronType`). Those are name
resolution against the object-type table — a wider movement than this card's
empty-field class — and they stay **open**.

A hand-broken variant of `corpus/golden/jutterswet-gelijkdeling/ir.json` —
one field emptied, a one-field diff. The shape is schema-valid: the
version's `sourceAttribute` is `["string","null"]` on the wire, which is
precisely why the verifier, not the schema, is the fence.

Falsifiable, clause-exact: neuter `wf034_verdelingen` and this case and its
`targetAttribute` sibling turn clean, the four incidental co-citations above
lose their WF-034 line while staying refused for their own rule, and nothing
else over the four zones moves.

Since L4 an allocation is an `Allocation` with its versions (delta B19), and a
version's fields carry their IR-SPEC names (`sourceType`, `sourceAttribute`,
`targetType`, `targetAttribute`, `method`, `ratioAttribute`,
`maximumAttribute`, `remainderAttribute`, `orderingAttribute`,
`connectingRole`); the report cites the version and names the L4 field. The
measurements above name the fields as they were spelled then (`bronType` →
`sourceType`, `bronAttribuut` → `sourceAttribute`, `doelType` → `targetType`).
`expect.txt` pins the arm.
