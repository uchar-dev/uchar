# wf020-defaultsgroep-duplicaat

Violates **WF-020** (`spec/IR-SPEC.md` ch. 3.1): both `PrioritizedDefaults`
groups are named `uitkering bij betwisting`.

    WF-020 FAIL /rules/prioritizedDefaults/1: duplicate name 'uitkering bij
    betwisting' in prioritizedDefaults

The WITHIN-ARRAY shape, which falls independently of the cross-kind one
(`wf020-defaultsgroep-botst-met-regel`): the check compares within each named
array and across them in two separate arms, so an implementation that adds
the defaults groups to only one of the two passes one case and not the other.

The document is otherwise exactly its golden: two declared-unordered groups
on one target, which ch. 6.1 admits — what it may not do is give them one
name, because a name is an identity and these are two forms.

Falsifiable in one edit: give the second group its own name back.

Minimal hand-broken variant of `corpus/golden/jutterswet-wrakconflict`.
