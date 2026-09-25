Violates WF-098 (`spec/IR-SPEC.md` ch. 4.1): a `ContextValue` named
`Rekendatum`, the v0 spelling of `evaluationDate`.

    WF-098 FAIL /rules/rules/0/versions/0/value/date/name: context value
    'Rekendatum' is not one of the two admissible names evaluationDate,
    evaluationYear: the context-value name is a closed set, spelled byte for
    byte

**Why the v0 spelling is no context value since L5.** The landing L5 spelled
every enumeration value of the wire in English lowerCamel (`spec/IR-SPEC.md`,
Conventions), and the closed set of context-value names with it: the two
session values are `evaluationDate` and `evaluationYear`. The name stays a
free string on the wire and in memory, carried verbatim, so the v0 spelling
`Rekendatum` is a name outside the set like any other — as `de dag` has been
since L4 (`wf098-de-dag-als-contextwaarde`). No reader maps it onto
`evaluationDate`: a second spelling of one name would be a silent fallback.

**Every executor refuses it at load.** The shared OCaml loader, when an
executor loads the document, refuses the node with this same sentence; the
verifier's loader loads it so that the rule can cite it. The Drools loader
stops earlier on this document, at the `TemporalRead` whose `date` the node
is, a form it does not carry (`Expr:TemporalRead`); where it reaches a
`ContextValue` named `Rekendatum` in a form it does carry, it refuses the
name with this same sentence.

**The document.** `corpus/golden/anchor/jutterswet-penningvintage/ir.json`
with the name of its one `ContextValue` changed from `evaluationDate` to
`Rekendatum`; type and source record unchanged. Its sister
`wf098-contextwaarde-naam-kapitalisatie` carries `EvaluationDate`, the same
name recapitalised, and pins the byte-exact comparison; this witness pins
that the v0 spelling left the set.

Falsifiable, arm-exact: adding `Rekendatum` back to the admissible names
(`context_value_names` in `spec/verifier/lib/checks.ml`) turns this case clean
and nothing else — 707 documents compared over golden, rood and negatief, one
verdict moved (measured at the L5 landing).
