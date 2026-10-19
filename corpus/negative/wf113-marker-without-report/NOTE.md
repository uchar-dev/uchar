Violates WF-113 (b) (`spec/IR-SPEC.md` ch. 2): an `Unresolved` marker that no
diagnostic accounts for — the document carries a marker and an empty
`diagnostics` list.

    WF-113 FAIL /rules/rules/0/versions/0/condition: Unresolved marker
    'de verjaringsgrens' is not accounted for: no diagnostic message contains
    its source snippet verbatim

A minimal hand-broken variant of `corpus/golden/jutterswet-vondstverjaring/ir.json`:
the right-hand operand of the rule's comparison, the date literal
`31-01-2024`, is replaced by the EXPRESSION marker
`{"node": "Unresolved", "type": {"node": "Date", "precision": "day"},
"source": {"snippet": "de verjaringsgrens"}}` — a name the model does not
declare, which is how a resolver hands the wire an operand it could not
resolve — and the comparison's own source snippet follows it.
`diagnostics` stays empty. The marker itself is legal wire content (WF-044 is
behavioural, see `corpus/red/wf044-unresolved-*`); what this rule refuses is a
marker nothing reports. The marker sits INSIDE a condition, so this witness
also pins that the verifier's marker walk descends from a condition into its
operands. The condition-marker variant is witnessed by
`wf113-report-names-marker-negated`.

Since L5 the marker is an `Unresolved` and its text sits in `source.snippet`
(delta B26), which the rule reads; `expect.txt` pins the arm.
