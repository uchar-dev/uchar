Violates WF-090 (`spec/IR-SPEC.md` ch. 2.4) in the **fold-source**
reference position: the `AttribuutRef` that feeds
`EERSTE(kern van alle schuiten van de Berging)` carries a `type` naming
`dossier` while the attribute it reads — `kern` of `Schuit` — is declared
with the struct domain `logboek`. The fold node and its operand agree with
each other and the target agrees with the fold node; the lie sits one
level down, in the reference.

    WF-090 FAIL /resolvedRegels/0/waarde/argumenten/0/type: fold-source
    attribute reference 'kern' is typed with struct domain 'dossier', but
    the declaration it reads from names struct domain 'logboek'

Why it has to exist: a fold source with an `elementObjecttype` is the one
reference position the expression walk never enters — it is routed to the
fold-source shape checks instead — so an arm applied only in `walk_expr`
would never see it. The struct twin of `wf089-foldbron-enum`, and the same
routing hole. Verifier-clean before; ref faulted (`field "waarde" is not
present on the record value`). Clingo carries neither this document nor
its faithful twin: both stop at `TRANSLATION FAULT: expression kind not
translated (no corpus case exercises it)`, measured on the faithful twin
too, so ref is the executor witness here.

Falsifiable, arm-exact: call `wf089_domain_identity` instead of
`use_site_identity` in the fold-source branch of the argument walk
(keeping the shape check) and the WF-090 line disappears here, while
`wf090-doorgeefluik` — whose lie is on the fold node — keeps its own.
Re-measured after the WF-038 ordering-fold clause landed (2026-09-05):
two reports change under that mutant, this one and one WF-091 line on
`wf091-foldbron-eenheid` (the same branch carries the unit facet), and
this document does not turn clean, since a struct contributor under an
`EERSTE` violates the ordering-fold clause on the same operand. The
falsification signal is the rule cited: under the mutant it is rejected
for WF-038 and no longer for the WF-090 its directory declares.
