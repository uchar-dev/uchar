Violates WF-022 (`spec/IR-SPEC.md` ch. 3.1), the arm on a target name that
decides no member kind: the subject type `Jutter` declares `toelage` twice, once
as an attribute (a whole `Number`) and once as a characteristic, and a rule
version writes `toelage`.

    WF-022 FAIL /rules/rules/0/versions/0/target: target 'toelage'
    names both an attribute and a characteristic of 'Jutter': which member a
    rule writes decides its kind, and this name decides nothing

**Why the name decides nothing.** Since L4 the wire writes no rule kind
(delta B20): a version with a `presentation` tag or with `initialisation`
true writes an attribute, a version with neither attributes a characteristic,
and the target's declaration says which member the name is. A name that is an
attribute and a characteristic of the same type (WF-002 admits the pair, the
two facets are two namespaces) leaves that cross without an answer, so the
rule refuses the target before either kind is read into it. The comparison
uses the verifier's name identity and the subject type with its supertypes.

**The document.** `corpus/golden/anchor/jutterswet-toelagegronden/ir.json`,
the one document in the four zones that declares such a name, and carries no
rule writing it. The witness keeps its model, adds one rule
`toelage van een jutter` (`equation`, value `10`) and drops the six
defaults groups: a rule on a defaults target is WF-050's single-writer lift as
well, so without them the report is the one line above. The anchor is the
arm's golden.

Falsifiable, arm-exact: emptying the arm in `wf022` of
`spec/verifier/lib/checks.ml` (the case `true, true` of the member match)
turns this case clean and nothing else — 698 documents compared over golden,
rood and negatief, one verdict moved.
