Violates WF-067 (`spec/IR-SPEC.md` ch. 2.11): `even` invokes `oneven` and
`oneven` invokes `even` — a cycle in the invocation graph. Function
definitions MUST have an acyclic invocation graph (no recursion: the Catala
mirror, and what keeps termination and stratification trivial).

A minimal hand-built document.
