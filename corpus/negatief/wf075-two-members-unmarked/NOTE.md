Violates WF-075 (`spec/IR-SPEC.md` ch. 6.1; `spec/RUNTIME-SEMANTICS.md`
ch. 7.9): the group default's `exceptions` holds two exception groups but is
not declared `unordered` — two or more exception groups require the
unordered marking, the root regime (WF-050's declared-unordered rule)
applied at every level.

A minimal hand-built document: a group default whose `exceptions` holds two
exception groups (`grondY` → 10.00, `grondZ` → 20.00) with `unordered`
false.
