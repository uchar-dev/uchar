Violates WF-090 (`spec/IR-SPEC.md` ch. 2.4) on the READ side, the
`ParameterReference` position: the parameter `grondslag` is declared with
the struct domain `dossier`, the reference to it is typed `logboek`, and
the `FieldAccess` over it reads `bedrag`.

    WF-090 FAIL /resolvedRegels/5/waarde/base/type: parameter reference
    'grondslag' is typed with struct domain 'logboek', but the declaration
    it reads from names struct domain 'dossier'

Why it has to exist: a parameter is a declaration like an attribute, and
nothing else crosses a reference against it — WF-072 pins a
**declaration's** struct use site against its domain, never an expression
`type`. Verifier-clean before; ref faulted (`field "bedrag" is not present
on the record value`), and clingo refused the translation ahead of it —
`a struct field-access base here is an own-subject attribute read, a bound
binder or a struct-typed field access (no corpus case for more)` — the
same shape of carrier fence that leaves WF-089's `ParameterRef` arm
without an executor witness. Ref is the executor witness here.

Falsifiable, arm-exact: call `wf089_domain_identity` instead of
`use_site_identity` in the `Parameter_ref` arm of `walk_expr` and this case
alone turns clean; every other WF-090 case stays red.
