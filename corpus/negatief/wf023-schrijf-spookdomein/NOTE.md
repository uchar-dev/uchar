Violates WF-023 (`spec/IR-SPEC.md` ch. 3.1) on its domain clause with a
GHOST domain: the value written into `vaarwater` (declared `Vaarwater`) is
a `Literal` `Noord` typed `Enumeration` of domain `Spook`, which `domains`
does not declare. WF-088 is a repeat check and calls a use site naming no
declared domain "not its business"; WF-003 covers the model layer only;
WF-039 checks the label against the LOCAL list, which contains `Noord`.

    WF-023 FAIL /rules/rules/0/versions/0: a value of the enumeration
    domain 'Spook' is assigned to a target of the enumeration domain 'Vaarwater'

Why it has to exist: this is the "ordinary expression `type` naming a
ghost domain" flank WF-088's clause records as unenforced — at a WRITE
position it is now enforced, because the declared target names a domain
and the value names another (declared or not, the identity fails). Before
the clause the document was verifier-clean; ref faulted `match has no arm
for enumeration value "Noord"` at the faithful reader, clingo left
`B.tarief` underived. Where a ghost-domain literal touches no declaration
at all (compared with another such literal) it stays unenforced and
harmless — measured, both executors PASS.

Falsifiable: make the two-sided domain-mismatch branch of
`wf023_assignment` return `[]` and this case passes.
