Violates WF-080 (`spec/IR-SPEC.md` ch. 4.1): a value-position `AttribuutRef`
resolves to a **characteristic** endpoint, not an attribute. The rule
`markering van een jutter` equates the `Booleaan` attribute `markering` to
`de penninggerechtigd van de Jutter` — but `penninggerechtigd` is a declared
kenmerk of `Jutter`, not an attribute, so the read carries no attribute value.

Pins the closed `has_attr || has_kenmerk` leak of the kenmerk exploration.
The value-position member check accepted either an attribute or a
characteristic endpoint; a characteristic is read through a characteristic
check (ch. 4.5, a `KenmerkCheck` condition), never an `AttribuutRef` in a
value slot. WF-080 fires only in value positions (the rule value, a member
value, a condition operand, a default consequence) — a rule TARGET lvalue is
WF-022's job and is not walked, so `KENMERKTOEKENNING` targets are unaffected.
The serialized `type {Booleaan}` keeps the assignment kind-clean for WF-023,
so the old verifier had nothing else to catch.

Old verifier: `PASS` (the has_attr||has_kenmerk leak admits it). New verifier:
`WF-080 FAIL … 'penninggerechtigd' is a characteristic of 'Jutter', not an
attribute`.
