Violates WF-113 (a) (`spec/IR-SPEC.md` ch. 2): a `WARNING` whose `line` is -1.

    WF-113 FAIL /diagnostics/0/line: diagnostic with line -1: a line is a source position, and 0 is the value for none (a hand-built document)

A minimal hand-broken variant of `corpus/golden/jutterswet-vondstverjaring/ir.json`:
the golden unchanged, plus one entry with severity `WARNING`, the
decision-table message of `wf113-severity-null`, and `"line": -1`. That `line`
is an integer is the loader's check; that it is at least 0 is this rule's. A
line is a source position, and 0 is the value for none: a hand-built document
has no source, as the two red documents `wf044-onopgelost-guard` and
`wf044-onopgelost-reached` show with `"line": 0`. A negative line names no
position. The schema types `line` as an integer and leaves the bound to this
rule, as it leaves `count` ≥ 1 to WF-005, so that this witness stays a document
the schema admits.
