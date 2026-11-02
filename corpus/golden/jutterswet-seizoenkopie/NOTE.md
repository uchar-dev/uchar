# jutterswet-seizoenkopie — the cell written, verifier-only

A witness of the cell as a write position (`IR-SPEC.md` ch. 3.1, ch. 3.3;
`RUNTIME-SEMANTICS.md` ch. 5.8c) and of the dependency graph per cell
(`IR-SPEC.md` ch. 5.1). Hand-written, without a source: the RegelSpraak
frontend refuses a cell as the target of a rule (RGS-307, RGS-308).

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

## Why no replay case

Measured 2026-11-02, with a case that supplies the cells `voorjaar` 12.50
and `najaar` 4.00 and with a case without input:

| executor | with cells | without input |
|---|---|---|
| verifier (gate 3) | `verifier: PASS` | — |
| `ref` (gate 5) | `CASE ERROR: j1.vondstgewicht: a cells input is checked and then refused: neither executor that shares this reader evaluates a cell` | `EVALUATION FAULT: REF-012: rule "najaarsgewicht van een jutter": unsupported target shape` |
| `clingo` (gate 6) | the same case error (the case reader of `ref`) | `TRANSLATION FAULT: CLINGO-026: unsupported rule target` |
| Drools (gate 12) | `EXCLUDE DROOLS-298: loader: a write of a cell of the dimensioned attribute 'vondstgewicht' by 'najaarsgewicht van een jutter': this branch writes no cell` | the same |

So a case here pins nothing yet. Gate 12 lists the golden as `NOCASE`, and
gate 15 reports it among the goldens the Drools loader refuses.
