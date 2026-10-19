Violates WF-033 (`spec/IR-SPEC.md` ch. 3.9): an object creation in a recursive
rule group MUST carry a guard, and here the second version of one does not.
Broken from `corpus/golden/anchor/jutterswet-wachtersketen`: the creation
`werf de opvolger`, the member of the recursive group `estafette`, is split
into two versions with disjoint validity — the golden's version, guard
included, valid to 2024-12-31, and a copy valid from 2025-01-01 without its
`condition`.

    WF-033 FAIL /rules/ruleGroups/0: unguarded object creation 'werf de
    opvolger #2' in a recursive group

Rood-eerst: until 2026-09-25 the rule coupled a member to its creation by
the form name and read the FIRST version it found — here the guarded one —
so the unguarded second version passed; the verifier of that day rejected
the document on WF-052 alone and gate 3 reported it `rejected, but not for
WF-033`. Since then every version of the named creation is read, and a
version of a form with several is named by its number (`#2`).

Until the WF-052 decision of 2026-09-25 the document also carried a WF-052
line (`connecting relation 'opvolging' has a to-one created-side role and 2
object-creation writers`): that rule counted the two versions of one form as
two writers, although their validity windows are disjoint (WF-021 holds).
Since then the versions of one object creation are one writer, and the report
is the WF-033 line alone.
`expect.txt` pins the arm.
