# jutterswet-cel-schrijfgrens

The witness of EF-006 on a cell (`spec/RUNTIME-SEMANTICS.md` ch. 2.4 point 1,
ch. 5.7b, ch. 5.8c): the value a rule writes into a cell is collapsed to the
declared representation of the cell's attribute, and a value that does not
fit is a fault. Here "Het vondstgewicht in najaar van een Jutter moet berekend
worden als zijn vondstgewicht in voorjaar maal 1,5", with `vondstgewicht` a
number with 2 decimals, and the case supplies the cell `voorjaar` as 1.25:
1.25 maal 1.5 is 1.875, which does not fit 2 decimals.

The document is the rule `najaarsgewicht van een jutter` of
`corpus/golden/jutterswet-seizoenboeking` with the factor 1,5 in place of 2,
and the `Jutter` with its `vondstgewicht` alone; it is verifier-clean. The
RegelSpraak frontend refuses this rule statically, since its value carries
more decimals than its target, so the document is IR without a source.

`ref` faults with EF-006 and the parameters `place` j1.vondstgewicht[najaar],
`value` 15/8 and `decimals` 2 (the `ref/test` row `WV-5.8c-schrijfgrens-ef006`
pins all three); the ASP backend with EF-006 and `place` vondstgewicht[najaar]
(its fence names the member, without the instance). The pin names no
parameter, as the other EF-006 witnesses do: the two executors spell the place
apart. The Drools backend faults with EF-006 and the parameters of `ref`,
`place` j1.vondstgewicht[najaar], `value` 15/8 and `decimals` 2, on both
branches (the Drools leg of gate 8; `DimensionCellWitnessTest` pins all
three).
