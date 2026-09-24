Violates WF-031 (`spec/IR-SPEC.md` ch. 3.7): a day-kind condition reads role
membership — the `RoleMembership` check that the subject fills the role
`wachtjutter` of the fact type `wachtindeling`. Role membership is derived
fact-base state and falls outside WF-031's closed admission list (dates,
date components, eerste paasdag, literals, parameters and other day kinds).

    WF-031 FAIL /rules/resolvedDagsoorten/0/voorwaarde: day-kind condition
    reads role membership 'wachtjutter' — a day-kind voorwaarde references
    only dates, date-components, eerste-paasdag, literals, parameters and
    other day kinds, never derived object state (an attribute or
    characteristic read)

**Rewritten at L2 (2026-09-24), stamp: herschreven (i).** L2 merged the two
role-check nodes into `RoleMembership {factType, role, negated}` (B9),
which names its fact type instead of deriving it from the subject. A
day-kind condition has no subject, so the migration had nothing to derive
from and refused the document; it was written by hand with the fact type
named. The WF-034 line the old node drew for its missing filler type has no
successor: the node names its relation, and the declarations resolve.
