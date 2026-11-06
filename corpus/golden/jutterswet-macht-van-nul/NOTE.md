# jutterswet-macht-van-nul

Nul tot een negatieve macht (`RUNTIME-SEMANTICS.md` ch. 1.4 point 4, ch. 2.2):
a negative exponent inverts the power of its base, and the inverse of a
numeric zero is a division by it. The power is leeg and deposits one §9.5 fact
of kind EF-005 on the rule × subject that evaluates it, as 1 ÷ 0 does. The
document raises the gewicht of a Vondst to the power −1 and −0,5 under a
rounding: in the value of "lichtheid" and "wortelmaat", in the condition of
"lichte vondst", and in the filter of the fold of "lichte vondsten", which
deposits on the Jutter that reads the fold.

Case 1: V1 has gewicht 0, V2 4 and V3 10. V1's lichtheid and wortelmaat are
leeg, V1 is not licht (a comparison with leeg does not hold), and four facts of
kind EF-005 stand: on V1 for "lichtheid", "wortelmaat" and "lichte vondst", on
J1 for "lichte vondsten". V2 is 0,25 and 0,5 and licht; V3 0,1 and 0,31. Case
2 has no zero: consistent.

Measured 2026-11-06 (card `ref-macht-van-nul`): until that card the reference
interpreter answered 0 to the power −1 with Zarith's infinity, which the
rounding then divided by, `Fatal error: exception Division_by_zero`; both
branches of the Drools backend faulted with the runtime's
`ArithmeticException: Breuk met noemer 0`. The ASP backend does not translate
a negative exponent and refuses the golden.
