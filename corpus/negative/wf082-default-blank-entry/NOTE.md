# Violates WF-082 — blank statuteAnnotations entry on a default

The exception default `schorsing art 4bis` carries
`"statuteAnnotations": [""]`: a statute-annotation entry of a Default
(IR-SPEC ch. 6.1) that is the empty string. An entry is a non-blank
citation; a blank entry is a trace of a dropped annotation and the document
is ill-formed. The schema's `minLength: 1` also rejects this form; the
verifier check is the normative one and must fire regardless.

The document is the golden `corpus/golden/jutterswet-tweeledige-grondslag/ir.json`
with that one entry blanked. `expect.txt` pins the arm and its parameter
`kind=default` (the rule arm is `wf082-blank-entry`).
