# wf037-aantal-elementtype-zonder-operand

Violates WF-037 (`spec/IR-SPEC.md` ch. 4.3) in its arm for a fold that
names its population and hands no source (the card
aantal-bronargument-welgevormd): an aggregation that carries `elementType`
is an instance fold, and an instance fold reads its contributors from ONE
source argument. This `count` carries the element type `Vondst` and no
argument at all. It is the twin of `wf037-aantal-geen-operand`, the same
empty argument list WITHOUT an element type: there the element type is
missing, here the operand.

The document. A `Jutter` whose `aantal aangemelde vondsten` (whole,
non-negative, unitless) is the target; a `Vondst` with a kg `gewicht`; the
fact type `vondstaanmelding` (role `aanmelder` filled by `Jutter`, role
`vondst` by `Vondst`, many). The `count` carries `elementType: "Vondst"`,
an empty argument list and no range; the node type is the count's own
derived type. The missing operand is the only defect.

Why no range. Since L2 the loader demands a range of a fold with ONE
argument and an `elementType`; a fold with NO argument loads either way.
With `sourceRole` written beside the empty list the verifier refused the
node on WF-047 (`a fold-source role binding on a node that is not an
instance fold over a relation`), a sentence that names the binding and not
the missing operand. This document leaves the range out, so that the
missing operand is its only defect.

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
                  (TODO aggregatie AANTAL)

Both OCaml refusals were bottom sentences of `scripts/check-rood.sh`: they
name the position and not the construct.

Rood-eerst, measured on this document. Verifier before the arm (source
`spec/verifier/lib/checks.ml` md5 `2e9dd78483864a00da56bd327406f635`,
binary md5 `e0bafac419b97e989317b13194932efe`, built with `--root
spec/verifier` as gate 3 builds it): `verifier: PASS`, exit 0.

`expect.txt` pins the arm.
