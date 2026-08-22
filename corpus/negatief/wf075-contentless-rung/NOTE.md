Violates WF-075 (`spec/IR-SPEC.md` ch. 6.1; `spec/RUNTIME-SEMANTICS.md`
ch. 7.9): a rung carries neither its own decision/consequence nor a
nested exception sub-group (and is not the initialisation rung) — a
contentless rung decides nothing. Every rung must be a plain rung (own
content), a group-rung (`excepties`), or the initialisation rung.

A minimal hand-built document: a group-rung whose single member chain
holds one rung that carries only a justification (`grondX`) and no
consequence, besluit, or sub-group.
