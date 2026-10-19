# division-by-numeric-zero

The document is the jutterswet-vindersloon anchor's twin: `deel` divides
`opbrengst` by `aandeel`. Here `aandeel` is a FILLED numeric zero — not
empty. Since 2026-10-19 (card `zero-division-channel`) v1 routes a filled
dividend divided by a numeric zero into the §9.5 channel
(`RUNTIME-SEMANTICS.md` ch. 1.4 point 4): the quotient is leeg and the rule
deposits a fact of kind EF-005 on its subject, as a leeg divisor does with
EF-031. The case pins it: V1's `deel` is leeg and the channel carries
`deel van een vondst × V1` with code EF-005; every other rule writes as the
anchor's twin does.

The distinction from the empty divisor stays load-bearing: the two kinds
share the channel and differ in their code, so an executor that coalesced
the numeric zero into the empty-divisor kind (or into a 0 result) fails
this case on the code or on the value.

Until 2026-10-19 this was the red case `corpus/red/division-by-numeric-zero`:
the v0-mirror hard fault EF-005 in ref (`arith.div_gen`), in the ASP
backend (a `viol/2` witness) and in the Drools backend (`DelingDoorNul`).
The production runtime still faults there; uchar's executors deposit.
