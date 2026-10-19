Violates WF-063 (`spec/IR-SPEC.md` ch. 4.7): the `Match` over
`beschikking` matches `Afgewezen` in two arms. The arms must cover the
declared constructors exactly — each **once**, none missing, none
duplicated (the compile-time totality of row 36; the `dups_by` branch of
the check). All three declared constructors are present here, so the
sole violation is the duplicate.

A minimal hand-built document.
