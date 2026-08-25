# Violates WF-082 — empty bronAnnotaties list

The rule carries `"bronAnnotaties": []`: the statute-annotation extension
field written as an EMPTY list. The field is written whole or not at all —
a rule without annotations omits it entirely, so an empty list is a trace
of absence and the document is ill-formed. The same rule rejects a blank
or whitespace-padded entry.

Minimal own-authored document (Jutterswet vocabulary); the well-formed
counterpart is `corpus/golden/jutterswet-hardheid/ir.json`, whose rules
carry one and two annotations and whose third rule omits the field.
