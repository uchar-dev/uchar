# wf104-supertype-zelfverwijzing

Violates **WF-104** (`spec/IR-SPEC.md` ch. 2.2): `Wrakrecht` names itself.

    WF-104 FAIL /objecttypen/Wrakrecht/supertype: supertype cycle (a type
    would be its own ancestor, so member resolution has no root to stop at):
    Wrakrecht -> Wrakrecht

The depth-1 cycle, kept as a second witness beside the two-step one because
the two fall independently: a reader that special-cases the self-edge — the
cheapest way to write the check, and a tempting one, since a self-edge looks
like a harmless no-op — passes the two-step case and not this one.
