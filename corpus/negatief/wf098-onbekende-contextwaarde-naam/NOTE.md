Violates WF-098 (`spec/IR-SPEC.md` ch. 4.1): a **context value** whose `name` is
not one of the two admissible names — `de dagX` where the closed set holds
`evaluationDate` and `evaluationYear` (until L4 it held `de dag` as well; see
the last paragraph).

    WF-098 FAIL /rules/dayKindDefinitions/0/condition/parts/0/left/name: context
    value 'de dagX' is not one of the two admissible names evaluationDate,
    evaluationYear: the context-value name is a closed set, spelled byte for
    byte

**Why this is an enumeration and not a resolution.** Nothing in the document
declares a context value: there is no table for the name to resolve against, so
this is not WF-034's class. The admissible names are fixed by the spec, which
makes the clause an opsomming — and writing one is a scope decision about what
v1 admits, not an arm that consults a declaration the document carries.

**Observability before the rule: none.** Measured 2026-09-10 on the state that
predates this card, one field of
`corpus/golden/jutterswet-schouwrooster/ir.json` twisted:

    verifier  PASS
    ref       oracle: PASS (7 expectations) / PASS (3 expectations)
    clingo    clingo: PASS (7 expectations) / PASS (3 expectations)

— byte for byte what the untouched golden yields. The reason is that no
executor evaluates a day-kind definition today: `ref` and the ASP backend both
refuse day-kind durations outright, and the Drools backend's `de dag` reader is
reached only from the two duration forms (§6.10/§6.11), which this document does
not carry. So the name has stopped meaning anything and not one consumer
notices. That is what makes this a wire arm rather than a differential finding.

**The document keeps its three untwisted occurrences on purpose.** Only the
first of the golden's four `de dag` occurrences is twisted; the other three
are the bound day, a `BoundDay` node since L4, and the report shows exactly one
line, at the twisted position. The witness therefore also pins that the check
is per-node, not per-document.

**Reach, measured 2026-09-10.** Every one of the **14** `Constante` nodes the
four zones plus the parity zone hold was twisted, one at a time, and the check
fires at each of the 14 with the position in the message — inside a day-kind
definition's flat composite, inside a nested composite, as a function argument,
as the date index of a temporal read and inside a `Let` body. The rule reaches
every position the corpus carries.

A hand-broken variant of `corpus/golden/jutterswet-schouwrooster/ir.json`: one
field, `"naam": "de dag"` → `"naam": "de dagX"` at the first of four
occurrences, a one-line diff on the v0 wire; since L4 the twisted node is
`{"node": "ContextValue", "name": "de dagX", …}`, schema-valid, since the schema
leaves a context value's `name` a free string for this rule to close.

Falsifiable, arm-exact: replace the `| Constante { naam; _ } -> (…)` branch of
`walk_expr` in `spec/verifier/lib/checks.ml` with `| Constante _ -> []` and this
case turns clean together with its one sister — 580 documents compared over the
four zones plus the parity zone, exactly two verdicts moved, not one report line
elsewhere. Dropping only the byte-exactness (relaxing
`List.mem n [ … ]` to a `norm` comparison) frees the sister and leaves this case
refused, so the two witnesses fall independently.

Since L4 the node is a `ContextValue` (delta B23), its name is written in
`name`, and the closed set holds two names, since L5 spelled `evaluationDate`
and `evaluationYear` (delta B6): the
bound day of a day-kind definition is its own node, `BoundDay`, and a context
value named `de dag` falls outside the set like any other name (witness
`wf098-de-dag-als-contextwaarde`). A context value without a name cannot be
written since L4. `expect.txt` pins the arm.
