# wf083-vouwbron-anker-op-tussentype

Violates **WF-083 (c)** (`spec/IR-SPEC.md` ch. 4.3) at the position WF-102
created: the fold source's `objecttype` names an INTERMEDIATE type of the
range instead of the fold's anchor.

    WF-083 FAIL /rules/resolvedRegels/0/versions/0/value/arguments/0: fold-source
    objecttype 'Boot' is not the fold's anchor 'Vondst' (a set source reads each
    member of the elementType): the executors resolve 'gewicht' against the
    anchor, never against this field, so the two spellings name different
    declarations

Before the path there were two kinds of type in this position — the element
type and the fold subject — and (c) crossed against whichever the shape
selected. A `bronPad` introduces a third: the types the walk passes THROUGH.
They are range and never anchor; every member of the folded set is still an
`elementObjecttype` instance and every declaration is still looked up there.
So the clause needs no new arm, but it does need this witness, because the
shape where a plausible-looking wrong name is available did not exist before:
`Boot` is a type the fold demonstrably walks, and it declares no `gewicht` at
all.

One co-firing line, incidental and expected: `Boot` declares no `gewicht`, so
WF-034 reports the unresolvable member at the same pointer. That is the shape
of the defect rather than a second one — an anchor lie at a type the fold
really walks is exactly an attribute read against the wrong declaration — and
gate 3 keys on the directory's own rule, which fires first.

**Migrated at L2 (2026-09-24), stamp: herschreven (m).** The document is the
migration's translation (`spec/proposals/v1-migratie/m2.py`): the set source
lost its own `type` and set reading (B11, B13); it is the set source because
the fold carries a range (D8 (i)), so its anchor is the element type, and
its `objecttype` still names the intermediate type of the path. The rule
still fires on the same arm, and `expect.txt` pins it (U1).

Since L4 the node is an `Aggregation` or a `BuiltinCall` (delta B21), each
with only its own fields — `arguments`, and on an aggregation `elementType`,
`emptyFallback`, `dimensionSelection`, `sourceRole` and `sourcePath`, on a
builtin `granularity`; the report cites and names them. `expect.txt` pins the
arm.
