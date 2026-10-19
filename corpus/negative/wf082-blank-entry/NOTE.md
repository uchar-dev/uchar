# Violates WF-082 — blank statuteAnnotations entry

The rule carries `"statuteAnnotations": [""]`: a statute-annotation entry
that is the empty string. An annotation entry must be a non-blank
citation; a blank entry is a trace of a dropped annotation and the
document is ill-formed. The schema's `minLength: 1` also rejects this
form; the verifier check is the normative one and must fire regardless.

Minimal own-authored document (Jutterswet vocabulary); the well-formed
counterpart is `corpus/golden/jutterswet-hardheidsoverschot/ir.json`,
whose rules carry one and two annotations and whose third rule omits
the field.

Since L4 the statute annotations sit on the form as `statuteAnnotations`
(delta B19), one list for all its versions, carried verbatim from the v0
field; the report cites that field of the form and names it. `expect.txt` pins
the arm.
