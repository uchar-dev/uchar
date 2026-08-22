# struct-field-overprecision

A record field value whose exact rational does not fit the field's
declared decimals (40.005 on the 2-decimal `waarde` field of the
`dossier` struct) MUST fault in both executors at the input boundary:
the reference interpreter's precision fence, now recursing into record
values (`expectedFault.ref` = "does not fit 2 decimals"), and the ASP
backend's per-field integer scaling (`expectedFault.clingo` = "does not
fit 2 decimals"). Before the fence recursed, ref kept the over-precise
rational inside the struct — faulting only late at the write boundary
on a derived value, or not at all when no bounded write reads it — while
the ASP side already faulted at the field. A divergence on schema-valid
input, closed here.

The document is the jutterswet-vondstdossier anchor verbatim; only the
case is red.
