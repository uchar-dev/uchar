Violates WF-090 (`spec/IR-SPEC.md` ch. 2.4) at the third write position —
a prioritized-defaults consequence. The `PrioritizedDefaults` on
`Jutter.samenvatting` (declared with the struct domain `dossier`) carries
one default whose consequence is a `StructLiteral` of `logboek`
(`initialisatie: true`); a rule reads `samenvatting.waarde` faithfully.

    WF-090 FAIL /prioritizedDefaults/0/defaults/0/consequentie: a value of
    the struct domain 'logboek' is assigned to a target of the struct
    domain 'dossier'

Why it has to exist beside the other two write cases: the ch. 7 rules
shape the group, none types the written value, and the walk recurses into
exception groups which inherit the root target — the same recursion
`wf023-defaults-exceptiegroep-domein` pins for the keuzetype clause.
Verifier-clean before; ref faulted (`field "waarde" is not present on the
record value`) where the faithful twin evaluates the whole document
without a fault. Clingo carries neither this document nor its faithful
twin: both stop at `TRANSLATION FAULT: unscalable datatype in the ASP
translation`, the pre-existing carrier fence on struct-valued defaults —
so ref is the only executor witness here, measured, not assumed.

Falsifiable, arm-exact: suppress WF-090 at write position 3 only (filter
`x.wf <> "WF-090"` off the `wf023_assignment` result in the defaults
branch of `wf023`) and this case alone turns clean; the rule-target and
object-creation cases stay red.
