# Violates WF-082 — empty statuteAnnotations list on a default

The base default of the group `penningrecht art 3` carries
`"statuteAnnotations": []`: the statute-annotation extension field of a
Default (IR-SPEC ch. 6.1) written as an EMPTY list. As on a rule, the field
is written whole or not at all — a default without annotations omits it, so
an empty list is a trace of absence and the document is ill-formed. The
schema's `minItems: 1` also rejects this form; the verifier check is the
normative one and must fire regardless.

The document is the golden `corpus/golden/jutterswet-tweeledige-grondslag/ir.json`
with that one list emptied; in the golden the base default carries two
annotations, one of which contains "; ". `expect.txt` pins the arm and its
parameter `kind=default`: the report comes from the defaults, not from a
rule (the rule arm is `wf082-empty-source-annotations`).
