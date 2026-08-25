Violates WF-034 (`spec/IR-SPEC.md` ch. 4.3/4.6): the fold-source
argument writes `duinkavels` — a synthesized plural; the role is
declared `duinkavel` with no plural — so the name resolves to no
declared role, and the narrowed type-pair fallback cannot stand in:
**two** fact types (`kaveltoewijzing` and `kavelpacht`) connect
`Jutter` to `Duinkavel`, so no unique connecting relation derives the
source. Zero or multiple connecting fact types keep the loud
rejection — the fallback never guesses.

The bindingless fold is also WF-048-ill-formed for the same
ambiguity; the WF-034 citation is the one this directory declares.

A minimal hand-built document on the shape of
`corpus/golden/jutterswet-kaveltelling/ir.json`, which witnesses the
accepting side (a unique connecting fact type).
