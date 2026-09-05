Violates WF-089 (`spec/IR-SPEC.md` ch. 2.4) in the **fold-source**
reference position: the `AttribuutRef` that feeds
`EERSTE(kust van alle schuiten van de Berging)` carries a `type` naming
`Vaarwater` while the attribute it reads — `kust` of `Schuit` — is
declared `Kust`. Here the fold node and its operand agree with each
other; the lie sits one level down, in the reference.

    WF-089 FAIL /resolvedRegels/0/waarde/argumenten/0/type: fold-source
    attribute reference 'kust' is typed with keuzetype domain
    'Vaarwater', but the declaration it reads from names domain 'Kust'

Why it has to exist: a fold source with an `elementObjecttype` is the one
reference position the expression walk never enters — it is routed to the
fold-source shape check (`WF-053`/`WF-080`/`WF-083`/`WF-034`) instead of
the generic expression walk, so WF-089's `AttribuutRef` arm never saw it.
That made it a routing hole rather than a rule gap: the same lie one
position to the left is `wf089-attribuutref-enum`. Verifier-clean before
the arm was applied here too; ref faulted `match has no arm for
enumeration value "Noord"`, clingo refused the translation on its
pre-existing enum-operand fence.

Falsifiable: drop the `wf089_domain_identity` call from the fold-source
branch of the argument walk (keeping the shape check) and the WF-089
line disappears here, while `wf089-aggregatie-doorgeefluik-enum` and
`wf089-minimum-doorgeefluik` — whose lie is on the fold node — keep
theirs. Re-measured after the WF-038 ordering-fold clause landed
(2026-09-05): three reports change under that mutant — this one,
`wf090-foldbron` (the struct twin, which the same branch carries) and
one WF-091 line on `wf091-foldbron-eenheid` — and none of them turns
clean any more, because an `EERSTE` over an enumeration contributor
violates the ordering-fold clause on the very same operand. The
falsification signal is the rule cited: under the mutant this document
is rejected for WF-038 and no longer for the WF-089 its directory
declares.
