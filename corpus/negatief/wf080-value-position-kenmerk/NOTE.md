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
Until L2 the reference carried a serialized `type {Booleaan}`, which kept
the assignment kind-clean for WF-023, so the verifier before WF-080 had
nothing else to catch.

Old verifier: `PASS` (the has_attr||has_kenmerk leak admits it). New verifier:
`WF-080 FAIL … 'penninggerechtigd' is a characteristic of 'Jutter', not an
attribute`.

**Since L2 (2026-09-24)** the reference carries no `type` of its own (B13):
its type is its declaration's, and a characteristic declares no datatype, so
the verifier's static type of this read is none at all (`expr_type` goes
through `resolved_attr_datatype`, which resolves attributes only) and WF-023
has no type to cross. The report is the one WF-080 line:

    WF-080 FAIL /rules/resolvedRegels/0/waarde: 'penninggerechtigd' is a
    characteristic of 'Jutter', not an attribute — a characteristic endpoint
    is ill-formed in a value position

On the L2 union `ref` still typed such a read as a boolean; the L2 fix round
gave every reader the verifier's answer (no static type, and a refusal under
WF-080 in a value position).
