Violates WF-026 (`spec/IR-SPEC.md` ch. 3.2): two allocations writing the
same (type, attribute) target.

    WF-026 FAIL /rules/verdelingen/0: two allocations write target (Jutter,
    aantal aangemelde vondsten)

**Rewritten at L2 (2026-09-24), stamp: herschreven (i).** As
`wf020-crossform-collision`, for both allocations: they carried no source
type and no recipient role, and since L2 the role is required (B12). Written
by hand with the fact type `strandgoedverdeling`, `Jutter` as source type
and the `ontvanger` role bound on each; the shared target is untouched.
