# vintage-uncovered-date

The ch. 8.4 session-start coverage error: the document vintage-reads
the parameter `penningbedrag` at the `Rekendatum` constant, but the
Parameterset's timeline (2025–2026) does not cover the evaluation date
2028-03-15. Both executors run the same shared session-start check —
the reference interpreter at evaluation entry, the ASP backend before a
program is emitted — so both fault with the same report: a coverage gap
is a session-start error, never a default and never a mid-evaluation
leeg. The document is the `jutterswet-penningvintage` anchor verbatim;
only the case is red.
