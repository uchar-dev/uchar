Violates WF-075 (`spec/IR-SPEC.md` ch. 6.1; `spec/RUNTIME-SEMANTICS.md`
ch. 7.9): a default inside an exception group carries `initialisatie`
true — the initialisation and input defaults are root-only; a nested
default may never be an initialisation default.

A minimal hand-built document: a form-a default (`grondX` → 40.00) whose
exception group holds a single member chain consisting of one
initialisation default.
