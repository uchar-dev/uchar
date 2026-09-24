Violates WF-054 (`spec/IR-SPEC.md` ch. 3.6): a parent-scoped uniqueness
whose scope binding binds the role `kavel`, a to-many role. A scope parent
is one instance; a to-many scope role names many.

    WF-054 FAIL /rules/uniciteiten/0/versions/0/scope: the scope role 'kavel' is
    to-many — a scope parent is one instance

**Rewritten at L2 (2026-09-24), stamp: herschreven (m).** (First stamped
(i); the L2 fix round measured the document byte-identical to the
migration's translation of its L1 form.) Until L2 the scope
was the written field `kavel` without a binding, and the verifier derived
the role by name. Since L2 the binding IS the scoping (B8, D12): the
migration wrote `scopeRol` from the written name, and the to-many role is
what remains. `expect.txt` pins the arm (U1).

Since L4 the uniqueness is a `Uniqueness` with one `Source` (delta B27), and
the scope binding is the version's `scope`, which the report cites.
`expect.txt` pins the arm.
