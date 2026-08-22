Violates WF-075 (`spec/IR-SPEC.md` ch. 6.1; `spec/RUNTIME-SEMANTICS.md`
ch. 7.9): a rung inside an exception sub-group carries `initialisatie`
true — the initialisation and input rungs are root-only; a sub-rung may
never be an initialisation rung.

A minimal hand-built document: a form-a rung (`grondX` → 40.00) whose
sub-group holds a single member chain consisting of one initialisation
rung.
