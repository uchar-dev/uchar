# jutterswet-penningvintage-defaults-zonder-tijdlijn

The ch. 8.4 session-start check reaches every vintage read of the document,
the consequence of a default included. The document is the
`jutterswet-penningvintage` anchor with its rule written as a defaults group:
the one default reads `penningbedrag` at the evaluation date 2025-06-15, and
the Parameterset carries no timeline for it. The read is of an unbound
parameter: EF-014 naming `penningbedrag`, raised at session start before any
rule fires, with no Jutter (case1) and with one (case2). Case1 is the reason
the check walks the document and not the objects: without an instance no
default is ever evaluated, and the fault still stands.

ref and the ASP backend both report EF-014 with `parameter` = `penningbedrag`.
The Drools backend refuses the document at load (`EXCLUDE DROOLS-023`: it does
not carry the vintage read), as for `jutterswet-penningvintage-zonder-tijdlijn`.
