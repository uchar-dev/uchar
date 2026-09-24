Violates WF-021 (`spec/IR-SPEC.md`): Two versions of one rule whose validity windows both cover every date.

A minimal hand-broken variant of `corpus/golden/jutterswet-penningdrempel/ir.json`.

Since L4 a named form writes its versions as a list under one name (delta
B19): this document is one `Rule` with two versions whose `validity` windows
overlap, and the report cites the pointer of the first version. The in-memory
records name the two versions `<name> #1` and `<name> #2`, the spelling the
message quotes. `expect.txt` pins the arm.
