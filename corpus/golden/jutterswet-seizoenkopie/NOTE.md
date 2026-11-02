# jutterswet-seizoenkopie — two cells copied from a third

A witness of the cell as a write position (`IR-SPEC.md` ch. 3.1, ch. 3.3;
`RUNTIME-SEMANTICS.md` ch. 5.8c), of the dependency graph per cell
(`IR-SPEC.md` ch. 5.1) and of the standing norm per cell (ch. 7.8).
Hand-written, without a source; `jutterswet-seizoenboeking` is the golden
of a cell write written in RegelSpraak.

## What it carries

A dimension `seizoen` with the labels `voorjaar`, `zomer`, `najaar`; on the
`Jutter` a `vondstgewicht` and on the `Vangst` a `vangstgewicht`, both in
`kg` and dimensioned by it; a fact type `vangstboeking` between the two.

| form | writes | reads |
|---|---|---|
| `najaarsgewicht van een jutter` | the cell `najaar` of `vondstgewicht` | the cell `voorjaar` of `vondstgewicht` |
| `zomergewicht van een jutter` | the cell `zomer` of `vondstgewicht` | the cell `voorjaar` of `vondstgewicht` |
| `boek een vangst bij elke jutter` (object creation) | the cell `najaar` of the created `vangstgewicht` (`MemberInit.labels`) | the cell `najaar` of the subject's `vondstgewicht` |

What the verifier admits here, and refuses one change away:

- a `DimensionCellRead` as a rule target, and `labels` on a `MemberInit`
  (WF-022, WF-040, WF-125 (b)): `corpus/negative/wf125-whole-target`,
  `wf125-member-without-labels`, `wf040-target-label-count`;
- a cell computed from another cell of the same attribute is no cycle: the
  graph has a node per cell, and its one edge runs from
  `najaarsgewicht van een jutter` to the creation over the cell
  `attr:jutter:vondstgewicht[najaar]` (`verifier --dependency-edges`). Under
  the graph per attribute, of the verifier before this golden, a rule that
  writes `vondstgewicht` from a cell of `vondstgewicht` was a WF-045 cycle
  (measured 2026-11-02 on mutant j of the dimension research);
- two unconditional computing rules of different forms on two cells of one
  attribute write two targets (WF-079): `corpus/negative/wf079-two-writers-one-cell`
  puts both on `najaar`.

## The case

Hand-computed. `case1-kopie-en-staande-invoer`: J1 with the cells `voorjaar`
12.50 and `najaar` 4.00 gives `najaar` 12.50 (the computed value displaces
the input of the cell) and `zomer` 12.50; J2 with the cell `zomer` 7.00
alone gives `zomer` 7.00 and `najaar` empty. The copy of an empty cell is
leeg, and the rule on `zomer` is that cell's only writer, so it is a
standing target (ch. 7.8): a leeg evaluation writes nothing and the input
stands. The case is no witness of the creation, whose object no case can
name; `jutterswet-seizoenboeking` carries the cells of a made object back to
a jutter, and `ref/test` reads them there.

## Where the executors stand

Measured 2026-11-02: `ref`, the ASP backend and the Drools backend replay the
case (gates 5, 6 and 12; `ref/test` `WV-5.8c-schrijven-staande-invoer`; the
Drools backend on both branches, `PLAYED 1`): a cell is a standing target of
its own in all three. Its rule graph carries the cells, and gate 15 compares
it with the verifier's.
