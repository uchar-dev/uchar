Violates WF-077 (`spec/IR-SPEC.md` ch. 4.10): the `IfThenElse` condition reads
a struct field (`positie.bedrag`) directly as an operand. A field read, like an
attribute or characteristic read, must pass through a `Let` binding
first; a condition may READ only binders, parameters and literals.

A minimal hand-built document.
