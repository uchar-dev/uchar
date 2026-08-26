Violates WF-034 (`spec/IR-SPEC.md` ch. 4.3): the fold-source argument
writes `pupillen` — a synthesized plural that resolves to no declared
role — so the narrowed type-pair fallback must derive the source. The
only fact type connecting `Jutter` to `Jutter` is the **self-typed**
`mentorschap`, whose two roles (`mentor`, `leerling`) are BOTH
candidates: the connecting role is not unique. The fallback counts
candidate ROLES, not fact types (v0's `verbindendeRol`), so a single
self-typed two-role fact type offers two candidates and blocks the
derivation — exactly the block the executors' `connecting_role` and the
ch. 4.1/5.5 type-pair derivation carry.

Without the role-count, a fact-type count would see ONE connecting fact
type and silently resolve to one of the two roles — a silent divergence
from the pinned runtime, which derives null (ambiguous) there. This
document pins the verifier arm of that fix: ref faults
`the connecting role from "Jutter" to "Jutter" is ambiguous
(2 candidate roles)`, and the verifier lifts the same block to
well-formedness.

A minimal hand-built document on the shape of
`corpus/golden/jutterswet-kaveltelling/ir.json` (the accepting side, a
unique connecting fact type over a distinct type pair) — here the type
pair is self-typed, so the derivation blocks.
