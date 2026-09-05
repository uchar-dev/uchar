Violates WF-034 (`spec/IR-SPEC.md` ch. 4.6): a **fact creation** (ch. 3.4)
whose intermediate step names an object **type** that is not the type its own
hop reaches.

    WF-034 FAIL /feitCreaties/0/tussenstappen/0/type: the hop over
    'kustligging' reaches 'Post', but the intermediate bean type names
    'Bestaatniet'

The clause is the one described in full in the sibling NOTE
(`wf034-feitcreatie-onbekend-zetveld`). Three of the creation's four v0 bean
fields are role names; this one is not. `tussenstappen[j].type` is the object
type the path stands on after hop *j* — the fact class the bean-model executor
binds a variable of — so it crosses against the hop itself: the **filler of
the role that hop's `rolBinding` binds**, resolved with the very
`resolve_binding` the WF-028 chain walk uses. Here the binding is
`kustligging`/`post`, the bound role `post` is filled by `Post`, and the step
must say `Post`.

That is a sharper cross than "is a declared object type", and it is the right
one: the two facts must agree, and where they do not the document has two
different answers to the same question. The corpus agrees with it 3 out of 3.
Where the binding does not resolve at all, the arm stays silent — that report
belongs to WF-028.

**Measured**, on this document against `corpus/golden/jutterswet-postwacht/case.json`:

    ref       : oracle: PASS (10 expectations)
    clingo    : clingo: PASS (10 expectations)
    bean model: ERROR NullPointerException: Cannot read field "naam" because
                "ot" is null

The bean-model executor looks the name up in the object-type table, gets
nothing back, and dereferences it — the crash is one step further from the
document than the other three in this family, which is exactly why the type
half needed its own arm. The family's full measurement table (14 of 14
positions verifier-clean, both OCaml executors clean on all 14) is in the
sibling NOTE.

*Measured breadth:* zero outside the four `wf034-feitcreatie-onbekend-*`
witnesses; see the sibling NOTE for the four-zone sweep.

A hand-broken variant of `corpus/golden/jutterswet-postwacht/ir.json` — one
field repointed, a one-line diff; schema-valid, since `Tussenstap.type` is
`["string","null"]` on the v0 wire.

Falsifiable, arm-exact: negate the guard of the
`Ok (_, bound, _), Some tn when norm tn <> norm (filler bound)` match in
`wf034_feitcreatie_beanvelden` (`spec/verifier/lib/checks.ml`) and this case,
alone in all four zones, turns clean — 500 documents compared, one verdict
moved, no report elsewhere.
