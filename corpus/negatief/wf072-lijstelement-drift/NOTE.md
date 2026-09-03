Violates WF-072 (`spec/IR-SPEC.md` ch. 2.4): the `dossiers` attribute is a
`ListType` whose ELEMENT is a struct use site of domain `dossier`, and that
element's inline copy declares `telling` as a 2-decimal number where the
declaration declares a whole number. A use site must repeat its declaration
whole at every depth — the `ListType` look-through.

The arm this case pins: before it, `wf072` matched a top-level `StructType`
attribute or parameter datatype only, so a drifted copy one `ListType` deep
was verifier-clean. Measured on such a document (an anchor golden with one
drifted list element): ref faulted at the field read while clingo reported
`PASS (3 expectations)` — a verifier-clean ref-versus-clingo split.

A minimal hand-built document.
