Violates WF-076 (`spec/IR-SPEC.md` ch. 4.10): the `Let` binder `x` is read
inside its own `value` — a binder is visible only in the `body`, never in the
value that defines it. (The self-reference is also unbound in the value's
scope, so WF-064 fires alongside; the declared rule is WF-076.)

A minimal hand-built document.
