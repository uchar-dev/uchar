# jutterswet-opvolgerversies — two versions of one creation, one writer

The witness that the versions of one object creation are ONE writer of the
relation they create (WF-052, RUNTIME-SEMANTICS ch. 7.4: version selection
is temporal). A `Wachter` recruits an `opvolger` over the fact type
`opvolging`, whose created-side role is to-one (`Eén voorganger heeft één
opvolger`). The creation `werf de opvolger` is the member of the recursive
group `estafette` and has two versions with disjoint validity, both guarded
(`rang < 3`):

| version | validity | the new `opvolger`'s `rang` |
|---|---|---|
| 1 | 2025-01-01 to 2025-12-31 | the Wachter's `rang` + 1 |
| 2 | from 2026-01-01 | the Wachter's `rang` + 2 |

`opvolgerrang` is the sum of the `rang` of all opvolgers of a Wachter, or 0
if there are none.

## Hand-computed absolutes (one Wachter W1, `rang` 1)

| case | evaluation date | the chain | W1.opvolgerrang |
|---|---|---|---|
| case1-geen-versie | 2024-06-01 | no version is valid: no creation | **0** |
| case2-eerste-versie | 2025-06-01 | W1 (1) → 2 → 3, and 3 is not below 3 | **2** |
| case3-tweede-versie | 2026-06-01 | W1 (1) → 3, and 3 is not below 3 | **3** |

Each case is consistent. `ref`, the ASP backend and both Drools branches
replay the three cases with these values (measured 2026-09-25; Drools
PLAYED 3 in each branch), and a do-nothing run mismatches every one.

## What it pins

Until 2026-09-25 WF-052 counted each version as a writer and refused the
document (`WF-052 FAIL /rules/objectCreations/0/versions/1: connecting
relation 'opvolging' has a to-one created-side role and 2 object-creation
writers; at most one may write it`), so the form could not stand in the
corpus. Since then a writer is a form: the two versions never co-apply,
and whether versions overlap is WF-021's question. Two DIFFERENT
creations on one to-one role stay refused
(`corpus/negatief/wf052-two-writers-one-to-one-relation`), and two
overlapping versions of one creation are WF-021's
(`corpus/negatief/wf021-creatie-versies-overlappen`).

The source is `bron.rgs`; the RegelSpraak frontend writes the two
versions as one form and makes the creation a member of its group, and
gate 10 re-emits this document byte-identically.
