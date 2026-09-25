# wf091-setbron-som-datum

Violates WF-091 (`spec/IR-SPEC.md` ch. 4.1): the operand of a `total` or
`sum`, and the base of the pro-rata part, is not a Number.

Origin: the red case `corpus/rood/datumsetbron-somvouw`, whose `ir.json` this is,
byte for byte. The verifier passed it until the numeric-operand arm of
WF-091 existed; the arm and its report follow in the next commit of the
card ref-klipvouw-nul.
