Violates WF-090 (`spec/IR-SPEC.md` ch. 2.4) on the READ side, the
`BinderRef` position: inside the `MapList` lambda the parameter `d` is
declared with the struct domain `dossier` (faithfully, against the
list-typed source it maps), while the `BinderRef` reading it is typed
`logboek` and the body reads `.bedrag`. WF-078 crosses a `BinderRef`
against its binder in **soort** only — struct against struct — so the lie
passed.

    WF-090 FAIL /rules/resolvedRegels/1/waarde/list/function/body/base/type:
    binder reference 'd' is typed with struct domain 'logboek', but the
    declaration it reads from names struct domain 'dossier'

Why it has to exist beside the attribute cases: the binder environment is
its own declaration source (a match arm's payload, a lambda or function
parameter, a `Let` value, a filter element), and no attribute is misread
here. Verifier-clean before; ref faulted (`field "bedrag" is not present
on the record value`) and clingo folded a **wrong answer**,
`J1.totaalwaarde = [0]` where 12.50 was expected.

Falsifiable, arm-exact: call `wf089_domain_identity` instead of
`use_site_identity` in the `Binder_ref` arm of `walk_expr` and this case
alone turns clean; the attribute, parameter, dimension-cell, fold-source,
aggregation and write cases stay red.
