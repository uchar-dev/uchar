# wf037-gevuldetelling-elementtype-zonder-operand

Violates WF-037 (`spec/IR-SPEC.md` ch. 4.3) in its arm for a fold that
names its population and hands no source (the card
aantal-bronargument-welgevormd): this `filledCount` carries the element
type `Vondst` and no argument at all. The filled count reads a value on
each contributor; without an argument it has nothing to read.

    WF-037 FAIL /rules/rules/0/versions/0/value: a filledCount
    aggregation carries elementType "Vondst" but no source operand: an
    instance fold ranges over a written source

The document. A `Jutter` whose `aantal gevulde vondstgewichten` (whole,
non-negative, unitless) is the target; a `Vondst` with a kg `gewicht`; the
fact type `vondstaanmelding` (role `aanmelder` filled by `Jutter`, role
`vondst` by `Vondst`, many). The `filledCount` carries
`elementType: "Vondst"`, an empty argument list and no range. The missing
operand is the only defect.

Before the arm the verifier already refused the node, on the filled
count's arity arm:

    WF-037 FAIL /rules/rules/0/versions/0/value: the filled count takes
    exactly ONE argument, its set source, and this node carries 0: there
    is no listed form of this kind — the source language spells it
    nowhere, so there is no articulated operand list to admit (ch. 4.3)

The verdict was right and its ground was not: an empty argument list is no
listed form, and "no articulated operand list to admit" is the ground for
two or more operands (`wf037-aantalgevulde-tweede-argument`). Since the arm
the arity arm keeps that spelling and steps aside on this one, which
carries the sentence of the count's twin
(`wf037-aantal-elementtype-zonder-operand`).

Measured before the arm on this document, with a case outside the corpus
(the population of golden `jutterswet-vondsttelling`: `J1` reaches `V1`,
`V2` and `V3`, `J2` nothing, `J3` `V4`):

    verifier    : WF-037, the arity arm above, exit 1
    ref         : EVALUATION FAULT: unsupported instance-fold source shape
    ASP backend : TRANSLATION FAULT: expression kind not translated (no
                  corpus case exercises it)
    Drools      : EXCLUDE transpiler SKIP (aggregatie buiten Phase-C
                  scope); DRL-text branch EXCLUDE drl-transpiler MARKER
                  (TODO aggregatie AANTAL_GEVULDE)

Both OCaml refusals were bottom sentences of `scripts/check-rood.sh`: they
name the position and not the construct. Since the arm `ref` and the ASP
backend refuse the form in the verifier's sentence, each from a named arm
beside its residual one (`ref/test/test_ref.ml` pins `ref`'s, lines
`WF-037-elementtype-*`). Since the card aantal-bronarm-en-drools-lader
the Drools loader refuses the form too, at load and in the verifier's
sentence, so neither branch reaches the refusals above
(`backends/drools/src/test/java/dev/uchar/regelspraak/loader/TellingBronarmWitnessTest.java`).

Rood-eerst, measured on this document. Verifier before the arm (source
`spec/verifier/lib/checks.ml` md5 `2e9dd78483864a00da56bd327406f635`,
binary md5 `e0bafac419b97e989317b13194932efe`, built with `--root
spec/verifier` as gate 3 builds it): the arity line above, exit 1; gate 3
reports the case rejected for WF-037 `but not on the arm` its `expect.txt`
names.
With the arm (source md5 `389773713ef066c7ccbf5ba414db0758`, binary md5
`6f7eebf248dbdefcc0ed66c291ecf56f`): the single rejection above, exit 1.

Falsifiable, arm-exact: the sweep over golden, rood, negatief and the parity
zone with the verifier before the arm, the verifier with it and four
mutants of it covers the five witnesses of the arm together, this one
included, and is recorded once, in
`corpus/negatief/wf037-aantal-elementtype-zonder-operand/NOTE.md`.

`expect.txt` pins the arm.
