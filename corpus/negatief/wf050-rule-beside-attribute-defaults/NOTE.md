Violates WF-050 (`spec/IR-SPEC.md`): a plain GELIJKSTELLING rule
writes `Jutter.waardering` while an attribute ladder owns that
target — an unordered plain writer would silently defeat the ladder's
decisions (the ch. 7.1 single-writer lift, attribute side).

A minimal hand-built document on the shape of
`corpus/golden/anchor/jutterswet-vondstwaardering/ir.json`.
