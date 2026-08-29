Violates WF-075 (`spec/IR-SPEC.md` ch. 6.1; `spec/RUNTIME-SEMANTICS.md`
ch. 7.9): a default carries neither its own decision/consequence nor a
nested exception group (and is not the initialisation default) — a
contentless default decides nothing. Every default must be a plain
default (own content), a group default (`excepties`), or the
initialisation default.

A minimal hand-built document: a group default whose single member chain
holds one default that carries only a justification (`grondX`) and no
consequence, besluit, or exception group.
