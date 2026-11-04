# context-value-behind-a-holding-condition

The document of `corpus/golden/context-value-behind-a-failing-condition`, with
V1 actief and no evaluation date (asOfDate null). The value of "peildatum van
een vondst" reads the evaluation date and the second member of the condition of
"gemelde vondst" the evaluation year; each is a fault, EF-011
(`spec/RUNTIME-SEMANTICS.md` ch. 8.2, 5.7b). The case pins no parameter: the
`contextValue` of the row is emitted by no executor.

Every executor faults where it evaluates the read: `ref` in its evaluation,
the ASP backend at solving (a `viol/2` witness whose body is the reach of the
read), and the Drools backend in its red run.
