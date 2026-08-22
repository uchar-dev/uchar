Violates WF-064 (`spec/IR-SPEC.md` ch. 4.7): the `BinderRef` `bedrag`
sits outside any enclosing binder — there is no match arm binding it.
Every `BinderRef` must resolve to an enclosing binder of its name
(lexically). `VariabeleRef` stays outside this rule (row 24).

A minimal hand-built document.
