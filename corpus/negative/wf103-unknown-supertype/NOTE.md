# wf103-unknown-supertype

Violates **WF-103** (`spec/IR-SPEC.md` ch. 2.2): `Wrakrecht` declares a
supertype no declaration carries.

    WF-103 FAIL /model/objectTypes/0/supertype: supertype 'Strandrecht' is
    not a declared object type: the chain ends at a name nothing declares, so
    no member resolves through it

WF-003 already says every model-layer name reference resolves; this rule
takes a number of its own because the failure mode is specific — the chain
that a member lookup walks simply stops at a name, and every reader then
answers "not declared" for a member the author believes is inherited — and
because a witness has to be able to cite it.
