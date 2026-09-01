Violates WF-089 (`spec/IR-SPEC.md` ch. 2.4): a `BinderRef` carries a
`type` naming the sum domain `oordeel` while the binder it resolves to
reads `beschikking`. The `Let` binds `b` to `de beschikking van de
Jutter` — an `AttribuutRef` whose own `type` faithfully names
`beschikking` — and the body matches on `b` typed `oordeel`, with the
arms covering `oordeel`.

    WF-089 FAIL /resolvedRegels/0/waarde/body/scrutinee/type: binder
    reference 'b' is typed with keuzetype domain 'oordeel', but the
    declaration it reads from names domain 'beschikking'

Why it has to exist: WF-078 crosses a `BinderRef`'s type against its
binder's declared datatype by SOORT only — sum type against sum type
passes — so the domain lie rode through. The binder's declared datatype
is the WF-078 environment's (here the `Let` value's type; for a match arm
its declared payload, for a lambda or `FunctionDef` parameter its declared
type), and WF-089 reads the same slot for the domain. Before the rule:
`verifier: PASS`; ref faulted (`match has no arm for constructor
"Toegekend"`); clingo left `uitkering` underived where 1275.50 was
expected; Drools auto-excluded the `SumType` datatype.

Falsifiable: disable the `wf089_domain_identity` call in the `Binder_ref`
arm and this case passes (WF-078 still agrees on soort) while the
attribute, parameter and cell cases stay red.
