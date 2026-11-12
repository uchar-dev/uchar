# fractional-power-negative-base — a negative base under a fractional exponent

The wortelmaat of a Vondst is its gewicht to the power 0,5, rounded down to 2
decimals. A negative base has no real power under any fractional exponent,
with or without a rounding (`RUNTIME-SEMANTICS.md` ch. 2.2): the power is leeg
and deposits one §9.5 fact of kind EF-044 on the rule × subject that evaluates
it, as a negative radicand is leeg under the root (ch. 1.2).

Case 1: V1's gewicht is −4. Its wortelmaat is leeg and one fact of kind
EF-044 stands on V1 for "wortelmaat". Case 2: V1's gewicht is 9, 9^0,5 = 3
exactly, consistent.

Until 2026-11-12 this document was the red case of EF-044 on the fault
channel: the reference interpreter and both branches of the Drools backend
broke the session off on the negative base, while the square root of the same
negative number was leeg with its deposit. The ASP backend translates a whole
non-negative literal exponent only and refuses the golden with CLINGO-041.
