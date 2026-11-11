# jutterswet-penningvintage-zonder-tijdlijn

The ch. 8.4 session-start check: the document vintage-reads the parameter
`penningbedrag` at the evaluation date 2025-06-15, and the Parameterset
carries no timeline for it. The timeline is the binding of a granular
parameter, so the read is of an unbound parameter: EF-014 naming
`penningbedrag`, raised at session start before any rule fires. The document
is the `jutterswet-penningvintage` anchor verbatim; only the case is red.

ref and the ASP backend share the session-start check and both report EF-014
with `parameter` = `penningbedrag`. The Drools backend refuses the document at
load (`EXCLUDE DROOLS-023`: it does not carry the vintage read), as for
`vintage-uncovered-date`.
