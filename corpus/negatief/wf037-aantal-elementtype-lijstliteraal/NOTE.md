# wf037-aantal-elementtype-lijstliteraal

Violates WF-037 (`spec/IR-SPEC.md` ch. 4.3) in its arm for a fold that
names its population and hands no source (the card
aantal-bronargument-welgevormd): under a count that carries `elementType`
the single argument is the count source, the written name of the role the
range binds. This `count` carries the element type `Vondst`, the range
`vondstaanmelding`/`vondst`, and as its one argument a `ListLiteral`
(ch. 4.8) of two unitless decimals, 1.00 and 2.00: a list of values, and
no source of instances. It is the twin of `wf037-aantal-lijstliteraal`, the
same argument WITHOUT an element type.

The document. A `Jutter` whose `aantal aangemelde vondsten` (whole,
non-negative, unitless) is the target; a `Vondst` with a kg `gewicht`; the
fact type `vondstaanmelding` (role `aanmelder` filled by `Jutter`, role
`vondst` by `Vondst`, many). The range is written because the loader
demands it of a fold with one argument and an `elementType` (since L2);
without it the document does not load. The binding itself is sound: it
relates the fold subject `Jutter` to the element type `Vondst`.

Before the arm the verifier refused the node on WF-047 alone, a sentence
that names the binding and not the argument:

    WF-047 FAIL /rules/rules/0/versions/0/value/sourceRole: a fold-source
    role binding on a node that is not an instance fold over a relation

Measured before the arm on this document, with a case outside the corpus
(the population of golden `jutterswet-vondsttelling`: `J1` reaches `V1`,
`V2` and `V3`, `J2` nothing, `J3` `V4`):

    verifier    : WF-047 (above), exit 1
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
spec/verifier` as gate 3 builds it): the WF-047 line above and no WF-037
line, exit 1; gate 3 reports the case `rejected, but not for WF-037`.

`expect.txt` pins the arm.
