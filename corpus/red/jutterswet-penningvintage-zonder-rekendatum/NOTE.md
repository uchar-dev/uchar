# jutterswet-penningvintage-zonder-rekendatum

The ch. 8.4 session-start check: the document vintage-reads the parameter
`penningbedrag` at the `evaluationDate` context value, and the session carries
no evaluation date (`asOfDate` null). The evaluation date is the context value
the vintage read needs, so the fault is EF-011, raised at session start before
any rule fires, whether or not an instance exists for the read. The document
is the `jutterswet-penningvintage` anchor verbatim; only the case is red.

ref and the ASP backend share the session-start check and both report EF-011
without parameters. The Drools backend refuses the document at load
(`EXCLUDE DROOLS-023`: it does not carry the vintage read), as for
`vintage-uncovered-date`.
