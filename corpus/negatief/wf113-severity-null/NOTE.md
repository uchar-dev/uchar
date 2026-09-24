Violates WF-113 (a) (`spec/IR-SPEC.md` ch. 2): a diagnostic whose `severity` is
null.

    WF-113 FAIL /diagnostics/0/severity: diagnostic without a severity: the one wire severity is warning

A minimal hand-broken variant of `corpus/golden/jutterswet-vondstverjaring/ir.json`:
the golden unchanged, plus one entry in `diagnostics` with `"severity": null`,
line 5, and a message in the form the RegelSpraak frontend writes for a
decision table with only a header row (the form of `jutterswet-kaletabel`'s
`WARNING`). The table it names is not in the golden's source; the message's
content does not matter to clause (a), only the missing severity does.
`spec/schema/uchar-ir-v1.schema.json` admits only `WARNING` and refuses the
null; the verifier's loader reads null, `ERROR` and `INFO`, so that this rule
gives the reason instead of a load failure that names none. The two named
severities are witnessed by `wf113-severity-error` and `wf113-severity-info`.
