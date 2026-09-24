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

    WF-088 FAIL /rules/consistenties/0/versions/0/assertion/rechts/type: enumeration
    use site disagrees with the declaration of domain 'keurstatus' (the same
    values, in declaration order, required)

**Migrated at L2 (2026-09-24), stamp: herschreven (m).** The document is the
migration's translation (`spec/proposals/v1-migratie/m2.py`): the references
lost their own `type` (B13); the drift sits on a `Literal`'s `type`, which
stays (V9 (a)). The rule still fires on the same arm, and `expect.txt` pins
it.
