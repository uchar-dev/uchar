Violates WF-072 (`spec/IR-SPEC.md` ch. 2.4): the struct domain `bundel`
declares a field `kern` of struct domain `dossier`, and that field datatype
carries an inline copy declaring `telling` as a 2-decimal number where the
declaration of `dossier` declares a whole number. A struct field inside
`domains` is a use site of ANOTHER domain's declaration (WF-071 admits
declared struct domains there), so it must repeat it whole.

The arm this case pins: the `domains` position. Unlike WF-088, whose
enumerations WF-060/065/071 fence out of every declaration position, a struct
declaration may nest another struct — and the nested inline copy is what the
executors read (ref's case loader field-wise, the ASP backend positionally).
The attribute `nest` repeats `bundel` faithfully, so the attribute arm stays
silent: only the declaration position fires.

A minimal hand-built document.
