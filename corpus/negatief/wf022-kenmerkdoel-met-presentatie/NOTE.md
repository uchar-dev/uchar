Violates WF-022 (`spec/IR-SPEC.md` ch. 3.1), the member-kind arm in its first
direction: a rule version carries the `presentation` tag `GELIJKSTELLING`, and
its target `penninggerechtigd` is a characteristic of `Jutter`.

    WF-022 FAIL /rules/rules/0/versions/0/target: a version with a
    presentation or with initialisation true writes an attribute, so its target
    MUST name one, and 'penninggerechtigd' is a characteristic of 'Jutter'

**Why the tag decides the member kind.** Since L4 the wire writes no rule kind
(delta B20). The loader reads the kind from the version's fields: a
`presentation` tag makes it a computation (`GELIJKSTELLING` or `BEREKENING`),
`initialisation` true an initialisation, and neither a characteristic
attribution. The first two write an attribute value; the third attributes a
characteristic. The rule crosses that reading against the declaration of the
member the target names, so a version whose fields say "attribute" and whose
target is a characteristic cannot pass.

**The document.** `corpus/golden/jutterswet-penningdrempel/ir.json` — one
rule, one version, a characteristic target — with one field added to the
version, `"presentation": "GELIJKSTELLING"`.

Falsifiable, arm-exact: emptying the arm `false, true when computes` of the
member match in `wf022` of `spec/verifier/lib/checks.ml` turns this case clean
and nothing else — 698 documents compared over golden, rood and negatief, one
verdict moved. The sister witness `wf022-attribuutdoel-zonder-presentatie`
pins the other direction.
