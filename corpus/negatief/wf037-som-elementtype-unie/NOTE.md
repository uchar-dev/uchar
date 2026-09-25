# wf037-som-elementtype-unie

Violates WF-037 (`spec/IR-SPEC.md` ch. 4.3) in its fold-source arm: an
aggregation that carries `elementType` takes one argument, its fold
source, and under a value fold that source is the attribute read on each
contributor, an `AttributeReference`. This `sum` carries the element type
`Vondst`, the range `vondstaanmelding`/`vondst`, and as its one argument
an `InstanceSetUnion` of two selections: a set of instances, and no value
to add up.

    WF-037 FAIL /rules/rules/0/versions/0/value: a sum aggregation carries
    elementType "Vondst" and an InstanceSetUnion operand: the source of an
    instance fold is an AttributeReference

That is the verifier's one report. The union also stands where WF-110 (a)
forbids it — the single argument of a `count` is its one admissible
position — and until the fix round of the card
aantal-bronarm-en-drools-lader the verifier reported it there too, at
`/rules/rules/0/versions/0/value/arguments/0` (`an instance-set union
stands where nothing consumes it: …`), two rules for one defect. Since
that round WF-110 (a) steps aside where the fold-source arm names the
union, as it steps aside for clause (c) under a count, so the node is
reported once, in the sentence every executor gives: measured with a case
outside the corpus (the population of golden `jutterswet-vondsttelling`),
`ref` and the ASP backend fault on it in this sentence and the Drools
loader refuses it in it, in both branches, before and after that round.

The document is that of `wf037-som-elementtype-literaal` with the union in
place of the literal; the selections are well-formed (two of them, one
population, each with its relation), so the argument's position is the
only defect.

`expect.txt` pins the arm.
