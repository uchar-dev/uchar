# division-by-numeric-zero

The document is the jutterswet-vindersloon anchor's twin: `deel` divides
`opbrengst` by `aandeel`. Here `aandeel` is a FILLED numeric zero — not
empty. The empty-value discipline (RUNTIME-SEMANTICS ch. 1.2a, §6.5)
routes an *empty* divisor to the §9.5 channel beside an empty result, but
a *numeric zero* divisor is a hard fault in both executors (PLAN §9
decision 7): ref faults in `arith.div_gen`, and the clingo translation
derives a `viol/2` witness whose reason the UNSAT report names. The
distinction is load-bearing: an executor that coalesces the zero-divisor
case into the empty-divisor channel (or into a 0 result) would silently
change the legal meaning of a filled zero.
