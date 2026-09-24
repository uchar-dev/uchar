# wf037-aantal-zonder-elementtype

Violates WF-037 (`spec/IR-SPEC.md` ch. 4.3) in the count's SOURCE arm —
the maintainer decision of 2026-09-23 (option (a) of
`backlog/done/20260920-aantal-zonder-bron.md`): a count is an instance
fold and MUST carry an element type, the same sentence the filled count's
element-type arm states. This is the arity-1 spelling with an ordinary own
read: an `AANTAL` with no `elementType` and ONE argument that is no
fold source.

    WF-037 FAIL /rules/rules/0/versions/0/value: the count is an
    INSTANCE fold and MUST carry an element type: without one its argument is no
    fold source at all — and with no argument there is none — so the count has
    no contributors to count, only its operand, whatever that operand holds (ch.
    4.3)

The document. A `Jutter` with two weighed posts of her own (`gewogen
vangst`, `gewogen bijvangst`, kg with two decimals) and a count-typed
target, `aantal gewogen posten` (whole, non-negative, unitless). The
`AANTAL` carries one argument, the subject's own read of `gewogen vangst`,
and no `elementType`. Every other clause holds: no facet, the node type is
the count's own derived type; the verifier reports one fault.

Why the form left the wire. It was verifier-clean and exactly ONE executor
answered it. Measured before the arm on this shape, with a case in which
`J1` carries `gewogen vangst` = 3.00 and `J2` carries nothing:

    verifier    : PASS
    ref         : J1 1, J2 0   (the FILLED count of one own value)
    ASP backend : TRANSLATION FAULT: expression kind not translated (no
                  corpus case exercises it)
    Drools      : EXCLUDE transpiler SKIP (aggregatie buiten Phase-C
                  scope); DRL-text branch EXCLUDE drl-transpiler MARKER
                  (TODO aggregatie AANTAL)

A producer wrote this shape too: before the frontend's count-source ERROR
(2026-09-23), "het aantal gewogen vangst van de Jutter" emitted exactly it
with `diagnostics: []`.

One answers, the rest refuse, under a well-formed document — the class
`spec/PLAN.md` §6 calls a spec fault. Since the arm the reference
interpreter's value arm under the count, out of reach through any
well-formed document, refuses by name as the second line of defence:
`a count folds INSTANCES and this node hands it 1 value(s) instead …`.

Rood-eerst, measured on this document. Verifier before the arm (source
`spec/verifier/lib/checks.ml` md5 `30105e069c36ed916f1f7bef984bde10`,
binary md5 `ea2811943bebe68c667c88e911728435`, built with `--root
spec/verifier` as gate 3 builds it): `verifier: PASS`, exit 0. With the arm
(source md5 `8ff8078c0a1cb375d8ea232408bfab9b`, binary md5
`4948e9e50f32989525d4410359ec1567`): the single rejection above, exit 1.

Falsifiable, arm-exact: guard the count-source arm off (`when false` on its
refusing case, one line; source md5 `67db51d0ed8694a33013dc4b34f494eb`).
Swept over golden + rood + negatief + parity (746 documents), exactly
three documents move — the three negatiefs of this arm
(`wf037-aantal-zonder-elementtype`, `wf037-aantal-geen-operand`,
`wf037-aantal-lijstliteraal`) — and the mutant's reports are
byte-identical to the pre-arm verifier's on all 746. A second mutant
removes the union exit instead: five goldens turn red
(`jutterswet-keurunie`, `-uniekwantor`, `-uniestratum`,
`-unievoorwaarde`, `-vondstunie`), so the exit is load-bearing and
witnessed by the green corpus.

Since L4 the node is an `Aggregation` or a `BuiltinCall` (delta B21), each
with only its own fields — `arguments`, and on an aggregation `elementType`,
`emptyFallback`, `dimensionSelection`, `sourceRole` and `sourcePath`, on a
builtin `granularity`; the report cites and names them. `expect.txt` pins the
arm.
