# input-overprecision

A case value whose exact rational does not fit the attribute's declared
decimals (40.005 on a 2-decimal attribute) MUST fault in both executors
at the input boundary: the reference interpreter's precision fence
("does not fit 2 decimals") and the ASP backend's integer scaling
(idem). Before the fence the reference interpreter silently kept the
over-precise rational while the ASP side faulted — a divergence on
schema-valid input (the standing-norm card's breaker find).

The document is the jutterswet-waardestelling anchor verbatim; only the
case is red.
