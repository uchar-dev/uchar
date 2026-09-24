# Violates WF-082 — empty statuteAnnotations list

The rule carries `"statuteAnnotations": []`: the statute-annotation extension
field written as an EMPTY list. The field is written whole or not at all —
a rule without annotations omits it entirely, so an empty list is a trace
of absence and the document is ill-formed. The same rule rejects a blank
or whitespace-padded entry.

Minimal own-authored document (Jutterswet vocabulary); the well-formed
counterpart is `corpus/golden/jutterswet-hardheidsoverschot/ir.json`, whose rules
carry one and two annotations and whose third rule omits the field.

Since L4 the statute annotations sit on the form as `statuteAnnotations`
(delta B19), one list for all its versions, carried verbatim from the v0
field; the report cites that field of the form and names it. `expect.txt` pins
the arm.
