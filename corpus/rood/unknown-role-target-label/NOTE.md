# unknown-role-target-label

The **referential-integrity arm** of the case readers: a role link that
names a label the case never declares. `H1` is a `Haven` whose `liggers`
are `S1` and `S9`; only `S1` is an instance. Both executors refuse with
the same sentence — by construction, not as independent confirmation:
ref and the ASP backend share one case reader (`Case.load_file`), so for
load faults this zone is a regression fence, not a differential:

    role "liggers" on "H1" links unknown label "S9"

The second witness of the red zone's load-fault contract, and
deliberately a different reader arm from `ambiguous-case-role-key`: that
one fails while a single instance is being read, in the role-key name
resolution; this one fails in the cross-instance pass that runs after
the whole input is built, where a link can first be seen to point at
nothing. A contract that rests on one arm is a contract about one bug.

The dangling label is a *typo*, not a shape error: the case schema
(gate 4) admits any non-empty string as a role target, and the document
(gate 3) is verifier-clean — nothing in either layer can know which
labels a case happens to declare. Cross-instance reference is the case
reader's own obligation, and refusing it loudly is what keeps a
mistyped label from silently shrinking a fold. `S1` is delivered
correctly beside `S9` precisely so the refusal cannot be read as "the
loader rejects this role": the good link and the bad one sit under one
key, and only the bad one is named.

## Not carried by the Drools backend

Measured 2026-09-19: the backend's own loader refuses the same input,
naming the same unknown label (`role 'liggers' names unknown label S9`),
but it refuses it as a form it does not carry — an `EXCLUDE` in the red
leg, which pins a `drools` fragment only on a `FAULT`. Gate 8 therefore
lists the case with that reason rather than pinning a fragment. The agreement is real and worth recording even though
the leg cannot pin it: all three executors reject this case, and no
executor computes anything from it.
