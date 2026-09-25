# wf037-som-elementtype-zonder-operand

Violates WF-037 (`spec/IR-SPEC.md` ch. 4.3) in its arm for a fold that
names its population and hands no source (the card
aantal-bronargument-welgevormd), under a VALUE fold: this `sum` carries the
element type `Vondst` and no argument at all.

    WF-037 FAIL /rules/rules/0/versions/0/value: a sum aggregation
    carries elementType "Vondst" but no source operand: an instance fold
    ranges over a written source

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
name the position and not the construct. Since the arm `ref` and the ASP
backend refuse the form in the verifier's sentence, each from a named arm
beside its residual one (`ref/test/test_ref.ml` pins `ref`'s, lines
`WF-037-elementtype-*`); both Drools branches keep the refusals above.

Rood-eerst, measured on this document. Verifier before the arm (source
`spec/verifier/lib/checks.ml` md5 `2e9dd78483864a00da56bd327406f635`,
binary md5 `e0bafac419b97e989317b13194932efe`, built with `--root
spec/verifier` as gate 3 builds it): `verifier: PASS`, exit 0.
With the arm (source md5 `389773713ef066c7ccbf5ba414db0758`, binary md5
`6f7eebf248dbdefcc0ed66c291ecf56f`): the single rejection above, exit 1.

Falsifiable, arm-exact. Swept over golden + rood + negatief + parity (742
documents) with the verifier before the arm, the verifier with it, and
four mutants of it, each built with `--root` as gate 3 builds it:

- with the arm against before it: exactly five documents move, the five
  witnesses of this arm (`wf037-aantal-elementtype-zonder-operand`,
  `wf037-aantal-elementtype-lijstliteraal`,
  `wf037-gevuldetelling-elementtype-zonder-operand`,
  `wf037-gevuldetelling-elementtype-lijstliteraal`,
  `wf037-som-elementtype-zonder-operand`); the other 737 keep their
  reports byte for byte;
- the shape recognition off (its two cases guarded `when false`; source
  md5 `cfac5e9579041af9d23c0ca1c6016c91`): the reports equal those of the
  verifier before the arm on all 742 documents;
- the arm silent while the rules that step aside for it keep doing so
  (source md5 `10f3ecc1fd743357bfc06198daf888e6`): all five witnesses pass
  the verifier, three of which the verifier before the arm refused (the
  list-literal count on WF-047, the two filled counts on their own arms) —
  the stepping aside is sound only together with the arm;
- WF-047 not stepping aside (source md5
  `894a0be580eb7ca4ba8ac0300397ed5b`): only
  `wf037-aantal-elementtype-lijstliteraal` moves, and it gains the WF-047
  line beside this arm's;
- the filled count's arity and set-source arms not stepping aside (source
  md5 `d7f95850435963dbb616347a6c92a021`): only the two
  `wf037-gevuldetelling-*` witnesses move, each gaining the line of the arm
  that refused it before.

The last two mutants change no verdict, so gate 3 cannot see them; they
are recorded because without the stepping aside one malformation is
reported under two rules.

`expect.txt` pins the arm.
