# irrational-power-without-rounding — an irrational power that no rounding follows

The wortelmaat of a Vondst, an unbounded number, is its gewicht to the power
0,5, without a rounding. RegelSpraak demands a rounding after a power and a
square root, so this document has no `source.rgs`: it is the IR of
`fractional-power-negative-base` with the rounding taken out and the target
declared without decimals (WF-023 holds a target of 2 decimals to a rounding).
V1's gewicht is 2, whose square root is irrational, and an irrational power
or root exists only under its rounding (RUNTIME-SEMANTICS ch. 2.2): the
session faults with EF-045 in the reference interpreter and both branches of
the Drools backend. The ASP backend refuses the fractional exponent with
CLINGO-041 (gate 8's divergence list).
