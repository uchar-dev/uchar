Violates WF-061 (`spec/IR-SPEC.md` ch. 2.4): the `beschikking` attribute
is a `SumType` use site naming domain `beschikking`, but `domeinen`
declares no such domain. A use site MUST name a *declared* sum-type
domain — the nominal source of truth is the declaration in `domeinen`,
never the inline (here orphaned) constructor copy.

A minimal hand-built document.
