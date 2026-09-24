Violates WF-098 (`spec/IR-SPEC.md` ch. 4.1): a `ContextValue` named `de dag`.

    WF-098 FAIL
    /rules/resolvedDagsoorten/0/voorwaarde/onderdelen/0/links/arguments/0/name:
    context value 'de dag' is not one of the two admissible names Rekendatum,
    Rekenjaar: the context-value name is a closed set, spelled byte for byte

**Why `de dag` is no context value since L4.** Until L4 one node carried the
two session values and the bound day of a day-kind definition, told apart by
name. Since L4 the bound day is a node of its own, `BoundDay`, which carries no
name (delta B23), and a `ContextValue` is one of the two session values,
`Rekendatum` or `Rekenjaar`. A `ContextValue` named `de dag` is therefore a
name outside the closed set like any other; no reader maps it onto the bound
day.

**Every executor refuses it at load.** The shared OCaml loader, when an
executor loads the document, refuses the node with this same sentence; the
verifier's loader loads it so that the rule can cite it. The day-kind position
does not add a WF-031 report: that rule's session-value arm names the two
admissible names and nothing else.

**The document.** `corpus/golden/jutterswet-bergingsklok/ir.json` with the
first `BoundDay` of its day-kind definition replaced by
`{"node": "ContextValue", "name": "de dag", …}` with the same type and `bron`.

Falsifiable, arm-exact: adding `de dag` back to the admissible names
(`context_value_names` in `spec/verifier/lib/checks.ml`) turns this case clean
and nothing else — 698 documents compared over golden, rood and negatief, one
verdict moved.
