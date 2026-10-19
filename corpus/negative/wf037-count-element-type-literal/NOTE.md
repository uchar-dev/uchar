# wf037-count-element-type-literal

Violates WF-037 (`spec/IR-SPEC.md` ch. 4.3) in its fold-source arm (the card
aantal-bronarm-en-drools-lader): an aggregation that carries `elementType`
takes one argument, its fold source, and that source is an
`AttributeReference` — under `count`, beside the range, the written name of
the counted role. This `count` carries the element type `Vondst`, the range
`vondstaanmelding`/`vondst`, and as its one argument a `Literal`, the
unitless decimal 2.00: a value, and no source of instances.

    WF-037 FAIL /rules/rules/0/versions/0/value: a count aggregation
    carries elementType "Vondst" and a Literal operand: the source of an
    instance fold is an AttributeReference

The document. A `Jutter` whose `aantal aangemelde vondsten` (whole,
non-negative, unitless) is the target; a `Vondst` with a kg `gewicht`; the
fact type `vondstaanmelding` (role `aanmelder` filled by `Jutter`, role
`vondst` by `Vondst`, many). The range is written because the loader
demands it of a fold with one argument and an `elementType`; without it
the document does not load. The binding itself is sound: it relates the
fold subject `Jutter` to the element type `Vondst`. The argument is the
only defect.

Before the arm the verifier refused the node on WF-047 alone, a sentence
that names the binding and not the argument:

    WF-047 FAIL /rules/rules/0/versions/0/value/sourceRole: a fold-source
    role binding on a node that is not an instance fold over a relation

Since the arm WF-037 carries the shape and WF-047 steps aside on it.

Measured before the arm on this document, with a case outside the corpus
(the population of golden `jutterswet-vondsttelling`: `J1` reaches `V1`,
`V2` and `V3`, `J2` nothing, `J3` `V4`; expected counts 3, 0 and 1):

    verifier    : WF-047 (above), exit 1
    ref         : EVALUATION FAULT: unsupported instance-fold source shape
    ASP backend : TRANSLATION FAULT: expression kind not translated (no
                  corpus case exercises it)
    Drools      : PLAYED 1 in both branches — it counted the instances the
                  range reaches, 3, 0 and 1, and never read the argument

The two OCaml refusals were bottom sentences of `scripts/check-red.sh`,
which name the position and not the construct, and the Drools backend
answered a document the verifier refuses. Since the arm `ref` and the ASP
backend refuse the form in the verifier's sentence from named arms
(`ref/test/test_ref.ml` and `backends/clingo/test/test_clingo.ml` pin
them), and the Drools loader refuses it at load in the same sentence, in
both branches.

Rood-eerst, measured on this document. Verifier before the arm (source
`spec/verifier/lib/checks.ml` md5 `69d21de724efab29098b02855809413c`,
binary md5 `7816a181b61c7e337e9f2b045877f7b6`, built with `--root
spec/verifier` as gate 3 builds it): the WF-047 line above and no WF-037
line, exit 1; gate 3 reported the case `rejected, but not for WF-037`.
With the arm (source md5 `b48d9e30c9a1443ea163526f78cb8e7e`, with
`spec/verifier/lib/spelling.ml` md5 `4e5d068ce55d62129d6db20d7f644a14`,
binary md5 `d4c902a9e3aac73f4e791489cce7853a`): a single WF-037
rejection, exit 1, whose sentence then ended "over its range" (see the
widening below).

Falsifiable, arm-exact. Swept over golden + negative + red + the parity
zone (762 documents) with the verifier before the arm, the verifier with
it and six mutants of it, each built with `--root` as gate 3 builds it:

- with the arm against before it: exactly seven documents move, the six
  `wf037-aantal-elementtype-*` witnesses of this arm (`-literaal`,
  `-binair`, `-parameter`, `-variabele`, `-builtin`, `-onopgelost`) and
  `wf037-filled-count-not-set-source`, whose refusal moves from the filled
  count's set-source sentence to this arm's; no verdict changes, and the
  other 755 keep their reports byte for byte;
- the shape recognition off (source md5
  `6f9d454309a8ee7e0060fffc41e61513`): the six count witnesses fall back
  to the WF-047 line, and the filled count's witness too, because the
  filled count's set-source arm is part of this arm now — so the
  recognition is what refuses the filled count's form under WF-037;
- the arm silent while the rules that step aside for it keep doing so
  (source md5 `a3f07594924afae10d2e434ac95390c0`): all seven witnesses
  pass the verifier;
- WF-047 not stepping aside (source md5
  `4383541c0b89255b383ba44abb3534c8`): the seven witnesses each gain the
  WF-047 line beside this arm's;
- the `ListLiteral` not excluded (source md5
  `daa29cfd213fb4fca0b72107fc17fd95`): the two list-literal witnesses of
  the converse arm (`wf037-count-element-type-list-literal`,
  `wf037-filled-count-element-type-list-literal`) each gain this arm's
  line beside their own;
- the union under a count not excluded (source md5
  `d8d925c35e29c76173921f56c04cbe74`): `wf110-union-beside-fold-source` gains
  this arm's line beside WF-110 (c);
- the arm under `count` alone (source md5
  `239624b7339aef457faa001d72f48d97`): `wf037-filled-count-not-set-source`
  is refused on the WF-047 line instead of WF-037.

Gate 3 sees the first, the second and the last mutant (a witness passes,
or is refused under another rule than its directory declares). The other
three change no verdict and no declared rule, so gate 3 cannot see them;
they are recorded because without the exclusions and the stepping aside
one malformation is reported under two rules or twice under one.

**Widened on the same card to every aggregation kind.** The arm first
spoke under `count` and `filledCount` alone, ending its sentence "over its
range"; measured next, a value fold with an `elementType`, a range and a
`Literal` or a `ParameterReference` was refused on WF-047's binding sentence
alone (both Drools branches: a transpiler SKIP, and in the DRL-text branch a
ClassCastException on `sum`, `total`, `minimum` and `maximum`), and a
`total` without a range over a `Literal` was verifier-clean while no
executor ran it. The arm now reads every kind, its sentence ends "is an
AttributeReference" (a total without a range has no range), and three
witnesses joined it: `wf037-sum-element-type-literal`,
`wf037-sum-element-type-parameter` and `wf037-total-element-type-literal`.
Verifier with the widening: source md5 `1763a8017629c4b3e6a6c0c895d85ac1`,
binary md5 `cc6adade8d60c20bece0bc08cef18658`. Swept against the verifier
before the widening over golden + negative + red + the parity zone (766
documents): exactly ten move — the seven earlier witnesses of this arm by
the shortened sentence alone, and the three new ones — with one verdict
change, `wf037-total-element-type-literal` (passed, now refused); the
other 756 keep their reports byte for byte.

`expect.txt` pins the arm.
