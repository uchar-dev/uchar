# Violates WF-082 — whitespace-padded bronAnnotaties entry

The rule carries `"bronAnnotaties": [" art. 3 Jutterswet "]`: a
statute-annotation entry with leading/trailing whitespace. Entries ride
the wire trimmed; a padded entry defeats byte-stable citation matching.
Note the schema does NOT catch this form (`minLength: 1` accepts a
padded string) — the verifier is the sole enforcement of this clause,
which is exactly why this case is pinned.

Minimal own-authored document (Jutterswet vocabulary); the well-formed
counterpart is `corpus/golden/jutterswet-hardheidsoverschot/ir.json`,
whose rules carry one and two annotations and whose third rule omits
the field.

Since L4 the statute annotations sit on the form as `statuteAnnotations`
(delta B19), one list for all its versions, carried verbatim from the v0
field; the report cites that field of the form and names it. `expect.txt` pins
the arm.
