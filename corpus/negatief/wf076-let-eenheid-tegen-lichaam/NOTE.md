Violates WF-091 (`spec/IR-SPEC.md` ch. 4.1) on the `BinderRef` arm, unit
facet. A `Let` binds `b` to `het gewicht van de Jutter` — a `Numeriek`
attribute declared in `kg` — and the body reads `b` with a serialized
`type` in `t` (the same canonical `kg`, factor 1000). The `Let` node type
repeats the body type, so WF-076 is satisfied.

    WF-091 FAIL /rules/resolvedRegels/0/waarde/body/type: unit mismatch: binder
    reference 'b' is typed 't' of system 'Gewicht' (factor 1000 to 'kg'),
    but the declaration it reads from is 'kg' of system 'Gewicht'
    (factor 1 to 'kg')

Why it has to exist beside `wf091-attribuutref-eenheid-converteerbaar`:
the binder arm is the one arm whose soort facet is WF-078's, and WF-078
crossed the soort ALONE — its own comment and the ch. 4.7 prose named
decimals and units as deliberately uncrossed. This case is that sentence's
successor made falsifiable: number equals number, so WF-078 is silent, and
the unit facet has to carry the refusal under WF-091.

Falsifiable, arm-exact: drop the `~soort` argument and this case is
unchanged (its facet is not soort); disable the `Binder_ref` arm's
`use_site_identity` call and this case turns clean together with
`wf078-binderref-type-lie`, `wf089-binderref-sum` and `wf090-binderref`,
while every other arm stays red.
