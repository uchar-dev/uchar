Violates WF-067 (`spec/IR-SPEC.md` ch. 2.11): the body of the function
`opgehoogd saldo` reads an attribute (`AttribuutRef` to `Jutter.saldo`). A
function body is closed — it may reference only its own parameters
(`BinderRef`), document parameters (`ParameterRef`), literals, operators,
`Construct`/`Match`/`IfThenElse`, and other functions via `Invoke`; any
attribute, characteristic, aggregation, temporal or variable read is
ill-formed.

A minimal hand-built document.
