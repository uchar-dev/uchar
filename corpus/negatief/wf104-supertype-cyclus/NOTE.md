# wf104-supertype-cyclus

Violates **WF-104** (`spec/IR-SPEC.md` ch. 2.2): `Strandrecht` and
`Wrakrecht` name each other as supertype.

    WF-104 FAIL /objecttypen/Strandrecht/supertype: supertype cycle (a type
    would be its own ancestor, so member resolution has no root to stop at):
    Strandrecht -> Wrakrecht -> Strandrecht

Member resolution walks the chain upward until it runs out of parents. On a
cycle it never does, so every reader must either loop or cut the walk off at
a private depth of its own — which is the same class of failure WF-085
refuses on the day-kind graph, and the check is that rule's colour DFS with
a named cycle path.
