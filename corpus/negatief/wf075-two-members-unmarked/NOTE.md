Violates WF-075 (`spec/IR-SPEC.md` ch. 6.1; `spec/RUNTIME-SEMANTICS.md`
ch. 7.9): the group default's exception group carries two members but is
not declared `ongeordend` — two or more members require the unordered
marking, the root regime (WF-050's declared-unordered rule) applied at
every level.

A minimal hand-built document: a group default whose `excepties` holds two
member chains (`grondY` → 10.00, `grondZ` → 20.00) with `ongeordend`
false.
