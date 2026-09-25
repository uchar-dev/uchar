Violates WF-021 (`spec/IR-SPEC.md` ch. 3.1): the validity windows of the
versions of one form MUST be disjoint. Broken from
`corpus/golden/jutterswet-opvolgerversies`: the first version of the object
creation `werf de opvolger` runs to 2026-06-30 instead of 2025-12-31, so it
overlaps the second, which starts on 2026-01-01.

    WF-021 FAIL /rules/objectCreations/0/versions/0: validity windows of
    versions 'werf de opvolger #1' and 'werf de opvolger #2' overlap
    (objectCreations)

The witness of the `objectCreations` list, beside `wf021-version-overlap`
for `rules`. It stands since the WF-052 decision of 2026-09-25: the versions
of one object creation are one writer of the relation it creates, so WF-052
no longer refuses two versions of one creation on a to-one role, and an
overlap between them is WF-021's alone. The verifier before that decision
reported a second line on this document, under WF-052 (`connecting relation
'opvolging' has a to-one created-side role and 2 object-creation writers`).
`expect.txt` pins the arm.
