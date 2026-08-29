# jutterswet-wachtgeld — unordered exception group (Uitzonderingen ongeordend)

Exercises the uchar-dialect **unordered group** form
(PRIORITIZED-DEFAULTS-EVOLUTIE §4.2, the §9.5 surface): three grounds
writing ONE shared attribute target `wachtgeld`, lowered by the RegelSpraak
frontend to THREE sibling declared-unordered `PrioritizedDefaults` entries
(the `toelagegronden` anchor shape). Own Jutterswet-flavoured domain, own
numbers.

## Source (`bron.rgs`)

An `Uitzonderingen ongeordend:` group with three members, each a gelijkstelling
writing `wachtgeld`:

- **wachtgeld voor kustwachters** — `wachtgeld = 30` when `kustwacht`.
- **wachtgeld voor havenmeesters** — `wachtgeld = 45` when `havenmeester`.
- **wachtgeld voor strandvoogden** — `wachtgeld = 30` when `strandvoogd`.

## Lowering

Three sibling `PrioritizedDefaults` entries on `Jutter.wachtgeld`, each with
`ongeordend: true` (WF-050: two-or-more default groups on one target require
the marking on every one) and a single attribute default (`besluit: null`,
`consequentie` the value).
All three members leave `resolvedRegels`; each keeps a `Regel` stub in
`regels`. Because a RegelSpraak kenmerk assignment is monotone `waar`, a
genuine §9.5 conflict is only expressible on an **attribute** target (two
members writing different values) — hence the attribute group here.

## §9.5 reconciliation (RUNTIME-SEMANTICS ch. 7.2)

For declared-unordered default groups on one target: members that fire and
DECIDE THE SAME value reconcile idempotently (one write); members that fire
and decide DIFFERENT values produce NO write plus one path-keyed §9.5
Inconsistentie keyed `Jutter.wachtgeld`.

## Case derivations (hand-computed, absolute)

| case | characteristics | members firing | decisions | outcome |
|------|-----------------|----------------|-----------|---------|
| case1-enkel    | kustwacht              | M1        | {30}     | `wachtgeld = 30`, consistent |
| case2-conflict | kustwacht, havenmeester| M1, M2    | {30, 45} | no write → `wachtgeld` empty + **inconsistent** `Jutter.wachtgeld` |
| case3-gelijk   | kustwacht, strandvoogd | M1, M3    | {30, 30} | idempotent → `wachtgeld = 30`, consistent |
| case4-geen     | (none)                 | none      | {}       | no member fires → `wachtgeld` empty, consistent |

case2 is the §9.5 conflict (two members decide differently); case3 is the
agree case (two members decide the same); case1 and case4 pin the single-fire
and no-fire boundaries.
