# wf037-som-elementtype-zonder-operand

Violates WF-037 (`spec/IR-SPEC.md` ch. 4.3) in its arm for a fold that
names its population and hands no source (the card
aantal-bronargument-welgevormd), under a VALUE fold: this `sum` carries the
element type `Vondst` and no argument at all.

The arm is not the counts' alone. WF-037's clause that defines the listed
value fold (`value fold with fewer than two operands`) reads only folds
WITHOUT an element type, and the fold-source rules (WF-083, WF-093) read
only a fold with exactly ONE argument; a value fold with an element type
and no argument fell between the two. Measured before the arm, all six
value folds — `sum`, `total`, `minimum`, `maximum`, `first`, `last` — were
verifier-clean in this shape, and every executor refused each of them:
`ref` and the ASP backend with the two sentences below, both Drools
branches with a transpiler SKIP and a MARKER that names the kind. This
document witnesses the six with one kind, as `wf037-eerste-een-operand`
witnesses the listed-form clause for all of them.

The document. A `Jutter` whose `aangemeld gewicht` (kg, two decimals) is
the target; a `Vondst` with a kg `gewicht`; the fact type
`vondstaanmelding` (role `aanmelder` filled by `Jutter`, role `vondst` by
`Vondst`, many). The `sum` carries `elementType: "Vondst"`, an empty
argument list, no range and no `emptyFallback`; its node type is the
target's. The missing operand is the only defect.

Why the form leaves the wire. It was verifier-clean and no executor ran
it. Measured before the arm on this document, with a case outside the
corpus (the population of golden `jutterswet-vondsttelling`: `J1` reaches
`V1`, `V2` and `V3`, `J2` nothing, `J3` `V4`):

    verifier    : PASS
    ref         : EVALUATION FAULT: unsupported instance-fold source shape
    ASP backend : TRANSLATION FAULT: expression kind not translated (no
                  corpus case exercises it)
    Drools      : EXCLUDE transpiler SKIP (aggregatie buiten Phase-C
                  scope); DRL-text branch EXCLUDE drl-transpiler MARKER
                  (TODO aggregatie SOM)

Both OCaml refusals were bottom sentences of `scripts/check-rood.sh`: they
name the position and not the construct.

Rood-eerst, measured on this document. Verifier before the arm (source
`spec/verifier/lib/checks.ml` md5 `2e9dd78483864a00da56bd327406f635`,
binary md5 `e0bafac419b97e989317b13194932efe`, built with `--root
spec/verifier` as gate 3 builds it): `verifier: PASS`, exit 0.

`expect.txt` pins the arm.
