Violates WF-088 (`spec/IR-SPEC.md` ch. 2.4) in the EXPRESSION layer: the
model layer is impeccable — the `keurstatus` attribute repeats the
declaration exactly — but a `Literal` deep in a consistency rule carries
an enumeration `type` with a fourth value `Verweerd`, and takes that
value as its own.

This is the hole the rule was written for, in miniature. WF-039 checks
the literal's label against the type it carries, so `Verweerd` is fine
there; WF-063 checks match arms against the declaration, which never
mentions `Verweerd`. Before WF-088 the document was verifier-clean while
ref faulted at evaluation and clingo derived nothing.

Its sibling `wf088-use-site-value-drift` drifts at the ATTRIBUTE, so a
verifier carrying only WF-088's model-layer arm still rejected that one
and the mutant survived gate 3. This document is what needs the term in
walk_expr's type prologue.

A minimal hand-built document.
