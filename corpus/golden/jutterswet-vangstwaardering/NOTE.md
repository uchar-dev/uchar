# jutterswet-vangstwaardering — a nested record, and two domain spellings

A `Vangst` builds its `waardering` as a record of the declared struct
domain `waardering` (`toeslag`, and `kern` of the struct domain
`kernwaarde`: `eenheden`, `grondslag`); `heffing` adds the nested
`grondslag` to the `toeslag`, and `grondslagkopie` reads the nested
`grondslag` alone. The two cases pin absolute values, measured with the
reference interpreter and confirmed by the ASP backend.

## The domain name in another case (since 2026-09-25)

Two expression types spell the domain `kernwaarde` as **`KERNWAARDE`**:

- the nested `StructLiteral` that fills the field `kern` in `waardering
  van een vangst` (`/rules/rules/0/versions/0/value/fields/1/value/type`),
  which WF-073 compares with the declared field datatype;
- the inner `FieldAccess` that reads `kern` in `grondslagkopie van een
  vangst` (`/rules/rules/2/versions/0/value/base/type`), which WF-074
  compares with the declared field datatype.

IR-SPEC ch. 2.1 matches a name case-insensitively where a rule compares two
datatypes whole, so the golden is verifier-clean. Making that comparison
byte for byte (`same_datatype` as `a = b` in `spec/verifier/lib/checks.ml`)
turns it red at exactly these two positions, measured:

    WF-073 FAIL /rules/rules/0/versions/0/value: field 'kern' value is
    struct 'KERNWAARDE', but the declared field datatype is struct
    'kernwaarde'
    WF-074 FAIL /rules/rules/2/versions/0/value/base/type: field access type
    struct 'KERNWAARDE' differs from the declared field datatype struct
    'kernwaarde'

`ref` and the ASP backend replay both cases with the pinned values, the
spellings changing nothing; the Drools backend refuses the document at load
in both branches, as before: `EXCLUDE loader: form not carried by the v0
Drools backend: Datatype:StructType (unknown datatype node)`.
