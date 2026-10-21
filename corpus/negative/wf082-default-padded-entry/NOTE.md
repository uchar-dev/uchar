# Violates WF-082 — whitespace-padded statuteAnnotations entry on a nested default

The default of the block `vervallen herroeping`, a default inside an
exception group (RUNTIME-SEMANTICS ch. 7.9), carries
`"statuteAnnotations": [" art. 4, derde laag "]`: an entry with leading and
trailing whitespace. Entries ride the wire trimmed. The schema does NOT
catch this form (`minLength: 1` accepts a padded string), so the verifier is
the sole enforcement of this clause; the case also holds that the check
reaches a default at depth, not only the root chain of a group.

The document is the golden `corpus/golden/jutterswet-uitsluiting-rgs/ir.json`
with that one entry padded. `expect.txt` pins the arm and its parameter
`kind=default` (the rule arm is `wf082-padded-entry`).
