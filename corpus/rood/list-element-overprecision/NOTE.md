# list-element-overprecision

A list element value whose exact rational does not fit the declared
decimals (40.005 on the 2-decimal `waarde` field of a `dossier` element
of the `dossiers` list) MUST fault in both executors at the input
boundary: the reference interpreter's precision fence, now recursing
into list elements and their nested records (`expectedFault.ref` =
"does not fit 2 decimals"), and the ASP backend's per-element integer
scaling (`expectedFault.clingo` = "does not fit 2 decimals"). This is
the same gap as struct fields, open since the Phase-5b list kernel: ref
kept precision inside a list where the ASP side faulted. Closed here.

The document is the jutterswet-vondstdossier anchor verbatim; only the
case is red.
