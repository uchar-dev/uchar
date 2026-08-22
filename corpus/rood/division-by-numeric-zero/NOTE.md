# division-by-numeric-zero

The document is the jutterswet-vindersloon anchor's twin: `deel` divides
`opbrengst` by `aandeel`. Here `aandeel` is a FILLED numeric zero — not
empty. The empty-value discipline (RUNTIME-SEMANTICS ch. 1.2a, §6.5)
routes an *empty* divisor to the §9.5 channel beside an empty result; a
*numeric zero* divisor is a hard fault in both executors — the
**v0-mirror behaviour that RUNTIME-SEMANTICS ch. 1.4 point 4 (PLAN §9
decision 7) explicitly keeps as the mirror**, while the same decision
routes this case into the §9.5 channel in v1: a decided, tracked delta
(recorded in `ASP-TRANSLATION.md` S6/S15) that both executors will
take together when the v1 divergence lands. Until then: ref faults in
`arith.div_gen`, and the clingo translation derives a `viol/2` witness
whose reason the UNSAT report names. The distinction is load-bearing
twice over: an executor that coalesces the zero-divisor case into the
empty-divisor channel (or into a 0 result) *today* would silently
change the legal meaning of a filled zero — and when the v1 delta
lands, this case flips to expecting the channel fact in both executors
at once, never one at a time.
