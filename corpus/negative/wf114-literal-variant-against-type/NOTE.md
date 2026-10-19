Violates WF-114 (`spec/IR-SPEC.md` ch. 4.1): a `Literal` whose value variant
is not the one its type names — the value `{"text": "5"}` under the type
`Number` (with the unit `€`).

    WF-114 FAIL /rules/rules/0/versions/0/value/right/value: literal
    value is a text value but its type is a Number: the variant is the one the
    type names

**Why the schema cannot refuse it.** Since L4 a literal's value is typed
(delta B22): one variant of the value union of the case format, beside the
literal's `type`. One datatype union serves every literal, so the schema admits
every variant beside every type; the cross is this rule's. The other literal
rules key on the variant (WF-081 on `number`, WF-084 on `date`, WF-095 on
`dateTime`), so a text variant under a `Number` meets none of them.

**Every executor refuses it at load.** The shared OCaml loader, when an
executor loads the document, refuses the literal with this same sentence, so
no executor reads the text `5` as a number; the verifier's loader loads it so
that the rule can cite it.

**The document.** `corpus/golden/jutterswet-meldloon/ir.json` with one value
changed: `{"number": "5"}` → `{"text": "5"}` at the right operand of the
first rule's value.

Falsifiable, arm-exact: making the WF-114 report in the `Literal` arm of
`walk_expr` (`spec/verifier/lib/checks.ml`) return nothing turns this case and
its sister `wf114-date-variant-against-precision` clean and nothing else — 698
documents compared over golden, red and negative, two verdicts moved.
