# wf037-gevuldetelling-elementtype-lijstliteraal

Violates WF-037 (`spec/IR-SPEC.md` ch. 4.3) in its arm for a fold that
names its population and hands no source (the card
aantal-bronargument-welgevormd): this `filledCount` carries the element
type `Vondst`, the range `vondstaanmelding`/`vondst`, and as its one
argument a `ListLiteral` (ch. 4.8) of two unitless decimals, 1.00 and
2.00: a list of values, and no source of instances.

    WF-037 FAIL /rules/rules/0/versions/0/value: a filledCount
    aggregation carries elementType "Vondst" and a ListLiteral operand: a
    listed value is no source of instances

The document. A `Jutter` whose `aantal gevulde vondstgewichten` (whole,
non-negative, unitless) is the target; a `Vondst` with a kg `gewicht`; the
fact type `vondstaanmelding` (role `aanmelder` filled by `Jutter`, role
`vondst` by `Vondst`, many). The range is written because the loader
demands it of a fold with one argument and an `elementType` (since L2);
without it the document does not load.

Before the arm the verifier already refused the node, on the filled
count's set-source arm:

    WF-037 FAIL /rules/rules/0/versions/0/value: the filled count's single
    argument MUST be its set source, an attribute reference read on each
    contributor: this kind reads a VALUE per contributor and tests that
    value for presence (ch. 4.3)

The verdict was right. The arm gives the list literal the sentence the
count carries for the same argument (`wf037-aantal-elementtype-lijstliteraal`),
so the two counts refuse it in one sentence; the set-source arm keeps every
other source that is no attribute reference (`wf037-aantalgevulde-niet-setbron`,
a literal).

Measured before the arm on this document, with a case outside the corpus
(the population of golden `jutterswet-vondsttelling`: `J1` reaches `V1`,
`V2` and `V3`, `J2` nothing, `J3` `V4`):

    verifier    : WF-037, the set-source arm above, exit 1
    ref         : EVALUATION FAULT: unsupported instance-fold source shape
    ASP backend : TRANSLATION FAULT: expression kind not translated (no
                  corpus case exercises it)
    Drools      : EXCLUDE loader: form not carried by the v0 Drools
                  backend: Expr:ListLiteral (unknown expression node), in
                  both branches

Both OCaml refusals were bottom sentences of `scripts/check-rood.sh`: they
name the position and not the construct.

Rood-eerst, measured on this document. Verifier before the arm (source
`spec/verifier/lib/checks.ml` md5 `2e9dd78483864a00da56bd327406f635`,
binary md5 `e0bafac419b97e989317b13194932efe`, built with `--root
spec/verifier` as gate 3 builds it): the set-source line above, exit 1;
gate 3 reports the case rejected for WF-037 `but not on the arm` its
`expect.txt` names.
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
