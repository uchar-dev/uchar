# duplicate-instance-label

The case input carries two instances under the label `S1`: a `Schuit` of
romplengte 12 and a `Schuit` of romplengte 7. A role link and an expectation
name an instance by its label, so a label names one instance; the second
`S1` would otherwise land on the first or replace it. Every executor refuses
the case while it reads it, before any rule fires, with the fault kind
EF-039 of the case input (RUNTIME-SEMANTICS ch. 5.7 step 3, ch. 5.7b) and
the label as its parameter.

The document is the one of `unknown-role-target-label`: `H1` is a `Haven`
whose `liggers` include `S1`. ref and the ASP backend share one case reader
(`Case.load_file`), so for a fault of the case input this zone is a
regression fence for those two; the Drools backend reads the case with a
reader of its own (`CaseInput`) and reports the same code and parameter.
