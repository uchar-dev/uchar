# wf037-aantal-opgesomd

Violates WF-037 (`spec/IR-SPEC.md` ch. 4.3) in the count's ARITY clause —
the refusal of 2026-09-20, which took the LISTED form of a count off the
wire. This is the form the decision was taken about: a `count` with no
`elementType`, no `sourceRole`, and two ordinary reads as its operand
list.

    WF-037 FAIL /rules/rules/0/versions/0/value: a count folds
    INSTANCES and has no LISTED form, and this node carries 2 operands: the
    source language spells a count over a subject expression alone (ch. 4.3), so
    there is no articulated operand list to admit — the only reading an operand
    list leaves a count is the number of FILLED operands, and that meaning
    belongs to the filled count over its set source, never to this kind

The document. A `Jutter` with two weighed posts of her own — `gewogen
vangst` and `gewogen bijvangst`, both kg with two decimals — and a
count-typed target, `aantal gewogen posten` (whole, non-negative,
unitless). The `count` reads both posts as ordinary own-attribute reads.
Every other clause holds: the node type is the count's own derived type
(WF-091 is silent), no facet is present, and the verifier reports one fault.

Why the form left the wire. It was verifier-clean and exactly ONE executor
answered it. `ref` counted the FILLED operands — precisely the meaning the
set-source decision of 2026-09-19 took off the wire under this same kind —
while the other two refused. Measured on this document, with a case in
which `J1` carries `gewogen vangst` and leaves `gewogen bijvangst` empty:

    verifier    : PASS
    ref         : 1   (it counts the FILLED operands)
    ASP backend : TRANSLATION FAULT: expression kind not translated (no
                  corpus case exercises it)
    Drools      : EXCLUDE transpiler SKIP (aggregatie buiten Phase-C scope);
                  DRL-text branch EXCLUDE drl-transpiler MARKER (TODO
                  aggregatie AANTAL)

One answers, two refuse, under a well-formed document — a spec fault, and
no differential gate could see it because no corpus document carried the
form. The source language has nothing to mirror here: it defines the count
over instances, spells no operand list for it, and its typing document
excludes the value reading.

**A producer wrote it anyway, and that is what makes this rule a cut rather
than a closed door.** The RegelSpraak frontend's grammar carries an operand
list at the count — a widening of the one-operand source production, gone
unregistered until this decision — and its resolver built every child as an
argument. Measured 2026-09-20, before the fence: the sentence "Het aantal
gewogen posten van een Jutter moet berekend worden als het aantal de gewogen
vangst van de Jutter en de gewogen bijvangst van de Jutter" — the source
reading of this document's own rule — emitted an `AANTAL` with two operands
and no `elementObjecttype`, this document's shape, carrying
`diagnostics: []`. The frontend refuses it at the node from
the same day, with a named ERROR that stops the document reaching disk; the
grammar keeps the widening as a registered divergence, because deleting an
alternative re-associates sentences rather than refusing them. The
maintainer refused the wire form rather than normalise one executor's
reading into a second meaning under one kind (2026-09-20, route (a) of
`backlog/done/20260920-aantal-opgesomde-telling.md`). The listed filled
count stays expressible the day someone norms a source spelling for it;
`filledCount` already carries the meaning over a set source.

Rood-eerst, measured on this document. Pre-refusal verifier (binary md5
`c7356ed0f3002e933a550b899e888a1a`, source `spec/verifier/lib/checks.ml`
md5 `b46b9dac7f3efa77eeea3dc85bcdaf33`): `verifier: PASS`. With the arm
(source md5 `a697e2fe0924696b0eee0c73858e73eb`): the single rejection
above.

Falsifiable, arm-exact: disable the count's arity arm in
`spec/verifier/lib/checks.ml` — one guard, everything else untouched.
Swept over golden + rood + negatief + parity (715 documents) exactly three
documents move: this case and its two siblings turn clean again. No
golden, no red case and no other negatief changes a verdict, and the
mutant's reports are byte-identical to the pre-refusal verifier's
everywhere. The one-argument count is untouched by construction: the arm
reads `arity >= 2`, and all 109 one-argument counts in those zones keep
their reports.

Since L4 the node is an `Aggregation` or a `BuiltinCall` (delta B21), each
with only its own fields — `arguments`, and on an aggregation `elementType`,
`emptyFallback`, `dimensionSelection`, `sourceRole` and `sourcePath`, on a
builtin `granularity`; the report cites and names them. The measurements above
name the fields as they were spelled then (`elementObjecttype` →
`elementType`; since L5 `AANTAL` → `count`). `expect.txt` pins the arm.
